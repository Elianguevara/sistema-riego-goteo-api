package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.ReservoirTurnRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.ReservoirTurn;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.WaterSource;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User; // <-- IMPORTAR
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.ReservoirTurnRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.WaterSourceRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder; // <-- IMPORTAR
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects; // <-- IMPORTAR

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservoirTurnService {

    private final ReservoirTurnRepository reservoirTurnRepository;
    private final WaterSourceRepository waterSourceRepository;
    private final AuditService auditService;

    @Transactional
    public ReservoirTurn createReservoirTurn(Integer farmId, Integer waterSourceId, ReservoirTurnRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        WaterSource waterSource = waterSourceRepository.findById(waterSourceId)
                .orElseThrow(() -> new ResourceNotFoundException("WaterSource", "id", waterSourceId));

        if (!waterSource.getFarm().getId().equals(farmId)) {
            throw new IllegalArgumentException("La fuente de agua ID " + waterSourceId +
                    " no pertenece a la finca ID " + farmId + ".");
        }

        if (request.getEndDatetime().before(request.getStartDatetime())) {
            throw new IllegalArgumentException("La fecha de finalización del turno no puede ser anterior a la fecha de inicio.");
        }

        ReservoirTurn reservoirTurn = new ReservoirTurn();
        reservoirTurn.setWaterSource(waterSource);
        reservoirTurn.setStartDatetime(request.getStartDatetime());
        reservoirTurn.setEndDatetime(request.getEndDatetime());

        ReservoirTurn savedTurn = reservoirTurnRepository.save(reservoirTurn);
        
        // --- AUDITORÍA DE CREACIÓN ---
        auditService.logChange(currentUser, "CREATE", ReservoirTurn.class.getSimpleName(), "id", null, savedTurn.getId().toString());

        log.info("Creando turno de embalse para fuente de agua ID {} desde {} hasta {}",
                waterSourceId, request.getStartDatetime(), request.getEndDatetime());
        return savedTurn;
    }

    @Transactional
    public ReservoirTurn updateReservoirTurn(Integer turnId, ReservoirTurnRequest request) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ReservoirTurn reservoirTurn = getReservoirTurnById(turnId);

        // --- AUDITORÍA DE ACTUALIZACIÓN ---
        if (!Objects.equals(reservoirTurn.getStartDatetime(), request.getStartDatetime())) {
            auditService.logChange(currentUser, "UPDATE", ReservoirTurn.class.getSimpleName(), "startDatetime", Objects.toString(reservoirTurn.getStartDatetime(), null), Objects.toString(request.getStartDatetime(), null));
        }
        if (!Objects.equals(reservoirTurn.getEndDatetime(), request.getEndDatetime())) {
            auditService.logChange(currentUser, "UPDATE", ReservoirTurn.class.getSimpleName(), "endDatetime", Objects.toString(reservoirTurn.getEndDatetime(), null), Objects.toString(request.getEndDatetime(), null));
        }

        if (request.getEndDatetime().before(request.getStartDatetime())) {
            throw new IllegalArgumentException("La fecha de finalización del turno no puede ser anterior a la fecha de inicio.");
        }

        reservoirTurn.setStartDatetime(request.getStartDatetime());
        reservoirTurn.setEndDatetime(request.getEndDatetime());

        log.info("Actualizando turno de embalse ID {}", turnId);
        return reservoirTurnRepository.save(reservoirTurn);
    }

    @Transactional
    public void deleteReservoirTurn(Integer turnId) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ReservoirTurn reservoirTurn = getReservoirTurnById(turnId);

        // --- AUDITORÍA DE BORRADO ---
        auditService.logChange(currentUser, "DELETE", ReservoirTurn.class.getSimpleName(), "id", reservoirTurn.getId().toString(), null);

        log.warn("Eliminando turno de embalse ID {}", turnId);
        reservoirTurnRepository.delete(reservoirTurn);
    }
    
    // --- MÉTODOS GET (SIN CAMBIOS) ---

    @Transactional(readOnly = true)
    public List<ReservoirTurn> getReservoirTurnsByWaterSource(Integer farmId, Integer waterSourceId) {
        WaterSource waterSource = waterSourceRepository.findById(waterSourceId)
                .orElseThrow(() -> new ResourceNotFoundException("WaterSource", "id", waterSourceId));

        if (!waterSource.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("WaterSource", "id", waterSourceId + " para la finca ID " + farmId);
        }
        return reservoirTurnRepository.findByWaterSourceOrderByStartDatetimeDesc(waterSource);
    }

    @Transactional(readOnly = true)
    public ReservoirTurn getReservoirTurnById(Integer turnId) {
        return reservoirTurnRepository.findById(turnId)
                .orElseThrow(() -> new ResourceNotFoundException("ReservoirTurn", "id", turnId));
    }
}