package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.HumidityAlert;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
public class HumidityAlertResponse {
    private Integer id;

    private Integer sensorId;
    private String sensorType; // Tipo del sensor que generó la alerta

    private Integer sectorId; // Derivado del sensor
    private String sectorName; // Derivado del sensor

    private Integer farmId; // Derivado del sensor
    private String farmName; // Derivado del sensor

    private BigDecimal humidityLevel; // Nivel de humedad que disparó la alerta
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date alertDatetime;
    private String alertMessage;
    private BigDecimal humidityThreshold; // Umbral que se cruzó

    public HumidityAlertResponse(HumidityAlert alert) {
        this.id = alert.getId();
        if (alert.getHumiditySensor() != null) {
            this.sensorId = alert.getHumiditySensor().getId();
            this.sensorType = alert.getHumiditySensor().getSensorType();
            if (alert.getHumiditySensor().getSector() != null) {
                this.sectorId = alert.getHumiditySensor().getSector().getId();
                this.sectorName = alert.getHumiditySensor().getSector().getName();
                if (alert.getHumiditySensor().getSector().getFarm() != null) {
                    this.farmId = alert.getHumiditySensor().getSector().getFarm().getId();
                    this.farmName = alert.getHumiditySensor().getSector().getFarm().getName();
                }
            }
        }
        this.humidityLevel = alert.getHumidityLevel();
        this.alertDatetime = alert.getAlertDatetime();
        this.alertMessage = alert.getAlertMessage();
        this.humidityThreshold = alert.getHumidityThreshold();
    }
}