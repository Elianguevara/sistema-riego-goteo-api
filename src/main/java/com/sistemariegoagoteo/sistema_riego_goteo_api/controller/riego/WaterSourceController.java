package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.WaterSourceRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.WaterSourceResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.WaterSource;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.WaterSourceService;
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
@RequestMapping() // Ruta base flexible, definida a nivel de método
@RequiredArgsConstructor
@Slf4j
public class WaterSourceController {

    private final WaterSourceService waterSourceService;

    /**
     * Crea una nueva fuente de agua para una finca específica.
     */
    @PostMapping("/api/farms/{farmId}/watersources")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_INFRAESTRUCTURA')")
    public ResponseEntity<?> createWaterSource(@PathVariable Integer farmId,
                                               @Valid @RequestBody WaterSourceRequest request) {
        log.info("Solicitud POST para crear fuente de agua para finca ID {}", farmId);
        try {
            WaterSource newWaterSource = waterSourceService.createWaterSource(farmId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new WaterSourceResponse(newWaterSource));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo crear fuente de agua, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al crear fuente de agua: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Obtiene todas las fuentes de agua para una finca específica.
     */
    @GetMapping("/api/farms/{farmId}/watersources")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_INFRAESTRUCTURA')")
    public ResponseEntity<?> getWaterSourcesByFarm(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener fuentes de agua de la finca ID {}", farmId);
        try {
            List<WaterSourceResponse> responses = waterSourceService.getWaterSourcesByFarm(farmId)
                    .stream()
                    .map(WaterSourceResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Obtiene una fuente de agua específica por su ID global.
     */
    @GetMapping("/api/watersources/{waterSourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_INFRAESTRUCTURA')")
    public ResponseEntity<?> getWaterSourceById(@PathVariable Integer waterSourceId) {
        log.info("Solicitud GET para obtener fuente de agua ID: {}", waterSourceId);
        try {
            WaterSource waterSource = waterSourceService.getWaterSourceById(waterSourceId);
            // Podrías añadir validación para asegurar que el usuario tiene acceso a la finca de esta fuente
            return ResponseEntity.ok(new WaterSourceResponse(waterSource));
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Actualiza una fuente de agua existente.
     */
    @PutMapping("/api/watersources/{waterSourceId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_INFRAESTRUCTURA')")
    public ResponseEntity<?> updateWaterSource(@PathVariable Integer waterSourceId,
                                               @Valid @RequestBody WaterSourceRequest request) {
        log.info("Solicitud PUT para actualizar fuente de agua ID {}", waterSourceId);
        try {
            WaterSource updatedWaterSource = waterSourceService.updateWaterSource(waterSourceId, request);
            return ResponseEntity.ok(new WaterSourceResponse(updatedWaterSource));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo actualizar fuente de agua, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al actualizar fuente de agua: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina una fuente de agua.
     */
    @DeleteMapping("/api/watersources/{waterSourceId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_INFRAESTRUCTURA')")
    public ResponseEntity<?> deleteWaterSource(@PathVariable Integer waterSourceId) {
        log.info("Solicitud DELETE para eliminar fuente de agua ID: {}", waterSourceId);
        try {
            waterSourceService.deleteWaterSource(waterSourceId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo eliminar fuente de agua, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) { // Para el caso de no poder borrar por dependencias
             log.warn("Eliminación no permitida para fuente de agua ID {}: {}", waterSourceId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }
}