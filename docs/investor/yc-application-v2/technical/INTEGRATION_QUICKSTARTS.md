# HDIM Integration Quickstart Guides

> Step-by-step guides for connecting your EHR and data sources to HDIM.

---

## Table of Contents

1. [Epic FHIR Integration](#epic-fhir-integration)
2. [Cerner FHIR Integration](#cerner-fhir-integration)
3. [athenahealth Integration](#athenahealth-integration)
4. [Generic FHIR R4 Integration](#generic-fhir-r4-integration)
5. [CSV Upload Integration](#csv-upload-integration)
6. [n8n Workflow Integration](#n8n-workflow-integration)
7. [Troubleshooting Guide](#troubleshooting-guide)

---

## Epic FHIR Integration

### Overview

HDIM connects to Epic via FHIR R4 APIs, supporting both real-time queries and Bulk FHIR export for larger data volumes.

**Time to Complete:** 2-4 hours
**Prerequisites:** Epic administrator access, MyChart/App Orchard registration
**Integration Types:** SMART on FHIR, Bulk FHIR, Backend Services

---

### Step 1: Register HDIM in App Orchard

1. Log into the Epic App Orchard (https://appmarket.epic.com)
2. Navigate to **Connections** → **Add Connection**
3. Search for "Health Data In Motion" or "HDIM"
4. Click **Request Connection**
5. Complete the connection request form:
   - Organization name
   - Epic environment (Production/Non-Prod)
   - Use case: Quality Measurement
6. Submit and await approval (typically 1-2 business days)

---

### Step 2: Configure Epic FDI (FHIR Data Integration)

Once approved, configure your Epic environment:

1. Access Epic Hyperspace as an administrator
2. Navigate to **Epic Button** → **Admin** → **FDI**
3. Create a new FHIR connection:
   - **App Name:** HDIM Quality Measurement
   - **Client ID:** (Provided by HDIM)
   - **Redirect URI:** https://app.healthdatainmotion.com/oauth/callback
   - **Scopes:**
     ```
     patient/*.read
     Observation.read
     Condition.read
     MedicationRequest.read
     Procedure.read
     Immunization.read
     ```

4. Enable Bulk FHIR export:
   - **Export Types:** Patient, Observation, Condition, MedicationRequest
   - **Schedule:** Nightly (recommended) or on-demand

---

### Step 3: Configure HDIM

1. Log into HDIM Dashboard
2. Navigate to **Settings** → **Integrations** → **Add Integration**
3. Select **Epic FHIR**
4. Enter your Epic configuration:
   - **Epic Base URL:** https://your-epic-instance.epic.com/FHIR/R4
   - **Client ID:** (From Epic FDI setup)
   - **Client Secret:** (From Epic FDI setup)
   - **Organization OID:** Your Epic organization identifier

5. Click **Test Connection**
6. Upon success, click **Save and Activate**

---

### Step 4: Initial Data Sync

1. Navigate to **Data** → **Sync**
2. Click **Run Initial Sync**
3. Select sync scope:
   - **Full Population:** All patients (recommended for first sync)
   - **Date Range:** Patients with activity in date range
4. Monitor sync progress in the dashboard

**Expected Duration:**
| Patient Count | Estimated Time |
|---------------|----------------|
| <5,000 | 15-30 minutes |
| 5,000-25,000 | 1-2 hours |
| 25,000-100,000 | 4-8 hours |
| >100,000 | Schedule overnight |

---

### Step 5: Enable SMART on FHIR (Optional)

For embedded quality views within Epic:

1. In Epic Hyperspace, navigate to **MyChart** → **External Apps**
2. Add HDIM as a SMART on FHIR app:
   - **Launch URL:** https://app.healthdatainmotion.com/smart/launch
   - **Context:** Patient, Encounter
3. Configure toolbar button or Activity placement
4. Test in non-production environment first

---

### Epic-Specific Notes

**Rate Limits:**
Epic enforces rate limits. HDIM automatically handles throttling, but initial syncs may take longer for large populations.

**Bulk FHIR:**
For organizations >50,000 patients, we recommend Bulk FHIR nightly export rather than real-time queries.

**MyChart vs. Backend:**
- Backend Services (OAuth 2.0 client credentials) for automated data sync
- SMART on FHIR for embedded provider views

---

## Cerner FHIR Integration

### Overview

HDIM connects to Cerner (Oracle Health) via their Ignite FHIR R4 APIs.

**Time to Complete:** 2-4 hours
**Prerequisites:** Cerner administrator access, CODE Console access
**Integration Types:** SMART on FHIR, Backend OAuth, Bulk Export

---

### Step 1: Register in Cerner CODE Console

1. Log into Cerner CODE Console (https://code.cerner.com)
2. Navigate to **My Applications** → **New Application**
3. Create application:
   - **Name:** HDIM Quality Measurement
   - **Type:** Backend Services
   - **Authorized URI:** https://app.healthdatainmotion.com/oauth/callback
4. Request access scopes:
   ```
   patient/Patient.read
   patient/Observation.read
   patient/Condition.read
   patient/MedicationRequest.read
   patient/Procedure.read
   patient/Immunization.read
   system/*.read (for bulk operations)
   ```
5. Submit for approval

---

### Step 2: Configure Cerner Millennium

Work with your Cerner administrator to:

1. Approve the CODE Console application
2. Configure domain mapping
3. Enable FHIR R4 endpoint access
4. Set up service account (for backend services)

**Cerner FHIR Base URL Format:**
```
https://fhir-ehr.cerner.com/r4/{tenant_id}
```

---

### Step 3: Configure HDIM

1. Log into HDIM Dashboard
2. Navigate to **Settings** → **Integrations** → **Add Integration**
3. Select **Cerner FHIR**
4. Enter configuration:
   - **Cerner Tenant ID:** Your Cerner tenant identifier
   - **Client ID:** From CODE Console
   - **Client Secret:** From CODE Console
   - **Environment:** Production or Sandbox

5. Click **Test Connection**
6. Upon success, click **Save and Activate**

---

### Step 4: Initial Data Sync

Follow the same process as Epic (Step 4 above).

---

### Cerner-Specific Notes

**Tenant Configuration:**
Each Cerner client has a unique tenant ID. Ensure you're using the correct one.

**Bulk Export:**
Cerner supports Bulk FHIR export. For large populations, configure nightly export jobs.

**PowerChart Integration:**
SMART on FHIR apps can be embedded in PowerChart. Work with Cerner support for configuration.

---

## athenahealth Integration

### Overview

athenahealth provides API access through the athenahealth API Marketplace.

**Time to Complete:** 1-2 hours
**Prerequisites:** athenahealth administrator access
**Integration Types:** athenahealth API, CSV export

---

### Step 1: Enable API Access

1. Log into athenahealth Marketplace
2. Navigate to **Settings** → **API Access**
3. Enable API for your practice
4. Generate API credentials:
   - **Client ID**
   - **Client Secret**
   - **Practice ID**

---

### Step 2: Configure HDIM

1. Log into HDIM Dashboard
2. Navigate to **Settings** → **Integrations** → **Add Integration**
3. Select **athenahealth**
4. Enter configuration:
   - **Practice ID:** Your athenahealth practice ID
   - **Client ID:** From athenahealth
   - **Client Secret:** From athenahealth
   - **Environment:** Production or Preview

5. Click **Test Connection**
6. Upon success, click **Save and Activate**

---

### Step 3: Initial Data Sync

1. Navigate to **Data** → **Sync**
2. Click **Run Initial Sync**
3. athenahealth syncs typically complete within 1-2 hours

---

### athenahealth-Specific Notes

**API Rate Limits:**
athenahealth enforces strict rate limits. HDIM handles throttling automatically.

**Data Scope:**
athenahealth API provides access to:
- Patient demographics
- Appointments
- Clinical documents
- Lab results
- Medications
- Problems (conditions)

**Alternative: CSV Export**
If API access is limited, use athenahealth's built-in reports to export CSV files for upload to HDIM.

---

## Generic FHIR R4 Integration

### Overview

For EHRs not specifically listed, HDIM supports any FHIR R4 compliant server.

**Supported EHRs:** NextGen, eClinicalWorks, Greenway, Allscripts, Practice Fusion, and any FHIR R4 compliant system.

---

### Step 1: Gather FHIR Server Information

Obtain from your EHR vendor:
- FHIR R4 Base URL
- Authentication method (OAuth 2.0, API Key, Basic Auth)
- Client credentials
- Available scopes/resources

---

### Step 2: Configure HDIM

1. Log into HDIM Dashboard
2. Navigate to **Settings** → **Integrations** → **Add Integration**
3. Select **Generic FHIR R4**
4. Configure connection:

**OAuth 2.0 Configuration:**
```yaml
base_url: https://your-ehr.com/fhir/r4
auth_type: oauth2
token_url: https://your-ehr.com/oauth/token
client_id: your_client_id
client_secret: your_client_secret
scopes:
  - patient/*.read
  - launch/patient
```

**API Key Configuration:**
```yaml
base_url: https://your-ehr.com/fhir/r4
auth_type: api_key
api_key_header: X-API-Key
api_key: your_api_key
```

5. Click **Test Connection**
6. If successful, proceed to resource mapping

---

### Step 3: Resource Mapping

HDIM automatically maps standard FHIR resources:

| HDIM Data | FHIR Resource |
|-----------|---------------|
| Patients | Patient |
| Conditions | Condition |
| Medications | MedicationRequest, MedicationStatement |
| Labs | Observation (category: laboratory) |
| Vitals | Observation (category: vital-signs) |
| Procedures | Procedure |
| Immunizations | Immunization |
| Encounters | Encounter |

For non-standard mappings, contact support@healthdatainmotion.com.

---

### Step 4: Test and Validate

1. Run **Test Sync** with a small patient sample (10-50 patients)
2. Verify data appears correctly in HDIM
3. Check quality measure calculations
4. Resolve any mapping issues
5. Run full sync

---

## CSV Upload Integration

### Overview

The simplest integration method for practices without FHIR APIs or those wanting a quick start.

**Time to Complete:** 30 minutes
**Prerequisites:** Ability to export patient data from your EHR

---

### Step 1: Export Data from Your EHR

Export the following data from your EHR:

**Required: Patient Demographics**
- Patient ID/MRN
- First Name
- Last Name
- Date of Birth
- Gender
- Address (optional but recommended)
- Phone (optional)
- Primary Provider (optional)

**Required: Clinical Data (at least one)**
- Conditions/Diagnoses (ICD-10 codes)
- Medications (RxNorm or NDC codes)
- Lab Results (LOINC codes)
- Procedures (CPT or HCPCS codes)

---

### Step 2: Format Data

HDIM accepts CSV files with the following structure:

**patients.csv**
```csv
mrn,first_name,last_name,dob,gender,address,city,state,zip,phone,provider_npi
12345,John,Smith,1965-03-15,M,123 Main St,Portland,OR,97201,503-555-1234,1234567890
12346,Jane,Doe,1978-08-22,F,456 Oak Ave,Seattle,WA,98101,206-555-5678,1234567890
```

**conditions.csv**
```csv
mrn,icd10_code,description,onset_date,status
12345,E11.9,Type 2 Diabetes,2020-01-15,active
12345,I10,Essential Hypertension,2019-06-01,active
12346,J45.909,Asthma,2015-03-10,active
```

**medications.csv**
```csv
mrn,rxnorm_code,medication_name,start_date,status
12345,860975,Metformin 500mg,2020-01-20,active
12345,314076,Lisinopril 10mg,2019-06-15,active
```

**labs.csv**
```csv
mrn,loinc_code,test_name,value,unit,date
12345,4548-4,Hemoglobin A1c,7.2,%,2024-12-15
12345,2093-3,Cholesterol,185,mg/dL,2024-12-15
```

---

### Step 3: Upload to HDIM

1. Log into HDIM Dashboard
2. Navigate to **Data** → **Import**
3. Select **CSV Upload**
4. Upload files in order:
   1. patients.csv (required first)
   2. conditions.csv
   3. medications.csv
   4. labs.csv
5. HDIM automatically maps columns; verify mappings
6. Click **Import**

---

### Step 4: Verify Data

1. Navigate to **Patients** and verify patient count
2. Check **Quality Dashboard** for measure calculations
3. Drill into sample patients to verify data accuracy

---

### Step 5: Schedule Regular Uploads

For ongoing data sync:

1. Navigate to **Settings** → **Integrations** → **CSV Schedule**
2. Configure:
   - **Frequency:** Daily, Weekly, or Custom
   - **Upload Method:** Manual, SFTP, or AWS S3
3. For SFTP/S3, obtain credentials from HDIM

---

### EHR-Specific Export Guides

**DrChrono:**
1. Reports → Data Export → Custom Export
2. Select: Patients, Diagnoses, Medications, Lab Results
3. Export as CSV

**eClinicalWorks:**
1. Reports → Custom Reports → Patient Data
2. Include: Demographics, Problems, Meds, Labs
3. Export to CSV

**Practice Fusion:**
1. Admin → Reports → Data Export
2. Select all clinical data types
3. Download as CSV

---

## n8n Workflow Integration

### Overview

For EHRs without FHIR APIs, use n8n workflow automation to transform and sync data.

**Time to Complete:** 2-4 hours
**Prerequisites:** n8n instance (cloud or self-hosted), source data access

---

### Step 1: Set Up n8n

If you don't have n8n:

**Cloud (Recommended for quick start):**
1. Sign up at https://n8n.io
2. Create a workspace

**Self-Hosted:**
```bash
docker run -d \
  --name n8n \
  -p 5678:5678 \
  -v ~/.n8n:/home/node/.n8n \
  n8nio/n8n
```

---

### Step 2: Import HDIM Workflow Template

1. Download template from: `https://app.healthdatainmotion.com/integrations/n8n-templates`
2. In n8n, navigate to **Workflows** → **Import**
3. Select the downloaded template

---

### Step 3: Configure Source Connection

**For Database Sources (SQL Server, MySQL, PostgreSQL):**

1. In the n8n workflow, locate the **Database Query** node
2. Configure connection:
   ```
   Host: your-database-server.com
   Port: 1433 (SQL Server) / 3306 (MySQL) / 5432 (PostgreSQL)
   Database: your_database
   User: readonly_user
   Password: ********
   ```
3. Customize the query for your schema

**Example SQL Query:**
```sql
SELECT
  p.patient_id AS mrn,
  p.first_name,
  p.last_name,
  p.date_of_birth AS dob,
  p.gender,
  d.icd10_code,
  d.diagnosis_name,
  d.onset_date
FROM patients p
LEFT JOIN diagnoses d ON p.patient_id = d.patient_id
WHERE p.last_updated > $1
```

**For File Sources (SFTP, FTP):**

1. Locate the **SFTP** node
2. Configure connection to your file server
3. Set file path and schedule

---

### Step 4: Configure HDIM Destination

1. Locate the **HDIM API** node
2. Enter your HDIM API credentials:
   - **API Key:** Your HDIM API key
   - **Organization ID:** Your HDIM org ID
3. Select data type: `fhir_bundle` or `csv`

---

### Step 5: Map Data to FHIR

Use the **Transform** node to convert your data to FHIR:

```javascript
// Transform patient record to FHIR Patient resource
{
  "resourceType": "Patient",
  "id": $json.mrn,
  "name": [{
    "given": [$json.first_name],
    "family": $json.last_name
  }],
  "birthDate": $json.dob,
  "gender": $json.gender.toLowerCase()
}
```

---

### Step 6: Test and Activate

1. Click **Execute Workflow** to test
2. Verify data appears in HDIM
3. Enable **Active** toggle for scheduled runs
4. Set schedule (e.g., daily at 2 AM)

---

### Pre-Built n8n Templates

| Template | Use Case |
|----------|----------|
| `hdim-sql-to-fhir` | SQL database to HDIM |
| `hdim-csv-sftp` | SFTP CSV files to HDIM |
| `hdim-hl7-transform` | HL7v2 messages to FHIR |
| `hdim-lab-feed` | Lab result processing |

Download from: https://app.healthdatainmotion.com/integrations/n8n-templates

---

## Troubleshooting Guide

### Common Issues

#### Connection Failed

**Symptoms:** "Unable to connect to FHIR server"

**Solutions:**
1. Verify FHIR base URL is correct (ends in `/fhir/r4` typically)
2. Check credentials are valid and not expired
3. Ensure your IP is whitelisted in EHR firewall
4. Verify OAuth scopes are approved

#### Authentication Errors

**Symptoms:** 401 Unauthorized or 403 Forbidden

**Solutions:**
1. Regenerate client credentials
2. Verify token endpoint URL
3. Check scope permissions
4. Ensure service account is active

#### Slow Sync Performance

**Symptoms:** Sync takes longer than expected

**Solutions:**
1. For >50k patients, use Bulk FHIR export
2. Run initial sync during off-peak hours
3. Enable pagination (HDIM handles automatically)
4. Contact support for optimization

#### Missing Data

**Symptoms:** Some patients or clinical data not appearing

**Solutions:**
1. Verify resource permissions in EHR
2. Check data quality in source system
3. Review HDIM data mapping logs
4. Validate code systems (ICD-10, LOINC, RxNorm)

#### Quality Measures Not Calculating

**Symptoms:** Dashboard shows 0% or missing measures

**Solutions:**
1. Verify denominator-eligible patients exist
2. Check clinical data is mapped correctly
3. Ensure dates fall within measurement period
4. Review exclusion criteria

---

### Getting Help

**Documentation:** https://docs.healthdatainmotion.com

**Integration Support:**
- Email: integrations@healthdatainmotion.com
- Response time: <4 hours (business hours)

**Technical Support:**
- Email: support@healthdatainmotion.com
- Enterprise: Dedicated Slack channel

**Office Hours:**
Weekly integration office hours for live troubleshooting
- Wednesdays, 2 PM ET
- Register at: https://app.healthdatainmotion.com/office-hours

---

*Last Updated: December 2025*
