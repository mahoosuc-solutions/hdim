package com.healthdata.hcc.service;

import com.healthdata.hcc.persistence.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Documentation Gap Detection Service
 *
 * Analyzes patient diagnosis codes to identify documentation improvement opportunities
 * that could increase RAF scores. Focuses on:
 *
 * 1. Specificity Gaps - Non-specific codes that could be coded more specifically
 * 2. Laterality Gaps - Codes missing required laterality
 * 3. Severity Gaps - Conditions that may warrant higher severity coding
 * 4. Missing HCC Gaps - Conditions documented but not coded to HCC level
 * 5. V28 Transition Gaps - Codes that changed from V24 to V28
 *
 * CMS Documentation Requirements:
 * - MEAT criteria: Monitor, Evaluate, Assess/Address, Treat
 * - Annual recapture of chronic conditions
 * - Specificity to highest level supported by documentation
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentationGapService {

    private final DiagnosisHccMapRepository diagnosisMapRepository;
    private final DocumentationGapRepository gapRepository;
    private final PatientHccProfileRepository profileRepository;

    /**
     * Analyze patient diagnoses and identify documentation gaps.
     *
     * @param tenantId Tenant identifier
     * @param patientId Patient identifier
     * @param currentDiagnoses Current ICD-10 codes for the patient
     * @param profileYear Year for analysis
     * @return List of identified documentation gaps
     */
    @Transactional
    public List<DocumentationGapEntity> analyzeDocumentationGaps(
            String tenantId,
            UUID patientId,
            List<String> currentDiagnoses,
            int profileYear) {

        log.info("Analyzing documentation gaps for patient {} with {} diagnoses",
            patientId, currentDiagnoses.size());

        List<DocumentationGapEntity> gaps = new ArrayList<>();

        // Get crosswalk data for all diagnoses
        List<DiagnosisHccMapEntity> mappings = diagnosisMapRepository.findByIcd10Codes(currentDiagnoses);
        Map<String, DiagnosisHccMapEntity> mappingByCode = mappings.stream()
            .collect(Collectors.toMap(DiagnosisHccMapEntity::getIcd10Code, m -> m, (a, b) -> a));

        for (String diagnosis : currentDiagnoses) {
            DiagnosisHccMapEntity mapping = mappingByCode.get(diagnosis);

            // Check for specificity gaps
            if (mapping != null && Boolean.TRUE.equals(mapping.getRequiresSpecificity())) {
                gaps.add(createSpecificityGap(tenantId, patientId, profileYear, diagnosis, mapping));
            }

            // Check for V28 transition impacts
            if (mapping != null && Boolean.TRUE.equals(mapping.getChangedInV28())) {
                gaps.add(createV28TransitionGap(tenantId, patientId, profileYear, diagnosis, mapping));
            }

            // Check for unspecified codes that should be more specific
            if (isUnspecifiedCode(diagnosis)) {
                gaps.addAll(findSpecificityOpportunities(tenantId, patientId, profileYear, diagnosis, mapping));
            }
        }

        // Check for missing HCC conditions
        gaps.addAll(findMissingHccOpportunities(tenantId, patientId, profileYear, currentDiagnoses, mappings));

        // Save gaps and update profile
        List<DocumentationGapEntity> savedGaps = gapRepository.saveAll(
            gaps.stream().filter(Objects::nonNull).toList());

        // Update patient profile with gap summary
        updateProfileWithGaps(tenantId, patientId, profileYear, savedGaps);

        log.info("Identified {} documentation gaps for patient {}", savedGaps.size(), patientId);

        return savedGaps;
    }

    /**
     * Get all open documentation gaps for a patient.
     */
    public List<DocumentationGapEntity> getOpenGaps(String tenantId, UUID patientId, int profileYear) {
        return gapRepository.findByTenantIdAndPatientIdAndProfileYear(tenantId, patientId, profileYear)
            .stream()
            .filter(g -> "OPEN".equals(g.getStatus()) || "PENDING_REVIEW".equals(g.getStatus()))
            .toList();
    }

    /**
     * Get high-value gaps across population.
     */
    public List<DocumentationGapEntity> getHighValueGaps(String tenantId, int profileYear, BigDecimal minRafImpact) {
        return gapRepository.findHighValueGaps(tenantId, profileYear, minRafImpact);
    }

    /**
     * Mark a gap as addressed.
     */
    @Transactional
    public DocumentationGapEntity addressGap(UUID gapId, String addressedBy, String newIcd10Code) {
        DocumentationGapEntity gap = gapRepository.findById(gapId)
            .orElseThrow(() -> new IllegalArgumentException("Gap not found: " + gapId));

        gap.setStatus(DocumentationGapEntity.GapStatus.ADDRESSED);
        gap.setAddressedBy(addressedBy);
        gap.setAddressedAt(LocalDateTime.now());

        return gapRepository.save(gap);
    }

    /**
     * Dismiss a gap (not applicable).
     */
    @Transactional
    public DocumentationGapEntity dismissGap(UUID gapId, String dismissedBy, String reason) {
        DocumentationGapEntity gap = gapRepository.findById(gapId)
            .orElseThrow(() -> new IllegalArgumentException("Gap not found: " + gapId));

        gap.setStatus(DocumentationGapEntity.GapStatus.REJECTED);
        gap.setAddressedBy(dismissedBy);
        gap.setAddressedAt(LocalDateTime.now());
        gap.setClinicalGuidance("Dismissed: " + reason);

        return gapRepository.save(gap);
    }

    private DocumentationGapEntity createSpecificityGap(
            String tenantId, UUID patientId, int profileYear,
            String diagnosis, DiagnosisHccMapEntity mapping) {

        return DocumentationGapEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .profileYear(profileYear)
            .currentIcd10(diagnosis)
            .currentIcd10Description(mapping.getIcd10Description())
            .currentHccV24(mapping.getHccCodeV24())
            .currentHccV28(mapping.getHccCodeV28())
            .gapType(DocumentationGapEntity.GapType.UNSPECIFIED)
            .rafImpactV24(estimateSpecificityImpact(mapping, "V24"))
            .rafImpactV28(estimateSpecificityImpact(mapping, "V28"))
            .rafImpactBlended(estimateSpecificityImpact(mapping, "BLENDED"))
            .priority(calculatePriority(mapping))
            .clinicalGuidance(mapping.getSpecificityGuidance())
            .requiredDocumentation(generateRequiredDocumentation(mapping))
            .status(DocumentationGapEntity.GapStatus.OPEN)
            .build();
    }

    private DocumentationGapEntity createV28TransitionGap(
            String tenantId, UUID patientId, int profileYear,
            String diagnosis, DiagnosisHccMapEntity mapping) {

        // Only create if there's a significant change
        if (mapping.getHccCodeV24() == null && mapping.getHccCodeV28() != null) {
            // Code now maps to HCC in V28 - positive impact
            return null;
        }

        if (mapping.getHccCodeV24() != null && mapping.getHccCodeV28() == null) {
            // Code no longer maps to HCC in V28 - need alternative
            return DocumentationGapEntity.builder()
                .tenantId(tenantId)
                .patientId(patientId)
                .profileYear(profileYear)
                .currentIcd10(diagnosis)
                .currentIcd10Description(mapping.getIcd10Description())
                .currentHccV24(mapping.getHccCodeV24())
                .currentHccV28(null)
                .gapType(DocumentationGapEntity.GapType.V28_SPECIFICITY)
                .rafImpactV24(BigDecimal.ZERO)
                .rafImpactV28(mapping.getCoefficientV24() != null ?
                    BigDecimal.valueOf(-mapping.getCoefficientV24()) : BigDecimal.ZERO)
                .rafImpactBlended(calculateBlendedImpact(BigDecimal.ZERO,
                    mapping.getCoefficientV24() != null ?
                        BigDecimal.valueOf(-mapping.getCoefficientV24()) : BigDecimal.ZERO))
                .priority("HIGH")
                .clinicalGuidance(mapping.getV28ChangeDescription())
                .requiredDocumentation("Review documentation to identify alternative HCC-eligible code for V28")
                .status(DocumentationGapEntity.GapStatus.OPEN)
                .build();
        }

        return null;
    }

    private List<DocumentationGapEntity> findSpecificityOpportunities(
            String tenantId, UUID patientId, int profileYear,
            String diagnosis, DiagnosisHccMapEntity mapping) {

        List<DocumentationGapEntity> gaps = new ArrayList<>();

        // Check for unspecified diabetes
        if (diagnosis.startsWith("E11.9")) {
            gaps.add(createGap(tenantId, patientId, profileYear, diagnosis, mapping,
                DocumentationGapEntity.GapType.UNSPECIFIED,
                "Consider documenting specific complications (nephropathy, retinopathy, neuropathy)",
                "Document presence/absence of: chronic kidney disease, retinopathy, neuropathy, peripheral vascular disease"));
        }

        // Check for unspecified heart failure
        if (diagnosis.startsWith("I50.9")) {
            gaps.add(createGap(tenantId, patientId, profileYear, diagnosis, mapping,
                DocumentationGapEntity.GapType.UNSPECIFIED,
                "Specify heart failure type (systolic, diastolic, combined) and stage",
                "Document: ejection fraction, systolic/diastolic dysfunction, NYHA class"));
        }

        // Check for unspecified COPD
        if (diagnosis.startsWith("J44.9")) {
            gaps.add(createGap(tenantId, patientId, profileYear, diagnosis, mapping,
                DocumentationGapEntity.GapType.UNSPECIFIED,
                "Document COPD severity and any exacerbations",
                "Document: FEV1, GOLD stage, exacerbation history, oxygen requirements"));
        }

        // Check for unspecified CKD
        if (diagnosis.startsWith("N18.9")) {
            gaps.add(createGap(tenantId, patientId, profileYear, diagnosis, mapping,
                DocumentationGapEntity.GapType.UNSPECIFIED,
                "Specify CKD stage based on eGFR",
                "Document: most recent eGFR value, CKD stage (1-5), dialysis status if applicable"));
        }

        return gaps;
    }

    private List<DocumentationGapEntity> findMissingHccOpportunities(
            String tenantId, UUID patientId, int profileYear,
            List<String> currentDiagnoses, List<DiagnosisHccMapEntity> mappings) {

        List<DocumentationGapEntity> gaps = new ArrayList<>();

        // Check for common conditions that should have HCC codes
        Set<String> diagnosisPrefixes = currentDiagnoses.stream()
            .map(d -> d.length() >= 3 ? d.substring(0, 3) : d)
            .collect(Collectors.toSet());

        // Check for diabetes without complications coded
        if (diagnosisPrefixes.contains("E11") || diagnosisPrefixes.contains("E10")) {
            boolean hasComplication = currentDiagnoses.stream()
                .anyMatch(d -> d.matches("E1[01]\\.[1-6].*"));
            if (!hasComplication) {
                gaps.add(createGap(tenantId, patientId, profileYear, null, null,
                    DocumentationGapEntity.GapType.MISSING_COMPLICATION,
                    "Patient has diabetes - review for documentable complications",
                    "Screen for: nephropathy (N18.*), retinopathy, neuropathy, peripheral vascular disease"));
            }
        }

        // Check for CHF without underlying cause coded
        if (diagnosisPrefixes.contains("I50")) {
            boolean hasEtiology = currentDiagnoses.stream()
                .anyMatch(d -> d.startsWith("I25") || d.startsWith("I11") || d.startsWith("I42"));
            if (!hasEtiology) {
                gaps.add(createGap(tenantId, patientId, profileYear, null, null,
                    DocumentationGapEntity.GapType.MISSING_TYPE,
                    "Heart failure documented - consider coding underlying cause",
                    "Review for: ischemic cardiomyopathy (I25.*), hypertensive heart disease (I11.*), other cardiomyopathy (I42.*)"));
            }
        }

        return gaps;
    }

    private DocumentationGapEntity createGap(
            String tenantId, UUID patientId, int profileYear,
            String diagnosis, DiagnosisHccMapEntity mapping,
            DocumentationGapEntity.GapType gapType, String guidance, String requiredDoc) {

        return DocumentationGapEntity.builder()
            .tenantId(tenantId)
            .patientId(patientId)
            .profileYear(profileYear)
            .currentIcd10(diagnosis)
            .currentIcd10Description(mapping != null ? mapping.getIcd10Description() : null)
            .currentHccV24(mapping != null ? mapping.getHccCodeV24() : null)
            .currentHccV28(mapping != null ? mapping.getHccCodeV28() : null)
            .gapType(gapType)
            .rafImpactV24(BigDecimal.valueOf(0.15))  // Estimated impact
            .rafImpactV28(BigDecimal.valueOf(0.12))
            .rafImpactBlended(calculateBlendedImpact(BigDecimal.valueOf(0.15), BigDecimal.valueOf(0.12)))
            .priority("MEDIUM")
            .clinicalGuidance(guidance)
            .requiredDocumentation(requiredDoc)
            .status(DocumentationGapEntity.GapStatus.OPEN)
            .build();
    }

    private boolean isUnspecifiedCode(String diagnosis) {
        // Common unspecified code patterns
        return diagnosis.endsWith(".9") ||
               diagnosis.endsWith(".90") ||
               diagnosis.contains("unspecified");
    }

    private BigDecimal estimateSpecificityImpact(DiagnosisHccMapEntity mapping, String model) {
        // Estimated additional RAF from coding more specifically
        // In production, calculate based on actual coefficient differences
        return BigDecimal.valueOf(0.08);
    }

    private BigDecimal calculateBlendedImpact(BigDecimal v24Impact, BigDecimal v28Impact) {
        // 2025 weighting: 33% V24, 67% V28
        return v24Impact.multiply(BigDecimal.valueOf(0.33))
            .add(v28Impact.multiply(BigDecimal.valueOf(0.67)));
    }

    private String calculatePriority(DiagnosisHccMapEntity mapping) {
        if (mapping.getCoefficientV28() != null && mapping.getCoefficientV28() > 0.3) {
            return "HIGH";
        } else if (mapping.getCoefficientV28() != null && mapping.getCoefficientV28() > 0.1) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String generateRequiredDocumentation(DiagnosisHccMapEntity mapping) {
        StringBuilder doc = new StringBuilder();
        doc.append("To code more specifically, document:\n");
        doc.append("- Current clinical status and any changes\n");
        doc.append("- Treatment plan and response to treatment\n");
        doc.append("- Any complications or manifestations\n");

        if (mapping.getSpecificityGuidance() != null) {
            doc.append("\nSpecific requirements:\n");
            doc.append(mapping.getSpecificityGuidance());
        }

        return doc.toString();
    }

    private void updateProfileWithGaps(String tenantId, UUID patientId, int profileYear,
            List<DocumentationGapEntity> gaps) {

        profileRepository.findByTenantIdAndPatientIdAndProfileYear(tenantId, patientId, profileYear)
            .ifPresent(profile -> {
                profile.setDocumentationGapCount(gaps.size());

                BigDecimal totalUplift = gaps.stream()
                    .map(DocumentationGapEntity::getRafImpactBlended)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                profile.setPotentialRafUplift(totalUplift);

                profileRepository.save(profile);
            });
    }
}
