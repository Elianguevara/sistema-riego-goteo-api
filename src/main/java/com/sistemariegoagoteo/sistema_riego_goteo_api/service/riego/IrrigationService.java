package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User; // <-- IMPORTAR
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder; // <-- IMPORTAR
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Objects; // <-- IMPORTAR
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IrrigationService {

    private final IrrigationRepository irrigationRepository;
    private final SectorRepository sectorRepository;
    private final IrrigationEquipmentRepository equipmentRepository;
    private final AuditService auditService;

    @Transactional
    public Irrigation logIrrigation(Integer farmId, Integer sectorId, IrrigationRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca ID " + farmId));

        IrrigationEquipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id", request.getEquipmentId()));

        if (!equipment.getFarm().getId().equals(sector.getFarm().getId())) {
            throw new IllegalArgumentException("El equipo de irrigación ID " + equipment.getId() +
                    " no pertenece a la finca ID " + sector.getFarm().getId() + " del sector.");
        }

        Irrigation irrigation = new Irrigation();
        irrigation.setSector(sector);
        irrigation.setEquipment(equipment);
        irrigation.setStartDatetime(request.getStartDatetime());
        irrigation.setEndDatetime(request.getEndDatetime());
        
        // ... (resto de la lógica de cálculo)
        if (request.getStartDatetime() != null && request.getEndDatetime() != null) {
            irrigation.setIrrigationHours(calculateIrrigationHours(request.getStartDatetime(), request.getEndDatetime()));
            irrigation.setWaterAmount(calculateWaterAmount(equipment.getMeasuredFlow(), irrigation.getIrrigationHours()));
        }

        Irrigation savedIrrigation = irrigationRepository.save(irrigation);

        // --- AUDITORÍA DE CREACIÓN ---
        auditService.logChange(currentUser, "CREATE", Irrigation.class.getSimpleName(), "id", null, savedIrrigation.getId().toString());

        log.info("Registrando riego para sector ID {} con equipo ID {} desde {}", sectorId, equipment.getId(), request.getStartDatetime());
        return savedIrrigation;
    }

    @Transactional
    public Irrigation updateIrrigation(Integer irrigationId, IrrigationRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Irrigation irrigation = getIrrigationById(irrigationId);

        // --- AUDITORÍA DE ACTUALIZACIÓN ---
        if (!Objects.equals(irrigation.getEquipment().getId(), request.getEquipmentId())) {
             auditService.logChange(currentUser, "UPDATE", Irrigation.class.getSimpleName(), "equipment_id", Objects.toString(irrigation.getEquipment().getId(), null), Objects.toString(request.getEquipmentId(), null));
        }
        if (!Objects.equals(irrigation.getStartDatetime(), request.getStartDatetime())) {
             auditService.logChange(currentUser, "UPDATE", Irrigation.class.getSimpleName(), "startDatetime", Objects.toString(irrigation.getStartDatetime(), null), Objects.toString(request.getStartDatetime(), null));
        }
        if (!Objects.equals(irrigation.getEndDatetime(), request.getEndDatetime())) {
             auditService.logChange(currentUser, "UPDATE", Irrigation.class.getSimpleName(), "endDatetime", Objects.toString(irrigation.getEndDatetime(), null), Objects.toString(request.getEndDatetime(), null));
        }

        // ... (resto de la lógica de actualización)
        irrigation.setStartDatetime(request.getStartDatetime());
        irrigation.setEndDatetime(request.getEndDatetime());
        // ...

        log.info("Actualizando registro de riego ID {}", irrigationId);
        return irrigationRepository.save(irrigation);
    }

    @Transactional
    public void deleteIrrigation(Integer irrigationId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Irrigation irrigation = getIrrigationById(irrigationId);

        // --- AUDITORÍA DE BORRADO ---
        auditService.logChange(currentUser, "DELETE", Irrigation.class.getSimpleName(), "id", irrigation.getId().toString(), null);

        log.warn("Eliminando registro de riego ID {}", irrigationId);
        irrigationRepository.delete(irrigation);
    }

    // --- MÉTODOS GET Y DE CÁLCULO (SIN CAMBIOS) ---

    @Transactional(readOnly = true)
    public List<Irrigation> getIrrigationsBySector(Integer farmId, Integer sectorId) {
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca ID " + farmId));
        return irrigationRepository.findBySectorOrderByStartDatetimeDesc(sector);
    }

    @Transactional(readOnly = true)
    public Irrigation getIrrigationById(Integer irrigationId) {
        return irrigationRepository.findById(irrigationId)
                .orElseThrow(() -> new ResourceNotFoundException("Irrigation", "id", irrigationId));
    }

    private BigDecimal calculateIrrigationHours(Date start, Date end) {
        if (start == null || end == null || end.before(start)) {
            return BigDecimal.ZERO;
        }
        long diffInMillis = end.getTime() - start.getTime();
        double hours = (double) diffInMillis / TimeUnit.HOURS.toMillis(1);
        return BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateWaterAmount(BigDecimal flowRateLitersPerHour, BigDecimal hours) {
        if (flowRateLitersPerHour == null || hours == null || flowRateLitersPerHour.compareTo(BigDecimal.ZERO) <= 0 || hours.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return flowRateLitersPerHour.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }
}