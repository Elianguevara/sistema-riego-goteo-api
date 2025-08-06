package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.audit;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.audit.ChangeHistoryResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.audit.SynchronizationRecordResponse; // Nuevo DTO
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.audit.SynchronizationStatusUpdateRequest; // Nuevo DTO
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.audit.BatchSyncRequest; // Nuevo DTO
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.ChangeHistory;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.audit.Synchronization; // Nuevo Modelo
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;

import jakarta.validation.Valid; // Para @Valid
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map; // Para respuesta simple

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')") // Proteger todo el controlador para administradores
public class AuditController {

    private final AuditService auditService;

    // --- Endpoints de ChangeHistory (existentes) ---
    @GetMapping("/change-history")
    public ResponseEntity<Page<ChangeHistoryResponse>> getChangeHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String affectedTable,
            @RequestParam(required = false) String actionType, // <-- PARÁMETRO AÑADIDO
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Date endDate,
            @PageableDefault(size = 20, sort = "changeDatetime,desc") Pageable pageable) {
        
        log.info("Fetching change history with params: userId={}, affectedTable={}, actionType={}, searchTerm={}",
                userId, affectedTable, actionType, searchTerm);
        
        // --- LLAMADA AL SERVICIO ACTUALIZADA ---
        Page<ChangeHistory> historyPage = auditService.getChangeHistory(userId, affectedTable, actionType, searchTerm, startDate, endDate, pageable);
        
        Page<ChangeHistoryResponse> responsePage = historyPage.map(ChangeHistoryResponse::new);
        return ResponseEntity.ok(responsePage);
    }

    @GetMapping("/change-history/{logId}")
    public ResponseEntity<ChangeHistoryResponse> getChangeHistoryDetail(@PathVariable Integer logId) {
        log.info("Fetching change history detail for log ID: {}", logId);
        ChangeHistory history = auditService.getChangeHistoryDetail(logId)
                .orElseThrow(() -> new ResourceNotFoundException("ChangeHistory", "id", logId));
        return ResponseEntity.ok(new ChangeHistoryResponse(history));
    }

    // --- Nuevos Endpoints para Synchronization ---

    /**
     * Obtiene registros de sincronización pendientes (isSynchronized = false).
     * Opcionalmente filtrado por nombre de tabla.
     */
    @GetMapping("/synchronization/pending")
    // @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_SYNC_CLIENT')") // Podría tener un rol específico para el cliente de sincronización
    public ResponseEntity<Page<SynchronizationRecordResponse>> getPendingSynchronizations(
            @RequestParam(required = false) String tableName,
            @PageableDefault(size = 100, sort = "modificationDatetime,asc") Pageable pageable) {
        log.info("Fetching pending synchronization records, tableName: {}", tableName);
        Page<Synchronization> syncPage = auditService.getPendingSynchronizations(tableName, pageable);
        Page<SynchronizationRecordResponse> responsePage = syncPage.map(SynchronizationRecordResponse::new);
        return ResponseEntity.ok(responsePage);
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
        Page<SynchronizationRecordResponse> responsePage = syncPage.map(SynchronizationRecordResponse::new);
        return ResponseEntity.ok(responsePage);
    }


    /**
     * Actualiza el estado de sincronización de un registro específico.
     */
    @PutMapping("/synchronization/{syncId}/status")
    // @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_SYNC_CLIENT')")
    public ResponseEntity<SynchronizationRecordResponse> updateSynchronizationStatus(
            @PathVariable Integer syncId,
            @Valid @RequestBody SynchronizationStatusUpdateRequest request) {
        log.info("Updating synchronization status for sync ID {}: {}", syncId, request.getIsSynchronized());
        try {
            Synchronization updatedSync = auditService.updateSynchronizationStatus(syncId, request.getIsSynchronized());
            return ResponseEntity.ok(new SynchronizationRecordResponse(updatedSync));
        } catch (ResourceNotFoundException e) {
            log.warn("Cannot update sync status, resource not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null); // No hay cuerpo de respuesta específico aquí
        }
    }

    /**
     * Actualiza el estado de sincronización para un lote de registros.
     */
    @PostMapping("/synchronization/batch-update-status")
    // @PreAuthorize("hasAnyRole('ADMIN', 'SYSTEM_SYNC_CLIENT')")
    public ResponseEntity<?> batchUpdateSynchronizationStatus(@Valid @RequestBody BatchSyncRequest request) {
        log.info("Batch updating synchronization status for {} IDs to: {}", request.getSyncIds().size(), request.getIsSynchronized());
        int updatedCount = auditService.batchUpdateSynchronizationStatus(request.getSyncIds(), request.getIsSynchronized());
        return ResponseEntity.ok(Map.of("message", "Batch update processed.", "updatedCount", updatedCount));
    }

    // NOTA: No se exponen endpoints POST para crear Synchronization records directamente vía API,
    // ya que estos deben ser generados por el sistema cuando las entidades principales cambian.
    // El método `recordModificationForSync` en AuditService es para uso interno.
}