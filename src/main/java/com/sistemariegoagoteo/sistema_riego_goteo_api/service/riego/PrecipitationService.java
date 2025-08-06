package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.PrecipitationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Precipitation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User; // <-- IMPORTAR
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.PrecipitationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder; // <-- IMPORTAR
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects; // <-- IMPORTAR

@Service
@RequiredArgsConstructor
@Slf4j
public class PrecipitationService {

    private final PrecipitationRepository precipitationRepository;
    private final FarmRepository farmRepository;
    private final AuditService auditService;

    private static final BigDecimal FIVE_MM = new BigDecimal("5.00");
    private static final BigDecimal EFFECTIVE_RAIN_FACTOR = new BigDecimal("0.75");

    @Transactional
    public Precipitation createPrecipitation(Integer farmId, PrecipitationRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        Precipitation precipitation = new Precipitation();
        precipitation.setFarm(farm);
        precipitation.setPrecipitationDate(request.getPrecipitationDate());
        precipitation.setMmRain(request.getMmRain().setScale(2, RoundingMode.HALF_UP));
        precipitation.setMmEffectiveRain(calculateEffectiveRain(precipitation.getMmRain()));

        Precipitation savedPrecipitation = precipitationRepository.save(precipitation);

        // --- AUDITORÍA DE CREACIÓN ---
        auditService.logChange(currentUser, "CREATE", Precipitation.class.getSimpleName(), "mmRain", null, savedPrecipitation.getMmRain().toString());

        log.info("Registrando precipitación para finca ID {} en fecha {}: {}mm total, {}mm efectiva",
                farmId, request.getPrecipitationDate(), precipitation.getMmRain(), precipitation.getMmEffectiveRain());
        return savedPrecipitation;
    }

    @Transactional
    public Precipitation updatePrecipitation(Integer precipitationId, PrecipitationRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Precipitation precipitation = getPrecipitationById(precipitationId);

        // --- AUDITORÍA DE ACTUALIZACIÓN ---
        if (!Objects.equals(precipitation.getPrecipitationDate(), request.getPrecipitationDate())) {
            auditService.logChange(currentUser, "UPDATE", Precipitation.class.getSimpleName(), "precipitationDate", Objects.toString(precipitation.getPrecipitationDate(), null), Objects.toString(request.getPrecipitationDate(), null));
        }
        if (precipitation.getMmRain().compareTo(request.getMmRain()) != 0) {
            auditService.logChange(currentUser, "UPDATE", Precipitation.class.getSimpleName(), "mmRain", precipitation.getMmRain().toString(), request.getMmRain().toString());
        }

        precipitation.setPrecipitationDate(request.getPrecipitationDate());
        precipitation.setMmRain(request.getMmRain().setScale(2, RoundingMode.HALF_UP));
        precipitation.setMmEffectiveRain(calculateEffectiveRain(precipitation.getMmRain()));

        log.info("Actualizando precipitación ID {}: {}mm total, {}mm efectiva",
                precipitationId, precipitation.getMmRain(), precipitation.getMmEffectiveRain());
        return precipitationRepository.save(precipitation);
    }

    @Transactional
    public void deletePrecipitation(Integer precipitationId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Precipitation precipitation = getPrecipitationById(precipitationId);

        // --- AUDITORÍA DE BORRADO ---
        auditService.logChange(currentUser, "DELETE", Precipitation.class.getSimpleName(), "id", precipitation.getId().toString(), null);

        log.warn("Eliminando precipitación ID {}", precipitationId);
        precipitationRepository.delete(precipitation);
    }

    // --- MÉTODOS GET Y DE CÁLCULO (SIN CAMBIOS) ---

    @Transactional(readOnly = true)
    public List<Precipitation> getPrecipitationsByFarm(Integer farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));
        return precipitationRepository.findByFarmOrderByPrecipitationDateDesc(farm);
    }

    @Transactional(readOnly = true)
    public Precipitation getPrecipitationById(Integer precipitationId) {
        return precipitationRepository.findById(precipitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Precipitation", "id", precipitationId));
    }

    private BigDecimal calculateEffectiveRain(BigDecimal mmRainTotal) {
        if (mmRainTotal == null || mmRainTotal.compareTo(FIVE_MM) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal effectiveRain = mmRainTotal.subtract(FIVE_MM).multiply(EFFECTIVE_RAIN_FACTOR);
        return effectiveRain.setScale(2, RoundingMode.HALF_UP);
    }
}