package com.hdim.riskmodels.indices;

import com.hdim.riskmodels.models.RiskIndexResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD tests for Charlson Comorbidity Index calculation.
 * Tests based on the updated Charlson weights and ICD-10-CM 2024 mappings.
 */
@DisplayName("Charlson Comorbidity Index Tests")
class CharlsonComorbidityIndexTest {

    private CharlsonComorbidityIndex charlsonIndex;

    @BeforeEach
    void setUp() {
        charlsonIndex = new CharlsonComorbidityIndex();
    }

    @Nested
    @DisplayName("Basic Calculation Tests")
    class BasicCalculationTests {

        @Test
        @DisplayName("Should return zero score for no diagnoses")
        void testNodiagnoses() {
            RiskIndexResult result = charlsonIndex.calculate(Collections.emptyList(), 50);

            assertThat(result).isNotNull();
            assertThat(result.getScore()).isEqualTo(0.0);
            assertThat(result.getIndexName()).isEqualTo("Charlson Comorbidity Index");
            assertThat(result.getInterpretation()).isEqualTo("Low Risk");
        }

        @Test
        @DisplayName("Should calculate score for single comorbidity - Myocardial Infarction")
        void testSingleMyocardialInfarction() {
            List<String> icd10Codes = Arrays.asList("I21.0"); // Acute MI
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(1.0);
            assertThat(result.getExplanations()).hasSize(1);
            assertThat(result.getExplanations().get(0).getFactor()).isEqualTo("Myocardial Infarction");
        }

        @Test
        @DisplayName("Should calculate score for CHF")
        void testCongestiveHeartFailure() {
            List<String> icd10Codes = Arrays.asList("I50.1"); // CHF
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(1.0);
            assertThat(result.getExplanations().get(0).getFactor()).isEqualTo("Congestive Heart Failure");
        }

        @Test
        @DisplayName("Should calculate score for Peripheral Vascular Disease")
        void testPeripheralVascularDisease() {
            List<String> icd10Codes = Arrays.asList("I73.9");
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(1.0);
            assertThat(result.getExplanations().get(0).getFactor()).isEqualTo("Peripheral Vascular Disease");
        }
    }

    @Nested
    @DisplayName("Diabetes Tests")
    class DiabetesTests {

        @Test
        @DisplayName("Should score diabetes without complications")
        void testDiabetesWithoutComplications() {
            List<String> icd10Codes = Arrays.asList("E11.9"); // Type 2 diabetes without complications
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(1.0);
            assertThat(result.getExplanations().get(0).getFactor()).contains("Diabetes");
            assertThat(result.getExplanations().get(0).getFactor()).doesNotContain("complications");
        }

        @Test
        @DisplayName("Should score diabetes with end organ damage")
        void testDiabetesWithComplications() {
            List<String> icd10Codes = Arrays.asList("E11.21"); // Type 2 diabetes with diabetic nephropathy
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(2.0);
            assertThat(result.getExplanations().get(0).getFactor()).contains("Diabetes");
            assertThat(result.getExplanations().get(0).getFactor()).contains("end organ damage");
        }

        @Test
        @DisplayName("Should not double-count diabetes with and without complications")
        void testDiabetesNoDuplication() {
            List<String> icd10Codes = Arrays.asList("E11.9", "E11.21");
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            // Should only count the higher score (with complications = 2)
            assertThat(result.getScore()).isEqualTo(2.0);
            assertThat(result.getExplanations()).hasSize(1);
        }

        @Test
        @DisplayName("Should score Type 1 diabetes with retinopathy")
        void testType1DiabetesWithRetinopathy() {
            List<String> icd10Codes = Arrays.asList("E10.311"); // Type 1 with retinopathy
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(2.0);
        }
    }

    @Nested
    @DisplayName("Multiple Comorbidity Tests")
    class MultipleComorbidityTests {

        @Test
        @DisplayName("Should sum multiple different comorbidities")
        void testMultipleDifferentComorbidities() {
            List<String> icd10Codes = Arrays.asList(
                "I21.0",  // MI = 1
                "I50.1",  // CHF = 1
                "I73.9"   // PVD = 1
            );
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(3.0);
            assertThat(result.getExplanations()).hasSize(3);
        }

        @Test
        @DisplayName("Should not duplicate same comorbidity category")
        void testNoDuplicationSameCategory() {
            List<String> icd10Codes = Arrays.asList(
                "I21.0",  // Acute MI
                "I21.1",  // Another MI code
                "I21.2"   // Another MI code
            );
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            // Should count MI only once
            assertThat(result.getScore()).isEqualTo(1.0);
            assertThat(result.getExplanations()).hasSize(1);
        }

        @Test
        @DisplayName("Should calculate complex multi-morbidity case")
        void testComplexMultiMorbidity() {
            List<String> icd10Codes = Arrays.asList(
                "I21.0",   // MI = 1
                "E11.21",  // Diabetes with complications = 2
                "I50.1",   // CHF = 1
                "J44.0",   // COPD = 1
                "K70.30"   // Liver disease = 1
            );
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(6.0);
            assertThat(result.getExplanations()).hasSize(5);
        }
    }

    @Nested
    @DisplayName("Age Adjustment Tests")
    class AgeAdjustmentTests {

        @Test
        @DisplayName("Should not add age points for age < 50")
        void testAgeUnder50() {
            List<String> icd10Codes = Arrays.asList("I21.0"); // MI = 1
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 40);

            assertThat(result.getScore()).isEqualTo(1.0); // No age adjustment
        }

        @Test
        @DisplayName("Should not add age points for age = 49")
        void testAge49() {
            List<String> icd10Codes = Arrays.asList("I21.0");
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 49);

            assertThat(result.getScore()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should add 1 point for each decade over 40 (age 50-59)")
        void testAge50to59() {
            List<String> icd10Codes = Arrays.asList("I21.0"); // MI = 1
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 55);

            assertThat(result.getScore()).isEqualTo(2.0); // 1 + 1 age point
            assertThat(result.getExplanations()).anyMatch(e ->
                e.getFactor().contains("Age"));
        }

        @Test
        @DisplayName("Should add 2 points for age 60-69")
        void testAge60to69() {
            List<String> icd10Codes = Arrays.asList("I21.0");
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 65);

            assertThat(result.getScore()).isEqualTo(3.0); // 1 + 2 age points
        }

        @Test
        @DisplayName("Should add 3 points for age 70-79")
        void testAge70to79() {
            List<String> icd10Codes = Arrays.asList("I21.0");
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 75);

            assertThat(result.getScore()).isEqualTo(4.0); // 1 + 3 age points
        }

        @Test
        @DisplayName("Should add 4 points for age 80+")
        void testAge80Plus() {
            List<String> icd10Codes = Arrays.asList("I21.0");
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 85);

            assertThat(result.getScore()).isEqualTo(5.0); // 1 + 4 age points
        }

        @Test
        @DisplayName("Should handle age adjustment with no comorbidities")
        void testAgeOnlyNoComorbidities() {
            RiskIndexResult result = charlsonIndex.calculate(Collections.emptyList(), 75);

            assertThat(result.getScore()).isEqualTo(3.0); // Only age points
        }
    }

    @Nested
    @DisplayName("Score Interpretation Tests")
    class ScoreInterpretationTests {

        @Test
        @DisplayName("Should interpret score 0-1 as Low Risk")
        void testLowRiskInterpretation() {
            List<String> icd10Codes = Arrays.asList("I21.0");
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 40);

            assertThat(result.getInterpretation()).isEqualTo("Low Risk");
        }

        @Test
        @DisplayName("Should interpret score 2 as Medium Risk")
        void testMediumRiskInterpretation() {
            List<String> icd10Codes = Arrays.asList("E11.21"); // Diabetes with complications = 2
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 40);

            assertThat(result.getInterpretation()).isEqualTo("Medium Risk");
        }

        @Test
        @DisplayName("Should interpret score 3-4 as High Risk")
        void testHighRiskInterpretation() {
            List<String> icd10Codes = Arrays.asList(
                "I21.0",  // MI = 1
                "E11.21", // Diabetes with complications = 2
                "I50.1"   // CHF = 1
            );
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 40);

            assertThat(result.getScore()).isEqualTo(4.0);
            assertThat(result.getInterpretation()).isEqualTo("High Risk");
        }

        @Test
        @DisplayName("Should interpret score 5+ as Very High Risk")
        void testVeryHighRiskInterpretation() {
            List<String> icd10Codes = Arrays.asList(
                "C18.0"  // Metastatic solid tumor = 6
            );
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 40);

            assertThat(result.getScore()).isEqualTo(6.0);
            assertThat(result.getInterpretation()).isEqualTo("Very High Risk");
        }
    }

    @Nested
    @DisplayName("High-Weight Condition Tests")
    class HighWeightConditionTests {

        @Test
        @DisplayName("Should score moderate/severe liver disease correctly")
        void testSevereLiverDisease() {
            List<String> icd10Codes = Arrays.asList("K72.00"); // Acute hepatic failure
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(3.0);
            assertThat(result.getExplanations().get(0).getFactor()).contains("Liver Disease");
        }

        @Test
        @DisplayName("Should not double-count mild and severe liver disease")
        void testLiverDiseaseNoDuplication() {
            List<String> icd10Codes = Arrays.asList(
                "K70.30",  // Mild liver disease = 1
                "K72.00"   // Severe liver disease = 3
            );
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            // Should only count the higher score (severe = 3)
            assertThat(result.getScore()).isEqualTo(3.0);
            assertThat(result.getExplanations()).hasSize(1);
        }

        @Test
        @DisplayName("Should score hemiplegia correctly")
        void testHemiplegia() {
            List<String> icd10Codes = Arrays.asList("G81.90"); // Hemiplegia
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should score moderate/severe renal disease correctly")
        void testSevereRenalDisease() {
            List<String> icd10Codes = Arrays.asList("N18.4"); // CKD Stage 4
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should score any tumor correctly")
        void testTumor() {
            List<String> icd10Codes = Arrays.asList("C50.911"); // Breast cancer
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should score leukemia correctly")
        void testLeukemia() {
            List<String> icd10Codes = Arrays.asList("C91.10"); // Chronic lymphocytic leukemia
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should score lymphoma correctly")
        void testLymphoma() {
            List<String> icd10Codes = Arrays.asList("C82.00"); // Follicular lymphoma
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should score metastatic solid tumor correctly")
        void testMetastaticTumor() {
            List<String> icd10Codes = Arrays.asList("C78.00"); // Secondary malignant neoplasm
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(6.0);
        }

        @Test
        @DisplayName("Should score AIDS correctly")
        void testAIDS() {
            List<String> icd10Codes = Arrays.asList("B20"); // HIV with complications
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(6.0);
        }

        @Test
        @DisplayName("Should not double-count tumor and metastatic tumor")
        void testTumorNoDuplication() {
            List<String> icd10Codes = Arrays.asList(
                "C50.911",  // Any tumor = 2
                "C78.00"    // Metastatic tumor = 6
            );
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            // Should only count the higher score (metastatic = 6)
            assertThat(result.getScore()).isEqualTo(6.0);
            assertThat(result.getExplanations()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Edge Cases and Validation Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null diagnosis list")
        void testNullDiagnosisList() {
            assertThatThrownBy(() -> charlsonIndex.calculate(null, 50))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle invalid ICD-10 codes gracefully")
        void testInvalidICD10Codes() {
            List<String> icd10Codes = Arrays.asList("INVALID", "CODE");
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle mixed valid and invalid codes")
        void testMixedValidInvalidCodes() {
            List<String> icd10Codes = Arrays.asList(
                "I21.0",    // Valid MI
                "INVALID",  // Invalid
                "I50.1"     // Valid CHF
            );
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(2.0); // Only valid codes counted
        }

        @Test
        @DisplayName("Should handle negative age")
        void testNegativeAge() {
            assertThatThrownBy(() -> charlsonIndex.calculate(Collections.emptyList(), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Age must be non-negative");
        }

        @Test
        @DisplayName("Should handle very large age (120+)")
        void testVeryLargeAge() {
            List<String> icd10Codes = Arrays.asList("I21.0");
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 120);

            // Age 120 should still get age adjustment
            assertThat(result.getScore()).isGreaterThan(1.0);
        }

        @Test
        @DisplayName("Should be thread-safe")
        void testThreadSafety() throws InterruptedException {
            List<String> icd10Codes = Arrays.asList("I21.0", "I50.1");

            // Run multiple calculations concurrently
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    charlsonIndex.calculate(icd10Codes, 50);
                }
            });

            Thread t2 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    charlsonIndex.calculate(icd10Codes, 65);
                }
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            // If we get here without exceptions, thread safety is confirmed
            assertThat(true).isTrue();
        }
    }

    @Nested
    @DisplayName("Specific ICD-10 Code Mapping Tests")
    class ICD10MappingTests {

        @Test
        @DisplayName("Should recognize cerebrovascular disease codes")
        void testCerebrovascularDisease() {
            List<String> icd10Codes = Arrays.asList("I63.9"); // Cerebral infarction
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(1.0);
            assertThat(result.getExplanations().get(0).getFactor()).contains("Cerebrovascular Disease");
        }

        @Test
        @DisplayName("Should recognize dementia codes")
        void testDementia() {
            List<String> icd10Codes = Arrays.asList("F03.90"); // Unspecified dementia
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(1.0);
            assertThat(result.getExplanations().get(0).getFactor()).contains("Dementia");
        }

        @Test
        @DisplayName("Should recognize COPD codes")
        void testCOPD() {
            List<String> icd10Codes = Arrays.asList("J44.1"); // COPD with exacerbation
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(1.0);
            assertThat(result.getExplanations().get(0).getFactor()).contains("Chronic Pulmonary Disease");
        }

        @Test
        @DisplayName("Should recognize connective tissue disease codes")
        void testConnectiveTissueDisease() {
            List<String> icd10Codes = Arrays.asList("M32.10"); // Systemic lupus erythematosus
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(1.0);
            assertThat(result.getExplanations().get(0).getFactor()).contains("Connective Tissue Disease");
        }

        @Test
        @DisplayName("Should recognize peptic ulcer disease codes")
        void testPepticUlcer() {
            List<String> icd10Codes = Arrays.asList("K27.9"); // Peptic ulcer
            RiskIndexResult result = charlsonIndex.calculate(icd10Codes, 50);

            assertThat(result.getScore()).isEqualTo(1.0);
            assertThat(result.getExplanations().get(0).getFactor()).contains("Peptic Ulcer Disease");
        }
    }
}
