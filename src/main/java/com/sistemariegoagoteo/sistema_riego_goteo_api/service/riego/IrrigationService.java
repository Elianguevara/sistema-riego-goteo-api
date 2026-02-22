package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.calendar.IrrigationCalendarEventDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.calendar.SectorMonthlyIrrigationDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Precipitation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.PrecipitationRepository;
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
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio encargado de la lógica de negocio relacionada con los registros de
 * riego.
 * Maneja la creación, cálculo de volúmenes, auditoría y recuperación de datos
 * históricos.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IrrigationService {

    /**
     * Repositorio para la gestión de registros de riego.
     */
    private final IrrigationRepository irrigationRepository;

    /**
     * Repositorio para la gestión de sectores.
     */
    private final SectorRepository sectorRepository;

    /**
     * Repositorio para la gestión de equipos de riego.
     */
    private final IrrigationEquipmentRepository equipmentRepository;

    /**
     * Servicio de auditoría para registrar cambios en los riegos.
     */
    private final AuditService auditService;

    /**
     * Repositorio para la gestión de fincas.
     */
    private final FarmRepository farmRepository;

    /**
     * Repositorio para la gestión de precipitaciones.
     */
    private final PrecipitationRepository precipitationRepository;

    /**
     * Factor de conversión de Metros Cúbicos a Hectolitros (1 m³ = 10 hL).
     */
    private static final BigDecimal METERS_CUBIC_TO_HECTOLITERS = new BigDecimal("10");

    /**
     * Crea un nuevo registro de riego calculando automáticamente la duración y el
     * consumo de agua.
     *
     * @param request DTO con los datos del riego (fechas, sector, equipo).
     * @return La entidad Irrigation persistida.
     * @throws ResourceNotFoundException Si el sector o el equipo no existen.
     */
    @Transactional
    public Irrigation createIrrigation(IrrigationRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Sector sector = sectorRepository.findById(request.getSectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", request.getSectorId()));

        IrrigationEquipment equipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("IrrigationEquipment", "id", request.getEquipmentId()));

        // 1. Calcular duración precisa
        BigDecimal irrigationHours = calculateIrrigationHours(request.getStartDateTime(), request.getEndDateTime());

        // 2. Calcular volumen de agua (en Hectolitros) basado en el flujo del equipo
        BigDecimal waterAmount = calculateWaterAmount(equipment.getMeasuredFlow(), irrigationHours);

        Irrigation irrigation = new Irrigation();
        irrigation.setSector(sector);
        irrigation.setEquipment(equipment);
        irrigation.setStartDatetime(request.getStartDateTime());
        irrigation.setEndDatetime(request.getEndDateTime());
        irrigation.setIrrigationHours(irrigationHours);
        irrigation.setWaterAmount(waterAmount);

        Irrigation savedIrrigation = irrigationRepository.save(irrigation);

        auditService.logChange(currentUser, "CREATE", Irrigation.class.getSimpleName(), "id", null,
                savedIrrigation.getId().toString());

        log.info("Usuario {} registró un nuevo riego (ID: {}) para el sector {}", currentUser.getUsername(),
                savedIrrigation.getId(), sector.getName());
        return savedIrrigation;
    }

    /**
     * Actualiza un registro de riego existente, recalculando los valores derivados
     * si cambian las fechas o el equipo.
     *
     * @param irrigationId ID del riego a actualizar.
     * @param request      DTO con los nuevos datos.
     * @return La entidad Irrigation actualizada.
     */
    @Transactional
    public Irrigation updateIrrigation(Integer irrigationId, IrrigationRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Irrigation irrigation = getIrrigationById(irrigationId);

        IrrigationEquipment newEquipment = equipmentRepository.findById(request.getEquipmentId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("IrrigationEquipment", "id", request.getEquipmentId()));

        // Recálculo de valores
        BigDecimal newIrrigationHours = calculateIrrigationHours(request.getStartDateTime(), request.getEndDateTime());
        BigDecimal newWaterAmount = calculateWaterAmount(newEquipment.getMeasuredFlow(), newIrrigationHours);

        // Registro de auditoría para campos críticos
        logAndAuditChanges(currentUser, irrigation, request, newEquipment, newIrrigationHours, newWaterAmount);

        // Aplicar cambios
        irrigation.setEquipment(newEquipment);
        irrigation.setStartDatetime(request.getStartDateTime());
        irrigation.setEndDatetime(request.getEndDateTime());
        irrigation.setIrrigationHours(newIrrigationHours);
        irrigation.setWaterAmount(newWaterAmount);

        log.info("Actualizando registro de riego ID {}", irrigationId);
        return irrigationRepository.save(irrigation);
    }

    /**
     * Elimina un registro de riego.
     *
     * @param irrigationId ID del riego a eliminar.
     */
    @Transactional
    public void deleteIrrigation(Integer irrigationId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Irrigation irrigation = getIrrigationById(irrigationId);

        auditService.logChange(currentUser, "DELETE", Irrigation.class.getSimpleName(), "id",
                irrigation.getId().toString(), null);

        log.warn("Eliminando registro de riego ID {}", irrigationId);
        irrigationRepository.delete(irrigation);
    }

    /**
     * Obtiene el historial de riegos de un sector específico.
     */
    @Transactional(readOnly = true)
    public List<Irrigation> getIrrigationsBySector(Integer farmId, Integer sectorId) {
        Sector sector = sectorRepository.findByIdAndFarm_Id(sectorId, farmId)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Sector", "id", sectorId + " para la finca ID " + farmId));
        return irrigationRepository.findBySectorOrderByStartDatetimeDesc(sector);
    }

    /**
     * Busca un riego por su ID.
     */
    @Transactional(readOnly = true)
    public Irrigation getIrrigationById(Integer irrigationId) {
        return irrigationRepository.findById(irrigationId)
                .orElseThrow(() -> new ResourceNotFoundException("Irrigation", "id", irrigationId));
    }

    /**
     * Calcula la diferencia de horas entre dos fechas con precisión BigDecimal.
     * Evita el uso de 'double' para prevenir errores de punto flotante.
     *
     * @param start Fecha de inicio.
     * @param end   Fecha de fin.
     * @return Horas con 2 decimales de precisión.
     */
    private BigDecimal calculateIrrigationHours(java.time.LocalDateTime start, java.time.LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        long diffInMinutes = java.time.Duration.between(start, end).toMinutes();
        BigDecimal minutes = BigDecimal.valueOf(diffInMinutes);

        // División precisa: (minutos / 60.0)
        return minutes.divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula la cantidad de agua consumida.
     * Asume que el caudal (flowRate) está en m³/h y convierte el resultado a
     * Hectolitros.
     *
     * @param flowRateCubicMetersPerHour Caudal del equipo.
     * @param hours                      Horas de funcionamiento.
     * @return Volumen en Hectolitros (hL).
     */
    private BigDecimal calculateWaterAmount(BigDecimal flowRateCubicMetersPerHour, BigDecimal hours) {
        if (flowRateCubicMetersPerHour == null || hours == null
                || flowRateCubicMetersPerHour.compareTo(BigDecimal.ZERO) <= 0
                || hours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        // 1. Calcular volumen en Metros Cúbicos
        BigDecimal volumeInCubicMeters = flowRateCubicMetersPerHour.multiply(hours);

        // 2. Convertir m³ a Hectolitros y redondear
        return volumeInCubicMeters.multiply(METERS_CUBIC_TO_HECTOLITERS).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Obtiene los datos de riego mensual agrupados por sector y día para la vista
     * de calendario.
     */
    @Transactional(readOnly = true)
    public List<SectorMonthlyIrrigationDTO> getMonthlyIrrigationData(Integer farmId, int year, int month) {
        if (!farmRepository.existsById(farmId)) {
            throw new ResourceNotFoundException("Farm", "id", farmId);
        }

        // Determinar rango de fechas del mes
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startOfMonth = yearMonth.atDay(1);
        LocalDate endOfMonth = yearMonth.atEndOfMonth();

        // Obtener precipitaciones del mes para la finca
        List<Precipitation> precipitations = precipitationRepository.findByFarm_IdAndPrecipitationDateBetween(
                farmId, startOfMonth, endOfMonth);

        // Agrupar precipitaciones por día y sumar mmRain
        Map<Integer, BigDecimal> dailyRain = precipitations.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPrecipitationDate().getDayOfMonth(),
                        Collectors.reducing(BigDecimal.ZERO, Precipitation::getMmRain, BigDecimal::add)));

        List<Sector> sectors = sectorRepository.findByFarm_Id(farmId);
        if (sectors.isEmpty()) {
            return new ArrayList<>();
        }

        // Obtener riegos en una sola consulta
        LocalDateTime startOfMonthDateTime = startOfMonth.atStartOfDay();
        LocalDateTime endOfMonthDateTime = endOfMonth.atTime(23, 59, 59);

        List<Irrigation> irrigations = irrigationRepository.findBySectorInAndStartDatetimeBetween(sectors,
                startOfMonthDateTime,
                endOfMonthDateTime);

        // Agrupar riegos por sector
        Map<Sector, List<Irrigation>> irrigationsBySector = irrigations.stream()
                .collect(Collectors.groupingBy(Irrigation::getSector));

        // Construir DTOs
        return sectors.stream().map(sector -> {
            SectorMonthlyIrrigationDTO sectorDTO = new SectorMonthlyIrrigationDTO();
            sectorDTO.setSectorId(sector.getId());
            sectorDTO.setSectorName(sector.getName());

            List<Irrigation> sectorIrrigations = irrigationsBySector.getOrDefault(sector, new ArrayList<>());

            // Agrupar por día del mes
            Map<Integer, List<IrrigationCalendarEventDTO>> dailyIrrigations = sectorIrrigations.stream()
                    .collect(Collectors.groupingBy(
                            irrigation -> irrigation.getStartDatetime().getDayOfMonth(),
                            Collectors.mapping(IrrigationCalendarEventDTO::new, Collectors.toList())));

            sectorDTO.setDailyIrrigations(dailyIrrigations);
            sectorDTO.setDailyPrecipitations(dailyRain);
            return sectorDTO;
        }).collect(Collectors.toList());
    }

    // --- Métodos Auxiliares Privados ---

    private void logAndAuditChanges(User user, Irrigation irrigation, IrrigationRequest request,
            IrrigationEquipment newEquipment, BigDecimal newHours, BigDecimal newWater) {
        if (!Objects.equals(irrigation.getEquipment().getId(), request.getEquipmentId())) {
            auditService.logChange(user, "UPDATE", Irrigation.class.getSimpleName(), "equipment_id",
                    String.valueOf(irrigation.getEquipment().getId()), String.valueOf(request.getEquipmentId()));
        }
        if (!Objects.equals(irrigation.getStartDatetime(), request.getStartDateTime())) {
            auditService.logChange(user, "UPDATE", Irrigation.class.getSimpleName(), "startDatetime",
                    String.valueOf(irrigation.getStartDatetime()), String.valueOf(request.getStartDateTime()));
        }
        if (!Objects.equals(irrigation.getEndDatetime(), request.getEndDateTime())) {
            auditService.logChange(user, "UPDATE", Irrigation.class.getSimpleName(), "endDatetime",
                    String.valueOf(irrigation.getEndDatetime()), String.valueOf(request.getEndDateTime()));
        }
        if (irrigation.getIrrigationHours().compareTo(newHours) != 0) {
            auditService.logChange(user, "UPDATE", Irrigation.class.getSimpleName(), "irrigationHours",
                    String.valueOf(irrigation.getIrrigationHours()), String.valueOf(newHours));
        }
        if (irrigation.getWaterAmount().compareTo(newWater) != 0) {
            auditService.logChange(user, "UPDATE", Irrigation.class.getSimpleName(), "waterAmount",
                    String.valueOf(irrigation.getWaterAmount()), String.valueOf(newWater));
        }
    }

}
