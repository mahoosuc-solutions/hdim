# Test Harness Database Setup

## Issue

Services are failing to connect because the `healthdata` database doesn't exist:

```
FATAL: database "healthdata" does not exist
```

## Quick Fix

Run the database setup script:

```bash
./scripts/setup-test-harness-database.sh
```

This will:
1. Find the PostgreSQL container
2. Check if `healthdata` database exists
3. Create it if missing
4. Restart affected services

## Manual Setup

If you prefer to do it manually:

```bash
# Find PostgreSQL container
docker ps --filter "name=postgres"

# Create database
docker exec -it <postgres-container> psql -U postgres -c "CREATE DATABASE healthdata;"

# Restart services
docker-compose restart care-gap-service patient-service quality-measure-service
```

## Verify

Check database exists:
```bash
docker exec -it <postgres-container> psql -U postgres -c "\l" | grep healthdata
```

Check service health:
```bash
curl http://localhost:18080/care-gap/actuator/health
curl http://localhost:18080/patient/actuator/health
curl http://localhost:18080/quality-measure/actuator/health
```

## Services Affected

These services need the `healthdata` database:
- `care-gap-service`
- `patient-service`
- `quality-measure-service`
- `fhir-service`
- `demo-seeding-service`

After creating the database, restart these services:
```bash
docker-compose restart care-gap-service patient-service quality-measure-service fhir-service demo-seeding-service
```

## Database Migrations

After creating the database, you may need to run migrations:

```bash
# Check if migrations are needed
# (This depends on your backend setup)

# For Spring Boot with Flyway/Liquibase
# Migrations typically run automatically on service startup
```

## Troubleshooting

### Database still not found
- Verify PostgreSQL container is running: `docker ps | grep postgres`
- Check database was created: `docker exec <container> psql -U postgres -l`
- Verify service configuration points to correct database name

### Services still failing
- Check service logs: `docker logs <service-name>`
- Verify database connection string in service configuration
- Ensure services are restarted after database creation

---

**Last Updated**: January 15, 2026
