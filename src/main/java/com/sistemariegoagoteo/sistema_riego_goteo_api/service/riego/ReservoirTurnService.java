package com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.ReservoirTurnRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.ReservoirTurn;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.WaterSource;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.ReservoirTurnRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.WaterSourceRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservoirTurnService {

    private final ReservoirTurnRepository reservoirTurnRepository;
    private final WaterSourceRepository waterSourceRepository;
    private final AuditService auditService;

    @Transactional
    public ReservoirTurn createReservoirTurn(Integer farmId, Integer waterSourceId, ReservoirTurnRequest request) {
        WaterSource waterSource = waterSourceRepository.findById(waterSourceId)
                .orElseThrow(() -> new ResourceNotFoundException("WaterSource", "id", waterSourceId));

        // Validar que la fuente de agua pertenezca a la finca especificada
        if (!waterSource.getFarm().getId().equals(farmId)) {
            throw new IllegalArgumentException("La fuente de agua ID " + waterSourceId +
                    " no pertenece a la finca ID " + farmId + ".");
        }

        if (request.getEndDatetime().before(request.getStartDatetime())) {
            throw new IllegalArgumentException("La fecha de finalización del turno no puede ser anterior a la fecha de inicio.");
        }

        // Opcional: Validar solapamiento de turnos para la misma fuente de agua
        // List<ReservoirTurn> overlappingTurns = reservoirTurnRepository.findOverlappingTurns(
        // waterSourceId, request.getStartDatetime(), request.getEndDatetime());
        // if (!overlappingTurns.isEmpty()) {
        // throw new IllegalArgumentException("El turno se solapa con turnos existentes para esta fuente de agua.");
        // }

        ReservoirTurn reservoirTurn = new ReservoirTurn();
        reservoirTurn.setWaterSource(waterSource);
        reservoirTurn.setStartDatetime(request.getStartDatetime());
        reservoirTurn.setEndDatetime(request.getEndDatetime());

        log.info("Creando turno de embalse para fuente de agua ID {} desde {} hasta {}",
                waterSourceId, request.getStartDatetime(), request.getEndDatetime());
        return reservoirTurnRepository.save(reservoirTurn);
    }

    @Transactional(readOnly = true)
    public List<ReservoirTurn> getReservoirTurnsByWaterSource(Integer farmId, Integer waterSourceId) {
        WaterSource waterSource = waterSourceRepository.findById(waterSourceId)
                .orElseThrow(() -> new ResourceNotFoundException("WaterSource", "id", waterSourceId));

        if (!waterSource.getFarm().getId().equals(farmId)) {
            throw new ResourceNotFoundException("WaterSource", "id", waterSourceId + " para la finca ID " + farmId);
        }
        // Asumiendo que ReservoirTurnRepository tiene findByWaterSourceOrderByStartDatetimeDesc
        return reservoirTurnRepository.findByWaterSourceOrderByStartDatetimeDesc(waterSource);
    }

    @Transactional(readOnly = true)
    public ReservoirTurn getReservoirTurnById(Integer turnId) {
        return reservoirTurnRepository.findById(turnId)
                .orElseThrow(() -> new ResourceNotFoundException("ReservoirTurn", "id", turnId));
    }

    @Transactional
    public ReservoirTurn updateReservoirTurn(Integer turnId, ReservoirTurnRequest request) {
        ReservoirTurn reservoirTurn = getReservoirTurnById(turnId); // Valida existencia

        // La fuente de agua de un turno existente no se suele cambiar.
        // Si se necesitara, hay que validar la nueva fuente y su finca.

        if (request.getEndDatetime().before(request.getStartDatetime())) {
            throw new IllegalArgumentException("La fecha de finalización del turno no puede ser anterior a la fecha de inicio.");
        }

        // Opcional: Validar solapamiento de turnos al actualizar, excluyendo el turno actual
        // List<ReservoirTurn> overlappingTurns = reservoirTurnRepository.findOverlappingTurnsExcludingCurrent(
        // reservoirTurn.getWaterSource().getId(), request.getStartDatetime(), request.getEndDatetime(), turnId);
        // if (!overlappingTurns.isEmpty()) {
        // throw new IllegalArgumentException("El turno actualizado se solapa con otros turnos existentes.");
        // }

        reservoirTurn.setStartDatetime(request.getStartDatetime());
        reservoirTurn.setEndDatetime(request.getEndDatetime());

        log.info("Actualizando turno de embalse ID {}", turnId);
        return reservoirTurnRepository.save(reservoirTurn);
    }

    @Transactional
    public void deleteReservoirTurn(Integer turnId) {
        ReservoirTurn reservoirTurn = getReservoirTurnById(turnId);
        log.warn("Eliminando turno de embalse ID {}", turnId);
        reservoirTurnRepository.delete(reservoirTurn);
    }
}