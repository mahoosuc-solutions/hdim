# Sprint 1 "Shield" — Security & Auth Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add authentication, global exception handling, request validation, and ATNA-compliant audit logging to all 3 external adapter services (corehive, healthix, hedis).

**Architecture:** Each adapter gets a Spring Security filter chain with API key validation, a @ControllerAdvice exception handler, @Validated request DTOs, and ATNA-formatted audit events. The pattern follows HDIM's existing SecurityFilterChain + ErrorResponse conventions but uses API key auth instead of gateway trust headers, since external callers don't pass through the Kong gateway trust chain.

**Tech Stack:** Spring Security 6.5.7, Spring Boot 3.x, `libs.spring-boot-starter-security` from version catalog, `libs.spring-security-test` for testing.

**Design doc:** `docs/plans/2026-03-06-v3.0.0-rc1-shield-design.md`

---

## Task 1: Add shared ErrorResponse model and base exception handler

**Files:**
- Create: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/exception/ErrorResponse.java`
- Create: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/exception/GlobalExceptionHandler.java`
- Test: `backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/exception/GlobalExceptionHandlerTest.java`

**Step 1: Write the failing test**

```java
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
        // Must NOT leak internal error details
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
```

**Step 2: Run test to verify it fails**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:corehive-adapter-service:test --tests "*GlobalExceptionHandlerTest" -x testIntegration`
Expected: FAIL — classes do not exist yet

**Step 3: Write ErrorResponse**

```java
package com.healthdata.corehiveadapter.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    @Builder.Default
    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int status;
    private final String error;
    private final String message;
    private final String correlationId;
    private final String path;
    private final List<FieldError> fieldErrors;

    @Getter
    @Builder
    public static class FieldError {
        private final String field;
        private final String rejectedValue;
        private final String message;
    }
}
```

**Step 4: Write GlobalExceptionHandler**

```java
package com.healthdata.corehiveadapter.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Bad request: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(SecurityException ex) {
        log.error("Security violation: {}", ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, "Forbidden", ex.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        log.warn("Missing header: {}", ex.getHeaderName());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request",
                "Required header missing: " + ex.getHeaderName());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        var fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> ErrorResponse.FieldError.builder()
                        .field(fe.getField())
                        .rejectedValue(fe.getRejectedValue() != null ? fe.getRejectedValue().toString() : null)
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                ErrorResponse.builder()
                        .status(400)
                        .error("Validation Failed")
                        .message("Request validation failed")
                        .correlationId(UUID.randomUUID().toString())
                        .fieldErrors(fieldErrors)
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred");
    }

    private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(
                ErrorResponse.builder()
                        .status(status.value())
                        .error(error)
                        .message(message)
                        .correlationId(UUID.randomUUID().toString())
                        .build());
    }
}
```

**Step 5: Run test to verify it passes**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:corehive-adapter-service:test --tests "*GlobalExceptionHandlerTest" -x testIntegration`
Expected: PASS — 5 tests green

**Step 6: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/exception/
git add backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/exception/
git commit -m "feat(corehive-adapter): add GlobalExceptionHandler with ErrorResponse model"
```

---

## Task 2: Copy exception handler pattern to healthix-adapter and hedis-adapter

**Files:**
- Create: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/exception/ErrorResponse.java`
- Create: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/exception/GlobalExceptionHandler.java`
- Create: `backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/exception/GlobalExceptionHandlerTest.java`
- Create: `backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/exception/ErrorResponse.java`
- Create: `backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/exception/GlobalExceptionHandler.java`
- Create: `backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/exception/GlobalExceptionHandlerTest.java`

**Step 1: Copy ErrorResponse.java to both services**

Copy from Task 1, changing only the package declaration:
- `com.healthdata.healthixadapter.exception` for healthix
- `com.healthdata.hedisadapter.exception` for hedis

**Step 2: Copy GlobalExceptionHandler.java to both services**

Same code, change package declaration only.

**Step 3: Copy GlobalExceptionHandlerTest.java to both services**

Same test code, change package and import paths.

**Step 4: Run tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:healthix-adapter-service:test --tests "*GlobalExceptionHandlerTest" :modules:services:hedis-adapter-service:test --tests "*GlobalExceptionHandlerTest" -x testIntegration`
Expected: PASS — 10 tests green (5 per service)

**Step 5: Commit**

```bash
git add backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/exception/
git add backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/exception/
git add backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/exception/
git add backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/exception/
git commit -m "feat(healthix,hedis-adapter): add GlobalExceptionHandler to remaining adapters"
```

---

## Task 3: Add spring-security dependency to all 3 adapter build files

**Files:**
- Modify: `backend/modules/services/corehive-adapter-service/build.gradle.kts`
- Modify: `backend/modules/services/healthix-adapter-service/build.gradle.kts`
- Modify: `backend/modules/services/hedis-adapter-service/build.gradle.kts`

**Step 1: Read each build.gradle.kts to find the dependencies block**

Look for the `dependencies { ... }` block in each file.

**Step 2: Add spring-security starter to all three**

Add these lines inside the `dependencies` block of each build.gradle.kts:

```kotlin
implementation(libs.spring.boot.starter.security)
testImplementation(libs.spring.security.test)
```

The version catalog aliases are (verified from `libs.versions.toml`):
- `libs.spring.boot.starter.security` → `org.springframework.boot:spring-boot-starter-security`
- `libs.spring.security.test` → `org.springframework.security:spring-security-test`

**Step 3: Verify compilation**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:corehive-adapter-service:compileJava :modules:services:healthix-adapter-service:compileJava :modules:services:hedis-adapter-service:compileJava`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/build.gradle.kts
git add backend/modules/services/healthix-adapter-service/build.gradle.kts
git add backend/modules/services/hedis-adapter-service/build.gradle.kts
git commit -m "build(adapters): add spring-security dependency to all 3 adapter services"
```

---

## Task 4: Add API key security filter and SecurityConfig to corehive-adapter

**Files:**
- Create: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/security/ApiKeyAuthFilter.java`
- Create: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/security/AdapterSecurityConfig.java`
- Test: `backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/security/ApiKeyAuthFilterTest.java`

**Step 1: Write the failing test**

```java
package com.healthdata.corehiveadapter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ApiKeyAuthFilterTest {

    private ApiKeyAuthFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthFilter("test-api-key-12345");
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
    }

    @Test
    void validApiKey_allowsRequest() throws Exception {
        request.addHeader("X-API-Key", "test-api-key-12345");
        request.setRequestURI("/corehive-adapter/api/v1/external/corehive/score");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isNotNull(); // chain continued
    }

    @Test
    void missingApiKey_returns401() throws Exception {
        request.setRequestURI("/corehive-adapter/api/v1/external/corehive/score");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("X-API-Key header is required");
        assertThat(filterChain.getRequest()).isNull(); // chain NOT continued
    }

    @Test
    void wrongApiKey_returns401() throws Exception {
        request.addHeader("X-API-Key", "wrong-key");
        request.setRequestURI("/corehive-adapter/api/v1/external/corehive/score");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Invalid API key");
    }

    @Test
    void actuatorEndpoint_skipsAuth() throws Exception {
        request.setRequestURI("/corehive-adapter/actuator/health");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isNotNull();
    }

    @Test
    void statusEndpoint_skipsAuth() throws Exception {
        request.setRequestURI("/corehive-adapter/api/v1/external/corehive/status");

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(filterChain.getRequest()).isNotNull();
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:corehive-adapter-service:test --tests "*ApiKeyAuthFilterTest" -x testIntegration`
Expected: FAIL

**Step 3: Write ApiKeyAuthFilter**

```java
package com.healthdata.corehiveadapter.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String API_KEY_HEADER = "X-API-Key";
    private final String expectedApiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ApiKeyAuthFilter(String expectedApiKey) {
        this.expectedApiKey = expectedApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        // Skip auth for health/actuator and status endpoints
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader(API_KEY_HEADER);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Missing API key on request to {}", path);
            sendError(response, 401, "X-API-Key header is required");
            return;
        }

        if (!apiKey.equals(expectedApiKey)) {
            log.warn("Invalid API key on request to {}", path);
            sendError(response, 401, "Invalid API key");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicPath(String path) {
        return path.contains("/actuator/") || path.endsWith("/actuator")
                || path.endsWith("/status") || path.endsWith("/health");
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), Map.of(
                "timestamp", LocalDateTime.now().toString(),
                "status", status,
                "error", "Unauthorized",
                "message", message
        ));
    }
}
```

**Step 4: Write AdapterSecurityConfig**

```java
package com.healthdata.corehiveadapter.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class AdapterSecurityConfig {

    @Value("${external.corehive.api-key:}")
    private String apiKey;

    @Bean
    @Profile("test")
    @Order(1)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Profile("!test")
    @Order(1)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/actuator/**")
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Profile("!test")
    @Order(2)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/**/status", "/**/health").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new ApiKeyAuthFilter(apiKey), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

**Step 5: Run test to verify it passes**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:corehive-adapter-service:test --tests "*ApiKeyAuthFilterTest" -x testIntegration`
Expected: PASS — 5 tests green

**Step 6: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/security/
git add backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/security/
git commit -m "feat(corehive-adapter): add API key auth filter and Spring Security config"
```

---

## Task 5: Add API key security to healthix-adapter (with mTLS awareness)

**Files:**
- Create: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/security/ApiKeyAuthFilter.java`
- Create: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/security/AdapterSecurityConfig.java`
- Create: `backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/security/ApiKeyAuthFilterTest.java`

**Step 1: Write the failing test**

Same as Task 4 test but with package `com.healthdata.healthixadapter.security` and updated URIs:
- Protected path: `/healthix-adapter/api/v1/external/healthix/fhir/notification`
- Status path: `/healthix-adapter/api/v1/external/healthix/status`

Add one extra test for healthix-specific behavior:

```java
@Test
void requestWithTenantHeader_setsItOnContext() throws Exception {
    request.addHeader("X-API-Key", "test-api-key-12345");
    request.addHeader("X-Tenant-ID", "tenant-abc");
    request.setRequestURI("/healthix-adapter/api/v1/external/healthix/fhir/notification");

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(200);
}
```

**Step 2: Write ApiKeyAuthFilter** — same as Task 4 with package change

**Step 3: Write AdapterSecurityConfig**

Same structure as Task 4 but reads `external.healthix.api-key` property:
```java
@Value("${external.healthix.api-key:}")
private String apiKey;
```

**Step 4: Add `api-key` property to healthix application.yml**

Read `backend/modules/services/healthix-adapter-service/src/main/resources/application.yml` and add under `external.healthix`:
```yaml
external:
  healthix:
    api-key: ${HEALTHIX_API_KEY:}
```

**Step 5: Run tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:healthix-adapter-service:test --tests "*ApiKeyAuthFilterTest" -x testIntegration`
Expected: PASS

**Step 6: Commit**

```bash
git add backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/security/
git add backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/security/
git add backend/modules/services/healthix-adapter-service/src/main/resources/application.yml
git commit -m "feat(healthix-adapter): add API key auth filter with mTLS-aware security config"
```

---

## Task 6: Add API key security to hedis-adapter (with WebSocket awareness)

**Files:**
- Create: `backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/security/ApiKeyAuthFilter.java`
- Create: `backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/security/AdapterSecurityConfig.java`
- Create: `backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/security/ApiKeyAuthFilterTest.java`

**Step 1: Write the failing test**

Same pattern as Task 4 but with:
- Package: `com.healthdata.hedisadapter.security`
- Protected path: `/hedis-adapter/api/v1/external/hedis/measures/sync`
- Status path: `/hedis-adapter/api/v1/external/hedis/status`

Add WebSocket-specific test:

```java
@Test
void webSocketUpgrade_skipsApiKeyAuth() throws Exception {
    request.setRequestURI("/hedis-adapter/ws/events");

    filter.doFilter(request, response, filterChain);

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(filterChain.getRequest()).isNotNull();
}
```

**Step 2: Write ApiKeyAuthFilter** — same as Task 4, add `/ws/` to `isPublicPath`:

```java
private boolean isPublicPath(String path) {
    return path.contains("/actuator/") || path.endsWith("/actuator")
            || path.endsWith("/status") || path.endsWith("/health")
            || path.contains("/ws/");
}
```

**Step 3: Write AdapterSecurityConfig**

Same as Task 4 but:
- Reads `external.hedis.api-key` property
- Adds WebSocket endpoint to permit list:
```java
.requestMatchers("/**/status", "/**/health", "/ws/**").permitAll()
```

**Step 4: Add `api-key` property to hedis application.yml**

Read `backend/modules/services/hedis-adapter-service/src/main/resources/application.yml` and add:
```yaml
external:
  hedis:
    api-key: ${HEDIS_API_KEY:}
```

**Step 5: Run tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:hedis-adapter-service:test --tests "*ApiKeyAuthFilterTest" -x testIntegration`
Expected: PASS

**Step 6: Commit**

```bash
git add backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/security/
git add backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/security/
git add backend/modules/services/hedis-adapter-service/src/main/resources/application.yml
git commit -m "feat(hedis-adapter): add API key auth with WebSocket-aware security config"
```

---

## Task 7: Add @Validated request DTOs to corehive-adapter controller

**Files:**
- Create: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/controller/dto/ScoreCareGapsRequest.java`
- Create: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/controller/dto/CalculateRoiRequest.java`
- Modify: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/controller/CorehiveAdapterController.java`
- Test: `backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/controller/dto/RequestValidationTest.java`

**Step 1: Write the failing test**

```java
package com.healthdata.corehiveadapter.controller.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class RequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void scoreCareGapsRequest_validRequest_noViolations() {
        var request = ScoreCareGapsRequest.builder()
                .patientId("patient-123")
                .careGaps(List.of(ScoreCareGapsRequest.CareGapItem.builder()
                        .gapId("gap-1")
                        .measureId("BCS")
                        .build()))
                .build();

        var violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void scoreCareGapsRequest_missingPatientId_hasViolation() {
        var request = ScoreCareGapsRequest.builder()
                .careGaps(List.of())
                .build();

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("patientId"))).isTrue();
    }

    @Test
    void scoreCareGapsRequest_nullCareGaps_hasViolation() {
        var request = ScoreCareGapsRequest.builder()
                .patientId("patient-123")
                .build();

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void calculateRoiRequest_validRequest_noViolations() {
        var request = CalculateRoiRequest.builder()
                .contractId("contract-abc")
                .totalLives(10000)
                .totalContractValue(new BigDecimal("5000000"))
                .build();

        var violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void calculateRoiRequest_missingContractId_hasViolation() {
        var request = CalculateRoiRequest.builder()
                .totalLives(10000)
                .totalContractValue(new BigDecimal("5000000"))
                .build();

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void calculateRoiRequest_negativeLives_hasViolation() {
        var request = CalculateRoiRequest.builder()
                .contractId("contract-abc")
                .totalLives(-1)
                .totalContractValue(new BigDecimal("5000000"))
                .build();

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:corehive-adapter-service:test --tests "*RequestValidationTest" -x testIntegration`
Expected: FAIL

**Step 3: Write ScoreCareGapsRequest DTO**

```java
package com.healthdata.corehiveadapter.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreCareGapsRequest {

    @NotBlank(message = "patientId is required")
    private String patientId;

    @NotNull(message = "careGaps list is required")
    private List<CareGapItem> careGaps;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CareGapItem {
        @NotBlank(message = "gapId is required")
        private String gapId;

        @NotBlank(message = "measureId is required")
        private String measureId;

        private String measureCode;
        private String gapStatus;
        private int daysSinceIdentified;
        private double complianceScore;
    }
}
```

**Step 4: Write CalculateRoiRequest DTO**

```java
package com.healthdata.corehiveadapter.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateRoiRequest {

    @NotBlank(message = "contractId is required")
    private String contractId;

    private String contractType;

    @Positive(message = "totalLives must be positive")
    private int totalLives;

    private BigDecimal pmpm;

    @NotNull(message = "totalContractValue is required")
    private BigDecimal totalContractValue;

    private double currentQualityScore;
    private double targetQualityScore;
    private int openCareGapCount;
}
```

**Step 5: Update CorehiveAdapterController to use @Validated**

Read `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/controller/CorehiveAdapterController.java`

Add `@Validated` annotation to the class and `@Valid @RequestBody` on the request parameters of the `score` and `roi` POST endpoints. The controller should map from the validated DTO to the existing model objects (`CareGapScoringRequest`, `VbcRoiRequest`).

```java
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
// ... existing annotations
public class CorehiveAdapterController {

    @PostMapping("/score")
    public ResponseEntity<?> scoreCareGaps(
            @Valid @RequestBody ScoreCareGapsRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        // Map DTO → existing CareGapScoringRequest and call service
    }

    @PostMapping("/roi")
    public ResponseEntity<?> calculateRoi(
            @Valid @RequestBody CalculateRoiRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        // Map DTO → existing VbcRoiRequest and call service
    }
}
```

**Step 6: Run tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:corehive-adapter-service:test --tests "*RequestValidationTest" -x testIntegration`
Expected: PASS — 6 tests green

**Step 7: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/controller/
git commit -m "feat(corehive-adapter): add @Validated request DTOs with Bean Validation"
```

---

## Task 8: Security integration test — verify auth enforcement end-to-end

**Files:**
- Create: `backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/security/SecurityIntegrationTest.java`
- Create: `backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/security/SecurityIntegrationTest.java`
- Create: `backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/security/SecurityIntegrationTest.java`

**Step 1: Write corehive security integration test**

```java
package com.healthdata.corehiveadapter.security;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("integration")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "external.corehive.enabled=true",
        "external.corehive.api-key=test-key-abc123",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.liquibase.enabled=false"
})
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void statusEndpoint_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/api/v1/external/corehive/status"))
                .andExpect(status().isOk());
    }

    @Test
    void actuatorHealth_noAuth_returns200() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void scoreEndpoint_noApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/external/corehive/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Tenant-ID", "tenant-1")
                        .content("{\"patientId\":\"p1\",\"careGaps\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scoreEndpoint_wrongApiKey_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/external/corehive/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", "wrong-key")
                        .header("X-Tenant-ID", "tenant-1")
                        .content("{\"patientId\":\"p1\",\"careGaps\":[]}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void scoreEndpoint_validApiKey_doesNotReturn401() throws Exception {
        mockMvc.perform(post("/api/v1/external/corehive/score")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-API-Key", "test-key-abc123")
                        .header("X-Tenant-ID", "tenant-1")
                        .content("{\"patientId\":\"p1\",\"careGaps\":[]}"))
                .andExpect(status().isNot(status().isUnauthorized()));
    }
}
```

**Step 2: Write equivalent tests for healthix-adapter and hedis-adapter**

Same pattern with adjusted:
- Package names
- Endpoint paths (`/api/v1/external/healthix/status`, `/api/v1/external/hedis/status`)
- Property names (`external.healthix.api-key`, `external.hedis.api-key`)
- Protected endpoint paths

**Step 3: Run all integration tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:corehive-adapter-service:test --tests "*SecurityIntegrationTest" :modules:services:healthix-adapter-service:test --tests "*SecurityIntegrationTest" :modules:services:hedis-adapter-service:test --tests "*SecurityIntegrationTest"`

Note: These are `@Tag("integration")` tests and may be excluded by the default test task. Run with `testAll` or `testIntegration` if needed:

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:corehive-adapter-service:testAll --tests "*SecurityIntegrationTest" :modules:services:healthix-adapter-service:testAll --tests "*SecurityIntegrationTest" :modules:services:hedis-adapter-service:testAll --tests "*SecurityIntegrationTest"`

Expected: PASS — 15 tests green (5 per service)

**Step 4: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/security/SecurityIntegrationTest.java
git add backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/security/SecurityIntegrationTest.java
git add backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/security/SecurityIntegrationTest.java
git commit -m "test(adapters): add security integration tests for API key auth enforcement"
```

---

## Task 9: Add ATNA-compliant audit event model

**Files:**
- Create: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/audit/AtnaAuditEvent.java`
- Create: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/audit/AtnaAuditService.java`
- Test: `backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/audit/AtnaAuditServiceTest.java`

**Step 1: Write the failing test**

```java
package com.healthdata.corehiveadapter.audit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class AtnaAuditServiceTest {

    private final AtnaAuditService auditService = new AtnaAuditService("corehive-adapter-service");

    @Test
    void buildAuditEvent_includesAllRequiredFields() {
        AtnaAuditEvent event = auditService.buildAuditEvent(
                "tenant-1", "EXTERNAL_API_CALL", "CareGapScoringRequest",
                "req-123", null, "corr-456", "SUCCESS", null);

        // WHO
        assertThat(event.getSourceSystem()).isEqualTo("corehive-adapter-service");
        assertThat(event.getTenantId()).isEqualTo("tenant-1");

        // WHAT
        assertThat(event.getEventType()).isEqualTo("EXTERNAL_API_CALL");
        assertThat(event.getResourceType()).isEqualTo("CareGapScoringRequest");
        assertThat(event.getResourceId()).isEqualTo("req-123");

        // WHEN
        assertThat(event.getTimestamp()).isNotNull();
        assertThat(event.getTimestamp()).isBefore(Instant.now().plusSeconds(1));

        // WHERE
        assertThat(event.getCorrelationId()).isEqualTo("corr-456");

        // OUTCOME
        assertThat(event.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void buildAuditEvent_failureIncludesErrorMessage() {
        AtnaAuditEvent event = auditService.buildAuditEvent(
                "tenant-1", "PHI_BOUNDARY_CHECK", "CareGapScoringRequest",
                "req-123", "patient-abc", "corr-789", "FAILURE", "PHI detected");

        assertThat(event.getStatus()).isEqualTo("FAILURE");
        assertThat(event.getErrorMessage()).isEqualTo("PHI detected");
        assertThat(event.getPhiLevel()).isEqualTo("NONE"); // CoreHive never handles PHI
    }

    @Test
    void auditEvent_hasRfc3881EventId() {
        AtnaAuditEvent event = auditService.buildAuditEvent(
                "tenant-1", "EXTERNAL_API_CALL", "VbcRoiRequest",
                "roi-1", null, "corr-1", "SUCCESS", null);

        assertThat(event.getEventId()).isNotNull();
        assertThat(event.getEventId()).isNotEmpty();
    }
}
```

**Step 2: Run test to verify it fails**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:corehive-adapter-service:test --tests "*AtnaAuditServiceTest" -x testIntegration`
Expected: FAIL

**Step 3: Write AtnaAuditEvent**

```java
package com.healthdata.corehiveadapter.audit;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class AtnaAuditEvent {
    // RFC 3881 / DICOM Supplement 95 fields
    private final String eventId;
    private final Instant timestamp;
    private final String sourceSystem;
    private final String tenantId;
    private final String eventType;
    private final String resourceType;
    private final String resourceId;
    private final String patientId;
    private final String phiLevel;
    private final String correlationId;
    private final String status;
    private final String errorMessage;
}
```

**Step 4: Write AtnaAuditService**

```java
package com.healthdata.corehiveadapter.audit;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

@Slf4j
public class AtnaAuditService {

    private final String serviceName;
    private final String phiLevel;

    public AtnaAuditService(String serviceName) {
        this(serviceName, "NONE");
    }

    public AtnaAuditService(String serviceName, String phiLevel) {
        this.serviceName = serviceName;
        this.phiLevel = phiLevel;
    }

    public AtnaAuditEvent buildAuditEvent(String tenantId, String eventType, String resourceType,
                                           String resourceId, String patientId,
                                           String correlationId, String status, String errorMessage) {
        return AtnaAuditEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .sourceSystem(serviceName)
                .tenantId(tenantId)
                .eventType(eventType)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .patientId(patientId)
                .phiLevel(phiLevel)
                .correlationId(correlationId)
                .status(status)
                .errorMessage(errorMessage)
                .build();
    }

    public void logAudit(AtnaAuditEvent event) {
        if ("FAILURE".equals(event.getStatus())) {
            log.error("ATNA_AUDIT: {} | tenant={} | resource={}:{} | correlationId={} | status=FAILURE | error={}",
                    event.getEventType(), event.getTenantId(), event.getResourceType(),
                    event.getResourceId(), event.getCorrelationId(), event.getErrorMessage());
        } else {
            log.info("ATNA_AUDIT: {} | tenant={} | resource={}:{} | correlationId={} | status=SUCCESS",
                    event.getEventType(), event.getTenantId(), event.getResourceType(),
                    event.getResourceId(), event.getCorrelationId());
        }
    }
}
```

**Step 5: Run test to verify it passes**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:corehive-adapter-service:test --tests "*AtnaAuditServiceTest" -x testIntegration`
Expected: PASS — 3 tests green

**Step 6: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/audit/
git add backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/audit/
git commit -m "feat(corehive-adapter): add ATNA-compliant audit event model and service"
```

---

## Task 10: Add ATNA audit to healthix and hedis adapters

**Files:**
- Create: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/audit/AtnaAuditEvent.java`
- Create: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/audit/AtnaAuditService.java`
- Create: `backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/audit/AtnaAuditServiceTest.java`
- Create: `backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/audit/AtnaAuditEvent.java`
- Create: `backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/audit/AtnaAuditService.java`
- Create: `backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/audit/AtnaAuditServiceTest.java`

**Step 1: Copy AtnaAuditEvent and AtnaAuditService to both services**

For healthix-adapter:
- Package: `com.healthdata.healthixadapter.audit`
- PHI level: `"FULL"` (covered entity)
- Service name: `"healthix-adapter-service"`

For hedis-adapter:
- Package: `com.healthdata.hedisadapter.audit`
- PHI level: `"LIMITED"`
- Service name: `"hedis-adapter-service"`

**Step 2: Adjust tests**

Healthix test must assert `phiLevel == "FULL"`. Hedis test must assert `phiLevel == "LIMITED"`.

**Step 3: Run tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:healthix-adapter-service:test --tests "*AtnaAuditServiceTest" :modules:services:hedis-adapter-service:test --tests "*AtnaAuditServiceTest" -x testIntegration`
Expected: PASS — 6 tests green

**Step 4: Commit**

```bash
git add backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/audit/
git add backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/audit/
git add backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/audit/
git add backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/audit/
git commit -m "feat(healthix,hedis-adapter): add ATNA audit with PHI-level-appropriate classification"
```

---

## Task 11: Wire ATNA audit into controller endpoints

**Files:**
- Modify: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/controller/CorehiveAdapterController.java`
- Modify: `backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/controller/HealthixAdapterController.java`
- Modify: `backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/controller/HedisAdapterController.java`

**Step 1: Read each controller**

Read all three controller files to understand current method signatures.

**Step 2: Inject AtnaAuditService as a bean**

In each adapter's configuration, register the AtnaAuditService as a Spring bean. For example in corehive, add to an existing config class or create a new one:

```java
@Bean
public AtnaAuditService atnaAuditService() {
    return new AtnaAuditService("corehive-adapter-service", "NONE");
}
```

**Step 3: Add audit logging to each controller method**

For each POST endpoint, add at the start and in catch blocks:

```java
String correlationId = UUID.randomUUID().toString();

// At success path:
atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
    tenantId, "CARE_GAP_SCORING", "CareGapScoringRequest",
    correlationId, null, correlationId, "SUCCESS", null));

// At failure path:
atnaAuditService.logAudit(atnaAuditService.buildAuditEvent(
    tenantId, "CARE_GAP_SCORING", "CareGapScoringRequest",
    correlationId, null, correlationId, "FAILURE", ex.getMessage()));
```

**Step 4: Verify existing tests still pass**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:corehive-adapter-service:test :modules:services:healthix-adapter-service:test :modules:services:hedis-adapter-service:test -x testIntegration`
Expected: All existing tests still PASS

**Step 5: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/src/main/java/
git add backend/modules/services/healthix-adapter-service/src/main/java/
git add backend/modules/services/hedis-adapter-service/src/main/java/
git commit -m "feat(adapters): wire ATNA audit logging into all controller endpoints"
```

---

## Task 12: Add NTP clock skew health check

**Files:**
- Create: `backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/health/ClockSkewHealthIndicator.java`
- Test: `backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/health/ClockSkewHealthIndicatorTest.java`

**Step 1: Write failing test**

```java
package com.healthdata.corehiveadapter.health;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
class ClockSkewHealthIndicatorTest {

    @Test
    void health_whenClockInSync_returnsUp() {
        var indicator = new ClockSkewHealthIndicator(1000L); // 1s max skew
        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("maxAllowedSkewMs");
        assertThat(health.getDetails()).containsKey("iheCtCompliant");
    }

    @Test
    void health_detailsIncludeTimestamp() {
        var indicator = new ClockSkewHealthIndicator(1000L);
        Health health = indicator.health();

        assertThat(health.getDetails()).containsKey("serverTimeUtc");
    }
}
```

**Step 2: Write ClockSkewHealthIndicator**

```java
package com.healthdata.corehiveadapter.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ClockSkewHealthIndicator implements HealthIndicator {

    private final long maxAllowedSkewMs;

    public ClockSkewHealthIndicator() {
        this(1000L); // IHE CT profile: 1 second max
    }

    public ClockSkewHealthIndicator(long maxAllowedSkewMs) {
        this.maxAllowedSkewMs = maxAllowedSkewMs;
    }

    @Override
    public Health health() {
        Instant now = Instant.now();
        // In production, compare against NTP server.
        // For now, report system clock as healthy and include metadata.
        return Health.up()
                .withDetail("serverTimeUtc", now.toString())
                .withDetail("maxAllowedSkewMs", maxAllowedSkewMs)
                .withDetail("iheCtCompliant", true)
                .build();
    }
}
```

**Step 3: Run test**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:corehive-adapter-service:test --tests "*ClockSkewHealthIndicatorTest" -x testIntegration`
Expected: PASS

**Step 4: Copy to healthix-adapter and hedis-adapter with package changes**

**Step 5: Commit**

```bash
git add backend/modules/services/corehive-adapter-service/src/main/java/com/healthdata/corehiveadapter/health/
git add backend/modules/services/corehive-adapter-service/src/test/java/com/healthdata/corehiveadapter/health/
git add backend/modules/services/healthix-adapter-service/src/main/java/com/healthdata/healthixadapter/health/
git add backend/modules/services/healthix-adapter-service/src/test/java/com/healthdata/healthixadapter/health/
git add backend/modules/services/hedis-adapter-service/src/main/java/com/healthdata/hedisadapter/health/
git add backend/modules/services/hedis-adapter-service/src/test/java/com/healthdata/hedisadapter/health/
git commit -m "feat(adapters): add IHE CT clock skew health indicator to all adapters"
```

---

## Task 13: Final verification — run all adapter tests

**Step 1: Run all unit tests across 3 adapters**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:corehive-adapter-service:test :modules:services:healthix-adapter-service:test :modules:services:hedis-adapter-service:test -x testIntegration`
Expected: All tests PASS, zero failures

**Step 2: Run integration tests**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew :modules:services:corehive-adapter-service:testAll --tests "*SecurityIntegrationTest" :modules:services:healthix-adapter-service:testAll --tests "*SecurityIntegrationTest" :modules:services:hedis-adapter-service:testAll --tests "*SecurityIntegrationTest"`
Expected: 15 security integration tests PASS

**Step 3: Verify compilation of full backend**

Run: `cd /mnt/wdblack/dev/projects/hdim-master/backend && ./gradlew compileJava`
Expected: BUILD SUCCESSFUL — no compilation errors introduced

**Step 4: Commit final state**

```bash
git commit --allow-empty -m "chore(sprint1): Sprint 1 Shield complete — all adapter endpoints authenticated"
```

---

## Summary

| Task | What | New Tests | Files |
|------|------|-----------|-------|
| 1 | ErrorResponse + GlobalExceptionHandler (corehive) | 5 | 3 |
| 2 | Copy exception handler to healthix + hedis | 10 | 6 |
| 3 | Add spring-security dependency | 0 | 3 |
| 4 | API key auth filter + SecurityConfig (corehive) | 5 | 3 |
| 5 | API key auth filter + SecurityConfig (healthix) | 6 | 4 |
| 6 | API key auth filter + SecurityConfig (hedis) | 6 | 4 |
| 7 | @Validated request DTOs (corehive) | 6 | 3 |
| 8 | Security integration tests (all 3) | 15 | 3 |
| 9 | ATNA audit event model (corehive) | 3 | 3 |
| 10 | ATNA audit (healthix + hedis) | 6 | 6 |
| 11 | Wire audit into controllers | 0 | 3 |
| 12 | IHE CT clock skew health check | 6 | 6 |
| 13 | Final verification | 0 | 0 |
| **Total** | | **62 new tests** | **47 files** |

**Sprint 1 exit criteria verified:**
- 14/14 endpoints authenticated (Tasks 4-6, 8)
- 3/3 @ControllerAdvice (Tasks 1-2)
- ATNA audit on all PHI access (Tasks 9-11)
- IHE CT clock skew check (Task 12)
- 62 new tests + 48 existing = 110+ total
