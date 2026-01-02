package com.healthdata.cdr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO representing a parsed CDA/C-CDA document.
 * Contains extracted clinical data from various CDA sections.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CdaDocument {

    /**
     * Tenant ID for multi-tenant support.
     */
    private String tenantId;

    /**
     * Document unique identifier from CDA header.
     */
    private String documentId;

    /**
     * Document type (CCD, DischargeSummary, ProgressNote, etc.).
     */
    private String documentType;

    /**
     * C-CDA template OID identifying the document type.
     */
    private String templateId;

    /**
     * Raw CDA XML document.
     */
    private String rawDocument;

    /**
     * Document effective time from CDA header.
     */
    private LocalDateTime effectiveTime;

    /**
     * Confidentiality code (N, R, V).
     */
    private String confidentialityCode;

    /**
     * Document title from CDA header.
     */
    private String title;

    /**
     * Custodian organization information.
     */
    private Map<String, Object> custodian;

    /**
     * Author information from CDA header.
     */
    private List<Map<String, Object>> authors;

    // Extracted clinical sections

    /**
     * Patient demographics from recordTarget.
     */
    private Map<String, Object> patient;

    /**
     * Problems/Conditions from Problem Section (2.16.840.1.113883.10.20.22.2.5.1).
     */
    private List<Map<String, Object>> problems;

    /**
     * Medications from Medications Section (2.16.840.1.113883.10.20.22.2.1.1).
     */
    private List<Map<String, Object>> medications;

    /**
     * Allergies from Allergies Section (2.16.840.1.113883.10.20.22.2.6.1).
     */
    private List<Map<String, Object>> allergies;

    /**
     * Immunizations from Immunizations Section (2.16.840.1.113883.10.20.22.2.2.1).
     */
    private List<Map<String, Object>> immunizations;

    /**
     * Procedures from Procedures Section (2.16.840.1.113883.10.20.22.2.7.1).
     */
    private List<Map<String, Object>> procedures;

    /**
     * Lab Results from Results Section (2.16.840.1.113883.10.20.22.2.3.1).
     */
    private List<Map<String, Object>> results;

    /**
     * Vital Signs from Vital Signs Section (2.16.840.1.113883.10.20.22.2.4.1).
     */
    private List<Map<String, Object>> vitalSigns;

    /**
     * Encounters from Encounters Section (2.16.840.1.113883.10.20.22.2.22.1).
     */
    private List<Map<String, Object>> encounters;

    /**
     * Social History from Social History Section.
     */
    private List<Map<String, Object>> socialHistory;

    /**
     * Family History from Family History Section.
     */
    private List<Map<String, Object>> familyHistory;

    // Processing metadata

    /**
     * Processing status (PARSED, ERROR, CONVERTED).
     */
    private String status;

    /**
     * Error message if processing failed.
     */
    private String errorMessage;

    /**
     * Timestamp when document was processed.
     */
    private LocalDateTime processedAt;

    /**
     * Validation warnings (non-fatal issues).
     */
    private List<String> warnings;
}
