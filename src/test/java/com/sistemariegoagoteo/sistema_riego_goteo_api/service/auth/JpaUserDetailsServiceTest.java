package com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios para JpaUserDetailsService.
 * Valida que Spring Security puede recuperar usuarios desde la BD
 * correctamente.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JpaUserDetailsService - Tests Unitarios")
class JpaUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private JpaUserDetailsService jpaUserDetailsService;

    @Test
    @DisplayName("loadUserByUsername() debe retornar el usuario cuando existe")
    void loadUserByUsername_UserFound_ReturnsUserDetails() {
        Role role = new Role("OPERARIO");
        User user = new User("Test Usuario", "test_user", "encoded_pass", "test@test.com", role);

        when(userRepository.findByUsername("test_user")).thenReturn(Optional.of(user));

        UserDetails result = jpaUserDetailsService.loadUserByUsername("test_user");

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("test_user");
    }

    @Test
    @DisplayName("loadUserByUsername() debe lanzar UsernameNotFoundException si el usuario no existe")
    void loadUserByUsername_UserNotFound_ThrowsException() {
        when(userRepository.findByUsername("noexiste")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jpaUserDetailsService.loadUserByUsername("noexiste"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("noexiste");
    }
}
