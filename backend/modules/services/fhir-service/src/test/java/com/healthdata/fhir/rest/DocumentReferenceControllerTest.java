package com.healthdata.fhir.rest;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.hl7.fhir.r4.model.DocumentReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.healthdata.fhir.service.DocumentReferenceService;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;

@ExtendWith(MockitoExtension.class)
@DisplayName("DocumentReference Controller Tests")
class DocumentReferenceControllerTest {

    private static final String TENANT_ID = "tenant-1";
    private static final IParser JSON_PARSER = FhirContext.forR4().newJsonParser().setPrettyPrint(false);

    @Mock
    private DocumentReferenceService documentReferenceService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        DocumentReferenceController controller = new DocumentReferenceController(documentReferenceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should create document reference")
    void shouldCreateDocumentReference() throws Exception {
        DocumentReference docRef = new DocumentReference();
        docRef.setId(UUID.randomUUID().toString());
        when(documentReferenceService.createDocumentReference(eq(TENANT_ID), any(DocumentReference.class), eq("user")))
                .thenReturn(docRef);

        mockMvc.perform(post("/fhir/DocumentReference")
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(docRef)))
                .andExpect(status().isCreated())
                .andExpect(content().string(containsString(docRef.getId())));
    }

    @Test
    @DisplayName("Should get document reference")
    void shouldGetDocumentReference() throws Exception {
        UUID docId = UUID.randomUUID();
        DocumentReference docRef = new DocumentReference();
        docRef.setId(docId.toString());
        when(documentReferenceService.getDocumentReference(TENANT_ID, docId))
                .thenReturn(Optional.of(docRef));

        mockMvc.perform(get("/fhir/DocumentReference/{id}", docId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(docRef.getId())));
    }

    @Test
    @DisplayName("Should return not found when document missing")
    void shouldReturnNotFoundWhenDocumentMissing() throws Exception {
        UUID docId = UUID.randomUUID();
        when(documentReferenceService.getDocumentReference(TENANT_ID, docId))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/fhir/DocumentReference/{id}", docId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should update document reference")
    void shouldUpdateDocumentReference() throws Exception {
        UUID docId = UUID.randomUUID();
        DocumentReference docRef = new DocumentReference();
        docRef.setId(docId.toString());
        when(documentReferenceService.updateDocumentReference(eq(TENANT_ID), eq(docId), any(DocumentReference.class), eq("user")))
                .thenReturn(docRef);

        mockMvc.perform(put("/fhir/DocumentReference/{id}", docId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user")
                        .contentType("application/fhir+json")
                        .content(JSON_PARSER.encodeResourceToString(docRef)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(docRef.getId())));
    }

    @Test
    @DisplayName("Should delete document reference")
    void shouldDeleteDocumentReference() throws Exception {
        UUID docId = UUID.randomUUID();

        mockMvc.perform(delete("/fhir/DocumentReference/{id}", docId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .header("X-User-ID", "user"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should search documents with invalid references")
    void shouldSearchDocumentsWithInvalidReferences() throws Exception {
        DocumentReference docRef = new DocumentReference();
        docRef.setId(UUID.randomUUID().toString());
        when(documentReferenceService.searchDocuments(
                eq(TENANT_ID),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(docRef)));

        mockMvc.perform(get("/fhir/DocumentReference")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/not-a-uuid")
                        .param("encounter", "Encounter/not-a-uuid"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Bundle")))
                .andExpect(content().string(containsString(docRef.getId())));

        verify(documentReferenceService).searchDocuments(
                eq(TENANT_ID),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(PageRequest.class));
    }

    @Test
    @DisplayName("Should search documents with references")
    void shouldSearchDocumentsWithReferences() throws Exception {
        UUID patientId = UUID.randomUUID();
        UUID encounterId = UUID.randomUUID();
        DocumentReference docRef = new DocumentReference();
        docRef.setId(UUID.randomUUID().toString());
        when(documentReferenceService.searchDocuments(
                eq(TENANT_ID),
                eq(patientId),
                eq(encounterId),
                eq("current"),
                eq("34133-9"),
                eq("clinical-note"),
                eq("application/pdf"),
                any(PageRequest.class)))
            .thenReturn(new PageImpl<>(List.of(docRef)));

        mockMvc.perform(get("/fhir/DocumentReference")
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("patient", "Patient/" + patientId)
                        .param("encounter", "Encounter/" + encounterId)
                        .param("status", "current")
                        .param("type", "34133-9")
                        .param("category", "clinical-note")
                        .param("contenttype", "application/pdf")
                        .param("_page", "0")
                        .param("_count", "20"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(docRef.getId())));
    }

    @Test
    @DisplayName("Should get documents by patient")
    void shouldGetDocumentsByPatient() throws Exception {
        UUID patientId = UUID.randomUUID();
        DocumentReference docRef = new DocumentReference();
        docRef.setId(UUID.randomUUID().toString());
        when(documentReferenceService.getDocumentsByPatient(TENANT_ID, patientId))
                .thenReturn(List.of(docRef));

        mockMvc.perform(get("/fhir/DocumentReference/patient/{patientId}", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(docRef.getId())));
    }

    @Test
    @DisplayName("Should get current documents by patient")
    void shouldGetCurrentDocumentsByPatient() throws Exception {
        UUID patientId = UUID.randomUUID();
        DocumentReference docRef = new DocumentReference();
        docRef.setId(UUID.randomUUID().toString());
        when(documentReferenceService.getCurrentDocuments(TENANT_ID, patientId))
                .thenReturn(List.of(docRef));

        mockMvc.perform(get("/fhir/DocumentReference/patient/{patientId}/current", patientId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(docRef.getId())));
    }

    @Test
    @DisplayName("Should get documents by encounter")
    void shouldGetDocumentsByEncounter() throws Exception {
        UUID encounterId = UUID.randomUUID();
        DocumentReference docRef = new DocumentReference();
        docRef.setId(UUID.randomUUID().toString());
        when(documentReferenceService.getDocumentsByEncounter(TENANT_ID, encounterId))
                .thenReturn(List.of(docRef));

        mockMvc.perform(get("/fhir/DocumentReference/encounter/{encounterId}", encounterId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(docRef.getId())));
    }

    @Test
    @DisplayName("Should get documents by type")
    void shouldGetDocumentsByType() throws Exception {
        UUID patientId = UUID.randomUUID();
        DocumentReference docRef = new DocumentReference();
        docRef.setId(UUID.randomUUID().toString());
        when(documentReferenceService.getDocumentsByType(TENANT_ID, patientId, "34133-9"))
                .thenReturn(List.of(docRef));

        mockMvc.perform(get("/fhir/DocumentReference/patient/{patientId}/type/{typeCode}", patientId, "34133-9")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(docRef.getId())));
    }

    @Test
    @DisplayName("Should return not found when latest document missing")
    void shouldReturnNotFoundWhenLatestMissing() throws Exception {
        UUID patientId = UUID.randomUUID();
        when(documentReferenceService.getLatestDocumentByType(TENANT_ID, patientId, "34133-9"))
                .thenReturn(Optional.empty());

        mockMvc.perform(get("/fhir/DocumentReference/patient/{patientId}/type/{type}/latest", patientId, "34133-9")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return latest document when present")
    void shouldReturnLatestDocumentWhenPresent() throws Exception {
        UUID patientId = UUID.randomUUID();
        DocumentReference docRef = new DocumentReference();
        docRef.setId(UUID.randomUUID().toString());
        when(documentReferenceService.getLatestDocumentByType(TENANT_ID, patientId, "34133-9"))
                .thenReturn(Optional.of(docRef));

        mockMvc.perform(get("/fhir/DocumentReference/patient/{patientId}/type/{type}/latest", patientId, "34133-9")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(docRef.getId())));
    }

    @Test
    @DisplayName("Should search documents by description")
    void shouldSearchDocumentsByDescription() throws Exception {
        UUID patientId = UUID.randomUUID();
        DocumentReference docRef = new DocumentReference();
        docRef.setId(UUID.randomUUID().toString());
        when(documentReferenceService.searchByDescription(TENANT_ID, patientId, "summary"))
                .thenReturn(List.of(docRef));

        mockMvc.perform(get("/fhir/DocumentReference/patient/{patientId}/search", patientId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("q", "summary"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(docRef.getId())));
    }

    @Test
    @DisplayName("Should get documents by date range")
    void shouldGetDocumentsByDateRange() throws Exception {
        UUID patientId = UUID.randomUUID();
        DocumentReference docRef = new DocumentReference();
        docRef.setId(UUID.randomUUID().toString());
        when(documentReferenceService.getDocumentsByDateRange(eq(TENANT_ID), eq(patientId), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(docRef));

        mockMvc.perform(get("/fhir/DocumentReference/patient/{patientId}/date-range", patientId)
                        .header("X-Tenant-ID", TENANT_ID)
                        .param("start", "2025-01-01T00:00:00Z")
                        .param("end", "2025-01-02T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(docRef.getId())));
    }
}
