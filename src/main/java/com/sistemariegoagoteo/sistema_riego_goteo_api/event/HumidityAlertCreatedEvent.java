package com.sistemariegoagoteo.sistema_riego_goteo_api.event;

public record HumidityAlertCreatedEvent(Integer alertId, Integer farmId, String sensorName, String humidityLevel) {
}
