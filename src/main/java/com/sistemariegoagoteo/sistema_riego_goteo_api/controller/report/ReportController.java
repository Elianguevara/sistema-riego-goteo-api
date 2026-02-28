package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.report;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.TaskResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.report.ReportTask;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.report.ExcelReportService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.report.PdfReportService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.report.ReportTaskService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.security.access.AccessDeniedException;
import java.security.Principal;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Controlador REST para generación y descarga de reportes corporativos.
 *
 * Endpoints existentes (flujo asíncrono):
 * GET /api/reports/generate → inicia generación de reporte
 * GET /api/reports/status/{taskId} → consulta estado de la tarea
 * GET /api/reports/download/{taskId} → descarga el archivo generado
 *
 * Nuevos endpoints (descarga directa sincrónica):
 * GET /api/reports/tasks/pdf → PDF de todas las tareas
 * GET /api/reports/tasks/excel → Excel de todas las tareas
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * PREREQUISITO: Añadir el siguiente método a TaskService para que compile:
 *
 * {@code @Transactional(readOnly = true)}
 * {@code public List<Task> getAllTasks() { return taskRepository.findAll(); }}
 *
 * TaskService no expone actualmente una consulta sin filtros; ese método es el
 * mínimo necesario para que los reportes de ADMIN accedan a todas las tareas.
 * ─────────────────────────────────────────────────────────────────────────────
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA')")
public class ReportController {

    private final ReportTaskService reportTaskService;
    private final PdfReportService pdfReportService;
    private final ExcelReportService excelReportService;
    private final TaskService taskService;

    // Formato de fecha para los nombres de archivo (ej: "20240315")
    private static final DateTimeFormatter FILE_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // Formato de fecha legible para las celdas del reporte (detectado por
    // ExcelReportService)
    private static final SimpleDateFormat CELL_DATE_FMT = new SimpleDateFormat("dd/MM/yyyy");

    // MIME type oficial de Excel OOXML (.xlsx)
    private static final String XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    // ════════════════════════════════════════════════════════════════════════
    // ENDPOINTS EXISTENTES — sin modificaciones
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Inicia la generación asíncrona de un reporte.
     * Devuelve HTTP 202 Accepted con el ID de la tarea de seguimiento.
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
     * Consulta el estado actual de una tarea de generación de reporte.
     */
    @GetMapping("/status/{taskId}")
    public ResponseEntity<ReportTask> getReportStatus(@PathVariable UUID taskId) {
        return ResponseEntity.ok(reportTaskService.getTaskStatus(taskId));
    }

    /**
     * Descarga el archivo generado una vez que la tarea está en estado COMPLETED.
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
        String fmt = task.getFormat().toUpperCase();
        String contentType = switch (fmt) {
            case "CSV" -> "text/csv";
            case "XLSX" -> XLSX_MIME;
            default -> MediaType.APPLICATION_PDF_VALUE;
        };

        String dateStr = task.getCompletedAt() != null
                ? task.getCompletedAt().format(FILE_DATE_FMT)
                : LocalDate.now().format(FILE_DATE_FMT);
        String baseName = resolveReportBaseName(task.getReportType());
        String filename = baseName + "-" + dateStr + "." + task.getFormat().toLowerCase();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .contentLength(file.length())
                .body(resource);
    }

    // ════════════════════════════════════════════════════════════════════════
    // NUEVOS ENDPOINTS — descarga directa de tareas
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Genera y descarga un reporte PDF con todas las tareas del sistema.
     *
     * El nombre del archivo incluye la fecha actual:
     * {@code tasks-report-YYYYMMDD.pdf}
     *
     * @param principal Inyectado por Spring Security; provee el nombre del usuario
     *                  solicitante
     */
    @GetMapping("/tasks/pdf")
    public ResponseEntity<byte[]> downloadTasksPdf(Principal principal) {
        String requester = resolveRequesterName(principal);
        log.info("[ReportController] PDF de tareas solicitado por '{}'", requester);

        try {
            // 1. Obtener todas las tareas y convertirlas al formato de tabla
            List<String[]> tableData = buildTaskTableData(taskService.getAllTasks());

            // 2. Generar el PDF con membrete corporativo, footer y tabla estilizada
            byte[] bytes = pdfReportService.generateCorporateReport(
                    tableData, "Reporte de Tareas", requester);

            String filename = "tasks-report-" + LocalDate.now().format(FILE_DATE_FMT) + ".pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(bytes.length)
                    .body(bytes);

        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ReportController] Error al generar PDF de tareas: {}", e.getMessage(), e);
            return buildErrorResponse("Error al generar el reporte PDF: " + e.getMessage());
        }
    }

    /**
     * Genera y descarga un reporte Excel (.xlsx) con todas las tareas del sistema.
     *
     * El nombre del archivo incluye la fecha actual:
     * {@code tasks-report-YYYYMMDD.xlsx}
     * Las columnas con fechas en formato dd/MM/yyyy son convertidas automáticamente
     * a celdas de fecha nativa por {@link ExcelReportService}.
     *
     * @param principal Inyectado por Spring Security; se usa solo para el log de
     *                  auditoría
     */
    @GetMapping("/tasks/excel")
    public ResponseEntity<byte[]> downloadTasksExcel(Principal principal) {
        String requester = resolveRequesterName(principal);
        log.info("[ReportController] Excel de tareas solicitado por '{}'", requester);

        try {
            // 1. Obtener todas las tareas y convertirlas al formato de tabla
            List<String[]> tableData = buildTaskTableData(taskService.getAllTasks());

            // 2. Generar el libro Excel con membrete, logo y tabla estilizada
            byte[] bytes = excelReportService.generateReport(tableData, "Reporte de Tareas");

            String filename = "tasks-report-" + LocalDate.now().format(FILE_DATE_FMT) + ".xlsx";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.parseMediaType(XLSX_MIME))
                    .contentLength(bytes.length)
                    .body(bytes);

        } catch (AccessDeniedException e) {
            throw e;
        } catch (Exception e) {
            log.error("[ReportController] Error al generar Excel de tareas: {}", e.getMessage(), e);
            return buildErrorResponse("Error al generar el reporte Excel: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILIDADES PRIVADAS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Convierte la lista de entidades
     * {@link com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Task}
     * al formato de tabla {@code List<String[]>} requerido por los servicios de
     * reporte.
     *
     * Estructura del resultado:
     * 
     * <pre>
     *   [0]    → { "ID", "Descripción", "Sector", "Finca", "Estado", "Creado por", "Asignado a", "Fecha" }
     *   [1..n] → valores de cada tarea
     * </pre>
     *
     * Las fechas se formatean como "dd/MM/yyyy" para que {@link ExcelReportService}
     * las detecte automáticamente y las convierta a celdas de fecha nativa en
     * Excel.
     */
    private List<String[]> buildTaskTableData(
            List<com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Task> tasks) {

        List<String[]> data = new ArrayList<>();

        // Fila 0: encabezados de columna
        data.add(new String[] {
                "ID", "Descripción", "Sector", "Finca",
                "Estado", "Creado por", "Asignado a", "Fecha Creación"
        });

        // Filas 1..n: una fila por tarea
        for (var task : tasks) {
            // Construir el DTO para aprovechar la lógica de mapeo ya existente
            TaskResponse dto = new TaskResponse(task);

            data.add(new String[] {
                    String.valueOf(dto.getId()),
                    nullSafe(dto.getDescription()),
                    nullSafe(dto.getSectorName()),
                    nullSafe(dto.getFarmName()),
                    dto.getStatus() != null ? dto.getStatus().name() : null,
                    nullSafe(dto.getCreatedByUsername()),
                    nullSafe(dto.getAssignedToUsername()),
                    // Formato dd/MM/yyyy → detectado como fecha por ExcelReportService
                    dto.getCreatedAt() != null ? CELL_DATE_FMT.format(dto.getCreatedAt()) : null
            });
        }

        return data;
    }

    /**
     * Devuelve el nombre del usuario autenticado, o "Sistema" si no hay principal.
     * Evita NullPointerException en entornos de test donde Principal puede ser
     * null.
     */
    private String resolveRequesterName(Principal principal) {
        return (principal != null && principal.getName() != null)
                ? principal.getName()
                : "Sistema";
    }

    /**
     * Retorna el valor o {@code null} si es nulo o en blanco.
     * Los servicios de reporte reemplazan null por "—" (guión largo).
     */
    private String nullSafe(String value) {
        return (value != null && !value.isBlank()) ? value : null;
    }

    /**
     * Convierte el tipo de reporte interno a un nombre de archivo legible.
     * Ej: "WATER_BALANCE" → "balance-hidrico"
     */
    private String resolveReportBaseName(String reportType) {
        if (reportType == null)
            return "reporte";
        return switch (reportType.toUpperCase()) {
            case "WATER_BALANCE" -> "balance-hidrico";
            case "OPERATIONS_LOG" -> "bitacora-operaciones";
            case "PERIOD_SUMMARY" -> "resumen-periodo";
            default -> reportType.toLowerCase().replace('_', '-');
        };
    }

    /**
     * Construye una respuesta de error 500 con el mensaje en texto plano.
     * Se usa como cuerpo de {@code ResponseEntity<byte[]>} para que el cliente
     * reciba un mensaje legible en lugar de un body vacío.
     */
    private ResponseEntity<byte[]> buildErrorResponse(String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_PLAIN)
                .body(message.getBytes(StandardCharsets.UTF_8));
    }
}
