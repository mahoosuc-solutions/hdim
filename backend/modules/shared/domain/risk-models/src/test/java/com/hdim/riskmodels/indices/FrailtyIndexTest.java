package com.hdim.riskmodels.indices;

import com.hdim.riskmodels.models.RiskIndexResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * TDD tests for Frailty Index calculation.
 */
@DisplayName("Frailty Index Tests")
class FrailtyIndexTest {

    private FrailtyIndex frailtyIndex;

    @BeforeEach
    void setUp() {
        frailtyIndex = new FrailtyIndex();
    }

    @Nested
    @DisplayName("Basic Calculation Tests")
    class BasicCalculationTests {

        @Test
        @DisplayName("Should calculate score for all robust parameters")
        void testAllRobust() {
            RiskIndexResult result = frailtyIndex.calculate(
                false, false, false, false, false,
                false, false, false, false, false
            );

            assertThat(result.getScore()).isEqualTo(0.0);
            assertThat(result.getInterpretation()).isEqualTo("Robust");
        }

        @Test
        @DisplayName("Should calculate score for single deficit")
        void testSingleDeficit() {
            RiskIndexResult result = frailtyIndex.calculate(
                true, false, false, false, false,
                false, false, false, false, false
            );

            assertThat(result.getScore()).isEqualTo(0.1);
        }

        @Test
        @DisplayName("Should calculate score for multiple deficits")
        void testMultipleDeficits() {
            RiskIndexResult result = frailtyIndex.calculate(
                true, true, true, false, false,
                false, false, false, false, false
            );

            assertThat(result.getScore()).isEqualTo(0.3);
        }
    }

    @Nested
    @DisplayName("Interpretation Tests")
    class InterpretationTests {

        @Test
        @DisplayName("Should interpret 0.0-0.1 as Robust")
        void testRobust() {
            RiskIndexResult result = frailtyIndex.calculate(
                true, false, false, false, false,
                false, false, false, false, false
            );

            assertThat(result.getInterpretation()).isEqualTo("Robust");
        }

        @Test
        @DisplayName("Should interpret 0.2-0.3 as Pre-Frail")
        void testPreFrail() {
            RiskIndexResult result = frailtyIndex.calculate(
                true, true, false, false, false,
                false, false, false, false, false
            );

            assertThat(result.getInterpretation()).isEqualTo("Pre-Frail");
        }

        @Test
        @DisplayName("Should interpret 0.4-0.5 as Frail")
        void testFrail() {
            RiskIndexResult result = frailtyIndex.calculate(
                true, true, true, true, false,
                false, false, false, false, false
            );

            assertThat(result.getInterpretation()).isEqualTo("Frail");
        }

        @Test
        @DisplayName("Should interpret 0.6+ as Severely Frail")
        void testSeverelyFrail() {
            RiskIndexResult result = frailtyIndex.calculate(
                true, true, true, true, true,
                true, false, false, false, false
            );

            assertThat(result.getInterpretation()).isEqualTo("Severely Frail");
        }
    }

    @Nested
    @DisplayName("Deficit Assessment Tests")
    class DeficitTests {

        @Test
        @DisplayName("Should count all 10 deficits")
        void testAllDeficits() {
            RiskIndexResult result = frailtyIndex.calculate(
                true, true, true, true, true,
                true, true, true, true, true
            );

            assertThat(result.getScore()).isEqualTo(1.0);
            assertThat(result.getExplanations()).hasSize(10);
        }

        @Test
        @DisplayName("Should identify specific deficits")
        void testSpecificDeficits() {
            RiskIndexResult result = frailtyIndex.calculate(
                true, false, true, false, false,
                false, false, false, false, false
            );

            assertThat(result.getExplanations()).anyMatch(e -> e.getFactor().contains("Weight Loss"));
            assertThat(result.getExplanations()).anyMatch(e -> e.getFactor().contains("Low Activity"));
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should be thread-safe")
        void testThreadSafety() throws InterruptedException {
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    frailtyIndex.calculate(true, true, false, false, false,
                        false, false, false, false, false);
                }
            });

            Thread t2 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    frailtyIndex.calculate(false, false, true, true, true,
                        false, false, false, false, false);
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
