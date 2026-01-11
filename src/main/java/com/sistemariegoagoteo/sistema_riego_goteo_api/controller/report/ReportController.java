package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.report;

import com.lowagie.text.DocumentException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.report.ReportDataService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.report.ReportFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA')")
public class ReportController {

    private final ReportDataService reportDataService;
    private final ReportFileService reportFileService;

    /**
     * Genera y descarga reportes en formato PDF o CSV.
     * Soporta múltiples tipos de reporte (Balance Hídrico, Bitácora, Resumen).
     *
     * @param reportType Tipo de reporte (WATER_BALANCE, OPERATIONS_LOG, PERIOD_SUMMARY).
     * @param farmId ID de la finca.
     * @param startDate Fecha inicio.
     * @param endDate Fecha fin.
     * @param format Formato de salida (PDF o CSV).
     * @param sectorIds (Opcional) IDs de sectores para filtrar.
     * @param operationType (Opcional) Tipo de operación para filtrar bitácora.
     * @param userId (Opcional) ID de usuario para filtrar bitácora.
     * @return Recurso binario con el archivo generado.
     * @throws IOException Si ocurre un error de entrada/salida al generar el archivo.
     * @throws DocumentException Si ocurre un error al generar el PDF.
     */
    @GetMapping("/generate")
    public ResponseEntity<Resource> generateReport(
            @RequestParam String reportType,
            @RequestParam Integer farmId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
            @RequestParam(defaultValue = "PDF") String format,
            @RequestParam(required = false) List<Integer> sectorIds,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Long userId) throws IOException, DocumentException {

        byte[] reportContent;
        String filename;
        String contentType;

        // Selección de lógica según el tipo de reporte
        // Si reportType no es válido, lanzamos IllegalArgumentException (manejada globalmente)
        switch (reportType.toUpperCase()) {
            case "WATER_BALANCE" -> {
                var waterData = reportDataService.getWaterBalanceData(farmId, startDate, endDate, sectorIds);
                if ("CSV".equalsIgnoreCase(format)) {
                    reportContent = reportFileService.generateWaterBalanceCsv(waterData);
                    filename = "Balance_Hidrico.csv";
                    contentType = "text/csv";
                } else {
                    reportContent = reportFileService.generateWaterBalancePdf(waterData);
                    filename = "Balance_Hidrico.pdf";
                    contentType = MediaType.APPLICATION_PDF_VALUE;
                }
            }
            case "OPERATIONS_LOG" -> {
                var logData = reportDataService.getOperationsLogData(farmId, startDate, endDate, operationType, userId);
                if ("CSV".equalsIgnoreCase(format)) {
                    reportContent = reportFileService.generateOperationsLogCsv(logData);
                    filename = "Bitacora_Operaciones.csv";
                    contentType = "text/csv";
                } else {
                    reportContent = reportFileService.generateOperationsLogPdf(logData);
                    filename = "Bitacora_Operaciones.pdf";
                    contentType = MediaType.APPLICATION_PDF_VALUE;
                }
            }
            case "PERIOD_SUMMARY" -> {
                var summaryData = reportDataService.getPeriodSummaryData(farmId, startDate, endDate);
                if ("CSV".equalsIgnoreCase(format)) {
                    reportContent = reportFileService.generatePeriodSummaryCsv(summaryData);
                    filename = "Resumen_Periodo.csv";
                    contentType = "text/csv";
                } else {
                    reportContent = reportFileService.generatePeriodSummaryPdf(summaryData);
                    filename = "Resumen_Periodo.pdf";
                    contentType = MediaType.APPLICATION_PDF_VALUE;
                }
            }
            default -> throw new IllegalArgumentException("Tipo de reporte no válido: " + reportType);
        }

        ByteArrayResource resource = new ByteArrayResource(reportContent);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(resource.contentLength())
                .body(resource);
    }
}