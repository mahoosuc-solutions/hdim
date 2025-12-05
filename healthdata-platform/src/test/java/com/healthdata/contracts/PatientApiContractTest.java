package com.healthdata.contracts;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.BaseIntegrationTest;
import com.healthdata.patient.dto.CreatePatientRequest;
import com.healthdata.patient.dto.UpdatePatientRequest;
import com.healthdata.patient.entity.Patient;
import com.healthdata.patient.repository.PatientRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive API Contract Tests for Patient API
 *
 * Tests validate:
 * - HTTP status codes
 * - Response structure
 * - Request validation
 * - Business rule enforcement
 * - Database persistence
 * - Multi-tenant isolation
 * - Error handling
 *
 * @author TDD Swarm Agent 5B
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PatientApiContractTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_PATH = "/api/patients";
    private static final String TENANT_ID = "test-tenant-1";

    @BeforeEach
    public void setUp() {
        patientRepository.deleteAll();
    }

    // ==================== CREATE PATIENT CONTRACTS ====================

    @Nested
    @DisplayName("POST /api/patients - Create Patient")
    class CreatePatientContract {

        @Test
        @DisplayName("Valid request returns 201 Created with patient object")
        void createPatient_ValidRequest_Returns201() throws Exception {
            CreatePatientRequest request = CreatePatientRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .dateOfBirth(LocalDate.of(1980, 1, 15))
                    .gender("male")
                    .mrn("MRN001")
                    .email("john.doe@example.com")
                    .phone("555-0100")
                    .tenantId(TENANT_ID)
                    .build();

            MvcResult result = mockMvc.perform(post(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.mrn").value("MRN001"))
                    .andExpect(jsonPath("$.email").value("john.doe@example.com"))
                    .andExpect(jsonPath("$.gender").value("male"))
                    .andReturn();

            // Verify database persistence
            String responseBody = result.getResponse().getContentAsString();
            String patientId = JsonPath.read(responseBody, "$.id");

            Patient persistedPatient = patientRepository.findById(patientId).orElseThrow();
            assertThat(persistedPatient.getFirstName()).isEqualTo("John");
            assertThat(persistedPatient.getLastName()).isEqualTo("Doe");
            assertThat(persistedPatient.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(persistedPatient.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Missing required firstName returns 400 Bad Request")
        void createPatient_MissingFirstName_Returns400() throws Exception {
            CreatePatientRequest request = CreatePatientRequest.builder()
                    .lastName("Doe")
                    .dateOfBirth(LocalDate.of(1980, 1, 15))
                    .mrn("MRN002")
                    .tenantId(TENANT_ID)
                    .build();

            mockMvc.perform(post(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").value(containsString("firstName")));
        }

        @Test
        @DisplayName("Missing required lastName returns 400 Bad Request")
        void createPatient_MissingLastName_Returns400() throws Exception {
            CreatePatientRequest request = CreatePatientRequest.builder()
                    .firstName("John")
                    .dateOfBirth(LocalDate.of(1980, 1, 15))
                    .mrn("MRN003")
                    .tenantId(TENANT_ID)
                    .build();

            mockMvc.perform(post(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Duplicate MRN within tenant returns 409 Conflict")
        void createPatient_DuplicateMrn_Returns409() throws Exception {
            // Create first patient
            Patient existing = Patient.builder()
                    .id(UUID.randomUUID().toString())
                    .firstName("Existing")
                    .lastName("Patient")
                    .mrn("MRN001")
                    .tenantId(TENANT_ID)
                    .build();
            patientRepository.save(existing);

            // Try to create patient with same MRN
            CreatePatientRequest request = CreatePatientRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .mrn("MRN001")
                    .dateOfBirth(LocalDate.of(1980, 1, 15))
                    .tenantId(TENANT_ID)
                    .build();

            mockMvc.perform(post(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.error").value(containsString("MRN")));
        }

        @Test
        @DisplayName("Same MRN in different tenant succeeds (multi-tenant isolation)")
        void createPatient_SameMrnDifferentTenant_Returns201() throws Exception {
            // Create patient in tenant 1
            Patient tenant1Patient = Patient.builder()
                    .id(UUID.randomUUID().toString())
                    .firstName("Tenant1")
                    .lastName("Patient")
                    .mrn("MRN001")
                    .tenantId("tenant-1")
                    .build();
            patientRepository.save(tenant1Patient);

            // Create patient with same MRN in tenant 2
            CreatePatientRequest request = CreatePatientRequest.builder()
                    .firstName("Tenant2")
                    .lastName("Patient")
                    .mrn("MRN001")
                    .dateOfBirth(LocalDate.of(1980, 1, 15))
                    .tenantId("tenant-2")
                    .build();

            mockMvc.perform(post(BASE_PATH)
                            .header("X-Tenant-ID", "tenant-2")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mrn").value("MRN001"))
                    .andExpect(jsonPath("$.tenantId").value("tenant-2"));
        }

        @Test
        @DisplayName("Response includes proper HTTP headers")
        void createPatient_ResponseHeaders_Valid() throws Exception {
            CreatePatientRequest request = CreatePatientRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .mrn("MRN004")
                    .dateOfBirth(LocalDate.of(1980, 1, 15))
                    .tenantId(TENANT_ID)
                    .build();

            MvcResult result = mockMvc.perform(post(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(header().string("Content-Type", containsString("application/json")))
                    .andReturn();

            String location = result.getResponse().getHeader("Location");
            assertThat(location).contains(BASE_PATH);
        }

        @Test
        @DisplayName("Invalid email format returns 400")
        void createPatient_InvalidEmail_Returns400() throws Exception {
            CreatePatientRequest request = CreatePatientRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .mrn("MRN005")
                    .email("invalid-email")
                    .dateOfBirth(LocalDate.of(1980, 1, 15))
                    .tenantId(TENANT_ID)
                    .build();

            mockMvc.perform(post(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("email")));
        }

        @Test
        @DisplayName("Future date of birth returns 400")
        void createPatient_FutureDateOfBirth_Returns400() throws Exception {
            CreatePatientRequest request = CreatePatientRequest.builder()
                    .firstName("John")
                    .lastName("Doe")
                    .mrn("MRN006")
                    .dateOfBirth(LocalDate.now().plusDays(1))
                    .tenantId(TENANT_ID)
                    .build();

            mockMvc.perform(post(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("date")));
        }
    }

    // ==================== GET PATIENT CONTRACTS ====================

    @Nested
    @DisplayName("GET /api/patients/{id} - Get Patient by ID")
    class GetPatientContract {

        @Test
        @DisplayName("Valid ID returns 200 OK with patient")
        void getPatient_ValidId_Returns200() throws Exception {
            Patient patient = createTestPatient("John", "Doe", "MRN001", TENANT_ID);

            mockMvc.perform(get(BASE_PATH + "/" + patient.getId())
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(patient.getId()))
                    .andExpect(jsonPath("$.firstName").value("John"))
                    .andExpect(jsonPath("$.lastName").value("Doe"))
                    .andExpect(jsonPath("$.mrn").value("MRN001"));
        }

        @Test
        @DisplayName("Non-existent ID returns 404 Not Found")
        void getPatient_NonExistentId_Returns404() throws Exception {
            String nonExistentId = UUID.randomUUID().toString();

            mockMvc.perform(get(BASE_PATH + "/" + nonExistentId)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").exists())
                    .andExpect(jsonPath("$.message").value(containsString("not found")));
        }

        @Test
        @DisplayName("Tenant isolation enforced - different tenant returns 403")
        void getPatient_DifferentTenant_Returns403() throws Exception {
            Patient patient = createTestPatient("John", "Doe", "MRN002", "tenant-1");

            mockMvc.perform(get(BASE_PATH + "/" + patient.getId())
                            .header("X-Tenant-ID", "tenant-2")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value(containsString("access denied")));
        }

        @Test
        @DisplayName("Missing tenant header returns 400")
        void getPatient_MissingTenantHeader_Returns400() throws Exception {
            Patient patient = createTestPatient("John", "Doe", "MRN003", TENANT_ID);

            mockMvc.perform(get(BASE_PATH + "/" + patient.getId())
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("tenant")));
        }

        @Test
        @DisplayName("Response contains all expected fields")
        void getPatient_ResponseStructure_Valid() throws Exception {
            Patient patient = createTestPatient("John", "Doe", "MRN004", TENANT_ID);
            patient.setEmail("john@example.com");
            patient.setPhone("555-0100");
            patientRepository.save(patient);

            mockMvc.perform(get(BASE_PATH + "/" + patient.getId())
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.firstName").exists())
                    .andExpect(jsonPath("$.lastName").exists())
                    .andExpect(jsonPath("$.mrn").exists())
                    .andExpect(jsonPath("$.email").exists())
                    .andExpect(jsonPath("$.phone").exists())
                    .andExpect(jsonPath("$.tenantId").exists())
                    .andExpect(jsonPath("$.createdAt").exists())
                    .andExpect(jsonPath("$.updatedAt").exists());
        }
    }

    // ==================== UPDATE PATIENT CONTRACTS ====================

    @Nested
    @DisplayName("PUT /api/patients/{id} - Update Patient")
    class UpdatePatientContract {

        @Test
        @DisplayName("Valid update returns 200 OK with updated patient")
        void updatePatient_ValidRequest_Returns200() throws Exception {
            Patient patient = createTestPatient("John", "Doe", "MRN001", TENANT_ID);

            UpdatePatientRequest request = UpdatePatientRequest.builder()
                    .firstName("Jane")
                    .lastName("Smith")
                    .email("jane.smith@example.com")
                    .phone("555-0200")
                    .build();

            mockMvc.perform(put(BASE_PATH + "/" + patient.getId())
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("Jane"))
                    .andExpect(jsonPath("$.lastName").value("Smith"))
                    .andExpect(jsonPath("$.email").value("jane.smith@example.com"))
                    .andExpect(jsonPath("$.phone").value("555-0200"));

            // Verify database update
            Patient updated = patientRepository.findById(patient.getId()).orElseThrow();
            assertThat(updated.getFirstName()).isEqualTo("Jane");
            assertThat(updated.getLastName()).isEqualTo("Smith");
            assertThat(updated.getEmail()).isEqualTo("jane.smith@example.com");
            assertThat(updated.getUpdatedAt()).isAfter(updated.getCreatedAt());
        }

        @Test
        @DisplayName("Cannot update immutable MRN field")
        void updatePatient_UpdateMrn_Returns400() throws Exception {
            Patient patient = createTestPatient("John", "Doe", "MRN001", TENANT_ID);

            UpdatePatientRequest request = UpdatePatientRequest.builder()
                    .mrn("MRN002")
                    .build();

            mockMvc.perform(put(BASE_PATH + "/" + patient.getId())
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("Cannot update MRN")));
        }

        @Test
        @DisplayName("Cannot update tenant ID")
        void updatePatient_UpdateTenantId_Returns400() throws Exception {
            Patient patient = createTestPatient("John", "Doe", "MRN002", TENANT_ID);

            UpdatePatientRequest request = UpdatePatientRequest.builder()
                    .firstName("Jane")
                    .tenantId("different-tenant")
                    .build();

            mockMvc.perform(put(BASE_PATH + "/" + patient.getId())
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value(containsString("Cannot update tenant")));
        }

        @Test
        @DisplayName("Partial update succeeds")
        void updatePatient_PartialUpdate_Returns200() throws Exception {
            Patient patient = createTestPatient("John", "Doe", "MRN003", TENANT_ID);
            patient.setEmail("old@example.com");
            patientRepository.save(patient);

            UpdatePatientRequest request = UpdatePatientRequest.builder()
                    .email("new@example.com")
                    .build();

            mockMvc.perform(put(BASE_PATH + "/" + patient.getId())
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.firstName").value("John")) // Unchanged
                    .andExpect(jsonPath("$.email").value("new@example.com")); // Changed
        }

        @Test
        @DisplayName("Update non-existent patient returns 404")
        void updatePatient_NonExistent_Returns404() throws Exception {
            String nonExistentId = UUID.randomUUID().toString();

            UpdatePatientRequest request = UpdatePatientRequest.builder()
                    .firstName("Jane")
                    .build();

            mockMvc.perform(put(BASE_PATH + "/" + nonExistentId)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    // ==================== LIST PATIENTS CONTRACTS ====================

    @Nested
    @DisplayName("GET /api/patients - List Patients")
    class ListPatientsContract {

        @Test
        @DisplayName("Returns paginated list of patients")
        void listPatients_ValidQuery_ReturnsPaginated() throws Exception {
            // Create 15 test patients
            for (int i = 0; i < 15; i++) {
                createTestPatient("Patient" + i, "Test", "MRN" + String.format("%03d", i), TENANT_ID);
            }

            mockMvc.perform(get(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("page", "0")
                            .param("size", "10")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(10))
                    .andExpect(jsonPath("$.totalElements").value(15))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.number").value(0))
                    .andExpect(jsonPath("$.size").value(10));
        }

        @Test
        @DisplayName("Second page returns remaining elements")
        void listPatients_SecondPage_ReturnsRemaining() throws Exception {
            for (int i = 0; i < 15; i++) {
                createTestPatient("Patient" + i, "Test", "MRN" + i, TENANT_ID);
            }

            mockMvc.perform(get(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("page", "1")
                            .param("size", "10")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(5))
                    .andExpect(jsonPath("$.number").value(1))
                    .andExpect(jsonPath("$.last").value(true));
        }

        @Test
        @DisplayName("Supports filtering by firstName")
        void listPatients_FilterByFirstName_ReturnsFiltered() throws Exception {
            createTestPatient("John", "Smith", "MRN001", TENANT_ID);
            createTestPatient("Jane", "Doe", "MRN002", TENANT_ID);
            createTestPatient("John", "Johnson", "MRN003", TENANT_ID);
            createTestPatient("Bob", "Brown", "MRN004", TENANT_ID);

            mockMvc.perform(get(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("firstName", "John")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[*].firstName", everyItem(is("John"))));
        }

        @Test
        @DisplayName("Supports filtering by lastName")
        void listPatients_FilterByLastName_ReturnsFiltered() throws Exception {
            createTestPatient("John", "Smith", "MRN005", TENANT_ID);
            createTestPatient("Jane", "Smith", "MRN006", TENANT_ID);
            createTestPatient("Bob", "Johnson", "MRN007", TENANT_ID);

            mockMvc.perform(get(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("lastName", "Smith")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[*].lastName", everyItem(is("Smith"))));
        }

        @Test
        @DisplayName("Supports sorting by firstName ascending")
        void listPatients_SortByFirstNameAsc_ReturnsSorted() throws Exception {
            createTestPatient("Charlie", "Brown", "MRN008", TENANT_ID);
            createTestPatient("Alice", "Smith", "MRN009", TENANT_ID);
            createTestPatient("Bob", "Johnson", "MRN010", TENANT_ID);

            mockMvc.perform(get(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("sort", "firstName,asc")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].firstName").value("Alice"))
                    .andExpect(jsonPath("$.content[1].firstName").value("Bob"))
                    .andExpect(jsonPath("$.content[2].firstName").value("Charlie"));
        }

        @Test
        @DisplayName("Supports sorting by lastName descending")
        void listPatients_SortByLastNameDesc_ReturnsSorted() throws Exception {
            createTestPatient("John", "Adams", "MRN011", TENANT_ID);
            createTestPatient("Jane", "Brown", "MRN012", TENANT_ID);
            createTestPatient("Bob", "Carter", "MRN013", TENANT_ID);

            mockMvc.perform(get(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("sort", "lastName,desc")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].lastName").value("Carter"))
                    .andExpect(jsonPath("$.content[1].lastName").value("Brown"))
                    .andExpect(jsonPath("$.content[2].lastName").value("Adams"));
        }

        @Test
        @DisplayName("Tenant isolation - only returns patients from same tenant")
        void listPatients_TenantIsolation_OnlyReturnsTenantData() throws Exception {
            createTestPatient("Tenant1", "Patient1", "MRN014", "tenant-1");
            createTestPatient("Tenant1", "Patient2", "MRN015", "tenant-1");
            createTestPatient("Tenant2", "Patient1", "MRN016", "tenant-2");
            createTestPatient("Tenant2", "Patient2", "MRN017", "tenant-2");

            mockMvc.perform(get(BASE_PATH)
                            .header("X-Tenant-ID", "tenant-1")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[*].tenantId", everyItem(is("tenant-1"))));
        }

        @Test
        @DisplayName("Empty result set returns empty array")
        void listPatients_NoResults_ReturnsEmpty() throws Exception {
            mockMvc.perform(get(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.content.length()").value(0))
                    .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        @DisplayName("Search by MRN partial match")
        void listPatients_SearchByMrnPartial_ReturnsMatches() throws Exception {
            createTestPatient("Patient", "One", "MRN-001-A", TENANT_ID);
            createTestPatient("Patient", "Two", "MRN-001-B", TENANT_ID);
            createTestPatient("Patient", "Three", "MRN-002-A", TENANT_ID);

            mockMvc.perform(get(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .param("mrn", "MRN-001")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(2))
                    .andExpect(jsonPath("$.content[*].mrn", everyItem(containsString("MRN-001"))));
        }
    }

    // ==================== DELETE PATIENT CONTRACTS ====================

    @Nested
    @DisplayName("DELETE /api/patients/{id} - Delete Patient")
    class DeletePatientContract {

        @Test
        @DisplayName("Valid delete returns 204 No Content")
        void deletePatient_ValidId_Returns204() throws Exception {
            Patient patient = createTestPatient("John", "Doe", "MRN001", TENANT_ID);

            mockMvc.perform(delete(BASE_PATH + "/" + patient.getId())
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isNoContent());

            // Verify patient is deleted
            assertThat(patientRepository.findById(patient.getId())).isEmpty();
        }

        @Test
        @DisplayName("Delete non-existent patient returns 404")
        void deletePatient_NonExistent_Returns404() throws Exception {
            String nonExistentId = UUID.randomUUID().toString();

            mockMvc.perform(delete(BASE_PATH + "/" + nonExistentId)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Cannot delete patient from different tenant")
        void deletePatient_DifferentTenant_Returns403() throws Exception {
            Patient patient = createTestPatient("John", "Doe", "MRN002", "tenant-1");

            mockMvc.perform(delete(BASE_PATH + "/" + patient.getId())
                            .header("X-Tenant-ID", "tenant-2")
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isForbidden());

            // Verify patient still exists
            assertThat(patientRepository.findById(patient.getId())).isPresent();
        }
    }

    // ==================== ERROR HANDLING CONTRACTS ====================

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandlingContract {

        @Test
        @DisplayName("Invalid JSON returns 400 with proper error message")
        void invalidJson_Returns400() throws Exception {
            String invalidJson = "{invalid json}";

            mockMvc.perform(post(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").exists());
        }

        @Test
        @DisplayName("Unsupported media type returns 415")
        void unsupportedMediaType_Returns415() throws Exception {
            mockMvc.perform(post(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("plain text"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("Method not allowed returns 405")
        void methodNotAllowed_Returns405() throws Exception {
            mockMvc.perform(patch(BASE_PATH)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(APPLICATION_JSON))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    // ==================== HELPER METHODS ====================

    private Patient createTestPatient(String firstName, String lastName, String mrn, String tenantId) {
        Patient patient = Patient.builder()
                .id(UUID.randomUUID().toString())
                .firstName(firstName)
                .lastName(lastName)
                .mrn(mrn)
                .dateOfBirth(LocalDate.of(1980, 1, 15))
                .gender("male")
                .tenantId(tenantId)
                .build();
        return patientRepository.save(patient);
    }
}
