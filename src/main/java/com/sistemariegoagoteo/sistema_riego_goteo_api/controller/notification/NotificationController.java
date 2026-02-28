package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.notification;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.notification.AppNotification;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Gestión de notificaciones in-app del usuario")
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "Obtener notificaciones no leídas", description = "Retorna una lista de notificaciones que el usuario autenticado aún no ha leído.")
    @GetMapping("/unread")
    public ResponseEntity<List<AppNotification>> getUnreadNotifications(@AuthenticationPrincipal User currentUser) {
        List<AppNotification> unread = notificationService.getUnreadNotificationsForUser(currentUser);
        return ResponseEntity.ok(unread);
    }

    @Operation(summary = "Ver todas las notificaciones paginadas", description = "Retorna el historial completo de notificaciones de un usuario.")
    @GetMapping
    public ResponseEntity<Page<AppNotification>> getAllNotifications(@AuthenticationPrincipal User currentUser,
            Pageable pageable) {
        Page<AppNotification> page = notificationService.getAllNotificationsForUser(currentUser, pageable);
        return ResponseEntity.ok(page);
    }

    @Operation(summary = "Obtener contador de no leídas", description = "Retorna el número total de notificaciones no leídas para el badge de la UI.")
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal User currentUser) {
        long count = notificationService.getUnreadCountForUser(currentUser);
        return ResponseEntity.ok(count);
    }

    @Operation(summary = "Marcar notificación como leída", responses = {
            @ApiResponse(responseCode = "200", description = "Notificación marcada como leída exitosamente"),
            @ApiResponse(responseCode = "403", description = "No autorizado para modificar esta notificación"),
            @ApiResponse(responseCode = "404", description = "Notificación no encontrada")
    })
    @PutMapping("/{id}/read")
    public ResponseEntity<AppNotification> markAsRead(@PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        AppNotification updated = notificationService.markAsRead(id, currentUser);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Marcar todas las notificaciones como leídas", description = "Actualiza el estado de todas las notificaciones no leídas del usuario a leídas.")
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal User currentUser) {
        notificationService.markAllAsRead(currentUser);
        return ResponseEntity.ok().build();
    }
}
