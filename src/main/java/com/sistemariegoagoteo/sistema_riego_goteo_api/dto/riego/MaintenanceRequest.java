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
public class MaintenanceRequest {

    @NotNull(message = "La fecha del mantenimiento es requerida.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private Date date;

    @NotBlank(message = "La descripción no puede estar vacía.")
    @Size(max = 65535, message = "La descripción es demasiado larga (max 65535 caracteres para TEXT).") // Ajustar si es necesario
    private String description;

    @PositiveOrZero(message = "Las horas de trabajo deben ser un valor positivo o cero.")
    private BigDecimal workHours; // Puede ser nulo si no aplica

    // equipmentId vendrá de la ruta del controlador
}