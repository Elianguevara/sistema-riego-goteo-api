package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.HumiditySensor;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad HumiditySensor.
 */
@Repository
public interface HumiditySensorRepository extends JpaRepository<HumiditySensor, Integer> {
    List<HumiditySensor> findBySector(Sector sector);
    List<HumiditySensor> findBySectorOrderBySensorTypeAsc(Sector sector);
    Optional<HumiditySensor> findBySensorTypeAndSector(String sensorType, Sector sector); // Para validación de unicidad por tipo/sector
}