package com.healthdata.documentation.rest;

import com.healthdata.documentation.dto.ClinicalDocumentDto;
import com.healthdata.documentation.dto.DocumentAttachmentDto;
import com.healthdata.documentation.persistence.DocumentAttachmentEntity;
import com.healthdata.documentation.repository.ClinicalDocumentRepository;
import com.healthdata.documentation.repository.DocumentAttachmentRepository;
import com.healthdata.documentation.service.ClinicalDocumentService;
import com.healthdata.documentation.test.TestFileGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for OCR functionality
 *
 * Tests cover:
 * - PDF upload with OCR extraction
 * - Image upload with OCR extraction
 * - OCR status polling
 * - OCR reprocessing
 * - Full-text search on OCR'd documents
 * - Error handling for unsupported files
 *
 * Uses Testcontainers for real PostgreSQL database with full-text search
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class OcrIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClinicalDocumentService documentService;

    @Autowired
    private ClinicalDocumentRepository documentRepository;

    @Autowired
    private DocumentAttachmentRepository attachmentRepository;

    @TempDir
    Path tempDir;

    private static final String TENANT_ID = "test-tenant";
    private static final String PATIENT_ID = "patient-123";

    private UUID documentId;

    @BeforeEach
    void setUp() {
        // Clean up test data
        attachmentRepository.deleteAll();
        documentRepository.deleteAll();

        // Create a test clinical document
        ClinicalDocumentDto documentDto = new ClinicalDocumentDto();
        documentDto.setPatientId(PATIENT_ID);
        documentDto.setDocumentType("LAB_RESULT");
        documentDto.setTitle("Test Lab Result");
        documentDto.setStatus("FINAL");

        ClinicalDocumentDto created = documentService.createDocument(documentDto, TENANT_ID);
        documentId = created.getId();
    }

    /**
     * Test PDF upload with OCR extraction
     * Verifies async OCR processing and text extraction
     */
    @Test
    @WithMockUser(authorities = "CONFIG_READ")
    void testPdfUploadWithOcrExtraction() throws Exception {
        // Create a test PDF with text
        byte[] pdfContent = createTestPdfWithText("Lab Result: Hemoglobin A1c 7.2%");
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "lab-result.pdf",
                "application/pdf",
                pdfContent
        );

        // Upload the PDF
        String response = mockMvc.perform(multipart("/api/documents/clinical/{id}/upload", documentId)
                        .file(pdfFile)
                        .param("title", "Lab Result PDF")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("lab-result.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.ocrStatus").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID attachmentId = extractAttachmentId(response);

        // Wait for async OCR processing to complete (max 30 seconds)
        await()
                .atMost(java.time.Duration.ofSeconds(30))
                .pollInterval(java.time.Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    Optional<DocumentAttachmentEntity> attachment = attachmentRepository.findByIdAndTenantId(attachmentId, TENANT_ID);
                    assertThat(attachment).isPresent();
                    assertThat(attachment.get().getOcrStatus()).isEqualTo("COMPLETED");
                });

        // Verify OCR text was extracted
        Optional<DocumentAttachmentEntity> attachment = attachmentRepository.findByIdAndTenantId(attachmentId, TENANT_ID);
        assertThat(attachment).isPresent();
        assertThat(attachment.get().getOcrText()).isNotEmpty();
        assertThat(attachment.get().getOcrText()).containsIgnoringCase("Hemoglobin");
        assertThat(attachment.get().getOcrText()).containsIgnoringCase("7.2");
        assertThat(attachment.get().getOcrProcessedAt()).isNotNull();
        assertThat(attachment.get().getOcrErrorMessage()).isNull();
    }

    /**
     * Test image upload with OCR extraction
     * Verifies Tesseract OCR processing for PNG images
     */
    @Test
    @WithMockUser(authorities = "CONFIG_READ")
    void testImageUploadWithOcrExtraction() throws Exception {
        // Create a test PNG image with text
        byte[] pngContent = createTestImageWithText("Patient Name: John Doe\nDOB: 01/15/1980");
        MockMultipartFile pngFile = new MockMultipartFile(
                "file",
                "patient-demographics.png",
                "image/png",
                pngContent
        );

        // Upload the image
        String response = mockMvc.perform(multipart("/api/documents/clinical/{id}/upload", documentId)
                        .file(pngFile)
                        .param("title", "Patient Demographics")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("patient-demographics.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.ocrStatus").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID attachmentId = extractAttachmentId(response);

        // Wait for async OCR processing
        await()
                .atMost(java.time.Duration.ofSeconds(30))
                .pollInterval(java.time.Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    Optional<DocumentAttachmentEntity> attachment = attachmentRepository.findByIdAndTenantId(attachmentId, TENANT_ID);
                    assertThat(attachment).isPresent();
                    assertThat(attachment.get().getOcrStatus()).isIn("COMPLETED", "FAILED");
                });

        // Verify OCR text extraction
        Optional<DocumentAttachmentEntity> attachment = attachmentRepository.findByIdAndTenantId(attachmentId, TENANT_ID);
        assertThat(attachment).isPresent();

        if (attachment.get().getOcrStatus().equals("COMPLETED")) {
            assertThat(attachment.get().getOcrText()).isNotEmpty();
            // Note: OCR accuracy may vary, so we check for partial matches
            assertThat(attachment.get().getOcrText().toLowerCase()).containsAnyOf("john", "doe", "patient");
        }
    }

    /**
     * Test OCR status polling
     * Verifies status endpoint returns correct processing state
     */
    @Test
    @WithMockUser(authorities = "CONFIG_READ")
    void testOcrStatusPolling() throws Exception {
        // Upload a test file
        byte[] pdfContent = createTestPdfWithText("Status test content");
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "status-test.pdf",
                "application/pdf",
                pdfContent
        );

        String response = mockMvc.perform(multipart("/api/documents/clinical/{id}/upload", documentId)
                        .file(pdfFile)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID attachmentId = extractAttachmentId(response);

        // Poll OCR status
        mockMvc.perform(get("/api/documents/clinical/attachments/{attachmentId}/ocr-status", attachmentId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.status").isIn("PENDING", "PROCESSING", "COMPLETED", "FAILED"))
                .andExpect(jsonPath("$.hasText").exists());

        // Wait for completion
        await()
                .atMost(java.time.Duration.ofSeconds(30))
                .pollInterval(java.time.Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    mockMvc.perform(get("/api/documents/clinical/attachments/{attachmentId}/ocr-status", attachmentId)
                                    .header("X-Tenant-ID", TENANT_ID))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath("$.status").value("COMPLETED"))
                            .andExpect(jsonPath("$.processedAt").exists())
                            .andExpect(jsonPath("$.hasText").value(true));
                });
    }

    /**
     * Test OCR reprocessing
     * Verifies ability to retry failed or incomplete OCR processing
     */
    @Test
    @WithMockUser(authorities = "CONFIG_READ")
    void testOcrReprocessing() throws Exception {
        // Upload a test file
        byte[] pdfContent = createTestPdfWithText("Reprocess test content");
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                "reprocess-test.pdf",
                "application/pdf",
                pdfContent
        );

        String response = mockMvc.perform(multipart("/api/documents/clinical/{id}/upload", documentId)
                        .file(pdfFile)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID attachmentId = extractAttachmentId(response);

        // Wait for initial processing
        await()
                .atMost(java.time.Duration.ofSeconds(30))
                .pollInterval(java.time.Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    Optional<DocumentAttachmentEntity> attachment = attachmentRepository.findByIdAndTenantId(attachmentId, TENANT_ID);
                    assertThat(attachment).isPresent();
                    assertThat(attachment.get().getOcrStatus()).isIn("COMPLETED", "FAILED");
                });

        // Trigger reprocessing
        mockMvc.perform(post("/api/documents/clinical/attachments/{attachmentId}/reprocess-ocr", attachmentId)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isAccepted());

        // Verify status reset to PENDING
        Optional<DocumentAttachmentEntity> attachment = attachmentRepository.findByIdAndTenantId(attachmentId, TENANT_ID);
        assertThat(attachment).isPresent();
        assertThat(attachment.get().getOcrStatus()).isEqualTo("PENDING");

        // Wait for reprocessing to complete
        await()
                .atMost(java.time.Duration.ofSeconds(30))
                .pollInterval(java.time.Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    Optional<DocumentAttachmentEntity> reprocessed = attachmentRepository.findByIdAndTenantId(attachmentId, TENANT_ID);
                    assertThat(reprocessed).isPresent();
                    assertThat(reprocessed.get().getOcrStatus()).isEqualTo("COMPLETED");
                });
    }

    /**
     * Test full-text search on OCR'd documents
     * Verifies PostgreSQL full-text search with ts_vector and relevance ranking
     */
    @Test
    @WithMockUser(authorities = "CONFIG_READ")
    void testFullTextSearchOnOcrDocuments() throws Exception {
        // Upload multiple documents with different content
        uploadAndWaitForOcr("diabetes-diagnosis.pdf", "Diagnosis: Type 2 Diabetes Mellitus. HbA1c: 8.5%");
        uploadAndWaitForOcr("hypertension-note.pdf", "Blood Pressure: 150/95 mmHg. Diagnosis: Essential Hypertension");
        uploadAndWaitForOcr("cholesterol-lab.pdf", "Total Cholesterol: 240 mg/dL. LDL: 160 mg/dL. High cholesterol");

        // Search for diabetes
        mockMvc.perform(get("/api/documents/clinical/search-ocr")
                        .param("query", "diabetes")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].fileName").value("diabetes-diagnosis.pdf"))
                .andExpect(jsonPath("$.content[0].ocrText").value(org.hamcrest.Matchers.containsStringIgnoringCase("diabetes")));

        // Search for hypertension
        mockMvc.perform(get("/api/documents/clinical/search-ocr")
                        .param("query", "hypertension blood pressure")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].fileName").value("hypertension-note.pdf"));

        // Search for cholesterol (should return cholesterol lab first, then potentially diabetes due to related terms)
        mockMvc.perform(get("/api/documents/clinical/search-ocr")
                        .param("query", "cholesterol")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].fileName").value("cholesterol-lab.pdf"));

        // Verify pagination
        mockMvc.perform(get("/api/documents/clinical/search-ocr")
                        .param("query", "diagnosis")
                        .param("page", "0")
                        .param("size", "2")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.size").value(2));
    }

    /**
     * Test error handling for unsupported file types
     * Verifies validation of file content types
     */
    @Test
    @WithMockUser(authorities = "CONFIG_READ")
    void testUnsupportedFileTypeRejection() throws Exception {
        // Try to upload unsupported file type
        MockMultipartFile unsupportedFile = new MockMultipartFile(
                "file",
                "document.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "fake docx content".getBytes()
        );

        mockMvc.perform(multipart("/api/documents/clinical/{id}/upload", documentId)
                        .file(unsupportedFile)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test error handling for oversized files
     * Verifies file size limit enforcement
     */
    @Test
    @WithMockUser(authorities = "CONFIG_READ")
    void testOversizedFileRejection() throws Exception {
        // Create a file larger than the 10MB limit
        byte[] largeContent = new byte[11 * 1024 * 1024]; // 11MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file",
                "large-document.pdf",
                "application/pdf",
                largeContent
        );

        mockMvc.perform(multipart("/api/documents/clinical/{id}/upload", documentId)
                        .file(largeFile)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test multi-tenant isolation for OCR search
     * Verifies tenant-based data isolation
     */
    @Test
    @WithMockUser(authorities = "CONFIG_READ")
    void testMultiTenantOcrSearchIsolation() throws Exception {
        // Upload document for tenant1
        uploadAndWaitForOcr("tenant1-doc.pdf", "Tenant 1 confidential data");

        // Create document for tenant2
        String tenant2 = "tenant2";
        ClinicalDocumentDto tenant2Doc = new ClinicalDocumentDto();
        tenant2Doc.setPatientId("patient-456");
        tenant2Doc.setDocumentType("LAB_RESULT");
        tenant2Doc.setTitle("Tenant 2 Document");
        tenant2Doc.setStatus("FINAL");
        ClinicalDocumentDto tenant2Created = documentService.createDocument(tenant2Doc, tenant2);

        // Upload document for tenant2
        byte[] tenant2Content = createTestPdfWithText("Tenant 2 confidential data");
        MockMultipartFile tenant2File = new MockMultipartFile(
                "file",
                "tenant2-doc.pdf",
                "application/pdf",
                tenant2Content
        );

        String response = mockMvc.perform(multipart("/api/documents/clinical/{id}/upload", tenant2Created.getId())
                        .file(tenant2File)
                        .header("X-Tenant-ID", tenant2))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID tenant2AttachmentId = extractAttachmentId(response);

        await()
                .atMost(java.time.Duration.ofSeconds(30))
                .pollInterval(java.time.Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    Optional<DocumentAttachmentEntity> attachment = attachmentRepository.findByIdAndTenantId(tenant2AttachmentId, tenant2);
                    assertThat(attachment).isPresent();
                    assertThat(attachment.get().getOcrStatus()).isEqualTo("COMPLETED");
                });

        // Search as tenant1 - should only see tenant1 data
        mockMvc.perform(get("/api/documents/clinical/search-ocr")
                        .param("query", "confidential")
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fileName").value("tenant1-doc.pdf"));

        // Search as tenant2 - should only see tenant2 data
        mockMvc.perform(get("/api/documents/clinical/search-ocr")
                        .param("query", "confidential")
                        .header("X-Tenant-ID", tenant2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].fileName").value("tenant2-doc.pdf"));
    }

    // Helper methods

    private void uploadAndWaitForOcr(String fileName, String content) throws Exception {
        byte[] pdfContent = createTestPdfWithText(content);
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file",
                fileName,
                "application/pdf",
                pdfContent
        );

        String response = mockMvc.perform(multipart("/api/documents/clinical/{id}/upload", documentId)
                        .file(pdfFile)
                        .header("X-Tenant-ID", TENANT_ID))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        UUID attachmentId = extractAttachmentId(response);

        await()
                .atMost(java.time.Duration.ofSeconds(30))
                .pollInterval(java.time.Duration.ofSeconds(2))
                .untilAsserted(() -> {
                    Optional<DocumentAttachmentEntity> attachment = attachmentRepository.findByIdAndTenantId(attachmentId, TENANT_ID);
                    assertThat(attachment).isPresent();
                    assertThat(attachment.get().getOcrStatus()).isEqualTo("COMPLETED");
                });
    }

    private UUID extractAttachmentId(String response) {
        // Simple JSON parsing to extract ID
        // In real implementation, use Jackson ObjectMapper
        int idStart = response.indexOf("\"id\":\"") + 6;
        int idEnd = response.indexOf("\"", idStart);
        return UUID.fromString(response.substring(idStart, idEnd));
    }

    /**
     * Create a simple test PDF with embedded text
     * Uses PDFBox to create a basic PDF document
     */
    private byte[] createTestPdfWithText(String text) throws IOException {
        return TestFileGenerator.createPdfWithText(text);
    }

    /**
     * Create a test image with text for OCR
     * Uses Java AWT to create a simple image
     */
    private byte[] createTestImageWithText(String text) throws IOException {
        return TestFileGenerator.createPngImageWithText(text);
    }
}
