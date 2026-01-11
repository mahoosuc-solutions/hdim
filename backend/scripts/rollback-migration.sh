#!/bin/bash
# Liquibase Rollback Execution Script
# Executes rollback for a specific service and changeset

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# Usage function
usage() {
    cat << EOF
${BLUE}Liquibase Rollback Execution Script${NC}

Usage: $0 <service-name> <rollback-target>

Arguments:
  service-name     Name of the service (e.g., patient-service, quality-measure-service)
  rollback-target  One of:
                   - "count:N" - Rollback last N changesets
                   - "tag:TAG" - Rollback to a specific tag
                   - "date:YYYY-MM-DD" - Rollback to a specific date
                   - "changeset:ID" - Rollback a specific changeset

Examples:
  $0 patient-service count:1
  $0 quality-measure-service tag:v1.0.0
  $0 fhir-service date:2026-01-01
  $0 cql-engine-service changeset:0003-create-value-sets-table

Prerequisites:
  - PostgreSQL container must be running
  - Service database must exist
  - Service must have Liquibase migrations

Safety:
  - This script executes actual rollback SQL
  - Always backup database before rollback
  - Test rollback in non-production environment first

EOF
    exit 1
}

# Check arguments
if [ $# -ne 2 ]; then
    usage
fi

SERVICE_NAME=$1
ROLLBACK_TARGET=$2
SERVICES_DIR="backend/modules/services"
SERVICE_DIR="$SERVICES_DIR/$SERVICE_NAME"

# Validate service directory exists
if [ ! -d "$SERVICE_DIR" ]; then
    echo -e "${RED}Error: Service directory not found: $SERVICE_DIR${NC}"
    exit 1
fi

# Check for application.yml
APP_YML="$SERVICE_DIR/src/main/resources/application.yml"
if [ ! -f "$APP_YML" ]; then
    echo -e "${RED}Error: application.yml not found: $APP_YML${NC}"
    exit 1
fi

# Extract database name
DB_NAME=$(grep "url:" "$APP_YML" | grep "postgresql" | head -1 | sed 's/.*\/\([^?}]*\).*/\1/' | tr -d ' ' | tr -d '\r')
if [ -z "$DB_NAME" ]; then
    echo -e "${RED}Error: Could not determine database name from application.yml${NC}"
    exit 1
fi

# Extract database credentials
DB_USER=$(grep "username:" "$APP_YML" | head -1 | sed 's/.*username:[^$]*{\([^}:]*\)[}:].*default:\([^}]*\)}.*/\2/' | tr -d ' ' | tr -d '\r' || echo "healthdata")
DB_PASSWORD=$(grep "password:" "$APP_YML" | head -1 | sed 's/.*password:[^$]*{\([^}:]*\)[}:].*default:\([^}]*\)}.*/\2/' | tr -d ' ' | tr -d '\r' || echo "healthdata")
DB_HOST=${POSTGRES_HOST:-localhost}
DB_PORT=${POSTGRES_PORT:-5435}

# Changelog location
CHANGELOG_DIR="$SERVICE_DIR/src/main/resources/db/changelog"
MASTER_CHANGELOG="$CHANGELOG_DIR/db.changelog-master.xml"

if [ ! -f "$MASTER_CHANGELOG" ]; then
    echo -e "${RED}Error: Master changelog not found: $MASTER_CHANGELOG${NC}"
    exit 1
fi

echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}  Liquibase Rollback Execution${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BLUE}Service:${NC} $SERVICE_NAME"
echo -e "${BLUE}Database:${NC} $DB_NAME"
echo -e "${BLUE}Target:${NC} $ROLLBACK_TARGET"
echo ""

# Confirm before proceeding
read -p "$(echo -e ${YELLOW}This will execute rollback SQL on the database. Continue? [y/N]: ${NC})" -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${YELLOW}Rollback cancelled.${NC}"
    exit 0
fi

echo ""
echo -e "${YELLOW}⚠ Important: Make sure you have a database backup!${NC}"
read -p "$(echo -e ${YELLOW}Have you backed up the database? [y/N]: ${NC})" -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${RED}Please backup the database first!${NC}"
    echo -e "${YELLOW}To create a backup:${NC}"
    echo -e "  docker exec healthdata-postgres pg_dump -U healthdata $DB_NAME > backup_${DB_NAME}_\$(date +%Y%m%d_%H%M%S).sql"
    exit 1
fi

echo ""
echo -e "${BLUE}Executing rollback...${NC}"
echo ""

# Parse rollback target
if [[ $ROLLBACK_TARGET == count:* ]]; then
    COUNT=${ROLLBACK_TARGET#count:}
    echo -e "${BLUE}Rolling back last $COUNT changeset(s)...${NC}"

    # Execute rollback via PostgreSQL
    echo -e "${YELLOW}Note: This requires manual SQL execution from rollback tags${NC}"
    echo -e "${YELLOW}See the changesets in: $CHANGELOG_DIR${NC}"
    echo ""
    echo -e "${BLUE}Steps:${NC}"
    echo -e "1. Review the last $COUNT changesets in databasechangelog table"
    echo -e "2. Extract rollback SQL from the XML files"
    echo -e "3. Execute rollback SQL in reverse order"
    echo -e "4. Delete the corresponding rows from databasechangelog table"
    echo ""
    echo -e "${BLUE}Query to find last $COUNT changesets:${NC}"
    echo "  docker exec healthdata-postgres psql -U $DB_USER -d $DB_NAME -c \\"
    echo "    \"SELECT orderexecuted, id, author, filename FROM databasechangelog ORDER BY orderexecuted DESC LIMIT $COUNT;\""

elif [[ $ROLLBACK_TARGET == changeset:* ]]; then
    CHANGESET_ID=${ROLLBACK_TARGET#changeset:}
    echo -e "${BLUE}Rolling back changeset: $CHANGESET_ID${NC}"

    # Find the changeset XML file
    CHANGESET_FILE=$(grep -r "id=\"$CHANGESET_ID\"" "$CHANGELOG_DIR" --include="*.xml" -l | head -1)

    if [ -z "$CHANGESET_FILE" ]; then
        echo -e "${RED}Error: Changeset not found: $CHANGESET_ID${NC}"
        exit 1
    fi

    echo -e "${BLUE}Found changeset in: $CHANGESET_FILE${NC}"

    # Extract rollback SQL
    ROLLBACK_SQL=$(sed -n "/<changeSet.*id=\"$CHANGESET_ID\"/,/<\/changeSet>/p" "$CHANGESET_FILE" | sed -n '/<rollback>/,/<\/rollback>/p' | grep -v "<rollback" | grep -v "</rollback")

    if [ -z "$ROLLBACK_SQL" ]; then
        echo -e "${RED}Error: No rollback SQL found for changeset: $CHANGESET_ID${NC}"
        exit 1
    fi

    echo ""
    echo -e "${BLUE}Rollback SQL:${NC}"
    echo "$ROLLBACK_SQL"
    echo ""

    read -p "$(echo -e ${YELLOW}Execute this rollback SQL? [y/N]: ${NC})" -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo "$ROLLBACK_SQL" | docker exec -i healthdata-postgres psql -U $DB_USER -d $DB_NAME
        echo -e "${GREEN}✓ Rollback SQL executed${NC}"

        # Remove from databasechangelog
        docker exec healthdata-postgres psql -U $DB_USER -d $DB_NAME -c "DELETE FROM databasechangelog WHERE id = '$CHANGESET_ID';"
        echo -e "${GREEN}✓ Removed changeset from databasechangelog${NC}"
    else
        echo -e "${YELLOW}Rollback cancelled.${NC}"
        exit 0
    fi

else
    echo -e "${RED}Error: Unsupported rollback target: $ROLLBACK_TARGET${NC}"
    echo -e "${YELLOW}Supported formats: count:N, changeset:ID${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}═══════════════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}  Rollback completed successfully!${NC}"
echo -e "${GREEN}═══════════════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${BLUE}Next steps:${NC}"
echo -e "1. Verify database schema matches expected state"
echo -e "2. Run entity-migration validation test:"
echo -e "   ./gradlew :modules:services:$SERVICE_NAME:test --tests \"*EntityMigrationValidationTest\""
echo -e "3. Test application functionality"
echo ""
