package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.notification;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.notification.AppNotification;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<AppNotification, Long> {

    List<AppNotification> findByDestinatarioOrderByCreatedAtDesc(User destinatario);

    Page<AppNotification> findByDestinatarioOrderByCreatedAtDesc(User destinatario, Pageable pageable);

    List<AppNotification> findByDestinatarioAndIsReadFalseOrderByCreatedAtDesc(User destinatario);

    long countByDestinatarioAndIsReadFalse(User destinatario);

    @Modifying
    @Query("UPDATE AppNotification n SET n.isRead = true WHERE n.destinatario = :user AND n.isRead = false")
    void markAllAsReadForUser(User user);
}