package com.sistemariegoagoteo.sistema_riego_goteo_api.event;

public record TaskStatusUpdatedEvent(Long taskId, Long createdByUserId, String statusName, String operatorName) {
}
