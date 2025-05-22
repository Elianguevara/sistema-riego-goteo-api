package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.ReservoirTurnRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.ReservoirTurnResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.ReservoirTurn;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.ReservoirTurnService;
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
public class ReservoirTurnController {

    private final ReservoirTurnService reservoirTurnService;

    /**
     * Crea un nuevo turno de embalse para una fuente de agua específica de una finca.
     */
    @PostMapping("/api/farms/{farmId}/watersources/{waterSourceId}/reservoirturns")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_TURNOS_EMBALSE')")
    public ResponseEntity<?> createReservoirTurn(@PathVariable Integer farmId,
                                                 @PathVariable Integer waterSourceId,
                                                 @Valid @RequestBody ReservoirTurnRequest request) {
        log.info("Solicitud POST para crear turno de embalse para fuente ID {} en finca ID {}", waterSourceId, farmId);
        try {
            ReservoirTurn newTurn = reservoirTurnService.createReservoirTurn(farmId, waterSourceId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ReservoirTurnResponse(newTurn));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo crear turno, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al crear turno: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Obtiene todos los turnos de embalse para una fuente de agua específica de una finca.
     */
    @GetMapping("/api/farms/{farmId}/watersources/{waterSourceId}/reservoirturns")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_TURNOS_EMBALSE')")
    public ResponseEntity<?> getReservoirTurnsByWaterSource(@PathVariable Integer farmId, @PathVariable Integer waterSourceId) {
        log.info("Solicitud GET para obtener turnos de embalse para fuente ID {} en finca ID {}", waterSourceId, farmId);
        try {
            List<ReservoirTurnResponse> responses = reservoirTurnService.getReservoirTurnsByWaterSource(farmId, waterSourceId)
                    .stream()
                    .map(ReservoirTurnResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Obtiene un turno de embalse específico por su ID global.
     */
    @GetMapping("/api/reservoirturns/{turnId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_TURNOS_EMBALSE')")
    public ResponseEntity<?> getReservoirTurnById(@PathVariable Integer turnId) {
        log.info("Solicitud GET para obtener turno de embalse ID: {}", turnId);
        try {
            ReservoirTurn turn = reservoirTurnService.getReservoirTurnById(turnId);
            return ResponseEntity.ok(new ReservoirTurnResponse(turn));
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Actualiza un turno de embalse existente.
     */
    @PutMapping("/api/reservoirturns/{turnId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_TURNOS_EMBALSE')")
    public ResponseEntity<?> updateReservoirTurn(@PathVariable Integer turnId,
                                                 @Valid @RequestBody ReservoirTurnRequest request) {
        log.info("Solicitud PUT para actualizar turno de embalse ID {}", turnId);
        try {
            ReservoirTurn updatedTurn = reservoirTurnService.updateReservoirTurn(turnId, request);
            return ResponseEntity.ok(new ReservoirTurnResponse(updatedTurn));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo actualizar turno, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al actualizar turno: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina un turno de embalse.
     */
    @DeleteMapping("/api/reservoirturns/{turnId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_TURNOS_EMBALSE')")
    public ResponseEntity<?> deleteReservoirTurn(@PathVariable Integer turnId) {
        log.info("Solicitud DELETE para eliminar turno de embalse ID: {}", turnId);
        try {
            reservoirTurnService.deleteReservoirTurn(turnId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo eliminar turno, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }
}