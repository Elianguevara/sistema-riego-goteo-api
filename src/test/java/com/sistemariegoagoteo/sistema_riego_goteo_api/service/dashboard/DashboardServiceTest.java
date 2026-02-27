package com.sistemariegoagoteo.sistema_riego_goteo_api.service.dashboard;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.DashboardKpiResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private SectorRepository sectorRepository;
    @Mock
    private IrrigationEquipmentRepository equipmentRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    void getDashboardKpis_Success() {
        when(userRepository.count()).thenReturn(10L);
        when(farmRepository.count()).thenReturn(5L);
        when(sectorRepository.count()).thenReturn(20L);
        when(sectorRepository.countByEquipmentStatus("ACTIVO")).thenReturn(15L);
        when(equipmentRepository.countByStatus()).thenReturn(Arrays.asList(
                new Object[] { "ACTIVO", 30L },
                new Object[] { "INACTIVO", 5L }));

        DashboardKpiResponse result = dashboardService.getDashboardKpis();

        assertNotNull(result);
        assertEquals(10L, result.getTotalUsers());
        assertEquals(5L, result.getTotalFarms());
        assertEquals(20L, result.getTotalSectors());
        assertEquals(15L, result.getActiveSectors());
        assertEquals(14L, result.getActiveAlerts()); // Hardcoded implementation limit
        assertEquals(2, result.getEquipmentStatusCount().size());
        assertEquals(30L, result.getEquipmentStatusCount().get("ACTIVO"));
    }
}
