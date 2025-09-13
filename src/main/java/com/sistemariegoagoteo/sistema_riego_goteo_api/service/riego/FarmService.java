package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FarmRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.geocoding.GeocodingService; // <-- 1. IMPORTAR NUEVO SERVICIO
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

@Service
@RequiredArgsConstructor
@Slf4j // <-- AÑADIR ANOTACIÓN SLF4J
public class FarmService {

    private final FarmRepository farmRepository;
    private final AuditService auditService;
    private final GeocodingService geocodingService; // <-- 2. INYECTAR EL SERVICIO

    @Transactional
    public Farm createFarm(FarmRequest farmRequest) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Farm farm = new Farm();
        farm.setName(farmRequest.getName());
        farm.setLocation(farmRequest.getLocation());
        farm.setReservoirCapacity(farmRequest.getReservoirCapacity());
        farm.setFarmSize(farmRequest.getFarmSize());

        // --- 3. LÓGICA DE GEOCODIFICACIÓN ---
        // Si el usuario no provee latitud/longitud, intentamos obtenerlas desde la ubicación.
        if (farmRequest.getLatitude() == null || farmRequest.getLongitude() == null) {
            log.info("No se proveyeron coordenadas para la finca '{}'. Intentando geocodificar desde la ubicación...", farmRequest.getName());
            geocodingService.getCoordinates(farmRequest.getLocation()).ifPresent(coords -> {
                farm.setLatitude(coords.latitude());
                farm.setLongitude(coords.longitude());
                log.info("Geocodificación exitosa. Lat: {}, Lon: {}", coords.latitude(), coords.longitude());
            });
        } else {
            farm.setLatitude(farmRequest.getLatitude());
            farm.setLongitude(farmRequest.getLongitude());
        }
        // ------------------------------------

        Farm savedFarm = farmRepository.save(farm);

        // ... (resto de la auditoría)
        auditService.logChange(currentUser, "CREATE", Farm.class.getSimpleName(), "all", null, "Nueva finca ID: " + savedFarm.getId());

        return savedFarm;
    }

    // ... (getAllFarms y getFarmById sin cambios)

    @Transactional
    public Farm updateFarm(Integer farmId, FarmRequest farmRequest) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Farm farm = getFarmById(farmId);

        // ... (lógica de auditoría existente)

        farm.setName(farmRequest.getName());
        farm.setLocation(farmRequest.getLocation());
        farm.setReservoirCapacity(farmRequest.getReservoirCapacity());
        farm.setFarmSize(farmRequest.getFarmSize());

        // --- 4. LÓGICA DE GEOCODIFICACIÓN EN LA ACTUALIZACIÓN ---
        // Si la ubicación cambia, o si no había coordenadas, las recalculamos.
        boolean locationChanged = !Objects.equals(farm.getLocation(), farmRequest.getLocation());
        if (locationChanged || farm.getLatitude() == null || farm.getLongitude() == null) {
            log.info("La ubicación de la finca ID {} cambió. Re-geocodificando...", farmId);
            geocodingService.getCoordinates(farmRequest.getLocation()).ifPresentOrElse(
                    coords -> {
                        farm.setLatitude(coords.latitude());
                        farm.setLongitude(coords.longitude());
                        log.info("Re-geocodificación exitosa. Lat: {}, Lon: {}", coords.latitude(), coords.longitude());
                    },
                    () -> {
                        // Si no se encuentra, borramos las coordenadas anteriores para evitar inconsistencias
                        farm.setLatitude(null);
                        farm.setLongitude(null);
                        log.warn("No se pudieron obtener nuevas coordenadas para la ubicación: {}", farmRequest.getLocation());
                    }
            );
        }
        // --------------------------------------------------------

        return farmRepository.save(farm);
    }

    // ... (deleteFarm y findFarmsByUsername sin cambios)
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

        auditService.logChange(currentUser, "DELETE", Farm.class.getSimpleName(), "id", farm.getId().toString(), null);

        farmRepository.delete(farm);
    }

    @Transactional(readOnly = true)
    public List<Farm> findFarmsByUsername(String username) {
        return farmRepository.findFarmsByUsername(username);
    }
}