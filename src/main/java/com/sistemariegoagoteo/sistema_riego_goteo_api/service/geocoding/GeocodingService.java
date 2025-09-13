package com.sistemariegoagoteo.sistema_riego_goteo_api.service.geocoding;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.geocoding.GeocodingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Slf4j
public class GeocodingService {

    private final RestTemplate restTemplate;

    @Value("${geocoding.api.key}")
    private String apiKey;

    @Value("${geocoding.api.url}")
    private String apiUrl;

    public GeocodingService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Optional<Coordinates> getCoordinates(String location) {
        if (location == null || location.isBlank()) {
            return Optional.empty();
        }

        String url = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("q", location)
                .queryParam("key", apiKey)
                .queryParam("limit", 1) // Solo queremos el resultado más relevante
                .queryParam("countrycode", "ar") // Priorizamos resultados en Argentina
                .toUriString();

        try {
            GeocodingResponse response = restTemplate.getForObject(url, GeocodingResponse.class);

            if (response != null && response.getResults() != null && !response.getResults().isEmpty()) {
                GeocodingResponse.Geometry geometry = response.getResults().get(0).getGeometry();
                if (geometry != null && geometry.getLat() != null && geometry.getLng() != null) {
                    return Optional.of(new Coordinates(geometry.getLat(), geometry.getLng()));
                }
            }
        } catch (Exception e) {
            log.error("Error al llamar a la API de geocodificación para la ubicación '{}': {}", location, e.getMessage());
        }

        return Optional.empty();
    }

    // Clase auxiliar para devolver las coordenadas
    public record Coordinates(BigDecimal latitude, BigDecimal longitude) {}
}