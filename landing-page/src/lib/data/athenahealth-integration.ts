import type { IntegrationPageData } from './intersystems-integration';

export const ATHENAHEALTH_INTEGRATION: IntegrationPageData = {
  ehrName: 'Athenahealth',
  tagline: 'Cloud-Native Quality Measurement for the Modern Practice',
  heroDescription:
    'Athenahealth is cloud-native, which means HDIM deploys in the customer\'s cloud VPC and connects to athena APIs over HTTPS — no on-premises footprint required. The clinical value is straightforward: ambulatory practices face the same HEDIS and value-based care reporting burden as large health systems, but they\'re doing it with a fraction of the IT staff. HDIM automates what those practices are currently handling with manual abstraction and spreadsheets.',

  sections: [
    {
      id: 'overview',
      title: 'Overview',
      type: 'text',
      content:
        'From an architecture standpoint, Athenahealth is one of the cleaner integrations we support. Because all athena customers share the same managed OAuth2 infrastructure at api.platform.athena.io, HDIM can be configured once and deployed to any athena customer without per-site credential management. There are no VPNs to negotiate, no on-premises API servers to reach, and no version fragmentation across sites — you point HDIM at the athena FHIR R4 endpoint, configure OAuth2 client credentials, and the connection works.\n\nBut the architectural simplicity is secondary to the clinical problem it solves. Independent physician practices, specialty groups, and community health organizations on athena have the same HEDIS reporting requirements as large payer-contracted health systems — but they do not have quality departments running manual abstraction cycles. Many are submitting MIPS quality data based on incomplete chart pulls or outsourced abstraction vendors. HDIM sits between the athena CDR and those workflows, running CQL measure logic continuously against live FHIR data. Practices see their care gaps in real time rather than retroactively.\n\nHDIM deploys in the customer\'s cloud VPC — typically the same region as their athena connectivity — and all data flows over outbound HTTPS. Clinical data does not leave the customer\'s environment.',
    },
    {
      id: 'architecture',
      title: 'Architecture',
      type: 'diagram',
      content:
        'HDIM connects to Athenahealth through the athenaClinicals FHIR R4 API hosted on the athenaNet cloud platform. All data flows through Athenahealth\'s managed OAuth2 layer before entering the HDIM pipeline.',
      codeSnippet: `┌──────────────────────────────────────────────────────────────────┐
│                      athenaNet Cloud Platform                    │
│                                                                  │
│  ┌───────────────────┐   ┌────────────────────────────────────┐  │
│  │ Athenahealth       │   │ athenaClinicals FHIR R4 API        │  │
│  │ Identity Provider  │ → │ (OAuth2-protected REST endpoint)   │  │
│  │ (OAuth2 / OIDC)    │   │ https://api.platform.athena.io/    │  │
│  └───────────────────┘   └──────────────┬─────────────────────┘  │
│                                         │ FHIR R4 REST            │
└─────────────────────────────────────────┼────────────────────────┘
                                          │
                                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                          HDIM Platform                          │
│                                                                 │
│  ┌──────────┐   ┌──────────────┐   ┌──────────┐   ┌─────────┐  │
│  │ FHIR     │ → │ Patient      │ → │ CQL      │ → │ Care Gap│  │
│  │ Service  │   │ Service      │   │ Engine   │   │ Service │  │
│  │ (8085)   │   │ (8084)       │   │ (8081)   │   │ (8086)  │  │
│  └──────────┘   └──────────────┘   └──────────┘   └─────────┘  │
│       ↑                                                │        │
│       │              ┌──────────────┐                  ▼        │
│       └──────────────│ Quality      │←── Quality Reports &     │
│                      │ Measure Svc  │    Care Gap Closures     │
│                      │ (8087)       │                           │
│                      └──────────────┘                           │
└─────────────────────────────────────────────────────────────────┘`,
      codeLanguage: 'text',
    },
    {
      id: 'integration-steps',
      title: 'Integration Guide',
      type: 'steps',
      content:
        'Follow these six steps to connect HDIM with your Athenahealth athenaClinicals deployment.',
      items: [
        'Register your application in the Athena Developer Portal — Navigate to https://developer.athenahealth.com and create a new application. Select the FHIR R4 API product and request the required clinical data scopes (patient/Patient.read, patient/Observation.read, patient/Condition.read, etc.). Note your Client ID and Client Secret upon registration.',
        'Configure OAuth2 credentials in HDIM — Set the FHIR_CLIENT_ID, FHIR_CLIENT_SECRET, and FHIR_TOKEN_URL environment variables using the credentials from the Athena Developer Portal. Athenahealth uses standard OAuth2 client credentials flow for backend service integrations — no SMART on FHIR context required for server-to-server calls.',
        'Set up the FHIR R4 endpoint URL — Configure FHIR_SERVER_URL to point to the athenaClinicals FHIR R4 base URL for your practice or group: https://api.platform.athena.io/fhir/r4. For multi-practice deployments, configure HDIM\'s tenant-to-practice-id mapping to route requests to the correct Athenahealth practice ID.',
        'Configure Athenahealth-specific resource mappings — Athenahealth uses a subset of FHIR R4 with proprietary extensions for practice management data. Configure HDIM\'s athena resource mapping profile to correctly interpret athena-specific Observation codes, Encounter class codes, and Coverage payer identifiers used in HEDIS measure logic.',
        'Deploy HDIM on your infrastructure — Deploy HDIM on your cloud VPC or on-premises servers using Docker Compose. Set SPRING_PROFILES_ACTIVE=production and FHIR_SERVICE_URL to the Athenahealth FHIR R4 endpoint. Configure connection pool sizing to respect Athenahealth\'s API rate limits (5 requests/second per practice) and set OAuth2 token refresh intervals appropriate for your practice count. HDIM connects to Athenahealth\'s cloud API from within your network — PHI processing stays on your infrastructure.',
        'Verify end-to-end connectivity — Run the HDIM integration smoke test suite to confirm OAuth2 token acquisition, FHIR resource retrieval, athena-specific field mapping, CQL/HEDIS measure evaluation, and care gap detection are all functioning correctly. Use the /api/v1/integration/verify endpoint to trigger automated verification.',
      ],
    },
    {
      id: 'configuration',
      title: 'Configuration',
      type: 'code',
      content:
        'Add these environment variables to your HDIM deployment to connect to Athenahealth athenaClinicals.',
      codeSnippet: `# .env — Athenahealth athenaClinicals Connection
FHIR_SERVICE_URL=https://api.platform.athena.io/fhir/r4
FHIR_AUTH_TYPE=oauth2
FHIR_CLIENT_ID=<your-athena-client-id>
FHIR_CLIENT_SECRET=<your-athena-client-secret>
FHIR_TOKEN_URL=https://api.platform.athena.io/oauth2/v1/token
FHIR_SCOPE=system/Patient.read system/Observation.read system/Condition.read system/Procedure.read system/MedicationRequest.read system/Immunization.read system/DiagnosticReport.read system/Coverage.read

# Athenahealth Practice Configuration
ATHENA_PRACTICE_ID=<your-practice-id>
ATHENA_API_VERSION=v1

# Rate Limiting — Athena enforces 5 req/sec per practice; stay under it
FHIR_RATE_LIMIT_REQUESTS_PER_SECOND=5
FHIR_CONNECTION_POOL_SIZE=10

# Spring Boot profile and ambulatory-optimized settings
SPRING_PROFILES_ACTIVE=production
FHIR_BULK_EXPORT_ENABLED=false
PATIENT_BATCH_SIZE=500
REDIS_CACHE_MAX_MEMORY=512mb`,
      codeLanguage: 'bash',
    },
    {
      id: 'cloud-advantage',
      title: 'Cloud-Native Integration Advantages',
      type: 'text',
      content:
        'Athenahealth\'s fully cloud-native architecture provides several meaningful advantages when integrating with HDIM compared to on-premises EHR systems.\n\n• No on-site infrastructure required — Because Athenahealth runs on managed cloud infrastructure, there is no need to deploy HDIM inside a practice\'s network or configure site-to-site VPNs. All connectivity is outbound HTTPS from HDIM to the athenaNet API, simplifying network architecture and security review.\n\n• Standardized OAuth2 across all customers — Unlike Epic (where each hospital system configures its own SMART on FHIR authorization server) or on-premises Cerner (where API access varies by site), Athenahealth provides a single, consistent OAuth2 endpoint at api.platform.athena.io for all customers. This means HDIM can be configured once and deployed to any Athenahealth customer without per-site credential management.\n\n• Faster time to integration — The combination of standardized OAuth2, a well-documented FHIR R4 API, and Athenahealth\'s developer portal with sandbox environments typically reduces integration time from weeks (for on-premises EHRs) to days. HDIM\'s athena profile pre-configures all Athenahealth-specific settings, enabling same-day connectivity for new customers.\n\n• Automatic API upgrades — Athenahealth manages API versioning centrally, so HDIM customers benefit from FHIR API improvements without requiring any infrastructure changes. HDIM maintains compatibility with the current Athenahealth FHIR R4 API version and provides migration guidance when breaking changes are announced.\n\n• Ambulatory quality measure focus — Athenahealth\'s customer base is concentrated in ambulatory settings (independent practices, specialty groups, community health centers) — the same settings where many HEDIS ambulatory measures (Diabetes Care, Preventive Screenings, Well-Child Visits) generate the highest volume of care gaps. HDIM\'s athena profile prioritizes ambulatory CQL measure libraries to match this use case.',
    },
    {
      id: 'data-exchange',
      title: 'Data Exchange Protocols',
      type: 'table',
      content: 'HDIM supports multiple data exchange protocols with Athenahealth.',
      tableData: [
        {
          label: 'FHIR R4 REST',
          value:
            'Primary — real-time patient queries and resource retrieval via athenaClinicals FHIR R4 API at api.platform.athena.io',
        },
        {
          label: 'Athena Proprietary API',
          value:
            'Supplemental access to practice management data (scheduling, billing codes, payer contracts) not yet available in the FHIR R4 surface; used for HEDIS attribution and coverage verification',
        },
        {
          label: 'Bulk Export (SFTP)',
          value:
            'Population-level data export for batch HEDIS measure evaluation across large ambulatory populations; Athenahealth delivers NDJSON-formatted files via SFTP for offline processing',
        },
        {
          label: 'Changed Data Capture',
          value:
            'Webhook-based event notifications from athenaNet when clinical data changes (new lab results, updated diagnoses, completed encounters); used by HDIM to trigger incremental care gap re-evaluation without full patient re-query',
        },
      ],
    },
  ],

  fhirResources: [
    { name: 'Patient', description: 'Demographics, identifiers, contact information' },
    { name: 'Encounter', description: 'Office visits, telehealth encounters, and clinical interactions' },
    { name: 'Condition', description: 'Diagnoses, problems, and health concerns (ICD-10)' },
    { name: 'Observation', description: 'Lab results, vitals, social history, and clinical assessments' },
    { name: 'Procedure', description: 'Procedures, interventions, preventive screenings, and immunizations administered' },
    { name: 'MedicationRequest', description: 'Prescriptions and medication orders' },
    { name: 'Immunization', description: 'Vaccination records and immunization history' },
    { name: 'DiagnosticReport', description: 'Laboratory reports and diagnostic study results' },
    { name: 'AllergyIntolerance', description: 'Allergy and adverse reaction records' },
    { name: 'Coverage', description: 'Insurance and payer information for HEDIS measure attribution' },
  ],

  deploymentModels: [
    {
      name: 'Cloud-Hosted',
      description:
        'Deploy HDIM in a cloud environment (AWS, Azure, or GCP) to co-locate with Athenahealth\'s cloud infrastructure. Minimizes network latency to the athenaNet API, simplifies OAuth2 connectivity, and eliminates on-premises infrastructure requirements. Recommended for new Athenahealth integrations.',
    },
    {
      name: 'Docker Compose',
      description:
        'Single-command deployment with Docker Compose using the athena profile. Ideal for pilot programs, development environments, and smaller ambulatory deployments. Includes all 51+ HDIM services pre-configured with Athenahealth-specific rate limiting and resource mappings.',
    },
    {
      name: 'Kubernetes',
      description:
        'Production-grade orchestration with auto-scaling, rolling updates, and high availability. Recommended for large ambulatory network deployments processing multiple Athenahealth practice IDs or high patient volumes across health systems with significant Athenahealth penetration.',
    },
  ],
};
