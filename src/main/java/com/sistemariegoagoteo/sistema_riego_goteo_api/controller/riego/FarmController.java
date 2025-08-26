package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FarmRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FarmResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.DeletionNotAllowedException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.FarmService;
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
@RequestMapping("/api/farms")
@RequiredArgsConstructor
@Slf4j
public class FarmController {

    private final FarmService farmService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FarmResponse> createFarm(@Valid @RequestBody FarmRequest farmRequest) {
        log.info("Solicitud POST para crear finca: {}", farmRequest.getName());
        Farm newFarm = farmService.createFarm(farmRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new FarmResponse(newFarm));
    }

    /**
     * Obtiene las fincas. El comportamiento depende del rol del usuario.
     * - ADMIN/ANALISTA: Obtiene todas las fincas.
     * - OPERARIO: Obtiene solo las fincas a las que está asignado.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')") // <-- MODIFICACIÓN: Se añade OPERARIO
    public ResponseEntity<List<FarmResponse>> getAllFarms() {
        log.info("Solicitud GET para obtener fincas");
        List<FarmResponse> farmResponses = farmService.getAllFarms().stream()
                .map(FarmResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(farmResponses);
    }

    @GetMapping("/{farmId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    public ResponseEntity<FarmResponse> getFarmById(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener finca con ID: {}", farmId);
        try {
            Farm farm = farmService.getFarmById(farmId);
            return ResponseEntity.ok(new FarmResponse(farm));
        } catch (ResourceNotFoundException e) {
            log.warn("Finca no encontrada con ID: {}", farmId);
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{farmId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateFarm(@PathVariable Integer farmId,
                                        @Valid @RequestBody FarmRequest farmRequest) {
        log.info("Solicitud PUT para actualizar finca con ID: {}", farmId);
        try {
            Farm updatedFarm = farmService.updateFarm(farmId, farmRequest);
            return ResponseEntity.ok(new FarmResponse(updatedFarm));
        } catch (ResourceNotFoundException e) {
            log.warn("Finca no encontrada para actualizar con ID: {}", farmId);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error al actualizar finca {}: {}", farmId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{farmId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFarm(@PathVariable Integer farmId) {
        log.info("Solicitud DELETE para eliminar finca con ID: {}", farmId);
        try {
            farmService.deleteFarm(farmId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("Finca no encontrada para eliminar con ID: {}", farmId);
            return ResponseEntity.notFound().build();
        } catch (DeletionNotAllowedException e) {
            log.warn("Intento de eliminación no permitido para finca ID {}: {}", farmId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
    }
}
