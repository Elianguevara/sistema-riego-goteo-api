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
 * Item individual de sincronización de mantenimiento de equipo desde el móvil.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceSyncItem {

    @NotNull(message = "El ID local del móvil es requerido.")
    private String localId;

    @NotNull(message = "El ID del equipo es requerido.")
    private Integer equipmentId;

    @NotNull(message = "La fecha de mantenimiento es requerida.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate date;

    @NotNull(message = "La descripción es requerida.")
    private String description;

    @PositiveOrZero(message = "Las horas de trabajo deben ser un valor positivo o cero.")
    private BigDecimal workHours;
}
