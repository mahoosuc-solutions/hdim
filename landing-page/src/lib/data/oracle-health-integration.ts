import type { IntegrationPageData } from './intersystems-integration';

export const ORACLE_HEALTH_INTEGRATION: IntegrationPageData = {
  ehrName: 'Oracle Health (Cerner)',
  tagline: 'Enterprise Quality Measurement on the Millennium Platform',
  heroDescription:
    'Connect HDIM to Oracle Health (formerly Cerner) to evaluate CQL/HEDIS quality measures across the Millennium platform — the EHR powering 27% of US hospitals. Native CDS Hooks integration delivers real-time care gap alerts at the point of care, while Oracle Cloud migration support future-proofs your quality measurement infrastructure.',

  sections: [
    {
      id: 'overview',
      title: 'Overview',
      type: 'text',
      content:
        'Oracle Health, formerly Cerner Corporation, operates the Millennium platform — one of the two dominant EHR systems in the United States with approximately 27% of US hospital market share. In 2022, Oracle Corporation acquired Cerner and began the strategic migration of Millennium onto Oracle Cloud Infrastructure (OCI), creating a path toward cloud-native EHR capabilities.\n\nThe Millennium platform provides strong FHIR R4 support through the Cerner FHIR Server, which exposes standardized REST APIs for patient data retrieval, clinical document exchange, and bulk data export. CDS Hooks is a first-class feature of Millennium, enabling real-time clinical decision support cards to surface inside clinical workflows — making Oracle Health an ideal integration target for HDIM\'s care gap alerting capabilities.\n\nHDIM integrates with Oracle Health via the Cerner FHIR R4 endpoint, leveraging OAuth2 SMART on FHIR authentication through the Cerner Code developer program. The integration supports real-time patient queries, population-level Bulk FHIR export, and bidirectional CDS Hooks for surfacing care gap notifications directly in the Millennium clinical workflow.',
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
        'Enable CDS Hooks for care gap alerts — Register HDIM as a CDS Hooks service provider in the Millennium CDS configuration. Point the hook endpoint to https://{hdim-host}/cds-hooks/services. HDIM exposes patient-view and order-select hooks for care gap detection. Configure which HEDIS measure care gaps should surface as CDS Cards in clinical workflows (e.g., HbA1c testing reminders for diabetic patients).',
        'Deploy HDIM — Use Docker Compose or the HDIM installer with Oracle Health-specific environment variables. Run docker compose --profile oracle-health up -d to start all 51+ services with Cerner-optimized connection pooling and CDS Hooks listener pre-configured. Alternatively, use the HDIM installer: ./hdim-installer.sh --ehr cerner --tenant-id {your-millennium-tenant-id}.',
        'Verify integration — Run the built-in verification suite to confirm end-to-end connectivity. Execute docker compose exec fhir-service curl http://localhost:8085/fhir/actuator/health to check FHIR connectivity. Submit a test CDS Hooks prefetch request to confirm care gap card generation. Review HDIM audit logs to verify FHIR resource ingestion, CQL measure evaluation, and care gap detection are functioning correctly.',
      ],
    },
    {
      id: 'configuration',
      title: 'Configuration',
      type: 'code',
      content:
        'Add these environment variables to your HDIM deployment to connect to Oracle Health Millennium.',
      codeSnippet: `# .env — Oracle Health (Cerner Millennium) Connection

# Cerner Millennium FHIR R4 Endpoint
# Cloud / Cerner-hosted tenants:
FHIR_SERVER_URL=https://fhir-ehr.cerner.com/r4/<your-millennium-tenant-id>
# On-premises Millennium:
# FHIR_SERVER_URL=https://<millennium-host>/fhir/r4

# OAuth2 SMART on FHIR (Backend Services)
FHIR_AUTH_TYPE=oauth2
CERNER_CLIENT_ID=<your-cerner-code-client-id>
CERNER_CLIENT_SECRET=<your-cerner-code-client-secret>
FHIR_TOKEN_URL=https://authorization.cerner.com/tenants/<your-tenant-id>/protocols/oauth2/profiles/smart-v1/token

# CDS Hooks (real-time care gap alerts in Millennium workflows)
CDS_HOOKS_ENABLED=true
CDS_HOOKS_BASE_URL=https://<hdim-host>/cds-hooks
CDS_HOOKS_PATIENT_VIEW=true
CDS_HOOKS_ORDER_SELECT=true

# Bulk FHIR Export (population-level analysis)
FHIR_BULK_EXPORT_ENABLED=true
FHIR_BULK_EXPORT_POLL_INTERVAL=30

# Oracle Cloud Infrastructure (if using OCI-hosted Millennium)
# OCI_TENANT_ID=<your-oci-tenancy-ocid>
# OCI_REGION=us-ashburn-1

# Connection tuning
FHIR_CONNECTION_POOL_SIZE=20
PATIENT_BATCH_SIZE=1000
REDIS_CACHE_MAX_MEMORY=1gb`,
      codeLanguage: 'bash',
    },
    {
      id: 'cds-hooks',
      title: 'CDS Hooks Integration',
      type: 'text',
      content:
        'Oracle Health Millennium provides native CDS Hooks support, making it one of the most powerful integration targets for real-time clinical decision support. HDIM leverages this capability to surface care gap notifications directly within clinician workflows — at precisely the moment when intervention is most impactful.\n\nWhen a clinician opens a patient chart in Millennium (triggering the patient-view hook), HDIM evaluates applicable HEDIS quality measures in real time and returns CDS Cards identifying open care gaps. For example, if a diabetic patient is due for an HbA1c test, HDIM returns a card with the care gap details, the overdue duration, and a suggested action — all surfaced inline in the Millennium clinical workflow without requiring the clinician to leave the EHR.\n\nHDIM supports the following CDS Hooks workflows with Oracle Health:\n\n• patient-view: Evaluates all applicable HEDIS measures when a patient chart is opened. Returns care gap cards for overdue preventive screenings, chronic disease management, and medication adherence.\n\n• order-select: Intercepts medication and procedure orders to check for HEDIS measure opportunities. For example, flags when a statin order for a cardiovascular patient presents an opportunity to address a related care gap.\n\n• appointment-book: Pre-visit care gap reminders when an appointment is scheduled, enabling care coordination staff to prepare relevant preventive care materials.\n\nCDS Hook responses from HDIM include structured FHIR-based suggestions that can auto-populate orders, referrals, or care plan entries in Millennium, enabling one-click care gap closure directly from the alert card.',
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
          label: 'CDS Hooks',
          value:
            'Real-time clinical decision support cards surfaced in Millennium workflows. HDIM acts as a CDS Hooks service provider for patient-view, order-select, and appointment-book hooks, delivering care gap alerts at the point of care.',
        },
        {
          label: 'Webhooks',
          value:
            'Event-driven notifications for care gap status changes, measure evaluation completions, and quality report generation. HDIM publishes webhook events to configured endpoints for downstream system integration.',
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
        'Deploy HDIM on your existing infrastructure alongside an on-premises Millennium installation. Supports RHEL 7/8, Ubuntu 20.04+, and Windows Server 2019+. Data never leaves your network, meeting strict data residency and HIPAA security requirements. Use the HDIM installer with --ehr cerner for Millennium-optimized configuration.',
    },
    {
      name: 'Docker Compose',
      description:
        'Single-command deployment with Docker Compose for rapid pilot programs, development environments, and mid-size Oracle Health implementations. Includes all 51+ HDIM services pre-configured with Cerner FHIR connectivity and CDS Hooks listener. Run docker compose --profile oracle-health up -d to start.',
    },
    {
      name: 'Kubernetes',
      description:
        'Production-grade container orchestration with auto-scaling, rolling updates, and high availability. Recommended for health systems processing large patient populations across multiple Millennium facilities. Compatible with OCI Kubernetes Engine (OKE) for deployments on Oracle Cloud Infrastructure alongside cloud-hosted Millennium.',
    },
  ],
};
