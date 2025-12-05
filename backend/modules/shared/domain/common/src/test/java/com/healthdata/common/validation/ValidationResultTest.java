package com.healthdata.common.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ValidationResultTest {

    @Test
    @DisplayName("success without warnings should reuse singleton instance")
    void successWithoutWarnings_shouldReuseSingleton() {
        ValidationResult first = ValidationResult.success();
        ValidationResult second = ValidationResult.success(List.of());

        assertThat(first).isSameAs(second);
        assertThat(first.isValid()).isTrue();
        assertThat(first.getWarnings()).isEmpty();
    }

    @Test
    @DisplayName("failure should require at least one error")
    void failure_shouldRequireErrors() {
        assertThatThrownBy(() -> ValidationResult.failure(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one error");
    }

    @Test
    @DisplayName("merge should combine warnings and errors appropriately")
    void merge_shouldCombineMessages() {
        ValidationResult success = ValidationResult.success(List.of("be mindful"));
        ValidationResult failure = ValidationResult.failure(List.of("missing id"));

        ValidationResult merged = success.merge(failure);

        assertThat(merged.isValid()).isFalse();
        assertThat(merged.getErrors()).containsExactly("missing id");
        assertThat(merged.getWarnings()).containsExactly("be mindful");
    }
}
