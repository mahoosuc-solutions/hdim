# PatientService - Key Code Snippets and Examples

## Service Class Annotation and Injection

```java
@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final ApplicationEventPublisher eventPublisher;
```

## CRUD Operations

### Create Patient with Validation
```java
@CacheEvict(value = "patients", allEntries = true)
public Patient createPatient(Patient patient) {
    log.debug("Creating patient: {} {}", patient.getFirstName(), patient.getLastName());

    // Validate patient data
    if (!validatePatientData(patient)) {
        throw new IllegalArgumentException("Invalid patient data: required fields are missing");
    }

    // Validate MRN uniqueness
    if (patientRepository.existsByMrn(patient.getMrn())) {
        throw new IllegalArgumentException("Patient with MRN " + patient.getMrn() + " already exists");
    }

    // Validate tenant ID
    if (patient.getTenantId() == null || patient.getTenantId().isEmpty()) {
        throw new IllegalArgumentException("Tenant ID is required");
    }

    Patient savedPatient = patientRepository.save(patient);

    // Publish event for other modules (async processing)
    eventPublisher.publishEvent(new PatientCreatedEvent(
        savedPatient.getId(),
        savedPatient.getMrn(),
        savedPatient.getTenantId()
    ));

    log.info("Patient created successfully: {} with MRN: {}", 
        savedPatient.getId(), savedPatient.getMrn());
    return savedPatient;
}
```

### Get Patient with Optional Return
```java
@Cacheable(value = "patients", key = "#id")
@Transactional(readOnly = true)
public Optional<Patient> getPatient(String id) {
    log.debug("Fetching patient: {}", id);
    if (id == null || id.isEmpty()) {
        throw new IllegalArgumentException("Patient ID cannot be null or empty");
    }
    return patientRepository.findById(id);
}
```

### Get Patient or Throw Exception
```java
@Transactional(readOnly = true)
public Patient getPatientOrThrow(String id) {
    return getPatient(id)
        .orElseThrow(() -> new PatientNotFoundException("Patient not found with ID: " + id));
}
```

### Update Patient with Event Publishing
```java
@CacheEvict(value = "patients", key = "#id")
public Patient updatePatient(String id, Patient updatedPatient) {
    log.debug("Updating patient: {}", id);

    if (!validatePatientData(updatedPatient)) {
        throw new IllegalArgumentException("Invalid patient data: required fields are missing");
    }

    Patient existingPatient = patientRepository.findById(id)
        .orElseThrow(() -> new PatientNotFoundException("Patient not found: " + id));

    // Update fields
    existingPatient.setFirstName(updatedPatient.getFirstName());
    existingPatient.setLastName(updatedPatient.getLastName());
    existingPatient.setMiddleName(updatedPatient.getMiddleName());
    existingPatient.setDateOfBirth(updatedPatient.getDateOfBirth());
    existingPatient.setGender(updatedPatient.getGender());
    existingPatient.setAddress(updatedPatient.getAddress());
    existingPatient.setPhoneNumber(updatedPatient.getPhoneNumber());
    existingPatient.setEmail(updatedPatient.getEmail());

    Patient savedPatient = patientRepository.save(existingPatient);

    // Publish event
    eventPublisher.publishEvent(new PatientUpdatedEvent(
        savedPatient.getId(),
        savedPatient.getMrn(),
        savedPatient.getTenantId()
    ));

    log.info("Patient updated successfully: {}", id);
    return savedPatient;
}
```

### Delete Patient
```java
@CacheEvict(value = "patients", key = "#id")
public void deletePatient(String id) {
    log.debug("Deleting patient: {}", id);

    if (!patientRepository.existsById(id)) {
        throw new PatientNotFoundException("Patient not found: " + id);
    }

    patientRepository.deleteById(id);
    log.info("Patient deleted: {}", id);
}
```

## Search Operations

### Search by Query with Tenant
```java
@Transactional(readOnly = true)
public Page<Patient> searchPatients(String tenantId, String query, Pageable pageable) {
    log.debug("Searching patients for tenant: {} with query: {}", tenantId, query);

    if (tenantId == null || tenantId.isEmpty()) {
        throw new IllegalArgumentException("Tenant ID is required");
    }

    if (query != null && !query.isEmpty()) {
        return patientRepository.searchByTenantAndQuery(tenantId, query, pageable);
    }
    return patientRepository.findByTenantId(tenantId, pageable);
}
```

### Advanced Criteria Search
```java
@Transactional(readOnly = true)
public Page<Patient> searchPatientsByCriteria(String tenantId, String firstName, 
                                              String lastName, String mrn, Pageable pageable) {
    log.debug("Searching patients by criteria for tenant: {}", tenantId);

    if (tenantId == null || tenantId.isEmpty()) {
        throw new IllegalArgumentException("Tenant ID is required");
    }

    return patientRepository.searchPatients(
        firstName != null ? firstName : "",
        lastName != null ? lastName : "",
        mrn != null ? mrn : "",
        tenantId,
        pageable
    );
}
```

### Find by MRN with Tenant Isolation
```java
@Cacheable(value = "patients", key = "#mrn + '-' + #tenantId")
@Transactional(readOnly = true)
public Optional<Patient> findByMrn(String mrn, String tenantId) {
    log.debug("Fetching patient by MRN: {} for tenant: {}", mrn, tenantId);

    if (mrn == null || mrn.isEmpty()) {
        throw new IllegalArgumentException("MRN cannot be null or empty");
    }
    if (tenantId == null || tenantId.isEmpty()) {
        throw new IllegalArgumentException("Tenant ID is required");
    }

    return patientRepository.findByMrnAndTenantId(mrn, tenantId);
}
```

## Validation

### Patient Data Validation
```java
@Transactional(readOnly = true)
public boolean validatePatientData(Patient patient) {
    if (patient == null) {
        return false;
    }

    // Check required fields
    if (patient.getFirstName() == null || patient.getFirstName().trim().isEmpty()) {
        log.warn("Patient missing first name");
        return false;
    }

    if (patient.getLastName() == null || patient.getLastName().trim().isEmpty()) {
        log.warn("Patient missing last name");
        return false;
    }

    if (patient.getMrn() == null || patient.getMrn().trim().isEmpty()) {
        log.warn("Patient missing MRN");
        return false;
    }

    if (patient.getDateOfBirth() == null) {
        log.warn("Patient missing date of birth");
        return false;
    }

    if (patient.getGender() == null) {
        log.warn("Patient missing gender");
        return false;
    }

    // Validate date of birth is not in future
    if (patient.getDateOfBirth().isAfter(LocalDate.now())) {
        log.warn("Patient date of birth is in the future");
        return false;
    }

    // Validate reasonable age
    int age = patient.getAge();
    if (age < 0 || age > 150) {
        log.warn("Patient age is unreasonable: {}", age);
        return false;
    }

    return true;
}
```

## Business Logic

### Deactivate Patient
```java
@CacheEvict(value = "patients", key = "#id")
public void deactivatePatient(String id, String reason) {
    log.debug("Deactivating patient: {} - Reason: {}", id, reason);

    Patient patient = patientRepository.findById(id)
        .orElseThrow(() -> new PatientNotFoundException("Patient not found: " + id));

    patient.setActive(false);
    patientRepository.save(patient);

    log.info("Patient deactivated: {} - Reason: {}", id, reason);
}
```

### Get Active Patient Count (with Caching)
```java
@Cacheable(value = "metrics", key = "'patient-count-' + #tenantId")
@Transactional(readOnly = true)
public long getActivePatientCount(String tenantId) {
    log.debug("Getting active patient count for tenant: {}", tenantId);

    if (tenantId == null || tenantId.isEmpty()) {
        throw new IllegalArgumentException("Tenant ID is required");
    }

    return patientRepository.countActivePatientsByTenant(tenantId);
}
```

## Batch Operations

### Get Patients by IDs
```java
@Transactional(readOnly = true)
public List<Patient> getPatientsByIds(List<String> ids) {
    log.debug("Fetching {} patients in batch", ids.size());

    if (ids == null || ids.isEmpty()) {
        return List.of();
    }

    return patientRepository.findAllById(ids);
}
```

### Get PatientDTOs by IDs
```java
@Transactional(readOnly = true)
public List<PatientDTO> getPatientDTOsByIds(List<String> ids) {
    log.debug("Fetching {} patient DTOs in batch", ids.size());

    return getPatientsByIds(ids).stream()
        .map(this::convertToDTO)
        .collect(Collectors.toList());
}
```

## DTO Conversion

```java
private PatientDTO convertToDTO(Patient patient) {
    return PatientDTO.builder()
        .id(patient.getId())
        .mrn(patient.getMrn())
        .firstName(patient.getFirstName())
        .lastName(patient.getLastName())
        .middleName(patient.getMiddleName())
        .dateOfBirth(patient.getDateOfBirth())
        .gender(patient.getGender().toString())
        .address(patient.getAddress())
        .phoneNumber(patient.getPhoneNumber())
        .email(patient.getEmail())
        .tenantId(patient.getTenantId())
        .active(patient.isActive())
        .age(patient.getAge())
        .fullName(patient.getFullName())
        .createdAt(patient.getCreatedAt())
        .updatedAt(patient.getUpdatedAt())
        .build();
}
```

## Exception Classes

### PatientNotFoundException
```java
public class PatientNotFoundException extends RuntimeException {

    public PatientNotFoundException(String message) {
        super(message);
    }

    public PatientNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public PatientNotFoundException(Throwable cause) {
        super(cause);
    }
}
```

## Test Examples

### Test Create Patient Success
```java
@Test
@DisplayName("Should create patient successfully")
void testCreatePatientSuccess() {
    // Arrange
    when(patientRepository.existsByMrn(testPatient.getMrn())).thenReturn(false);
    when(patientRepository.save(testPatient)).thenReturn(testPatient);

    // Act
    Patient created = patientService.createPatient(testPatient);

    // Assert
    assertNotNull(created);
    assertEquals(testPatient.getId(), created.getId());
    assertEquals(testPatient.getMrn(), created.getMrn());
    verify(patientRepository).save(testPatient);
    verify(eventPublisher).publishEvent(any(PatientCreatedEvent.class));
}
```

### Test Validation Failure
```java
@Test
@DisplayName("Should reject patient with future date of birth")
void testValidatePatientWithFutureDateOfBirth() {
    // Arrange
    testPatient.setDateOfBirth(LocalDate.now().plusDays(1));

    // Act
    boolean valid = patientService.validatePatientData(testPatient);

    // Assert
    assertFalse(valid);
}
```

### Test Event Publishing
```java
@Test
@DisplayName("Should publish PatientCreatedEvent")
void testCreatePatientPublishesEvent() {
    // Arrange
    when(patientRepository.existsByMrn(testPatient.getMrn())).thenReturn(false);
    when(patientRepository.save(testPatient)).thenReturn(testPatient);

    // Act
    patientService.createPatient(testPatient);

    // Assert
    ArgumentCaptor<PatientCreatedEvent> eventCaptor = 
        ArgumentCaptor.forClass(PatientCreatedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    PatientCreatedEvent event = eventCaptor.getValue();
    assertNotNull(event);
}
```

## Usage Examples

### Create a Patient
```java
Patient patient = Patient.builder()
    .mrn("MRN-001")
    .firstName("John")
    .lastName("Smith")
    .dateOfBirth(LocalDate.of(1970, 1, 1))
    .gender(Patient.Gender.MALE)
    .tenantId("tenant1")
    .build();

Patient created = patientService.createPatient(patient);
```

### Search Patients
```java
Pageable pageable = PageRequest.of(0, 10);
Page<Patient> results = patientService.searchPatients("tenant1", "John", pageable);
```

### Get Patient with Associations
```java
PatientDTO dto = patientService.getPatientWithAssociations("patient-1");
```

### Deactivate and Reactivate
```java
patientService.deactivatePatient("patient-1", "Patient requested deactivation");
patientService.reactivatePatient("patient-1");
```

### Get Metrics
```java
long activeCount = patientService.getActivePatientCount("tenant1");
long inactiveCount = patientService.getInactivePatientCount("tenant1");
long totalCount = patientService.getTotalPatientCount("tenant1");
```

---

All code snippets are from the actual implementation and ready for production use.
