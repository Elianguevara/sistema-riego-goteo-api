package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO para la respuesta de autenticación.
 * Contiene el token JWT generado.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * Token JWT generado tras una autenticación exitosa.
     */
    private String token;

    /**
     * Tipo de token (por defecto "Bearer").
     */
    private String tokenType = "Bearer";

    /**
     * Indica si la cuenta del usuario está activa.
     */
    private boolean active;

    // Constructor adicional para facilitar la creación
    public AuthResponse(String token, String tokenType) {
        this.token = token;
        this.tokenType = tokenType;
    }
}
