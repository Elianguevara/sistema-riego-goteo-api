package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.OperationLogRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.OperationLogResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.OperationLog;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.OperationLogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping() // Ruta base flexible
@RequiredArgsConstructor
@Slf4j
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * Crea una nueva entrada en la bitácora de operaciones para una finca.
     */
    @PostMapping("/api/farms/{farmId}/operationlogs")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('REGISTRAR_OPERACION')")
    public ResponseEntity<?> createOperationLog(@PathVariable Integer farmId,
                                                @Valid @RequestBody OperationLogRequest request) {
        log.info("Solicitud POST para crear entrada en bitácora para finca ID {}", farmId);
        try {
            OperationLog newLog = operationLogService.createOperationLog(farmId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new OperationLogResponse(newLog));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo crear entrada, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al crear entrada: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Obtiene todas las entradas de la bitácora para una finca, con filtro opcional por tipo de operación.
     */
    @GetMapping("/api/farms/{farmId}/operationlogs")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_BITACORA_OPERACIONES')")
    public ResponseEntity<?> getOperationLogsByFarm(
            @PathVariable Integer farmId,
            @RequestParam(required = false) String type) { // <-- Parámetro opcional para filtrar
        log.info("Solicitud GET para obtener bitácora de la finca ID {}", farmId);
        try {
            List<OperationLogResponse> responses = operationLogService.getOperationLogsByFarm(farmId)
                    .stream()
                    // --- Lógica de filtrado opcional ---
                    .filter(log -> type == null || log.getOperationType().equalsIgnoreCase(type))
                    .map(OperationLogResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Obtiene una entrada específica de la bitácora por su ID.
     */
    @GetMapping("/api/operationlogs/{logId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_BITACORA_OPERACIONES')")
    public ResponseEntity<?> getOperationLogById(@PathVariable Integer logId) {
        log.info("Solicitud GET para obtener entrada de bitácora ID: {}", logId);
        try {
            OperationLog operationLog = operationLogService.getOperationLogById(logId);
            return ResponseEntity.ok(new OperationLogResponse(operationLog));
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Actualiza una entrada existente en la bitácora.
     */
    @PutMapping("/api/operationlogs/{logId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('MODIFICAR_OPERACION')")
    public ResponseEntity<?> updateOperationLog(@PathVariable Integer logId,
                                                @Valid @RequestBody OperationLogRequest request) {
        log.info("Solicitud PUT para actualizar entrada de bitácora ID {}", logId);
        try {
            OperationLog updatedLog = operationLogService.updateOperationLog(logId, request);
            return ResponseEntity.ok(new OperationLogResponse(updatedLog));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo actualizar entrada, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al actualizar entrada: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina una entrada de la bitácora.
     */
    @DeleteMapping("/api/operationlogs/{logId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ELIMINAR_OPERACION')")
    public ResponseEntity<?> deleteOperationLog(@PathVariable Integer logId) {
        log.info("Solicitud DELETE para eliminar entrada de bitácora ID: {}", logId);
        try {
            operationLogService.deleteOperationLog(logId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo eliminar entrada, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}