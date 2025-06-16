package com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Servicio para manejar operaciones JWT (JSON Web Token).
 * Se encarga de generar, validar y extraer información de los tokens.
 */
@Service
public class JwtService {

    // Inyecta la clave secreta desde application.properties
    @Value("${jwt.secret}")
    private String secretKeyString;

    // Inyecta el tiempo de expiración desde application.properties
    @Value("${jwt.expiration}")
    private long jwtExpiration;

    /**
     * Extrae el nombre de usuario (subject) del token JWT.
     *
     * @param token El token JWT.
     * @return El nombre de usuario contenido en el token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae una claim específica del token JWT usando una función resolver.
     *
     * @param token          El token JWT.
     * @param claimsResolver La función para extraer la claim deseada.
     * @param <T>            El tipo de la claim a extraer.
     * @return La claim extraída.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Genera un token JWT para un usuario específico (UserDetails).
     *
     * @param userDetails Los detalles del usuario para quien se genera el token.
     * @return El token JWT generado como String.
     */
    public String generateToken(UserDetails userDetails) {
        // Puedes añadir claims adicionales si lo necesitas
        Map<String, Object> extraClaims = new HashMap<>();
        // Extraer el rol del UserDetails y añadirlo a los claims
        String rol = userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse("SIN_ROL"); // Un valor por defecto por si acaso

        extraClaims.put("rol", rol);
        // --- FIN DE LA MODIFICACIÓN ---
        return buildToken(extraClaims, userDetails, jwtExpiration);
    }

    /**
     * Genera un token JWT con claims adicionales.
     *
     * @param extraClaims Claims adicionales para incluir en el payload del token.
     * @param userDetails Los detalles del usuario.
     * @param expiration  El tiempo de expiración en milisegundos.
     * @return El token JWT generado.
     */
    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts.builder()
                .setClaims(extraClaims) // Establece las claims adicionales
                .setSubject(userDetails.getUsername()) // Establece el 'subject' (normalmente el username)
                .setIssuedAt(new Date(System.currentTimeMillis())) // Fecha de emisión
                .setExpiration(new Date(System.currentTimeMillis() + expiration)) // Fecha de expiración
                .signWith(getSigningKey(), SignatureAlgorithm.HS512) // Firma el token con la clave secreta y algoritmo HS512
                .compact(); // Construye el token y lo serializa a String
    }

    /**
     * Valida si un token JWT es válido para un usuario específico.
     * Comprueba que el username coincida y que el token no haya expirado.
     * La validación de la firma se realiza implícitamente al extraer las claims.
     *
     * @param token       El token JWT a validar.
     * @param userDetails Los detalles del usuario contra los que validar.
     * @return true si el token es válido, false en caso contrario.
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (Exception e) {
            // Si ocurre cualquier excepción durante la extracción/validación (token malformado, expirado, firma inválida),
            // consideramos el token inválido.
            // Podrías loggear el error aquí si necesitas más detalle.
             // log.error("Error validating JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Comprueba si un token JWT ha expirado.
     *
     * @param token El token JWT.
     * @return true si el token ha expirado, false si aún es válido.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Extrae la fecha de expiración del token JWT.
     *
     * @param token El token JWT.
     * @return La fecha de expiración como objeto Date.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae todas las claims (payload) del token JWT.
     * Verifica la firma del token en el proceso. Si la firma es inválida o el token
     * está malformado/expirado, lanzará una excepción.
     *
     * @param token El token JWT.
     * @return El objeto Claims que contiene el payload del token.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(Decoders.BASE64.decode(secretKeyString)) // Establece la clave secreta para verificar la firma
                .build() // Construye el parser
                .parseClaimsJws(token) // Parsea el token firmado
                .getBody(); // Obtiene el cuerpo (claims)
    }

    /**
     * Obtiene la clave secreta (SecretKey) utilizada para firmar y verificar los tokens JWT.
     * Decodifica la clave secreta definida en Base64 en application.properties.
     *
     * @return La SecretKey para usar con el algoritmo HS512.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKeyString); // Decodifica la clave desde Base64
        // Crea una SecretKey compatible con algoritmos HMAC-SHA (como HS512)
        return Keys.hmacShaKeyFor(keyBytes);
    }
}