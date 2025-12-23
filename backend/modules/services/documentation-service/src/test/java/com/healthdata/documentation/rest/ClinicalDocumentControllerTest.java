package com.healthdata.documentation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.documentation.dto.ClinicalDocumentDto;
import com.healthdata.documentation.dto.DocumentAttachmentDto;
import com.healthdata.documentation.service.ClinicalDocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Clinical Document Controller Tests")
class ClinicalDocumentControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private ClinicalDocumentService documentService;

    @InjectMocks
    private ClinicalDocumentController controller;

    private static final String TENANT_ID = "tenant-123";
    private static final UUID DOCUMENT_ID = UUID.randomUUID();
    private static final UUID ATTACHMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Nested
    @DisplayName("GET /api/documents/clinical Tests")
    class GetDocumentsTests {

        @Test
        @DisplayName("Should return documents for tenant")
        void shouldReturnDocuments() throws Exception {
            when(documentService.getDocuments(TENANT_ID)).thenReturn(List.of(sampleDocument()));

            mockMvc.perform(get("/api/documents/clinical")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(DOCUMENT_ID.toString()));
        }

        @Test
        @DisplayName("Should return paginated documents")
        void shouldReturnPaginatedDocuments() throws Exception {
            Pageable pageable = PageRequest.of(0, 2);
            Page<ClinicalDocumentDto> page = new PageImpl<>(List.of(sampleDocument()), pageable, 1);
            when(documentService.getDocumentsPaginated(eq(TENANT_ID), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/documents/clinical/paginated")
                            .param("page", "0")
                            .param("size", "2")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(DOCUMENT_ID.toString()));
        }
    }

    @Nested
    @DisplayName("GET /api/documents/clinical/{id} Tests")
    class GetDocumentTests {

        @Test
        @DisplayName("Should return document when found")
        void shouldReturnDocumentWhenFound() throws Exception {
            when(documentService.getDocument(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(sampleDocument()));

            mockMvc.perform(get("/api/documents/clinical/{id}", DOCUMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(DOCUMENT_ID.toString()));
        }

        @Test
        @DisplayName("Should return 404 when document not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(documentService.getDocument(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/documents/clinical/{id}", DOCUMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("GET /api/documents/clinical/patient/{patientId} Tests")
    class GetPatientDocumentsTests {

        @Test
        @DisplayName("Should return patient documents")
        void shouldReturnPatientDocuments() throws Exception {
            when(documentService.getPatientDocuments(eq(TENANT_ID), eq("patient-456")))
                    .thenReturn(List.of(sampleDocument()));

            mockMvc.perform(get("/api/documents/clinical/patient/{patientId}", "patient-456")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(DOCUMENT_ID.toString()));
        }

        @Test
        @DisplayName("Should return paginated patient documents")
        void shouldReturnPaginatedPatientDocuments() throws Exception {
            Pageable pageable = PageRequest.of(1, 5);
            Page<ClinicalDocumentDto> page = new PageImpl<>(List.of(sampleDocument()), pageable, 6);
            when(documentService.getPatientDocumentsPaginated(eq(TENANT_ID), eq("patient-456"), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/documents/clinical/patient/{patientId}/paginated", "patient-456")
                            .param("page", "1")
                            .param("size", "5")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(DOCUMENT_ID.toString()));
        }
    }

    @Test
    @DisplayName("GET /api/documents/clinical/search should return results")
    void shouldSearchDocuments() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ClinicalDocumentDto> page = new PageImpl<>(List.of(sampleDocument()), pageable, 1);
        when(documentService.searchDocuments(eq(TENANT_ID), eq("query"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/documents/clinical/search")
                        .param("query", "query")
                        .param("page", "0")
                        .param("size", "10")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(DOCUMENT_ID.toString()));
    }

    @Nested
    @DisplayName("POST /api/documents/clinical Tests")
    class CreateDocumentTests {

        @Test
        @DisplayName("Should create document and return 201")
        void shouldCreateDocument() throws Exception {
            when(documentService.createDocument(any(ClinicalDocumentDto.class), eq(TENANT_ID)))
                    .thenReturn(sampleDocument());

            mockMvc.perform(post("/api/documents/clinical")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleDocument())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(DOCUMENT_ID.toString()));
        }
    }

    @Nested
    @DisplayName("PUT /api/documents/clinical/{id} Tests")
    class UpdateDocumentTests {

        @Test
        @DisplayName("Should update document")
        void shouldUpdateDocument() throws Exception {
            when(documentService.updateDocument(eq(DOCUMENT_ID), any(ClinicalDocumentDto.class), eq(TENANT_ID)))
                    .thenReturn(Optional.of(sampleDocument()));

            mockMvc.perform(put("/api/documents/clinical/{id}", DOCUMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleDocument())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(DOCUMENT_ID.toString()));
        }

        @Test
        @DisplayName("Should return 404 when update target missing")
        void shouldReturn404WhenMissing() throws Exception {
            when(documentService.updateDocument(eq(DOCUMENT_ID), any(ClinicalDocumentDto.class), eq(TENANT_ID)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(put("/api/documents/clinical/{id}", DOCUMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleDocument())))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/documents/clinical/{id} Tests")
    class DeleteDocumentTests {

        @Test
        @DisplayName("Should delete document and return 204")
        void shouldDeleteDocument() throws Exception {
            when(documentService.deleteDocument(DOCUMENT_ID, TENANT_ID)).thenReturn(true);

            mockMvc.perform(delete("/api/documents/clinical/{id}", DOCUMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 when document not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(documentService.deleteDocument(DOCUMENT_ID, TENANT_ID)).thenReturn(false);

            mockMvc.perform(delete("/api/documents/clinical/{id}", DOCUMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Attachment endpoints Tests")
    class AttachmentTests {

        @Test
        @DisplayName("Should add attachment and return 201")
        void shouldAddAttachment() throws Exception {
            when(documentService.addAttachment(eq(DOCUMENT_ID), any(DocumentAttachmentDto.class), eq(TENANT_ID)))
                    .thenReturn(sampleAttachment());

            mockMvc.perform(post("/api/documents/clinical/{id}/attachments", DOCUMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleAttachment())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(ATTACHMENT_ID.toString()));
        }

        @Test
        @DisplayName("Should return attachment when found")
        void shouldReturnAttachmentWhenFound() throws Exception {
            when(documentService.getAttachment(ATTACHMENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(sampleAttachment()));

            mockMvc.perform(get("/api/documents/clinical/attachments/{attachmentId}", ATTACHMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(ATTACHMENT_ID.toString()));
        }

        @Test
        @DisplayName("Should return 404 when attachment not found")
        void shouldReturn404WhenAttachmentNotFound() throws Exception {
            when(documentService.getAttachment(ATTACHMENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/documents/clinical/attachments/{attachmentId}", ATTACHMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should delete attachment and return 204")
        void shouldDeleteAttachment() throws Exception {
            when(documentService.deleteAttachment(ATTACHMENT_ID, TENANT_ID)).thenReturn(true);

            mockMvc.perform(delete("/api/documents/clinical/attachments/{attachmentId}", ATTACHMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 when attachment delete target missing")
        void shouldReturn404WhenAttachmentMissing() throws Exception {
            when(documentService.deleteAttachment(ATTACHMENT_ID, TENANT_ID)).thenReturn(false);

            mockMvc.perform(delete("/api/documents/clinical/attachments/{attachmentId}", ATTACHMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("Should pass tenant ID through on create")
    void shouldPassTenantId() throws Exception {
        when(documentService.createDocument(any(ClinicalDocumentDto.class), eq(TENANT_ID)))
                .thenReturn(sampleDocument());

        mockMvc.perform(post("/api/documents/clinical")
                        .header("X-Tenant-ID", TENANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleDocument())))
                .andExpect(status().isCreated());

        verify(documentService).createDocument(any(ClinicalDocumentDto.class), eq(TENANT_ID));
    }

    private ClinicalDocumentDto sampleDocument() {
        return ClinicalDocumentDto.builder()
                .id(DOCUMENT_ID)
                .patientId("patient-456")
                .documentType("Discharge Summary")
                .status("current")
                .title("Summary")
                .documentDate(LocalDateTime.now())
                .build();
    }

    private DocumentAttachmentDto sampleAttachment() {
        return DocumentAttachmentDto.builder()
                .id(ATTACHMENT_ID)
                .clinicalDocumentId(DOCUMENT_ID)
                .contentType("application/pdf")
                .fileName("summary.pdf")
                .build();
    }
}
