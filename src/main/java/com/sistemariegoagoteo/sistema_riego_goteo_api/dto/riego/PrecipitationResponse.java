package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Precipitation;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
public class PrecipitationResponse {
    private Integer id;

    private Integer farmId;
    private String farmName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date precipitationDate;
    private BigDecimal mmRain; // Lluvia total
    private BigDecimal mmEffectiveRain; // Lluvia efectiva

    public PrecipitationResponse(Precipitation precipitation) {
        this.id = precipitation.getId();
        if (precipitation.getFarm() != null) {
            this.farmId = precipitation.getFarm().getId();
            this.farmName = precipitation.getFarm().getName();
        }
        this.precipitationDate = precipitation.getPrecipitationDate();
        this.mmRain = precipitation.getMmRain();
        this.mmEffectiveRain = precipitation.getMmEffectiveRain();
    }
}