package com.sistemariegoagoteo.sistema_riego_goteo_api.controller.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FarmRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FarmResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.FarmService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controlador REST para la gestión de fincas.
 * <p>
 * Proporciona endpoints para realizar operaciones CRUD sobre las fincas,
 * con acceso restringido según el rol del usuario.
 * </p>
 */
@RestController
@RequestMapping("/api/farms")
@RequiredArgsConstructor
@Slf4j
public class FarmController {

    /**
     * Servicio de lógica de negocio para fincas.
     */
    private final FarmService farmService;

    /**
     * Crea una nueva finca.
     * <p>
     * Requiere el rol 'ADMIN'.
     * </p>
     *
     * @param farmRequest DTO con los datos de la finca.
     * @return {@link FarmResponse} con la finca creada.
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FarmResponse> createFarm(@Valid @RequestBody FarmRequest farmRequest) {
        log.info("Solicitud POST para crear finca: {}", farmRequest.getName());
        Farm newFarm = farmService.createFarm(farmRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(new FarmResponse(newFarm));
    }

    /**
     * Obtiene el listado de todas las fincas registradas.
     * <p>
     * Accesible para todos los roles autenticados.
     * </p>
     *
     * @return Lista de {@link FarmResponse}.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    public ResponseEntity<List<FarmResponse>> getAllFarms() {
        log.info("Solicitud GET para obtener fincas");
        List<FarmResponse> farmResponses = farmService.getAllFarms().stream()
                .map(FarmResponse::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(farmResponses);
    }

    /**
     * Obtiene una finca específica por su ID.
     *
     * @param farmId Identificador de la finca.
     * @return {@link FarmResponse} con la información de la finca.
     */
    @GetMapping("/{farmId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALISTA', 'OPERARIO')")
    public ResponseEntity<FarmResponse> getFarmById(@PathVariable Integer farmId) {
        log.info("Solicitud GET para obtener finca con ID: {}", farmId);
        Farm farm = farmService.getFarmById(farmId);
        return ResponseEntity.ok(new FarmResponse(farm));
    }

    /**
     * Actualiza la información de una finca existente.
     * <p>
     * Requiere el rol 'ADMIN'.
     * </p>
     *
     * @param farmId      Identificador de la finca a actualizar.
     * @param farmRequest DTO con los nuevos datos.
     * @return {@link FarmResponse} con la finca actualizada.
     */
    @PutMapping("/{farmId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FarmResponse> updateFarm(@PathVariable Integer farmId,
            @Valid @RequestBody FarmRequest farmRequest) {
        log.info("Solicitud PUT para actualizar finca con ID: {}", farmId);
        Farm updatedFarm = farmService.updateFarm(farmId, farmRequest);
        return ResponseEntity.ok(new FarmResponse(updatedFarm));
    }

    /**
     * Elimina una finca del sistema.
     * <p>
     * Requiere el rol 'ADMIN'.
     * </p>
     *
     * @param farmId Identificador de la finca a eliminar.
     * @return ResponseEntity con estado 204 (No Content).
     */
    @DeleteMapping("/{farmId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteFarm(@PathVariable Integer farmId) {
        log.info("Solicitud DELETE para eliminar finca con ID: {}", farmId);
        farmService.deleteFarm(farmId);
        return ResponseEntity.noContent().build();
    }
}
