package com.healthdata.predictive.service;

import com.healthdata.predictive.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PopulationRiskStratifier Tests")
class PopulationRiskStratifierTest {

    @Mock
    private ReadmissionRiskPredictor readmissionPredictor;

    @InjectMocks
    private PopulationRiskStratifier riskStratifier;

    @Test
    @DisplayName("Should stratify population by risk tiers")
    void shouldStratifyPopulationByRiskTiers() {
        List<String> patientIds = Arrays.asList("p1", "p2", "p3", "p4");
        when(readmissionPredictor.predict30DayRisk(any(), eq("p1"), any()))
            .thenReturn(createRiskScore("p1", 20, RiskTier.LOW));
        when(readmissionPredictor.predict30DayRisk(any(), eq("p2"), any()))
            .thenReturn(createRiskScore("p2", 40, RiskTier.MODERATE));
        when(readmissionPredictor.predict30DayRisk(any(), eq("p3"), any()))
            .thenReturn(createRiskScore("p3", 65, RiskTier.HIGH));
        when(readmissionPredictor.predict30DayRisk(any(), eq("p4"), any()))
            .thenReturn(createRiskScore("p4", 85, RiskTier.VERY_HIGH));

        List<RiskCohort> cohorts = riskStratifier.stratifyPopulation("tenant-1", patientIds, new HashMap<>());

        assertNotNull(cohorts);
        assertEquals(4, cohorts.size());
    }

    @Test
    @DisplayName("Should calculate cohort statistics")
    void shouldCalculateCohortStatistics() {
        List<String> patientIds = Arrays.asList("p1", "p2");
        when(readmissionPredictor.predict30DayRisk(any(), any(), any()))
            .thenReturn(createRiskScore("p1", 50, RiskTier.MODERATE));

        List<RiskCohort> cohorts = riskStratifier.stratifyPopulation("tenant-1", patientIds, new HashMap<>());

        assertFalse(cohorts.isEmpty());
        cohorts.forEach(cohort -> {
            assertTrue(cohort.getPatientCount() >= 0);
            assertNotNull(cohort.getAverageRiskScore());
        });
    }

    @Test
    @DisplayName("Should identify high-risk patients")
    void shouldIdentifyHighRiskPatients() {
        List<String> patientIds = Arrays.asList("p1", "p2", "p3");
        when(readmissionPredictor.predict30DayRisk(any(), eq("p1"), any()))
            .thenReturn(createRiskScore("p1", 70, RiskTier.HIGH));
        when(readmissionPredictor.predict30DayRisk(any(), eq("p2"), any()))
            .thenReturn(createRiskScore("p2", 20, RiskTier.LOW));
        when(readmissionPredictor.predict30DayRisk(any(), eq("p3"), any()))
            .thenReturn(createRiskScore("p3", 88, RiskTier.VERY_HIGH));

        List<String> highRiskPatients = riskStratifier.getHighRiskPatients("tenant-1", patientIds, new HashMap<>());

        assertEquals(2, highRiskPatients.size());
        assertTrue(highRiskPatients.contains("p1"));
        assertTrue(highRiskPatients.contains("p3"));
    }

    @Test
    @DisplayName("Should throw exception for null tenant ID")
    void shouldThrowExceptionForNullTenantId() {
        assertThrows(IllegalArgumentException.class, () ->
            riskStratifier.stratifyPopulation(null, Arrays.asList("p1"), new HashMap<>()));
    }

    @Test
    @DisplayName("Should throw exception for null patient IDs")
    void shouldThrowExceptionForNullPatientIds() {
        assertThrows(IllegalArgumentException.class, () ->
            riskStratifier.stratifyPopulation("tenant-1", null, new HashMap<>()));
    }

    @Test
    @DisplayName("Should handle empty patient list")
    void shouldHandleEmptyPatientList() {
        List<RiskCohort> cohorts = riskStratifier.stratifyPopulation("tenant-1", Collections.emptyList(), new HashMap<>());
        assertNotNull(cohorts);
        assertTrue(cohorts.isEmpty());
    }

    @Test
    @DisplayName("Should generate cohort IDs")
    void shouldGenerateCohortIds() {
        List<String> patientIds = Arrays.asList("p1");
        when(readmissionPredictor.predict30DayRisk(any(), any(), any()))
            .thenReturn(createRiskScore("p1", 50, RiskTier.MODERATE));

        List<RiskCohort> cohorts = riskStratifier.stratifyPopulation("tenant-1", patientIds, new HashMap<>());

        cohorts.forEach(cohort -> assertNotNull(cohort.getCohortId()));
    }

    @Test
    @DisplayName("Should include tenant ID in cohorts")
    void shouldIncludeTenantIdInCohorts() {
        List<String> patientIds = Arrays.asList("p1");
        when(readmissionPredictor.predict30DayRisk(any(), any(), any()))
            .thenReturn(createRiskScore("p1", 50, RiskTier.MODERATE));

        List<RiskCohort> cohorts = riskStratifier.stratifyPopulation("tenant-1", patientIds, new HashMap<>());

        cohorts.forEach(cohort -> assertEquals("tenant-1", cohort.getTenantId()));
    }

    @Test
    @DisplayName("Should batch process large populations")
    void shouldBatchProcessLargePopulations() {
        List<String> patientIds = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            patientIds.add("p" + i);
        }
        when(readmissionPredictor.predict30DayRisk(any(), any(), any()))
            .thenReturn(createRiskScore("p1", 50, RiskTier.MODERATE));

        List<RiskCohort> cohorts = riskStratifier.stratifyPopulation("tenant-1", patientIds, new HashMap<>());

        assertNotNull(cohorts);
    }

    private ReadmissionRiskScore createRiskScore(String patientId, double score, RiskTier tier) {
        return ReadmissionRiskScore.builder()
            .patientId(patientId)
            .score(score)
            .riskTier(tier)
            .build();
    }
}
