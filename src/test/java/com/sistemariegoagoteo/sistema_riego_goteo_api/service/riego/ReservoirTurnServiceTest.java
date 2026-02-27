package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.ReservoirTurnRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.ReservoirTurn;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.WaterSource;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.ReservoirTurnRepository;
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

import java.util.Date;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservoirTurnServiceTest {

    @Mock
    private ReservoirTurnRepository reservoirTurnRepository;
    @Mock
    private WaterSourceRepository waterSourceRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private ReservoirTurnService reservoirTurnService;

    private User authUser;
    private Farm farm;
    private WaterSource waterSource;
    private ReservoirTurn reservoirTurn;
    private Date baseTime;
    private Date baseTimePlus1H;
    private Date baseTimePlus2H;
    private Date baseTimePlus4H;
    private Date baseTimePlus5H;
    private Date baseTimePlus24H;
    private Date baseTimePlus26H;

    @BeforeEach
    void setUp() {
        authUser = new User();
        authUser.setId(1L);
        authUser.setUsername("testuser");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authUser, null, new java.util.ArrayList<>()));

        farm = new Farm();
        farm.setId(1);

        waterSource = new WaterSource();
        waterSource.setId(1);
        waterSource.setFarm(farm);

        baseTime = new Date();
        long t = baseTime.getTime();
        baseTimePlus1H = new Date(t + 3600000);
        baseTimePlus2H = new Date(t + 7200000);
        baseTimePlus4H = new Date(t + 14400000);
        baseTimePlus5H = new Date(t + 18000000);
        baseTimePlus24H = new Date(t + 86400000);
        baseTimePlus26H = new Date(t + 93600000);

        reservoirTurn = new ReservoirTurn();
        reservoirTurn.setId(1);
        reservoirTurn.setWaterSource(waterSource);
        reservoirTurn.setStartDatetime(baseTime);
        reservoirTurn.setEndDatetime(baseTimePlus4H);
    }

    @Test
    void createReservoirTurn_Success() {
        ReservoirTurnRequest request = new ReservoirTurnRequest();
        request.setStartDatetime(baseTimePlus24H);
        request.setEndDatetime(baseTimePlus26H);

        when(waterSourceRepository.findById(1)).thenReturn(Optional.of(waterSource));
        when(reservoirTurnRepository.save(any(ReservoirTurn.class))).thenAnswer(i -> {
            ReservoirTurn rt = i.getArgument(0);
            rt.setId(2);
            return rt;
        });

        ReservoirTurn result = reservoirTurnService.createReservoirTurn(1, 1, request);

        assertNotNull(result);
        assertEquals(2, result.getId());
        verify(auditService).logChange(eq(authUser), eq("CREATE"), eq("ReservoirTurn"), eq("id"), isNull(), eq("2"));
    }

    @Test
    void createReservoirTurn_WrongFarm_ThrowsException() {
        ReservoirTurnRequest request = new ReservoirTurnRequest();
        when(waterSourceRepository.findById(1)).thenReturn(Optional.of(waterSource));

        assertThrows(IllegalArgumentException.class, () -> reservoirTurnService.createReservoirTurn(99, 1, request));
        verify(reservoirTurnRepository, never()).save(any());
    }

    @Test
    void createReservoirTurn_InvalidDates_ThrowsException() {
        ReservoirTurnRequest request = new ReservoirTurnRequest();
        request.setStartDatetime(baseTimePlus1H);
        request.setEndDatetime(baseTime); // Fin antes de inicio

        when(waterSourceRepository.findById(1)).thenReturn(Optional.of(waterSource));

        assertThrows(IllegalArgumentException.class, () -> reservoirTurnService.createReservoirTurn(1, 1, request));
        verify(reservoirTurnRepository, never()).save(any());
    }

    @Test
    void updateReservoirTurn_Success() {
        ReservoirTurnRequest request = new ReservoirTurnRequest();
        request.setStartDatetime(baseTimePlus1H);
        request.setEndDatetime(baseTimePlus5H);

        when(reservoirTurnRepository.findById(1)).thenReturn(Optional.of(reservoirTurn));
        when(reservoirTurnRepository.save(any(ReservoirTurn.class))).thenAnswer(i -> i.getArgument(0));

        ReservoirTurn result = reservoirTurnService.updateReservoirTurn(1, request);

        assertEquals(baseTimePlus1H, result.getStartDatetime());
        verify(auditService, times(2)).logChange(eq(authUser), eq("UPDATE"), eq("ReservoirTurn"), anyString(),
                anyString(), anyString());
    }

    @Test
    void deleteReservoirTurn_Success() {
        when(reservoirTurnRepository.findById(1)).thenReturn(Optional.of(reservoirTurn));

        reservoirTurnService.deleteReservoirTurn(1);

        verify(reservoirTurnRepository).delete(reservoirTurn);
        verify(auditService).logChange(eq(authUser), eq("DELETE"), eq("ReservoirTurn"), eq("id"), eq("1"), isNull());
    }
}
