package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {
    private long totalUsers;
    private long activeUsers;
    private long inactiveUsers;
    private Map<String, Long> usersByRole;
}