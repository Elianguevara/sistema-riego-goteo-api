package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.weather;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.weather.WeatherResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.FarmService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/farms/{farmId}/weather")
@RequiredArgsConstructor
@Slf4j
public class WeatherController {

    private final WeatherService weatherService;
    private final FarmService farmService;

    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    public ResponseEntity<?> getCurrentWeatherForFarm(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener el clima actual de la finca ID {}", farmId);
        try {
            Farm farm = farmService.getFarmById(farmId);
            WeatherResponse weatherResponse = weatherService.getCurrentWeather(farm);
            return ResponseEntity.ok(weatherResponse);
        } catch (ResourceNotFoundException e) {
            log.warn("No se pudo obtener el clima. Causa: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            log.error("Error al procesar la solicitud de clima para la finca ID {}: {}", farmId, e.getMessage());
            // Devolvemos un 409 Conflict si la finca no tiene coordenadas, por ejemplo.
            return ResponseEntity.status(409).body(e.getMessage());
        }
    }
}