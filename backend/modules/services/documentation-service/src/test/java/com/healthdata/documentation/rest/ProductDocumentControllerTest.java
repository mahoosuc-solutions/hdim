package com.healthdata.documentation.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.documentation.dto.DocumentFeedbackDto;
import com.healthdata.documentation.dto.DocumentVersionDto;
import com.healthdata.documentation.dto.ProductDocumentDto;
import com.healthdata.documentation.service.ProductDocumentService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("Product Document Controller Tests")
class ProductDocumentControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private ProductDocumentService documentService;

    @InjectMocks
    private ProductDocumentController controller;

    private static final String TENANT_ID = "tenant-123";
    private static final String DOCUMENT_ID = "getting-started";
    private static final UUID VERSION_ID = UUID.randomUUID();
    private static final UUID FEEDBACK_ID = UUID.randomUUID();
    private static final String EDITOR_USER = "editor";
    private static final String FEEDBACK_USER = "user-1";

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
    @DisplayName("GET /api/documents/product Tests")
    class GetDocumentsTests {

        @Test
        @DisplayName("Should return documents for tenant")
        void shouldReturnDocuments() throws Exception {
            when(documentService.getDocuments(TENANT_ID)).thenReturn(List.of(sampleDocument()));

            mockMvc.perform(get("/api/documents/product")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(DOCUMENT_ID));
        }

        @Test
        @DisplayName("Should return paginated documents")
        void shouldReturnPaginatedDocuments() throws Exception {
            Pageable pageable = PageRequest.of(0, 3);
            Page<ProductDocumentDto> page = new PageImpl<>(List.of(sampleDocument()), pageable, 1);
            when(documentService.getDocumentsPaginated(eq(TENANT_ID), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/documents/product/paginated")
                            .param("page", "0")
                            .param("size", "3")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(DOCUMENT_ID));
        }

        @Test
        @DisplayName("Should return published documents")
        void shouldReturnPublishedDocuments() throws Exception {
            Pageable pageable = PageRequest.of(0, 5);
            Page<ProductDocumentDto> page = new PageImpl<>(List.of(sampleDocument()), pageable, 1);
            when(documentService.getPublishedDocuments(eq(TENANT_ID), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/documents/product/published")
                            .param("page", "0")
                            .param("size", "5")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(DOCUMENT_ID));
        }
    }

    @Nested
    @DisplayName("GET /api/documents/product/{id} Tests")
    class GetDocumentTests {

        @Test
        @DisplayName("Should return document when found")
        void shouldReturnDocumentWhenFound() throws Exception {
            when(documentService.getDocument(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(sampleDocument()));

            mockMvc.perform(get("/api/documents/product/{id}", DOCUMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(DOCUMENT_ID));
        }

        @Test
        @DisplayName("Should return 404 when document not found")
        void shouldReturn404WhenNotFound() throws Exception {
            when(documentService.getDocument(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(get("/api/documents/product/{id}", DOCUMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("GET /api/documents/product/search should return results")
    void shouldSearchDocuments() throws Exception {
        Pageable pageable = PageRequest.of(0, 10);
        Page<ProductDocumentDto> page = new PageImpl<>(List.of(sampleDocument()), pageable, 1);
        when(documentService.searchDocuments(eq(TENANT_ID), eq("q"), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/documents/product/search")
                        .param("query", "q")
                        .param("page", "0")
                        .param("size", "10")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(DOCUMENT_ID));
    }

    @Test
    @DisplayName("GET /api/documents/product/categories should return categories")
    void shouldReturnCategories() throws Exception {
        when(documentService.getCategories(TENANT_ID)).thenReturn(List.of("guides", "api"));

        mockMvc.perform(get("/api/documents/product/categories")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("guides"));
    }

    @Nested
    @DisplayName("POST /api/documents/product Tests")
    class CreateDocumentTests {

        @Test
        @DisplayName("Should create document and return 201")
        void shouldCreateDocument() throws Exception {
            when(documentService.createDocument(any(ProductDocumentDto.class), eq(TENANT_ID)))
                    .thenReturn(sampleDocument());

            mockMvc.perform(post("/api/documents/product")
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleDocument())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(DOCUMENT_ID));
        }
    }

    @Nested
    @DisplayName("PUT /api/documents/product/{id} Tests")
    class UpdateDocumentTests {

        @Test
        @DisplayName("Should update document")
        void shouldUpdateDocument() throws Exception {
            when(documentService.updateDocument(eq(DOCUMENT_ID), any(ProductDocumentDto.class), eq(TENANT_ID), eq(EDITOR_USER)))
                    .thenReturn(Optional.of(sampleDocument()));

            mockMvc.perform(put("/api/documents/product/{id}", DOCUMENT_ID)
                            .principal(new UsernamePasswordAuthenticationToken(EDITOR_USER, "n/a"))
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleDocument())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(DOCUMENT_ID));
        }

        @Test
        @DisplayName("Should return 404 when document not found for update")
        void shouldReturn404WhenNotFound() throws Exception {
            when(documentService.updateDocument(eq(DOCUMENT_ID), any(ProductDocumentDto.class), eq(TENANT_ID), eq(EDITOR_USER)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(put("/api/documents/product/{id}", DOCUMENT_ID)
                            .principal(new UsernamePasswordAuthenticationToken(EDITOR_USER, "n/a"))
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleDocument())))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Publish/Archive/Delete Tests")
    class PublishArchiveDeleteTests {

        @Test
        @DisplayName("Should publish document")
        void shouldPublishDocument() throws Exception {
            when(documentService.publishDocument(eq(DOCUMENT_ID), eq(TENANT_ID), eq(EDITOR_USER)))
                    .thenReturn(Optional.of(sampleDocument()));

            mockMvc.perform(post("/api/documents/product/{id}/publish", DOCUMENT_ID)
                            .principal(new UsernamePasswordAuthenticationToken(EDITOR_USER, "n/a"))
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(DOCUMENT_ID));
        }

        @Test
        @DisplayName("Should return 404 when publish target missing")
        void shouldReturn404WhenPublishMissing() throws Exception {
            when(documentService.publishDocument(eq(DOCUMENT_ID), eq(TENANT_ID), eq(EDITOR_USER)))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post("/api/documents/product/{id}/publish", DOCUMENT_ID)
                            .principal(new UsernamePasswordAuthenticationToken(EDITOR_USER, "n/a"))
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should archive document")
        void shouldArchiveDocument() throws Exception {
            when(documentService.archiveDocument(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.of(sampleDocument()));

            mockMvc.perform(post("/api/documents/product/{id}/archive", DOCUMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(DOCUMENT_ID));
        }

        @Test
        @DisplayName("Should return 404 when archive target missing")
        void shouldReturn404WhenArchiveMissing() throws Exception {
            when(documentService.archiveDocument(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(Optional.empty());

            mockMvc.perform(post("/api/documents/product/{id}/archive", DOCUMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("Should delete document")
        void shouldDeleteDocument() throws Exception {
            when(documentService.deleteDocument(DOCUMENT_ID, TENANT_ID)).thenReturn(true);

            mockMvc.perform(delete("/api/documents/product/{id}", DOCUMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @DisplayName("Should return 404 when delete target missing")
        void shouldReturn404WhenDeleteMissing() throws Exception {
            when(documentService.deleteDocument(DOCUMENT_ID, TENANT_ID)).thenReturn(false);

            mockMvc.perform(delete("/api/documents/product/{id}", DOCUMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("Versions and Feedback Tests")
    class VersionAndFeedbackTests {

        @Test
        @DisplayName("Should return document versions")
        void shouldReturnVersions() throws Exception {
            when(documentService.getVersions(DOCUMENT_ID, TENANT_ID))
                    .thenReturn(List.of(sampleVersion()));

            mockMvc.perform(get("/api/documents/product/{id}/versions", DOCUMENT_ID)
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(VERSION_ID.toString()));
        }

        @Test
        @DisplayName("Should create new version")
        void shouldCreateVersion() throws Exception {
            when(documentService.createVersion(eq(DOCUMENT_ID), any(DocumentVersionDto.class), eq(TENANT_ID), eq(EDITOR_USER)))
                    .thenReturn(sampleVersion());

            mockMvc.perform(post("/api/documents/product/{id}/versions", DOCUMENT_ID)
                            .principal(new UsernamePasswordAuthenticationToken(EDITOR_USER, "n/a"))
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleVersion())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(VERSION_ID.toString()));
        }

        @Test
        @DisplayName("Should submit feedback")
        void shouldSubmitFeedback() throws Exception {
            when(documentService.submitFeedback(eq(DOCUMENT_ID), any(DocumentFeedbackDto.class), eq(TENANT_ID), eq(FEEDBACK_USER)))
                    .thenReturn(sampleFeedback());

            mockMvc.perform(post("/api/documents/product/{id}/feedback", DOCUMENT_ID)
                            .principal(new UsernamePasswordAuthenticationToken(FEEDBACK_USER, "n/a"))
                            .header("X-Tenant-ID", TENANT_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(sampleFeedback())))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(FEEDBACK_ID.toString()));
        }

        @Test
        @DisplayName("Should return feedback page")
        void shouldReturnFeedback() throws Exception {
            Pageable pageable = PageRequest.of(0, 4);
            Page<DocumentFeedbackDto> page = new PageImpl<>(List.of(sampleFeedback()), pageable, 1);
            when(documentService.getFeedback(eq(DOCUMENT_ID), eq(TENANT_ID), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/documents/product/{id}/feedback", DOCUMENT_ID)
                            .param("page", "0")
                            .param("size", "4")
                            .header("X-Tenant-ID", TENANT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(FEEDBACK_ID.toString()));
        }
    }

    private ProductDocumentDto sampleDocument() {
        return ProductDocumentDto.builder()
                .id(DOCUMENT_ID)
                .title("Getting Started Guide")
                .portalType("product")
                .path("/docs/getting-started")
                .category("guides")
                .summary("Intro guide for new users.")
                .tags(new String[]{"intro", "setup", "basics"})
                .version("1.0")
                .status("draft")
                .lastUpdated(LocalDate.now())
                .build();
    }

    private DocumentVersionDto sampleVersion() {
        return DocumentVersionDto.builder()
                .id(VERSION_ID)
                .documentId(DOCUMENT_ID)
                .versionNumber("1.1")
                .content("Updated content")
                .changedBy("editor")
                .isMajorVersion(false)
                .isPublished(false)
                .build();
    }

    private DocumentFeedbackDto sampleFeedback() {
        return DocumentFeedbackDto.builder()
                .id(FEEDBACK_ID)
                .documentId(DOCUMENT_ID)
                .rating(4)
                .comment("Helpful")
                .feedbackType("GENERAL")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
