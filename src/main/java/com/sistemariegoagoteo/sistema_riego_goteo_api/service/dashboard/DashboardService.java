package com.sistemariegoagoteo.sistema_riego_goteo_api.service.dashboard;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.DashboardKpiResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository; // <-- CORREGIDO
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final FarmRepository farmRepository;
    private final SectorRepository sectorRepository;
    private final IrrigationEquipmentRepository equipmentRepository; // <-- CORREGIDO

    public DashboardKpiResponse getDashboardKpis() {
        long totalUsers = userRepository.count();
        long totalFarms = farmRepository.count();
        long totalSectors = sectorRepository.count();

        long activeAlerts = 14; // Lógica a implementar para alertas

        // Nueva lógica para contar equipos por estado
        Map<String, Long> equipmentStatusCount = equipmentRepository.countByStatus().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0], // El estado (e.g., "Operativo")
                        row -> (Long) row[1]     // El conteo
                ));

        return new DashboardKpiResponse(totalUsers, totalFarms, totalSectors, activeAlerts, equipmentStatusCount);
    }
}