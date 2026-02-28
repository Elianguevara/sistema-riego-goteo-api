package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.PrecipitationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Precipitation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.PrecipitationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.config.SystemConfigService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.config.AgronomicConfigDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrecipitationServiceTest {

    @Mock
    private PrecipitationRepository precipitationRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private SystemConfigService systemConfigService;

    @InjectMocks
    private PrecipitationService precipitationService;

    private User authUser;
    private Farm farm;
    private Precipitation precipitation;
    private AgronomicConfigDTO agConfig;

    @BeforeEach
    void setUp() {
        authUser = new User();
        authUser.setId(1L);
        authUser.setUsername("testuser");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authUser, null, new java.util.ArrayList<>()));

        farm = new Farm();
        farm.setId(1);

        precipitation = new Precipitation();
        precipitation.setId(1);
        precipitation.setFarm(farm);
        precipitation.setMmRain(new BigDecimal("10.00"));
        precipitation.setMmEffectiveRain(new BigDecimal("3.75")); // (10 - 5) * 0.75
        precipitation.setPrecipitationDate(LocalDate.of(2026, 2, 27));

        agConfig = new AgronomicConfigDTO();
        agConfig.setPrecipitationEffectivenessThresholdMm(5.0f);
        agConfig.setEffectiveRainCoefficient(0.75f);
    }

    @Test
    void createPrecipitation_Success_EffectiveRainCalculated() {
        PrecipitationRequest request = new PrecipitationRequest();
        request.setPrecipitationDate(LocalDate.of(2026, 2, 28));
        request.setMmRain(new BigDecimal("15.5"));

        when(systemConfigService.getAgronomicConfig()).thenReturn(agConfig);
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(precipitationRepository.save(any(Precipitation.class))).thenAnswer(i -> {
            Precipitation p = i.getArgument(0);
            p.setId(2);
            return p;
        });

        Precipitation result = precipitationService.createPrecipitation(1, request);

        assertNotNull(result);
        assertEquals(2, result.getId());
        assertEquals(new BigDecimal("15.50"), result.getMmRain());
        // (15.5 - 5) * 0.75 = 7.875 -> 7.88
        assertEquals(new BigDecimal("7.88"), result.getMmEffectiveRain());

        verify(auditService).logChange(any(User.class), eq("CREATE"), eq("Precipitation"), eq("mmRain"), isNull(),
                eq("15.50"));
    }

    @Test
    void createPrecipitation_LowRain_ZeroEffective() {
        PrecipitationRequest request = new PrecipitationRequest();
        request.setPrecipitationDate(LocalDate.of(2026, 2, 28));
        request.setMmRain(new BigDecimal("4.0"));

        when(systemConfigService.getAgronomicConfig()).thenReturn(agConfig);
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(precipitationRepository.save(any(Precipitation.class))).thenAnswer(i -> i.getArgument(0));

        Precipitation result = precipitationService.createPrecipitation(1, request);

        assertEquals(new BigDecimal("4.00"), result.getMmRain());
        assertEquals(new BigDecimal("0.00"), result.getMmEffectiveRain());
    }

    @Test
    void updatePrecipitation_Success() {
        PrecipitationRequest request = new PrecipitationRequest();
        request.setPrecipitationDate(LocalDate.of(2026, 2, 27));
        request.setMmRain(new BigDecimal("12.00")); // Change amount

        when(systemConfigService.getAgronomicConfig()).thenReturn(agConfig);
        when(precipitationRepository.findById(1)).thenReturn(Optional.of(precipitation));
        when(precipitationRepository.save(any(Precipitation.class))).thenAnswer(i -> i.getArgument(0));

        Precipitation result = precipitationService.updatePrecipitation(1, request);

        assertEquals(new BigDecimal("12.00"), result.getMmRain());
        assertEquals(new BigDecimal("5.25"), result.getMmEffectiveRain()); // (12-5)*0.75
        verify(auditService).logChange(any(User.class), eq("UPDATE"), eq("Precipitation"), eq("mmRain"), eq("10.00"),
                eq("12.00"));
    }

    @Test
    void deletePrecipitation_Success() {
        when(precipitationRepository.findById(1)).thenReturn(Optional.of(precipitation));

        precipitationService.deletePrecipitation(1);

        verify(precipitationRepository).delete(precipitation);
        verify(auditService).logChange(any(User.class), eq("DELETE"), eq("Precipitation"), eq("id"), eq("1"), isNull());
    }
}
