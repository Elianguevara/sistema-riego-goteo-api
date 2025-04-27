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
    private String nombre;
    private String username;
    private String email;
    private String nombreRol; // Nombre del rol
    private boolean activo;
    private java.util.Date fechaUltimoLogin; // Puede ser null

    // Constructor para facilitar la conversión desde la entidad User
    public UserResponse(User user) {
        this.id = user.getId();
        this.nombre = user.getNombre();
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.nombreRol = user.getRol() != null ? user.getRol().getNombreRol() : null; // Manejo de rol nulo
        this.activo = user.isActivo();
        this.fechaUltimoLogin = user.getFechaUltimoLogin();
    }
}