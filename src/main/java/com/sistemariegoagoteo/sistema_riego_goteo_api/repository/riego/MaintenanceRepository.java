package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Maintenance;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * Repositorio para la entidad Maintenance.
 */
@Repository
public interface MaintenanceRepository extends JpaRepository<Maintenance, Integer> {
    List<Maintenance> findByIrrigationEquipment(IrrigationEquipment irrigationEquipment);
    List<Maintenance> findByDate(Date date);
    List<Maintenance> findByIrrigationEquipmentAndDateBetween(IrrigationEquipment irrigationEquipment, Date startDate, Date endDate);
}