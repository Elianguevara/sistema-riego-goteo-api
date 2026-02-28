package com.sistemariegoagoteo.sistema_riego_goteo_api.service.notification;

import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.notification.AppNotification;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.notification.NotificationType;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public List<AppNotification> getUnreadNotificationsForUser(User user) {
        return notificationRepository.findByDestinatarioAndIsReadFalseOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public Page<AppNotification> getAllNotificationsForUser(User user, Pageable pageable) {
        return notificationRepository.findByDestinatarioOrderByCreatedAtDesc(user, pageable);
    }

    @Transactional(readOnly = true)
    public long getUnreadCountForUser(User user) {
        return notificationRepository.countByDestinatarioAndIsReadFalse(user);
    }

    @Transactional
    public AppNotification markAsRead(Long notificationId, User user) {
        AppNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("AppNotification", "id", notificationId));

        if (!notification.getDestinatario().getId().equals(user.getId())) {
            throw new SecurityException("No tienes permiso para modificar esta notificación.");
        }

        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(User user) {
        notificationRepository.markAllAsReadForUser(user);
    }

    @Transactional
    public void createNotification(User recipient, String message, String entityType, Long entityId, String actionUrl) {
        AppNotification notification = new AppNotification();
        notification.setDestinatario(recipient);
        notification.setMessage(message);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification.setActionUrl(actionUrl);
        notification.setType(NotificationType.INFO);
        notification.setCreatedAt(new Date());
        notificationRepository.save(notification);
    }
}