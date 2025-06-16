package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DashboardKpiResponse {
    private long totalUsers;
    private long totalFarms;
    private long activeSectors; // O el nombre que prefieras
    private long activeAlerts;
}