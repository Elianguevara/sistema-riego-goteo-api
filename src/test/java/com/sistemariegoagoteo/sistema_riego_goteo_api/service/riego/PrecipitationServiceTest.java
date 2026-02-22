package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.PrecipitationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.PrecipitationSummaryResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Precipitation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.PrecipitationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PrecipitationService - Tests Unitarios")
class PrecipitationServiceTest {

    @Mock
    private PrecipitationRepository precipitationRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private PrecipitationService precipitationService;

    private User testUser;
    private Farm testFarm;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");

        testFarm = new Farm();
        testFarm.setId(1);
        testFarm.setName("Finca Test");

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(testUser);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("createPrecipitation() debe guardar correctamente usando LocalDate")
    void createPrecipitation_Ok() {
        LocalDate date = LocalDate.now();
        PrecipitationRequest request = new PrecipitationRequest();
        request.setPrecipitationDate(date);
        request.setMmRain(new BigDecimal("10.00"));

        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));
        when(precipitationRepository.save(any(Precipitation.class))).thenAnswer(i -> i.getArguments()[0]);

        Precipitation result = precipitationService.createPrecipitation(1, request);

        assertThat(result.getPrecipitationDate()).isEqualTo(date);
        assertThat(result.getMmRain()).isEqualByComparingTo("10.00");
        assertThat(result.getMmEffectiveRain()).isEqualByComparingTo("3.75"); // (10 - 5) * 0.75
        verify(precipitationRepository).save(any());
    }

    @Test
    @DisplayName("getMonthlyPrecipitation() debe calcular rango correcto")
    void getMonthlyPrecipitation_Ok() {
        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));
        when(precipitationRepository.getMonthlyPrecipitation(testFarm, 2026, 2)).thenReturn(new BigDecimal("50.00"));

        PrecipitationSummaryResponse summary = precipitationService.getMonthlyPrecipitation(1, 2026, 2);

        assertThat(summary.getStartDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(summary.getEndDate()).isEqualTo(LocalDate.of(2026, 2, 28));
        assertThat(summary.getTotalMmRain()).isEqualByComparingTo("50.00");
    }
}
