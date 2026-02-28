package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.WaterSourceRequest;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.WaterSource;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.WaterSourceRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Arrays;
import java.util.List;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WaterSourceServiceTest {

    @Mock
    private WaterSourceRepository waterSourceRepository;
    @Mock
    private FarmRepository farmRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private WaterSourceService waterSourceService;

    private User authUser;
    private Farm farm;
    private WaterSource waterSource;

    @BeforeEach
    void setUp() {
        authUser = new User();
        authUser.setId(1L);
        authUser.setUsername("testuser");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authUser, null, new java.util.ArrayList<>()));

        farm = new Farm();
        farm.setId(1);
        farm.setName("Finca Test");

        waterSource = new WaterSource();
        waterSource.setId(1);
        waterSource.setFarm(farm);
        waterSource.setType("POZO");
    }

    @Test
    void createWaterSource_Success() {
        WaterSourceRequest request = new WaterSourceRequest();
        request.setType("REPRESA");

        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(waterSourceRepository.findByTypeAndFarm("REPRESA", farm)).thenReturn(Optional.empty());
        when(waterSourceRepository.save(any(WaterSource.class))).thenAnswer(i -> {
            WaterSource ws = i.getArgument(0);
            ws.setId(2);
            return ws;
        });

        WaterSource result = waterSourceService.createWaterSource(1, request);

        assertNotNull(result);
        assertEquals("REPRESA", result.getType());
        assertEquals(2, result.getId());
        verify(auditService).logChange(any(User.class), eq("CREATE"), eq("WaterSource"), eq("type"), isNull(),
                eq("REPRESA"));
        verify(waterSourceRepository).save(any(WaterSource.class));
    }

    @Test
    void createWaterSource_DuplicateType_ThrowsException() {
        WaterSourceRequest request = new WaterSourceRequest();
        request.setType("POZO");

        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(waterSourceRepository.findByTypeAndFarm("POZO", farm)).thenReturn(Optional.of(waterSource));

        assertThrows(IllegalArgumentException.class, () -> waterSourceService.createWaterSource(1, request));
        verify(waterSourceRepository, never()).save(any(WaterSource.class));
    }

    @Test
    void updateWaterSource_Success() {
        WaterSourceRequest request = new WaterSourceRequest();
        request.setType("CANAL");

        when(waterSourceRepository.findById(1)).thenReturn(Optional.of(waterSource));
        when(waterSourceRepository.findByTypeAndFarm("CANAL", farm)).thenReturn(Optional.empty());
        when(waterSourceRepository.save(any(WaterSource.class))).thenAnswer(i -> i.getArgument(0));

        WaterSource result = waterSourceService.updateWaterSource(1, request);

        assertEquals("CANAL", result.getType());
        verify(auditService).logChange(any(User.class), eq("UPDATE"), eq("WaterSource"), eq("type"), eq("POZO"),
                eq("CANAL"));
    }

    @Test
    void deleteWaterSource_Success() {
        when(waterSourceRepository.findById(1)).thenReturn(Optional.of(waterSource));

        waterSourceService.deleteWaterSource(1);

        verify(waterSourceRepository).delete(waterSource);
        verify(auditService).logChange(any(User.class), eq("DELETE"), eq("WaterSource"), eq("id"), eq("1"), isNull());
    }

    @Test
    void getWaterSourcesByFarm_Success() {
        when(farmRepository.findById(1)).thenReturn(Optional.of(farm));
        when(waterSourceRepository.findByFarmOrderByTypeAsc(farm)).thenReturn(Arrays.asList(waterSource));

        List<WaterSource> results = waterSourceService.getWaterSourcesByFarm(1);

        assertEquals(1, results.size());
        assertEquals("POZO", results.get(0).getType());
    }
}
