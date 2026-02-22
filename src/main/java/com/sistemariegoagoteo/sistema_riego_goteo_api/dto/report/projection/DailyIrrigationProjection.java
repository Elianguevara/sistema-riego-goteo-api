package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyIrrigationProjection {
    LocalDate getIrrigationDate();

    BigDecimal getWaterAmount();

    BigDecimal getHours();
}
