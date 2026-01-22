package com.healthdata.auditquery.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.healthdata.auditquery.dto.AuditEventResponse;
import com.healthdata.auditquery.dto.AuditSearchRequest;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.opencsv.CSVWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for exporting audit logs to various formats.
 *
 * <p>Supports CSV, JSON, and PDF exports for HIPAA compliance reporting.
 * All exports are limited to 100,000 records to prevent memory overflow.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditExportService {

    private final AuditQueryService auditQueryService;
    private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .enable(SerializationFeature.INDENT_OUTPUT);

    private static final int MAX_EXPORT_RECORDS = 100000;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_INSTANT;

    /**
     * Export audit logs to CSV format.
     *
     * @param tenantId tenant ID for isolation
     * @param request search criteria
     * @return CSV file as byte array
     */
    public byte[] exportToCsv(String tenantId, AuditSearchRequest request) {
        log.info("Exporting audit logs to CSV for tenant {}", tenantId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter osw = new OutputStreamWriter(baos, StandardCharsets.UTF_8);
             CSVWriter writer = new CSVWriter(osw)) {

            // Write header
            String[] header = {
                "Event ID", "Timestamp", "Tenant ID", "User ID", "Username", "Role",
                "IP Address", "Action", "Resource Type", "Resource ID", "Outcome",
                "Service Name", "Method Name", "Request Path", "Purpose of Use",
                "Duration (ms)", "Error Message"
            };
            writer.writeNext(header);

            // Fetch and write data (paginated to prevent memory issues)
            int pageSize = 1000;
            int currentPage = 0;
            int totalWritten = 0;

            while (totalWritten < MAX_EXPORT_RECORDS) {
                AuditSearchRequest pageRequest = new AuditSearchRequest(
                    request.userId(), request.username(), request.role(),
                    request.resourceType(), request.resourceId(),
                    request.action(), request.outcome(), request.serviceName(),
                    request.ipAddress(), request.startTime(), request.endTime(),
                    currentPage, pageSize, request.sortBy(), request.sortDirection()
                );

                Page<AuditEventResponse> page = auditQueryService.searchAuditEvents(tenantId, pageRequest);

                if (page.isEmpty()) {
                    break;
                }

                for (AuditEventResponse event : page.getContent()) {
                    String[] row = {
                        event.id().toString(),
                        event.timestamp().toString(),
                        event.tenantId(),
                        event.userId(),
                        event.username(),
                        event.role(),
                        event.ipAddress(),
                        event.action() != null ? event.action().name() : "",
                        event.resourceType(),
                        event.resourceId(),
                        event.outcome() != null ? event.outcome().name() : "",
                        event.serviceName(),
                        event.methodName(),
                        event.requestPath(),
                        event.purposeOfUse(),
                        event.durationMs() != null ? event.durationMs().toString() : "",
                        event.errorMessage()
                    };
                    writer.writeNext(row);
                    totalWritten++;
                }

                if (page.isLast() || totalWritten >= MAX_EXPORT_RECORDS) {
                    break;
                }

                currentPage++;
            }

            writer.flush();
            log.info("CSV export complete: {} records written", totalWritten);
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to export audit logs to CSV", e);
            throw new RuntimeException("CSV export failed", e);
        }
    }

    /**
     * Export audit logs to JSON format.
     *
     * @param tenantId tenant ID for isolation
     * @param request search criteria
     * @return JSON file as byte array
     */
    public byte[] exportToJson(String tenantId, AuditSearchRequest request) {
        log.info("Exporting audit logs to JSON for tenant {}", tenantId);

        try {
            List<AuditEventResponse> allEvents = new ArrayList<>();
            int pageSize = 1000;
            int currentPage = 0;
            int totalFetched = 0;

            while (totalFetched < MAX_EXPORT_RECORDS) {
                AuditSearchRequest pageRequest = new AuditSearchRequest(
                    request.userId(), request.username(), request.role(),
                    request.resourceType(), request.resourceId(),
                    request.action(), request.outcome(), request.serviceName(),
                    request.ipAddress(), request.startTime(), request.endTime(),
                    currentPage, pageSize, request.sortBy(), request.sortDirection()
                );

                Page<AuditEventResponse> page = auditQueryService.searchAuditEvents(tenantId, pageRequest);

                if (page.isEmpty()) {
                    break;
                }

                allEvents.addAll(page.getContent());
                totalFetched += page.getNumberOfElements();

                if (page.isLast() || totalFetched >= MAX_EXPORT_RECORDS) {
                    break;
                }

                currentPage++;
            }

            log.info("JSON export complete: {} records written", totalFetched);
            return objectMapper.writeValueAsBytes(allEvents);

        } catch (Exception e) {
            log.error("Failed to export audit logs to JSON", e);
            throw new RuntimeException("JSON export failed", e);
        }
    }

    /**
     * Export audit logs to PDF format.
     *
     * @param tenantId tenant ID for isolation
     * @param request search criteria
     * @return PDF file as byte array
     */
    public byte[] exportToPdf(String tenantId, AuditSearchRequest request) {
        log.info("Exporting audit logs to PDF for tenant {}", tenantId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Title
            document.add(new Paragraph("HIPAA Audit Log Report")
                .setBold()
                .setFontSize(18));

            document.add(new Paragraph("Tenant: " + tenantId)
                .setFontSize(12));

            if (request.startTime() != null || request.endTime() != null) {
                String timeRange = String.format("Time Range: %s to %s",
                    request.startTime() != null ? FORMATTER.format(request.startTime()) : "Beginning",
                    request.endTime() != null ? FORMATTER.format(request.endTime()) : "Now"
                );
                document.add(new Paragraph(timeRange).setFontSize(10));
            }

            document.add(new Paragraph("\n"));

            // Table with key columns (limited for PDF readability)
            float[] columnWidths = {80f, 100f, 80f, 80f, 100f, 80f};
            Table table = new Table(columnWidths);
            table.addHeaderCell("Timestamp");
            table.addHeaderCell("User");
            table.addHeaderCell("Action");
            table.addHeaderCell("Resource Type");
            table.addHeaderCell("Resource ID");
            table.addHeaderCell("Outcome");

            // Fetch and write data (limited to 1000 records for PDF)
            int maxPdfRecords = Math.min(MAX_EXPORT_RECORDS, 1000);
            AuditSearchRequest pageRequest = new AuditSearchRequest(
                request.userId(), request.username(), request.role(),
                request.resourceType(), request.resourceId(),
                request.action(), request.outcome(), request.serviceName(),
                request.ipAddress(), request.startTime(), request.endTime(),
                0, maxPdfRecords, request.sortBy(), request.sortDirection()
            );

            Page<AuditEventResponse> page = auditQueryService.searchAuditEvents(tenantId, pageRequest);

            for (AuditEventResponse event : page.getContent()) {
                table.addCell(FORMATTER.format(event.timestamp()));
                table.addCell(event.username() != null ? event.username() : event.userId());
                table.addCell(event.action() != null ? event.action().name() : "");
                table.addCell(event.resourceType() != null ? event.resourceType() : "");
                table.addCell(event.resourceId() != null ? event.resourceId() : "");
                table.addCell(event.outcome() != null ? event.outcome().name() : "");
            }

            document.add(table);

            if (page.getTotalElements() > maxPdfRecords) {
                document.add(new Paragraph(String.format(
                    "\nNote: Showing %d of %d total records. Use CSV or JSON export for complete data.",
                    maxPdfRecords, page.getTotalElements()
                )).setFontSize(10).setItalic());
            }

            document.close();
            log.info("PDF export complete: {} records written", page.getNumberOfElements());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Failed to export audit logs to PDF", e);
            throw new RuntimeException("PDF export failed", e);
        }
    }
}
