package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de actualización de datos de un usuario por parte del Admin.
 * Permite actualizar nombre y email. No permite cambiar rol ni contraseña directamente aquí.
 */
@Data
@NoArgsConstructor
public class UserUpdateRequest {

    @NotBlank(message = "El nombre completo no puede estar vacío")
    @Size(min = 3, max = 100, message = "El nombre debe tener entre 3 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El formato del email no es válido")
    @Size(max = 100, message = "El email no puede exceder los 100 caracteres")
    private String email;

    // Nota: No incluimos 'activo' aquí, se manejará con un endpoint específico.
    // Nota: No incluimos 'rol' aquí, cambiar roles podría requerir lógica adicional.
    // Nota: No incluimos 'password' aquí, debe ser un proceso separado y seguro.
}