package com.sistemariegoagoteo.sistema_riego_goteo_api.service.dashboard;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.DashboardKpiResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.HumidityAlertRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final SectorRepository sectorRepository;

    public DashboardKpiResponse getDashboardKpis() {
        long totalUsers = userRepository.count();
        long totalFarms = farmRepository.count();
        long activeSectors = sectorRepository.count(); // Asumiendo que todos los sectores en la DB están "activos"
        long activeAlerts = 14; // Aquí deberías definir la lógica para contar alertas "activas"
        // Ejemplo: humidityAlertRepository.countByIsAcknowledgedFalse();

        return new DashboardKpiResponse(totalUsers, totalFarms, activeSectors, activeAlerts);
    }
}