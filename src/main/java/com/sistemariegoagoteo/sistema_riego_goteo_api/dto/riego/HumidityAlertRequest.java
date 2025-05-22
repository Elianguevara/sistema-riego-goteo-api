package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class HumidityAlertRequest {

    // sensorId vendrá de la ruta del controlador

    @NotNull(message = "El nivel de humedad que generó la alerta es requerido.")
    private BigDecimal humidityLevel;

    @NotNull(message = "La fecha y hora de la alerta son requeridas.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date alertDatetime;

    @NotBlank(message = "El mensaje de la alerta no puede estar vacío.")
    @Size(max = 255, message = "El mensaje de la alerta no puede exceder los 255 caracteres.")
    private String alertMessage;

    @NotNull(message = "El umbral de humedad que se cruzó es requerido.")
    private BigDecimal humidityThreshold;
}