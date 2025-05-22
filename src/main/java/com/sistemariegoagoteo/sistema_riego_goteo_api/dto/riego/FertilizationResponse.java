package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Fertilization;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
public class FertilizationResponse {
    private Integer id;

    private Integer sectorId;
    private String sectorName;

    private Integer farmId; // Derivado del sector
    private String farmName; // Derivado del sector

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date date;
    private String fertilizerType;
    private BigDecimal litersApplied;

    public FertilizationResponse(Fertilization fertilization) {
        this.id = fertilization.getId();
        if (fertilization.getSector() != null) {
            this.sectorId = fertilization.getSector().getId();
            this.sectorName = fertilization.getSector().getName();
            if (fertilization.getSector().getFarm() != null) {
                this.farmId = fertilization.getSector().getFarm().getId();
                this.farmName = fertilization.getSector().getFarm().getName();
            }
        }
        this.date = fertilization.getDate();
        this.fertilizerType = fertilization.getFertilizerType();
        this.litersApplied = fertilization.getLitersApplied();
    }
}