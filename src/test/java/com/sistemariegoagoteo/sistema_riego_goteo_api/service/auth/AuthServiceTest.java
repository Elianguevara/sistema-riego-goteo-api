package com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.AuthRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.AuthResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.RegisterRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.RoleRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para AuthService usando Mockito.
 * Verifica la lógica de autenticación y registro de administrador.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService - Tests Unitarios")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private Role adminRole;
    private User testUser;

    @BeforeEach
    void setUp() {
        adminRole = new Role("ADMIN");
        adminRole.setId(1);
        testUser = new User("Admin Test", "admin_test", "encodedPass", "admin@test.com", adminRole);
        testUser.setActive(true);
    }

    // ===== TESTS DE authenticate() =====

    @Test
    @DisplayName("authenticate() debe retornar AuthResponse con token cuando las credenciales son válidas")
    void authenticate_credencialesValidas_retornaAuthResponse() {
        AuthRequest request = new AuthRequest("admin_test", "password123");
        Authentication authMock = mock(Authentication.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authMock);
        when(authMock.getPrincipal()).thenReturn(testUser);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("mock.jwt.token");

        AuthResponse response = authService.authenticate(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock.jwt.token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        verify(userRepository, times(1)).save(any(User.class));
        verify(jwtService, times(1)).generateToken(any(User.class));
    }

    @Test
    @DisplayName("authenticate() debe lanzar BadCredentialsException cuando las credenciales son inválidas")
    void authenticate_credencialesInvalidas_lanzaBadCredentialsException() {
        AuthRequest request = new AuthRequest("admin_test", "wrongPassword");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(BadCredentialsException.class);
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("authenticate() debe lanzar RuntimeException si el usuario está inactivo")
    void authenticate_usuarioInactivo_lanzaRuntimeException() {
        testUser.setActive(false);
        AuthRequest request = new AuthRequest("admin_test", "password123");
        Authentication authMock = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(authMock);
        when(authMock.getPrincipal()).thenReturn(testUser);

        assertThatThrownBy(() -> authService.authenticate(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("inactiva");
        verify(jwtService, never()).generateToken(any());
    }

    // ===== TESTS DE registerAdmin() =====

    @Test
    @DisplayName("registerAdmin() debe guardar y retornar el usuario cuando los datos son válidos")
    void registerAdmin_datosValidos_retornaUsuarioCreado() {
        RegisterRequest request = new RegisterRequest(
                "Nuevo Admin", "nuevo_admin", "pass123", "nuevo@test.com", "ADMIN");
        when(roleRepository.findByRoleName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.existsByUsername("nuevo_admin")).thenReturn(false);
        when(userRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        User result = authService.registerAdmin(request);

        assertThat(result).isNotNull();
        verify(userRepository, times(1)).save(any(User.class));
        verify(passwordEncoder, times(1)).encode("pass123");
    }

    @Test
    @DisplayName("registerAdmin() debe lanzar IllegalArgumentException si el rol no es ADMIN")
    void registerAdmin_rolNoAdmin_lanzaIllegalArgumentException() {
        RegisterRequest request = new RegisterRequest(
                "Operario", "operario1", "pass123", "op@test.com", "OPERARIO");

        assertThatThrownBy(() -> authService.registerAdmin(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ADMIN");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerAdmin() debe lanzar RuntimeException si el username ya existe")
    void registerAdmin_usernameExistente_lanzaRuntimeException() {
        RegisterRequest request = new RegisterRequest(
                "Admin", "admin_test", "pass123", "otro@test.com", "ADMIN");
        when(roleRepository.findByRoleName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.existsByUsername("admin_test")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerAdmin(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("usuario");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerAdmin() debe lanzar RuntimeException si el email ya existe")
    void registerAdmin_emailExistente_lanzaRuntimeException() {
        RegisterRequest request = new RegisterRequest(
                "Admin", "nuevo_admin", "pass123", "admin@test.com", "ADMIN");
        when(roleRepository.findByRoleName("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("admin@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerAdmin(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("email");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("registerAdmin() debe lanzar RuntimeException si el rol ADMIN no existe en BD")
    void registerAdmin_rolAdminNoExisteEnBD_lanzaRuntimeException() {
        RegisterRequest request = new RegisterRequest(
                "Admin", "nuevo_admin", "pass123", "nuevo@test.com", "ADMIN");
        when(roleRepository.findByRoleName("ADMIN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.registerAdmin(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ADMIN");
    }
}
