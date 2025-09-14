package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class DashboardKpiResponse {
    private long totalUsers;
    private long totalFarms;
    private long totalSectors;
    private long activeSectors; // <-- CAMPO AÑADIDO
    private long activeAlerts;
    private Map<String, Long> equipmentStatusCount;
}