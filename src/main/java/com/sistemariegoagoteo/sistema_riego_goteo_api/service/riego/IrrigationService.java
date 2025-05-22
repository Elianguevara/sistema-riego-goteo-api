package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
//import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
//import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class IrrigationService {

    private final IrrigationRepository irrigationRepository;
    private final SectorRepository sectorRepository;
    private final IrrigationEquipmentRepository equipmentRepository;
    //private final FarmRepository farmRepository; // Para validar la finca

    @Transactional
    public Irrigation logIrrigation(Integer farmId, Integer sectorId, IrrigationRequest request) {
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca ID " + farmId));

        IrrigationEquipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id", request.getEquipmentId()));

        // Validar que el equipo pertenece a la misma finca que el sector
        if (!equipment.getFarm().getId().equals(sector.getFarm().getId())) {
            throw new IllegalArgumentException("El equipo de irrigación ID " + equipment.getId() +
                    " no pertenece a la finca ID " + sector.getFarm().getId() + " del sector.");
        }

        // Validar que el equipo especificado sea el mismo que está asignado al sector (opcional, según reglas de negocio)
        // if (sector.getEquipment() != null && !sector.getEquipment().getId().equals(equipment.getId())) {
        //    throw new IllegalArgumentException("El equipo ID " + equipment.getId() +
        //            " no es el equipo asignado por defecto al sector ID " + sector.getId() + " (" + sector.getEquipment().getName() + ")");
        // }


        Irrigation irrigation = new Irrigation();
        irrigation.setSector(sector);
        irrigation.setEquipment(equipment);
        irrigation.setStartDatetime(request.getStartDatetime());
        irrigation.setEndDatetime(request.getEndDatetime());

        // Validar que endDatetime sea posterior a startDatetime si ambos están presentes
        if (request.getStartDatetime() != null && request.getEndDatetime() != null) {
            if (request.getEndDatetime().before(request.getStartDatetime())) {
                throw new IllegalArgumentException("La fecha de finalización no puede ser anterior a la fecha de inicio.");
            }
            // Calcular horas de riego si no se proporcionaron
            if (request.getIrrigationHours() == null) {
                irrigation.setIrrigationHours(calculateIrrigationHours(request.getStartDatetime(), request.getEndDatetime()));
            } else {
                irrigation.setIrrigationHours(request.getIrrigationHours());
            }

            // Calcular cantidad de agua si no se proporcionó y hay caudal y horas
            if (request.getWaterAmount() == null && equipment.getMeasuredFlow() != null && irrigation.getIrrigationHours() != null) {
                irrigation.setWaterAmount(calculateWaterAmount(equipment.getMeasuredFlow(), irrigation.getIrrigationHours()));
            } else {
                irrigation.setWaterAmount(request.getWaterAmount());
            }
        } else { // Si solo se está iniciando o se dan datos parciales
             irrigation.setIrrigationHours(request.getIrrigationHours());
             irrigation.setWaterAmount(request.getWaterAmount());
        }


        log.info("Registrando riego para sector ID {} con equipo ID {} desde {}", sectorId, equipment.getId(), request.getStartDatetime());
        return irrigationRepository.save(irrigation);
    }

    @Transactional(readOnly = true)
    public List<Irrigation> getIrrigationsBySector(Integer farmId, Integer sectorId) {
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca ID " + farmId));
        return irrigationRepository.findBySectorOrderByStartDatetimeDesc(sector); // Asumir este método en el repo
    }

    @Transactional(readOnly = true)
    public Irrigation getIrrigationById(Integer irrigationId) {
        return irrigationRepository.findById(irrigationId)
                .orElseThrow(() -> new ResourceNotFoundException("Irrigation", "id", irrigationId));
    }

    @Transactional
    public Irrigation updateIrrigation(Integer irrigationId, IrrigationRequest request) {
        Irrigation irrigation = getIrrigationById(irrigationId); // Reutiliza el get para validar existencia

        // Validar y obtener el equipo si se cambia
        if (!irrigation.getEquipment().getId().equals(request.getEquipmentId())) {
            IrrigationEquipment newEquipment = equipmentRepository.findById(request.getEquipmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id", request.getEquipmentId()));
            // Validar que el nuevo equipo pertenezca a la misma finca que el sector del riego
            if (!newEquipment.getFarm().getId().equals(irrigation.getSector().getFarm().getId())) {
                 throw new IllegalArgumentException("El nuevo equipo de irrigación ID " + newEquipment.getId() +
                    " no pertenece a la finca ID " + irrigation.getSector().getFarm().getId() + " del sector.");
            }
            irrigation.setEquipment(newEquipment);
        }

        irrigation.setStartDatetime(request.getStartDatetime());
        irrigation.setEndDatetime(request.getEndDatetime());

        if (request.getStartDatetime() != null && request.getEndDatetime() != null) {
             if (request.getEndDatetime().before(request.getStartDatetime())) {
                throw new IllegalArgumentException("La fecha de finalización no puede ser anterior a la fecha de inicio.");
            }
            irrigation.setIrrigationHours(request.getIrrigationHours() != null ? request.getIrrigationHours() : calculateIrrigationHours(request.getStartDatetime(), request.getEndDatetime()));
            irrigation.setWaterAmount(request.getWaterAmount() != null ? request.getWaterAmount() : calculateWaterAmount(irrigation.getEquipment().getMeasuredFlow(), irrigation.getIrrigationHours()));
        } else {
            irrigation.setIrrigationHours(request.getIrrigationHours());
            irrigation.setWaterAmount(request.getWaterAmount());
        }

        log.info("Actualizando registro de riego ID {}", irrigationId);
        return irrigationRepository.save(irrigation);
    }


    @Transactional
    public void deleteIrrigation(Integer irrigationId) {
        Irrigation irrigation = getIrrigationById(irrigationId);
        log.warn("Eliminando registro de riego ID {}", irrigationId);
        irrigationRepository.delete(irrigation);
    }

    // --- Métodos de Cálculo ---
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
            return null; // O BigDecimal.ZERO si se prefiere no nulo
        }
        // Asumiendo que measuredFlow está en L/h (o m3/h, ajustar unidades)
        // Aquí asumimos L/h y el resultado será en Litros.
        return flowRateLitersPerHour.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }
}