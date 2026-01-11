package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.PrecipitationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.PrecipitationResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.PrecipitationSummaryResponse;
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
@RequestMapping()
@RequiredArgsConstructor
@Slf4j
public class PrecipitationController {

    private final PrecipitationService precipitationService;

    /**
     * Registra una nueva precipitación para una finca específica.
     * * @param farmId ID de la finca.
     * @param request Datos de la precipitación (fecha, mm, notas).
     * @return La precipitación registrada.
     */
    @PostMapping("/api/farms/{farmId}/precipitations")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('REGISTRAR_PRECIPITACION')")
    public ResponseEntity<PrecipitationResponse> createPrecipitation(@PathVariable Integer farmId,
                                                                     @Valid @RequestBody PrecipitationRequest request) {
        log.info("Solicitud POST para registrar precipitación para finca ID {}", farmId);
        Precipitation newPrecipitation = precipitationService.createPrecipitation(farmId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new PrecipitationResponse(newPrecipitation));
    }

    /**
     * Obtiene todos los registros de precipitación para una finca específica.
     * * @param farmId ID de la finca.
     * @return Lista histórica de precipitaciones.
     */
    @GetMapping("/api/farms/{farmId}/precipitations")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_PRECIPITACION')")
    public ResponseEntity<List<PrecipitationResponse>> getPrecipitationsByFarm(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener precipitaciones de la finca ID {}", farmId);
        List<PrecipitationResponse> responses = precipitationService.getPrecipitationsByFarm(farmId)
                .stream()
                .map(PrecipitationResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene un registro de precipitación específico por su ID global.
     * * @param precipitationId ID del registro.
     * @return Detalle de la precipitación.
     */
    @GetMapping("/api/precipitations/{precipitationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_PRECIPITACION')")
    public ResponseEntity<PrecipitationResponse> getPrecipitationById(@PathVariable Integer precipitationId) {
        log.info("Solicitud GET para obtener precipitación ID: {}", precipitationId);
        Precipitation precipitation = precipitationService.getPrecipitationById(precipitationId);
        return ResponseEntity.ok(new PrecipitationResponse(precipitation));
    }

    /**
     * Actualiza un registro de precipitación existente.
     * * @param precipitationId ID del registro a actualizar.
     * @param request Nuevos datos.
     * @return Registro actualizado.
     */
    @PutMapping("/api/precipitations/{precipitationId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERARIO') or hasAuthority('MODIFICAR_PRECIPITACION')")
    public ResponseEntity<PrecipitationResponse> updatePrecipitation(@PathVariable Integer precipitationId,
                                                                     @Valid @RequestBody PrecipitationRequest request) {
        log.info("Solicitud PUT para actualizar precipitación ID {}", precipitationId);
        Precipitation updatedPrecipitation = precipitationService.updatePrecipitation(precipitationId, request);
        return ResponseEntity.ok(new PrecipitationResponse(updatedPrecipitation));
    }

    /**
     * Elimina un registro de precipitación.
     * * @param precipitationId ID del registro a eliminar.
     * @return 204 No Content.
     */
    @DeleteMapping("/api/precipitations/{precipitationId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('ELIMINAR_PRECIPITACION')")
    public ResponseEntity<Void> deletePrecipitation(@PathVariable Integer precipitationId) {
        log.info("Solicitud DELETE para eliminar precipitación ID: {}", precipitationId);
        precipitationService.deletePrecipitation(precipitationId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene el resumen de precipitación para una fecha específica.
     */
    @GetMapping("/api/farms/{farmId}/precipitations/summary/daily")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    public ResponseEntity<PrecipitationSummaryResponse> getDailyPrecipitation(
            @PathVariable Integer farmId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date date) {
        log.info("Solicitud GET para obtener la precipitación diaria de la finca ID {} para la fecha {}", farmId, date);
        PrecipitationSummaryResponse summary = precipitationService.getDailyPrecipitation(farmId, date);
        return ResponseEntity.ok(summary);
    }

    /**
     * Obtiene el resumen de precipitación mensual.
     */
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

    /**
     * Obtiene el resumen de precipitación anual (generalmente para el año hidrológico o calendario).
     */
    @GetMapping("/api/farms/{farmId}/precipitations/summary/annual")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    public ResponseEntity<PrecipitationSummaryResponse> getAnnualPrecipitation(
            @PathVariable Integer farmId,
            @RequestParam int year) {
        log.info("Solicitud GET para obtener la precipitación anual de la finca ID {} para el año {}", farmId, year);
        PrecipitationSummaryResponse summary = precipitationService.getAnnualPrecipitation(farmId, year);
        return ResponseEntity.ok(summary);
    }
}