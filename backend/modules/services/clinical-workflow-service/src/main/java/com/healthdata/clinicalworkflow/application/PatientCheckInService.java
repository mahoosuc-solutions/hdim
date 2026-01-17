package com.healthdata.clinicalworkflow.application;

import com.healthdata.clinicalworkflow.domain.model.PatientCheckInEntity;
import com.healthdata.clinicalworkflow.domain.repository.PatientCheckInRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Patient Check-In Service
 *
 * Manages patient check-in workflow for Medical Assistants:
 * - Patient check-in process
 * - Insurance verification
 * - Demographics updates
 * - Consent collection
 * - Wait time calculation
 *
 * HIPAA Compliance:
 * - All methods enforce multi-tenant isolation
 * - Audit logging via @Audited annotations (to be added at controller level)
 * - Cache TTL <= 5 minutes for PHI data
 *
 * Integration Points:
 * - FHIR Appointment: Links to appointment resource
 * - FHIR Encounter: Creates encounter on check-in
 * - WaitingQueueService: Adds patient to queue after check-in
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PatientCheckInService {

    private final PatientCheckInRepository checkInRepository;

    /**
     * Check in patient
     *
     * Main workflow for patient check-in. Creates check-in record and
     * initializes workflow state.
     *
     * @param patientId the patient ID
     * @param appointmentId the appointment ID (optional)
     * @param tenantId the tenant ID (HIPAA §164.312(d))
     * @return created check-in record
     */
    @Transactional
    public PatientCheckInEntity checkInPatient(UUID patientId, String appointmentId, String tenantId) {
        log.debug("Checking in patient {} for appointment {} in tenant {}",
                patientId, appointmentId, tenantId);

        // Check if patient already checked in for this appointment
        if (appointmentId != null) {
            checkInRepository.findByTenantIdAndAppointmentId(tenantId, appointmentId)
                    .ifPresent(existing -> {
                        throw new IllegalStateException(
                                "Patient already checked in for appointment: " + appointmentId);
                    });
        }

        PatientCheckInEntity checkIn = PatientCheckInEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .appointmentId(appointmentId)
                .checkInTime(Instant.now())
                .checkedInBy("system") // Will be set by controller from auth context
                .status("checked-in")
                .insuranceVerified(false)
                .demographicsUpdated(false)
                .consentObtained(false)
                .build();

        PatientCheckInEntity saved = checkInRepository.save(checkIn);

        log.info("Patient checked in: {} for patient {} in tenant {}",
                saved.getId(), patientId, tenantId);

        return saved;
    }

    /**
     * Verify insurance
     *
     * Mark insurance as verified for check-in.
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @return updated check-in record
     */
    @Transactional
    public PatientCheckInEntity verifyInsurance(UUID patientId, String tenantId) {
        log.debug("Verifying insurance for patient {} in tenant {}", patientId, tenantId);

        List<PatientCheckInEntity> checkIns = checkInRepository
                .findByTenantIdAndPatientIdOrderByCheckInTimeDesc(tenantId, patientId);

        if (checkIns.isEmpty()) {
            throw new IllegalStateException("No check-in record found for patient: " + patientId);
        }

        PatientCheckInEntity checkIn = checkIns.get(0); // Get most recent
        checkIn.setInsuranceVerified(true);

        PatientCheckInEntity updated = checkInRepository.save(checkIn);

        log.info("Insurance verified for patient {} in tenant {}", patientId, tenantId);

        return updated;
    }

    /**
     * Obtain consent
     *
     * Mark consent as obtained for check-in.
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @return updated check-in record
     */
    @Transactional
    public PatientCheckInEntity obtainConsent(UUID patientId, String tenantId) {
        log.debug("Obtaining consent for patient {} in tenant {}", patientId, tenantId);

        List<PatientCheckInEntity> checkIns = checkInRepository
                .findByTenantIdAndPatientIdOrderByCheckInTimeDesc(tenantId, patientId);

        if (checkIns.isEmpty()) {
            throw new IllegalStateException("No check-in record found for patient: " + patientId);
        }

        PatientCheckInEntity checkIn = checkIns.get(0);
        checkIn.setConsentObtained(true);

        PatientCheckInEntity updated = checkInRepository.save(checkIn);

        log.info("Consent obtained for patient {} in tenant {}", patientId, tenantId);

        return updated;
    }

    /**
     * Update demographics
     *
     * Mark demographics as updated and store any changes.
     *
     * @param patientId the patient ID
     * @param demographics the demographics updates (for logging purposes)
     * @param tenantId the tenant ID
     * @return updated check-in record
     */
    @Transactional
    public PatientCheckInEntity updateDemographics(
            UUID patientId, Map<String, Object> demographics, String tenantId) {
        log.debug("Updating demographics for patient {} in tenant {}", patientId, tenantId);

        List<PatientCheckInEntity> checkIns = checkInRepository
                .findByTenantIdAndPatientIdOrderByCheckInTimeDesc(tenantId, patientId);

        if (checkIns.isEmpty()) {
            throw new IllegalStateException("No check-in record found for patient: " + patientId);
        }

        PatientCheckInEntity checkIn = checkIns.get(0);
        checkIn.setDemographicsUpdated(true);

        // Store demographics updates in notes if provided
        if (demographics != null && !demographics.isEmpty()) {
            String notes = checkIn.getNotes() != null ? checkIn.getNotes() + "\n" : "";
            notes += "Demographics updated: " + demographics.toString();
            checkIn.setNotes(notes);
        }

        PatientCheckInEntity updated = checkInRepository.save(checkIn);

        log.info("Demographics updated for patient {} in tenant {}", patientId, tenantId);

        return updated;
    }

    /**
     * Calculate waiting time
     *
     * Calculate time since check-in (if still waiting) or
     * return stored wait time (if already seen).
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @return waiting time in minutes
     */
    @Cacheable(value = "waitingTimes", key = "#tenantId + ':' + #patientId")
    public Integer calculateWaitingTime(UUID patientId, String tenantId) {
        log.debug("Calculating waiting time for patient {} in tenant {}", patientId, tenantId);

        List<PatientCheckInEntity> checkIns = checkInRepository
                .findByTenantIdAndPatientIdOrderByCheckInTimeDesc(tenantId, patientId);

        if (checkIns.isEmpty()) {
            return 0;
        }

        PatientCheckInEntity checkIn = checkIns.get(0);

        // If wait time already calculated, return it
        if (checkIn.getWaitingTimeMinutes() != null) {
            return checkIn.getWaitingTimeMinutes();
        }

        // Calculate current wait time
        long waitMinutes = Duration.between(checkIn.getCheckInTime(), Instant.now()).toMinutes();

        return (int) waitMinutes;
    }

    /**
     * Get check-in history for patient
     *
     * Retrieves all check-in records for patient, ordered by most recent.
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @return list of check-in records
     */
    public List<PatientCheckInEntity> getCheckInHistory(UUID patientId, String tenantId) {
        log.debug("Retrieving check-in history for patient {} in tenant {}", patientId, tenantId);

        return checkInRepository.findByTenantIdAndPatientIdOrderByCheckInTimeDesc(
                tenantId, patientId);
    }

    /**
     * Find today's check-ins
     *
     * Retrieves all check-ins for today in tenant.
     *
     * @param tenantId the tenant ID
     * @return list of today's check-ins
     */
    @Cacheable(value = "todayCheckIns", key = "#tenantId")
    public List<PatientCheckInEntity> findTodayCheckIns(String tenantId) {
        log.debug("Finding today's check-ins for tenant {}", tenantId);

        // Get start and end of day in system timezone
        ZonedDateTime startOfDay = LocalDate.now()
                .atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime endOfDay = startOfDay.plusDays(1);

        return checkInRepository.findTodayCheckIns(
                tenantId,
                startOfDay.toInstant(),
                endOfDay.toInstant());
    }

    /**
     * Get check-in by ID
     *
     * @param checkInId the check-in ID
     * @param tenantId the tenant ID
     * @return check-in record
     */
    public PatientCheckInEntity getCheckInById(UUID checkInId, String tenantId) {
        log.debug("Retrieving check-in {} in tenant {}", checkInId, tenantId);

        return checkInRepository.findByIdAndTenantId(checkInId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Check-in not found: " + checkInId));
    }

    /**
     * Get check-in by appointment ID
     *
     * @param appointmentId the appointment ID
     * @param tenantId the tenant ID
     * @return check-in record
     */
    public PatientCheckInEntity getCheckInByAppointment(String appointmentId, String tenantId) {
        log.debug("Retrieving check-in for appointment {} in tenant {}", appointmentId, tenantId);

        return checkInRepository.findByTenantIdAndAppointmentId(tenantId, appointmentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Check-in not found for appointment: " + appointmentId));
    }

    /**
     * Count check-ins for patient
     *
     * @param patientId the patient ID
     * @param tenantId the tenant ID
     * @return count of check-ins
     */
    public long countCheckIns(UUID patientId, String tenantId) {
        return checkInRepository.countByTenantIdAndPatientId(tenantId, patientId);
    }

    /**
     * Get check-ins by status
     *
     * @param status the status filter
     * @param tenantId the tenant ID
     * @return list of check-ins with status
     */
    public List<PatientCheckInEntity> getCheckInsByStatus(String status, String tenantId) {
        log.debug("Retrieving check-ins with status {} in tenant {}", status, tenantId);

        return checkInRepository.findByTenantIdAndStatusOrderByCheckInTimeDesc(tenantId, status);
    }

    /**
     * Update check-in status
     *
     * @param checkInId the check-in ID
     * @param status the new status
     * @param tenantId the tenant ID
     * @return updated check-in
     */
    @Transactional
    public PatientCheckInEntity updateStatus(UUID checkInId, String status, String tenantId) {
        log.debug("Updating check-in {} status to {} in tenant {}", checkInId, status, tenantId);

        PatientCheckInEntity checkIn = getCheckInById(checkInId, tenantId);
        checkIn.setStatus(status);

        PatientCheckInEntity updated = checkInRepository.save(checkIn);

        log.info("Check-in {} status updated to {} in tenant {}", checkInId, status, tenantId);

        return updated;
    }
}
