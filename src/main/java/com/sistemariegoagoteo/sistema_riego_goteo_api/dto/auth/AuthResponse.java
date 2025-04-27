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

    private String token;
    // Podrías añadir más información si fuera necesario, como el tipo de token ("Bearer")
    private String tokenType = "Bearer";
}
