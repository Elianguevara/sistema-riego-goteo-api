package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationResponse;
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

@RestController
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
    public ResponseEntity<IrrigationResponse> createIrrigation(@Valid @RequestBody IrrigationRequest request) {
        log.info("Solicitud POST para crear un nuevo registro de riego para el sector ID {}", request.getSectorId());
        Irrigation newIrrigation = irrigationService.createIrrigation(request);
        return new ResponseEntity<>(new IrrigationResponse(newIrrigation), HttpStatus.CREATED);
    }
    // --- FIN DEL MÉTODO AÑADIDO ---

    @GetMapping("/api/farms/{farmId}/irrigations/monthly-view")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    public ResponseEntity<List<SectorMonthlyIrrigationDTO>> getMonthlyIrrigationView(
            @PathVariable Integer farmId,
            @RequestParam int year,
            @RequestParam int month) {
        log.info("Solicitud GET para la vista mensual de riegos en la finca ID {} para {}-{}", farmId, year, month);
        List<SectorMonthlyIrrigationDTO> monthlyData = irrigationService.getMonthlyIrrigationData(farmId, year, month);
        return ResponseEntity.ok(monthlyData);
    }

    @GetMapping("/api/farms/{farmId}/sectors/{sectorId}/irrigations")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_RIEGO')")
    public ResponseEntity<List<IrrigationResponse>> getIrrigationsBySector(@PathVariable Integer farmId,
            @PathVariable Integer sectorId) {
        log.info("Solicitud GET para obtener riegos del sector ID {} en finca ID {}", sectorId, farmId);
        List<IrrigationResponse> responses = irrigationService.getIrrigationsBySector(farmId, sectorId).stream()
                .map(IrrigationResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/api/irrigations/{irrigationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_RIEGO')")
    public ResponseEntity<IrrigationResponse> getIrrigationById(@PathVariable Integer irrigationId) {
        log.info("Solicitud GET para obtener riego ID: {}", irrigationId);
        Irrigation irrigation = irrigationService.getIrrigationById(irrigationId);
        return ResponseEntity.ok(new IrrigationResponse(irrigation));
    }

    @PutMapping("/api/irrigations/{irrigationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('MODIFICAR_RIEGO')")
    public ResponseEntity<IrrigationResponse> updateIrrigation(@PathVariable Integer irrigationId,
            @Valid @RequestBody IrrigationRequest request) {
        log.info("Solicitud PUT para actualizar riego ID {}: inicio {}, fin {}", irrigationId,
                request.getStartDateTime(), request.getEndDateTime());
        Irrigation updatedIrrigation = irrigationService.updateIrrigation(irrigationId, request);
        return ResponseEntity.ok(new IrrigationResponse(updatedIrrigation));
    }

    @DeleteMapping("/api/irrigations/{irrigationId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ELIMINAR_RIEGO')")
    public ResponseEntity<Void> deleteIrrigation(@PathVariable Integer irrigationId) {
        log.info("Solicitud DELETE para eliminar riego ID: {}", irrigationId);
        irrigationService.deleteIrrigation(irrigationId);
        return ResponseEntity.noContent().build();
    }
}