# HDIM Common Coding Patterns

## Essential Patterns for Consistent Development

---

## 1. Controller Pattern

### Standard REST Controller

```java
package com.healthdata.patient.api.v1;

import com.healthdata.patient.application.PatientService;
import com.healthdata.patient.api.v1.dto.PatientResponse;
import com.healthdata.patient.api.v1.dto.CreatePatientRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/patients")
@RequiredArgsConstructor
@Validated
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')")
    public ResponseEntity<PatientResponse> getPatient(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        PatientResponse patient = patientService.getPatient(patientId, tenantId);

        // PHI response headers
        return ResponseEntity.ok()
            .header("Cache-Control", "no-store, no-cache, must-revalidate, private")
            .header("Pragma", "no-cache")
            .header("Expires", "0")
            .body(patient);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<PatientResponse> createPatient(
            @Valid @RequestBody CreatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        PatientResponse created = patientService.createPatient(request, tenantId);

        return ResponseEntity
            .created(URI.create("/api/v1/patients/" + created.getId()))
            .body(created);
    }

    @PutMapping("/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<PatientResponse> updatePatient(
            @PathVariable String patientId,
            @Valid @RequestBody UpdatePatientRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        PatientResponse updated = patientService.updatePatient(patientId, request, tenantId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{patientId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePatient(
            @PathVariable String patientId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        patientService.deletePatient(patientId, tenantId);
        return ResponseEntity.noContent().build();
    }
}
```

### Key Elements
- `@RequiredArgsConstructor` for dependency injection
- `@Validated` for parameter validation
- `@PreAuthorize` on every endpoint
- `X-Tenant-ID` header required
- PHI cache-control headers on GET
- Proper HTTP status codes

---

## 2. Service Pattern

### Application Service

```java
package com.healthdata.patient.application;

import com.healthdata.patient.domain.model.Patient;
import com.healthdata.patient.domain.repository.PatientRepository;
import com.healthdata.patient.api.v1.dto.*;
import com.healthdata.shared.exception.ResourceNotFoundException;
import com.healthdata.shared.audit.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final AuditService auditService;
    private final PatientMapper patientMapper;

    @Cacheable(value = "patientData", key = "#patientId")
    public PatientResponse getPatient(String patientId, String tenantId) {
        log.debug("Fetching patient: {} for tenant: {}", patientId, tenantId);

        Patient patient = patientRepository.findByIdAndTenant(patientId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        auditService.logAccess("PHI_ACCESS", "Patient", patientId, "VIEW");

        return patientMapper.toResponse(patient);
    }

    @Transactional
    @CacheEvict(value = "patientData", key = "#result.id")
    public PatientResponse createPatient(CreatePatientRequest request, String tenantId) {
        log.info("Creating patient for tenant: {}", tenantId);

        Patient patient = patientMapper.toEntity(request);
        patient.setTenantId(tenantId);

        Patient saved = patientRepository.save(patient);

        auditService.logAccess("PHI_CREATE", "Patient", saved.getId(), "CREATE");

        return patientMapper.toResponse(saved);
    }

    @Transactional
    @CacheEvict(value = "patientData", key = "#patientId")
    public PatientResponse updatePatient(
            String patientId,
            UpdatePatientRequest request,
            String tenantId) {

        Patient patient = patientRepository.findByIdAndTenant(patientId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        patientMapper.updateEntity(patient, request);
        Patient updated = patientRepository.save(patient);

        auditService.logAccess("PHI_UPDATE", "Patient", patientId, "UPDATE");

        return patientMapper.toResponse(updated);
    }

    @Transactional
    @CacheEvict(value = "patientData", key = "#patientId")
    public void deletePatient(String patientId, String tenantId) {
        Patient patient = patientRepository.findByIdAndTenant(patientId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        patientRepository.delete(patient);

        auditService.logAccess("PHI_DELETE", "Patient", patientId, "DELETE");
    }
}
```

### Key Elements
- `@Transactional(readOnly = true)` at class level
- `@Transactional` on write operations
- `@Cacheable` with proper key
- `@CacheEvict` on mutations
- Audit logging for PHI access
- Tenant isolation enforcement
- Proper exception handling

---

## 3. Repository Pattern

### Multi-Tenant Repository

```java
package com.healthdata.patient.domain.repository;

import com.healthdata.patient.domain.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {

    // CRITICAL: All queries MUST filter by tenantId

    @Query("SELECT p FROM Patient p WHERE p.id = :id AND p.tenantId = :tenantId")
    Optional<Patient> findByIdAndTenant(
        @Param("id") String id,
        @Param("tenantId") String tenantId
    );

    @Query("SELECT p FROM Patient p WHERE p.tenantId = :tenantId")
    List<Patient> findAllByTenant(@Param("tenantId") String tenantId);

    @Query("SELECT p FROM Patient p WHERE p.fhirId = :fhirId AND p.tenantId = :tenantId")
    Optional<Patient> findByFhirIdAndTenant(
        @Param("fhirId") String fhirId,
        @Param("tenantId") String tenantId
    );

    @Query("SELECT p FROM Patient p " +
           "WHERE p.lastName LIKE %:lastName% " +
           "AND p.tenantId = :tenantId")
    List<Patient> searchByLastNameAndTenant(
        @Param("lastName") String lastName,
        @Param("tenantId") String tenantId
    );

    @Query("SELECT COUNT(p) FROM Patient p WHERE p.tenantId = :tenantId")
    long countByTenant(@Param("tenantId") String tenantId);

    void deleteByIdAndTenant(String id, String tenantId);
}
```

### Key Elements
- Explicit `@Query` with tenant filter
- No methods without tenant parameter
- Use `Optional<T>` for single results
- Descriptive method names

---

## 4. Entity Pattern

### Complete Entity Example

```java
package com.healthdata.patient.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patients", indexes = {
    @Index(name = "idx_patients_tenant_id", columnList = "tenant_id"),
    @Index(name = "idx_patients_fhir_id", columnList = "fhir_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 50)
    private String tenantId;

    @Column(name = "fhir_id", nullable = false, length = 255)
    private String fhirId;

    @Column(name = "first_name", length = 100)
    private String firstName;

    @Column(name = "last_name", length = 100)
    private String lastName;

    @Column(name = "date_of_birth")
    private Instant dateOfBirth;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        if (active == null) {
            active = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
```

### Key Elements
- `@Table` with indexes
- `tenant_id` column ALWAYS
- `@PrePersist` for timestamps
- `@PreUpdate` for updates
- Snake_case column names
- CamelCase field names

---

## 5. Security Configuration Pattern

### Service Security Config

```java
package com.healthdata.patient.config;

import com.healthdata.authentication.security.TrustedHeaderAuthFilter;
import com.healthdata.authentication.security.TrustedTenantAccessFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class PatientSecurityConfig {

    private final TrustedHeaderAuthFilter trustedHeaderAuthFilter;
    private final TrustedTenantAccessFilter trustedTenantAccessFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/api/v1/**").authenticated()
                .anyRequest().authenticated())
            .addFilterBefore(trustedHeaderAuthFilter,
                UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(trustedTenantAccessFilter,
                TrustedHeaderAuthFilter.class);

        return http.build();
    }
}
```

---

## 6. Exception Handling Pattern

### Global Exception Handler

```java
package com.healthdata.patient.api.exception;

import com.healthdata.shared.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.NOT_FOUND.value())
            .error("Not Found")
            .message(ex.getMessage())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(TenantAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleTenantAccessDenied(
            TenantAccessDeniedException ex) {
        log.error("Tenant access denied: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.FORBIDDEN.value())
            .error("Forbidden")
            .message(ex.getMessage())
            .build();

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            ValidationException ex) {
        log.warn("Validation failed: {}", ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.BAD_REQUEST.value())
            .error("Bad Request")
            .message(ex.getMessage())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", Instant.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("errors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected error", ex);

        ErrorResponse error = ErrorResponse.builder()
            .timestamp(Instant.now())
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error("Internal Server Error")
            .message("An unexpected error occurred")
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
```

---

## 7. DTO Pattern

### Request DTO

```java
package com.healthdata.patient.api.v1.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.Instant;

@Data
public class CreatePatientRequest {

    @NotBlank(message = "FHIR ID is required")
    @Size(max = 255, message = "FHIR ID must not exceed 255 characters")
    private String fhirId;

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @Past(message = "Date of birth must be in the past")
    private Instant dateOfBirth;

    @Email(message = "Email must be valid")
    private String email;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number")
    private String phoneNumber;
}
```

### Response DTO

```java
package com.healthdata.patient.api.v1.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PatientResponse {

    private String id;
    private String fhirId;
    private String firstName;
    private String lastName;
    private Instant dateOfBirth;
    private String email;
    private String phoneNumber;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
```

---

## 8. Testing Patterns

### Unit Test

```java
@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private PatientMapper patientMapper;

    @InjectMocks
    private PatientService patientService;

    private Patient testPatient;
    private PatientResponse testResponse;

    @BeforeEach
    void setUp() {
        testPatient = Patient.builder()
            .id(UUID.randomUUID())
            .tenantId("TENANT001")
            .fhirId("patient-123")
            .firstName("John")
            .lastName("Doe")
            .build();

        testResponse = PatientResponse.builder()
            .id(testPatient.getId().toString())
            .fhirId("patient-123")
            .firstName("John")
            .lastName("Doe")
            .build();
    }

    @Test
    void shouldReturnPatient_WhenPatientExists() {
        // Given
        when(patientRepository.findByIdAndTenant(anyString(), anyString()))
            .thenReturn(Optional.of(testPatient));
        when(patientMapper.toResponse(any())).thenReturn(testResponse);

        // When
        PatientResponse result = patientService.getPatient("123", "TENANT001");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getFirstName()).isEqualTo("John");
        verify(auditService).logAccess(eq("PHI_ACCESS"), any(), any(), any());
    }

    @Test
    void shouldThrowException_WhenPatientNotFound() {
        // Given
        when(patientRepository.findByIdAndTenant(anyString(), anyString()))
            .thenReturn(Optional.empty());

        // When / Then
        assertThrows(ResourceNotFoundException.class, () ->
            patientService.getPatient("123", "TENANT001")
        );
    }
}
```

### Integration Test

```java
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class PatientControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
        .withDatabaseName("testdb");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldReturnPatient_WhenValidRequest() throws Exception {
        mockMvc.perform(get("/api/v1/patients/123")
                .header("X-Auth-User-Id", "user-123")
                .header("X-Auth-Tenant-Ids", "TENANT001")
                .header("X-Auth-Roles", "ADMIN")
                .header("X-Tenant-ID", "TENANT001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("123"))
            .andExpect(header().string("Cache-Control", containsString("no-store")));
    }
}
```

---

## 9. Configuration Pattern

### Application Configuration

```yaml
# application.yml
spring:
  application:
    name: patient-service

  datasource:
    url: jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5435}/${POSTGRES_DB:healthdata_patient}
    username: ${POSTGRES_USER:healthdata}
    password: ${POSTGRES_PASSWORD:password}
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2

  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml

  cache:
    type: redis
    redis:
      time-to-live: 300000  # 5 minutes for PHI

  redis:
    host: ${REDIS_HOST:localhost}
    port: ${REDIS_PORT:6380}
    password: ${REDIS_PASSWORD:}

server:
  port: 8084

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

---

## Resources

- **CLAUDE.md**: Complete coding standards
- **Examples**: Look at existing services for patterns
- **Testing**: Use EntityMigrationValidationTest as template
