package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class IrrigationRequest {

    @NotNull(message = "El ID del equipo de irrigación es requerido.")
    private Integer equipmentId;

    @NotNull(message = "La fecha y hora de inicio del riego son requeridas.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date startDatetime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date endDatetime; // Opcional al inicio, se puede actualizar después

    @PositiveOrZero(message = "La cantidad de agua debe ser un valor positivo o cero.")
    private BigDecimal waterAmount; // Opcional, podría calcularse o ingresarse manualmente

    @PositiveOrZero(message = "Las horas de riego deben ser un valor positivo o cero.")
    private BigDecimal irrigationHours; // Opcional, podría calcularse

    // El sectorId vendrá de la ruta del controlador
}