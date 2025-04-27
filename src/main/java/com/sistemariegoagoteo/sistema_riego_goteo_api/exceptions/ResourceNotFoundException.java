package com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Excepción personalizada para indicar que un recurso específico no fue encontrado.
 * La anotación @ResponseStatus hace que Spring devuelva automáticamente un código 404 Not Found
 * cuando esta excepción no es capturada explícitamente por un @ExceptionHandler.
 */
@ResponseStatus(value = HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L; // Buena práctica para excepciones serializables

    /**
     * Constructor que genera un mensaje estándar "Recurso no encontrado con campo : 'valor'".
     * @param resourceName Nombre del recurso (ej. "User", "Role", "Finca").
     * @param fieldName Nombre del campo por el que se buscó (ej. "id", "username").
     * @param fieldValue Valor del campo que se buscó.
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s no encontrado con %s : '%s'", resourceName, fieldName, fieldValue));
        // Puedes añadir campos adicionales a la excepción si necesitas más contexto
        // private String resourceName;
        // private String fieldName;
        // private Object fieldValue;
    }

    // Puedes añadir otros constructores si necesitas mensajes diferentes
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
