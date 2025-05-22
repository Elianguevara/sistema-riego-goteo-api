package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PrecipitationRequest {

    @NotNull(message = "La fecha de la precipitación es requerida.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date precipitationDate;

    @NotNull(message = "Los milímetros de lluvia total son requeridos.")
    @PositiveOrZero(message = "Los milímetros de lluvia deben ser un valor positivo o cero.")
    private BigDecimal mmRain; // Lluvia total

    // farmId vendrá de la ruta del controlador
    // mmEffectiveRain se calculará en el servicio
}