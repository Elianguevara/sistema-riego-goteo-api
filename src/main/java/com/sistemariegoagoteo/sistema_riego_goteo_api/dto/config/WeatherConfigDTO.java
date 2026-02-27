package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class WeatherConfigDTO {

    @NotNull(message = "El estado de activación del servicio de clima es obligatorio")
    private Boolean weatherServiceEnabled;

    // Nota: Esta property no tendrá @NotBlank duro aquí mismo en todas las
    // instancias
    // porque al hacer PUT, un usuario podría enviar el asteriscado viejo si no
    // quiere cambiarla.
    // La dejaremos permitida pero en el SystemConfigService validaremos que si es
    // nula, no sobreescriba la existente.
    private String weatherApiKey;

    @NotNull(message = "El intervalo de actualización es obligatorio")
    @Min(value = 5, message = "El mínimo es 5 minutos")
    @Max(value = 1440, message = "El máximo es 1 día (1440 minutos)")
    private Integer weatherUpdateIntervalMinutes;

    @NotBlank(message = "El proveedor de clima no puede estar en blanco")
    private String weatherProvider;
}
