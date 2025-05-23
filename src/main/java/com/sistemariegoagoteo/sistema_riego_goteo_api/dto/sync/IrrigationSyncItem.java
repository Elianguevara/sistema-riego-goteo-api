package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IrrigationSyncItem {

    @NotNull(message = "El ID local del móvil es requerido para el seguimiento.")
    private String localId; // ID único generado por el móvil (ej. UUID)

    @NotNull(message = "El ID del sector es requerido.")
    private Integer sectorId;

    @NotNull(message = "El ID del equipo de irrigación es requerido.")
    private Integer equipmentId;

    @NotNull(message = "La fecha y hora de inicio del riego son requeridas.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date startDatetime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date endDatetime; // Puede ser nulo si el riego aún no ha terminado

    @PositiveOrZero(message = "La cantidad de agua debe ser un valor positivo o cero.")
    private BigDecimal waterAmount;

    @PositiveOrZero(message = "Las horas de riego deben ser un valor positivo o cero.")
    private BigDecimal irrigationHours;
}