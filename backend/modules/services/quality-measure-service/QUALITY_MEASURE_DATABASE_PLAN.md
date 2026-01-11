# Quality Measure Database Implementation Plan

## Executive Summary

This document outlines the complete database schema implementation for quality measure (QM) lifecycle management, including creation, editing, versioning, and patient-specific personalization.

**Status**: Phase 1 Complete (Core tables exist) | Phase 2-5 Pending (Personalization, tracking)

---

## Current State Analysis

### ✅ Existing Tables (Phase 1 - Complete)

| Table | Purpose | Migration | Status |
|-------|---------|-----------|--------|
| `quality_measures` | Standard measure definitions (HEDIS, CMS) | 0026 | ✅ Exists |
| `custom_measures` | Organization-defined custom measures | 0003 | ✅ Exists |
| `measure_versions` | Immutable version history for custom measures | 0031 | ✅ Exists |
| `measure_results` | Patient-level calculation results | 0027 | ✅ Exists |
| `measure_populations` | Population stratification data | 0028 | ✅ Exists |

### ❌ Missing Components (Phase 2-5)

1. **Patient-specific measure personalization**
2. **Measure assignment and eligibility tracking**
3. **Configuration profiles and reusable templates**
4. **Comprehensive audit trails**
5. **Patient-specific thresholds and overrides**
6. **Measure execution scheduling and history**

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                   Quality Measure System                     │
└─────────────────────────────────────────────────────────────┘
                            │
        ┌───────────────────┼───────────────────┐
        ▼                   ▼                   ▼
┌──────────────┐   ┌──────────────┐   ┌──────────────┐
│   Standard   │   │    Custom    │   │   Patient    │
│   Measures   │   │   Measures   │   │ Assignments  │
│  (HEDIS/CMS) │   │  (Org-level) │   │  (Per-PT)    │
└──────────────┘   └──────────────┘   └──────────────┘
        │                   │                   │
        │                   │                   │
        └───────────────────┴───────────────────┘
                            │
                            ▼
                  ┌──────────────────┐
                  │  Measure Results │
                  │  (Calculations)  │
                  └──────────────────┘
```

---

## Phase 2: Patient-Specific Measure Assignments

### 2.1 Measure Assignment Table

**Purpose**: Track which measures apply to which patients, with eligibility rules.

**Migration**: `0034-create-patient-measure-assignments.xml`

```sql
CREATE TABLE patient_measure_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    patient_id UUID NOT NULL,
    measure_id UUID NOT NULL,
    measure_type VARCHAR(20) NOT NULL, -- 'STANDARD', 'CUSTOM'

    -- Assignment Details
    assigned_by UUID NOT NULL, -- User who assigned
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    assignment_reason TEXT,

    -- Status
    active BOOLEAN NOT NULL DEFAULT true,
    effective_from DATE NOT NULL,
    effective_until DATE,

    -- Eligibility
    eligibility_criteria JSONB, -- Dynamic eligibility rules
    auto_assigned BOOLEAN DEFAULT false, -- Was this auto-assigned by system?

    -- Deactivation
    deactivated_by UUID,
    deactivated_at TIMESTAMP WITH TIME ZONE,
    deactivation_reason TEXT,

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_pma_patient FOREIGN KEY (patient_id)
        REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_pma_assigned_by FOREIGN KEY (assigned_by)
        REFERENCES users(id),
    CONSTRAINT uk_patient_measure UNIQUE (patient_id, measure_id, active)
);

CREATE INDEX idx_pma_patient ON patient_measure_assignments(patient_id, active);
CREATE INDEX idx_pma_measure ON patient_measure_assignments(measure_id, active);
CREATE INDEX idx_pma_tenant ON patient_measure_assignments(tenant_id);
CREATE INDEX idx_pma_effective_period ON patient_measure_assignments(effective_from, effective_until);
```

**Benefits**:
- Track which patients are enrolled in which quality measures
- Support automated assignment based on criteria
- Historical tracking of measure assignments
- Support for date-based measure activation/deactivation

---

## Phase 3: Patient-Specific Measure Overrides

### 3.1 Patient Measure Overrides Table

**Purpose**: Allow patient-specific customization of measure parameters without modifying the base measure.

**Migration**: `0035-create-patient-measure-overrides.xml`

```sql
CREATE TABLE patient_measure_overrides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    patient_id UUID NOT NULL,
    measure_id UUID NOT NULL,

    -- Override Configuration
    override_type VARCHAR(50) NOT NULL, -- 'THRESHOLD', 'PARAMETER', 'EXCLUSION', 'INCLUSION_CRITERIA'
    override_field VARCHAR(100) NOT NULL, -- Which field is being overridden
    original_value TEXT, -- Original value from measure definition
    override_value TEXT NOT NULL, -- Patient-specific value
    value_type VARCHAR(50), -- 'NUMERIC', 'DATE', 'BOOLEAN', 'TEXT', 'JSON'

    -- Clinical Justification
    clinical_reason TEXT NOT NULL, -- Why this override is needed
    supporting_evidence JSONB, -- Links to clinical notes, labs, etc.
    approved_by UUID, -- Clinician who approved override
    approved_at TIMESTAMP WITH TIME ZONE,

    -- Status
    active BOOLEAN NOT NULL DEFAULT true,
    effective_from DATE NOT NULL,
    effective_until DATE,

    -- Review
    requires_periodic_review BOOLEAN DEFAULT true,
    review_frequency_days INTEGER DEFAULT 90,
    last_reviewed_at TIMESTAMP WITH TIME ZONE,
    next_review_date DATE,

    -- Audit
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_pmo_patient FOREIGN KEY (patient_id)
        REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_pmo_created_by FOREIGN KEY (created_by)
        REFERENCES users(id)
);

CREATE INDEX idx_pmo_patient ON patient_measure_overrides(patient_id, measure_id, active);
CREATE INDEX idx_pmo_measure ON patient_measure_overrides(measure_id, active);
CREATE INDEX idx_pmo_review ON patient_measure_overrides(next_review_date)
    WHERE active = true AND requires_periodic_review = true;
CREATE INDEX idx_pmo_tenant ON patient_measure_overrides(tenant_id);
```

**Example Use Cases**:
```json
{
  "override_type": "THRESHOLD",
  "override_field": "hba1c_target",
  "original_value": "7.0",
  "override_value": "8.0",
  "clinical_reason": "Patient has severe hypoglycemia history; relaxed target per ADA guidelines",
  "supporting_evidence": {
    "note_ids": ["note-123", "note-456"],
    "lab_results": ["lab-789"],
    "encounter_ids": ["enc-999"]
  }
}
```

---

## Phase 4: Measure Configuration Profiles

### 4.1 Measure Configuration Profiles Table

**Purpose**: Reusable measure configuration templates for patient populations.

**Migration**: `0036-create-measure-config-profiles.xml`

```sql
CREATE TABLE measure_config_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,

    -- Profile Identity
    profile_name VARCHAR(255) NOT NULL,
    profile_code VARCHAR(100) NOT NULL, -- Unique code for API access
    description TEXT,

    -- Target Population
    population_criteria JSONB NOT NULL, -- Rules defining who this applies to
    priority INTEGER DEFAULT 0, -- Higher priority profiles override lower

    -- Configuration
    config_overrides JSONB NOT NULL, -- Map of field -> value overrides

    -- Status
    active BOOLEAN NOT NULL DEFAULT true,
    effective_from DATE NOT NULL,
    effective_until DATE,

    -- Clinical Governance
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    approval_notes TEXT,

    -- Audit
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_mcp_created_by FOREIGN KEY (created_by)
        REFERENCES users(id),
    CONSTRAINT uk_profile_code UNIQUE (tenant_id, profile_code)
);

CREATE INDEX idx_mcp_tenant ON measure_config_profiles(tenant_id, active);
CREATE INDEX idx_mcp_effective ON measure_config_profiles(effective_from, effective_until);
```

**Example Profile**:
```json
{
  "profile_name": "Elderly Diabetes Management (Age 75+)",
  "profile_code": "ELDERLY_DM_75PLUS",
  "population_criteria": {
    "conditions": [
      {"field": "age", "operator": ">=", "value": 75},
      {"field": "diagnosis_codes", "operator": "includes", "value": ["E11.9"]}
    ]
  },
  "config_overrides": {
    "hba1c_target": 8.0,
    "bp_systolic_target": 150,
    "ldl_target": 100
  }
}
```

### 4.2 Patient Profile Assignments Table

**Migration**: `0037-create-patient-profile-assignments.xml`

```sql
CREATE TABLE patient_profile_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    patient_id UUID NOT NULL,
    profile_id UUID NOT NULL,

    -- Assignment
    assigned_by UUID,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    auto_assigned BOOLEAN DEFAULT false,
    assignment_reason TEXT,

    -- Status
    active BOOLEAN NOT NULL DEFAULT true,
    effective_from DATE NOT NULL,
    effective_until DATE,

    CONSTRAINT fk_ppa_patient FOREIGN KEY (patient_id)
        REFERENCES patients(id) ON DELETE CASCADE,
    CONSTRAINT fk_ppa_profile FOREIGN KEY (profile_id)
        REFERENCES measure_config_profiles(id) ON DELETE CASCADE,
    CONSTRAINT uk_patient_profile UNIQUE (patient_id, profile_id, active)
);

CREATE INDEX idx_ppa_patient ON patient_profile_assignments(patient_id, active);
CREATE INDEX idx_ppa_profile ON patient_profile_assignments(profile_id);
```

---

## Phase 5: Comprehensive Audit and Tracking

### 5.1 Measure Execution History

**Purpose**: Track every time a measure is calculated for audit and debugging.

**Migration**: `0038-create-measure-execution-history.xml`

```sql
CREATE TABLE measure_execution_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,

    -- Execution Context
    measure_id UUID NOT NULL,
    measure_version VARCHAR(50), -- Version at time of execution
    patient_id UUID NOT NULL,
    execution_timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    -- Execution Details
    execution_mode VARCHAR(50), -- 'SCHEDULED', 'MANUAL', 'API', 'BULK'
    triggered_by UUID, -- User or system job that triggered
    execution_context JSONB, -- Additional context (e.g., batch_id, api_request_id)

    -- Performance
    start_time TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time TIMESTAMP WITH TIME ZONE,
    duration_ms BIGINT,

    -- Result
    result_id UUID, -- FK to measure_results
    execution_status VARCHAR(50) NOT NULL, -- 'SUCCESS', 'FAILURE', 'PARTIAL', 'SKIPPED'
    error_message TEXT,
    error_details JSONB,

    -- Data Used
    data_sources JSONB, -- Which data was used for calculation
    data_period_start DATE,
    data_period_end DATE,

    -- Overrides Applied
    overrides_applied JSONB, -- Which patient overrides were active
    profile_applied UUID, -- FK to measure_config_profiles

    -- Audit
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_meh_patient ON measure_execution_history(patient_id, execution_timestamp DESC);
CREATE INDEX idx_meh_measure ON measure_execution_history(measure_id, execution_timestamp DESC);
CREATE INDEX idx_meh_status ON measure_execution_history(execution_status);
CREATE INDEX idx_meh_result ON measure_execution_history(result_id);
```

### 5.2 Measure Modification Audit

**Purpose**: Track all changes to measure definitions for compliance and debugging.

**Migration**: `0039-create-measure-modification-audit.xml`

```sql
CREATE TABLE measure_modification_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,

    -- What Changed
    entity_type VARCHAR(50) NOT NULL, -- 'QUALITY_MEASURE', 'CUSTOM_MEASURE', 'OVERRIDE', 'PROFILE'
    entity_id UUID NOT NULL,
    entity_name VARCHAR(255),

    -- Change Details
    operation VARCHAR(50) NOT NULL, -- 'CREATE', 'UPDATE', 'DELETE', 'ACTIVATE', 'DEACTIVATE', 'PUBLISH'
    field_name VARCHAR(100), -- Which field was changed (for UPDATE)
    old_value TEXT,
    new_value TEXT,
    change_summary TEXT NOT NULL,

    -- Context
    reason TEXT,
    impact_assessment TEXT, -- How many patients affected, etc.
    rollback_available BOOLEAN DEFAULT false,
    rollback_data JSONB, -- Data needed to rollback change

    -- Approval Workflow
    requires_approval BOOLEAN DEFAULT false,
    approval_status VARCHAR(50), -- 'PENDING', 'APPROVED', 'REJECTED'
    approved_by UUID,
    approved_at TIMESTAMP WITH TIME ZONE,
    approval_notes TEXT,

    -- Who/When/Where
    modified_by UUID NOT NULL,
    modified_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    ip_address VARCHAR(45),
    user_agent TEXT,

    CONSTRAINT fk_mma_modified_by FOREIGN KEY (modified_by)
        REFERENCES users(id)
);

CREATE INDEX idx_mma_entity ON measure_modification_audit(entity_type, entity_id, modified_at DESC);
CREATE INDEX idx_mma_modified_by ON measure_modification_audit(modified_by, modified_at DESC);
CREATE INDEX idx_mma_tenant ON measure_modification_audit(tenant_id);
CREATE INDEX idx_mma_approval ON measure_modification_audit(approval_status)
    WHERE requires_approval = true;
```

### 5.3 Patient Measure Eligibility Cache

**Purpose**: Cached computation of which measures apply to each patient (performance optimization).

**Migration**: `0040-create-patient-measure-eligibility-cache.xml`

```sql
CREATE TABLE patient_measure_eligibility_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(100) NOT NULL,
    patient_id UUID NOT NULL,
    measure_id UUID NOT NULL,

    -- Eligibility Result
    is_eligible BOOLEAN NOT NULL,
    eligibility_reason TEXT,
    eligibility_criteria_met JSONB, -- Which criteria passed/failed

    -- Cache Metadata
    calculated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    valid_until TIMESTAMP WITH TIME ZONE NOT NULL,
    invalidated BOOLEAN DEFAULT false,

    -- Data Version
    patient_data_version VARCHAR(100), -- Hash/version of patient data used
    measure_version VARCHAR(50),

    CONSTRAINT uk_pmec_patient_measure UNIQUE (patient_id, measure_id)
);

CREATE INDEX idx_pmec_patient ON patient_measure_eligibility_cache(patient_id)
    WHERE invalidated = false;
CREATE INDEX idx_pmec_measure ON patient_measure_eligibility_cache(measure_id)
    WHERE invalidated = false;
CREATE INDEX idx_pmec_valid_until ON patient_measure_eligibility_cache(valid_until);
```

---

## Implementation Priority & Timeline

### Phase 2: Patient Assignments (Priority: HIGH)
**Estimated Effort**: 3-5 days
- Create patient_measure_assignments table
- Implement assignment API endpoints
- Add automated assignment rules engine
- Create assignment management UI

### Phase 3: Patient Overrides (Priority: HIGH)
**Estimated Effort**: 5-7 days
- Create patient_measure_overrides table
- Implement override resolution logic (which override wins)
- Add clinical justification workflow
- Create override management UI with approval workflow

### Phase 4: Configuration Profiles (Priority: MEDIUM)
**Estimated Effort**: 5-7 days
- Create measure_config_profiles table
- Create patient_profile_assignments table
- Implement profile matching engine
- Add profile management UI

### Phase 5: Audit & Tracking (Priority: MEDIUM)
**Estimated Effort**: 4-6 days
- Create measure_execution_history table
- Create measure_modification_audit table
- Create patient_measure_eligibility_cache table
- Implement automatic audit logging
- Create audit report dashboards

---

## Data Flow Examples

### Example 1: Calculating a Measure with Patient Overrides

```
1. Lookup patient_measure_assignments
   → Is patient enrolled in measure? YES

2. Check patient_measure_eligibility_cache
   → Is cache valid? NO → Recalculate eligibility

3. Load base measure definition from quality_measures
   → HbA1c target: 7.0%

4. Check patient_profile_assignments
   → Patient has "Elderly DM 75+" profile
   → Profile overrides: HbA1c target → 8.0%

5. Check patient_measure_overrides
   → Patient-specific override: HbA1c target → 8.5%
   → Reason: "Severe hypoglycemia history"

6. Apply override resolution (patient > profile > base)
   → Final HbA1c target: 8.5%

7. Calculate measure with personalized parameters

8. Store result in measure_results

9. Log execution in measure_execution_history
   → Record which overrides were applied
```

### Example 2: Editing a Custom Measure

```
1. User requests edit to custom_measure

2. Create new version in measure_versions
   → Version: 1.2.0 → 1.3.0
   → Store old CQL and new CQL
   → Change summary: "Updated LDL threshold"

3. Log change in measure_modification_audit
   → Operation: UPDATE
   → Field: cql_text
   → Old value: [stored]
   → New value: [stored]
   → Impact: 234 patients affected

4. If change requires approval:
   → Set approval_status = 'PENDING'
   → Notify approvers
   → Wait for approval

5. On approval:
   → Set measure_versions.is_current = true
   → Update custom_measures.version
   → Invalidate patient_measure_eligibility_cache for affected patients

6. Trigger recalculation of affected measures
```

---

## HIPAA Compliance & Security

### Audit Requirements ✅
- **All measure modifications tracked** in measure_modification_audit
- **All calculations logged** in measure_execution_history
- **Patient overrides require clinical justification** with supporting evidence
- **Approval workflows** for sensitive changes
- **Immutable version history** in measure_versions

### Data Access Controls
- **Tenant isolation** on all tables via tenant_id
- **Row-level security (RLS)** recommended for multi-tenant deployments
- **Audit all PHI access** via existing audit_events table
- **Encryption at rest** for all clinical justification fields

### Data Retention
- **measure_execution_history**: Retain 7 years (HIPAA minimum)
- **measure_modification_audit**: Retain indefinitely (regulatory compliance)
- **measure_results**: Retain 10 years (quality reporting requirements)
- **patient_measure_overrides**: Retain as long as clinically relevant

---

## API Design Recommendations

### RESTful Endpoints

```
POST   /api/v1/measures/{measureId}/assign
POST   /api/v1/patients/{patientId}/measure-overrides
GET    /api/v1/patients/{patientId}/applicable-measures
GET    /api/v1/measures/{measureId}/calculation-history
POST   /api/v1/measures/{measureId}/calculate
GET    /api/v1/measures/{measureId}/versions
POST   /api/v1/measures/{measureId}/versions/{version}/publish
GET    /api/v1/audit/measure-modifications
```

---

## Migration Checklist

### Pre-Migration
- [ ] Backup production database
- [ ] Review all existing measure_results for data integrity
- [ ] Document current measure assignment logic
- [ ] Identify patients with special requirements

### Migration Execution
- [ ] Run Phase 2 migrations (0034-0037)
- [ ] Migrate existing measure assignments to patient_measure_assignments
- [ ] Run Phase 3 migrations (0038-0040)
- [ ] Create initial configuration profiles for common populations
- [ ] Backfill measure_execution_history from measure_results

### Post-Migration
- [ ] Verify foreign key constraints
- [ ] Run entity-migration validation tests
- [ ] Test measure calculation with overrides
- [ ] Verify audit trail completeness
- [ ] Update API documentation

---

## Next Steps

1. **Review and approve this plan** with clinical stakeholders
2. **Create Liquibase migration files** (0034-0040)
3. **Update JPA entities** to match new schema
4. **Implement service layer** for override resolution
5. **Create API endpoints** for CRUD operations
6. **Build management UI** for clinical staff
7. **Write comprehensive tests** for all scenarios

---

**Document Version**: 1.0
**Last Updated**: 2026-01-11
**Author**: Claude Code
**Status**: Awaiting Review
