# HDIM Demo Platform Architecture

**Version**: 1.0
**Last Updated**: January 3, 2026
**Purpose**: Enable professional video demonstrations of HDIM capabilities for healthcare payer/ACO audiences

---

## Executive Summary

The HDIM Demo Platform provides a complete, production-like environment optimized for creating compelling video demonstrations. It features:

- **Realistic synthetic patient data** (FHIR R4 compliant)
- **Pre-configured demo scenarios** for HEDIS, patient journeys, risk stratification, and multi-tenant workflows
- **One-command reset capability** to restore demo state between recordings
- **Performance-optimized** for smooth video capture
- **Demo mode UI enhancements** with highlights and guided workflows

**Target Audience**: Healthcare payers, ACOs, and value-based care organizations

---

## Demo Scenarios

### Scenario 1: HEDIS Quality Measure Evaluation & Care Gaps
**Duration**: 3-5 minutes
**Value Proposition**: Automate quality measure evaluation, identify care gaps at scale

**Demo Flow**:
1. Login as "Acme Health (acme-health)" quality director
2. Dashboard shows 5,000 attributed patients
3. Run BCS (Breast Cancer Screening) measure evaluation
4. System identifies 247 care gaps in real-time
5. Drill into specific patient (Sarah Martinez, age 52)
6. View care gap details, recommended interventions
7. Generate member outreach list
8. Show QRDA I/III export for CMS submission

**Key Metrics to Highlight**:
- Evaluation time: 5,000 patients in 12 seconds
- Care gap closure rate: 18% improvement projected
- HEDIS score impact: +3.2 points (BCS measure)

### Scenario 2: Patient Care Journey
**Duration**: 4-6 minutes
**Value Proposition**: 360° patient view with clinical decision support

**Demo Flow**:
1. Search for patient "Michael Chen" (diabetic, HCC risk score 2.4)
2. View comprehensive FHIR timeline:
   - 3 recent encounters
   - 2 active conditions (Type 2 Diabetes, Hypertension)
   - 5 medications
   - Lab results (A1C trending up)
3. System flags 4 active care gaps:
   - HbA1c Poor Control (CDC-E)
   - Annual Eye Exam (EED)
   - Statin Therapy (SPC)
   - Blood Pressure Control (CBP)
4. Review SDOH screening (food insecurity detected)
5. View recommended interventions
6. Show care team coordination
7. Track consent for data sharing

**Patient Personas**:
- Michael Chen: Complex diabetic patient
- Sarah Martinez: Preventive care gap
- Emma Johnson: High HCC risk, multiple comorbidities
- Carlos Rodriguez: SDOH barriers

### Scenario 3: Risk Stratification & Predictive Analytics
**Duration**: 3-4 minutes
**Value Proposition**: Identify high-risk patients before costly events

**Demo Flow**:
1. Open Analytics Dashboard
2. View population risk distribution (pyramid chart)
3. Filter to high-risk cohort (HCC > 2.0)
4. Show predictive analytics:
   - 87 patients at risk for hospital admission (next 90 days)
   - Estimated cost impact: $1.2M
5. Drill into patient "Emma Johnson" (HCC 3.7)
6. View risk factors and intervention opportunities
7. Show ROI calculator for preventive interventions
8. Generate care management worklist

**Analytics to Show**:
- Risk stratification accuracy: 92%
- Cost avoidance potential: $450K annually
- Care gap closure ROI: 3.2x

### Scenario 4: Multi-Tenant Administration
**Duration**: 2-3 minutes
**Value Proposition**: Secure, scalable SaaS platform

**Demo Flow**:
1. Login as super admin
2. Show 3 demo tenants:
   - Acme Health Plan (5,000 patients)
   - Summit Care ACO (12,000 patients)
   - Valley Health System (8,500 patients)
3. Switch tenant context to "Summit Care ACO"
4. Verify data isolation (only Summit patients visible)
5. Show tenant-specific configuration:
   - Custom quality measures
   - Integration settings
   - User roles and permissions
6. Demonstrate audit logging
7. Show tenant usage metrics

---

## Technical Architecture

### Component Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Demo Platform Stack                       │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────┐  ┌──────────────────┐                 │
│  │  Demo CLI Tool   │  │  Admin Portal    │                 │
│  │  (Reset/Seed)    │  │  (UI Enhanced)   │                 │
│  └────────┬─────────┘  └────────┬─────────┘                 │
│           │                     │                            │
│           v                     v                            │
│  ┌──────────────────────────────────────────┐               │
│  │     Demo Data Seeding Service            │               │
│  │  - Synthetic Patient Generator           │               │
│  │  - Scenario Script Engine                │               │
│  │  - State Management                      │               │
│  └────────┬─────────────────────────────────┘               │
│           │                                                  │
│           v                                                  │
│  ┌──────────────────────────────────────────┐               │
│  │       Core HDIM Services                 │               │
│  │  - FHIR Service                          │               │
│  │  - Patient Service                       │               │
│  │  - Quality Measure Service               │               │
│  │  - Care Gap Service                      │               │
│  │  - Analytics Service                     │               │
│  │  - HCC Service                           │               │
│  └────────┬─────────────────────────────────┘               │
│           │                                                  │
│           v                                                  │
│  ┌──────────────────────────────────────────┐               │
│  │     PostgreSQL (Demo Database)           │               │
│  │  - acme-health (Acme Health)             │               │
│  │  - summit-care (Summit Care)             │               │
│  │  - valley-health (Valley Health)         │               │
│  └──────────────────────────────────────────┘               │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

### Smart Routing & Authoritative Sources

**Authoritative sources**:
- **Patient**: external/customer FHIR server (authoritative).
- **Generated resources from Quality Measure/CQL processing** (MeasureReport, derived Observation/Condition/Procedure, etc.):
  internal advanced FHIR for versioned history.

**Routing rules**:
- **Reads**
  - Patient: external FHIR
  - Generated resources: internal advanced FHIR
- **Writes**
  - Patient: external FHIR only
  - Generated resources: internal advanced FHIR only (versioned)

**Tenant alignment**:
- Demo tenant IDs include `demo-tenant` (primary) and `acme-health` (payer demo).

### External FHIR Access Requirements

Outbound calls to external FHIR servers must be tenant-aware and auditable:
- Per-tenant endpoint and credentials (basic, bearer, OAuth2).
- Fail-closed if tenant mapping is missing.
- Audit/provenance for outbound reads and writes.

### New Components to Build

#### 1. Demo Data Seeding Service
**Location**: `backend/modules/services/demo-seeding-service/`

**Responsibilities**:
- Generate synthetic FHIR-compliant patient data
- Seed quality measures and care gaps
- Manage demo scenario state
- Reset demo data to baseline
- Track demo session state

**API Endpoints**:
```
POST   /api/v1/demo/reset                     # Reset all demo data
POST   /api/v1/demo/scenarios/{scenarioId}    # Load specific scenario
GET    /api/v1/demo/scenarios                 # List available scenarios
POST   /api/v1/demo/patients/generate         # Generate synthetic patients
GET    /api/v1/demo/status                    # Current demo state
```

**Database Schema**:
```sql
-- Demo scenarios metadata
CREATE TABLE demo_scenarios (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    scenario_type VARCHAR(50),  -- HEDIS, PATIENT_JOURNEY, RISK, ADMIN
    data_snapshot JSONB,
    created_at TIMESTAMP WITH TIME ZONE,
    version INTEGER
);

-- Demo session tracking
CREATE TABLE demo_sessions (
    id UUID PRIMARY KEY,
    scenario_id UUID REFERENCES demo_scenarios(id),
    started_at TIMESTAMP WITH TIME ZONE,
    last_reset_at TIMESTAMP WITH TIME ZONE,
    session_state JSONB
);

-- Synthetic patient templates
CREATE TABLE synthetic_patient_templates (
    id UUID PRIMARY KEY,
    persona_name VARCHAR(255),  -- "Complex Diabetic", "Preventive Gap", etc.
    fhir_bundle JSONB,
    care_gaps JSONB,
    hcc_score DECIMAL(3,2),
    template_version INTEGER
);
```

#### 2. Demo CLI Tool
**Location**: `backend/tools/demo-cli/`

**Commands**:
```bash
# Reset demo to clean state
./demo-cli reset

# Load specific scenario
./demo-cli load-scenario hedis-evaluation

# Generate synthetic patients
./demo-cli generate-patients --count 5000 --tenant acme-health

# List scenarios
./demo-cli list-scenarios

# Check demo status
./demo-cli status

# Create demo snapshot
./demo-cli snapshot create "before-video-1"

# Restore snapshot
./demo-cli snapshot restore "before-video-1"
```

**Technology**: Spring Boot CLI application

#### 3. Synthetic Patient Generator
**Location**: `backend/modules/services/demo-seeding-service/src/main/java/com/healthdata/demo/generator/`

**Features**:
- FHIR R4 compliant patient resources
- Realistic names (using name libraries)
- Age/gender distribution matching US demographics
- Condition prevalence matching real-world rates
- Medication patterns for chronic conditions
- Lab results with clinically plausible ranges
- Encounter patterns (office visits, ER, hospital)

**Patient Personas** (Pre-defined Templates):

1. **Complex Diabetic** (Michael Chen):
   - Type 2 Diabetes (E11.9)
   - Hypertension (I10)
   - A1C trending up (7.2% → 8.1%)
   - Multiple care gaps
   - HCC score: 2.4

2. **Preventive Care Gap** (Sarah Martinez):
   - Age 52, overdue for mammogram
   - Otherwise healthy
   - Single BCS care gap
   - HCC score: 0.8

3. **High Risk Multi-Morbid** (Emma Johnson):
   - CHF (I50.9)
   - COPD (J44.9)
   - CKD Stage 3 (N18.3)
   - Diabetes (E11.9)
   - 6 active medications
   - HCC score: 3.7
   - Predicted admission risk: 78%

4. **SDOH Barriers** (Carlos Rodriguez):
   - Diabetes (E11.9)
   - Food insecurity (Z59.4)
   - Housing instability (Z59.0)
   - Medication non-adherence
   - HCC score: 2.1

**Generation Strategy**:
```java
public class SyntheticPatientGenerator {

    public List<Patient> generateCohort(int count, TenantContext tenant) {
        // 60% low-risk (HCC < 1.0)
        // 30% moderate-risk (HCC 1.0-2.0)
        // 10% high-risk (HCC > 2.0)

        // Condition prevalence:
        // - Diabetes: 12%
        // - Hypertension: 30%
        // - CHF: 3%
        // - COPD: 6%
        // - CKD: 8%
    }

    public Patient generateFromTemplate(PatientTemplate template) {
        // Use template as base, randomize specific values
    }
}
```

#### 4. Demo Mode UI Enhancements
**Location**: `frontend/src/app/demo-mode/`

**Features**:

**A. Demo Mode Toggle**:
```typescript
// src/app/services/demo-mode.service.ts
@Injectable()
export class DemoModeService {
  private isDemoMode$ = new BehaviorSubject<boolean>(false);

  enableDemoMode() {
    // Enable tooltips, highlights, simplified navigation
  }

  disableDemoMode() {
    // Return to production UI
  }
}
```

**B. Visual Enhancements**:
- **Tooltips**: Explain what each metric means (hover to see)
- **Highlights**: Subtle glow on key data points
- **Guided Flows**: Step-by-step wizards for complex workflows
- **Performance Metrics**: Show processing times prominently
- **Success Animations**: Celebrate successful actions (care gap closure, etc.)

**C. Demo Navigation Bar**:
```html
<div class="demo-control-bar" *ngIf="demoMode">
  <button (click)="resetScenario()">Reset Scenario</button>
  <select [(ngModel)]="currentScenario">
    <option value="hedis">HEDIS Evaluation</option>
    <option value="patient-journey">Patient Journey</option>
    <option value="risk">Risk Stratification</option>
    <option value="admin">Multi-Tenant</option>
  </select>
  <span class="demo-timer">Recording: {{recordingTime}}</span>
</div>
```

**D. Demo-Specific Routes**:
```typescript
const routes: Routes = [
  { path: 'demo/hedis-evaluation', component: HedisEvaluationDemoComponent },
  { path: 'demo/patient-journey/:patientId', component: PatientJourneyDemoComponent },
  { path: 'demo/risk-stratification', component: RiskStratificationDemoComponent },
  { path: 'demo/multi-tenant', component: MultiTenantDemoComponent }
];
```

---

## Demo Data Specifications

### Tenant Configurations

#### Tenant 1: Acme Health Plan
- **Tenant ID**: acme-health
- **Type**: Regional Health Plan
- **Patients**: 5,000
- **Focus**: HEDIS quality measures
- **Active Measures**: BCS, COL, CBP, CDC, EED, SPC
- **Care Gaps**: 1,247 total
- **Users**:
  - demo_admin@acmehealth.com (ADMIN)
  - demo_evaluator@acmehealth.com (EVALUATOR)
  - demo_analyst@acmehealth.com (ANALYST)

#### Tenant 2: Summit Care ACO
- **Type**: Accountable Care Organization
- **Patients**: 12,000
- **Focus**: Risk adjustment and care management
- **Active Measures**: All HEDIS + Custom CQL measures
- **Care Gaps**: 3,142 total
- **Users**:
  - demo_admin@summitcare.com (ADMIN)
  - demo_evaluator@summitcare.com (EVALUATOR)

#### Tenant 3: Valley Health System
- **Type**: Integrated Delivery Network
- **Patients**: 8,500
- **Focus**: Population health and SDOH
- **Active Measures**: HEDIS + SDOH screening
- **Care Gaps**: 2,089 total
- **Users**:
  - demo_admin@valleyhealth.com (ADMIN)

### Tenant Validation (Acme Health)

Validate the Acme Health tenant after seeding:

1) Generate patients for acme-health (demo seeding service):
```bash
curl -X POST http://localhost:8098/demo/api/v1/demo/patients/generate \
  -H "Content-Type: application/json" \
  -d '{"count":5000,"tenantId":"acme-health","careGapPercentage":28}'
```

2) Verify access through the gateway:
```bash
curl -s "http://localhost:8080/fhir/Patient?_count=1" -H "X-Tenant-ID: acme-health"
curl -s "http://localhost:8080/care-gap/api/v1/care-gaps?tenantId=acme-health" -H "X-Tenant-ID: acme-health"
```

3) Optional database spot-check:
```sql
SELECT COUNT(*) FROM patients WHERE tenant_id = 'acme-health';
SELECT COUNT(*) FROM care_gaps WHERE tenant_id = 'acme-health';
```

### Quality Measures (Pre-configured)

| Measure | Code | Description | Demo Gap Count |
|---------|------|-------------|----------------|
| Breast Cancer Screening | BCS | Women 50-74 mammogram | 247 |
| Colorectal Cancer Screening | COL | Adults 50-75 colonoscopy | 412 |
| Blood Pressure Control | CBP | BP < 140/90 for hypertensives | 189 |
| Diabetes HbA1c Control | CDC-E | A1C < 8% for diabetics | 156 |
| Eye Exam for Diabetics | EED | Annual retinal exam | 203 |
| Statin Therapy | SPC | Diabetics 40-75 on statin | 178 |

### Synthetic Patient Distribution

**Age Distribution**:
- 18-44: 25%
- 45-64: 40%
- 65+: 35%

**Gender**:
- Female: 52%
- Male: 48%

**Race/Ethnicity** (US Census approximation):
- White: 60%
- Hispanic/Latino: 18%
- Black/African American: 13%
- Asian: 6%
- Other: 3%

**Condition Prevalence**:
- Diabetes: 12% (600 patients out of 5,000)
- Hypertension: 30% (1,500 patients)
- CHF: 3% (150 patients)
- COPD: 6% (300 patients)
- CKD: 8% (400 patients)
- Cancer (history): 5% (250 patients)

**HCC Risk Distribution**:
- Low risk (< 1.0): 60% (3,000 patients)
- Moderate risk (1.0-2.0): 30% (1,500 patients)
- High risk (> 2.0): 10% (500 patients)

---

## Performance Optimization

### Target Performance Metrics

| Metric | Target | Purpose |
|--------|--------|---------|
| Dashboard load time | < 2 seconds | First impression |
| Quality measure evaluation | < 15 seconds for 5K patients | Show real-time capability |
| Patient search | < 500ms | Instant results |
| Patient detail load | < 1 second | Smooth navigation |
| Care gap query | < 2 seconds | Fast drill-down |
| Chart rendering | < 1 second | Visual impact |

### Optimization Strategies

#### 1. Database Pre-warming
```sql
-- Run before demo to cache frequently accessed data
SELECT * FROM patients WHERE tenant_id = 'acme-health' LIMIT 100;
SELECT * FROM care_gaps WHERE tenant_id = 'acme-health';
SELECT * FROM quality_measures WHERE tenant_id = 'acme-health';
```

#### 2. Redis Caching Strategy
```yaml
# Demo-specific cache configuration
spring:
  cache:
    redis:
      time-to-live: 3600000  # 1 hour for demo (vs 300s for production PHI)
      cache-null-values: false

# Pre-cache demo data
demo:
  cache:
    preload:
      - patients
      - care-gaps
      - quality-measures
      - analytics-dashboards
```

#### 3. Database Indexes
```sql
-- Ensure optimal indexes for demo queries
CREATE INDEX idx_patients_tenant_id ON patients(tenant_id);
CREATE INDEX idx_care_gaps_patient_id ON care_gaps(patient_id);
CREATE INDEX idx_care_gaps_measure_id ON care_gaps(measure_id);
CREATE INDEX idx_quality_evaluations_tenant ON quality_evaluations(tenant_id, evaluation_date DESC);
```

#### 4. Connection Pooling
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 5000
```

#### 5. Frontend Optimizations
- Pre-fetch patient list while showing dashboard
- Lazy load charts (render on scroll)
- Use virtual scrolling for long lists
- Cache static assets aggressively
- Use production build (AOT compilation)

---

## Demo Platform Deployment

### Environment Setup

**Option 1: Dedicated Demo Server** (Recommended)
```yaml
# docker-compose.demo.yml
version: '3.8'

services:
  demo-postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: healthdata_demo
    volumes:
      - ./demo-data-snapshots:/snapshots
    ports:
      - "5436:5432"

  demo-redis:
    image: redis:7
    ports:
      - "6381:6379"

  demo-seeding-service:
    build: ./backend/modules/services/demo-seeding-service
    environment:
      SPRING_PROFILES_ACTIVE: demo
    depends_on:
      - demo-postgres

  # All core services with demo profile
  fhir-service:
    environment:
      SPRING_PROFILES_ACTIVE: demo

  # ... other services
```

**Option 2: Local Development with Demo Profile**
```bash
# Start with demo profile
docker compose -f docker-compose.demo.yml up -d

# Run demo seeding
./backend/tools/demo-cli/demo-cli reset
./backend/tools/demo-cli/demo-cli load-scenario hedis-evaluation
```

### Demo Data Snapshots

**Create baseline snapshots** for quick reset:
```bash
# Create snapshot after seeding
./demo-cli snapshot create "baseline-hedis"
./demo-cli snapshot create "baseline-patient-journey"
./demo-cli snapshot create "baseline-risk"

# Snapshots stored as SQL dumps
ls -l demo-data-snapshots/
  baseline-hedis-2026-01-03.sql
  baseline-patient-journey-2026-01-03.sql
  baseline-risk-2026-01-03.sql
```

**Fast restore** (< 30 seconds):
```bash
./demo-cli snapshot restore "baseline-hedis"
# Drops demo schema, restores from snapshot, clears caches
```

---

## Video Recording Guide

### Pre-Recording Checklist

- [ ] Demo environment running (all services healthy)
- [ ] Demo scenario loaded
- [ ] Browser cache cleared
- [ ] Demo mode enabled in UI
- [ ] Screen resolution set to 1920x1080
- [ ] Browser zoom at 100%
- [ ] Notifications disabled
- [ ] Bookmarks bar hidden
- [ ] Demo user logged in
- [ ] Test run through script completed
- [ ] License compliance verified (docs/compliance/THIRD_PARTY_NOTICES.md)

### Recording Setup

**Recommended Tools**:
- **Screen Recording**: OBS Studio, Loom, or ScreenFlow
- **Resolution**: 1920x1080 (Full HD)
- **Frame Rate**: 30 fps
- **Browser**: Chrome (best performance)
- **Window Size**: Maximized, hide browser UI (F11)

**Audio**:
- Use external microphone (not laptop mic)
- Record narration separately, sync in editing
- Alternatively, do voiceover after recording

### Recording Tips

1. **Slow down**: Move mouse slowly and deliberately
2. **Pause**: Give viewers time to read metrics
3. **Highlight**: Use demo mode tooltips to explain
4. **Smooth transitions**: Wait for page loads to complete
5. **Consistent pacing**: 3-5 seconds per screen
6. **Reset between takes**: Use quick reset for retries

### Scenario Scripts

Detailed click-by-click scripts for each scenario are in:
- `docs/demo-scripts/HEDIS_EVALUATION_SCRIPT.md`
- `docs/demo-scripts/PATIENT_JOURNEY_SCRIPT.md`
- `docs/demo-scripts/RISK_STRATIFICATION_SCRIPT.md`
- `docs/demo-scripts/MULTI_TENANT_SCRIPT.md`

---

## Security Considerations

### Demo Data Privacy

**NO REAL PHI**: All demo data is synthetically generated and HIPAA-compliant:
- Synthetic names from public name libraries
- Randomized birthdates
- Fake addresses, phone numbers, emails
- No real patient identifiers

### Demo Environment Isolation

```yaml
# Separate database for demo
demo:
  database:
    host: demo-postgres
    name: healthdata_demo  # NOT production database

# Demo-specific users (cannot access production)
demo:
  users:
    - username: demo_admin@acmehealth.com
      tenant_access: [demo-tenant, acme-health]
      production_access: false
```

### Public Demo Considerations

If hosting publicly accessible demo:
- [ ] No authentication required (read-only demo mode)
- [ ] Rate limiting (prevent abuse)
- [ ] Automated reset every 24 hours
- [ ] Monitoring and alerting
- [ ] Clear disclaimer: "Demo data only"

---

## Maintenance & Updates

### Demo Data Refresh

**Quarterly**: Update synthetic data to reflect:
- New quality measures
- Updated clinical guidelines
- New FHIR resources
- Platform features

### Scenario Updates

**As features ship**: Create new demo scenarios:
- New analytics capabilities
- New integrations
- New workflows

### Performance Monitoring

**Track demo performance**:
```sql
-- Log demo session performance
CREATE TABLE demo_performance_metrics (
    id UUID PRIMARY KEY,
    scenario_id UUID,
    metric_name VARCHAR(100),
    metric_value DECIMAL(10,2),
    recorded_at TIMESTAMP WITH TIME ZONE
);

-- Example metrics
INSERT INTO demo_performance_metrics VALUES
  (uuid_generate_v4(), 'hedis-eval', 'evaluation_time_seconds', 12.4, now()),
  (uuid_generate_v4(), 'hedis-eval', 'patient_load_time_ms', 847, now());
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1)
- [ ] Create demo database schema
- [ ] Build demo seeding service skeleton
- [ ] Implement synthetic patient generator
- [ ] Create demo CLI tool
- [ ] Set up demo Docker environment

### Phase 2: Scenarios (Week 2)
- [ ] Implement HEDIS evaluation scenario
- [ ] Implement patient journey scenario
- [ ] Implement risk stratification scenario
- [ ] Implement multi-tenant scenario
- [ ] Create scenario scripts

### Phase 3: UI Enhancements (Week 3)
- [ ] Build demo mode toggle
- [ ] Add tooltips and highlights
- [ ] Create demo navigation
- [ ] Optimize performance
- [ ] Test full workflows

### Phase 4: Polish & Documentation (Week 4)
- [ ] Create recording guide
- [ ] Record practice videos
- [ ] Performance tuning
- [ ] Create snapshot system
- [ ] Final testing

---

## Success Metrics

**Demo Platform Quality**:
- [ ] All scenarios complete in < 5 minutes
- [ ] Performance targets met (dashboard < 2s, etc.)
- [ ] Zero errors during recording
- [ ] Reset time < 30 seconds
- [ ] UI is polished and professional

**Video Quality**:
- [ ] Clear value proposition in first 30 seconds
- [ ] Key metrics highlighted effectively
- [ ] Smooth, professional pacing
- [ ] No technical glitches visible
- [ ] Compelling narrative flow

---

## Appendix

### Demo User Credentials

| Tenant | Username | Password | Role |
|--------|----------|----------|------|
| Acme Health | demo_admin@acmehealth.com | Demo2026! | ADMIN |
| Acme Health | demo_evaluator@acmehealth.com | Demo2026! | EVALUATOR |
| Summit Care | demo_admin@summitcare.com | Demo2026! | ADMIN |
| Valley Health | demo_admin@valleyhealth.com | Demo2026! | ADMIN |

### Demo URLs

| Service | URL |
|---------|-----|
| Demo Portal | http://localhost:4200?demo=true |
| Demo Admin API | http://localhost:8099/demo/api/v1 |
| Demo Seeding Service | http://localhost:8098 |

### Support Resources

- **Demo Platform Issues**: Create ticket with `[DEMO]` prefix
- **Recording Questions**: Contact sales enablement team
- **Data Refresh Requests**: Submit to engineering team

---

**Document Owner**: Engineering Team
**Review Cadence**: Quarterly
**Next Review**: April 2026
