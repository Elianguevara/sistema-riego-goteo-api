package com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth;

import com.sistemariegoagoteo.sistema_riego_goteo_api.config.jwt.JwtConfig; // <-- IMPORTAR
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User; // <-- IMPORTAR USER
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.config.SystemConfigService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.config.SecurityConfigDTO;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Servicio encargado de la gestión de tokens JWT (JSON Web Tokens).
 * <p>
 * Proporciona funcionalidades para generar, extraer información y validar
 * tokens,
 * asegurando la comunicación segura entre el cliente y el servidor.
 * </p>
 */
@Service
@RequiredArgsConstructor // <-- AÑADIR ESTA ANOTACIÓN
public class JwtService {

    /**
     * Configuración de JWT inyectada (secreto y tiempo de expiración).
     */
    private final JwtConfig jwtConfig;
    private final SystemConfigService systemConfigService;

    // Ya no necesitamos los campos individuales con @Value
    // @Value("${jwt.secret}")
    // private String secretKeyString;
    // @Value("${jwt.expiration}")
    // private long jwtExpiration;

    /**
     * Extrae el nombre de usuario (subject) de un token JWT.
     *
     * @param token El token JWT.
     * @return El nombre de usuario contenido en el token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae un "claim" específico del token utilizando un resolver.
     *
     * @param <T>            Tipo de dato del claim.
     * @param token          El token JWT.
     * @param claimsResolver Función para resolver el claim deseado.
     * @return El valor del claim extraído.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Genera un token JWT para un usuario con claims adicionales (nombre y rol).
     *
     * @param userDetails Los detalles del usuario para el cual se genera el token.
     * @return El token JWT generado.
     */
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        // --- INICIO DE LA MODIFICACIÓN ---
        if (userDetails instanceof User) {
            User user = (User) userDetails;
            extraClaims.put("name", user.getName()); // <-- AÑADIR EL NOMBRE COMPLETO
        }
        // --- FIN DE LA MODIFICACIÓN ---
        String rol = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse("SIN_ROL");
        extraClaims.put("rol", rol);

        SecurityConfigDTO securityConfig = systemConfigService.getSecurityConfig();
        // Convertir las horas configuradas a milisegundos
        long expirationMs = securityConfig.getSessionDurationHours() * 3600000L;

        return buildToken(extraClaims, userDetails, expirationMs);
    }

    /**
     * Construye físicamente el token JWT con los claims y configuración
     * proporcionada.
     *
     * @param extraClaims Claims adicionales a incluir.
     * @param userDetails Información del usuario.
     * @param expiration  Tiempo de expiración en milisegundos.
     * @return El token JWT compacto.
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * Valida si un token es correcto para un usuario específico y no ha expirado.
     *
     * @param token       El token JWT a validar.
     * @param userDetails Los detalles del usuario esperado.
     * @return {@code true} si el token es válido, {@code false} en caso contrario.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Comprueba si el token ha superado su fecha de expiración.
     *
     * @param token El token JWT.
     * @return {@code true} si el token ha expirado, {@code false} en caso
     *         contrario.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extrae la fecha de expiración de un token.
     *
     * @param token El token JWT.
     * @return La fecha de expiración.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae todos los claims contenidos en el token.
     *
     * @param token El token JWT.
     * @return Objeto Claims con toda la información del token.
     */
    private Claims extractAllClaims(String token) {
        // Usamos el valor desde jwtConfig
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // El método getSigningKey ya usará la nueva variable
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Obtiene la clave de firma a partir del secreto configurado en Base64.
     *
     * @return La clave secreta para firmar/validar tokens.
     */
    private SecretKey getSigningKey() {
        // Usamos el valor desde jwtConfig
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}