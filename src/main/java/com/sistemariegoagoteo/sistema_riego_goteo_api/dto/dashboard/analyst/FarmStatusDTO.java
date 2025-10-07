package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.analyst;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class FarmStatusDTO {
    private Integer farmId;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String status; // "OK", "ALERTA", etc.
    private int activeAlertsCount;

    public FarmStatusDTO(Farm farm) {
        this.farmId = farm.getId();
        this.name = farm.getName();
        this.latitude = farm.getLatitude();
        this.longitude = farm.getLongitude();
    }
}