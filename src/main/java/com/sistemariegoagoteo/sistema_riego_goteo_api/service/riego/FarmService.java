package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FarmRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.geocoding.GeocodingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Servicio encargado de la gestión de fincas.
 * <p>
 * Incluye funcionalidades para la creación, actualización, recuperación y
 * eliminación de fincas,
 * integrando servicios de geocodificación automática para obtener coordenadas
 * basadas en la ubicación.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FarmService {

    /**
     * Repositorio para la persistencia de datos de fincas.
     */
    private final FarmRepository farmRepository;

    /**
     * Servicio de auditoría para registrar cambios en las fincas.
     */
    private final AuditService auditService;

    /**
     * Servicio de geocodificación externa para obtener coordenadas
     * (latitud/longitud).
     */
    private final GeocodingService geocodingService;

    /**
     * Crea una nueva finca en el sistema.
     * Si no se proporcionan coordenadas, intenta obtenerlas automáticamente vía
     * geocodificación.
     *
     * @param farmRequest DTO con los datos de la finca a crear.
     * @return La entidad Farm persistida.
     */
    @Transactional
    public Farm createFarm(FarmRequest farmRequest) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Farm farm = new Farm();
        farm.setName(farmRequest.getName());
        farm.setLocation(farmRequest.getLocation());
        farm.setReservoirCapacity(farmRequest.getReservoirCapacity());
        farm.setFarmSize(farmRequest.getFarmSize());

        if (farmRequest.getLatitude() == null || farmRequest.getLongitude() == null) {
            log.info("No se proveyeron coordenadas para la finca '{}'. Intentando geocodificar...",
                    farmRequest.getName());
            geocodingService.getCoordinates(farmRequest.getLocation()).ifPresent(coords -> {
                farm.setLatitude(coords.latitude());
                farm.setLongitude(coords.longitude());
                log.info("Geocodificación exitosa. Lat: {}, Lon: {}", coords.latitude(), coords.longitude());
            });
        } else {
            farm.setLatitude(farmRequest.getLatitude());
            farm.setLongitude(farmRequest.getLongitude());
        }

        Farm savedFarm = farmRepository.save(farm);
        auditService.logChange(currentUser, "CREATE", Farm.class.getSimpleName(), "all", null,
                "Nueva finca ID: " + savedFarm.getId());
        return savedFarm;
    }

    @Transactional
    public Farm updateFarm(Integer farmId, FarmRequest farmRequest) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Farm farm = getFarmById(farmId);

        // --- LÓGICA DE AUDITORÍA IMPLEMENTADA ---
        if (!Objects.equals(farm.getName(), farmRequest.getName())) {
            auditService.logChange(currentUser, "UPDATE", Farm.class.getSimpleName(), "name", farm.getName(),
                    farmRequest.getName());
            farm.setName(farmRequest.getName());
        }
        if (farm.getReservoirCapacity().compareTo(farmRequest.getReservoirCapacity()) != 0) {
            auditService.logChange(currentUser, "UPDATE", Farm.class.getSimpleName(), "reservoirCapacity",
                    Objects.toString(farm.getReservoirCapacity()),
                    Objects.toString(farmRequest.getReservoirCapacity()));
            farm.setReservoirCapacity(farmRequest.getReservoirCapacity());
        }
        if (farm.getFarmSize().compareTo(farmRequest.getFarmSize()) != 0) {
            auditService.logChange(currentUser, "UPDATE", Farm.class.getSimpleName(), "farmSize",
                    Objects.toString(farm.getFarmSize()), Objects.toString(farmRequest.getFarmSize()));
            farm.setFarmSize(farmRequest.getFarmSize());
        }

        boolean locationChanged = !Objects.equals(farm.getLocation(), farmRequest.getLocation());
        if (locationChanged) {
            auditService.logChange(currentUser, "UPDATE", Farm.class.getSimpleName(), "location", farm.getLocation(),
                    farmRequest.getLocation());
            farm.setLocation(farmRequest.getLocation());
        }

        if (locationChanged || farm.getLatitude() == null || farm.getLongitude() == null) {
            log.info("La ubicación de la finca ID {} cambió o no tiene coordenadas. Re-geocodificando...", farmId);
            geocodingService.getCoordinates(farmRequest.getLocation()).ifPresentOrElse(
                    coords -> {
                        if (!Objects.equals(farm.getLatitude(), coords.latitude())) {
                            auditService.logChange(currentUser, "UPDATE", Farm.class.getSimpleName(), "latitude",
                                    Objects.toString(farm.getLatitude()), Objects.toString(coords.latitude()));
                            farm.setLatitude(coords.latitude());
                        }
                        if (!Objects.equals(farm.getLongitude(), coords.longitude())) {
                            auditService.logChange(currentUser, "UPDATE", Farm.class.getSimpleName(), "longitude",
                                    Objects.toString(farm.getLongitude()), Objects.toString(coords.longitude()));
                            farm.setLongitude(coords.longitude());
                        }
                        log.info("Re-geocodificación exitosa. Lat: {}, Lon: {}", coords.latitude(), coords.longitude());
                    },
                    () -> {
                        log.warn("No se pudieron obtener nuevas coordenadas para la ubicación: {}",
                                farmRequest.getLocation());
                        farm.setLatitude(null);
                        farm.setLongitude(null);
                    });
        }

        return farmRepository.save(farm);
    }

    @Transactional(readOnly = true)
    public List<Farm> getAllFarms() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return new ArrayList<>();
        }

        User currentUser = (User) authentication.getPrincipal();
        String username = currentUser.getUsername();

        String role = currentUser.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");

        if (role.equals("ROLE_ADMIN") || role.equals("ROLE_ANALISTA")) {
            return farmRepository.findAll();
        }

        if (role.equals("ROLE_OPERARIO")) {
            return farmRepository.findFarmsByUsername(username);
        }

        return new ArrayList<>();
    }

    @Transactional(readOnly = true)
    public Farm getFarmById(Integer farmId) {
        return farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));
    }

    @Transactional
    public void deleteFarm(Integer farmId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Farm farm = getFarmById(farmId);

        // Desvincular a todos los usuarios asignados a esta finca antes de eliminarla
        // para evitar errores de Foreign Key constraint en la tabla user_farm
        for (User user : farm.getUsers()) {
            user.getFarms().remove(farm);
        }

        auditService.logChange(currentUser, "DELETE", Farm.class.getSimpleName(), "id", farm.getId().toString(), null);

        farmRepository.delete(farm);
    }

    @Transactional(readOnly = true)
    public List<Farm> findFarmsByUsername(String username) {
        return farmRepository.findFarmsByUsername(username);
    }
}
