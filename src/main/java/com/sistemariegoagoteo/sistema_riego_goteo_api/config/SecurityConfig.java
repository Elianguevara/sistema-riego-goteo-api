package com.sistemariegoagoteo.sistema_riego_goteo_api.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod; // Para especificar métodos HTTP
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity; // Habilita @PreAuthorize
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer; // Para deshabilitar CSRF
import org.springframework.security.config.http.SessionCreationPolicy; // Para política STATELESS
import org.springframework.security.core.userdetails.UserDetailsService; // Nuestro JpaUserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // Para insertar nuestro filtro antes

import com.sistemariegoagoteo.sistema_riego_goteo_api.config.jwt.JwtAuthenticationFilter;

/**
 * Configuración central de Spring Security.
 * Define beans para codificación de contraseñas, proveedor de autenticación,
 * gestor de autenticación y la cadena de filtros de seguridad HTTP.
 */
@Configuration // Indica que esta clase contiene configuraciones de beans de Spring
@EnableWebSecurity // Habilita la seguridad web de Spring Security
@EnableMethodSecurity // Habilita la seguridad a nivel de método (ej. @PreAuthorize("hasRole('ADMIN')"))
@RequiredArgsConstructor // Lombok: genera constructor con campos final (jwtAuthFilter, userDetailsService)
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter; // Nuestro filtro JWT
    private final UserDetailsService userDetailsService; // Nuestro JpaUserDetailsService

    /**
     * Bean para definir el codificador de contraseñas.
     * Usamos BCrypt, que es el estándar recomendado.
     *
     * @return Una instancia de BCryptPasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Bean para el proveedor de autenticación (AuthenticationProvider).
     * Utiliza DaoAuthenticationProvider, que se integra con UserDetailsService
     * para obtener los detalles del usuario y PasswordEncoder para verificar la contraseña.
     *
     * @return Una instancia de AuthenticationProvider configurada.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // Establece el UserDetailsService personalizado para cargar usuarios
        authProvider.setUserDetailsService(userDetailsService);
        // Establece el PasswordEncoder para comparar contraseñas
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Bean para el gestor de autenticación (AuthenticationManager).
     * Spring Boot 3 lo configura automáticamente a partir de AuthenticationConfiguration.
     * Es necesario para el proceso de autenticación de login.
     *
     * @param config La configuración de autenticación de Spring.
     * @return El AuthenticationManager configurado.
     * @throws Exception Si ocurre un error al obtener el AuthenticationManager.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Bean para configurar la cadena de filtros de seguridad HTTP.
     * Aquí definimos qué peticiones requieren autenticación, qué roles son necesarios,
     * deshabilitamos CSRF, configuramos la gestión de sesiones como STATELESS (para JWT)
     * y añadimos nuestro filtro JWT personalizado.
     *
     * @param http El objeto HttpSecurity para configurar la seguridad web.
     * @return La SecurityFilterChain construida.
     * @throws Exception Si ocurre un error durante la configuración.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Deshabilitar CSRF (Cross-Site Request Forgery) - Común en APIs REST stateless
                .csrf(AbstractHttpConfigurer::disable)

                // Configurar reglas de autorización para las peticiones HTTP
                .authorizeHttpRequests(auth -> auth
                        // Permitir acceso público a los endpoints de login y registro inicial de admin
                        .requestMatchers("/api/auth/login", "/api/auth/register/admin").permitAll()
                         // Permitir acceso público a la documentación de Swagger/OpenAPI (si la usas)
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        // El endpoint de registro general requiere el rol ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ADMIN")
                        // Proteger otros endpoints que requieran roles específicos (EJEMPLOS)
                        // .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // .requestMatchers("/api/analista/**").hasAnyRole("ADMIN", "ANALISTA")
                        // .requestMatchers("/api/operario/**").hasAnyRole("ADMIN", "OPERARIO")
                        // Cualquier otra petición requiere autenticación
                        .anyRequest().authenticated()
                )

                // Configurar la gestión de sesiones
                .sessionManagement(session -> session
                        // Usar política STATELESS: no se crea ni se usa sesión HTTP. Cada request se autentica independientemente (ideal para JWT).
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // Registrar nuestro AuthenticationProvider personalizado
                .authenticationProvider(authenticationProvider())

                // Añadir nuestro filtro JWT ANTES del filtro estándar de autenticación por usuario/contraseña
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // Construir y devolver la cadena de filtros de seguridad
        return http.build();
    }
}
