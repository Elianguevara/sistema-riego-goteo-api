package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.EnergyConsumptionRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.EnergyConsumption;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.EnergyConsumptionRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.FarmRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EnergyConsumptionService {

    private final EnergyConsumptionRepository energyConsumptionRepository;
    private final FarmRepository farmRepository;

    @Transactional
    public EnergyConsumption createEnergyConsumption(Integer farmId, EnergyConsumptionRequest request) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));

        // Opcional: Validar si ya existe un registro de consumo para esta finca en esta fecha y tipo de energía
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

        log.info("Registrando consumo de energía para finca ID {} en fecha {}: {} kWh, tipo {}",
                farmId, request.getConsumptionDate(), energyConsumption.getKwhConsumed(), request.getEnergyType());
        return energyConsumptionRepository.save(energyConsumption);
    }

    @Transactional(readOnly = true)
    public List<EnergyConsumption> getEnergyConsumptionsByFarm(Integer farmId) {
        Farm farm = farmRepository.findById(farmId)
                .orElseThrow(() -> new ResourceNotFoundException("Farm", "id", farmId));
        // Asumiendo que EnergyConsumptionRepository tiene findByFarmOrderByConsumptionDateDesc
        return energyConsumptionRepository.findByFarmOrderByConsumptionDateDesc(farm);
    }

    @Transactional(readOnly = true)
    public EnergyConsumption getEnergyConsumptionById(Integer consumptionId) {
        return energyConsumptionRepository.findById(consumptionId)
                .orElseThrow(() -> new ResourceNotFoundException("EnergyConsumption", "id", consumptionId));
    }

    @Transactional
    public EnergyConsumption updateEnergyConsumption(Integer consumptionId, EnergyConsumptionRequest request) {
        EnergyConsumption energyConsumption = getEnergyConsumptionById(consumptionId); // Valida existencia

        // La finca de un registro de consumo no debería cambiar.
        // Validar si al cambiar fecha o tipo, se genera un duplicado (excluyendo el actual)
        if (!energyConsumption.getConsumptionDate().equals(request.getConsumptionDate()) ||
            !energyConsumption.getEnergyType().equalsIgnoreCase(request.getEnergyType())) {
            energyConsumptionRepository.findByFarmAndConsumptionDateAndEnergyType(
                    energyConsumption.getFarm(), request.getConsumptionDate(), request.getEnergyType())
                .ifPresent(ec -> {
                    if (!ec.getId().equals(consumptionId)) { // Asegurarse que no sea el mismo registro
                        throw new IllegalArgumentException(
                            "Ya existe otro registro de consumo de energía para la finca '" + energyConsumption.getFarm().getName() +
                            "' en la fecha " + request.getConsumptionDate() +
                            " y tipo de energía '" + request.getEnergyType() + "'."
                        );
                    }
                });
        }


        energyConsumption.setConsumptionDate(request.getConsumptionDate());
        energyConsumption.setKwhConsumed(request.getKwhConsumed().setScale(2, RoundingMode.HALF_UP));
        energyConsumption.setEnergyType(request.getEnergyType());

        log.info("Actualizando consumo de energía ID {}", consumptionId);
        return energyConsumptionRepository.save(energyConsumption);
    }

    @Transactional
    public void deleteEnergyConsumption(Integer consumptionId) {
        EnergyConsumption energyConsumption = getEnergyConsumptionById(consumptionId);
        log.warn("Eliminando consumo de energía ID {}", consumptionId);
        energyConsumptionRepository.delete(energyConsumption);
    }
}