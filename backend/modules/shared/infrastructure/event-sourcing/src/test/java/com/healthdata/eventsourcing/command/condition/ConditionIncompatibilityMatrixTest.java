package com.healthdata.eventsourcing.command.condition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ConditionIncompatibilityMatrix Tests")
class ConditionIncompatibilityMatrixTest {
    private ConditionIncompatibilityMatrix matrix;

    @BeforeEach
    void setUp() {
        matrix = new ConditionIncompatibilityMatrix();
    }

    @Test
    @DisplayName("Should detect E10 (Type 1 DM) incompatible with E11 (Type 2 DM)")
    void shouldDetectDiabetesTypeIncompatibility() {
        assertThat(matrix.areIncompatible("E10.9", "E11.9")).isTrue();
        assertThat(matrix.areIncompatible("E11.9", "E10.9")).isTrue();
    }

    @Test
    @DisplayName("Should detect E10.2 (Type 1 with neuropathy) incompatible with E11.21 (Type 2 with neuropathy)")
    void shouldDetectComplicatedDiabetesIncompatibility() {
        assertThat(matrix.areIncompatible("E10.22", "E11.21")).isTrue();
    }

    @Test
    @DisplayName("Should detect E10 incompatible with O24.4 (Gestational DM)")
    void shouldDetectType1GestationalIncompatibility() {
        assertThat(matrix.areIncompatible("E10.9", "O24.41")).isTrue();
    }

    @Test
    @DisplayName("Should detect E11 incompatible with O24.4 (Gestational DM)")
    void shouldDetectType2GestationalIncompatibility() {
        assertThat(matrix.areIncompatible("E11.9", "O24.41")).isTrue();
    }

    @Test
    @DisplayName("Should detect E10.9 (uncomplicated) incompatible with E10.2 (complicated)")
    void shouldDetectComplicationLevelIncompatibility() {
        assertThat(matrix.areIncompatible("E10.9", "E10.22")).isTrue();
    }

    @Test
    @DisplayName("Should allow E10.21 (Type 1 with renal) with E10.22 (Type 1 with neuropathy)")
    void shouldAllowMultipleComplicationsOfSameType() {
        assertThat(matrix.areIncompatible("E10.21", "E10.22")).isFalse();
    }

    @Test
    @DisplayName("Should find incompatible codes in a list")
    void shouldFindIncompatibleCodes() {
        List<String> existing = List.of("E10.9", "I10");  // Type 1 DM and Hypertension
        List<String> incompatible = matrix.findIncompatibleCodes("E11.9", existing);  // Type 2 DM
        assertThat(incompatible).contains("E10.9");
    }

    @Test
    @DisplayName("Should return reason for incompatibility")
    void shouldReturnIncompatibilityReason() {
        String reason = matrix.getReasonForIncompatibility("E10.9", "E11.9");
        assertThat(reason).contains("mutually exclusive");
    }

    @Test
    @DisplayName("Should allow compatible conditions")
    void shouldAllowCompatibleConditions() {
        assertThat(matrix.areIncompatible("I10", "E11.9")).isFalse();  // Hypertension with Type 2 DM
        assertThat(matrix.areIncompatible("J44.0", "E10.9")).isFalse();  // COPD with Type 1 DM
    }

    @Test
    @DisplayName("Should detect all E10.x variants")
    void shouldDetectAllType1Variants() {
        assertThat(matrix.areIncompatible("E10.2", "E11.9")).isTrue();
        assertThat(matrix.areIncompatible("E10.3", "E11.9")).isTrue();
        assertThat(matrix.areIncompatible("E10.4", "E11.9")).isTrue();
        assertThat(matrix.areIncompatible("E10.5", "E11.9")).isTrue();
        assertThat(matrix.areIncompatible("E10.6", "E11.9")).isTrue();
        assertThat(matrix.areIncompatible("E10.7", "E11.9")).isTrue();
        assertThat(matrix.areIncompatible("E10.8", "E11.9")).isTrue();
        assertThat(matrix.areIncompatible("E10.9", "E11.9")).isTrue();
    }

    @Test
    @DisplayName("Should handle O24.4 variants for gestational DM")
    void shouldHandleGestationalDMVariants() {
        assertThat(matrix.areIncompatible("E10.9", "O24.410")).isTrue();
        assertThat(matrix.areIncompatible("E10.9", "O24.419")).isTrue();
    }
}
