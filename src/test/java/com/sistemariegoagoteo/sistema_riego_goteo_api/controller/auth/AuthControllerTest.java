package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.AuthRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.AuthResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.RegisterRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.AuthService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.JwtService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests de capa web para AuthController usando @WebMvcTest (carga solo el
 * controlador y MockMvc).
 * Se mockean todos los servicios requeridos.
 */
@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@DisplayName("AuthController - Tests de Capa Web")
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private UserService userService;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private UserDetailsService userDetailsService;

        @Test
        @WithMockUser
        @DisplayName("POST /api/auth/login debe retornar 200 OK con token cuando las credenciales son válidas")
        void login_credencialesValidas_retorna200ConToken() throws Exception {
                AuthRequest request = new AuthRequest("admin_test", "password123");
                AuthResponse response = new AuthResponse("mock.jwt.token", "Bearer", true);

                when(authService.authenticate(any(AuthRequest.class))).thenReturn(response);

                mockMvc.perform(post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$.token").value("mock.jwt.token"))
                                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                                .andExpect(jsonPath("$.active").value(true));
        }

        @Test
        @DisplayName("POST /api/auth/login debe retornar 401 cuando las credenciales son inválidas")
        void login_credencialesInvalidas_retorna401() throws Exception {
                AuthRequest request = new AuthRequest("admin_test", "wrongPassword");

                when(authService.authenticate(any(AuthRequest.class)))
                                .thenThrow(new BadCredentialsException("Credenciales inválidas."));

                mockMvc.perform(post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        @WithMockUser
        @DisplayName("POST /api/auth/login debe retornar 400 cuando el body está vacío o inválido")
        void login_requestInvalida_retorna400() throws Exception {
                // username vacío viola @NotBlank
                AuthRequest request = new AuthRequest("", "password123");

                mockMvc.perform(post("/api/auth/login")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockUser
        @DisplayName("POST /api/auth/register/admin debe retornar 201 al registrar admin exitosamente")
        void registerAdmin_datosValidos_retorna201() throws Exception {
                RegisterRequest request = new RegisterRequest(
                                "Nuevo Admin", "nuevo_admin", "pass1234", "nuevo@test.com", "ADMIN");

                Role adminRole = new Role("ADMIN");
                adminRole.setId(1);
                User adminUser = new User("Nuevo Admin", "nuevo_admin", "encodedPass", "nuevo@test.com", adminRole);
                adminUser.setActive(true);

                when(authService.registerAdmin(any(RegisterRequest.class))).thenReturn(adminUser);

                mockMvc.perform(post("/api/auth/register/admin")
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isCreated())
                                .andExpect(content().string(org.hamcrest.Matchers.containsString("nuevo_admin")));
        }
}
