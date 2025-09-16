package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.PrecipitationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.PrecipitationResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.PrecipitationSummaryResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Precipitation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.PrecipitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping() // Ruta base flexible, definida a nivel de método
@RequiredArgsConstructor
@Slf4j
public class PrecipitationController {

    private final PrecipitationService precipitationService;

    /**
     * Registra una nueva precipitación para una finca específica.
     */
    @PostMapping("/api/farms/{farmId}/precipitations")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('REGISTRAR_PRECIPITACION')")
    public ResponseEntity<?> createPrecipitation(@PathVariable Integer farmId,
                                                 @Valid @RequestBody PrecipitationRequest request) {
        log.info("Solicitud POST para registrar precipitación para finca ID {}", farmId);
        try {
            Precipitation newPrecipitation = precipitationService.createPrecipitation(farmId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(new PrecipitationResponse(newPrecipitation));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo registrar precipitación, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al registrar precipitación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Obtiene todos los registros de precipitación para una finca específica.
     */
    @GetMapping("/api/farms/{farmId}/precipitations")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_PRECIPITACION')")
    public ResponseEntity<?> getPrecipitationsByFarm(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener precipitaciones de la finca ID {}", farmId);
        try {
            List<PrecipitationResponse> responses = precipitationService.getPrecipitationsByFarm(farmId)
                    .stream()
                    .map(PrecipitationResponse::new)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(responses);
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Obtiene un registro de precipitación específico por su ID global.
     */
    @GetMapping("/api/precipitations/{precipitationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_PRECIPITACION')")
    public ResponseEntity<?> getPrecipitationById(@PathVariable Integer precipitationId) {
        log.info("Solicitud GET para obtener precipitación ID: {}", precipitationId);
        try {
            Precipitation precipitation = precipitationService.getPrecipitationById(precipitationId);
            return ResponseEntity.ok(new PrecipitationResponse(precipitation));
        } catch (ResourceNotFoundException e) {
            log.warn("Recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Actualiza un registro de precipitación existente.
     */
    @PutMapping("/api/precipitations/{precipitationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('MODIFICAR_PRECIPITACION')")
    public ResponseEntity<?> updatePrecipitation(@PathVariable Integer precipitationId,
                                                 @Valid @RequestBody PrecipitationRequest request) {
        log.info("Solicitud PUT para actualizar precipitación ID {}", precipitationId);
        try {
            Precipitation updatedPrecipitation = precipitationService.updatePrecipitation(precipitationId, request);
            return ResponseEntity.ok(new PrecipitationResponse(updatedPrecipitation));
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo actualizar precipitación, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Argumento inválido al actualizar precipitación: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Elimina un registro de precipitación.
     */
    @DeleteMapping("/api/precipitations/{precipitationId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ELIMINAR_PRECIPITACION')")
    public ResponseEntity<?> deletePrecipitation(@PathVariable Integer precipitationId) {
        log.info("Solicitud DELETE para eliminar precipitación ID: {}", precipitationId);
        try {
            precipitationService.deletePrecipitation(precipitationId);
            return ResponseEntity.noContent().build();
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo eliminar precipitación, recurso no encontrado: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }


    @GetMapping("/api/farms/{farmId}/precipitations/summary/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    public ResponseEntity<PrecipitationSummaryResponse> getDailyPrecipitation(
            @PathVariable Integer farmId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date date) {
        log.info("Solicitud GET para obtener la precipitación diaria de la finca ID {} para la fecha {}", farmId, date);
        PrecipitationSummaryResponse summary = precipitationService.getDailyPrecipitation(farmId, date);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/api/farms/{farmId}/precipitations/summary/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    public ResponseEntity<PrecipitationSummaryResponse> getMonthlyPrecipitation(
            @PathVariable Integer farmId,
            @RequestParam int year,
            @RequestParam int month) {
        log.info("Solicitud GET para obtener la precipitación mensual de la finca ID {} para el mes {}-{}", farmId, year, month);
        PrecipitationSummaryResponse summary = precipitationService.getMonthlyPrecipitation(farmId, year, month);
        return ResponseEntity.ok(summary);
    }

    @GetMapping("/api/farms/{farmId}/precipitations/summary/annual")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    public ResponseEntity<PrecipitationSummaryResponse> getAnnualPrecipitation(
            @PathVariable Integer farmId,
            @RequestParam int year) {
        log.info("Solicitud GET para obtener la precipitación anual de la finca ID {} para el año que comienza en mayo de {}", farmId, year);
        PrecipitationSummaryResponse summary = precipitationService.getAnnualPrecipitation(farmId, year);
        return ResponseEntity.ok(summary);
    }

}