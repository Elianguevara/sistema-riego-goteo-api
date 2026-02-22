package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.SectorRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
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
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para SectorService usando Mockito.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SectorService - Tests Unitarios")
class SectorServiceTest {

    @Mock
    private SectorRepository sectorRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private IrrigationEquipmentRepository irrigationEquipmentRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private SectorService sectorService;

    private Farm testFarm;
    private IrrigationEquipment testEquipment;
    private Sector testSector;
    private SectorRequest sectorRequest;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testFarm = new Farm();
        testFarm.setId(1);
        testFarm.setName("Finca Test");
        testFarm.setReservoirCapacity(new BigDecimal("500"));
        testFarm.setFarmSize(new BigDecimal("20"));

        testEquipment = new IrrigationEquipment();
        testEquipment.setId(10);
        testEquipment.setName("Equipo Principal");
        testEquipment.setMeasuredFlow(new BigDecimal("5.00"));
        testEquipment.setFarm(testFarm); // El equipo pertenece a la misma finca

        testSector = new Sector();
        testSector.setId(100);
        testSector.setName("Sector Norte");
        testSector.setFarm(testFarm);

        sectorRequest = new SectorRequest();
        sectorRequest.setName("Sector Norte");
        sectorRequest.setEquipmentId(10);

        Role adminRole = new Role("ADMIN");
        adminRole.setId(1);
        adminUser = new User("Admin", "admin", "pass", "admin@test.com", adminRole);
        adminUser.setActive(true);

        // Configurar SecurityContext
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(adminUser);
        SecurityContextHolder.setContext(securityContext);
    }

    // ===== TESTS DE createSector() =====

    @Test
    @DisplayName("createSector() debe crear el sector correctamente cuando los datos son válidos")
    void createSector_datosValidos_creaYRetornaSector() {
        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));
        when(sectorRepository.findByNameAndFarm(anyString(), any(Farm.class))).thenReturn(Optional.empty());
        when(irrigationEquipmentRepository.findById(10)).thenReturn(Optional.of(testEquipment));
        when(sectorRepository.save(any(Sector.class))).thenReturn(testSector);
        doNothing().when(auditService).logChange(any(), any(), any(), any(), any(), any());

        Sector result = sectorService.createSector(1, sectorRequest);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Sector Norte");
        verify(sectorRepository, times(1)).save(any(Sector.class));
    }

    @Test
    @DisplayName("createSector() debe lanzar IllegalArgumentException si ya existe un sector con ese nombre en la finca")
    void createSector_sectorDuplicado_lanzaIllegalArgumentException() {
        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));
        when(sectorRepository.findByNameAndFarm("Sector Norte", testFarm))
                .thenReturn(Optional.of(testSector));

        assertThatThrownBy(() -> sectorService.createSector(1, sectorRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sector Norte");
        verify(sectorRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSector() debe lanzar IllegalArgumentException si el equipo no pertenece a la finca")
    void createSector_equipoNoPerteneceFinca_lanzaIllegalArgumentException() {
        Farm otraFinca = new Farm();
        otraFinca.setId(99);
        otraFinca.setName("Otra Finca");
        otraFinca.setReservoirCapacity(BigDecimal.ZERO);
        otraFinca.setFarmSize(BigDecimal.ZERO);
        testEquipment.setFarm(otraFinca); // El equipo pertenece a otra finca

        when(farmRepository.findById(1)).thenReturn(Optional.of(testFarm));
        when(sectorRepository.findByNameAndFarm(anyString(), any(Farm.class))).thenReturn(Optional.empty());
        when(irrigationEquipmentRepository.findById(10)).thenReturn(Optional.of(testEquipment));

        assertThatThrownBy(() -> sectorService.createSector(1, sectorRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no pertenece");
        verify(sectorRepository, never()).save(any());
    }

    @Test
    @DisplayName("createSector() debe lanzar ResourceNotFoundException si la finca no existe")
    void createSector_farmaNoExiste_lanzaResourceNotFoundException() {
        when(farmRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sectorService.createSector(999, sectorRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== TESTS DE getSectorsByFarmId() =====

    @Test
    @DisplayName("getSectorsByFarmId() debe retornar la lista de sectores de la finca")
    void getSectorsByFarmId_farmaExiste_retornaSectores() {
        when(farmRepository.existsById(1)).thenReturn(true);
        when(sectorRepository.findByFarm_Id(1)).thenReturn(List.of(testSector));

        List<Sector> result = sectorService.getSectorsByFarmId(1);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Sector Norte");
    }

    @Test
    @DisplayName("getSectorsByFarmId() debe lanzar ResourceNotFoundException si la finca no existe")
    void getSectorsByFarmId_farmaNoExiste_lanzaResourceNotFoundException() {
        when(farmRepository.existsById(999)).thenReturn(false);

        assertThatThrownBy(() -> sectorService.getSectorsByFarmId(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== TESTS DE deleteSector() =====

    @Test
    @DisplayName("deleteSector() debe eliminar el sector y registrar auditoría")
    void deleteSector_sectorExistente_eliminaYRegistraAuditoria() {
        when(sectorRepository.findByIdAndFarm_Id(100, 1)).thenReturn(Optional.of(testSector));
        doNothing().when(auditService).logChange(any(), any(), any(), any(), any(), any());

        sectorService.deleteSector(1, 100);

        verify(sectorRepository, times(1)).delete(testSector);
        verify(auditService, times(1)).logChange(any(), eq("DELETE"), any(), any(), any(), any());
    }
}
