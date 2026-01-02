# Entity Relationship Diagram
**Event-Driven Health Assessment Platform**
**48 Tables Across 8 Microservices**

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          EVENT SOURCING LAYER                            │
│  ┌────────────────────────────────────────────────────────────────────┐ │
│  │ EVENT STORE (event_processing_db)                                  │ │
│  │                                                                     │ │
│  │  events                                                             │ │
│  │  ├─ id (PK)                                                        │ │
│  │  ├─ tenant_id                                                      │ │
│  │  ├─ event_type, aggregate_type, aggregate_id                      │ │
│  │  ├─ event_data (TEXT), metadata (TEXT)                           │ │
│  │  ├─ correlation_id, causation_id, version                        │ │
│  │  ├─ processed, processed_at, timestamp                           │ │
│  │  └─ user_id                                                       │ │
│  │                                                                     │ │
│  │  event_subscriptions                                               │ │
│  │  ├─ id (PK), tenant_id                                            │ │
│  │  ├─ event_type, subscriber_url                                    │ │
│  │  └─ active, retry_count                                           │ │
│  │                                                                     │ │
│  │  dead_letter_queue                                                 │ │
│  │  ├─ id (PK), tenant_id, original_event_id                        │ │
│  │  ├─ event_payload (JSONB + GIN)                                  │ │
│  │  ├─ error_message, stack_trace                                    │ │
│  │  ├─ retry_count, last_retry_at                                    │ │
│  │  └─ resolved, resolved_at                                         │ │
│  └────────────────────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
                                  ↓
        ┌─────────────────────────┴──────────────────────────┐
        ↓                                                     ↓
┌────────────────────┐                             ┌────────────────────┐
│   WRITE MODEL      │                             │    READ MODEL      │
│   (FHIR Store)     │                             │  (Denormalized)    │
│   fhir_db          │                             │  quality_measure_db│
└────────────────────┘                             └────────────────────┘
```

---

## Database 1: Event Processing Service (event_processing_db)

### events (Event Store)
```
┌──────────────────┐
│ events           │
├──────────────────┤
│ id PK            │ UUID
│ tenant_id        │ VARCHAR(64) + INDEX
│ event_type       │ VARCHAR(128) + INDEX
│ aggregate_type   │ VARCHAR(128) ┐
│ aggregate_id     │ VARCHAR(128) │ COMPOSITE INDEX
│ version          │ BIGINT       ┘
│ event_data       │ TEXT
│ metadata         │ TEXT
│ user_id          │ VARCHAR(128)
│ correlation_id   │ VARCHAR(128) + INDEX
│ causation_id     │ VARCHAR(128) + INDEX
│ timestamp        │ TIMESTAMP + INDEX (DESC)
│ processed        │ BOOLEAN + INDEX
│ processed_at     │ TIMESTAMP
└──────────────────┘
  │
  │ correlation_id
  ├──────────────────────────────┐
  │                               │
  │ causation_id (parent event)  │
  └──────────────────────────────┘
```

**Indexes (7 total):**
1. `idx_events_aggregate` (aggregate_type, aggregate_id, version)
2. `idx_events_type` (event_type, timestamp DESC)
3. `idx_events_tenant` (tenant_id, timestamp DESC)
4. `idx_events_correlation` (correlation_id)
5. `idx_events_processed` (processed, timestamp)
6. `idx_events_causation` (causation_id) WHERE NOT NULL
7. `idx_events_user` (tenant_id, user_id, timestamp DESC) WHERE NOT NULL

**Key Relationships:**
- `correlation_id`: Links all events in a business transaction
- `causation_id`: Points to the event that triggered this event
- `aggregate_id`: References the business entity (Patient, Measure, etc.)

---

### event_subscriptions (Subscription Registry)
```
┌──────────────────────┐
│ event_subscriptions  │
├──────────────────────┤
│ id PK                │ UUID
│ tenant_id            │ VARCHAR(64)
│ event_type           │ VARCHAR(128)
│ subscriber_url       │ VARCHAR(512)
│ active               │ BOOLEAN
│ retry_count          │ INTEGER
│ created_at           │ TIMESTAMP
│ updated_at           │ TIMESTAMP
└──────────────────────┘
```

---

### dead_letter_queue (Failed Events)
```
┌───────────────────────┐
│ dead_letter_queue     │
├───────────────────────┤
│ id PK                 │ UUID
│ tenant_id             │ VARCHAR(64)
│ original_event_id     │ UUID ──────┐
│ event_payload         │ JSONB + GIN│  References events.id
│ error_message         │ TEXT       │
│ stack_trace           │ TEXT       │
│ retry_count           │ INTEGER    │
│ last_retry_at         │ TIMESTAMP  │
│ resolved              │ BOOLEAN    │
│ resolved_at           │ TIMESTAMP  │
│ created_at            │ TIMESTAMP  │
│ updated_at            │ TIMESTAMP  │
└───────────────────────┘            │
                                     │
                                     ↓
                            ┌──────────────┐
                            │ events.id    │
                            └──────────────┘
```

**GIN Index:** `idx_dlq_event_payload_gin` ON event_payload

---

## Database 2: FHIR Service (fhir_db)

### patients (FHIR Patient Resources)
```
┌──────────────────┐
│ patients         │
├──────────────────┤
│ id PK            │ UUID
│ tenant_id        │ VARCHAR(255) + INDEX
│ fhir_id          │ VARCHAR(255) + UNIQUE
│ resource_json    │ JSONB + GIN INDEX
│ active           │ BOOLEAN
│ deleted_at       │ TIMESTAMP
│ created_at       │ TIMESTAMP
│ updated_at       │ TIMESTAMP
└──────────────────┘
  │
  │ 1:N
  ↓
┌──────────────────┐
│ observations     │
├──────────────────┤
│ id PK            │ UUID
│ tenant_id        │ VARCHAR(255)
│ patient_id       │ VARCHAR(255) ─────→ patients.fhir_id (conceptual)
│ fhir_id          │ VARCHAR(255) + UNIQUE
│ resource_json    │ JSONB + GIN INDEX
│ observation_date │ DATE + INDEX
│ code_system      │ VARCHAR(100)
│ code_value       │ VARCHAR(100)
│ created_at       │ TIMESTAMP
│ updated_at       │ TIMESTAMP
└──────────────────┘
```

**GIN Indexes (8 total):**
1. `idx_patients_resource_json_gin`
2. `idx_observations_resource_json_gin`
3. `idx_conditions_resource_json_gin`
4. `idx_medication_requests_resource_json_gin`
5. `idx_encounters_resource_json_gin`
6. `idx_procedures_resource_json_gin`
7. `idx_allergy_intolerances_resource_json_gin`
8. `idx_immunizations_resource_json_gin`

**Partial Indexes:**
- `idx_patients_active` (tenant_id, id) WHERE deleted_at IS NULL
- `idx_encounters_duration` (tenant_id, duration_minutes DESC) WHERE duration_minutes IS NOT NULL

---

### FHIR Resource Pattern (Repeated for all resources)
```
All FHIR resources follow this pattern:
┌──────────────────────┐
│ <resource>           │
├──────────────────────┤
│ id PK                │ UUID
│ tenant_id            │ VARCHAR(255) + INDEX
│ patient_id           │ VARCHAR(255) (conceptual FK)
│ fhir_id              │ VARCHAR(255) + UNIQUE
│ resource_json        │ JSONB + GIN INDEX
│ <resource_date>      │ DATE + INDEX
│ <resource_specific>  │ Various types
│ created_at           │ TIMESTAMP
│ updated_at           │ TIMESTAMP
└──────────────────────┘
```

**FHIR Resources:**
1. patients
2. observations (labs, vitals, assessments)
3. conditions (diagnoses)
4. medication_requests
5. encounters (visits)
6. procedures
7. allergy_intolerances
8. immunizations

---

## Database 3: Patient Service (patient_db)

### patient_demographics
```
┌──────────────────────────┐
│ patient_demographics     │
├──────────────────────────┤
│ id PK                    │ UUID
│ tenant_id                │ VARCHAR(100) ┐
│ mrn                      │ VARCHAR(50)  │ UNIQUE (tenant_id, mrn)
│ first_name               │ VARCHAR(100) │
│ last_name                │ VARCHAR(100) │
│ date_of_birth            │ DATE         │
│ gender                   │ VARCHAR(20)  │
│ zip_code                 │ VARCHAR(10) + INDEX
│ active                   │ BOOLEAN + INDEX
│ created_at               │ TIMESTAMP    │
│ updated_at               │ TIMESTAMP    │
└──────────────────────────┘
  │
  │ 1:N
  ├──────────────────────────────────────┐
  │                                      │
  ↓                                      ↓
┌──────────────────────┐     ┌──────────────────────┐
│ patient_insurance    │     │ patient_risk_scores  │
├──────────────────────┤     ├──────────────────────┤
│ id PK                │     │ id PK                │
│ patient_id FK        │     │ patient_id FK        │
│ tenant_id            │     │ tenant_id            │
│ payer_name           │     │ risk_score           │
│ policy_number        │     │ risk_category        │
│ coverage_type        │     │ factors JSONB + GIN  │
│ effective_date       │     │ comorbidities JSONB+│
│ expiration_date      │     │ created_at           │
│ created_at           │     │ updated_at           │
│ updated_at           │     │                      │
└──────────────────────┘     └──────────────────────┘
```

**Unique Constraint:** `idx_patient_demo_tenant_mrn` UNIQUE (tenant_id, mrn) WHERE mrn IS NOT NULL

**GIN Indexes (2):**
1. `idx_patient_risk_factors_gin` ON patient_risk_scores(factors)
2. `idx_patient_risk_comorbidities_gin` ON patient_risk_scores(comorbidities)

---

## Database 4: CQL Engine Service (cql_engine_db)

### cql_libraries
```
┌──────────────────────┐
│ cql_libraries        │
├──────────────────────┤
│ id PK                │ UUID
│ tenant_id            │ VARCHAR(100) + INDEX
│ name                 │ VARCHAR(255)
│ version              │ VARCHAR(50)
│ content              │ TEXT (CQL source code)
│ compiled_elm         │ JSONB + GIN INDEX
│ active               │ BOOLEAN
│ created_at           │ TIMESTAMP
│ updated_at           │ TIMESTAMP
└──────────────────────┘
  │
  │ Referenced by
  ↓
┌──────────────────────┐
│ cql_evaluations      │
├──────────────────────┤
│ id PK                │ UUID
│ tenant_id            │ VARCHAR(100)
│ patient_id           │ VARCHAR(100)
│ library_id           │ UUID (FK to cql_libraries.id)
│ measure_id           │ VARCHAR(100)
│ result               │ JSONB + GIN INDEX
│ context_data         │ JSONB + GIN INDEX
│ status               │ VARCHAR(20) + INDEX
│ evaluation_date      │ TIMESTAMP + INDEX
│ created_at           │ TIMESTAMP
│ updated_at           │ TIMESTAMP
└──────────────────────┘
```

**GIN Indexes (3):**
1. `idx_cql_libraries_compiled_elm_gin`
2. `idx_cql_eval_result_gin`
3. `idx_cql_eval_context_data_gin`

**Composite Indexes:**
- `idx_cql_eval_status` (status, evaluation_date DESC)

---

### value_sets
```
┌──────────────────────┐
│ value_sets           │
├──────────────────────┤
│ id PK                │ UUID
│ tenant_id            │ VARCHAR(100)
│ oid                  │ VARCHAR(255) (e.g., 2.16.840.1.113883.3.464.1003.104.12.1011)
│ name                 │ VARCHAR(255)
│ version              │ VARCHAR(50)
│ code_system          │ VARCHAR(100) (SNOMED, LOINC, RxNorm)
│ codes                │ TEXT (comma-separated)
│ created_at           │ TIMESTAMP
│ updated_at           │ TIMESTAMP
└──────────────────────┘
```

---

## Database 5: Quality Measure Service (quality_measure_db)

### quality_measure_results (READ MODEL)
```
┌─────────────────────────────┐
│ quality_measure_results     │
├─────────────────────────────┤
│ id PK                       │ UUID
│ tenant_id                   │ VARCHAR(50) ┐
│ patient_id                  │ UUID        │
│ measure_id                  │ VARCHAR(100)│ COMPOSITE INDEX
│ measure_year                │ INTEGER     ┘
│ measure_name                │ VARCHAR(255)
│ measure_category            │ VARCHAR(50)
│ numerator_compliant         │ BOOLEAN + COMPLIANCE INDEX
│ denominator_eligible        │ BOOLEAN
│ compliance_rate             │ DOUBLE
│ score                       │ DOUBLE
│ calculation_date            │ DATE + INDEX
│ cql_library                 │ VARCHAR(200)
│ cql_result                  │ JSONB + GIN INDEX
│ created_at                  │ TIMESTAMP
│ updated_at                  │ TIMESTAMP
│ created_by                  │ VARCHAR(100)
└─────────────────────────────┘
  │
  │ Triggers when non-compliant
  ↓
┌─────────────────────────────┐
│ care_gaps                   │
├─────────────────────────────┤
│ id PK                       │ UUID
│ tenant_id                   │ VARCHAR(100) + INDEX
│ patient_id                  │ VARCHAR(100) + INDEX
│ category                    │ VARCHAR(50) (ENUM)
│ gap_type                    │ VARCHAR(100)
│ title                       │ VARCHAR(255)
│ description                 │ TEXT
│ priority                    │ VARCHAR(20) + INDEX (URGENT, HIGH, MEDIUM, LOW)
│ status                      │ VARCHAR(20) + INDEX (OPEN, IN_PROGRESS, ADDRESSED, CLOSED)
│ quality_measure             │ VARCHAR(50) + INDEX
│ measure_result_id           │ UUID FK ──────────┐
│ created_from_measure        │ BOOLEAN           │
│ auto_closed                 │ BOOLEAN           │
│ recommendation              │ TEXT              │
│ evidence                    │ TEXT              │
│ evidence_resource_id        │ VARCHAR(100)      │
│ evidence_resource_type      │ VARCHAR(50)       │
│ due_date                    │ TIMESTAMP + INDEX │
│ identified_date             │ TIMESTAMP         │
│ addressed_date              │ TIMESTAMP         │
│ addressed_by                │ VARCHAR(255)      │
│ addressed_notes             │ TEXT              │
│ closed_at                   │ TIMESTAMP         │
│ closed_by                   │ VARCHAR(255)      │
│ matching_codes              │ TEXT              │
│ created_at                  │ TIMESTAMP         │
│ updated_at                  │ TIMESTAMP         │
└─────────────────────────────┘
                                      │
                                      │ Auto-closure linkage
                                      ↓
                            ┌─────────────────────┐
                            │ quality_measure_    │
                            │ results.id          │
                            └─────────────────────┘
```

**Care Gap Indexes (8):**
1. `idx_cg_patient_status` (patient_id, status)
2. `idx_cg_patient_priority` (patient_id, priority)
3. `idx_cg_due_date` (due_date)
4. `idx_cg_quality_measure` (quality_measure)
5. `idx_cg_patient_measure_status` (patient_id, measure_result_id, status)
6. `idx_cg_tenant` (tenant_id)
7. `idx_cg_tenant_patient` (tenant_id, patient_id)
8. `idx_cg_patient_category` (patient_id, category)

---

### risk_assessments
```
┌─────────────────────────────┐
│ risk_assessments            │
├─────────────────────────────┤
│ id PK                       │ UUID
│ tenant_id                   │ VARCHAR(100) + INDEX
│ patient_id                  │ VARCHAR(100) + INDEX
│ risk_score                  │ INTEGER (0-100)
│ risk_level                  │ VARCHAR(20) ENUM (LOW, MODERATE, HIGH, VERY_HIGH)
│ chronic_condition_count     │ INTEGER
│ risk_factors                │ JSONB + GIN INDEX
│ predicted_outcomes          │ JSONB + GIN INDEX
│ recommendations             │ JSONB + GIN INDEX
│ assessment_date             │ TIMESTAMP + INDEX
│ created_at                  │ TIMESTAMP
│ updated_at                  │ TIMESTAMP
└─────────────────────────────┘
```

**GIN Indexes (3):**
1. `idx_risk_assessments_risk_factors_gin`
2. `idx_risk_assessments_predicted_outcomes_gin`
3. `idx_risk_assessments_recommendations_gin`

**Composite Indexes:**
- `idx_ra_patient_date` (patient_id, assessment_date DESC)
- `idx_ra_risk_level` (patient_id, risk_level)
- `idx_ra_tenant_risk_level` (tenant_id, risk_level)

---

### health_scores (NEW - Phase 7)
```
┌─────────────────────────────┐
│ health_scores               │
├─────────────────────────────┤
│ id PK                       │ UUID
│ tenant_id                   │ VARCHAR(100) + INDEX
│ patient_id                  │ VARCHAR(100) + INDEX
│ overall_score               │ DECIMAL(5,2) (0-100)
│ physical_health_score       │ DECIMAL(5,2) (30% weight)
│ mental_health_score         │ DECIMAL(5,2) (25% weight)
│ social_determinants_score   │ DECIMAL(5,2) (15% weight)
│ preventive_care_score       │ DECIMAL(5,2) (15% weight)
│ chronic_disease_score       │ DECIMAL(5,2) (15% weight)
│ calculated_at               │ TIMESTAMP + INDEX
│ previous_score              │ DECIMAL(5,2)
│ significant_change          │ BOOLEAN + INDEX (delta >= 10)
│ change_reason               │ TEXT
│ created_at                  │ TIMESTAMP
│ updated_at                  │ TIMESTAMP
└─────────────────────────────┘
  │
  │ Historical snapshot
  ↓
┌─────────────────────────────┐
│ health_score_history        │
├─────────────────────────────┤
│ id PK                       │ UUID
│ tenant_id                   │ VARCHAR(100) + INDEX
│ patient_id                  │ VARCHAR(100) + INDEX
│ overall_score               │ DECIMAL(5,2)
│ physical_health_score       │ DECIMAL(5,2)
│ mental_health_score         │ DECIMAL(5,2)
│ social_determinants_score   │ DECIMAL(5,2)
│ preventive_care_score       │ DECIMAL(5,2)
│ chronic_disease_score       │ DECIMAL(5,2)
│ calculated_at               │ TIMESTAMP + INDEX
│ previous_score              │ DECIMAL(5,2)
│ score_delta                 │ DECIMAL(6,2)
│ change_reason               │ TEXT
│ created_at                  │ TIMESTAMP
└─────────────────────────────┘
```

**Health Score Indexes:**
1. `idx_hs_patient_calc` (patient_id, calculated_at DESC)
2. `idx_hs_tenant_patient` (tenant_id, patient_id)
3. `idx_hs_significant_change` (significant_change, calculated_at DESC)
4. `idx_hs_overall_score` (tenant_id, overall_score)

**Historical Trend Indexes:**
1. `idx_hsh_patient_date` (patient_id, calculated_at DESC)
2. `idx_hsh_tenant` (tenant_id, calculated_at DESC)
3. `idx_hsh_tenant_patient` (tenant_id, patient_id, calculated_at DESC)

---

### mental_health_assessments
```
┌─────────────────────────────┐
│ mental_health_assessments   │
├─────────────────────────────┤
│ id PK                       │ UUID
│ tenant_id                   │ VARCHAR(100)
│ patient_id                  │ VARCHAR(100)
│ assessment_type             │ VARCHAR(50) (PHQ-9, GAD-7, etc.)
│ score                       │ INTEGER
│ severity                    │ VARCHAR(20) (NONE, MILD, MODERATE, SEVERE)
│ assessment_date             │ TIMESTAMP + INDEX
│ created_at                  │ TIMESTAMP
│ updated_at                  │ TIMESTAMP
└─────────────────────────────┘
```

---

### saved_reports
```
┌─────────────────────────────┐
│ saved_reports               │
├─────────────────────────────┤
│ id PK                       │ UUID
│ tenant_id                   │ VARCHAR(100)
│ report_name                 │ VARCHAR(255)
│ report_type                 │ VARCHAR(50)
│ parameters                  │ TEXT (JSON)
│ created_by                  │ VARCHAR(100)
│ created_at                  │ TIMESTAMP
│ updated_at                  │ TIMESTAMP
└─────────────────────────────┘
```

---

### custom_measures
```
┌─────────────────────────────┐
│ custom_measures             │
├─────────────────────────────┤
│ id PK                       │ UUID
│ tenant_id                   │ VARCHAR(100)
│ measure_name                │ VARCHAR(255)
│ measure_description         │ TEXT
│ cql_logic                   │ TEXT
│ value_sets                  │ JSONB + GIN INDEX
│ active                      │ BOOLEAN
│ created_by                  │ VARCHAR(100)
│ created_at                  │ TIMESTAMP
│ updated_at                  │ TIMESTAMP
│ deleted_at                  │ TIMESTAMP (soft delete)
│ deleted_by                  │ VARCHAR(100)
└─────────────────────────────┘
```

**GIN Index:** `idx_custom_measures_value_sets_gin`

---

## Database 6: Care Gap Service (care_gap_db)

### care_gaps (Duplicate - managed separately)
```
Same structure as quality_measure_db.care_gaps
(Duplicate for service isolation, eventual consistency)
```

### care_gap_recommendations
```
┌──────────────────────────────┐
│ care_gap_recommendations     │
├──────────────────────────────┤
│ id PK                        │ UUID
│ care_gap_id FK               │ UUID → care_gaps.id
│ tenant_id                    │ VARCHAR(100)
│ recommendation_text          │ TEXT
│ priority                     │ VARCHAR(20)
│ created_at                   │ TIMESTAMP
│ updated_at                   │ TIMESTAMP
└──────────────────────────────┘
```

### care_gap_closures
```
┌──────────────────────────────┐
│ care_gap_closures            │
├──────────────────────────────┤
│ id PK                        │ UUID
│ care_gap_id FK               │ UUID → care_gaps.id
│ tenant_id                    │ VARCHAR(100)
│ closure_date                 │ TIMESTAMP
│ closure_method               │ VARCHAR(100)
│ evidence                     │ TEXT
│ closed_by                    │ VARCHAR(100)
│ created_at                   │ TIMESTAMP
│ updated_at                   │ TIMESTAMP
└──────────────────────────────┘
```

---

## Database 7: Analytics Service (analytics_db)

### analytics_metrics
```
┌──────────────────────────────┐
│ analytics_metrics            │
├──────────────────────────────┤
│ id PK                        │ UUID
│ tenant_id                    │ VARCHAR(100) + INDEX
│ metric_name                  │ VARCHAR(255)
│ metric_value                 │ DOUBLE
│ metric_type                  │ VARCHAR(50)
│ period_start                 │ DATE + INDEX
│ period_end                   │ DATE + INDEX
│ created_at                   │ TIMESTAMP
│ updated_at                   │ TIMESTAMP
└──────────────────────────────┘
```

### analytics_reports
```
┌──────────────────────────────┐
│ analytics_reports            │
├──────────────────────────────┤
│ id PK                        │ UUID
│ tenant_id                    │ VARCHAR(100)
│ report_title                 │ VARCHAR(255)
│ report_data                  │ TEXT (JSON)
│ report_type                  │ VARCHAR(50)
│ generated_date               │ TIMESTAMP
│ created_at                   │ TIMESTAMP
│ updated_at                   │ TIMESTAMP
└──────────────────────────────┘
```

### star_ratings
```
┌──────────────────────────────┐
│ star_ratings                 │
├──────────────────────────────┤
│ id PK                        │ UUID
│ tenant_id                    │ VARCHAR(100)
│ contract_id                  │ VARCHAR(50)
│ rating_year                  │ INTEGER
│ overall_rating               │ DECIMAL(3,2)
│ domain_ratings               │ TEXT (JSON)
│ created_at                   │ TIMESTAMP
│ updated_at                   │ TIMESTAMP
└──────────────────────────────┘
```

---

## Database 8: Consent Service (consent_db)

### consents
```
┌──────────────────────────────┐
│ consents                     │
├──────────────────────────────┤
│ id PK                        │ UUID
│ tenant_id                    │ VARCHAR(100) + INDEX
│ patient_id                   │ VARCHAR(100) + INDEX
│ consent_type                 │ VARCHAR(50)
│ status                       │ VARCHAR(20) (ACTIVE, REVOKED)
│ consent_date                 │ TIMESTAMP
│ expiration_date              │ TIMESTAMP
│ revocation_date              │ TIMESTAMP
│ granted_by                   │ VARCHAR(255)
│ created_at                   │ TIMESTAMP
│ updated_at                   │ TIMESTAMP
└──────────────────────────────┘
  │
  │ 1:N
  ├────────────────────────────────────────┐
  │                                        │
  ↓                                        ↓
┌──────────────────────────┐   ┌──────────────────────┐
│ consent_policies         │   │ consent_history      │
├──────────────────────────┤   ├──────────────────────┤
│ id PK                    │   │ id PK                │
│ tenant_id                │   │ consent_id FK        │
│ policy_name              │   │ tenant_id            │
│ policy_description       │   │ action               │
│ required                 │   │ changed_by           │
│ created_at               │   │ change_date          │
│ updated_at               │   │ created_at           │
└──────────────────────────┘   └──────────────────────┘
```

---

## Event Flow Diagram

### Example: HbA1c Test Result Processing

```
1. FHIR Observation Created
   ┌────────────────────────────┐
   │ observations               │
   │ - patient_id: P123         │
   │ - code: HbA1c              │
   │ - value: 9.5%              │
   │ - date: 2025-11-25         │
   └────────────────────────────┘
            │
            │ Publishes Event
            ↓
   ┌────────────────────────────┐
   │ events                     │
   │ - event_type: Observation  │
   │   Created                  │
   │ - aggregate_id: P123       │
   │ - correlation_id: C001     │
   └────────────────────────────┘
            │
            │ Event Processor
            ↓
   ┌────────────────────────────┐
   │ cql_evaluations            │
   │ - patient_id: P123         │
   │ - measure_id: CMS122       │
   │ - result: NON_COMPLIANT    │
   └────────────────────────────┘
            │
            │ Publishes Event
            ↓
   ┌────────────────────────────┐
   │ events                     │
   │ - event_type: Measure      │
   │   Calculated               │
   │ - causation_id: (prev evt) │
   │ - correlation_id: C001     │
   └────────────────────────────┘
            │
            │ Updates Read Model
            ↓
   ┌────────────────────────────┐
   │ quality_measure_results    │
   │ - patient_id: P123         │
   │ - measure_id: CMS122       │
   │ - numerator_compliant: F   │
   └────────────────────────────┘
            │
            │ Triggers Care Gap
            ↓
   ┌────────────────────────────┐
   │ care_gaps                  │
   │ - patient_id: P123         │
   │ - title: Diabetes Control  │
   │ - priority: HIGH           │
   │ - status: OPEN             │
   │ - measure_result_id: M456  │
   └────────────────────────────┘
            │
            │ Triggers Risk Reassessment
            ↓
   ┌────────────────────────────┐
   │ risk_assessments           │
   │ - patient_id: P123         │
   │ - risk_level: HIGH         │
   │   (was: MODERATE)          │
   │ - risk_factors: [HbA1c...]│
   └────────────────────────────┘
            │
            │ Triggers Health Score Update
            ↓
   ┌────────────────────────────┐
   │ health_scores              │
   │ - patient_id: P123         │
   │ - overall_score: 72        │
   │   (was: 78)                │
   │ - significant_change: T    │
   │ - chronic_disease_score: ↓ │
   └────────────────────────────┘
            │
            │ Historical Snapshot
            ↓
   ┌────────────────────────────┐
   │ health_score_history       │
   │ - patient_id: P123         │
   │ - overall_score: 72        │
   │ - score_delta: -6          │
   │ - calculated_at: NOW()     │
   └────────────────────────────┘
```

---

## Index Summary by Type

### Primary Keys (48 total)
All tables have UUID primary keys

### Tenant Isolation Indexes (43 total)
43/48 tables have tenant_id columns with indexes

### JSONB GIN Indexes (21 total)
1. Event Processing (1): dead_letter_queue.event_payload
2. FHIR (8): All resource_json columns
3. Quality Measure (5): cql_result, value_sets, risk_factors×3
4. CQL Engine (3): compiled_elm, result, context_data
5. Patient (2): factors, comorbidities
6. Custom Measures (1): value_sets

### Composite Indexes (30+ total)
- Events: (aggregate_type, aggregate_id, version)
- Quality Measure Results: (tenant_id, patient_id, measure_id, measure_year)
- Care Gaps: (patient_id, measure_result_id, status)
- Risk Assessments: (tenant_id, risk_level)
- Health Scores: (patient_id, calculated_at DESC)

### Partial Indexes (4 total)
- patients: WHERE deleted_at IS NULL
- events: WHERE causation_id IS NOT NULL
- events: WHERE user_id IS NOT NULL
- encounters: WHERE duration_minutes IS NOT NULL

---

**ERD Documentation Prepared By:** Data Architecture Team
**Date:** November 25, 2025
**Total Tables:** 48
**Total Indexes:** 150+
**Total GIN Indexes:** 21
