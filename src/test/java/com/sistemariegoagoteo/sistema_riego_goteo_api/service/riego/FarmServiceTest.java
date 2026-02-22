package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FarmRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.geocoding.GeocodingService;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para FarmService usando Mockito.
 * Se mockea el SecurityContextHolder para simular usuarios autenticados.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("FarmService - Tests Unitarios")
class FarmServiceTest {

    @Mock
    private FarmRepository farmRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private GeocodingService geocodingService;

    @InjectMocks
    private FarmService farmService;

    private User adminUser;
    private User operarioUser;
    private Farm testFarm;
    private FarmRequest farmRequest;

    @BeforeEach
    void setUp() {
        Role adminRole = new Role("ADMIN");
        adminRole.setId(1);
        adminUser = new User("Admin", "admin", "pass", "admin@test.com", adminRole);
        adminUser.setActive(true);

        Role operarioRole = new Role("OPERARIO");
        operarioRole.setId(3);
        operarioUser = new User("Operario", "operario1", "pass", "op@test.com", operarioRole);

        testFarm = new Farm();
        testFarm.setId(1);
        testFarm.setName("Finca La Esperanza");
        testFarm.setLocation("La Serena, Chile");
        testFarm.setReservoirCapacity(new BigDecimal("1000.00"));
        testFarm.setFarmSize(new BigDecimal("50.00"));

        farmRequest = new FarmRequest();
        farmRequest.setName("Finca La Esperanza");
        farmRequest.setLocation("La Serena, Chile");
        farmRequest.setReservoirCapacity(new BigDecimal("1000.00"));
        farmRequest.setFarmSize(new BigDecimal("50.00"));
        farmRequest.setLatitude(new BigDecimal("-29.9027"));
        farmRequest.setLongitude(new BigDecimal("-71.2519"));
    }

    /**
     * Configura el SecurityContextHolder para simular un usuario autenticado.
     */
    private void mockSecurityContext(User user) {
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authentication.isAuthenticated()).thenReturn(true);
        SecurityContextHolder.setContext(securityContext);
    }

    // ===== TESTS DE createFarm() =====

    @Test
    @DisplayName("createFarm() debe guardar y retornar la finca cuando los datos son válidos")
    void createFarm_datosValidos_guardaYRetornaFinca() {
        mockSecurityContext(adminUser);
        when(farmRepository.save(any(Farm.class))).thenReturn(testFarm);
        doNothing().when(auditService).logChange(any(), any(), any(), any(), any(), any());

        Farm result = farmService.createFarm(farmRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Finca La Esperanza");
        verify(farmRepository, times(1)).save(any(Farm.class));
        verify(auditService, times(1)).logChange(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("createFarm() debe usar geocodificación si no se proveen coordenadas")
    void createFarm_sinCoordenadas_intentaGeocodificar() {
        mockSecurityContext(adminUser);
        farmRequest.setLatitude(null);
        farmRequest.setLongitude(null);
        when(geocodingService.getCoordinates(anyString())).thenReturn(Optional.empty());
        when(farmRepository.save(any(Farm.class))).thenReturn(testFarm);
        doNothing().when(auditService).logChange(any(), any(), any(), any(), any(), any());

        farmService.createFarm(farmRequest);

        verify(geocodingService, times(1)).getCoordinates(anyString());
    }

    // ===== TESTS DE getFarmById() =====

    @Test
    @DisplayName("getFarmById() debe retornar la finca cuando existe")
    void getFarmById_farmExistente_retornaFinca() {
        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));

        Farm result = farmService.getFarmById(1);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    @DisplayName("getFarmById() debe lanzar ResourceNotFoundException cuando la finca no existe")
    void getFarmById_farmNoExiste_lanzaResourceNotFoundException() {
        when(farmRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> farmService.getFarmById(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== TESTS DE deleteFarm() =====

    @Test
    @DisplayName("deleteFarm() debe eliminar la finca y registrar auditoría")
    void deleteFarm_farmExistente_eliminaYRegistraAuditoria() {
        mockSecurityContext(adminUser);
        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));
        doNothing().when(auditService).logChange(any(), any(), any(), any(), any(), any());

        farmService.deleteFarm(1);

        verify(farmRepository, times(1)).delete(testFarm);
        verify(auditService, times(1)).logChange(any(), eq("DELETE"), any(), any(), any(), any());
    }

    // ===== TESTS DE getAllFarms() =====

    @Test
    @DisplayName("getAllFarms() debe retornar todas las fincas para el rol ADMIN")
    void getAllFarms_rolAdmin_retornaTodasLasFincas() {
        mockSecurityContext(adminUser);
        when(farmRepository.findAll()).thenReturn(List.of(testFarm));

        List<Farm> result = farmService.getAllFarms();

        assertThat(result).hasSize(1);
        verify(farmRepository, times(1)).findAll();
        verify(farmRepository, never()).findFarmsByUsername(anyString());
    }

    @Test
    @DisplayName("getAllFarms() debe retornar solo las fincas del operario para el rol OPERARIO")
    void getAllFarms_rolOperario_retornaFincasDelOperario() {
        mockSecurityContext(operarioUser);
        when(farmRepository.findFarmsByUsername("operario1")).thenReturn(List.of(testFarm));

        List<Farm> result = farmService.getAllFarms();

        assertThat(result).hasSize(1);
        verify(farmRepository, times(1)).findFarmsByUsername("operario1");
        verify(farmRepository, never()).findAll();
    }
}
