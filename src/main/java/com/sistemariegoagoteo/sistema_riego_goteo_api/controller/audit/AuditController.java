package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.audit;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.audit.BatchSyncRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.audit.ChangeHistoryResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.audit.SynchronizationRecordResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.audit.SynchronizationStatusUpdateRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.ChangeHistory;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.Synchronization;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    // --- Endpoints de ChangeHistory ---

    /**
     * Obtiene el historial de cambios del sistema con múltiples filtros.
     */
    @GetMapping("/change-history")
    public ResponseEntity<Page<ChangeHistoryResponse>> getChangeHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String affectedTable,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
            @PageableDefault(size = 20, sort = "changeDatetime,desc") Pageable pageable) {

        log.info("Fetching change history with params: userId={}, affectedTable={}, actionType={}, searchTerm={}",
                userId, affectedTable, actionType, searchTerm);

        Page<ChangeHistory> historyPage = auditService.getChangeHistory(userId, affectedTable, actionType, searchTerm, startDate, endDate, pageable);
        return ResponseEntity.ok(historyPage.map(ChangeHistoryResponse::new));
    }

    /**
     * Obtiene el detalle completo de un registro de cambio específico.
     */
    @GetMapping("/change-history/{logId}")
    public ResponseEntity<ChangeHistoryResponse> getChangeHistoryDetail(@PathVariable Integer logId) {
        log.info("Fetching change history detail for log ID: {}", logId);
        // El servicio o el orElseThrow lanzan ResourceNotFoundException si no existe, manejado globalmente
        ChangeHistory history = auditService.getChangeHistoryDetail(logId)
                .orElseThrow(() -> new com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException("ChangeHistory", "id", logId));
        return ResponseEntity.ok(new ChangeHistoryResponse(history));
    }

    // --- Endpoints para Synchronization ---

    /**
     * Obtiene registros de sincronización pendientes (isSynchronized = false).
     */
    @GetMapping("/synchronization/pending")
    public ResponseEntity<Page<SynchronizationRecordResponse>> getPendingSynchronizations(
            @RequestParam(required = false) String tableName,
            @PageableDefault(size = 100, sort = "modificationDatetime,asc") Pageable pageable) {
        log.info("Fetching pending synchronization records, tableName: {}", tableName);
        Page<Synchronization> syncPage = auditService.getPendingSynchronizations(tableName, pageable);
        return ResponseEntity.ok(syncPage.map(SynchronizationRecordResponse::new));
    }

    /**
     * Obtiene todos los registros de sincronización, con filtros opcionales.
     */
    @GetMapping("/synchronization")
    public ResponseEntity<Page<SynchronizationRecordResponse>> getAllSynchronizationRecords(
            @RequestParam(required = false) String tableName,
            @RequestParam(required = false) Boolean isSynchronized,
            @PageableDefault(size = 20, sort = "modificationDatetime,desc") Pageable pageable) {
        log.info("Fetching all synchronization records, tableName: {}, isSynchronized: {}", tableName, isSynchronized);
        Page<Synchronization> syncPage = auditService.getAllSynchronizationRecords(tableName, isSynchronized, pageable);
        return ResponseEntity.ok(syncPage.map(SynchronizationRecordResponse::new));
    }

    /**
     * Actualiza el estado de sincronización de un registro específico.
     */
    @PutMapping("/synchronization/{syncId}/status")
    public ResponseEntity<SynchronizationRecordResponse> updateSynchronizationStatus(
            @PathVariable Integer syncId,
            @Valid @RequestBody SynchronizationStatusUpdateRequest request) {
        log.info("Updating synchronization status for sync ID {}: {}", syncId, request.getIsSynchronized());
        Synchronization updatedSync = auditService.updateSynchronizationStatus(syncId, request.getIsSynchronized());
        return ResponseEntity.ok(new SynchronizationRecordResponse(updatedSync));
    }

    /**
     * Actualiza el estado de sincronización para un lote de registros.
     */
    @PostMapping("/synchronization/batch-update-status")
    public ResponseEntity<Map<String, Object>> batchUpdateSynchronizationStatus(@Valid @RequestBody BatchSyncRequest request) {
        log.info("Batch updating synchronization status for {} IDs to: {}", request.getSyncIds().size(), request.getIsSynchronized());
        int updatedCount = auditService.batchUpdateSynchronizationStatus(request.getSyncIds(), request.getIsSynchronized());
        return ResponseEntity.ok(Map.of("message", "Batch update processed.", "updatedCount", updatedCount));
    }
}