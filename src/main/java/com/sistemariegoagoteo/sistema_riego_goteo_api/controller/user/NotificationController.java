package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.user;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.notification.NotificationResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Obtiene las notificaciones del usuario autenticado de forma paginada.
     *
     * @param pageable Configuración de paginación (por defecto ordena por fecha descendente).
     * @return Página de notificaciones.
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getUserNotifications(
            @PageableDefault(size = 15, sort = "createdAt,desc") Pageable pageable) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(currentUser, pageable);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Marca una notificación específica como leída.
     *
     * @param id ID de la notificación.
     * @return 204 No Content.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        notificationService.markAsRead(id, currentUser);
        return ResponseEntity.noContent().build();
    }

    /**
     * Obtiene el conteo de notificaciones no leídas.
     * Útil para mostrar badges en la interfaz de usuario.
     *
     * @return Mapa con la clave "unreadCount".
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadNotificationsCount() {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        long count = notificationService.getUnreadNotificationsCount(currentUser);
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}