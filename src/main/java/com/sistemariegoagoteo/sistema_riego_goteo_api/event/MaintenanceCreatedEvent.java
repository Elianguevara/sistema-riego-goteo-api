package com.sistemariegoagoteo.sistema_riego_goteo_api.event;

public record MaintenanceCreatedEvent(Integer maintenanceId, Integer farmId, String equipmentName, String description) {
}
