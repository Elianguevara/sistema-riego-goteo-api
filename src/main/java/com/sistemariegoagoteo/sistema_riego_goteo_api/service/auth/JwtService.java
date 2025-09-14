package com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth;
import com.sistemariegoagoteo.sistema_riego_goteo_api.config.jwt.JwtConfig; // <-- IMPORTAR
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User; // <-- IMPORTAR USER
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor; // <-- AÑADIR ESTA IMPORTACIÓN
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor // <-- AÑADIR ESTA ANOTACIÓN
public class JwtService {

    // Inyectamos la clase de configuración completa
    private final JwtConfig jwtConfig;

    // Ya no necesitamos los campos individuales con @Value
    // @Value("${jwt.secret}")
    // private String secretKeyString;
    // @Value("${jwt.expiration}")
    // private long jwtExpiration;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

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
        // Usamos el valor desde jwtConfig
        return buildToken(extraClaims, userDetails, jwtConfig.getExpiration());
    }

    private String buildToken(
            Map<String, Object> extraClaims,
            UserDetails userDetails,
            long expiration
    ) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        // Usamos el valor desde jwtConfig
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey()) // El método getSigningKey ya usará la nueva variable
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSigningKey() {
        // Usamos el valor desde jwtConfig
        byte[] keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}