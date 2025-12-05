package com.healthdata.sdoh.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthdata.sdoh.entity.SdohDiagnosisEntity;
import com.healthdata.sdoh.model.SdohCategory;
import com.healthdata.sdoh.model.SdohDiagnosis;
import com.healthdata.sdoh.repository.SdohDiagnosisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ICD-10-CM Z-code (Z55-Z65) mapping service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ZCodeMapper {

    private final SdohDiagnosisRepository diagnosisRepository;
    private final ObjectMapper objectMapper;

    private static final Map<SdohCategory, List<String>> CATEGORY_TO_ZCODES = new HashMap<>();

    static {
        CATEGORY_TO_ZCODES.put(SdohCategory.FOOD_INSECURITY, Arrays.asList("Z59.4", "Z59.41", "Z59.48"));
        CATEGORY_TO_ZCODES.put(SdohCategory.HOUSING_INSTABILITY, Arrays.asList("Z59.0", "Z59.00", "Z59.01", "Z59.02"));
        CATEGORY_TO_ZCODES.put(SdohCategory.TRANSPORTATION, Arrays.asList("Z59.82"));
        CATEGORY_TO_ZCODES.put(SdohCategory.FINANCIAL_STRAIN, Arrays.asList("Z59.5", "Z59.6", "Z59.7"));
        CATEGORY_TO_ZCODES.put(SdohCategory.EDUCATION, Arrays.asList("Z55.0", "Z55.1", "Z55.2", "Z55.3", "Z55.4", "Z55.8", "Z55.9"));
        CATEGORY_TO_ZCODES.put(SdohCategory.EMPLOYMENT, Arrays.asList("Z56.0", "Z56.1", "Z56.2", "Z56.3", "Z56.4", "Z56.5", "Z56.6", "Z56.81", "Z56.82", "Z56.89", "Z56.9"));
        CATEGORY_TO_ZCODES.put(SdohCategory.UTILITIES, Arrays.asList("Z59.1"));
        CATEGORY_TO_ZCODES.put(SdohCategory.SOCIAL_ISOLATION, Arrays.asList("Z60.2", "Z60.3", "Z60.4"));
        CATEGORY_TO_ZCODES.put(SdohCategory.INTERPERSONAL_VIOLENCE, Arrays.asList("Z69.0", "Z69.1", "Z91.410", "Z91.411", "Z91.412", "Z91.419"));
    }

    private static final Map<String, String> ZCODE_DESCRIPTIONS = new HashMap<>();

    static {
        ZCODE_DESCRIPTIONS.put("Z59.4", "Lack of adequate food and safe drinking water");
        ZCODE_DESCRIPTIONS.put("Z59.41", "Food insecurity");
        ZCODE_DESCRIPTIONS.put("Z59.48", "Other specified lack of adequate food");
        ZCODE_DESCRIPTIONS.put("Z59.0", "Homelessness");
        ZCODE_DESCRIPTIONS.put("Z59.00", "Homelessness unspecified");
        ZCODE_DESCRIPTIONS.put("Z59.01", "Sheltered homelessness");
        ZCODE_DESCRIPTIONS.put("Z59.02", "Unsheltered homelessness");
        ZCODE_DESCRIPTIONS.put("Z59.82", "Transportation insecurity");
        ZCODE_DESCRIPTIONS.put("Z59.5", "Extreme poverty");
        ZCODE_DESCRIPTIONS.put("Z59.6", "Low income");
        ZCODE_DESCRIPTIONS.put("Z59.7", "Insufficient social insurance and welfare support");
        ZCODE_DESCRIPTIONS.put("Z55.0", "Illiteracy and low-level literacy");
        ZCODE_DESCRIPTIONS.put("Z55.1", "Schooling unavailable and unattainable");
        ZCODE_DESCRIPTIONS.put("Z56.0", "Unemployment, unspecified");
        ZCODE_DESCRIPTIONS.put("Z56.1", "Change of job");
        ZCODE_DESCRIPTIONS.put("Z56.2", "Threat of job loss");
        ZCODE_DESCRIPTIONS.put("Z59.1", "Inadequate housing");
    }

    public List<String> getZCodesForCategory(SdohCategory category) {
        if (category == null) {
            return Collections.emptyList();
        }
        return CATEGORY_TO_ZCODES.getOrDefault(category, Collections.emptyList());
    }

    public String getZCodeDescription(String zCode) {
        return ZCODE_DESCRIPTIONS.getOrDefault(zCode, "Unknown Z-code");
    }

    public List<String> mapNeedsToZCodes(Map<SdohCategory, Boolean> needs) {
        return needs.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .flatMap(category -> getZCodesForCategory(category).stream())
                .limit(1) // Take first Z-code for each category
                .collect(Collectors.toList());
    }

    @Transactional
    public SdohDiagnosis createDiagnosis(String tenantId, String patientId, SdohCategory category, String diagnosedBy) {
        List<String> zCodes = getZCodesForCategory(category);
        if (zCodes.isEmpty()) {
            throw new IllegalArgumentException("No Z-codes found for category: " + category);
        }

        String zCode = zCodes.get(0);
        SdohDiagnosis diagnosis = SdohDiagnosis.builder()
                .patientId(patientId)
                .tenantId(tenantId)
                .zCode(zCode)
                .zCodeDescription(getZCodeDescription(zCode))
                .category(category)
                .status(SdohDiagnosis.DiagnosisStatus.ACTIVE)
                .diagnosisDate(LocalDateTime.now())
                .diagnosedBy(diagnosedBy)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        SdohDiagnosisEntity entity = convertToEntity(diagnosis);
        entity = diagnosisRepository.save(entity);

        return convertToModel(entity);
    }

    public List<SdohDiagnosis> getPatientDiagnoses(String tenantId, String patientId) {
        return diagnosisRepository.findByTenantIdAndPatientId(tenantId, patientId).stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    public List<SdohDiagnosis> getActiveDiagnoses(String tenantId, String patientId) {
        return diagnosisRepository.findByTenantIdAndPatientIdAndStatus(
                        tenantId, patientId, SdohDiagnosis.DiagnosisStatus.ACTIVE).stream()
                .map(this::convertToModel)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateDiagnosisStatus(String diagnosisId, SdohDiagnosis.DiagnosisStatus status) {
        diagnosisRepository.findById(diagnosisId).ifPresent(entity -> {
            entity.setStatus(status);
            diagnosisRepository.save(entity);
        });
    }

    public boolean isValidZCode(String zCode) {
        if (zCode == null || zCode.isEmpty()) {
            return false;
        }
        return zCode.matches("^Z[0-9]{2}\\.[0-9]{1,2}$") || zCode.matches("^Z[0-9]{2}$");
    }

    public Optional<SdohCategory> getCategoryFromZCode(String zCode) {
        return CATEGORY_TO_ZCODES.entrySet().stream()
                .filter(entry -> entry.getValue().contains(zCode))
                .map(Map.Entry::getKey)
                .findFirst();
    }

    public String exportToFhirCondition(SdohDiagnosis diagnosis) {
        return String.format("{\"resourceType\": \"Condition\", \"code\": {\"coding\": [{\"system\": \"http://hl7.org/fhir/sid/icd-10-cm\", \"code\": \"%s\"}]}}",
                diagnosis.getZCode());
    }

    private SdohDiagnosisEntity convertToEntity(SdohDiagnosis diagnosis) {
        return SdohDiagnosisEntity.builder()
                .diagnosisId(diagnosis.getDiagnosisId())
                .patientId(diagnosis.getPatientId())
                .tenantId(diagnosis.getTenantId())
                .zCode(diagnosis.getZCode())
                .zCodeDescription(diagnosis.getZCodeDescription())
                .category(diagnosis.getCategory())
                .clinicalNote(diagnosis.getClinicalNote())
                .status(diagnosis.getStatus())
                .diagnosisDate(diagnosis.getDiagnosisDate())
                .diagnosedBy(diagnosis.getDiagnosedBy())
                .createdAt(diagnosis.getCreatedAt())
                .updatedAt(diagnosis.getUpdatedAt())
                .build();
    }

    private SdohDiagnosis convertToModel(SdohDiagnosisEntity entity) {
        return SdohDiagnosis.builder()
                .diagnosisId(entity.getDiagnosisId())
                .patientId(entity.getPatientId())
                .tenantId(entity.getTenantId())
                .zCode(entity.getZCode())
                .zCodeDescription(entity.getZCodeDescription())
                .category(entity.getCategory())
                .clinicalNote(entity.getClinicalNote())
                .status(entity.getStatus())
                .diagnosisDate(entity.getDiagnosisDate())
                .diagnosedBy(entity.getDiagnosedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
