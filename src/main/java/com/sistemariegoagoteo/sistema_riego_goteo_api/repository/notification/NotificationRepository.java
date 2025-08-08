package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.notification;


import com.sistemariegoagoteo.sistema_riego_goteo_api.model.notification.Notification;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // Para buscar notificaciones no leídas de un usuario
    Page<Notification> findByRecipientAndIsReadFalseOrderByCreatedAtDesc(User recipient, Pageable pageable);

    // Para buscar todas las notificaciones de un usuario
    Page<Notification> findByRecipientOrderByCreatedAtDesc(User recipient, Pageable pageable);
}
