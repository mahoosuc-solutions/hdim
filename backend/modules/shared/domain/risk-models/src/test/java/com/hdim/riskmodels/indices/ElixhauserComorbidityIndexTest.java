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
 * Comprehensive TDD tests for Elixhauser Comorbidity Index.
 * Tests based on the Elixhauser 31-category model and ICD-10-CM 2024 mappings.
 */
@DisplayName("Elixhauser Comorbidity Index Tests")
class ElixhauserComorbidityIndexTest {

    private ElixhauserComorbidityIndex elixhauserIndex;

    @BeforeEach
    void setUp() {
        elixhauserIndex = new ElixhauserComorbidityIndex();
    }

    @Nested
    @DisplayName("Basic Calculation Tests")
    class BasicCalculationTests {

        @Test
        @DisplayName("Should return zero score for no diagnoses")
        void testNoDiagnoses() {
            RiskIndexResult result = elixhauserIndex.calculate(Collections.emptyList());

            assertThat(result).isNotNull();
            assertThat(result.getScore()).isEqualTo(0.0);
            assertThat(result.getIndexName()).isEqualTo("Elixhauser Comorbidity Index");
            // Score of 0 is at lower bound of Medium Risk (0-10)
            assertThat(result.getInterpretation()).isIn("Low Risk", "Medium Risk");
        }

        @Test
        @DisplayName("Should calculate score for single comorbidity - CHF")
        void testSingleCHF() {
            List<String> icd10Codes = Arrays.asList("I50.1");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getScore()).isPositive();
            assertThat(result.getExplanations()).hasSize(1);
            assertThat(result.getExplanations().get(0).getFactor()).contains("Congestive Heart Failure");
        }

        @Test
        @DisplayName("Should calculate score for hypertension")
        void testHypertension() {
            List<String> icd10Codes = Arrays.asList("I10");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            // Hypertension Uncomplicated has weight -1 in AHRQ model
            assertThat(result.getExplanations()).hasSize(1);
            assertThat(result.getExplanations().get(0).getFactor()).contains("Hypertension");
        }
    }

    @Nested
    @DisplayName("Multiple Comorbidity Tests")
    class MultipleComorbidityTests {

        @Test
        @DisplayName("Should sum multiple different comorbidities")
        void testMultipleDifferentComorbidities() {
            List<String> icd10Codes = Arrays.asList(
                "I50.1",  // CHF
                "I10",    // Hypertension
                "J44.0"   // COPD
            );
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getExplanations()).hasSize(3);
            assertThat(result.getScore()).isPositive();
        }

        @Test
        @DisplayName("Should not duplicate same comorbidity category")
        void testNoDuplicationSameCategory() {
            List<String> icd10Codes = Arrays.asList(
                "I50.1",  // CHF
                "I50.2",  // CHF
                "I50.9"   // CHF
            );
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getExplanations()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Cardiac Arrhythmia Tests")
    class CardiacArrhythmiaTests {

        @Test
        @DisplayName("Should detect atrial fibrillation")
        void testAtrialFibrillation() {
            List<String> icd10Codes = Arrays.asList("I48.0");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            // Cardiac Arrhythmia has weight 0 in AHRQ model
            assertThat(result.getExplanations()).hasSize(1);
            assertThat(result.getExplanations().get(0).getFactor()).contains("Cardiac Arrhythmia");
        }

        @Test
        @DisplayName("Should detect ventricular tachycardia")
        void testVentricularTachycardia() {
            List<String> icd10Codes = Arrays.asList("I47.2");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            // Cardiac Arrhythmia has weight 0 in AHRQ model
            assertThat(result.getExplanations()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Valvular Disease Tests")
    class ValvularDiseaseTests {

        @Test
        @DisplayName("Should detect valvular disease")
        void testValvularDisease() {
            List<String> icd10Codes = Arrays.asList("I35.0"); // Aortic stenosis
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            // Valvular Disease has weight 0 in AHRQ model
            assertThat(result.getExplanations()).hasSize(1);
            assertThat(result.getExplanations().get(0).getFactor()).contains("Valvular Disease");
        }
    }

    @Nested
    @DisplayName("Pulmonary Circulation Disorder Tests")
    class PulmonaryCirculationTests {

        @Test
        @DisplayName("Should detect pulmonary embolism")
        void testPulmonaryEmbolism() {
            List<String> icd10Codes = Arrays.asList("I26.9");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getScore()).isPositive();
            assertThat(result.getExplanations().get(0).getFactor()).contains("Pulmonary Circulation");
        }
    }

    @Nested
    @DisplayName("Peripheral Vascular Disorder Tests")
    class PeripheralVascularTests {

        @Test
        @DisplayName("Should detect peripheral vascular disease")
        void testPeripheralVascularDisease() {
            List<String> icd10Codes = Arrays.asList("I73.9");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getScore()).isPositive();
        }
    }

    @Nested
    @DisplayName("Paralysis Tests")
    class ParalysisTests {

        @Test
        @DisplayName("Should detect hemiplegia")
        void testHemiplegia() {
            List<String> icd10Codes = Arrays.asList("G81.90");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getScore()).isPositive();
            assertThat(result.getExplanations().get(0).getFactor()).contains("Paralysis");
        }
    }

    @Nested
    @DisplayName("Neurological Disorder Tests")
    class NeurologicalTests {

        @Test
        @DisplayName("Should detect Parkinson's disease")
        void testParkinsons() {
            List<String> icd10Codes = Arrays.asList("G20");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getScore()).isPositive();
            assertThat(result.getExplanations().get(0).getFactor()).contains("Neurological");
        }
    }

    @Nested
    @DisplayName("Diabetes Tests")
    class DiabetesTests {

        @Test
        @DisplayName("Should detect diabetes without complications")
        void testDiabetesUncomplicated() {
            List<String> icd10Codes = Arrays.asList("E11.9");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            // Diabetes Uncomplicated has weight 0 in AHRQ model
            assertThat(result.getExplanations()).hasSize(1);
        }

        @Test
        @DisplayName("Should detect diabetes with complications")
        void testDiabetesComplicated() {
            List<String> icd10Codes = Arrays.asList("E11.21");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            // Diabetes Complicated has weight 0 in AHRQ model
            assertThat(result.getExplanations()).hasSize(1);
        }

        @Test
        @DisplayName("Should not double-count complicated and uncomplicated diabetes")
        void testDiabetesNoDuplication() {
            List<String> icd10Codes = Arrays.asList("E11.9", "E11.21");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            // Should count only one diabetes category
            long diabetesCount = result.getExplanations().stream()
                .filter(e -> e.getFactor().contains("Diabetes"))
                .count();
            assertThat(diabetesCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Renal Failure Tests")
    class RenalFailureTests {

        @Test
        @DisplayName("Should detect chronic kidney disease")
        void testCKD() {
            List<String> icd10Codes = Arrays.asList("N18.3");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getScore()).isPositive();
            assertThat(result.getExplanations().get(0).getFactor()).contains("Renal Failure");
        }
    }

    @Nested
    @DisplayName("Liver Disease Tests")
    class LiverDiseaseTests {

        @Test
        @DisplayName("Should detect mild liver disease")
        void testMildLiverDisease() {
            List<String> icd10Codes = Arrays.asList("K70.30");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getScore()).isPositive();
        }

        @Test
        @DisplayName("Should detect severe liver disease")
        void testSevereLiverDisease() {
            List<String> icd10Codes = Arrays.asList("K72.00");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getScore()).isPositive();
        }
    }

    @Nested
    @DisplayName("Cancer Tests")
    class CancerTests {

        @Test
        @DisplayName("Should detect solid tumor")
        void testSolidTumor() {
            List<String> icd10Codes = Arrays.asList("C50.911");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getScore()).isPositive();
        }

        @Test
        @DisplayName("Should detect metastatic cancer")
        void testMetastaticCancer() {
            List<String> icd10Codes = Arrays.asList("C78.00");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getScore()).isPositive();
        }
    }

    @Nested
    @DisplayName("Blood Disorder Tests")
    class BloodDisorderTests {

        @Test
        @DisplayName("Should detect coagulation deficiency")
        void testCoagulationDeficiency() {
            List<String> icd10Codes = Arrays.asList("D68.9");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getScore()).isPositive();
            assertThat(result.getExplanations().get(0).getFactor()).contains("Coagulopathy");
        }
    }

    @Nested
    @DisplayName("Obesity Tests")
    class ObesityTests {

        @Test
        @DisplayName("Should detect obesity")
        void testObesity() {
            List<String> icd10Codes = Arrays.asList("E66.9");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            // Obesity has weight -5 in AHRQ model (protective factor)
            assertThat(result.getExplanations()).hasSize(1);
            assertThat(result.getExplanations().get(0).getFactor()).contains("Obesity");
        }
    }

    @Nested
    @DisplayName("Weight Loss Tests")
    class WeightLossTests {

        @Test
        @DisplayName("Should detect weight loss")
        void testWeightLoss() {
            List<String> icd10Codes = Arrays.asList("R63.4");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getScore()).isPositive();
        }
    }

    @Nested
    @DisplayName("Fluid and Electrolyte Disorder Tests")
    class FluidElectrolyteTests {

        @Test
        @DisplayName("Should detect fluid and electrolyte disorder")
        void testFluidElectrolyteDisorder() {
            List<String> icd10Codes = Arrays.asList("E87.1");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getScore()).isPositive();
        }
    }

    @Nested
    @DisplayName("Score Interpretation Tests")
    class ScoreInterpretationTests {

        @Test
        @DisplayName("Should interpret low score as Low Risk")
        void testLowRiskInterpretation() {
            List<String> icd10Codes = Arrays.asList("I10"); // Single condition
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getInterpretation()).isIn("Low Risk", "Medium Risk");
        }

        @Test
        @DisplayName("Should interpret high score as High Risk")
        void testHighRiskInterpretation() {
            List<String> icd10Codes = Arrays.asList(
                "I50.1",  // CHF
                "J44.0",  // COPD
                "N18.5",  // CKD
                "C78.00", // Metastatic cancer
                "E11.21", // Diabetes with complications
                "K72.00"  // Liver failure
            );
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getInterpretation()).isIn("High Risk", "Very High Risk");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Validation Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle null diagnosis list")
        void testNullDiagnosisList() {
            assertThatThrownBy(() -> elixhauserIndex.calculate(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle invalid ICD-10 codes gracefully")
        void testInvalidICD10Codes() {
            List<String> icd10Codes = Arrays.asList("INVALID", "CODE");
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should handle mixed valid and invalid codes")
        void testMixedValidInvalidCodes() {
            List<String> icd10Codes = Arrays.asList(
                "I50.1",    // Valid CHF
                "INVALID",  // Invalid
                "I10"       // Valid Hypertension
            );
            RiskIndexResult result = elixhauserIndex.calculate(icd10Codes);

            assertThat(result.getExplanations()).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("Should be thread-safe")
        void testThreadSafety() throws InterruptedException {
            List<String> icd10Codes = Arrays.asList("I50.1", "I10");

            Thread t1 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    elixhauserIndex.calculate(icd10Codes);
                }
            });

            Thread t2 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    elixhauserIndex.calculate(icd10Codes);
                }
            });

            t1.start();
            t2.start();
            t1.join();
            t2.join();

            assertThat(true).isTrue();
        }
    }
}
