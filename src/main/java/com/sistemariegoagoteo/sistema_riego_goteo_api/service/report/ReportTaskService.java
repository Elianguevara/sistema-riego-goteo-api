package com.sistemariegoagoteo.sistema_riego_goteo_api.service.report;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.OperationsLogReportDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.PeriodSummaryReportDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.WaterBalanceReportDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.report.ReportTask;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.report.ReportTaskRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.config.SystemConfigService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.config.ReportConfigDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportTaskService {

    private final ReportTaskRepository reportTaskRepository;
    private final ReportDataService reportDataService;
    private final ReportFileService reportFileService;
    private final ExcelReportService excelReportService;
    private final PdfReportService pdfReportService;
    private final com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository userRepository;
    private final SystemConfigService systemConfigService;

    public ReportTask createAndStartTask(String reportType, Integer farmId, Date startDate, Date endDate,
            String format, List<Integer> sectorIds, String operationType, Long userId) {

        if (format == null || format.isBlank()) {
            ReportConfigDTO reportConfig = systemConfigService.getReportConfig();
            format = reportConfig.getDefaultReportFormat();
        }

        ReportTask task = ReportTask.builder()
                .reportType(reportType)
                .format(format)
                .status(ReportTask.ReportStatus.PENDING)
                .build();

        task = reportTaskRepository.save(task);

        generateReportAsync(task.getId(), reportType, farmId, startDate, endDate,
                format, sectorIds, operationType, userId);

        return task;
    }

    @Async
    public void generateReportAsync(UUID taskId, String reportType, Integer farmId,
            Date startDate, Date endDate, String format,
            List<Integer> sectorIds, String operationType, Long userId) {

        ReportTask task = reportTaskRepository.findById(taskId).orElseThrow();
        task.setStatus(ReportTask.ReportStatus.PROCESSING);
        reportTaskRepository.save(task);

        String requesterName = "Sistema";
        if (userId != null) {
            requesterName = userRepository.findById(userId)
                    .map(com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User::getName)
                    .orElse("Sistema");
        }

        try {
            Path tempFile = Files.createTempFile("report_" + taskId + "_", "." + format.toLowerCase());
            String filePath = tempFile.toAbsolutePath().toString();

            switch (reportType.toUpperCase()) {

                case "WATER_BALANCE" -> {
                    var data = reportDataService.getWaterBalanceData(farmId, startDate, endDate, sectorIds);
                    if ("CSV".equalsIgnoreCase(format)) {
                        try (Writer writer = new FileWriter(filePath)) {
                            reportFileService.generateWaterBalanceCsv(data, writer);
                        }
                    } else if ("XLSX".equalsIgnoreCase(format)) {
                        byte[] bytes = excelReportService.generateReport(
                                buildWaterBalanceTableData(data), "Balance Hídrico");
                        Files.write(tempFile, bytes);
                    } else {
                        byte[] bytes = pdfReportService.generateCorporateReport(
                                buildWaterBalanceTableData(data), "Balance Hídrico", requesterName);
                        Files.write(tempFile, bytes);
                    }
                }

                case "OPERATIONS_LOG" -> {
                    var data = reportDataService.getOperationsLogData(
                            farmId, startDate, endDate, operationType, userId);
                    if ("CSV".equalsIgnoreCase(format)) {
                        try (Writer writer = new FileWriter(filePath)) {
                            reportFileService.generateOperationsLogCsv(data, writer);
                        }
                    } else if ("XLSX".equalsIgnoreCase(format)) {
                        byte[] bytes = excelReportService.generateReport(
                                buildOperationsLogTableData(data), "Bitácora de Operaciones");
                        Files.write(tempFile, bytes);
                    } else {
                        byte[] bytes = pdfReportService.generateCorporateReport(
                                buildOperationsLogTableData(data), "Bitácora de Operaciones", requesterName);
                        Files.write(tempFile, bytes);
                    }
                }

                case "PERIOD_SUMMARY" -> {
                    var data = reportDataService.getPeriodSummaryData(farmId, startDate, endDate);
                    if ("CSV".equalsIgnoreCase(format)) {
                        try (Writer writer = new FileWriter(filePath)) {
                            reportFileService.generatePeriodSummaryCsv(data, writer);
                        }
                    } else if ("XLSX".equalsIgnoreCase(format)) {
                        byte[] bytes = excelReportService.generateReport(
                                buildPeriodSummaryTableData(data), "Resumen del Período");
                        Files.write(tempFile, bytes);
                    } else {
                        byte[] bytes = pdfReportService.generateCorporateReport(
                                buildPeriodSummaryTableData(data), "Resumen del Período", requesterName);
                        Files.write(tempFile, bytes);
                    }
                }

                default -> throw new IllegalArgumentException("Tipo de reporte no soportado: " + reportType);
            }

            task.setFilePath(filePath);
            task.setStatus(ReportTask.ReportStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            reportTaskRepository.save(task);
            log.info("[ReportTaskService] Tarea {} completada: {}", taskId, filePath);

        } catch (Exception e) {
            log.error("[ReportTaskService] Error generando reporte para tarea {}: {}", taskId, e.getMessage(), e);
            task.setStatus(ReportTask.ReportStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            reportTaskRepository.save(task);
        }
    }

    public ReportTask getTaskStatus(UUID taskId) {
        return reportTaskRepository.findById(taskId).orElseThrow();
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONVERSIÓN DTO → List<String[]> para ExcelReportService
    // ════════════════════════════════════════════════════════════════════════

    private List<String[]> buildWaterBalanceTableData(WaterBalanceReportDTO data) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] {
                "Sector",
                "Agua Riego Total (m³)",
                "Lluvia Efectiva Total (mm)",
                "Horas Riego Total"
        });

        if (data.getSectors() != null) {
            for (var sector : data.getSectors()) {
                var sum = sector.getSummary();
                rows.add(new String[] {
                        sector.getSectorName(),
                        sum != null ? bd(sum.getTotalIrrigationWater()) : null,
                        sum != null ? bd(sum.getTotalEffectiveRain()) : null,
                        sum != null ? bd(sum.getTotalIrrigationHours()) : null
                });
            }
        }

        if (data.getFarmTotals() != null) {
            var t = data.getFarmTotals();
            rows.add(new String[] {
                    "TOTAL FINCA",
                    bd(t.getTotalIrrigationWater()),
                    bd(t.getTotalEffectiveRain()),
                    bd(t.getTotalIrrigationHours())
            });
        }
        return rows;
    }

    private List<String[]> buildOperationsLogTableData(OperationsLogReportDTO data) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] { "Fecha y Hora", "Tipo", "Descripción", "Ubicación", "Usuario" });

        if (data.getOperations() != null) {
            for (var op : data.getOperations()) {
                rows.add(new String[] {
                        op.getDatetime(),
                        op.getType(),
                        op.getDescription(),
                        op.getLocation(),
                        op.getUserName()
                });
            }
        }
        return rows;
    }

    private List<String[]> buildPeriodSummaryTableData(PeriodSummaryReportDTO data) {
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[] { "Métrica", "Valor" });

        if (data.getWaterSummary() != null) {
            var ws = data.getWaterSummary();
            rows.add(new String[] { "Agua de Riego Total (m³)", bd(ws.getTotalIrrigationWaterM3()) });
            rows.add(new String[] { "Horas de Riego Total", bd(ws.getTotalIrrigationHours()) });
            rows.add(new String[] { "Precipitación Total (mm)", bd(ws.getTotalPrecipitationMM()) });
            rows.add(new String[] { "Precipitación Efectiva (mm)", bd(ws.getTotalEffectivePrecipitationMM()) });
        }

        if (data.getOperationsSummary() != null) {
            var os = data.getOperationsSummary();
            rows.add(new String[] { "Tareas Creadas", String.valueOf(os.getTasksCreated()) });
            rows.add(new String[] { "Tareas Completadas", String.valueOf(os.getTasksCompleted()) });
            rows.add(new String[] { "Registros de Mantenimiento", String.valueOf(os.getMaintenanceRecords()) });
            rows.add(new String[] { "Registros de Fertilización", String.valueOf(os.getFertilizationRecords()) });
        }

        if (data.getWaterUsageBySector() != null) {
            rows.add(new String[] { "", "" });
            rows.add(new String[] { "Uso de Agua por Sector (m³)", "" });
            for (var sector : data.getWaterUsageBySector()) {
                rows.add(new String[] { sector.getSectorName(), bd(sector.getTotalWaterM3()) });
            }
        }

        return rows;
    }

    /** Convierte BigDecimal a String limpio; retorna null si es null. */
    private String bd(BigDecimal value) {
        return value != null ? value.toPlainString() : null;
    }
}
