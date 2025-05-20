package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO simple para actualizar el estado (activo/inactivo) de un usuario.
 */
@Data
@NoArgsConstructor
public class UserStatusUpdateRequest {

    @NotNull(message = "El estado 'activo' no puede ser nulo")
    private Boolean active; // Usamos Boolean para permitir null check por @NotNull
}