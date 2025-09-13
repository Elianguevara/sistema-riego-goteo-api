// src/main/java/com/sistemariegoagoteo/sistema_riego_goteo_api/service/weather/WeatherService.java
package com.sistemariegoagoteo.sistema_riego_goteo_api.service.weather;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.weather.WeatherResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class WeatherService {

    private final RestTemplate restTemplate;

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.url}")
    private String apiUrl;

    public WeatherService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public WeatherResponse getCurrentWeather(Farm farm) {
        if (farm.getLatitude() == null || farm.getLongitude() == null) {
            // Puedes lanzar una excepción o devolver un Optional vacío
            throw new IllegalStateException("La finca no tiene coordenadas para consultar el clima.");
        }

        String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("lat", farm.getLatitude())
                .queryParam("lon", farm.getLongitude())
                .queryParam("appid", apiKey)
                .queryParam("units", "metric") // Para obtener temperatura en Celsius
                .queryParam("lang", "es")     // Para descripciones en español
                .toUriString();

        return restTemplate.getForObject(url, WeatherResponse.class);
    }
}