package com.healthdata.sdoh.service;

import com.healthdata.sdoh.entity.HrsnScreeningSessionEntity;
import com.healthdata.sdoh.entity.SdohInterventionEntity;
import com.healthdata.sdoh.repository.HrsnScreeningSessionRepository;
import com.healthdata.sdoh.repository.SdohInterventionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * SDOH Care Gap Service
 *
 * Integrates SDOH screening with the care gap identification system.
 * Creates and manages care gaps for:
 * - SDOH-1: Incomplete HRSN screening (not all 5 domains assessed)
 * - SDOH-2: Positive screen without documented intervention
 *
 * This service monitors HRSN screening sessions and automatically
 * creates/closes care gaps based on screening completion and intervention status.
 *
 * Regulatory Context:
 * - CMS SDOH-1/SDOH-2 measures are mandatory as of 2024
 * - ACOs must report via APP Plus starting 2028
 *
 * @see HrsnScreeningSessionEntity
 * @see SdohInterventionEntity
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SdohCareGapService {

    private final HrsnScreeningSessionRepository screeningRepository;
    private final SdohInterventionRepository interventionRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private static final String SDOH_SCREENING_GAP_MEASURE_ID = "CMS_SDOH1";
    private static final String SDOH_INTERVENTION_GAP_MEASURE_ID = "CMS_SDOH2";
    private static final String CARE_GAP_TOPIC = "care-gap-identified";

    /**
     * Evaluate SDOH care gaps for a patient.
     *
     * Checks:
     * 1. SDOH-1: Has patient been screened for all 5 HRSN domains?
     * 2. SDOH-2: If positive, has intervention been documented?
     *
     * @param tenantId The tenant identifier
     * @param patientId The patient identifier
     * @param measurementPeriodStart Start of measurement period
     * @param measurementPeriodEnd End of measurement period
     * @return List of identified SDOH care gaps
     */
    @Transactional(readOnly = true)
    public List<SdohCareGap> evaluateSdohCareGaps(
            String tenantId,
            UUID patientId,
            LocalDateTime measurementPeriodStart,
            LocalDateTime measurementPeriodEnd) {

        log.info("Evaluating SDOH care gaps for patient {} in tenant {}", patientId, tenantId);

        List<SdohCareGap> gaps = new ArrayList<>();

        // Get completed screenings in measurement period
        List<HrsnScreeningSessionEntity> screenings = screeningRepository.findCompletedInPeriod(
            tenantId, patientId, measurementPeriodStart, measurementPeriodEnd);

        // Check SDOH-1: All domains screened?
        SdohCareGap sdoh1Gap = evaluateSdoh1Gap(tenantId, patientId, screenings);
        if (sdoh1Gap != null) {
            gaps.add(sdoh1Gap);
        }

        // Check SDOH-2: Positive screen with intervention?
        SdohCareGap sdoh2Gap = evaluateSdoh2Gap(tenantId, patientId, screenings);
        if (sdoh2Gap != null) {
            gaps.add(sdoh2Gap);
        }

        log.info("Found {} SDOH care gaps for patient {}", gaps.size(), patientId);
        return gaps;
    }

    /**
     * Evaluate SDOH-1 gap: Has patient been screened for all 5 HRSN domains?
     */
    private SdohCareGap evaluateSdoh1Gap(
            String tenantId,
            UUID patientId,
            List<HrsnScreeningSessionEntity> screenings) {

        // Check if any screening has all domains completed
        boolean allDomainsScreened = screenings.stream()
            .anyMatch(s -> Boolean.TRUE.equals(s.getAllDomainsScreened()));

        if (allDomainsScreened) {
            return null; // No gap
        }

        // Determine which domains are missing
        Set<String> screenedDomains = new HashSet<>();
        for (HrsnScreeningSessionEntity session : screenings) {
            if (Boolean.TRUE.equals(session.getFoodInsecurityCompleted())) {
                screenedDomains.add("FOOD_INSECURITY");
            }
            if (Boolean.TRUE.equals(session.getHousingInstabilityCompleted())) {
                screenedDomains.add("HOUSING_INSTABILITY");
            }
            if (Boolean.TRUE.equals(session.getTransportationCompleted())) {
                screenedDomains.add("TRANSPORTATION");
            }
            if (Boolean.TRUE.equals(session.getUtilitiesCompleted())) {
                screenedDomains.add("UTILITIES");
            }
            if (Boolean.TRUE.equals(session.getInterpersonalSafetyCompleted())) {
                screenedDomains.add("INTERPERSONAL_SAFETY");
            }
        }

        List<String> missingDomains = new ArrayList<>();
        if (!screenedDomains.contains("FOOD_INSECURITY")) missingDomains.add("Food Insecurity");
        if (!screenedDomains.contains("HOUSING_INSTABILITY")) missingDomains.add("Housing Instability");
        if (!screenedDomains.contains("TRANSPORTATION")) missingDomains.add("Transportation");
        if (!screenedDomains.contains("UTILITIES")) missingDomains.add("Utilities");
        if (!screenedDomains.contains("INTERPERSONAL_SAFETY")) missingDomains.add("Interpersonal Safety");

        String gapDescription = missingDomains.isEmpty()
            ? "SDOH screening not performed during measurement period"
            : "SDOH screening incomplete - missing domains: " + String.join(", ", missingDomains);

        return SdohCareGap.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId(SDOH_SCREENING_GAP_MEASURE_ID)
            .measureName("SDOH Screening Rate (SDOH-1)")
            .measureCategory("CMS")
            .gapType("sdoh-screening")
            .gapStatus("open")
            .gapDescription(gapDescription)
            .gapReason("Not all 5 HRSN domains screened during measurement period")
            .priority(screenedDomains.isEmpty() ? "high" : "medium")
            .recommendation("Administer AHC-HRSN or PRAPARE screening tool to assess all 5 HRSN domains")
            .recommendedAction("Schedule SDOH screening during next visit")
            .domainsScreenedCount(screenedDomains.size())
            .missingDomains(missingDomains)
            .billingCode("G0136")
            .build();
    }

    /**
     * Evaluate SDOH-2 gap: If positive screen, has intervention been documented?
     */
    private SdohCareGap evaluateSdoh2Gap(
            String tenantId,
            UUID patientId,
            List<HrsnScreeningSessionEntity> screenings) {

        // Find screenings with positive results
        Optional<HrsnScreeningSessionEntity> positiveSession = screenings.stream()
            .filter(s -> Boolean.TRUE.equals(s.getAnyDomainPositive()))
            .findFirst();

        if (positiveSession.isEmpty()) {
            return null; // No positive screen, no SDOH-2 gap
        }

        HrsnScreeningSessionEntity session = positiveSession.get();

        // Check if interventions exist for positive domains
        List<String> positiveDomainsWithoutIntervention = new ArrayList<>();

        if (Boolean.TRUE.equals(session.getFoodInsecurityPositive()) &&
            !interventionRepository.hasInterventionForDomain(session.getId(),
                SdohInterventionEntity.HrsnDomain.FOOD_INSECURITY)) {
            positiveDomainsWithoutIntervention.add("Food Insecurity");
        }

        if (Boolean.TRUE.equals(session.getHousingInstabilityPositive()) &&
            !interventionRepository.hasInterventionForDomain(session.getId(),
                SdohInterventionEntity.HrsnDomain.HOUSING_INSTABILITY)) {
            positiveDomainsWithoutIntervention.add("Housing Instability");
        }

        if (Boolean.TRUE.equals(session.getTransportationPositive()) &&
            !interventionRepository.hasInterventionForDomain(session.getId(),
                SdohInterventionEntity.HrsnDomain.TRANSPORTATION)) {
            positiveDomainsWithoutIntervention.add("Transportation");
        }

        if (Boolean.TRUE.equals(session.getUtilitiesPositive()) &&
            !interventionRepository.hasInterventionForDomain(session.getId(),
                SdohInterventionEntity.HrsnDomain.UTILITIES)) {
            positiveDomainsWithoutIntervention.add("Utilities");
        }

        if (Boolean.TRUE.equals(session.getInterpersonalSafetyPositive()) &&
            !interventionRepository.hasInterventionForDomain(session.getId(),
                SdohInterventionEntity.HrsnDomain.INTERPERSONAL_SAFETY)) {
            positiveDomainsWithoutIntervention.add("Interpersonal Safety");
        }

        if (positiveDomainsWithoutIntervention.isEmpty()) {
            return null; // All positive domains have interventions
        }

        return SdohCareGap.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .measureId(SDOH_INTERVENTION_GAP_MEASURE_ID)
            .measureName("SDOH Screen Positive Rate (SDOH-2)")
            .measureCategory("CMS")
            .gapType("sdoh-intervention")
            .gapStatus("open")
            .gapDescription("Positive SDOH screen without documented intervention for: " +
                String.join(", ", positiveDomainsWithoutIntervention))
            .gapReason("Patient screened positive for HRSN needs without documented intervention or referral")
            .priority("high") // Positive screens without intervention are high priority
            .recommendation("Provide referral to community resources or social services for identified HRSN needs")
            .recommendedAction("Create intervention or referral for positive SDOH domains")
            .screeningSessionId(session.getId())
            .positiveDomainCount(session.getPositiveDomainCount())
            .positiveDomainsWithoutIntervention(positiveDomainsWithoutIntervention)
            .build();
    }

    /**
     * Publish SDOH care gap to Kafka for processing by care gap service.
     */
    public void publishSdohCareGap(SdohCareGap gap) {
        try {
            String event = String.format(
                "{\"tenantId\":\"%s\",\"patientId\":\"%s\",\"measureId\":\"%s\",\"measureName\":\"%s\"," +
                "\"gapType\":\"%s\",\"gapStatus\":\"%s\",\"gapDescription\":\"%s\",\"priority\":\"%s\"," +
                "\"recommendation\":\"%s\",\"timestamp\":\"%s\"}",
                gap.getTenantId(),
                gap.getPatientId(),
                gap.getMeasureId(),
                gap.getMeasureName(),
                gap.getGapType(),
                gap.getGapStatus(),
                gap.getGapDescription(),
                gap.getPriority(),
                gap.getRecommendation(),
                LocalDate.now()
            );
            kafkaTemplate.send(CARE_GAP_TOPIC, event);
            log.info("Published SDOH care gap event for patient {}", gap.getPatientId());
        } catch (Exception e) {
            log.error("Error publishing SDOH care gap event: {}", e.getMessage());
        }
    }

    /**
     * Process completed HRSN screening session and update care gaps.
     *
     * Called when a screening session is completed. Automatically:
     * 1. Closes SDOH-1 gap if all domains screened
     * 2. Creates SDOH-2 gap if positive screen without intervention
     *
     * @param session The completed screening session
     */
    @Transactional
    public void processCompletedScreening(HrsnScreeningSessionEntity session) {
        log.info("Processing completed HRSN screening session {} for patient {}",
            session.getId(), session.getPatientId());

        // Get measurement period (current year)
        LocalDateTime periodStart = LocalDateTime.of(LocalDate.now().getYear(), 1, 1, 0, 0);
        LocalDateTime periodEnd = LocalDateTime.of(LocalDate.now().getYear(), 12, 31, 23, 59);

        // Re-evaluate care gaps
        List<SdohCareGap> gaps = evaluateSdohCareGaps(
            session.getTenantId(),
            session.getPatientId(),
            periodStart,
            periodEnd
        );

        // Publish gaps for processing
        for (SdohCareGap gap : gaps) {
            publishSdohCareGap(gap);
        }
    }

    /**
     * SDOH Care Gap DTO
     */
    @lombok.Data
    @lombok.Builder
    public static class SdohCareGap {
        private String tenantId;
        private UUID patientId;
        private String measureId;
        private String measureName;
        private String measureCategory;
        private String gapType;
        private String gapStatus;
        private String gapDescription;
        private String gapReason;
        private String priority;
        private String recommendation;
        private String recommendedAction;
        private String billingCode;

        // SDOH-1 specific fields
        private Integer domainsScreenedCount;
        private List<String> missingDomains;

        // SDOH-2 specific fields
        private UUID screeningSessionId;
        private Integer positiveDomainCount;
        private List<String> positiveDomainsWithoutIntervention;
    }
}
