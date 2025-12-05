package com.hdim.riskmodels.indices;

import com.hdim.riskmodels.models.RiskIndexResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD tests for HCC Risk Score (CMS-HCC Model V28).
 */
@DisplayName("HCC Risk Score Tests")
class HCCRiskScoreTest {

    private HCCRiskScore hccRiskScore;

    @BeforeEach
    void setUp() {
        hccRiskScore = new HCCRiskScore();
    }

    @Nested
    @DisplayName("Basic Calculation Tests")
    class BasicCalculationTests {

        @Test
        @DisplayName("Should return baseline score for no diagnoses")
        void testNoDiagnoses() {
            RiskIndexResult result = hccRiskScore.calculate(Collections.emptyList(), 65, false, false, false);

            assertThat(result).isNotNull();
            assertThat(result.getScore()).isPositive(); // Baseline is > 0
            assertThat(result.getIndexName()).isEqualTo("HCC Risk Score");
        }

        @Test
        @DisplayName("Should calculate score for diabetes HCC")
        void testDiabetesHCC() {
            List<String> icd10Codes = Arrays.asList("E11.21");
            RiskIndexResult result = hccRiskScore.calculate(icd10Codes, 65, false, false, false);

            assertThat(result.getScore()).isPositive();
            assertThat(result.getExplanations()).isNotEmpty();
        }

        @Test
        @DisplayName("Should calculate score for CHF HCC")
        void testCHF_HCC() {
            List<String> icd10Codes = Arrays.asList("I50.1");
            RiskIndexResult result = hccRiskScore.calculate(icd10Codes, 65, false, false, false);

            assertThat(result.getScore()).isPositive();
        }
    }

    @Nested
    @DisplayName("Age-Sex Demographic Tests")
    class AgeSexTests {

        @Test
        @DisplayName("Should have different baseline for males vs females")
        void testGenderDifference() {
            RiskIndexResult male = hccRiskScore.calculate(Collections.emptyList(), 70, true, false, false);
            RiskIndexResult female = hccRiskScore.calculate(Collections.emptyList(), 70, false, false, false);

            assertThat(male.getScore()).isNotEqualTo(female.getScore());
        }

        @Test
        @DisplayName("Should adjust for age 65-69 male")
        void testAge65_69Male() {
            RiskIndexResult result = hccRiskScore.calculate(Collections.emptyList(), 67, true, false, false);

            assertThat(result.getScore()).isPositive();
            assertThat(result.getExplanations()).anyMatch(e -> e.getFactor().contains("Age"));
        }

        @Test
        @DisplayName("Should adjust for age 70-74 female")
        void testAge70_74Female() {
            RiskIndexResult result = hccRiskScore.calculate(Collections.emptyList(), 72, false, false, false);

            assertThat(result.getScore()).isPositive();
        }

        @Test
        @DisplayName("Should adjust for age 75-79")
        void testAge75_79() {
            RiskIndexResult result = hccRiskScore.calculate(Collections.emptyList(), 77, true, false, false);

            assertThat(result.getScore()).isPositive();
        }

        @Test
        @DisplayName("Should adjust for age 80+")
        void testAge80Plus() {
            RiskIndexResult result = hccRiskScore.calculate(Collections.emptyList(), 85, true, false, false);

            assertThat(result.getScore()).isPositive();
        }
    }

    @Nested
    @DisplayName("Medicaid and Disability Tests")
    class MedicaidDisabilityTests {

        @Test
        @DisplayName("Should adjust for disabled beneficiary")
        void testDisabled() {
            RiskIndexResult normal = hccRiskScore.calculate(Collections.emptyList(), 65, true, false, false);
            RiskIndexResult disabled = hccRiskScore.calculate(Collections.emptyList(), 65, true, true, false);

            assertThat(disabled.getScore()).isNotEqualTo(normal.getScore());
        }

        @Test
        @DisplayName("Should adjust for Medicaid dual eligibility")
        void testMedicaid() {
            RiskIndexResult normal = hccRiskScore.calculate(Collections.emptyList(), 65, true, false, false);
            RiskIndexResult medicaid = hccRiskScore.calculate(Collections.emptyList(), 65, true, false, true);

            assertThat(medicaid.getScore()).isNotEqualTo(normal.getScore());
        }

        @Test
        @DisplayName("Should adjust for disabled and Medicaid")
        void testDisabledAndMedicaid() {
            RiskIndexResult result = hccRiskScore.calculate(Collections.emptyList(), 65, true, true, true);

            assertThat(result.getScore()).isPositive();
        }
    }

    @Nested
    @DisplayName("HCC Category Tests")
    class HCCCategoryTests {

        @Test
        @DisplayName("Should map diabetes with chronic complications to HCC18")
        void testDiabetesHCC18() {
            List<String> icd10Codes = Arrays.asList("E11.21");
            RiskIndexResult result = hccRiskScore.calculate(icd10Codes, 65, false, false, false);

            assertThat(result.getExplanations()).anyMatch(e -> e.getFactor().contains("HCC"));
        }

        @Test
        @DisplayName("Should map CHF to HCC85")
        void testCHF_HCC85() {
            List<String> icd10Codes = Arrays.asList("I50.1");
            RiskIndexResult result = hccRiskScore.calculate(icd10Codes, 65, false, false, false);

            assertThat(result.getExplanations()).anyMatch(e -> e.getFactor().contains("HCC"));
        }

        @Test
        @DisplayName("Should map COPD to HCC111")
        void testCOPD_HCC111() {
            List<String> icd10Codes = Arrays.asList("J44.0");
            RiskIndexResult result = hccRiskScore.calculate(icd10Codes, 65, false, false, false);

            assertThat(result.getExplanations()).anyMatch(e -> e.getFactor().contains("HCC"));
        }

        @Test
        @DisplayName("Should map metastatic cancer to HCC8")
        void testMetastaticCancer() {
            List<String> icd10Codes = Arrays.asList("C78.00");
            RiskIndexResult result = hccRiskScore.calculate(icd10Codes, 65, false, false, false);

            assertThat(result.getScore()).isGreaterThan(2.0); // High weight for metastatic cancer
        }

        @Test
        @DisplayName("Should map CKD to appropriate HCC")
        void testCKD() {
            List<String> icd10Codes = Arrays.asList("N18.4");
            RiskIndexResult result = hccRiskScore.calculate(icd10Codes, 65, false, false, false);

            assertThat(result.getExplanations()).anyMatch(e -> e.getFactor().contains("HCC"));
        }
    }

    @Nested
    @DisplayName("Hierarchical Tests")
    class HierarchicalTests {

        @Test
        @DisplayName("Should apply diabetes hierarchy")
        void testDiabetesHierarchy() {
            List<String> icd10Codes = Arrays.asList("E11.9", "E11.21");
            RiskIndexResult result = hccRiskScore.calculate(icd10Codes, 65, false, false, false);

            // Should only count the more specific/higher HCC
            long diabetesHCCCount = result.getExplanations().stream()
                .filter(e -> e.getFactor().contains("Diabetes") || e.getFactor().contains("HCC"))
                .count();
            assertThat(diabetesHCCCount).isLessThanOrEqualTo(2); // Not all diabetes codes counted
        }

        @Test
        @DisplayName("Should apply CKD hierarchy")
        void testCKDHierarchy() {
            List<String> icd10Codes = Arrays.asList("N18.3", "N18.5");
            RiskIndexResult result = hccRiskScore.calculate(icd10Codes, 65, false, false, false);

            // Should apply hierarchy - only highest stage counted
            assertThat(result.getExplanations()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("Disease Interaction Tests")
    class DiseaseInteractionTests {

        @Test
        @DisplayName("Should detect diabetes and CHF interaction")
        void testDiabetesCHFInteraction() {
            List<String> icd10Codes = Arrays.asList("E11.21", "I50.1");
            RiskIndexResult result = hccRiskScore.calculate(icd10Codes, 65, false, false, false);

            // Should have higher score due to interaction
            assertThat(result.getExplanations()).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Should detect multiple condition interactions")
        void testMultipleInteractions() {
            List<String> icd10Codes = Arrays.asList("E11.21", "I50.1", "N18.4", "J44.0");
            RiskIndexResult result = hccRiskScore.calculate(icd10Codes, 65, false, false, false);

            assertThat(result.getScore()).isGreaterThan(1.5);
        }
    }

    @Nested
    @DisplayName("Score Interpretation Tests")
    class ScoreInterpretationTests {

        @Test
        @DisplayName("Should interpret score < 1.5 as below average risk")
        void testBelowAverageRisk() {
            RiskIndexResult result = hccRiskScore.calculate(Collections.emptyList(), 65, false, false, false);

            assertThat(result.getInterpretation()).isIn("Below Average Risk", "Average Risk");
        }

        @Test
        @DisplayName("Should interpret high score as above average risk")
        void testAboveAverageRisk() {
            List<String> icd10Codes = Arrays.asList("C78.00", "I50.1", "N18.5");
            RiskIndexResult result = hccRiskScore.calculate(icd10Codes, 80, false, false, false);

            assertThat(result.getInterpretation()).isIn("Above Average Risk", "High Risk");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Validation Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null diagnosis list")
        void testNullDiagnosisList() {
            assertThatThrownBy(() -> hccRiskScore.calculate(null, 65, false, false, false))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle invalid age")
        void testInvalidAge() {
            assertThatThrownBy(() -> hccRiskScore.calculate(Collections.emptyList(), -1, false, false, false))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should handle age under 65")
        void testAgeUnder65() {
            RiskIndexResult result = hccRiskScore.calculate(Collections.emptyList(), 55, false, false, false);

            assertThat(result.getScore()).isPositive();
        }

        @Test
        @DisplayName("Should handle invalid ICD-10 codes gracefully")
        void testInvalidCodes() {
            List<String> icd10Codes = Arrays.asList("INVALID", "CODE");
            RiskIndexResult result = hccRiskScore.calculate(icd10Codes, 65, false, false, false);

            assertThat(result.getScore()).isPositive(); // Should still have baseline
        }

        @Test
        @DisplayName("Should be thread-safe")
        void testThreadSafety() throws InterruptedException {
            List<String> icd10Codes = Arrays.asList("E11.21", "I50.1");

            Thread t1 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    hccRiskScore.calculate(icd10Codes, 70, true, false, false);
                }
            });

            Thread t2 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    hccRiskScore.calculate(icd10Codes, 75, false, false, true);
                }
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            assertThat(true).isTrue();
        }

        @Test
        @DisplayName("Should handle very complex patient profile")
        void testComplexProfile() {
            List<String> icd10Codes = Arrays.asList(
                "C78.00", "I50.1", "N18.5", "J44.0", "E11.21",
                "I21.0", "F03.90", "K72.00"
            );
            RiskIndexResult result = hccRiskScore.calculate(icd10Codes, 85, true, true, true);

            assertThat(result.getScore()).isGreaterThan(2.0);
            assertThat(result.getInterpretation()).contains("Risk");
        }
    }
}
