# Docker Integration Testing Guide

## Quick Start - All Services

### 1. Start Infrastructure and Services

```bash
# Start all services with Docker Compose
docker-compose up -d

# Check service health
docker-compose ps

# View logs
docker-compose logs -f quality-measure-service
docker-compose logs -f clinical-portal
```

### 2. Verify Services are Running

```bash
# Check PostgreSQL
docker exec healthdata-postgres psql -U healthdata -d healthdata_cql -c "\dt"

# Check Redis
docker exec healthdata-redis redis-cli ping

# Check Kafka
docker exec healthdata-kafka kafka-topics --list --bootstrap-server localhost:9092

# Check Quality Measure Service
curl http://localhost:8087/quality-measure/actuator/health

# Check Frontend
curl http://localhost:4202
```

### 3. Run Integration Tests

```bash
# Backend API tests (when services are running)
cd backend
./gradlew :modules:services:quality-measure-service:test

# Frontend unit tests
cd apps/clinical-portal
npm run test

# E2E tests (requires all services running)
npx playwright test apps/clinical-portal-e2e/src/reports.e2e.spec.ts
```

## Service Ports

| Service | Port | URL |
|---------|------|-----|
| Clinical Portal (Frontend) | 4202 | http://localhost:4202 |
| Quality Measure Service | 8087 | http://localhost:8087/quality-measure |
| Patient Service | 8084 | http://localhost:8084/patient |
| FHIR Service | 8085 | http://localhost:8085/fhir |
| CQL Engine Service | 8081 | http://localhost:8081/cql-engine |
| Care Gap Service | 8086 | http://localhost:8086/care-gap |
| PostgreSQL | 5435 | localhost:5435 |
| Redis | 6380 | localhost:6380 |
| Kafka | 9094 | localhost:9094 |

## Environment Variables Required

### Quality Measure Service
```env
# JWT Configuration
JWT_SECRET=your_secret_key_here

# Database
DB_PASSWORD=healthdata_password
SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/healthdata_cql
SPRING_DATASOURCE_USERNAME=healthdata

# Service URLs (internal Docker network)
PATIENT_SERVICE_URL=http://patient-service:8084
FHIR_SERVICE_URL=http://fhir-service:8085
CQL_ENGINE_URL=http://cql-engine-service:8081
CARE_GAP_SERVICE_URL=http://care-gap-service:8086

# Cache
SPRING_REDIS_HOST=redis
SPRING_REDIS_PORT=6379

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
```

### Clinical Portal
```env
API_BASE_URL=http://localhost:8087
ENVIRONMENT=development
```

## Test Scenarios

### Scenario 1: Generate Patient Report
1. Login to Clinical Portal (http://localhost:4202)
2. Navigate to Reports page
3. Click "Generate Patient Report"
4. Select patient from list
5. Enter report name
6. Click Generate
7. Verify report appears in Saved Reports

**Expected API Calls:**
```
POST /quality-measure/report/patient/save
GET /quality-measure/reports?type=PATIENT
```

### Scenario 2: Export Report to CSV
1. Navigate to Saved Reports tab
2. Find a report
3. Click CSV export button
4. Verify file downloads

**Expected API Call:**
```
GET /quality-measure/reports/{id}/export/csv
```

### Scenario 3: Delete Report
1. Navigate to Saved Reports tab
2. Click Delete button on a report
3. Confirm deletion
4. Verify report removed from list

**Expected API Calls:**
```
DELETE /quality-measure/reports/{id}
GET /quality-measure/reports
```

## Troubleshooting

### Service Won't Start
```bash
# Check logs
docker-compose logs service-name

# Restart specific service
docker-compose restart service-name

# Rebuild if code changed
docker-compose up -d --build service-name
```

### Database Issues
```bash
# Reset database
docker-compose down -v postgres
docker-compose up -d postgres

# Run migrations manually
docker exec healthdata-postgres psql -U healthdata -d healthdata_cql -f /path/to/migration.sql
```

### Network Issues
```bash
# Inspect network
docker network inspect healthdata-network

# Verify service can reach others
docker exec quality-measure-service curl http://patient-service:8084/actuator/health
```

### Clear All Data and Restart
```bash
# Stop everything
docker-compose down

# Remove volumes (WARNING: Deletes all data)
docker-compose down -v

# Start fresh
docker-compose up -d

# Wait for health checks
watch docker-compose ps
```

## Test Data Setup

### Create Test User
```sql
-- Connect to database
docker exec -it healthdata-postgres psql -U healthdata -d healthdata_cql

-- Create test user
INSERT INTO users (id, username, email, password_hash, tenant_id, created_at)
VALUES (
  gen_random_uuid(),
  'testuser',
  'test@example.com',
  '$2a$10$...',  -- bcrypt hash for "password123"
  'TENANT001',
  NOW()
);

-- Grant role
INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_USER' FROM users WHERE username = 'testuser';
```

### Create Test Patient (via FHIR Service)
```bash
curl -X POST http://localhost:8085/fhir/Patient \
  -H "Content-Type: application/fhir+json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "resourceType": "Patient",
    "identifier": [{"value": "TEST001"}],
    "name": [{"family": "Doe", "given": ["John"]}],
    "gender": "male",
    "birthDate": "1970-01-01"
  }'
```

## Continuous Integration

### GitHub Actions Example
```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Start services
        run: docker-compose up -d
        
      - name: Wait for health
        run: |
          timeout 300 bash -c 'until docker-compose ps | grep -q "healthy"; do sleep 5; done'
      
      - name: Run tests
        run: |
          npm run test
          npx playwright test
      
      - name: Stop services
        run: docker-compose down -v
```

## Production Deployment Notes

### Differences from Development
1. **JWT_SECRET**: Use strong random key, never commit
2. **Database**: Use managed PostgreSQL (RDS, CloudSQL)
3. **Redis**: Use managed Redis (ElastiCache, MemoryStore)
4. **Kafka**: Use managed Kafka (MSK, Confluent Cloud)
5. **HTTPS**: All external traffic over TLS
6. **Health Checks**: Configure load balancer health checks
7. **Monitoring**: APM, logs aggregation, metrics
8. **Backups**: Automated database backups
9. **Secrets**: Use secret management (Vault, AWS Secrets Manager)
10. **Scaling**: Horizontal pod autoscaling for services

### Health Check Endpoints
All services expose:
- `GET /actuator/health` - Overall health
- `GET /actuator/health/liveness` - Kubernetes liveness probe
- `GET /actuator/health/readiness` - Kubernetes readiness probe

## Success Criteria

✅ **All Tests Passing:**
- Unit tests: 18/18 for reports service
- E2E smoke tests: 11/11 for UI validation
- Integration tests: API contract validation

✅ **All Services Healthy:**
```bash
docker-compose ps
# All services show "Up" and "healthy"
```

✅ **End-to-End Flow Works:**
1. User can log in
2. Can generate patient report
3. Can view saved reports
4. Can export to CSV/Excel
5. Can delete reports
