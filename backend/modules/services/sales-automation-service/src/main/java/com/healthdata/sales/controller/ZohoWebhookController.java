package com.healthdata.sales.controller;

import com.healthdata.sales.config.ZohoConfig;
import com.healthdata.sales.service.ZohoSyncService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Controller for handling Zoho CRM webhooks
 * Receives notifications when records are created/updated/deleted in Zoho
 */
@RestController
@RequestMapping("/api/sales/zoho/webhook")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Zoho Webhooks",
    description = """
        APIs for Zoho CRM integration and synchronization.

        Handles bi-directional sync with Zoho CRM:
        - Webhook receiver for real-time notifications from Zoho
        - Manual sync trigger for full synchronization
        - Status endpoint for monitoring sync health

        Supported Zoho modules:
        - Leads
        - Accounts
        - Contacts
        - Deals (mapped to Opportunities)

        Webhook signature verification ensures data integrity.
        """
)
public class ZohoWebhookController {

    private final ZohoConfig zohoConfig;
    private final ZohoSyncService zohoSyncService;

    /**
     * Main webhook endpoint for Zoho notifications
     * Zoho sends POST requests when records are modified
     */
    @PostMapping
    @Operation(
        summary = "Receive Zoho webhook",
        description = """
            Handles incoming Zoho CRM notifications.

            Zoho sends webhooks when records are created, updated, or deleted.
            The payload includes module name, operation type, and affected records.

            Signature verification is performed if a webhook secret is configured.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Webhook processed successfully"),
        @ApiResponse(responseCode = "401", description = "Invalid webhook signature"),
        @ApiResponse(responseCode = "503", description = "Webhooks are disabled")
    })
    public ResponseEntity<Map<String, Object>> handleWebhook(
        @Parameter(description = "Zoho webhook signature for verification")
        @RequestHeader(value = "X-Zoho-Signature", required = false) String signature,
        @Parameter(description = "Webhook payload from Zoho", required = true)
        @RequestBody Map<String, Object> payload
    ) {
        log.info("Received Zoho webhook: {}", payload);

        // Verify webhook is enabled
        if (!zohoConfig.getWebhook().isEnabled()) {
            log.warn("Zoho webhook received but webhooks are disabled");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of("status", "error", "message", "Webhooks disabled"));
        }

        // Verify signature if secret is configured
        if (zohoConfig.getWebhook().getSecret() != null &&
            !zohoConfig.getWebhook().getSecret().isBlank()) {
            if (!verifySignature(signature, payload)) {
                log.warn("Invalid Zoho webhook signature");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("status", "error", "message", "Invalid signature"));
            }
        }

        try {
            processWebhook(payload);
            return ResponseEntity.ok(Map.of("status", "success"));
        } catch (Exception e) {
            log.error("Error processing Zoho webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("status", "error", "message", e.getMessage()));
        }
    }

    /**
     * Webhook verification endpoint for Zoho
     * Zoho may call this with GET to verify the endpoint
     */
    @GetMapping
    @Operation(
        summary = "Verify webhook endpoint",
        description = """
            Verification endpoint for Zoho webhook setup.

            Zoho calls this endpoint with a challenge parameter when configuring webhooks.
            The challenge must be echoed back to confirm endpoint ownership.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Endpoint verified, challenge echoed")
    })
    public ResponseEntity<Map<String, Object>> verifyEndpoint(
        @Parameter(description = "Challenge string from Zoho to echo back")
        @RequestParam(required = false) String challenge
    ) {
        log.info("Zoho webhook verification request received");
        Map<String, Object> response = new HashMap<>();
        response.put("status", "active");
        if (challenge != null) {
            response.put("challenge", challenge);
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Manual sync trigger endpoint
     */
    @PostMapping("/sync")
    @Operation(
        summary = "Trigger manual sync",
        description = """
            Manually triggers a full Zoho sync for the tenant.

            Syncs all supported modules (Leads, Accounts, Contacts, Deals)
            from Zoho to HDIM. Use when webhooks may have been missed
            or for initial data import.

            Returns sync statistics including records processed and any errors.
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sync completed, results returned"),
        @ApiResponse(responseCode = "503", description = "Sync is disabled")
    })
    @SecurityRequirement(name = "bearer-jwt")
    public ResponseEntity<ZohoSyncService.FullSyncResult> triggerSync(
        @Parameter(description = "Tenant identifier", required = true)
        @RequestHeader("X-Tenant-ID") UUID tenantId
    ) {
        log.info("Manual Zoho sync triggered for tenant {}", tenantId);
        ZohoSyncService.FullSyncResult result = zohoSyncService.fullSync(tenantId);
        return ResponseEntity.ok(result);
    }

    /**
     * Get sync status
     */
    @GetMapping("/status")
    @Operation(
        summary = "Get sync status",
        description = """
            Returns Zoho sync configuration status.

            Includes:
            - Sync enabled/disabled status
            - Webhook enabled/disabled status
            - Sync interval configuration
            - API base URL
            """
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sync status returned")
    })
    public ResponseEntity<Map<String, Object>> getSyncStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("syncEnabled", zohoConfig.getSync().isEnabled());
        status.put("webhookEnabled", zohoConfig.getWebhook().isEnabled());
        status.put("syncIntervalMinutes", zohoConfig.getSync().getIntervalMinutes());
        status.put("apiBaseUrl", zohoConfig.getApi().getBaseUrl());
        return ResponseEntity.ok(status);
    }

    // ==================== Private Helpers ====================

    private void processWebhook(Map<String, Object> payload) {
        String module = extractModule(payload);
        String operation = extractOperation(payload);
        List<Map<String, Object>> records = extractRecords(payload);

        log.info("Processing Zoho webhook: module={}, operation={}, recordCount={}",
            module, operation, records.size());

        // For now, just log the webhook - actual processing would trigger sync
        // In production, this would:
        // 1. Parse the webhook payload
        // 2. Identify the affected records
        // 3. Queue updates for processing
        // 4. Apply changes to HDIM database

        for (Map<String, Object> record : records) {
            String zohoId = (String) record.get("id");
            log.info("Webhook record: module={}, id={}, operation={}", module, zohoId, operation);

            // Check for HDIM ID to determine if this is a known record
            String hdimIdField = getHdimIdField(module);
            String hdimId = record.get(hdimIdField) != null ? record.get(hdimIdField).toString() : null;

            if (hdimId != null) {
                log.info("Known HDIM record: {} = {}", hdimIdField, hdimId);
                // Would trigger update to local record
            } else {
                log.info("New Zoho record without HDIM ID: {}", zohoId);
                // Would queue for review or auto-import
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String extractModule(Map<String, Object> payload) {
        // Zoho webhook format varies, try different structures
        if (payload.containsKey("module")) {
            Object module = payload.get("module");
            if (module instanceof Map) {
                return (String) ((Map<String, Object>) module).get("api_name");
            }
            return module.toString();
        }
        return "Unknown";
    }

    private String extractOperation(Map<String, Object> payload) {
        if (payload.containsKey("operation")) {
            return payload.get("operation").toString();
        }
        if (payload.containsKey("event")) {
            return payload.get("event").toString();
        }
        return "unknown";
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractRecords(Map<String, Object> payload) {
        if (payload.containsKey("data")) {
            Object data = payload.get("data");
            if (data instanceof List) {
                return (List<Map<String, Object>>) data;
            } else if (data instanceof Map) {
                return List.of((Map<String, Object>) data);
            }
        }
        if (payload.containsKey("ids")) {
            // Sometimes Zoho just sends IDs
            Object ids = payload.get("ids");
            if (ids instanceof List) {
                List<Map<String, Object>> records = new ArrayList<>();
                for (Object id : (List<?>) ids) {
                    records.add(Map.of("id", id.toString()));
                }
                return records;
            }
        }
        return List.of();
    }

    private String getHdimIdField(String module) {
        return switch (module) {
            case "Leads" -> "HDIM_Lead_ID";
            case "Accounts" -> "HDIM_Account_ID";
            case "Contacts" -> "HDIM_Contact_ID";
            case "Deals" -> "HDIM_Opportunity_ID";
            default -> "HDIM_ID";
        };
    }

    private boolean verifySignature(String signature, Map<String, Object> payload) {
        if (signature == null || signature.isBlank()) {
            return false;
        }

        try {
            String secret = zohoConfig.getWebhook().getSecret();
            String payloadString = payload.toString(); // Simplified - should use JSON serialization

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payloadString.getBytes(StandardCharsets.UTF_8));

            String computedSignature = Base64.getEncoder().encodeToString(hash);
            return signature.equals(computedSignature);
        } catch (Exception e) {
            log.error("Error verifying webhook signature: {}", e.getMessage());
            return false;
        }
    }
}
