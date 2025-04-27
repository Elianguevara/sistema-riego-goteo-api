package com.sistemariegoagoteo.sistema_riego_goteo_api.config.jwt;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // Para logging (opcional pero recomendado)
import org.springframework.lang.NonNull; // Para indicar que los parámetros no deben ser nulos
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter; // Asegura que el filtro se ejecute solo una vez por request

import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.JwtService;

import java.io.IOException;

/**
 * Filtro de Spring Security que intercepta las peticiones HTTP para procesar el token JWT.
 * Se ejecuta una vez por cada petición para validar el token y establecer la autenticación
 * en el contexto de seguridad si el token es válido.
 */
@Component // Marca esta clase como un componente de Spring para que pueda ser inyectado
@RequiredArgsConstructor // Lombok: genera constructor con campos final (jwtService, userDetailsService)
@Slf4j // Lombok: proporciona un logger SLF4J estático llamado 'log'
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService; // Nuestro JpaUserDetailsService será inyectado aquí

    /**
     * Lógica principal del filtro.
     *
     * @param request     La petición HTTP entrante.
     * @param response    La respuesta HTTP.
     * @param filterChain Cadena de filtros de Spring Security.
     * @throws ServletException Si ocurre un error relacionado con el servlet.
     * @throws IOException      Si ocurre un error de entrada/salida.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain // Permite pasar la request al siguiente filtro
    ) throws ServletException, IOException {

        // 1. Obtener la cabecera Authorization
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String username;

        // 2. Verificar si la cabecera existe y tiene el formato correcto ("Bearer <token>")
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Si no hay token o el formato es incorrecto, pasar al siguiente filtro y salir
            filterChain.doFilter(request, response);
            return;
        }

        // 3. Extraer el token JWT (quitando el prefijo "Bearer ")
        jwt = authHeader.substring(7); // Longitud de "Bearer "

        try {
            // 4. Extraer el username del token usando JwtService
            username = jwtService.extractUsername(jwt);

            // 5. Validar el token:
            //    - El username no es nulo.
            //    - No hay ya una autenticación establecida en el SecurityContext (evita reprocesar)
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Cargar los detalles del usuario desde la BD usando UserDetailsService
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                // Validar el token contra los UserDetails cargados
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    // Si el token es válido, crear un objeto de autenticación
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails, // Principal (el objeto UserDetails)
                            null, // Credenciales (no necesarias para JWT ya validado)
                            userDetails.getAuthorities() // Autoridades (roles)
                    );
                    // Añadir detalles adicionales de la petición web a la autenticación
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    // Establecer la autenticación en el SecurityContextHolder
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    log.debug("Usuario autenticado exitosamente: {}", username); // Log de debug
                } else {
                     log.warn("Token JWT inválido para el usuario: {}", username); // Log de advertencia
                }
            }
        } catch (Exception e) {
            // Capturar cualquier excepción durante la validación del token (expirado, malformado, etc.)
            log.error("Error al procesar el token JWT: {}", e.getMessage());
            // No establecer autenticación si hay error
            SecurityContextHolder.clearContext(); // Limpiar contexto por seguridad
        }


        // 6. Pasar la petición al siguiente filtro en la cadena
        filterChain.doFilter(request, response);
    }
}
