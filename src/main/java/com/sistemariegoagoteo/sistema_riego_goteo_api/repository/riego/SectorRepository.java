package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectorRepository extends JpaRepository<Sector, Integer> {
    Optional<Sector> findByNameAndFarm(String name, Farm farm);
    List<Sector> findByFarm(Farm farm);
    List<Sector> findByFarm_Id(Integer farmId);
    Optional<Sector> findByIdAndFarm_Id(Integer sectorId, Integer farmId);
    boolean existsByEquipment_Id(Integer equipmentId);

    /**
     * NUEVO MÉTODO: Busca todos los sectores cuyo equipo de riego asociado
     * tenga un estado específico (ignorando mayúsculas y minúsculas).
     * @param status El estado del equipo a buscar (ej. "Operativo").
     * @return Una lista de sectores que cumplen con el criterio.
     */
    @Query("SELECT s FROM Sector s WHERE s.equipment IS NOT NULL AND lower(s.equipment.equipmentStatus) = lower(:status)")
    List<Sector> findSectorsByEquipmentStatus(@Param("status") String status);

    /**
     * NUEVO MÉTODO: Cuenta todos los sectores cuyo equipo de riego asociado
     * tenga un estado específico.
     * @param status El estado del equipo a contar (ej. "ACTIVO").
     * @return El número total de sectores que cumplen con el criterio.
     */
    @Query("SELECT count(s) FROM Sector s WHERE s.equipment IS NOT NULL AND lower(s.equipment.equipmentStatus) = lower(:status)")
    long countByEquipmentStatus(@Param("status") String status);
}