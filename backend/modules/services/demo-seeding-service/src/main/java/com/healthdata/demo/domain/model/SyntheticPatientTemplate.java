package com.healthdata.demo.domain.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Pre-defined patient templates (personas) for consistent demo experiences.
 *
 * Patient personas provide recognizable, relatable examples:
 * - Michael Chen: Complex diabetic patient
 * - Sarah Martinez: Preventive care gap patient
 * - Emma Johnson: High-risk multi-morbid patient
 * - Carlos Rodriguez: SDOH barriers patient
 */
@Entity
@Table(name = "synthetic_patient_templates")
public class SyntheticPatientTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "persona_name", nullable = false, unique = true, length = 100)
    private String personaName;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "age", nullable = false)
    private Integer age;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Column(name = "hcc_score", nullable = false, precision = 4, scale = 2)
    private BigDecimal hccScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fhir_bundle", columnDefinition = "JSONB")
    private String fhirBundle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "conditions", columnDefinition = "JSONB")
    private String conditions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "medications", columnDefinition = "JSONB")
    private String medications;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "care_gaps", columnDefinition = "JSONB")
    private String careGaps;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "observations", columnDefinition = "JSONB")
    private String observations;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "encounters", columnDefinition = "JSONB")
    private String encounters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sdoh_factors", columnDefinition = "JSONB")
    private String sdohFactors;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_category", nullable = false, length = 20)
    private RiskCategory riskCategory;

    @Column(name = "scenario_notes", columnDefinition = "TEXT")
    private String scenarioNotes;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "template_version", nullable = false)
    private Integer templateVersion = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Constructors
    public SyntheticPatientTemplate() {}

    public SyntheticPatientTemplate(String personaName, String firstName, String lastName,
                                     Integer age, Gender gender, BigDecimal hccScore,
                                     RiskCategory riskCategory) {
        this.personaName = personaName;
        this.displayName = firstName + " " + lastName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
        this.gender = gender;
        this.hccScore = hccScore;
        this.riskCategory = riskCategory;
    }

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getPersonaName() { return personaName; }
    public void setPersonaName(String personaName) { this.personaName = personaName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }

    public Gender getGender() { return gender; }
    public void setGender(Gender gender) { this.gender = gender; }

    public BigDecimal getHccScore() { return hccScore; }
    public void setHccScore(BigDecimal hccScore) { this.hccScore = hccScore; }

    public String getFhirBundle() { return fhirBundle; }
    public void setFhirBundle(String fhirBundle) { this.fhirBundle = fhirBundle; }

    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }

    public String getMedications() { return medications; }
    public void setMedications(String medications) { this.medications = medications; }

    public String getCareGaps() { return careGaps; }
    public void setCareGaps(String careGaps) { this.careGaps = careGaps; }

    public String getObservations() { return observations; }
    public void setObservations(String observations) { this.observations = observations; }

    public String getEncounters() { return encounters; }
    public void setEncounters(String encounters) { this.encounters = encounters; }

    public String getSdohFactors() { return sdohFactors; }
    public void setSdohFactors(String sdohFactors) { this.sdohFactors = sdohFactors; }

    public RiskCategory getRiskCategory() { return riskCategory; }
    public void setRiskCategory(RiskCategory riskCategory) { this.riskCategory = riskCategory; }

    public String getScenarioNotes() { return scenarioNotes; }
    public void setScenarioNotes(String scenarioNotes) { this.scenarioNotes = scenarioNotes; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Integer getTemplateVersion() { return templateVersion; }
    public void setTemplateVersion(Integer templateVersion) { this.templateVersion = templateVersion; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * Gender options for patient templates.
     */
    public enum Gender {
        MALE, FEMALE, OTHER, UNKNOWN
    }

    /**
     * Risk category for patient stratification.
     */
    public enum RiskCategory {
        LOW,      // HCC < 1.0
        MODERATE, // HCC 1.0-2.0
        HIGH      // HCC > 2.0
    }
}
