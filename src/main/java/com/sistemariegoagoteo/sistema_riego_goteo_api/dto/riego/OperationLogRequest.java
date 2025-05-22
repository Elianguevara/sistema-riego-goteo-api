package com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
public class OperationLogRequest {

    @NotNull(message = "La fecha y hora de inicio de la operación son requeridas.")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date startDatetime;

    // endDatetime puede ser nulo si se está registrando el inicio de una operación
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", timezone = "UTC")
    private Date endDatetime;

    // farmId vendrá de la ruta del controlador
    // Si se quisiera añadir más contexto, como un tipo de operación o descripción, iría aquí.
}