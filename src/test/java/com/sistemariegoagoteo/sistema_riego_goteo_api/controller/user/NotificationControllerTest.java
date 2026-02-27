package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.user;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.notification.NotificationResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.notification.Notification;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.JwtService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de capa web para NotificationController.
 */
@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false) // Desactiva los filtros de seguridad que retornan 403 cuando no hay JWT
                                          // válido
@ActiveProfiles("test")
@DisplayName("NotificationController - Tests de Capa Web")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsService userDetailsService;

    private NotificationResponse notificationResponse;
    private User mockUser;

    @BeforeEach
    void setUp() {
        Notification testNotification = new Notification();
        testNotification.setId(45L);
        testNotification.setMessage("Mensaje de prueba");
        testNotification.setEntityType("TASK");
        testNotification.setEntityId(123L);
        testNotification.setActionUrl("/tasks/123");
        testNotification.setRead(false);
        testNotification.setCreatedAt(new Date());

        notificationResponse = new NotificationResponse(testNotification);

        mockUser = new User();
        mockUser.setId(1L);
        mockUser.setUsername("elian");

        // Injectar manualmente el Principal en el SecurityContext para que
        // SecurityContextHolder.getContext().getAuthentication().getPrincipal() obtenga
        // un User en el Controller
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(mockUser, null, null);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("GET /api/notifications debe retornar 200 OK con página de notificaciones")
    void getUserNotifications_retorna200ConPagina() throws Exception {
        Page<NotificationResponse> mockPage = new PageImpl<>(List.of(notificationResponse));
        when(notificationService.getUserNotifications(any(User.class), any(Pageable.class))).thenReturn(mockPage);

        mockMvc.perform(get("/api/notifications"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content[0].message").value("Mensaje de prueba"))
                .andExpect(jsonPath("$.content[0].entityType").value("TASK"))
                .andExpect(jsonPath("$.content[0].entityId").value(123L))
                .andExpect(jsonPath("$.content[0].actionUrl").value("/tasks/123"))
                .andExpect(jsonPath("$.content[0].read").value(false))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("PUT /api/notifications/{id}/read debe retornar 204 No Content")
    void markAsRead_retorna204NoContent() throws Exception {
        doNothing().when(notificationService).markAsRead(anyLong(), any(User.class));

        mockMvc.perform(put("/api/notifications/45/read"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/notifications/unread-count debe retornar 200 OK con conteo correcto")
    void getUnreadNotificationsCount_retorna200ConConteo() throws Exception {
        when(notificationService.getUnreadNotificationsCount(any(User.class))).thenReturn(7L);

        mockMvc.perform(get("/api/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.unreadCount").value(7));
    }
}
