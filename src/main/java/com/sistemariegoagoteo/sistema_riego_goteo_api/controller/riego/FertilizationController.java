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
@RequestMapping("/api/fertilization")
@RequiredArgsConstructor
@Slf4j
public class FertilizationController {

    private final FertilizationService fertilizationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ANALISTA', 'OPERARIO') or hasAuthority('CREAR_FERTILIZACION')")
    public ResponseEntity<?> createFertilization(@Valid @RequestBody FertilizationRequest request) {
        // ... (sin cambios aquí)
        log.info("Solicitud POST para crear una nueva fertilización en el sector ID {}", request.getSectorId());
        try {
            Fertilization newFertilization = fertilizationService.createFertilization(request);
            return new ResponseEntity<>(new FertilizationResponse(newFertilization), HttpStatus.CREATED);
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo crear la fertilización, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/sector/{sectorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    public ResponseEntity<?> getFertilizationsBySector(@PathVariable Integer sectorId) {
        log.info("Solicitud GET para obtener fertilizaciones del sector ID {}", sectorId);
        try {
            // --- LLAMADA AL SERVICIO CORREGIDA (solo 1 argumento) ---
            List<FertilizationResponse> responses = fertilizationService.getFertilizationsBySector(sectorId).stream()
                    .map(FertilizationResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PutMapping("/{fertilizationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('MODIFICAR_FERTILIZACION')")
    public ResponseEntity<?> updateFertilization(@PathVariable Integer fertilizationId,
                                                 @Valid @RequestBody FertilizationRequest request) {
        // ... (sin cambios aquí)
        log.info("Solicitud PUT para actualizar fertilización ID {}", fertilizationId);
        try {
            Fertilization updatedFertilization = fertilizationService.updateFertilization(fertilizationId, request);
            return ResponseEntity.ok(new FertilizationResponse(updatedFertilization));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo actualizar fertilización, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @DeleteMapping("/{fertilizationId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ELIMINAR_FERTILIZACION')")
    public ResponseEntity<?> deleteFertilization(@PathVariable Integer fertilizationId) {
        // ... (sin cambios aquí)
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