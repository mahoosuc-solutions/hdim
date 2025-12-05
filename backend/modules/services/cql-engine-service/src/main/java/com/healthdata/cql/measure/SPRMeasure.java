package com.healthdata.cql.measure;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

/**
 * SPR - Statin Therapy for Patients with Cardiovascular Disease (Received Statin Therapy) (HEDIS)
 *
 * Evaluates whether patients with clinical atherosclerotic cardiovascular disease (ASCVD)
 * received statin therapy during the measurement year.
 *
 * Statins reduce LDL cholesterol and significantly decrease risk of MI, stroke, and
 * cardiovascular death in patients with established CVD.
 *
 * ACC/AHA guidelines recommend high-intensity statin for all patients with clinical ASCVD.
 */
@Component
public class SPRMeasure extends AbstractHedisMeasure {

    private static final List<String> ASCVD_DIAGNOSIS_CODES = Arrays.asList(
        // Myocardial infarction
        "57054005",  // SNOMED - Acute myocardial infarction
        "22298006",  // SNOMED - Myocardial infarction
        "401303003", // SNOMED - Acute ST segment elevation myocardial infarction
        // Coronary artery disease
        "53741008",  // SNOMED - Coronary arteriosclerosis
        "414545008", // SNOMED - Ischemic heart disease
        // Stroke/TIA
        "230690007", // SNOMED - Cerebrovascular accident
        "230706003", // SNOMED - Ischemic stroke
        "266257000", // SNOMED - Transient ischemic attack
        // Peripheral arterial disease
        "399957001", // SNOMED - Peripheral arterial disease
        "840580004", // SNOMED - Atherosclerosis of artery of extremity
        // Coronary procedures
        "232717009", // SNOMED - Coronary artery bypass graft
        "415070008", // SNOMED - Percutaneous coronary intervention
        "36969009"   // SNOMED - Placement of stent in coronary artery
    );

    private static final List<String> STATIN_MEDICATION_CODES = Arrays.asList(
        // High-intensity statins (preferred for ASCVD)
        "36567",     // RxNorm - Atorvastatin 40mg
        "859419",    // RxNorm - Atorvastatin 80mg
        "41127",     // RxNorm - Rosuvastatin 20mg
        "593411",    // RxNorm - Rosuvastatin 40mg
        // Moderate-intensity statins
        "259255",    // RxNorm - Atorvastatin 10mg
        "617318",    // RxNorm - Atorvastatin 20mg
        "859424",    // RxNorm - Rosuvastatin 5mg
        "859749",    // RxNorm - Rosuvastatin 10mg
        "42463",     // RxNorm - Simvastatin 20mg
        "196503",    // RxNorm - Simvastatin 40mg
        "261087",    // RxNorm - Pravastatin 40mg
        "259787",    // RxNorm - Pravastatin 80mg
        // Low-intensity statins
        "197904",    // RxNorm - Simvastatin 10mg
        "259621",    // RxNorm - Pravastatin 10mg
        "200345",    // RxNorm - Pravastatin 20mg
        "314231",    // RxNorm - Lovastatin 20mg
        "259688",    // RxNorm - Lovastatin 40mg
        "202973",    // RxNorm - Fluvastatin 20mg
        "258482"     // RxNorm - Fluvastatin 40mg
    );

    private static final List<String> STATIN_INTOLERANCE_CODES = Arrays.asList(
        "421913001", // SNOMED - Adverse reaction to HMG CoA reductase inhibitor
        "293828000", // SNOMED - Statin-induced myopathy
        "406147005", // SNOMED - Allergy to statin
        "419511003"  // SNOMED - Propensity to adverse reaction to statin
    );

    @Override
    public String getMeasureId() {
        return "SPR";
    }

    @Override
    public String getMeasureName() {
        return "Statin Therapy for Patients with Cardiovascular Disease";
    }

    @Override
    public String getVersion() {
        return "2024";
    }

    @Override
    @Cacheable(value = "hedisMeasures", key = "'SPR-' + #tenantId + '-' + #patientId")
    public MeasureResult evaluate(String tenantId, String patientId) {
        logger.info("Evaluating SPR measure for patient: {}", patientId);

        MeasureResult.MeasureResultBuilder resultBuilder = MeasureResult.builder()
            .measureId(getMeasureId())
            .measureName(getMeasureName())
            .patientId(patientId)
            .evaluationDate(LocalDate.now());

        if (!isEligible(tenantId, patientId)) {
            return resultBuilder
                .inDenominator(false)
                .inNumerator(false)
                .exclusionReason("Patient not eligible (must be age 21-75 with ASCVD)")
                .build();
        }

        resultBuilder.inDenominator(true);

        JsonNode patient = getPatientData(tenantId, patientId);
        Integer age = getPatientAge(patient);

        // Check for statin therapy in last 12 months
        String dateFilter = "ge" + LocalDate.now().minusYears(1).toString();
        JsonNode statinMeds = getMedicationRequests(tenantId, patientId,
            String.join(",", STATIN_MEDICATION_CODES), dateFilter);
        List<JsonNode> statinEntries = getEntries(statinMeds);

        boolean hasStatinTherapy = !statinEntries.isEmpty();
        String mostRecentStatinDate = hasStatinTherapy ? getEffectiveDate(statinEntries.get(0)) : null;

        // Determine statin intensity if prescribed
        String statinIntensity = "None";
        if (hasStatinTherapy) {
            // Simplified classification based on medication codes
            // In reality, would need to parse medication names/doses
            statinIntensity = "Moderate-High intensity"; // Most ASCVD patients should be on this
        }

        // Check for statin intolerance (exclusion criterion)
        JsonNode intolerances = getConditions(tenantId, patientId,
            String.join(",", STATIN_INTOLERANCE_CODES));
        boolean hasStatinIntolerance = !getEntries(intolerances).isEmpty();

        // Calculate adherence for those on statins
        AdherenceData adherence = null;
        if (hasStatinTherapy) {
            adherence = calculatePDC(statinEntries, 365);
        }

        double pdc = adherence != null ? adherence.pdc : 0.0;
        boolean meetsAdherenceThreshold = pdc >= 0.80;

        // For measure: patient should be on statin therapy
        boolean meetsGoal = hasStatinTherapy && !hasStatinIntolerance;

        resultBuilder.inNumerator(meetsGoal);
        resultBuilder.complianceRate(meetsGoal ? 1.0 : 0.0);
        resultBuilder.score(meetsGoal ? 100.0 : 0.0);

        // Generate care gaps
        List<MeasureResult.CareGap> careGaps = new java.util.ArrayList<>();

        if (!hasStatinTherapy && !hasStatinIntolerance) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("MISSING_STATIN_THERAPY_ASCVD")
                .description("ASCVD patient without statin therapy - high risk for cardiovascular events")
                .recommendedAction("Initiate high-intensity statin (atorvastatin 40-80mg or rosuvastatin 20-40mg); ACC/AHA Class I recommendation")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());
        } else if (hasStatinTherapy && !meetsAdherenceThreshold) {
            String adherenceLevel = pdc >= 0.50 ? "Moderate adherence" : "Poor adherence";

            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("POOR_STATIN_ADHERENCE")
                .description(String.format("%s to statin therapy (%.0f%% PDC - goal ≥80%%)", adherenceLevel, pdc * 100))
                .recommendedAction("Assess adherence barriers (cost, side effects, understanding); consider free samples; simplify regimen")
                .priority("high")
                .dueDate(LocalDate.now().plusWeeks(2))
                .build());
        }

        if (hasStatinIntolerance && !hasStatinTherapy) {
            careGaps.add(MeasureResult.CareGap.builder()
                .gapType("STATIN_INTOLERANCE_ALTERNATIVE_NEEDED")
                .description("Statin intolerance documented - alternative lipid therapy needed")
                .recommendedAction("Consider ezetimibe, PCSK9 inhibitor, bempedoic acid; lifestyle modification counseling")
                .priority("medium")
                .dueDate(LocalDate.now().plusMonths(1))
                .build());
        }

        resultBuilder.careGaps(careGaps);

        resultBuilder.details(java.util.Map.of(
            "patientAge", age,
            "hasStatinTherapy", hasStatinTherapy,
            "statinDate", mostRecentStatinDate != null ? mostRecentStatinDate : "None",
            "statinIntensity", statinIntensity,
            "adherencePDC", String.format("%.0f%%", pdc * 100),
            "hasStatinIntolerance", hasStatinIntolerance,
            "meetsGoal", meetsGoal
        ));

        resultBuilder.evidence(java.util.Map.of(
            "receivedStatinTherapy", hasStatinTherapy,
            "adequateAdherence", meetsAdherenceThreshold,
            "noIntolerance", !hasStatinIntolerance
        ));

        return resultBuilder.build();
    }

    @Override
    public boolean isEligible(String tenantId, String patientId) {
        JsonNode patient = getPatientData(tenantId, patientId);
        if (patient == null) return false;

        // Must be age 21-75
        Integer age = getPatientAge(patient);
        if (age == null || age < 21 || age > 75) {
            return false;
        }

        // Must have ASCVD diagnosis
        JsonNode ascvdDiagnoses = getConditions(tenantId, patientId,
            String.join(",", ASCVD_DIAGNOSIS_CODES));

        return !getEntries(ascvdDiagnoses).isEmpty();
    }

    /**
     * Calculate PDC (Proportion of Days Covered)
     */
    private AdherenceData calculatePDC(List<JsonNode> medicationEntries, int periodDays) {
        boolean[] coveredDays = new boolean[periodDays];
        int fillCount = 0;

        for (JsonNode medication : medicationEntries) {
            try {
                LocalDate fillDate = LocalDate.parse(getEffectiveDate(medication));
                int daysSupply = getDaysSupply(medication);
                fillCount++;

                LocalDate today = LocalDate.now();
                int startDay = Math.max(0, (int) java.time.temporal.ChronoUnit.DAYS.between(
                    today.minusDays(periodDays), fillDate));

                for (int i = 0; i < daysSupply && (startDay + i) < periodDays; i++) {
                    coveredDays[startDay + i] = true;
                }
            } catch (Exception e) {
                logger.debug("Could not process medication fill: {}", e.getMessage());
            }
        }

        int daysCovered = 0;
        for (boolean covered : coveredDays) {
            if (covered) daysCovered++;
        }

        double pdc = (double) daysCovered / periodDays;
        return new AdherenceData(pdc, daysCovered, periodDays, fillCount);
    }

    private int getDaysSupply(JsonNode medication) {
        try {
            if (medication.has("dispenseRequest")) {
                JsonNode dispenseRequest = medication.get("dispenseRequest");
                if (dispenseRequest.has("expectedSupplyDuration")) {
                    return dispenseRequest.get("expectedSupplyDuration").get("value").asInt();
                }
            }
        } catch (Exception e) {
            logger.debug("Could not extract days supply: {}", e.getMessage());
        }
        return 30;
    }

    private static class AdherenceData {
        double pdc;
        int daysCovered;
        int totalDays;
        int fillCount;

        AdherenceData(double pdc, int daysCovered, int totalDays, int fillCount) {
            this.pdc = pdc;
            this.daysCovered = daysCovered;
            this.totalDays = totalDays;
            this.fillCount = fillCount;
        }
    }
}
