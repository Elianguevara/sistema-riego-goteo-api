package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta tras el registro exitoso de un usuario.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterResponse {

    private Long id;
    private String username;
    private String name;
    private String email;
    private String rol;
    private boolean active;
    private String message;
}
