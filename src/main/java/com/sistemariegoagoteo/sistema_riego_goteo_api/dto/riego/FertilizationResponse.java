package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Fertilization;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.UnitOfMeasure; // <-- Importar el nuevo Enum
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
    private Integer farmId;
    private String farmName;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date date;
    private String fertilizerType;

    // --- CAMPOS MODIFICADOS ---
    private BigDecimal quantity; // Reemplaza a litersApplied
    private UnitOfMeasure quantityUnit; // Campo nuevo para la unidad

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

        // --- LÓGICA DE MAPEO ACTUALIZADA ---
        this.quantity = fertilization.getQuantity();
        this.quantityUnit = fertilization.getQuantityUnit();
    }
}