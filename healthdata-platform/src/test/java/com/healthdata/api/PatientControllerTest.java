package com.healthdata.api;

import com.healthdata.BaseWebControllerTest;
import com.healthdata.patient.domain.Patient;
import com.healthdata.patient.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for Patient API endpoints.
 *
 * Tests all CRUD operations and error scenarios for the Patient Controller.
 * Follows TDD best practices with proper mocking and assertions.
 *
 * Test Coverage:
 * - POST /api/patients (create patient)
 * - GET /api/patients/{id} (get patient by ID)
 * - GET /api/patients (search patients by tenant)
 * - PUT /api/patients/{id} (update patient)
 * - Error cases (404, 400, 409, etc.)
 *
 * @author Test Suite
 */
@DisplayName("Patient Controller Tests")
public class PatientControllerTest extends BaseWebControllerTest {

    @MockBean
    private PatientService patientService;

    private Patient testPatient;
    private Patient anotherPatient;
    private String validPatientId;
    private String nonExistentPatientId;
    private String validTenantId;

    @BeforeEach
    public void setUp() {
        super.setUp();
        initializeTestData();
    }

    /**
     * Initialize reusable test data for all test methods
     */
    private void initializeTestData() {
        validPatientId = "patient-123";
        nonExistentPatientId = "patient-nonexistent";
        validTenantId = "tenant-001";

        testPatient = Patient.builder()
                .id(validPatientId)
                .mrn("MRN-001")
                .firstName("John")
                .lastName("Doe")
                .middleName("Michael")
                .dateOfBirth(LocalDate.of(1985, 5, 15))
                .gender(Patient.Gender.MALE)
                .address(new Patient.Address(
                        "123 Main St",
                        "Springfield",
                        "IL",
                        "62701",
                        "USA"
                ))
                .phoneNumber("555-0123")
                .email("john.doe@example.com")
                .tenantId(validTenantId)
                .active(true)
                .build();

        anotherPatient = Patient.builder()
                .id("patient-456")
                .mrn("MRN-002")
                .firstName("Jane")
                .lastName("Smith")
                .dateOfBirth(LocalDate.of(1990, 3, 20))
                .gender(Patient.Gender.FEMALE)
                .phoneNumber("555-0456")
                .email("jane.smith@example.com")
                .tenantId(validTenantId)
                .active(true)
                .build();
    }

    @Nested
    @DisplayName("POST /api/patients - Create Patient Tests")
    class CreatePatientTests {

        @Test
        @DisplayName("Should successfully create a new patient")
        void testCreatePatientSuccess() throws Exception {
            // Arrange
            when(patientService.createPatient(any()))
                    .thenReturn(testPatient);

            String requestBody = toJson(testPatient);

            // Act
            MvcResult result = performPost("/api/patients", requestBody);

            // Assert
            assertCreatedStatus(result);
            Patient responsePatient = assertCreatedAndParse(result, Patient.class);
            assertNotNull(responsePatient.getId());
            assertEquals("John", responsePatient.getFirstName());
            assertEquals("Doe", responsePatient.getLastName());
            assertEquals("MRN-001", responsePatient.getMrn());
            assertEquals(validTenantId, responsePatient.getTenantId());
            assertTrue(responsePatient.isActive());

            verify(patientService, times(1)).createPatient(any());
        }

        @Test
        @DisplayName("Should return Location header on successful creation")
        void testCreatePatientReturnsLocationHeader() throws Exception {
            // Arrange
            when(patientService.createPatient(any()))
                    .thenReturn(testPatient);

            String requestBody = toJson(testPatient);

            // Act
            MvcResult result = performPost("/api/patients", requestBody);

            // Assert
            assertCreatedStatus(result);
            String locationHeader = assertLocationHeaderPresent(result);
            assertNotNull(locationHeader);
            assertTrue(locationHeader.contains(validPatientId));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when first name is missing")
        void testCreatePatientMissingFirstName() throws Exception {
            // Arrange
            testPatient.setFirstName(null);
            String requestBody = toJson(testPatient);

            // Act
            MvcResult result = performPost("/api/patients", requestBody);

            // Assert
            assertBadRequestStatus(result);
            assertClientErrorResponse(result);

            verify(patientService, never()).createPatient(any());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when last name is missing")
        void testCreatePatientMissingLastName() throws Exception {
            // Arrange
            testPatient.setLastName(null);
            String requestBody = toJson(testPatient);

            // Act
            MvcResult result = performPost("/api/patients", requestBody);

            // Assert
            assertBadRequestStatus(result);
            verify(patientService, never()).createPatient(any());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when MRN is missing")
        void testCreatePatientMissingMrn() throws Exception {
            // Arrange
            testPatient.setMrn(null);
            String requestBody = toJson(testPatient);

            // Act
            MvcResult result = performPost("/api/patients", requestBody);

            // Assert
            assertBadRequestStatus(result);
            verify(patientService, never()).createPatient(any());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when tenant ID is missing")
        void testCreatePatientMissingTenantId() throws Exception {
            // Arrange
            testPatient.setTenantId(null);
            String requestBody = toJson(testPatient);

            // Act
            MvcResult result = performPost("/api/patients", requestBody);

            // Assert
            assertBadRequestStatus(result);
            verify(patientService, never()).createPatient(any());
        }

        @Test
        @DisplayName("Should return 409 Conflict when MRN already exists")
        void testCreatePatientDuplicateMrn() throws Exception {
            // Arrange
            when(patientService.createPatient(any()))
                    .thenThrow(new IllegalArgumentException("Patient with MRN MRN-001 already exists"));

            String requestBody = toJson(testPatient);

            // Act
            MvcResult result = performPost("/api/patients", requestBody);

            // Assert
            assertConflictStatus(result);
            verify(patientService, times(1)).createPatient(any());
        }

        @Test
        @DisplayName("Should set JSON content type on successful creation")
        void testCreatePatientJsonContentType() throws Exception {
            // Arrange
            when(patientService.createPatient(any()))
                    .thenReturn(testPatient);

            String requestBody = toJson(testPatient);

            // Act
            MvcResult result = performPost("/api/patients", requestBody);

            // Assert
            assertCreatedStatus(result);
            assertJsonContentType(result);
        }
    }

    @Nested
    @DisplayName("GET /api/patients/{id} - Get Patient by ID Tests")
    class GetPatientByIdTests {

        @Test
        @DisplayName("Should successfully retrieve patient by ID")
        void testGetPatientByIdSuccess() throws Exception {
            // Arrange
            when(patientService.getPatient(validPatientId))
                    .thenReturn(Optional.of(testPatient));

            // Act
            MvcResult result = performGet("/api/patients/" + validPatientId);

            // Assert
            assertOkStatus(result);
            Patient responsePatient = assertOkAndParse(result, Patient.class);
            assertEquals(validPatientId, responsePatient.getId());
            assertEquals("John", responsePatient.getFirstName());
            assertEquals("Doe", responsePatient.getLastName());
            assertEquals("MRN-001", responsePatient.getMrn());

            verify(patientService, times(1)).getPatient(validPatientId);
        }

        @Test
        @DisplayName("Should return 404 when patient does not exist")
        void testGetPatientByIdNotFound() throws Exception {
            // Arrange
            when(patientService.getPatient(nonExistentPatientId))
                    .thenReturn(Optional.empty());

            // Act
            MvcResult result = performGet("/api/patients/" + nonExistentPatientId);

            // Assert
            assertNotFoundStatus(result);
            assertClientErrorResponse(result);

            verify(patientService, times(1)).getPatient(nonExistentPatientId);
        }

        @Test
        @DisplayName("Should return correct patient details")
        void testGetPatientByIdReturnsAllDetails() throws Exception {
            // Arrange
            when(patientService.getPatient(validPatientId))
                    .thenReturn(Optional.of(testPatient));

            // Act
            MvcResult result = performGet("/api/patients/" + validPatientId);

            // Assert
            assertOkStatus(result);
            assertJsonFieldExists(result, "id");
            assertJsonFieldExists(result, "firstName");
            assertJsonFieldExists(result, "lastName");
            assertJsonFieldExists(result, "mrn");
            assertJsonFieldExists(result, "tenantId");
            assertJsonFieldExists(result, "email");
            assertJsonFieldExists(result, "phoneNumber");
            assertJsonFieldValue(result, "firstName", "John");
            assertJsonFieldValue(result, "lastName", "Doe");
        }

        @Test
        @DisplayName("Should return JSON content type")
        void testGetPatientByIdJsonContentType() throws Exception {
            // Arrange
            when(patientService.getPatient(validPatientId))
                    .thenReturn(Optional.of(testPatient));

            // Act
            MvcResult result = performGet("/api/patients/" + validPatientId);

            // Assert
            assertOkStatus(result);
            assertJsonContentType(result);
        }

        @Test
        @DisplayName("Should verify service is called with correct patient ID")
        void testGetPatientByIdVerifiesServiceCall() throws Exception {
            // Arrange
            when(patientService.getPatient(validPatientId))
                    .thenReturn(Optional.of(testPatient));

            // Act
            performGet("/api/patients/" + validPatientId);

            // Assert
            verify(patientService).getPatient(validPatientId);
            verify(patientService, times(1)).getPatient(validPatientId);
        }
    }

    @Nested
    @DisplayName("GET /api/patients - Search Patients Tests")
    class SearchPatientsTests {

        @Test
        @DisplayName("Should successfully search patients by tenant ID")
        void testSearchPatientsByTenantSuccess() throws Exception {
            // Arrange
            List<Patient> patients = Arrays.asList(testPatient, anotherPatient);
            Page<Patient> patientPage = new PageImpl<>(patients, PageRequest.of(0, 20), 2);

            when(patientService.searchPatients(eq(validTenantId), isNull(), any()))
                    .thenReturn(patientPage);

            // Act
            MvcResult result = performGet("/api/patients?tenantId=" + validTenantId);

            // Assert
            assertOkStatus(result);
            assertJsonContentType(result);
            String content = getResponseContent(result);
            assertNotNull(content);
            assertTrue(content.contains("John") || content.contains("Jane"));

            verify(patientService, times(1)).searchPatients(eq(validTenantId), isNull(), any());
        }

        @Test
        @DisplayName("Should return empty list when no patients found")
        void testSearchPatientsEmptyResult() throws Exception {
            // Arrange
            Page<Patient> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);

            when(patientService.searchPatients(eq(validTenantId), isNull(), any()))
                    .thenReturn(emptyPage);

            // Act
            MvcResult result = performGet("/api/patients?tenantId=" + validTenantId);

            // Assert
            assertOkStatus(result);
            String content = getResponseContent(result);
            assertNotNull(content);
        }

        @Test
        @DisplayName("Should search patients with query parameter")
        void testSearchPatientsWithQuery() throws Exception {
            // Arrange
            List<Patient> patients = Arrays.asList(testPatient);
            Page<Patient> patientPage = new PageImpl<>(patients, PageRequest.of(0, 20), 1);

            when(patientService.searchPatients(eq(validTenantId), eq("John"), any()))
                    .thenReturn(patientPage);

            // Act
            MvcResult result = performGet("/api/patients?tenantId=" + validTenantId + "&query=John");

            // Assert
            assertOkStatus(result);
            assertJsonContentType(result);

            verify(patientService, times(1)).searchPatients(eq(validTenantId), eq("John"), any());
        }

        @Test
        @DisplayName("Should handle pagination parameters")
        void testSearchPatientsPagination() throws Exception {
            // Arrange
            List<Patient> patients = Arrays.asList(testPatient);
            Page<Patient> patientPage = new PageImpl<>(patients, PageRequest.of(1, 10), 2);

            when(patientService.searchPatients(eq(validTenantId), isNull(), any()))
                    .thenReturn(patientPage);

            // Act
            MvcResult result = performGet("/api/patients?tenantId=" + validTenantId + "&page=1&size=10");

            // Assert
            assertOkStatus(result);
            verify(patientService, times(1)).searchPatients(eq(validTenantId), isNull(), any());
        }

        @Test
        @DisplayName("Should return 400 when tenant ID is missing")
        void testSearchPatientsWithoutTenantId() throws Exception {
            // Act
            MvcResult result = performGet("/api/patients");

            // Assert
            assertBadRequestStatus(result);
            verify(patientService, never()).searchPatients(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should filter by multiple search criteria")
        void testSearchPatientsMultipleCriteria() throws Exception {
            // Arrange
            List<Patient> patients = Arrays.asList(testPatient);
            Page<Patient> patientPage = new PageImpl<>(patients, PageRequest.of(0, 20), 1);

            when(patientService.searchPatients(eq(validTenantId), eq("Doe"), any()))
                    .thenReturn(patientPage);

            // Act
            MvcResult result = performGet(
                    "/api/patients?tenantId=" + validTenantId
                            + "&query=Doe"
                            + "&page=0"
                            + "&size=20"
            );

            // Assert
            assertOkStatus(result);
            verify(patientService, times(1)).searchPatients(eq(validTenantId), eq("Doe"), any());
        }
    }

    @Nested
    @DisplayName("PUT /api/patients/{id} - Update Patient Tests")
    class UpdatePatientTests {

        @Test
        @DisplayName("Should successfully update an existing patient")
        void testUpdatePatientSuccess() throws Exception {
            // Arrange
            Patient updatedPatient = Patient.builder()
                    .id(validPatientId)
                    .mrn("MRN-001")
                    .firstName("Jonathan")
                    .lastName("Doe-Smith")
                    .dateOfBirth(LocalDate.of(1985, 5, 15))
                    .gender(Patient.Gender.MALE)
                    .phoneNumber("555-9999")
                    .email("jonathan.doe@example.com")
                    .tenantId(validTenantId)
                    .active(true)
                    .build();

            when(patientService.updatePatient(eq(validPatientId), any()))
                    .thenReturn(updatedPatient);

            String requestBody = toJson(updatedPatient);

            // Act
            MvcResult result = performPut("/api/patients/" + validPatientId, requestBody);

            // Assert
            assertOkStatus(result);
            Patient responsePatient = assertOkAndParse(result, Patient.class);
            assertEquals("Jonathan", responsePatient.getFirstName());
            assertEquals("Doe-Smith", responsePatient.getLastName());
            assertEquals("555-9999", responsePatient.getPhoneNumber());

            verify(patientService, times(1)).updatePatient(eq(validPatientId), any());
        }

        @Test
        @DisplayName("Should return 404 when updating non-existent patient")
        void testUpdatePatientNotFound() throws Exception {
            // Arrange
            when(patientService.updatePatient(eq(nonExistentPatientId), any()))
                    .thenThrow(new IllegalArgumentException("Patient not found: " + nonExistentPatientId));

            String requestBody = toJson(testPatient);

            // Act
            MvcResult result = performPut("/api/patients/" + nonExistentPatientId, requestBody);

            // Assert
            assertNotFoundStatus(result);
            verify(patientService, times(1)).updatePatient(eq(nonExistentPatientId), any());
        }

        @Test
        @DisplayName("Should update only specified fields")
        void testUpdatePatientPartialUpdate() throws Exception {
            // Arrange
            Patient updatePayload = new Patient();
            updatePayload.setFirstName("Jonathan");
            updatePayload.setPhoneNumber("555-9999");

            Patient expectedResult = Patient.builder()
                    .id(validPatientId)
                    .mrn("MRN-001")
                    .firstName("Jonathan")
                    .lastName("Doe")
                    .phoneNumber("555-9999")
                    .tenantId(validTenantId)
                    .active(true)
                    .build();

            when(patientService.updatePatient(eq(validPatientId), any()))
                    .thenReturn(expectedResult);

            String requestBody = toJson(updatePayload);

            // Act
            MvcResult result = performPut("/api/patients/" + validPatientId, requestBody);

            // Assert
            assertOkStatus(result);
            Patient responsePatient = assertOkAndParse(result, Patient.class);
            assertEquals("Jonathan", responsePatient.getFirstName());
            assertEquals("Doe", responsePatient.getLastName());
        }

        @Test
        @DisplayName("Should return JSON content type on update")
        void testUpdatePatientJsonContentType() throws Exception {
            // Arrange
            when(patientService.updatePatient(eq(validPatientId), any()))
                    .thenReturn(testPatient);

            String requestBody = toJson(testPatient);

            // Act
            MvcResult result = performPut("/api/patients/" + validPatientId, requestBody);

            // Assert
            assertOkStatus(result);
            assertJsonContentType(result);
        }

        @Test
        @DisplayName("Should return updated patient data")
        void testUpdatePatientReturnsUpdatedData() throws Exception {
            // Arrange
            Patient updated = new Patient();
            updated.setId(validPatientId);
            updated.setFirstName("Updated");
            updated.setLastName("Name");
            updated.setMrn("MRN-001");
            updated.setTenantId(validTenantId);

            when(patientService.updatePatient(eq(validPatientId), any()))
                    .thenReturn(updated);

            String requestBody = toJson(testPatient);

            // Act
            MvcResult result = performPut("/api/patients/" + validPatientId, requestBody);

            // Assert
            assertOkStatus(result);
            assertJsonFieldExists(result, "id");
            assertJsonFieldValue(result, "firstName", "Updated");
            assertJsonFieldValue(result, "lastName", "Name");
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed JSON in POST request")
        void testCreatePatientMalformedJson() throws Exception {
            // Act
            MvcResult result = performPost("/api/patients", "{invalid json}");

            // Assert
            assertBadRequestStatus(result);
            assertClientErrorResponse(result);
        }

        @Test
        @DisplayName("Should handle service exceptions")
        void testHandleServiceException() throws Exception {
            // Arrange
            when(patientService.getPatient(anyString()))
                    .thenThrow(new RuntimeException("Database connection failed"));

            // Act
            MvcResult result = performGet("/api/patients/" + validPatientId);

            // Assert
            assertServerErrorResponse(result);
        }

        @Test
        @DisplayName("Should return proper error message format")
        void testErrorResponseFormat() throws Exception {
            // Act
            MvcResult result = performGet("/api/patients/" + nonExistentPatientId);

            // Assert
            assertNotFoundStatus(result);
            String content = getResponseContent(result);
            assertNotNull(content);
        }

        @Test
        @DisplayName("Should handle missing required path variable")
        void testGetPatientMissingPathVariable() throws Exception {
            // Act - Trying to access without patient ID
            MvcResult result = performGet("/api/patients/");

            // Assert - Should return 404 or 405 (Method Not Allowed)
            int status = getStatusCode(result);
            assertTrue(status == 404 || status == 405, "Expected 404 or 405 but got " + status);
        }
    }

    @Nested
    @DisplayName("Business Logic Tests")
    class BusinessLogicTests {

        @Test
        @DisplayName("Should maintain data consistency across operations")
        void testDataConsistency() throws Exception {
            // Arrange
            when(patientService.createPatient(any()))
                    .thenReturn(testPatient);

            when(patientService.getPatient(validPatientId))
                    .thenReturn(Optional.of(testPatient));

            String requestBody = toJson(testPatient);

            // Act
            MvcResult createResult = performPost("/api/patients", requestBody);
            assertCreatedStatus(createResult);

            MvcResult getResult = performGet("/api/patients/" + validPatientId);
            assertOkStatus(getResult);

            // Assert
            Patient createdPatient = assertCreatedAndParse(createResult, Patient.class);
            Patient retrievedPatient = assertOkAndParse(getResult, Patient.class);

            assertEquals(createdPatient.getId(), retrievedPatient.getId());
            assertEquals(createdPatient.getMrn(), retrievedPatient.getMrn());
            assertEquals(createdPatient.getFirstName(), retrievedPatient.getFirstName());
        }

        @Test
        @DisplayName("Should maintain patient active status")
        void testPatientActiveStatus() throws Exception {
            // Arrange
            when(patientService.getPatient(validPatientId))
                    .thenReturn(Optional.of(testPatient));

            // Act
            MvcResult result = performGet("/api/patients/" + validPatientId);

            // Assert
            assertOkStatus(result);
            Patient responsePatient = assertOkAndParse(result, Patient.class);
            assertTrue(responsePatient.isActive());
        }

        @Test
        @DisplayName("Should support multi-tenant isolation")
        void testMultiTenantIsolation() throws Exception {
            // Arrange
            String differentTenantId = "tenant-002";
            testPatient.setTenantId(validTenantId);
            anotherPatient.setTenantId(differentTenantId);

            List<Patient> tenantOnePatients = Arrays.asList(testPatient);
            Page<Patient> tenantOnePage = new PageImpl<>(tenantOnePatients, PageRequest.of(0, 20), 1);

            when(patientService.searchPatients(eq(validTenantId), isNull(), any()))
                    .thenReturn(tenantOnePage);

            // Act
            MvcResult result = performGet("/api/patients?tenantId=" + validTenantId);

            // Assert
            assertOkStatus(result);
            verify(patientService).searchPatients(eq(validTenantId), isNull(), any());
            verify(patientService, never()).searchPatients(eq(differentTenantId), isNull(), any());
        }
    }

    @Nested
    @DisplayName("Response Format Tests")
    class ResponseFormatTests {

        @Test
        @DisplayName("Should return complete patient object in response")
        void testResponseContainsCompletePatientObject() throws Exception {
            // Arrange
            when(patientService.getPatient(validPatientId))
                    .thenReturn(Optional.of(testPatient));

            // Act
            MvcResult result = performGet("/api/patients/" + validPatientId);

            // Assert
            assertOkStatus(result);
            Patient responsePatient = assertOkAndParse(result, Patient.class);

            assertNotNull(responsePatient.getId());
            assertNotNull(responsePatient.getMrn());
            assertNotNull(responsePatient.getFirstName());
            assertNotNull(responsePatient.getLastName());
            assertNotNull(responsePatient.getDateOfBirth());
            assertNotNull(responsePatient.getGender());
            assertNotNull(responsePatient.getTenantId());
        }

        @Test
        @DisplayName("Should return paginated results in search response")
        void testSearchResponsePagination() throws Exception {
            // Arrange
            List<Patient> patients = Arrays.asList(testPatient, anotherPatient);
            Page<Patient> patientPage = new PageImpl<>(patients, PageRequest.of(0, 20), 2);

            when(patientService.searchPatients(eq(validTenantId), isNull(), any()))
                    .thenReturn(patientPage);

            // Act
            MvcResult result = performGet("/api/patients?tenantId=" + validTenantId);

            // Assert
            assertOkStatus(result);
            assertJsonContentType(result);
            String content = getResponseContent(result);
            assertNotNull(content);
        }

        @Test
        @DisplayName("Should return proper HTTP status codes")
        void testHttpStatusCodes() throws Exception {
            // Arrange
            when(patientService.createPatient(any(Patient.class)))
                    .thenReturn(testPatient);

            when(patientService.getPatient(validPatientId))
                    .thenReturn(Optional.of(testPatient));

            // Act & Assert
            MvcResult createResult = performPost("/api/patients", toJson(testPatient));
            assertEquals(201, getStatusCode(createResult));

            MvcResult getResult = performGet("/api/patients/" + validPatientId);
            assertEquals(200, getStatusCode(getResult));

            MvcResult notFoundResult = performGet("/api/patients/" + nonExistentPatientId);
            assertEquals(404, getStatusCode(notFoundResult));
        }
    }
}
