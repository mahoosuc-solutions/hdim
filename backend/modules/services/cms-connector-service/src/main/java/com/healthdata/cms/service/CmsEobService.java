package com.healthdata.cms.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.healthdata.cms.client.DpcClient;
import com.healthdata.cms.model.CmsClaim;
import com.healthdata.cms.repository.CmsClaimRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.ExplanationOfBenefit;
import org.hl7.fhir.r4.model.Money;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

/**
 * CMS Explanation of Benefit Service
 *
 * Handles fetching, parsing, and persisting ExplanationOfBenefit (EOB) data
 * from CMS Data at Point of Care (DPC) API.
 *
 * EOB resources contain Medicare claims data including:
 * - Part A (Hospital)
 * - Part B (Physician)
 * - Part D (Prescription)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CmsEobService {

    private final DpcClient dpcClient;
    private final CmsClaimRepository claimRepository;
    private final FhirContext fhirContext;

    /**
     * Fetch and parse ExplanationOfBenefit resources for a patient
     *
     * @param patientId DPC patient/beneficiary ID
     * @param tenantId  Tenant ID for multi-tenancy
     * @return List of parsed CmsClaim entities
     */
    @Transactional(readOnly = true)
    public List<CmsClaim> fetchEobForPatient(String patientId, UUID tenantId) {
        log.info("Fetching EOB data for patient: {} (tenant: {})", patientId, tenantId);

        try {
            // Fetch EOB from DPC API
            String eobJson = dpcClient.getExplanationOfBenefits(patientId);

            // Parse FHIR Bundle
            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, eobJson);

            // Convert to CmsClaim entities
            List<CmsClaim> claims = parseEobBundle(bundle, patientId, tenantId);

            log.info("Parsed {} EOB records for patient: {}", claims.size(), patientId);
            return claims;

        } catch (Exception e) {
            log.error("Failed to fetch EOB for patient {}: {}", patientId, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch EOB data: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch EOB data with date filter
     *
     * @param patientId        DPC patient/beneficiary ID
     * @param tenantId         Tenant ID
     * @param lastUpdatedAfter Only fetch EOBs updated after this date (ISO-8601)
     * @return List of parsed CmsClaim entities
     */
    @Transactional(readOnly = true)
    public List<CmsClaim> fetchEobForPatient(String patientId, UUID tenantId, String lastUpdatedAfter) {
        log.info("Fetching EOB data for patient: {} (tenant: {}, since: {})",
                patientId, tenantId, lastUpdatedAfter);

        try {
            String eobJson = dpcClient.getExplanationOfBenefits(patientId, lastUpdatedAfter);

            IParser parser = fhirContext.newJsonParser();
            Bundle bundle = parser.parseResource(Bundle.class, eobJson);

            List<CmsClaim> claims = parseEobBundle(bundle, patientId, tenantId);

            log.info("Parsed {} EOB records for patient: {} (since: {})",
                    claims.size(), patientId, lastUpdatedAfter);
            return claims;

        } catch (Exception e) {
            log.error("Failed to fetch EOB for patient {} (since {}): {}",
                    patientId, lastUpdatedAfter, e.getMessage(), e);
            throw new RuntimeException("Failed to fetch EOB data: " + e.getMessage(), e);
        }
    }

    /**
     * Fetch and persist EOB data for a patient
     *
     * @param patientId DPC patient/beneficiary ID
     * @param tenantId  Tenant ID
     * @return Number of claims persisted
     */
    @Transactional
    public int syncEobForPatient(String patientId, UUID tenantId) {
        log.info("Syncing EOB data for patient: {} (tenant: {})", patientId, tenantId);

        List<CmsClaim> claims = fetchEobForPatient(patientId, tenantId);

        if (claims.isEmpty()) {
            log.info("No EOB records to sync for patient: {}", patientId);
            return 0;
        }

        // Persist claims
        List<CmsClaim> savedClaims = claimRepository.saveAll(claims);
        log.info("Synced {} EOB records for patient: {}", savedClaims.size(), patientId);

        return savedClaims.size();
    }

    /**
     * Parse a FHIR Bundle containing ExplanationOfBenefit resources
     */
    private List<CmsClaim> parseEobBundle(Bundle bundle, String patientId, UUID tenantId) {
        List<CmsClaim> claims = new ArrayList<>();

        if (bundle == null || bundle.getEntry() == null) {
            log.warn("Empty or null bundle received for patient: {}", patientId);
            return claims;
        }

        for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
            if (entry.getResource() instanceof ExplanationOfBenefit eob) {
                try {
                    CmsClaim claim = convertEobToClaim(eob, patientId, tenantId);
                    claims.add(claim);
                } catch (Exception e) {
                    log.warn("Failed to parse EOB entry for patient {}: {}", patientId, e.getMessage());
                }
            }
        }

        return claims;
    }

    /**
     * Convert a FHIR ExplanationOfBenefit resource to CmsClaim entity
     */
    private CmsClaim convertEobToClaim(ExplanationOfBenefit eob, String patientId, UUID tenantId) {
        CmsClaim claim = new CmsClaim();

        claim.setId(UUID.randomUUID());
        claim.setTenantId(tenantId);
        claim.setClaimId(eob.getIdElement().getIdPart());
        claim.setBeneficiaryId(patientId);
        claim.setDataSource("DPC");
        claim.setImportedAt(LocalDateTime.now());
        claim.setIsProcessed(false);
        claim.setHasValidationErrors(false);
        claim.setDeduplicationStatus("PENDING");

        // Extract financial totals
        if (eob.hasTotal()) {
            for (ExplanationOfBenefit.TotalComponent total : eob.getTotal()) {
                String category = total.getCategory().getCodingFirstRep().getCode();
                Money amount = total.getAmount();

                if ("submitted".equalsIgnoreCase(category) && amount != null) {
                    claim.setTotalChargeAmount(amount.getValue().doubleValue());
                } else if ("benefit".equalsIgnoreCase(category) && amount != null) {
                    claim.setTotalAllowedAmount(amount.getValue().doubleValue());
                }
            }
        }

        // Generate content hash for deduplication
        String contentHash = generateContentHash(eob);
        claim.setContentHash(contentHash);

        return claim;
    }

    /**
     * Generate SHA-256 hash of EOB content for deduplication
     */
    private String generateContentHash(ExplanationOfBenefit eob) {
        try {
            String content = fhirContext.newJsonParser().encodeResourceToString(eob);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            log.error("Failed to generate content hash: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get EOB summary for a patient
     *
     * @param patientId DPC patient/beneficiary ID
     * @return Summary of EOB data
     */
    public EobSummary getEobSummary(String patientId, UUID tenantId) {
        List<CmsClaim> claims = fetchEobForPatient(patientId, tenantId);

        double totalCharges = claims.stream()
                .filter(c -> c.getTotalChargeAmount() != null)
                .mapToDouble(CmsClaim::getTotalChargeAmount)
                .sum();

        double totalAllowed = claims.stream()
                .filter(c -> c.getTotalAllowedAmount() != null)
                .mapToDouble(CmsClaim::getTotalAllowedAmount)
                .sum();

        return EobSummary.builder()
                .patientId(patientId)
                .tenantId(tenantId)
                .totalClaims(claims.size())
                .totalChargeAmount(totalCharges)
                .totalAllowedAmount(totalAllowed)
                .lastRetrieved(LocalDateTime.now())
                .build();
    }

    /**
     * EOB Summary DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EobSummary {
        private String patientId;
        private UUID tenantId;
        private int totalClaims;
        private double totalChargeAmount;
        private double totalAllowedAmount;
        private LocalDateTime lastRetrieved;
    }
}
