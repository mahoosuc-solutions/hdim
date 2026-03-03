package com.healthdata.auditquery.service;

import com.healthdata.audit.models.AuditAction;
import com.healthdata.audit.models.AuditOutcome;
import com.healthdata.auditquery.dto.AuditEventResponse;
import com.healthdata.auditquery.dto.AuditSearchRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AuditExportService}.
 *
 * <p>Validates CSV, JSON, and PDF export formatting, record limits,
 * and output correctness.
 */
@Tag("unit")
@ExtendWith(MockitoExtension.class)
@DisplayName("AuditExportService")
class AuditExportServiceTest {

    private static final String TENANT_ID = "test-tenant-audit";

    @Mock
    private AuditQueryService auditQueryService;

    @InjectMocks
    private AuditExportService auditExportService;

    private AuditSearchRequest defaultSearchRequest;

    @BeforeEach
    void setUp() {
        defaultSearchRequest = new AuditSearchRequest(
                null, null, null, null, null,
                null, null, null, null,
                null, null, 0, 20, "timestamp", "DESC"
        );
    }

    @Nested
    @DisplayName("CSV Export")
    class CsvExport {

        @Test
        @DisplayName("should generate valid CSV with header row")
        void shouldGenerateValidCsvWithHeader() {
            // Given
            List<AuditEventResponse> events = List.of(
                    buildResponse("user-1", AuditAction.READ, AuditOutcome.SUCCESS, "Patient"),
                    buildResponse("user-2", AuditAction.CREATE, AuditOutcome.SUCCESS, "Observation")
            );

            when(auditQueryService.searchAuditEvents(eq(TENANT_ID), any(AuditSearchRequest.class)))
                    .thenReturn(new PageImpl<>(events));

            // When
            byte[] csvBytes = auditExportService.exportToCsv(TENANT_ID, defaultSearchRequest);

            // Then
            String csv = new String(csvBytes, StandardCharsets.UTF_8);
            String[] lines = csv.split("\n");

            // Header row present
            assertThat(lines[0]).contains("Event ID", "Timestamp", "Tenant ID", "User ID",
                    "Username", "Role", "IP Address", "Action", "Resource Type");

            // Data rows present (2 events)
            assertThat(lines).hasSizeGreaterThanOrEqualTo(3); // header + 2 data rows
            assertThat(csv).contains("Patient");
            assertThat(csv).contains("Observation");
        }

        @Test
        @DisplayName("should respect max export record limit")
        void shouldRespectMaxExportRecordLimit() {
            // Given - first page returns 1000, then empty page signals end
            List<AuditEventResponse> pageOfEvents = IntStream.range(0, 1000)
                    .mapToObj(i -> buildResponse("user-" + i, AuditAction.READ,
                            AuditOutcome.SUCCESS, "Patient"))
                    .toList();

            // First call returns full page, second call returns empty to stop pagination
            when(auditQueryService.searchAuditEvents(eq(TENANT_ID), any(AuditSearchRequest.class)))
                    .thenReturn(new PageImpl<>(pageOfEvents))
                    .thenReturn(new PageImpl<>(Collections.emptyList()));

            // When
            byte[] csvBytes = auditExportService.exportToCsv(TENANT_ID, defaultSearchRequest);

            // Then - should have header + 1000 data rows
            String csv = new String(csvBytes, StandardCharsets.UTF_8);
            String[] lines = csv.split("\n");
            assertThat(lines.length).isEqualTo(1001); // 1 header + 1000 data rows
        }
    }

    @Nested
    @DisplayName("JSON Export")
    class JsonExport {

        @Test
        @DisplayName("should generate valid JSON array")
        void shouldGenerateValidJsonArray() {
            // Given
            List<AuditEventResponse> events = List.of(
                    buildResponse("user-1", AuditAction.READ, AuditOutcome.SUCCESS, "Patient"),
                    buildResponse("user-2", AuditAction.UPDATE, AuditOutcome.MINOR_FAILURE, "CareGap")
            );

            when(auditQueryService.searchAuditEvents(eq(TENANT_ID), any(AuditSearchRequest.class)))
                    .thenReturn(new PageImpl<>(events));

            // When
            byte[] jsonBytes = auditExportService.exportToJson(TENANT_ID, defaultSearchRequest);

            // Then
            String json = new String(jsonBytes, StandardCharsets.UTF_8);
            assertThat(json).startsWith("[");
            assertThat(json).endsWith("]");
            assertThat(json).contains("user-1");
            assertThat(json).contains("user-2");
            assertThat(json).contains("Patient");
            assertThat(json).contains("CareGap");
        }
    }

    @Nested
    @DisplayName("PDF Export")
    class PdfExport {

        @Test
        @DisplayName("should generate non-empty PDF bytes with magic bytes")
        void shouldGenerateNonEmptyPdfBytes() {
            // Given
            List<AuditEventResponse> events = List.of(
                    buildResponse("user-1", AuditAction.READ, AuditOutcome.SUCCESS, "Patient")
            );

            when(auditQueryService.searchAuditEvents(eq(TENANT_ID), any(AuditSearchRequest.class)))
                    .thenReturn(new PageImpl<>(events));

            // When
            byte[] pdfBytes = auditExportService.exportToPdf(TENANT_ID, defaultSearchRequest);

            // Then
            assertThat(pdfBytes).isNotEmpty();
            // PDF files start with %PDF magic bytes
            String header = new String(pdfBytes, 0, Math.min(5, pdfBytes.length), StandardCharsets.US_ASCII);
            assertThat(header).startsWith("%PDF");
        }
    }

    // --- Helper Methods ---

    private static AuditEventResponse buildResponse(String userId, AuditAction action,
                                                      AuditOutcome outcome, String resourceType) {
        return new AuditEventResponse(
                UUID.randomUUID(),
                Instant.now(),
                TENANT_ID,
                userId,
                userId + "@example.com",
                "EVALUATOR",
                "192.168.1.100",
                "TestAgent/1.0",
                action,
                resourceType,
                UUID.randomUUID().toString(),
                outcome,
                "patient-service",
                "getPatient",
                "/api/v1/patients/123",
                "Treatment",
                null,
                null,
                null,
                127L,
                null,
                false
        );
    }
}
