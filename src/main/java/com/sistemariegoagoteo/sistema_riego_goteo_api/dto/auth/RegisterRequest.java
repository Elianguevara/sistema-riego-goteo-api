package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO para la solicitud de registro de un nuevo usuario.
 * Usado tanto para el registro inicial del admin como para el registro
 * de Analistas/Operarios por parte del Admin.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "El nombre completo no puede estar vacío")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El nombre de usuario no puede estar vacío")
    @Size(min = 4, max = 50, message = "El nombre de usuario debe tener entre 4 y 50 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "El nombre de usuario solo puede contener letras, números y guion bajo")
    private String username;

    @NotBlank(message = "La contraseña no puede estar vacía")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    // Podrías añadir validaciones de complejidad de contraseña si lo deseas
    private String password;

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El formato del email no es válido")
    @Size(max = 100, message = "El email no puede exceder los 100 caracteres")
    private String email;

    /**
     * Nombre del rol a asignar (ej. "ADMIN", "ANALISTA", "OPERARIO").
     * La validación específica del rol permitido se hará en el servicio.
     */
    @NotBlank(message = "El rol no puede estar vacío")
    private String rol;
}