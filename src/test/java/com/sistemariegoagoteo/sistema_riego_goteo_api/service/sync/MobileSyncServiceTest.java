package com.sistemariegoagoteo.sistema_riego_goteo_api.service.sync;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncBatchRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncItem;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MobileSyncServiceTest {

    @Mock
    private IrrigationRepository irrigationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SectorRepository sectorRepository;
    @Mock
    private IrrigationEquipmentRepository equipmentRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private MobileSyncService mobileSyncService;

    private User syncUser;
    private Farm farm;
    private Sector sector;
    private IrrigationEquipment equipment;

    @BeforeEach
    void setUp() {
        syncUser = new User();
        syncUser.setId(1L);
        syncUser.setUsername("syncuser");

        farm = new Farm();
        farm.setId(1);

        sector = new Sector();
        sector.setId(1);
        sector.setFarm(farm);

        equipment = new IrrigationEquipment();
        equipment.setId(1);
        equipment.setFarm(farm);
        equipment.setMeasuredFlow(new BigDecimal("5.0")); // 5 m3/h
    }

    @Test
    void processIrrigationBatch_SuccessCreate() {
        // Arrange
        IrrigationSyncItem item1 = new IrrigationSyncItem();
        item1.setLocalId("local-1");
        item1.setSectorId(1);
        item1.setEquipmentId(1);
        item1.setStartDatetime(LocalDateTime.of(2026, 2, 27, 8, 0));
        item1.setEndDatetime(LocalDateTime.of(2026, 2, 27, 10, 0)); // 2 horas de riego

        IrrigationSyncBatchRequest request = new IrrigationSyncBatchRequest();
        request.setIrrigations(Arrays.asList(item1));

        when(userRepository.findByUsername("syncuser")).thenReturn(Optional.of(syncUser));
        when(sectorRepository.findAllById(any())).thenReturn(Arrays.asList(sector));
        when(equipmentRepository.findAllById(any())).thenReturn(Arrays.asList(equipment));
        when(irrigationRepository.findByLocalMobileId("local-1")).thenReturn(Optional.empty());

        when(irrigationRepository.saveAll(anyList())).thenAnswer(i -> {
            List<Irrigation> savedList = i.getArgument(0);
            savedList.get(0).setId(100);
            return savedList;
        });

        // Act
        IrrigationSyncResponse response = mobileSyncService.processIrrigationBatch("syncuser", request);

        // Assert
        assertEquals(1, response.getTotalItems());
        assertEquals(1, response.getSuccessfulItems());
        assertEquals(0, response.getFailedItems());
        assertEquals(100, response.getResults().get(0).getServerId());

        verify(auditService).logChange(eq(syncUser), eq("SYNC_CREATE"), eq("Irrigation"), anyString(), isNull(),
                eq("100"));
    }

    @Test
    void processIrrigationBatch_EquipmentMismatch() {
        // Arrange
        Farm anotherFarm = new Farm();
        anotherFarm.setId(2);

        IrrigationEquipment invalidEquipment = new IrrigationEquipment();
        invalidEquipment.setId(2);
        invalidEquipment.setFarm(anotherFarm); // Pertenece a otra finca

        IrrigationSyncItem item1 = new IrrigationSyncItem();
        item1.setLocalId("local-invalid");
        item1.setSectorId(1);
        item1.setEquipmentId(2);

        IrrigationSyncBatchRequest request = new IrrigationSyncBatchRequest();
        request.setIrrigations(Arrays.asList(item1));

        when(userRepository.findByUsername("syncuser")).thenReturn(Optional.of(syncUser));
        when(sectorRepository.findAllById(any())).thenReturn(Arrays.asList(sector));
        when(equipmentRepository.findAllById(any())).thenReturn(Arrays.asList(invalidEquipment));

        // Act
        IrrigationSyncResponse response = mobileSyncService.processIrrigationBatch("syncuser", request);

        // Assert
        assertEquals(1, response.getTotalItems());
        assertEquals(0, response.getSuccessfulItems());
        assertEquals(1, response.getFailedItems());
        assertFalse(response.getResults().get(0).isSuccess());
        assertTrue(response.getResults().get(0).getMessage().contains("no pertenece a la finca del sector"));
    }

    @Test
    void processIrrigationBatch_EmptyList() {
        IrrigationSyncBatchRequest emptyRequest = new IrrigationSyncBatchRequest();
        emptyRequest.setIrrigations(java.util.Collections.emptyList());

        when(userRepository.findByUsername("syncuser")).thenReturn(Optional.of(syncUser));

        IrrigationSyncResponse response = mobileSyncService.processIrrigationBatch("syncuser", emptyRequest);

        assertEquals(0, response.getTotalItems());
        assertEquals(0, response.getSuccessfulItems());
    }
}
