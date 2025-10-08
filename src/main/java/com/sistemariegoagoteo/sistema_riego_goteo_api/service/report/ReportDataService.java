package com.sistemariegoagoteo.sistema_riego_goteo_api.service.report;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.*;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.*;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.*;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ReportDataService {

    private final FarmRepository farmRepository;
    private final SectorRepository sectorRepository;
    private final IrrigationRepository irrigationRepository;
    private final PrecipitationRepository precipitationRepository;
    private final OperationLogRepository operationLogRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final FertilizationRepository fertilizationRepository;
    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public WaterBalanceReportDTO getWaterBalanceData(Integer farmId, Date startDate, Date endDate, List<Integer> sectorIds) {
        Farm farm = farmRepository.findById(farmId).orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        List<Sector> targetSectors = (sectorIds == null || sectorIds.isEmpty())
                ? sectorRepository.findByFarm_Id(farmId)
                : sectorRepository.findAllById(sectorIds);

        WaterBalanceReportDTO report = new WaterBalanceReportDTO();
        report.setFarmName(farm.getName());

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        WaterBalanceReportDTO.DateRange dateRange = new WaterBalanceReportDTO.DateRange();
        dateRange.setStart(sdf.format(startDate));
        dateRange.setEnd(sdf.format(endDate));
        report.setDateRange(dateRange);

        // Inicializar listas para evitar NullPointerException
        report.setSectors(new ArrayList<>());
        report.setFarmTotals(new WaterBalanceReportDTO.FarmTotals());


        if (targetSectors.isEmpty()) {
            return report; // Devuelve reporte vacío si no hay sectores que analizar
        }

        List<Irrigation> irrigations = irrigationRepository.findBySectorInAndStartDatetimeBetween(targetSectors, startDate, endDate);
        List<Precipitation> precipitations = precipitationRepository.findByFarm_IdAndPrecipitationDateBetween(farmId, startDate, endDate);

        Map<LocalDate, BigDecimal> dailyRainMap = precipitations.stream()
                .collect(Collectors.toMap(
                        // --- ESTA ES LA LÍNEA CORREGIDA ---
                        p -> ((java.sql.Date) p.getPrecipitationDate()).toLocalDate(),
                        Precipitation::getMmEffectiveRain,
                        BigDecimal::add
                ));


        List<WaterBalanceReportDTO.SectorData> sectorDataList = new ArrayList<>();
        for (Sector sector : targetSectors) {
            WaterBalanceReportDTO.SectorData sectorData = new WaterBalanceReportDTO.SectorData();
            sectorData.setSectorId(sector.getId());
            sectorData.setSectorName(sector.getName());

            Map<LocalDate, List<Irrigation>> irrigationsByDate = irrigations.stream()
                    .filter(i -> i.getSector().getId().equals(sector.getId()))
                    .collect(Collectors.groupingBy(i -> i.getStartDatetime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()));

            List<WaterBalanceReportDTO.DailyData> dailyDataList = new ArrayList<>();
            LocalDate start = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate end = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

            for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                WaterBalanceReportDTO.DailyData daily = new WaterBalanceReportDTO.DailyData();
                daily.setDate(date.toString());
                BigDecimal dailyIrrigation = irrigationsByDate.getOrDefault(date, Collections.emptyList()).stream()
                        .map(Irrigation::getWaterAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                daily.setIrrigationWater(dailyIrrigation);
                daily.setEffectiveRain(dailyRainMap.getOrDefault(date, BigDecimal.ZERO));
                dailyDataList.add(daily);
            }
            sectorData.setDailyData(dailyDataList);

            WaterBalanceReportDTO.Summary summary = new WaterBalanceReportDTO.Summary();
            summary.setTotalIrrigationWater(dailyDataList.stream().map(WaterBalanceReportDTO.DailyData::getIrrigationWater).reduce(BigDecimal.ZERO, BigDecimal::add));
            summary.setTotalEffectiveRain(dailyDataList.stream().map(WaterBalanceReportDTO.DailyData::getEffectiveRain).reduce(BigDecimal.ZERO, BigDecimal::add));
            summary.setTotalIrrigationHours(irrigations.stream().filter(i -> i.getSector().getId().equals(sector.getId())).map(Irrigation::getIrrigationHours).reduce(BigDecimal.ZERO, BigDecimal::add));
            sectorData.setSummary(summary);
            sectorDataList.add(sectorData);
        }
        report.setSectors(sectorDataList);

        WaterBalanceReportDTO.FarmTotals farmTotals = new WaterBalanceReportDTO.FarmTotals();
        farmTotals.setTotalIrrigationWater(sectorDataList.stream().map(s -> s.getSummary().getTotalIrrigationWater()).reduce(BigDecimal.ZERO, BigDecimal::add));
        farmTotals.setTotalEffectiveRain(dailyRainMap.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        farmTotals.setTotalIrrigationHours(sectorDataList.stream().map(s -> s.getSummary().getTotalIrrigationHours()).reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setFarmTotals(farmTotals);

        return report;
    }

    // ... (El resto de los métodos permanecen igual) ...
    public OperationsLogReportDTO getOperationsLogData(Integer farmId, Date startDate, Date endDate, String operationType, Long userId) {
        Farm farm = farmRepository.findById(farmId).orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        List<OperationsLogReportDTO.Operation> operations = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

        // 1. Irrigations
        irrigationRepository.findBySector_Farm_IdAndStartDatetimeBetween(farmId, startDate, endDate).forEach(i -> {
            OperationsLogReportDTO.Operation op = new OperationsLogReportDTO.Operation();
            op.setDatetime(sdf.format(i.getStartDatetime()));
            op.setType("RIEGO");
            op.setDescription(String.format("Riego: %.2f m³ en %.2f horas.", i.getWaterAmount(), i.getIrrigationHours()));
            op.setLocation("Sector: " + i.getSector().getName());
            op.setUserName("N/A"); // User info not available on Irrigation model
            operations.add(op);
        });

        // 2. Maintenances
        maintenanceRepository.findAll().stream()
                .filter(m -> m.getIrrigationEquipment().getFarm().getId().equals(farmId) && !m.getDate().before(startDate) && !m.getDate().after(endDate))
                .forEach(m -> {
                    OperationsLogReportDTO.Operation op = new OperationsLogReportDTO.Operation();
                    op.setDatetime(sdf.format(m.getDate()));
                    op.setType("MANTENIMIENTO");
                    op.setDescription(m.getDescription());
                    op.setLocation("Equipo: " + m.getIrrigationEquipment().getName());
                    op.setUserName("N/A"); // User info not available
                    operations.add(op);
                });

        // 3. Fertilizations
        fertilizationRepository.findAll().stream()
                .filter(f -> f.getSector().getFarm().getId().equals(farmId) && !f.getDate().before(startDate) && !f.getDate().after(endDate))
                .forEach(f -> {
                    OperationsLogReportDTO.Operation op = new OperationsLogReportDTO.Operation();
                    op.setDatetime(sdf.format(f.getDate()));
                    op.setType("FERTILIZACION");
                    op.setDescription(String.format("%s: %.2f %s", f.getFertilizerType(), f.getQuantity(), f.getQuantityUnit()));
                    op.setLocation("Sector: " + f.getSector().getName());
                    op.setUserName("N/A"); // User info not available
                    operations.add(op);
                });

        Stream<OperationsLogReportDTO.Operation> stream = operations.stream();
        if (operationType != null && !operationType.isEmpty()) {
            stream = stream.filter(op -> op.getType().equalsIgnoreCase(operationType));
        }

        OperationsLogReportDTO report = new OperationsLogReportDTO();
        report.setFarmName(farm.getName());
        OperationsLogReportDTO.DateRange dateRange = new OperationsLogReportDTO.DateRange();
        dateRange.setStart(sdf.format(startDate));
        dateRange.setEnd(sdf.format(endDate));
        report.setDateRange(dateRange);
        report.setOperations(stream.sorted(Comparator.comparing(OperationsLogReportDTO.Operation::getDatetime).reversed()).collect(Collectors.toList()));

        return report;
    }

    public PeriodSummaryReportDTO getPeriodSummaryData(Integer farmId, Date startDate, Date endDate) {
        Farm farm = farmRepository.findById(farmId).orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        PeriodSummaryReportDTO report = new PeriodSummaryReportDTO();
        report.setFarmName(farm.getName());

        PeriodSummaryReportDTO.Period period = new PeriodSummaryReportDTO.Period();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        period.setStart(sdf.format(startDate));
        period.setEnd(sdf.format(endDate));
        report.setPeriod(period);

        // Water Summary
        PeriodSummaryReportDTO.WaterSummary waterSummary = new PeriodSummaryReportDTO.WaterSummary();
        List<Irrigation> irrigations = irrigationRepository.findBySector_Farm_IdAndStartDatetimeBetween(farmId, startDate, endDate);
        waterSummary.setTotalIrrigationWaterM3(irrigations.stream().map(Irrigation::getWaterAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        waterSummary.setTotalIrrigationHours(irrigations.stream().map(Irrigation::getIrrigationHours).reduce(BigDecimal.ZERO, BigDecimal::add));

        List<Precipitation> precipitations = precipitationRepository.findByFarm_IdAndPrecipitationDateBetween(farmId, startDate, endDate);
        waterSummary.setTotalPrecipitationMM(precipitations.stream().map(Precipitation::getMmRain).reduce(BigDecimal.ZERO, BigDecimal::add));
        waterSummary.setTotalEffectivePrecipitationMM(precipitations.stream().map(Precipitation::getMmEffectiveRain).reduce(BigDecimal.ZERO, BigDecimal::add));
        report.setWaterSummary(waterSummary);

        // Operations Summary
        PeriodSummaryReportDTO.OperationsSummary opsSummary = new PeriodSummaryReportDTO.OperationsSummary();
        opsSummary.setTasksCreated(taskRepository.countBySector_Farm_IdAndCreatedAtBetween(farmId, startDate, endDate));
        opsSummary.setTasksCompleted(taskRepository.countBySector_Farm_IdAndCreatedAtBetweenAndStatus(farmId, startDate, endDate, TaskStatus.COMPLETADA));
        opsSummary.setMaintenanceRecords(maintenanceRepository.countByIrrigationEquipment_Farm_IdAndDateBetween(farmId, startDate, endDate));
        opsSummary.setFertilizationRecords(fertilizationRepository.countBySector_Farm_IdAndDateBetween(farmId, startDate, endDate));
        report.setOperationsSummary(opsSummary);

        // Water Usage By Sector
        report.setWaterUsageBySector(
                irrigations.stream()
                        .collect(Collectors.groupingBy(
                                i -> i.getSector().getName(),
                                Collectors.reducing(BigDecimal.ZERO, Irrigation::getWaterAmount, BigDecimal::add)
                        ))
                        .entrySet().stream()
                        .map(entry -> {
                            PeriodSummaryReportDTO.WaterUsageBySector usage = new PeriodSummaryReportDTO.WaterUsageBySector();
                            usage.setSectorName(entry.getKey());
                            usage.setTotalWaterM3(entry.getValue());
                            return usage;
                        })
                        .collect(Collectors.toList())
        );

        return report;
    }
}