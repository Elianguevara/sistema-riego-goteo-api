package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.EnergyConsumption;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
public class EnergyConsumptionResponse {
    private Integer id;

    private Integer farmId;
    private String farmName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date consumptionDate;
    private BigDecimal kwhConsumed;
    private String energyType; // Ej: Red Eléctrica, Generador Diesel, Solar

    public EnergyConsumptionResponse(EnergyConsumption energyConsumption) {
        this.id = energyConsumption.getId();
        if (energyConsumption.getFarm() != null) {
            this.farmId = energyConsumption.getFarm().getId();
            this.farmName = energyConsumption.getFarm().getName();
        }
        this.consumptionDate = energyConsumption.getConsumptionDate();
        this.kwhConsumed = energyConsumption.getKwhConsumed();
        this.energyType = energyConsumption.getEnergyType();
    }
}