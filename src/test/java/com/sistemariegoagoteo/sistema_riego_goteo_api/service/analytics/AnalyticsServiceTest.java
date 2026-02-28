package com.sistemariegoagoteo.sistema_riego_goteo_api.service.analytics;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.analytics.IrrigationRecordDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.analytics.IrrigationSectorSummaryDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.analytics.IrrigationTimeseriesDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.projection.DailyIrrigationProjection;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.projection.SectorIrrigationProjection;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private IrrigationRepository irrigationRepository;
    @Mock
    private SectorRepository sectorRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Date startDate;
    private Date endDate;
    private Sector sector;

    @BeforeEach
    void setUp() {
        startDate = new Date(System.currentTimeMillis() - 86400000L * 7); // 7 días atrás
        endDate = new Date();
        sector = new Sector();
        sector.setId(1);
        sector.setName("Sector A");
    }

    @Test
    void getIrrigationSummary_Success() {
        SectorIrrigationProjection proj = mock(SectorIrrigationProjection.class);
        when(proj.getSectorId()).thenReturn(1);
        when(proj.getSectorName()).thenReturn("Sector A");
        when(proj.getWaterAmount()).thenReturn(new BigDecimal("100.5"));
        when(proj.getHours()).thenReturn(new BigDecimal("5.5"));

        when(irrigationRepository.getSectorIrrigationTotals(eq(1), org.mockito.ArgumentMatchers.<List<Integer>>any(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(proj));

        List<IrrigationSectorSummaryDTO> result = analyticsService.getIrrigationSummary(1, startDate, endDate,
                Arrays.asList(1));

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Sector A", result.get(0).getSectorName());
        assertEquals(new BigDecimal("100.5"), result.get(0).getTotalWaterAmount());
    }

    @Test
    void getIrrigationTimeseries_Success() {
        LocalDate start = LocalDate.now().minusDays(2);
        LocalDate end = LocalDate.now();

        DailyIrrigationProjection proj = mock(DailyIrrigationProjection.class);
        when(proj.getIrrigationDate()).thenReturn(start);
        when(proj.getWaterAmount()).thenReturn(new BigDecimal("50.0"));
        when(proj.getHours()).thenReturn(new BigDecimal("2.5"));

        when(irrigationRepository.getDailyIrrigationTotals(eq(1), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(Arrays.asList(proj));

        List<IrrigationTimeseriesDTO> result = analyticsService.getIrrigationTimeseries(1, start, end);

        assertNotNull(result);
        assertEquals(3, result.size()); // start, start+1, end (3 días)
        assertEquals(new BigDecimal("50.0"), result.get(0).getWaterAmount());
        assertEquals(new BigDecimal("0"), result.get(1).getWaterAmount()); // Empty day filled with ZERO
    }

    @Test
    void getIrrigationRecords_Success() {
        Irrigation irrigation = new Irrigation();
        irrigation.setId(1);
        irrigation.setSector(sector);
        irrigation.setStartDatetime(LocalDateTime.now());
        irrigation.setEndDatetime(LocalDateTime.now().plusHours(1));
        irrigation.setWaterAmount(new BigDecimal("10.0"));

        Pageable pageable = PageRequest.of(0, 10);
        Page<Irrigation> page = new PageImpl<>(Arrays.asList(irrigation));

        when(irrigationRepository.findAll(org.mockito.ArgumentMatchers.<Specification<Irrigation>>any(), eq(pageable)))
                .thenReturn(page);

        Page<IrrigationRecordDTO> result = analyticsService.getIrrigationRecords(1, startDate, endDate,
                Collections.singletonList(1), pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Sector A", result.getContent().get(0).getSectorName());
        assertEquals(new BigDecimal("10.0"), result.getContent().get(0).getWaterAmount());
    }

    @Test
    void getIrrigationSummary_EmptySectorIds_QueriesFromFarm() {
        // Cuando sectorIds es null, debe buscar todos los sectores de la finca
        when(sectorRepository.findByFarm_Id(1)).thenReturn(List.of(sector));
        when(irrigationRepository.getSectorIrrigationTotals(eq(1), org.mockito.ArgumentMatchers.<List<Integer>>any(),
                any(LocalDateTime.class),
                any(LocalDateTime.class))).thenReturn(List.of());

        List<IrrigationSectorSummaryDTO> result = analyticsService.getIrrigationSummary(1, startDate, endDate, null);

        assertNotNull(result);
        assertEquals(0, result.size()); // Repositorio devuelve vacío → lista vacía
    }

    @Test
    void getIrrigationTimeseries_AllDaysEmpty_ReturnZeroForEachDay() {
        LocalDate start = LocalDate.now().minusDays(1);
        LocalDate end = LocalDate.now();

        // Repositorio no devuelve datos → todos los días deben tener ZERO
        when(irrigationRepository.getDailyIrrigationTotals(eq(1), any(LocalDateTime.class),
                any(LocalDateTime.class))).thenReturn(List.of());

        List<IrrigationTimeseriesDTO> result = analyticsService.getIrrigationTimeseries(1, start, end);

        assertNotNull(result);
        assertEquals(2, result.size()); // start + end = 2 días
        result.forEach(dto -> assertEquals(BigDecimal.ZERO, dto.getWaterAmount()));
    }
}
