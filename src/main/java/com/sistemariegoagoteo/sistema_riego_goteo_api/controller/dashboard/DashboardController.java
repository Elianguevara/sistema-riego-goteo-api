package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.dashboard;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.DashboardKpiResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Obtiene los KPIs generales del sistema para el dashboard principal.
     * Accesible para Administradores y Analistas.
     *
     * @return Objeto con los indicadores clave de rendimiento.
     */
    @GetMapping("/kpis")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA')")
    public ResponseEntity<DashboardKpiResponse> getDashboardKpis() {
        DashboardKpiResponse kpis = dashboardService.getDashboardKpis();
        return ResponseEntity.ok(kpis);
    }
}