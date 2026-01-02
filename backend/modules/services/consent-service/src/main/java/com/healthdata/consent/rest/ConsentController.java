package com.healthdata.consent.rest;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.healthdata.consent.persistence.ConsentEntity;
import com.healthdata.consent.service.ConsentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/consents")
@RequiredArgsConstructor
public class ConsentController {

    private final ConsentService consentService;

    /**
     * Create a new consent
     * POST /api/consents
     */
    @PostMapping
    public ResponseEntity<ConsentEntity> createConsent(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @RequestBody ConsentEntity consent) {
        try {
            ConsentEntity created = consentService.createConsent(tenantId, consent, userId);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Get consent by ID
     * GET /api/consents/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ConsentEntity> getConsent(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {
        return consentService.getConsent(tenantId, id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Update consent
     * PUT /api/consents/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ConsentEntity> updateConsent(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id,
            @RequestBody ConsentEntity consent) {
        try {
            ConsentEntity updated = consentService.updateConsent(tenantId, id, consent, userId);
            return ResponseEntity.ok(updated);
        } catch (ConsentService.ConsentNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Delete consent
     * DELETE /api/consents/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteConsent(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id) {
        try {
            consentService.deleteConsent(tenantId, id, userId);
            return ResponseEntity.noContent().build();
        } catch (ConsentService.ConsentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Revoke consent
     * POST /api/consents/{id}/revoke
     */
    @PostMapping("/{id}/revoke")
    public ResponseEntity<ConsentEntity> revokeConsent(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestHeader(value = "X-User-ID", required = false, defaultValue = "system") String userId,
            @PathVariable UUID id,
            @RequestParam(value = "reason", required = false) String reason) {
        try {
            ConsentEntity revoked = consentService.revokeConsent(tenantId, id, reason, userId);
            return ResponseEntity.ok(revoked);
        } catch (ConsentService.ConsentNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get all consents for a patient
     * GET /api/consents/patient/{patientId}
     */
    @GetMapping("/patient/{patientId}")
    public ResponseEntity<List<ConsentEntity>> getConsentsByPatient(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {
        List<ConsentEntity> consents = consentService.getConsentsByPatient(tenantId, patientId);
        return ResponseEntity.ok(consents);
    }

    /**
     * Get consents for a patient with pagination
     * GET /api/consents/patient/{patientId}/page
     */
    @GetMapping("/patient/{patientId}/page")
    public ResponseEntity<Page<ConsentEntity>> getConsentsByPatientPaginated(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ConsentEntity> consents = consentService.getConsentsByPatient(tenantId, patientId, pageable);
        return ResponseEntity.ok(consents);
    }

    /**
     * Get active consents for a patient
     * GET /api/consents/patient/{patientId}/active
     */
    @GetMapping("/patient/{patientId}/active")
    public ResponseEntity<List<ConsentEntity>> getActiveConsentsByPatient(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {
        List<ConsentEntity> consents = consentService.getActiveConsentsByPatient(tenantId, patientId);
        return ResponseEntity.ok(consents);
    }

    /**
     * Get active consents for a patient and scope
     * GET /api/consents/patient/{patientId}/active/scope/{scope}
     */
    @GetMapping("/patient/{patientId}/active/scope/{scope}")
    public ResponseEntity<List<ConsentEntity>> getActiveConsentsByPatientAndScope(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @PathVariable String scope) {
        List<ConsentEntity> consents = consentService.getActiveConsentsByPatientAndScope(tenantId, patientId, scope);
        return ResponseEntity.ok(consents);
    }

    /**
     * Get active consents for a patient and category
     * GET /api/consents/patient/{patientId}/active/category/{category}
     */
    @GetMapping("/patient/{patientId}/active/category/{category}")
    public ResponseEntity<List<ConsentEntity>> getActiveConsentsByPatientAndCategory(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @PathVariable String category) {
        List<ConsentEntity> consents = consentService.getActiveConsentsByPatientAndCategory(tenantId, patientId, category);
        return ResponseEntity.ok(consents);
    }

    /**
     * Get active consents for a patient and data class
     * GET /api/consents/patient/{patientId}/active/data-class/{dataClass}
     */
    @GetMapping("/patient/{patientId}/active/data-class/{dataClass}")
    public ResponseEntity<List<ConsentEntity>> getActiveConsentsByPatientAndDataClass(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @PathVariable String dataClass) {
        List<ConsentEntity> consents = consentService.getActiveConsentsByPatientAndDataClass(tenantId, patientId, dataClass);
        return ResponseEntity.ok(consents);
    }

    /**
     * Get revoked consents for a patient
     * GET /api/consents/patient/{patientId}/revoked
     */
    @GetMapping("/patient/{patientId}/revoked")
    public ResponseEntity<List<ConsentEntity>> getRevokedConsentsByPatient(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {
        List<ConsentEntity> consents = consentService.getRevokedConsentsByPatient(tenantId, patientId);
        return ResponseEntity.ok(consents);
    }

    /**
     * Get expired consents for a patient
     * GET /api/consents/patient/{patientId}/expired
     */
    @GetMapping("/patient/{patientId}/expired")
    public ResponseEntity<List<ConsentEntity>> getExpiredConsentsByPatient(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId) {
        List<ConsentEntity> consents = consentService.getExpiredConsentsByPatient(tenantId, patientId);
        return ResponseEntity.ok(consents);
    }

    /**
     * Get consents expiring soon
     * GET /api/consents/patient/{patientId}/expiring-soon?days={days}
     */
    @GetMapping("/patient/{patientId}/expiring-soon")
    public ResponseEntity<List<ConsentEntity>> getConsentsExpiringSoon(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @RequestParam(value = "days", defaultValue = "30") int days) {
        List<ConsentEntity> consents = consentService.getConsentsExpiringSoon(tenantId, patientId, days);
        return ResponseEntity.ok(consents);
    }

    /**
     * Check if patient has active consent for scope
     * GET /api/consents/patient/{patientId}/check/scope/{scope}
     */
    @GetMapping("/patient/{patientId}/check/scope/{scope}")
    public ResponseEntity<ConsentCheckResponse> checkConsentForScope(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @PathVariable String scope) {
        boolean hasConsent = consentService.hasActiveConsentForScope(tenantId, patientId, scope);
        return ResponseEntity.ok(new ConsentCheckResponse(hasConsent));
    }

    /**
     * Check if patient has active consent for category
     * GET /api/consents/patient/{patientId}/check/category/{category}
     */
    @GetMapping("/patient/{patientId}/check/category/{category}")
    public ResponseEntity<ConsentCheckResponse> checkConsentForCategory(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @PathVariable String category) {
        boolean hasConsent = consentService.hasActiveConsentForCategory(tenantId, patientId, category);
        return ResponseEntity.ok(new ConsentCheckResponse(hasConsent));
    }

    /**
     * Check if patient has active consent for data class
     * GET /api/consents/patient/{patientId}/check/data-class/{dataClass}
     */
    @GetMapping("/patient/{patientId}/check/data-class/{dataClass}")
    public ResponseEntity<ConsentCheckResponse> checkConsentForDataClass(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @PathVariable String dataClass) {
        boolean hasConsent = consentService.hasActiveConsentForDataClass(tenantId, patientId, dataClass);
        return ResponseEntity.ok(new ConsentCheckResponse(hasConsent));
    }

    /**
     * Check if authorized party has consent to access patient data
     * GET /api/consents/patient/{patientId}/check/authorized-party/{authorizedPartyId}
     */
    @GetMapping("/patient/{patientId}/check/authorized-party/{authorizedPartyId}")
    public ResponseEntity<ConsentCheckResponse> checkConsentForAuthorizedParty(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID patientId,
            @PathVariable String authorizedPartyId) {
        boolean hasConsent = consentService.hasActiveConsentForAuthorizedParty(tenantId, patientId, authorizedPartyId);
        return ResponseEntity.ok(new ConsentCheckResponse(hasConsent));
    }

    /**
     * Validate data access request
     * POST /api/consents/validate-access
     */
    @PostMapping("/validate-access")
    public ResponseEntity<ConsentValidationResponse> validateDataAccess(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody DataAccessRequest request) {
        ConsentService.ConsentValidationResult result = consentService.validateDataAccess(
                tenantId,
                request.getPatientId(),
                request.getScope(),
                request.getCategory(),
                request.getDataClass(),
                request.getAuthorizedPartyId()
        );

        ConsentValidationResponse response = new ConsentValidationResponse(
                result.isPermitted(),
                result.getReason()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     * GET /api/consents/_health
     */
    @GetMapping("/_health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("{\"status\": \"UP\", \"service\": \"Consent\"}");
    }

    // DTOs

    public static class ConsentCheckResponse {
        private final boolean hasConsent;

        public ConsentCheckResponse(boolean hasConsent) {
            this.hasConsent = hasConsent;
        }

        public boolean isHasConsent() {
            return hasConsent;
        }
    }

    public static class ConsentValidationResponse {
        private final boolean permitted;
        private final String reason;

        public ConsentValidationResponse(boolean permitted, String reason) {
            this.permitted = permitted;
            this.reason = reason;
        }

        public boolean isPermitted() {
            return permitted;
        }

        public String getReason() {
            return reason;
        }
    }

    public static class DataAccessRequest {
        private UUID patientId;
        private String scope;
        private String category;
        private String dataClass;
        private String authorizedPartyId;

        public UUID getPatientId() {
            return patientId;
        }

        public void setPatientId(UUID patientId) {
            this.patientId = patientId;
        }

        public String getScope() {
            return scope;
        }

        public void setScope(String scope) {
            this.scope = scope;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getDataClass() {
            return dataClass;
        }

        public void setDataClass(String dataClass) {
            this.dataClass = dataClass;
        }

        public String getAuthorizedPartyId() {
            return authorizedPartyId;
        }

        public void setAuthorizedPartyId(String authorizedPartyId) {
            this.authorizedPartyId = authorizedPartyId;
        }
    }
}
