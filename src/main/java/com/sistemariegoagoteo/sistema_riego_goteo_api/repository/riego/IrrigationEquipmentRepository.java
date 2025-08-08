package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad IrrigationEquipment.
 */
@Repository
public interface IrrigationEquipmentRepository extends JpaRepository<IrrigationEquipment, Integer> {
    Optional<IrrigationEquipment> findByNameAndFarm(String name, Farm farm);
    List<IrrigationEquipment> findByFarm(Farm farm); // Ya la tenías
    List<IrrigationEquipment> findByFarm_Id(Integer farmId); // Alternativa por ID
    Optional<IrrigationEquipment> findByIdAndFarm_Id(Integer equipmentId, Integer farmId);
    List<IrrigationEquipment> findByEquipmentTypeAndFarm(String equipmentType, Farm farm);
    @Query("SELECT e.equipmentStatus, COUNT(e) FROM IrrigationEquipment e GROUP BY e.equipmentStatus")
    List<Object[]> countByStatus();// Ya la tenías
}