package com.healthdata.patient.service;

import com.healthdata.metrics.HealthcareMetrics;
import com.healthdata.patient.client.CareGapServiceClient;
import com.healthdata.patient.client.HccServiceClient;
import com.healthdata.patient.dto.PatientRiskAssessmentResponse;
import com.healthdata.patient.dto.PatientRiskAssessmentResponse.DataAvailability;
import com.healthdata.patient.dto.PatientRiskAssessmentResponse.RiskLevel;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for patient risk assessment.
 *
 * Aggregates data from HCC Service and Care Gap Service to provide
 * a comprehensive risk assessment combining:
 * - CMS-HCC RAF scores (V24, V28, blended)
 * - Care gap counts and priorities
 * - Documentation gap opportunities
 * - Recapture opportunities
 *
 * HIPAA Compliance:
 * - Cache TTL limited to 5 minutes for PHI
 * - All operations are audited
 * - Tenant isolation enforced
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PatientRiskAssessmentService {

    private final HccServiceClient hccServiceClient;
    private final CareGapServiceClient careGapServiceClient;
    private final HealthcareMetrics healthcareMetrics;
    private final Tracer tracer;

    /**
     * HCC code to human-readable condition mapping.
     * Maps commonly encountered HCC codes to clinical descriptions.
     */
    private static final Map<String, String> HCC_CONDITION_MAP = Map.ofEntries(
        // Diabetes
        Map.entry("HCC17", "Diabetes with Acute Complications"),
        Map.entry("HCC18", "Diabetes with Chronic Complications"),
        Map.entry("HCC19", "Diabetes without Complication"),
        // Heart
        Map.entry("HCC85", "Congestive Heart Failure"),
        Map.entry("HCC86", "Acute Myocardial Infarction"),
        Map.entry("HCC87", "Unstable Angina and Other Acute Ischemic Heart Disease"),
        Map.entry("HCC88", "Angina Pectoris"),
        // COPD/Respiratory
        Map.entry("HCC111", "Chronic Obstructive Pulmonary Disease"),
        Map.entry("HCC112", "Fibrosis of Lung and Other Chronic Lung Disorders"),
        // Renal
        Map.entry("HCC134", "Dialysis Status"),
        Map.entry("HCC135", "Acute Renal Failure"),
        Map.entry("HCC136", "Chronic Kidney Disease, Stage 5"),
        Map.entry("HCC137", "Chronic Kidney Disease, Severe (Stage 4)"),
        Map.entry("HCC138", "Chronic Kidney Disease, Moderate (Stage 3)"),
        // Cancer
        Map.entry("HCC8", "Metastatic Cancer and Acute Leukemia"),
        Map.entry("HCC9", "Lung and Other Severe Cancers"),
        Map.entry("HCC10", "Lymphoma and Other Cancers"),
        Map.entry("HCC11", "Colorectal, Bladder, and Other Cancers"),
        Map.entry("HCC12", "Breast, Prostate, and Other Cancers and Tumors"),
        // Stroke/Vascular
        Map.entry("HCC96", "Ischemic or Unspecified Stroke"),
        Map.entry("HCC100", "Hemiplegia/Hemiparesis"),
        Map.entry("HCC103", "Cerebral Palsy"),
        Map.entry("HCC106", "Atherosclerosis of the Extremities with Ulceration or Gangrene"),
        Map.entry("HCC107", "Vascular Disease with Complications"),
        // Mental Health
        Map.entry("HCC55", "Major Depressive, Bipolar, and Paranoid Disorders"),
        Map.entry("HCC57", "Schizophrenia"),
        Map.entry("HCC58", "Reactive and Unspecified Psychosis"),
        // Liver
        Map.entry("HCC27", "End-Stage Liver Disease"),
        Map.entry("HCC28", "Cirrhosis of Liver"),
        Map.entry("HCC29", "Chronic Hepatitis")
    );

    /**
     * Get comprehensive risk assessment for a patient.
     *
     * Combines HCC profile data with care gap counts to provide a complete
     * risk picture. Gracefully handles missing data from either service.
     *
     * @param tenantId tenant identifier for multi-tenant isolation
     * @param patientId patient UUID as string
     * @return comprehensive risk assessment response
     */
    @Cacheable(value = "patientRiskAssessment", key = "#tenantId + ':' + #patientId",
               unless = "#result == null")
    public PatientRiskAssessmentResponse getRiskAssessment(String tenantId, String patientId) {
        Span span = tracer.spanBuilder("patient.get_risk_assessment")
                .setAttribute("tenant.id", tenantId)
                .setAttribute("patient.id", patientId)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            log.info("Calculating risk assessment for patient {} in tenant {}", patientId, tenantId);
            long queryStartTime = System.currentTimeMillis();

            UUID patientUuid;
            try {
                patientUuid = UUID.fromString(patientId);
            } catch (IllegalArgumentException e) {
                log.warn("Invalid patient ID format: {}", patientId);
                span.setStatus(StatusCode.OK);
                return buildEmptyResponse(patientId);
            }

            int currentYear = LocalDate.now().getYear();

            // Fetch data from services (with graceful fallbacks)
            HccServiceClient.HccProfileResponse hccProfile = fetchHccProfile(tenantId, patientUuid, currentYear);
            CareGapServiceClient.CareGapCountResponse careGapCounts = fetchCareGapCounts(tenantId, patientId);

            PatientRiskAssessmentResponse response = buildRiskAssessment(
                    patientId, hccProfile, careGapCounts, currentYear);

            healthcareMetrics.recordPatientQuery("risk-assessment", java.time.Duration.ofMillis(System.currentTimeMillis() - queryStartTime));
            span.setStatus(StatusCode.OK);
            return response;
        } catch (Exception e) {
            span.recordException(e);
            span.setStatus(StatusCode.ERROR, e.getMessage());
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Fetch HCC profile with graceful error handling.
     */
    private HccServiceClient.HccProfileResponse fetchHccProfile(String tenantId, UUID patientId, int year) {
        try {
            HccServiceClient.HccProfileResponse profile = hccServiceClient.getPatientHccProfile(
                tenantId, patientId, year);
            if (profile != null) {
                log.debug("Retrieved HCC profile for patient {}: RAF blended = {}",
                    patientId, profile.getRafScoreBlended());
            }
            return profile;
        } catch (Exception e) {
            log.warn("Failed to fetch HCC profile for patient {}: {}", patientId, e.getMessage());
            return null;
        }
    }

    /**
     * Fetch care gap counts with graceful error handling.
     */
    private CareGapServiceClient.CareGapCountResponse fetchCareGapCounts(String tenantId, String patientId) {
        try {
            CareGapServiceClient.CareGapCountResponse counts = careGapServiceClient.getCareGapCount(
                tenantId, patientId);
            if (counts != null) {
                log.debug("Retrieved care gap counts for patient {}: {} open, {} high priority",
                    patientId, counts.getOpen(), counts.getHighPriority());
            }
            return counts;
        } catch (Exception e) {
            log.warn("Failed to fetch care gap counts for patient {}: {}", patientId, e.getMessage());
            return null;
        }
    }

    /**
     * Build the comprehensive risk assessment response.
     */
    private PatientRiskAssessmentResponse buildRiskAssessment(
            String patientId,
            HccServiceClient.HccProfileResponse hccProfile,
            CareGapServiceClient.CareGapCountResponse careGapCounts,
            int currentYear) {

        // Determine RAF scores
        BigDecimal rafBlended = hccProfile != null ? hccProfile.getRafScoreBlended() : null;
        BigDecimal rafV24 = hccProfile != null ? hccProfile.getRafScoreV24() : null;
        BigDecimal rafV28 = hccProfile != null ? hccProfile.getRafScoreV28() : null;

        // Calculate risk level and score
        RiskLevel riskLevel = PatientRiskAssessmentResponse.calculateRiskLevel(rafBlended);
        Integer riskScore = PatientRiskAssessmentResponse.calculateRiskScore(rafBlended);

        // Extract HCC information
        List<String> hccs = extractHccs(hccProfile);
        List<String> topHccs = hccs.stream().limit(5).collect(Collectors.toList());
        List<String> chronicConditions = mapHccsToConditions(hccs);

        // Extract care gap information
        int openCareGaps = careGapCounts != null ? (int) careGapCounts.getOpen() : 0;
        int highPriorityCareGaps = careGapCounts != null ? (int) careGapCounts.getHighPriority() : 0;
        int overdueCareGaps = careGapCounts != null ? (int) careGapCounts.getOverdue() : 0;

        // Extract opportunity metrics
        BigDecimal potentialUplift = hccProfile != null ? hccProfile.getPotentialRafUplift() : null;
        Integer docGapCount = hccProfile != null ? hccProfile.getDocumentationGapCount() : null;
        Integer recaptureCount = hccProfile != null ? hccProfile.getRecaptureOpportunitiesCount() : null;

        // Build data availability indicators
        DataAvailability dataAvailability = DataAvailability.builder()
            .hccDataAvailable(hccProfile != null)
            .careGapDataAvailable(careGapCounts != null)
            .documentationGapDataAvailable(hccProfile != null && hccProfile.getDocumentationGaps() != null)
            .build();

        return PatientRiskAssessmentResponse.builder()
            .patientId(patientId)
            // RAF scores
            .rafScoreBlended(rafBlended)
            .rafScoreV24(rafV24)
            .rafScoreV28(rafV28)
            // Risk classification
            .riskLevel(riskLevel)
            .riskScore(riskScore)
            // HCC details
            .hccCount(hccs.size())
            .topHccs(topHccs)
            .chronicConditions(chronicConditions)
            // Care gaps
            .openCareGaps(openCareGaps)
            .highPriorityCareGaps(highPriorityCareGaps)
            .overdueCareGaps(overdueCareGaps)
            // Opportunities
            .potentialRafUplift(potentialUplift)
            .documentationGapCount(docGapCount)
            .recaptureOpportunities(recaptureCount)
            // Metadata
            .calculatedAt(Instant.now())
            .profileYear(currentYear)
            .dataAvailability(dataAvailability)
            .build();
    }

    /**
     * Extract combined HCC list from profile (prioritizing V28 as it's the newer model).
     */
    private List<String> extractHccs(HccServiceClient.HccProfileResponse profile) {
        if (profile == null) {
            return Collections.emptyList();
        }

        // Prefer V28 HCCs as they're the current standard
        if (profile.getHccsV28() != null && !profile.getHccsV28().isEmpty()) {
            return profile.getHccsV28();
        }

        if (profile.getHccsV24() != null && !profile.getHccsV24().isEmpty()) {
            return profile.getHccsV24();
        }

        return Collections.emptyList();
    }

    /**
     * Map HCC codes to human-readable chronic condition names.
     */
    private List<String> mapHccsToConditions(List<String> hccs) {
        if (hccs == null || hccs.isEmpty()) {
            return Collections.emptyList();
        }

        return hccs.stream()
            .map(hcc -> HCC_CONDITION_MAP.getOrDefault(hcc, null))
            .filter(Objects::nonNull)
            .distinct()
            .limit(5)  // Limit to top 5 conditions for display
            .collect(Collectors.toList());
    }

    /**
     * Build empty response when data is unavailable.
     */
    private PatientRiskAssessmentResponse buildEmptyResponse(String patientId) {
        return PatientRiskAssessmentResponse.builder()
            .patientId(patientId)
            .riskLevel(RiskLevel.LOW)
            .riskScore(0)
            .hccCount(0)
            .topHccs(Collections.emptyList())
            .chronicConditions(Collections.emptyList())
            .openCareGaps(0)
            .highPriorityCareGaps(0)
            .overdueCareGaps(0)
            .calculatedAt(Instant.now())
            .profileYear(LocalDate.now().getYear())
            .dataAvailability(DataAvailability.builder()
                .hccDataAvailable(false)
                .careGapDataAvailable(false)
                .documentationGapDataAvailable(false)
                .build())
            .build();
    }
}
