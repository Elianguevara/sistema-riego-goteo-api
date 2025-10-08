package com.sistemariegoagoteo.sistema_riego_goteo_api.service.analytics;

import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.analytics.IrrigationRecordDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.analytics.IrrigationSectorSummaryDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.dto.analytics.IrrigationTimeseriesDTO;
import com.sistemariegoagoteo.sistema_riego_goteo_api.model.riego.Irrigation;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.IrrigationRepository;
import com.sistemariegoagoteo.sistema_riego_goteo_api.repository.riego.SectorRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
@Transactional(readOnly = true)
public class AnalyticsService {

    private final IrrigationRepository irrigationRepository;
    private final SectorRepository sectorRepository;

    public List<IrrigationSectorSummaryDTO> getIrrigationSummary(Integer farmId, Date startDate, Date endDate, List<Integer> sectorIds) {
        List<Integer> targetSectorIds = sectorIds;
        if (targetSectorIds == null || targetSectorIds.isEmpty()) {
            targetSectorIds = sectorRepository.findByFarm_Id(farmId).stream()
                    .map(sector -> sector.getId())
                    .collect(Collectors.toList());
        }
        if(targetSectorIds.isEmpty()){
            return List.of();
        }

        List<Object[]> results = irrigationRepository.getIrrigationSummaryBySectors(farmId, targetSectorIds, startDate, endDate);
        return results.stream()
                .map(res -> new IrrigationSectorSummaryDTO(
                        ((Number) res[0]).intValue(),
                        (String) res[1],
                        (BigDecimal) res[2],
                        (BigDecimal) res[3]
                ))
                .collect(Collectors.toList());
    }

    public List<IrrigationTimeseriesDTO> getIrrigationTimeseries(Integer sectorId, LocalDate startDate, LocalDate endDate) {
        Date start = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        List<Object[]> results = irrigationRepository.getDailyIrrigationTotals(sectorId, start, end);
        Map<LocalDate, IrrigationTimeseriesDTO> dailyDataMap = results.stream()
                .collect(Collectors.toMap(
                        res -> ((java.sql.Date) res[0]).toLocalDate(),
                        res -> new IrrigationTimeseriesDTO(
                                ((java.sql.Date) res[0]).toLocalDate(),
                                (BigDecimal) res[1],
                                (BigDecimal) res[2]
                        )
                ));

        return Stream.iterate(startDate, date -> date.plusDays(1))
                .limit(startDate.until(endDate).getDays() + 1)
                .map(date -> dailyDataMap.getOrDefault(date, new IrrigationTimeseriesDTO(date, BigDecimal.ZERO, BigDecimal.ZERO)))
                .collect(Collectors.toList());
    }

    public Page<IrrigationRecordDTO> getIrrigationRecords(Integer farmId, Date startDate, Date endDate, List<Integer> sectorIds, Pageable pageable) {
        Specification<Irrigation> spec = (root, query, cb) -> {
            Predicate p = cb.conjunction();
            p = cb.and(p, cb.equal(root.get("sector").get("farm").get("id"), farmId));
            p = cb.and(p, cb.between(root.get("startDatetime"), startDate, endDate));
            if (sectorIds != null && !sectorIds.isEmpty()) {
                p = cb.and(p, root.get("sector").get("id").in(sectorIds));
            }
            return p;
        };
        return irrigationRepository.findAll(spec, pageable).map(IrrigationRecordDTO::new);
    }
}