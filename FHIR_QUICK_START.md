# FHIR Integration Quick Start Guide

## Immediate Next Steps

### 1. Populate FHIR Server with Demo Data (15-20 minutes)

```bash
# Run the population script
cd /home/webemo-aaron/projects/healthdata-in-motion
./load-fhir-demo-data.sh

# This will create:
# - 15 patients with Diabetes (SNOMED: 44054006)
# - 12 patients with Hypertension (SNOMED: 59621000)
# - 13 HbA1c observations (LOINC: 4548-4)
# - 12 Blood pressure observations (LOINC: 8480-6, 8462-4)
# - 10 Depression screenings PHQ-9 (LOINC: 44249-1)
# - 20 Medication requests (Metformin, Lisinopril)
# - 40 Office visit encounters
```

### 2. Validate FHIR Data

```bash
# Run validation script
./validate-fhir-data.sh

# Expected output: All checks should pass ✓
# - Patients: 73+ (PASS)
# - Conditions: 27+ (PASS)
# - Observations: 254+ (PASS)
# - MedicationRequests: 30+ (PASS)
# - Encounters: 40+ (PASS)
```

### 3. Test CQL Engine Integration

```bash
# Get authentication token
TOKEN=$(curl -s -X POST http://localhost:9000/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"demo.doctor","password":"demo123"}' | jq -r '.accessToken')

# Test CQL evaluation with FHIR data
curl -X POST http://localhost:8081/cql/evaluate \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: demo-clinic" \
  -d '{
    "patientId": "1",
    "libraryName": "CMS122",
    "version": "1.0.0"
  }'
```

### 4. Verify Quality Measure Results

```bash
# Check quality measure calculations
curl "http://localhost:8087/quality-measure/results?page=0&size=20" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: demo-clinic"

# Should return results with compliant/non-compliant patients
```

### 5. Check Care Gap Generation

```bash
# View care gaps
curl "http://localhost:9000/api/care-gaps" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: demo-clinic"

# Should show high/medium/low priority gaps
```

---

## FHIR Server Quick Reference

### Access Information
- **External URL:** http://localhost:8083/fhir
- **Container URL:** http://fhir-service-mock:8080/fhir
- **FHIR Version:** R4 (4.0.1)
- **Database:** PostgreSQL `healthdata_fhir`
- **Tenant ID:** demo-clinic

### Useful FHIR Queries

```bash
# Get all patients
curl 'http://localhost:8083/fhir/Patient?_count=10'

# Get patient by ID
curl 'http://localhost:8083/fhir/Patient/1'

# Get conditions for patient
curl 'http://localhost:8083/fhir/Condition?patient=1'

# Get diabetes conditions
curl 'http://localhost:8083/fhir/Condition?code=44054006'

# Get HbA1c observations for patient
curl 'http://localhost:8083/fhir/Observation?patient=1&code=4548-4'

# Get blood pressure observations
curl 'http://localhost:8083/fhir/Observation?patient=2&code=8480-6'

# Get active medications for patient
curl 'http://localhost:8083/fhir/MedicationRequest?patient=1&status=active'

# Count total patients
curl 'http://localhost:8083/fhir/Patient?_summary=count'

# Search patients by name
curl 'http://localhost:8083/fhir/Patient?name=Doe'
```

### Key SNOMED-CT Codes
- **44054006** - Diabetes mellitus type 2
- **59621000** - Essential hypertension
- **370143000** - Major depressive disorder

### Key LOINC Codes
- **4548-4** - Hemoglobin A1c/Hemoglobin.total in Blood
- **8480-6** - Systolic blood pressure
- **8462-4** - Diastolic blood pressure
- **44249-1** - PHQ-9 quick depression assessment panel
- **48643-1** - Glomerular filtration rate (eGFR)

### Key RxNorm Codes
- **860975** - metformin 500 MG Oral Tablet
- **314076** - lisinopril 10 MG Oral Tablet
- **312940** - sertraline 50 MG Oral Tablet

---

## Troubleshooting

### FHIR Server Not Responding
```bash
# Check container status
docker compose ps fhir-service-mock

# Restart if needed
docker compose restart fhir-service-mock

# Check logs
docker compose logs fhir-service-mock --tail=50

# Test metadata endpoint
curl http://localhost:8083/fhir/metadata | jq '.fhirVersion'
```

### Health Check Shows Unhealthy (but server works)
```bash
# This is a known cosmetic issue - the server is functional
# The healthcheck endpoint may be timing out but API works fine

# Verify functionality
curl http://localhost:8083/fhir/metadata

# If returns FHIR CapabilityStatement, server is working
```

### No Data Returned from Queries
```bash
# Check if data was loaded
curl 'http://localhost:8083/fhir/Patient?_summary=count'

# If count is low, run population script again
./load-fhir-demo-data.sh
```

### CQL Engine Can't Access FHIR Data
```bash
# Check FHIR_SERVER_URL environment variable
docker compose exec cql-engine-service env | grep FHIR

# Should show: FHIR_SERVER_URL=http://fhir-service-mock:8080/fhir

# Test connectivity from CQL container
docker compose exec cql-engine-service curl http://fhir-service-mock:8080/fhir/metadata
```

---

## Demo Talking Points

### FHIR Integration Value Proposition

1. **Standards-Based Architecture**
   - "HDIM uses FHIR R4, the industry standard for healthcare data exchange"
   - "Every patient, observation, condition is a properly structured FHIR resource"
   - "No proprietary formats - this is how modern healthcare systems communicate"

2. **Real-Time Clinical Data Access**
   - "CQL engine retrieves data directly from FHIR repository"
   - "No batch ETL processes - quality measures calculated on live data"
   - "Same FHIR API your EHR exposes - seamless integration"

3. **Interoperability Ready**
   - "FHIR resources validated against official profiles"
   - "LOINC codes for labs, SNOMED codes for diagnoses, RxNorm for medications"
   - "Ready to integrate with any FHIR-compliant system"

4. **Scalable and Extensible**
   - "HAPI FHIR server handles millions of resources"
   - "Add new resource types without database schema changes"
   - "Support for FHIR Subscriptions for real-time notifications"

### Demo Flow with FHIR

1. **Show Patient in FHIR Server**
   - Open patient FHIR resource in browser
   - Highlight proper coding (SNOMED, LOINC)
   - Show resource relationships (patient → conditions → observations)

2. **Trigger Quality Measure Calculation**
   - Call CQL engine with patient ID
   - Show CQL retrieving FHIR resources
   - Display calculated compliance result

3. **Show Care Gap Generation**
   - Display care gaps dashboard
   - Drill into specific patient
   - Show FHIR observations that created the gap

4. **Clinical Portal Integration**
   - Navigate to patient detail page
   - Show demographics, conditions, observations from FHIR
   - Highlight real-time data synchronization

---

## Next Enhancements

### Short-term (Optional, if time permits)
1. Add immunization records for pediatric measures (CIS/FVA)
2. Add procedure records for preventive screenings
3. Add more observation types (BMI, smoking status, etc.)
4. Validate multi-tenant data segregation

### Medium-term (Post-demo)
1. Use Synthea to generate 100+ realistic patients
2. Implement FHIR Subscriptions for real-time notifications
3. Add US Core profile validation
4. Performance test with 10,000+ patients

### Long-term (Future roadmap)
1. Integrate with real EHR FHIR endpoint
2. Implement SMART-on-FHIR authentication
3. Add CDS Hooks for clinical decision support
4. FHIR Bulk Data Export ($export) implementation

---

## Documentation References

- **FHIR Integration Plan:** [FHIR_INTEGRATION_PLAN.md](./FHIR_INTEGRATION_PLAN.md)
- **FHIR R4 Spec:** http://hl7.org/fhir/R4/
- **HAPI FHIR Docs:** https://hapifhir.io/
- **Quality Measure Requirements:** [Demo Day Quick Start](./DEMO_DAY_QUICK_START.md)

---

## Status Checklist

Before demo:
- [ ] Run `./load-fhir-demo-data.sh` ✅
- [ ] Run `./validate-fhir-data.sh` ✅
- [ ] Test CQL engine FHIR access
- [ ] Verify quality measure calculations
- [ ] Check care gap generation
- [ ] Test Clinical Portal patient detail
- [ ] Practice FHIR demo talking points
- [ ] Memorize key patient IDs and codes

**Last Updated:** November 24, 2025
