package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.SectorResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.SectorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sectors") // Ruta base para endpoints de sectores globales
@RequiredArgsConstructor
@Slf4j
public class GlobalSectorController {

    private final SectorService sectorService;

    /**
     * Obtiene una lista de todos los sectores activos del sistema.
     * Un sector se considera activo si su equipo de riego asociado tiene el estado 'Operativo'.
     *
     * @return ResponseEntity con la lista de sectores activos.
     */
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    public ResponseEntity<List<SectorResponse>> getActiveSectors() {
        log.info("Solicitud GET para obtener todos los sectores activos");
        List<SectorResponse> activeSectors = sectorService.getActiveSectors().stream()
                .map(SectorResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(activeSectors);
    }
}