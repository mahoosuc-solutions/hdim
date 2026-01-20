# HDIM Developer Onboarding Guide

Welcome to HealthData-in-Motion! This guide accelerates your path to productivity, typically 2-3 weeks from zero to first contribution.

---

## Phase 0: Prerequisites (Before Starting)

### Must-Haves

- Java 21 LTS installed (`java -version`)
- Docker Desktop installed and running
- Git installed (`git --version`)
- 16GB RAM minimum (8GB minimum, but slow)
- 50GB free disk space

### Verify Setup

```bash
java -version                   # Java 21.x
docker --version                # 24.0+
docker run hello-world          # Docker works
git --version                   # 2.4+
```

### Accounts/Access

- [ ] GitHub access to repo (ask team lead)
- [ ] Slack access to #engineering, #architecture-discussion
- [ ] Jira access (if used)
- [ ] PostgreSQL credentials (if needed)

---

## Day 1: Environment Setup (2-3 hours)

### 1. Clone Repository

```bash
git clone https://github.com/your-org/hdim-master.git
cd hdim-master
```

### 2. Verify Quick Start (Docker)

Deploy full stack in <5 minutes:

```bash
# Start all services
docker compose up -d

# Wait 60 seconds for services to start
sleep 60

# Check status
docker compose ps

# View logs
docker compose logs quality-measure-service | head -50
```

**Expected output**: All services running ✓

**Troubleshooting**:
- Port conflicts? → `docker compose down` before restart
- Out of memory? → Increase Docker memory to 8GB+
- Connection refused? → Wait longer (services starting)

### 3. Verify Gradle Build

```bash
cd backend

# Download dependencies (first time takes 5-10 min)
./gradlew downloadDependencies

# Build a simple service (tests included)
./gradlew :modules:services:patient-service:build -x test
```

**Expected output**: Build successful ✓

### 4. Explore Local URLs

Once services running:

| Service | URL | Purpose |
|---------|-----|---------|
| **Grafana** | http://localhost:3001 | Metrics dashboards |
| **Prometheus** | http://localhost:9090 | Raw metrics |
| **Jaeger** | http://localhost:16686 | Distributed tracing |
| **Patient Service API** | http://localhost:8084/swagger-ui.html | Swagger docs |
| **PostgreSQL** | localhost:5435 | Database (psql client) |

### 5. First Database Query

```bash
# Connect to PostgreSQL
docker exec -it hdim-postgres psql -U healthdata -d patient_db

# List tables
\dt

# Count patients
SELECT COUNT(*) FROM patients;

# Exit
\q
```

---

## Day 2-3: Architecture Understanding (4-6 hours)

### Must-Read Documents (in this order)

1. **[CLAUDE.md](../CLAUDE.md)** (30 min) - Project overview, key info
   - Skim sections: Overview, Tech Stack, Structure, Common Commands
   - Deep dive: HIPAA Compliance, Authentication, Testing Requirements

2. **[Documentation Portal](../docs/README.md)** (20 min) - Navigation and index
   - Understand structure (architecture, services, operations)
   - Bookmark key sections

3. **[System Architecture](../docs/architecture/SYSTEM_ARCHITECTURE.md)** (30 min) - 51 services overview
   - Understand core services (patient, quality-measure, care-gap, fhir)
   - Event streaming with Kafka
   - Database-per-service pattern

4. **[Event Sourcing Architecture](../docs/architecture/EVENT_SOURCING_ARCHITECTURE.md)** (30 min) - If working on event services
   - Event store, projections, event handlers
   - Write path vs read path
   - Event replay capability

5. **[TDD Swarm Methodology](../docs/development/TDD_SWARM.md)** (30 min) - How we develop
   - RED phase (tests first)
   - GREEN phase (implementation)
   - REFACTOR phase (optimization)

6. **[Service Catalog](../docs/services/SERVICE_CATALOG.md)** (20 min) - All 51 services
   - Bookmark for reference
   - Port mappings, team ownership

### Key Concepts to Understand

**Multi-Tenant**: Every query filters by tenant_id
```java
// ✓ CORRECT
findByIdAndTenant(patientId, tenantId)

// ✗ WRONG
findById(patientId)  // What tenant?
```

**Event Sourcing**: Immutable event log, not entity mutations
```java
// ✓ Event: "PatientCreatedEvent"
// ✗ Entity: Patient table with UPDATE/DELETE

// Read from projections (denormalized data)
// Write to event store (append-only log)
```

**HIPAA Compliance**: PHI cache TTL ≤ 5 minutes
```yaml
# REQUIRED
@Cacheable(value = "patientData", key = "#patientId")
# Redis TTL: max 300 seconds
```

**Gateway-Trust Auth**: JWT validated once at gateway
```
Client → Gateway (validate JWT) → Service (trust headers)
                                  ↑ No re-validation!
```

---

## Week 1: Hands-On Development (30-40 hours)

### Task 1: Build and Test a Microservice

**Goal**: Compile and test existing microservice

```bash
cd backend

# Build patient-service
./gradlew :modules:services:patient-service:build

# Run tests
./gradlew :modules:services:patient-service:test

# Generate coverage report
./gradlew :modules:services:patient-service:jacocoTestReport
open modules/services/patient-service/build/reports/jacoco/test/html/index.html
```

**Expected**: All tests passing, >80% coverage

### Task 2: Create a Simple Feature

Pick ONE of these:

**Option A: Add REST endpoint** (Easiest)
- Add new GET endpoint to existing service
- Follow pattern in existing controllers
- Write unit + integration tests

**Option B: Add database table** (Medium)
- Create new JPA entity
- Create Liquibase migration
- Add repository + service methods
- Tests for entity-migration sync

**Option C: Create event handler** (Medium-Hard - if familiar with events)
- Add Kafka consumer for event topic
- Build/update projection
- Event handler service pattern

### Task 3: Code Review

Before committing, review your code:

```
Checklist:
☐ Tests written first (RED phase)
☐ Code passes tests (GREEN phase)
☐ No HIPAA violations (cache TTL, audit logging)
☐ No cross-tenant data leaks (tenant_id filtering)
☐ Follows naming conventions (classes, methods)
☐ Comments explain WHY not WHAT
☐ No hardcoded values (use configuration)
```

### Task 4: Create Pull Request

```bash
git checkout -b feature/my-feature
git commit -m "Add new feature description"
git push origin feature/my-feature
# Open PR on GitHub
```

**PR Checklist**:
- [ ] Tests included
- [ ] Documentation updated
- [ ] No breaking changes
- [ ] Follows code style
- [ ] Addresses issue #XXX

---

## Week 2: Deeper Learning (20-30 hours)

### Topic 1: Database & Migrations

**Read**: [Liquibase Workflow Guide](../backend/docs/LIQUIBASE_DEVELOPMENT_WORKFLOW.md) (CRITICAL)

**Hands-on**:
1. Create new Liquibase migration
2. Add column to existing table
3. Verify rollback capability
4. Test with local database

### Topic 2: Testing Patterns

**Read**: [Testing Patterns](TESTING_PATTERNS.md)

**Hands-on**:
1. Write unit test (mock dependencies)
2. Write integration test (real database)
3. Test multi-tenant isolation
4. Test HIPAA cache compliance

### Topic 3: Distributed Tracing

**Read**: [Distributed Tracing Guide](../backend/docs/DISTRIBUTED_TRACING_GUIDE.md)

**Hands-on**:
1. Make API call, observe trace in Jaeger
2. Follow trace across 2+ services
3. Add custom span to service
4. Verify trace appears in Jaeger UI

### Topic 4: API Security

**Read**: [Gateway Trust Architecture](../backend/docs/GATEWAY_TRUST_ARCHITECTURE.md)

**Hands-on**:
1. Call API with valid JWT
2. Observe X-Auth-* headers
3. Call API without JWT (401)
4. Verify header validation working

---

## Week 3: First Contribution (20-30 hours)

### Pick ONE of These Paths

**Path A: Bug Fix**
- Find issue labeled "good-first-issue"
- Understand root cause
- Write failing test
- Fix code to pass test
- Submit PR

**Path B: Documentation**
- Add missing documentation
- Improve existing guides
- Fix typos/clarity issues
- Submit PR

**Path C: Feature Implementation**
- Pick feature from backlog
- Design solution (get feedback)
- Write tests first (RED)
- Implement code (GREEN)
- Optimize (REFACTOR)
- Submit PR

### Successful PR Traits

✓ Clear commit messages explaining WHY
✓ Tests demonstrate behavior
✓ No hardcoded values
✓ Follows existing patterns
✓ Comments where non-obvious
✓ Updates documentation
✓ No breaking changes

---

## Ongoing: Reference Materials

### Command Reference

```bash
# Build
./gradlew build                                    # Build all
./gradlew :modules:services:SERVICE:build         # Build one service
./gradlew :modules:services:SERVICE:test          # Tests only

# Docker
docker compose up -d                               # Start services
docker compose logs -f SERVICE_NAME                # Watch logs
docker compose ps                                  # Status
docker compose down                                # Stop everything

# Git
git status                                         # Changed files
git diff                                           # Changes
git log --oneline                                  # History
git checkout -b feature/name                       # New branch

# Database
docker exec -it hdim-postgres psql -U healthdata -d patient_db
  \dt                                             # List tables
  \d+ patients                                    # Table schema
  SELECT * FROM patients LIMIT 5;                 # Query
```

### Key File Locations

| What | Location |
|------|----------|
| **HIPAA Rules** | `backend/HIPAA-CACHE-COMPLIANCE.md` |
| **Code Standards** | `backend/docs/CODING_STANDARDS.md` |
| **Database Guide** | `backend/docs/DATABASE_ARCHITECTURE_GUIDE.md` |
| **Architecture** | `docs/architecture/SYSTEM_ARCHITECTURE.md` |
| **Services** | `docs/services/SERVICE_CATALOG.md` |
| **API Specs** | `backend/modules/services/SERVICE_NAME/README.md` |

### Key URLs (When Services Running)

| Service | URL |
|---------|-----|
| Grafana Dashboards | http://localhost:3001 |
| Jaeger Tracing | http://localhost:16686 |
| Prometheus Metrics | http://localhost:9090 |
| Patient Service Docs | http://localhost:8084/swagger-ui.html |
| Quality Measure Docs | http://localhost:8087/swagger-ui.html |

---

## Getting Help

### Questions?

1. **Quick questions** → Ask in #engineering Slack
2. **Design decisions** → See `docs/architecture/decisions/` (ADRs)
3. **Technical debt** → Post in #architecture-discussion
4. **Stuck on task** → Pair program with team member
5. **Understanding code** → Check unit tests (they document behavior)

### Common Issues

| Problem | Solution |
|---------|----------|
| Port 8001-8089 in use | `docker compose down` or kill conflicting process |
| Out of disk space | `docker system prune` then `docker volume prune` |
| Build times out | `cd backend && ./gradlew downloadDependencies` |
| Can't connect to DB | Wait 30s (database starting), or restart: `docker compose restart postgres` |
| Tests fail locally | Try `./gradlew clean test` |

---

## Learning Paths by Role

### Backend Developer

1. Days 1-3: Setup + Architecture understanding
2. Week 1: Add REST endpoint or database table
3. Week 2: Event sourcing (if working on event services)
4. Week 3+: Core service development

**Key guides**: CLAUDE.md, Liquibase Workflow, Coding Standards, TDD Swarm

### Full-Stack Developer (Backend + Frontend)

1. Days 1-3: Setup + Backend architecture
2. Week 1: Backend feature
3. Week 2: Learn Angular/Frontend patterns
4. Week 3+: Full-stack feature development

**Key guides**: Backend guides + Frontend patterns

### DevOps/Infrastructure

1. Days 1-3: Setup + Docker/Kubernetes understanding
2. Week 1: Deploy services locally
3. Week 2: Deployment automation, monitoring
4. Week 3+: Production infrastructure

**Key guides**: Deployment guide, Docker Compose usage, Operations guide

### Quality/Testing

1. Days 1-3: Setup + Testing understanding
2. Week 1: Write unit tests for service
3. Week 2: Integration tests, test containers
4. Week 3+: Test automation, quality metrics

**Key guides**: Testing Patterns, TDD Swarm, TESTING_GUIDE.md

---

## Graduation: First PR Merged ✨

Once your first PR is merged:

✅ Celebrate! (Team will congratulate in #engineering)
✅ Ask for feedback on experience
✅ Identify which area to dive deeper into
✅ Continue with more complex features

**Typical progression**:
- Week 1-2: Simple bugfixes
- Week 3-4: Small features
- Month 2: More complex features
- Month 3+: Architecture-level work

---

## Additional Resources

### Documentation
- **[CLAUDE.md](../CLAUDE.md)** - Project conventions
- **[System Architecture](../docs/architecture/SYSTEM_ARCHITECTURE.md)** - Complete overview
- **[Service Catalog](../docs/services/SERVICE_CATALOG.md)** - All services
- **[Troubleshooting Guide](../docs/troubleshooting/)** - Problem solving

### Tools & Monitoring
- **Grafana** (http://localhost:3001) - Metrics dashboard
- **Jaeger** (http://localhost:16686) - Trace visualization
- **Prometheus** (http://localhost:9090) - Raw metrics

### Code Examples
- Look at `patient-service` for CRUD pattern
- Look at `patient-event-service` for event sourcing
- Look at `quality-measure-service` for complex business logic
- Tests in each service show expected usage

---

_Last Updated: January 19, 2026_
_Version: 1.0_
_Typical Time to First PR: 2-3 weeks_
_Welcome to the team! 🚀_
