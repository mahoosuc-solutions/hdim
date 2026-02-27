#!/bin/bash
# Database Schema Validation Script
# Validates entity definitions against actual database schema

set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5435}"
DB_USER="${DB_USER:-healthdata}"
DB_PASSWORD="${DB_PASSWORD:-demo_password_2024}"
DB_FALLBACK_PASSWORD="${DB_FALLBACK_PASSWORD:-healthdata_password}"
STRICT_SCHEMA="${STRICT_SCHEMA:-0}"

echo "=== Database Schema Validation ==="
echo "Host: $DB_HOST:$DB_PORT"
echo "Strict mode: $STRICT_SCHEMA"
echo ""

MISSING_COUNT=0

# Function to check table existence
check_table() {
    local db=$1
    local table=$2
    local exists
    exists=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$db" -tAc "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '$table');" 2>/dev/null)
    local status=$?
    if [ "$status" -ne 0 ] && [ -n "$DB_FALLBACK_PASSWORD" ] && [ "$DB_FALLBACK_PASSWORD" != "$DB_PASSWORD" ]; then
        exists=$(PGPASSWORD="$DB_FALLBACK_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$db" -tAc "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '$table');" 2>/dev/null)
        status=$?
    fi
    if [ "$status" -ne 0 ]; then
        echo "ERROR"
        return 0
    fi
    echo "$exists"
}

# Function to list columns
list_columns() {
    local db=$1
    local table=$2
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$db" -c "SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name = '$table' ORDER BY ordinal_position;" 2>/dev/null || echo "ERROR"
}

# Function to list indexes
list_indexes() {
    local db=$1
    local table=$2
    PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$db" -c "SELECT indexname, indexdef FROM pg_indexes WHERE tablename = '$table';" 2>/dev/null || echo "ERROR"
}

echo "=== Audit Module Tables ==="
echo ""

# Check audit tables in fhir_db (where audit_events exists)
AUDIT_TABLES=("audit_events" "qa_reviews" "ai_agent_decision_events" "configuration_engine_events" "user_configuration_action_events" "data_quality_issues" "clinical_decisions" "mpi_merges")

for table in "${AUDIT_TABLES[@]}"; do
    exists=$(check_table "fhir_db" "$table")
    if [ "$exists" = "t" ]; then
        echo "✅ $table exists in fhir_db"
    elif [ "$exists" = "ERROR" ]; then
        echo "❌ $table check failed in fhir_db (database connectivity/auth issue)"
        MISSING_COUNT=$((MISSING_COUNT + 1))
    else
        echo "❌ $table MISSING in fhir_db"
        MISSING_COUNT=$((MISSING_COUNT + 1))
    fi
done

echo ""
echo "=== FHIR Service Tables ==="
FHIR_TABLES=("patients" "observations" "conditions" "encounters" "medication_requests")
for table in "${FHIR_TABLES[@]}"; do
    exists=$(check_table "fhir_db" "$table")
    if [ "$exists" = "t" ]; then
        echo "✅ $table exists in fhir_db"
    elif [ "$exists" = "ERROR" ]; then
        echo "❌ $table check failed in fhir_db (database connectivity/auth issue)"
        MISSING_COUNT=$((MISSING_COUNT + 1))
    else
        echo "❌ $table MISSING in fhir_db"
        MISSING_COUNT=$((MISSING_COUNT + 1))
    fi
done

echo ""
echo "=== Patient Service Tables ==="
PATIENT_TABLES=("patient_demographics" "patient_identifiers" "patient_risk_scores")
for table in "${PATIENT_TABLES[@]}"; do
    exists=$(check_table "patient_db" "$table")
    if [ "$exists" = "t" ]; then
        echo "✅ $table exists in patient_db"
    elif [ "$exists" = "ERROR" ]; then
        echo "❌ $table check failed in patient_db (database connectivity/auth issue)"
        MISSING_COUNT=$((MISSING_COUNT + 1))
    else
        echo "❌ $table MISSING in patient_db"
        MISSING_COUNT=$((MISSING_COUNT + 1))
    fi
done

echo ""
echo "=== Notification Service Tables ==="
NOTIFICATION_TABLES=("notifications" "notification_templates" "notification_preferences")
for table in "${NOTIFICATION_TABLES[@]}"; do
    exists=$(check_table "notification_db" "$table")
    if [ "$exists" = "t" ]; then
        echo "✅ $table exists in notification_db"
    elif [ "$exists" = "ERROR" ]; then
        echo "❌ $table check failed in notification_db (database connectivity/auth issue)"
        MISSING_COUNT=$((MISSING_COUNT + 1))
    else
        echo "❌ $table MISSING in notification_db"
        MISSING_COUNT=$((MISSING_COUNT + 1))
    fi
done

echo ""
echo "Validation complete! Missing table checks: $MISSING_COUNT"

if [ "$STRICT_SCHEMA" = "1" ] && [ "$MISSING_COUNT" -gt 0 ]; then
    echo "Schema validation failed in strict mode."
    exit 1
fi
