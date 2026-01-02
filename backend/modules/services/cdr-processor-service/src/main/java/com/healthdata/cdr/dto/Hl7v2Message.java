package com.healthdata.cdr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * DTO representing a parsed HL7 v2 message.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Hl7v2Message {

    /**
     * Tenant ID for multi-tenant support.
     */
    private String tenantId;

    /**
     * Message type (e.g., ADT, ORU, ORM).
     */
    private String messageType;

    /**
     * Message trigger event (e.g., A01, R01, O01).
     */
    private String triggerEvent;

    /**
     * Full message type code (e.g., ADT^A01).
     */
    private String messageCode;

    /**
     * Message control ID from MSH segment.
     */
    private String messageControlId;

    /**
     * Sending application.
     */
    private String sendingApplication;

    /**
     * Sending facility.
     */
    private String sendingFacility;

    /**
     * Receiving application.
     */
    private String receivingApplication;

    /**
     * Receiving facility.
     */
    private String receivingFacility;

    /**
     * Message timestamp.
     */
    private LocalDateTime messageDateTime;

    /**
     * HL7 version (e.g., 2.5).
     */
    private String version;

    /**
     * Patient ID.
     */
    private String patientId;

    /**
     * Patient name.
     */
    private String patientName;

    /**
     * Visit/Encounter number.
     */
    private String visitNumber;

    /**
     * Raw HL7 message.
     */
    private String rawMessage;

    /**
     * Parsed message data (segment-specific).
     */
    private Map<String, Object> parsedData;

    /**
     * Processing status.
     */
    private String status;

    /**
     * Error message if parsing failed.
     */
    private String errorMessage;

    /**
     * Timestamp when the message was processed.
     */
    private LocalDateTime processedAt;
}
