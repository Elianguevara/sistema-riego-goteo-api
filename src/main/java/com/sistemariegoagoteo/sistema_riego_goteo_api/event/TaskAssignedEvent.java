package com.sistemariegoagoteo.sistema_riego_goteo_api.event;

public record TaskAssignedEvent(Long taskId, Long assignedToUserId, String description) {
}
