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
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.calendar.SectorMonthlyIrrigationDTO;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping()
@RequiredArgsConstructor
@Slf4j
public class IrrigationController {

    private final IrrigationService irrigationService;

    // --- MÉTODO AÑADIDO PARA LA CREACIÓN DE RIEGOS ---
    /**
     * Crea un nuevo registro de irrigación.
     * Permitido para Analistas y Operarios.
     */
    @PostMapping("/api/irrigation")
    @PreAuthorize("hasAnyRole('ANALISTA', 'OPERARIO') or hasAuthority('CREAR_RIEGO')")
    public ResponseEntity<?> createIrrigation(@Valid @RequestBody IrrigationRequest request) {
        log.info("Solicitud POST para crear un nuevo registro de riego para el sector ID {}", request.getSectorId());
        try {
            Irrigation newIrrigation = irrigationService.createIrrigation(request);
            return new ResponseEntity<>(new IrrigationResponse(newIrrigation), HttpStatus.CREATED);
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo crear el riego, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al crear riego: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // --- FIN DEL MÉTODO AÑADIDO ---

    @GetMapping("/api/farms/{farmId}/irrigations/monthly-view")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    public ResponseEntity<List<SectorMonthlyIrrigationDTO>> getMonthlyIrrigationView(
            @PathVariable Integer farmId,
            @RequestParam int year,
            @RequestParam int month) {
        log.info("Solicitud GET para la vista mensual de riegos en la finca ID {} para {}-{}", farmId, year, month);
        // ... (resto del método sin cambios)
        try {
            List<SectorMonthlyIrrigationDTO> monthlyData = irrigationService.getMonthlyIrrigationData(farmId, year, month);
            return ResponseEntity.ok(monthlyData);
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado al generar vista mensual: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @GetMapping("/api/farms/{farmId}/sectors/{sectorId}/irrigations")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_RIEGO')")
    public ResponseEntity<?> getIrrigationsBySector(@PathVariable Integer farmId, @PathVariable Integer sectorId) {
        // ... (resto del método sin cambios)
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

    @GetMapping("/api/irrigations/{irrigationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_RIEGO')")
    public ResponseEntity<?> getIrrigationById(@PathVariable Integer irrigationId) {
        // ... (resto del método sin cambios)
        log.info("Solicitud GET para obtener riego ID: {}", irrigationId);
        try {
            Irrigation irrigation = irrigationService.getIrrigationById(irrigationId);
            return ResponseEntity.ok(new IrrigationResponse(irrigation));
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/api/irrigations/{irrigationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('MODIFICAR_RIEGO')")
    public ResponseEntity<?> updateIrrigation(@PathVariable Integer irrigationId,
                                              @Valid @RequestBody IrrigationRequest request) {
        // ... (resto del método sin cambios)
        log.info("Solicitud PUT para actualizar riego ID {}: inicio {}, fin {}", irrigationId, request.getStartDateTime(), request.getEndDateTime());
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

    @DeleteMapping("/api/irrigations/{irrigationId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ELIMINAR_RIEGO')")
    public ResponseEntity<?> deleteIrrigation(@PathVariable Integer irrigationId) {
        // ... (resto del método sin cambios)
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