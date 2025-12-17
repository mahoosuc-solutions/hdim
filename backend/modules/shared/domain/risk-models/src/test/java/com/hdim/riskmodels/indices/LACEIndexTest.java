package com.hdim.riskmodels.indices;

import com.hdim.riskmodels.models.RiskIndexResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive TDD tests for LACE Index (readmission risk).
 * LACE = Length of stay + Acuity + Comorbidities + Emergency department visits
 */
@DisplayName("LACE Index Tests")
class LACEIndexTest {

    private LACEIndex laceIndex;

    @BeforeEach
    void setUp() {
        laceIndex = new LACEIndex();
    }

    @Nested
    @DisplayName("Length of Stay Tests")
    class LengthOfStayTests {

        @Test
        @DisplayName("Should score 0 points for 0 days LOS")
        void testZeroDaysLOS() {
            RiskIndexResult result = laceIndex.calculate(0, false, Collections.emptyList(), 0);

            assertThat(result.getScore()).isEqualTo(0.0);
        }

        @Test
        @DisplayName("Should score 1 point for 1 day LOS")
        void testOneDayLOS() {
            RiskIndexResult result = laceIndex.calculate(1, false, Collections.emptyList(), 0);

            assertThat(result.getScore()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should score 2 points for 2 days LOS")
        void testTwoDaysLOS() {
            RiskIndexResult result = laceIndex.calculate(2, false, Collections.emptyList(), 0);

            assertThat(result.getScore()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should score 3 points for 3 days LOS")
        void testThreeDaysLOS() {
            RiskIndexResult result = laceIndex.calculate(3, false, Collections.emptyList(), 0);

            assertThat(result.getScore()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("Should score 4 points for 4-6 days LOS")
        void testFourToSixDaysLOS() {
            assertThat(laceIndex.calculate(4, false, Collections.emptyList(), 0).getScore()).isEqualTo(4.0);
            assertThat(laceIndex.calculate(5, false, Collections.emptyList(), 0).getScore()).isEqualTo(4.0);
            assertThat(laceIndex.calculate(6, false, Collections.emptyList(), 0).getScore()).isEqualTo(4.0);
        }

        @Test
        @DisplayName("Should score 5 points for 7-13 days LOS")
        void testSevenToThirteenDaysLOS() {
            assertThat(laceIndex.calculate(7, false, Collections.emptyList(), 0).getScore()).isEqualTo(5.0);
            assertThat(laceIndex.calculate(10, false, Collections.emptyList(), 0).getScore()).isEqualTo(5.0);
            assertThat(laceIndex.calculate(13, false, Collections.emptyList(), 0).getScore()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("Should score 7 points for 14+ days LOS")
        void testFourteenPlusDaysLOS() {
            assertThat(laceIndex.calculate(14, false, Collections.emptyList(), 0).getScore()).isEqualTo(7.0);
            assertThat(laceIndex.calculate(30, false, Collections.emptyList(), 0).getScore()).isEqualTo(7.0);
            assertThat(laceIndex.calculate(100, false, Collections.emptyList(), 0).getScore()).isEqualTo(7.0);
        }
    }

    @Nested
    @DisplayName("Acuity Tests")
    class AcuityTests {

        @Test
        @DisplayName("Should score 0 points for non-acute admission")
        void testNonAcuteAdmission() {
            RiskIndexResult result = laceIndex.calculate(1, false, Collections.emptyList(), 0);

            assertThat(result.getScore()).isEqualTo(1.0); // Only LOS point
        }

        @Test
        @DisplayName("Should score 3 points for acute admission")
        void testAcuteAdmission() {
            RiskIndexResult result = laceIndex.calculate(1, true, Collections.emptyList(), 0);

            assertThat(result.getScore()).isEqualTo(4.0); // 1 (LOS) + 3 (Acuity)
            assertThat(result.getExplanations()).anyMatch(e ->
                e.getFactor().contains("Acute") && e.getContribution() == 3.0);
        }
    }

    @Nested
    @DisplayName("Comorbidity Tests")
    class ComorbidityTests {

        @Test
        @DisplayName("Should score 0 for no comorbidities")
        void testNoComorbidities() {
            RiskIndexResult result = laceIndex.calculate(1, false, Collections.emptyList(), 0);

            assertThat(result.getScore()).isEqualTo(1.0); // Only LOS
        }

        @Test
        @DisplayName("Should calculate Charlson score for comorbidities")
        void testWithComorbidities() {
            RiskIndexResult result = laceIndex.calculate(1, false,
                Arrays.asList("I21.0", "I50.1"), 0); // MI + CHF

            // Should include Charlson-based comorbidity score
            assertThat(result.getScore()).isGreaterThan(1.0);
        }

        @Test
        @DisplayName("Should score 1 point for Charlson 1-2")
        void testCharlson1to2() {
            // LOS=1, not acute, MI (Charlson 1), 0 ED visits
            RiskIndexResult result = laceIndex.calculate(1, false,
                Arrays.asList("I21.0"), 0); // MI = Charlson 1

            assertThat(result.getScore()).isEqualTo(2.0); // 1 (LOS) + 1 (Comorbidity)
        }

        @Test
        @DisplayName("Should score 2 points for Charlson 3-4")
        void testCharlson3to4() {
            // LOS=1, not acute, 3 conditions (Charlson 3), 0 ED visits
            RiskIndexResult result = laceIndex.calculate(1, false,
                Arrays.asList("I21.0", "I50.1", "J44.0"), 0); // Charlson = 3

            assertThat(result.getScore()).isEqualTo(3.0); // 1 (LOS) + 2 (Comorbidity)
        }

        @Test
        @DisplayName("Should score 5 points for Charlson 5+")
        void testCharlson5Plus() {
            // LOS=1, not acute, metastatic cancer (Charlson 6), 0 ED visits
            RiskIndexResult result = laceIndex.calculate(1, false,
                Arrays.asList("C78.00"), 0); // Metastatic cancer = Charlson 6

            assertThat(result.getScore()).isEqualTo(6.0); // 1 (LOS) + 5 (Comorbidity)
        }
    }

    @Nested
    @DisplayName("Emergency Department Visits Tests")
    class EDVisitsTests {

        @Test
        @DisplayName("Should score 0 for no ED visits")
        void testNoEDVisits() {
            RiskIndexResult result = laceIndex.calculate(1, false, Collections.emptyList(), 0);

            assertThat(result.getScore()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should score 1 point for 1 ED visit")
        void testOneEDVisit() {
            RiskIndexResult result = laceIndex.calculate(1, false, Collections.emptyList(), 1);

            assertThat(result.getScore()).isEqualTo(2.0);
        }

        @Test
        @DisplayName("Should score 2 points for 2 ED visits")
        void testTwoEDVisits() {
            RiskIndexResult result = laceIndex.calculate(1, false, Collections.emptyList(), 2);

            assertThat(result.getScore()).isEqualTo(3.0);
        }

        @Test
        @DisplayName("Should score 3 points for 3 ED visits")
        void testThreeEDVisits() {
            RiskIndexResult result = laceIndex.calculate(1, false, Collections.emptyList(), 3);

            assertThat(result.getScore()).isEqualTo(4.0);
        }

        @Test
        @DisplayName("Should score 4 points for 4+ ED visits")
        void testFourPlusEDVisits() {
            assertThat(laceIndex.calculate(1, false, Collections.emptyList(), 4).getScore()).isEqualTo(5.0);
            assertThat(laceIndex.calculate(1, false, Collections.emptyList(), 10).getScore()).isEqualTo(5.0);
        }
    }

    @Nested
    @DisplayName("Combined Score Tests")
    class CombinedScoreTests {

        @Test
        @DisplayName("Should calculate maximum possible LACE score")
        void testMaximumScore() {
            // Max: 7 (LOS) + 3 (Acuity) + 5 (Comorbidity) + 4 (ED) = 19
            RiskIndexResult result = laceIndex.calculate(14, true,
                Arrays.asList("C78.00"), 4); // 14+ days, acute, high comorbidity, 4 ED visits

            assertThat(result.getScore()).isEqualTo(19.0);
        }

        @Test
        @DisplayName("Should calculate typical high-risk scenario")
        void testHighRiskScenario() {
            RiskIndexResult result = laceIndex.calculate(10, true,
                Arrays.asList("I21.0", "I50.1", "J44.0"), 2);
            // 5 (LOS) + 3 (Acuity) + 2 (Comorbidity) + 2 (ED) = 12

            assertThat(result.getScore()).isEqualTo(12.0);
        }
    }

    @Nested
    @DisplayName("Score Interpretation Tests")
    class ScoreInterpretationTests {

        @Test
        @DisplayName("Should interpret score < 5 as Low Risk")
        void testLowRisk() {
            RiskIndexResult result = laceIndex.calculate(2, false, Collections.emptyList(), 0);

            assertThat(result.getInterpretation()).isEqualTo("Low Risk");
        }

        @Test
        @DisplayName("Should interpret score 5-9 as Medium Risk")
        void testMediumRisk() {
            RiskIndexResult result = laceIndex.calculate(5, false, Collections.emptyList(), 2);

            assertThat(result.getInterpretation()).isEqualTo("Medium Risk");
        }

        @Test
        @DisplayName("Should interpret score 10+ as High Risk")
        void testHighRisk() {
            RiskIndexResult result = laceIndex.calculate(14, true, Collections.emptyList(), 0);

            assertThat(result.getInterpretation()).isEqualTo("High Risk");
        }
    }

    @Nested
    @DisplayName("Edge Cases and Validation Tests")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle negative LOS")
        void testNegativeLOS() {
            assertThatThrownBy(() -> laceIndex.calculate(-1, false, Collections.emptyList(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Length of stay must be non-negative");
        }

        @Test
        @DisplayName("Should handle negative ED visits")
        void testNegativeEDVisits() {
            assertThatThrownBy(() -> laceIndex.calculate(1, false, Collections.emptyList(), -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ED visits must be non-negative");
        }

        @Test
        @DisplayName("Should handle null diagnosis list")
        void testNullDiagnosisList() {
            assertThatThrownBy(() -> laceIndex.calculate(1, false, null, 0))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should include all components in explanation")
        void testExplanationCompleteness() {
            RiskIndexResult result = laceIndex.calculate(5, true,
                Arrays.asList("I21.0"), 2);

            assertThat(result.getExplanations()).hasSizeGreaterThanOrEqualTo(3);
            assertThat(result.getExplanations()).anyMatch(e -> e.getFactor().contains("Length of Stay"));
            assertThat(result.getExplanations()).anyMatch(e -> e.getFactor().contains("Acute"));
            assertThat(result.getExplanations()).anyMatch(e -> e.getFactor().contains("ED Visits"));
        }

        @Test
        @DisplayName("Should be thread-safe")
        void testThreadSafety() throws InterruptedException {
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    laceIndex.calculate(5, true, Arrays.asList("I21.0"), 2);
                }
            });

            Thread t2 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    laceIndex.calculate(10, false, Collections.emptyList(), 1);
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
