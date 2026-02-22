package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO para la solicitud de autenticación (login).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthRequest {

    /**
     * Nombre de usuario para la autenticación.
     */
    @NotBlank(message = "El nombre de usuario no puede estar vacío")
    private String username;

    /**
     * Contraseña del usuario.
     */
    @NotBlank(message = "La contraseña no puede estar vacía")
    private String password;
}
