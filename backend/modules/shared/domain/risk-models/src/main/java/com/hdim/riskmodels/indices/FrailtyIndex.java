package com.hdim.riskmodels.indices;

import com.hdim.riskmodels.models.RiskExplanation;
import com.hdim.riskmodels.models.RiskIndexResult;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Calculates Frailty Index based on deficit accumulation model.
 * Assesses 10 key frailty indicators.
 */
public class FrailtyIndex {

    private static final String INDEX_NAME = "Frailty Index";
    private static final String VERSION = "2024";

    public RiskIndexResult calculate(
        boolean hasWeightLoss,
        boolean hasLowActivity,
        boolean hasExhaustion,
        boolean hasWeakness,
        boolean hasSlowWalking,
        boolean hasCognitiveDecline,
        boolean hasMultipleFalls,
        boolean hasADLDependency,
        boolean hasPolypharmacy,
        boolean hasMultiMorbidity
    ) {
        List<RiskExplanation> explanations = new ArrayList<>();
        int deficitCount = 0;

        if (hasWeightLoss) {
            deficitCount++;
            explanations.add(RiskExplanation.builder()
                .factor("Weight Loss")
                .description("Unintentional weight loss")
                .contribution(0.1)
                .build());
        }

        if (hasLowActivity) {
            deficitCount++;
            explanations.add(RiskExplanation.builder()
                .factor("Low Activity")
                .description("Reduced physical activity")
                .contribution(0.1)
                .build());
        }

        if (hasExhaustion) {
            deficitCount++;
            explanations.add(RiskExplanation.builder()
                .factor("Exhaustion")
                .description("Self-reported exhaustion")
                .contribution(0.1)
                .build());
        }

        if (hasWeakness) {
            deficitCount++;
            explanations.add(RiskExplanation.builder()
                .factor("Weakness")
                .description("Reduced grip strength")
                .contribution(0.1)
                .build());
        }

        if (hasSlowWalking) {
            deficitCount++;
            explanations.add(RiskExplanation.builder()
                .factor("Slow Walking")
                .description("Reduced gait speed")
                .contribution(0.1)
                .build());
        }

        if (hasCognitiveDecline) {
            deficitCount++;
            explanations.add(RiskExplanation.builder()
                .factor("Cognitive Decline")
                .description("Impaired cognition")
                .contribution(0.1)
                .build());
        }

        if (hasMultipleFalls) {
            deficitCount++;
            explanations.add(RiskExplanation.builder()
                .factor("Multiple Falls")
                .description("History of falls")
                .contribution(0.1)
                .build());
        }

        if (hasADLDependency) {
            deficitCount++;
            explanations.add(RiskExplanation.builder()
                .factor("ADL Dependency")
                .description("Activities of daily living dependency")
                .contribution(0.1)
                .build());
        }

        if (hasPolypharmacy) {
            deficitCount++;
            explanations.add(RiskExplanation.builder()
                .factor("Polypharmacy")
                .description("Multiple medications (5+)")
                .contribution(0.1)
                .build());
        }

        if (hasMultiMorbidity) {
            deficitCount++;
            explanations.add(RiskExplanation.builder()
                .factor("Multi-Morbidity")
                .description("Multiple chronic conditions")
                .contribution(0.1)
                .build());
        }

        double score = deficitCount / 10.0;
        String interpretation = interpretScore(score);

        return RiskIndexResult.builder()
            .indexName(INDEX_NAME)
            .score(score)
            .interpretation(interpretation)
            .explanations(explanations)
            .calculatedAt(Instant.now())
            .version(VERSION)
            .build();
    }

    private String interpretScore(double score) {
        if (score < 0.2) {
            return "Robust";
        } else if (score < 0.4) {
            return "Pre-Frail";
        } else if (score < 0.6) {
            return "Frail";
        } else {
            return "Severely Frail";
        }
    }
}
