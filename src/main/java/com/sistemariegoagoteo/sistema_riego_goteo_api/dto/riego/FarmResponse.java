package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
public class FarmResponse {
    private Integer id;
    private String name;
    private String location;
    private BigDecimal reservoirCapacity;
    private BigDecimal farmSize;

    public FarmResponse(Farm farm) {
        this.id = farm.getId();
        this.name = farm.getName();
        this.location = farm.getLocation();
        this.reservoirCapacity = farm.getReservoirCapacity();
        this.farmSize = farm.getFarmSize();
    }
}