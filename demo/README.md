# HDIM Care Gap Demo Platform

A minimal, single-VM demonstration environment showcasing HDIM's real-time care gap identification and closure capabilities.

## Quick Start

### Option 1: Test Then Deploy (Recommended)

```bash
# Make scripts executable
chmod +x *.sh

# Test deployment comprehensively (~30 minutes)
./test-demo-deployment.sh

# If tests pass, start the demo
./start-demo.sh

# Open browser to http://localhost:4200
# Login: demo_admin / demo123
# Quality Measure Dev: demo.developer / demo123
```

### Option 2: Direct Deploy

```bash
# Start immediately (no testing)
./start-demo.sh

# Wait for "Demo is ready!" message
# Open browser to http://localhost:4200
```

Optional: copy `demo/.env.example` to `demo/.env` and set `POSTGRES_PASSWORD` to keep database credentials consistent across services.

## What's Included

This demo includes:

- **6 Core Microservices**: Gateway, FHIR Server, Patient Service, Care Gap Service, Quality Measure Service, CQL Engine
- **Clinical Portal Frontend**: Angular-based care manager interface
- **Demo Data**: 10 patients with 16 realistic care gaps across 11 HEDIS measures
- **Full Walkthrough Guide**: Step-by-step demo script for customer presentations

## System Requirements

| Requirement | Minimum | Recommended |
|-------------|---------|-------------|
| RAM | 8 GB | 16 GB |
| Disk Space | 20 GB | 40 GB |
| Docker | 24.0+ | Latest |
| Docker Compose | 2.20+ | Latest |

## Files

| File | Purpose |
|------|---------|
| `test-demo-deployment.sh` | **Comprehensive E2E testing** (37 tests) |
| `start-demo.sh` | One-command full-stack startup (users + seeding) |
| `docker-compose.demo.yml` | Full demo stack configuration |
| `.env.example` | Demo environment variable template (database password) |
| `init-demo-db.sh` | Database initialization |
| `seed-demo-data.sh` | Demo patient and care gap data |
| `validate-demo-data.sh` | Validate seeded data against expected results |
| `DEMO_WALKTHROUGH.md` | Customer presentation guide (15-20 min) |
| `DEPLOYMENT_GUIDE.md` | Multi-platform deployment guide |
| `TEST_DEPLOYMENT_SUMMARY.md` | Test suite documentation |

## Commands

### Testing Commands

```bash
# Comprehensive E2E test (recommended before deployment)
./test-demo-deployment.sh

# Test for specific platform
./test-demo-deployment.sh --platform cloud
./test-demo-deployment.sh --platform swarm
./test-demo-deployment.sh --platform k8s

# Skip performance tests (faster, ~15 min vs ~30 min)
./test-demo-deployment.sh --skip-perf

# Skip rebuild (use existing images)
./test-demo-deployment.sh --skip-build

# Custom report location
./test-demo-deployment.sh --report-dir /tmp/reports
```

### Deployment Commands

```bash
# Start demo
./start-demo.sh

# Start with fresh build
./start-demo.sh --build

# Clean start (removes all data)
./start-demo.sh --clean

# Check status
./start-demo.sh --status

# Stop demo
./start-demo.sh --stop

# Validate seeded data
./validate-demo-data.sh
```

## Ports

| Port | Service | Description |
|------|---------|-------------|
| 4200 | Clinical Portal | Web UI |
| 8080 | API Gateway | REST API entry point |
| 8081 | CQL Engine | Clinical Quality Language execution |
| 8084 | Patient Service | Patient demographics |
| 8085 | FHIR Service | FHIR R4 resource server |
| 8086 | Care Gap Service | Gap identification & tracking |
| 8087 | Quality Measure | HEDIS measure evaluation |
| 5435 | PostgreSQL | Database |
| 6380 | Redis | Cache |

## Demo Data

### Hero Patient: Maria Garcia

- **MRN**: MRN-2024-4521
- **Age**: 57 years old (Female)
- **Care Gaps**:
  - Colorectal Cancer Screening (HIGH priority, 127 days overdue)
  - Breast Cancer Screening (MEDIUM priority, 45 days overdue)

### All Demo Patients (10 total)

1. Maria Garcia - Colorectal + Breast Cancer Screening
2. Sarah Johnson - Breast Cancer Screening (380 days overdue)
3. Linda Chen - Diabetes HbA1c + Eye Exam
4. Michael Williams - Statin Therapy
5. Patricia Davis - Cervical Cancer Screening
6. James Thompson - Annual Wellness + Colorectal (5 years overdue!)
7. Barbara Martinez - Osteoporosis Screening
8. William Anderson - Depression Follow-up
9. Susan Taylor - Diabetic Eye + Kidney Health
10. David Brown - Kidney Health + Statin + Blood Pressure

## Architecture

```
┌──────────────────────────────────────────────────────────────┐
│                    Clinical Portal (4200)                     │
│                        Angular 17                             │
└──────────────────────────┬───────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────┐
│                    API Gateway (8080)                         │
│                  Spring Cloud Gateway                         │
└───────┬─────────┬─────────┬─────────┬─────────┬─────────────┘
        │         │         │         │         │
   ┌────▼───┐ ┌───▼────┐ ┌──▼───┐ ┌──▼────┐ ┌──▼────┐
   │Patient │ │ FHIR   │ │ Care │ │Quality│ │ CQL   │
   │Service │ │Service │ │ Gap  │ │Measure│ │Engine │
   │ (8084) │ │ (8085) │ │(8086)│ │(8087) │ │(8081) │
   └───┬────┘ └───┬────┘ └──┬───┘ └───┬───┘ └───┬───┘
       │          │         │         │         │
┌──────▼──────────▼─────────▼─────────▼─────────▼──────────────┐
│                    PostgreSQL (5435)                          │
│                    Redis Cache (6380)                         │
└──────────────────────────────────────────────────────────────┘
```

## Customization

### Adding More Patients

Edit `seed-demo-data.sh` and add entries using the `create_patient` and `create_care_gap` functions.

### Changing Demo Credentials

Update the JWT_SECRET in `docker-compose.demo.yml` for different authentication settings.

### Connecting to Real Data

For POC with real data:
1. Configure FHIR service to connect to your EHR's FHIR API
2. Update authentication settings
3. Disable demo data seeding

## Troubleshooting

### Services Not Starting

```bash
# View logs
docker compose -f docker-compose.demo.yml logs -f

# Check specific service
docker compose -f docker-compose.demo.yml logs care-gap-service
```

### Memory Issues

If services crash with OOM errors:
1. Increase Docker memory allocation (Docker Desktop → Settings → Resources)
2. Reduce number of services by commenting out non-essential services

### Port Conflicts

Check for conflicting services:
```bash
# Linux/Mac
lsof -i :4200
lsof -i :8080

# Stop conflicting service or change port mapping in docker-compose.demo.yml
```

### Database Connection Errors

```bash
# Reset database
./start-demo.sh --clean
```

## Security Note

This demo environment uses development-mode security settings:
- Static JWT secret
- Gateway trust mode enabled
- No SSL/TLS

**DO NOT use this configuration in production.**

## Support

- **Demo Guide**: See [DEMO_WALKTHROUGH.md](DEMO_WALKTHROUGH.md)
- **Full Documentation**: [docs.healthdatainmotion.com](https://docs.healthdatainmotion.com)
- **Email**: support@healthdatainmotion.com

---

*Last Updated: December 2025*
