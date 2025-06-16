package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SelfPasswordUpdateRequest {

  @NotBlank(message = "La contraseña actual es requerida.")
  private String currentPassword;

  @NotBlank(message = "La nueva contraseña no puede estar vacía.")
  @Size(min = 8, max = 100, message = "La nueva contraseña debe tener entre 8 y 100 caracteres.")
  private String newPassword;

  // Campo adicional para confirmación
  @NotBlank(message = "La confirmación de la contraseña es requerida.")
  private String confirmPassword;
}