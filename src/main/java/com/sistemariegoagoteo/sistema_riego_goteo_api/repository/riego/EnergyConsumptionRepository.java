package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.EnergyConsumption;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad EnergyConsumption.
 */
@Repository
public interface EnergyConsumptionRepository extends JpaRepository<EnergyConsumption, Integer> {
    List<EnergyConsumption> findByFarm(Farm farm);
    List<EnergyConsumption> findByConsumptionDate(Date date);
    List<EnergyConsumption> findByFarmAndConsumptionDateBetween(Farm farm, Date startDate, Date endDate);
    List<EnergyConsumption> findByFarmOrderByConsumptionDateDesc(Farm farm);
    Optional<EnergyConsumption> findByFarmAndConsumptionDateAndEnergyType(Farm farm, Date consumptionDate, String energyType);
    List<EnergyConsumption> findByFarmAndConsumptionDateBetweenOrderByConsumptionDateDesc(Farm farm, Date startDate, Date endDate);
}