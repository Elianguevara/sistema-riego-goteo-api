package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.ReservoirTurn;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class ReservoirTurnResponse {
    private Integer id;

    private Integer waterSourceId;
    private String waterSourceType; // Tipo de la fuente de agua

    private Integer farmId; // Derivado de la fuente de agua
    private String farmName; // Derivado de la fuente de agua

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date startDatetime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date endDatetime;

    public ReservoirTurnResponse(ReservoirTurn reservoirTurn) {
        this.id = reservoirTurn.getId();
        if (reservoirTurn.getWaterSource() != null) {
            this.waterSourceId = reservoirTurn.getWaterSource().getId();
            this.waterSourceType = reservoirTurn.getWaterSource().getType();
            if (reservoirTurn.getWaterSource().getFarm() != null) {
                this.farmId = reservoirTurn.getWaterSource().getFarm().getId();
                this.farmName = reservoirTurn.getWaterSource().getFarm().getName();
            }
        }
        this.startDatetime = reservoirTurn.getStartDatetime();
        this.endDatetime = reservoirTurn.getEndDatetime();
    }
}