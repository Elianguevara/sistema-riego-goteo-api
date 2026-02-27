package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AgronomicConfigDTO {

    @NotNull(message = "El coeficiente de lluvia efectiva no puede ser nulo")
    @DecimalMin(value = "0.0", message = "El coeficiente no puede ser menor a 0.0")
    @DecimalMax(value = "1.0", message = "El coeficiente no puede exceder 1.0")
    private Float effectiveRainCoefficient;

    @NotNull(message = "Las horas máximas de riego son obligatorias")
    @Min(value = 1, message = "El riego debe durar al menos 1 hora")
    @Max(value = 24, message = "No se puede regar más de 24 horas por día")
    private Integer maxIrrigationHoursPerDay;

    @NotNull(message = "El intervalo de riego es obligatorio")
    @Min(value = 1, message = "El intervalo mínimo debe ser 1 hora")
    private Integer minIrrigationIntervalHours;

    @NotNull(message = "El umbral de efectividad de precipitación es obligatorio")
    @DecimalMin(value = "0.0", message = "El umbral no puede ser negativo")
    private Float precipitationEffectivenessThresholdMm;

    @NotNull(message = "El umbral del reservorio es obligatorio")
    @Min(value = 0, message = "El mínimo porcentaje es 0")
    @Max(value = 100, message = "El máximo porcentaje es 100")
    private Integer reservoirLowThresholdPercent;
}
