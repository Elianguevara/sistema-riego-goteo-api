package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Arrays;

@RestController
@RequestMapping("/api")
public class OperationTypeController {

    /**
     * Devuelve la lista de tipos de operación permitidos para la bitácora.
     * Esta lista centralizada permite que todos los clientes (web, móvil)
     * muestren las mismas opciones consistentes.
     *
     * @return Una lista de strings con los tipos de operación estandarizados.
     */
    @GetMapping("/operation-types")
    @PreAuthorize("isAuthenticated()") // Solo para usuarios autenticados
    public ResponseEntity<List<String>> getOperationTypes() {
        List<String> operationTypes = Arrays.asList(
                "Mantenimiento General",
                "Mantenimiento Correctivo",
                "Revisión de Sensores",
                "Aplicación de Fertilizante",
                "Aplicación de Herbicida/Pesticida",
                "Labores de Suelo",
                "Cosecha",
                "Monitoreo de Cultivo",
                "Incidencia Climática",
                "Otros"
        );
        return ResponseEntity.ok(operationTypes);
    }
}