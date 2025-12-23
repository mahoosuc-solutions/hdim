package com.healthdata.migration.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("Should map not found IllegalArgumentException to 404")
    void shouldMapNotFoundTo404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalArgumentException(new IllegalArgumentException("Job not found"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("status", 404);
    }

    @Test
    @DisplayName("Should map IllegalArgumentException to 400")
    void shouldMapIllegalArgumentTo400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalArgumentException(new IllegalArgumentException("Invalid input"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("status", 400);
    }

    @Test
    @DisplayName("Should return validation errors for invalid arguments")
    void shouldReturnValidationErrors() {
        BindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "field1", "must not be blank"));

        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<Map<String, Object>> response = handler.handleValidationException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("fieldErrors");
        Object fieldErrors = response.getBody().get("fieldErrors");
        assertThat(fieldErrors).isInstanceOf(Map.class);
        @SuppressWarnings("unchecked")
        Map<String, String> fieldErrorsMap = (Map<String, String>) fieldErrors;
        assertThat(fieldErrorsMap).containsEntry("field1", "must not be blank");
    }

    @Test
    @DisplayName("Should handle missing request header")
    void shouldHandleMissingRequestHeader() {
        MissingRequestHeaderException exception = mock(MissingRequestHeaderException.class);
        when(exception.getHeaderName()).thenReturn("X-Tenant-Id");

        ResponseEntity<Map<String, Object>> response = handler.handleMissingHeader(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("message", "Missing required header: X-Tenant-Id");
    }

    @Test
    @DisplayName("Should handle generic exception as 500")
    void shouldHandleGenericException() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleGenericException(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).containsEntry("status", 500);
    }
}
