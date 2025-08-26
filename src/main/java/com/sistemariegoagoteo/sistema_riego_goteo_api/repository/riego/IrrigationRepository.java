package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio para la entidad Irrigation.
 */
@Repository
public interface IrrigationRepository extends JpaRepository<Irrigation, Integer> {
    List<Irrigation> findBySector(Sector sector);
    List<Irrigation> findByEquipment(IrrigationEquipment equipment);
    List<Irrigation> findByStartDatetimeBetween(Date startDate, Date endDate);
    List<Irrigation> findBySectorAndStartDatetimeBetween(Sector sector, Date startDate, Date endDate);
    List<Irrigation> findBySectorOrderByStartDatetimeDesc(Sector sector);
    Optional<Irrigation> findByLocalMobileId(String localMobileId); // Nuevo método

    // --- MÉTODO AÑADIDO PARA LA VISTA DE CALENDARIO ---
    /**
     * Busca todos los riegos que pertenecen a una lista de sectores y que ocurrieron
     * dentro de un rango de fechas específico.
     * @param sectors Lista de sectores a buscar.
     * @param startDate Fecha de inicio del rango.
     * @param endDate Fecha de fin del rango.
     * @return Una lista de riegos que cumplen con los criterios.
     */
    List<Irrigation> findBySectorInAndStartDatetimeBetween(List<Sector> sectors, Date startDate, Date endDate);
}