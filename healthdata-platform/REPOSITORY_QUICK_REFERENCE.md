# Repository Quick Reference Guide

## File Locations

```
src/main/java/com/healthdata/
├── patient/repository/
│   └── PatientRepository.java
├── fhir/repository/
│   ├── ObservationRepository.java
│   ├── ConditionRepository.java
│   └── MedicationRequestRepository.java
├── quality/repository/
│   └── QualityMeasureResultRepository.java
├── caregap/repository/
│   └── CareGapRepository.java
└── shared/security/repository/
    └── AuditLogRepository.java
```

## Quick Method Reference

### PatientRepository
```java
// Search and retrieval
Optional<Patient> findByMrnAndTenantId(String mrn, String tenantId);
List<Patient> findByFirstNameAndLastNameAndTenantId(String firstName, String lastName, String tenantId);
Page<Patient> searchPatients(String firstName, String lastName, String mrn, String tenantId, Pageable pageable);

// List operations
Page<Patient> findAllActivePatientsForTenant(String tenantId, Pageable pageable);
long countActivePatientsByTenant(String tenantId);
```

### ObservationRepository
```java
// Retrieve observations
List<Observation> findByPatientIdAndCode(String patientId, String code);
Optional<Observation> findLatestByPatientIdAndCode(String patientId, String code);
List<Observation> findByPatientIdAndDateRange(String patientId, LocalDateTime startDate, LocalDateTime endDate);

// Category-based queries
Page<Observation> findByPatientIdAndCategoryOrderByEffectiveDateDesc(String patientId, String category, Pageable pageable);

// Metrics
long countByPatientIdAndCategory(String patientId, String category);
```

### ConditionRepository
```java
// Condition retrieval
List<Condition> findByPatientId(String patientId);
List<Condition> findActiveConditionsByPatientId(String patientId);
List<Condition> findByPatientIdAndCode(String patientId, String code);

// Advanced queries
List<Condition> findSevereActiveConditions(String patientId);
Page<Condition> findRecentByPatientId(String patientId, Pageable pageable);

// Metrics
long countActiveConditionsByTenant(String tenantId);
```

### MedicationRequestRepository
```java
// Medication retrieval
List<MedicationRequest> findByPatientId(String patientId);
List<MedicationRequest> findActiveByPatientId(String patientId);
List<MedicationRequest> findByPatientIdAndCode(String patientId, String code);

// Special queries
List<MedicationRequest> findMedicationsExpiringWithinDays(String patientId, int daysUntilExpiry);
List<MedicationRequest> findActiveMedicationsByTenantAndCondition(String tenantId, String conditionCode);

// Updates
void updateMedicationStatus(String patientId, String oldStatus, String newStatus);
```

### QualityMeasureResultRepository
```java
// Result retrieval
List<MeasureResult> findByPatientIdAndMeasureId(String patientId, String measureId);
Page<MeasureResult> findByMeasureIdAndTenant(String measureId, String tenantId, Pageable pageable);
Page<MeasureResult> findLatestResultsByPatient(String patientId, Pageable pageable);

// Compliance queries
List<MeasureResult> findCompliantResultsByPatient(String patientId);
List<MeasureResult> findNonCompliantResultsByPatient(String patientId);
Page<MeasureResult> findByTenantIdAndCompliant(String tenantId, boolean compliant, Pageable pageable);

// Aggregations
Object[] aggregateMeasureResults(String measureId, String tenantId);
Double getComplianceRate(String measureId, String tenantId);
Double getAverageScoreByMeasure(String measureId, String tenantId);
Page<MeasureResult> findTopPerformers(String tenantId, Pageable pageable);
```

### CareGapRepository
```java
// Gap retrieval
List<CareGap> findByPatientId(String patientId);
List<CareGap> findOpenGapsByPatient(String patientId);
Page<CareGap> findGapsByTypeAndPriority(String gapType, String priority, String tenantId, Pageable pageable);

// Priority queries
List<CareGap> findOverdueGaps(String tenantId);
List<CareGap> findHighRiskGaps(Double minScore);
List<CareGap> findGapsDueSoon(String tenantId, int daysUntilDue);

// Metrics
long countGapsByStatusAndTenant(String status, String tenantId);
Double getTotalFinancialImpact(String tenantId);

// Updates
void updateGapStatus(String gapId, String newStatus, String closureReason);
void updateGapsByPatientAndType(String patientId, String gapType, String newStatus);
```

### AuditLogRepository
```java
// Audit log retrieval
Page<AuditLog> findByUserId(String userId, Pageable pageable);
Page<AuditLog> findByEntityType(String entityType, Pageable pageable);
Page<AuditLog> findByDateRange(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
Page<AuditLog> findChangesByEntity(String entityType, String entityId, Pageable pageable);

// Specific queries
List<AuditLog> findSensitiveActions(String tenantId);
List<AuditLog> findFailedAccessAttempts(LocalDateTime startDate, LocalDateTime endDate);
List<AuditLog> findPatientAccessLogs(String patientId);
List<AuditLog> findDeletionAudit(String entityType, LocalDateTime startDate, LocalDateTime endDate);

// Forensics
List<AuditLog> findRapidAccessAttempts(String userId, int secondsWindow);
List<AuditLog> findRecentSensitiveActions(String tenantId, int minutesAgo);
```

## Common Usage Patterns

### Search with Pagination
```java
@GetMapping("/patients/search")
public ResponseEntity<Page<Patient>> searchPatients(
    @RequestParam String firstName,
    @RequestParam String lastName,
    @RequestParam String mrn,
    @RequestParam int page,
    @RequestParam int size,
    @RequestHeader String tenantId) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("lastName").ascending());
    return ResponseEntity.ok(
        patientRepository.searchPatients(firstName, lastName, mrn, tenantId, pageable)
    );
}
```

### Get Latest Record
```java
Optional<Observation> latestBloodPressure = observationRepository
    .findLatestByPatientIdAndCode(patientId, "85354-9"); // BP LOINC code

if (latestBloodPressure.isPresent()) {
    // Use the observation
}
```

### Track Medication Status
```java
// Mark all active medications as on-hold
medicationRepository.updateMedicationStatus(patientId, "active", "on-hold");
```

### Get Care Gap Metrics
```java
// Count open gaps
long openGaps = careGapRepository.countGapsByStatusAndTenant("OPEN", tenantId);

// Calculate financial impact
Double totalImpact = careGapRepository.getTotalFinancialImpact(tenantId);

// Find overdue gaps for outreach
List<CareGap> overdueGaps = careGapRepository.findOverdueGaps(tenantId);
```

### Quality Measure Analytics
```java
// Get compliance rate
Double complianceRate = measureResultRepository.getComplianceRate("HEDIS-CDC", tenantId);

// Find top performing organizations
Page<MeasureResult> topPerformers = measureResultRepository
    .findTopPerformers(tenantId, PageRequest.of(0, 10));

// Get non-compliant patients for outreach
Page<MeasureResult> nonCompliant = measureResultRepository
    .findByTenantIdAndCompliant(tenantId, false, PageRequest.of(0, 50));
```

### HIPAA Compliance Auditing
```java
// Get all access to a patient
List<AuditLog> accessLogs = auditLogRepository.findPatientAccessLogs(patientId);

// Find deletions in timeframe
LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
List<AuditLog> deletions = auditLogRepository.findDeletionAudit(
    "Observation", thirtyDaysAgo, LocalDateTime.now()
);

// Monitor failed access attempts
LocalDateTime lastHour = LocalDateTime.now().minusHours(1);
List<AuditLog> failedAttempts = auditLogRepository
    .findFailedAccessAttempts(lastHour, LocalDateTime.now());
```

## Data Model Relationships

```
Patient (1) ---- (*) Observation
           ---- (*) Condition
           ---- (*) MedicationRequest
           ---- (*) CareGap
           ---- (*) MeasureResult

Tenant (1) ---- (*) Patient
       ---- (*) CareGap
       ---- (*) MeasureResult
       ---- (*) AuditLog

MeasureResult is linked to CareGap via measureId
```

## Important Notes

1. **Tenant Isolation**: All queries automatically filter by tenantId
2. **Pagination**: Use for any list with > 100 expected records
3. **Optional**: Always handle Optional results from single-record queries
4. **Sorting**: Default sorting provided by most pageable queries
5. **Transactions**: @Modifying operations are automatically transactional

## Performance Tips

1. Always use pagination for population-level queries
2. Use indexes on tenantId, patientId, status fields
3. Cache frequently accessed metrics
4. Consider batch processing for bulk updates
5. Monitor slow query logs for optimization

## Example Service Implementation

```java
@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientRepository patientRepository;

    public Page<Patient> searchPatients(
        String firstName, String lastName, String mrn,
        String tenantId, int page, int size) {

        Pageable pageable = PageRequest.of(
            page, size, Sort.by("lastName", "firstName").ascending()
        );
        return patientRepository.searchPatients(
            firstName, lastName, mrn, tenantId, pageable
        );
    }

    public long getActivePatientCount(String tenantId) {
        return patientRepository.countActivePatientsByTenant(tenantId);
    }
}
```

## Troubleshooting

| Issue | Solution |
|-------|----------|
| No results | Check tenantId parameter, verify patient is active |
| Slow queries | Add database indexes, use pagination |
| Out of memory | Use pagination instead of loading all results |
| Null pointer | Use Optional.orElse() or orElseThrow() |
| Transaction issues | Ensure @Transactional on service methods |

---

For detailed documentation, see `REPOSITORIES_IMPLEMENTATION.md`
