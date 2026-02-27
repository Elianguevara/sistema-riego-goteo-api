package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.HumiditySensorRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.HumiditySensor;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.HumiditySensorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HumiditySensorServiceTest {

    @Mock
    private HumiditySensorRepository humiditySensorRepository;
    @Mock
    private SectorRepository sectorRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private HumiditySensorService humiditySensorService;

    private User authUser;
    private Farm farm;
    private Sector sector;
    private HumiditySensor sensor;
    private Date baseDate;

    @BeforeEach
    void setUp() {
        authUser = new User();
        authUser.setId(1L);
        authUser.setUsername("testuser");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authUser, null, new java.util.ArrayList<>()));

        farm = new Farm();
        farm.setId(1);

        sector = new Sector();
        sector.setId(1);
        sector.setFarm(farm);

        baseDate = new Date();

        sensor = new HumiditySensor();
        sensor.setId(1);
        sensor.setSector(sector);
        sensor.setSensorType("TENSIOMETRO");
        sensor.setHumidityLevel(new BigDecimal("45.5"));
        sensor.setMeasurementDatetime(baseDate);
    }

    @Test
    void createHumiditySensor_Success() {
        HumiditySensorRequest request = new HumiditySensorRequest();
        request.setSensorType("SONDA");
        request.setHumidityLevel(new BigDecimal("60.2"));
        request.setMeasurementDatetime(new Date());

        when(sectorRepository.findByIdAndFarm_Id(1, 1)).thenReturn(Optional.of(sector));
        when(humiditySensorRepository.save(any(HumiditySensor.class))).thenAnswer(i -> {
            HumiditySensor h = i.getArgument(0);
            h.setId(2);
            return h;
        });

        HumiditySensor result = humiditySensorService.createHumiditySensor(1, 1, request);

        assertEquals(2, result.getId());
        assertEquals("SONDA", result.getSensorType());
        verify(auditService).logChange(eq(authUser), eq("CREATE"), eq("HumiditySensor"), eq("sensorType"), isNull(),
                eq("SONDA"));
    }

    @Test
    void updateHumiditySensor_Success() {
        HumiditySensorRequest request = new HumiditySensorRequest();
        request.setSensorType("SONDA"); // Changed
        request.setHumidityLevel(new BigDecimal("45.5")); // Same
        request.setMeasurementDatetime(sensor.getMeasurementDatetime()); // Same

        when(humiditySensorRepository.findById(1)).thenReturn(Optional.of(sensor));
        when(humiditySensorRepository.save(any(HumiditySensor.class))).thenAnswer(i -> i.getArgument(0));

        HumiditySensor result = humiditySensorService.updateHumiditySensor(1, request);

        assertEquals("SONDA", result.getSensorType());
        verify(auditService, times(1)).logChange(eq(authUser), eq("UPDATE"), eq("HumiditySensor"), anyString(),
                anyString(), anyString());
    }

    @Test
    void deleteHumiditySensor_Success() {
        when(humiditySensorRepository.findById(1)).thenReturn(Optional.of(sensor));

        humiditySensorService.deleteHumiditySensor(1);

        verify(humiditySensorRepository).delete(sensor);
        verify(auditService).logChange(eq(authUser), eq("DELETE"), eq("HumiditySensor"), eq("id"), eq("1"), isNull());
    }
}
