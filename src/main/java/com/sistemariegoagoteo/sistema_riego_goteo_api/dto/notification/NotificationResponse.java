package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.notification;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.notification.Notification;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class NotificationResponse {
    private Long id;
    private String message;
    private String link;
    private boolean isRead;
    private Date createdAt;

    public NotificationResponse(Notification notification) {
        this.id = notification.getId();
        this.message = notification.getMessage();
        this.link = notification.getLink();
        this.isRead = notification.isRead();
        this.createdAt = notification.getCreatedAt();
    }
}