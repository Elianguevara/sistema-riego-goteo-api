package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.analytics;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.analytics.IrrigationRecordDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.analytics.IrrigationSectorSummaryDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.analytics.IrrigationTimeseriesDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.analytics.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * Obtiene un resumen consolidado de riego agrupado por sectores.
     *
     * @param farmId ID de la finca.
     * @param startDate Fecha de inicio (Date).
     * @param endDate Fecha de fin (Date).
     * @param sectorIds Lista opcional de IDs de sectores para filtrar.
     * @return Lista con el resumen por sector.
     */
    @GetMapping("/irrigation/summary")
    public ResponseEntity<List<IrrigationSectorSummaryDTO>> getIrrigationSummary(
            @RequestParam Integer farmId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
            @RequestParam(required = false) List<Integer> sectorIds) {

        List<IrrigationSectorSummaryDTO> summary = analyticsService.getIrrigationSummary(farmId, startDate, endDate, sectorIds);
        return ResponseEntity.ok(summary);
    }

    /**
     * Obtiene datos de series temporales de riego para gráficos.
     *
     * @param sectorId ID del sector.
     * @param startDate Fecha de inicio (LocalDate).
     * @param endDate Fecha de fin (LocalDate).
     * @return Lista de puntos de datos temporales.
     */
    @GetMapping("/irrigation/timeseries")
    public ResponseEntity<List<IrrigationTimeseriesDTO>> getIrrigationTimeSeries(
            @RequestParam Integer sectorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<IrrigationTimeseriesDTO> timeSeries = analyticsService.getIrrigationTimeseries(sectorId, startDate, endDate);
        return ResponseEntity.ok(timeSeries);
    }

    /**
     * Obtiene un listado paginado y detallado de los registros de riego.
     *
     * @param farmId ID de la finca.
     * @param startDate Fecha de inicio.
     * @param endDate Fecha de fin.
     * @param sectorIds Lista opcional de sectores.
     * @param pageable Configuración de paginación.
     * @return Página de registros detallados.
     */
    @GetMapping("/irrigation/records")
    public ResponseEntity<Page<IrrigationRecordDTO>> getIrrigationRecords(
            @RequestParam Integer farmId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate,
            @RequestParam(required = false) List<Integer> sectorIds,
            Pageable pageable) {

        Page<IrrigationRecordDTO> records = analyticsService.getIrrigationRecords(farmId, startDate, endDate, sectorIds, pageable);
        return ResponseEntity.ok(records);
    }
}