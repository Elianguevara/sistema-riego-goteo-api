package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class OrganizationConfigDTO {

    @NotBlank(message = "El nombre de la organización es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    private String organizationName;

    @Size(max = 200, message = "La dirección no puede superar los 200 caracteres")
    private String organizationAddress;

    @Size(max = 30, message = "El teléfono no puede superar los 30 caracteres")
    private String organizationPhone;

    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    private String organizationEmail;
}
