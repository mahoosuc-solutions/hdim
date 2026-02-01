#!/bin/bash
# Database Schema Validation Script
# Validates entity definitions against actual database schema

set -e

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5435}"
DB_USER="${DB_USER:-healthdata}"
DB_PASSWORD="${DB_PASSWORD:-healthdata_password}"

echo "=== Database Schema Validation ==="
echo "Host: $DB_HOST:$DB_PORT"
echo ""

# Function to check table existence
check_table() {
    local db=$1
    local table=$2
    local exists=$(PGPASSWORD="$DB_PASSWORD" psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$db" -tAc "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '$table');" 2>/dev/null || echo "false")
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
    else
        echo "❌ $table MISSING in fhir_db"
    fi
done

echo ""
echo "=== FHIR Service Tables ==="
FHIR_TABLES=("patients" "observations" "conditions" "encounters" "medication_requests")
for table in "${FHIR_TABLES[@]}"; do
    exists=$(check_table "fhir_db" "$table")
    if [ "$exists" = "t" ]; then
        echo "✅ $table exists in fhir_db"
    else
        echo "❌ $table MISSING in fhir_db"
    fi
done

echo ""
echo "=== Patient Service Tables ==="
PATIENT_TABLES=("patient_demographics" "patient_identifiers" "patient_risk_scores")
for table in "${PATIENT_TABLES[@]}"; do
    exists=$(check_table "patient_db" "$table")
    if [ "$exists" = "t" ]; then
        echo "✅ $table exists in patient_db"
    else
        echo "❌ $table MISSING in patient_db"
    fi
done

echo ""
echo "=== Notification Service Tables ==="
NOTIFICATION_TABLES=("notifications" "notification_templates" "notification_preferences")
for table in "${NOTIFICATION_TABLES[@]}"; do
    exists=$(check_table "notification_db" "$table")
    if [ "$exists" = "t" ]; then
        echo "✅ $table exists in notification_db"
    else
        echo "❌ $table MISSING in notification_db"
    fi
done

echo ""
echo "Validation complete!"
