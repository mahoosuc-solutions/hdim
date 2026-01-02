package com.healthdata.priorauth.service;

import com.healthdata.priorauth.dto.PriorAuthRequestDTO;
import com.healthdata.priorauth.persistence.PriorAuthRequestEntity;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for PasClaimBuilder.
 *
 * Tests FHIR Claim resource building per Da Vinci PAS specification.
 */
class PasClaimBuilderTest {

    private PasClaimBuilder pasClaimBuilder;

    @BeforeEach
    void setUp() {
        pasClaimBuilder = new PasClaimBuilder();
    }

    @Nested
    @DisplayName("buildClaim() tests")
    class BuildClaimTests {

        @Test
        @DisplayName("Should build basic claim with required fields")
        void buildClaim_withRequiredFields_shouldBuildValidClaim() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            PriorAuthRequestEntity entity = createBasicEntity();

            // When
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // Then
            assertThat(claim).isNotNull();
            assertThat(claim.getMeta().getProfile()).hasSize(1);
            assertThat(claim.getMeta().getProfile().get(0).getValue())
                .isEqualTo("http://hl7.org/fhir/us/davinci-pas/StructureDefinition/profile-claim");
            assertThat(claim.getStatus()).isEqualTo(Claim.ClaimStatus.ACTIVE);
            assertThat(claim.getUse()).isEqualTo(Claim.Use.PREAUTHORIZATION);
            assertThat(claim.getPatient().getReference()).isEqualTo("Patient/123");
            assertThat(claim.getCreated()).isNotNull();
        }

        @Test
        @DisplayName("Should set identifier from PA request ID")
        void buildClaim_shouldSetIdentifier() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            PriorAuthRequestEntity entity = createBasicEntity();
            entity.setPaRequestId("PA-2024-001");

            // When
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // Then
            assertThat(claim.getIdentifier()).hasSize(1);
            assertThat(claim.getIdentifier().get(0).getSystem()).isEqualTo("urn:healthdata:prior-auth");
            assertThat(claim.getIdentifier().get(0).getValue()).isEqualTo("PA-2024-001");
        }

        @Test
        @DisplayName("Should set insurer from payer ID")
        void buildClaim_shouldSetInsurer() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            request.setPayerId("PAYER001");
            PriorAuthRequestEntity entity = createBasicEntity();

            // When
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // Then
            assertThat(claim.getInsurer()).isNotNull();
            assertThat(claim.getInsurer().getIdentifier().getValue()).isEqualTo("PAYER001");
        }

        @Test
        @DisplayName("Should add diagnosis codes with ICD-10 system")
        void buildClaim_withDiagnosisCodes_shouldAddDiagnoses() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            request.setDiagnosisCodes(Arrays.asList("E11.9", "I10", "J44.9"));
            PriorAuthRequestEntity entity = createBasicEntity();

            // When
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // Then
            assertThat(claim.getDiagnosis()).hasSize(3);
            assertThat(claim.getDiagnosis().get(0).getSequence()).isEqualTo(1);
            assertThat(claim.getDiagnosis().get(0).getDiagnosisCodeableConcept()
                .getCodingFirstRep().getSystem()).isEqualTo("http://hl7.org/fhir/sid/icd-10-cm");
            assertThat(claim.getDiagnosis().get(0).getDiagnosisCodeableConcept()
                .getCodingFirstRep().getCode()).isEqualTo("E11.9");
            assertThat(claim.getDiagnosis().get(1).getSequence()).isEqualTo(2);
            assertThat(claim.getDiagnosis().get(2).getSequence()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should add CPT procedure codes (5 digits)")
        void buildClaim_withCptCodes_shouldUseCptSystem() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            request.setProcedureCodes(Arrays.asList("99213", "99214"));
            PriorAuthRequestEntity entity = createBasicEntity();

            // When
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // Then
            assertThat(claim.getItem()).hasSize(2);
            assertThat(claim.getItem().get(0).getProductOrService()
                .getCodingFirstRep().getSystem()).isEqualTo("http://www.ama-assn.org/go/cpt");
            assertThat(claim.getItem().get(0).getProductOrService()
                .getCodingFirstRep().getCode()).isEqualTo("99213");
        }

        @Test
        @DisplayName("Should add HCPCS procedure codes (non-5-digit)")
        void buildClaim_withHcpcsCodes_shouldUseHcpcsSystem() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            request.setProcedureCodes(Arrays.asList("J3490", "A4206"));
            PriorAuthRequestEntity entity = createBasicEntity();

            // When
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // Then
            assertThat(claim.getItem()).hasSize(2);
            assertThat(claim.getItem().get(0).getProductOrService()
                .getCodingFirstRep().getSystem()).isEqualTo("https://www.cms.gov/Medicare/Coding/HCPCSReleaseCodeSets");
            assertThat(claim.getItem().get(0).getProductOrService()
                .getCodingFirstRep().getCode()).isEqualTo("J3490");
        }

        @Test
        @DisplayName("Should add quantity to items when specified")
        void buildClaim_withQuantity_shouldSetItemQuantity() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            request.setProcedureCodes(List.of("99213"));
            request.setQuantityRequested(5);
            PriorAuthRequestEntity entity = createBasicEntity();

            // When
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // Then
            assertThat(claim.getItem().get(0).getQuantity().getValue().intValue()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should add single service code when no procedure codes")
        void buildClaim_withSingleServiceCode_shouldAddAsItem() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            request.setServiceCode("99213");
            request.setServiceDescription("Office visit");
            request.setQuantityRequested(1);
            request.setProcedureCodes(null);
            PriorAuthRequestEntity entity = createBasicEntity();

            // When
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // Then
            assertThat(claim.getItem()).hasSize(1);
            assertThat(claim.getItem().get(0).getProductOrService()
                .getCodingFirstRep().getCode()).isEqualTo("99213");
            assertThat(claim.getItem().get(0).getProductOrService()
                .getCodingFirstRep().getDisplay()).isEqualTo("Office visit");
        }

        @Test
        @DisplayName("Should map STAT urgency to stat priority")
        void buildClaim_withStatUrgency_shouldSetStatPriority() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            request.setUrgency(PriorAuthRequestEntity.Urgency.STAT);
            PriorAuthRequestEntity entity = createBasicEntity();

            // When
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // Then
            assertThat(claim.getPriority().getCodingFirstRep().getCode()).isEqualTo("stat");
        }

        @Test
        @DisplayName("Should map ROUTINE urgency to normal priority")
        void buildClaim_withRoutineUrgency_shouldSetNormalPriority() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            request.setUrgency(PriorAuthRequestEntity.Urgency.ROUTINE);
            PriorAuthRequestEntity entity = createBasicEntity();

            // When
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // Then
            assertThat(claim.getPriority().getCodingFirstRep().getCode()).isEqualTo("normal");
        }

        @Test
        @DisplayName("Should set provider reference with NPI")
        void buildClaim_withProvider_shouldSetProviderReference() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            request.setProviderId("PROVIDER001");
            request.setProviderNpi("1234567890");
            PriorAuthRequestEntity entity = createBasicEntity();

            // When
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // Then
            assertThat(claim.getProvider()).isNotNull();
            assertThat(claim.getProvider().getIdentifier().getSystem())
                .isEqualTo("http://hl7.org/fhir/sid/us-npi");
            assertThat(claim.getProvider().getIdentifier().getValue()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("Should set facility reference when provided")
        void buildClaim_withFacility_shouldSetFacilityReference() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            request.setFacilityId("FAC001");
            PriorAuthRequestEntity entity = createBasicEntity();

            // When
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // Then
            assertThat(claim.getFacility()).isNotNull();
            assertThat(claim.getFacility().getIdentifier().getValue()).isEqualTo("FAC001");
        }

        @Test
        @DisplayName("Should add insurance with coverage reference")
        void buildClaim_shouldAddInsurance() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            request.setPayerId("PAYER001");
            PriorAuthRequestEntity entity = createBasicEntity();

            // When
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // Then
            assertThat(claim.getInsurance()).hasSize(1);
            assertThat(claim.getInsurance().get(0).getSequence()).isEqualTo(1);
            assertThat(claim.getInsurance().get(0).getFocal()).isTrue();
            assertThat(claim.getInsurance().get(0).getCoverage().getIdentifier().getValue())
                .isEqualTo("PAYER001");
        }

        @Test
        @DisplayName("Should add supporting information")
        void buildClaim_withSupportingInfo_shouldAddSupportingInfoComponents() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            Map<String, Object> supportingInfo = new LinkedHashMap<>();
            supportingInfo.put("clinical-notes", "Patient requires urgent MRI");
            supportingInfo.put("prior-treatment", true);
            request.setSupportingInfo(supportingInfo);
            PriorAuthRequestEntity entity = createBasicEntity();

            // When
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // Then
            assertThat(claim.getSupportingInfo()).hasSize(2);
            assertThat(claim.getSupportingInfo().get(0).getCategory()
                .getCodingFirstRep().getCode()).isEqualTo("clinical-notes");
            assertThat(((StringType) claim.getSupportingInfo().get(0).getValue()).getValue())
                .isEqualTo("Patient requires urgent MRI");
            assertThat(((BooleanType) claim.getSupportingInfo().get(1).getValue()).getValue())
                .isTrue();
        }
    }

    @Nested
    @DisplayName("buildClaimBundle() tests")
    class BuildClaimBundleTests {

        @Test
        @DisplayName("Should create bundle with claim as first entry")
        void buildClaimBundle_shouldHaveClaimAsFirstEntry() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            PriorAuthRequestEntity entity = createBasicEntity();
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // When
            Bundle bundle = pasClaimBuilder.buildClaimBundle(claim, null);

            // Then
            assertThat(bundle.getType()).isEqualTo(Bundle.BundleType.COLLECTION);
            assertThat(bundle.getTimestamp()).isNotNull();
            assertThat(bundle.getEntry()).hasSize(1);
            assertThat(bundle.getEntry().get(0).getResource()).isInstanceOf(Claim.class);
            assertThat(bundle.getEntry().get(0).getFullUrl()).startsWith("urn:uuid:");
        }

        @Test
        @DisplayName("Should include additional resources in bundle")
        void buildClaimBundle_withAdditionalResources_shouldIncludeAll() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            PriorAuthRequestEntity entity = createBasicEntity();
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            Patient patient = new Patient();
            patient.setId("123");
            patient.addName().setFamily("Test").addGiven("Patient");

            Coverage coverage = new Coverage();
            coverage.setId("COV001");

            List<Resource> additionalResources = Arrays.asList(patient, coverage);

            // When
            Bundle bundle = pasClaimBuilder.buildClaimBundle(claim, additionalResources);

            // Then
            assertThat(bundle.getEntry()).hasSize(3);
            assertThat(bundle.getEntry().get(0).getResource()).isInstanceOf(Claim.class);
            assertThat(bundle.getEntry().get(1).getResource()).isInstanceOf(Patient.class);
            assertThat(bundle.getEntry().get(2).getResource()).isInstanceOf(Coverage.class);
        }

        @Test
        @DisplayName("Should handle empty additional resources list")
        void buildClaimBundle_withEmptyAdditionalResources_shouldOnlyHaveClaim() {
            // Given
            PriorAuthRequestDTO request = createBasicRequest();
            PriorAuthRequestEntity entity = createBasicEntity();
            Claim claim = pasClaimBuilder.buildClaim(request, entity, "Patient/123");

            // When
            Bundle bundle = pasClaimBuilder.buildClaimBundle(claim, Collections.emptyList());

            // Then
            assertThat(bundle.getEntry()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("parseClaimResponse() tests")
    class ParseClaimResponseTests {

        @Test
        @DisplayName("Should parse complete outcome as APPROVED")
        void parseClaimResponse_withCompleteOutcome_shouldReturnApproved() {
            // Given
            Map<String, Object> response = new HashMap<>();
            response.put("outcome", "complete");

            // When
            PasClaimBuilder.PaDecision decision = pasClaimBuilder.parseClaimResponse(response);

            // Then
            assertThat(decision.getStatus()).isEqualTo(PriorAuthRequestEntity.Status.APPROVED);
        }

        @Test
        @DisplayName("Should parse partial outcome as PARTIALLY_APPROVED")
        void parseClaimResponse_withPartialOutcome_shouldReturnPartiallyApproved() {
            // Given
            Map<String, Object> response = new HashMap<>();
            response.put("outcome", "partial");

            // When
            PasClaimBuilder.PaDecision decision = pasClaimBuilder.parseClaimResponse(response);

            // Then
            assertThat(decision.getStatus()).isEqualTo(PriorAuthRequestEntity.Status.PARTIALLY_APPROVED);
        }

        @Test
        @DisplayName("Should parse error outcome as ERROR")
        void parseClaimResponse_withErrorOutcome_shouldReturnError() {
            // Given
            Map<String, Object> response = new HashMap<>();
            response.put("outcome", "error");

            // When
            PasClaimBuilder.PaDecision decision = pasClaimBuilder.parseClaimResponse(response);

            // Then
            assertThat(decision.getStatus()).isEqualTo(PriorAuthRequestEntity.Status.ERROR);
        }

        @Test
        @DisplayName("Should parse queued outcome as PENDING_REVIEW")
        void parseClaimResponse_withQueuedOutcome_shouldReturnPendingReview() {
            // Given
            Map<String, Object> response = new HashMap<>();
            response.put("outcome", "queued");

            // When
            PasClaimBuilder.PaDecision decision = pasClaimBuilder.parseClaimResponse(response);

            // Then
            assertThat(decision.getStatus()).isEqualTo(PriorAuthRequestEntity.Status.PENDING_REVIEW);
        }

        @Test
        @DisplayName("Should parse unknown outcome as DENIED")
        void parseClaimResponse_withUnknownOutcome_shouldReturnDenied() {
            // Given
            Map<String, Object> response = new HashMap<>();
            response.put("outcome", "rejected");

            // When
            PasClaimBuilder.PaDecision decision = pasClaimBuilder.parseClaimResponse(response);

            // Then
            assertThat(decision.getStatus()).isEqualTo(PriorAuthRequestEntity.Status.DENIED);
        }

        @Test
        @DisplayName("Should return ERROR status for null response")
        void parseClaimResponse_withNullResponse_shouldReturnError() {
            // When
            PasClaimBuilder.PaDecision decision = pasClaimBuilder.parseClaimResponse(null);

            // Then
            assertThat(decision.getStatus()).isEqualTo(PriorAuthRequestEntity.Status.ERROR);
            assertThat(decision.getReason()).isEqualTo("No response received from payer");
        }

        @Test
        @DisplayName("Should extract authorization number")
        void parseClaimResponse_withPreAuthRef_shouldExtractAuthNumber() {
            // Given
            Map<String, Object> response = new HashMap<>();
            response.put("outcome", "complete");
            response.put("preAuthRef", "AUTH-2024-12345");

            // When
            PasClaimBuilder.PaDecision decision = pasClaimBuilder.parseClaimResponse(response);

            // Then
            assertThat(decision.getAuthNumber()).isEqualTo("AUTH-2024-12345");
        }

        @Test
        @DisplayName("Should extract disposition as reason")
        void parseClaimResponse_withDisposition_shouldExtractReason() {
            // Given
            Map<String, Object> response = new HashMap<>();
            response.put("outcome", "complete");
            response.put("disposition", "Approved for requested service");

            // When
            PasClaimBuilder.PaDecision decision = pasClaimBuilder.parseClaimResponse(response);

            // Then
            assertThat(decision.getReason()).isEqualTo("Approved for requested service");
        }

        @Test
        @DisplayName("Should extract approved quantity from adjudication")
        void parseClaimResponse_withAdjudication_shouldExtractApprovedQuantity() {
            // Given
            Map<String, Object> response = new HashMap<>();
            response.put("outcome", "partial");

            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put("value", 3);

            Map<String, Object> adjudication = new HashMap<>();
            adjudication.put("category", "benefit");
            adjudication.put("value", valueMap);

            Map<String, Object> item = new HashMap<>();
            item.put("adjudication", List.of(adjudication));

            response.put("item", List.of(item));

            // When
            PasClaimBuilder.PaDecision decision = pasClaimBuilder.parseClaimResponse(response);

            // Then
            assertThat(decision.getApprovedQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should handle response without adjudication")
        void parseClaimResponse_withoutAdjudication_shouldNotSetApprovedQuantity() {
            // Given
            Map<String, Object> response = new HashMap<>();
            response.put("outcome", "complete");

            Map<String, Object> item = new HashMap<>();
            response.put("item", List.of(item));

            // When
            PasClaimBuilder.PaDecision decision = pasClaimBuilder.parseClaimResponse(response);

            // Then
            assertThat(decision.getApprovedQuantity()).isNull();
        }

        @Test
        @DisplayName("Should handle case-insensitive outcome values")
        void parseClaimResponse_withUppercaseOutcome_shouldParsecorrectly() {
            // Given
            Map<String, Object> response = new HashMap<>();
            response.put("outcome", "COMPLETE");

            // When
            PasClaimBuilder.PaDecision decision = pasClaimBuilder.parseClaimResponse(response);

            // Then
            assertThat(decision.getStatus()).isEqualTo(PriorAuthRequestEntity.Status.APPROVED);
        }

        @Test
        @DisplayName("Should parse full response with all fields")
        void parseClaimResponse_withFullResponse_shouldExtractAllFields() {
            // Given
            Map<String, Object> response = new HashMap<>();
            response.put("outcome", "complete");
            response.put("preAuthRef", "AUTH-2024-99999");
            response.put("disposition", "Request approved as submitted");

            Map<String, Object> valueMap = new HashMap<>();
            valueMap.put("value", 10);

            Map<String, Object> adjudication = new HashMap<>();
            adjudication.put("category", "benefit");
            adjudication.put("value", valueMap);

            Map<String, Object> item = new HashMap<>();
            item.put("adjudication", List.of(adjudication));

            response.put("item", List.of(item));

            // When
            PasClaimBuilder.PaDecision decision = pasClaimBuilder.parseClaimResponse(response);

            // Then
            assertThat(decision.getStatus()).isEqualTo(PriorAuthRequestEntity.Status.APPROVED);
            assertThat(decision.getAuthNumber()).isEqualTo("AUTH-2024-99999");
            assertThat(decision.getReason()).isEqualTo("Request approved as submitted");
            assertThat(decision.getApprovedQuantity()).isEqualTo(10);
        }
    }

    // Helper methods

    private PriorAuthRequestDTO createBasicRequest() {
        return PriorAuthRequestDTO.builder()
            .patientId(UUID.randomUUID())
            .serviceCode("99213")
            .serviceDescription("Office visit")
            .urgency(PriorAuthRequestEntity.Urgency.ROUTINE)
            .payerId("PAYER001")
            .build();
    }

    private PriorAuthRequestEntity createBasicEntity() {
        return PriorAuthRequestEntity.builder()
            .id(UUID.randomUUID())
            .tenantId("tenant-1")
            .patientId(UUID.randomUUID())
            .paRequestId("PA-" + UUID.randomUUID().toString().substring(0, 8))
            .status(PriorAuthRequestEntity.Status.DRAFT)
            .urgency(PriorAuthRequestEntity.Urgency.ROUTINE)
            .build();
    }
}
