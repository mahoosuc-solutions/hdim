# Sample Data for Clinical Portal

This directory contains sample data for testing and demonstrating the Clinical Portal application.

## Contents

- `hedis-measures.json` - 5 HEDIS quality measures in JSON format
- `sample-patients.json` - 10 sample patients in FHIR R4 Bundle format
- `load-sample-data.py` - Python script to load data via APIs
- `load-sample-data.sh` - Bash script to load data (requires jq)
- `load-hedis-measures.sql` - SQL script to load measures directly into database
- `LOAD_REPORT.md` - Detailed report of the data loading process

## Quick Start

### Load All Sample Data

```bash
cd /home/webemo-aaron/projects/healthdata-in-motion/sample-data

# Load patients via FHIR API
python3 load-sample-data.py

# Load HEDIS measures via database (bypasses JWT auth)
docker exec -i healthdata-postgres psql -U healthdata -d healthdata_cql < load-hedis-measures.sql
```

### Verify Data Loaded

```bash
# Check HEDIS measures
docker exec healthdata-postgres psql -U healthdata -d healthdata_cql \
  -c "SELECT name, version, status FROM cql_libraries WHERE tenant_id = 'default';"

# Check patients
curl "http://localhost:8083/fhir/Patient?_count=20"
```

## Sample Data Overview

### HEDIS Measures (5 measures)
1. **HEDIS-CDC** - Comprehensive Diabetes Care (HbA1c Control)
2. **HEDIS-CBP** - Controlling High Blood Pressure
3. **HEDIS-COL** - Colorectal Cancer Screening
4. **HEDIS-BCS** - Breast Cancer Screening
5. **HEDIS-CIS** - Childhood Immunization Status

### Sample Patients (10 patients)
1. John Doe (66M) - Diabetic
2. Jane Smith (46F) - Hypertensive
3. Robert Johnson (73M) - Screening due
4. Maria Garcia (51F) - Screening due
5. Michael Brown (3M) - Immunizations needed
6. Sarah Davis (69F) - Multiple conditions
7. James Wilson (56M) - Pre-diabetic
8. Linda Martinez (61F) - Hypertensive
9. David Anderson (39M) - Healthy
10. Emily Taylor (43F) - Overdue screenings

## API Endpoints

### FHIR Server
- **Base URL:** http://localhost:8083/fhir
- **Get all patients:** GET /fhir/Patient?_count=100
- **Get patient by ID:** GET /fhir/Patient/{id}
- **Create patient:** POST /fhir/Patient
- **Authentication:** None (HAPI FHIR mock server)

### CQL Engine (Requires JWT)
- **Base URL:** http://localhost:8081/cql-engine/api/v1
- **List libraries:** GET /cql/libraries/active
- **Create library:** POST /cql/libraries
- **Evaluate measure:** POST /cql/evaluate
- **Authentication:** Bearer JWT token required
- **Headers Required:** X-Tenant-ID: default

## Authentication Note

The CQL Engine requires JWT authentication. For development, data can be loaded directly into the database using the SQL script. For production, configure an authentication service to generate JWT tokens.

## Next Steps

1. Add clinical data to patients (conditions, observations, procedures)
2. Configure authentication service for JWT tokens
3. Test quality measure evaluation
4. Expand sample data set

## Troubleshooting

### jq not installed (for bash script)
Use the Python script instead: `python3 load-sample-data.py`

### JWT authentication errors
Load measures via SQL: `docker exec -i healthdata-postgres psql -U healthdata -d healthdata_cql < load-hedis-measures.sql`

### FHIR server not responding
Check if container is running: `docker ps | grep fhir`

## See Also

- `LOAD_REPORT.md` - Detailed load report with verification
- `/backend/modules/services/cql-engine-service/` - CQL Engine service code
- `/backend/modules/services/fhir-service/` - FHIR service code
