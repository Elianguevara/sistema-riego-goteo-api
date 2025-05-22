package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class EnergyConsumptionRequest {

    @NotNull(message = "La fecha del consumo es requerida.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date consumptionDate;

    @NotNull(message = "Los kWh consumidos son requeridos.")
    @PositiveOrZero(message = "Los kWh consumidos deben ser un valor positivo o cero.")
    private BigDecimal kwhConsumed;

    @NotBlank(message = "El tipo de energía no puede estar vacío.")
    @Size(max = 50, message = "El tipo de energía no puede exceder los 50 caracteres.")
    private String energyType;

    // farmId vendrá de la ruta del controlador
}