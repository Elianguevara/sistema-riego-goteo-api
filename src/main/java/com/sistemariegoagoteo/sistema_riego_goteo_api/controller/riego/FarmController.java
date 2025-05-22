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
@RequestMapping("/api/farms") // Ruta base para los endpoints de fincas
@RequiredArgsConstructor
@Slf4j
// Podrías aplicar seguridad a nivel de clase si todas las operaciones requieren el mismo rol/permiso
// @PreAuthorize("hasRole('ADMIN')") // o el rol que corresponda
public class FarmController {

    private final FarmService farmService;

    /**
     * Crea una nueva finca.
     * Requiere rol ADMIN (o el que definas).
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')") // O el rol/permiso específico
    public ResponseEntity<FarmResponse> createFarm(@Valid @RequestBody FarmRequest farmRequest) {
        log.info("Solicitud POST para crear finca: {}", farmRequest.getName());
        Farm newFarm = farmService.createFarm(farmRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new FarmResponse(newFarm));
    }

    /**
     * Obtiene todas las fincas.
     * Podría ser accesible por roles como ADMIN, ANALISTA.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA')") // Ajusta los roles según sea necesario
    public ResponseEntity<List<FarmResponse>> getAllFarms() {
        log.info("Solicitud GET para obtener todas las fincas");
        List<FarmResponse> farmResponses = farmService.getAllFarms().stream()
                .map(FarmResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(farmResponses);
    }

    /**
     * Obtiene una finca específica por su ID.
     */
    @GetMapping("/{farmId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')") // Ajusta según quién necesite ver detalles de una finca
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

    /**
     * Actualiza una finca existente.
     */
      @PutMapping("/{farmId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateFarm(@PathVariable Integer farmId, // Cambiado de ResponseEntity<FarmResponse> a ResponseEntity<?>
                                                   @Valid @RequestBody FarmRequest farmRequest) {
        log.info("Solicitud PUT para actualizar finca con ID: {}", farmId);
        try {
            Farm updatedFarm = farmService.updateFarm(farmId, farmRequest);
            return ResponseEntity.ok(new FarmResponse(updatedFarm)); // Esto sigue siendo ResponseEntity<FarmResponse>, compatible con <?>
        } catch (ResourceNotFoundException e) {
            log.warn("Finca no encontrada para actualizar con ID: {}", farmId);
            return ResponseEntity.notFound().build(); // Esto es ResponseEntity<Void>, compatible con <?>
        } catch (Exception e) { // Captura otras posibles excepciones de validación del servicio
            log.error("Error al actualizar finca {}: {}", farmId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage()); // Esto es ResponseEntity<String>, ahora compatible con <?>
        }
    }

    /**
     * Elimina una finca.
     */
    @DeleteMapping("/{farmId}")
    @PreAuthorize("hasRole('ADMIN')") // Usualmente una operación restringida
    public ResponseEntity<Void> deleteFarm(@PathVariable Integer farmId) {
        log.info("Solicitud DELETE para eliminar finca con ID: {}", farmId);
        try {
            farmService.deleteFarm(farmId);
            return ResponseEntity.noContent().build(); // 204 No Content
        } catch (ResourceNotFoundException e) {
            log.warn("Finca no encontrada para eliminar con ID: {}", farmId);
            return ResponseEntity.notFound().build();
        } catch (DeletionNotAllowedException e) { // Excepción personalizada para validaciones de borrado
            log.warn("Intento de eliminación no permitido para finca ID {}: {}", farmId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null); // O .badRequest() con mensaje
        }
    }
}