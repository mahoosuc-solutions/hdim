import type { IntegrationPageData } from './intersystems-integration';

export const EPIC_INTEGRATION: IntegrationPageData = {
  ehrName: 'Epic Systems',
  tagline: 'Enterprise HEDIS Quality Measurement for the Largest US EHR',
  heroDescription:
    'Epic\'s CDR holds the clinical data, but providers can\'t see care gaps in real time. HDIM deploys alongside your Epic infrastructure — on your servers, inside your network — and sits between the FHIR layer and your clinical workflows, computing care gaps, quality scores, and risk stratification directly from the raw FHIR resources Epic already exposes.',

  sections: [
    {
      id: 'overview',
      title: 'Overview',
      type: 'text',
      content:
        'Epic\'s FHIR R4 implementation is the most mature in the industry — over 350 health systems, comprehensive resource coverage, Bulk FHIR export, and a well-documented SMART on FHIR authorization framework. The data is there. The problem is that raw FHIR resources — scattered encounters, conditions, observations, medication requests — do not tell a Chief Medical Officer which patients have open care gaps, which measures are trending below threshold, or which populations need intervention before the next HEDIS submission window closes.\n\nHDIM deploys on your infrastructure — on-prem RHEL servers or inside your cloud VPC — and fronts the Epic FHIR endpoint. It ingests patient resources via RS384 JWT backend authentication (no end-user OAuth flows), evaluates 52+ HEDIS measures using the CQL engine, and produces care gap alerts, quality scores, and risk stratification that providers can act on at the point of care. The data never leaves your network. HDIM is not a SaaS extraction tool — it is clinical processing infrastructure that sits between your CDR and your clinical workflows.\n\nFor population-level evaluation, HDIM uses Epic\'s Bulk FHIR export ($export) to process entire patient panels in batch, while real-time FHIR queries handle on-demand evaluation during clinical encounters. Both paths produce the same quality intelligence — the difference is latency and scale.',
    },
    {
      id: 'architecture',
      title: 'Architecture',
      type: 'diagram',
      content:
        'HDIM connects to Epic through the Epic FHIR R4 server registered in App Orchard. Clinical data flows from EpicCare and MyChart through the Epic FHIR layer into the HDIM microservice pipeline for quality measure evaluation.',
      codeSnippet: `┌──────────────────────────────────────────────────────────────────────┐
│                           Epic Systems                               │
│                                                                      │
│  ┌───────────────┐   ┌───────────────┐   ┌────────────────────────┐ │
│  │  EpicCare     │   │  MyChart      │   │  App Orchard           │ │
│  │  (Clinician   │   │  (Patient     │   │  (Marketplace /        │ │
│  │   Workflows)  │   │   Portal)     │   │   Distribution)        │ │
│  └───────┬───────┘   └───────┬───────┘   └────────────┬───────────┘ │
│          └─────────┬─────────┘                        │             │
│                    ▼                                  ▼             │
│         ┌──────────────────────────────────────────────┐           │
│         │     Epic FHIR R4 Server                       │           │
│         │     (SMART on FHIR / RS384 JWT Auth)          │           │
│         │     Bulk FHIR Export ($export)                │           │
│         └──────────────────┬───────────────────────────┘           │
└────────────────────────────┼────────────────────────────────────────┘
                             │ FHIR R4 REST API
                             │ (RS384 JWT Bearer Token)
                             ▼
┌──────────────────────────────────────────────────────────────────────┐
│                           HDIM Platform                              │
│                                                                      │
│  ┌──────────┐   ┌──────────────┐   ┌──────────┐   ┌──────────────┐ │
│  │ FHIR     │ → │ Patient      │ → │ CQL      │ → │ Care Gap     │ │
│  │ Service  │   │ Service      │   │ Engine   │   │ Service      │ │
│  │ (8085)   │   │ (8084)       │   │ (8081)   │   │ (8086)       │ │
│  └──────────┘   └──────────────┘   └──────────┘   └──────┬───────┘ │
│       ↑                                                   │         │
│       │              ┌──────────────────┐                 ▼         │
│       └──────────────│ Quality Measure  │←── Quality Reports &     │
│                      │ Service          │    Care Gap Closures      │
│                      │ (8087)           │                            │
│                      └──────────────────┘                            │
└──────────────────────────────────────────────────────────────────────┘`,
      codeLanguage: 'text',
    },
    {
      id: 'integration-steps',
      title: 'Integration Guide',
      type: 'steps',
      content:
        'Follow these six steps to connect HDIM with your Epic deployment through App Orchard.',
      items: [
        'Register in Epic App Orchard — Submit your HDIM deployment as a backend system application in the Epic App Orchard. Obtain a non-expiring client ID issued by Epic\'s App Orchard review team. Backend apps using RS384 JWT do not require user-facing OAuth approval flows.',
        'Configure RS384 JWT signing certificate — Generate an RSA 2048-bit or EC P-384 key pair. Register the public key (JWK format) with Epic via the App Orchard developer portal. Store the private key securely and set EPIC_PRIVATE_KEY_PATH in your HDIM environment. Epic uses RS384 (RSA + SHA-384) for backend service authentication.',
        'Set up FHIR R4 endpoint URL — Obtain your Epic organization\'s FHIR base URL from your Epic technical contact. The format is typically https://your-epic-host/api/FHIR/R4. Set FHIR_SERVER_URL in your HDIM configuration. Confirm the endpoint supports the $metadata capability statement.',
        'Configure Bulk FHIR export for population data — Enable Epic\'s Bulk FHIR ($export) endpoint for population-level quality measure evaluation. Confirm your App Orchard application is granted system/*.read scope. Set FHIR_BULK_EXPORT_ENABLED=true and configure the polling interval for your expected export volume.',
        'Deploy HDIM on your infrastructure — Deploy HDIM on your RHEL servers or cloud VPC using Docker Compose or Kubernetes. Configure spring.profiles.active=production and set FHIR_SERVICE_URL to your Epic FHIR R4 endpoint. HDIM connects to Epic over your private network — PHI never crosses your network boundary.',
        'Verify with smoke tests — Run the HDIM integration verification suite to confirm RS384 JWT authentication, FHIR R4 resource retrieval (Patient, Encounter, Condition, Observation), Bulk FHIR export initiation and polling, CQL measure evaluation, and care gap detection are all working end-to-end against your Epic sandbox environment before promoting to production.',
      ],
    },
    {
      id: 'configuration',
      title: 'Configuration',
      type: 'code',
      content:
        'HDIM deploys on your infrastructure and connects to your Epic FHIR endpoint directly. Configure these Spring Boot properties for each deployment.',
      codeSnippet: `# application.properties (or environment variables)

# Active profile
spring.profiles.active=production

# Epic FHIR R4 endpoint (on your network)
FHIR_SERVICE_URL=https://your-epic-host/api/FHIR/R4
FHIR_AUTH_TYPE=oauth2-jwt
EPIC_CLIENT_ID=<your-app-orchard-client-id>
EPIC_PRIVATE_KEY_PATH=/run/secrets/epic_private_key.pem
FHIR_TOKEN_URL=https://your-epic-host/oauth2/token
FHIR_JWT_ALGORITHM=RS384

# Bulk FHIR Export for population-level evaluation
FHIR_BULK_EXPORT_ENABLED=true
FHIR_BULK_EXPORT_POLL_INTERVAL=30

# Connection tuning (adjust for your Epic FHIR server capacity)
FHIR_CONNECTION_POOL_SIZE=20
KAFKA_PARTITIONS=12
PATIENT_BATCH_SIZE=1000`,
      codeLanguage: 'bash',
    },
    {
      id: 'epic-ecosystem',
      title: 'Epic Ecosystem Integration',
      type: 'text',
      content:
        'Epic\'s ecosystem extends well beyond the core EHR, providing HDIM with multiple integration points for comprehensive quality measurement coverage.\n\n**App Orchard Marketplace** — Epic\'s App Orchard is the official marketplace for third-party applications that integrate with Epic. Publishing HDIM through App Orchard provides health systems with a pre-vetted, security-reviewed integration path, reducing procurement friction and accelerating deployment timelines. App Orchard applications receive a stable non-expiring client ID and are subject to Epic\'s review for data access scope, security practices, and HIPAA compliance.\n\n**MyChart Patient Access API** — MyChart is Epic\'s patient portal with over 200 million enrolled patients. HDIM can leverage the MyChart SMART on FHIR patient access APIs to supplement clinical data with patient-reported outcomes and consent preferences, enabling more complete HEDIS measure numerator capture for measures that include patient engagement criteria.\n\n**Care Everywhere (HIE)** — Epic\'s Care Everywhere network enables participating organizations to exchange clinical documents and FHIR resources across Epic installations. HDIM can ingest Care Everywhere records to build a longitudinal patient view that spans multiple Epic instances — critical for accurate attribution and measure denominator construction in multi-payer or regional quality programs.',
    },
    {
      id: 'data-exchange',
      title: 'Data Exchange Protocols',
      type: 'table',
      content: 'HDIM supports multiple data exchange protocols with Epic.',
      tableData: [
        {
          label: 'FHIR R4 REST',
          value:
            'Primary — real-time patient queries and resource retrieval via Epic\'s SMART on FHIR server using RS384 JWT backend authentication',
        },
        {
          label: 'Bulk FHIR Export',
          value:
            'Population-level asynchronous export (NDJSON) via $export endpoint for batch HEDIS quality measure evaluation across entire patient panels',
        },
        {
          label: 'CDS Hooks',
          value:
            'Event-driven care gap alerts surfaced at the point of care within EpicCare clinician workflows using Epic\'s CDS Hooks integration',
        },
        {
          label: 'MyChart Patient Access API',
          value:
            'Patient-authorized SMART on FHIR access for patient-reported outcomes, consent capture, and engagement metrics used in HEDIS numerator supplemental data',
        },
        {
          label: 'Care Everywhere (HIE)',
          value:
            'Cross-organization FHIR document exchange via Epic\'s Care Everywhere network for longitudinal patient records spanning multiple Epic installations',
        },
      ],
    },
  ],

  fhirResources: [
    { name: 'Patient', description: 'Demographics, identifiers, MRN, and contact information' },
    { name: 'Encounter', description: 'Office visits, hospital admissions, and telehealth interactions' },
    { name: 'Condition', description: 'Active problems, diagnoses, and chronic condition history (ICD-10)' },
    { name: 'Observation', description: 'Lab results, vitals, social determinants of health, and assessments' },
    { name: 'Procedure', description: 'Surgical procedures, preventive screenings, and clinical interventions' },
    { name: 'MedicationRequest', description: 'Outpatient prescriptions and inpatient medication orders' },
    { name: 'Immunization', description: 'Vaccination records including COVID-19 and childhood immunization schedules' },
    { name: 'DiagnosticReport', description: 'Laboratory panels, pathology, and radiology report results' },
    { name: 'AllergyIntolerance', description: 'Allergy and adverse drug reaction records' },
    { name: 'DocumentReference', description: 'Clinical notes, C-CDA documents, and scanned records' },
    { name: 'Coverage', description: 'Insurance and payer information for HEDIS member attribution' },
    { name: 'Goal', description: 'Care plan goals for chronic disease management and preventive care tracking' },
  ],

  deploymentModels: [
    {
      name: 'On-Premises',
      description:
        'Deploy HDIM on your existing on-premises infrastructure alongside your Epic installation. Data never leaves your network, satisfying Epic\'s data governance requirements and your organization\'s data residency policies. Supports RHEL 7/8 and Ubuntu 20.04+. Recommended for health systems with Epic on-prem hosting agreements.',
      link: '/docs/deployment/RHEL7-DEPLOYMENT-GUIDE.md',
    },
    {
      name: 'Docker Compose',
      description:
        'Single-command deployment with Docker Compose using the Epic profile. Ideal for Epic sandbox integration testing, pilot programs, and smaller ambulatory deployments. Includes all 51+ HDIM services pre-configured with Epic RS384 JWT authentication and App Orchard-compatible network settings.',
    },
    {
      name: 'Kubernetes',
      description:
        'Production-grade orchestration with auto-scaling, rolling updates, and high availability. Recommended for large health system deployments with Epic serving 100K+ patients. Supports Epic\'s Cloud hosting environments (Microsoft Azure, Amazon Web Services) where Epic\'s cloud-hosted customers operate.',
    },
  ],
};
