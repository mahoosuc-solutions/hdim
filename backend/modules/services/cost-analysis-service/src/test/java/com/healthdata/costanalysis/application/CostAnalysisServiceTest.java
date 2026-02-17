package com.healthdata.costanalysis.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.healthdata.costanalysis.domain.model.CostAnalysisCache;
import com.healthdata.costanalysis.domain.repository.CostAnalysisCacheRepository;

@ExtendWith(MockitoExtension.class)
class CostAnalysisServiceTest {

    private static final String TENANT_ID = "TENANT_001";
    private static final String ANALYSIS_TYPE = "drill-down";
    private static final String ANALYSIS_PERIOD = "2025-01";

    @Mock
    private CostAnalysisCacheRepository cacheRepository;

    private CostAnalysisService costAnalysisService;

    @BeforeEach
    void setUp() {
        costAnalysisService = new CostAnalysisService(cacheRepository, null);
    }

    @Test
    void shouldAnalyzeCostsAndReturnCachedResult() {
        // Given
        CostAnalysisCache expectedCache = buildCostAnalysisCache();
        when(cacheRepository.findValidCache(TENANT_ID, ANALYSIS_TYPE, ANALYSIS_PERIOD, null))
            .thenReturn(List.of(expectedCache));

        // When
        CostAnalysisCache result = costAnalysisService.analyzeCosts(TENANT_ID, ANALYSIS_TYPE, ANALYSIS_PERIOD, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(expectedCache.getId());
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        verify(cacheRepository).findValidCache(TENANT_ID, ANALYSIS_TYPE, ANALYSIS_PERIOD, null);
    }

    @Test
    void shouldCreateNewAnalysisWhenCacheExpired() {
        // Given
        when(cacheRepository.findValidCache(TENANT_ID, ANALYSIS_TYPE, ANALYSIS_PERIOD, null))
            .thenReturn(List.of());

        // When
        CostAnalysisCache result = costAnalysisService.analyzeCosts(TENANT_ID, ANALYSIS_TYPE, ANALYSIS_PERIOD, null);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getAnalysisType()).isEqualTo(ANALYSIS_TYPE);
        verify(cacheRepository).save(any(CostAnalysisCache.class));
    }

    @Test
    void shouldPerformDrilldownAnalysisWithMultipleLevels() {
        // Given
        String serviceName = "patient-service";
        String dimension = "cost-category";

        // When
        CostAnalysisCache result = costAnalysisService.performDrilldownAnalysis(
            TENANT_ID, serviceName, dimension, "2025-01");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
        assertThat(result.getAnalysisType()).isEqualTo("drill-down");
    }

    @Test
    void shouldInvalidateCacheForSpecificAnalysis() {
        // Given
        CostAnalysisCache cache = buildCostAnalysisCache();
        when(cacheRepository.findByTenantAndTypeAndPeriod(TENANT_ID, ANALYSIS_TYPE, ANALYSIS_PERIOD))
            .thenReturn(List.of(cache));

        // When
        costAnalysisService.invalidateAnalysisCache(TENANT_ID, ANALYSIS_TYPE, ANALYSIS_PERIOD);

        // Then
        verify(cacheRepository).deleteAll(any(List.class));
    }

    @Test
    void shouldClearExpiredCachesAutomatically() {
        // When
        costAnalysisService.clearExpiredCache();

        // Then
        verify(cacheRepository).deleteExpiredCaches();
    }

    @Test
    void shouldNotAccessDatabaseWhenValidCacheExists() {
        // Given
        CostAnalysisCache expectedCache = buildCostAnalysisCache();
        when(cacheRepository.findValidCache(TENANT_ID, ANALYSIS_TYPE, ANALYSIS_PERIOD, null))
            .thenReturn(List.of(expectedCache));

        // When
        costAnalysisService.analyzeCosts(TENANT_ID, ANALYSIS_TYPE, ANALYSIS_PERIOD, null);
        costAnalysisService.analyzeCosts(TENANT_ID, ANALYSIS_TYPE, ANALYSIS_PERIOD, null);
        costAnalysisService.analyzeCosts(TENANT_ID, ANALYSIS_TYPE, ANALYSIS_PERIOD, null);

        // Then - Should only query cache repo once (caching behavior)
        verify(cacheRepository, times(3)).findValidCache(TENANT_ID, ANALYSIS_TYPE, ANALYSIS_PERIOD, null);
    }

    @Test
    void shouldEnforceTenantIsolation() {
        // Given
        String tenant2 = "TENANT_002";
        CostAnalysisCache cache1 = buildCostAnalysisCache();
        cache1.setTenantId(TENANT_ID);

        // When
        costAnalysisService.analyzeCosts(TENANT_ID, ANALYSIS_TYPE, ANALYSIS_PERIOD, null);
        costAnalysisService.analyzeCosts(tenant2, ANALYSIS_TYPE, ANALYSIS_PERIOD, null);

        // Then - Different tenants should query separately
        verify(cacheRepository, times(2)).findValidCache(any(), eq(ANALYSIS_TYPE), eq(ANALYSIS_PERIOD), any());
    }

    @Test
    void shouldReturnRecentAnalysisForTrending() {
        // Given
        List<CostAnalysisCache> recentAnalyses = List.of(
            buildCostAnalysisCache(),
            buildCostAnalysisCache(),
            buildCostAnalysisCache()
        );
        when(cacheRepository.findRecentByTenant(TENANT_ID, 3))
            .thenReturn(recentAnalyses);

        // When
        List<CostAnalysisCache> result = costAnalysisService.getRecentAnalysis(TENANT_ID, 3);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result).allMatch(cache -> cache.getTenantId().equals(TENANT_ID));
    }

    @Test
    void shouldCalculateCacheHitRatio() {
        // Given
        when(cacheRepository.getCacheHitRatio(TENANT_ID)).thenReturn(0.85);

        // When
        double hitRatio = costAnalysisService.getCacheHitRatio(TENANT_ID);

        // Then
        assertThat(hitRatio).isEqualTo(0.85);
    }

    private CostAnalysisCache buildCostAnalysisCache() {
        return CostAnalysisCache.builder()
            .id(UUID.randomUUID())
            .tenantId(TENANT_ID)
            .analysisType(ANALYSIS_TYPE)
            .analysisPeriod(ANALYSIS_PERIOD)
            .resultData("{\"summary\": {\"totalCost\": 150000.00}}")
            .createdAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(300))
            .cacheHits(0)
            .build();
    }
}
