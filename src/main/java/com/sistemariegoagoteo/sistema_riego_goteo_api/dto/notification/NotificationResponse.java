package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.notification;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.notification.AppNotification;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class NotificationResponse {
    private Long id;
    private String message;
    private String actionUrl;
    private boolean isRead;
    private Date createdAt;
    private String entityType;
    private Long entityId;

    public NotificationResponse(AppNotification notification) {
        this.id = notification.getId();
        this.message = notification.getMessage();
        this.actionUrl = notification.getActionUrl();
        this.isRead = notification.isRead();
        this.createdAt = notification.getCreatedAt();
        this.entityType = notification.getEntityType();
        this.entityId = notification.getEntityId();
    }
}