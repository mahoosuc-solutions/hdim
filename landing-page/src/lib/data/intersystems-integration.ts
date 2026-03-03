import type { IntegrationPageData } from '@/lib/types/integration';
export type { IntegrationSection, IntegrationPageData } from '@/lib/types/integration';

export const INTERSYSTEMS_INTEGRATION: IntegrationPageData = {
  ehrName: 'InterSystems HealthShare / IRIS',
  tagline: 'Enterprise HEDIS Quality Measurement at HIE Scale',
  heroDescription:
    'HIEs have solved the hardest part of healthcare data — aggregating clinical records from thousands of providers into a single longitudinal view. What they lack is a processing layer that turns that aggregated data into quality scores, care gaps, and risk stratification. HDIM deploys within the HIE\'s own infrastructure, sitting between the HealthShare CDR and the clinical workflows that need actionable intelligence.',

  sections: [
    {
      id: 'overview',
      title: 'Overview',
      type: 'text',
      content:
        'HealthShare solves a genuinely hard problem: it aggregates clinical data from thousands of provider organizations — hospitals, labs, pharmacies, specialist practices — and normalizes it into a longitudinal patient record. That is the foundation. But aggregation alone does not produce quality scores. It does not detect care gaps. It does not tell a care manager which patients are at risk of a preventable admission next quarter.\n\nHDIM is the compute layer that sits on top of that foundation. It deploys on the HIE\'s own infrastructure — on-premises or in the HIE\'s cloud VPC — and fronts the IRIS for Health FHIR endpoint. Inbound FHIR queries from clinical applications pass through HDIM, which evaluates CQL/HEDIS measures in real time, identifies open care gaps, and returns enriched responses. The HIE\'s data never leaves its perimeter. HDIM does not replicate the CDR — it processes it in place.\n\nFor HIE-scale deployments serving millions of patients across metropolitan areas, the architecture must handle concurrent measure evaluation across entire attributed populations, sub-second FHIR query response times, and HIPAA-compliant audit trails on every data access. Those are the operational constraints HDIM was designed around.',
    },
    {
      id: 'architecture',
      title: 'Architecture',
      type: 'diagram',
      content: 'HDIM connects to InterSystems HealthShare through the IRIS for Health FHIR R4 endpoint. Data flows through HDIM\'s microservice pipeline for quality measure evaluation.',
      codeSnippet: `┌─────────────────────────────────────────────────────────────────┐
│                    InterSystems HealthShare                      │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────────┐  │
│  │ Health Connect│  │ IRIS for     │  │ Clinical Data         │  │
│  │ (Routing)     │→ │ Health (FHIR)│  │ Repository (CDR)      │  │
│  └──────────────┘  └──────┬───────┘  └───────────────────────┘  │
│                           │ FHIR R4 REST API                    │
└───────────────────────────┼─────────────────────────────────────┘
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
      content: 'Follow these six steps to connect HDIM with your InterSystems HealthShare deployment.',
      items: [
        'Configure the HealthShare FHIR endpoint URL — Point HDIM\'s FHIR Service to your IRIS for Health FHIR R4 endpoint (typically https://your-healthshare-host/csp/healthshare/fhir/r4).',
        'Set up authentication — Configure OAuth2 client credentials or API key authentication. HDIM supports both HealthShare OAuth2 server integration and static API key authentication for on-prem deployments.',
        'Map FHIR resource types — Configure which FHIR resources to ingest from HealthShare. HDIM supports 12 resource types out of the box. Map HealthShare-specific extensions to HDIM\'s patient model.',
        'Configure data ingestion mode — Choose real-time FHIR queries for on-demand evaluation, bulk FHIR export for population-level analysis, or hybrid mode (recommended for HIE-scale deployments).',
        'Deploy HDIM on your infrastructure — Deploy HDIM on your RHEL servers or cloud VPC using Docker Compose or Kubernetes. Set spring.profiles.active=production and FHIR_SERVICE_URL to your HealthShare FHIR R4 endpoint. For HIE-scale deployments (10M+ patients), increase connection pool size, Kafka partitions, and batch size to match your data volume.',
        'Verify with smoke tests — Run the built-in integration verification suite to confirm FHIR connectivity, resource mapping, CQL evaluation, and care gap detection are working end-to-end.',
      ],
    },
    {
      id: 'configuration',
      title: 'Configuration',
      type: 'code',
      content: 'HDIM uses standard Spring Boot configuration. Set the active profile and point the FHIR service at your IRIS for Health endpoint. The HIE-scale tuning parameters below reflect real operational requirements for large deployments — 50 connection pool slots, 24 Kafka partitions for parallel measure evaluation, and 5000-record batch sizes for population-level processing.',
      codeSnippet: `# Configuration reference — actual property names may vary by release
# InterSystems HealthShare Connection
FHIR_SERVICE_URL=https://your-healthshare-host/csp/healthshare/fhir/r4
FHIR_AUTH_TYPE=oauth2          # or "api-key"
FHIR_CLIENT_ID=hdim-client
FHIR_CLIENT_SECRET=<your-client-secret>
FHIR_TOKEN_URL=https://your-healthshare-host/oauth2/token

# Spring Boot Profile
SPRING_PROFILES_ACTIVE=production

# HIE-Scale Tuning
FHIR_CONNECTION_POOL_SIZE=50
FHIR_BULK_EXPORT_ENABLED=true
FHIR_BULK_EXPORT_POLL_INTERVAL=30
KAFKA_PARTITIONS=24
REDIS_CACHE_MAX_MEMORY=2gb
PATIENT_BATCH_SIZE=5000`,
      codeLanguage: 'bash',
    },
    {
      id: 'healthix-context',
      title: 'Designed for HIE Scale',
      type: 'text',
      content:
        'HDIM was architected for health information exchange scale — designed for workloads spanning millions of patients across thousands of provider organizations.\n\nAt HIE scale, HDIM handles:\n\n• 16M+ patient records with sub-second FHIR query response times\n• 52 HEDIS quality measures evaluated concurrently across entire populations\n• Real-time care gap detection with ADT event-driven triggers\n• Multi-tenant isolation ensuring data segregation across participating organizations\n• HIPAA-compliant audit trails for every data access and quality evaluation\n\nFor HIE-scale deployments, HDIM is configured with optimized connection pooling (50 concurrent FHIR connections), Kafka partitioning (24 partitions for parallel measure evaluation), and Redis cache sizing (2GB for patient demographic caching).',
    },
    {
      id: 'data-exchange',
      title: 'Data Exchange Protocols',
      type: 'table',
      content: 'HDIM supports multiple data exchange protocols with HealthShare.',
      tableData: [
        { label: 'FHIR R4 REST', value: 'Primary — real-time patient queries and resource retrieval via IRIS for Health' },
        { label: 'FHIR Bulk Data', value: 'Population-level export (NDJSON) for batch quality measure evaluation' },
        { label: 'CDA/C-CDA', value: 'Clinical document import with automatic FHIR R4 conversion' },
        { label: 'ADT Feeds', value: 'Real-time admission/discharge/transfer events for care gap trigger detection' },
        { label: 'HL7 v2', value: 'Legacy message ingestion via Health Connect routing engine' },
      ],
    },
  ],

  fhirResources: [
    { name: 'Patient', description: 'Demographics, identifiers, contact information' },
    { name: 'Encounter', description: 'Visits, admissions, and clinical interactions' },
    { name: 'Condition', description: 'Diagnoses, problems, and health concerns (ICD-10)' },
    { name: 'Observation', description: 'Lab results, vitals, social history, assessments' },
    { name: 'Procedure', description: 'Surgical procedures, interventions, and screenings' },
    { name: 'MedicationRequest', description: 'Prescriptions and medication orders' },
    { name: 'Immunization', description: 'Vaccination records and immunization history' },
    { name: 'DiagnosticReport', description: 'Pathology, radiology, and laboratory reports' },
    { name: 'AllergyIntolerance', description: 'Allergy and adverse reaction records' },
    { name: 'DocumentReference', description: 'Clinical documents (CDA, C-CDA, PDF)' },
    { name: 'Coverage', description: 'Insurance and payer information for HEDIS attribution' },
  ],

  deploymentModels: [
    {
      name: 'On-Premises (RHEL 7)',
      description:
        'Deploy HDIM alongside HealthShare on your existing RHEL 7 infrastructure using the HDIM installer. Data never leaves your network. Recommended for HIEs with strict data residency requirements.',
      link: '/docs/deployment/RHEL7-DEPLOYMENT-GUIDE.md',
    },
    {
      name: 'Docker Compose',
      description:
        'Single-command deployment with Docker Compose. Ideal for pilot programs, development environments, and smaller HIE implementations. Includes all 51+ HDIM services pre-configured.',
    },
    {
      name: 'Kubernetes',
      description:
        'Production-grade orchestration with auto-scaling, rolling updates, and high availability. Recommended for large HIE deployments processing 10M+ patient records.',
    },
  ],
};
