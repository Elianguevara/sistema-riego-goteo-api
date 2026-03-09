package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.FertilizationRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Fertilization;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FertilizationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class FertilizationService {

    private final FertilizationRepository fertilizationRepository;
    private final SectorRepository sectorRepository;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @Transactional
    public Fertilization createFertilization(FertilizationRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        Sector sector = sectorRepository.findById(request.getSectorId())
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", request.getSectorId()));

        Fertilization fertilization = new Fertilization();
        fertilization.setSector(sector);
        fertilization.setDate(request.getDate());
        fertilization.setFertilizerType(request.getFertilizerType());
        fertilization.setQuantity(request.getQuantity());
        fertilization.setQuantityUnit(request.getQuantityUnit());

        Fertilization savedFertilization = fertilizationRepository.save(fertilization);

        auditService.logChange(currentUser, "CREATE", Fertilization.class.getSimpleName(), "id", null, savedFertilization.getId().toString());

        // --- NOTIFICACIÓN AL OPERARIO (confirmación) ---
        String msgOperario = String.format(
                "Fertilización registrada exitosamente en sector '%s'. ID: %d | Tipo: %s | Cantidad: %.2f %s.",
                sector.getName(), savedFertilization.getId(),
                savedFertilization.getFertilizerType(),
                savedFertilization.getQuantity(),
                savedFertilization.getQuantityUnit() != null ? savedFertilization.getQuantityUnit().name() : "");
        notificationService.createNotification(currentUser, msgOperario, "FERTILIZATION",
                Long.valueOf(savedFertilization.getId()), "/fertilizations/" + savedFertilization.getId());

        // --- NOTIFICACIÓN A ADMINS ---
        String msgAdmin = String.format("El operario '%s' registró una fertilización en sector '%s' (Finca: '%s'). ID: %d.",
                currentUser.getUsername(), sector.getName(), sector.getFarm().getName(), savedFertilization.getId());
        userRepository.findByRol_RoleName("ADMIN").forEach(admin ->
                notificationService.createNotification(admin, msgAdmin, "FERTILIZATION",
                        Long.valueOf(savedFertilization.getId()), "/fertilizations/" + savedFertilization.getId()));

        log.info("Usuario {} registró fertilización (ID: {}) en sector {}", currentUser.getUsername(), savedFertilization.getId(), sector.getName());
        return savedFertilization;
    }

    @Transactional
    public Fertilization updateFertilization(Integer fertilizationId, FertilizationRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Fertilization fertilization = getFertilizationById(fertilizationId);

        // ... Lógica de auditoría y actualización ...
        if (!Objects.equals(fertilization.getDate(), request.getDate())) {
            auditService.logChange(currentUser, "UPDATE", Fertilization.class.getSimpleName(), "date", Objects.toString(fertilization.getDate(), null), Objects.toString(request.getDate(), null));
            fertilization.setDate(request.getDate());
        }
        if (!Objects.equals(fertilization.getFertilizerType(), request.getFertilizerType())) {
            auditService.logChange(currentUser, "UPDATE", Fertilization.class.getSimpleName(), "fertilizerType", fertilization.getFertilizerType(), request.getFertilizerType());
            fertilization.setFertilizerType(request.getFertilizerType());
        }
        if (fertilization.getQuantity().compareTo(request.getQuantity()) != 0) {
            auditService.logChange(currentUser, "UPDATE", Fertilization.class.getSimpleName(), "quantity", fertilization.getQuantity().toString(), request.getQuantity().toString());
            fertilization.setQuantity(request.getQuantity());
        }
        if (fertilization.getQuantityUnit() != request.getQuantityUnit()) {
            auditService.logChange(currentUser, "UPDATE", Fertilization.class.getSimpleName(), "quantityUnit", fertilization.getQuantityUnit().name(), request.getQuantityUnit().name());
            fertilization.setQuantityUnit(request.getQuantityUnit());
        }

        log.info("Actualizando fertilización ID {}", fertilizationId);
        return fertilizationRepository.save(fertilization);
    }

    @Transactional
    public void deleteFertilization(Integer fertilizationId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Fertilization fertilization = getFertilizationById(fertilizationId);
        auditService.logChange(currentUser, "DELETE", Fertilization.class.getSimpleName(), "id", fertilization.getId().toString(), null);
        log.warn("Eliminando fertilización ID {}", fertilizationId);
        fertilizationRepository.delete(fertilization);
    }

    // --- FIRMA DEL MÉTODO CORREGIDA (solo 1 argumento) ---
    @Transactional(readOnly = true)
    public List<Fertilization> getFertilizationsBySector(Integer sectorId) {
        Sector sector = sectorRepository.findById(sectorId)
                .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", sectorId));
        return fertilizationRepository.findBySectorOrderByDateDesc(sector);
    }

    @Transactional(readOnly = true)
    public Fertilization getFertilizationById(Integer fertilizationId) {
        return fertilizationRepository.findById(fertilizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Fertilization", "id", fertilizationId));
    }
}