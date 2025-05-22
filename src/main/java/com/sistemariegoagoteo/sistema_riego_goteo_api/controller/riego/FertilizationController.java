package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FertilizationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FertilizationResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Fertilization;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.FertilizationService;
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
public class FertilizationController {

    private final FertilizationService fertilizationService;

    /**
     * Registra una nueva fertilización para un sector específico de una finca.
     */
    @PostMapping("/api/farms/{farmId}/sectors/{sectorId}/fertilizations")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('REGISTRAR_FERTILIZACION')")
    public ResponseEntity<?> createFertilization(@PathVariable Integer farmId,
                                                 @PathVariable Integer sectorId,
                                                 @Valid @RequestBody FertilizationRequest request) {
        log.info("Solicitud POST para registrar fertilización para sector ID {} en finca ID {}", sectorId, farmId);
        try {
            Fertilization newFertilization = fertilizationService.createFertilization(farmId, sectorId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new FertilizationResponse(newFertilization));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo registrar fertilización, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al registrar fertilización: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Obtiene todos los registros de fertilización para un sector específico de una finca.
     */
    @GetMapping("/api/farms/{farmId}/sectors/{sectorId}/fertilizations")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_FERTILIZACION')")
    public ResponseEntity<?> getFertilizationsBySector(@PathVariable Integer farmId, @PathVariable Integer sectorId) {
        log.info("Solicitud GET para obtener fertilizaciones del sector ID {} en finca ID {}", sectorId, farmId);
        try {
            List<FertilizationResponse> responses = fertilizationService.getFertilizationsBySector(farmId, sectorId)
                    .stream()
                    .map(FertilizationResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Obtiene un registro de fertilización específico por su ID global.
     */
    @GetMapping("/api/fertilizations/{fertilizationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_FERTILIZACION')")
    public ResponseEntity<?> getFertilizationById(@PathVariable Integer fertilizationId) {
        log.info("Solicitud GET para obtener fertilización ID: {}", fertilizationId);
        try {
            Fertilization fertilization = fertilizationService.getFertilizationById(fertilizationId);
            // Podrías añadir validación para asegurar que el usuario tiene acceso a la finca/sector de esta fertilización
            return ResponseEntity.ok(new FertilizationResponse(fertilization));
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Actualiza un registro de fertilización existente.
     */
    @PutMapping("/api/fertilizations/{fertilizationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('MODIFICAR_FERTILIZACION')")
    public ResponseEntity<?> updateFertilization(@PathVariable Integer fertilizationId,
                                                 @Valid @RequestBody FertilizationRequest request) {
        log.info("Solicitud PUT para actualizar fertilización ID {}", fertilizationId);
        try {
            Fertilization updatedFertilization = fertilizationService.updateFertilization(fertilizationId, request);
            return ResponseEntity.ok(new FertilizationResponse(updatedFertilization));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo actualizar fertilización, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al actualizar fertilización: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina un registro de fertilización.
     */
    @DeleteMapping("/api/fertilizations/{fertilizationId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ELIMINAR_FERTILIZACION')")
    public ResponseEntity<?> deleteFertilization(@PathVariable Integer fertilizationId) {
        log.info("Solicitud DELETE para eliminar fertilización ID: {}", fertilizationId);
        try {
            fertilizationService.deleteFertilization(fertilizationId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo eliminar fertilización, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}