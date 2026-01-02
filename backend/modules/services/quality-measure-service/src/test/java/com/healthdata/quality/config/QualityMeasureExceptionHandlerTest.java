package com.healthdata.quality.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.MethodParameter;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@DisplayName("Quality Measure Exception Handler Tests")
class QualityMeasureExceptionHandlerTest {

    private final QualityMeasureExceptionHandler handler = new QualityMeasureExceptionHandler();

    @Test
    @DisplayName("Should return bad request for constraint violations")
    void shouldHandleConstraintViolation() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("field");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("required");

        ResponseEntity<Map<String, Object>> response =
            handler.handleConstraintViolationException(new ConstraintViolationException(Set.of(violation)));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Bad Request");
        assertThat(response.getBody().get("violations").toString()).contains("field: required");
    }

    @Test
    @DisplayName("Should return bad request for method argument errors")
    void shouldHandleMethodArgumentNotValid() throws Exception {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "field", "missing"));
        MethodParameter parameter = new MethodParameter(
            DummyController.class.getDeclaredMethod("handleBody", String.class), 0);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleMethodArgumentNotValidException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("violations").toString()).contains("field: missing");
    }

    @Test
    @DisplayName("Should return bad request for type mismatch")
    void shouldHandleTypeMismatch() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
            "bad", String.class, "tenantId", null, new IllegalArgumentException("bad"));

        ResponseEntity<Map<String, Object>> response = handler.handleMethodArgumentTypeMismatchException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Invalid parameter type: tenantId");
    }

    @Test
    @DisplayName("Should return bad request for missing parameter and header")
    void shouldHandleMissingRequestParameterAndHeader() throws Exception {
        MissingServletRequestParameterException paramEx =
            new MissingServletRequestParameterException("tenantId", "String");
        ResponseEntity<Map<String, Object>> paramResponse =
            handler.handleMissingServletRequestParameterException(paramEx);
        assertThat(paramResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(paramResponse.getBody().get("message").toString()).contains("tenantId");

        MethodParameter parameter = new MethodParameter(
            DummyController.class.getDeclaredMethod("handle", String.class), 0);
        MissingRequestHeaderException headerEx = new MissingRequestHeaderException("X-Tenant-Id", parameter);
        ResponseEntity<Map<String, Object>> headerResponse =
            handler.handleMissingRequestHeaderException(headerEx);
        assertThat(headerResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(headerResponse.getBody().get("message").toString()).contains("X-Tenant-Id");
    }

    @Test
    @DisplayName("Should return bad request for illegal argument")
    void shouldHandleIllegalArgument() {
        ResponseEntity<Map<String, Object>> response =
            handler.handleIllegalArgumentException(new IllegalArgumentException("bad input"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "bad input");
    }

    @Test
    @DisplayName("Should return server error for runtime and generic exceptions")
    void shouldHandleRuntimeAndGenericExceptions() {
        ResponseEntity<Map<String, Object>> runtimeResponse =
            handler.handleRuntimeException(new RuntimeException("boom"));
        assertThat(runtimeResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(runtimeResponse.getBody()).containsEntry("error", "Internal Server Error");

        ResponseEntity<Map<String, Object>> genericResponse =
            handler.handleGenericException(new Exception("boom"));
        assertThat(genericResponse.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(genericResponse.getBody()).containsEntry("message", "An unexpected error occurred");
    }

    private static class DummyController {
        @SuppressWarnings("unused")
        void handle(@org.springframework.web.bind.annotation.RequestHeader("X-Tenant-Id") String header) {}

        @SuppressWarnings("unused")
        void handleBody(@org.springframework.web.bind.annotation.RequestBody String body) {}
    }
}
