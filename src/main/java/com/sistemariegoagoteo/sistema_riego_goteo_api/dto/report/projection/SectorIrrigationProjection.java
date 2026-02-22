package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.projection;

import java.math.BigDecimal;

public interface SectorIrrigationProjection {
    Integer getSectorId();

    String getSectorName();

    BigDecimal getWaterAmount();

    BigDecimal getHours();
}
