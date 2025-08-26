package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FarmRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import lombok.RequiredArgsConstructor;
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
public class FarmService {

    private final FarmRepository farmRepository;
    private final AuditService auditService;

    @Transactional
    public Farm createFarm(FarmRequest farmRequest) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Farm farm = new Farm();
        farm.setName(farmRequest.getName());
        farm.setLocation(farmRequest.getLocation());
        farm.setReservoirCapacity(farmRequest.getReservoirCapacity());
        farm.setFarmSize(farmRequest.getFarmSize());

        Farm savedFarm = farmRepository.save(farm);

        auditService.logChange(currentUser, "CREATE", Farm.class.getSimpleName(), "name", null, savedFarm.getName());
        auditService.logChange(currentUser, "CREATE", Farm.class.getSimpleName(), "location", null, savedFarm.getLocation());
        auditService.logChange(currentUser, "CREATE", Farm.class.getSimpleName(), "reservoirCapacity", null, savedFarm.getReservoirCapacity().toString());
        auditService.logChange(currentUser, "CREATE", Farm.class.getSimpleName(), "farmSize", null, savedFarm.getFarmSize().toString());

        auditService.recordModificationForSync(Farm.class.getSimpleName(), savedFarm.getId());

        return savedFarm;
    }

    /**
     * Devuelve fincas según el rol del usuario autenticado.
     * ADMIN/ANALISTA: Todas las fincas.
     * OPERARIO: Solo las fincas asignadas.
     */
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
    public Farm updateFarm(Integer farmId, FarmRequest farmRequest) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Farm farm = getFarmById(farmId);

        if (!Objects.equals(farm.getName(), farmRequest.getName())) {
            auditService.logChange(currentUser, "UPDATE", Farm.class.getSimpleName(), "name", farm.getName(), farmRequest.getName());
        }
        if (!Objects.equals(farm.getLocation(), farmRequest.getLocation())) {
            auditService.logChange(currentUser, "UPDATE", Farm.class.getSimpleName(), "location", farm.getLocation(), farmRequest.getLocation());
        }
        if (farm.getReservoirCapacity().compareTo(farmRequest.getReservoirCapacity()) != 0) {
            auditService.logChange(currentUser, "UPDATE", Farm.class.getSimpleName(), "reservoirCapacity", farm.getReservoirCapacity().toString(), farmRequest.getReservoirCapacity().toString());
        }
        if (farm.getFarmSize().compareTo(farmRequest.getFarmSize()) != 0) {
            auditService.logChange(currentUser, "UPDATE", Farm.class.getSimpleName(), "farmSize", farm.getFarmSize().toString(), farmRequest.getFarmSize().toString());
        }

        farm.setName(farmRequest.getName());
        farm.setLocation(farmRequest.getLocation());
        farm.setReservoirCapacity(farmRequest.getReservoirCapacity());
        farm.setFarmSize(farmRequest.getFarmSize());

        Farm updatedFarm = farmRepository.save(farm);

        auditService.recordModificationForSync(Farm.class.getSimpleName(), updatedFarm.getId());

        return updatedFarm;
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
