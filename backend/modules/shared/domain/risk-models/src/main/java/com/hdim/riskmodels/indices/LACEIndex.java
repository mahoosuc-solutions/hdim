package com.hdim.riskmodels.indices;

import com.hdim.riskmodels.models.RiskExplanation;
import com.hdim.riskmodels.models.RiskIndexResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Calculates the LACE Index for predicting 30-day readmission risk.
 *
 * LACE = Length of stay + Acuity + Comorbidities + Emergency department visits
 * Score range: 0-19
 *
 * Thread-safe implementation.
 */
public class LACEIndex {

    private static final String INDEX_NAME = "LACE Index";
    private static final String VERSION = "2024";

    private final CharlsonComorbidityIndex charlsonIndex;

    public LACEIndex() {
        this.charlsonIndex = new CharlsonComorbidityIndex();
    }

    /**
     * Calculates the LACE Index.
     *
     * @param lengthOfStayDays Length of hospital stay in days
     * @param acuteAdmission Whether admission was through emergency department
     * @param icd10Codes List of ICD-10 diagnosis codes for comorbidities
     * @param edVisitsIn6Months Number of ED visits in 6 months prior to admission
     * @return RiskIndexResult containing LACE score and explanations
     */
    public RiskIndexResult calculate(int lengthOfStayDays, boolean acuteAdmission,
                                     List<String> icd10Codes, int edVisitsIn6Months) {
        Objects.requireNonNull(icd10Codes, "ICD-10 codes list cannot be null");
        if (lengthOfStayDays < 0) {
            throw new IllegalArgumentException("Length of stay must be non-negative");
        }
        if (edVisitsIn6Months < 0) {
            throw new IllegalArgumentException("ED visits must be non-negative");
        }

        List<RiskExplanation> explanations = new ArrayList<>();
        double totalScore = 0.0;

        // L: Length of Stay
        double losScore = calculateLengthOfStayScore(lengthOfStayDays);
        totalScore += losScore;
        if (losScore > 0) {
            explanations.add(RiskExplanation.builder()
                .factor("Length of Stay")
                .description(String.format("%d day(s) hospital stay", lengthOfStayDays))
                .contribution(losScore)
                .build());
        }

        // A: Acuity of Admission
        if (acuteAdmission) {
            totalScore += 3.0;
            explanations.add(RiskExplanation.builder()
                .factor("Acute Admission")
                .description("Emergency admission through ED")
                .contribution(3.0)
                .build());
        }

        // C: Comorbidities (based on Charlson)
        double comorbidityScore = calculateComorbidityScore(icd10Codes);
        if (comorbidityScore > 0) {
            totalScore += comorbidityScore;
            explanations.add(RiskExplanation.builder()
                .factor("Comorbidity Score")
                .description(String.format("Charlson-based comorbidity burden (score: %.0f)", comorbidityScore))
                .contribution(comorbidityScore)
                .build());
        }

        // E: Emergency Department Visits
        double edScore = calculateEDVisitsScore(edVisitsIn6Months);
        if (edScore > 0) {
            totalScore += edScore;
            explanations.add(RiskExplanation.builder()
                .factor("ED Visits")
                .description(String.format("%d ED visit(s) in past 6 months", edVisitsIn6Months))
                .contribution(edScore)
                .build());
        }

        String interpretation = interpretScore(totalScore);

        return RiskIndexResult.builder()
            .indexName(INDEX_NAME)
            .score(totalScore)
            .interpretation(interpretation)
            .explanations(explanations)
            .calculatedAt(Instant.now())
            .version(VERSION)
            .build();
    }

    /**
     * Calculates Length of Stay score (0-7 points).
     */
    private double calculateLengthOfStayScore(int days) {
        if (days == 0) return 0;
        if (days == 1) return 1;
        if (days == 2) return 2;
        if (days == 3) return 3;
        if (days <= 6) return 4;
        if (days <= 13) return 5;
        return 7; // 14+ days
    }

    /**
     * Calculates Comorbidity score based on Charlson (0-5 points).
     */
    private double calculateComorbidityScore(List<String> icd10Codes) {
        if (icd10Codes.isEmpty()) {
            return 0;
        }

        // Use Charlson without age adjustment for LACE
        RiskIndexResult charlsonResult = charlsonIndex.calculate(icd10Codes, 0);
        double charlsonScore = charlsonResult.getScore();

        if (charlsonScore == 0) return 0;
        if (charlsonScore <= 2) return 1;
        if (charlsonScore <= 4) return 2;
        return 5; // Charlson >= 5
    }

    /**
     * Calculates ED Visits score (0-4 points).
     */
    private double calculateEDVisitsScore(int edVisits) {
        if (edVisits == 0) return 0;
        if (edVisits == 1) return 1;
        if (edVisits == 2) return 2;
        if (edVisits == 3) return 3;
        return 4; // 4+ visits
    }

    /**
     * Interprets the LACE score into risk categories.
     */
    private String interpretScore(double score) {
        if (score < 5) {
            return "Low Risk";
        } else if (score < 10) {
            return "Medium Risk";
        } else {
            return "High Risk";
        }
    }
}
