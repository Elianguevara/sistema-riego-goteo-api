package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordUpdateRequest {
  @NotBlank @Size(min=8, max=100)
  private String newPassword;
}
