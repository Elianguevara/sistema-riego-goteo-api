package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IrrigationSyncBatchRequest {

    // Se podría añadir el ID del dispositivo si fuera necesario para rastreo
    // private String deviceId;

    @NotEmpty(message = "La lista de riegos no puede estar vacía.")
    @NotNull
    private List<@Valid IrrigationSyncItem> irrigations;
}