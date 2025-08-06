package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.EnergyConsumptionRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.EnergyConsumption;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User; // <-- IMPORTAR
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.EnergyConsumptionRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder; // <-- IMPORTAR
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;
import java.util.Objects; // <-- IMPORTAR

@Service
@RequiredArgsConstructor
@Slf4j
public class EnergyConsumptionService {

    private final EnergyConsumptionRepository energyConsumptionRepository;
    private final FarmRepository farmRepository;
    private final AuditService auditService;

    @Transactional
    public EnergyConsumption createEnergyConsumption(Integer farmId, EnergyConsumptionRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        // Validación de duplicados
        energyConsumptionRepository.findByFarmAndConsumptionDateAndEnergyType(farm, request.getConsumptionDate(), request.getEnergyType())
            .ifPresent(ec -> {
                throw new IllegalArgumentException(
                    "Ya existe un registro de consumo de energía para la finca '" + farm.getName() +
                    "' en la fecha " + request.getConsumptionDate() +
                    " y tipo de energía '" + request.getEnergyType() + "'."
                );
            });

        EnergyConsumption energyConsumption = new EnergyConsumption();
        energyConsumption.setFarm(farm);
        energyConsumption.setConsumptionDate(request.getConsumptionDate());
        energyConsumption.setKwhConsumed(request.getKwhConsumed().setScale(2, RoundingMode.HALF_UP));
        energyConsumption.setEnergyType(request.getEnergyType());

        EnergyConsumption savedConsumption = energyConsumptionRepository.save(energyConsumption);

        // --- AUDITORÍA DE CREACIÓN ---
        auditService.logChange(currentUser, "CREATE", EnergyConsumption.class.getSimpleName(), "kwhConsumed", null, savedConsumption.getKwhConsumed().toString());
        auditService.logChange(currentUser, "CREATE", EnergyConsumption.class.getSimpleName(), "energyType", null, savedConsumption.getEnergyType());
        auditService.logChange(currentUser, "CREATE", EnergyConsumption.class.getSimpleName(), "consumptionDate", null, savedConsumption.getConsumptionDate().toString());

        log.info("Registrando consumo de energía para finca ID {} en fecha {}: {} kWh, tipo {}",
                farmId, request.getConsumptionDate(), energyConsumption.getKwhConsumed(), request.getEnergyType());
        return savedConsumption;
    }

    @Transactional
    public EnergyConsumption updateEnergyConsumption(Integer consumptionId, EnergyConsumptionRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        EnergyConsumption energyConsumption = getEnergyConsumptionById(consumptionId);

        // --- AUDITORÍA DE ACTUALIZACIÓN ---
        if (!Objects.equals(energyConsumption.getConsumptionDate(), request.getConsumptionDate())) {
             auditService.logChange(currentUser, "UPDATE", EnergyConsumption.class.getSimpleName(), "consumptionDate", Objects.toString(energyConsumption.getConsumptionDate(), null), Objects.toString(request.getConsumptionDate(), null));
        }
        if (!Objects.equals(energyConsumption.getEnergyType(), request.getEnergyType())) {
             auditService.logChange(currentUser, "UPDATE", EnergyConsumption.class.getSimpleName(), "energyType", energyConsumption.getEnergyType(), request.getEnergyType());
        }
        if (energyConsumption.getKwhConsumed().compareTo(request.getKwhConsumed()) != 0) {
             auditService.logChange(currentUser, "UPDATE", EnergyConsumption.class.getSimpleName(), "kwhConsumed", energyConsumption.getKwhConsumed().toString(), request.getKwhConsumed().toString());
        }
        
        // ... (Validación de duplicados)

        energyConsumption.setConsumptionDate(request.getConsumptionDate());
        energyConsumption.setKwhConsumed(request.getKwhConsumed().setScale(2, RoundingMode.HALF_UP));
        energyConsumption.setEnergyType(request.getEnergyType());

        log.info("Actualizando consumo de energía ID {}", consumptionId);
        return energyConsumptionRepository.save(energyConsumption);
    }

    @Transactional
    public void deleteEnergyConsumption(Integer consumptionId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        EnergyConsumption energyConsumption = getEnergyConsumptionById(consumptionId);

        // --- AUDITORÍA DE BORRADO ---
        auditService.logChange(currentUser, "DELETE", EnergyConsumption.class.getSimpleName(), "id", energyConsumption.getId().toString(), null);

        log.warn("Eliminando consumo de energía ID {}", consumptionId);
        energyConsumptionRepository.delete(energyConsumption);
    }

    // --- MÉTODOS GET (SIN CAMBIOS) ---
    
    @Transactional(readOnly = true)
    public List<EnergyConsumption> getEnergyConsumptionsByFarm(Integer farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));
        return energyConsumptionRepository.findByFarmOrderByConsumptionDateDesc(farm);
    }

    @Transactional(readOnly = true)
    public EnergyConsumption getEnergyConsumptionById(Integer consumptionId) {
        return energyConsumptionRepository.findById(consumptionId)
                .orElseThrow(() -> new ResourceNotFoundException("EnergyConsumption", "id", consumptionId));
    }
}