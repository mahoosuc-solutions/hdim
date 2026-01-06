# HDIM Demo Platform - User Guide

**Version**: 1.0
**Last Updated**: January 2026
**Audience**: Sales Engineers, Demo Operators, Marketing Team

---

## Table of Contents

1. [Overview](#overview)
2. [Quick Start](#quick-start)
3. [Demo Scenarios](#demo-scenarios)
4. [Using the Demo CLI](#using-the-demo-cli)
5. [Demo Mode UI](#demo-mode-ui)
6. [Recording Best Practices](#recording-best-practices)
7. [Common Workflows](#common-workflows)
8. [FAQ](#faq)

---

## Overview

The HDIM Demo Platform is a self-contained environment for creating professional video demonstrations of HDIM's healthcare analytics capabilities. It includes:

- **Synthetic patient data** - HIPAA-compliant, realistic healthcare data
- **Pre-configured scenarios** - 4 demo scenarios targeting different audiences
- **Demo Mode UI** - Enhanced UI with tooltips and visual highlights
- **CLI Tools** - Command-line tools for environment management
- **Snapshot/Restore** - Quick reset between recording takes

### Target Audiences

| Scenario | Target Audience | Duration |
|----------|-----------------|----------|
| HEDIS Evaluation | Quality Directors, VPs | 3-5 min |
| Patient Journey | Care Managers, CMOs | 4-6 min |
| Risk Stratification | ACO Leadership, CFOs | 3-4 min |
| Multi-Tenant Admin | IT Directors, CISOs | 2-3 min |

---

## Quick Start

### Prerequisites

- Docker Desktop installed and running
- Terminal access (Bash, PowerShell, or similar)
- Web browser (Chrome recommended for recording)

### 1. Start the Demo Environment

```bash
# Navigate to project root
cd hdim-master

# Start all demo services
docker compose -f docker-compose.demo.yml up -d

# Wait for services to be healthy (typically 60-90 seconds)
docker compose -f docker-compose.demo.yml ps
```

### 2. Initialize Demo Data

```bash
# Navigate to CLI tool
cd backend/tools/demo-cli

# Reset and initialize demo environment
./demo-cli.sh reset
./demo-cli.sh load-scenario hedis-evaluation
```

### 3. Access Demo Portal

Open your browser to: `http://localhost:4200?demo=true`

### 4. Login Credentials

| Role | Username | Password |
|------|----------|----------|
| Demo Admin | demo_admin@hdim.ai | demo123 |
| Demo Analyst | demo_analyst@hdim.ai | demo123 |
| Demo Viewer | demo_viewer@hdim.ai | demo123 |

---

## Demo Scenarios

### Scenario 1: HEDIS Quality Measure Evaluation

**Best For**: Quality Directors, VP of Quality, Population Health Leaders

**Key Messages**:
- Automate HEDIS measure calculation
- Identify care gaps instantly
- Generate CMS-ready reports

**Data Included**:
- 5,000 synthetic patients
- 247 care gaps across 6 measures
- 4 quality measures pre-evaluated
- QRDA I/III export ready

**Load Command**:
```bash
./demo-cli.sh load-scenario hedis-evaluation
```

---

### Scenario 2: Patient Care Journey

**Best For**: Care Managers, Medical Directors, CMOs

**Key Messages**:
- 360° patient view
- Clinical decision support
- Risk-based interventions

**Data Included**:
- 1,000 patients with detailed clinical history
- 4 featured patient personas:
  - Michael Chen (Complex Diabetic)
  - Sarah Martinez (Preventive Gap)
  - Emma Johnson (High Risk)
  - Carlos Rodriguez (SDOH Barriers)

**Load Command**:
```bash
./demo-cli.sh load-scenario patient-journey
```

---

### Scenario 3: Risk Stratification & Analytics

**Best For**: ACO Leadership, CFOs, Population Health Analysts

**Key Messages**:
- Predict high-risk patients
- Optimize resource allocation
- Demonstrate ROI on interventions

**Data Included**:
- 10,000 patients with HCC scores
- Risk distribution: 60% low / 30% moderate / 10% high
- Cost avoidance projections
- Trend analysis data

**Load Command**:
```bash
./demo-cli.sh load-scenario risk-stratification
```

---

### Scenario 4: Multi-Tenant Administration

**Best For**: IT Directors, CISOs, Procurement

**Key Messages**:
- Enterprise-grade security
- Complete data isolation
- Role-based access control

**Data Included**:
- 3 demo tenants:
  - Acme Health Plan (5,000 patients)
  - Metro Medical Group (2,500 patients)
  - Regional ACO (8,000 patients)
- Multiple user roles per tenant
- Audit log examples

**Load Command**:
```bash
./demo-cli.sh load-scenario multi-tenant
```

---

## Using the Demo CLI

The Demo CLI is located at `backend/tools/demo-cli/demo-cli.sh`.

### Available Commands

| Command | Description |
|---------|-------------|
| `reset` | Reset all demo data to clean state |
| `load-scenario <name>` | Load a specific demo scenario |
| `list-scenarios` | List all available scenarios |
| `generate-patients` | Generate synthetic patients |
| `status` | Check demo environment status |
| `snapshot create <name>` | Create a database snapshot |
| `snapshot restore <name>` | Restore from a snapshot |
| `snapshot list` | List available snapshots |
| `initialize` | Initialize scenarios and templates |

### Examples

```bash
# Check current status
./demo-cli.sh status

# List available scenarios
./demo-cli.sh list-scenarios

# Load HEDIS scenario
./demo-cli.sh load-scenario hedis-evaluation

# Create snapshot before recording
./demo-cli.sh snapshot create "before-take-1"

# Restore after failed take
./demo-cli.sh snapshot restore "before-take-1"

# Generate additional patients
./demo-cli.sh generate-patients --count 1000 --tenant acme-health --care-gap-percentage 30
```

---

## Demo Mode UI

### Activating Demo Mode

Add `?demo=true` to any URL to enable demo mode:

```
http://localhost:4200/dashboard?demo=true
http://localhost:4200/patients?demo=true
http://localhost:4200/quality-measures?demo=true
```

### Demo Control Bar

When demo mode is active, a control bar appears at the top of the screen:

| Element | Description |
|---------|-------------|
| Recording Timer | Elapsed time since demo started |
| Scenario Selector | Switch between loaded scenarios |
| Reset Button | Reset current scenario |
| Snapshot Controls | Create/restore snapshots |
| Demo Mode Toggle | Enable/disable demo features |

### Demo Tooltips

Hover over elements with a purple glow to see demo tooltips explaining:
- What the metric represents
- Why it matters to the audience
- Suggested talking points

### Visual Highlights

Key metrics are highlighted with subtle visual effects:
- Purple glow on important numbers
- Animated transitions on data changes
- Larger fonts for key statistics

---

## Recording Best Practices

### Before Recording

1. **Create a snapshot**: Always create a snapshot before recording
   ```bash
   ./demo-cli.sh snapshot create "take-1-baseline"
   ```

2. **Verify data**: Check that expected data is present
   ```bash
   ./demo-cli.sh status
   ```

3. **Clear browser cache**: Ensure no stale data is displayed
4. **Close unnecessary tabs**: Reduce system resource usage
5. **Set screen resolution**: 1920x1080 recommended

### During Recording

1. **Follow the script**: Use the demo scripts in `docs/demo-scripts/`
2. **Speak naturally**: Narration should be conversational
3. **Pause on key metrics**: Allow viewers to absorb important numbers
4. **Highlight value**: Focus on business outcomes, not features

### After Recording

1. **Save the recording immediately**
2. **Create notes**: Document any issues or improvements
3. **Review the footage**: Check for technical issues before editing

### Handling Mistakes

If you make a mistake during recording:

```bash
# Restore to baseline
./demo-cli.sh snapshot restore "take-1-baseline"

# Reload the page and continue
```

---

## Common Workflows

### Workflow 1: Recording a HEDIS Demo

```bash
# 1. Start fresh
./demo-cli.sh reset

# 2. Load HEDIS scenario
./demo-cli.sh load-scenario hedis-evaluation

# 3. Create baseline snapshot
./demo-cli.sh snapshot create "hedis-baseline"

# 4. Open demo UI
open "http://localhost:4200?demo=true"

# 5. Login as demo_analyst@hdim.ai / demo123

# 6. Follow script: docs/demo-scripts/HEDIS_EVALUATION_SCRIPT.md

# 7. If need to retry:
./demo-cli.sh snapshot restore "hedis-baseline"
```

### Workflow 2: Preparing for Multiple Demos in One Day

```bash
# Morning: Create baselines for all scenarios
./demo-cli.sh load-scenario hedis-evaluation
./demo-cli.sh snapshot create "hedis-morning"

./demo-cli.sh load-scenario patient-journey
./demo-cli.sh snapshot create "patient-morning"

./demo-cli.sh load-scenario risk-stratification
./demo-cli.sh snapshot create "risk-morning"

# Throughout the day: Quick restore between demos
./demo-cli.sh snapshot restore "hedis-morning"
```

### Workflow 3: Custom Patient Generation

```bash
# Generate custom cohort for specific demo needs
./demo-cli.sh generate-patients \
  --count 2000 \
  --tenant "custom-demo" \
  --care-gap-percentage 35

# Create snapshot of custom data
./demo-cli.sh snapshot create "custom-demo-v1"
```

---

## FAQ

### Q: How long does it take to load a scenario?

**A**: Typical load times:
- HEDIS Evaluation: ~30 seconds
- Patient Journey: ~15 seconds
- Risk Stratification: ~45 seconds
- Multi-Tenant: ~60 seconds

### Q: What if the demo services won't start?

**A**: Check Docker Desktop is running and ports aren't in use:
```bash
docker compose -f docker-compose.demo.yml down
docker compose -f docker-compose.demo.yml up -d
```

### Q: How do I reset everything to a clean state?

**A**:
```bash
# Stop all services
docker compose -f docker-compose.demo.yml down -v

# Remove demo data volumes
docker volume rm hdim-master_demo_postgres_data hdim-master_demo_snapshots

# Restart fresh
docker compose -f docker-compose.demo.yml up -d
./demo-cli.sh reset
```

### Q: Can I run demos offline?

**A**: Yes, once services are running, no internet connection is required. All data is generated locally.

### Q: How often should demo data be refreshed?

**A**: Recommend refreshing demo data:
- When new measures are added
- After major feature releases
- Quarterly for realistic date ranges

### Q: What's the recommended screen recording software?

**A**:
- **OBS Studio** (free, professional features)
- **Loom** (easy cloud sharing)
- **Screenflow** (Mac, great editing)
- **Camtasia** (Windows, comprehensive)

---

## Support

For demo platform issues:

1. Check `docs/DEMO_TROUBLESHOOTING.md` for common issues
2. Run `./demo-cli.sh status` to diagnose
3. Review service logs: `docker compose -f docker-compose.demo.yml logs`
4. Contact engineering team for complex issues

---

**Last Updated**: January 2026
