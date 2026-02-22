package com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth;

import com.sistemariegoagoteo.sistema_riego_goteo_api.config.jwt.JwtConfig;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios para JwtService.
 * No requiere Spring Context, se instancia directamente con valores de prueba.
 */
@DisplayName("JwtService - Tests Unitarios")
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    // Clave Base64 de exactamente 88 caracteres (528 bits >= 512 bits requerido por
    // HS512)
    private static final String TEST_SECRET = "dGVzdFNlY3JldEtleUZvclRlc3RpbmdQdXJwb3Nlc09ubHlBbmRJdE5lZWRzVG9CZUxvbmdFbm91Z2hGb3JIUzUxMkFsZ29yaXRobQ==";
    private static final long TEST_EXPIRATION = 86400000L; // 24 horas

    @BeforeEach
    void setUp() {
        JwtConfig jwtConfig = new JwtConfig();
        jwtConfig.setSecret(TEST_SECRET);
        jwtConfig.setExpiration(TEST_EXPIRATION);

        jwtService = new JwtService(jwtConfig);

        Role adminRole = new Role("ADMIN");
        testUser = new User("Admin Test", "admin_test", "password123", "admin@test.com", adminRole);
    }

    @Test
    @DisplayName("generateToken() debe generar un token no nulo y no vacío")
    void generateToken_debeRetornarTokenNoNulo() {
        String token = jwtService.generateToken(testUser);

        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("extractUsername() debe extraer correctamente el username del token")
    void extractUsername_debeRetornarUsernameCorrector() {
        String token = jwtService.generateToken(testUser);

        String extractedUsername = jwtService.extractUsername(token);

        assertThat(extractedUsername).isEqualTo(testUser.getUsername());
    }

    @Test
    @DisplayName("isTokenValid() debe retornar true para token generado con el mismo usuario")
    void isTokenValid_debeRetornarTrueConTokenValido() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() debe retornar false si el usuario difiere del token")
    void isTokenValid_debeRetornarFalseConUsuarioDiferente() {
        String token = jwtService.generateToken(testUser);

        Role role = new Role("OPERARIO");
        User otroUsuario = new User("Otro", "otro_usuario", "pass", "otro@test.com", role);
        boolean isValid = jwtService.isTokenValid(token, otroUsuario);

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("generateToken() debe incluir el claim 'rol' en el token")
    void generateToken_debeIncluirClaimRol() {
        String token = jwtService.generateToken(testUser);

        // Extraemos el claim 'rol' del token
        String rol = jwtService.extractClaim(token, claims -> claims.get("rol", String.class));

        assertThat(rol).isEqualTo("ADMIN");
    }
}
