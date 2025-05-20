package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar la información de un usuario en las respuestas de la API.
 * Excluye información sensible como la contraseña.
 */
@Data
@NoArgsConstructor
public class UserResponse {
    private Long id;
    private String name;
    private String username;
    private String email;
    private String roleName; // Nombre del rol
    private boolean active;
    private java.util.Date lastLogin; // Puede ser null

    // Constructor para facilitar la conversión desde la entidad User
    public UserResponse(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.roleName = user.getRol() != null ? user.getRol().getRoleName() : null; // Manejo de rol nulo
        this.active = user.isActive();
        this.lastLogin = user.getLastLogin();
    }
}