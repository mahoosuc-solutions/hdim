package com.healthdata.auditquery.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("unit")
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    /**
     * Inner test controller that throws specific exceptions to exercise the GlobalExceptionHandler.
     */
    @RestController
    static class TestController {

        @PostMapping("/test/validate")
        public String validate(@Valid @RequestBody ValidationRequest request) {
            return "ok";
        }

        @GetMapping("/test/required-header")
        public String requiredHeader(@RequestHeader("X-Tenant-ID") String tenantId) {
            return "ok: " + tenantId;
        }

        @GetMapping("/test/illegal-argument")
        public String illegalArgument() {
            throw new IllegalArgumentException("Invalid parameter value");
        }

        @GetMapping("/test/unhandled")
        public String unhandled() {
            throw new RuntimeException("Something went terribly wrong");
        }

        static class ValidationRequest {
            @NotBlank(message = "Name is required")
            private String name;

            public String getName() { return name; }
            public void setName(String name) { this.name = name; }
        }
    }

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Nested
    @DisplayName("Validation Error Handling")
    class ValidationErrors {

        @Test
        @DisplayName("Should return 400 for validation errors")
        void shouldReturn400_ForValidationErrors() throws Exception {
            // Given - empty body triggers @NotBlank validation failure
            String invalidBody = "{\"name\": \"\"}";

            // When / Then
            mockMvc.perform(post("/test/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Validation Failed"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("name"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("Name is required"));
        }
    }

    @Nested
    @DisplayName("Missing Header Handling")
    class MissingHeader {

        @Test
        @DisplayName("Should return 400 for missing required header")
        void shouldReturn400_ForMissingRequiredHeader() throws Exception {
            // When / Then - no X-Tenant-ID header provided
            mockMvc.perform(get("/test/required-header"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Missing Header"))
                    .andExpect(jsonPath("$.message").value("Required header is missing"));
        }
    }

    @Nested
    @DisplayName("IllegalArgument Handling")
    class IllegalArgument {

        @Test
        @DisplayName("Should return 400 for illegal argument")
        void shouldReturn400_ForIllegalArgument() throws Exception {
            // When / Then
            mockMvc.perform(get("/test/illegal-argument"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.error").value("Invalid Argument"))
                    .andExpect(jsonPath("$.message").value("Invalid parameter value"));
        }
    }

    @Nested
    @DisplayName("Unhandled Exception Handling")
    class UnhandledException {

        @Test
        @DisplayName("Should return 500 for unhandled exception")
        void shouldReturn500_ForUnhandledException() throws Exception {
            // When / Then
            mockMvc.perform(get("/test/unhandled"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.status").value(500))
                    .andExpect(jsonPath("$.error").value("Internal Server Error"))
                    .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."));
        }
    }
}
