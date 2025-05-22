package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FarmRequest {
    @NotBlank(message = "El nombre de la finca no puede estar vacío.")
    @Size(max = 100, message = "El nombre de la finca no puede exceder los 100 caracteres.")
    private String name;

    @Size(max = 255, message = "La ubicación no puede exceder los 255 caracteres.")
    private String location;

    @NotNull(message = "La capacidad del reservorio es requerida.")
    @PositiveOrZero(message = "La capacidad del reservorio debe ser un valor positivo o cero.")
    private BigDecimal reservoirCapacity;

    @NotNull(message = "El tamaño de la finca es requerido.")
    @PositiveOrZero(message = "El tamaño de la finca debe ser un valor positivo o cero.")
    private BigDecimal farmSize;
}