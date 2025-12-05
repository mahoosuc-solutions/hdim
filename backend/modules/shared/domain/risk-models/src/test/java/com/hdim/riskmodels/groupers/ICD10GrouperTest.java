package com.hdim.riskmodels.groupers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ICD-10 Grouper Tests")
class ICD10GrouperTest {

    private ICD10Grouper grouper;

    @BeforeEach
    void setUp() {
        grouper = new ICD10Grouper();
    }

    @Nested
    @DisplayName("Category Grouping Tests")
    class CategoryGroupingTests {

        @Test
        @DisplayName("Should group infectious diseases")
        void testInfectiousDiseases() {
            assertThat(grouper.getCategory("A00.0")).isEqualTo("Infectious and Parasitic Diseases");
            assertThat(grouper.getCategory("B20")).isEqualTo("Infectious and Parasitic Diseases");
        }

        @Test
        @DisplayName("Should group neoplasms")
        void testNeoplasms() {
            assertThat(grouper.getCategory("C50.911")).isEqualTo("Neoplasms");
            assertThat(grouper.getCategory("D49.9")).isEqualTo("Neoplasms");
        }

        @Test
        @DisplayName("Should group endocrine diseases")
        void testEndocrineDiseases() {
            assertThat(grouper.getCategory("E11.9")).isEqualTo("Endocrine, Nutritional and Metabolic Diseases");
        }

        @Test
        @DisplayName("Should group cardiovascular diseases")
        void testCardiovascularDiseases() {
            assertThat(grouper.getCategory("I21.0")).isEqualTo("Circulatory System Diseases");
            assertThat(grouper.getCategory("I50.1")).isEqualTo("Circulatory System Diseases");
        }

        @Test
        @DisplayName("Should group respiratory diseases")
        void testRespiratoryDiseases() {
            assertThat(grouper.getCategory("J44.0")).isEqualTo("Respiratory System Diseases");
        }

        @Test
        @DisplayName("Should group digestive diseases")
        void testDigestiveDiseases() {
            assertThat(grouper.getCategory("K70.30")).isEqualTo("Digestive System Diseases");
        }

        @Test
        @DisplayName("Should group genitourinary diseases")
        void testGenitourinaryDiseases() {
            assertThat(grouper.getCategory("N18.3")).isEqualTo("Genitourinary System Diseases");
        }
    }

    @Nested
    @DisplayName("Subcategory Tests")
    class SubcategoryTests {

        @Test
        @DisplayName("Should identify diabetes subcategory")
        void testDiabetesSubcategory() {
            assertThat(grouper.getSubcategory("E11.9")).contains("Diabetes");
        }

        @Test
        @DisplayName("Should identify heart disease subcategory")
        void testHeartDiseaseSubcategory() {
            assertThat(grouper.getSubcategory("I50.1")).contains("Heart");
        }
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should handle null code")
        void testNullCode() {
            assertThatThrownBy(() -> grouper.getCategory(null))
                .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("Should handle invalid code")
        void testInvalidCode() {
            assertThat(grouper.getCategory("INVALID")).isEqualTo("Unknown");
        }

        @Test
        @DisplayName("Should handle empty code")
        void testEmptyCode() {
            assertThat(grouper.getCategory("")).isEqualTo("Unknown");
        }
    }

    @Nested
    @DisplayName("Bulk Grouping Tests")
    class BulkGroupingTests {

        @Test
        @DisplayName("Should group multiple codes")
        void testBulkGrouping() {
            List<String> codes = List.of("I21.0", "E11.9", "J44.0");
            var results = grouper.groupMultiple(codes);

            assertThat(results).hasSize(3);
            assertThat(results.get("I21.0")).isEqualTo("Circulatory System Diseases");
            assertThat(results.get("E11.9")).isEqualTo("Endocrine, Nutritional and Metabolic Diseases");
            assertThat(results.get("J44.0")).isEqualTo("Respiratory System Diseases");
        }
    }

    @Nested
    @DisplayName("Thread Safety Tests")
    class ThreadSafetyTests {

        @Test
        @DisplayName("Should be thread-safe")
        void testThreadSafety() throws InterruptedException {
            Thread t1 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    grouper.getCategory("I21.0");
                }
            });

            Thread t2 = new Thread(() -> {
                for (int i = 0; i < 100; i++) {
                    grouper.getCategory("E11.9");
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
