// En SectorRepository.java
package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

// ... imports ...
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Integer> {
    Optional<Sector> findByNameAndFarm(String name, Farm farm);
    List<Sector> findByFarm(Farm farm); // Usará el objeto Farm
    List<Sector> findByFarm_Id(Integer farmId); // Alternativa, buscar por el ID de la finca
    Optional<Sector> findByIdAndFarm_Id(Integer sectorId, Integer farmId);
    boolean existsByEquipment_Id(Integer equipmentId); //  Para la validación
}