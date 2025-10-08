package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.calendar.IrrigationCalendarEventDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.calendar.SectorMonthlyIrrigationDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class IrrigationService {

    private final IrrigationRepository irrigationRepository;
    private final SectorRepository sectorRepository;
    private final IrrigationEquipmentRepository equipmentRepository;
    private final AuditService auditService;
    private final FarmRepository farmRepository;

    @Transactional
    public Irrigation createIrrigation(IrrigationRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Sector sector = sectorRepository.findById(request.getSectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", request.getSectorId()));

        IrrigationEquipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("IrrigationEquipment", "id", request.getEquipmentId()));

        // --- LÓGICA DE CÁLCULO CENTRALIZADA ---
        BigDecimal irrigationHours = calculateIrrigationHours(request.getStartDateTime(), request.getEndDateTime());
        BigDecimal waterAmount = calculateWaterAmount(equipment.getMeasuredFlow(), irrigationHours);

        Irrigation irrigation = new Irrigation();
        irrigation.setSector(sector);
        irrigation.setEquipment(equipment);
        irrigation.setStartDatetime(request.getStartDateTime());
        irrigation.setEndDatetime(request.getEndDateTime());
        irrigation.setIrrigationHours(irrigationHours); // Valor calculado
        irrigation.setWaterAmount(waterAmount); // Valor calculado

        Irrigation savedIrrigation = irrigationRepository.save(irrigation);

        auditService.logChange(currentUser, "CREATE", Irrigation.class.getSimpleName(), "id", null,
                savedIrrigation.getId().toString());

        log.info("Usuario {} registró un nuevo riego (ID: {}) para el sector {}", currentUser.getUsername(),
                savedIrrigation.getId(), sector.getName());
        return savedIrrigation;
    }

    @Transactional
    public Irrigation updateIrrigation(Integer irrigationId, IrrigationRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Irrigation irrigation = getIrrigationById(irrigationId);

        IrrigationEquipment newEquipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("IrrigationEquipment", "id", request.getEquipmentId()));

        // --- LÓGICA DE CÁLCULO CENTRALIZADA EN LA ACTUALIZACIÓN ---
        BigDecimal newIrrigationHours = calculateIrrigationHours(request.getStartDateTime(), request.getEndDateTime());
        BigDecimal newWaterAmount = calculateWaterAmount(newEquipment.getMeasuredFlow(), newIrrigationHours);

        // --- AUDITORÍA COMPLETA Y DETALLADA ---
        if (!Objects.equals(irrigation.getEquipment().getId(), request.getEquipmentId())) {
            auditService.logChange(currentUser, "UPDATE", Irrigation.class.getSimpleName(), "equipment_id",
                    Objects.toString(irrigation.getEquipment().getId()), Objects.toString(request.getEquipmentId()));
        }
        if (!Objects.equals(irrigation.getStartDatetime(), request.getStartDateTime())) {
            auditService.logChange(currentUser, "UPDATE", Irrigation.class.getSimpleName(), "startDatetime",
                    Objects.toString(irrigation.getStartDatetime()), Objects.toString(request.getStartDateTime()));
        }
        if (!Objects.equals(irrigation.getEndDatetime(), request.getEndDateTime())) {
            auditService.logChange(currentUser, "UPDATE", Irrigation.class.getSimpleName(), "endDatetime",
                    Objects.toString(irrigation.getEndDatetime()), Objects.toString(request.getEndDateTime()));
        }
        if (irrigation.getIrrigationHours().compareTo(newIrrigationHours) != 0) {
            auditService.logChange(currentUser, "UPDATE", Irrigation.class.getSimpleName(), "irrigationHours",
                    Objects.toString(irrigation.getIrrigationHours()), Objects.toString(newIrrigationHours));
        }
        if (!Objects.equals(irrigation.getWaterAmount(), newWaterAmount)) {
            auditService.logChange(currentUser, "UPDATE", Irrigation.class.getSimpleName(), "waterAmount",
                    Objects.toString(irrigation.getWaterAmount()), Objects.toString(newWaterAmount));
        }

        // --- ACTUALIZACIÓN DE TODOS LOS CAMPOS ---
        irrigation.setEquipment(newEquipment);
        irrigation.setStartDatetime(request.getStartDateTime());
        irrigation.setEndDatetime(request.getEndDateTime());
        irrigation.setIrrigationHours(newIrrigationHours); // Valor recalculado
        irrigation.setWaterAmount(newWaterAmount); // Valor recalculado

        log.info("Actualizando registro de riego ID {}", irrigationId);
        return irrigationRepository.save(irrigation);
    }

    @Transactional
    public void deleteIrrigation(Integer irrigationId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Irrigation irrigation = getIrrigationById(irrigationId);
        auditService.logChange(currentUser, "DELETE", Irrigation.class.getSimpleName(), "id",
                irrigation.getId().toString(), null);
        log.warn("Eliminando registro de riego ID {}", irrigationId);
        irrigationRepository.delete(irrigation);
    }

    @Transactional(readOnly = true)
    public List<Irrigation> getIrrigationsBySector(Integer farmId, Integer sectorId) {
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca ID " + farmId));
        return irrigationRepository.findBySectorOrderByStartDatetimeDesc(sector);
    }

    @Transactional(readOnly = true)
    public Irrigation getIrrigationById(Integer irrigationId) {
        return irrigationRepository.findById(irrigationId)
                .orElseThrow(() -> new ResourceNotFoundException("Irrigation", "id", irrigationId));
    }

    private BigDecimal calculateIrrigationHours(Date start, Date end) {
        if (start == null || end == null || end.before(start)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        long diffInMillis = end.getTime() - start.getTime();
        double hours = (double) diffInMillis / (1000 * 60 * 60); // Conversión explícita a horas
        return BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateWaterAmount(BigDecimal flowRateCubicMetersPerHour, BigDecimal hours) {
        if (flowRateCubicMetersPerHour == null || hours == null
                || flowRateCubicMetersPerHour.compareTo(BigDecimal.ZERO) <= 0
                || hours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        // Asumiendo que 'measuredFlow' está en m³/hora, la fórmula es directa.
        return flowRateCubicMetersPerHour.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<SectorMonthlyIrrigationDTO> getMonthlyIrrigationData(Integer farmId, int year, int month) {
        if (!farmRepository.existsById(farmId)) {
            throw new ResourceNotFoundException("Farm", "id", farmId);
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        Date startOfMonth = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endOfMonth = Date.from(endDate.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());

        List<Sector> sectors = sectorRepository.findByFarm_Id(farmId);
        if (sectors.isEmpty()) {
            return new ArrayList<>();
        }

        List<Irrigation> irrigations = irrigationRepository.findBySectorInAndStartDatetimeBetween(sectors, startOfMonth,
                endOfMonth);

        Map<Sector, List<Irrigation>> irrigationsBySector = irrigations.stream()
                .collect(Collectors.groupingBy(Irrigation::getSector));

        return sectors.stream().map(sector -> {
            SectorMonthlyIrrigationDTO sectorDTO = new SectorMonthlyIrrigationDTO();
            sectorDTO.setSectorId(sector.getId());
            sectorDTO.setSectorName(sector.getName());

            List<Irrigation> sectorIrrigations = irrigationsBySector.getOrDefault(sector, new ArrayList<>());

            Map<Integer, List<IrrigationCalendarEventDTO>> dailyIrrigations = sectorIrrigations.stream()
                    .collect(Collectors.groupingBy(
                            irrigation -> irrigation.getStartDatetime().toInstant().atZone(ZoneId.systemDefault())
                                    .getDayOfMonth(),
                            Collectors.mapping(IrrigationCalendarEventDTO::new, Collectors.toList())));

            sectorDTO.setDailyIrrigations(dailyIrrigations);
            return sectorDTO;
        }).collect(Collectors.toList());
    }
}