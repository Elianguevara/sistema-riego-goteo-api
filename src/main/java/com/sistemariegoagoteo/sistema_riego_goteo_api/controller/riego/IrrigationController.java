package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.IrrigationService;
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
@RequestMapping() // La ruta base se definirá a nivel de método para más flexibilidad
@RequiredArgsConstructor
@Slf4j
public class IrrigationController {

    private final IrrigationService irrigationService;

    /**
     * Registra un nuevo evento de irrigación para un sector específico.
     */
    @PostMapping("/api/farms/{farmId}/sectors/{sectorId}/irrigations")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('REGISTRAR_RIEGO')")
    public ResponseEntity<?> logIrrigation(@PathVariable Integer farmId,
                                           @PathVariable Integer sectorId,
                                           @Valid @RequestBody IrrigationRequest request) {
        log.info("Solicitud POST para registrar riego en finca ID {}, sector ID {}: inicio {}", farmId, sectorId, request.getStartDatetime());
        try {
            Irrigation newIrrigation = irrigationService.logIrrigation(farmId, sectorId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new IrrigationResponse(newIrrigation));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo registrar riego, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al registrar riego: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Obtiene todos los registros de irrigación para un sector específico.
     */
    @GetMapping("/api/farms/{farmId}/sectors/{sectorId}/irrigations")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_RIEGO')")
    public ResponseEntity<?> getIrrigationsBySector(@PathVariable Integer farmId, @PathVariable Integer sectorId) {
        log.info("Solicitud GET para obtener riegos del sector ID {} en finca ID {}", sectorId, farmId);
        try {
            List<IrrigationResponse> responses = irrigationService.getIrrigationsBySector(farmId, sectorId).stream()
                    .map(IrrigationResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Obtiene un registro de irrigación específico por su ID.
     * No está anidado ya que el ID del riego es único globalmente.
     */
    @GetMapping("/api/irrigations/{irrigationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_RIEGO')")
    public ResponseEntity<?> getIrrigationById(@PathVariable Integer irrigationId) {
        log.info("Solicitud GET para obtener riego ID: {}", irrigationId);
        try {
            Irrigation irrigation = irrigationService.getIrrigationById(irrigationId);
            // Se podría añadir una validación de seguridad aquí para asegurar que el usuario
            // tiene permisos sobre la finca/sector de este riego, si fuera necesario.
            return ResponseEntity.ok(new IrrigationResponse(irrigation));
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Actualiza un registro de irrigación existente.
     */
    @PutMapping("/api/irrigations/{irrigationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('MODIFICAR_RIEGO')")
    public ResponseEntity<?> updateIrrigation(@PathVariable Integer irrigationId,
                                              @Valid @RequestBody IrrigationRequest request) {
        log.info("Solicitud PUT para actualizar riego ID {}: inicio {}, fin {}", irrigationId, request.getStartDatetime(), request.getEndDatetime());
        try {
            Irrigation updatedIrrigation = irrigationService.updateIrrigation(irrigationId, request);
            return ResponseEntity.ok(new IrrigationResponse(updatedIrrigation));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo actualizar riego, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al actualizar riego: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina un registro de irrigación.
     */
    @DeleteMapping("/api/irrigations/{irrigationId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ELIMINAR_RIEGO')")
    public ResponseEntity<?> deleteIrrigation(@PathVariable Integer irrigationId) {
        log.info("Solicitud DELETE para eliminar riego ID: {}", irrigationId);
        try {
            irrigationService.deleteIrrigation(irrigationId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo eliminar riego, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}