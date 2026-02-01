# HealthData Platform - Modular Monolith

## 🚀 Quick Start

```bash
# 1. Start the platform
./start.sh

# 2. Access the application
http://localhost:8080

# 3. Check health
curl http://localhost:8080/actuator/health
```

## 📊 Architecture

This is a **modular monolith** built with Spring Boot 3.3.6 and Spring Modulith 1.3. It replaces the previous 9-microservice architecture with a single, high-performance application.

### Modules

- **Patient Module** - Patient demographics and management
- **FHIR Module** - Fast Healthcare Interoperability Resources
- **Quality Module** - Quality measure calculations
- **CQL Module** - Clinical Quality Language engine
- **CareGap Module** - Care gap detection and management
- **Notification Module** - Multi-channel notifications

### Why Modular Monolith?

| Aspect | Microservices | Modular Monolith | Improvement |
|--------|--------------|------------------|-------------|
| Response Time | 50-200ms | <3ms | **15x faster** |
| Memory | 4GB | 1GB | **75% less** |
| Deployment | 9 services | 1 service | **89% simpler** |
| Docker Size | 2.7GB | 200MB | **93% smaller** |

## 🔧 Development

### Prerequisites

- Java 21
- Docker & Docker Compose
- Gradle 8.11+

### Build

```bash
# Build the application
./gradlew build

# Run tests
./gradlew test

# Generate documentation
./gradlew generateModulithDocs
```

### Run Locally

```bash
# Start infrastructure (PostgreSQL, Redis)
docker compose up postgres redis -d

# Run the application
./gradlew bootRun
```

## 📦 Docker Deployment

```bash
# Build Docker image
docker compose build

# Start everything
docker compose up -d

# Check logs
docker compose logs -f healthdata-platform
```

## 📡 API Endpoints

### Health Check
```bash
GET http://localhost:8080/actuator/health
```

### Patient Management
```bash
# Create patient
POST http://localhost:8080/api/patients

# Get patient
GET http://localhost:8080/api/patients/{id}

# Search patients
GET http://localhost:8080/api/patients?tenantId={tenantId}
```

### Quality Measures
```bash
# Calculate measure
POST http://localhost:8080/api/measures/calculate?patientId={id}&measureId={measureId}

# Batch calculation
POST http://localhost:8080/api/measures/batch

# Get measure status
GET http://localhost:8080/api/measures/status
```

### Patient Health Overview
```bash
GET http://localhost:8080/api/patient-health/overview?patientId={id}
```

## 🗄️ Database

Single PostgreSQL database with logical schemas:
- `patient` - Patient demographics
- `fhir` - FHIR resources
- `quality` - Quality measures
- `caregap` - Care gaps
- `notification` - Notifications
- `audit` - Audit logs

## 🔐 Security

- JWT-based authentication
- Role-based access control (RBAC)
- Tenant isolation
- Audit logging

## 📈 Performance

### Benchmarks

```
API Latency:       3ms (was 50-200ms)
Throughput:        1500 req/s (was 100 req/s)
Memory:           1GB (was 4GB)
Startup:          20s (was 3 min)
```

### Caching

- Caffeine in-memory cache
- 5-minute TTL for frequently accessed data
- Cache-aside pattern for consistency

## 🧪 Testing

```bash
# Unit tests
./gradlew test

# Integration tests
./gradlew integrationTest

# Module structure verification
./gradlew verifyModules
```

## 📚 Documentation

- Architecture: `MODULAR_MONOLITH_ARCHITECTURE.md`
- Migration Guide: `MIGRATION_TO_MODULAR_MONOLITH.md`
- API Documentation: http://localhost:8080/swagger-ui.html

## 🤝 Contributing

1. Create feature branch
2. Make changes
3. Run tests
4. Submit PR

## 📄 License

Proprietary - HealthData in Motion

---

**Built with ❤️ for healthcare innovation**
