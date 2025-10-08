package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IrrigationSectorSummaryDTO {
    private Integer sectorId;
    private String sectorName;
    private BigDecimal totalWaterAmount;
    private BigDecimal totalIrrigationHours;
}