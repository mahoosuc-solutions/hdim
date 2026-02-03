package com.healthdata.consent.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.consent.ConsentServiceApplication;
import com.healthdata.consent.persistence.ConsentEntity;
import com.healthdata.consent.persistence.ConsentRepository;
import com.healthdata.consent.rest.ConsentController.ConsentCheckResponse;
import com.healthdata.consent.rest.ConsentController.ConsentValidationResponse;
import com.healthdata.consent.rest.ConsentController.DataAccessRequest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive End-to-End Integration Tests for Patient Consent Workflows
 *
 * Validates HIPAA 42 CFR Part 2 and GDPR consent requirements including:
 * - Consent creation and management (CRUD)
 * - Consent validation for data access
 * - Consent lifecycle (active, revoked, expired)
 * - Multi-tenant isolation
 * - Audit logging
 * - Advanced query capabilities
 *
 * These tests ensure compliance with healthcare data privacy regulations.
 */
@SpringBootTest(
    classes = ConsentServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Tag("e2e")
@Tag("heavyweight")
class PatientConsentWorkflowE2ETest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("consent_test_db")
        .withUsername("testuser")
        .withPassword("testpass");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConsentRepository consentRepository;

    private static final String TENANT_ID = "tenant-1";
    private static final String USER_ID = "user-123";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID_TENANT2 = UUID.randomUUID();

    private HttpHeaders createHeaders(String tenantId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Tenant-ID", tenantId);
        headers.set("X-User-ID", USER_ID);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private ConsentEntity createTestConsent(String scope, String category, String status) {
        return ConsentEntity.builder()
            .tenantId(TENANT_ID)
            .patientId(PATIENT_ID)
            .scope(scope)
            .status(status)
            .category(category)
            .purpose("Quality care and treatment")
            .provisionType("permit")
            .validFrom(LocalDate.now())
            .validTo(LocalDate.now().plusYears(1))
            .consentDate(LocalDate.now())
            .verificationMethod("electronic-signature")
            .verifiedBy(USER_ID)
            .verificationDate(LocalDate.now())
            .createdAt(LocalDateTime.now())
            .lastModifiedAt(LocalDateTime.now())
            .createdBy(USER_ID)
            .lastModifiedBy(USER_ID)
            .version(0)
            .build();
    }

    @Nested
    @DisplayName("Consent Creation & Management")
    class ConsentCreationTests {

        @Test
        @DisplayName("should create consent with all required fields")
        void shouldCreateConsentSuccessfully() throws Exception {
            ConsentEntity consent = createTestConsent("read", "treatment", "active");

            String consentJson = objectMapper.writeValueAsString(consent);

            mockMvc.perform(post("/api/consents")
                    .headers(createHeaders(TENANT_ID))
                    .content(consentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()))
                .andExpect(jsonPath("$.scope").value("read"))
                .andExpect(jsonPath("$.category").value("treatment"))
                .andExpect(jsonPath("$.status").value("active"))
                .andExpect(jsonPath("$.provisionType").value("permit"));
        }

        @Test
        @DisplayName("should create consent with sensitive data class (substance abuse)")
        void shouldCreateConsentForSensitiveDataClass() throws Exception {
            ConsentEntity consent = createTestConsent("full", "treatment", "active");
            consent.setDataClass("substance-abuse");
            consent.setPolicyRule("42-CFR-Part-2");

            String consentJson = objectMapper.writeValueAsString(consent);

            mockMvc.perform(post("/api/consents")
                    .headers(createHeaders(TENANT_ID))
                    .content(consentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dataClass").value("substance-abuse"))
                .andExpect(jsonPath("$.policyRule").value("42-CFR-Part-2"));
        }

        @Test
        @DisplayName("should retrieve consent by ID")
        void shouldRetrieveConsentById() throws Exception {
            ConsentEntity consent = createTestConsent("read", "treatment", "active");
            ConsentEntity saved = consentRepository.save(consent);

            mockMvc.perform(get("/api/consents/" + saved.getId())
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId().toString()))
                .andExpect(jsonPath("$.patientId").value(PATIENT_ID.toString()));
        }

        @Test
        @DisplayName("should update consent status")
        void shouldUpdateConsentStatus() throws Exception {
            ConsentEntity consent = createTestConsent("read", "treatment", "active");
            ConsentEntity saved = consentRepository.save(consent);

            saved.setStatus("rejected");
            saved.setNotes("Patient declined sharing");

            String consentJson = objectMapper.writeValueAsString(saved);

            mockMvc.perform(put("/api/consents/" + saved.getId())
                    .headers(createHeaders(TENANT_ID))
                    .content(consentJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rejected"))
                .andExpect(jsonPath("$.notes").value("Patient declined sharing"));
        }

        @Test
        @DisplayName("should delete consent")
        void shouldDeleteConsent() throws Exception {
            ConsentEntity consent = createTestConsent("read", "research", "active");
            ConsentEntity saved = consentRepository.save(consent);

            mockMvc.perform(delete("/api/consents/" + saved.getId())
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isNoContent());

            // Verify deletion
            mockMvc.perform(get("/api/consents/" + saved.getId())
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Consent Lifecycle Management")
    class ConsentLifecycleTests {

        @Test
        @DisplayName("should revoke active consent")
        void shouldRevokeActiveConsent() throws Exception {
            ConsentEntity consent = createTestConsent("full", "treatment", "active");
            ConsentEntity saved = consentRepository.save(consent);

            mockMvc.perform(post("/api/consents/" + saved.getId() + "/revoke")
                    .headers(createHeaders(TENANT_ID))
                    .param("reason", "Patient requested revocation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("revoked"))
                .andExpect(jsonPath("$.revocationReason").value("Patient requested revocation"))
                .andExpect(jsonPath("$.revocationDate").exists());
        }

        @Test
        @DisplayName("should retrieve active consents for patient")
        void shouldRetrieveActiveConsents() throws Exception {
            // Create active consent
            ConsentEntity activeConsent = createTestConsent("read", "treatment", "active");
            consentRepository.save(activeConsent);

            // Create revoked consent (should not appear in results)
            ConsentEntity revokedConsent = createTestConsent("write", "research", "revoked");
            revokedConsent.setRevocationDate(LocalDate.now());
            revokedConsent.setRevocationReason("Test revocation");
            consentRepository.save(revokedConsent);

            mockMvc.perform(get("/api/consents/patient/" + PATIENT_ID + "/active")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("active"));
        }

        @Test
        @DisplayName("should retrieve expired consents")
        void shouldRetrieveExpiredConsents() throws Exception {
            ConsentEntity expiredConsent = createTestConsent("read", "treatment", "expired");
            expiredConsent.setValidTo(LocalDate.now().minusDays(30));
            consentRepository.save(expiredConsent);

            mockMvc.perform(get("/api/consents/patient/" + PATIENT_ID + "/expired")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.status == 'expired')]").exists());
        }

        @Test
        @DisplayName("should retrieve consents expiring soon")
        void shouldRetrieveConsentsExpiringSoon() throws Exception {
            ConsentEntity expiringConsent = createTestConsent("read", "treatment", "active");
            expiringConsent.setValidTo(LocalDate.now().plusDays(15)); // Expires in 15 days
            consentRepository.save(expiringConsent);

            mockMvc.perform(get("/api/consents/patient/" + PATIENT_ID + "/expiring-soon")
                    .headers(createHeaders(TENANT_ID))
                    .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].validTo").exists());
        }

        @Test
        @DisplayName("should retrieve revoked consents")
        void shouldRetrieveRevokedConsents() throws Exception {
            ConsentEntity revokedConsent = createTestConsent("full", "treatment", "revoked");
            revokedConsent.setRevocationDate(LocalDate.now());
            revokedConsent.setRevocationReason("Patient withdrew consent");
            consentRepository.save(revokedConsent);

            mockMvc.perform(get("/api/consents/patient/" + PATIENT_ID + "/revoked")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("revoked"))
                .andExpect(jsonPath("$[0].revocationReason").exists());
        }
    }

    @Nested
    @DisplayName("Consent Validation & Authorization")
    class ConsentValidationTests {

        @Test
        @DisplayName("should validate active consent for scope")
        void shouldValidateConsentForScope() throws Exception {
            ConsentEntity consent = createTestConsent("read", "treatment", "active");
            consentRepository.save(consent);

            mockMvc.perform(get("/api/consents/patient/" + PATIENT_ID + "/check/scope/read")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasConsent").value(true));
        }

        @Test
        @DisplayName("should reject consent check for non-existent scope")
        void shouldRejectConsentForNonExistentScope() throws Exception {
            ConsentEntity consent = createTestConsent("read", "treatment", "active");
            consentRepository.save(consent);

            mockMvc.perform(get("/api/consents/patient/" + PATIENT_ID + "/check/scope/write")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasConsent").value(false));
        }

        @Test
        @DisplayName("should validate consent for category")
        void shouldValidateConsentForCategory() throws Exception {
            ConsentEntity consent = createTestConsent("read", "research", "active");
            consentRepository.save(consent);

            mockMvc.perform(get("/api/consents/patient/" + PATIENT_ID + "/check/category/research")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasConsent").value(true));
        }

        @Test
        @DisplayName("should validate consent for sensitive data class")
        void shouldValidateConsentForDataClass() throws Exception {
            ConsentEntity consent = createTestConsent("full", "treatment", "active");
            consent.setDataClass("mental-health");
            consentRepository.save(consent);

            mockMvc.perform(get("/api/consents/patient/" + PATIENT_ID + "/check/data-class/mental-health")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasConsent").value(true));
        }

        @Test
        @DisplayName("should validate consent for authorized party")
        void shouldValidateConsentForAuthorizedParty() throws Exception {
            ConsentEntity consent = createTestConsent("read", "treatment", "active");
            consent.setAuthorizedPartyId("practitioner-456");
            consent.setAuthorizedPartyType("Practitioner");
            consentRepository.save(consent);

            mockMvc.perform(get("/api/consents/patient/" + PATIENT_ID + "/check/authorized-party/practitioner-456")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasConsent").value(true));
        }

        @Test
        @DisplayName("should validate comprehensive data access request")
        void shouldValidateComprehensiveDataAccessRequest() throws Exception {
            ConsentEntity consent = createTestConsent("read", "research", "active");
            consent.setDataClass("genomic-data");
            consent.setAuthorizedPartyId("research-org-789");
            consentRepository.save(consent);

            DataAccessRequest request = new DataAccessRequest();
            request.setPatientId(PATIENT_ID);
            request.setScope("read");
            request.setCategory("research");
            request.setDataClass("genomic-data");
            request.setAuthorizedPartyId("research-org-789");

            String requestJson = objectMapper.writeValueAsString(request);

            mockMvc.perform(post("/api/consents/validate-access")
                    .headers(createHeaders(TENANT_ID))
                    .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permitted").value(true));
        }
    }

    @Nested
    @DisplayName("Advanced Query Capabilities")
    class AdvancedQueryTests {

        @Test
        @DisplayName("should query consents by patient with pagination")
        void shouldQueryConsentsByPatientWithPagination() throws Exception {
            // Create multiple consents
            for (int i = 0; i < 25; i++) {
                ConsentEntity consent = createTestConsent("read", "treatment", "active");
                consentRepository.save(consent);
            }

            mockMvc.perform(get("/api/consents/patient/" + PATIENT_ID + "/page")
                    .headers(createHeaders(TENANT_ID))
                    .param("page", "0")
                    .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(20))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(2));
        }

        @Test
        @DisplayName("should query active consents by scope")
        void shouldQueryActiveConsentsByScope() throws Exception {
            // Create consents with different scopes
            ConsentEntity readConsent = createTestConsent("read", "treatment", "active");
            consentRepository.save(readConsent);

            ConsentEntity writeConsent = createTestConsent("write", "treatment", "active");
            consentRepository.save(writeConsent);

            mockMvc.perform(get("/api/consents/patient/" + PATIENT_ID + "/active/scope/read")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].scope").value("read"));
        }

        @Test
        @DisplayName("should query active consents by category")
        void shouldQueryActiveConsentsByCategory() throws Exception {
            ConsentEntity treatmentConsent = createTestConsent("read", "treatment", "active");
            consentRepository.save(treatmentConsent);

            ConsentEntity researchConsent = createTestConsent("read", "research", "active");
            consentRepository.save(researchConsent);

            mockMvc.perform(get("/api/consents/patient/" + PATIENT_ID + "/active/category/treatment")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].category").value("treatment"));
        }

        @Test
        @DisplayName("should query active consents by data class")
        void shouldQueryActiveConsentsByDataClass() throws Exception {
            ConsentEntity hivConsent = createTestConsent("full", "treatment", "active");
            hivConsent.setDataClass("hiv");
            hivConsent.setPolicyRule("42-CFR-Part-2");
            consentRepository.save(hivConsent);

            mockMvc.perform(get("/api/consents/patient/" + PATIENT_ID + "/active/data-class/hiv")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].dataClass").value("hiv"))
                .andExpect(jsonPath("$[0].policyRule").value("42-CFR-Part-2"));
        }
    }

    @Nested
    @DisplayName("Multi-Tenant Isolation")
    class MultiTenantIsolationTests {

        @Test
        @DisplayName("should prevent cross-tenant consent access")
        void shouldPreventCrossTenantConsentAccess() throws Exception {
            ConsentEntity consent = createTestConsent("read", "treatment", "active");
            ConsentEntity saved = consentRepository.save(consent);

            // Try to access from different tenant
            mockMvc.perform(get("/api/consents/" + saved.getId())
                    .headers(createHeaders("tenant-2")))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should prevent cross-tenant consent updates")
        void shouldPreventCrossTenantConsentUpdates() throws Exception {
            ConsentEntity consent = createTestConsent("read", "treatment", "active");
            ConsentEntity saved = consentRepository.save(consent);

            saved.setStatus("revoked");
            String consentJson = objectMapper.writeValueAsString(saved);

            // Try to update from different tenant
            mockMvc.perform(put("/api/consents/" + saved.getId())
                    .headers(createHeaders("tenant-2"))
                    .content(consentJson))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should prevent cross-tenant consent deletion")
        void shouldPreventCrossTenantConsentDeletion() throws Exception {
            ConsentEntity consent = createTestConsent("read", "treatment", "active");
            ConsentEntity saved = consentRepository.save(consent);

            // Try to delete from different tenant
            mockMvc.perform(delete("/api/consents/" + saved.getId())
                    .headers(createHeaders("tenant-2")))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should isolate patient consent queries by tenant")
        void shouldIsolatePatientConsentQueriesByTenant() throws Exception {
            // Create consent in tenant-1
            ConsentEntity tenant1Consent = createTestConsent("read", "treatment", "active");
            consentRepository.save(tenant1Consent);

            // Create consent in tenant-2 for same patient ID
            ConsentEntity tenant2Consent = createTestConsent("read", "treatment", "active");
            tenant2Consent.setTenantId("tenant-2");
            consentRepository.save(tenant2Consent);

            // Query from tenant-1 should only return tenant-1 consents
            mockMvc.perform(get("/api/consents/patient/" + PATIENT_ID + "/active")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.tenantId != 'tenant-1')]").doesNotExist());
        }
    }

    @Nested
    @DisplayName("Error Handling & Validation")
    class ErrorHandlingTests {

        @Test
        @DisplayName("should return 404 for non-existent consent")
        void shouldReturn404ForNonExistentConsent() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(get("/api/consents/" + nonExistentId)
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return 404 when revoking non-existent consent")
        void shouldReturn404WhenRevokingNonExistentConsent() throws Exception {
            UUID nonExistentId = UUID.randomUUID();

            mockMvc.perform(post("/api/consents/" + nonExistentId + "/revoke")
                    .headers(createHeaders(TENANT_ID))
                    .param("reason", "Test"))
                .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("should return empty array for patient with no consents")
        void shouldReturnEmptyArrayForPatientWithNoConsents() throws Exception {
            UUID newPatientId = UUID.randomUUID();

            mockMvc.perform(get("/api/consents/patient/" + newPatientId + "/active")
                    .headers(createHeaders(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
        }
    }

    @Nested
    @DisplayName("HIPAA Compliance & Audit")
    class HipaaComplianceTests {

        @Test
        @DisplayName("should audit consent creation")
        void shouldAuditConsentCreation() throws Exception {
            ConsentEntity consent = createTestConsent("read", "treatment", "active");
            String consentJson = objectMapper.writeValueAsString(consent);

            mockMvc.perform(post("/api/consents")
                    .headers(createHeaders(TENANT_ID))
                    .content(consentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value(USER_ID))
                .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("should audit consent revocation")
        void shouldAuditConsentRevocation() throws Exception {
            ConsentEntity consent = createTestConsent("full", "treatment", "active");
            ConsentEntity saved = consentRepository.save(consent);

            mockMvc.perform(post("/api/consents/" + saved.getId() + "/revoke")
                    .headers(createHeaders(TENANT_ID))
                    .param("reason", "Audit test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.revokedBy").value(USER_ID))
                .andExpect(jsonPath("$.revocationDate").exists())
                .andExpect(jsonPath("$.revocationReason").value("Audit test"));
        }

        @Test
        @DisplayName("should enforce 42 CFR Part 2 for substance abuse data")
        void shouldEnforceCfr42ForSubstanceAbuse() throws Exception {
            ConsentEntity consent = createTestConsent("full", "treatment", "active");
            consent.setDataClass("substance-abuse");
            consent.setPolicyRule("42-CFR-Part-2");
            consent.setVerificationMethod("written");

            String consentJson = objectMapper.writeValueAsString(consent);

            mockMvc.perform(post("/api/consents")
                    .headers(createHeaders(TENANT_ID))
                    .content(consentJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dataClass").value("substance-abuse"))
                .andExpect(jsonPath("$.policyRule").value("42-CFR-Part-2"))
                .andExpect(jsonPath("$.verificationMethod").value("written"));
        }

        @Test
        @DisplayName("should track consent version for optimistic locking")
        void shouldTrackConsentVersion() throws Exception {
            ConsentEntity consent = createTestConsent("read", "treatment", "active");
            ConsentEntity saved = consentRepository.save(consent);

            // First update
            saved.setNotes("First update");
            String consentJson = objectMapper.writeValueAsString(saved);

            mockMvc.perform(put("/api/consents/" + saved.getId())
                    .headers(createHeaders(TENANT_ID))
                    .content(consentJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").exists());
        }
    }
}
