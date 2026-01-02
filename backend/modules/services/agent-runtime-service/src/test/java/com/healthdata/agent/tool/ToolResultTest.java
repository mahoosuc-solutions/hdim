package com.healthdata.agent.tool;

import com.healthdata.agent.tool.Tool.ToolResult;
import com.healthdata.agent.tool.Tool.ValidationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tool Result and Validation Tests")
class ToolResultTest {

    @Nested
    @DisplayName("ToolResult Tests")
    class ToolResultTests {

        @Test
        @DisplayName("should create success result with content")
        void successWithContent() {
            ToolResult result = ToolResult.success("Operation completed");

            assertThat(result.success()).isTrue();
            assertThat(result.content()).isEqualTo("Operation completed");
            assertThat(result.data()).isNull();
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("should create success result with content and data")
        void successWithContentAndData() {
            Map<String, Object> data = Map.of("patientId", "123", "count", 5);
            ToolResult result = ToolResult.success("Found 5 records", data);

            assertThat(result.success()).isTrue();
            assertThat(result.content()).isEqualTo("Found 5 records");
            assertThat(result.data()).isEqualTo(data);
            assertThat(result.errorMessage()).isNull();
        }

        @Test
        @DisplayName("should create error result")
        void errorResult() {
            ToolResult result = ToolResult.error("Connection timeout");

            assertThat(result.success()).isFalse();
            assertThat(result.content()).isNull();
            assertThat(result.data()).isNull();
            assertThat(result.errorMessage()).isEqualTo("Connection timeout");
        }

        @Test
        @DisplayName("should format success result for LLM")
        void successToToolResultContent() {
            ToolResult result = ToolResult.success("Patient data retrieved");

            assertThat(result.toToolResultContent()).isEqualTo("Patient data retrieved");
        }

        @Test
        @DisplayName("should format null content success result for LLM")
        void nullContentSuccessToToolResultContent() {
            ToolResult result = ToolResult.success(null);

            assertThat(result.toToolResultContent()).isEqualTo("Operation completed successfully.");
        }

        @Test
        @DisplayName("should format error result for LLM")
        void errorToToolResultContent() {
            ToolResult result = ToolResult.error("Database unavailable");

            assertThat(result.toToolResultContent()).isEqualTo("Error: Database unavailable");
        }
    }

    @Nested
    @DisplayName("ValidationResult Tests")
    class ValidationResultTests {

        @Test
        @DisplayName("should create valid result")
        void validResult() {
            ValidationResult result = ValidationResult.valid();

            assertThat(result.isValid()).isTrue();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("should create invalid result with varargs errors")
        void invalidWithVarargs() {
            ValidationResult result = ValidationResult.invalid(
                "Missing required field: patientId",
                "Invalid format for date field"
            );

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).containsExactly(
                "Missing required field: patientId",
                "Invalid format for date field"
            );
        }

        @Test
        @DisplayName("should create invalid result with list errors")
        void invalidWithList() {
            List<String> errors = List.of("Error 1", "Error 2", "Error 3");
            ValidationResult result = ValidationResult.invalid(errors);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).hasSize(3);
            assertThat(result.errors()).containsExactlyElementsOf(errors);
        }

        @Test
        @DisplayName("should create invalid result with single error")
        void invalidWithSingleError() {
            ValidationResult result = ValidationResult.invalid("Required parameter missing");

            assertThat(result.isValid()).isFalse();
            assertThat(result.errors()).containsExactly("Required parameter missing");
        }
    }
}
