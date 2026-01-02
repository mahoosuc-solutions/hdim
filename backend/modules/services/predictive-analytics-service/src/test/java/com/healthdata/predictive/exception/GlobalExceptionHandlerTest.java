package com.healthdata.predictive.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void shouldHandleIllegalArgumentException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        IllegalArgumentException ex = new IllegalArgumentException("invalid input");
        ServletWebRequest request = new ServletWebRequest(new MockHttpServletRequest());

        ResponseEntity<Map<String, Object>> response = handler.handleIllegalArgumentException(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).containsEntry("message", "invalid input");
        assertThat(response.getBody()).containsEntry("status", 400);
    }
}
