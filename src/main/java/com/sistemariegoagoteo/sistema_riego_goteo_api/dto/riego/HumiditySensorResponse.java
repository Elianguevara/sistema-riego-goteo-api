package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.HumiditySensor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

@Data
@NoArgsConstructor
public class HumiditySensorResponse {
    private Integer id;
    private String sensorType;
    private BigDecimal humidityLevel;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date measurementDatetime;

    private Integer sectorId;
    private String sectorName;
    private Integer farmId;
    private String farmName;

    public HumiditySensorResponse(HumiditySensor sensor) {
        this.id = sensor.getId();
        this.sensorType = sensor.getSensorType();
        this.humidityLevel = sensor.getHumidityLevel();
        this.measurementDatetime = sensor.getMeasurementDatetime();
        if (sensor.getSector() != null) {
            this.sectorId = sensor.getSector().getId();
            this.sectorName = sensor.getSector().getName();
            if (sensor.getSector().getFarm() != null) {
                this.farmId = sensor.getSector().getFarm().getId();
                this.farmName = sensor.getSector().getFarm().getName();
            }
        }
    }
}