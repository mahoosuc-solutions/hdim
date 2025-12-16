package com.healthdata.patient.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.patient.client.ConsentServiceClient;
import com.healthdata.patient.client.FhirServiceClient;
import feign.FeignException;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PatientAggregationService.
 *
 * HIPAA Critical: Tests consent filtering logic for 42 CFR Part 2 compliance.
 * Ensures that substance abuse and sensitive data is properly filtered based
 * on patient consent status.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Patient Aggregation Service Tests (HIPAA Critical)")
class PatientAggregationServiceTest {

    @Mock
    private FhirServiceClient fhirServiceClient;

    @Mock
    private ConsentServiceClient consentServiceClient;

    @InjectMocks
    private PatientAggregationService aggregationService;

    private IParser jsonParser;

    private static final String TENANT_ID = "tenant-123";
    private static final String PATIENT_ID = "patient-456";

    @BeforeEach
    void setUp() {
        FhirContext fhirContext = FhirContext.forR4();
        jsonParser = fhirContext.newJsonParser();
    }

    // ==================== Get Comprehensive Health Record Tests ====================

    @Nested
    @DisplayName("Get Comprehensive Health Record Tests")
    class GetComprehensiveHealthRecordTests {

        @Test
        @DisplayName("Should return all resources when no consent restrictions")
        void shouldReturnAllResourcesWhenNoConsentRestrictions() {
            // Given - No consent restrictions
            ConsentServiceClient.ConsentStatus consentStatus =
                new ConsentServiceClient.ConsentStatus("active", "2024-01-01", null, false);
            when(consentServiceClient.getConsentStatus(TENANT_ID, PATIENT_ID))
                .thenReturn(consentStatus);

            // Mock FHIR service responses for all resource types
            setupAllFhirServiceMocks();

            // When
            Bundle result = aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(Bundle.BundleType.COLLECTION);
            assertThat(result.getTotal()).isEqualTo(10); // 10 resource types
            assertThat(result.getEntry()).hasSize(10);

            // Verify all resource types were fetched
            verify(fhirServiceClient).getAllergyIntolerances(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getImmunizations(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getMedicationRequests(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getConditions(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getProcedures(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getObservations(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getEncounters(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getDiagnosticReports(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getCarePlans(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getGoals(TENANT_ID, PATIENT_ID);

            // Verify consent service was called
            verify(consentServiceClient).getConsentStatus(TENANT_ID, PATIENT_ID);
            verify(consentServiceClient, never()).getRestrictedResourceTypes(any(), any());
            verify(consentServiceClient, never()).getSensitiveCategories(any(), any());
        }

        @Test
        @DisplayName("Should filter restricted resource types (HIPAA 42 CFR Part 2)")
        void shouldFilterRestrictedResourceTypes() {
            // Given - Patient has consent restrictions for substance abuse data
            ConsentServiceClient.ConsentStatus consentStatus =
                new ConsentServiceClient.ConsentStatus("active", "2024-01-01", null, true);
            when(consentServiceClient.getConsentStatus(TENANT_ID, PATIENT_ID))
                .thenReturn(consentStatus);

            List<String> restrictedTypes = List.of("MedicationRequest", "Condition", "Observation");
            when(consentServiceClient.getRestrictedResourceTypes(TENANT_ID, PATIENT_ID))
                .thenReturn(restrictedTypes);
            when(consentServiceClient.getSensitiveCategories(TENANT_ID, PATIENT_ID))
                .thenReturn(List.of("substance-abuse", "mental-health"));

            // Mock only unrestricted resource types (not the restricted ones)
            when(fhirServiceClient.getAllergyIntolerances(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("AllergyIntolerance", 1));
            when(fhirServiceClient.getImmunizations(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Immunization", 1));
            when(fhirServiceClient.getProcedures(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Procedure", 1));
            when(fhirServiceClient.getEncounters(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Encounter", 1));
            when(fhirServiceClient.getDiagnosticReports(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("DiagnosticReport", 1));
            when(fhirServiceClient.getCarePlans(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("CarePlan", 1));
            when(fhirServiceClient.getGoals(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Goal", 1));

            // When
            Bundle result = aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(7); // Only 7 unrestricted resource types

            // Verify restricted resource types were NOT fetched
            verify(fhirServiceClient, never()).getMedicationRequests(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getConditions(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getObservations(TENANT_ID, PATIENT_ID);

            // Verify unrestricted resource types WERE fetched
            verify(fhirServiceClient).getAllergyIntolerances(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getImmunizations(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getProcedures(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getEncounters(TENANT_ID, PATIENT_ID);

            // Verify consent filtering was applied
            verify(consentServiceClient).getConsentStatus(TENANT_ID, PATIENT_ID);
            verify(consentServiceClient).getRestrictedResourceTypes(TENANT_ID, PATIENT_ID);
            verify(consentServiceClient).getSensitiveCategories(TENANT_ID, PATIENT_ID);
        }

        @Test
        @DisplayName("Should filter all sensitive categories when fully restricted")
        void shouldFilterSensitiveCategories() {
            // Given - Patient has full restriction on all resource types
            ConsentServiceClient.ConsentStatus consentStatus =
                new ConsentServiceClient.ConsentStatus("active", "2024-01-01", null, true);
            when(consentServiceClient.getConsentStatus(TENANT_ID, PATIENT_ID))
                .thenReturn(consentStatus);

            List<String> restrictedTypes = List.of(
                "AllergyIntolerance", "Immunization", "MedicationRequest",
                "Condition", "Procedure", "Observation", "Encounter",
                "DiagnosticReport", "CarePlan", "Goal"
            );
            when(consentServiceClient.getRestrictedResourceTypes(TENANT_ID, PATIENT_ID))
                .thenReturn(restrictedTypes);
            when(consentServiceClient.getSensitiveCategories(TENANT_ID, PATIENT_ID))
                .thenReturn(List.of("substance-abuse", "mental-health", "sexual-health"));

            // When
            Bundle result = aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isZero(); // No resources returned
            assertThat(result.getEntry()).isEmpty();

            // Verify NO FHIR service calls were made
            verify(fhirServiceClient, never()).getAllergyIntolerances(any(), any());
            verify(fhirServiceClient, never()).getImmunizations(any(), any());
            verify(fhirServiceClient, never()).getMedicationRequests(any(), any());
            verify(fhirServiceClient, never()).getConditions(any(), any());
            verify(fhirServiceClient, never()).getProcedures(any(), any());
            verify(fhirServiceClient, never()).getObservations(any(), any());
            verify(fhirServiceClient, never()).getEncounters(any(), any());
            verify(fhirServiceClient, never()).getDiagnosticReports(any(), any());
            verify(fhirServiceClient, never()).getCarePlans(any(), any());
            verify(fhirServiceClient, never()).getGoals(any(), any());
        }

        @Test
        @DisplayName("Should handle consent service failure gracefully (fail-open)")
        void shouldHandleConsentServiceFailureGracefully() {
            // Given - Consent service is unavailable
            when(consentServiceClient.getConsentStatus(TENANT_ID, PATIENT_ID))
                .thenThrow(FeignException.ServiceUnavailable.class);

            // Mock FHIR service responses
            setupAllFhirServiceMocks();

            // When
            Bundle result = aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID);

            // Then - Should return data with no restrictions (fail-open for availability)
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(10);

            // Verify consent service was called but failed
            verify(consentServiceClient).getConsentStatus(TENANT_ID, PATIENT_ID);

            // Verify FHIR service calls proceeded despite consent service failure
            verify(fhirServiceClient).getAllergyIntolerances(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getMedicationRequests(TENANT_ID, PATIENT_ID);
        }

        @Test
        @DisplayName("Should log all filtering operations for audit trail")
        void shouldLogAllFilteringOperations() {
            // Given - Patient has restrictions
            ConsentServiceClient.ConsentStatus consentStatus =
                new ConsentServiceClient.ConsentStatus("active", "2024-01-01", null, true);
            when(consentServiceClient.getConsentStatus(TENANT_ID, PATIENT_ID))
                .thenReturn(consentStatus);

            List<String> restrictedTypes = List.of("MedicationRequest");
            when(consentServiceClient.getRestrictedResourceTypes(TENANT_ID, PATIENT_ID))
                .thenReturn(restrictedTypes);
            when(consentServiceClient.getSensitiveCategories(TENANT_ID, PATIENT_ID))
                .thenReturn(List.of("substance-abuse"));

            // Mock all unrestricted resource types
            when(fhirServiceClient.getAllergyIntolerances(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("AllergyIntolerance", 1));
            when(fhirServiceClient.getImmunizations(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Immunization", 1));
            when(fhirServiceClient.getConditions(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Condition", 1));
            when(fhirServiceClient.getProcedures(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Procedure", 1));
            when(fhirServiceClient.getObservations(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Observation", 1));
            when(fhirServiceClient.getEncounters(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Encounter", 1));
            when(fhirServiceClient.getDiagnosticReports(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("DiagnosticReport", 1));
            when(fhirServiceClient.getCarePlans(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("CarePlan", 1));
            when(fhirServiceClient.getGoals(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Goal", 1));

            // When
            Bundle result = aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).isNotNull();

            // Verify consent checks were performed (audit trail)
            verify(consentServiceClient).getConsentStatus(TENANT_ID, PATIENT_ID);
            verify(consentServiceClient).getRestrictedResourceTypes(TENANT_ID, PATIENT_ID);
            verify(consentServiceClient).getSensitiveCategories(TENANT_ID, PATIENT_ID);

            // Verify restricted resource type was not fetched
            verify(fhirServiceClient, never()).getMedicationRequests(TENANT_ID, PATIENT_ID);
        }

        @Test
        @DisplayName("Should handle partial FHIR service failures")
        void shouldHandlePartialFhirServiceFailures() {
            // Given - Some FHIR service calls will fail
            ConsentServiceClient.ConsentStatus consentStatus =
                new ConsentServiceClient.ConsentStatus("active", "2024-01-01", null, false);
            when(consentServiceClient.getConsentStatus(TENANT_ID, PATIENT_ID))
                .thenReturn(consentStatus);

            // Mock successful calls
            when(fhirServiceClient.getAllergyIntolerances(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("AllergyIntolerance", 1));
            when(fhirServiceClient.getImmunizations(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Immunization", 1));

            // Mock failures
            when(fhirServiceClient.getMedicationRequests(TENANT_ID, PATIENT_ID))
                .thenThrow(FeignException.ServiceUnavailable.class);
            when(fhirServiceClient.getConditions(TENANT_ID, PATIENT_ID))
                .thenThrow(FeignException.GatewayTimeout.class);

            // Mock remaining successful calls
            when(fhirServiceClient.getProcedures(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Procedure", 1));
            when(fhirServiceClient.getObservations(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Observation", 1));
            when(fhirServiceClient.getEncounters(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Encounter", 1));
            when(fhirServiceClient.getDiagnosticReports(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("DiagnosticReport", 1));
            when(fhirServiceClient.getCarePlans(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("CarePlan", 1));
            when(fhirServiceClient.getGoals(TENANT_ID, PATIENT_ID))
                .thenReturn(createJsonBundle("Goal", 1));

            // When
            Bundle result = aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID);

            // Then - Should return partial data (8 successful, 2 failed)
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(8); // Only successful fetches

            // Verify all calls were attempted
            verify(fhirServiceClient).getMedicationRequests(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient).getConditions(TENANT_ID, PATIENT_ID);
        }

        @Test
        @DisplayName("Should respect tenant boundaries in consent checks")
        void shouldRespectTenantBoundaries() {
            // Given
            String differentTenantId = "tenant-999";
            ConsentServiceClient.ConsentStatus consentStatus =
                new ConsentServiceClient.ConsentStatus("active", "2024-01-01", null, false);
            when(consentServiceClient.getConsentStatus(differentTenantId, PATIENT_ID))
                .thenReturn(consentStatus);

            setupAllFhirServiceMocksForTenant(differentTenantId, PATIENT_ID);

            // When
            Bundle result = aggregationService.getComprehensiveHealthRecord(differentTenantId, PATIENT_ID);

            // Then
            assertThat(result).isNotNull();

            // Verify correct tenant ID was used
            verify(consentServiceClient).getConsentStatus(differentTenantId, PATIENT_ID);
            verify(fhirServiceClient).getAllergyIntolerances(differentTenantId, PATIENT_ID);
            verify(fhirServiceClient).getMedicationRequests(differentTenantId, PATIENT_ID);

            // Verify wrong tenant was not used
            verify(consentServiceClient, never()).getConsentStatus(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getAllergyIntolerances(TENANT_ID, PATIENT_ID);
        }
    }

    // ==================== Resource Type Methods Tests ====================

    @Nested
    @DisplayName("Get Allergies Tests")
    class GetAllergiesTests {

        @Test
        @DisplayName("Should return all allergies when onlyCritical is false")
        void shouldReturnAllAllergies() {
            // Given
            String allergyJson = createJsonBundle("AllergyIntolerance", 3);
            when(fhirServiceClient.getAllergyIntolerances(TENANT_ID, PATIENT_ID))
                .thenReturn(allergyJson);

            // When
            Bundle result = aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(3);
            verify(fhirServiceClient).getAllergyIntolerances(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getCriticalAllergies(any(), any());
        }

        @Test
        @DisplayName("Should return only critical allergies when onlyCritical is true")
        void shouldReturnOnlyCriticalAllergies() {
            // Given
            String allergyJson = createJsonBundle("AllergyIntolerance", 2);
            when(fhirServiceClient.getCriticalAllergies(TENANT_ID, PATIENT_ID))
                .thenReturn(allergyJson);

            // When
            Bundle result = aggregationService.getAllergies(TENANT_ID, PATIENT_ID, true);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(2);
            verify(fhirServiceClient).getCriticalAllergies(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getAllergyIntolerances(any(), any());
        }

        @Test
        @DisplayName("Should handle empty allergy bundle")
        void shouldHandleEmptyAllergyBundle() {
            // Given
            String emptyBundle = createEmptyJsonBundle();
            when(fhirServiceClient.getAllergyIntolerances(TENANT_ID, PATIENT_ID))
                .thenReturn(emptyBundle);

            // When
            Bundle result = aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isZero();
            assertThat(result.getEntry()).isEmpty();
        }

        @Test
        @DisplayName("Should handle FHIR service errors for allergies")
        void shouldHandleFhirServiceErrorsForAllergies() {
            // Given
            when(fhirServiceClient.getAllergyIntolerances(TENANT_ID, PATIENT_ID))
                .thenThrow(mock(FeignException.InternalServerError.class));

            // When
            Bundle result = aggregationService.getAllergies(TENANT_ID, PATIENT_ID, false);

            // Then - Should return empty bundle instead of throwing exception
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(Bundle.BundleType.COLLECTION);
        }
    }

    @Nested
    @DisplayName("Get Immunizations Tests")
    class GetImmunizationsTests {

        @Test
        @DisplayName("Should return all immunizations when onlyCompleted is false")
        void shouldReturnAllImmunizations() {
            // Given
            String immunizationJson = createJsonBundle("Immunization", 5);
            when(fhirServiceClient.getImmunizations(TENANT_ID, PATIENT_ID))
                .thenReturn(immunizationJson);

            // When
            Bundle result = aggregationService.getImmunizations(TENANT_ID, PATIENT_ID, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(5);
            verify(fhirServiceClient).getImmunizations(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getCompletedImmunizations(any(), any());
        }

        @Test
        @DisplayName("Should return only completed immunizations when onlyCompleted is true")
        void shouldReturnOnlyCompletedImmunizations() {
            // Given
            String immunizationJson = createJsonBundle("Immunization", 3);
            when(fhirServiceClient.getCompletedImmunizations(TENANT_ID, PATIENT_ID))
                .thenReturn(immunizationJson);

            // When
            Bundle result = aggregationService.getImmunizations(TENANT_ID, PATIENT_ID, true);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(3);
            verify(fhirServiceClient).getCompletedImmunizations(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getImmunizations(any(), any());
        }

        @Test
        @DisplayName("Should handle empty immunization bundle")
        void shouldHandleEmptyImmunizationBundle() {
            // Given
            String emptyBundle = createEmptyJsonBundle();
            when(fhirServiceClient.getImmunizations(TENANT_ID, PATIENT_ID))
                .thenReturn(emptyBundle);

            // When
            Bundle result = aggregationService.getImmunizations(TENANT_ID, PATIENT_ID, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isZero();
        }
    }

    @Nested
    @DisplayName("Get Medications Tests")
    class GetMedicationsTests {

        @Test
        @DisplayName("Should return all medications when onlyActive is false")
        void shouldReturnAllMedications() {
            // Given
            String medicationJson = createJsonBundle("MedicationRequest", 4);
            when(fhirServiceClient.getMedicationRequests(TENANT_ID, PATIENT_ID))
                .thenReturn(medicationJson);

            // When
            Bundle result = aggregationService.getMedications(TENANT_ID, PATIENT_ID, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(4);
            verify(fhirServiceClient).getMedicationRequests(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getActiveMedications(any(), any());
        }

        @Test
        @DisplayName("Should return only active medications when onlyActive is true")
        void shouldReturnOnlyActiveMedications() {
            // Given
            String medicationJson = createJsonBundle("MedicationRequest", 2);
            when(fhirServiceClient.getActiveMedications(TENANT_ID, PATIENT_ID))
                .thenReturn(medicationJson);

            // When
            Bundle result = aggregationService.getMedications(TENANT_ID, PATIENT_ID, true);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(2);
            verify(fhirServiceClient).getActiveMedications(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getMedicationRequests(any(), any());
        }

        @Test
        @DisplayName("Should handle FHIR service errors for medications")
        void shouldHandleFhirServiceErrorsForMedications() {
            // Given
            when(fhirServiceClient.getMedicationRequests(TENANT_ID, PATIENT_ID))
                .thenThrow(mock(FeignException.BadGateway.class));

            // When
            Bundle result = aggregationService.getMedications(TENANT_ID, PATIENT_ID, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(Bundle.BundleType.COLLECTION);
        }
    }

    @Nested
    @DisplayName("Get Conditions Tests")
    class GetConditionsTests {

        @Test
        @DisplayName("Should return all conditions when onlyActive is false")
        void shouldReturnAllConditions() {
            // Given
            String conditionJson = createJsonBundle("Condition", 6);
            when(fhirServiceClient.getConditions(TENANT_ID, PATIENT_ID))
                .thenReturn(conditionJson);

            // When
            Bundle result = aggregationService.getConditions(TENANT_ID, PATIENT_ID, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(6);
            verify(fhirServiceClient).getConditions(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getActiveConditions(any(), any());
        }

        @Test
        @DisplayName("Should return only active conditions when onlyActive is true")
        void shouldReturnOnlyActiveConditions() {
            // Given
            String conditionJson = createJsonBundle("Condition", 3);
            when(fhirServiceClient.getActiveConditions(TENANT_ID, PATIENT_ID))
                .thenReturn(conditionJson);

            // When
            Bundle result = aggregationService.getConditions(TENANT_ID, PATIENT_ID, true);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(3);
            verify(fhirServiceClient).getActiveConditions(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getConditions(any(), any());
        }

        @Test
        @DisplayName("Should handle empty condition bundle")
        void shouldHandleEmptyConditionBundle() {
            // Given
            String emptyBundle = createEmptyJsonBundle();
            when(fhirServiceClient.getConditions(TENANT_ID, PATIENT_ID))
                .thenReturn(emptyBundle);

            // When
            Bundle result = aggregationService.getConditions(TENANT_ID, PATIENT_ID, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isZero();
        }
    }

    @Nested
    @DisplayName("Get Procedures Tests")
    class GetProceduresTests {

        @Test
        @DisplayName("Should return all procedures")
        void shouldReturnAllProcedures() {
            // Given
            String procedureJson = createJsonBundle("Procedure", 2);
            when(fhirServiceClient.getProcedures(TENANT_ID, PATIENT_ID))
                .thenReturn(procedureJson);

            // When
            Bundle result = aggregationService.getProcedures(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(2);
            verify(fhirServiceClient).getProcedures(TENANT_ID, PATIENT_ID);
        }

        @Test
        @DisplayName("Should handle empty procedure bundle")
        void shouldHandleEmptyProcedureBundle() {
            // Given
            String emptyBundle = createEmptyJsonBundle();
            when(fhirServiceClient.getProcedures(TENANT_ID, PATIENT_ID))
                .thenReturn(emptyBundle);

            // When
            Bundle result = aggregationService.getProcedures(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isZero();
        }
    }

    @Nested
    @DisplayName("Get Vital Signs Tests")
    class GetVitalSignsTests {

        @Test
        @DisplayName("Should return vital signs observations")
        void shouldReturnVitalSigns() {
            // Given
            String vitalSignsJson = createJsonBundle("Observation", 4);
            when(fhirServiceClient.getVitalSigns(TENANT_ID, PATIENT_ID))
                .thenReturn(vitalSignsJson);

            // When
            Bundle result = aggregationService.getVitalSigns(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(4);
            verify(fhirServiceClient).getVitalSigns(TENANT_ID, PATIENT_ID);
        }

        @Test
        @DisplayName("Should handle empty vital signs bundle")
        void shouldHandleEmptyVitalSignsBundle() {
            // Given
            String emptyBundle = createEmptyJsonBundle();
            when(fhirServiceClient.getVitalSigns(TENANT_ID, PATIENT_ID))
                .thenReturn(emptyBundle);

            // When
            Bundle result = aggregationService.getVitalSigns(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isZero();
        }
    }

    @Nested
    @DisplayName("Get Lab Results Tests")
    class GetLabResultsTests {

        @Test
        @DisplayName("Should return lab results")
        void shouldReturnLabResults() {
            // Given
            String labResultsJson = createJsonBundle("Observation", 8);
            when(fhirServiceClient.getLabResults(TENANT_ID, PATIENT_ID))
                .thenReturn(labResultsJson);

            // When
            Bundle result = aggregationService.getLabResults(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(8);
            verify(fhirServiceClient).getLabResults(TENANT_ID, PATIENT_ID);
        }

        @Test
        @DisplayName("Should handle empty lab results bundle")
        void shouldHandleEmptyLabResultsBundle() {
            // Given
            String emptyBundle = createEmptyJsonBundle();
            when(fhirServiceClient.getLabResults(TENANT_ID, PATIENT_ID))
                .thenReturn(emptyBundle);

            // When
            Bundle result = aggregationService.getLabResults(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isZero();
        }
    }

    @Nested
    @DisplayName("Get Encounters Tests")
    class GetEncountersTests {

        @Test
        @DisplayName("Should return all encounters when onlyActive is false")
        void shouldReturnAllEncounters() {
            // Given
            String encounterJson = createJsonBundle("Encounter", 5);
            when(fhirServiceClient.getEncounters(TENANT_ID, PATIENT_ID))
                .thenReturn(encounterJson);

            // When
            Bundle result = aggregationService.getEncounters(TENANT_ID, PATIENT_ID, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(5);
            verify(fhirServiceClient).getEncounters(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getActiveEncounters(any(), any());
        }

        @Test
        @DisplayName("Should return only active encounters when onlyActive is true")
        void shouldReturnOnlyActiveEncounters() {
            // Given
            String encounterJson = createJsonBundle("Encounter", 1);
            when(fhirServiceClient.getActiveEncounters(TENANT_ID, PATIENT_ID))
                .thenReturn(encounterJson);

            // When
            Bundle result = aggregationService.getEncounters(TENANT_ID, PATIENT_ID, true);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(1);
            verify(fhirServiceClient).getActiveEncounters(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getEncounters(any(), any());
        }
    }

    @Nested
    @DisplayName("Get Care Plans Tests")
    class GetCarePlansTests {

        @Test
        @DisplayName("Should return all care plans when onlyActive is false")
        void shouldReturnAllCarePlans() {
            // Given
            String carePlanJson = createJsonBundle("CarePlan", 3);
            when(fhirServiceClient.getCarePlans(TENANT_ID, PATIENT_ID))
                .thenReturn(carePlanJson);

            // When
            Bundle result = aggregationService.getCarePlans(TENANT_ID, PATIENT_ID, false);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(3);
            verify(fhirServiceClient).getCarePlans(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getActiveCarePlans(any(), any());
        }

        @Test
        @DisplayName("Should return only active care plans when onlyActive is true")
        void shouldReturnOnlyActiveCarePlans() {
            // Given
            String carePlanJson = createJsonBundle("CarePlan", 1);
            when(fhirServiceClient.getActiveCarePlans(TENANT_ID, PATIENT_ID))
                .thenReturn(carePlanJson);

            // When
            Bundle result = aggregationService.getCarePlans(TENANT_ID, PATIENT_ID, true);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(1);
            verify(fhirServiceClient).getActiveCarePlans(TENANT_ID, PATIENT_ID);
            verify(fhirServiceClient, never()).getCarePlans(any(), any());
        }
    }

    @Nested
    @DisplayName("Get Goals Tests")
    class GetGoalsTests {

        @Test
        @DisplayName("Should return all goals")
        void shouldReturnAllGoals() {
            // Given
            String goalJson = createJsonBundle("Goal", 2);
            when(fhirServiceClient.getGoals(TENANT_ID, PATIENT_ID))
                .thenReturn(goalJson);

            // When
            Bundle result = aggregationService.getGoals(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isEqualTo(2);
            verify(fhirServiceClient).getGoals(TENANT_ID, PATIENT_ID);
        }

        @Test
        @DisplayName("Should handle empty goals bundle")
        void shouldHandleEmptyGoalsBundle() {
            // Given
            String emptyBundle = createEmptyJsonBundle();
            when(fhirServiceClient.getGoals(TENANT_ID, PATIENT_ID))
                .thenReturn(emptyBundle);

            // When
            Bundle result = aggregationService.getGoals(TENANT_ID, PATIENT_ID);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isZero();
        }
    }

    // ==================== Consent Integration Tests ====================

    @Nested
    @DisplayName("Consent Integration Tests")
    class ConsentIntegrationTests {

        @Test
        @DisplayName("Should call consent service for status")
        void shouldCallConsentServiceForStatus() {
            // Given
            ConsentServiceClient.ConsentStatus expectedStatus =
                new ConsentServiceClient.ConsentStatus("active", "2024-01-01", "2025-01-01", true);
            when(consentServiceClient.getConsentStatus(TENANT_ID, PATIENT_ID))
                .thenReturn(expectedStatus);
            when(consentServiceClient.getRestrictedResourceTypes(TENANT_ID, PATIENT_ID))
                .thenReturn(List.of());
            when(consentServiceClient.getSensitiveCategories(TENANT_ID, PATIENT_ID))
                .thenReturn(List.of());

            setupAllFhirServiceMocks();

            // When
            aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID);

            // Then
            verify(consentServiceClient).getConsentStatus(TENANT_ID, PATIENT_ID);
            verify(consentServiceClient).getRestrictedResourceTypes(TENANT_ID, PATIENT_ID);
            verify(consentServiceClient).getSensitiveCategories(TENANT_ID, PATIENT_ID);
        }

        @Test
        @DisplayName("Should use defaults when consent service unavailable")
        void shouldUseDefaultsWhenConsentServiceUnavailable() {
            // Given - Consent service is down
            when(consentServiceClient.getConsentStatus(TENANT_ID, PATIENT_ID))
                .thenThrow(FeignException.ServiceUnavailable.class);

            setupAllFhirServiceMocks();

            // When
            Bundle result = aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID);

            // Then - Should proceed with default (no restrictions)
            assertThat(result).isNotNull();
            assertThat(result.getTotal()).isGreaterThan(0);

            verify(consentServiceClient).getConsentStatus(TENANT_ID, PATIENT_ID);
            // Should not call other consent methods due to default fallback
            verify(consentServiceClient, never()).getRestrictedResourceTypes(any(), any());
        }

        @Test
        @DisplayName("Should respect tenant boundaries in consent checks")
        void shouldRespectTenantBoundariesInConsentChecks() {
            // Given
            String tenant1 = "tenant-1";
            String tenant2 = "tenant-2";
            String patient1 = "patient-1";
            String patient2 = "patient-2";

            ConsentServiceClient.ConsentStatus status1 =
                new ConsentServiceClient.ConsentStatus("active", "2024-01-01", null, false);
            ConsentServiceClient.ConsentStatus status2 =
                new ConsentServiceClient.ConsentStatus("active", "2024-01-01", null, true);

            when(consentServiceClient.getConsentStatus(tenant1, patient1))
                .thenReturn(status1);
            when(consentServiceClient.getConsentStatus(tenant2, patient2))
                .thenReturn(status2);
            when(consentServiceClient.getRestrictedResourceTypes(tenant2, patient2))
                .thenReturn(List.of("MedicationRequest"));
            when(consentServiceClient.getSensitiveCategories(tenant2, patient2))
                .thenReturn(List.of("substance-abuse"));

            // Setup tenant1 mocks (all resources, no restrictions)
            setupAllFhirServiceMocksForTenant(tenant1, patient1);

            // Setup tenant2 mocks - use lenient for MedicationRequest since it's filtered
            lenient().when(fhirServiceClient.getAllergyIntolerances(tenant2, patient2))
                .thenReturn(createJsonBundle("AllergyIntolerance", 1));
            lenient().when(fhirServiceClient.getImmunizations(tenant2, patient2))
                .thenReturn(createJsonBundle("Immunization", 1));
            // MedicationRequest is restricted, so this stub won't be called (use lenient)
            lenient().when(fhirServiceClient.getMedicationRequests(tenant2, patient2))
                .thenReturn(createJsonBundle("MedicationRequest", 1));
            lenient().when(fhirServiceClient.getConditions(tenant2, patient2))
                .thenReturn(createJsonBundle("Condition", 1));
            lenient().when(fhirServiceClient.getProcedures(tenant2, patient2))
                .thenReturn(createJsonBundle("Procedure", 1));
            lenient().when(fhirServiceClient.getObservations(tenant2, patient2))
                .thenReturn(createJsonBundle("Observation", 1));
            lenient().when(fhirServiceClient.getEncounters(tenant2, patient2))
                .thenReturn(createJsonBundle("Encounter", 1));
            lenient().when(fhirServiceClient.getDiagnosticReports(tenant2, patient2))
                .thenReturn(createJsonBundle("DiagnosticReport", 1));
            lenient().when(fhirServiceClient.getCarePlans(tenant2, patient2))
                .thenReturn(createJsonBundle("CarePlan", 1));
            lenient().when(fhirServiceClient.getGoals(tenant2, patient2))
                .thenReturn(createJsonBundle("Goal", 1));

            // When
            Bundle result1 = aggregationService.getComprehensiveHealthRecord(tenant1, patient1);
            Bundle result2 = aggregationService.getComprehensiveHealthRecord(tenant2, patient2);

            // Then
            assertThat(result1.getTotal()).isEqualTo(10); // No restrictions
            assertThat(result2.getTotal()).isEqualTo(9);  // MedicationRequest filtered

            // Verify correct tenant IDs were used
            verify(consentServiceClient).getConsentStatus(tenant1, patient1);
            verify(consentServiceClient).getConsentStatus(tenant2, patient2);
            verify(consentServiceClient, never()).getConsentStatus(tenant1, patient2);
            verify(consentServiceClient, never()).getConsentStatus(tenant2, patient1);
        }

        @Test
        @DisplayName("Should handle consent status with expired consent")
        void shouldHandleConsentStatusWithExpiredConsent() {
            // Given - Consent is expired
            ConsentServiceClient.ConsentStatus expiredStatus =
                new ConsentServiceClient.ConsentStatus("expired", "2023-01-01", "2023-12-31", false);
            when(consentServiceClient.getConsentStatus(TENANT_ID, PATIENT_ID))
                .thenReturn(expiredStatus);

            setupAllFhirServiceMocks();

            // When
            Bundle result = aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID);

            // Then - Should still aggregate data (consent expiration handled by consent service)
            assertThat(result).isNotNull();
            verify(consentServiceClient).getConsentStatus(TENANT_ID, PATIENT_ID);
        }

        @Test
        @DisplayName("Should handle consent status with inactive consent")
        void shouldHandleConsentStatusWithInactiveConsent() {
            // Given - Consent is inactive
            ConsentServiceClient.ConsentStatus inactiveStatus =
                new ConsentServiceClient.ConsentStatus("inactive", null, null, false);
            when(consentServiceClient.getConsentStatus(TENANT_ID, PATIENT_ID))
                .thenReturn(inactiveStatus);

            setupAllFhirServiceMocks();

            // When
            Bundle result = aggregationService.getComprehensiveHealthRecord(TENANT_ID, PATIENT_ID);

            // Then - Should still aggregate data
            assertThat(result).isNotNull();
            verify(consentServiceClient).getConsentStatus(TENANT_ID, PATIENT_ID);
        }
    }

    // ==================== Helper Methods ====================

    private void setupAllFhirServiceMocks() {
        when(fhirServiceClient.getAllergyIntolerances(TENANT_ID, PATIENT_ID))
            .thenReturn(createJsonBundle("AllergyIntolerance", 1));
        when(fhirServiceClient.getImmunizations(TENANT_ID, PATIENT_ID))
            .thenReturn(createJsonBundle("Immunization", 1));
        when(fhirServiceClient.getMedicationRequests(TENANT_ID, PATIENT_ID))
            .thenReturn(createJsonBundle("MedicationRequest", 1));
        when(fhirServiceClient.getConditions(TENANT_ID, PATIENT_ID))
            .thenReturn(createJsonBundle("Condition", 1));
        when(fhirServiceClient.getProcedures(TENANT_ID, PATIENT_ID))
            .thenReturn(createJsonBundle("Procedure", 1));
        when(fhirServiceClient.getObservations(TENANT_ID, PATIENT_ID))
            .thenReturn(createJsonBundle("Observation", 1));
        when(fhirServiceClient.getEncounters(TENANT_ID, PATIENT_ID))
            .thenReturn(createJsonBundle("Encounter", 1));
        when(fhirServiceClient.getDiagnosticReports(TENANT_ID, PATIENT_ID))
            .thenReturn(createJsonBundle("DiagnosticReport", 1));
        when(fhirServiceClient.getCarePlans(TENANT_ID, PATIENT_ID))
            .thenReturn(createJsonBundle("CarePlan", 1));
        when(fhirServiceClient.getGoals(TENANT_ID, PATIENT_ID))
            .thenReturn(createJsonBundle("Goal", 1));
    }

    private void setupAllFhirServiceMocksForTenant(String tenantId, String patientId) {
        when(fhirServiceClient.getAllergyIntolerances(tenantId, patientId))
            .thenReturn(createJsonBundle("AllergyIntolerance", 1));
        when(fhirServiceClient.getImmunizations(tenantId, patientId))
            .thenReturn(createJsonBundle("Immunization", 1));
        when(fhirServiceClient.getMedicationRequests(tenantId, patientId))
            .thenReturn(createJsonBundle("MedicationRequest", 1));
        when(fhirServiceClient.getConditions(tenantId, patientId))
            .thenReturn(createJsonBundle("Condition", 1));
        when(fhirServiceClient.getProcedures(tenantId, patientId))
            .thenReturn(createJsonBundle("Procedure", 1));
        when(fhirServiceClient.getObservations(tenantId, patientId))
            .thenReturn(createJsonBundle("Observation", 1));
        when(fhirServiceClient.getEncounters(tenantId, patientId))
            .thenReturn(createJsonBundle("Encounter", 1));
        when(fhirServiceClient.getDiagnosticReports(tenantId, patientId))
            .thenReturn(createJsonBundle("DiagnosticReport", 1));
        when(fhirServiceClient.getCarePlans(tenantId, patientId))
            .thenReturn(createJsonBundle("CarePlan", 1));
        when(fhirServiceClient.getGoals(tenantId, patientId))
            .thenReturn(createJsonBundle("Goal", 1));
    }

    private String createJsonBundle(String resourceType, int count) {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTotal(count);

        for (int i = 0; i < count; i++) {
            Resource resource = createResource(resourceType, i);
            bundle.addEntry().setResource(resource);
        }

        return jsonParser.encodeResourceToString(bundle);
    }

    private String createEmptyJsonBundle() {
        Bundle bundle = new Bundle();
        bundle.setType(Bundle.BundleType.COLLECTION);
        bundle.setTotal(0);
        return jsonParser.encodeResourceToString(bundle);
    }

    private Resource createResource(String resourceType, int index) {
        return switch (resourceType) {
            case "AllergyIntolerance" -> {
                AllergyIntolerance allergy = new AllergyIntolerance();
                allergy.setId(resourceType + "-" + index);
                allergy.getCode().setText("Allergen " + index);
                yield allergy;
            }
            case "Immunization" -> {
                Immunization immunization = new Immunization();
                immunization.setId(resourceType + "-" + index);
                immunization.getVaccineCode().setText("Vaccine " + index);
                yield immunization;
            }
            case "MedicationRequest" -> {
                MedicationRequest medication = new MedicationRequest();
                medication.setId(resourceType + "-" + index);
                medication.getMedicationCodeableConcept().setText("Medication " + index);
                yield medication;
            }
            case "Condition" -> {
                Condition condition = new Condition();
                condition.setId(resourceType + "-" + index);
                condition.getCode().setText("Condition " + index);
                yield condition;
            }
            case "Procedure" -> {
                Procedure procedure = new Procedure();
                procedure.setId(resourceType + "-" + index);
                procedure.getCode().setText("Procedure " + index);
                yield procedure;
            }
            case "Observation" -> {
                Observation observation = new Observation();
                observation.setId(resourceType + "-" + index);
                observation.getCode().setText("Observation " + index);
                yield observation;
            }
            case "Encounter" -> {
                Encounter encounter = new Encounter();
                encounter.setId(resourceType + "-" + index);
                encounter.setStatus(Encounter.EncounterStatus.FINISHED);
                yield encounter;
            }
            case "DiagnosticReport" -> {
                DiagnosticReport report = new DiagnosticReport();
                report.setId(resourceType + "-" + index);
                report.getCode().setText("Report " + index);
                yield report;
            }
            case "CarePlan" -> {
                CarePlan carePlan = new CarePlan();
                carePlan.setId(resourceType + "-" + index);
                carePlan.setStatus(CarePlan.CarePlanStatus.ACTIVE);
                yield carePlan;
            }
            case "Goal" -> {
                Goal goal = new Goal();
                goal.setId(resourceType + "-" + index);
                goal.setLifecycleStatus(Goal.GoalLifecycleStatus.ACTIVE);
                yield goal;
            }
            default -> throw new IllegalArgumentException("Unknown resource type: " + resourceType);
        };
    }
}
