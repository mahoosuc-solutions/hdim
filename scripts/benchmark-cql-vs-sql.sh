#!/bin/bash

# Performance Benchmark: CQL/FHIR vs Traditional SQL
# This script measures and compares performance between:
# 1. CQL/FHIR-based evaluation (current system)
# 2. Traditional SQL-based evaluation (baseline comparison)

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration
BASE_URL="${BASE_URL:-http://localhost:18080}"
TENANT_ID="${TENANT_ID:-acme-health}"
RESULTS_DIR="${RESULTS_DIR:-./benchmark-results}"
ITERATIONS="${ITERATIONS:-100}"
CONCURRENT_USERS="${CONCURRENT_USERS:-10}"
WARMUP_ITERATIONS="${WARMUP_ITERATIONS:-10}"

# Database connection (for SQL benchmarks)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5435}"
DB_NAME="${DB_NAME:-healthdata_db}"
DB_USER="${DB_USER:-healthdata}"
DB_PASSWORD="${DB_PASSWORD:-demo_password_2024}"

# Create results directory
mkdir -p "$RESULTS_DIR"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
RESULTS_FILE="$RESULTS_DIR/benchmark_${TIMESTAMP}.json"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}CQL/FHIR vs SQL Performance Benchmark${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to check prerequisites
check_prerequisites() {
    echo -e "${CYAN}Checking prerequisites...${NC}"
    
    # Check if services are running
    if ! curl -s --max-time 2 "$BASE_URL/actuator/health" > /dev/null 2>&1; then
        echo -e "${RED}✗${NC} Gateway service not accessible at $BASE_URL"
        exit 1
    fi
    
    # Check if PostgreSQL container is accessible
    local postgres_container=$(docker ps --format '{{.Names}}' | grep -E "(postgres|hdim-demo-postgres)" | head -1)
    
    if [ -z "$postgres_container" ]; then
        echo -e "${RED}✗${NC} PostgreSQL container not found"
        exit 1
    fi
    
    if ! docker exec "$postgres_container" pg_isready -U "$DB_USER" > /dev/null 2>&1; then
        echo -e "${RED}✗${NC} PostgreSQL not accessible"
        exit 1
    fi
    
    # Check required tools
    command -v jq > /dev/null || { echo -e "${RED}✗${NC} jq not installed"; exit 1; }
    command -v docker > /dev/null || { echo -e "${RED}✗${NC} docker not installed"; exit 1; }
    # Note: psql will be used via docker exec, not required on host
    
    echo -e "${GREEN}✓${NC} All prerequisites met"
    echo ""
}

# Function to get test patient IDs
get_test_patients() {
    echo -e "${CYAN}Fetching test patients...${NC}"
    
    # Try API first
    PATIENT_IDS=$(curl -s "$BASE_URL/api/v1/patients?page=0&size=100" \
        -H "X-Tenant-ID: $TENANT_ID" \
        2>/dev/null | jq -r '.content[].id' 2>/dev/null | head -20)
    
    # If API fails, try direct database query
    if [ -z "$PATIENT_IDS" ]; then
        local postgres_container=$(docker ps --format '{{.Names}}' | grep -E "(postgres|hdim-demo-postgres)" | head -1)
        if [ -n "$postgres_container" ]; then
            PATIENT_IDS=$(docker exec "$postgres_container" psql -U "$DB_USER" -d "$DB_NAME" \
                -t -A -c "SELECT id::text FROM patient.patients LIMIT 20;" 2>/dev/null | tr -d ' ')
        fi
    fi
    
    if [ -z "$PATIENT_IDS" ]; then
        echo -e "${YELLOW}⚠${NC} No test patients found via API or database"
        echo -e "${YELLOW}⚠${NC} Using placeholder patient IDs for demonstration"
        # Use placeholder UUIDs for demo
        PATIENT_IDS="00000000-0000-0000-0000-000000000001
00000000-0000-0000-0000-000000000002
00000000-0000-0000-0000-000000000003"
    fi
    
    PATIENT_COUNT=$(echo "$PATIENT_IDS" | wc -l)
    echo -e "${GREEN}✓${NC} Using $PATIENT_COUNT test patients"
    echo ""
}

# Function to get test measure/library IDs
get_test_measures() {
    echo -e "${CYAN}Fetching test measures...${NC}"
    
    # Try API first
    MEASURE_IDS=$(curl -s "$BASE_URL/api/v1/quality-measures?page=0&size=10" \
        -H "X-Tenant-ID: $TENANT_ID" \
        2>/dev/null | jq -r '.content[].id' 2>/dev/null | head -5)
    
    # If API fails, use defaults
    if [ -z "$MEASURE_IDS" ]; then
        echo -e "${YELLOW}⚠${NC} No measures found via API, using default"
        MEASURE_IDS="HEDIS-CDC
HEDIS-CBP
HEDIS-BCS"
    fi
    
    echo -e "${GREEN}✓${NC} Using measures: $(echo "$MEASURE_IDS" | tr '\n' ' ')"
    echo ""
}

# Function to benchmark CQL/FHIR evaluation
benchmark_cql_evaluation() {
    local patient_id=$1
    local measure_id=$2
    local iteration=$3
    
    local start_time=$(date +%s%N)
    
    # Make CQL evaluation request
    local response=$(curl -s -w "\n%{http_code}" -X POST \
        "$BASE_URL/api/v1/cql/evaluations?libraryId=$measure_id&patientId=$patient_id" \
        -H "X-Tenant-ID: $TENANT_ID" \
        -H "Content-Type: application/json" \
        --max-time 30)
    
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')
    local end_time=$(date +%s%N)
    
    local duration_ms=$(( (end_time - start_time) / 1000000 ))
    
    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        echo "$duration_ms"
        return 0
    else
        echo "ERROR:$http_code"
        return 1
    fi
}

# Function to benchmark SQL evaluation
benchmark_sql_evaluation() {
    local patient_id=$1
    local measure_id=$2
    local iteration=$3
    
    local start_time=$(date +%s%N)
    
    # Execute SQL query (example: HEDIS CDC - Diabetes HbA1c Control)
    local sql_query=""
    case "$measure_id" in
        "HEDIS-CDC"|"HEDIS-DC")
            sql_query=$(cat <<'EOF'
WITH patient_data AS (
    SELECT 
        p.id as patient_id,
        p.birth_date,
        EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) as age
    FROM patient.patients p
    WHERE p.id = $1
),
denominator AS (
    SELECT 
        pd.patient_id,
        CASE 
            WHEN pd.age BETWEEN 18 AND 75 
            AND EXISTS (
                SELECT 1 FROM fhir.conditions c
                WHERE c.patient_id = pd.patient_id
                AND c.code IN ('E10', 'E11', 'E10.9', 'E11.9')
                AND c.tenant_id = $2
            )
            THEN 1 ELSE 0
        END as in_denominator
    FROM patient_data pd
),
numerator AS (
    SELECT 
        d.patient_id,
        CASE 
            WHEN d.in_denominator = 1
            AND EXISTS (
                SELECT 1 FROM fhir.observations o
                WHERE o.patient_id = d.patient_id
                AND o.code = '4548-4'  -- HbA1c LOINC code
                AND o.effective_date_time >= CURRENT_DATE - INTERVAL '1 year'
                AND o.value_numeric <= 7.0
                AND o.tenant_id = $2
            )
            THEN 1 ELSE 0
        END as in_numerator
    FROM denominator d
)
SELECT 
    patient_id,
    in_denominator,
    in_numerator,
    CASE WHEN in_denominator = 1 AND in_numerator = 1 THEN true ELSE false END as compliant
FROM numerator;
EOF
            )
            ;;
        "HEDIS-CBP"|"HEDIS-BPC")
            sql_query=$(cat <<'EOF'
WITH patient_data AS (
    SELECT 
        p.id as patient_id,
        p.birth_date,
        EXTRACT(YEAR FROM AGE(CURRENT_DATE, p.birth_date)) as age
    FROM patient.patients p
    WHERE p.id = $1
),
denominator AS (
    SELECT 
        pd.patient_id,
        CASE 
            WHEN pd.age BETWEEN 18 AND 85 
            AND EXISTS (
                SELECT 1 FROM fhir.conditions c
                WHERE c.patient_id = pd.patient_id
                AND c.code LIKE 'I10%'
                AND c.tenant_id = $2
            )
            THEN 1 ELSE 0
        END as in_denominator
    FROM patient_data pd
),
numerator AS (
    SELECT 
        d.patient_id,
        CASE 
            WHEN d.in_denominator = 1
            AND EXISTS (
                SELECT 1 FROM fhir.observations o
                WHERE o.patient_id = d.patient_id
                AND o.code IN ('8480-6', '8462-4')  -- BP LOINC codes
                AND o.effective_date_time >= CURRENT_DATE - INTERVAL '1 year'
                AND (
                    (o.code = '8480-6' AND o.value_numeric < 140) OR
                    (o.code = '8462-4' AND o.value_numeric < 90)
                )
                AND o.tenant_id = $2
            )
            THEN 1 ELSE 0
        END as in_numerator
    FROM denominator d
)
SELECT 
    patient_id,
    in_denominator,
    in_numerator,
    CASE WHEN in_denominator = 1 AND in_numerator = 1 THEN true ELSE false END as compliant
FROM numerator;
EOF
            )
            ;;
        *)
            # Generic SQL query
            sql_query="SELECT COUNT(*) FROM patient.patients WHERE id = \$1;"
            ;;
    esac
    
    # Execute SQL query using docker exec (since psql may not be on host)
    local postgres_container=$(docker ps --format '{{.Names}}' | grep -E "(postgres|hdim-demo-postgres)" | head -1)
    
    if [ -z "$postgres_container" ]; then
        echo "ERROR:POSTGRES_CONTAINER_NOT_FOUND"
        return 1
    fi
    
    # Use docker exec to run psql
    local result=$(docker exec "$postgres_container" psql -U "$DB_USER" -d "$DB_NAME" \
        -t -A -c "$sql_query" \
        -v patient_id="$patient_id" \
        -v tenant_id="$TENANT_ID" 2>&1)
    
    local end_time=$(date +%s%N)
    local duration_ms=$(( (end_time - start_time) / 1000000 ))
    
    if [ $? -eq 0 ]; then
        echo "$duration_ms"
        return 0
    else
        echo "ERROR:SQL_FAILED"
        return 1
    fi
}

# Function to run warmup
run_warmup() {
    echo -e "${CYAN}Running warmup ($WARMUP_ITERATIONS iterations)...${NC}"
    
    local first_patient=$(echo "$PATIENT_IDS" | head -n1)
    local first_measure=$(echo "$MEASURE_IDS" | head -n1)
    
    for i in $(seq 1 $WARMUP_ITERATIONS); do
        benchmark_cql_evaluation "$first_patient" "$first_measure" "$i" > /dev/null 2>&1
        benchmark_sql_evaluation "$first_patient" "$first_measure" "$i" > /dev/null 2>&1
    done
    
    echo -e "${GREEN}✓${NC} Warmup complete"
    echo ""
}

# Function to calculate statistics
calculate_stats() {
    local values=("$@")
    local count=${#values[@]}
    
    if [ $count -eq 0 ]; then
        echo "0,0,0,0,0"
        return
    fi
    
    # Sort values
    IFS=$'\n' sorted=($(sort -n <<<"${values[*]}"))
    unset IFS
    
    # Calculate percentiles
    local p50_idx=$(( count * 50 / 100 ))
    local p95_idx=$(( count * 95 / 100 ))
    local p99_idx=$(( count * 99 / 100 ))
    
    local min=${sorted[0]}
    local max=${sorted[$((count-1))]}
    local p50=${sorted[$p50_idx]}
    local p95=${sorted[$p95_idx]}
    local p99=${sorted[$p99_idx]}
    
    # Calculate average
    local sum=0
    for val in "${sorted[@]}"; do
        sum=$((sum + val))
    done
    local avg=$((sum / count))
    
    echo "$avg,$p50,$p95,$p99,$min,$max"
}

# Main benchmark execution
main() {
    check_prerequisites
    get_test_patients
    get_test_measures
    run_warmup
    
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Running Performance Benchmarks${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo "Configuration:"
    echo "  Iterations: $ITERATIONS"
    echo "  Concurrent Users: $CONCURRENT_USERS"
    echo "  Results Directory: $RESULTS_DIR"
    echo ""
    
    # Initialize results arrays
    declare -a cql_times
    declare -a sql_times
    declare -a cql_errors
    declare -a sql_errors
    
    local cql_success=0
    local sql_success=0
    
    # Run benchmarks
    local patient_array=($PATIENT_IDS)
    local measure_array=($MEASURE_IDS)
    
    for i in $(seq 1 $ITERATIONS); do
        local patient_idx=$(( (i - 1) % ${#patient_array[@]} ))
        local measure_idx=$(( (i - 1) % ${#measure_array[@]} ))
        local patient_id=${patient_array[$patient_idx]}
        local measure_id=${measure_array[$measure_idx]}
        
        # Benchmark CQL
        local cql_result=$(benchmark_cql_evaluation "$patient_id" "$measure_id" "$i" 2>&1)
        if [[ "$cql_result" =~ ^[0-9]+$ ]]; then
            cql_times+=($cql_result)
            ((cql_success++))
        else
            cql_errors+=("$cql_result")
        fi
        
        # Benchmark SQL
        local sql_result=$(benchmark_sql_evaluation "$patient_id" "$measure_id" "$i" 2>&1)
        if [[ "$sql_result" =~ ^[0-9]+$ ]]; then
            sql_times+=($sql_result)
            ((sql_success++))
        else
            sql_errors+=("$sql_result")
        fi
        
        # Progress indicator
        if [ $((i % 10)) -eq 0 ]; then
            echo -ne "\rProgress: $i/$ITERATIONS iterations completed..."
        fi
    done
    
    echo ""
    echo ""
    
    # Calculate statistics
    local cql_stats=$(calculate_stats "${cql_times[@]}")
    local sql_stats=$(calculate_stats "${sql_times[@]}")
    
    # Parse statistics
    IFS=',' read -r cql_avg cql_p50 cql_p95 cql_p99 cql_min cql_max <<< "$cql_stats"
    IFS=',' read -r sql_avg sql_p50 sql_p95 sql_p99 sql_min sql_max <<< "$sql_stats"
    
    # Calculate speedup
    local speedup=$(echo "scale=2; $sql_avg / $cql_avg" | bc)
    local speedup_p95=$(echo "scale=2; $sql_p95 / $cql_p95" | bc)
    
    # Generate report
    cat > "$RESULTS_FILE" <<EOF
{
  "timestamp": "$TIMESTAMP",
  "configuration": {
    "iterations": $ITERATIONS,
    "concurrent_users": $CONCURRENT_USERS,
    "warmup_iterations": $WARMUP_ITERATIONS,
    "base_url": "$BASE_URL",
    "tenant_id": "$TENANT_ID"
  },
  "results": {
    "cql_fhir": {
      "success_count": $cql_success,
      "error_count": ${#cql_errors[@]},
      "statistics": {
        "avg_ms": $cql_avg,
        "p50_ms": $cql_p50,
        "p95_ms": $cql_p95,
        "p99_ms": $cql_p99,
        "min_ms": $cql_min,
        "max_ms": $cql_max
      },
      "errors": $(printf '%s\n' "${cql_errors[@]}" | jq -R . | jq -s .)
    },
    "sql_traditional": {
      "success_count": $sql_success,
      "error_count": ${#sql_errors[@]},
      "statistics": {
        "avg_ms": $sql_avg,
        "p50_ms": $sql_p50,
        "p95_ms": $sql_p95,
        "p99_ms": $sql_p99,
        "min_ms": $sql_min,
        "max_ms": $sql_max
      },
      "errors": $(printf '%s\n' "${sql_errors[@]}" | jq -R . | jq -s .)
    },
    "comparison": {
      "speedup_avg": $speedup,
      "speedup_p95": $speedup_p95,
      "improvement_percent": $(echo "scale=1; ($sql_avg - $cql_avg) * 100 / $sql_avg" | bc)
    }
  }
}
EOF
    
    # Print summary
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}Benchmark Results Summary${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
    echo -e "${CYAN}CQL/FHIR Performance:${NC}"
    echo "  Average:    ${GREEN}${cql_avg}ms${NC}"
    echo "  P50:        ${GREEN}${cql_p50}ms${NC}"
    echo "  P95:        ${GREEN}${cql_p95}ms${NC}"
    echo "  P99:        ${GREEN}${cql_p99}ms${NC}"
    echo "  Success:    ${GREEN}$cql_success/$ITERATIONS${NC}"
    echo ""
    echo -e "${CYAN}SQL Traditional Performance:${NC}"
    echo "  Average:    ${YELLOW}${sql_avg}ms${NC}"
    echo "  P50:        ${YELLOW}${sql_p50}ms${NC}"
    echo "  P95:        ${YELLOW}${sql_p95}ms${NC}"
    echo "  P99:        ${YELLOW}${sql_p99}ms${NC}"
    echo "  Success:    ${YELLOW}$sql_success/$ITERATIONS${NC}"
    echo ""
    echo -e "${CYAN}Performance Comparison:${NC}"
    echo "  Speedup (Avg):  ${GREEN}${speedup}x${NC} faster"
    echo "  Speedup (P95):  ${GREEN}${speedup_p95}x${NC} faster"
    echo "  Improvement:    ${GREEN}$(echo "scale=1; ($sql_avg - $cql_avg) * 100 / $sql_avg" | bc)%${NC}"
    echo ""
    echo -e "${GREEN}✓${NC} Results saved to: $RESULTS_FILE"
    echo ""
}

# Run main function
main "$@"
