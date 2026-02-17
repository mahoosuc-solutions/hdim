package com.healthdata.costanalysis.application;

import com.healthdata.costanalysis.domain.model.CostDailySummaryEntity;
import com.healthdata.costanalysis.domain.repository.CostDailySummaryRepository;
import com.healthdata.costanalysis.domain.repository.CostTrackingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CostAggregationService {

    private final CostTrackingRepository costTrackingRepository;
    private final CostDailySummaryRepository costDailySummaryRepository;

    @Transactional
    @Scheduled(cron = "${hdim.cost.daily-aggregation-cron:0 0 2 * * *}", zone = "UTC")
    public void aggregateDailyCosts() {
        LocalDate day = LocalDate.now(ZoneOffset.UTC).minusDays(1);
        aggregateDay(day);
    }

    @Transactional
    public void aggregateDay(LocalDate day) {
        Instant start = day.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant end = day.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        List<Object[]> rows = costTrackingRepository.aggregateDailySummary(start, end);

        List<CostDailySummaryEntity> summaries = new ArrayList<>();
        for (Object[] row : rows) {
            summaries.add(CostDailySummaryEntity.builder()
                .id(UUID.randomUUID())
                .summaryDate(day)
                .tenantId((String) row[0])
                .serviceId((String) row[1])
                .featureKey((String) row[2])
                .totalCost((BigDecimal) row[3])
                .sampleCount((Long) row[4])
                .build());
        }

        if (!summaries.isEmpty()) {
            costDailySummaryRepository.saveAll(summaries);
        }
        log.info("Daily cost aggregation complete for {} with {} summaries", day, summaries.size());
    }
}
