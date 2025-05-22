package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class IrrigationEquipmentRequest {

    @NotBlank(message = "El nombre del equipo no puede estar vacío.")
    @Size(max = 100, message = "El nombre del equipo no puede exceder los 100 caracteres.")
    private String name;

    @PositiveOrZero(message = "El caudal medido debe ser un valor positivo o cero.")
    private BigDecimal measuredFlow; // Puede ser nulo si no aplica o no se conoce inicialmente

    @NotNull(message = "Debe indicar si tiene caudalímetro.")
    private Boolean hasFlowMeter;

    @NotBlank(message = "El tipo de equipo no puede estar vacío.")
    @Size(max = 50, message = "El tipo de equipo no puede exceder los 50 caracteres.")
    private String equipmentType; // Ej: Bomba, Válvula Principal, Filtro

    @NotBlank(message = "El estado del equipo no puede estar vacío.")
    @Size(max = 50, message = "El estado del equipo no puede exceder los 50 caracteres.")
    private String equipmentStatus; // Ej: Operativo, Mantenimiento, Averiado

    // farmId se manejará a través de la ruta en el controlador.
}