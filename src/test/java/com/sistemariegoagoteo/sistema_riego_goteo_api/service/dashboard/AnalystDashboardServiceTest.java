package com.sistemariegoagoteo.sistema_riego_goteo_api.service.dashboard;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.analyst.FarmStatusDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.analyst.TaskSummaryDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.analyst.WaterBalanceDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Precipitation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.TaskStatus;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.PrecipitationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalystDashboardServiceTest {

    @Mock
    private FarmRepository farmRepository;
    @Mock
    private IrrigationRepository irrigationRepository;
    @Mock
    private PrecipitationRepository precipitationRepository;
    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private AnalystDashboardService analystDashboardService;

    private Farm farm;
    private Date startDate;
    private Date endDate;

    @BeforeEach
    void setUp() {
        farm = new Farm();
        farm.setId(1);
        farm.setName("Finca Principal");

        startDate = new Date(System.currentTimeMillis() - 86400000L * 7); // 7 días atrás
        endDate = new Date();
    }

    @Test
    void getFarmsStatus_Success() {
        when(farmRepository.findAll()).thenReturn(Arrays.asList(farm));

        List<FarmStatusDTO> results = analystDashboardService.getFarmsStatus();

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Finca Principal", results.get(0).getName());
        assertEquals("OK", results.get(0).getStatus()); // According to current mock
        assertEquals(0, results.get(0).getActiveAlertsCount());
    }

    @Test
    void getWaterBalance_Success() {
        Irrigation irrigation = new Irrigation();
        irrigation.setWaterAmount(new BigDecimal("50.0"));
        irrigation.setStartDatetime(LocalDateTime.now());

        Precipitation precipitation = new Precipitation();
        precipitation.setMmEffectiveRain(new BigDecimal("12.5"));
        precipitation.setPrecipitationDate(LocalDate.now());

        when(irrigationRepository.findBySector_Farm_IdAndStartDatetimeBetween(eq(1), any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(irrigation));

        when(precipitationRepository.findByFarm_IdAndPrecipitationDateBetween(eq(1), any(LocalDate.class),
                any(LocalDate.class)))
                .thenReturn(Arrays.asList(precipitation));

        List<WaterBalanceDTO> results = analystDashboardService.getWaterBalance(1, startDate, endDate);

        assertNotNull(results);
        assertEquals(1, results.size()); // Assuming both fall on the exact same LocalDate
        WaterBalanceDTO balance = results.get(0);
        assertEquals(new BigDecimal("50.0"), balance.getIrrigationWater());
        assertEquals(new BigDecimal("12.5"), balance.getEffectiveRain());
    }

    @Test
    void getTaskSummary_Success() {
        User user = new User();
        user.setId(1L);

        when(taskRepository.countByCreatedBy(user)).thenReturn(10L);
        when(taskRepository.countByCreatedByAndStatus(user, TaskStatus.PENDIENTE)).thenReturn(5L);
        when(taskRepository.countByCreatedByAndStatus(user, TaskStatus.EN_PROGRESO)).thenReturn(3L);
        when(taskRepository.countByCreatedByAndStatus(user, TaskStatus.COMPLETADA)).thenReturn(2L);

        TaskSummaryDTO result = analystDashboardService.getTaskSummary(user);

        assertNotNull(result);
        assertEquals(10L, result.getTotalTasks());
        assertEquals(5L, result.getPendingTasks());
        assertEquals(3L, result.getInProgressTasks());
        assertEquals(2L, result.getCompletedTasks());
    }
}
