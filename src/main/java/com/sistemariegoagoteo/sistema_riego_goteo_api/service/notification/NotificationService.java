package com.sistemariegoagoteo.sistema_riego_goteo_api.service.notification;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.notification.NotificationResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.notification.Notification;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(User user, Pageable pageable) {
        return notificationRepository.findByRecipientOrderByCreatedAtDesc(user, pageable)
                .map(NotificationResponse::new);
    }

    @Transactional
    public void markAsRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        // Asegurarse de que solo el dueño pueda marcarla como leída
        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new SecurityException("No tienes permiso para modificar esta notificación.");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    // --- NUEVO MÉTODO AÑADIDO ---
    @Transactional(readOnly = true)
    public long getUnreadNotificationsCount(User user) {
        return notificationRepository.countByRecipientAndIsReadFalse(user);
    }
}