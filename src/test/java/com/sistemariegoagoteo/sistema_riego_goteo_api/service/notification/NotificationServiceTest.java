package com.sistemariegoagoteo.sistema_riego_goteo_api.service.notification;

import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.notification.AppNotification;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.notification.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para NotificationService usando Mockito.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificationService - Tests Unitarios")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private User otherUser;
    private AppNotification testNotification;

    @BeforeEach
    void setUp() {
        Role operarioRole = new Role("OPERARIO");
        operarioRole.setId(3);

        testUser = new User("Elian", "elian", "pass", "elian@test.com", operarioRole);
        testUser.setId(1L);

        otherUser = new User("Otro", "otro", "pass", "otro@test.com", operarioRole);
        otherUser.setId(2L);

        testNotification = new AppNotification();
        testNotification.setId(1L);
        testNotification.setDestinatario(testUser);
        testNotification.setMessage("Mensaje de prueba");
        testNotification.setEntityType("TASK");
        testNotification.setEntityId(45L);
        testNotification.setActionUrl("/tasks/45");
        testNotification.setRead(false);
        testNotification.setCreatedAt(new Date());
    }

    @Test
    @DisplayName("createNotification() debe guardar la notificación y retornar")
    void createNotification_datosValidos_guardaNotificacion() {
        when(notificationRepository.save(any(AppNotification.class))).thenReturn(testNotification);

        notificationService.createNotification(testUser, "Test message", "TASK", 100L, "/tasks/100");

        ArgumentCaptor<AppNotification> captor = ArgumentCaptor.forClass(AppNotification.class);
        verify(notificationRepository, times(1)).save(captor.capture());

        AppNotification saved = captor.getValue();
        assertThat(saved.getDestinatario()).isEqualTo(testUser);
        assertThat(saved.getMessage()).isEqualTo("Test message");
        assertThat(saved.getEntityType()).isEqualTo("TASK");
        assertThat(saved.getEntityId()).isEqualTo(100L);
        assertThat(saved.getActionUrl()).isEqualTo("/tasks/100");
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("getUserNotifications() debe retornar las notificaciones paginadas del usuario")
    void getAllNotificationsForUser_usuarioValido_retornaPagina() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<AppNotification> mockPage = new PageImpl<>(List.of(testNotification));
        when(notificationRepository.findByDestinatarioOrderByCreatedAtDesc(testUser, pageable)).thenReturn(mockPage);

        Page<AppNotification> result = notificationService.getAllNotificationsForUser(testUser, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        AppNotification res = result.getContent().get(0);
        assertThat(res.getId()).isEqualTo(testNotification.getId());
        assertThat(res.getMessage()).isEqualTo(testNotification.getMessage());
        assertThat(res.getEntityType()).isEqualTo(testNotification.getEntityType());
        assertThat(res.getEntityId()).isEqualTo(testNotification.getEntityId());
        assertThat(res.getActionUrl()).isEqualTo(testNotification.getActionUrl());
        assertThat(res.isRead()).isFalse();
    }

    @Test
    @DisplayName("markAsRead() debe actualizar isRead a true si el usuario es el dueño")
    void markAsRead_duenoValido_actualizaNotificacion() {
        when(notificationRepository.findById(testNotification.getId())).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(AppNotification.class))).thenReturn(testNotification);

        notificationService.markAsRead(testNotification.getId(), testUser);

        assertThat(testNotification.isRead()).isTrue();
        verify(notificationRepository, times(1)).save(testNotification);
    }

    @Test
    @DisplayName("markAsRead() debe lanzar SecurityException si el usuario no es el dueño")
    void markAsRead_usuarioNoEsDueno_lanzaSecurityException() {
        when(notificationRepository.findById(testNotification.getId())).thenReturn(Optional.of(testNotification));

        assertThatThrownBy(() -> notificationService.markAsRead(testNotification.getId(), otherUser))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("No tienes permiso");

        verify(notificationRepository, never()).save(any(AppNotification.class));
    }

    @Test
    @DisplayName("markAsRead() debe lanzar ResourceNotFoundException si no existe")
    void markAsRead_notificacionNoExiste_lanzaResourceNotFoundException() {
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(999L, testUser))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getUnreadNotificationsCount() debe retornar el conteo para el usuario")
    void getUnreadCountForUser_usuarioValido_retornaConteo() {
        when(notificationRepository.countByDestinatarioAndIsReadFalse(testUser)).thenReturn(5L);

        long count = notificationService.getUnreadCountForUser(testUser);

        assertThat(count).isEqualTo(5L);
        verify(notificationRepository, times(1)).countByDestinatarioAndIsReadFalse(testUser);
    }
}
