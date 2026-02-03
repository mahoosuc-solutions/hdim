package com.healthdata.consent.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.consent.persistence.ConsentEntity;
import com.healthdata.consent.service.ConsentService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller tests for ConsentController.
 * Tests REST API endpoints for HIPAA 42 CFR Part 2 consent management.
 */
@WebMvcTest(ConsentController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@DisplayName("Consent Controller Tests")
class ConsentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ConsentService consentService;

    private static final String TENANT_ID = "tenant-123";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID CONSENT_ID = UUID.randomUUID();

    @Nested
    @DisplayName("POST /api/consents Tests")
    class CreateConsentTests {

        @Test
        @DisplayName("Should create consent and return 201")
        void shouldCreateConsent() throws Exception {
            // Given
            ConsentEntity consent = createValidConsent();
            ConsentEntity savedConsent = createValidConsent();
            savedConsent.setId(CONSENT_ID);

            when(consentService.createConsent(eq(TENANT_ID), any(ConsentEntity.class), anyString()))
                    .thenReturn(savedConsent);

            // When/Then
            mockMvc.perform(post("/api/consents")
                            .header("X-Tenant-ID", TENANT_ID)
                            .header("X-User-ID", "test-user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(consent)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(CONSENT_ID.toString()))
                    .andExpect(jsonPath("$.scope").value("read"))
                    .andExpect(jsonPath("$.status").value("active"));
        }

        @Test
        @DisplayName("Should return 400 for invalid consent")
        void shouldReturn400ForInvalidConsent() throws Exception {
            // Given
            ConsentEntity consent = createValidConsent();
            when(consentService.createConsent(eq(TENANT_ID), any(ConsentEntity.class), anyString()))
                    .thenThrow(new IllegalArgumentException("Invalid consent"));

            // When/Then
            mockMvc.perform(post("/api/consents")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(consent)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should use default user ID when not provided")
        void shouldUseDefaultUserId() throws Exception {
            // Given
            ConsentEntity consent = createValidConsent();
            ConsentEntity savedConsent = createValidConsent();
            savedConsent.setId(CONSENT_ID);

            when(consentService.createConsent(eq(TENANT_ID), any(ConsentEntity.class), eq("system")))
                    .thenReturn(savedConsent);

            // When/Then
            mockMvc.perform(post("/api/consents")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(consent)))
                    .andExpect(status().isCreated());

            verify(consentService).createConsent(eq(TENANT_ID), any(ConsentEntity.class), eq("system"));
        }
    }

    @Nested
    @DisplayName("GET /api/consents/{id} Tests")
    class GetConsentTests {

        @Test
        @DisplayName("Should return consent when found")
        void shouldReturnConsentWhenFound() throws Exception {
            // Given
            ConsentEntity consent = createValidConsent();
            consent.setId(CONSENT_ID);
            when(consentService.getConsent(TENANT_ID, CONSENT_ID))
                    .thenReturn(Optional.of(consent));

            // When/Then
            mockMvc.perform(get("/api/consents/{id}", CONSENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(CONSENT_ID.toString()));
        }

        @Test
        @DisplayName("Should return 404 when consent not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            when(consentService.getConsent(TENANT_ID, CONSENT_ID))
                    .thenReturn(Optional.empty());

            // When/Then
            mockMvc.perform(get("/api/consents/{id}", CONSENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("PUT /api/consents/{id} Tests")
    class UpdateConsentTests {

        @Test
        @DisplayName("Should update consent successfully")
        void shouldUpdateConsent() throws Exception {
            // Given
            ConsentEntity consent = createValidConsent();
            consent.setId(CONSENT_ID);
            consent.setScope("write");

            when(consentService.updateConsent(eq(TENANT_ID), eq(CONSENT_ID), any(ConsentEntity.class), anyString()))
                    .thenReturn(consent);

            // When/Then
            mockMvc.perform(put("/api/consents/{id}", CONSENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .header("X-User-ID", "test-user")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(consent)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.scope").value("write"));
        }

        @Test
        @DisplayName("Should return 404 when consent not found for update")
        void shouldReturn404WhenNotFoundForUpdate() throws Exception {
            // Given
            ConsentEntity consent = createValidConsent();
            when(consentService.updateConsent(eq(TENANT_ID), eq(CONSENT_ID), any(ConsentEntity.class), anyString()))
                    .thenThrow(new ConsentService.ConsentNotFoundException("Consent not found"));

            // When/Then
            mockMvc.perform(put("/api/consents/{id}", CONSENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(consent)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should return 400 for invalid update")
        void shouldReturn400ForInvalidUpdate() throws Exception {
            // Given
            ConsentEntity consent = createValidConsent();
            when(consentService.updateConsent(eq(TENANT_ID), eq(CONSENT_ID), any(ConsentEntity.class), anyString()))
                    .thenThrow(new IllegalArgumentException("Invalid consent"));

            // When/Then
            mockMvc.perform(put("/api/consents/{id}", CONSENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(consent)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/consents/{id} Tests")
    class DeleteConsentTests {

        @Test
        @DisplayName("Should delete consent and return 204")
        void shouldDeleteConsent() throws Exception {
            // Given
            doNothing().when(consentService).deleteConsent(TENANT_ID, CONSENT_ID, "test-user");

            // When/Then
            mockMvc.perform(delete("/api/consents/{id}", CONSENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .header("X-User-ID", "test-user"))
                    .andExpect(status().isNoContent());

            verify(consentService).deleteConsent(TENANT_ID, CONSENT_ID, "test-user");
        }

        @Test
        @DisplayName("Should return 404 when consent not found for delete")
        void shouldReturn404WhenNotFoundForDelete() throws Exception {
            // Given
            doThrow(new ConsentService.ConsentNotFoundException("Consent not found"))
                    .when(consentService).deleteConsent(TENANT_ID, CONSENT_ID, "system");

            // When/Then
            mockMvc.perform(delete("/api/consents/{id}", CONSENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/consents/{id}/revoke Tests")
    class RevokeConsentTests {

        @Test
        @DisplayName("Should revoke consent successfully")
        void shouldRevokeConsent() throws Exception {
            // Given
            ConsentEntity consent = createValidConsent();
            consent.setId(CONSENT_ID);
            consent.setStatus("revoked");
            consent.setRevocationReason("Patient request");

            when(consentService.revokeConsent(eq(TENANT_ID), eq(CONSENT_ID), eq("Patient request"), anyString()))
                    .thenReturn(consent);

            // When/Then
            mockMvc.perform(post("/api/consents/{id}/revoke", CONSENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .header("X-User-ID", "test-user")
                            .param("reason", "Patient request"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("revoked"));
        }

        @Test
        @DisplayName("Should revoke consent without reason")
        void shouldRevokeConsentWithoutReason() throws Exception {
            // Given
            ConsentEntity consent = createValidConsent();
            consent.setId(CONSENT_ID);
            consent.setStatus("revoked");

            when(consentService.revokeConsent(eq(TENANT_ID), eq(CONSENT_ID), isNull(), anyString()))
                    .thenReturn(consent);

            // When/Then
            mockMvc.perform(post("/api/consents/{id}/revoke", CONSENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("revoked"));
        }

        @Test
        @DisplayName("Should return 404 when consent not found for revocation")
        void shouldReturn404WhenNotFoundForRevoke() throws Exception {
            // Given
            when(consentService.revokeConsent(eq(TENANT_ID), eq(CONSENT_ID), any(), anyString()))
                    .thenThrow(new ConsentService.ConsentNotFoundException("Consent not found"));

            // When/Then
            mockMvc.perform(post("/api/consents/{id}/revoke", CONSENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/consents/patient/{patientId} Tests")
    class GetConsentsByPatientTests {

        @Test
        @DisplayName("Should return all consents for patient")
        void shouldReturnConsentsForPatient() throws Exception {
            // Given
            List<ConsentEntity> consents = List.of(
                    createValidConsentWithId(UUID.randomUUID()),
                    createValidConsentWithId(UUID.randomUUID())
            );
            when(consentService.getConsentsByPatient(TENANT_ID, PATIENT_ID))
                    .thenReturn(consents);

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        @DisplayName("Should return empty list when patient has no consents")
        void shouldReturnEmptyListWhenNoConsents() throws Exception {
            // Given
            when(consentService.getConsentsByPatient(TENANT_ID, PATIENT_ID))
                    .thenReturn(List.of());

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /api/consents/patient/{patientId}/page Tests")
    class GetConsentsByPatientPaginatedTests {

        @Test
        @DisplayName("Should return paginated consents for patient")
        void shouldReturnPaginatedConsents() throws Exception {
            // Given
            List<ConsentEntity> consents = List.of(createValidConsentWithId(UUID.randomUUID()));
            Page<ConsentEntity> page = new PageImpl<>(consents);

            when(consentService.getConsentsByPatient(eq(TENANT_ID), eq(PATIENT_ID), any(Pageable.class)))
                    .thenReturn(page);

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}/page", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("page", "0")
                            .param("size", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/consents/patient/{patientId}/active Tests")
    class GetActiveConsentsByPatientTests {

        @Test
        @DisplayName("Should return active consents for patient")
        void shouldReturnActiveConsents() throws Exception {
            // Given
            ConsentEntity consent = createValidConsentWithId(UUID.randomUUID());
            consent.setStatus("active");
            when(consentService.getActiveConsentsByPatient(TENANT_ID, PATIENT_ID))
                    .thenReturn(List.of(consent));

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}/active", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("active"));
        }
    }

    @Nested
    @DisplayName("GET /api/consents/patient/{patientId}/active/scope/{scope} Tests")
    class GetActiveConsentsByPatientAndScopeTests {

        @Test
        @DisplayName("Should return active consents for patient and scope")
        void shouldReturnActiveConsentsForScope() throws Exception {
            // Given
            ConsentEntity consent = createValidConsentWithId(UUID.randomUUID());
            consent.setScope("read");
            when(consentService.getActiveConsentsByPatientAndScope(TENANT_ID, PATIENT_ID, "read"))
                    .thenReturn(List.of(consent));

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}/active/scope/{scope}", PATIENT_ID, "read")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].scope").value("read"));
        }
    }

    @Nested
    @DisplayName("GET /api/consents/patient/{patientId}/active/category/{category} Tests")
    class GetActiveConsentsByPatientAndCategoryTests {

        @Test
        @DisplayName("Should return active consents for patient and category")
        void shouldReturnActiveConsentsForCategory() throws Exception {
            // Given
            ConsentEntity consent = createValidConsentWithId(UUID.randomUUID());
            consent.setCategory("treatment");
            when(consentService.getActiveConsentsByPatientAndCategory(TENANT_ID, PATIENT_ID, "treatment"))
                    .thenReturn(List.of(consent));

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}/active/category/{category}",
                            PATIENT_ID, "treatment")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].category").value("treatment"));
        }
    }

    @Nested
    @DisplayName("GET /api/consents/patient/{patientId}/active/data-class/{dataClass} Tests")
    class GetActiveConsentsByPatientAndDataClassTests {

        @Test
        @DisplayName("Should return active consents for patient and data class")
        void shouldReturnActiveConsentsForDataClass() throws Exception {
            // Given
            ConsentEntity consent = createValidConsentWithId(UUID.randomUUID());
            consent.setDataClass("substance-abuse");
            when(consentService.getActiveConsentsByPatientAndDataClass(TENANT_ID, PATIENT_ID, "substance-abuse"))
                    .thenReturn(List.of(consent));

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}/active/data-class/{dataClass}",
                            PATIENT_ID, "substance-abuse")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].dataClass").value("substance-abuse"));
        }
    }

    @Nested
    @DisplayName("GET /api/consents/patient/{patientId}/revoked Tests")
    class GetRevokedConsentsByPatientTests {

        @Test
        @DisplayName("Should return revoked consents for patient")
        void shouldReturnRevokedConsents() throws Exception {
            // Given
            ConsentEntity consent = createValidConsentWithId(UUID.randomUUID());
            consent.setStatus("revoked");
            when(consentService.getRevokedConsentsByPatient(TENANT_ID, PATIENT_ID))
                    .thenReturn(List.of(consent));

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}/revoked", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].status").value("revoked"));
        }
    }

    @Nested
    @DisplayName("GET /api/consents/patient/{patientId}/expired Tests")
    class GetExpiredConsentsByPatientTests {

        @Test
        @DisplayName("Should return expired consents for patient")
        void shouldReturnExpiredConsents() throws Exception {
            // Given
            ConsentEntity consent = createValidConsentWithId(UUID.randomUUID());
            consent.setStatus("expired");
            consent.setValidTo(LocalDate.now().minusDays(30));
            when(consentService.getExpiredConsentsByPatient(TENANT_ID, PATIENT_ID))
                    .thenReturn(List.of(consent));

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}/expired", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("GET /api/consents/patient/{patientId}/expiring-soon Tests")
    class GetConsentsExpiringSoonTests {

        @Test
        @DisplayName("Should return consents expiring soon with default days")
        void shouldReturnConsentsExpiringSoonWithDefaultDays() throws Exception {
            // Given
            ConsentEntity consent = createValidConsentWithId(UUID.randomUUID());
            consent.setValidTo(LocalDate.now().plusDays(15));
            when(consentService.getConsentsExpiringSoon(TENANT_ID, PATIENT_ID, 30))
                    .thenReturn(List.of(consent));

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}/expiring-soon", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("Should return consents expiring soon with custom days")
        void shouldReturnConsentsExpiringSoonWithCustomDays() throws Exception {
            // Given
            ConsentEntity consent = createValidConsentWithId(UUID.randomUUID());
            consent.setValidTo(LocalDate.now().plusDays(5));
            when(consentService.getConsentsExpiringSoon(TENANT_ID, PATIENT_ID, 7))
                    .thenReturn(List.of(consent));

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}/expiring-soon", PATIENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("days", "7"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("Consent Check Endpoint Tests")
    class ConsentCheckTests {

        @Test
        @DisplayName("Should check consent for scope - has consent")
        void shouldCheckConsentForScopeHasConsent() throws Exception {
            // Given
            when(consentService.hasActiveConsentForScope(TENANT_ID, PATIENT_ID, "read"))
                    .thenReturn(true);

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}/check/scope/{scope}",
                            PATIENT_ID, "read")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasConsent").value(true));
        }

        @Test
        @DisplayName("Should check consent for scope - no consent")
        void shouldCheckConsentForScopeNoConsent() throws Exception {
            // Given
            when(consentService.hasActiveConsentForScope(TENANT_ID, PATIENT_ID, "write"))
                    .thenReturn(false);

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}/check/scope/{scope}",
                            PATIENT_ID, "write")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasConsent").value(false));
        }

        @Test
        @DisplayName("Should check consent for category")
        void shouldCheckConsentForCategory() throws Exception {
            // Given
            when(consentService.hasActiveConsentForCategory(TENANT_ID, PATIENT_ID, "treatment"))
                    .thenReturn(true);

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}/check/category/{category}",
                            PATIENT_ID, "treatment")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasConsent").value(true));
        }

        @Test
        @DisplayName("Should check consent for data class")
        void shouldCheckConsentForDataClass() throws Exception {
            // Given
            when(consentService.hasActiveConsentForDataClass(TENANT_ID, PATIENT_ID, "substance-abuse"))
                    .thenReturn(true);

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}/check/data-class/{dataClass}",
                            PATIENT_ID, "substance-abuse")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasConsent").value(true));
        }

        @Test
        @DisplayName("Should check consent for authorized party")
        void shouldCheckConsentForAuthorizedParty() throws Exception {
            // Given
            when(consentService.hasActiveConsentForAuthorizedParty(TENANT_ID, PATIENT_ID, "org-123"))
                    .thenReturn(true);

            // When/Then
            mockMvc.perform(get("/api/consents/patient/{patientId}/check/authorized-party/{authorizedPartyId}",
                            PATIENT_ID, "org-123")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.hasConsent").value(true));
        }
    }

    @Nested
    @DisplayName("POST /api/consents/validate-access Tests")
    class ValidateDataAccessTests {

        @Test
        @DisplayName("Should validate data access - permitted")
        void shouldValidateDataAccessPermitted() throws Exception {
            // Given
            ConsentController.DataAccessRequest request = new ConsentController.DataAccessRequest();
            request.setPatientId(PATIENT_ID);
            request.setScope("read");
            request.setCategory("treatment");
            request.setDataClass("general");
            request.setAuthorizedPartyId("org-123");

            ConsentService.ConsentValidationResult result =
                    ConsentService.ConsentValidationResult.permitted("Access permitted by active consent");

            when(consentService.validateDataAccess(
                    eq(TENANT_ID), eq(PATIENT_ID), eq("read"), eq("treatment"),
                    eq("general"), eq("org-123")))
                    .thenReturn(result);

            // When/Then
            mockMvc.perform(post("/api/consents/validate-access")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.permitted").value(true))
                    .andExpect(jsonPath("$.reason").value("Access permitted by active consent"));
        }

        @Test
        @DisplayName("Should validate data access - denied")
        void shouldValidateDataAccessDenied() throws Exception {
            // Given
            ConsentController.DataAccessRequest request = new ConsentController.DataAccessRequest();
            request.setPatientId(PATIENT_ID);
            request.setScope("write");
            request.setCategory("research");
            request.setDataClass("substance-abuse");
            request.setAuthorizedPartyId("org-456");

            ConsentService.ConsentValidationResult result =
                    ConsentService.ConsentValidationResult.denied("No active consent found for requested access");

            when(consentService.validateDataAccess(
                    eq(TENANT_ID), eq(PATIENT_ID), eq("write"), eq("research"),
                    eq("substance-abuse"), eq("org-456")))
                    .thenReturn(result);

            // When/Then
            mockMvc.perform(post("/api/consents/validate-access")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.permitted").value(false))
                    .andExpect(jsonPath("$.reason").value("No active consent found for requested access"));
        }
    }

    @Nested
    @DisplayName("GET /api/consents/_health Tests")
    class HealthCheckTests {

        @Test
        @DisplayName("Should return health status")
        void shouldReturnHealthStatus() throws Exception {
            // When/Then
            mockMvc.perform(get("/api/consents/_health"))
                    .andExpect(status().isOk())
                    .andExpect(content().json("{\"status\": \"UP\", \"service\": \"Consent\"}"));
        }
    }

    // ==================== Helper Methods ====================

    private ConsentEntity createValidConsent() {
        return ConsentEntity.builder()
                .patientId(PATIENT_ID)
                .tenantId(TENANT_ID)
                .scope("read")
                .status("active")
                .category("treatment")
                .purpose("Healthcare treatment purposes")
                .authorizedPartyType("organization")
                .authorizedPartyId("org-123")
                .authorizedPartyName("Test Healthcare Organization")
                .dataClass("general")
                .policyRule("hipaa-tpo")
                .provisionType("permit")
                .validFrom(LocalDate.now())
                .validTo(LocalDate.now().plusYears(1))
                .consentDate(LocalDate.now())
                .verificationMethod("electronic-signature")
                .verifiedBy("Test User")
                .verificationDate(LocalDate.now())
                .createdAt(Instant.now())
                .lastModifiedAt(Instant.now())
                .createdBy("system")
                .lastModifiedBy("system")
                .version(0)
                .build();
    }

    private ConsentEntity createValidConsentWithId(UUID id) {
        ConsentEntity consent = createValidConsent();
        consent.setId(id);
        return consent;
    }
}
