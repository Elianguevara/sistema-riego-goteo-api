package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationRepository;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para IrrigationService.
 * Verifica especialmente los cálculos de horas e irrigación y la lógica CRUD.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("IrrigationService - Tests Unitarios")
class IrrigationServiceTest {

    @Mock
    private IrrigationRepository irrigationRepository;
    @Mock
    private SectorRepository sectorRepository;
    @Mock
    private IrrigationEquipmentRepository equipmentRepository;
    @Mock
    private AuditService auditService;
    @Mock
    private FarmRepository farmRepository;

    @InjectMocks
    private IrrigationService irrigationService;

    private Farm testFarm;
    private Sector testSector;
    private IrrigationEquipment testEquipment;
    private Irrigation testIrrigation;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testFarm = new Farm();
        testFarm.setId(1);
        testFarm.setName("Finca Test");
        testFarm.setReservoirCapacity(new BigDecimal("1000"));
        testFarm.setFarmSize(new BigDecimal("50"));

        testEquipment = new IrrigationEquipment();
        testEquipment.setId(10);
        testEquipment.setName("Equipo A");
        testEquipment.setMeasuredFlow(new BigDecimal("5.00")); // 5 m³/h

        testSector = new Sector();
        testSector.setId(100);
        testSector.setName("Sector Norte");
        testSector.setFarm(testFarm);

        testIrrigation = new Irrigation();
        testIrrigation.setId(1000);
        testIrrigation.setSector(testSector);
        testIrrigation.setEquipment(testEquipment);
        testIrrigation.setIrrigationHours(new BigDecimal("2.00"));
        testIrrigation.setWaterAmount(new BigDecimal("100.00")); // 5 m³/h * 2h * 10 hL/m³

        Role adminRole = new Role("ADMIN");
        adminRole.setId(1);
        adminUser = new User("Admin", "admin", "pass", "admin@test.com", adminRole);
        adminUser.setActive(true);

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(adminUser);
        SecurityContextHolder.setContext(securityContext);
    }

    // ===== TESTS DE createIrrigation() =====

    @Test
    @DisplayName("createIrrigation() debe calcular correctamente irrigationHours y waterAmount")
    void createIrrigation_calculosCorrectos_creaIrrigacion() {
        // Inicio: 8:00, Fin: 10:00 → 2 horas
        // 5 m³/h * 2h = 10 m³ * 10 hL/m³ = 100 hL
        long now = System.currentTimeMillis();
        Date startDate = new Date(now);
        Date endDate = new Date(now + 2 * 3600000L); // + 2 horas en milisegundos

        IrrigationRequest request = new IrrigationRequest();
        request.setSectorId(100);
        request.setEquipmentId(10);
        request.setStartDateTime(startDate);
        request.setEndDateTime(endDate);

        when(sectorRepository.findById(100)).thenReturn(Optional.of(testSector));
        when(equipmentRepository.findById(10)).thenReturn(Optional.of(testEquipment));
        when(irrigationRepository.save(any(Irrigation.class))).thenAnswer(inv -> {
            Irrigation saved = inv.getArgument(0);
            saved.setId(1000);
            return saved;
        });
        doNothing().when(auditService).logChange(any(), any(), any(), any(), any(), any());

        Irrigation result = irrigationService.createIrrigation(request);

        assertThat(result).isNotNull();
        // Verificar horas calculadas: 2.00 horas
        assertThat(result.getIrrigationHours()).isEqualByComparingTo(new BigDecimal("2.00"));
        // Verificar agua: 5 m³/h * 2h * 10 hL/m³ = 100 hL
        assertThat(result.getWaterAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
        verify(irrigationRepository, times(1)).save(any(Irrigation.class));
    }

    @Test
    @DisplayName("createIrrigation() debe lanzar ResourceNotFoundException si el sector no existe")
    void createIrrigation_sectorNoExiste_lanzaResourceNotFoundException() {
        IrrigationRequest request = new IrrigationRequest();
        request.setSectorId(999);
        request.setEquipmentId(10);
        request.setStartDateTime(new Date());
        request.setEndDateTime(new Date());

        when(sectorRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> irrigationService.createIrrigation(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(irrigationRepository, never()).save(any());
    }

    @Test
    @DisplayName("createIrrigation() debe lanzar ResourceNotFoundException si el equipo no existe")
    void createIrrigation_equipoNoExiste_lanzaResourceNotFoundException() {
        IrrigationRequest request = new IrrigationRequest();
        request.setSectorId(100);
        request.setEquipmentId(999);
        request.setStartDateTime(new Date());
        request.setEndDateTime(new Date());

        when(sectorRepository.findById(100)).thenReturn(Optional.of(testSector));
        when(equipmentRepository.findById(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> irrigationService.createIrrigation(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(irrigationRepository, never()).save(any());
    }

    // ===== TESTS DE getIrrigationById() =====

    @Test
    @DisplayName("getIrrigationById() debe retornar el riego cuando existe")
    void getIrrigationById_irrigacionExistente_retornaIrrigacion() {
        when(irrigationRepository.findById(1000)).thenReturn(Optional.of(testIrrigation));

        Irrigation result = irrigationService.getIrrigationById(1000);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1000);
        assertThat(result.getWaterAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("getIrrigationById() debe lanzar ResourceNotFoundException si no existe")
    void getIrrigationById_irrigacionNoExiste_lanzaResourceNotFoundException() {
        when(irrigationRepository.findById(9999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> irrigationService.getIrrigationById(9999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ===== TESTS DE getIrrigationsBySector() =====

    @Test
    @DisplayName("getIrrigationsBySector() debe retornar la lista de riegos del sector")
    void getIrrigationsBySector_sectorValido_retornaListaRiegos() {
        when(sectorRepository.findByIdAndFarm_Id(100, 1)).thenReturn(Optional.of(testSector));
        when(irrigationRepository.findBySectorOrderByStartDatetimeDesc(testSector))
                .thenReturn(List.of(testIrrigation));

        List<Irrigation> result = irrigationService.getIrrigationsBySector(1, 100);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1000);
    }

    // ===== TESTS DE deleteIrrigation() =====

    @Test
    @DisplayName("deleteIrrigation() debe eliminar el riego y registrar auditoría")
    void deleteIrrigation_irrigacionExistente_eliminaYRegistraAuditoria() {
        when(irrigationRepository.findById(1000)).thenReturn(Optional.of(testIrrigation));
        doNothing().when(auditService).logChange(any(), any(), any(), any(), any(), any());

        irrigationService.deleteIrrigation(1000);

        verify(irrigationRepository, times(1)).delete(testIrrigation);
        verify(auditService, times(1)).logChange(any(), eq("DELETE"), any(), any(), any(), any());
    }
}
