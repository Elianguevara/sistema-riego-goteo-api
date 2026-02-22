package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.report;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.report.ReportTask;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.report.ReportTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA')")
public class ReportController {

    private final ReportTaskService reportTaskService;

    /**
     * Inicia la generación asíncrona de reportes.
     * Devuelve un 202 Accepted con el ID de la tarea.
     */
    @GetMapping("/generate")
    public ResponseEntity<ReportTask> generateReport(
            @RequestParam String reportType,
            @RequestParam Integer farmId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
            @RequestParam(defaultValue = "PDF") String format,
            @RequestParam(required = false) List<Integer> sectorIds,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) Long userId) {

        ReportTask task = reportTaskService.createAndStartTask(
                reportType, farmId, startDate, endDate, format, sectorIds, operationType, userId);

        return ResponseEntity.status(HttpStatus.ACCEPTED).body(task);
    }

    /**
     * Consulta el estado de una tarea de reporte.
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<ReportTask> getReportStatus(@PathVariable UUID taskId) {
        return ResponseEntity.ok(reportTaskService.getTaskStatus(taskId));
    }

    /**
     * Descarga el archivo de reporte generado.
     */
    @GetMapping("/download/{taskId}")
    public ResponseEntity<Resource> downloadReport(@PathVariable UUID taskId) throws IOException {
        ReportTask task = reportTaskService.getTaskStatus(taskId);

        if (task.getStatus() != ReportTask.ReportStatus.COMPLETED) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        File file = new File(task.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        FileSystemResource resource = new FileSystemResource(file);
        String contentType = task.getFormat().equalsIgnoreCase("CSV") ? "text/csv" : MediaType.APPLICATION_PDF_VALUE;
        String filename = "Reporte_" + task.getReportType() + "." + task.getFormat().toLowerCase();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(file.length())
                .body(resource);
    }
}
