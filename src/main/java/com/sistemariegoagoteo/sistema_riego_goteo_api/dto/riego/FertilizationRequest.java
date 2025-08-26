package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.UnitOfMeasure; // <-- Importar el nuevo Enum
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class FertilizationRequest {

    @NotNull(message = "El ID del sector es requerido.")
    private Integer sectorId;

    @NotNull(message = "La fecha de la fertilización es requerida.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date date;

    @NotBlank(message = "El tipo de fertilizante no puede estar vacío.")
    @Size(max = 100, message = "El tipo de fertilizante no puede exceder los 100 caracteres.")
    private String fertilizerType;

    // --- CAMPOS MODIFICADOS ---
    @NotNull(message = "La cantidad es requerida.")
    @Positive(message = "La cantidad debe ser un valor positivo.")
    private BigDecimal quantity; // Reemplaza a litersApplied

    @NotNull(message = "La unidad de medida es requerida (KG o LITERS).")
    private UnitOfMeasure quantityUnit; // Campo nuevo para la unidad
}