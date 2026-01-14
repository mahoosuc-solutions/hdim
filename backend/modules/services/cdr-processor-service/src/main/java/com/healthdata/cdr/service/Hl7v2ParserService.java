package com.healthdata.cdr.service;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v25.segment.MSH;
import ca.uhn.hl7v2.parser.Parser;
import com.healthdata.cdr.audit.CdrProcessorAuditIntegration;
import com.healthdata.cdr.dto.Hl7v2Message;
import com.healthdata.cdr.handler.AdtMessageHandler;
import com.healthdata.cdr.handler.BarMessageHandler;
import com.healthdata.cdr.handler.DftMessageHandler;
import com.healthdata.cdr.handler.MdmMessageHandler;
import com.healthdata.cdr.handler.OrmMessageHandler;
import com.healthdata.cdr.handler.OruMessageHandler;
import com.healthdata.cdr.handler.PprMessageHandler;
import com.healthdata.cdr.handler.RdeMessageHandler;
import com.healthdata.cdr.handler.RasMessageHandler;
import com.healthdata.cdr.handler.SiuMessageHandler;
import com.healthdata.cdr.handler.VxuMessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for parsing HL7 v2 messages.
 *
 * Supports:
 * - ADT messages (Admit/Discharge/Transfer)
 * - ORU messages (Lab Results)
 * - ORM messages (Lab Orders)
 * - RDE messages (Pharmacy/Treatment Encoded Order)
 * - RAS messages (Pharmacy/Treatment Administration)
 * - VXU messages (Vaccination Updates)
 * - MDM messages (Medical Document Management)
 * - SIU messages (Scheduling Information Unsolicited)
 * - BAR messages (Billing Account Record)
 * - DFT messages (Detailed Financial Transaction)
 * - PPR messages (Patient Problem)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Hl7v2ParserService {

    private final Parser hl7v2Parser;
    private final AdtMessageHandler adtMessageHandler;
    private final OruMessageHandler oruMessageHandler;
    private final OrmMessageHandler ormMessageHandler;
    private final RdeMessageHandler rdeMessageHandler;
    private final RasMessageHandler rasMessageHandler;
    private final VxuMessageHandler vxuMessageHandler;
    private final MdmMessageHandler mdmMessageHandler;
    private final SiuMessageHandler siuMessageHandler;
    private final BarMessageHandler barMessageHandler;
    private final DftMessageHandler dftMessageHandler;
    private final PprMessageHandler pprMessageHandler;
    private final CdrProcessorAuditIntegration cdrProcessorAuditIntegration;

    private static final DateTimeFormatter HL7_DATE_FORMAT =
        DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    /**
     * Parse an HL7 v2 message.
     *
     * @param rawMessage The raw HL7 message string
     * @param tenantId Tenant ID for multi-tenant support
     * @return Parsed Hl7v2Message
     */
    public Hl7v2Message parseMessage(String rawMessage, String tenantId) {
        log.debug("Parsing HL7 v2 message for tenant: {}", tenantId);

        long startTime = System.currentTimeMillis();
        try {
            // Parse the message
            Message message = hl7v2Parser.parse(rawMessage);

            // Extract MSH segment
            MSH msh = (MSH) message.get("MSH");

            // Build base message DTO
            Hl7v2Message hl7Message = Hl7v2Message.builder()
                .tenantId(tenantId)
                .rawMessage(rawMessage)
                .messageType(msh.getMessageType().getMessageCode().getValue())
                .triggerEvent(msh.getMessageType().getTriggerEvent().getValue())
                .messageCode(msh.getMessageType().getMessageCode().getValue() + "^" +
                    msh.getMessageType().getTriggerEvent().getValue())
                .messageControlId(msh.getMessageControlID().getValue())
                .sendingApplication(msh.getSendingApplication().getNamespaceID().getValue())
                .sendingFacility(msh.getSendingFacility().getNamespaceID().getValue())
                .receivingApplication(msh.getReceivingApplication().getNamespaceID().getValue())
                .receivingFacility(msh.getReceivingFacility().getNamespaceID().getValue())
                .version(msh.getVersionID().getVersionID().getValue())
                .messageDateTime(parseHl7DateTime(msh.getDateTimeOfMessage().getTime().getValue()))
                .processedAt(LocalDateTime.now())
                .status("PARSED")
                .build();

            // Route to appropriate handler based on message type
            Map<String, Object> parsedData = routeToHandler(message, hl7Message.getMessageType());
            hl7Message.setParsedData(parsedData);

            log.info("Successfully parsed HL7 message: type={}, controlId={}",
                hl7Message.getMessageCode(), hl7Message.getMessageControlId());

            // Publish audit event
            cdrProcessorAuditIntegration.publishHl7MessageIngestEvent(
                tenantId,
                hl7Message.getMessageCode(),
                hl7Message.getMessageControlId(),
                null, // Patient ID not easily extracted here
                rawMessage.split("\r").length, // Segment count
                true,
                null,
                System.currentTimeMillis() - startTime,
                "system"
            );

            return hl7Message;

        } catch (HL7Exception e) {
            log.error("Failed to parse HL7 message: {}", e.getMessage(), e);
            
            // Publish audit event for failure
            cdrProcessorAuditIntegration.publishHl7MessageIngestEvent(
                tenantId,
                "UNKNOWN",
                "UNKNOWN",
                null,
                rawMessage.split("\r").length,
                false,
                e.getMessage(),
                System.currentTimeMillis() - startTime,
                "system"
            );
            
            return Hl7v2Message.builder()
                .tenantId(tenantId)
                .rawMessage(rawMessage)
                .status("ERROR")
                .errorMessage("Parse error: " + e.getMessage())
                .processedAt(LocalDateTime.now())
                .build();
        }
    }

    /**
     * Route message to appropriate handler based on type.
     *
     * @param message Parsed HL7 message
     * @param messageType Message type
     * @return Parsed data map
     * @throws HL7Exception If handler processing fails
     */
    private Map<String, Object> routeToHandler(Message message, String messageType) throws HL7Exception {
        return switch (messageType) {
            case "ADT" -> adtMessageHandler.handle(message);
            case "ORU" -> oruMessageHandler.handle(message);
            case "ORM" -> ormMessageHandler.handle(message);
            case "RDE" -> rdeMessageHandler.handle(message);
            case "RAS" -> rasMessageHandler.handle(message);
            case "VXU" -> vxuMessageHandler.handle(message);
            case "MDM" -> mdmMessageHandler.handle(message);
            case "SIU" -> siuMessageHandler.handle(message);
            case "BAR" -> barMessageHandler.handle(message);
            case "DFT" -> dftMessageHandler.handle(message);
            case "PPR" -> pprMessageHandler.handle(message);
            default -> {
                log.warn("Unsupported message type: {}", messageType);
                Map<String, Object> data = new HashMap<>();
                data.put("warning", "Message type not fully supported: " + messageType);
                yield data;
            }
        };
    }

    /**
     * Parse HL7 date/time string to LocalDateTime.
     *
     * @param hl7DateTime HL7 formatted date/time
     * @return LocalDateTime or null if parsing fails
     */
    private LocalDateTime parseHl7DateTime(String hl7DateTime) {
        if (hl7DateTime == null || hl7DateTime.isEmpty()) {
            return LocalDateTime.now();
        }

        try {
            // HL7 format: YYYYMMDDHHMMSS
            // Handle partial dates
            String normalized = hl7DateTime;
            if (normalized.length() < 14) {
                normalized = String.format("%-14s", normalized).replace(' ', '0');
            }
            return LocalDateTime.parse(normalized.substring(0, 14), HL7_DATE_FORMAT);
        } catch (Exception e) {
            log.warn("Failed to parse HL7 date/time: {}", hl7DateTime);
            return LocalDateTime.now();
        }
    }

    /**
     * Validate HL7 message format.
     *
     * @param rawMessage Raw HL7 message
     * @return true if valid, false otherwise
     */
    public boolean validateMessage(String rawMessage) {
        if (rawMessage == null || rawMessage.isEmpty()) {
            return false;
        }

        // Check for MSH segment
        if (!rawMessage.startsWith("MSH")) {
            return false;
        }

        // Check for minimum segments
        String[] segments = rawMessage.split("\r");
        return segments.length >= 1;
    }
}
