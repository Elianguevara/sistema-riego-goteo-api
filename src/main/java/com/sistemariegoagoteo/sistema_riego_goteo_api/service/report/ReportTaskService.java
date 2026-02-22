package com.sistemariegoagoteo.sistema_riego_goteo_api.service.report;

import com.lowagie.text.DocumentException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.report.ReportTask;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.report.ReportTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
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

    public ReportTask createAndStartTask(String reportType, Integer farmId, Date startDate, Date endDate,
            String format, List<Integer> sectorIds, String operationType, Long userId) {
        ReportTask task = ReportTask.builder()
                .reportType(reportType)
                .format(format)
                .status(ReportTask.ReportStatus.PENDING)
                .build();

        task = reportTaskRepository.save(task);

        // Iniciar procesamiento asíncrono
        generateReportAsync(task.getId(), reportType, farmId, startDate, endDate, format, sectorIds, operationType,
                userId);

        return task;
    }

    @Async
    public void generateReportAsync(UUID taskId, String reportType, Integer farmId, Date startDate, Date endDate,
            String format, List<Integer> sectorIds, String operationType, Long userId) {
        ReportTask task = reportTaskRepository.findById(taskId).orElseThrow();
        task.setStatus(ReportTask.ReportStatus.PROCESSING);
        reportTaskRepository.save(task);

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
                    } else {
                        try (OutputStream os = new FileOutputStream(filePath)) {
                            reportFileService.generateWaterBalancePdf(data, os);
                        }
                    }
                }
                case "OPERATIONS_LOG" -> {
                    var data = reportDataService.getOperationsLogData(farmId, startDate, endDate, operationType,
                            userId);
                    if ("CSV".equalsIgnoreCase(format)) {
                        try (Writer writer = new FileWriter(filePath)) {
                            reportFileService.generateOperationsLogCsv(data, writer);
                        }
                    } else {
                        try (OutputStream os = new FileOutputStream(filePath)) {
                            reportFileService.generateOperationsLogPdf(data, os);
                        }
                    }
                }
                case "PERIOD_SUMMARY" -> {
                    var data = reportDataService.getPeriodSummaryData(farmId, startDate, endDate);
                    if ("CSV".equalsIgnoreCase(format)) {
                        try (Writer writer = new FileWriter(filePath)) {
                            reportFileService.generatePeriodSummaryCsv(data, writer);
                        }
                    } else {
                        try (OutputStream os = new FileOutputStream(filePath)) {
                            reportFileService.generatePeriodSummaryPdf(data, os);
                        }
                    }
                }
                default -> throw new IllegalArgumentException("Invalid report type: " + reportType);
            }

            task.setFilePath(filePath);
            task.setStatus(ReportTask.ReportStatus.COMPLETED);
            task.setCompletedAt(LocalDateTime.now());
            reportTaskRepository.save(task);
            log.info("Report task {} completed successfully: {}", taskId, filePath);

        } catch (Exception e) {
            log.error("Error generating report for task {}: {}", taskId, e.getMessage(), e);
            task.setStatus(ReportTask.ReportStatus.FAILED);
            task.setErrorMessage(e.getMessage());
            task.setCompletedAt(LocalDateTime.now());
            reportTaskRepository.save(task);
        }
    }

    public ReportTask getTaskStatus(UUID taskId) {
        return reportTaskRepository.findById(taskId).orElseThrow();
    }
}
