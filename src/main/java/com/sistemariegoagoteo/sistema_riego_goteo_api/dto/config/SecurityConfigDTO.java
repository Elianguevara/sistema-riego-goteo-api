package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SecurityConfigDTO {

    @NotNull(message = "La duración de la sesión es obligatoria")
    @Min(value = 1, message = "La duración mínima es 1 hora")
    @Max(value = 72, message = "La duración máxima es 72 horas")
    private Integer sessionDurationHours;

    @NotNull(message = "El límite de intentos fallidos es obligatorio")
    @Min(value = 1, message = "El mínimo es 1 intento")
    @Max(value = 10, message = "El máximo permitido son 10 intentos")
    private Integer maxFailedLoginAttempts;

    @NotNull(message = "La longitud mínima de contraseña es obligatoria")
    @Min(value = 6, message = "El mínimo de longitud es 6 caracteres")
    @Max(value = 32, message = "La longitud máxima es de 32 caracteres")
    private Integer passwordMinLength;

    @NotNull(message = "Debe especificar si fuerza el cambio de contraseña inicial")
    private Boolean forcePasswordChangeOnFirstLogin;
}
