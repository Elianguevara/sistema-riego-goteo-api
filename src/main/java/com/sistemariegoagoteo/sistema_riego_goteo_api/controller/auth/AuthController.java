package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.auth;

import jakarta.validation.Valid; // Para activar la validación de DTOs
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize; // Para proteger endpoints
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.AuthRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.AuthResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.RegisterRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.AuthService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.UserService;

/**
 * Controlador REST para gestionar la autenticación y el registro de usuarios.
 */
@RestController
@RequestMapping("/api/auth") // Ruta base para todos los endpoints de este controlador
@RequiredArgsConstructor // Lombok: genera constructor con campos final
@Slf4j // Lombok: logger
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    /**
     * Endpoint para la autenticación de usuarios (login).
     * Recibe las credenciales y devuelve un token JWT si son válidas.
     *
     * @param authRequest DTO con username y password. La anotación @Valid activa las validaciones.
     * @return ResponseEntity con el AuthResponse (token) y estado OK (200),
     * o estado UNAUTHORIZED (401) si las credenciales son inválidas,
     * o estado BAD_REQUEST (400) para otros errores de autenticación.
     */
    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody AuthRequest authRequest) {
        try {
            AuthResponse authResponse = authService.authenticate(authRequest);
            return ResponseEntity.ok(authResponse); // 200 OK
        } catch (BadCredentialsException e) {
            log.warn("Intento de login fallido (credenciales inválidas) para: {}", authRequest.getUsername());
            // Devolver un mensaje genérico por seguridad
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Credenciales inválidas."); // 401 Unauthorized
        } catch (RuntimeException e) {
             log.error("Error inesperado durante el login para {}: {}", authRequest.getUsername(), e.getMessage());
            // Captura otros errores (ej. usuario inactivo)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage()); // 400 Bad Request
        }
    }

    /**
     * Endpoint para el registro INICIAL del usuario Administrador.
     * Este endpoint debería ser usado con precaución, idealmente solo una vez
     * o estar protegido de alguna manera adicional (ej. variable de entorno, IP específica).
     *
     * @param registerRequest DTO con los datos del admin. @Valid activa validaciones.
     * @return ResponseEntity con mensaje de éxito y estado CREATED (201),
     * o estado BAD_REQUEST (400) si hay errores (rol incorrecto, datos duplicados, etc.).
     */
    @PostMapping("/register/admin")
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // La validación del rol 'ADMIN' se hace dentro del servicio AuthService
            User adminUser = authService.registerAdmin(registerRequest);
            String successMessage = "Administrador registrado exitosamente con username: " + adminUser.getUsername();
            log.info(successMessage);
            return ResponseEntity.status(HttpStatus.CREATED).body(successMessage); // 201 Created
        } catch (RuntimeException e) {
            // Captura errores específicos de validación o de negocio (ej. usuario ya existe)
            log.error("Error al registrar admin {}: {}", registerRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage()); // 400 Bad Request
        }
    }

    /**
     * Endpoint para que un Administrador registre nuevos usuarios (Analista/Operario).
     * Protegido con @PreAuthorize para asegurar que solo usuarios con rol ADMIN puedan acceder.
     *
     * @param registerRequest DTO con los datos del nuevo usuario. @Valid activa validaciones.
     * @return ResponseEntity con mensaje de éxito y estado CREATED (201),
     * o estado BAD_REQUEST (400) si hay errores.
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')") // Solo accesible por ADMIN
    public ResponseEntity<?> registerUserByAdmin(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // La validación del rol (ANALISTA/OPERARIO) se hace en UserService
            User newUser = userService.registerUserByAdmin(registerRequest);
            String successMessage = "Usuario '" + newUser.getUsername() + "' registrado exitosamente con rol: " + newUser.getRol().getNombreRol();
            log.info(successMessage);
            return ResponseEntity.status(HttpStatus.CREATED).body(successMessage); // 201 Created
        } catch (RuntimeException e) {
            // Captura errores específicos de validación o de negocio
             log.error("Error al registrar usuario {} por admin: {}", registerRequest.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error: " + e.getMessage()); // 400 Bad Request
        }
    }
}
