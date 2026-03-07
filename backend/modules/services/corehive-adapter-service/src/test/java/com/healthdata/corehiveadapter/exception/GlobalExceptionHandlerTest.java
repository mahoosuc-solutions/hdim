package com.healthdata.corehiveadapter.exception;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.core.MethodParameter;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleIllegalArgument_returns400WithMessage() {
        var ex = new IllegalArgumentException("bad input");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("bad input");
        assertThat(response.getBody().getError()).isEqualTo("Bad Request");
        assertThat(response.getBody().getTimestamp()).isNotNull();
    }

    @Test
    void handleSecurityException_returns403() {
        var ex = new SecurityException("PHI detected in outbound request");
        ResponseEntity<ErrorResponse> response = handler.handleSecurityException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getError()).isEqualTo("Forbidden");
    }

    @Test
    void handleGenericException_returns500WithoutInternalDetails() {
        var ex = new RuntimeException("db connection failed");
        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
        assertThat(response.getBody().getMessage()).doesNotContain("db connection");
    }

    @Test
    void handleMissingHeader_returns400() throws Exception {
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("handleMissingHeader_returns400");
        MethodParameter param = new MethodParameter(method, -1);
        var ex = new MissingRequestHeaderException("X-Tenant-ID", param);
        ResponseEntity<ErrorResponse> response = handler.handleMissingHeader(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("X-Tenant-ID");
    }

    @Test
    void errorResponse_includesCorrelationId() {
        var ex = new IllegalArgumentException("test");
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex);

        assertThat(response.getBody().getCorrelationId()).isNotNull();
        assertThat(response.getBody().getCorrelationId()).isNotEmpty();
    }
}
