package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class FertilizationRequest {

    @NotNull(message = "La fecha de la fertilización es requerida.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date date;

    @NotBlank(message = "El tipo de fertilizante no puede estar vacío.")
    @Size(max = 100, message = "El tipo de fertilizante no puede exceder los 100 caracteres.")
    private String fertilizerType;

    @NotNull(message = "Los litros aplicados son requeridos.")
    @Positive(message = "Los litros aplicados deben ser un valor positivo.")
    private BigDecimal litersApplied;

    // sectorId vendrá de la ruta del controlador
}