package com.sistemariegoagoteo.sistema_riego_goteo_api.config;

import com.sistemariegoagoteo.sistema_riego_goteo_api.config.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Clase de configuración principal de Spring Security.
 * Define cómo se maneja la autenticación, autorización, CORS y la protección de endpoints.
 * Implementa una arquitectura Stateless basada en JWT.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Permite usar anotaciones como @PreAuthorize en los controladores
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Lista de orígenes permitidos para CORS, inyectada desde application.properties.
     * Permite flexibilidad para cambiar dominios sin recompilar.
     */
    @Value("#{'${cors.allowed-origins}'.split(',')}")
    private List<String> allowedOrigins;

    /**
     * Define el codificador de contraseñas de la aplicación.
     * Se utiliza BCrypt, que es el estándar actual para hashing seguro.
     *
     * @return Instancia de BCryptPasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura el proveedor de autenticación.
     * Conecta el UserDetailsService (para buscar usuarios en DB) con el PasswordEncoder
     * (para verificar contraseñas).
     *
     * @return El AuthenticationProvider configurado.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Expone el AuthenticationManager como un Bean.
     * Es el componente central que procesa las solicitudes de autenticación.
     *
     * @param config Configuración de autenticación de Spring.
     * @return El AuthenticationManager.
     * @throws Exception Si ocurre un error al obtener el manager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Configuración global de CORS (Cross-Origin Resource Sharing).
     * Define qué dominios externos pueden consumir esta API.
     *
     * @return Fuente de configuración de CORS basada en URL.
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Configura los orígenes permitidos desde el archivo de propiedades
        configuration.setAllowedOrigins(allowedOrigins);
        
        // Métodos HTTP permitidos
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Cabeceras permitidas (necesario para enviar el token Authorization)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        
        // Permite credenciales (cookies/headers de auth) si fuera necesario
        configuration.setAllowCredentials(true); 

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Define la cadena de filtros de seguridad (Security Filter Chain).
     * Aquí se establecen las reglas de acceso HTTP, manejo de sesiones y filtros personalizados.
     *
     * @param http Objeto HttpSecurity para configurar la seguridad web.
     * @return La cadena de filtros construida.
     * @throws Exception Si hay errores en la configuración.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. Configuración CORS (usa el bean corsConfigurationSource definido arriba)
                .cors(withDefaults())

                // 2. Deshabilitar CSRF (Cross-Site Request Forgery)
                // No es necesario en APIs REST stateless que usan tokens JWT en headers.
                .csrf(AbstractHttpConfigurer::disable)

                // 3. Reglas de Autorización de Endpoints
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos (Whitelist)
                        .requestMatchers("/api/auth/login").permitAll() // Login abierto
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll() // Documentación API
                        
                        // NOTA: El registro de admins (/register/admin) se ha eliminado de aquí por seguridad.
                        // Solo se debe crear admins mediante DataInitializer o un admin existente.

                        // Todo lo demás requiere autenticación (Token válido)
                        .anyRequest().authenticated()
                )

                // 4. Gestión de Sesiones
                // Se define como STATELESS porque usamos JWT. El servidor no guarda sesión de usuario.
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // 5. Configuración de Filtros
                .authenticationProvider(authenticationProvider())
                // Añadimos nuestro filtro JWT antes del filtro estándar de usuario/contraseña
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}