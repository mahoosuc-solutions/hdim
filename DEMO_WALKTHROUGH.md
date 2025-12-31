# HDIM Platform Demo Walkthrough

## Quick Start

### Prerequisites
- Docker Compose running with core profile
- Frontend dev server started

### Start Commands
```bash
# Backend services (from hdim-master directory)
cd /home/mahoosuc-solutions/projects/hdim-master/hdim-master
docker compose --profile core up -d

# Frontend (in separate terminal)
cd frontend
npm run dev
# Running at: http://localhost:3003
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

### Test Users
| Username | Password | Role |
|----------|----------|------|
| test_superadmin | password123 | SUPER_ADMIN |
| test_admin | password123 | ADMIN |
| test_evaluator | password123 | EVALUATOR |

## Working API Endpoints

### Authentication (Gateway - Port 8080)
```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username": "test_admin", "password": "password123"}'

# Response includes accessToken for subsequent requests
```

### FHIR API (via Gateway - Port 8080)
```bash
TOKEN="<from login response>"

# List Patients (returns 1000 patients)
curl "http://localhost:8080/fhir/Patient?_count=10" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default"

# Get Patient by ID
curl "http://localhost:8080/fhir/Patient/{patientId}" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default"

# Get Conditions for Patient
curl "http://localhost:8080/fhir/Condition?patient={patientId}" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: default"
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
| Gateway | 8080 | ✅ Healthy |
| FHIR Service | 8085 | ✅ Healthy |
| CQL Engine | 8081 | ✅ Healthy |
| Patient Service | 8084 | ✅ Healthy |
| Quality Measure | 8087 | ⚠️ Auth issues |
| Care Gap | 8086 | ⚠️ Auth issues |
| Frontend | 3003 | ✅ Running |
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
- Access at http://localhost:3003
- Login with test_admin credentials
- Navigate patient views

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
*Generated: December 30, 2025*
