# HDIM Demo Environment Guide

## Setting Up and Running Product Demonstrations

---

## Quick Start

### Starting the Demo Environment

```bash
# Start all services with demo data
docker compose --profile core up -d

# Verify all services are healthy
docker ps --format "table {{.Names}}\t{{.Status}}" | grep healthdata
```

### Accessing the Demo

| Component | URL | Credentials |
|-----------|-----|-------------|
| Clinical Portal | http://localhost:4200 | demo@hdim.io / Demo123! |
| Gateway API | http://localhost:8080 | (API access) |
| Jaeger Tracing | http://localhost:16686 | (no auth) |

---

## Demo Data Overview

### Sample Organizations

| Organization | Type | Patients | Providers |
|--------------|------|----------|-----------|
| Beacon ACO | ACO | 12,500 | 47 |
| Valley Health System | Health System | 8,200 | 32 |
| Community Care Network | FQHC | 4,300 | 18 |

### Sample Patients

The demo environment includes 25,000 synthetic patients with:
- Realistic demographics
- Multi-year clinical history
- HEDIS measure eligibility
- Care gaps (open and closed)
- Mental health screening data

### Quality Measures Pre-Configured

| Measure | Baseline Rate | Current Rate | Gap Count |
|---------|---------------|--------------|-----------|
| Breast Cancer Screening (BCS) | 68% | 87% | 342 |
| Colorectal Cancer Screening (COL) | 61% | 79% | 518 |
| Controlling Blood Pressure (CBP) | 72% | 84% | 412 |
| Diabetes HbA1c Control (HBD) | 65% | 81% | 287 |
| PHQ-9 Depression Screening | 42% | 78% | 156 |
| Medication Adherence - Diabetes | 74% | 88% | 203 |

---

## Demo Scenarios

### Scenario 1: Executive Dashboard Overview (5 min)

**Objective:** Show high-level quality visibility

**Steps:**
1. Login to Clinical Portal
2. Navigate to Dashboard
3. Highlight key metrics:
   - Overall quality score (87%)
   - Open care gaps (1,247)
   - Trend improvement (+23% this year)
4. Click into "At Risk" measures
5. Show real-time updates

**Talking Points:**
- "This replaces 40 hours of monthly reporting"
- "Data is real-time, not 3 months old"
- "One dashboard for your entire organization"

---

### Scenario 2: Care Gap Management (10 min)

**Objective:** Demonstrate gap identification and closure workflow

**Steps:**
1. Navigate to Care Gaps section
2. Filter by "Breast Cancer Screening"
3. Sort by "Days Overdue"
4. Click on patient "Maria Garcia"
5. Review patient details:
   - Last screening: 18 months ago
   - Risk level: High
   - Upcoming appointment: Next week
6. Create outreach task
7. Demonstrate task assignment

**Talking Points:**
- "No more spreadsheet tracking"
- "See exactly who needs what, when"
- "Integrated with your care coordination workflow"

---

### Scenario 3: Mental Health Screening (10 min)

**Objective:** Show PHQ-9/GAD-7 integration

**Steps:**
1. Navigate to Behavioral Health section
2. Show screening dashboard
3. Click on a patient with positive PHQ-9
4. Review screening history and trend
5. Show automatic alert workflow
6. Demonstrate severity classification

**Sample Patient: "James Thompson"**
- PHQ-9 Score: 14 (Moderate Depression)
- Previous scores: 8 → 11 → 14 (trending up)
- Auto-alert triggered for care coordinator
- Follow-up scheduled within 7 days

**Talking Points:**
- "50% of depression goes undiagnosed"
- "Automatic scoring and documentation"
- "No patient falls through the cracks"
- "This feature is unique to HDIM"

---

### Scenario 4: Provider Performance (10 min)

**Objective:** Show provider-level quality visibility

**Steps:**
1. Navigate to Provider Analytics
2. Select "Dr. Sarah Chen"
3. Review quality scorecard:
   - Overall score: 91%
   - Measure-by-measure performance
   - Comparison to peers
4. Show patient gap list for provider
5. Demonstrate provider notification

**Talking Points:**
- "Providers need to see their data"
- "Benchmarking drives improvement"
- "Actionable lists, not just reports"

---

### Scenario 5: Multi-EHR Integration (5 min)

**Objective:** Demonstrate cross-system data aggregation

**Steps:**
1. Navigate to System Settings > Integrations
2. Show connected EHR systems
3. Click on a patient with multi-source data
4. Highlight data from different sources:
   - Epic (primary care)
   - Cerner (hospital)
   - Lab (Quest)
5. Show unified quality view

**Talking Points:**
- "FHIR-native architecture"
- "Works with any EHR"
- "Unified view across your network"
- "90-day implementation"

---

## Demo Preparation Checklist

### Before the Demo

- [ ] Verify all Docker containers are healthy
- [ ] Login to Clinical Portal and confirm data loads
- [ ] Test each scenario flow
- [ ] Prepare backup screenshots in case of issues
- [ ] Know your audience (CMO, CIO, Quality Director)
- [ ] License compliance verified (docs/compliance/THIRD_PARTY_NOTICES.md)

### Technical Requirements

| Requirement | Specification |
|-------------|---------------|
| Browser | Chrome (latest) or Edge |
| Resolution | 1920x1080 minimum |
| Network | Stable connection for screen share |
| Docker RAM | 8GB minimum |

### Environment Reset

```bash
# Reset demo data to default state
docker compose --profile core down
docker volume prune -f
docker compose --profile core up -d
```

---

## Common Demo Questions & Answers

### Technical

**Q: How long does implementation take?**
A: "90 days for full deployment. We've had customers go live in 60 days for targeted use cases."

**Q: What EHR systems do you support?**
A: "Any EHR with FHIR R4 APIs - Epic, Cerner, Athena, Meditech, and 50+ others. We also support legacy HL7 if needed."

**Q: Is data real-time?**
A: "Yes. FHIR connections provide real-time data. Some EHRs may have 15-minute intervals, but never batch delays."

**Q: How do you handle PHI security?**
A: "HIPAA-compliant architecture, encryption at rest and in transit, role-based access control, and full audit logging."

### Business

**Q: What's the pricing?**
A: "We have tiered pricing starting at $500/month for smaller organizations. For your size, I'd estimate [reference PRICING_STRATEGY.md]. Happy to provide a detailed quote."

**Q: What's the typical ROI?**
A: "Our customers see 3,500-7,000% ROI in year one. Beacon ACO captured $2.4M in additional shared savings on a $48K investment."

**Q: How do you compare to [competitor]?**
A: "[Reference battle cards] The key differences are..."

---

## Troubleshooting

### Services Not Starting

```bash
# Check logs
docker logs healthdata-gateway-service

# Restart specific service
docker compose restart gateway-service
```

### Data Not Loading

```bash
# Verify database
docker exec healthdata-postgres psql -U healthdata -c "SELECT COUNT(*) FROM patients;"

# Check API health
curl http://localhost:8080/actuator/health
```

### Portal Not Accessible

```bash
# Check nginx
docker logs healthdata-clinical-portal

# Verify port binding
docker ps | grep 4200
```

---

## Post-Demo Follow-Up

### Send Within 24 Hours

1. Thank you email with:
   - Link to recorded demo (if applicable)
   - Relevant case study (match to their use case)
   - Pricing overview
   - Next steps proposal

2. Attach:
   - Executive summary one-pager
   - Technical architecture diagram
   - Security & compliance overview

### Schedule

- [ ] Follow-up call (within 1 week)
- [ ] Technical deep-dive (if requested)
- [ ] Pilot proposal (if qualified)

---

## Demo Resources

| Resource | Location |
|----------|----------|
| Case Studies | `docs/gtm/case-studies/` |
| Battle Cards | `docs/gtm/battle-cards/` |
| Pricing | `docs/gtm/PRICING_STRATEGY.md` |
| Competitive Analysis | `docs/gtm/COMPETITIVE_ANALYSIS.md` |
| Video Script | `docs/gtm/VIDEO_SCRIPT.md` |
