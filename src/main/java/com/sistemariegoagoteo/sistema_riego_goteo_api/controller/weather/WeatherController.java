package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.weather;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.weather.WeatherResponse;
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

    /**
     * Obtiene el clima actual para una finca específica.
     * Utiliza las coordenadas de la finca para consultar un servicio externo de clima.
     *
     * @param farmId ID de la finca.
     * @return Datos del clima actual.
     */
    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    public ResponseEntity<WeatherResponse> getCurrentWeatherForFarm(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener el clima actual de la finca ID {}", farmId);
        
        // Si la finca no existe, farmService lanza ResourceNotFoundException (manejado globalmente)
        Farm farm = farmService.getFarmById(farmId);
        
        // Si falla la API externa o faltan coordenadas, weatherService lanza la excepción apropiada
        // (asegúrate de que GlobalExceptionHandler capture IllegalStateException o una personalizada)
        WeatherResponse weatherResponse = weatherService.getCurrentWeather(farm);
        
        return ResponseEntity.ok(weatherResponse);
    }
}