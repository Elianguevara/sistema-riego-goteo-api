// src/main/java/com/sistemariegoagoteo/sistema_riego_goteo_api/dto/weather/WeatherResponse.java
package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {

    private Main main;
    private Wind wind;
    private List<Weather> weather;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Main {
        @JsonProperty("temp")
        private BigDecimal temperature;
        private BigDecimal humidity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Wind {
        private BigDecimal speed;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weather {
        private String main; // Ej: "Rain", "Clouds"
        private String description;
    }
}