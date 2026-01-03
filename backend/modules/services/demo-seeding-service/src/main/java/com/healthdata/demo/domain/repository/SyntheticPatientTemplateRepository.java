package com.healthdata.demo.domain.repository;

import com.healthdata.demo.domain.model.SyntheticPatientTemplate;
import com.healthdata.demo.domain.model.SyntheticPatientTemplate.Gender;
import com.healthdata.demo.domain.model.SyntheticPatientTemplate.RiskCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for SyntheticPatientTemplate entity.
 */
@Repository
public interface SyntheticPatientTemplateRepository extends JpaRepository<SyntheticPatientTemplate, UUID> {

    /**
     * Find template by persona name.
     */
    Optional<SyntheticPatientTemplate> findByPersonaName(String personaName);

    /**
     * Find all active templates.
     */
    List<SyntheticPatientTemplate> findByIsActiveTrueOrderByDisplayNameAsc();

    /**
     * Find templates by risk category.
     */
    List<SyntheticPatientTemplate> findByRiskCategoryAndIsActiveTrue(RiskCategory riskCategory);

    /**
     * Find templates by gender.
     */
    List<SyntheticPatientTemplate> findByGenderAndIsActiveTrue(Gender gender);

    /**
     * Check if persona name exists.
     */
    boolean existsByPersonaName(String personaName);

    /**
     * Find templates with care gaps defined.
     */
    @Query("SELECT t FROM SyntheticPatientTemplate t WHERE t.isActive = true " +
           "AND t.careGaps IS NOT NULL AND t.careGaps != '[]'")
    List<SyntheticPatientTemplate> findTemplatesWithCareGaps();

    /**
     * Find templates with SDOH factors.
     */
    @Query("SELECT t FROM SyntheticPatientTemplate t WHERE t.isActive = true " +
           "AND t.sdohFactors IS NOT NULL AND t.sdohFactors != '[]'")
    List<SyntheticPatientTemplate> findTemplatesWithSdohFactors();

    /**
     * Find high-risk templates (for risk stratification demo).
     */
    default List<SyntheticPatientTemplate> findHighRiskTemplates() {
        return findByRiskCategoryAndIsActiveTrue(RiskCategory.HIGH);
    }

    /**
     * Count templates by risk category.
     */
    long countByRiskCategoryAndIsActiveTrue(RiskCategory riskCategory);
}
