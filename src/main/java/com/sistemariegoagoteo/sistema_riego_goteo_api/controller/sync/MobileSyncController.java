package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.sync;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncBatchRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User; // Para obtener el User
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
@RequestMapping("/api/mobile/sync") // Ruta base para endpoints de sincronización móvil
@RequiredArgsConstructor
@Slf4j
public class MobileSyncController {

    private final MobileSyncService mobileSyncService;

    @PostMapping("/irrigations")
    @PreAuthorize("hasRole('OPERARIO')") // Solo los operarios pueden sincronizar sus riegos
    public ResponseEntity<IrrigationSyncResponse> syncIrrigationBatch(
            @Valid @RequestBody IrrigationSyncBatchRequest batchRequest) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName(); // Obtiene el username del operario autenticado

        log.info("Operario '{}' iniciando sincronización de {} registros de irrigación.", username, batchRequest.getIrrigations().size());

        IrrigationSyncResponse response = mobileSyncService.processIrrigationBatch(username, batchRequest);

        if (response.getFailedItems() > 0) {
            log.warn("Sincronización de riegos para operario '{}' completada con {} errores de {} items.",
                    username, response.getFailedItems(), response.getTotalItems());
            // Podrías devolver un 207 Multi-Status si quieres ser más específico,
            // pero un 200 OK con el detalle de errores en el cuerpo es común.
        } else {
            log.info("Sincronización de riegos para operario '{}' completada exitosamente ({} items).",
                    username, response.getTotalItems());
        }

        return ResponseEntity.ok(response);
    }
}