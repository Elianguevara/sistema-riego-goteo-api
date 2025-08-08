package com.sistemariegoagoteo.sistema_riego_goteo_api.service.notification;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.notification.Notification;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;

    @Transactional
    public void createNotification(User recipient, String message, String link) {
        Notification notification = new Notification();
        notification.setRecipient(recipient);
        notification.setMessage(message);
        notification.setLink(link);
        notification.setCreatedAt(new Date());
        notificationRepository.save(notification);
        // Aquí podrías integrar WebSockets para notificar en tiempo real al frontend
    }
    // ... métodos para marcar como leída, obtener notificaciones, etc.
}
