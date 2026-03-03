import type { IntegrationPageData } from '@/lib/types/integration';

export const ORACLE_HEALTH_INTEGRATION: IntegrationPageData = {
  ehrName: 'Oracle Health (Cerner)',
  tagline: 'Enterprise Quality Measurement on the Millennium Platform',
  heroDescription:
    'Millennium stores rich clinical data across 27% of US hospitals, but quality measurement typically requires extracting that data into a separate analytics warehouse — a process that introduces lag, adds infrastructure, and creates a second copy of PHI. HDIM deploys on your infrastructure and fronts the Millennium FHIR server directly, evaluating HEDIS measures against live data without building a parallel pipeline.',

  sections: [
    {
      id: 'overview',
      title: 'Overview',
      type: 'text',
      content:
        'The pattern at most Millennium sites is familiar: clinical data lives in the CDR, a data warehouse team extracts it nightly or weekly, an analytics team runs measure logic against the extract, and the results arrive days after the clinical encounter where they could have made a difference. The data was there when the patient was in the room — it just was not processed in time.\n\nHDIM eliminates this lag by deploying on your infrastructure and fronting the Millennium FHIR server. It evaluates CQL/HEDIS measures against live clinical data — encounters, conditions, observations, medication requests — as it flows through the FHIR R4 endpoint. The result is quality intelligence that is current at the time of care, not stale by the time it reaches the quality team.\n\nMillennium\'s FHIR R4 implementation is strong: OAuth2 SMART on FHIR authentication through the Cerner Code developer program, Bulk FHIR export for population-level analysis, and comprehensive resource coverage. HDIM leverages all of this from within your network boundary. For organizations migrating to Oracle Cloud Infrastructure, HDIM deploys in your OCI VPC with the same architecture — the processing layer stays co-located with the data.',
    },
    {
      id: 'architecture',
      title: 'Architecture',
      type: 'diagram',
      content:
        'HDIM connects to Oracle Health through the Cerner Millennium FHIR Server. Data flows through the HDIM microservice pipeline for quality measure evaluation, with CDS Hooks enabling real-time care gap alerts back into Millennium workflows.',
      codeSnippet: `┌──────────────────────────────────────────────────────────────────┐
│                   Oracle Health (Cerner Millennium)              │
│  ┌────────────────┐  ┌──────────────────┐  ┌─────────────────┐  │
│  │ Millennium EHR │  │ Cerner FHIR      │  │ CDS Hooks       │  │
│  │ (Clinical      │→ │ Server (R4)      │  │ Service         │  │
│  │  Workflows)    │  │ /r4 endpoint     │  │ (Hook triggers) │  │
│  └────────────────┘  └────────┬─────────┘  └────────▲────────┘  │
│                               │ FHIR R4 REST API     │ CDS Cards │
└───────────────────────────────┼──────────────────────┼───────────┘
                                │                      │
                                ▼                      │
┌────────────────────────────────────────────────────────────────────┐
│                            HDIM Platform                           │
│                                                                    │
│  ┌──────────┐   ┌──────────────┐   ┌──────────┐   ┌────────────┐  │
│  │ FHIR     │ → │ Patient      │ → │ CQL      │ → │ Care Gap   │  │
│  │ Service  │   │ Service      │   │ Engine   │   │ Service    │  │
│  │ (8085)   │   │ (8084)       │   │ (8081)   │   │ (8086)     │  │
│  └──────────┘   └──────────────┘   └──────────┘   └─────┬──────┘  │
│       ▲                                                  │         │
│       │                  ┌──────────────────┐            ▼         │
│       └──────────────────│ Quality Measure  │← Quality Reports &  │
│                          │ Service (8087)   │  CDS Hook Payloads  │
│                          └──────────────────┘                      │
└────────────────────────────────────────────────────────────────────┘`,
      codeLanguage: 'text',
    },
    {
      id: 'integration-steps',
      title: 'Integration Guide',
      type: 'steps',
      content:
        'Follow these six steps to connect HDIM with your Oracle Health Millennium deployment.',
      items: [
        'Register in Cerner Code Console — Create a developer account at code.cerner.com and register a new application. Submit for SMART on FHIR authorization and request the required FHIR resource scopes (patient/Patient.read, patient/Observation.read, patient/Condition.read, and others matching your HEDIS measures). For production use, complete the Cerner App Submission process to enable access to live patient data.',
        'Configure OAuth2 client credentials — Obtain your client_id and client_secret from the Cerner Code Console. HDIM uses the SMART on FHIR Backend Services authorization flow (RFC 7523 JWT bearer token) for system-level access. Set CERNER_CLIENT_ID and CERNER_CLIENT_SECRET in your HDIM environment configuration. The token endpoint is https://authorization.cerner.com/tenants/{tenant-id}/protocols/oauth2/profiles/smart-v1/token.',
        'Set up Millennium FHIR R4 endpoint — Configure FHIR_SERVER_URL to point to your organization\'s Millennium FHIR R4 endpoint in the format https://fhir-ehr.cerner.com/r4/{tenant-id}. For on-premises Millennium deployments, the endpoint follows the pattern https://{millennium-host}/fhir/r4. Verify connectivity using the FHIR capability statement: GET {FHIR_SERVER_URL}/metadata.',
        'Configure Bulk FHIR export — Enable Bulk FHIR export for population-level quality measurement. Set FHIR_BULK_EXPORT_ENABLED=true and configure the polling interval. Bulk export allows HDIM to evaluate HEDIS measures across your entire patient panel without individual FHIR queries.',
        'Deploy HDIM on your infrastructure — Deploy HDIM on your RHEL servers or cloud VPC using Docker Compose or Kubernetes. Set spring.profiles.active=production and FHIR_SERVICE_URL to your Millennium FHIR R4 endpoint. HDIM connects over your private network — PHI never leaves your infrastructure.',
        'Verify integration — Run the built-in verification suite to confirm end-to-end connectivity. Check FHIR connectivity via the health endpoint, verify OAuth2 token acquisition, confirm FHIR resource retrieval (Patient, Encounter, Condition, Observation), and validate CQL measure evaluation and care gap detection against your Millennium data.',
      ],
    },
    {
      id: 'configuration',
      title: 'Configuration',
      type: 'code',
      content:
        'HDIM deploys on your infrastructure and connects to your Millennium FHIR endpoint directly. Configure these Spring Boot properties for each deployment.',
      codeSnippet: `# Configuration reference — actual property names may vary by release

# Active profile
spring.profiles.active=production

# Cerner Millennium FHIR R4 endpoint (on your network)
# Cloud-hosted Millennium:
FHIR_SERVICE_URL=https://fhir-ehr.cerner.com/r4/<your-millennium-tenant-id>
# On-premises Millennium:
# FHIR_SERVICE_URL=https://<millennium-host>/fhir/r4

# OAuth2 SMART on FHIR (Backend Services)
FHIR_AUTH_TYPE=oauth2
FHIR_CLIENT_ID=<your-cerner-code-client-id>
FHIR_CLIENT_SECRET=<your-cerner-code-client-secret>
FHIR_TOKEN_URL=https://authorization.cerner.com/tenants/<your-tenant-id>/protocols/oauth2/profiles/smart-v1/token

# Bulk FHIR Export for population-level evaluation
FHIR_BULK_EXPORT_ENABLED=true
FHIR_BULK_EXPORT_POLL_INTERVAL=30

# Connection tuning (adjust for your Millennium capacity)
FHIR_CONNECTION_POOL_SIZE=20
KAFKA_PARTITIONS=12
PATIENT_BATCH_SIZE=1000`,
      codeLanguage: 'bash',
    },
    {
      id: 'cds-hooks',
      title: 'Roadmap: Point-of-Care Clinical Decision Support',
      type: 'text',
      content:
        'Millennium\'s native CDS Hooks support creates a natural extension point for HDIM — and it is on our roadmap. The architecture is straightforward: when a clinician opens a patient chart (patient-view hook), HDIM would evaluate applicable HEDIS measures against that patient\'s current clinical data and return CDS Cards identifying open care gaps directly in the Millennium workflow.\n\nThis capability is not yet implemented in the current HDIM release. Today, HDIM surfaces care gap intelligence through its own dashboard and API layer. The data and the evaluation logic are production-ready — the CDS Hooks delivery mechanism is the planned next step.\n\nWhen available, the CDS Hooks integration will support:\n\n• patient-view: Care gap cards surfaced when a patient chart is opened — overdue screenings, chronic disease management gaps, medication adherence issues.\n\n• order-select: Measure opportunity detection when orders are placed — flagging when a clinical action could close an existing care gap.\n\n• appointment-book: Pre-visit preparation — identifying care gaps before the patient arrives so care coordination staff can prepare.\n\nThe key architectural advantage: because HDIM deploys on your infrastructure alongside Millennium, the CDS Hooks server will communicate over your private network with sub-second latency. No external API calls, no data leaving your trust boundary.',
    },
    {
      id: 'data-exchange',
      title: 'Data Exchange Protocols',
      type: 'table',
      content: 'HDIM supports multiple data exchange protocols with Oracle Health Millennium.',
      tableData: [
        {
          label: 'FHIR R4 REST',
          value:
            'Primary protocol — real-time patient queries and resource retrieval via the Cerner Millennium FHIR Server. Supports all 12 FHIR resource types used in HEDIS measure evaluation.',
        },
        {
          label: 'Bulk FHIR Export',
          value:
            'Population-level asynchronous export using the FHIR Bulk Data Access specification (NDJSON format). Enables batch quality measure evaluation across entire patient panels for HEDIS reporting cycles.',
        },
        {
          label: 'CDS Hooks (Roadmap)',
          value:
            'Planned — not yet available. When implemented, HDIM will act as a CDS Hooks service provider for patient-view, order-select, and appointment-book hooks, delivering care gap alerts at the point of care within Millennium workflows.',
        },
        {
          label: 'HL7 v2',
          value:
            'Legacy message ingestion for ADT (admission/discharge/transfer), ORU (lab results), and ORM (orders) messages from Millennium interfaces. Converted to FHIR R4 resources before entering the HDIM evaluation pipeline.',
        },
      ],
    },
  ],

  fhirResources: [
    { name: 'Patient', description: 'Demographics, identifiers, contact information, and primary care attribution' },
    { name: 'Encounter', description: 'Inpatient visits, outpatient encounters, emergency department interactions' },
    { name: 'Condition', description: 'Diagnoses, chronic conditions, and active problem list entries (ICD-10-CM)' },
    { name: 'Observation', description: 'Lab results, vital signs, HbA1c values, BMI, blood pressure readings' },
    { name: 'Procedure', description: 'Surgical procedures, preventive screenings, and clinical interventions (CPT)' },
    { name: 'MedicationRequest', description: 'Prescription orders, refill requests, and medication therapy changes' },
    { name: 'MedicationStatement', description: 'Patient-reported medications and medication adherence records' },
    { name: 'Immunization', description: 'Vaccination records, immunization history, and administration dates' },
    { name: 'DiagnosticReport', description: 'Laboratory reports, radiology findings, and pathology results' },
    { name: 'AllergyIntolerance', description: 'Drug allergies, food intolerances, and adverse reaction history' },
    { name: 'Coverage', description: 'Insurance and payer information for HEDIS member attribution and reporting' },
    { name: 'CarePlan', description: 'Active care plans, goals, and care team assignments for gap closure tracking' },
  ],

  deploymentModels: [
    {
      name: 'On-Premises',
      description:
        'Deploy HDIM on your existing infrastructure alongside an on-premises Millennium installation. Supports RHEL 7/8 and Ubuntu 20.04+. Data never leaves your network, meeting strict data residency and HIPAA security requirements.',
    },
    {
      name: 'Docker Compose',
      description:
        'Single-command deployment with Docker Compose for rapid pilot programs, development environments, and mid-size Oracle Health implementations. Includes all 51+ HDIM services. Run docker compose up -d to start.',
    },
    {
      name: 'Kubernetes',
      description:
        'Production-grade container orchestration with auto-scaling, rolling updates, and high availability. Recommended for health systems processing large patient populations across multiple Millennium facilities. Compatible with OCI Kubernetes Engine (OKE) for deployments on Oracle Cloud Infrastructure alongside cloud-hosted Millennium.',
    },
  ],
};
