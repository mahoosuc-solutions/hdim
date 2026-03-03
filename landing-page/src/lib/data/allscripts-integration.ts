import type { IntegrationPageData } from './intersystems-integration';

export const ALLSCRIPTS_INTEGRATION: IntegrationPageData = {
  ehrName: 'Allscripts / Veradigm',
  tagline: 'Quality Measurement Across Ambulatory and Acute Care',
  heroDescription:
    'The hardest quality measurement problem in a mixed Allscripts environment is that Sunrise and TouchWorks operate as separate data silos. A diabetic patient seen in your hospital and your affiliated clinic appears in two systems with no unified quality view. HDIM deploys on your infrastructure, fronts both FHIR endpoints, and produces a single quality record that crosses the inpatient-ambulatory boundary.',

  sections: [
    {
      id: 'overview',
      title: 'Overview',
      type: 'text',
      content:
        'Dual-platform Allscripts environments — Sunrise inpatient alongside TouchWorks ambulatory — are among the most difficult configurations for clinical quality measurement. Both platforms expose FHIR R4 endpoints, but they use different patient identifier schemes, different encounter structures, and different code system conventions. Quality tools that query only one platform produce systematically wrong measure rates: patients who completed diabetic eye exams at the hospital are still flagged as gaps in the ambulatory quality report.\n\nHDIM is deployed on the customer\'s own infrastructure — on-premises servers or a private cloud VPC — and sits between both FHIR endpoints and the clinical workflows consuming quality data. It ingests from Sunrise and TouchWorks simultaneously, resolves patient identity across both using EMPI identifiers or probabilistic matching, and presents a single normalized FHIR resource stream to the CQL evaluation layer. The result is a quality measurement pipeline that sees the same patient record a care coordinator would if they could log into both systems at once — which today they cannot do efficiently at population scale.',
    },
    {
      id: 'architecture',
      title: 'Architecture',
      type: 'diagram',
      content:
        'HDIM connects to both Sunrise Clinical Manager and TouchWorks EHR through the Allscripts FHIR Server, providing a unified ingestion path into the HDIM quality measurement pipeline.',
      codeSnippet: `┌────────────────────────────────────────────────────────────────────┐
│                     Allscripts / Veradigm                          │
│                                                                    │
│  ┌──────────────────────┐     ┌──────────────────────────────────┐ │
│  │ Sunrise Clinical     │     │ TouchWorks EHR                   │ │
│  │ Manager (Inpatient)  │     │ (Ambulatory)                     │ │
│  └──────────┬───────────┘     └──────────────┬───────────────────┘ │
│             │                                │                     │
│             └──────────────┬─────────────────┘                     │
│                            ▼                                       │
│              ┌─────────────────────────┐                           │
│              │  Allscripts FHIR Server │                           │
│              │  (FHIR R4 REST API)     │                           │
│              └────────────┬────────────┘                           │
└───────────────────────────┼────────────────────────────────────────┘
                            │ FHIR R4 REST API
                            ▼
┌───────────────────────────────────────────────────────────────────┐
│                           HDIM Platform                           │
│                                                                   │
│  ┌──────────┐   ┌──────────────┐   ┌──────────┐   ┌───────────┐  │
│  │ FHIR     │ → │ Patient      │ → │ CQL      │ → │ Care Gap  │  │
│  │ Service  │   │ Service      │   │ Engine   │   │ Service   │  │
│  │ (8085)   │   │ (8084)       │   │ (8081)   │   │ (8086)    │  │
│  └──────────┘   └──────────────┘   └──────────┘   └───────────┘  │
│       ↑                                                  │        │
│       │              ┌──────────────┐                    ▼        │
│       └──────────────│ Quality      │←── Quality Reports &       │
│                      │ Measure Svc  │    Care Gap Closures        │
│                      │ (8087)       │                             │
│                      └──────────────┘                             │
└───────────────────────────────────────────────────────────────────┘`,
      codeLanguage: 'text',
    },
    {
      id: 'integration-steps',
      title: 'Integration Guide',
      type: 'steps',
      content:
        'Follow these six steps to connect HDIM with your Allscripts Sunrise or TouchWorks deployment.',
      items: [
        'Register in the Allscripts Developer Program — Create an account at developer.allscripts.com and register your HDIM application to obtain OAuth2 client credentials. For Veradigm data network access, contact Veradigm directly to establish a data sharing agreement and receive network API credentials.',
        'Configure OAuth2 authentication — Set up OAuth2 client credentials flow using the client ID and secret issued by Allscripts. HDIM supports both the Allscripts OAuth2 authorization server and static bearer token authentication for on-premises Sunrise deployments where OAuth2 infrastructure may not be available.',
        'Set up the FHIR R4 endpoint — Configure HDIM\'s FHIR Service to point to your Allscripts FHIR R4 base URL. For Sunrise, this is typically https://your-sunrise-host/fhir/r4. For TouchWorks, the endpoint format follows https://your-touchworks-host/Unity/FHIR/R4. Verify connectivity by running a test Patient read against a known patient identifier.',
        'Map FHIR resources from Sunrise or TouchWorks — Configure HDIM\'s resource mapping to account for Allscripts-specific FHIR extensions and identifier systems. Sunrise uses NPI-based practitioner identifiers and facility OIDs; TouchWorks uses practice-level tenant identifiers. HDIM\'s normalization layer handles both formats transparently.',
        'Deploy HDIM on your infrastructure — Set spring.profiles.active=production in your application.properties and configure FHIR_SERVICE_URL to point at your Sunrise or TouchWorks FHIR endpoint. For organizations running both platforms, deploy a separate HDIM instance for each and aggregate results at the reporting layer. Adjust FHIR_CONNECTION_POOL_SIZE and PATIENT_BATCH_SIZE based on your patient population size.',
        'Verify end-to-end integration — Execute the HDIM integration verification suite to confirm FHIR connectivity, patient demographic resolution, CQL measure evaluation, and care gap detection are functioning correctly for both Sunrise inpatient and TouchWorks ambulatory populations.',
      ],
    },
    {
      id: 'configuration',
      title: 'Configuration',
      type: 'code',
      content:
        'HDIM deploys on your infrastructure. Configure these properties in your Spring Boot environment to connect to your Allscripts FHIR endpoints.',
      codeSnippet: `# Configuration reference — actual property names may vary by release

# Active profile — controls logging, cache TTLs, and connection pool sizing
spring.profiles.active=production

# Allscripts FHIR endpoint (Sunrise or TouchWorks — on your network)
FHIR_SERVICE_URL=https://sunrise.your-health-system.org/fhir/r4
FHIR_AUTH_TYPE=oauth2
FHIR_CLIENT_ID=hdim-allscripts-client
FHIR_CLIENT_SECRET=<your-client-secret>
FHIR_TOKEN_URL=https://sunrise.your-health-system.org/oauth2/token

# Connection tuning
FHIR_CONNECTION_POOL_SIZE=20
PATIENT_BATCH_SIZE=500`,
      codeLanguage: 'bash',
    },
    {
      id: 'dual-platform',
      title: 'Dual-Platform Support: Sunrise and TouchWorks',
      type: 'text',
      content:
        'Many health systems running Allscripts operate both Sunrise Clinical Manager for their hospital and inpatient facilities and TouchWorks EHR across their affiliated physician practices and ambulatory clinics. These platforms expose distinct FHIR endpoint structures, use different patient identifier schemes, and represent clinical data at different levels of granularity — acute encounter-level detail in Sunrise vs. longitudinal outpatient visit history in TouchWorks.\n\nToday, HDIM connects to one FHIR endpoint at a time — either Sunrise or TouchWorks — and normalizes incoming resources to a common internal representation. For organizations running both platforms, the recommended deployment pattern is a separate HDIM instance per platform, with quality results aggregated at the reporting layer. Multi-source ingestion from both endpoints simultaneously is on the roadmap.\n\nFor HEDIS quality measures that require both acute and ambulatory data — such as Comprehensive Diabetes Care (CDC), Controlling High Blood Pressure (CBP), and Colorectal Cancer Screening (COL) — the dual-instance approach ensures each platform\'s data is evaluated independently, and aggregate reports combine results to identify true care gaps versus false positives caused by incomplete single-platform data.',
    },
    {
      id: 'data-exchange',
      title: 'Data Exchange Protocols',
      type: 'table',
      content: 'HDIM supports multiple data exchange protocols with Allscripts platforms.',
      tableData: [
        {
          label: 'FHIR R4 REST',
          value:
            'Primary — real-time patient queries and resource retrieval via the Allscripts FHIR Server for both Sunrise and TouchWorks',
        },
        {
          label: 'Allscripts Unity API',
          value:
            'Fallback for clinical data not yet exposed on FHIR endpoints; supports GetPatient, GetEncounters, GetMedications, and GetDocuments Unity API calls',
        },
        {
          label: 'HL7 v2',
          value:
            'Legacy ADT and ORU message ingestion for real-time event-driven care gap triggers; converted to FHIR R4 resources before entering the HDIM evaluation pipeline',
        },
        {
          label: 'CDA/C-CDA',
          value:
            'Clinical document import with automatic FHIR R4 conversion; supports Allscripts-generated Transition of Care and Referral Note documents',
        },
      ],
    },
  ],

  fhirResources: [
    { name: 'Patient', description: 'Demographics, identifiers, contact information' },
    { name: 'Encounter', description: 'Inpatient admissions (Sunrise) and ambulatory visits (TouchWorks)' },
    { name: 'Condition', description: 'Diagnoses, problems, and health concerns (ICD-10)' },
    { name: 'Observation', description: 'Lab results, vitals, social history, and clinical assessments' },
    { name: 'Procedure', description: 'Surgical procedures, interventions, screenings, and preventive services' },
    { name: 'MedicationRequest', description: 'Prescriptions and medication orders from both platforms' },
    { name: 'Immunization', description: 'Vaccination records and immunization history' },
    { name: 'DiagnosticReport', description: 'Pathology, radiology, and laboratory reports' },
    { name: 'AllergyIntolerance', description: 'Allergy and adverse reaction records' },
    { name: 'DocumentReference', description: 'Clinical documents including CDA, C-CDA, and discharge summaries' },
  ],

  deploymentModels: [
    {
      name: 'On-Premises',
      description:
        'Deploy HDIM on your existing on-premises infrastructure alongside Allscripts Sunrise or TouchWorks. Data never leaves your network. Recommended for health systems with strict data residency, compliance, or network isolation requirements. Supports RHEL 7/8 and Ubuntu 20.04+.',
    },
    {
      name: 'Docker Compose',
      description:
        'Single-command deployment with Docker Compose. Ideal for pilot programs, development environments, and ambulatory-only TouchWorks implementations. Includes all 51+ HDIM services pre-configured with the allscripts connection profile.',
    },
    {
      name: 'Kubernetes',
      description:
        'Production-grade orchestration with auto-scaling, rolling updates, and high availability. Recommended for large health systems running both Sunrise and TouchWorks across multiple facilities, where concurrent inpatient and ambulatory quality measurement workloads require independent scaling.',
    },
  ],
};
