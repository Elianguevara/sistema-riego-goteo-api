package com.sistemariegoagoteo.sistema_riego_goteo_api.service.dashboard;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.analyst.FarmStatusDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.analyst.TaskSummaryDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.dashboard.analyst.WaterBalanceDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Farm;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Precipitation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.TaskStatus;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.user.User;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AnalystDashboardService {

    private final FarmRepository farmRepository;
    private final HumidityAlertRepository humidityAlertRepository; // Para contar alertas
    private final IrrigationRepository irrigationRepository;
    private final PrecipitationRepository precipitationRepository;
    private final TaskRepository taskRepository;

    @Transactional(readOnly = true)
    public List<FarmStatusDTO> getFarmsStatus() {
        List<Farm> farms = farmRepository.findAll();
        // Lógica simple para determinar el estado. Se puede hacer más compleja.
        return farms.stream().map(farm -> {
            FarmStatusDTO dto = new FarmStatusDTO(farm);
            // Lógica de ejemplo para contar alertas (necesitarías un método en el repo)
            // int alertCount = humidityAlertRepository.countByHumiditySensor_Sector_Farm_IdAndResolvedIsFalse(farm.getId());
            int alertCount = 0; // Placeholder
            dto.setActiveAlertsCount(alertCount);
            dto.setStatus(alertCount > 0 ? "ALERTA" : "OK");
            return dto;
        }).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<WaterBalanceDTO> getWaterBalance(Integer farmId, Date startDate, Date endDate) {
        // Lógica para obtener el balance hídrico
        List<Irrigation> irrigations = irrigationRepository.findBySector_Farm_IdAndStartDatetimeBetween(farmId, startDate, endDate);
        List<Precipitation> precipitations = precipitationRepository.findByFarm_IdAndPrecipitationDateBetween(farmId, startDate, endDate);

        Map<LocalDate, BigDecimal> dailyIrrigation = irrigations.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getStartDatetime().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                        Collectors.mapping(Irrigation::getWaterAmount, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        Map<LocalDate, BigDecimal> dailyPrecipitation = precipitations.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getPrecipitationDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
                        Collectors.mapping(Precipitation::getMmEffectiveRain, Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))
                ));

        return Stream.concat(dailyIrrigation.keySet().stream(), dailyPrecipitation.keySet().stream())
                .distinct()
                .sorted()
                .map(date -> new WaterBalanceDTO(
                        Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()),
                        dailyIrrigation.getOrDefault(date, BigDecimal.ZERO),
                        dailyPrecipitation.getOrDefault(date, BigDecimal.ZERO)
                )).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskSummaryDTO getTaskSummary(User creator) {
        long total = taskRepository.countByCreatedBy(creator);
        long pending = taskRepository.countByCreatedByAndStatus(creator, TaskStatus.PENDIENTE);
        long inProgress = taskRepository.countByCreatedByAndStatus(creator, TaskStatus.EN_PROGRESO);
        long completed = taskRepository.countByCreatedByAndStatus(creator, TaskStatus.COMPLETADA);
        return new TaskSummaryDTO(total, pending, inProgress, completed);
    }
}