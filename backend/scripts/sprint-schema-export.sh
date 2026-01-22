#!/bin/bash
#
# Sprint Schema Export Tool
# ==========================
#
# Purpose: Generate Liquibase migration from entity changes made during a sprint.
#
# Usage:
#   ./scripts/sprint-schema-export.sh <service-name> <sprint-id>
#
# Example:
#   ./scripts/sprint-schema-export.sh quality-measure-service sprint-24
#
# This script:
# 1. Starts the service with H2 in-memory database (auto-generates schema from entities)
# 2. Exports the H2 schema to SQL
# 3. Compares with current PostgreSQL schema
# 4. Generates a Liquibase diff migration
# 5. Creates human-readable migration file for review
#
# Workflow:
#   Development → H2 (auto-schema) → Sprint End → Generate Migration → Review → Commit
#

set -e  # Exit on error

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Parse arguments
SERVICE_NAME=$1
SPRINT_ID=$2

if [ -z "$SERVICE_NAME" ] || [ -z "$SPRINT_ID" ]; then
    echo -e "${RED}Usage: $0 <service-name> <sprint-id>${NC}"
    echo ""
    echo "Example: $0 quality-measure-service sprint-24"
    exit 1
fi

# Derived variables
SERVICE_DIR="modules/services/${SERVICE_NAME}"
DB_NAME="${SERVICE_NAME//-/_}_db"
CHANGELOG_DIR="${SERVICE_DIR}/src/main/resources/db/changelog"
NEXT_MIGRATION_NUM=$(ls ${CHANGELOG_DIR}/*.xml 2>/dev/null | grep -oP '\d{4}' | sort -n | tail -1 | awk '{printf "%04d\n", $1 + 1}')
MIGRATION_FILE="${CHANGELOG_DIR}/${NEXT_MIGRATION_NUM}-${SPRINT_ID}-schema-changes.xml"
TEMP_DIR="/tmp/sprint-schema-${SERVICE_NAME}-${SPRINT_ID}"

echo -e "${BLUE}==================================================${NC}"
echo -e "${BLUE}Sprint Schema Export Tool${NC}"
echo -e "${BLUE}==================================================${NC}"
echo ""
echo -e "${GREEN}Service:${NC} ${SERVICE_NAME}"
echo -e "${GREEN}Sprint:${NC} ${SPRINT_ID}"
echo -e "${GREEN}Database:${NC} ${DB_NAME}"
echo -e "${GREEN}Next Migration:${NC} ${NEXT_MIGRATION_NUM}"
echo ""

# Check service exists
if [ ! -d "$SERVICE_DIR" ]; then
    echo -e "${RED}Error: Service directory not found: ${SERVICE_DIR}${NC}"
    exit 1
fi

# Create temp directory
mkdir -p "$TEMP_DIR"

echo -e "${YELLOW}Step 1: Exporting current PostgreSQL schema...${NC}"
docker exec healthdata-postgres pg_dump \
    -U healthdata \
    -d "$DB_NAME" \
    --schema-only \
    --no-owner \
    --no-privileges \
    > "${TEMP_DIR}/postgres-current.sql"
echo -e "${GREEN}✓ PostgreSQL schema exported${NC}"

echo ""
echo -e "${YELLOW}Step 2: Generating H2 schema from entities...${NC}"
echo -e "${BLUE}This step requires the service to have a schema export endpoint.${NC}"
echo -e "${BLUE}Alternatively, we'll use Hibernate schema generation.${NC}"

# Create temporary application-schema-export.yml
cat > "${TEMP_DIR}/application-schema-export.yml" <<EOF
spring:
  datasource:
    url: jdbc:h2:file:${TEMP_DIR}/h2db
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create
    show-sql: false
    properties:
      hibernate:
        format_sql: false
        dialect: org.hibernate.dialect.PostgreSQLDialect
  liquibase:
    enabled: false
EOF

# Run Hibernate schema generation
echo -e "${BLUE}Generating Hibernate schema...${NC}"
./gradlew :${SERVICE_DIR}:bootRun --args="--spring.config.additional-location=file:${TEMP_DIR}/application-schema-export.yml --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.action=create --spring.jpa.properties.jakarta.persistence.schema-generation.scripts.create-target=${TEMP_DIR}/h2-schema.sql" &
BOOT_PID=$!

# Wait for schema generation (or timeout after 30 seconds)
sleep 30
kill $BOOT_PID 2>/dev/null || true

if [ ! -f "${TEMP_DIR}/h2-schema.sql" ]; then
    echo -e "${RED}Error: Failed to generate H2 schema${NC}"
    echo -e "${YELLOW}Tip: Ensure service has proper entity scanning configuration${NC}"
    exit 1
fi

echo -e "${GREEN}✓ H2 schema generated${NC}"

echo ""
echo -e "${YELLOW}Step 3: Comparing schemas and generating diff...${NC}"

# Use Liquibase diff command
./gradlew :${SERVICE_DIR}:liquibaseDiffChangeLog \
    -PrunList=diffChangeLog \
    -PreferenceUrl="jdbc:postgresql://localhost:5435/${DB_NAME}" \
    -PreferenceUsername=healthdata \
    -PreferencePassword=healthdata_password \
    -Purl="jdbc:h2:file:${TEMP_DIR}/h2db" \
    -Pusername=sa \
    -Ppassword= \
    -PchangeLogFile="${MIGRATION_FILE}"

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Migration generated: ${MIGRATION_FILE}${NC}"
else
    echo -e "${RED}Error: Failed to generate migration${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Step 4: Reviewing generated migration...${NC}"
echo ""
cat "$MIGRATION_FILE"
echo ""

echo -e "${YELLOW}Step 5: Adding migration to master changelog...${NC}"
MASTER_CHANGELOG="${CHANGELOG_DIR}/db.changelog-master.xml"

# Add include before closing tag
sed -i "s|</databaseChangeLog>|    <!-- ${SPRINT_ID}: Schema changes from sprint ${SPRINT_ID} -->\n    <include file=\"db/changelog/${NEXT_MIGRATION_NUM}-${SPRINT_ID}-schema-changes.xml\"/>\n\n</databaseChangeLog>|" "$MASTER_CHANGELOG"

echo -e "${GREEN}✓ Migration added to master changelog${NC}"

echo ""
echo -e "${BLUE}==================================================${NC}"
echo -e "${GREEN}Sprint Schema Export Complete!${NC}"
echo -e "${BLUE}==================================================${NC}"
echo ""
echo -e "${YELLOW}Next Steps:${NC}"
echo "1. Review the generated migration: ${MIGRATION_FILE}"
echo "2. Test the migration: ./gradlew :${SERVICE_DIR}:test --tests '*EntityMigrationValidationTest'"
echo "3. Commit the migration with your sprint code:"
echo "   git add ${MIGRATION_FILE} ${MASTER_CHANGELOG}"
echo "   git add ${SERVICE_DIR}/src/main/java/..."
echo "   git commit -m \"feat(${SPRINT_ID}): Complete sprint ${SPRINT_ID} with schema migration\""
echo ""
echo -e "${YELLOW}Cleanup:${NC}"
echo "Temp files are in: ${TEMP_DIR}"
echo "Run: rm -rf ${TEMP_DIR}"
echo ""
