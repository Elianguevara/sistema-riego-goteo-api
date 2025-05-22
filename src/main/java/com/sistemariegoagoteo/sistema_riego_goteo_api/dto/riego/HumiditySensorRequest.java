package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
//import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class HumiditySensorRequest {

    @NotBlank(message = "El tipo de sensor no puede estar vacío.")
    @Size(max = 50, message = "El tipo de sensor no puede exceder los 50 caracteres.")
    private String sensorType;

    // Estos campos permiten registrar una lectura inicial o una actualización manual
    @PositiveOrZero(message = "El nivel de humedad debe ser un valor positivo o cero.")
    private BigDecimal humidityLevel;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date measurementDatetime;

    // sectorId vendrá de la ruta del controlador
}