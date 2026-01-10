---
name: hdim-service-create
description: Scaffold a new HDIM microservice with complete structure
category: hdim
---

# HDIM Service Create Command

Quickly scaffold a new HDIM microservice with all required structure and configurations.

## Usage

```
/hdim-service-create <service-name> <port>
```

**Parameters**:
- `service-name` - Name of the service (e.g., prescription-service)
- `port` - Port number for the service (e.g., 8091)

## What This Command Creates

1. **Directory Structure**:
   - `api/v1/` - REST controllers
   - `application/` - Application services
   - `domain/model/` - Domain entities
   - `domain/repository/` - Data repositories
   - `config/` - Spring configuration
   - `infrastructure/` - External integrations

2. **Configuration Files**:
   - `build.gradle.kts` - Gradle build configuration
   - `application.yml` - Spring Boot configuration
   - `db.changelog-master.xml` - Liquibase setup

3. **Java Classes**:
   - Spring Boot application class
   - Security configuration (Gateway Trust pattern)
   - Entity-migration validation test

4. **Updates**:
   - `settings.gradle.kts` - Adds service to build

## Example

```bash
# Create a new prescription service on port 8091
/hdim-service-create prescription-service 8091
```

## Implementation

```bash
#!/bin/bash

if [ "$#" -ne 2 ]; then
    echo "Usage: /hdim-service-create <service-name> <port>"
    echo "Example: /hdim-service-create prescription-service 8091"
    exit 1
fi

SERVICE_NAME=$1
PORT=$2

echo "🚀 Creating HDIM service: $SERVICE_NAME (port $PORT)"
bash .serena/workflows/new-service-setup.sh "$SERVICE_NAME" "$PORT"

echo ""
echo "✅ Service created successfully!"
echo ""
echo "📋 Next steps:"
echo "   1. Add service to docker-compose.yml"
echo "   2. Create initial migration"
echo "   3. Define domain entities"
echo "   4. Implement API controllers"
echo "   5. Add to .serena/SERVICE_INDEX.md"
```

## After Creation

The service will have:
- ✅ Proper security (Gateway Trust authentication)
- ✅ HIPAA-compliant configuration (5-minute cache TTL)
- ✅ Multi-tenant setup (tenantId filtering)
- ✅ Entity-migration validation test
- ✅ Complete Gradle build setup

## Build & Run

```bash
# Build
cd backend && ./gradlew :modules:services:<service-name>:build

# Run
cd backend && ./gradlew :modules:services:<service-name>:bootRun
```

## Related Commands

- `/hdim-validate` - Validate the new service
- `/hdim-memory` - Access development patterns
- `/dev:implement` - Implement service features
