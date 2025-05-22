package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Repositorio para la entidad Irrigation.
 */
@Repository
public interface IrrigationRepository extends JpaRepository<Irrigation, Integer> {
    List<Irrigation> findBySector(Sector sector);
    List<Irrigation> findByEquipment(IrrigationEquipment equipment);
    List<Irrigation> findByStartDatetimeBetween(Date startDate, Date endDate);
    List<Irrigation> findBySectorAndStartDatetimeBetween(Sector sector, Date startDate, Date endDate);
}