package com.sistemariegoagoteo.sistema_riego_goteo_api.service.sync;

//import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.riego.IrrigationRequest; // Reutilizamos el DTO de Irrigation
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncBatchRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncItem;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncResultItem;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
//import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User; // Para obtener el usuario
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
//import com.sistemariegoagoteo.sistema_riego_goteo_api.service.riego.IrrigationService; // Para lógica de negocio de riego
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Importante si haces múltiples saves

import java.util.Optional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MobileSyncService {

    private final IrrigationRepository irrigationRepository; // Usar directamente para control de idempotencia
    private final UserRepository userRepository;
    private final SectorRepository sectorRepository;
    private final IrrigationEquipmentRepository equipmentRepository; // Para buscar equipo

    // Podríamos hacer esto transaccional por lote, o manejar transacciones por ítem.
    // Por simplicidad, cada ítem se procesa individualmente.
    @Transactional // Es buena idea que el procesamiento del lote sea transaccional
    public IrrigationSyncResponse processIrrigationBatch(String username, IrrigationSyncBatchRequest batchRequest) {
        userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        List<IrrigationSyncResultItem> results = new ArrayList<>();
        int successfulItems = 0;
        int failedItems = 0;

        for (IrrigationSyncItem item : batchRequest.getIrrigations()) {
            IrrigationSyncResultItem resultItem = new IrrigationSyncResultItem();
            resultItem.setLocalId(item.getLocalId());
            try {
                Sector sector = sectorRepository.findById(item.getSectorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Sector", "id", item.getSectorId()));

                Integer farmId = sector.getFarm().getId(); // Para validación

                IrrigationEquipment equipment = equipmentRepository.findById(item.getEquipmentId())
                        .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id", item.getEquipmentId()));

                if (!equipment.getFarm().getId().equals(farmId)) {
                    throw new IllegalArgumentException("El equipo de irrigación ID " + equipment.getId() +
                            " no pertenece a la finca ID " + farmId + " del sector.");
                }

                if (item.getEndDatetime() != null && item.getStartDatetime() != null && item.getEndDatetime().before(item.getStartDatetime())) {
                    throw new IllegalArgumentException("La fecha de finalización no puede ser anterior a la fecha de inicio para el item local " + item.getLocalId());
                }

                // Lógica de Idempotencia: Buscar por localMobileId
                Optional<Irrigation> existingIrrigationOpt = irrigationRepository.findByLocalMobileId(item.getLocalId());

                Irrigation irrigationToSave;
                String actionTaken;

                if (existingIrrigationOpt.isPresent()) {
                    // El registro ya existe, actualízalo (política: el último gana)
                    irrigationToSave = existingIrrigationOpt.get();
                    log.info("Item con localId {} ya existe (serverId {}). Actualizando.", item.getLocalId(), irrigationToSave.getId());
                    actionTaken = "Actualizado";
                } else {
                    // El registro no existe, créalo
                    irrigationToSave = new Irrigation();
                    irrigationToSave.setLocalMobileId(item.getLocalId()); // MUY IMPORTANTE
                    actionTaken = "Creado";
                    log.info("Item con localId {} no existe. Creando nuevo registro.", item.getLocalId());
                }

                // Poblar/actualizar la entidad Irrigation
                irrigationToSave.setSector(sector);
                irrigationToSave.setEquipment(equipment);
                irrigationToSave.setStartDatetime(item.getStartDatetime());
                irrigationToSave.setEndDatetime(item.getEndDatetime());

                BigDecimal calculatedHours = item.getIrrigationHours();
                BigDecimal calculatedWater = item.getWaterAmount();

                if (item.getStartDatetime() != null && item.getEndDatetime() != null) {
                    if (calculatedHours == null) {
                        calculatedHours = calculateIrrigationHours(
                            item.getStartDatetime() == null ? null : new java.sql.Date(item.getStartDatetime().getTime()),
                            item.getEndDatetime() == null ? null : new java.sql.Date(item.getEndDatetime().getTime())
                        );
                    }
                    if (calculatedWater == null && equipment.getMeasuredFlow() != null && calculatedHours != null) {
                        calculatedWater = calculateWaterAmount(equipment.getMeasuredFlow(), calculatedHours);
                    }
                }
                irrigationToSave.setIrrigationHours(calculatedHours);
                irrigationToSave.setWaterAmount(calculatedWater);

                Irrigation savedIrrigation = irrigationRepository.save(irrigationToSave);

                resultItem.setServerId(savedIrrigation.getId());
                resultItem.setSuccess(true);
                resultItem.setMessage(actionTaken + " y sincronizado correctamente.");
                successfulItems++;

            } catch (ResourceNotFoundException e) {
                log.warn("Error de sincronización para item localId {}: {}", item.getLocalId(), e.getMessage());
                resultItem.setSuccess(false);
                resultItem.setMessage("Error de referencia: " + e.getMessage());
                failedItems++;
            } catch (IllegalArgumentException e) {
                log.warn("Error de validación de datos para item localId {}: {}", item.getLocalId(), e.getMessage());
                resultItem.setSuccess(false);
                resultItem.setMessage("Error de datos: " + e.getMessage());
                failedItems++;
            } catch (org.springframework.dao.DataIntegrityViolationException e) { // Capturar violación de unicidad
                log.warn("Error de integridad de datos (posible duplicado no manejado por lógica previa) para item localId {}: {}", item.getLocalId(), e.getMessage());
                resultItem.setSuccess(false);
                resultItem.setMessage("Error de integridad de datos. El registro podría ya existir o hay un conflicto.");
                failedItems++;
            } catch (Exception e) {
                log.error("Error inesperado sincronizando item localId {}: {}", item.getLocalId(), e.getMessage(), e);
                resultItem.setSuccess(false);
                resultItem.setMessage("Error inesperado en el servidor.");
                failedItems++;
            }
            results.add(resultItem);
        }

        return new IrrigationSyncResponse(batchRequest.getIrrigations().size(), successfulItems, failedItems, results);
    }

    // --- Métodos de Cálculo (copiados de IrrigationService para autosuficiencia o refactorizar a una clase utilitaria) ---
    private BigDecimal calculateIrrigationHours(Date start, Date end) {
        if (start == null || end == null || end.before(start)) {
            return BigDecimal.ZERO;
        }
        long diffInMillis = end.getTime() - start.getTime();
        double hours = (double) diffInMillis / TimeUnit.HOURS.toMillis(1);
        return BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateWaterAmount(BigDecimal flowRateLitersPerHour, BigDecimal hours) {
        if (flowRateLitersPerHour == null || hours == null || flowRateLitersPerHour.compareTo(BigDecimal.ZERO) <= 0 || hours.compareTo(BigDecimal.ZERO) <= 0) {
            return null; // O BigDecimal.ZERO si se prefiere no nulo
        }
        return flowRateLitersPerHour.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }
}