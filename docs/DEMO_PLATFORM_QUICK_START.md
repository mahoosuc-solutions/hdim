# HDIM Demo Platform - Quick Start Guide

**Last Updated**: January 3, 2026
**Status**: Implementation Ready
**For**: Developers, Sales, Marketing

---

## What is the Demo Platform?

A complete environment for creating professional video demonstrations of HDIM capabilities, featuring:

- **Realistic synthetic patient data** (FHIR R4 compliant, no real PHI)
- **4 pre-configured demo scenarios** (HEDIS, Patient Journey, Risk, Multi-Tenant)
- **One-command reset** (< 30 seconds between recordings)
- **Performance-optimized** for smooth video capture
- **Demo mode UI** with tooltips and highlights

---

## Documentation Index

| Document | Purpose | Audience |
|----------|---------|----------|
| **DEMO_PLATFORM_EXECUTIVE_SUMMARY.md** | Business overview, ROI, status | Executives, Sales Leadership |
| **DEMO_PLATFORM_ARCHITECTURE.md** | Complete technical architecture | Architects, Senior Engineers |
| **DEMO_PLATFORM_IMPLEMENTATION_PLAN.md** | Detailed implementation roadmap | Development Team |
| **demo-scripts/HEDIS_EVALUATION_SCRIPT.md** | Word-for-word recording script | Sales, Marketing, Video Production |
| **DEMO_PLATFORM_QUICK_START.md** | This file - quick reference | Everyone |

---

## Quick Reference

### For Sales/Marketing (Using the Demo Platform)

**Before Recording**:
```bash
# 1. Start demo environment
docker compose -f docker-compose.demo.yml up -d

# 2. Load your scenario
./demo-cli load-scenario hedis-evaluation

# 3. Create snapshot (for quick retries)
./demo-cli snapshot create "my-recording-attempt-1"
```

**During Recording**:
1. Open: `http://localhost:4200?demo=true`
2. Login: `demo_evaluator@acmehealth.com` / `Demo2026!`
3. Follow script: `docs/demo-scripts/HEDIS_EVALUATION_SCRIPT.md`
4. Demo mode features:
   - Tooltips explain metrics on hover
   - Key metrics highlighted
   - Performance timers visible

**After Recording (If Retry Needed)**:
```bash
# Restore snapshot (30 seconds)
./demo-cli snapshot restore "my-recording-attempt-1"

# Or full reset (2-3 minutes)
./demo-cli reset
./demo-cli load-scenario hedis-evaluation
```

### For Developers (Building the Platform)

**Project Structure**:
```
hdim-master/
├── docs/
│   ├── DEMO_PLATFORM_*.md          # All documentation
│   └── demo-scripts/                # Recording scripts
│       └── HEDIS_EVALUATION_SCRIPT.md
│
├── backend/modules/services/demo-seeding-service/
│   ├── build.gradle.kts
│   └── src/main/java/com/healthdata/demo/
│       ├── DemoSeedingApplication.java
│       ├── api/                     # REST controllers (TODO)
│       ├── application/             # Business logic (TODO)
│       ├── domain/                  # Entities, repos (TODO)
│       ├── generator/
│       │   └── SyntheticPatientGenerator.java (PARTIAL)
│       └── config/                  # Configuration (TODO)
│
├── backend/tools/demo-cli/          # CLI tool (TODO)
│   ├── demo-cli.sh
│   └── src/main/java/.../
│
└── docker-compose.demo.yml          # Demo deployment (TODO)
```

**Implementation Status**:
- ✅ Architecture documented (82KB)
- ✅ HEDIS demo script complete (23KB)
- ✅ SyntheticPatientGenerator foundation (400 lines)
- ⏳ Domain models (TODO)
- ⏳ REST API (TODO)
- ⏳ Demo CLI tool (TODO)
- ⏳ Demo mode UI (TODO)

**Next to Implement** (Priority Order):
1. Complete `SyntheticPatientGenerator` (medications, observations, encounters, procedures)
2. Create domain models and repositories
3. Implement REST API endpoints
4. Build demo CLI tool
5. Create demo mode UI enhancements

**See**: `DEMO_PLATFORM_IMPLEMENTATION_PLAN.md` for detailed task breakdown

---

## Demo Scenarios Overview

### 1. HEDIS Quality Measure Evaluation ⭐ (Ready to Record)

**Who**: Healthcare Payer Quality Directors
**What**: Automate HEDIS evaluation, identify care gaps, generate outreach
**Duration**: 3-5 minutes
**Script**: `demo-scripts/HEDIS_EVALUATION_SCRIPT.md`

**Key Moments**:
- 0:30 - Navigate to BCS (Breast Cancer Screening) measure
- 1:30 - Run evaluation: 5,000 patients
- 2:00 - Results: 247 care gaps, 71.7% rate, 22x ROI
- 3:30 - Patient detail: Sarah Martinez, 8mo overdue
- 4:15 - Generate outreach campaign
- 4:45 - QRDA export for CMS

**Metrics Showcased**:
- Speed: 5,000 patients in 12 seconds
- Automation: 247 care gaps identified
- ROI: $5,754 → $127,000 (22x)

### 2. Patient Care Journey

**Who**: Care Management Teams
**What**: 360° patient view, clinical decision support, SDOH
**Duration**: 4-6 minutes
**Status**: Architecture complete, script pending

**Patient Personas**:
1. Michael Chen - Complex diabetic (HCC 2.4)
2. Sarah Martinez - Preventive gap (HCC 1.8)
3. Emma Johnson - High-risk (HCC 3.7)
4. Carlos Rodriguez - SDOH barriers (HCC 2.1)

### 3. Risk Stratification & Predictive Analytics

**Who**: Population Health Leaders
**What**: Identify high-risk patients, prevent costly events
**Duration**: 3-4 minutes
**Status**: Architecture complete, script pending

**Metrics**:
- 92% accuracy
- $450K cost avoidance
- 3.2x care gap ROI

### 4. Multi-Tenant Administration

**Who**: Technical Buyers, IT Leaders
**What**: Secure SaaS, data isolation, tenant management
**Duration**: 2-3 minutes
**Status**: Architecture complete, script pending

**Demo Tenants**:
1. Acme Health Plan (5,000 patients)
2. Summit Care ACO (12,000 patients)
3. Valley Health System (8,500 patients)

---

## Command Reference (When Implemented)

### Demo CLI Commands

```bash
# Reset entire demo environment
./demo-cli reset

# Load specific scenario
./demo-cli load-scenario hedis-evaluation
./demo-cli load-scenario patient-journey
./demo-cli load-scenario risk-stratification
./demo-cli load-scenario multi-tenant

# List available scenarios
./demo-cli list-scenarios

# Generate synthetic patients
./demo-cli generate-patients --count 5000 --tenant acme-health

# Check demo status
./demo-cli status

# Snapshot management
./demo-cli snapshot create "snapshot-name"
./demo-cli snapshot list
./demo-cli snapshot restore "snapshot-name"
```

### Docker Commands

```bash
# Start demo environment
docker compose -f docker-compose.demo.yml up -d

# Check service health
docker compose -f docker-compose.demo.yml ps

# View logs
docker compose -f docker-compose.demo.yml logs -f demo-seeding-service

# Stop demo environment
docker compose -f docker-compose.demo.yml down

# Rebuild after code changes
docker compose -f docker-compose.demo.yml up -d --build
```

### API Endpoints (When Implemented)

```bash
# Reset demo
curl -X POST http://localhost:8098/api/v1/demo/reset

# Load scenario
curl -X POST http://localhost:8098/api/v1/demo/scenarios/hedis-evaluation

# List scenarios
curl http://localhost:8098/api/v1/demo/scenarios

# Generate patients
curl -X POST http://localhost:8098/api/v1/demo/patients/generate \
  -H "Content-Type: application/json" \
  -d '{"count": 5000, "tenantId": "acme-health"}'

# Check status
curl http://localhost:8098/api/v1/demo/status

# Create snapshot
curl -X POST http://localhost:8098/api/v1/demo/snapshots \
  -d '{"name": "my-snapshot"}'

# Restore snapshot
curl -X POST http://localhost:8098/api/v1/demo/snapshots/123/restore
```

---

## Demo Users

### Test Credentials

| Tenant | Username | Password | Role |
|--------|----------|----------|------|
| Acme Health | demo_admin@acmehealth.com | Demo2026! | ADMIN |
| Acme Health | demo_evaluator@acmehealth.com | Demo2026! | EVALUATOR |
| Acme Health | demo_analyst@acmehealth.com | Demo2026! | ANALYST |
| Summit Care | demo_admin@summitcare.com | Demo2026! | ADMIN |
| Valley Health | demo_admin@valleyhealth.com | Demo2026! | ADMIN |

**Security Note**: These are demo credentials only. Not valid in production.

---

## Demo Data Specifications

### Synthetic Patient Distribution

**Total Patients**: 25,000+ across all tenants
- Acme Health Plan: 5,000
- Summit Care ACO: 12,000
- Valley Health System: 8,500

**Age Distribution**:
- 18-44: 25%
- 45-64: 40%
- 65+: 35%

**Gender**:
- Female: 52%
- Male: 48%

**HCC Risk**:
- Low (< 1.0): 60%
- Moderate (1.0-2.0): 30%
- High (> 2.0): 10%

**Condition Prevalence**:
- Diabetes: 12%
- Hypertension: 30%
- CHF: 3%
- COPD: 6%
- CKD: 8%

### Quality Measures Configured

| Measure | Code | Demo Care Gaps |
|---------|------|----------------|
| Breast Cancer Screening | BCS | 247 |
| Colorectal Cancer Screening | COL | 412 |
| Blood Pressure Control | CBP | 189 |
| Diabetes HbA1c Control | CDC-E | 156 |
| Eye Exam for Diabetics | EED | 203 |
| Statin Therapy | SPC | 178 |

---

## Performance Targets

| Metric | Target | Importance |
|--------|--------|------------|
| Dashboard load | < 2s | Critical |
| Quality evaluation (5K patients) | < 15s | Critical |
| Patient search | < 500ms | High |
| Patient detail load | < 1s | High |
| Care gap query | < 2s | High |
| Demo reset time | < 30s | Medium |

**Why It Matters**: Smooth performance = professional videos = better sales outcomes

---

## Recording Best Practices

### Before Recording

1. **Environment Setup**:
   - [ ] Demo services running (`docker compose ps`)
   - [ ] Scenario loaded (`./demo-cli load-scenario hedis-evaluation`)
   - [ ] Snapshot created (`./demo-cli snapshot create "attempt-1"`)
   - [ ] Browser cache cleared
   - [ ] Demo mode enabled (`?demo=true`)

2. **Technical Setup**:
   - [ ] Screen resolution: 1920x1080
   - [ ] Browser zoom: 100%
   - [ ] Notifications disabled
   - [ ] Bookmarks bar hidden
   - [ ] Recording software ready (OBS, Loom, ScreenFlow)

3. **Script Preparation**:
   - [ ] Script reviewed: `demo-scripts/HEDIS_EVALUATION_SCRIPT.md`
   - [ ] Test run completed
   - [ ] Timing checkpoints noted
   - [ ] Narration practiced

### During Recording

**Tips**:
- **Slow down**: Move mouse slowly, deliberately
- **Pause**: Give viewers time to read metrics (2-3 seconds)
- **Highlight**: Use demo mode tooltips to explain
- **Wait**: Let page loads complete before speaking
- **Consistency**: Follow script timing checkpoints

**Script Timing** (HEDIS Demo):
- 0:30 - At quality measures list
- 1:00 - Viewing BCS measure details
- 1:30 - Clicked "Run Evaluation"
- 2:00 - Results displayed
- 2:30 - Viewing care gap list
- 3:30 - Patient detail view
- 4:15 - Generate outreach screen
- 4:45 - QRDA export screen
- 5:00 - End

### After Recording

**If Success**:
- Export video (1080p, 30fps)
- Edit: Add intro/outro, graphics, background music
- Publish: Website, YouTube, sales portal

**If Retry Needed**:
```bash
# Quick restore (30 seconds)
./demo-cli snapshot restore "attempt-1"

# Try again!
```

---

## Troubleshooting

### Demo Environment Won't Start

```bash
# Check Docker
docker compose -f docker-compose.demo.yml ps

# Check logs
docker compose -f docker-compose.demo.yml logs

# Restart services
docker compose -f docker-compose.demo.yml restart

# Nuclear option (clean restart)
docker compose -f docker-compose.demo.yml down
docker compose -f docker-compose.demo.yml up -d
```

### Evaluation Takes Too Long (> 20 seconds)

```bash
# Pre-warm cache
curl http://localhost:8087/api/v1/quality-measures/warm-cache

# Check database connection pool
docker compose logs -f quality-measure-service | grep "hikari"

# Verify indexes
docker exec -it hdim-demo-postgres psql -U healthdata -d healthdata_demo
\d patients  -- Check for indexes
```

### Demo Data Missing or Incorrect

```bash
# Full reset
./demo-cli reset

# Reload scenario
./demo-cli load-scenario hedis-evaluation

# Verify data
./demo-cli status
```

### Demo Mode UI Not Working

1. Check URL has `?demo=true` parameter
2. Clear browser cache
3. Hard refresh (Ctrl+Shift+R or Cmd+Shift+R)
4. Check browser console for errors

---

## FAQ

### Q: Can I modify the demo data?
**A**: Yes, but use the demo CLI or API. Don't modify database directly. Snapshots make it easy to experiment safely.

### Q: How long does a full reset take?
**A**: 2-3 minutes for full reset, < 30 seconds with snapshots.

### Q: Can I create custom demo scenarios?
**A**: Yes! Follow the pattern in `HEDIS_EVALUATION_SCRIPT.md`. Create custom scenarios via API or by defining new scenario templates.

### Q: Is the demo data HIPAA compliant?
**A**: Yes - it's 100% synthetic. No real PHI. Safe for public demos, videos, websites.

### Q: Can I run demos offline?
**A**: Yes, once the Docker environment is running, no internet required.

### Q: How do I add new patient personas?
**A**: Edit `SyntheticPatientGenerator.java`, add to `PatientTemplates` class, rebuild.

### Q: What if I want a 10-minute demo instead of 5 minutes?
**A**: Combine scenarios! E.g., HEDIS + Patient Journey. Or extend existing script.

### Q: Can prospects interact with the demo?
**A**: Yes! Give them demo credentials. They can explore safely. Data resets easily.

---

## Support

### For Questions
- **Architecture**: See `DEMO_PLATFORM_ARCHITECTURE.md`
- **Implementation**: See `DEMO_PLATFORM_IMPLEMENTATION_PLAN.md`
- **Recording**: See `demo-scripts/HEDIS_EVALUATION_SCRIPT.md`
- **Business Value**: See `DEMO_PLATFORM_EXECUTIVE_SUMMARY.md`

### Issue Tracking
- GitHub Issues with `[DEMO]` prefix
- Priority: P1 before prospect meetings
- Owner: Engineering Team

### Training
- Sales team training: Week 4 of implementation
- Video recording workshop: After first script complete
- Demo platform user manual: Coming soon

---

## Implementation Timeline

**Current Status**: Architecture complete, foundation code written

**Week 1**: Core seeding service (generators, API, database)
**Week 2**: Demo CLI tool, demo mode UI
**Week 3**: Docker deployment, performance optimization, testing
**Week 4**: Documentation, training, video recording, launch

**Go-Live Date**: 4 weeks from kickoff

---

## Success Metrics

**Technical**:
- [ ] All performance targets met
- [ ] Zero errors during recording
- [ ] Demo reset < 30 seconds
- [ ] All 4 scenarios functional

**Business**:
- [ ] 4 professional videos recorded
- [ ] Sales team trained
- [ ] 10+ prospect meetings using demos
- [ ] Measurable pipeline acceleration

---

**Last Updated**: January 3, 2026
**Next Review**: Weekly during implementation
**Owner**: Engineering & Sales Teams
