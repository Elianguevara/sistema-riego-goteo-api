// src/main/java/com/sistemariegoagoteo/sistema_riego_goteo_api/scheduler/WeatherScheduler.java
package com.sistemariegoagoteo.sistema_riego_goteo_api.scheduler;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.notification.NotificationService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.weather.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherScheduler {

    private final FarmRepository farmRepository;
    private final WeatherService weatherService;
    private final NotificationService notificationService;

    // Se ejecuta cada hora
    @Scheduled(fixedRate = 3600000)
    public void checkForRain() {
        log.info("Ejecutando tarea programada: Verificando pronóstico de lluvia...");
        List<Farm> farms = farmRepository.findAll();

        for (Farm farm : farms) {
            try {
                weatherService.getCurrentWeather(farm).getWeather().stream()
                        .filter(weather -> "Rain".equalsIgnoreCase(weather.getMain()))
                        .findFirst()
                        .ifPresent(weather -> {
                            String message = String.format(
                                    "Alerta de Lluvia para la finca '%s': %s.",
                                    farm.getName(),
                                    weather.getDescription());
                            // Notificar a todos los usuarios de la finca
                            farm.getUsers().forEach(user -> notificationService.createNotification(user, message,
                                    "FARM", Long.valueOf(farm.getId()), "/farms/" + farm.getId() + "/dashboard"));
                        });
            } catch (Exception e) {
                log.error("Error al obtener el clima para la finca ID {}: {}", farm.getId(), e.getMessage());
            }
        }
    }
}