package com.sistemariegoagoteo.sistema_riego_goteo_api.service.report;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.*;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.projection.*;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.*;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.*;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

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

        public WaterBalanceReportDTO getWaterBalanceData(Integer farmId, Date startDate, Date endDate,
                        List<Integer> sectorIds) {
                Farm farm = farmRepository.findById(farmId)
                                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

                List<Integer> targetSectorIds = (sectorIds == null || sectorIds.isEmpty())
                                ? sectorRepository.findByFarm_Id(farmId).stream().map(Sector::getId)
                                                .collect(Collectors.toList())
                                : sectorIds;

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

                if (targetSectorIds.isEmpty()) {
                        return report;
                }

                // AGREGACIÓN EN DB: Precipitaciones
                List<DailyRainProjection> dailyRains = precipitationRepository.findDailyRainByFarm(farmId, startDate,
                                endDate);
                Map<LocalDate, BigDecimal> dailyRainMap = dailyRains.stream()
                                .collect(Collectors.toMap(DailyRainProjection::getRainDate,
                                                DailyRainProjection::getAmount));

                // AGREGACIÓN EN DB: Resumen por Sector
                List<SectorIrrigationProjection> sectorSummaries = irrigationRepository
                                .getSectorIrrigationTotals(farmId, targetSectorIds, startDate, endDate);
                Map<Integer, SectorIrrigationProjection> sectorSummaryMap = sectorSummaries.stream()
                                .collect(Collectors.toMap(SectorIrrigationProjection::getSectorId, s -> s));

                List<WaterBalanceReportDTO.SectorData> sectorDataList = new ArrayList<>();
                LocalDate startLocalDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate endLocalDate = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                for (Integer sectorId : targetSectorIds) {
                        Sector sector = sectorRepository.findById(sectorId).orElse(null);
                        if (sector == null)
                                continue;

                        WaterBalanceReportDTO.SectorData sectorData = new WaterBalanceReportDTO.SectorData();
                        sectorData.setSectorId(sector.getId());
                        sectorData.setSectorName(sector.getName());

                        // AGREGACIÓN EN DB: Riegos Diarios por Sector
                        List<DailyIrrigationProjection> dailyIrrigations = irrigationRepository
                                        .getDailyIrrigationTotals(sectorId, startDate, endDate);
                        Map<LocalDate, DailyIrrigationProjection> dailyIrrigationMap = dailyIrrigations.stream()
                                        .collect(Collectors.toMap(DailyIrrigationProjection::getIrrigationDate,
                                                        i -> i));

                        List<WaterBalanceReportDTO.DailyData> dailyDataList = new ArrayList<>();
                        for (LocalDate date = startLocalDate; !date.isAfter(endLocalDate); date = date.plusDays(1)) {
                                WaterBalanceReportDTO.DailyData daily = new WaterBalanceReportDTO.DailyData();
                                daily.setDate(date.toString());

                                DailyIrrigationProjection dailyIrr = dailyIrrigationMap.get(date);
                                daily.setIrrigationWater(
                                                dailyIrr != null ? dailyIrr.getWaterAmount() : BigDecimal.ZERO);
                                daily.setEffectiveRain(dailyRainMap.getOrDefault(date, BigDecimal.ZERO));
                                dailyDataList.add(daily);
                        }
                        sectorData.setDailyData(dailyDataList);

                        WaterBalanceReportDTO.Summary summary = new WaterBalanceReportDTO.Summary();
                        SectorIrrigationProjection sectorSum = sectorSummaryMap.get(sectorId);
                        summary.setTotalIrrigationWater(
                                        sectorSum != null ? sectorSum.getWaterAmount() : BigDecimal.ZERO);
                        summary.setTotalIrrigationHours(sectorSum != null ? sectorSum.getHours() : BigDecimal.ZERO);
                        summary.setTotalEffectiveRain(dailyRains.stream().map(DailyRainProjection::getAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add));

                        sectorData.setSummary(summary);
                        sectorDataList.add(sectorData);
                }
                report.setSectors(sectorDataList);

                WaterBalanceReportDTO.FarmTotals farmTotals = new WaterBalanceReportDTO.FarmTotals();
                farmTotals.setTotalIrrigationWater(
                                sectorSummaries.stream().map(SectorIrrigationProjection::getWaterAmount)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                farmTotals.setTotalEffectiveRain(dailyRains.stream().map(DailyRainProjection::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                farmTotals.setTotalIrrigationHours(sectorSummaries.stream().map(SectorIrrigationProjection::getHours)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                report.setFarmTotals(farmTotals);

                return report;
        }

        public OperationsLogReportDTO getOperationsLogData(Integer farmId, Date startDate, Date endDate,
                        String operationType, Long userId) {
                Farm farm = farmRepository.findById(farmId)
                                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

                List<OperationLogProjection> allLogs = new ArrayList<>();

                // AGREGACIÓN/PROYECCIÓN EN DB
                if (operationType == null || "RIEGO".equalsIgnoreCase(operationType)) {
                        allLogs.addAll(irrigationRepository.getIrrigationLogs(farmId, startDate, endDate));
                }
                if (operationType == null || "MANTENIMIENTO".equalsIgnoreCase(operationType)) {
                        allLogs.addAll(maintenanceRepository.getMaintenanceLogs(farmId, startDate, endDate));
                }
                if (operationType == null || "FERTILIZACION".equalsIgnoreCase(operationType)) {
                        allLogs.addAll(fertilizationRepository.getFertilizationLogs(farmId, startDate, endDate));
                }

                OperationsLogReportDTO report = new OperationsLogReportDTO();
                report.setFarmName(farm.getName());
                OperationsLogReportDTO.DateRange dateRange = new OperationsLogReportDTO.DateRange();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                dateRange.setStart(sdf.format(startDate));
                dateRange.setEnd(sdf.format(endDate));
                report.setDateRange(dateRange);

                report.setOperations(allLogs.stream()
                                .sorted(Comparator.comparing(OperationLogProjection::getDatetime).reversed())
                                .map(proj -> {
                                        OperationsLogReportDTO.Operation op = new OperationsLogReportDTO.Operation();
                                        op.setDatetime(proj.getDatetime().toString());
                                        op.setType(proj.getType());
                                        op.setDescription(proj.getDescription());
                                        op.setLocation(proj.getLocation());
                                        op.setUserName(proj.getUserName());
                                        return op;
                                }).collect(Collectors.toList()));

                return report;
        }

        public PeriodSummaryReportDTO getPeriodSummaryData(Integer farmId, Date startDate, Date endDate) {
                Farm farm = farmRepository.findById(farmId)
                                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

                PeriodSummaryReportDTO report = new PeriodSummaryReportDTO();
                report.setFarmName(farm.getName());

                PeriodSummaryReportDTO.Period period = new PeriodSummaryReportDTO.Period();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                period.setStart(sdf.format(startDate));
                period.setEnd(sdf.format(endDate));
                report.setPeriod(period);

                // Water Summary (Using existings totals + optimized queries)
                PeriodSummaryReportDTO.WaterSummary waterSummary = new PeriodSummaryReportDTO.WaterSummary();

                // Obtenemos los sectores para el farm
                List<Integer> sectorIds = sectorRepository.findByFarm_Id(farmId).stream().map(Sector::getId)
                                .collect(Collectors.toList());
                List<SectorIrrigationProjection> sectorSummaries = irrigationRepository
                                .getSectorIrrigationTotals(farmId, sectorIds, startDate, endDate);

                waterSummary.setTotalIrrigationWaterM3(
                                sectorSummaries.stream().map(SectorIrrigationProjection::getWaterAmount)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                waterSummary.setTotalIrrigationHours(sectorSummaries.stream().map(SectorIrrigationProjection::getHours)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));

                List<DailyRainProjection> rains = precipitationRepository.findDailyRainByFarm(farmId, startDate,
                                endDate);
                waterSummary.setTotalPrecipitationMM(rains.stream().map(DailyRainProjection::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                waterSummary.setTotalEffectivePrecipitationMM(rains.stream().map(DailyRainProjection::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add));
                report.setWaterSummary(waterSummary);

                // Operations Summary
                PeriodSummaryReportDTO.OperationsSummary opsSummary = new PeriodSummaryReportDTO.OperationsSummary();
                opsSummary.setTasksCreated(
                                taskRepository.countBySector_Farm_IdAndCreatedAtBetween(farmId, startDate, endDate));
                opsSummary.setTasksCompleted(
                                taskRepository.countBySector_Farm_IdAndCreatedAtBetweenAndStatus(farmId, startDate,
                                                endDate, TaskStatus.COMPLETADA));
                opsSummary.setMaintenanceRecords(
                                maintenanceRepository.countByIrrigationEquipment_Farm_IdAndDateBetween(farmId,
                                                startDate, endDate));
                opsSummary.setFertilizationRecords(
                                fertilizationRepository.countBySector_Farm_IdAndDateBetween(farmId, startDate,
                                                endDate));
                report.setOperationsSummary(opsSummary);

                // Water Usage By Sector
                report.setWaterUsageBySector(sectorSummaries.stream()
                                .map(s -> {
                                        PeriodSummaryReportDTO.WaterUsageBySector usage = new PeriodSummaryReportDTO.WaterUsageBySector();
                                        usage.setSectorName(s.getSectorName());
                                        usage.setTotalWaterM3(s.getWaterAmount());
                                        return usage;
                                }).collect(Collectors.toList()));

                return report;
        }
}