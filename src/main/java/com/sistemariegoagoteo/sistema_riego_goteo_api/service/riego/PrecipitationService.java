package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.PrecipitationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Precipitation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.PrecipitationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        Precipitation precipitation = new Precipitation();
        precipitation.setFarm(farm);
        precipitation.setPrecipitationDate(request.getPrecipitationDate());
        precipitation.setMmRain(request.getMmRain().setScale(2, RoundingMode.HALF_UP));
        precipitation.setMmEffectiveRain(calculateEffectiveRain(precipitation.getMmRain()));

        log.info("Registrando precipitación para finca ID {} en fecha {}: {}mm total, {}mm efectiva",
                farmId, request.getPrecipitationDate(), precipitation.getMmRain(), precipitation.getMmEffectiveRain());
        return precipitationRepository.save(precipitation);
    }

    @Transactional(readOnly = true)
    public List<Precipitation> getPrecipitationsByFarm(Integer farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));
        // Asumiendo que PrecipitationRepository tiene findByFarmOrderByPrecipitationDateDesc
        return precipitationRepository.findByFarmOrderByPrecipitationDateDesc(farm);
    }

    @Transactional(readOnly = true)
    public Precipitation getPrecipitationById(Integer precipitationId) {
        return precipitationRepository.findById(precipitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Precipitation", "id", precipitationId));
    }

    @Transactional
    public Precipitation updatePrecipitation(Integer precipitationId, PrecipitationRequest request) {
        Precipitation precipitation = getPrecipitationById(precipitationId); // Valida existencia

        // La finca de un registro de precipitación no debería cambiar.
        // Si se cambia la fecha o los mmRain, se recalcula la lluvia efectiva.
        precipitation.setPrecipitationDate(request.getPrecipitationDate());
        precipitation.setMmRain(request.getMmRain().setScale(2, RoundingMode.HALF_UP));
        precipitation.setMmEffectiveRain(calculateEffectiveRain(precipitation.getMmRain()));

        log.info("Actualizando precipitación ID {}: {}mm total, {}mm efectiva",
                precipitationId, precipitation.getMmRain(), precipitation.getMmEffectiveRain());
        return precipitationRepository.save(precipitation);
    }

    @Transactional
    public void deletePrecipitation(Integer precipitationId) {
        Precipitation precipitation = getPrecipitationById(precipitationId);
        log.warn("Eliminando precipitación ID {}", precipitationId);
        precipitationRepository.delete(precipitation);
    }

    private BigDecimal calculateEffectiveRain(BigDecimal mmRainTotal) {
        if (mmRainTotal == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        // Si Lluvias < 5 mm se considera PE nula
        if (mmRainTotal.compareTo(FIVE_MM) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        // PE = 0.75 x (lluvia caída - 5 mm)
        BigDecimal effectiveRain = mmRainTotal.subtract(FIVE_MM).multiply(EFFECTIVE_RAIN_FACTOR);
        return effectiveRain.setScale(2, RoundingMode.HALF_UP);
    }
}