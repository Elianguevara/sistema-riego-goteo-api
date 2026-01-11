package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.sync;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncBatchRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.sync.MobileSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/sync")
@RequiredArgsConstructor
@Slf4j
public class MobileSyncController {

    private final MobileSyncService mobileSyncService;

    /**
     * Sincroniza un lote de registros de riego desde la aplicación móvil.
     * Solo accesible para usuarios con rol OPERARIO.
     *
     * @param batchRequest Lote de registros a sincronizar.
     * @return Respuesta con el resultado de la sincronización (éxitos y fallos).
     */
    @PostMapping("/irrigations")
    @PreAuthorize("hasRole('OPERARIO')")
    public ResponseEntity<IrrigationSyncResponse> syncIrrigationBatch(
            @Valid @RequestBody IrrigationSyncBatchRequest batchRequest) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        log.info("Operario '{}' iniciando sincronización de {} registros de irrigación.", username, batchRequest.getIrrigations().size());

        IrrigationSyncResponse response = mobileSyncService.processIrrigationBatch(username, batchRequest);

        if (response.getFailedItems() > 0) {
            log.warn("Sincronización de riegos para operario '{}' completada con {} errores de {} items.",
                    username, response.getFailedItems(), response.getTotalItems());
        } else {
            log.info("Sincronización de riegos para operario '{}' completada exitosamente ({} items).",
                    username, response.getTotalItems());
        }

        return ResponseEntity.ok(response);
    }
}