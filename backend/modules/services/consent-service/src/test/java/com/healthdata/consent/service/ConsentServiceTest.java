package com.healthdata.consent.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.consent.persistence.ConsentEntity;
import com.healthdata.consent.persistence.ConsentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ConsentService.
 * Tests consent management, validation, and privacy compliance logic.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Consent Service Tests")
class ConsentServiceTest {

    @Mock
    private ConsentRepository consentRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Captor
    private ArgumentCaptor<ConsentEntity> consentCaptor;

    private ConsentService consentService;
    private ObjectMapper objectMapper;

    private static final String TENANT_ID = "tenant-123";
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final String CREATED_BY = "test-user";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        consentService = new ConsentService(consentRepository, kafkaTemplate, objectMapper);
    }

    @Nested
    @DisplayName("Create Consent Tests")
    class CreateConsentTests {

        @Test
        @DisplayName("Should create consent successfully")
        void shouldCreateConsentSuccessfully() {
            // Given
            ConsentEntity consent = createValidConsent();
            when(consentRepository.save(any(ConsentEntity.class))).thenAnswer(inv -> {
                ConsentEntity saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            ConsentEntity result = consentService.createConsent(TENANT_ID, consent, CREATED_BY);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(result.getCreatedBy()).isEqualTo(CREATED_BY);
            verify(consentRepository).save(any(ConsentEntity.class));
        }

        @Test
        @DisplayName("Should set default status to active")
        void shouldSetDefaultStatusToActive() {
            // Given
            ConsentEntity consent = createValidConsent();
            consent.setStatus(null);
            when(consentRepository.save(any(ConsentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            ConsentEntity result = consentService.createConsent(TENANT_ID, consent, CREATED_BY);

            // Then
            assertThat(result.getStatus()).isEqualTo("active");
        }

        @Test
        @DisplayName("Should throw when patient ID is missing")
        void shouldThrowWhenPatientIdMissing() {
            // Given
            ConsentEntity consent = createValidConsent();
            consent.setPatientId(null);

            // When/Then
            assertThatThrownBy(() -> consentService.createConsent(TENANT_ID, consent, CREATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Patient ID");
        }

        @Test
        @DisplayName("Should throw when scope is missing")
        void shouldThrowWhenScopeMissing() {
            // Given
            ConsentEntity consent = createValidConsent();
            consent.setScope(null);

            // When/Then
            assertThatThrownBy(() -> consentService.createConsent(TENANT_ID, consent, CREATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("scope");
        }

        @Test
        @DisplayName("Should throw when category is missing")
        void shouldThrowWhenCategoryMissing() {
            // Given
            ConsentEntity consent = createValidConsent();
            consent.setCategory(null);

            // When/Then
            assertThatThrownBy(() -> consentService.createConsent(TENANT_ID, consent, CREATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("category");
        }

        @Test
        @DisplayName("Should throw when valid from date is missing")
        void shouldThrowWhenValidFromMissing() {
            ConsentEntity consent = createValidConsent();
            consent.setValidFrom(null);

            assertThatThrownBy(() -> consentService.createConsent(TENANT_ID, consent, CREATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Valid from");
        }

        @Test
        @DisplayName("Should throw when consent date is missing")
        void shouldThrowWhenConsentDateMissing() {
            ConsentEntity consent = createValidConsent();
            consent.setConsentDate(null);

            assertThatThrownBy(() -> consentService.createConsent(TENANT_ID, consent, CREATED_BY))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Consent date");
        }

        @Test
        @DisplayName("Should publish consent created event")
        void shouldPublishConsentCreatedEvent() {
            // Given
            ConsentEntity consent = createValidConsent();
            when(consentRepository.save(any(ConsentEntity.class))).thenAnswer(inv -> {
                ConsentEntity saved = inv.getArgument(0);
                saved.setId(UUID.randomUUID());
                return saved;
            });
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            consentService.createConsent(TENANT_ID, consent, CREATED_BY);

            // Then
            verify(kafkaTemplate).send(eq("consent.created"), anyString(), anyString());
        }

        @Test
        @DisplayName("Should swallow serialization errors when publishing events")
        void shouldSwallowSerializationErrors() throws Exception {
            ObjectMapper failingMapper = mock(ObjectMapper.class);
            ConsentService serviceWithFailingMapper = new ConsentService(consentRepository, kafkaTemplate, failingMapper);
            ConsentEntity consent = createValidConsent();
            consent.setId(UUID.randomUUID());

            when(consentRepository.save(any(ConsentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(failingMapper.writeValueAsString(any(ConsentEntity.class)))
                    .thenThrow(new JsonProcessingException("boom") { });

            ConsentEntity result = serviceWithFailingMapper.createConsent(TENANT_ID, consent, CREATED_BY);

            assertThat(result).isNotNull();
            verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should swallow Kafka errors when publishing events")
        void shouldSwallowKafkaErrors() throws Exception {
            ConsentEntity consent = createValidConsent();
            consent.setId(UUID.randomUUID());
            when(consentRepository.save(any(ConsentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("kafka down"));

            ConsentEntity result = consentService.createConsent(TENANT_ID, consent, CREATED_BY);

            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("Get Consent Tests")
    class GetConsentTests {

        @Test
        @DisplayName("Should return consent when found")
        void shouldReturnConsentWhenFound() {
            // Given
            UUID consentId = UUID.randomUUID();
            ConsentEntity consent = createValidConsent();
            consent.setId(consentId);
            when(consentRepository.findByTenantIdAndId(TENANT_ID, consentId))
                    .thenReturn(Optional.of(consent));

            // When
            Optional<ConsentEntity> result = consentService.getConsent(TENANT_ID, consentId);

            // Then
            assertThat(result).isPresent();
            assertThat(result.get().getId()).isEqualTo(consentId);
        }

        @Test
        @DisplayName("Should return empty when consent not found")
        void shouldReturnEmptyWhenNotFound() {
            // Given
            UUID consentId = UUID.randomUUID();
            when(consentRepository.findByTenantIdAndId(TENANT_ID, consentId))
                    .thenReturn(Optional.empty());

            // When
            Optional<ConsentEntity> result = consentService.getConsent(TENANT_ID, consentId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Update Consent Tests")
    class UpdateConsentTests {

        @Test
        @DisplayName("Should update consent successfully")
        void shouldUpdateConsentSuccessfully() {
            // Given
            UUID consentId = UUID.randomUUID();
            ConsentEntity existing = createValidConsent();
            existing.setId(consentId);
            existing.setScope("read");

            ConsentEntity update = createValidConsent();
            update.setScope("full");

            when(consentRepository.findByTenantIdAndId(TENANT_ID, consentId))
                    .thenReturn(Optional.of(existing));
            when(consentRepository.save(any(ConsentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            ConsentEntity result = consentService.updateConsent(TENANT_ID, consentId, update, CREATED_BY);

            // Then
            assertThat(result.getScope()).isEqualTo("full");
            assertThat(result.getLastModifiedBy()).isEqualTo(CREATED_BY);
        }

        @Test
        @DisplayName("Should throw when consent not found")
        void shouldThrowWhenConsentNotFound() {
            // Given
            UUID consentId = UUID.randomUUID();
            ConsentEntity update = createValidConsent();
            when(consentRepository.findByTenantIdAndId(TENANT_ID, consentId))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> consentService.updateConsent(TENANT_ID, consentId, update, CREATED_BY))
                    .isInstanceOf(ConsentService.ConsentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Delete Consent Tests")
    class DeleteConsentTests {

        @Test
        @DisplayName("Should delete consent successfully")
        void shouldDeleteConsentSuccessfully() {
            // Given
            UUID consentId = UUID.randomUUID();
            ConsentEntity existing = createValidConsent();
            existing.setId(consentId);

            when(consentRepository.findByTenantIdAndId(TENANT_ID, consentId))
                    .thenReturn(Optional.of(existing));
            doNothing().when(consentRepository).delete(any(ConsentEntity.class));
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            consentService.deleteConsent(TENANT_ID, consentId, CREATED_BY);

            // Then
            verify(consentRepository).delete(existing);
            verify(kafkaTemplate).send(eq("consent.deleted"), anyString(), anyString());
        }

        @Test
        @DisplayName("Should throw when consent not found")
        void shouldThrowWhenConsentNotFound() {
            // Given
            UUID consentId = UUID.randomUUID();
            when(consentRepository.findByTenantIdAndId(TENANT_ID, consentId))
                    .thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> consentService.deleteConsent(TENANT_ID, consentId, CREATED_BY))
                    .isInstanceOf(ConsentService.ConsentNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Revoke Consent Tests")
    class RevokeConsentTests {

        @Test
        @DisplayName("Should revoke consent successfully")
        void shouldRevokeConsentSuccessfully() {
            // Given
            UUID consentId = UUID.randomUUID();
            ConsentEntity existing = createValidConsent();
            existing.setId(consentId);
            existing.setStatus("active");

            when(consentRepository.findByTenantIdAndId(TENANT_ID, consentId))
                    .thenReturn(Optional.of(existing));
            when(consentRepository.save(any(ConsentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            ConsentEntity result = consentService.revokeConsent(TENANT_ID, consentId, "Patient request", CREATED_BY);

            // Then
            assertThat(result.getStatus()).isEqualTo("revoked");
            assertThat(result.getRevocationReason()).isEqualTo("Patient request");
            assertThat(result.getRevokedBy()).isEqualTo(CREATED_BY);
            assertThat(result.getRevocationDate()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("Should publish consent revoked event")
        void shouldPublishConsentRevokedEvent() {
            // Given
            UUID consentId = UUID.randomUUID();
            ConsentEntity existing = createValidConsent();
            existing.setId(consentId);

            when(consentRepository.findByTenantIdAndId(TENANT_ID, consentId))
                    .thenReturn(Optional.of(existing));
            when(consentRepository.save(any(ConsentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            // When
            consentService.revokeConsent(TENANT_ID, consentId, "Reason", CREATED_BY);

            // Then
            verify(kafkaTemplate).send(eq("consent.revoked"), anyString(), anyString());
        }
    }

    @Nested
    @DisplayName("Get Consents By Patient Tests")
    class GetConsentsByPatientTests {

        @Test
        @DisplayName("Should return all consents for patient")
        void shouldReturnAllConsentsForPatient() {
            // Given
            List<ConsentEntity> consents = List.of(
                    createValidConsent(),
                    createValidConsent()
            );
            when(consentRepository.findByTenantIdAndPatientIdOrderByConsentDateDesc(TENANT_ID, PATIENT_ID))
                    .thenReturn(consents);

            // When
            List<ConsentEntity> result = consentService.getConsentsByPatient(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("Should return active consents for patient")
        void shouldReturnActiveConsentsForPatient() {
            // Given
            ConsentEntity activeConsent = createValidConsent();
            activeConsent.setStatus("active");
            when(consentRepository.findActiveConsentsByPatient(eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class)))
                    .thenReturn(List.of(activeConsent));

            // When
            List<ConsentEntity> result = consentService.getActiveConsentsByPatient(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("active");
        }

        @Test
        @DisplayName("Should return active consents for patient and scope")
        void shouldReturnActiveConsentsForPatientAndScope() {
            ConsentEntity activeConsent = createValidConsent();
            when(consentRepository.findActiveConsentsByPatientAndScope(eq(TENANT_ID), eq(PATIENT_ID), eq("read"), any(LocalDate.class)))
                    .thenReturn(List.of(activeConsent));

            List<ConsentEntity> result = consentService.getActiveConsentsByPatientAndScope(TENANT_ID, PATIENT_ID, "read");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return active consents for patient and category")
        void shouldReturnActiveConsentsForPatientAndCategory() {
            ConsentEntity activeConsent = createValidConsent();
            when(consentRepository.findActiveConsentsByPatientAndCategory(eq(TENANT_ID), eq(PATIENT_ID), eq("treatment"), any(LocalDate.class)))
                    .thenReturn(List.of(activeConsent));

            List<ConsentEntity> result = consentService.getActiveConsentsByPatientAndCategory(TENANT_ID, PATIENT_ID, "treatment");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return active consents for patient and data class")
        void shouldReturnActiveConsentsForPatientAndDataClass() {
            ConsentEntity activeConsent = createValidConsent();
            when(consentRepository.findActiveConsentsByPatientAndDataClass(eq(TENANT_ID), eq(PATIENT_ID), eq("mental-health"), any(LocalDate.class)))
                    .thenReturn(List.of(activeConsent));

            List<ConsentEntity> result = consentService.getActiveConsentsByPatientAndDataClass(TENANT_ID, PATIENT_ID, "mental-health");

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Should return revoked consents for patient")
        void shouldReturnRevokedConsentsForPatient() {
            ConsentEntity revokedConsent = createValidConsent();
            revokedConsent.setStatus("revoked");
            when(consentRepository.findRevokedConsentsByPatient(eq(TENANT_ID), eq(PATIENT_ID)))
                    .thenReturn(List.of(revokedConsent));

            List<ConsentEntity> result = consentService.getRevokedConsentsByPatient(TENANT_ID, PATIENT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("revoked");
        }

        @Test
        @DisplayName("Should return expired consents for patient")
        void shouldReturnExpiredConsentsForPatient() {
            ConsentEntity expiredConsent = createValidConsent();
            expiredConsent.setStatus("expired");
            when(consentRepository.findExpiredConsentsByPatient(eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class)))
                    .thenReturn(List.of(expiredConsent));

            List<ConsentEntity> result = consentService.getExpiredConsentsByPatient(TENANT_ID, PATIENT_ID);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo("expired");
        }

        @Test
        @DisplayName("Should return paginated consents for patient")
        void shouldReturnPaginatedConsentsForPatient() {
            ConsentEntity consent = createValidConsent();
            Page<ConsentEntity> page = new PageImpl<>(List.of(consent));
            when(consentRepository.findByTenantIdAndPatientIdOrderByConsentDateDesc(eq(TENANT_ID), eq(PATIENT_ID), any(Pageable.class)))
                    .thenReturn(page);

            Page<ConsentEntity> result = consentService.getConsentsByPatient(TENANT_ID, PATIENT_ID, Pageable.unpaged());

            assertThat(result.getContent()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Consent Check Tests")
    class ConsentCheckTests {

        @Test
        @DisplayName("Should return true when active consent exists for scope")
        void shouldReturnTrueWhenActiveConsentExistsForScope() {
            // Given
            when(consentRepository.hasActiveConsentForScope(eq(TENANT_ID), eq(PATIENT_ID), eq("read"), any(LocalDate.class)))
                    .thenReturn(true);

            // When
            boolean result = consentService.hasActiveConsentForScope(TENANT_ID, PATIENT_ID, "read");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false when no active consent for scope")
        void shouldReturnFalseWhenNoActiveConsentForScope() {
            // Given
            when(consentRepository.hasActiveConsentForScope(eq(TENANT_ID), eq(PATIENT_ID), eq("write"), any(LocalDate.class)))
                    .thenReturn(false);

            // When
            boolean result = consentService.hasActiveConsentForScope(TENANT_ID, PATIENT_ID, "write");

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should check consent for category")
        void shouldCheckConsentForCategory() {
            // Given
            when(consentRepository.hasActiveConsentForCategory(eq(TENANT_ID), eq(PATIENT_ID), eq("treatment"), any(LocalDate.class)))
                    .thenReturn(true);

            // When
            boolean result = consentService.hasActiveConsentForCategory(TENANT_ID, PATIENT_ID, "treatment");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should check consent for data class")
        void shouldCheckConsentForDataClass() {
            // Given
            when(consentRepository.hasActiveConsentForDataClass(eq(TENANT_ID), eq(PATIENT_ID), eq("mental-health"), any(LocalDate.class)))
                    .thenReturn(true);

            // When
            boolean result = consentService.hasActiveConsentForDataClass(TENANT_ID, PATIENT_ID, "mental-health");

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should check consent for authorized party")
        void shouldCheckConsentForAuthorizedParty() {
            // Given
            when(consentRepository.hasActiveConsentForAuthorizedParty(eq(TENANT_ID), eq(PATIENT_ID), eq("org-123"), any(LocalDate.class)))
                    .thenReturn(true);

            // When
            boolean result = consentService.hasActiveConsentForAuthorizedParty(TENANT_ID, PATIENT_ID, "org-123");

            // Then
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Validate Data Access Tests")
    class ValidateDataAccessTests {

        @Test
        @DisplayName("Should permit access when matching consent exists")
        void shouldPermitAccessWhenMatchingConsentExists() {
            // Given
            ConsentEntity consent = createValidConsent();
            consent.setProvisionType("permit");
            consent.setScope("read");
            consent.setCategory("treatment");

            when(consentRepository.findActiveConsentsByPatient(eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class)))
                    .thenReturn(List.of(consent));

            // When
            ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
                    TENANT_ID, PATIENT_ID, "read", "treatment", null, null);

            // Then
            assertThat(result.isPermitted()).isTrue();
            assertThat(result.getReason()).contains("Active consent found");
        }

        @Test
        @DisplayName("Should deny access when no consents exist")
        void shouldDenyAccessWhenNoConsentsExist() {
            // Given
            when(consentRepository.findActiveConsentsByPatient(eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class)))
                    .thenReturn(List.of());

            // When
            ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
                    TENANT_ID, PATIENT_ID, "read", "treatment", null, null);

            // Then
            assertThat(result.isPermitted()).isFalse();
            assertThat(result.getReason()).contains("No active consents");
        }

        @Test
        @DisplayName("Should deny access when provision type is deny")
        void shouldDenyAccessWhenProvisionTypeIsDeny() {
            // Given
            ConsentEntity consent = createValidConsent();
            consent.setProvisionType("deny");
            consent.setScope("read");

            when(consentRepository.findActiveConsentsByPatient(eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class)))
                    .thenReturn(List.of(consent));

            // When
            ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
                    TENANT_ID, PATIENT_ID, "read", "treatment", null, null);

            // Then
            assertThat(result.isPermitted()).isFalse();
        }

        @Test
        @DisplayName("Should permit access with full scope consent")
        void shouldPermitAccessWithFullScopeConsent() {
            // Given
            ConsentEntity consent = createValidConsent();
            consent.setProvisionType("permit");
            consent.setScope("full");
            consent.setCategory("treatment");

            when(consentRepository.findActiveConsentsByPatient(eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class)))
                    .thenReturn(List.of(consent));

            // When
            ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
                    TENANT_ID, PATIENT_ID, "read", "treatment", null, null);

            // Then
            assertThat(result.isPermitted()).isTrue();
        }

        @Test
        @DisplayName("Should deny access when scope does not match")
        void shouldDenyAccessWhenScopeMismatch() {
            ConsentEntity consent = createValidConsent();
            consent.setProvisionType("permit");
            consent.setScope("write");
            consent.setCategory("treatment");

            when(consentRepository.findActiveConsentsByPatient(eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class)))
                    .thenReturn(List.of(consent));

            ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
                    TENANT_ID, PATIENT_ID, "read", "treatment", null, null);

            assertThat(result.isPermitted()).isFalse();
        }

        @Test
        @DisplayName("Should deny access when category does not match")
        void shouldDenyAccessWhenCategoryMismatch() {
            ConsentEntity consent = createValidConsent();
            consent.setProvisionType("permit");
            consent.setScope("read");
            consent.setCategory("research");

            when(consentRepository.findActiveConsentsByPatient(eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class)))
                    .thenReturn(List.of(consent));

            ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
                    TENANT_ID, PATIENT_ID, "read", "treatment", null, null);

            assertThat(result.isPermitted()).isFalse();
        }

        @Test
        @DisplayName("Should deny access when data class does not match")
        void shouldDenyAccessWhenDataClassMismatch() {
            ConsentEntity consent = createValidConsent();
            consent.setProvisionType("permit");
            consent.setScope("read");
            consent.setCategory("treatment");
            consent.setDataClass("mental-health");

            when(consentRepository.findActiveConsentsByPatient(eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class)))
                    .thenReturn(List.of(consent));

            ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
                    TENANT_ID, PATIENT_ID, "read", "treatment", "hiv", null);

            assertThat(result.isPermitted()).isFalse();
        }

        @Test
        @DisplayName("Should deny access when authorized party does not match")
        void shouldDenyAccessWhenAuthorizedPartyMismatch() {
            ConsentEntity consent = createValidConsent();
            consent.setProvisionType("permit");
            consent.setScope("read");
            consent.setCategory("treatment");
            consent.setAuthorizedPartyId("org-123");

            when(consentRepository.findActiveConsentsByPatient(eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class)))
                    .thenReturn(List.of(consent));

            ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
                    TENANT_ID, PATIENT_ID, "read", "treatment", null, "org-999");

            assertThat(result.isPermitted()).isFalse();
        }
    }

    @Nested
    @DisplayName("Get Expiring Consents Tests")
    class GetExpiringConsentsTests {

        @Test
        @DisplayName("Should return consents expiring soon")
        void shouldReturnConsentsExpiringSoon() {
            // Given
            ConsentEntity expiringConsent = createValidConsent();
            expiringConsent.setValidTo(LocalDate.now().plusDays(15));

            when(consentRepository.findConsentsExpiringSoon(eq(TENANT_ID), eq(PATIENT_ID), any(LocalDate.class), any(LocalDate.class)))
                    .thenReturn(List.of(expiringConsent));

            // When
            List<ConsentEntity> result = consentService.getConsentsExpiringSoon(TENANT_ID, PATIENT_ID, 30);

            // Then
            assertThat(result).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Process Expired Consents Tests")
    class ProcessExpiredConsentsTests {

        @Test
        @DisplayName("Should expire and publish events for expired consents")
        void shouldProcessExpiredConsents() {
            ConsentEntity expired = createValidConsent();
            expired.setId(UUID.randomUUID());
            expired.setStatus("active");

            when(consentRepository.findExpiredConsentsByPatient(eq(TENANT_ID), isNull(), any(LocalDate.class)))
                    .thenReturn(List.of(expired));
            when(consentRepository.save(any(ConsentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                    .thenReturn(CompletableFuture.completedFuture(null));

            int count = consentService.processExpiredConsents(TENANT_ID);

            assertThat(count).isEqualTo(1);
            assertThat(expired.getStatus()).isEqualTo("expired");
            verify(kafkaTemplate).send(eq("consent.expired"), anyString(), anyString());
        }
    }

    // ==================== Helper Methods ====================

    private ConsentEntity createValidConsent() {
        return ConsentEntity.builder()
                .patientId(PATIENT_ID)
                .scope("read")
                .status("active")
                .category("treatment")
                .purpose("Healthcare treatment")
                .provisionType("permit")
                .validFrom(LocalDate.now())
                .validTo(LocalDate.now().plusYears(1))
                .consentDate(LocalDate.now())
                .verificationMethod("electronic-signature")
                .createdAt(LocalDateTime.now())
                .lastModifiedAt(LocalDateTime.now())
                .createdBy(CREATED_BY)
                .lastModifiedBy(CREATED_BY)
                .build();
    }
}
