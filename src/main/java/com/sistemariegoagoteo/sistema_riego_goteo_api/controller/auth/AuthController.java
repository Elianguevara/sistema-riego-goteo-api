package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.auth;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.AuthRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.AuthResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth.RegisterRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.AuthService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.auth.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para gestionar la autenticación y el registro de usuarios.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    /**
     * Servicio de autenticación.
     */
    private final AuthService authService;

    /**
     * Servicio de gestión de usuarios.
     */
    private final UserService userService;

    /**
     * Endpoint para la autenticación de usuarios (login).
     * <p>
     * Permite a los usuarios obtener un token JWT proporcionando sus credenciales
     * válidas.
     * </p>
     *
     * @param authRequest DTO con username y password.
     * @return {@link AuthResponse} con el token JWT si las credenciales son
     *         válidas.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> authenticateUser(@Valid @RequestBody AuthRequest authRequest) {
        AuthResponse authResponse = authService.authenticate(authRequest);
        return ResponseEntity.ok(authResponse);
    }

    /**
     * Endpoint para el registro inicial del usuario Administrador.
     * <p>
     * Este endpoint suele utilizarse en el primer despliegue del sistema.
     * </p>
     *
     * @param registerRequest DTO con los datos del administrador.
     * @return Mensaje de éxito tras el registro.
     */
    @PostMapping("/register/admin")
    public ResponseEntity<String> registerAdmin(@Valid @RequestBody RegisterRequest registerRequest) {
        User adminUser = authService.registerAdmin(registerRequest);
        String successMessage = "Administrador registrado exitosamente con username: " + adminUser.getUsername();
        log.info(successMessage);
        return ResponseEntity.status(HttpStatus.CREATED).body(successMessage);
    }

    /**
     * Endpoint para que un Administrador registre nuevos usuarios
     * (Analista/Operario).
     * <p>
     * Requiere que el solicitante tenga el rol 'ADMIN'.
     * </p>
     *
     * @param registerRequest DTO con los datos del nuevo usuario.
     * @return Mensaje de éxito tras el registro.
     */
    @PostMapping("/register")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> registerUserByAdmin(@Valid @RequestBody RegisterRequest registerRequest) {
        User newUser = userService.registerUserByAdmin(registerRequest);
        String successMessage = "Usuario '" + newUser.getUsername() + "' registrado exitosamente con rol: "
                + newUser.getRol().getRoleName();
        log.info(successMessage);
        return ResponseEntity.status(HttpStatus.CREATED).body(successMessage);
    }
}
