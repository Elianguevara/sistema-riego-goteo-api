package com.sistemariegoagoteo.sistema_riego_goteo_api.service.report;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.OperationsLogReportDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.PeriodSummaryReportDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.WaterBalanceReportDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.projection.DailyIrrigationProjection;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.projection.DailyRainProjection;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.projection.OperationLogProjection;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.projection.SectorIrrigationProjection;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.TaskStatus;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportDataServiceTest {

        @Mock
        private FarmRepository farmRepository;
        @Mock
        private SectorRepository sectorRepository;
        @Mock
        private IrrigationRepository irrigationRepository;
        @Mock
        private PrecipitationRepository precipitationRepository;
        @Mock
        private MaintenanceRepository maintenanceRepository;
        @Mock
        private FertilizationRepository fertilizationRepository;
        @Mock
        private TaskRepository taskRepository;

        @InjectMocks
        private ReportDataService reportDataService;

        private Farm farm;
        private Sector sector;
        private Date startDate;
        private Date endDate;

        @BeforeEach
        void setUp() {
                farm = new Farm();
                farm.setId(1);
                farm.setName("Finca Test");

                sector = new Sector();
                sector.setId(1);
                sector.setName("Sector 1");
                sector.setFarm(farm);

                startDate = new Date(System.currentTimeMillis() - 86400000L * 7); // 7 days ago
                endDate = new Date();
        }

        @Test
        void getWaterBalanceData_Success() {
                when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
                when(sectorRepository.findByFarm_Id(1)).thenReturn(Arrays.asList(sector));
                when(sectorRepository.findById(1)).thenReturn(Optional.of(sector));

                // Mock projections
                DailyRainProjection rainProj = mock(DailyRainProjection.class);
                when(rainProj.getRainDate()).thenReturn(LocalDate.now());
                when(rainProj.getAmount()).thenReturn(new BigDecimal("10.0"));

                SectorIrrigationProjection sectorProj = mock(SectorIrrigationProjection.class);
                when(sectorProj.getSectorId()).thenReturn(1);
                when(sectorProj.getWaterAmount()).thenReturn(new BigDecimal("100.0"));
                when(sectorProj.getHours()).thenReturn(new BigDecimal("5.0"));

                when(precipitationRepository.findDailyRainByFarm(eq(1), any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(Arrays.asList(rainProj));
                when(irrigationRepository.getSectorIrrigationTotals(eq(1), anyList(), any(LocalDateTime.class),
                                any(LocalDateTime.class)))
                                .thenReturn(Arrays.asList(sectorProj));
                when(irrigationRepository.getDailyIrrigationTotals(eq(1), any(LocalDateTime.class),
                                any(LocalDateTime.class)))
                                .thenReturn(Arrays.asList());

                WaterBalanceReportDTO result = reportDataService.getWaterBalanceData(1, startDate, endDate, null);

                assertNotNull(result);
                assertEquals("Finca Test", result.getFarmName());
                assertEquals(1, result.getSectors().size());
                assertEquals(new BigDecimal("100.0"), result.getFarmTotals().getTotalIrrigationWater());
                assertEquals(new BigDecimal("10.0"), result.getFarmTotals().getTotalEffectiveRain());
        }

        @Test
        void getOperationsLogData_Success() {
                when(farmRepository.findById(1)).thenReturn(Optional.of(farm));

                OperationLogProjection logProj = mock(OperationLogProjection.class);
                when(logProj.getDatetime()).thenReturn(new Date());
                when(logProj.getType()).thenReturn("RIEGO");
                when(logProj.getDescription()).thenReturn("Riego completado");
                when(logProj.getUserName()).thenReturn("admin");

                when(irrigationRepository.getIrrigationLogs(eq(1), any(LocalDateTime.class), any(LocalDateTime.class)))
                                .thenReturn(Arrays.asList(logProj));

                OperationsLogReportDTO result = reportDataService.getOperationsLogData(1, startDate, endDate, "RIEGO",
                                null);

                assertNotNull(result);
                assertEquals(1, result.getOperations().size());
                assertEquals("RIEGO", result.getOperations().get(0).getType());
        }

        @Test
        void getPeriodSummaryData_Success() {
                when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
                when(sectorRepository.findByFarm_Id(1)).thenReturn(Arrays.asList(sector));

                DailyRainProjection rainProj = mock(DailyRainProjection.class);
                when(rainProj.getAmount()).thenReturn(new BigDecimal("20.0"));

                SectorIrrigationProjection sectorProj = mock(SectorIrrigationProjection.class);
                when(sectorProj.getWaterAmount()).thenReturn(new BigDecimal("50.0"));
                when(sectorProj.getHours()).thenReturn(new BigDecimal("10.0"));

                when(precipitationRepository.findDailyRainByFarm(eq(1), any(LocalDate.class), any(LocalDate.class)))
                                .thenReturn(Arrays.asList(rainProj));
                when(irrigationRepository.getSectorIrrigationTotals(eq(1), anyList(), any(LocalDateTime.class),
                                any(LocalDateTime.class)))
                                .thenReturn(Arrays.asList(sectorProj));

                when(taskRepository.countBySector_Farm_IdAndCreatedAtBetween(eq(1), eq(startDate), eq(endDate)))
                                .thenReturn(5L);
                when(taskRepository.countBySector_Farm_IdAndCreatedAtBetweenAndStatus(eq(1), eq(startDate), eq(endDate),
                                eq(TaskStatus.COMPLETADA))).thenReturn(3L);

                PeriodSummaryReportDTO result = reportDataService.getPeriodSummaryData(1, startDate, endDate);

                assertNotNull(result);
                assertEquals(new BigDecimal("50.0"), result.getWaterSummary().getTotalIrrigationWaterM3());
                assertEquals(5L, result.getOperationsSummary().getTasksCreated());
                assertEquals(3L, result.getOperationsSummary().getTasksCompleted());
        }
}
