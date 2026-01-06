package com.healthdata.quality.service;

import com.healthdata.quality.dto.BulkSignRequest;
import com.healthdata.quality.dto.BulkSignResponse;
import com.healthdata.quality.persistence.QualityMeasureResultEntity;
import com.healthdata.quality.persistence.QualityMeasureResultRepository;
import com.healthdata.quality.persistence.ResultSignatureEntity;
import com.healthdata.quality.persistence.ResultSignatureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for signing quality measure results.
 * Handles bulk signing with safety checks for abnormal results.
 *
 * HIPAA Compliance:
 * - All signing operations are audited via @Audited annotation in controller
 * - Immutable audit trail created via ResultSignatureEntity
 * - No caching of signing operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ResultSigningService {

    private final QualityMeasureResultRepository resultRepository;
    private final ResultSignatureRepository signatureRepository;

    /**
     * Bulk sign multiple quality measure results.
     *
     * Safety checks:
     * - Normal results can be signed in bulk without individual acknowledgment
     * - Abnormal results (non-compliant with high severity) require explicit acknowledgment
     * - Each signed result gets an immutable signature record for audit trail
     *
     * @param tenantId Tenant ID
     * @param request Bulk sign request
     * @param userId ID of the signing provider
     * @param username Username of the signing provider
     * @return BulkSignResponse with counts and pending abnormals
     */
    @Transactional
    public BulkSignResponse bulkSignResults(String tenantId, BulkSignRequest request,
                                            String userId, String username) {
        log.info("Bulk signing {} results for tenant {} by user {}",
                 request.getResultIds().size(), tenantId, userId);

        LocalDateTime signedAt = LocalDateTime.now();
        List<BulkSignResponse.PendingAbnormalResult> pendingAbnormal = new ArrayList<>();
        List<BulkSignResponse.FailedResult> failedResults = new ArrayList<>();
        int signedCount = 0;

        // Build map of acknowledgments for quick lookup
        Map<UUID, BulkSignRequest.AbnormalAcknowledgment> acknowledgmentMap =
            request.getAcknowledgments() != null
                ? request.getAcknowledgments().stream()
                    .collect(Collectors.toMap(
                        BulkSignRequest.AbnormalAcknowledgment::getResultId,
                        a -> a))
                : Collections.emptyMap();

        // Fetch all results at once for efficiency
        List<QualityMeasureResultEntity> results = resultRepository.findAllByIdInAndTenantId(
            request.getResultIds(), tenantId);

        // Track found IDs for missing check
        Set<UUID> foundIds = results.stream()
            .map(QualityMeasureResultEntity::getId)
            .collect(Collectors.toSet());

        // Check for missing results
        for (UUID requestedId : request.getResultIds()) {
            if (!foundIds.contains(requestedId)) {
                failedResults.add(BulkSignResponse.FailedResult.builder()
                    .resultId(requestedId)
                    .reason("Result not found or not accessible")
                    .build());
            }
        }

        // Process each result
        for (QualityMeasureResultEntity result : results) {
            try {
                // Check if already signed
                if (isAlreadySigned(result.getId())) {
                    failedResults.add(BulkSignResponse.FailedResult.builder()
                        .resultId(result.getId())
                        .reason("Result already signed")
                        .build());
                    continue;
                }

                // Check if abnormal and requires acknowledgment
                if (isAbnormalResult(result)) {
                    BulkSignRequest.AbnormalAcknowledgment ack =
                        acknowledgmentMap.get(result.getId());

                    if (ack == null || !ack.isAcknowledged()) {
                        // Add to pending abnormal list
                        pendingAbnormal.add(BulkSignResponse.PendingAbnormalResult.builder()
                            .resultId(result.getId())
                            .patientName(result.getPatientId().toString()) // Would need patient lookup
                            .resultType(result.getMeasureCategory())
                            .value(result.getNumeratorCompliant() ? "Compliant" : "Non-Compliant")
                            .measureName(result.getMeasureName())
                            .abnormalReason(getAbnormalReason(result))
                            .build());
                        continue;
                    }

                    // Sign with acknowledgment notes
                    signResult(result, userId, username, signedAt,
                              request.getSignatureType(), ack.getNotes());
                    signedCount++;
                } else {
                    // Normal result - can sign without acknowledgment
                    signResult(result, userId, username, signedAt,
                              request.getSignatureType(), request.getNotes());
                    signedCount++;
                }
            } catch (Exception e) {
                log.error("Failed to sign result {}: {}", result.getId(), e.getMessage());
                failedResults.add(BulkSignResponse.FailedResult.builder()
                    .resultId(result.getId())
                    .reason("Signing failed: " + e.getMessage())
                    .build());
            }
        }

        log.info("Bulk sign completed: {} signed, {} pending, {} failed",
                 signedCount, pendingAbnormal.size(), failedResults.size());

        return BulkSignResponse.builder()
            .signed(signedCount)
            .requiresAcknowledgment(pendingAbnormal.size())
            .failed(failedResults.size())
            .signedAt(signedAt)
            .signedBy(username)
            .pendingAbnormal(pendingAbnormal)
            .failedResults(failedResults)
            .build();
    }

    /**
     * Check if a result is already signed
     */
    private boolean isAlreadySigned(UUID resultId) {
        return signatureRepository.existsByResultId(resultId);
    }

    /**
     * Determine if a result is abnormal and requires individual acknowledgment
     */
    private boolean isAbnormalResult(QualityMeasureResultEntity result) {
        // Non-compliant results are considered abnormal
        if (result.getNumeratorCompliant() == null || !result.getNumeratorCompliant()) {
            // Additional severity check - if compliance rate is very low
            if (result.getComplianceRate() != null && result.getComplianceRate() < 50.0) {
                return true;
            }
            // Check score for abnormality
            if (result.getScore() != null && result.getScore() < 0.5) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get reason why result is flagged as abnormal
     */
    private String getAbnormalReason(QualityMeasureResultEntity result) {
        if (!Boolean.TRUE.equals(result.getNumeratorCompliant())) {
            if (result.getComplianceRate() != null && result.getComplianceRate() < 50.0) {
                return "Low compliance rate: " + result.getComplianceRate() + "%";
            }
            return "Non-compliant with measure criteria";
        }
        return "Requires review";
    }

    /**
     * Sign a result and create audit trail
     */
    private void signResult(QualityMeasureResultEntity result, String userId,
                           String username, LocalDateTime signedAt,
                           BulkSignRequest.SignatureType signatureType, String notes) {
        // Create immutable signature record
        ResultSignatureEntity signature = ResultSignatureEntity.builder()
            .id(UUID.randomUUID())
            .resultId(result.getId())
            .tenantId(result.getTenantId())
            .patientId(result.getPatientId())
            .measureId(result.getMeasureId())
            .signedBy(userId)
            .signedByUsername(username)
            .signedAt(signedAt)
            .signatureType(signatureType.name())
            .notes(notes)
            .numeratorCompliant(result.getNumeratorCompliant())
            .complianceRate(result.getComplianceRate())
            .build();

        signatureRepository.save(signature);

        log.debug("Result {} signed by {} at {}", result.getId(), username, signedAt);
    }

    /**
     * Get signature history for a result
     */
    @Transactional(readOnly = true)
    public Optional<ResultSignatureEntity> getSignature(UUID resultId, String tenantId) {
        return signatureRepository.findByResultIdAndTenantId(resultId, tenantId);
    }

    /**
     * Get all signatures by a user within a date range
     */
    @Transactional(readOnly = true)
    public List<ResultSignatureEntity> getSignaturesByUser(String tenantId, String userId,
                                                          LocalDateTime from, LocalDateTime to) {
        return signatureRepository.findByTenantIdAndSignedByAndSignedAtBetween(
            tenantId, userId, from, to);
    }
}
