package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Item individual de sincronización de fertilización desde el móvil.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FertilizationSyncItem {

    @NotNull(message = "El ID local del móvil es requerido.")
    private String localId;

    @NotNull(message = "El ID del sector es requerido.")
    private Integer sectorId;

    @NotNull(message = "La fecha de fertilización es requerida.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    private String fertilizerType;

    @NotNull(message = "La cantidad es requerida.")
    @PositiveOrZero(message = "La cantidad debe ser un valor positivo o cero.")
    private BigDecimal quantity;

    /** "KG" o "LITERS" — valores del enum UnitOfMeasure */
    @NotNull(message = "La unidad de medida es requerida.")
    private String quantityUnit;
}
