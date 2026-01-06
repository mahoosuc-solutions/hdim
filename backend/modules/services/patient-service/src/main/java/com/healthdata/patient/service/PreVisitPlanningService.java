package com.healthdata.patient.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.patient.dto.PreVisitSummaryResponse;
import com.healthdata.patient.dto.PreVisitSummaryResponse.*;
import com.healthdata.patient.client.CareGapServiceClient;
import com.healthdata.patient.client.FhirServiceClient;
import com.healthdata.patient.entity.PatientDemographicsEntity;
import com.healthdata.patient.repository.PatientDemographicsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Pre-Visit Planning Service
 *
 * Issue #6: Provides comprehensive pre-visit summary for providers by aggregating
 * data from FHIR service, Care Gap service, and generating AI-suggested agenda items.
 *
 * HIPAA Compliance:
 * - All PHI access is audited at the controller level
 * - No caching to ensure fresh data
 * - Multi-tenant filtering enforced at data access layer
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PreVisitPlanningService {

    private final PatientDemographicsRepository patientDemographicsRepository;
    private final FhirServiceClient fhirServiceClient;
    private final CareGapServiceClient careGapServiceClient;
    private final ObjectMapper objectMapper;

    /**
     * Generate a comprehensive pre-visit summary for a patient.
     *
     * @param tenantId   Tenant ID for multi-tenant filtering
     * @param providerId Provider ID requesting the summary
     * @param patientId  Patient ID for the summary
     * @return PreVisitSummaryResponse with all aggregated data
     */
    public PreVisitSummaryResponse getPreVisitSummary(String tenantId, String providerId, String patientId) {
        log.info("Generating pre-visit summary for patient: {} by provider: {} in tenant: {}",
                patientId, providerId, tenantId);

        // Fetch patient information - try by ID first, then by FHIR ID
        Optional<PatientDemographicsEntity> patientOpt;
        try {
            UUID patientUuid = UUID.fromString(patientId);
            patientOpt = patientDemographicsRepository.findByIdAndTenantId(patientUuid, tenantId);
        } catch (IllegalArgumentException e) {
            // Not a UUID, try by FHIR patient ID
            patientOpt = patientDemographicsRepository.findByFhirPatientIdAndTenantId(patientId, tenantId);
        }

        if (patientOpt.isEmpty()) {
            log.warn("Patient not found: {} in tenant: {}", patientId, tenantId);
            throw new RuntimeException("Patient not found: " + patientId);
        }
        PatientDemographicsEntity patient = patientOpt.get();

        // Aggregate data from various services
        List<CareGapItem> careGaps = fetchCareGaps(tenantId, patientId);
        List<RecentResultItem> recentResults = fetchRecentResults(tenantId, patientId);
        List<MedicationItem> medications = fetchMedications(tenantId, patientId);
        PatientDemographics demographics = buildDemographics(patient);
        RiskIndicators riskIndicators = buildRiskIndicators(tenantId, patientId, careGaps);

        // Generate AI-suggested agenda based on care gaps and recent results
        List<AgendaItem> suggestedAgenda = generateSuggestedAgenda(careGaps, recentResults, medications);

        // Build last visit summary
        String lastVisitSummary = buildLastVisitSummary(tenantId, patientId);

        PreVisitSummaryResponse response = PreVisitSummaryResponse.builder()
                .patientId(patientId)
                .patientName(formatPatientName(patient))
                .appointmentDate(null) // Would come from scheduling service
                .appointmentType("Follow-up") // Would come from scheduling service
                .careGaps(careGaps)
                .recentResults(recentResults)
                .medications(medications)
                .suggestedAgenda(suggestedAgenda)
                .lastVisitSummary(lastVisitSummary)
                .demographics(demographics)
                .riskIndicators(riskIndicators)
                .generatedAt(Instant.now())
                .build();

        log.info("Pre-visit summary generated for patient: {} with {} care gaps, {} results, {} medications",
                patientId, careGaps.size(), recentResults.size(), medications.size());

        return response;
    }

    /**
     * Fetch open care gaps for the patient from care-gap-service.
     */
    private List<CareGapItem> fetchCareGaps(String tenantId, String patientId) {
        try {
            List<CareGapServiceClient.CareGapDto> gaps = careGapServiceClient.getOpenCareGaps(tenantId, patientId);

            if (gaps == null || gaps.isEmpty()) {
                log.debug("No open care gaps found for patient: {}", patientId);
                return new ArrayList<>();
            }

            return gaps.stream()
                    .map(this::mapToCareGapItem)
                    .sorted(Comparator.comparing(CareGapItem::getPriority))
                    .limit(10)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.warn("Failed to fetch care gaps for patient: {}. Error: {}", patientId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private CareGapItem mapToCareGapItem(CareGapServiceClient.CareGapDto gap) {
        List<String> actions = generateCareGapActions(gap.getMeasureId(), gap.getRecommendationType());

        return CareGapItem.builder()
                .gapId(gap.getId())
                .measureId(gap.getMeasureId())
                .measureName(gap.getMeasureName())
                .priority(gap.getPriority() != null ? gap.getPriority().toUpperCase() : "MEDIUM")
                .recommendation(gap.getRecommendation())
                .dueDate(gap.getDueDate())
                .gapCategory(gap.getGapCategory())
                .suggestedActions(actions)
                .build();
    }

    /**
     * Generate suggested actions based on measure type.
     */
    private List<String> generateCareGapActions(String measureId, String recommendationType) {
        List<String> actions = new ArrayList<>();

        if (measureId == null) {
            actions.add("Review care gap details");
            return actions;
        }

        String upper = measureId.toUpperCase();

        if (upper.contains("HBA1C") || upper.contains("CDC") || upper.contains("DIABETES")) {
            actions.add("Order HbA1c lab");
            actions.add("Review current glucose control");
            actions.add("Assess medication adherence");
        } else if (upper.contains("BP") || upper.contains("HYPERTENSION") || upper.contains("CBP")) {
            actions.add("Check blood pressure");
            actions.add("Review BP log if available");
            actions.add("Assess antihypertensive compliance");
        } else if (upper.contains("COL") || upper.contains("COLORECTAL")) {
            actions.add("Discuss colonoscopy or FIT test");
            actions.add("Provide patient education");
            actions.add("Order appropriate screening");
        } else if (upper.contains("BCS") || upper.contains("BREAST") || upper.contains("MAMMOGRAM")) {
            actions.add("Order mammogram");
            actions.add("Provide imaging center contact");
        } else if (upper.contains("FLU") || upper.contains("INFLUENZA")) {
            actions.add("Administer flu vaccine");
            actions.add("Document in immunization registry");
        } else if (upper.contains("AWV") || upper.contains("WELLNESS")) {
            actions.add("Complete wellness checklist");
            actions.add("Update preventive care schedule");
        } else {
            // Generic actions based on recommendation type
            if ("IMMUNIZATION".equalsIgnoreCase(recommendationType)) {
                actions.add("Verify immunization history");
                actions.add("Administer recommended vaccine");
            } else if ("MEDICATION".equalsIgnoreCase(recommendationType)) {
                actions.add("Review current medications");
                actions.add("Consider therapy initiation/adjustment");
            } else if ("SCREENING".equalsIgnoreCase(recommendationType)) {
                actions.add("Order screening test");
                actions.add("Schedule follow-up for results");
            } else {
                actions.add("Address care gap during visit");
                actions.add("Document intervention");
            }
        }

        return actions;
    }

    /**
     * Fetch recent lab results and observations from FHIR service.
     */
    private List<RecentResultItem> fetchRecentResults(String tenantId, String patientId) {
        try {
            String observationsJson = fhirServiceClient.getLabResults(tenantId, patientId);
            if (observationsJson == null || observationsJson.isEmpty()) {
                return new ArrayList<>();
            }

            // Parse FHIR Bundle response
            JsonNode bundle = objectMapper.readTree(observationsJson);
            JsonNode entries = bundle.get("entry");
            if (entries == null || !entries.isArray()) {
                return new ArrayList<>();
            }

            List<RecentResultItem> results = new ArrayList<>();
            for (JsonNode entry : entries) {
                JsonNode resource = entry.get("resource");
                if (resource != null) {
                    RecentResultItem item = parseObservationToResult(resource);
                    if (item != null) {
                        results.add(item);
                    }
                }
            }

            return results.stream()
                    .sorted(Comparator.comparing(RecentResultItem::getDate,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(10)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Failed to fetch observations for patient: {}. Error: {}", patientId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private RecentResultItem parseObservationToResult(JsonNode resource) {
        try {
            String name = "";
            JsonNode code = resource.get("code");
            if (code != null && code.has("text")) {
                name = code.get("text").asText();
            } else if (code != null && code.has("coding") && code.get("coding").isArray()
                    && code.get("coding").size() > 0) {
                name = code.get("coding").get(0).path("display").asText("");
            }

            String value = "";
            String unit = "";
            JsonNode valueQuantity = resource.get("valueQuantity");
            if (valueQuantity != null) {
                value = valueQuantity.path("value").asText("");
                unit = valueQuantity.path("unit").asText("");
            } else if (resource.has("valueString")) {
                value = resource.get("valueString").asText("");
            }

            LocalDate date = null;
            if (resource.has("effectiveDateTime")) {
                String dateStr = resource.get("effectiveDateTime").asText();
                if (dateStr.length() >= 10) {
                    date = LocalDate.parse(dateStr.substring(0, 10));
                }
            }

            String interpretation = "normal";
            JsonNode interpNode = resource.get("interpretation");
            if (interpNode != null && interpNode.isArray() && interpNode.size() > 0) {
                JsonNode coding = interpNode.get(0).path("coding");
                if (coding.isArray() && coding.size() > 0) {
                    String interpCode = coding.get(0).path("code").asText("");
                    if ("H".equals(interpCode) || "HH".equals(interpCode) ||
                            "L".equals(interpCode) || "LL".equals(interpCode)) {
                        interpretation = "abnormal";
                    }
                    if ("HH".equals(interpCode) || "LL".equals(interpCode)) {
                        interpretation = "critical";
                    }
                }
            }

            String loincCode = "";
            if (code != null && code.has("coding") && code.get("coding").isArray()) {
                for (JsonNode coding : code.get("coding")) {
                    if ("http://loinc.org".equals(coding.path("system").asText())) {
                        loincCode = coding.path("code").asText("");
                        break;
                    }
                }
            }

            return RecentResultItem.builder()
                    .name(name)
                    .value(value)
                    .unit(unit)
                    .date(date)
                    .trend("stable") // Would need historical data to calculate
                    .previousValue(null)
                    .previousDate(null)
                    .interpretation(interpretation)
                    .loincCode(loincCode)
                    .build();

        } catch (Exception e) {
            log.debug("Failed to parse observation: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Fetch current medications from FHIR service.
     */
    private List<MedicationItem> fetchMedications(String tenantId, String patientId) {
        try {
            String medsJson = fhirServiceClient.getActiveMedications(tenantId, patientId);
            if (medsJson == null || medsJson.isEmpty()) {
                return new ArrayList<>();
            }

            // Parse FHIR Bundle response
            JsonNode bundle = objectMapper.readTree(medsJson);
            JsonNode entries = bundle.get("entry");
            if (entries == null || !entries.isArray()) {
                return new ArrayList<>();
            }

            List<MedicationItem> medications = new ArrayList<>();
            for (JsonNode entry : entries) {
                JsonNode resource = entry.get("resource");
                if (resource != null) {
                    MedicationItem item = parseMedicationRequestToItem(resource);
                    if (item != null) {
                        medications.add(item);
                    }
                }
            }

            return medications.stream()
                    .sorted(Comparator.comparing(MedicationItem::getName))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Failed to fetch medications for patient: {}. Error: {}", patientId, e.getMessage());
            return new ArrayList<>();
        }
    }

    private MedicationItem parseMedicationRequestToItem(JsonNode resource) {
        try {
            String name = "";
            JsonNode medCodeableConcept = resource.get("medicationCodeableConcept");
            if (medCodeableConcept != null && medCodeableConcept.has("text")) {
                name = medCodeableConcept.get("text").asText();
            } else if (medCodeableConcept != null && medCodeableConcept.has("coding") &&
                    medCodeableConcept.get("coding").isArray() &&
                    medCodeableConcept.get("coding").size() > 0) {
                name = medCodeableConcept.get("coding").get(0).path("display").asText("");
            }

            String dosage = "";
            String frequency = "";
            JsonNode dosageInstr = resource.get("dosageInstruction");
            if (dosageInstr != null && dosageInstr.isArray() && dosageInstr.size() > 0) {
                JsonNode firstDosage = dosageInstr.get(0);
                if (firstDosage.has("text")) {
                    dosage = firstDosage.get("text").asText();
                }
                JsonNode timing = firstDosage.get("timing");
                if (timing != null && timing.has("code") && timing.get("code").has("text")) {
                    frequency = timing.get("code").get("text").asText();
                }
            }

            String status = resource.path("status").asText("active");

            LocalDate startDate = null;
            if (resource.has("authoredOn")) {
                String dateStr = resource.get("authoredOn").asText();
                if (dateStr.length() >= 10) {
                    startDate = LocalDate.parse(dateStr.substring(0, 10));
                }
            }

            return MedicationItem.builder()
                    .name(name)
                    .dosage(dosage)
                    .frequency(frequency)
                    .adherence("unknown") // Would need adherence data
                    .startDate(startDate)
                    .prescriber(null) // Would need to resolve requester reference
                    .status(status)
                    .potentialIssues(new ArrayList<>())
                    .build();

        } catch (Exception e) {
            log.debug("Failed to parse medication: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Build patient demographics for quick reference.
     */
    private PatientDemographics buildDemographics(PatientDemographicsEntity patient) {
        Integer age = null;
        if (patient.getDateOfBirth() != null) {
            age = Period.between(patient.getDateOfBirth(), LocalDate.now()).getYears();
        }

        return PatientDemographics.builder()
                .age(age)
                .gender(patient.getGender())
                .preferredLanguage(patient.getPreferredLanguage())
                .primaryInsurance(null) // Would come from coverage service
                .phoneNumber(patient.getPhone())
                .lastVisitDate(null) // Would come from encounter service
                .build();
    }

    /**
     * Build risk indicators based on available data.
     */
    private RiskIndicators buildRiskIndicators(String tenantId, String patientId, List<CareGapItem> careGaps) {
        // Determine risk level based on care gap count and priorities
        long highPriorityCount = careGaps.stream()
                .filter(g -> "HIGH".equalsIgnoreCase(g.getPriority()))
                .count();

        String riskLevel;
        if (highPriorityCount >= 3 || careGaps.size() >= 7) {
            riskLevel = "very-high";
        } else if (highPriorityCount >= 2 || careGaps.size() >= 5) {
            riskLevel = "high";
        } else if (highPriorityCount >= 1 || careGaps.size() >= 3) {
            riskLevel = "moderate";
        } else {
            riskLevel = "low";
        }

        // Extract chronic conditions from care gaps
        List<String> conditions = careGaps.stream()
                .filter(g -> g.getMeasureId() != null)
                .map(g -> {
                    String id = g.getMeasureId().toUpperCase();
                    if (id.contains("DIABETES") || id.contains("HBA1C") || id.contains("CDC")) {
                        return "Diabetes";
                    } else if (id.contains("HYPERTENSION") || id.contains("CBP") || id.contains("BP")) {
                        return "Hypertension";
                    } else if (id.contains("ASTHMA") || id.contains("COPD")) {
                        return "Respiratory";
                    } else if (id.contains("DEPRESSION") || id.contains("BEHAVIORAL")) {
                        return "Behavioral Health";
                    }
                    return null;
                })
                .filter(c -> c != null)
                .distinct()
                .collect(Collectors.toList());

        return RiskIndicators.builder()
                .hccScore(null) // Would come from HCC service
                .riskLevel(riskLevel)
                .chronicConditions(conditions)
                .hasRecentHospitalization(false) // Would check encounters
                .hasRecentEDVisit(false) // Would check encounters
                .missedAppointments(0) // Would come from scheduling
                .build();
    }

    /**
     * Generate AI-suggested agenda items based on care gaps, results, and medications.
     */
    private List<AgendaItem> generateSuggestedAgenda(
            List<CareGapItem> careGaps,
            List<RecentResultItem> results,
            List<MedicationItem> medications) {

        List<AgendaItem> agenda = new ArrayList<>();
        int priority = 1;

        // Priority 1: Address high-priority care gaps
        for (CareGapItem gap : careGaps.stream()
                .filter(g -> "HIGH".equalsIgnoreCase(g.getPriority()))
                .limit(3)
                .collect(Collectors.toList())) {

            List<String> talkingPoints = new ArrayList<>();
            talkingPoints.add("Review current status");
            talkingPoints.addAll(gap.getSuggestedActions());

            agenda.add(AgendaItem.builder()
                    .topic("Address: " + gap.getMeasureName())
                    .timeEstimate(estimateTime(gap))
                    .priority(priority++)
                    .category("care-gap")
                    .rationale("High priority care gap due " +
                            (gap.getDueDate() != null ? "by " + gap.getDueDate() : "soon"))
                    .talkingPoints(talkingPoints)
                    .build());
        }

        // Priority 2: Review abnormal results
        for (RecentResultItem result : results.stream()
                .filter(r -> "abnormal".equalsIgnoreCase(r.getInterpretation()) ||
                        "critical".equalsIgnoreCase(r.getInterpretation()))
                .limit(2)
                .collect(Collectors.toList())) {

            List<String> talkingPoints = new ArrayList<>();
            talkingPoints.add("Current: " + result.getValue() + " " + (result.getUnit() != null ? result.getUnit() : ""));
            if (result.getPreviousValue() != null) {
                talkingPoints.add("Previous: " + result.getPreviousValue());
            }
            talkingPoints.add("Trend: " + (result.getTrend() != null ? result.getTrend() : "unknown"));

            agenda.add(AgendaItem.builder()
                    .topic("Review " + result.getName() + " result")
                    .timeEstimate("3 min")
                    .priority(priority++)
                    .category("result-review")
                    .rationale("Recent " + result.getInterpretation() + " result requires discussion")
                    .talkingPoints(talkingPoints)
                    .build());
        }

        // Priority 3: Medication review if multiple active medications
        if (medications.size() >= 5) {
            List<String> talkingPoints = new ArrayList<>();
            talkingPoints.add("Review " + medications.size() + " active medications");
            talkingPoints.add("Check for adherence issues");
            talkingPoints.add("Assess for duplications or interactions");

            agenda.add(AgendaItem.builder()
                    .topic("Medication reconciliation")
                    .timeEstimate("5 min")
                    .priority(priority++)
                    .category("medication")
                    .rationale("Patient on multiple medications")
                    .talkingPoints(talkingPoints)
                    .build());
        }

        // Priority 4: Medium priority care gaps
        for (CareGapItem gap : careGaps.stream()
                .filter(g -> "MEDIUM".equalsIgnoreCase(g.getPriority()))
                .limit(2)
                .collect(Collectors.toList())) {

            agenda.add(AgendaItem.builder()
                    .topic("Discuss: " + gap.getMeasureName())
                    .timeEstimate("3 min")
                    .priority(priority++)
                    .category("care-gap")
                    .rationale("Medium priority care gap to address")
                    .talkingPoints(gap.getSuggestedActions())
                    .build());
        }

        // Always add a closing item
        List<String> closingPoints = new ArrayList<>();
        closingPoints.add("Answer patient questions");
        closingPoints.add("Confirm follow-up plan");
        closingPoints.add("Verify patient understanding");

        agenda.add(AgendaItem.builder()
                .topic("Questions and follow-up")
                .timeEstimate("3 min")
                .priority(priority)
                .category("counseling")
                .rationale("Standard visit closure")
                .talkingPoints(closingPoints)
                .build());

        return agenda;
    }

    /**
     * Estimate time for addressing a care gap.
     */
    private String estimateTime(CareGapItem gap) {
        String measureId = gap.getMeasureId() != null ? gap.getMeasureId().toUpperCase() : "";

        if (measureId.contains("FLU") || measureId.contains("IMMUNIZATION")) {
            return "2 min";
        } else if (measureId.contains("HBA1C") || measureId.contains("DIABETES")) {
            return "5 min";
        } else if (measureId.contains("BP") || measureId.contains("HYPERTENSION")) {
            return "4 min";
        } else if (measureId.contains("SCREENING") || measureId.contains("COL") ||
                measureId.contains("BCS") || measureId.contains("CCS")) {
            return "3 min";
        } else if (measureId.contains("MEDICATION") || measureId.contains("SPC")) {
            return "4 min";
        }

        return "4 min"; // Default
    }

    /**
     * Build a summary of the last visit.
     */
    private String buildLastVisitSummary(String tenantId, String patientId) {
        try {
            String encountersJson = fhirServiceClient.getEncounters(tenantId, patientId);
            if (encountersJson == null || encountersJson.isEmpty()) {
                return "No recent visit data available.";
            }

            // Parse FHIR Bundle to get most recent encounter
            JsonNode bundle = objectMapper.readTree(encountersJson);
            JsonNode entries = bundle.get("entry");
            if (entries == null || !entries.isArray() || entries.size() == 0) {
                return "No recent visit data available.";
            }

            // Find most recent encounter with a reason
            for (JsonNode entry : entries) {
                JsonNode resource = entry.get("resource");
                if (resource != null && resource.has("reasonCode")) {
                    JsonNode reasonCode = resource.get("reasonCode");
                    if (reasonCode.isArray() && reasonCode.size() > 0) {
                        String reason = reasonCode.get(0).path("text").asText("");
                        if (!reason.isEmpty()) {
                            String date = resource.path("period").path("start").asText("");
                            if (date.length() >= 10) {
                                date = date.substring(0, 10);
                            }
                            return "Last visit (" + date + "): " + reason;
                        }
                    }
                }
            }

            return "Last visit data not available. Check encounter history for details.";

        } catch (Exception e) {
            log.warn("Failed to build last visit summary for patient: {}. Error: {}", patientId, e.getMessage());
            return "Last visit data not available.";
        }
    }

    /**
     * Format patient name in standard format.
     */
    private String formatPatientName(PatientDemographicsEntity patient) {
        StringBuilder name = new StringBuilder();

        if (patient.getLastName() != null) {
            name.append(patient.getLastName());
        }

        if (patient.getFirstName() != null) {
            if (name.length() > 0) {
                name.append(", ");
            }
            name.append(patient.getFirstName());
        }

        return name.length() > 0 ? name.toString() : "Unknown Patient";
    }
}
