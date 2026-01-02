# HealthData in Motion - Startup Guide & Issue Resolution

## Overview
This guide helps you identify and resolve startup issues with the HealthData in Motion platform.

## Quick Start

### 1. First Time Setup
```bash
# Clean start with full build
./start-platform.sh --build

# Start with frontend
./start-platform.sh --build --with-frontend
```

### 2. Regular Startup
```bash
# Start all services
./start-platform.sh

# Check for issues
./diagnose-issues.sh
```

### 3. Troubleshooting
```bash
# Run diagnostics
./diagnose-issues.sh

# View logs
docker compose logs -f [service-name]

# Clean restart
docker compose down -v
./start-platform.sh --build
```

## Current Issues Identified & Solutions

### Issue 1: Port Conflicts
**Problem**: Port 5435 already in use by another PostgreSQL container
**Solution**:
```bash
docker ps -a | grep postgres | awk '{print $1}' | xargs docker stop
docker ps -a | grep postgres | awk '{print $1}' | xargs docker rm
```

### Issue 2: Java Version Mismatch
**Problem**: Services compiled with Java 21 but Docker images use Java 17
**Solution**: Updated all Dockerfiles to use `eclipse-temurin:21-jdk-alpine`

### Issue 3: HQL/JPA Query Errors
**Problem**: Malformed HQL queries in repository classes
**Solution**:
- Check `@Query` annotations in repository classes
- Ensure proper JSONB syntax for PostgreSQL
- Review entity relationships and fetch strategies

### Issue 4: Service Dependencies
**Problem**: Services starting before dependencies are ready
**Solution**:
- Modified docker-compose.yml to use proper health checks
- Services now start in correct order: Infrastructure → Core Services → Gateway

## Architecture

### Services & Ports
- **PostgreSQL**: 5435 (multiple databases)
- **Redis**: 6380
- **Kafka**: 9094
- **Zookeeper**: 2182
- **Gateway Service**: 8080
- **FHIR Service**: 8081
- **Patient Service**: 8082
- **Care Gap Service**: 8083
- **Event Processing Service**: 8084
- **CQL Engine Service**: 8086
- **Quality Measure Service**: 8087
- **Clinical Portal**: 4200

### Database Structure
Each service has its own database:
- `fhir_db` - FHIR resource storage
- `quality_db` - Quality measures and results
- `cql_db` - CQL engine data
- `event_db` - Event processing
- `patient_db` - Patient management
- `caregap_db` - Care gap tracking

## Development Workflow

### Backend Development
```bash
# Build backend
cd backend
./gradlew build -x test

# Run specific service locally
./gradlew :modules:services:quality-measure-service:bootRun
```

### Frontend Development
```bash
# Install dependencies
npm install

# Start development server
npx nx serve clinical-portal
```

### Docker Development
```bash
# Build specific service
docker compose build quality-measure-service

# View real-time logs
docker compose logs -f quality-measure-service

# Execute commands in container
docker exec -it healthdata-quality-measure-service sh
```

## Common Commands

### Health Checks
```bash
# Check all services
for port in 8080 8081 8082 8083 8084 8086 8087; do
  echo "Port $port: $(curl -s http://localhost:$port/actuator/health | jq -r '.status')"
done

# Database check
docker exec healthdata-postgres psql -U healthdata -d healthdata_db -c '\l'
```

### Data Management
```bash
# Load sample data
./load-sample-data.sh

# Create test users
./create-test-users.sh

# Reset database
docker compose down -v
docker compose up -d postgres
```

## Environment Variables

Key environment variables for services:
- `SPRING_PROFILES_ACTIVE=docker` - Activates Docker profile
- `SPRING_DATASOURCE_URL` - Database connection string
- `SPRING_KAFKA_BOOTSTRAP_SERVERS` - Kafka connection
- `SERVER_PORT` - Service port

## Monitoring

### Service Status
```bash
docker compose ps
```

### Resource Usage
```bash
docker stats
```

### Kafka Topics
```bash
docker exec healthdata-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

## Deployment Options

### Local Development
Use the provided scripts for easy local development.

### Production Deployment
1. Build optimized images
2. Use environment-specific configurations
3. Enable security features
4. Set up monitoring and logging

## Need Help?

1. Run diagnostics: `./diagnose-issues.sh`
2. Check service logs: `docker compose logs [service-name]`
3. Review this guide for common issues
4. Check the main README.md for project documentation

## Scripts Provided

- `start-platform.sh` - Main startup script with build options
- `diagnose-issues.sh` - Diagnostic tool for troubleshooting
- `docker-compose.yml` - Service orchestration configuration

## Next Steps

1. Start the platform: `./start-platform.sh --build`
2. Access the Gateway API: http://localhost:8080
3. View the Clinical Portal: http://localhost:4200 (if started with --with-frontend)
4. Load sample data for testing
5. Begin development or testing