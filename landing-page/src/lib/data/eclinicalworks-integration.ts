import type { IntegrationPageData } from './intersystems-integration';

export const ECLINICALWORKS_INTEGRATION: IntegrationPageData = {
  ehrName: 'eClinicalWorks',
  tagline: 'Quality Measurement for Large Ambulatory Networks',
  heroDescription:
    'FQHCs and IPAs running eClinicalWorks need quality measurement at two levels simultaneously: per-site performance that each clinic can act on, and network-level rates that roll up for payer reporting. HDIM deploys inside your network, uses its multi-tenant architecture to isolate each site as its own tenant, and aggregates across all of them without ever co-mingling site-level patient data.',

  sections: [
    {
      id: 'overview',
      title: 'Overview',
      type: 'text',
      content:
        'eClinicalWorks serves networks that can span hundreds of practice sites under a single IPA or FQHC umbrella. The measurement challenge is not technical — eCW\'s FHIR R4 server is solid and the data is accessible. The challenge is organizational: a network needs to know which sites are underperforming on Colorectal Cancer Screening, which are above threshold on Controlling High Blood Pressure, and what the aggregate rate is for their value-based contract submission. No single query against a shared FHIR endpoint gives you that without leaking one site\'s data into another site\'s view.\n\nHDIM is deployed on the network\'s own infrastructure — either on-premises or inside their private cloud VPC — and fronts the eCW FHIR endpoint. Each practice site is provisioned as an isolated HDIM tenant. Patient data is ingested, normalized, and evaluated under strict tenant boundaries enforced at the database level. Network-level quality rollups are computed as aggregations across tenant results, not by querying a shared patient pool. The result is that a network medical director can see the population-level HEDIS rates across all sites, while each site\'s clinical team sees only their own patients and their own care gaps.',
    },
    {
      id: 'architecture',
      title: 'Architecture',
      type: 'diagram',
      content:
        'HDIM connects to eClinicalWorks through the eCW FHIR R4 server. Patient data flows through the HDIM microservice pipeline for quality measure evaluation and care gap detection across the ambulatory network.',
      codeSnippet: `┌──────────────────────────────────────────────────────────────────┐
│                     eClinicalWorks Cloud                         │
│                                                                  │
│  ┌────────────────┐  ┌──────────────────┐  ┌─────────────────┐  │
│  │ eCW V11+ EHR   │  │ eCW FHIR Server  │  │ PRISMA Health   │  │
│  │ (Multi-Site)   │→ │ (FHIR R4)        │  │ Analytics       │  │
│  └────────────────┘  └────────┬─────────┘  └─────────────────┘  │
│                               │ FHIR R4 REST API / OAuth2       │
└───────────────────────────────┼──────────────────────────────────┘
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
│                      │ Measure Svc  │    Care Gap Closures      │
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
        'Follow these six steps to connect HDIM with your eClinicalWorks deployment.',
      items: [
        'Register in the eCW Developer Program — Create an account at developer.eclinicalworks.com and register your HDIM integration as an authorized application. Submit the required FHIR integration request and receive your client credentials for the eCW OAuth2 authorization server.',
        'Configure OAuth2 client credentials — Set up OAuth2 client credentials flow in HDIM using the client ID and secret issued by eCW. HDIM supports the SMART on FHIR backend services authorization profile used by eCW V11+ for system-to-system API access without user interaction.',
        'Set up the FHIR R4 endpoint — Point HDIM\'s FHIR Service to your organization\'s eCW FHIR R4 base URL (typically https://your-org.eclinicalworks.com/fhir/r4). Verify connectivity by querying the FHIR capability statement at the $metadata endpoint.',
        'Configure FHIR resource mappings — Map eCW-specific FHIR profiles and extensions to HDIM\'s internal patient model. HDIM ships with a pre-built eCW resource mapping profile that handles eCW-specific coding systems, custom extensions, and the PRISMA consolidated patient identifier.',
        'Deploy HDIM on your network infrastructure — Set spring.profiles.active=production and configure FHIR_SERVICE_URL to your organization\'s eCW FHIR R4 endpoint. Each practice site is provisioned as a separate HDIM tenant at onboarding time, enforcing data isolation at the database level. Network-level quality rollups aggregate across tenant results without co-mingling patient records.',
        'Verify with smoke tests — Run the built-in integration verification suite to confirm FHIR connectivity, OAuth2 token acquisition, resource mapping accuracy, CQL evaluation, and care gap detection are functioning end-to-end across at least one representative practice site.',
      ],
    },
    {
      id: 'configuration',
      title: 'Configuration',
      type: 'code',
      content:
        'HDIM deploys on your network and connects to your eClinicalWorks FHIR endpoint directly. Configure these Spring Boot properties for each deployment.',
      codeSnippet: `# application.properties (or environment variables)

# Active profile — controls logging, cache TTLs, and connection pool sizing
spring.profiles.active=production

# eCW FHIR R4 endpoint (hosted by eClinicalWorks, accessed from your network)
FHIR_SERVICE_URL=https://your-org.eclinicalworks.com/fhir/r4
FHIR_AUTH_TYPE=oauth2
FHIR_CLIENT_ID=<your-ecw-client-id>
FHIR_CLIENT_SECRET=<your-ecw-client-secret>
FHIR_TOKEN_URL=https://your-org.eclinicalworks.com/oauth2/token

# Bulk export for initial population load
FHIR_BULK_EXPORT_ENABLED=true
FHIR_BULK_EXPORT_POLL_INTERVAL=60

# Multi-site network sizing
FHIR_CONNECTION_POOL_SIZE=30
KAFKA_PARTITIONS=12
PATIENT_BATCH_SIZE=2000`,
      codeLanguage: 'bash',
    },
    {
      id: 'ambulatory-network',
      title: 'Scaling Across Ambulatory Networks',
      type: 'text',
      content:
        'eClinicalWorks deployments typically span dozens to hundreds of practice sites within a single organization or IPA. HDIM is purpose-built to handle this distributed ambulatory network topology through its multi-tenant architecture and network-level quality aggregation capabilities.\n\nFor large eCW networks, HDIM provides:\n\n• Per-site tenant isolation — each practice site operates within its own HDIM tenant, ensuring data segregation and site-level quality reporting while enabling network-wide rollups for IPA or ACO-level value-based care contract reporting.\n\n• PRISMA integration — HDIM can consume the eCW PRISMA consolidated patient record to evaluate quality measures against the full longitudinal patient history across all sites where a patient has received care, not just the current treating practice.\n\n• Network-level quality dashboards — aggregate HEDIS measure rates, care gap counts, and quality scores across all sites in a single quality measure report for submission to payers and accreditation bodies.\n\n• Incremental patient sync — HDIM tracks eCW patient modification timestamps to perform efficient delta synchronization, pulling only records modified since the last evaluation cycle rather than re-ingesting the full patient population on each run.\n\n• Site-specific measure customization — each practice site can configure measure-specific exclusions, supplemental data sources, and reporting periods to reflect local clinical workflows while contributing to the network-level quality report.\n\nThis architecture enables HDIM to scale from a 5-site independent practice association to a 500-site integrated delivery network without architectural changes, using Kafka partition scaling and horizontal pod autoscaling in Kubernetes.',
    },
    {
      id: 'data-exchange',
      title: 'Data Exchange Protocols',
      type: 'table',
      content: 'HDIM supports multiple data exchange protocols with eClinicalWorks.',
      tableData: [
        {
          label: 'FHIR R4 REST',
          value:
            'Primary — real-time patient queries and resource retrieval via eCW FHIR R4 server (V11+)',
        },
        {
          label: 'eCW V11+ API',
          value:
            'Native eCW API access for advanced clinical data not yet surfaced through the FHIR layer, including custom clinical forms and practice-specific templates',
        },
        {
          label: 'HL7 v2',
          value:
            'Legacy HL7 v2 ADT and ORU message ingestion for practices on eCW versions prior to V11 or for real-time event-driven care gap triggers',
        },
        {
          label: 'Direct Messaging',
          value:
            'Direct secure messaging for care gap notification delivery to eCW care coordinators and for receiving updated clinical documents from referral networks',
        },
      ],
    },
  ],

  fhirResources: [
    { name: 'Patient', description: 'Demographics, identifiers, contact information' },
    { name: 'Encounter', description: 'Ambulatory visits, telehealth encounters, and clinical interactions' },
    { name: 'Condition', description: 'Diagnoses, problems, and health concerns (ICD-10)' },
    { name: 'Observation', description: 'Lab results, vitals, social history, and clinical assessments' },
    { name: 'Procedure', description: 'Office procedures, screenings, preventive care, and interventions' },
    { name: 'MedicationRequest', description: 'Prescriptions, medication orders, and medication administration records' },
    { name: 'Immunization', description: 'Vaccination records and immunization history' },
    { name: 'DiagnosticReport', description: 'Laboratory and radiology reports linked to encounters' },
    { name: 'AllergyIntolerance', description: 'Allergy and adverse reaction records' },
    { name: 'Coverage', description: 'Insurance and payer information for HEDIS attribution and value-based care contract assignment' },
  ],

  deploymentModels: [
    {
      name: 'Cloud-Hosted',
      description:
        'Deploy HDIM as a fully managed cloud service connecting to your eClinicalWorks cloud instance. Zero infrastructure management, automatic scaling, and 99.9% SLA. Ideal for large ambulatory networks that want rapid time-to-value without on-premises infrastructure investment.',
    },
    {
      name: 'Docker Compose',
      description:
        'Single-command deployment with Docker Compose for pilot programs, development environments, and smaller eCW network implementations. Includes all HDIM services pre-configured with the ecw-ambulatory profile for immediate FHIR R4 connectivity.',
    },
    {
      name: 'Kubernetes',
      description:
        'Production-grade orchestration with auto-scaling, rolling updates, and high availability for large eCW networks processing millions of ambulatory encounters. Supports horizontal scaling of CQL evaluation pods during peak HEDIS measurement periods.',
    },
  ],
};
