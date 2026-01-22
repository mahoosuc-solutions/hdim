# Clinical Workflow Service - Controller/Service Reconciliation Blueprint

**Status:** CRITICAL - 84 compilation errors require architectural alignment

**Generated:** January 17, 2026
**For:** Medical Assistant Dashboard Phase 2 Implementation

---

## EXECUTIVE SUMMARY

The Clinical Workflow Service has **84 compilation errors** caused by a fundamental mismatch between controller expectations (auto-generated, following HDIM platform standards) and service implementations (hand-written, using simpler design patterns).

**Root Cause:** Controllers were designed to follow HDIM API patterns (tenant-first, userId tracking, pagination, DTO processing) but services were built with minimal parameter sets.

**Solution:** Refactor services to match controller contracts rather than rewriting controllers.

**Estimated Effort:** 8-10 developer days with both Tier 1 (critical) and Tier 2+ (design) fixes.

---

## THE MISMATCH PATTERN

### Pattern 1: Parameter Order Inconsistency

```java
// Controllers ALWAYS pass:
service.operation(String tenantId, <request_or_id>, <userId>)

// But services expect:
service.operation(<UUID_or_request>, String tenantId)
service.operation(String appointmentType, UUID patientId, String tenantId)
service.operation(<UUID>, String appointmentId, String tenantId)
```

**Fix Pattern:**
```java
// Change all service method signatures to match this pattern:
@Transactional
public ResponseEntity<DomainEntity> operation(
    String tenantId,           // Always first
    <IdOrRequestDTO> request,  // Required params
    String userId) {           // Always last for audit
```

### Pattern 2: Missing Adapter Methods

Controllers call methods that don't exist. These need adapter methods that extract data from DTOs.

```java
// Controller calls this (Line 106 in CheckInController):
checkInService.checkInPatient(tenantId, request, userId)

// But service has this:
checkInPatient(UUID patientId, String appointmentId, String tenantId)

// Solution: Add adapter method:
@Transactional
public PatientCheckInEntity checkInPatient(
    String tenantId,
    CheckInRequest request,    // Extract patientId and appointmentId
    String userId) {

    UUID patientId = UUID.fromString(request.getPatientId());
    return checkInPatientInternal(patientId, request.getAppointmentId(), tenantId);
}
```

### Pattern 3: DTO Processing Not Implemented

Request DTOs have 5-8 fields but services ignore most of them.

```java
// CheckInRequest has these fields:
patientId           ✓ Used
appointmentId       ✓ Used
checkInTime         ✗ IGNORED - service uses Instant.now()
insuranceVerified   ✗ IGNORED - service hardcodes false
consentSigned       ✗ IGNORED
demographicsConfirmed ✗ IGNORED
notes               ✗ IGNORED
checkInMethod       ✗ IGNORED

// Fix: Use all request fields
PatientCheckInEntity checkIn = PatientCheckInEntity.builder()
    .tenantId(tenantId)
    .patientId(patientId)
    .appointmentId(request.getAppointmentId())
    .checkInTime(request.getCheckInTime() != null ?
        request.getCheckInTime().atZone(ZoneId.systemDefault()).toInstant() :
        Instant.now())
    .insuranceVerified(request.getInsuranceVerified() != null ?
        request.getInsuranceVerified() : false)
    .consentObtained(request.getConsentSigned() != null ?
        request.getConsentSigned() : false)
    .checkInMethod(request.getCheckInMethod())
    .notes(request.getNotes())
    .build();
```

### Pattern 4: Missing DTO Mapper

Controllers expect service to return domain entities which they wrap in DTOs, but no mapper exists.

```java
// This pattern repeats across all controllers:
VitalSignsResponse response = vitalsService.recordVitalSigns(tenantId, request, userId);
return ResponseEntity.status(HttpStatus.CREATED).body(response);

// But service returns:
public VitalSignsRecordEntity recordVitalSigns(...)
    return vitalsRepository.save(vitals);  // Returns entity, not DTO

// Solution: Add mapper method in service:
private VitalSignsResponse mapToResponse(VitalSignsRecordEntity entity) {
    return VitalSignsResponse.builder()
        .id(entity.getId())
        .patientId(entity.getPatientId().toString())
        .systolicBp(entity.getSystolicBp().intValue())
        .alertStatus(entity.getAlertStatus())
        // ... map all fields
        .build();
}
```

---

## TIER 1 FIX: CRITICAL COMPILATION ERRORS (2.5 days)

These 25+ errors must be fixed for project to compile.

### 1. PatientCheckInService - 7 errors

**Locations:** CheckInController Lines 106, 138, 170, 203, 240, 276, 312

**Fixes Required:**

#### 1a. Add checkInPatient adapter (Line 106)
```java
@Transactional
public PatientCheckInEntity checkInPatient(
        String tenantId,
        CheckInRequest request,
        String userId) {
    log.debug("Checking in patient {} for appointment {} in tenant {}",
            request.getPatientId(), request.getAppointmentId(), tenantId);

    UUID patientId;
    try {
        patientId = UUID.fromString(request.getPatientId());
    } catch (IllegalArgumentException e) {
        throw new ValidationException("Invalid patient ID format: " + request.getPatientId());
    }

    // Check for duplicate
    checkInRepository.findByTenantIdAndAppointmentId(tenantId, request.getAppointmentId())
            .ifPresent(existing -> {
                throw new IllegalStateException(
                        "Patient already checked in for appointment: " + request.getAppointmentId());
            });

    // Extract request fields
    Instant checkInTime = request.getCheckInTime() != null ?
            request.getCheckInTime().atZone(ZoneId.systemDefault()).toInstant() :
            Instant.now();

    PatientCheckInEntity checkIn = PatientCheckInEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .appointmentId(request.getAppointmentId())
            .checkInTime(checkInTime)
            .checkedInBy(userId)
            .status("checked-in")
            .insuranceVerified(request.getInsuranceVerified() != null ?
                    request.getInsuranceVerified() : false)
            .consentObtained(request.getConsentSigned() != null ?
                    request.getConsentSigned() : false)
            .demographicsUpdated(request.getDemographicsConfirmed() != null ?
                    request.getDemographicsConfirmed() : false)
            .notes(request.getNotes())
            .build();

    PatientCheckInEntity saved = checkInRepository.save(checkIn);

    log.info("Patient checked in: {} for appointment {} in tenant {} by user {}",
            saved.getId(), request.getAppointmentId(), tenantId, userId);

    return saved;
}
```

#### 1b. Add getCheckIn method (Line 138)
```java
public PatientCheckInEntity getCheckIn(String tenantId, UUID checkInId) {
    log.debug("Retrieving check-in {} in tenant {}", checkInId, tenantId);

    return checkInRepository.findByIdAndTenantId(checkInId, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException("Check-in", checkInId.toString()));
}
```

#### 1c. Add getTodaysCheckIn method (Line 170)
```java
public PatientCheckInEntity getTodaysCheckIn(String tenantId, String patientId) {
    log.debug("Retrieving today's check-in for patient {} in tenant {}", patientId, tenantId);

    UUID pid = UUID.fromString(patientId);
    LocalDate today = LocalDate.now();
    ZonedDateTime startOfDay = today.atStartOfDay(ZoneId.systemDefault());
    ZonedDateTime endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault());

    return checkInRepository.findTodayCheckIns(
                    tenantId,
                    startOfDay.toInstant(),
                    endOfDay.toInstant())
            .stream()
            .filter(c -> c.getPatientId().equals(pid))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Check-in for patient " + patientId + " today", ""));
}
```

#### 1d. Fix getCheckInHistory signature (Line 203)
```java
public List<PatientCheckInEntity> getCheckInHistory(
        String tenantId,
        String patientId,
        LocalDate startDate,
        LocalDate endDate,
        Pageable pageable) {
    log.debug("Retrieving check-in history for patient {} from {} to {} in tenant {}",
            patientId, startDate, endDate, tenantId);

    UUID pid = UUID.fromString(patientId);

    ZonedDateTime start = startDate != null ?
            startDate.atStartOfDay(ZoneId.systemDefault()) :
            LocalDate.now().minusMonths(12).atStartOfDay(ZoneId.systemDefault());

    ZonedDateTime end = endDate != null ?
            endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()) :
            LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault());

    // TODO: Implement pagination support
    // For now, return unpaged list sorted by check-in time descending
    return checkInRepository.findByTenantIdAndPatientIdAndCheckInTimeBetween(
            tenantId, pid, start.toInstant(), end.toInstant());
}
```

**NOTE:** Add these repository methods to `PatientCheckInRepository`:
```java
Optional<PatientCheckInEntity> findByIdAndTenantId(UUID id, String tenantId);
Optional<PatientCheckInEntity> findByTenantIdAndAppointmentId(String tenantId, String appointmentId);
List<PatientCheckInEntity> findTodayCheckIns(String tenantId, Instant startOfDay, Instant endOfDay);
List<PatientCheckInEntity> findByTenantIdAndPatientIdAndCheckInTimeBetween(
        String tenantId, UUID patientId, Instant start, Instant end);
```

#### 1e. Fix verifyInsurance signature (Line 240)
```java
@Transactional
public PatientCheckInEntity verifyInsurance(
        String tenantId,
        UUID checkInId,
        InsuranceVerificationRequest request,
        String userId) {
    log.debug("Verifying insurance for check-in {} in tenant {}", checkInId, tenantId);

    PatientCheckInEntity checkIn = getCheckIn(tenantId, checkInId);
    checkIn.setInsuranceVerified(true);
    checkIn.setVerifiedBy(userId);
    // Optional: Store insurance details if entity has insurance_provider column
    if (request.getInsuranceProvider() != null) {
        checkIn.setNotes((checkIn.getNotes() != null ? checkIn.getNotes() + "; " : "") +
                "Insurance: " + request.getInsuranceProvider());
    }

    PatientCheckInEntity updated = checkInRepository.save(checkIn);

    log.info("Insurance verified for check-in {} in tenant {}", checkInId, tenantId);

    return updated;
}
```

#### 1f. Rename obtainConsent → recordConsent (Line 276)
```java
@Transactional
public PatientCheckInEntity recordConsent(
        String tenantId,
        UUID checkInId,
        ConsentRequest request,
        String userId) {
    log.debug("Recording consent for check-in {} in tenant {}", checkInId, tenantId);

    PatientCheckInEntity checkIn = getCheckIn(tenantId, checkInId);
    checkIn.setConsentObtained(true);
    checkIn.setConsentObtainedBy(userId);
    if (request.getConsentType() != null) {
        checkIn.setNotes((checkIn.getNotes() != null ? checkIn.getNotes() + "; " : "") +
                "Consent: " + request.getConsentType());
    }

    PatientCheckInEntity updated = checkInRepository.save(checkIn);

    log.info("Consent recorded for check-in {} in tenant {}", checkInId, tenantId);

    return updated;
}
```

#### 1g. Fix updateDemographics signature and parameter order (Line 312)
```java
@Transactional
public PatientCheckInEntity updateDemographics(
        String tenantId,
        UUID checkInId,
        DemographicsUpdateRequest request,
        String userId) {
    log.debug("Updating demographics for check-in {} in tenant {}", checkInId, tenantId);

    PatientCheckInEntity checkIn = getCheckIn(tenantId, checkInId);
    checkIn.setDemographicsUpdated(true);
    checkIn.setDemographicsUpdatedBy(userId);

    // Store updated demographics in notes field
    String demographicsInfo = String.format(
            "Demographics updated: address=%s, phone=%s",
            request.getAddress() != null ? "changed" : "unchanged",
            request.getPhoneNumber() != null ? "changed" : "unchanged");
    checkIn.setNotes((checkIn.getNotes() != null ? checkIn.getNotes() + "; " : "") + demographicsInfo);

    PatientCheckInEntity updated = checkInRepository.save(checkIn);

    log.info("Demographics updated for check-in {} in tenant {}", checkInId, tenantId);

    return updated;
}
```

---

### 2. VitalSignsService - 7 errors

**Locations:** VitalsController Lines 105, 137, 166, 193, 225, 250, 284

**Fixes Required:**

#### 2a. Add recordVitalSigns adapter (Line 105)
```java
@Transactional
public VitalSignsRecordEntity recordVitalSigns(
        String tenantId,
        VitalSignsRequest request,
        String userId) {
    log.debug("Recording vital signs for patient {} in tenant {}",
            request.getPatientId(), tenantId);

    // Convert request parameters
    UUID patientId = UUID.fromString(request.getPatientId());

    // Create internal request with converted types
    com.healthdata.clinicalworkflow.application.VitalSignsService.VitalSignsRequest internalRequest =
        com.healthdata.clinicalworkflow.application.VitalSignsService.VitalSignsRequest.builder()
            .patientId(patientId)
            .encounterId(request.getEncounterId())
            .recordedBy(userId)
            .systolicBp(request.getSystolicBP() != null ?
                    BigDecimal.valueOf(request.getSystolicBP()) : null)
            .diastolicBp(request.getDiastolicBP() != null ?
                    BigDecimal.valueOf(request.getDiastolicBP()) : null)
            .heartRate(request.getHeartRate() != null ?
                    BigDecimal.valueOf(request.getHeartRate()) : null)
            .temperatureF(request.getTemperatureF() != null ?
                    new BigDecimal(request.getTemperatureF().toString()) : null)
            .respirationRate(request.getRespirationRate() != null ?
                    BigDecimal.valueOf(request.getRespirationRate()) : null)
            .oxygenSaturation(request.getOxygenSaturation() != null ?
                    BigDecimal.valueOf(request.getOxygenSaturation()) : null)
            .weightKg(convertPoundsToKg(request.getWeightLbs()))
            .heightCm(convertInchesToCm(request.getHeightInches()))
            .notes(request.getNotes())
            .build();

    return recordVitals(internalRequest, tenantId);
}

private BigDecimal convertPoundsToKg(Double pounds) {
    return pounds != null ?
            BigDecimal.valueOf(pounds * 0.453592).setScale(2, RoundingMode.HALF_UP) : null;
}

private BigDecimal convertInchesToCm(Integer inches) {
    return inches != null ?
            BigDecimal.valueOf(inches * 2.54).setScale(2, RoundingMode.HALF_UP) : null;
}
```

#### 2b. Add getVitalSigns method (Line 137)
```java
public VitalSignsRecordEntity getVitalSigns(String tenantId, UUID vitalsId) {
    return getVitalsById(vitalsId, tenantId);  // Alias to existing method
}
```

#### 2c. Fix getVitalsHistory with pagination (Line 166)
```java
public List<VitalSignsRecordEntity> getVitalsHistory(
        String tenantId,
        String patientId,
        Pageable pageable) {
    UUID pid = UUID.fromString(patientId);
    // TODO: Implement pagination - for now return all, sorted newest first
    return getAllPatientVitals(pid, tenantId);
}
```

#### 2d. Add getVitalAlerts parameter support (Line 193)
```java
public List<VitalSignsRecordEntity> getVitalAlerts(
        String tenantId,
        boolean includeAcknowledged) {
    List<VitalSignsRecordEntity> vitals = getVitalsAlerts(tenantId);

    // TODO: Add acknowledged field to entity to support filter
    // For now, return all alerts
    return vitals;
}
```

#### 2e. Fix getLatestVitals parameter types (Line 225)
```java
public VitalSignsRecordEntity getLatestVitals(String tenantId, String patientId) {
    UUID pid = UUID.fromString(patientId);
    return getLatestVitals(pid, tenantId)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Vital signs for patient " + patientId, ""));
}
```

#### 2f. Add getCriticalAlerts method (Line 250)
```java
public List<VitalSignsRecordEntity> getCriticalAlerts(String tenantId) {
    return vitalsRepository.findByTenantIdAndAlertStatusOrderByRecordedAtDesc(tenantId, "critical");
}
```

#### 2g. Add acknowledgeAlert method (Line 284)
```java
@Transactional
public VitalSignsRecordEntity acknowledgeAlert(
        String tenantId,
        UUID vitalsId,
        String userId) {
    VitalSignsRecordEntity vitals = getVitalsById(vitalsId, tenantId);

    // TODO: Add acknowledged_by and acknowledged_at fields to entity
    // vitals.setAcknowledgedBy(userId);
    // vitals.setAcknowledgedAt(Instant.now());

    return vitalsRepository.save(vitals);
}
```

---

### 3. RoomManagementService - 5 errors

**Locations:** RoomController Lines 68, 100, 178, 214, 248, 282, 316

**Fixes Required:**

#### 3a. Rename getOccupancyBoard → getRoomBoard (Line 68)
```java
@Cacheable(value = "occupancyBoard", key = "#tenantId")
public List<RoomAssignmentEntity> getRoomBoard(String tenantId) {
    return getOccupancyBoard(tenantId);  // Delegate to existing
}
```

#### 3b. Rename/alias getRoomStatus → getRoomDetails (Line 100)
```java
public RoomAssignmentEntity getRoomDetails(String tenantId, String roomNumber) {
    return getRoomStatus(roomNumber, tenantId);  // Swap parameter order
}
```

#### 3c. Fix assignPatientToRoom - needs request DTO processing (Line 178)
```java
@Transactional
public RoomAssignmentEntity assignPatientToRoom(
        String tenantId,
        String roomNumber,
        RoomAssignmentRequest request,
        String userId) {
    log.debug("Assigning patient {} to room {} in tenant {}",
            request.getPatientId(), roomNumber, tenantId);

    UUID patientId = UUID.fromString(request.getPatientId());

    // Use existing assignRoom but with extracted parameters
    return assignRoom(patientId, request.getEncounterId(), tenantId);
}
```

#### 3d. Add updateRoomStatus method (Line 214)
```java
@Transactional
public RoomAssignmentEntity updateRoomStatus(
        String tenantId,
        String roomNumber,
        RoomStatusUpdateRequest request,
        String userId) {
    RoomAssignmentEntity room = getRoomStatus(roomNumber, tenantId);

    switch (request.getStatus().toUpperCase()) {
        case "AVAILABLE":
            markRoomReady(roomNumber, tenantId);
            break;
        case "CLEANING":
            scheduleRoomCleaning(roomNumber, request.getCleaningMinutes() != null ?
                    request.getCleaningMinutes() : 15, tenantId);
            break;
        case "OUT_OF_SERVICE":
            // TODO: Add out-of-service status handling
            room.setStatus("out-of-service");
            break;
    }

    return roomRepository.save(room);
}
```

#### 3e. Fix markRoomReady parameter order (Line 248)
```java
@Transactional
public RoomAssignmentEntity markRoomReady(
        String tenantId,
        String roomNumber,
        String userId) {
    return markRoomReady(roomNumber, tenantId);  // Call existing, ignore userId
}
```

#### 3f. Fix dischargePatient - add patientId requirement (Line 282)
```java
@Transactional
public RoomAssignmentEntity dischargePatient(
        String tenantId,
        String roomNumber,
        String userId) {
    // Get room and discharge the patient in it
    RoomAssignmentEntity room = getRoomStatus(roomNumber, tenantId);
    if (room.getPatientId() == null) {
        throw new IllegalStateException("Room not occupied: " + roomNumber);
    }
    return dischargePatient(roomNumber, room.getPatientId(), tenantId);
}
```

#### 3g. Fix scheduleCleaning - add cleaning minutes (Line 316)
```java
@Transactional
public RoomAssignmentEntity scheduleCleaning(
        String tenantId,
        String roomNumber,
        String userId) {
    return scheduleRoomCleaning(roomNumber, 15, tenantId);  // Default 15 minutes
}
```

---

### 4. WaitingQueueService - 5 errors

**Locations:** QueueController Lines 72, 126, 158, 192, 225, 250, 276, 303

**Fixes Required:**

#### 4a. Fix getQueueStatus return type (Line 72)
```java
public QueueStatus getQueueStatus(String tenantId) {
    // Existing method returns QueueStatus inner class
    // Controller expects QueueStatusResponse - need mapper
    QueueStatus status = getQueueStatus(tenantId);

    return QueueStatusResponse.builder()
            .totalWaiting(status.getTotalWaiting())
            .urgentCount(status.getUrgentCount())
            .averageWaitMinutes(status.getAverageWaitMinutes())
            .build();
}
```

#### 4b. Fix addToQueue adapter (Line 126)
```java
@Transactional
public WaitingQueueEntity addToQueue(
        String tenantId,
        QueueEntryRequest request,
        String userId) {
    UUID patientId = UUID.fromString(request.getPatientId());
    return addToQueueWithPriority(patientId, request.getEncounterId(),
            request.getPriority() != null ? request.getPriority() : "normal",
            tenantId);
}
```

#### 4c. Fix getPatientQueueInfo parameter types (Line 158)
```java
public WaitingQueueEntity getPatientQueueInfo(String tenantId, String patientId) {
    UUID pid = UUID.fromString(patientId);
    return getPatientQueueInfo(pid, tenantId);
}
```

#### 4d. Fix callPatient signature (Line 192)
```java
@Transactional
public WaitingQueueEntity callPatient(String tenantId, String patientId, String userId) {
    UUID pid = UUID.fromString(patientId);
    return callPatient(pid, tenantId);
}
```

#### 4e. Fix removeFromQueue signature (Line 225)
```java
@Transactional
public void removeFromQueue(String tenantId, String patientId, String userId) {
    UUID pid = UUID.fromString(patientId);
    removeFromQueue(pid, tenantId);
}
```

#### 4f. Add getWaitTimes method (Line 250)
```java
public QueueWaitTimeResponse getWaitTimes(String tenantId) {
    // Calculate average wait times by priority
    Integer urgentWait = calculateEstimatedWait(tenantId, "urgent");
    Integer highWait = calculateEstimatedWait(tenantId, "high");
    Integer normalWait = calculateEstimatedWait(tenantId, "normal");
    Integer lowWait = calculateEstimatedWait(tenantId, "low");

    return QueueWaitTimeResponse.builder()
            .urgentWaitMinutes(urgentWait)
            .highWaitMinutes(highWait)
            .normalWaitMinutes(normalWait)
            .lowWaitMinutes(lowWait)
            .build();
}
```

#### 4g. Fix getQueueByPriority return type (Line 276)
```java
public Map<String, List<WaitingQueueEntity>> getQueueByPriority(
        String tenantId,
        String queueType) {
    // Group by priority
    List<WaitingQueueEntity> allEntries = getWaitingPatients(tenantId);

    return allEntries.stream()
            .collect(Collectors.groupingBy(
                    WaitingQueueEntity::getPriority,
                    Collectors.toList()));
}
```

#### 4h. Add reorderQueue method (Line 303)
```java
@Transactional
public QueueStatus reorderQueue(String tenantId, String userId) {
    prioritizeQueue(tenantId);
    return getQueueStatus(tenantId);
}
```

---

### 5. PreVisitChecklistService - 6 errors

**Locations:** PreVisitController Lines 84, 117, 168, 204, 240, 272, 304

**Fixes Required:**

#### 5a. Fix getPatientChecklist parameter types (Line 84)
```java
public PreVisitChecklistEntity getPatientChecklist(String tenantId, String patientId) {
    UUID pid = UUID.fromString(patientId);
    List<PreVisitChecklistEntity> checklists = getChecklistByPatient(pid, tenantId);

    return checklists.stream()
            .filter(c -> "pending".equals(c.getStatus()) || "in_progress".equals(c.getStatus()))
            .findFirst()
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Active checklist for patient " + patientId, ""));
}
```

#### 5b. Add getChecklistTemplate method (Line 117)
```java
@Cacheable(value = "checklistTemplate", key = "#tenantId + ':' + #appointmentType")
public PreVisitChecklistEntity getChecklistTemplate(String tenantId, String appointmentType) {
    // Return a template entity with standard items for the appointment type
    return PreVisitChecklistEntity.builder()
            .tenantId(tenantId)
            .appointmentType(appointmentType)
            .reviewMedicalHistory(false)
            .verifyInsurance(false)
            .updateDemographics(false)
            .reviewMedications(false)
            .reviewAllergies(false)
            .prepareVitalsEquipment(false)
            .reviewCareGaps(false)
            .obtainConsent(false)
            .status("template")
            .build();
}
```

#### 5c. Fix createChecklist adapter (Line 168)
```java
@Transactional
public PreVisitChecklistEntity createChecklist(
        String tenantId,
        CreateChecklistRequest request,
        String userId) {
    UUID patientId = UUID.fromString(request.getPatientId());

    return createChecklistForAppointment(
            request.getAppointmentType(),
            patientId,
            request.getEncounterId(),
            tenantId);
}
```

#### 5d. Fix completeChecklistItem - extract field from request (Line 204)
```java
@Transactional
public PreVisitChecklistEntity completeChecklistItem(
        String tenantId,
        UUID checklistId,
        ChecklistItemUpdateRequest request,
        String userId) {
    return completeChecklistItem(checklistId, request.getItemName(), tenantId);
}
```

#### 5e. Fix addCustomItem - extract field from request (Line 240)
```java
@Transactional
public PreVisitChecklistEntity addCustomItem(
        String tenantId,
        UUID checklistId,
        CustomChecklistItemRequest request,
        String userId) {
    return addCustomItem(checklistId, request.getTaskName(), tenantId);
}
```

#### 5f. Fix getChecklistProgress parameter order (Line 272)
```java
public ChecklistProgress getChecklistProgress(String tenantId, UUID checklistId) {
    return getChecklistProgress(checklistId, tenantId);
}
```

#### 5g. Add getIncompleteCriticalItems method (Line 304)
```java
public List<ChecklistItemResponse> getIncompleteCriticalItems(String tenantId, UUID checklistId) {
    PreVisitChecklistEntity checklist = getChecklistById(checklistId, tenantId);

    List<ChecklistItemResponse> criticalItems = new ArrayList<>();

    if (!checklist.getReviewMedicalHistory()) {
        criticalItems.add(createItemResponse("Review Medical History", false));
    }
    if (!checklist.getObtainConsent()) {
        criticalItems.add(createItemResponse("Obtain Consent", false));
    }
    // ... add other critical items

    return criticalItems;
}

private ChecklistItemResponse createItemResponse(String name, boolean completed) {
    return ChecklistItemResponse.builder()
            .name(name)
            .completed(completed)
            .required(true)
            .build();
}
```

---

## TIER 2 FIX: DTO MAPPING LAYER (2.5 days)

After Tier 1 compilation fixes, implement DTO mappers for all 5 services.

### Option A: Manual Mappers in Services

```java
// In PatientCheckInService:
private CheckInResponse mapToResponse(PatientCheckInEntity entity) {
    return CheckInResponse.builder()
            .id(entity.getId())
            .patientId(entity.getPatientId().toString())
            .appointmentId(entity.getAppointmentId())
            .checkInTime(LocalDateTime.ofInstant(entity.getCheckInTime(), ZoneId.systemDefault()))
            .status(entity.getStatus())
            .insuranceVerified(entity.getInsuranceVerified())
            .consentObtained(entity.getConsentObtained())
            .demographicsUpdated(entity.getDemographicsUpdated())
            .build();
}
```

### Option B: MapStruct Mappers (Recommended)

Add dependency to `build.gradle.kts`:
```kotlin
implementation("org.mapstruct:mapstruct:1.5.3.Final")
annotationProcessor("org.mapstruct:mapstruct-processor:1.5.3.Final")
```

Create mapper interfaces:
```java
@Mapper(componentModel = "spring")
public interface PatientCheckInMapper {
    CheckInResponse toResponse(PatientCheckInEntity entity);
    List<CheckInResponse> toResponseList(List<PatientCheckInEntity> entities);
}
```

---

## REPOSITORY METHODS TO ADD

All services call repository methods that may not exist. Add to respective repositories:

```java
// PatientCheckInRepository
Optional<PatientCheckInEntity> findByIdAndTenantId(UUID id, String tenantId);
Optional<PatientCheckInEntity> findByTenantIdAndAppointmentId(String tenantId, String appointmentId);
List<PatientCheckInEntity> findTodayCheckIns(String tenantId, Instant startOfDay, Instant endOfDay);
List<PatientCheckInEntity> findByTenantIdAndPatientIdAndCheckInTimeBetween(
        String tenantId, UUID patientId, Instant start, Instant end);

// VitalSignsRecordRepository
Optional<VitalSignsRecordEntity> findByIdAndTenantId(UUID id, String tenantId);
List<VitalSignsRecordEntity> findByTenantIdAndAlertStatusOrderByRecordedAtDesc(String tenantId, String alertStatus);
Optional<VitalSignsRecordEntity> findLatestVitalForPatient(UUID patientId, String tenantId);
long countCriticalAlertsByTenant(String tenantId);

// RoomAssignmentRepository
Optional<RoomAssignmentEntity> findByIdAndTenantId(UUID id, String tenantId);
List<RoomAssignmentEntity> findOccupancyBoard(String tenantId);
List<RoomAssignmentEntity> findCurrentOccupantsByTenant(String tenantId);

// WaitingQueueRepository
Optional<WaitingQueueEntity> findByIdAndTenantId(UUID id, String tenantId);
Optional<WaitingQueueEntity> findNextPatientInQueue(String tenantId);
List<WaitingQueueEntity> findQueueByPriority(String tenantId, String priority);

// PreVisitChecklistRepository
Optional<PreVisitChecklistEntity> findByIdAndTenantId(UUID id, String tenantId);
Optional<PreVisitChecklistEntity> findChecklistByAppointmentId(String appointmentId, String tenantId);
List<PreVisitChecklistEntity> findIncompleteChecklistsByTenant(String tenantId);
```

---

## MISSING ENTITY FIELDS

Services reference entity fields that may not exist. Add to domain entities:

```java
// PatientCheckInEntity
private String checkedInBy;              // User ID who checked in patient
private String verifiedBy;               // User who verified insurance
private String consentObtainedBy;        // User who obtained consent
private String demographicsUpdatedBy;    // User who updated demographics
private String checkInMethod;            // FRONT_DESK, KIOSK, MOBILE_APP

// VitalSignsRecordEntity
private String acknowledgedBy;           // User who acknowledged alert
private Instant acknowledgedAt;          // When alert was acknowledged

// RoomAssignmentEntity
// (Already has most fields - verify these exist)

// WaitingQueueEntity
private Instant calledAt;                // When patient was called
private Instant exitedQueueAt;           // When patient exited queue

// PreVisitChecklistEntity
private UUID appointmentId;              // Link to appointment
```

---

## PARAMETER TYPE CONVERSION HELPERS

Add utility methods to handle common conversions:

```java
public class ConversionUtils {
    public static UUID toUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid UUID format: " + id);
        }
    }

    public static Instant toInstant(LocalDateTime ldt) {
        return ldt != null ?
                ldt.atZone(ZoneId.systemDefault()).toInstant() : Instant.now();
    }

    public static LocalDateTime fromInstant(Instant instant) {
        return instant != null ?
                LocalDateTime.ofInstant(instant, ZoneId.systemDefault()) : null;
    }

    public static BigDecimal convertPoundsToKg(Double pounds) {
        return pounds != null ?
                BigDecimal.valueOf(pounds * 0.453592).setScale(2, RoundingMode.HALF_UP) : null;
    }
}
```

---

## IMPLEMENTATION CHECKLIST

### Week 1: Tier 1 Fixes
- [ ] Fix PatientCheckInService (7 methods)
- [ ] Fix VitalSignsService (7 methods)
- [ ] Fix RoomManagementService (7 methods)
- [ ] Fix WaitingQueueService (8 methods)
- [ ] Fix PreVisitChecklistService (7 methods)
- [ ] Add missing repository methods (30+ queries)
- [ ] Verify compilation: `./gradlew compileJava`

### Week 2: Tier 2 Fixes
- [ ] Implement DTO mappers (5 services × 3-4 mappers each)
- [ ] Update controller return types to use mappers
- [ ] Add pagination support to services
- [ ] Test DTO round-trips

### Week 3: Tier 3 Fixes
- [ ] Process all request DTO fields properly
- [ ] Add userId audit tracking to all services
- [ ] Add missing entity fields
- [ ] Implement filtering/sorting

### Week 4: Testing & Validation
- [ ] Write integration tests for each endpoint
- [ ] Test multi-tenant isolation
- [ ] Verify cache TTL compliance (≤5 minutes)
- [ ] Performance testing with large datasets

---

## NEXT STEPS

1. **Apply Tier 1 Fixes** - Pick one service and apply all fixes, test compilation
2. **Replicate Pattern** - Once first service compiles, replicate pattern across remaining 4 services
3. **Create Mapper Framework** - Set up MapStruct or manual mappers based on team preference
4. **Integration Testing** - Build end-to-end tests for MA workflows
5. **Phase 3 Planning** - Once Phase 2 validates, begin Scheduling Service

---

## REFERENCE LINKS

- HDIM API Specification: `/BACKEND_API_SPECIFICATION.md`
- Service Pattern Guide: `CLAUDE.md` (Controller Pattern section)
- Entity-Migration Sync: `/backend/docs/ENTITY_MIGRATION_GUIDE.md`
- HIPAA Compliance: `/backend/HIPAA-CACHE-COMPLIANCE.md`

---

**Document Version:** 1.0
**Last Updated:** January 17, 2026
**Status:** Ready for Implementation
