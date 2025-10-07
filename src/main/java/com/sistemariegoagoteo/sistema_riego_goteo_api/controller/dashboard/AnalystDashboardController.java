package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.dashboard;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.analyst.FarmStatusDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.analyst.TaskSummaryDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.analyst.WaterBalanceDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.dashboard.AnalystDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard/analyst")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ANALISTA')") // Proteger todo el controlador para analistas
public class AnalystDashboardController {

    private final AnalystDashboardService analystDashboardService;

    /**
     * Endpoint para la vista de mapa. Devuelve una lista de fincas con su estado.
     */
    @GetMapping("/farm-statuses")
    public ResponseEntity<List<FarmStatusDTO>> getFarmStatuses() {
        return ResponseEntity.ok(analystDashboardService.getFarmsStatus());
    }

    /**
     * Endpoint para el balance hídrico de una finca en un rango de fechas.
     */
    @GetMapping("/water-balance/{farmId}")
    public ResponseEntity<List<WaterBalanceDTO>> getWaterBalance(
            @PathVariable Integer farmId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) Date endDate) {
        return ResponseEntity.ok(analystDashboardService.getWaterBalance(farmId, startDate, endDate));
    }

    /**
     * Endpoint para el resumen de tareas creadas por el analista.
     */
    @GetMapping("/task-summary")
    public ResponseEntity<TaskSummaryDTO> getTaskSummary() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(analystDashboardService.getTaskSummary(currentUser));
    }
}