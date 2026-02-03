package com.healthdata.agentvalidation.domain.entity;

import com.healthdata.agentvalidation.domain.enums.UserStoryType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for TestSuite entity.
 */
@Tag("unit")
class TestSuiteEntityTest {

    @Test
    void shouldCreateTestSuite_WithAllFields() {
        // Given
        TestSuite testSuite = new TestSuite();
        testSuite.setId(UUID.randomUUID());
        testSuite.setName("Clinician Care Gap Review");
        testSuite.setUserStoryType(UserStoryType.PATIENT_SUMMARY_REVIEW);
        testSuite.setTargetRole("CLINICIAN");
        testSuite.setAgentType("clinical-decision");
        testSuite.setPassThreshold(new BigDecimal("0.80"));
        testSuite.setDescription("Tests clinician workflows for patient care gap review");

        // Then
        assertThat(testSuite.getId()).isNotNull();
        assertThat(testSuite.getName()).isEqualTo("Clinician Care Gap Review");
        assertThat(testSuite.getUserStoryType()).isEqualTo(UserStoryType.PATIENT_SUMMARY_REVIEW);
        assertThat(testSuite.getTargetRole()).isEqualTo("CLINICIAN");
        assertThat(testSuite.getAgentType()).isEqualTo("clinical-decision");
        assertThat(testSuite.getPassThreshold()).isEqualByComparingTo(new BigDecimal("0.80"));
    }

    @Test
    void shouldUseBuilderPattern() {
        // Given & When
        TestSuite testSuite = TestSuite.builder()
                .name("Quality Officer HEDIS Analysis")
                .userStoryType(UserStoryType.HEDIS_MEASURE_EVALUATION)
                .targetRole("QUALITY_OFFICER")
                .agentType("report-generator")
                .passThreshold(new BigDecimal("0.75"))
                .description("Tests quality officer workflows")
                .build();

        // Then
        assertThat(testSuite.getName()).isEqualTo("Quality Officer HEDIS Analysis");
        assertThat(testSuite.getUserStoryType()).isEqualTo(UserStoryType.HEDIS_MEASURE_EVALUATION);
        assertThat(testSuite.getTargetRole()).isEqualTo("QUALITY_OFFICER");
    }

    @Test
    void shouldSetDefaultValues() {
        // Given & When
        TestSuite testSuite = new TestSuite();

        // Then - check that test cases list is initialized
        assertThat(testSuite.getTestCases()).isNotNull();
    }
}
