package com.healthdata.costanalysis.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.costanalysis.domain.model.CostAnalysisCache;
import com.healthdata.costanalysis.domain.model.CostDailySummaryEntity;
import com.healthdata.costanalysis.domain.repository.CostAnalysisCacheRepository;
import com.healthdata.costanalysis.domain.repository.CostDailySummaryRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CostAnalysisService {

    private final CostAnalysisCacheRepository cacheRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CostDailySummaryRepository dailySummaryRepository;

    public CostAnalysisService(
        CostAnalysisCacheRepository cacheRepository,
        @Nullable CostDailySummaryRepository dailySummaryRepository
    ) {
        this.cacheRepository = cacheRepository;
        this.dailySummaryRepository = dailySummaryRepository;
    }

    @Transactional
    public CostAnalysisCache analyzeCosts(String tenantId, String analysisType, String analysisPeriod, String serviceName) {
        return cacheRepository.findValidCache(tenantId, analysisType, analysisPeriod, serviceName).stream()
            .findFirst()
            .map(cache -> {
                cache.setCacheHits(cache.getCacheHits() + 1);
                return cache;
            })
            .orElseGet(() -> createAndStoreAnalysis(tenantId, analysisType, analysisPeriod, serviceName));
    }

    @Transactional
    public CostAnalysisCache performDrilldownAnalysis(String tenantId, String serviceName, String dimension, String analysisPeriod) {
        return analyzeCosts(tenantId, "drill-down", analysisPeriod, serviceName);
    }

    @Transactional(readOnly = true)
    public List<CostAnalysisCache> getRecentAnalysis(String tenantId, int limit) {
        return cacheRepository.findRecentByTenant(tenantId, limit);
    }

    @Transactional(readOnly = true)
    public double getCacheHitRatio(String tenantId) {
        return cacheRepository.getCacheHitRatio(tenantId);
    }

    @Transactional
    public void invalidateAnalysisCache(String tenantId, String analysisType, String analysisPeriod) {
        List<CostAnalysisCache> caches = cacheRepository.findByTenantAndTypeAndPeriod(tenantId, analysisType, analysisPeriod);
        if (!caches.isEmpty()) {
            cacheRepository.deleteAll(caches);
        }
    }

    @Transactional
    public void clearExpiredCache() {
        cacheRepository.deleteExpiredCaches();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> summarizeTenantCosts(String tenantId, String analysisPeriod) {
        LocalDate periodDate = LocalDate.parse(analysisPeriod + "-01");
        LocalDate start = periodDate.withDayOfMonth(1);
        LocalDate end = start.plusMonths(1).minusDays(1);

        if (dailySummaryRepository == null) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("tenantId", tenantId);
            fallback.put("analysisPeriod", analysisPeriod);
            fallback.put("analysisType", "monthly-summary");
            fallback.put("totalCost", BigDecimal.ZERO);
            fallback.put("entries", 0);
            return fallback;
        }

        List<CostDailySummaryEntity> summaries = dailySummaryRepository.findByTenantIdAndSummaryDateBetween(tenantId, start, end);
        BigDecimal total = summaries.stream()
            .map(CostDailySummaryEntity::getTotalCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", tenantId);
        response.put("analysisPeriod", analysisPeriod);
        response.put("analysisType", "monthly-summary");
        response.put("totalCost", total);
        response.put("entries", summaries.size());
        return response;
    }

    private CostAnalysisCache createAndStoreAnalysis(
        String tenantId,
        String analysisType,
        String analysisPeriod,
        String serviceName
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", tenantId);
        payload.put("analysisType", analysisType);
        payload.put("analysisPeriod", analysisPeriod);
        payload.put("serviceName", serviceName);
        payload.put("generatedAt", Instant.now().toString());
        payload.put("summary", summarizeTenantCosts(tenantId, analysisPeriod));

        try {
            CostAnalysisCache cache = CostAnalysisCache.builder()
                .tenantId(tenantId)
                .analysisType(analysisType)
                .analysisPeriod(analysisPeriod)
                .serviceName(serviceName)
                .resultData(objectMapper.writeValueAsString(payload))
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .cacheHits(0)
                .build();
            CostAnalysisCache saved = cacheRepository.save(cache);
            return saved != null ? saved : cache;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize cost analysis payload", e);
        }
    }
}
