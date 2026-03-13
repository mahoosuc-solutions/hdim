#!/bin/bash
# seed-star-ratings-demo.sh — Populate star-ratings demo data for pilot demos
#
# Inserts realistic care-gap projections and star-rating snapshots into the
# care-gap-event-service database so that the /current, /trend, and /simulate
# endpoints return meaningful data without requiring event replay.
#
# Usage:
#   ./scripts/seed-star-ratings-demo.sh                    # defaults
#   TENANT_ID=acme-health ./scripts/seed-star-ratings-demo.sh
#   DB_HOST=remote-db DB_PORT=5435 ./scripts/seed-star-ratings-demo.sh
#
# Prerequisites: psql (PostgreSQL client), care-gap-event-service database running

set -euo pipefail

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5435}"
DB_NAME="${DB_NAME:-care_gap_event_db}"
DB_USER="${DB_USER:-healthdata}"
DB_PASS="${DB_PASS:-healthdata}"
TENANT_ID="${TENANT_ID:-demo-tenant}"

export PGPASSWORD="$DB_PASS"
PSQL="psql -h $DB_HOST -p $DB_PORT -U $DB_USER -d $DB_NAME -q"

echo "=== Star-Ratings Demo Seed ==="
echo "Target: $DB_HOST:$DB_PORT/$DB_NAME"
echo "Tenant: $TENANT_ID"
echo ""

# Measure codes paired with realistic numerator/denominator patterns for a
# mid-performing Medicare Advantage plan (~3.5 stars baseline).
# Format: code:numerator:denominator:status
# We create multiple patient-gap rows per measure to simulate population data.
MEASURES=(
  "BCS:72:100"
  "COL:65:100"
  "CBP:58:100"
  "CDC-H9:30:100"
  "CDC-H8:62:100"
  "CDC-E:70:100"
  "SPC:75:100"
  "MED-D:68:100"
  "MED-H:71:100"
  "MED-C:66:100"
  "HRM:80:100"
  "MRP:55:100"
  "KED:60:100"
  "ABA:85:100"
)

echo "Step 1: Clearing existing demo data for tenant $TENANT_ID..."
$PSQL <<SQL
DELETE FROM star_rating_snapshots WHERE tenant_id = '$TENANT_ID';
DELETE FROM star_rating_projections WHERE tenant_id = '$TENANT_ID';
DELETE FROM care_gap_projections WHERE tenant_id = '$TENANT_ID';
SQL

echo "Step 2: Inserting care-gap projections (${#MEASURES[@]} measures)..."

for measure_spec in "${MEASURES[@]}"; do
  IFS=':' read -r code numerator denominator <<< "$measure_spec"
  closed=$numerator
  open=$((denominator - numerator))

  # Insert closed gaps
  for i in $(seq 1 "$closed"); do
    patient_id="demo-patient-${code}-$(printf '%04d' $i)"
    $PSQL -c "INSERT INTO care_gap_projections (patient_id, tenant_id, gap_code, gap_description, severity, status, detection_date, closure_date, qualified, version, last_updated)
      VALUES ('$patient_id', '$TENANT_ID', '$code', 'Demo $code gap', 'HIGH', 'CLOSED', CURRENT_DATE - INTERVAL '90 days', CURRENT_DATE - INTERVAL '$(( RANDOM % 30 ))  days', true, 0, NOW())
      ON CONFLICT DO NOTHING;" 2>/dev/null || true
  done

  # Insert open gaps
  for i in $(seq 1 "$open"); do
    patient_id="demo-patient-${code}-open-$(printf '%04d' $i)"
    $PSQL -c "INSERT INTO care_gap_projections (patient_id, tenant_id, gap_code, gap_description, severity, status, detection_date, qualified, version, last_updated)
      VALUES ('$patient_id', '$TENANT_ID', '$code', 'Demo $code gap (open)', 'HIGH', 'OPEN', CURRENT_DATE - INTERVAL '$(( 30 + RANDOM % 60 )) days', true, 0, NOW())
      ON CONFLICT DO NOTHING;" 2>/dev/null || true
  done

  echo "  $code: $closed closed / $open open"
done

echo ""
echo "Step 3: Inserting 12-week trend snapshots..."

# Generate weekly snapshots showing gradual improvement (realistic for a plan
# working on care gap closures during a measurement year).
for week_offset in $(seq 12 -1 0); do
  snapshot_date=$(date -d "today - ${week_offset} weeks" +%Y-%m-%d 2>/dev/null || date -v-${week_offset}w +%Y-%m-%d)

  # Simulate gradual improvement: start at 2.8 stars, trend toward 3.5
  progress=$(echo "scale=2; (12 - $week_offset) / 12" | bc)
  overall=$(echo "scale=2; 2.80 + ($progress * 0.70)" | bc)
  rounded=$(echo "scale=1; r=$overall * 2; scale=0; r=r/1; scale=1; r/2" | bc)
  open_gaps=$((350 - (12 - week_offset) * 15))
  closed_gaps=$((1050 + (12 - week_offset) * 15))
  measure_count=14
  quality_bonus=$([ "$(echo "$rounded >= 4.0" | bc)" -eq 1 ] && echo "true" || echo "false")

  $PSQL -c "INSERT INTO star_rating_snapshots (tenant_id, snapshot_date, snapshot_granularity, overall_rating, rounded_rating, measure_count, open_gap_count, closed_gap_count, quality_bonus_eligible, captured_at)
    VALUES ('$TENANT_ID', '$snapshot_date', 'WEEKLY', $overall, $rounded, $measure_count, $open_gaps, $closed_gaps, $quality_bonus, NOW())
    ON CONFLICT ON CONSTRAINT uk_star_rating_snapshot_tenant_date_granularity DO NOTHING;" 2>/dev/null || true
done

# Monthly snapshots (last 3 months)
for month_offset in 3 2 1 0; do
  snapshot_date=$(date -d "today - ${month_offset} months" +%Y-%m-%d 2>/dev/null || date -v-${month_offset}m +%Y-%m-%d)

  progress=$(echo "scale=2; (3 - $month_offset) / 3" | bc)
  overall=$(echo "scale=2; 2.80 + ($progress * 0.70)" | bc)
  rounded=$(echo "scale=1; r=$overall * 2; scale=0; r=r/1; scale=1; r/2" | bc)
  open_gaps=$((350 - (3 - month_offset) * 60))
  closed_gaps=$((1050 + (3 - month_offset) * 60))
  quality_bonus=$([ "$(echo "$rounded >= 4.0" | bc)" -eq 1 ] && echo "true" || echo "false")

  $PSQL -c "INSERT INTO star_rating_snapshots (tenant_id, snapshot_date, snapshot_granularity, overall_rating, rounded_rating, measure_count, open_gap_count, closed_gap_count, quality_bonus_eligible, captured_at)
    VALUES ('$TENANT_ID', '$snapshot_date', 'MONTHLY', $overall, $rounded, 14, $open_gaps, $closed_gaps, $quality_bonus, NOW())
    ON CONFLICT ON CONSTRAINT uk_star_rating_snapshot_tenant_date_granularity DO NOTHING;" 2>/dev/null || true
done

echo ""
echo "Step 4: Verifying seed data..."

GAP_COUNT=$($PSQL -t -c "SELECT COUNT(*) FROM care_gap_projections WHERE tenant_id = '$TENANT_ID';")
SNAPSHOT_COUNT=$($PSQL -t -c "SELECT COUNT(*) FROM star_rating_snapshots WHERE tenant_id = '$TENANT_ID';")

echo "  Care gap projections: $(echo $GAP_COUNT | tr -d ' ')"
echo "  Star rating snapshots: $(echo $SNAPSHOT_COUNT | tr -d ' ')"

echo ""
echo "=== Done ==="
echo ""
echo "Verify with:"
echo "  curl http://localhost:8111/care-gap-event/api/v1/star-ratings/current -H 'X-Tenant-ID: $TENANT_ID'"
echo "  curl 'http://localhost:8111/care-gap-event/api/v1/star-ratings/trend?weeks=12&granularity=WEEKLY' -H 'X-Tenant-ID: $TENANT_ID'"
