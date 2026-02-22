package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Maintenance;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    List<Maintenance> findByIrrigationEquipmentAndDateBetween(IrrigationEquipment irrigationEquipment, Date startDate,
            Date endDate);

    List<Maintenance> findByIrrigationEquipmentOrderByDateDesc(IrrigationEquipment irrigationEquipment);

    // Podrías añadir otros métodos de búsqueda si son necesarios, por ejemplo, por
    // rango de fechas:
    // List<Maintenance>
    // findByIrrigationEquipmentAndDateBetweenOrderByDateDesc(IrrigationEquipment
    // irrigationEquipment, Date startDate, Date endDate);
    long countByIrrigationEquipment_Farm_IdAndDateBetween(Integer farmId, Date startDate, Date endDate);

    @Query("SELECT m.date as datetime, 'MANTENIMIENTO' as type, m.description as description, " +
            "CONCAT('Equipo: ', m.irrigationEquipment.name) as location, 'N/A' as userName " +
            "FROM Maintenance m WHERE m.irrigationEquipment.farm.id = :farmId AND m.date BETWEEN :startDate AND :endDate")
    List<com.sistemariegoagoteo.sistema_riego_goteo_api.dto.report.projection.OperationLogProjection> getMaintenanceLogs(
            @Param("farmId") Integer farmId, @Param("startDate") Date startDate, @Param("endDate") Date endDate);
}