# Demo Implementation & Service Tiers Plan

**Created**: January 15, 2026  
**Status**: Planning  
**Purpose**: Define bare minimum demo implementation and service tiers for testing and offering

---

## Executive Summary

Based on comprehensive review of customer requirements, demo scenarios, and pricing strategies, this document defines:

1. **Bare Minimum Demo Implementation** - Absolute minimum services needed to demonstrate core value
2. **Service Tiers** - Progressive service configurations for different customer segments
3. **Testing Strategy** - How to validate each tier
4. **Offering Strategy** - How to package and sell each tier

---

## Part 1: What Customers Want to See

### Core Demo Scenarios (From Documentation Review)

#### Scenario 1: HEDIS Quality Measure Evaluation (3-5 min)
**Value Prop**: Automate quality measures, identify care gaps at scale  
**Key Metrics**: 5,000 patients in 12 seconds, 247 care gaps, 22x ROI  
**Required Capabilities**:
- Quality measure evaluation (CQL execution)
- Care gap identification
- Patient data access (FHIR)
- Dashboard visualization
- Export capabilities

#### Scenario 2: Patient Care Journey (4-6 min)
**Value Prop**: 360° patient view with clinical decision support  
**Required Capabilities**:
- Patient search and detail view
- FHIR timeline (encounters, conditions, medications, labs)
- Care gap display per patient
- Risk scoring (HCC)
- SDOH screening (optional)

#### Scenario 3: Risk Stratification & Analytics (3-4 min)
**Value Prop**: Identify high-risk patients before costly events  
**Key Metrics**: 92% accuracy, $450K cost avoidance  
**Required Capabilities**:
- Risk stratification (HCC scoring)
- Predictive analytics
- Population analytics dashboard
- High-risk patient identification

#### Scenario 4: Multi-Tenant Administration (2-3 min)
**Value Prop**: Secure, scalable SaaS platform  
**Required Capabilities**:
- Multi-tenant data isolation
- User management
- Tenant configuration
- Audit logging

### Customer Segments (From ROI Calculator & Landing Page)

1. **Solo Practice** (50K patients)
   - Focus: Basic HEDIS, care gaps, labor savings
   - Budget: $49-299/month
   - ROI: 517% (moderate scenario)

2. **Regional Health System** (500K patients)
   - Focus: Multi-EHR, quality bonuses, automation
   - Budget: $2,499-10,000/month
   - ROI: 5,067% (moderate scenario)

3. **ACO Network** (150K patients, multi-EHR)
   - Focus: Unified reporting, coordination, real-time
   - Budget: $999-2,499/month
   - ROI: 2,025% (moderate scenario)

4. **Payer** (500K members)
   - Focus: Star ratings, member engagement, quality bonuses
   - Budget: Custom ($10K-50K/month)
   - ROI: 10,483% (moderate scenario)

---

## Part 2: Bare Minimum Demo Implementation

### Absolute Minimum Services (8 services)

**Infrastructure (3)**:
1. `postgres` - Database (all service databases)
2. `redis` - Cache and sessions
3. `kafka` + `zookeeper` - Event streaming (for audit events)

**Core Services (4)**:
4. `gateway-service` - API Gateway (routing, auth)
5. `fhir-service` - FHIR R4 server (patient data)
6. `cql-engine-service` - CQL measure evaluation
7. `care-gap-service` - Care gap identification

**Frontend (1)**:
8. `clinical-portal` - Angular frontend

**Total**: 8 services (3 infra + 4 core + 1 frontend)

### Why This Is Minimum

**Can Demonstrate**:
- ✅ HEDIS measure evaluation (CQL Engine)
- ✅ Care gap identification (Care Gap Service)
- ✅ Patient data access (FHIR Service)
- ✅ Basic dashboard (Clinical Portal)
- ✅ Multi-tenant isolation (Gateway)

**Cannot Demonstrate**:
- ❌ Risk stratification (needs HCC Service)
- ❌ Advanced analytics (needs Analytics Service)
- ❌ Quality measure management (needs Quality Measure Service)
- ❌ Patient service features (needs Patient Service)

### Minimal Demo Data Requirements

**Tenants**: 1 demo tenant (`acme-health`)
**Patients**: 10-50 patients (enough for demo, not overwhelming)
**Care Gaps**: 15-25 gaps across 3-5 HEDIS measures
**Measures**: 3-5 core measures (BCS, COL, CDC-E)
**Users**: 1 demo user (admin role)

### Minimal Demo Docker Compose

```yaml
# demo/docker-compose.minimal.yml
version: '3.8'

services:
  # Infrastructure
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: healthdata_demo
      POSTGRES_USER: healthdata
      POSTGRES_PASSWORD: demo_password_2024
    ports:
      - "5435:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U healthdata"]
      interval: 5s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6380:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5

  zookeeper:
    image: confluentinc/cp-zookeeper:7.4.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    healthcheck:
      test: ["CMD", "nc", "-z", "localhost", "2181"]
      interval: 10s
      timeout: 5s
      retries: 5

  kafka:
    image: confluentinc/cp-kafka:7.4.0
    depends_on:
      - zookeeper
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    healthcheck:
      test: ["CMD", "kafka-broker-api-versions", "--bootstrap-server", "localhost:29092"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Core Services
  gateway-service:
    build:
      context: ./backend
      dockerfile: modules/services/gateway-service/Dockerfile
    depends_on:
      - postgres
      - redis
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/gateway_db
      SPRING_DATASOURCE_USERNAME: healthdata
      SPRING_DATASOURCE_PASSWORD: demo_password_2024
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      SPRING_REDIS_HOST: redis
      SPRING_REDIS_PORT: 6379
    ports:
      - "8080:8080"
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5

  fhir-service:
    build:
      context: ./backend
      dockerfile: modules/services/fhir-service/Dockerfile
    depends_on:
      - postgres
      - gateway-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/fhir_db
      SPRING_DATASOURCE_USERNAME: healthdata
      SPRING_DATASOURCE_PASSWORD: demo_password_2024
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
    ports:
      - "8085:8085"
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:8085/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5

  cql-engine-service:
    build:
      context: ./backend
      dockerfile: modules/services/cql-engine-service/Dockerfile
    depends_on:
      - postgres
      - gateway-service
      - fhir-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/cql_db
      SPRING_DATASOURCE_USERNAME: healthdata
      SPRING_DATASOURCE_PASSWORD: demo_password_2024
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      FHIR_SERVICE_URL: http://fhir-service:8085
    ports:
      - "8081:8081"
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5

  care-gap-service:
    build:
      context: ./backend
      dockerfile: modules/services/care-gap-service/Dockerfile
    depends_on:
      - postgres
      - gateway-service
      - cql-engine-service
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/caregap_db
      SPRING_DATASOURCE_USERNAME: healthdata
      SPRING_DATASOURCE_PASSWORD: demo_password_2024
      SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:29092
      CQL_ENGINE_SERVICE_URL: http://cql-engine-service:8081
    ports:
      - "8086:8086"
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:8086/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 5

  # Frontend
  clinical-portal:
    build:
      context: ./apps/clinical-portal
    depends_on:
      - gateway-service
    environment:
      BACKEND_API_URL: http://gateway-service:8080
    ports:
      - "4200:4200"
    healthcheck:
      test: ["CMD", "wget", "--spider", "-q", "http://localhost:4200"]
      interval: 30s
      timeout: 10s
      retries: 5

networks:
  default:
    name: hdim-minimal-demo
```

### Minimal Demo Initialization

**Database Setup**:
- Create databases: `gateway_db`, `fhir_db`, `cql_db`, `caregap_db`
- Run Liquibase migrations
- Create demo user: `demo_admin` / `demo_password`

**Demo Data**:
- 10-50 synthetic patients (FHIR R4)
- 3-5 HEDIS measures (BCS, COL, CDC-E)
- 15-25 care gaps
- 1 tenant: `acme-health`

**Startup Time**: < 2 minutes (all services healthy)

---

## Part 3: Service Tiers for Testing and Offering

### Tier 1: Minimal Demo (Proof of Concept)

**Services**: 8 (3 infra + 4 core + 1 frontend)  
**Use Case**: Quick demo, POC, evaluation  
**Duration**: 15-30 minutes  
**Data**: 10-50 patients, 15-25 gaps

**What It Demonstrates**:
- ✅ Core HEDIS evaluation
- ✅ Care gap identification
- ✅ Basic patient view
- ✅ Multi-tenant architecture

**What It Doesn't Demonstrate**:
- ❌ Risk stratification
- ❌ Advanced analytics
- ❌ Quality measure management
- ❌ Patient service features

**Testing Requirements**:
- All services start successfully
- Demo user can login
- Can run CQL evaluation
- Can view care gaps
- Can view patient details

**Offering Strategy**:
- Free trial / POC
- Developer tier ($80/month)
- Community tier ($49/month) - limited to 2,500 patients

---

### Tier 2: Standard Demo (Sales Demo)

**Services**: 11 (3 infra + 7 core + 1 frontend)

**Additional Services**:
- `patient-service` - Patient data aggregation
- `quality-measure-service` - Measure management
- `elasticsearch` - FHIR search

**Use Case**: Full sales demonstration, customer evaluation  
**Duration**: 30-60 minutes  
**Data**: 50-500 patients, 100-500 gaps, 3 tenants

**What It Demonstrates**:
- ✅ Everything in Tier 1
- ✅ Quality measure management
- ✅ Patient service features
- ✅ Multi-tenant isolation (3 tenants)
- ✅ FHIR search capabilities

**Testing Requirements**:
- All Tier 1 tests pass
- Can manage quality measures
- Can search patients
- Can switch tenants
- Performance: < 2s dashboard load

**Offering Strategy**:
- Professional tier ($299/month) - up to 15,000 patients
- Business tier ($2,500/month) - up to 50,000 patients

---

### Tier 3: Advanced Demo (Enterprise Demo)

**Services**: 14 (5 infra + 8 core + 1 frontend)

**Additional Services**:
- `hcc-service` - Risk scoring
- `predictive-analytics-service` - Predictive analytics
- `jaeger` - Distributed tracing (optional)
- `prometheus` + `grafana` - Monitoring (optional)

**Use Case**: Enterprise sales, technical deep-dive  
**Duration**: 60-90 minutes  
**Data**: 500-5,000 patients, 500-2,500 gaps, 3 tenants

**What It Demonstrates**:
- ✅ Everything in Tier 2
- ✅ Risk stratification (HCC scoring)
- ✅ Predictive analytics
- ✅ Population health analytics
- ✅ Monitoring and observability

**Testing Requirements**:
- All Tier 2 tests pass
- Can calculate HCC scores
- Can view risk stratification
- Can run predictive analytics
- Performance: < 15s for 5K patient evaluation

**Offering Strategy**:
- Enterprise tier ($999/month) - up to 75,000 patients
- Enterprise Plus tier ($2,499/month) - up to 200,000 patients

---

### Tier 4: Full Platform Demo (Enterprise Plus)

**Services**: 19 (7 infra + 11 core + 1 frontend)

**Additional Services**:
- `event-processing-service` - Event processing
- `event-router-service` - Event routing
- `cdr-processor-service` - Data processing
- `analytics-service` - Advanced analytics
- `notification-service` - Notifications

**Use Case**: Enterprise evaluation, technical architecture review  
**Duration**: 90-120 minutes  
**Data**: 5,000-50,000 patients, 2,500-25,000 gaps, 3 tenants

**What It Demonstrates**:
- ✅ Everything in Tier 3
- ✅ Event-driven architecture
- ✅ Data processing pipeline
- ✅ Advanced analytics
- ✅ Notification system
- ✅ Full audit trail

**Testing Requirements**:
- All Tier 3 tests pass
- Event processing works
- Audit events published to Kafka
- Dead letter queue operational
- Full end-to-end workflows

**Offering Strategy**:
- Health System tier (Custom pricing) - unlimited patients
- On-premise deployments
- Private cloud deployments

---

## Part 4: Service Tier Comparison Matrix

| Feature | Tier 1: Minimal | Tier 2: Standard | Tier 3: Advanced | Tier 4: Full Platform |
|---------|-----------------|------------------|------------------|----------------------|
| **Services** | 8 | 11 | 14 | 19 |
| **Infrastructure** | Postgres, Redis, Kafka | + Elasticsearch | + Jaeger, Prometheus | + All observability |
| **Core Services** | Gateway, FHIR, CQL, Care Gap | + Patient, Quality | + HCC, Analytics | + Events, CDR, Notifications |
| **Patients** | 10-50 | 50-500 | 500-5,000 | 5,000-50,000 |
| **Care Gaps** | 15-25 | 100-500 | 500-2,500 | 2,500-25,000 |
| **Tenants** | 1 | 3 | 3 | 3 |
| **Demo Duration** | 15-30 min | 30-60 min | 60-90 min | 90-120 min |
| **Use Case** | POC, Quick Demo | Sales Demo | Enterprise Demo | Architecture Review |
| **Pricing Tier** | Community ($49) | Professional ($299) | Enterprise ($999) | Health System (Custom) |
| **Testing Focus** | Basic functionality | Full workflows | Advanced features | Enterprise scale |

---

## Part 5: Testing Strategy by Tier

### Tier 1 Testing Checklist

**Infrastructure**:
- [ ] Postgres starts and databases created
- [ ] Redis starts and accepts connections
- [ ] Kafka starts and topics created
- [ ] All services can resolve hostnames

**Core Functionality**:
- [ ] Demo user can login
- [ ] Can view dashboard
- [ ] Can run CQL evaluation (1 measure, 10 patients)
- [ ] Care gaps appear after evaluation
- [ ] Can view patient details
- [ ] Can view care gap details

**Performance**:
- [ ] Dashboard loads in < 3 seconds
- [ ] CQL evaluation completes in < 30 seconds (10 patients)
- [ ] Patient detail loads in < 2 seconds

---

### Tier 2 Testing Checklist

**All Tier 1 tests**:
- [ ] All Tier 1 tests pass

**Additional Functionality**:
- [ ] Can manage quality measures (list, view, configure)
- [ ] Can search patients (by name, MRN, DOB)
- [ ] Can switch between tenants
- [ ] Tenant data isolation verified
- [ ] FHIR search works (Patient, Observation, Condition)

**Performance**:
- [ ] Dashboard loads in < 2 seconds
- [ ] Patient search returns results in < 1 second
- [ ] CQL evaluation completes in < 60 seconds (500 patients)

---

### Tier 3 Testing Checklist

**All Tier 2 tests**:
- [ ] All Tier 2 tests pass

**Additional Functionality**:
- [ ] Can calculate HCC scores
- [ ] Can view risk stratification dashboard
- [ ] Can run predictive analytics
- [ ] Can view population health metrics
- [ ] Monitoring dashboards accessible (Grafana)

**Performance**:
- [ ] HCC calculation completes in < 5 seconds (100 patients)
- [ ] Risk stratification dashboard loads in < 3 seconds
- [ ] CQL evaluation completes in < 15 seconds (5,000 patients)

---

### Tier 4 Testing Checklist

**All Tier 3 tests**:
- [ ] All Tier 3 tests pass

**Additional Functionality**:
- [ ] Event processing service operational
- [ ] Dead letter queue accessible
- [ ] Audit events published to Kafka
- [ ] Event router processes events
- [ ] CDR processor can process data
- [ ] Notifications can be sent
- [ ] Full audit trail visible

**Performance**:
- [ ] Event processing latency < 100ms
- [ ] Audit event publishing < 50ms
- [ ] End-to-end workflow < 30 seconds (evaluation → gap → notification)

---

## Part 6: Offering Strategy

### Tier 1: Minimal Demo
**Target**: Developers, POC evaluators, small practices  
**Pricing**: Free trial, $49/month (Community), $80/month (Developer)  
**Deployment**: Docker Compose, single VM  
**Support**: Community forum, email  
**Timeline**: 1-2 weeks setup

### Tier 2: Standard Demo
**Target**: Small ACOs, practices, FQHCs  
**Pricing**: $299/month (Professional), $2,500/month (Business)  
**Deployment**: SaaS Multi-Tenant, Docker Compose  
**Support**: Email + phone, 48-hour response  
**Timeline**: 2-4 weeks setup

### Tier 3: Advanced Demo
**Target**: Mid-size ACOs, regional health systems  
**Pricing**: $999/month (Enterprise), $2,499/month (Enterprise Plus)  
**Deployment**: SaaS Dedicated, Private Cloud  
**Support**: Priority support, 24-hour response, dedicated CSM  
**Timeline**: 4-6 weeks setup

### Tier 4: Full Platform
**Target**: Large health systems, payers, state programs  
**Pricing**: Custom ($10K-50K/month)  
**Deployment**: On-Premise, Private Cloud, Dedicated Environment  
**Support**: Named engineer, custom SLA, quarterly reviews  
**Timeline**: 8-12 weeks setup

---

## Part 7: Implementation Roadmap

### Phase 1: Minimal Demo (Week 1-2)
- [ ] Create `docker-compose.minimal.yml`
- [ ] Create minimal initialization script
- [ ] Create minimal demo data (10 patients, 15 gaps)
- [ ] Test Tier 1 functionality
- [ ] Document minimal demo walkthrough

### Phase 2: Standard Demo (Week 3-4)
- [ ] Add Patient Service, Quality Measure Service, Elasticsearch
- [ ] Create standard demo data (50-500 patients)
- [ ] Test Tier 2 functionality
- [ ] Update demo walkthrough

### Phase 3: Advanced Demo (Week 5-6)
- [ ] Add HCC Service, Predictive Analytics Service
- [ ] Add monitoring (Jaeger, Prometheus, Grafana)
- [ ] Create advanced demo data (500-5,000 patients)
- [ ] Test Tier 3 functionality

### Phase 4: Full Platform Demo (Week 7-8)
- [ ] Add event processing, CDR processor, analytics
- [ ] Create full platform demo data (5,000-50,000 patients)
- [ ] Test Tier 4 functionality
- [ ] Complete documentation

---

## Part 8: Success Criteria

### Tier 1 Success
- ✅ All 8 services start in < 2 minutes
- ✅ Demo user can complete full workflow in < 15 minutes
- ✅ Zero critical errors
- ✅ Performance targets met

### Tier 2 Success
- ✅ All Tier 1 criteria met
- ✅ Can demonstrate all 4 core demo scenarios
- ✅ Multi-tenant isolation verified
- ✅ Performance targets met

### Tier 3 Success
- ✅ All Tier 2 criteria met
- ✅ Risk stratification and analytics work
- ✅ Monitoring dashboards functional
- ✅ Performance targets met

### Tier 4 Success
- ✅ All Tier 3 criteria met
- ✅ Event processing and audit operational
- ✅ Full platform capabilities demonstrated
- ✅ Enterprise-scale performance validated

---

## Next Steps

1. **Immediate**: Implement Tier 1 (Minimal Demo)
2. **Short-term**: Implement Tier 2 (Standard Demo)
3. **Medium-term**: Implement Tier 3 (Advanced Demo)
4. **Long-term**: Implement Tier 4 (Full Platform Demo)

---

**Document Owner**: Engineering & Product Teams  
**Review Cadence**: Monthly  
**Last Updated**: January 15, 2026
