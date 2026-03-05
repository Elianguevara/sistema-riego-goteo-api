package com.sistemariegoagoteo.sistema_riego_goteo_api.security;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.Role;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.RoleRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de Seguridad e Integración del filtro JWT.
 *
 * Nivel cubierto: Pruebas de Sistema (Caja Negra de Seguridad)
 *
 * Verifica que el JwtAuthenticationFilter se comporte correctamente
 * ante distintos estados del token: ausente, expirado, malformado,
 * firma incorrecta y válido con distintos roles.
 *
 * Usa @SpringBootTest para cargar el contexto completo (filtros reales,
 * UserDetailsService real, H2 en memoria) sin mocks de seguridad.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("JWT Security - Tests de Integración del Filtro")
class JwtSecurityIntegrationTest {

    // Debe coincidir con jwt.secret en application-test.properties
    private static final String TEST_JWT_SECRET =
            "dGVzdFNlY3JldEtleUZvclRlc3RpbmdQdXJwb3Nlc09ubHlBbmRJdE5lZWRzVG9CZUxvbmdFbm91Z2hGb3JIUzUxMkFsZ29yaXRobQ==";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User adminUser;
    private User operarioUser;

    /**
     * Crea usuarios de prueba en la BD H2 antes de cada test.
     * @Transactional hace rollback al terminar cada test -> BD limpia.
     */
    @BeforeEach
    void setUp() {
        // Obtener roles ya seeded por DataInitializer/SystemConfigSeeder
        Role adminRole = roleRepository.findByRoleName("ADMIN")
                .orElseGet(() -> roleRepository.save(new Role("ADMIN")));
        Role operarioRole = roleRepository.findByRoleName("OPERARIO")
                .orElseGet(() -> roleRepository.save(new Role("OPERARIO")));

        // Crear usuario ADMIN de prueba si no existe ya
        if (userRepository.findByUsername("test_admin_jwt").isEmpty()) {
            adminUser = new User("Test Admin JWT", "test_admin_jwt",
                    passwordEncoder.encode("pass1234"), "test_admin_jwt@test.com", adminRole);
            adminUser = userRepository.save(adminUser);
        } else {
            adminUser = userRepository.findByUsername("test_admin_jwt").get();
        }

        // Crear usuario OPERARIO de prueba si no existe ya
        if (userRepository.findByUsername("test_operario_jwt").isEmpty()) {
            operarioUser = new User("Test Operario JWT", "test_operario_jwt",
                    passwordEncoder.encode("pass1234"), "test_operario_jwt@test.com", operarioRole);
            operarioUser = userRepository.save(operarioUser);
        } else {
            operarioUser = userRepository.findByUsername("test_operario_jwt").get();
        }
    }

    // =========================================================================
    // GRUPO 1: Solicitudes sin token o con token inválido → 403 Forbidden
    //
    // NOTA: Spring Security devuelve 403 (y no 401) cuando no hay un
    // AuthenticationEntryPoint configurado explícitamente. Esto es el
    // comportamiento real de esta API stateless. El filtro JWT deja pasar
    // la request sin autenticar y el AccessDeniedHandler devuelve 403.
    // =========================================================================

    @Test
    @DisplayName("Sin header Authorization → 403 en endpoint protegido (no autenticado)")
    void sinToken_endpointProtegido_retorna403() throws Exception {
        mockMvc.perform(get("/api/farms"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Header Authorization sin prefijo 'Bearer ' → 403 (filtro ignora, pasa sin auth)")
    void tokenSinPrefixBearer_retorna403() throws Exception {
        String token = buildValidToken("test_admin_jwt", "ADMIN", 3_600_000L);

        mockMvc.perform(get("/api/farms")
                        .header("Authorization", token)) // Sin "Bearer "
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Token JWT expirado → 403 (filtro captura excepción, continúa sin auth)")
    void tokenJwtExpirado_retorna403() throws Exception {
        // Genera un token que expiró hace 1 hora (expirationMs negativo)
        String expiredToken = buildValidToken("test_admin_jwt", "ADMIN", -3_600_000L);

        mockMvc.perform(get("/api/farms")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Token JWT con firma incorrecta → 403 (SignatureException en filtro)")
    void tokenConFirmaIncorrecta_retorna403() throws Exception {
        // Token firmado con clave diferente a la del servidor
        String wrongSecret = "d3JvbmdTZWNyZXRLZXlGb3JUZXN0aW5nUHVycG9zZXNPbmx5QW5kSXROZWVkc1RvQmVMb25nRW5vdWdoRm9ySFM1MTJBZ29yaXRobQ==";
        byte[] keyBytes = Decoders.BASE64.decode(wrongSecret);
        SecretKey wrongKey = Keys.hmacShaKeyFor(keyBytes);

        String tamperedToken = Jwts.builder()
                .setSubject("test_admin_jwt")
                .claim("rol", "ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3_600_000L))
                .signWith(wrongKey, SignatureAlgorithm.HS512)
                .compact();

        mockMvc.perform(get("/api/farms")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Token JWT malformado (cadena aleatoria) → 403 (MalformedJwtException en filtro)")
    void tokenMalformado_retorna403() throws Exception {
        mockMvc.perform(get("/api/farms")
                        .header("Authorization", "Bearer este.no.es.un.jwt.valido"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // GRUPO 2: Token válido con rol correcto → 200 OK
    // =========================================================================

    @Test
    @DisplayName("Token válido con rol ADMIN → 200 OK en GET /api/farms")
    void tokenValidoAdmin_getEndpoint_retorna200() throws Exception {
        String validToken = buildValidToken("test_admin_jwt", "ADMIN", 3_600_000L);

        mockMvc.perform(get("/api/farms")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Token válido con rol OPERARIO → 200 OK en GET /api/farms")
    void tokenValidoOperario_getEndpoint_retorna200() throws Exception {
        String validToken = buildValidToken("test_operario_jwt", "OPERARIO", 3_600_000L);

        mockMvc.perform(get("/api/farms")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk());
    }

    // =========================================================================
    // GRUPO 3: Token válido pero sin el rol requerido → 403 Forbidden
    // =========================================================================

    @Test
    @DisplayName("Token válido con rol OPERARIO → 403 Forbidden en POST /api/farms (requiere ADMIN)")
    void tokenOperario_endpointSoloAdmin_retorna403() throws Exception {
        String operarioToken = buildValidToken("test_operario_jwt", "OPERARIO", 3_600_000L);

        // POST /api/farms requiere @PreAuthorize("hasRole('ADMIN')")
        mockMvc.perform(post("/api/farms")
                        .header("Authorization", "Bearer " + operarioToken)
                        .contentType("application/json")
                        .content("{\"name\":\"Finca Test\",\"reservoirCapacity\":100,\"farmSize\":50}"))
                .andExpect(status().isForbidden());
    }

    // =========================================================================
    // Utilitario: Construye un JWT firmado con el secreto de test
    // =========================================================================

    /**
     * Genera un JWT directamente con jjwt, usando el mismo secreto configurado
     * en application-test.properties. Permite controlar la expiración para
     * simular tokens válidos y expirados sin depender de JwtService.
     *
     * @param username    Subject del token.
     * @param role        Claim "rol" (sin prefijo ROLE_).
     * @param expirationMs Duración relativa al momento actual. Negativo = expirado.
     * @return El token JWT compacto.
     */
    private String buildValidToken(String username, String role, long expirationMs) {
        byte[] keyBytes = Decoders.BASE64.decode(TEST_JWT_SECRET);
        SecretKey key = Keys.hmacShaKeyFor(keyBytes);
        return Jwts.builder()
                .setSubject(username)
                .claim("rol", role)
                .setIssuedAt(new Date(System.currentTimeMillis() - Math.abs(expirationMs) - 1000))
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
}
