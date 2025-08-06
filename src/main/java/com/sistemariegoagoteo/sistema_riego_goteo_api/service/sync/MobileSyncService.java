package com.sistemariegoagoteo.sistema_riego_goteo_api.service.sync;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncBatchRequest;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncItem;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncResponse;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.sync.IrrigationSyncResultItem;
import com.sistemariegoagoteo.sistema_riego_goteo_api.exceptions.ResourceNotFoundException;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.IrrigationEquipment;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Sector;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User; // <-- IMPORTAR
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationEquipmentRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.user.UserRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.service.audit.AuditService; // <-- IMPORTAR
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final IrrigationRepository irrigationRepository;
    private final UserRepository userRepository;
    private final SectorRepository sectorRepository;
    private final IrrigationEquipmentRepository equipmentRepository;
    private final AuditService auditService; // <-- INYECTAR SERVICIO

    @Transactional
    public IrrigationSyncResponse processIrrigationBatch(String username, IrrigationSyncBatchRequest batchRequest) {
        User currentUser = userRepository.findByUsername(username)
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

                IrrigationEquipment equipment = equipmentRepository.findById(item.getEquipmentId())
                        .orElseThrow(() -> new ResourceNotFoundException("IrrigationEquipment", "id", item.getEquipmentId()));

                if (!equipment.getFarm().getId().equals(sector.getFarm().getId())) {
                    throw new IllegalArgumentException("El equipo de irrigación ID " + equipment.getId() +
                            " no pertenece a la finca ID " + sector.getFarm().getId() + " del sector.");
                }
                
                // ... (resto de validaciones)

                Optional<Irrigation> existingIrrigationOpt = irrigationRepository.findByLocalMobileId(item.getLocalId());

                Irrigation irrigationToSave;
                String actionTaken;

                if (existingIrrigationOpt.isPresent()) {
                    irrigationToSave = existingIrrigationOpt.get();
                    log.info("Item con localId {} ya existe (serverId {}). Actualizando.", item.getLocalId(), irrigationToSave.getId());
                    actionTaken = "UPDATE";
                } else {
                    irrigationToSave = new Irrigation();
                    irrigationToSave.setLocalMobileId(item.getLocalId());
                    actionTaken = "CREATE";
                    log.info("Item con localId {} no existe. Creando nuevo registro.", item.getLocalId());
                }
                
                // ... (lógica para poblar la entidad irrigationToSave)
                irrigationToSave.setSector(sector);
                irrigationToSave.setEquipment(equipment);
                irrigationToSave.setStartDatetime(item.getStartDatetime());
                irrigationToSave.setEndDatetime(item.getEndDatetime());
                // ... (cálculos)

                Irrigation savedIrrigation = irrigationRepository.save(irrigationToSave);

                // --- AUDITORÍA DE SINCRONIZACIÓN MÓVIL ---
                auditService.logChange(currentUser, actionTaken, Irrigation.class.getSimpleName(), "id", 
                    actionTaken.equals("UPDATE") ? savedIrrigation.getId().toString() : null, 
                    savedIrrigation.getId().toString());
                
                resultItem.setServerId(savedIrrigation.getId());
                resultItem.setSuccess(true);
                resultItem.setMessage(actionTaken + " y sincronizado correctamente.");
                successfulItems++;

            } catch (Exception e) {
                // ... (manejo de errores)
                failedItems++;
            }
            results.add(resultItem);
        }

        return new IrrigationSyncResponse(batchRequest.getIrrigations().size(), successfulItems, failedItems, results);
    }

    // --- Métodos de Cálculo (sin cambios) ---
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
            return null;
        }
        return flowRateLitersPerHour.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }
}