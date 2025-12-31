# HDIM Platform Demo Walkthrough

## Quick Start

The HDIM demo stack provides a fully functional healthcare interoperability platform with clinical portal, API gateway, and microservices.

### Demo Stack Access Points

| Service | URL | Purpose |
|---------|-----|---------|
| **Clinical Portal** | http://localhost:4200 | Web UI for clinical staff |
| **API Gateway** | http://localhost:8080 | REST API for integrations |
| **PostgreSQL** | localhost:5435 | Database (clinical-portal user: healthdata) |
| **Redis** | localhost:6380 | Cache layer |
| **Kafka** | localhost:9094 | Event streaming |

### Demo Users (All Password: `demo123`)

| Email | Username | Role | Permissions |
|-------|----------|------|-------------|
| demo_admin@hdim.ai | demo_admin | ADMIN, EVALUATOR | Full access, run evaluations |
| demo_analyst@hdim.ai | demo_analyst | ANALYST, EVALUATOR | View reports, run evaluations |
| demo_viewer@hdim.ai | demo_viewer | VIEWER | Read-only access |

**All demo users belong to tenant: DEMO001**

### Hero Patient for Walkthrough
**Maria Garcia** (MRN: MRN-2024-4521)
- 57-year-old female
- Colorectal Cancer Screening gap (127 days overdue)
- Breast Cancer Screening gap (45 days overdue)

See [demo/DEMO_WALKTHROUGH.md](demo/DEMO_WALKTHROUGH.md) for the complete 15-20 minute customer presentation guide.

---

## Startup Instructions

### Docker Compose Demo Stack (Recommended)

```bash
# Start all demo services including clinical portal
docker compose -f docker-compose.demo.yml up -d

# Wait for all services to be healthy (approximately 2-3 minutes)
docker compose -f docker-compose.demo.yml ps

# Once all services show "healthy", access the clinical portal
# Open browser to: http://localhost:4200
```

### Development Mode (Frontend Dev Server)

For frontend development with hot reload:

```bash
# Terminal 1: Start backend services
docker compose -f docker-compose.demo.yml up -d

# Terminal 2: Start Angular dev server
cd apps/clinical-portal
npm install  # if needed
npm run dev
# Frontend runs at: http://localhost:4200
```

## Demo Data

### Patients
- **1,000 demo patients** loaded in fhir_db
- Ages ranging from 1940-1967 birth dates
- Mix of male/female genders

### Conditions (641 total)
| Condition | ICD-10 Code | Count |
|-----------|-------------|-------|
| Diabetes Type 2 | E11.9 | 203 |
| Hypertension | I10 | 295 |
| Coronary Artery Disease | I25.10 | 143 |

### Clinical Portal Features

The clinical portal includes:
- **Patient Dashboard**: View patient list with clinical summaries
- **Patient Details**: Comprehensive patient health profiles with conditions, medications, observations
- **Care Gap Detection**: Identify missing preventive care and quality measures
- **Quality Evaluations**: Run HEDIS quality measure evaluations
- **Reports**: Generate quality reports for performance metrics
- **3D Visualizations**: Advanced clinical data visualizations
- **AI Assistant**: Clinical decision support chatbot

## Working API Endpoints

### Authentication (Gateway - Port 8080)
```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{\"username\": \"demo_admin\", \"password\": \"demo123\"}'"}'

# Response includes accessToken for subsequent requests
```

### FHIR API (via Gateway - Port 8080)

Note: The clinical portal uses HttpOnly cookies for HIPAA compliance. For API testing with curl, authenticate first as shown above.

```bash
# List Patients
curl "http://localhost:8080/fhir/Patient?_count=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO001"

# Get Patient by ID
curl "http://localhost:8080/fhir/Patient/{patientId}" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO001"

# Get Conditions for Patient
curl "http://localhost:8080/fhir/Condition?patient={patientId}" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: DEMO001"
```

### Health Checks (No Auth Required)
```bash
# Gateway health
curl http://localhost:8080/actuator/health

# FHIR service health
curl http://localhost:8085/actuator/health

# CQL Engine health
curl http://localhost:8081/actuator/health
```

## Service Ports

| Service | Port | Status |
|---------|------|--------|
| **Clinical Portal** | **4200** | **✅ Healthy** |
| API Gateway | 8080 | ✅ Healthy |
| FHIR Service | 8085 | ✅ Healthy |
| Patient Service | 8084 | ✅ Healthy |
| Quality Measure Service | 8087 | ✅ Healthy |
| Care Gap Service | 8086 | ✅ Healthy |
| PostgreSQL | 5435 | ✅ Healthy |
| Redis | 6380 | ✅ Healthy |
| Kafka | 9094 | ✅ Healthy |

## Demo Script

### 1. Show Platform Architecture
- Multi-tenant healthcare platform
- HIPAA-compliant data handling
- FHIR R4 standard compliance

### 2. Authentication Flow
- JWT-based authentication via Gateway
- Role-based access control (RBAC)
- Multi-tenant isolation

### 3. Patient Data
- Show patient list via FHIR API
- Demonstrate condition data (diabetes, hypertension, CAD)
- Multi-tenant data isolation

### 4. Clinical Portal (Frontend)
- Access at http://localhost:4200
- Login with demo_admin credentials (email: demo_admin@hdim.ai, password: demo123)
- Navigate patient list and view patient details
- Run care gap detection on a patient
- View quality measure evaluation results

## Backup & Restore

### Manual Backup
```bash
docker exec healthdata-backup /usr/local/bin/backup.sh
```

### List Backups
```bash
docker exec healthdata-backup /usr/local/bin/restore.sh --list
```

### Automated Schedule
- Daily at 2 AM
- 7-day retention policy

## Known Issues (Demo Mode)

1. **Quality Measure Service**: Returns 403 due to tenant access filter
   - Backend services need shared user database or gateway-trusted headers

2. **Care Gap Service**: Similar auth issues as Quality Measure

3. **Tracing**: OTEL connection errors (cosmetic, doesn't affect functionality)

## Production Considerations

- [ ] Configure proper JWT secret (not demo secret)
- [ ] Set up CORS for production domains
- [ ] Enable full audit logging
- [ ] Configure external secrets management
- [ ] Set up proper backup destinations
- [ ] Review security hardening checklist

---

## Demo Platform Files

| File | Description |
|------|-------------|
| [demo/README.md](demo/README.md) | Demo platform documentation |
| [demo/DEMO_WALKTHROUGH.md](demo/DEMO_WALKTHROUGH.md) | Customer presentation guide |
| [demo/start-demo.sh](demo/start-demo.sh) | One-command startup |
| [demo/seed-demo-data.sh](demo/seed-demo-data.sh) | Demo data loader |
| [demo/docker-compose.demo.yml](demo/docker-compose.demo.yml) | Minimal service config |

## Related Documentation

- [Video Production Package](docs/video/CARE_GAP_VIDEO_PRODUCTION_PACKAGE.md)
- [System Architecture](docs/architecture/SYSTEM_ARCHITECTURE.md)
- [Backend API Specification](BACKEND_API_SPECIFICATION.md)

---
*Last Updated: December 31, 2025*
