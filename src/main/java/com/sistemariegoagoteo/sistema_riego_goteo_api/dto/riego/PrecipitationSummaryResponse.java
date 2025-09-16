package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PrecipitationSummaryResponse {
    private Date startDate;
    private Date endDate;
    private BigDecimal totalMmRain;
    private BigDecimal totalMmEffectiveRain;
}