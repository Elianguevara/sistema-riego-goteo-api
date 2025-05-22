package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.WaterSource;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WaterSourceResponse {
    private Integer id;
    private String type; // Ej: Pozo, Represa, Canal, Red pública
    private Integer farmId;
    private String farmName;

    public WaterSourceResponse(WaterSource waterSource) {
        this.id = waterSource.getId();
        this.type = waterSource.getType();
        if (waterSource.getFarm() != null) {
            this.farmId = waterSource.getFarm().getId();
            this.farmName = waterSource.getFarm().getName();
        }
    }
}