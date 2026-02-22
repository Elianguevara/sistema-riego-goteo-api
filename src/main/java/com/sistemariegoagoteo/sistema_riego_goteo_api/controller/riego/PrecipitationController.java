package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.PrecipitationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.PrecipitationResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.PrecipitationSummaryResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Precipitation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.PrecipitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Precipitation", description = "API para la gestión de registros de lluvia")
public class PrecipitationController {

    private final PrecipitationService precipitationService;

    @PostMapping("/farms/{farmId}/precipitations")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('REGISTRAR_PRECIPITACION')")
    @Operation(summary = "Registrar nueva precipitación")
    public ResponseEntity<PrecipitationResponse> createPrecipitation(
            @PathVariable Integer farmId,
            @Valid @RequestBody PrecipitationRequest request) {
        log.info("Solicitud POST para registrar precipitación para finca ID {}", farmId);
        Precipitation precipitation = precipitationService.createPrecipitation(farmId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new PrecipitationResponse(precipitation));
    }

    @GetMapping("/farms/{farmId}/precipitations")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_PRECIPITACION')")
    @Operation(summary = "Obtener todas las precipitaciones de una finca")
    public ResponseEntity<List<PrecipitationResponse>> getPrecipitationsByFarm(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener precipitaciones de la finca ID {}", farmId);
        List<PrecipitationResponse> response = precipitationService.getPrecipitationsByFarm(farmId).stream()
                .map(PrecipitationResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/precipitations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_PRECIPITACION')")
    @Operation(summary = "Obtener detalle de una precipitación")
    public ResponseEntity<PrecipitationResponse> getPrecipitationById(@PathVariable Integer id) {
        log.info("Solicitud GET para obtener precipitación ID: {}", id);
        Precipitation precipitation = precipitationService.getPrecipitationById(id);
        return ResponseEntity.ok(new PrecipitationResponse(precipitation));
    }

    @PutMapping("/precipitations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('MODIFICAR_PRECIPITACION')")
    @Operation(summary = "Actualizar precipitación existente")
    public ResponseEntity<PrecipitationResponse> updatePrecipitation(
            @PathVariable Integer id,
            @Valid @RequestBody PrecipitationRequest request) {
        log.info("Solicitud PUT para actualizar precipitación ID {}", id);
        Precipitation precipitation = precipitationService.updatePrecipitation(id, request);
        return ResponseEntity.ok(new PrecipitationResponse(precipitation));
    }

    @DeleteMapping("/precipitations/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ELIMINAR_PRECIPITACION')")
    @Operation(summary = "Eliminar registro de precipitación")
    public ResponseEntity<Void> deletePrecipitation(@PathVariable Integer id) {
        log.info("Solicitud DELETE para eliminar precipitación ID: {}", id);
        precipitationService.deletePrecipitation(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/farms/{farmId}/precipitations/summary/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    @Operation(summary = "Obtener resumen de lluvia diaria")
    public ResponseEntity<PrecipitationSummaryResponse> getDailyPrecipitation(
            @PathVariable Integer farmId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("Solicitud GET para obtener la precipitación diaria de la finca ID {} para la fecha {}", farmId, date);
        return ResponseEntity.ok(precipitationService.getDailyPrecipitation(farmId, date));
    }

    @GetMapping("/farms/{farmId}/precipitations/summary/monthly")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    @Operation(summary = "Obtener resumen de lluvia mensual")
    public ResponseEntity<PrecipitationSummaryResponse> getMonthlyPrecipitation(
            @PathVariable Integer farmId,
            @RequestParam int year,
            @RequestParam int month) {
        log.info("Solicitud GET para obtener la precipitación mensual de la finca ID {} para el mes {}-{}", farmId,
                year, month);
        return ResponseEntity.ok(precipitationService.getMonthlyPrecipitation(farmId, year, month));
    }

    @GetMapping("/farms/{farmId}/precipitations/summary/annual")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    @Operation(summary = "Obtener resumen de lluvia anual (año agrícola)")
    public ResponseEntity<PrecipitationSummaryResponse> getAnnualPrecipitation(
            @PathVariable Integer farmId,
            @RequestParam int year) {
        log.info("Solicitud GET para obtener la precipitación anual de la finca ID {} para el año {}", farmId, year);
        return ResponseEntity.ok(precipitationService.getAnnualPrecipitation(farmId, year));
    }
}