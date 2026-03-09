package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.sync;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.*;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.sync.MobileSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mobile/sync")
@RequiredArgsConstructor
@Slf4j
public class MobileSyncController {

    private final MobileSyncService mobileSyncService;

    // ─── Riego ────────────────────────────────────────────────────────────────

    /**
     * Sincroniza un lote de registros de riego desde la aplicación móvil.
     * Solo accesible para usuarios con rol OPERARIO.
     */
    @PostMapping("/irrigations")
    @PreAuthorize("hasRole('OPERARIO')")
    public ResponseEntity<IrrigationSyncResponse> syncIrrigationBatch(
            @Valid @RequestBody IrrigationSyncBatchRequest batchRequest) {

        String username = currentUsername();
        log.info("Operario '{}' iniciando sincronización de {} registros de irrigación.",
                username, batchRequest.getIrrigations().size());

        IrrigationSyncResponse response = mobileSyncService.processIrrigationBatch(username, batchRequest);
        logResult("riegos", username, response.getFailedItems(), response.getTotalItems());
        return ResponseEntity.ok(response);
    }

    // ─── Fertilización ────────────────────────────────────────────────────────

    /**
     * Sincroniza un lote de registros de fertilización desde la aplicación móvil.
     */
    @PostMapping("/fertilizations")
    @PreAuthorize("hasRole('OPERARIO')")
    public ResponseEntity<MobileSyncResponse> syncFertilizationBatch(
            @Valid @RequestBody FertilizationSyncBatchRequest batchRequest) {

        String username = currentUsername();
        log.info("Operario '{}' iniciando sincronización de {} registros de fertilización.",
                username, batchRequest.getFertilizations().size());

        MobileSyncResponse response = mobileSyncService.processFertilizationBatch(username, batchRequest);
        logResult("fertilizaciones", username, response.getFailedItems(), response.getTotalItems());
        return ResponseEntity.ok(response);
    }

    // ─── Mantenimiento ────────────────────────────────────────────────────────

    /**
     * Sincroniza un lote de registros de mantenimiento desde la aplicación móvil.
     */
    @PostMapping("/maintenances")
    @PreAuthorize("hasRole('OPERARIO')")
    public ResponseEntity<MobileSyncResponse> syncMaintenanceBatch(
            @Valid @RequestBody MaintenanceSyncBatchRequest batchRequest) {

        String username = currentUsername();
        log.info("Operario '{}' iniciando sincronización de {} registros de mantenimiento.",
                username, batchRequest.getMaintenances().size());

        MobileSyncResponse response = mobileSyncService.processMaintenanceBatch(username, batchRequest);
        logResult("mantenimientos", username, response.getFailedItems(), response.getTotalItems());
        return ResponseEntity.ok(response);
    }

    // ─── Bitácora ─────────────────────────────────────────────────────────────

    /**
     * Sincroniza un lote de entradas de bitácora de operaciones desde la aplicación móvil.
     */
    @PostMapping("/operation-logs")
    @PreAuthorize("hasRole('OPERARIO')")
    public ResponseEntity<MobileSyncResponse> syncOperationLogBatch(
            @Valid @RequestBody OperationLogSyncBatchRequest batchRequest) {

        String username = currentUsername();
        log.info("Operario '{}' iniciando sincronización de {} entradas de bitácora.",
                username, batchRequest.getOperationLogs().size());

        MobileSyncResponse response = mobileSyncService.processOperationLogBatch(username, batchRequest);
        logResult("entradas de bitácora", username, response.getFailedItems(), response.getTotalItems());
        return ResponseEntity.ok(response);
    }

    // ─── Estado de tareas ─────────────────────────────────────────────────────

    /**
     * Sincroniza un lote de cambios de estado de tareas desde la aplicación móvil.
     */
    @PostMapping("/task-status")
    @PreAuthorize("hasRole('OPERARIO')")
    public ResponseEntity<MobileSyncResponse> syncTaskStatusBatch(
            @Valid @RequestBody TaskStatusSyncBatchRequest batchRequest) {

        String username = currentUsername();
        log.info("Operario '{}' iniciando sincronización de {} cambios de estado de tareas.",
                username, batchRequest.getTaskStatusUpdates().size());

        MobileSyncResponse response = mobileSyncService.processTaskStatusBatch(username, batchRequest);
        logResult("estados de tareas", username, response.getFailedItems(), response.getTotalItems());
        return ResponseEntity.ok(response);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }

    private void logResult(String entity, String username, int failed, int total) {
        if (failed > 0) {
            log.warn("Sincronización de {} para operario '{}' completada con {} errores de {} items.",
                    entity, username, failed, total);
        } else {
            log.info("Sincronización de {} para operario '{}' completada exitosamente ({} items).",
                    entity, username, total);
        }
    }
}
