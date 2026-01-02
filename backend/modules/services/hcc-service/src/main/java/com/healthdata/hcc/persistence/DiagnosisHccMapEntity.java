package com.healthdata.hcc.persistence;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity mapping ICD-10-CM diagnosis codes to HCC codes.
 *
 * Maps diagnosis codes to both V24 and V28 HCC models to support
 * the transition period through 2026.
 *
 * Data sources:
 * - CMS HCC mapping files (annual updates)
 * - ICD-10-CM code descriptions
 */
@Entity
@Table(name = "diagnosis_hcc_map", schema = "hcc",
    indexes = {
        @Index(name = "idx_diagnosis_hcc_icd10", columnList = "icd10_code"),
        @Index(name = "idx_diagnosis_hcc_v24", columnList = "hcc_code_v24"),
        @Index(name = "idx_diagnosis_hcc_v28", columnList = "hcc_code_v28")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosisHccMapEntity {

    @Id
    @Column(name = "icd10_code", length = 10)
    private String icd10Code;

    @Column(name = "icd10_description", length = 500)
    private String icd10Description;

    @Column(name = "hcc_code_v24", length = 10)
    private String hccCodeV24;

    @Column(name = "hcc_name_v24", length = 255)
    private String hccNameV24;

    @Column(name = "hcc_code_v28", length = 10)
    private String hccCodeV28;

    @Column(name = "hcc_name_v28", length = 255)
    private String hccNameV28;

    /**
     * Coefficient for V24 model (community, non-dual, age 65-69, etc.)
     * Actual coefficient depends on demographic factors - this is base coefficient.
     */
    @Column(name = "coefficient_v24")
    private Double coefficientV24;

    /**
     * Coefficient for V28 model.
     */
    @Column(name = "coefficient_v28")
    private Double coefficientV28;

    /**
     * Whether the code requires more specificity for V28 mapping.
     * Example: Diabetes without complications may need complication documentation.
     */
    @Column(name = "requires_specificity")
    private Boolean requiresSpecificity;

    /**
     * If requires_specificity=true, what additional documentation is needed.
     */
    @Column(name = "specificity_guidance", length = 500)
    private String specificityGuidance;

    /**
     * Whether this HCC changed between V24 and V28.
     */
    @Column(name = "changed_in_v28")
    private Boolean changedInV28;

    /**
     * Description of how the mapping changed in V28.
     */
    @Column(name = "v28_change_description", length = 500)
    private String v28ChangeDescription;

    /**
     * True if this diagnosis maps to an HCC in V24 but not V28.
     */
    public boolean isDroppedInV28() {
        return hccCodeV24 != null && hccCodeV28 == null;
    }

    /**
     * True if this diagnosis maps to an HCC in V28 but not V24.
     */
    public boolean isAddedInV28() {
        return hccCodeV24 == null && hccCodeV28 != null;
    }

    /**
     * True if this diagnosis maps to different HCCs in V24 vs V28.
     */
    public boolean hasHccChange() {
        if (hccCodeV24 == null && hccCodeV28 == null) return false;
        if (hccCodeV24 == null || hccCodeV28 == null) return true;
        return !hccCodeV24.equals(hccCodeV28);
    }
}
