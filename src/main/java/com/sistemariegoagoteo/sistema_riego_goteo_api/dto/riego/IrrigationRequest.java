package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class IrrigationRequest {

    @NotNull(message = "El ID del sector es requerido.")
    private Integer sectorId;

    @NotNull(message = "El ID del equipo de irrigación es requerido.")
    private Integer equipmentId;

    @NotNull(message = "La fecha y hora de inicio del riego son requeridas.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startDateTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endDateTime;

    /**
     * Opcional. Si se envía un valor, se interpreta como la lectura manual del
     * caudalímetro y se almacena tal cual (isManualWaterVolume = true).
     * Si es null, el backend calcula automáticamente: (horas × caudal del equipo).
     */
    @Nullable
    @PositiveOrZero(message = "La cantidad de agua debe ser un valor positivo o cero.")
    private BigDecimal waterAmount;

    @PositiveOrZero(message = "Las horas de riego deben ser un valor positivo o cero.")
    private BigDecimal irrigationHours;
}