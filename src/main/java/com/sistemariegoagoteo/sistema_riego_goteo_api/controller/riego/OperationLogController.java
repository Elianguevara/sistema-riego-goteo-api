package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.OperationLogRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.OperationLogResponse;
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
@RequestMapping()
@RequiredArgsConstructor
@Slf4j
public class OperationLogController {

    private final OperationLogService operationLogService;

    /**
     * Crea una nueva entrada en la bitácora de operaciones para una finca.
     * * @param farmId ID de la finca.
     * @param request Datos de la operación a registrar.
     * @return La entrada creada con estado 201 Created.
     */
    @PostMapping("/api/farms/{farmId}/operationlogs")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('REGISTRAR_OPERACION')")
    public ResponseEntity<OperationLogResponse> createOperationLog(@PathVariable Integer farmId,
                                                                   @Valid @RequestBody OperationLogRequest request) {
        log.info("Solicitud POST para crear entrada en bitácora para finca ID {}", farmId);
        OperationLog newLog = operationLogService.createOperationLog(farmId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new OperationLogResponse(newLog));
    }

    /**
     * Obtiene todas las entradas de la bitácora para una finca, con filtro opcional por tipo de operación.
     * * @param farmId ID de la finca.
     * @param type (Opcional) Tipo de operación para filtrar (ej. "Mantenimiento", "Cosecha").
     * @return Lista de entradas de bitácora.
     */
    @GetMapping("/api/farms/{farmId}/operationlogs")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_BITACORA_OPERACIONES')")
    public ResponseEntity<List<OperationLogResponse>> getOperationLogsByFarm(
            @PathVariable Integer farmId,
            @RequestParam(required = false) String type) {
        log.info("Solicitud GET para obtener bitácora de la finca ID {}", farmId);
        
        List<OperationLogResponse> responses = operationLogService.getOperationLogsByFarm(farmId)
                .stream()
                // Lógica de filtrado en memoria (idealmente mover al repositorio si crece mucho la data)
                .filter(log -> type == null || log.getOperationType().equalsIgnoreCase(type))
                .map(OperationLogResponse::new)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene una entrada específica de la bitácora por su ID.
     * * @param logId ID de la entrada de bitácora.
     * @return Detalle de la entrada.
     */
    @GetMapping("/api/operationlogs/{logId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_BITACORA_OPERACIONES')")
    public ResponseEntity<OperationLogResponse> getOperationLogById(@PathVariable Integer logId) {
        log.info("Solicitud GET para obtener entrada de bitácora ID: {}", logId);
        OperationLog operationLog = operationLogService.getOperationLogById(logId);
        return ResponseEntity.ok(new OperationLogResponse(operationLog));
    }

    /**
     * Actualiza una entrada existente en la bitácora.
     * * @param logId ID de la entrada a actualizar.
     * @param request Nuevos datos de la operación.
     * @return La entrada actualizada.
     */
    @PutMapping("/api/operationlogs/{logId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('MODIFICAR_OPERACION')")
    public ResponseEntity<OperationLogResponse> updateOperationLog(@PathVariable Integer logId,
                                                                   @Valid @RequestBody OperationLogRequest request) {
        log.info("Solicitud PUT para actualizar entrada de bitácora ID {}", logId);
        OperationLog updatedLog = operationLogService.updateOperationLog(logId, request);
        return ResponseEntity.ok(new OperationLogResponse(updatedLog));
    }

    /**
     * Elimina una entrada de la bitácora.
     * * @param logId ID de la entrada a eliminar.
     * @return 204 No Content si se elimina correctamente.
     */
    @DeleteMapping("/api/operationlogs/{logId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ELIMINAR_OPERACION')")
    public ResponseEntity<Void> deleteOperationLog(@PathVariable Integer logId) {
        log.info("Solicitud DELETE para eliminar entrada de bitácora ID: {}", logId);
        operationLogService.deleteOperationLog(logId);
        return ResponseEntity.noContent().build();
    }
}