package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FertilizationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FertilizationResponse;
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
@RequestMapping("/api/fertilization")
@RequiredArgsConstructor
@Slf4j
public class FertilizationController {

    private final FertilizationService fertilizationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ANALISTA', 'OPERARIO') or hasAuthority('CREAR_FERTILIZACION')")
    public ResponseEntity<FertilizationResponse> createFertilization(@Valid @RequestBody FertilizationRequest request) {
        log.info("Solicitud POST para crear una nueva fertilización en el sector ID {}", request.getSectorId());
        Fertilization newFertilization = fertilizationService.createFertilization(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new FertilizationResponse(newFertilization));
    }

    @GetMapping("/sector/{sectorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    public ResponseEntity<List<FertilizationResponse>> getFertilizationsBySector(@PathVariable Integer sectorId) {
        log.info("Solicitud GET para obtener fertilizaciones del sector ID {}", sectorId);
        List<FertilizationResponse> responses = fertilizationService.getFertilizationsBySector(sectorId).stream()
                .map(FertilizationResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{fertilizationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('MODIFICAR_FERTILIZACION')")
    public ResponseEntity<FertilizationResponse> updateFertilization(@PathVariable Integer fertilizationId,
                                                                     @Valid @RequestBody FertilizationRequest request) {
        log.info("Solicitud PUT para actualizar fertilización ID {}", fertilizationId);
        Fertilization updatedFertilization = fertilizationService.updateFertilization(fertilizationId, request);
        return ResponseEntity.ok(new FertilizationResponse(updatedFertilization));
    }

    @DeleteMapping("/{fertilizationId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ELIMINAR_FERTILIZACION')")
    public ResponseEntity<Void> deleteFertilization(@PathVariable Integer fertilizationId) {
        log.info("Solicitud DELETE para eliminar fertilización ID: {}", fertilizationId);
        fertilizationService.deleteFertilization(fertilizationId);
        return ResponseEntity.noContent().build();
    }
}