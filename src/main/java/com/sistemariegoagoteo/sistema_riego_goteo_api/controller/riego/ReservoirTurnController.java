package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.ReservoirTurnRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.ReservoirTurnResponse;
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
@RequestMapping()
@RequiredArgsConstructor
@Slf4j
public class ReservoirTurnController {

    private final ReservoirTurnService reservoirTurnService;

    /**
     * Crea un nuevo turno de embalse para una fuente de agua específica de una finca.
     * * @param farmId ID de la finca.
     * @param waterSourceId ID de la fuente de agua (embalse).
     * @param request Datos del turno.
     * @return El turno creado.
     */
    @PostMapping("/api/farms/{farmId}/watersources/{waterSourceId}/reservoirturns")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_TURNOS_EMBALSE')")
    public ResponseEntity<ReservoirTurnResponse> createReservoirTurn(@PathVariable Integer farmId,
                                                                     @PathVariable Integer waterSourceId,
                                                                     @Valid @RequestBody ReservoirTurnRequest request) {
        log.info("Solicitud POST para crear turno de embalse para fuente ID {} en finca ID {}", waterSourceId, farmId);
        ReservoirTurn newTurn = reservoirTurnService.createReservoirTurn(farmId, waterSourceId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new ReservoirTurnResponse(newTurn));
    }

    /**
     * Obtiene todos los turnos de embalse para una fuente de agua específica de una finca.
     * * @param farmId ID de la finca.
     * @param waterSourceId ID de la fuente de agua.
     * @return Lista de turnos asignados a esa fuente.
     */
    @GetMapping("/api/farms/{farmId}/watersources/{waterSourceId}/reservoirturns")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_TURNOS_EMBALSE')")
    public ResponseEntity<List<ReservoirTurnResponse>> getReservoirTurnsByWaterSource(@PathVariable Integer farmId, @PathVariable Integer waterSourceId) {
        log.info("Solicitud GET para obtener turnos de embalse para fuente ID {} en finca ID {}", waterSourceId, farmId);
        List<ReservoirTurnResponse> responses = reservoirTurnService.getReservoirTurnsByWaterSource(farmId, waterSourceId)
                .stream()
                .map(ReservoirTurnResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene un turno de embalse específico por su ID global.
     * * @param turnId ID del turno.
     * @return Detalle del turno.
     */
    @GetMapping("/api/reservoirturns/{turnId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_TURNOS_EMBALSE')")
    public ResponseEntity<ReservoirTurnResponse> getReservoirTurnById(@PathVariable Integer turnId) {
        log.info("Solicitud GET para obtener turno de embalse ID: {}", turnId);
        ReservoirTurn turn = reservoirTurnService.getReservoirTurnById(turnId);
        return ResponseEntity.ok(new ReservoirTurnResponse(turn));
    }

    /**
     * Actualiza un turno de embalse existente.
     * * @param turnId ID del turno a actualizar.
     * @param request Nuevos datos del turno.
     * @return Turno actualizado.
     */
    @PutMapping("/api/reservoirturns/{turnId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_TURNOS_EMBALSE')")
    public ResponseEntity<ReservoirTurnResponse> updateReservoirTurn(@PathVariable Integer turnId,
                                                                     @Valid @RequestBody ReservoirTurnRequest request) {
        log.info("Solicitud PUT para actualizar turno de embalse ID {}", turnId);
        ReservoirTurn updatedTurn = reservoirTurnService.updateReservoirTurn(turnId, request);
        return ResponseEntity.ok(new ReservoirTurnResponse(updatedTurn));
    }

    /**
     * Elimina un turno de embalse.
     * * @param turnId ID del turno a eliminar.
     * @return 204 No Content.
     */
    @DeleteMapping("/api/reservoirturns/{turnId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_TURNOS_EMBALSE')")
    public ResponseEntity<Void> deleteReservoirTurn(@PathVariable Integer turnId) {
        log.info("Solicitud DELETE para eliminar turno de embalse ID: {}", turnId);
        reservoirTurnService.deleteReservoirTurn(turnId);
        return ResponseEntity.noContent().build();
    }
}