package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.WaterSourceRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.WaterSourceResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.WaterSource;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.WaterSourceService;
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
@RequestMapping() // Ruta base flexible, los métodos definen la ruta completa
@RequiredArgsConstructor
@Slf4j
public class WaterSourceController {

    private final WaterSourceService waterSourceService;

    /**
     * Crea una nueva fuente de agua (pozo, canal, etc.) para una finca específica.
     *
     * @param farmId ID de la finca.
     * @param request Datos de la fuente de agua.
     * @return La fuente de agua creada.
     */
    @PostMapping("/api/farms/{farmId}/watersources")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_INFRAESTRUCTURA')")
    public ResponseEntity<WaterSourceResponse> createWaterSource(@PathVariable Integer farmId,
                                                                 @Valid @RequestBody WaterSourceRequest request) {
        log.info("Solicitud POST para crear fuente de agua para finca ID {}", farmId);
        WaterSource newWaterSource = waterSourceService.createWaterSource(farmId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new WaterSourceResponse(newWaterSource));
    }

    /**
     * Obtiene todas las fuentes de agua registradas para una finca específica.
     *
     * @param farmId ID de la finca.
     * @return Lista de fuentes de agua.
     */
    @GetMapping("/api/farms/{farmId}/watersources")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_INFRAESTRUCTURA')")
    public ResponseEntity<List<WaterSourceResponse>> getWaterSourcesByFarm(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener fuentes de agua de la finca ID {}", farmId);
        List<WaterSourceResponse> responses = waterSourceService.getWaterSourcesByFarm(farmId)
                .stream()
                .map(WaterSourceResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    /**
     * Obtiene una fuente de agua específica por su ID global.
     *
     * @param waterSourceId ID de la fuente de agua.
     * @return Detalles de la fuente de agua.
     */
    @GetMapping("/api/watersources/{waterSourceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO') or hasAuthority('VER_INFRAESTRUCTURA')")
    public ResponseEntity<WaterSourceResponse> getWaterSourceById(@PathVariable Integer waterSourceId) {
        log.info("Solicitud GET para obtener fuente de agua ID: {}", waterSourceId);
        WaterSource waterSource = waterSourceService.getWaterSourceById(waterSourceId);
        return ResponseEntity.ok(new WaterSourceResponse(waterSource));
    }

    /**
     * Actualiza los datos de una fuente de agua existente.
     *
     * @param waterSourceId ID de la fuente a actualizar.
     * @param request Nuevos datos.
     * @return La fuente de agua actualizada.
     */
    @PutMapping("/api/watersources/{waterSourceId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_INFRAESTRUCTURA')")
    public ResponseEntity<WaterSourceResponse> updateWaterSource(@PathVariable Integer waterSourceId,
                                                                 @Valid @RequestBody WaterSourceRequest request) {
        log.info("Solicitud PUT para actualizar fuente de agua ID {}", waterSourceId);
        WaterSource updatedWaterSource = waterSourceService.updateWaterSource(waterSourceId, request);
        return ResponseEntity.ok(new WaterSourceResponse(updatedWaterSource));
    }

    /**
     * Elimina una fuente de agua.
     * Si la fuente tiene dependencias (turnos, riegos), el servicio lanzará una excepción
     * que será manejada globalmente (devuelve 409 Conflict si es IllegalStateException).
     *
     * @param waterSourceId ID de la fuente a eliminar.
     * @return 204 No Content.
     */
    @DeleteMapping("/api/watersources/{waterSourceId}")
    @PreAuthorize("hasRole('ADMIN') or hasAuthority('GESTIONAR_INFRAESTRUCTURA')")
    public ResponseEntity<Void> deleteWaterSource(@PathVariable Integer waterSourceId) {
        log.info("Solicitud DELETE para eliminar fuente de agua ID: {}", waterSourceId);
        waterSourceService.deleteWaterSource(waterSourceId);
        return ResponseEntity.noContent().build();
    }
}