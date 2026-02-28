package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FertilizationRequest;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Fertilization;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.UnitOfMeasure;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FertilizationRepository;
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
class FertilizationServiceTest {

    @Mock
    private FertilizationRepository fertilizationRepository;
    @Mock
    private SectorRepository sectorRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private FertilizationService fertilizationService;

    private User authUser;
    private Sector sector;
    private Fertilization fertilization;
    private Date baseDate;

    @BeforeEach
    void setUp() {
        authUser = new User();
        authUser.setId(1L);
        authUser.setUsername("testuser");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(authUser, null, new java.util.ArrayList<>()));

        sector = new Sector();
        sector.setId(1);
        sector.setName("Sector 1");

        baseDate = new Date();

        fertilization = new Fertilization();
        fertilization.setId(1);
        fertilization.setSector(sector);
        fertilization.setDate(baseDate);
        fertilization.setFertilizerType("UREA");
        fertilization.setQuantity(new BigDecimal("50.0"));
        fertilization.setQuantityUnit(UnitOfMeasure.KG);
    }

    @Test
    void createFertilization_Success() {
        FertilizationRequest request = new FertilizationRequest();
        request.setSectorId(1);
        request.setDate(baseDate);
        request.setFertilizerType("NITK");
        request.setQuantityUnit(UnitOfMeasure.LITERS);
        when(sectorRepository.findById(1)).thenReturn(Optional.of(sector));
        when(fertilizationRepository.save(any(Fertilization.class))).thenAnswer(i -> {
            Fertilization f = i.getArgument(0);
            f.setId(2);
            return f;
        });

        Fertilization result = fertilizationService.createFertilization(request);

        assertNotNull(result);
        assertEquals(2, result.getId());
        assertEquals("NITK", result.getFertilizerType());
        verify(auditService).logChange(any(User.class), eq("CREATE"), eq("Fertilization"), eq("id"), isNull(), eq("2"));
    }

    @Test
    void updateFertilization_Success() {
        FertilizationRequest request = new FertilizationRequest();
        request.setDate(baseDate);
        request.setFertilizerType("UREA");
        request.setQuantity(new BigDecimal("60.0")); // Changed
        request.setQuantityUnit(UnitOfMeasure.KG);

        when(fertilizationRepository.findById(1)).thenReturn(Optional.of(fertilization));
        when(fertilizationRepository.save(any(Fertilization.class))).thenAnswer(i -> i.getArgument(0));

        Fertilization result = fertilizationService.updateFertilization(1, request);

        assertEquals(new BigDecimal("60.0"), result.getQuantity());
        verify(auditService, times(1)).logChange(any(User.class), eq("UPDATE"), eq("Fertilization"), anyString(),
                anyString(), anyString());
    }

    @Test
    void deleteFertilization_Success() {
        when(fertilizationRepository.findById(1)).thenReturn(Optional.of(fertilization));

        fertilizationService.deleteFertilization(1);

        verify(fertilizationRepository).delete(fertilization);
        verify(auditService).logChange(any(User.class), eq("DELETE"), eq("Fertilization"), eq("id"), eq("1"), isNull());
    }
}
