package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class ReservoirTurnRequest {

    @NotNull(message = "La fecha y hora de inicio del turno son requeridas.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date startDatetime;

    @NotNull(message = "La fecha y hora de finalización del turno son requeridas.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date endDatetime;

    // waterSourceId vendrá de la ruta del controlador
}