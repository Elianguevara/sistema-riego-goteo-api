package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.SectorRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.SectorResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.SectorService;
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
@RequestMapping("/api/farms/{farmId}/sectors") // Rutas anidadas bajo una finca específica
@RequiredArgsConstructor
@Slf4j
public class SectorController {

    private final SectorService sectorService;

    /**
     * Crea un nuevo sector para una finca específica.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_SECTORES')") // Ajusta los permisos
    public ResponseEntity<?> createSector(@PathVariable Integer farmId,
                                          @Valid @RequestBody SectorRequest sectorRequest) {
        log.info("Solicitud POST para crear sector en finca ID {}: {}", farmId, sectorRequest.getName());
        try {
            Sector newSector = sectorService.createSector(farmId, sectorRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(new SectorResponse(newSector));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo crear sector, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al crear sector: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Obtiene todos los sectores de una finca específica.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_SECTORES')")
    public ResponseEntity<?> getSectorsByFarmId(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener sectores de la finca ID: {}", farmId);
        try {
            List<SectorResponse> sectorResponses = sectorService.getSectorsByFarmId(farmId).stream()
                    .map(SectorResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(sectorResponses);
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo obtener sectores, finca no encontrada: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Obtiene un sector específico de una finca.
     */
    @GetMapping("/{sectorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_SECTORES')")
    public ResponseEntity<?> getSectorById(@PathVariable Integer farmId, @PathVariable Integer sectorId) {
        log.info("Solicitud GET para obtener sector ID {} de la finca ID: {}", sectorId, farmId);
        try {
            Sector sector = sectorService.getSectorByIdAndFarmId(farmId, sectorId);
            return ResponseEntity.ok(new SectorResponse(sector));
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Actualiza un sector existente.
     */
    @PutMapping("/{sectorId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_SECTORES')")
    public ResponseEntity<?> updateSector(@PathVariable Integer farmId,
                                          @PathVariable Integer sectorId,
                                          @Valid @RequestBody SectorRequest sectorRequest) {
        log.info("Solicitud PUT para actualizar sector ID {} en finca ID {}: {}", sectorId, farmId, sectorRequest.getName());
        try {
            Sector updatedSector = sectorService.updateSector(farmId, sectorId, sectorRequest);
            return ResponseEntity.ok(new SectorResponse(updatedSector));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo actualizar sector, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al actualizar sector: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina un sector de una finca.
     */
    @DeleteMapping("/{sectorId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_SECTORES')")
    public ResponseEntity<?> deleteSector(@PathVariable Integer farmId, @PathVariable Integer sectorId) {
        log.info("Solicitud DELETE para eliminar sector ID {} de la finca ID: {}", sectorId, farmId);
        try {
            sectorService.deleteSector(farmId, sectorId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo eliminar sector, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
        // Podrías añadir catch para DeletionNotAllowedException si lo implementas en el servicio
    }
}