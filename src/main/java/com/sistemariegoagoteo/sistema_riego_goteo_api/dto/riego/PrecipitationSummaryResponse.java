package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrecipitationSummaryResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal totalMmRain;
    private BigDecimal totalMmEffectiveRain;
}