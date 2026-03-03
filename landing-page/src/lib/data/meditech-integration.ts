import type { IntegrationPageData } from '@/lib/types/integration';

export const MEDITECH_INTEGRATION: IntegrationPageData = {
  ehrName: 'Meditech',
  tagline: 'Quality Measurement for Community Hospitals and Health Systems',
  heroDescription:
    'Community hospitals have the same HEDIS and value-based care reporting requirements as large academic medical centers — but 1/10th of the IT staff to meet them. HDIM deploys on the hospital\'s existing on-premises infrastructure, even a single RHEL server, and sits between Meditech Expanse and clinical workflows to automate what quality teams are currently handling with manual chart abstraction and spreadsheets.',

  sections: [
    {
      id: 'overview',
      title: 'Overview',
      type: 'text',
      content:
        'Meditech Expanse is the right EHR for this problem. It introduced native FHIR R4 APIs and US Core data model compliance, which means HDIM can connect directly to the clinical data repository without custom HL7 v2 interfaces or proprietary extract files. But the reality in community hospital deployments is that most organizations running Expanse have not fully leveraged those FHIR capabilities — because they do not have the engineering staff to build against them. The APIs are there. The connection logic is already written. HDIM fills the gap between what Expanse can expose and what the quality team can actually consume.\n\nHDIM deploys on the hospital\'s own infrastructure — on-premises RHEL, a private VMware cluster, or their cloud VPC — and fronts the Meditech FHIR Server. Clinical data never leaves the hospital\'s network perimeter. The HDIM services connect upstream to pull FHIR resources and run CQL measure logic, then surface care gap results into the workflows and registries the clinical team already uses. For a community hospital doing manual abstraction today, the operational shift is significant: quality reporting moves from a retrospective annual exercise to a continuous automated process running against live Meditech data.',
    },
    {
      id: 'architecture',
      title: 'Architecture',
      type: 'diagram',
      content:
        'HDIM connects to Meditech Expanse through the Meditech FHIR Server. Clinical data flows through HDIM\'s microservice pipeline for quality measure evaluation and care gap detection.',
      codeSnippet: `┌──────────────────────────────────────────────────────────────────┐
│                        Meditech Expanse                          │
│  ┌──────────────────┐  ┌──────────────────┐  ┌───────────────┐  │
│  │ Clinical Data     │  │ Meditech FHIR    │  │ Expanse Web   │  │
│  │ Repository        │→ │ Server (R4)      │  │ & Mobile EHR  │  │
│  └──────────────────┘  └────────┬─────────┘  └───────────────┘  │
│                                 │ FHIR R4 REST API               │
└─────────────────────────────────┼────────────────────────────────┘
                                  │
                                  ▼
┌───────────────────────────────────────────────────────────────────┐
│                           HDIM Platform                           │
│                                                                   │
│  ┌──────────┐   ┌──────────────┐   ┌──────────┐   ┌───────────┐ │
│  │ FHIR     │ → │ Patient      │ → │ CQL      │ → │ Care Gap  │ │
│  │ Service  │   │ Service      │   │ Engine   │   │ Service   │ │
│  │ (8085)   │   │ (8084)       │   │ (8081)   │   │ (8086)    │ │
│  └──────────┘   └──────────────┘   └──────────┘   └───────────┘ │
│       ↑                                                 │        │
│       │              ┌──────────────┐                   ▼        │
│       └──────────────│ Quality      │←── Quality Reports &      │
│                      │ Measure Svc  │    Care Gap Closures       │
│                      │ (8087)       │                            │
│                      └──────────────┘                            │
└───────────────────────────────────────────────────────────────────┘`,
      codeLanguage: 'text',
    },
    {
      id: 'integration-steps',
      title: 'Integration Guide',
      type: 'steps',
      content:
        'Follow these six steps to connect HDIM with your Meditech Expanse deployment.',
      items: [
        'Configure the Meditech Expanse FHIR endpoint — Point HDIM\'s FHIR Service to your Meditech FHIR Server base URL (typically https://your-meditech-host/fhir/r4). Obtain the endpoint from your Meditech technical contact or the Expanse administration console under System Configuration > FHIR Settings.',
        'Set up OAuth2 authentication — Register HDIM as an authorized SMART on FHIR application in the Meditech Expanse app gallery or via your Meditech system administrator. Configure the client ID, client secret, and token endpoint in HDIM\'s environment variables. Meditech Expanse supports both SMART backend services (client credentials) for server-to-server integration and SMART launch for user-delegated access.',
        'Map FHIR resources to HDIM\'s patient model — Configure which FHIR R4 resource types to ingest from the Meditech FHIR Server. Verify US Core profile conformance for each resource type. Map any Meditech-specific extensions (e.g., facility identifiers, provider NPIs) to HDIM\'s internal data model using the HDIM resource mapping configuration.',
        'Configure the data ingestion mode — Choose real-time FHIR queries for triggered evaluation (e.g., on ADT events), scheduled FHIR Bulk Data export for population-level HEDIS analysis, or hybrid mode. For community hospitals with fewer than 50,000 attributed lives, real-time mode is typically sufficient. For health system rollups across multiple Meditech sites, use bulk export with nightly scheduling.',
        'Deploy HDIM on your infrastructure — Deploy HDIM on your RHEL server or private cloud VPC using Docker Compose. Set SPRING_PROFILES_ACTIVE=production and FHIR_SERVICE_URL to your Meditech FHIR R4 endpoint. For community hospital volumes (5,000–150,000 attributed patients), right-size the deployment with reduced connection pool sizing (10 connections), fewer Kafka partitions (6), and conservative memory allocation — HDIM runs comfortably on a single 4-core/16GB server without enterprise-grade infrastructure.',
        'Verify connectivity with smoke tests — Run the HDIM integration verification suite to confirm FHIR endpoint reachability, OAuth2 token acquisition, resource ingestion for each configured type, CQL measure evaluation against a synthetic patient, and care gap detection. Review the verification report and resolve any resource mapping warnings before enabling production data flows.',
      ],
    },
    {
      id: 'configuration',
      title: 'Configuration',
      type: 'code',
      content:
        'Add these environment variables to your HDIM deployment to connect to Meditech Expanse.',
      codeSnippet: `# Configuration reference — actual property names may vary by release
# Meditech Expanse Connection
# FHIR_SERVICE_URL points to the Meditech FHIR Server on the hospital's network
FHIR_SERVICE_URL=https://your-meditech-host/fhir/r4
FHIR_AUTH_TYPE=oauth2
FHIR_CLIENT_ID=hdim-meditech-client
FHIR_CLIENT_SECRET=<your-client-secret>
FHIR_TOKEN_URL=https://your-meditech-host/oauth2/token
FHIR_SCOPE=system/*.read

# Spring Boot profile
SPRING_PROFILES_ACTIVE=production

# Right-sized for community hospital volumes (5,000–150,000 attributed lives)
# Smaller pool sizes and fewer Kafka partitions than enterprise deployments —
# runs comfortably on a single 4-core/16GB server
FHIR_CONNECTION_POOL_SIZE=10
FHIR_BULK_EXPORT_ENABLED=true
FHIR_BULK_EXPORT_POLL_INTERVAL=60
KAFKA_PARTITIONS=6
REDIS_CACHE_MAX_MEMORY=512mb
PATIENT_BATCH_SIZE=1000

# Meditech-Specific Settings
FHIR_MEDITECH_SITE_ID=<your-meditech-site-id>
FHIR_US_CORE_STRICT_VALIDATION=true
FHIR_BULK_EXPORT_GROUP_ID=<hedis-population-group-id>`,
      codeLanguage: 'bash',
    },
    {
      id: 'community-hospital',
      title: 'Why Community Hospitals Benefit',
      type: 'text',
      content:
        'Community hospitals and critical access hospitals face a paradox in value-based care: they are subject to the same HEDIS, MIPS, and CMS quality reporting requirements as large academic medical centers, yet operate with a fraction of the quality department resources. Many community hospitals rely on manual chart abstraction, spreadsheet-based care gap tracking, and annual retroactive reporting — processes that are labor-intensive, error-prone, and too slow to drive real clinical improvement.\n\nHDIM eliminates manual quality abstraction by automating measure evaluation directly against Meditech Expanse clinical data. Key benefits for community hospitals include:\n\n• Automated HEDIS measure evaluation — Replace manual chart review with real-time CQL execution against Meditech FHIR data. Evaluate 52+ HEDIS measures continuously without additional FTEs.\n\n• Proactive care gap closure — Identify open care gaps at the point of care, before the measurement year ends. HDIM surfaces actionable gaps in workflows that integrate with Meditech Expanse clinical alerts and registries.\n\n• Rural health quality priorities — HDIM\'s measure library includes CMS Rural Health measures, telehealth-eligible measure logic, and social determinants of health (SDOH) screening measures relevant to the populations community hospitals serve.\n\n• Right-sized infrastructure — The HDIM community hospital deployment profile runs on a single server (4 cores, 16GB RAM) using Docker Compose, with no Kubernetes or enterprise infrastructure required. Critical access hospitals can deploy HDIM on existing on-premises hardware.\n\n• Reduced reporting burden — Generate CMS Web Interface submissions, MIPS quality category data, and payer-specific HEDIS supplemental data feeds directly from HDIM, eliminating duplicate abstraction work across multiple reporting programs.',
    },
    {
      id: 'data-exchange',
      title: 'Data Exchange Protocols',
      type: 'table',
      content: 'HDIM supports multiple data exchange protocols with Meditech Expanse.',
      tableData: [
        {
          label: 'FHIR R4 REST',
          value:
            'Primary — real-time patient and population queries via the Meditech FHIR Server with US Core profile conformance',
        },
        {
          label: 'HL7 v2 ADT',
          value:
            'Admission/discharge/transfer event feeds for real-time care gap trigger detection and patient panel updates',
        },
        {
          label: 'CDA/C-CDA',
          value:
            'Clinical document import with automatic FHIR R4 conversion for transition-of-care records and referral documents',
        },
        {
          label: 'FHIR Bulk Export',
          value:
            'Population-level NDJSON export via the Meditech FHIR Bulk Data API for scheduled HEDIS cohort evaluation and annual reporting',
        },
      ],
    },
  ],

  fhirResources: [
    { name: 'Patient', description: 'Demographics, identifiers, contact information' },
    { name: 'Encounter', description: 'Inpatient visits, outpatient encounters, and ED interactions' },
    { name: 'Condition', description: 'Diagnoses, problems, and chronic condition records (ICD-10)' },
    { name: 'Observation', description: 'Lab results, vital signs, screenings, and clinical assessments' },
    { name: 'Procedure', description: 'Surgical procedures, preventive screenings, and interventions' },
    { name: 'MedicationRequest', description: 'Prescriptions, medication orders, and reconciliation records' },
    { name: 'Immunization', description: 'Vaccination records and immunization history' },
    { name: 'DiagnosticReport', description: 'Laboratory, pathology, and radiology reports' },
    { name: 'AllergyIntolerance', description: 'Allergy and adverse drug reaction records' },
    { name: 'DocumentReference', description: 'Clinical documents including CDA, C-CDA, and discharge summaries' },
  ],

  deploymentModels: [
    {
      name: 'On-Premises (RHEL)',
      description:
        'Deploy HDIM on your existing RHEL 7 or RHEL 8 on-premises infrastructure using the HDIM installer. All clinical data remains within your network perimeter — required for critical access hospitals and community health systems with strict data residency or network isolation policies.',
      link: '/docs/deployment/RHEL7-DEPLOYMENT-GUIDE.md',
    },
    {
      name: 'Docker Compose',
      description:
        'Single-command deployment with Docker Compose using the community hospital profile. Runs on a single server (4 cores, 16GB RAM minimum) with no Kubernetes or enterprise orchestration required. Recommended for community hospitals and pilot programs.',
    },
    {
      name: 'Kubernetes',
      description:
        'Production-grade orchestration with auto-scaling and high availability for health systems operating multiple Meditech Expanse sites. Enables centralized quality measurement across a regional network of community hospitals under a single HDIM deployment.',
    },
  ],
};
