package com.healthdata.clinicalworkflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.clinicalworkflow.api.v1.dto.*;
import com.healthdata.clinicalworkflow.domain.model.PatientCheckInEntity;
import com.healthdata.clinicalworkflow.domain.repository.PatientCheckInRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive integration tests for Patient Check-In Service (Tier 3 - Validation)
 *
 * Tests complete check-in workflow end-to-end including:
 * - Patient check-in creation
 * - Insurance verification
 * - Consent collection
 * - Demographics confirmation
 * - Multi-tenant isolation
 * - Audit trail verification
 * - Error scenarios
 * - History and pagination
 *
 * @author HDIM Platform Team
 * @version 1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@Transactional
@DisplayName("Patient Check-In Integration Tests")
class PatientCheckInIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("clinical_workflow_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PatientCheckInRepository checkInRepository;

    private static final String TENANT_ID_A = "TENANT_A";
    private static final String TENANT_ID_B = "TENANT_B";
    private static final String USER_ID = "user@example.com";
    private static final String PATIENT_ID = UUID.randomUUID().toString();
    private static final String APPOINTMENT_ID = "APPT001";

    @BeforeEach
    void setUp() {
        checkInRepository.deleteAll();
    }

    // ================================
    // Complete Check-In Workflow Tests
    // ================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Complete check-in workflow: create → verify insurance → obtain consent → update demographics")
    void testCompleteCheckInWorkflow() throws Exception {
        // Step 1: Create initial check-in
        CheckInRequest checkInRequest = CheckInRequest.builder()
                .patientId(PATIENT_ID)
                .appointmentId(APPOINTMENT_ID)
                .checkInTime(LocalDateTime.now())
                .insuranceVerified(false)
                .consentSigned(false)
                .demographicsConfirmed(false)
                .checkInMethod("FRONT_DESK")
                .build();

        MvcResult checkInResult = mockMvc.perform(post("/api/v1/check-in")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkInRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID))
                .andExpect(jsonPath("$.appointmentId").value(APPOINTMENT_ID))
                .andExpect(jsonPath("$.insuranceVerified").value(false))
                .andExpect(jsonPath("$.consentSigned").value(false))
                .andExpect(jsonPath("$.demographicsConfirmed").value(false))
                .andReturn();

        String checkInResponseJson = checkInResult.getResponse().getContentAsString();
        CheckInResponse checkInResponse = objectMapper.readValue(checkInResponseJson, CheckInResponse.class);
        UUID checkInId = checkInResponse.getId();

        // Verify entity in database
        Optional<PatientCheckInEntity> checkInEntityOpt = checkInRepository.findById(checkInId);
        assertThat(checkInEntityOpt).isPresent();
        PatientCheckInEntity checkInEntity = checkInEntityOpt.get();
        assertThat(checkInEntity.getTenantId()).isEqualTo(TENANT_ID_A);
        assertThat(checkInEntity.getCheckedInBy()).isEqualTo(USER_ID);
        assertThat(checkInEntity.getCheckInTime()).isNotNull();

        // Step 2: Verify insurance
        InsuranceVerificationRequest insuranceRequest = InsuranceVerificationRequest.builder()
                .verified(true)
                .insuranceProvider("Blue Cross")
                .memberId("BC123456")
                .groupNumber("GRP123")
                .verificationNotes("Insurance verified, active coverage")
                .build();

        mockMvc.perform(put("/api/v1/check-in/{id}/insurance", checkInId)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insuranceRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.insuranceVerified").value(true));

        // Verify entity updated
        checkInEntity = checkInRepository.findById(checkInId).orElseThrow();
        assertThat(checkInEntity.getInsuranceVerified()).isTrue();
        assertThat(checkInEntity.getVerifiedBy()).isEqualTo(USER_ID);

        // Step 3: Obtain consent
        ConsentRequest consentRequest = ConsentRequest.builder()
                .consentObtained(true)
                .consentType("TREATMENT")
                .signatureCaptured(true)
                .consentSignedAt(LocalDateTime.now())
                .notes("Patient signed electronically")
                .build();

        mockMvc.perform(put("/api/v1/check-in/{id}/consent", checkInId)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(consentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consentSigned").value(true));

        // Verify entity updated
        checkInEntity = checkInRepository.findById(checkInId).orElseThrow();
        assertThat(checkInEntity.getConsentObtained()).isTrue();
        assertThat(checkInEntity.getConsentObtainedBy()).isEqualTo(USER_ID);

        // Step 4: Update demographics
        DemographicsUpdateRequest demographicsRequest = DemographicsUpdateRequest.builder()
                .confirmed(true)
                .addressChanged(false)
                .phoneChanged(true)
                .updateNotes("Updated phone number")
                .build();

        mockMvc.perform(put("/api/v1/check-in/{id}/demographics", checkInId)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demographicsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.demographicsConfirmed").value(true));

        // Verify final entity state
        checkInEntity = checkInRepository.findById(checkInId).orElseThrow();
        assertThat(checkInEntity.getDemographicsUpdated()).isTrue();
        assertThat(checkInEntity.getDemographicsUpdatedBy()).isEqualTo(USER_ID);

        // Verify all workflow steps completed
        assertThat(checkInEntity.getInsuranceVerified()).isTrue();
        assertThat(checkInEntity.getConsentObtained()).isTrue();
        assertThat(checkInEntity.getDemographicsUpdated()).isTrue();
    }

    // ================================
    // Multi-Tenant Isolation Tests
    // ================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Multi-tenant isolation: tenant A cannot access tenant B's check-ins")
    void testMultiTenantIsolation() throws Exception {
        // Create check-in in tenant A
        CheckInRequest request = CheckInRequest.builder()
                .patientId(PATIENT_ID)
                .appointmentId(APPOINTMENT_ID)
                .checkInTime(LocalDateTime.now())
                .insuranceVerified(false)
                .consentSigned(false)
                .checkInMethod("FRONT_DESK")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/check-in")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        CheckInResponse checkInResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), CheckInResponse.class);
        UUID checkInId = checkInResponse.getId();

        // Try to access from tenant B - should fail
        mockMvc.perform(get("/api/v1/check-in/{id}", checkInId)
                        .header("X-Tenant-ID", TENANT_ID_B))
                .andExpect(status().isNotFound());

        // Try to update from tenant B - should fail
        InsuranceVerificationRequest insuranceRequest = InsuranceVerificationRequest.builder()
                .verified(true)
                .insuranceProvider("Blue Cross")
                .build();

        mockMvc.perform(put("/api/v1/check-in/{id}/insurance", checkInId)
                        .header("X-Tenant-ID", TENANT_ID_B)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insuranceRequest)))
                .andExpect(status().isNotFound());

        // Verify tenant A can still access
        mockMvc.perform(get("/api/v1/check-in/{id}", checkInId)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(checkInId.toString()));
    }

    // ================================
    // Error Scenario Tests
    // ================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Error: Duplicate check-in for same patient/appointment")
    void testDuplicateCheckIn() throws Exception {
        // Create first check-in
        CheckInRequest request = CheckInRequest.builder()
                .patientId(PATIENT_ID)
                .appointmentId(APPOINTMENT_ID)
                .checkInTime(LocalDateTime.now())
                .insuranceVerified(false)
                .consentSigned(false)
                .checkInMethod("FRONT_DESK")
                .build();

        mockMvc.perform(post("/api/v1/check-in")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Try to create duplicate check-in - should fail
        mockMvc.perform(post("/api/v1/check-in")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Error: Missing check-in (404)")
    void testMissingCheckIn() throws Exception {
        UUID nonExistentId = UUID.randomUUID();

        // Try to get non-existent check-in
        mockMvc.perform(get("/api/v1/check-in/{id}", nonExistentId)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isNotFound());

        // Try to update non-existent check-in
        InsuranceVerificationRequest insuranceRequest = InsuranceVerificationRequest.builder()
                .verified(true)
                .insuranceProvider("Blue Cross")
                .build();

        mockMvc.perform(put("/api/v1/check-in/{id}/insurance", nonExistentId)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insuranceRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Error: Invalid request - missing required fields")
    void testInvalidRequest() throws Exception {
        // Missing patientId
        CheckInRequest invalidRequest = CheckInRequest.builder()
                .appointmentId(APPOINTMENT_ID)
                .checkInTime(LocalDateTime.now())
                .build();

        mockMvc.perform(post("/api/v1/check-in")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    // ================================
    // Audit Trail Tests
    // ================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Verify audit trail: all audit fields populated correctly")
    void testAuditTrail() throws Exception {
        // Create check-in
        CheckInRequest request = CheckInRequest.builder()
                .patientId(PATIENT_ID)
                .appointmentId(APPOINTMENT_ID)
                .checkInTime(LocalDateTime.now())
                .insuranceVerified(false)
                .consentSigned(false)
                .checkInMethod("FRONT_DESK")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/check-in")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        CheckInResponse checkInResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), CheckInResponse.class);
        UUID checkInId = checkInResponse.getId();

        // Verify initial audit fields
        PatientCheckInEntity entity = checkInRepository.findById(checkInId).orElseThrow();
        assertThat(entity.getCheckedInBy()).isEqualTo(USER_ID);
        assertThat(entity.getCheckInTime()).isNotNull();

        // Verify insurance verification audit
        InsuranceVerificationRequest insuranceRequest = InsuranceVerificationRequest.builder()
                .verified(true)
                .insuranceProvider("Blue Cross")
                .build();

        mockMvc.perform(put("/api/v1/check-in/{id}/insurance", checkInId)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", "verifier@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(insuranceRequest)))
                .andExpect(status().isOk());

        entity = checkInRepository.findById(checkInId).orElseThrow();
        assertThat(entity.getVerifiedBy()).isEqualTo("verifier@example.com");

        // Verify consent audit
        ConsentRequest consentRequest = ConsentRequest.builder()
                .consentObtained(true)
                .consentType("TREATMENT")
                .build();

        mockMvc.perform(put("/api/v1/check-in/{id}/consent", checkInId)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", "nurse@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(consentRequest)))
                .andExpect(status().isOk());

        entity = checkInRepository.findById(checkInId).orElseThrow();
        assertThat(entity.getConsentObtainedBy()).isEqualTo("nurse@example.com");

        // Verify demographics audit
        DemographicsUpdateRequest demographicsRequest = DemographicsUpdateRequest.builder()
                .confirmed(true)
                .build();

        mockMvc.perform(put("/api/v1/check-in/{id}/demographics", checkInId)
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", "receptionist@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(demographicsRequest)))
                .andExpect(status().isOk());

        entity = checkInRepository.findById(checkInId).orElseThrow();
        assertThat(entity.getDemographicsUpdatedBy()).isEqualTo("receptionist@example.com");

        // Verify all audit fields are present
        assertThat(entity.getCheckedInBy()).isNotNull();
        assertThat(entity.getCheckInTime()).isNotNull();
        assertThat(entity.getVerifiedBy()).isNotNull();
        assertThat(entity.getConsentObtainedBy()).isNotNull();
        assertThat(entity.getDemographicsUpdatedBy()).isNotNull();
    }

    // ================================
    // History and Pagination Tests
    // ================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Get check-in history with pagination")
    void testCheckInHistoryWithPagination() throws Exception {
        // Create 5 check-ins
        for (int i = 1; i <= 5; i++) {
            CheckInRequest request = CheckInRequest.builder()
                    .patientId(UUID.randomUUID().toString())
                    .appointmentId("APPT00" + i)
                    .checkInTime(LocalDateTime.now().minusHours(i))
                    .insuranceVerified(false)
                    .consentSigned(false)
                    .checkInMethod("FRONT_DESK")
                    .build();

            mockMvc.perform(post("/api/v1/check-in")
                            .header("X-Tenant-ID", TENANT_ID_A)
                            .header("X-User-ID", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Get history with pagination (page 0, size 3)
        mockMvc.perform(get("/api/v1/check-in/history")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .param("page", "0")
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkIns", hasSize(3)))
                .andExpect(jsonPath("$.totalRecords").value(greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.currentPage").value(0))
                .andExpect(jsonPath("$.pageSize").value(3));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Get today's check-in for patient")
    void testGetTodaysCheckIn() throws Exception {
        // Create check-in for today
        CheckInRequest request = CheckInRequest.builder()
                .patientId(PATIENT_ID)
                .appointmentId(APPOINTMENT_ID)
                .checkInTime(LocalDateTime.now())
                .insuranceVerified(false)
                .consentSigned(false)
                .checkInMethod("FRONT_DESK")
                .build();

        mockMvc.perform(post("/api/v1/check-in")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Get today's check-in
        mockMvc.perform(get("/api/v1/check-in/patient/{patientId}/today", PATIENT_ID)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID))
                .andExpect(jsonPath("$.appointmentId").value(APPOINTMENT_ID));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Get check-in history with date range filter")
    void testCheckInHistoryWithDateRange() throws Exception {
        // Create check-ins on different dates
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        for (int i = 1; i <= 3; i++) {
            CheckInRequest request = CheckInRequest.builder()
                    .patientId(UUID.randomUUID().toString())
                    .appointmentId("APPT00" + i)
                    .checkInTime(yesterday.atTime(9, i * 10))
                    .insuranceVerified(false)
                    .consentSigned(false)
                    .checkInMethod("FRONT_DESK")
                    .build();

            mockMvc.perform(post("/api/v1/check-in")
                            .header("X-Tenant-ID", TENANT_ID_A)
                            .header("X-User-ID", USER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated());
        }

        // Get history for yesterday only
        mockMvc.perform(get("/api/v1/check-in/history")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .param("startDate", yesterday.toString())
                        .param("endDate", yesterday.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkIns", hasSize(greaterThanOrEqualTo(3))));
    }

    // ================================
    // Additional Test Scenarios
    // ================================

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Get check-in by ID")
    void testGetCheckInById() throws Exception {
        // Create check-in
        CheckInRequest request = CheckInRequest.builder()
                .patientId(PATIENT_ID)
                .appointmentId(APPOINTMENT_ID)
                .checkInTime(LocalDateTime.now())
                .insuranceVerified(false)
                .consentSigned(false)
                .checkInMethod("KIOSK")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/check-in")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        CheckInResponse checkInResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(), CheckInResponse.class);
        UUID checkInId = checkInResponse.getId();

        // Get by ID
        mockMvc.perform(get("/api/v1/check-in/{id}", checkInId)
                        .header("X-Tenant-ID", TENANT_ID_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(checkInId.toString()))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID))
                .andExpect(jsonPath("$.appointmentId").value(APPOINTMENT_ID))
                .andExpect(jsonPath("$.checkInMethod").value("KIOSK"));
    }

    @Test
    @WithMockUser(roles = "RECEPTIONIST")
    @DisplayName("Verify role-based access control")
    void testRoleBasedAccessControl() throws Exception {
        // RECEPTIONIST role can check in patients
        CheckInRequest request = CheckInRequest.builder()
                .patientId(PATIENT_ID)
                .appointmentId(APPOINTMENT_ID)
                .checkInTime(LocalDateTime.now())
                .insuranceVerified(false)
                .consentSigned(false)
                .checkInMethod("FRONT_DESK")
                .build();

        mockMvc.perform(post("/api/v1/check-in")
                        .header("X-Tenant-ID", TENANT_ID_A)
                        .header("X-User-ID", USER_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }
}
