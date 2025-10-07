package com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface IrrigationRepository extends JpaRepository<Irrigation, Integer> {
    List<Irrigation> findBySector(Sector sector);
    List<Irrigation> findByEquipment(IrrigationEquipment equipment);
    List<Irrigation> findByStartDatetimeBetween(Date startDate, Date endDate);
    List<Irrigation> findBySectorAndStartDatetimeBetween(Sector sector, Date startDate, Date endDate);
    List<Irrigation> findBySectorOrderByStartDatetimeDesc(Sector sector);
    Optional<Irrigation> findByLocalMobileId(String localMobileId);

    List<Irrigation> findBySectorInAndStartDatetimeBetween(List<Sector> sectors, Date startDate, Date endDate);

    // --- MÉTODO NUEVO AÑADIDO ---
    /**
     * Busca todos los riegos de una finca específica dentro de un rango de fechas.
     * Spring Data JPA creará la consulta anidando las propiedades (Sector -> Farm -> Id).
     */
    List<Irrigation> findBySector_Farm_IdAndStartDatetimeBetween(Integer farmId, Date startDate, Date endDate);
}