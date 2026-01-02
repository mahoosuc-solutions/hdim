#!/bin/bash

# Team B Backend API - Curl Examples
# Copy and paste these commands to test the batch endpoints

BASE_URL="http://localhost:8083"
TENANT_ID="tenant-1"

echo "=========================================="
echo "TEAM B BACKEND API - CURL EXAMPLES"
echo "=========================================="
echo ""

# ===========================================
# CUSTOM MEASURE CRUD
# ===========================================

echo "1. CREATE DRAFT MEASURE"
echo "----------------------------------------"
cat <<'EOF'
curl -X POST "http://localhost:8083/quality-measure/custom-measures" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Custom Measure",
    "description": "Testing custom measure creation",
    "category": "CUSTOM",
    "year": 2024
  }' | jq
EOF
echo ""
echo ""

echo "2. LIST ALL MEASURES"
echo "----------------------------------------"
cat <<'EOF'
curl "http://localhost:8083/quality-measure/custom-measures" \
  -H "X-Tenant-ID: tenant-1" | jq
EOF
echo ""
echo ""

echo "3. LIST DRAFT MEASURES"
echo "----------------------------------------"
cat <<'EOF'
curl "http://localhost:8083/quality-measure/custom-measures?status=DRAFT" \
  -H "X-Tenant-ID: tenant-1" | jq
EOF
echo ""
echo ""

echo "4. GET SINGLE MEASURE"
echo "----------------------------------------"
cat <<'EOF'
# Replace {measure-id} with actual UUID
curl "http://localhost:8083/quality-measure/custom-measures/{measure-id}" \
  -H "X-Tenant-ID: tenant-1" | jq
EOF
echo ""
echo ""

echo "5. UPDATE DRAFT MEASURE"
echo "----------------------------------------"
cat <<'EOF'
# Replace {measure-id} with actual UUID
curl -X PUT "http://localhost:8083/quality-measure/custom-measures/{measure-id}" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Updated Measure Name",
    "description": "Updated description",
    "category": "HEDIS",
    "year": 2025
  }' | jq
EOF
echo ""
echo ""

# ===========================================
# BATCH OPERATIONS
# ===========================================

echo "6. BATCH PUBLISH MEASURES"
echo "----------------------------------------"
cat <<'EOF'
# Replace UUIDs with actual measure IDs
curl -X POST "http://localhost:8083/quality-measure/custom-measures/batch-publish" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{
    "measureIds": [
      "550e8400-e29b-41d4-a716-446655440000",
      "550e8400-e29b-41d4-a716-446655440001"
    ]
  }' | jq

# Expected response:
# {
#   "publishedCount": 2,
#   "skippedCount": 0,
#   "failedCount": 0,
#   "errors": []
# }
EOF
echo ""
echo ""

echo "7. BATCH DELETE MEASURES (without force)"
echo "----------------------------------------"
cat <<'EOF'
# Replace UUIDs with actual measure IDs
curl -X DELETE "http://localhost:8083/quality-measure/custom-measures/batch-delete" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{
    "measureIds": [
      "550e8400-e29b-41d4-a716-446655440000"
    ],
    "force": false
  }' | jq

# Expected response:
# {
#   "deletedCount": 1,
#   "failedCount": 0,
#   "errors": [],
#   "measuresInUse": []
# }
EOF
echo ""
echo ""

echo "8. BATCH DELETE MEASURES (with force)"
echo "----------------------------------------"
cat <<'EOF'
# Replace UUIDs with actual measure IDs
curl -X DELETE "http://localhost:8083/quality-measure/custom-measures/batch-delete" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{
    "measureIds": [
      "550e8400-e29b-41d4-a716-446655440000"
    ],
    "force": true
  }' | jq
EOF
echo ""
echo ""

echo "9. DELETE SINGLE MEASURE"
echo "----------------------------------------"
cat <<'EOF'
# Replace {measure-id} with actual UUID
curl -X DELETE "http://localhost:8083/quality-measure/custom-measures/{measure-id}" \
  -H "X-Tenant-ID: tenant-1" \
  -w "\nHTTP Status: %{http_code}\n"

# Expected: HTTP Status: 204
EOF
echo ""
echo ""

# ===========================================
# PATIENT OPERATIONS (FHIR SERVICE)
# ===========================================

echo "10. CREATE PATIENT"
echo "----------------------------------------"
cat <<'EOF'
curl -X POST "http://localhost:8082/fhir/Patient" \
  -H "X-Tenant-Id: tenant-1" \
  -H "Content-Type: application/fhir+json" \
  -d '{
    "resourceType": "Patient",
    "name": [{
      "family": "Smith",
      "given": ["John"]
    }],
    "gender": "male",
    "birthDate": "1980-01-01"
  }' | jq
EOF
echo ""
echo ""

echo "11. GET PATIENT"
echo "----------------------------------------"
cat <<'EOF'
# Replace {patient-id} with actual UUID
curl "http://localhost:8082/fhir/Patient/{patient-id}" \
  -H "X-Tenant-Id: tenant-1" | jq
EOF
echo ""
echo ""

echo "12. SEARCH PATIENTS"
echo "----------------------------------------"
cat <<'EOF'
curl "http://localhost:8082/fhir/Patient?family=Smith&_count=10" \
  -H "X-Tenant-Id: tenant-1" | jq
EOF
echo ""
echo ""

echo "13. DELETE PATIENT (soft delete)"
echo "----------------------------------------"
cat <<'EOF'
# Replace {patient-id} with actual UUID
curl -X DELETE "http://localhost:8082/fhir/Patient/{patient-id}" \
  -H "X-Tenant-Id: tenant-1" \
  -w "\nHTTP Status: %{http_code}\n"

# Expected: HTTP Status: 204
EOF
echo ""
echo ""

# ===========================================
# TESTING SCENARIOS
# ===========================================

echo "14. COMPLETE WORKFLOW: Create → Publish → Delete"
echo "----------------------------------------"
cat <<'EOF'
# Step 1: Create two measures
M1=$(curl -s -X POST "http://localhost:8083/quality-measure/custom-measures" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{"name":"Measure 1","category":"CUSTOM","year":2024}' | jq -r '.id')

M2=$(curl -s -X POST "http://localhost:8083/quality-measure/custom-measures" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{"name":"Measure 2","category":"CUSTOM","year":2024}' | jq -r '.id')

echo "Created: $M1, $M2"

# Step 2: Batch publish
curl -X POST "http://localhost:8083/quality-measure/custom-measures/batch-publish" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d "{\"measureIds\": [\"$M1\", \"$M2\"]}" | jq

# Step 3: Verify published
curl "http://localhost:8083/quality-measure/custom-measures/$M1" \
  -H "X-Tenant-ID: tenant-1" | jq '.status'

# Step 4: Batch delete
curl -X DELETE "http://localhost:8083/quality-measure/custom-measures/batch-delete" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d "{\"measureIds\": [\"$M1\", \"$M2\"], \"force\": false}" | jq

# Step 5: Verify soft delete
curl "http://localhost:8083/quality-measure/custom-measures/$M1" \
  -H "X-Tenant-ID: tenant-1" | jq '.deletedAt'
EOF
echo ""
echo ""

echo "15. TEST MULTI-TENANT ISOLATION"
echo "----------------------------------------"
cat <<'EOF'
# Create measure in tenant-1
MEASURE=$(curl -s -X POST "http://localhost:8083/quality-measure/custom-measures" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{"name":"Tenant Test","category":"CUSTOM","year":2024}' | jq -r '.id')

echo "Created measure: $MEASURE"

# Try to access from tenant-2 (should fail)
curl "http://localhost:8083/quality-measure/custom-measures/$MEASURE" \
  -H "X-Tenant-ID: tenant-2" \
  -w "\nHTTP Status: %{http_code}\n"

# Expected: HTTP 500 or 404
EOF
echo ""
echo ""

echo "16. TEST BATCH PUBLISH IDEMPOTENCY"
echo "----------------------------------------"
cat <<'EOF'
# Create a measure
M=$(curl -s -X POST "http://localhost:8083/quality-measure/custom-measures" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d '{"name":"Idempotency Test","category":"CUSTOM","year":2024}' | jq -r '.id')

# Publish once
curl -X POST "http://localhost:8083/quality-measure/custom-measures/batch-publish" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d "{\"measureIds\": [\"$M\"]}" | jq '.publishedCount'
# Should return: 1

# Publish again
curl -X POST "http://localhost:8083/quality-measure/custom-measures/batch-publish" \
  -H "X-Tenant-ID: tenant-1" \
  -H "Content-Type: application/json" \
  -d "{\"measureIds\": [\"$M\"]}" | jq '.skippedCount'
# Should return: 1
EOF
echo ""
echo ""

# ===========================================
# DATABASE QUERIES
# ===========================================

echo "17. DATABASE: Check Soft Deleted Measures"
echo "----------------------------------------"
cat <<'EOF'
# Connect to PostgreSQL
docker-compose exec postgres psql -U postgres -d quality_db

# Or from host
psql -h localhost -U postgres -d quality_db

# Run query:
SELECT id, name, status, published_date, deleted_at, deleted_by
FROM custom_measures
WHERE tenant_id = 'tenant-1'
  AND deleted_at IS NOT NULL
ORDER BY deleted_at DESC
LIMIT 10;
EOF
echo ""
echo ""

echo "18. DATABASE: Check Audit Logs"
echo "----------------------------------------"
cat <<'EOF'
# Connect to PostgreSQL audit database
docker-compose exec postgres psql -U postgres -d audit_db

# Run query:
SELECT
  action,
  resource_type,
  resource_id,
  actor,
  occurred_at,
  outcome
FROM audit_events
WHERE resource_type = 'CustomMeasure'
  AND action IN ('UPDATE', 'DELETE')
ORDER BY occurred_at DESC
LIMIT 20;
EOF
echo ""
echo ""

echo "19. DATABASE: Count Measures by Status"
echo "----------------------------------------"
cat <<'EOF'
SELECT
  status,
  COUNT(*) as count,
  COUNT(CASE WHEN deleted_at IS NOT NULL THEN 1 END) as deleted_count
FROM custom_measures
WHERE tenant_id = 'tenant-1'
GROUP BY status;
EOF
echo ""
echo ""

# ===========================================
# TROUBLESHOOTING
# ===========================================

echo "20. TROUBLESHOOTING: Check Service Health"
echo "----------------------------------------"
cat <<'EOF'
# Quality Measure Service
curl http://localhost:8083/actuator/health | jq

# FHIR Service
curl http://localhost:8082/actuator/health | jq

# Check logs
docker-compose logs -f quality-measure-service
docker-compose logs -f fhir-service
EOF
echo ""
echo ""

echo "=========================================="
echo "NOTES"
echo "=========================================="
echo ""
echo "- Replace {measure-id}, {patient-id} with actual UUIDs"
echo "- All batch operations require valid X-Tenant-ID header"
echo "- Soft delete preserves data with deletedAt timestamp"
echo "- All operations are HIPAA audit logged"
echo "- Multi-tenant isolation enforced at repository level"
echo ""
echo "For automated testing, run:"
echo "  ./test-batch-endpoints.sh"
echo ""
echo "For integration tests, run:"
echo "  cd backend && ./gradlew :quality-measure-service:test"
echo ""
