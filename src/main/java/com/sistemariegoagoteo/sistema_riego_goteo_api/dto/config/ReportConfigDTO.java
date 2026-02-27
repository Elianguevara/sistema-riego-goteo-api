package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReportConfigDTO {

    @NotNull(message = "Los días de retención de reportes son obligatorios")
    @Min(value = 1, message = "El tiempo mínimo de retención es 1 día")
    @Max(value = 365, message = "El tiempo máximo de retención es de 365 días")
    private Integer reportRetentionDays;

    @NotNull(message = "El rango máximo en meses es obligatorio")
    @Min(value = 1, message = "El mínimo de rango es 1 mes")
    @Max(value = 24, message = "El máximo de rango es 24 meses")
    private Integer maxReportDateRangeMonths;

    @NotBlank(message = "El formato por defecto de los reportes es obligatorio")
    private String defaultReportFormat;
}
