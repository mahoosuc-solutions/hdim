import type { IntegrationPageData } from './intersystems-integration';

export const NEXTGEN_INTEGRATION: IntegrationPageData = {
  ehrName: 'NextGen Healthcare',
  tagline: 'Quality Measurement for Specialty and Ambulatory Practices',
  heroDescription:
    'Cardiology practices need quality measures for blood pressure control and anticoagulation management. Behavioral health practices need follow-up after hospitalization rates. Pediatric groups need well-child visit compliance. HEDIS covers 52 measures — but specialty practices are often accountable for a different set, and sometimes measures that don\'t exist in any standard library. HDIM deploys on your infrastructure, fronts your NextGen FHIR endpoint, and evaluates both standard HEDIS and custom CQL measures written for your specific contracts.',

  sections: [
    {
      id: 'overview',
      title: 'Overview',
      type: 'text',
      content:
        'NextGen is heavily used in specialty practices — cardiology, behavioral health, orthopedics, gastroenterology, pediatrics — where the quality measurement problem is fundamentally different from primary care. Primary care practices can run the full NCQA HEDIS measure set and call it done. Specialty practices operate under payer-specific contracts, MIPS Alternative Payment Models, and ACO quality programs that define their own numerators and denominators. The standard 52 HEDIS measures may cover three or four of the metrics a cardiology group is actually accountable for.\n\nHDIM is deployed on the practice\'s own infrastructure and sits in front of the NextGen FHIR endpoint. The CQL Engine supports both the NCQA-published HEDIS measure library and custom CQL logic authored by the practice or their quality team. A cardiology group can deploy a CQL measure for door-to-balloon time alongside standard HEDIS measures for Controlling High Blood Pressure — both evaluated against the same FHIR data stream, with results surfaced in the same care gap workflow. Providers see actionable gaps for the measures that matter to their contracts, not a generic HEDIS dashboard built for a primary care population.',
    },
    {
      id: 'architecture',
      title: 'Architecture',
      type: 'diagram',
      content:
        'HDIM connects to NextGen Healthcare through the NextGen FHIR R4 endpoint, optionally routing through a Mirth Connect channel for HL7 v2 or CCDA transformation before entering the HDIM microservice pipeline.',
      codeSnippet: `┌──────────────────────────────────────────────────────────────────┐
│                    NextGen Healthcare                            │
│  ┌─────────────────────┐        ┌────────────────────────────┐  │
│  │ NextGen Office /    │        │ Mirth Connect              │  │
│  │ NextGen Enterprise  │──────→ │ (Integration Engine)       │  │
│  │ (EHR)               │        │ HL7 v2 / CCDA / FHIR       │  │
│  └─────────────────────┘        └──────────────┬─────────────┘  │
│           │                                    │                │
│           │ FHIR R4 REST API (direct)          │ FHIR R4 (out) │
│           └───────────────┬────────────────────┘                │
│                           │                                     │
│               ┌───────────▼──────────┐                          │
│               │ NextGen FHIR Server  │                          │
│               │ (R4 Endpoint)        │                          │
│               └───────────┬──────────┘                          │
└───────────────────────────┼──────────────────────────────────────┘
                            │ FHIR R4 REST API
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
        'Follow these six steps to connect HDIM with your NextGen Healthcare deployment.',
      items: [
        'Configure the NextGen FHIR endpoint URL — Locate your NextGen FHIR R4 base URL in the NextGen Administrator console under Integration > FHIR Configuration. For NextGen Office it typically follows the pattern https://<practice>.nextgen.com/fhir/r4; for NextGen Enterprise it is the URL configured during your FHIR module installation. Set FHIR_SERVER_URL in your HDIM .env file to this value.',
        'Set up OAuth2 authentication — NextGen uses OAuth2 SMART on FHIR for third-party app authorization. Register HDIM as an application in the NextGen Developer Portal, obtain a client ID and client secret, and configure FHIR_AUTH_TYPE=oauth2, FHIR_CLIENT_ID, FHIR_CLIENT_SECRET, and FHIR_TOKEN_URL in your HDIM environment. Scopes required: patient/*.read, launch/patient, openid.',
        'Configure a Mirth Connect channel (optional) — If your NextGen deployment uses Mirth Connect for HL7 v2 or CCDA message routing, deploy the HDIM Mirth Connect channel bundle included in the installer package. The channel transforms inbound HL7 v2 ADT/ORU messages and C-CDA documents into FHIR R4 resources and forwards them to HDIM\'s FHIR Service ingestion endpoint. Skip this step if you are using the NextGen FHIR R4 API directly.',
        'Map FHIR resources to the HDIM patient model — Review the FHIR resource mapping configuration in hdim-fhir-mapping.yml. NextGen uses standard FHIR R4 resource profiles with US Core extensions. Confirm that your NextGen FHIR server\'s CapabilityStatement includes the resource types required for your quality measures (Patient, Encounter, Condition, Observation, Procedure, MedicationRequest, Immunization, DiagnosticReport, AllergyIntolerance, DocumentReference).',
        'Deploy HDIM on your infrastructure — Set spring.profiles.active=production and configure FHIR_SERVICE_URL to your NextGen FHIR R4 endpoint. For practice-scale deployments, set FHIR_CONNECTION_POOL_SIZE=20 and PATIENT_BATCH_SIZE=1000. For large multi-specialty groups spanning hundreds of thousands of patients, increase KAFKA_PARTITIONS and deploy HDIM\'s CQL Engine pods with horizontal autoscaling enabled.',
        'Verify end-to-end connectivity — Confirm FHIR endpoint reachability via the health check endpoint, verify OAuth2 token acquisition, test FHIR resource retrieval (Patient, Encounter, Observation), and validate CQL measure evaluation and care gap detection are functioning end-to-end against your NextGen environment.',
      ],
    },
    {
      id: 'configuration',
      title: 'Configuration',
      type: 'code',
      content:
        'HDIM deploys on your infrastructure. Configure these Spring Boot properties to connect to your NextGen FHIR endpoint.',
      codeSnippet: `# Configuration reference — actual property names may vary by release

# Active profile — controls logging, cache TTLs, and connection pool sizing
spring.profiles.active=production

# NextGen FHIR R4 endpoint (on your network or NextGen-hosted)
FHIR_SERVICE_URL=https://your-practice.nextgen.com/fhir/r4
FHIR_AUTH_TYPE=oauth2

# OAuth2 / SMART on FHIR
FHIR_CLIENT_ID=<your-hdim-client-id>
FHIR_CLIENT_SECRET=<your-client-secret>
FHIR_TOKEN_URL=https://your-practice.nextgen.com/oauth2/token
FHIR_SCOPES=patient/*.read launch/patient openid

# Mirth Connect (optional — omit if using the FHIR API directly)
MIRTH_CONNECT_ENABLED=true
MIRTH_CONNECT_HOST=<your-mirth-host>
MIRTH_CONNECT_PORT=6661
MIRTH_CONNECT_CHANNEL_ID=<hdim-inbound-channel-id>

# Connection tuning for practice-scale deployments
FHIR_CONNECTION_POOL_SIZE=20
KAFKA_PARTITIONS=8
PATIENT_BATCH_SIZE=1000`,
      codeLanguage: 'bash',
    },
    {
      id: 'specialty-practice',
      title: 'Specialty Practice Quality Measurement',
      type: 'text',
      content:
        'NextGen Healthcare is the EHR of choice for specialty practices — including cardiology, orthopedics, dermatology, gastroenterology, and behavioral health — where quality measurement requirements differ significantly from primary care.\n\nHDIM addresses specialty-specific quality measurement with NextGen in three key ways:\n\n• Specialty HEDIS Measures — HDIM supports the full NCQA HEDIS measure set, including measures relevant to specialty care such as Controlling High Blood Pressure (CBP), Diabetes Care (HbA1c, eye exams, nephropathy), Follow-Up After Hospitalization for Mental Illness (FUH), and Antidepressant Medication Management (AMM). Measure libraries are pre-packaged for common NextGen specialty configurations.\n\n• CQL Measure Customization — For practices participating in MIPS, APM, or payer-specific quality programs, HDIM\'s CQL Engine allows custom measure logic to be authored and deployed without modifying the core platform. NextGen\'s FHIR R4 resource profiles map cleanly to standard CQL data requirements.\n\n• Mirth Connect Specialty Workflows — Specialty practices commonly generate HL7 v2 ORU messages (lab results) and structured observation data that may not yet flow through the NextGen FHIR API. HDIM\'s Mirth Connect channel bundle captures these messages, transforms them to FHIR R4 Observation resources, and incorporates them into quality measure evaluation — ensuring complete data for numerator and denominator population identification.\n\n• Multi-Location Practice Support — NextGen Enterprise deployments frequently span multiple clinic locations with separate organizational units. HDIM\'s multi-tenant architecture maps NextGen practice groups to HDIM tenants, enabling location-level quality reporting and aggregate population views within a single HDIM deployment.',
    },
    {
      id: 'data-exchange',
      title: 'Data Exchange Protocols',
      type: 'table',
      content: 'HDIM supports multiple data exchange protocols with NextGen Healthcare.',
      tableData: [
        {
          label: 'FHIR R4 REST',
          value:
            'Primary — real-time patient queries and resource retrieval via the NextGen FHIR R4 endpoint (SMART on FHIR OAuth2)',
        },
        {
          label: 'Mirth Connect',
          value:
            'Optional middleware — transforms HL7 v2 ADT/ORU messages and C-CDA documents to FHIR R4 before ingestion into HDIM; recommended for practices with existing Mirth Connect channels',
        },
        {
          label: 'HL7 v2',
          value:
            'Legacy message ingestion (ADT A01/A03/A08, ORU R01) via Mirth Connect channel; supports lab results and admission/discharge events for care gap triggers',
        },
        {
          label: 'CCDA',
          value:
            'Clinical document import (Continuity of Care Document, Referral Note, Discharge Summary) with automatic FHIR R4 conversion via Mirth Connect or direct HDIM CDA parser',
        },
      ],
    },
  ],

  fhirResources: [
    { name: 'Patient', description: 'Demographics, identifiers, contact information' },
    { name: 'Encounter', description: 'Ambulatory visits, specialty consultations, and clinical interactions' },
    { name: 'Condition', description: 'Diagnoses, problems, and health concerns (ICD-10)' },
    { name: 'Observation', description: 'Lab results, vitals, specialty assessments, and social history' },
    { name: 'Procedure', description: 'Specialty procedures, interventions, and preventive screenings' },
    { name: 'MedicationRequest', description: 'Prescriptions and medication orders' },
    { name: 'Immunization', description: 'Vaccination records and immunization history' },
    { name: 'DiagnosticReport', description: 'Pathology, radiology, and laboratory reports' },
    { name: 'AllergyIntolerance', description: 'Allergy and adverse reaction records' },
    { name: 'DocumentReference', description: 'Clinical documents (CDA, C-CDA, PDF, Direct messages)' },
  ],

  deploymentModels: [
    {
      name: 'On-Premises',
      description:
        'Deploy HDIM on your own infrastructure alongside NextGen Enterprise. Data never leaves your network. Recommended for practices with strict data residency requirements or existing Mirth Connect infrastructure. Supports RHEL 7/8, Ubuntu 20.04+, and Windows Server 2019+.',
    },
    {
      name: 'Docker Compose',
      description:
        'Single-command deployment with Docker Compose using the ambulatory profile. Ideal for pilot programs, NextGen Office integrations, and smaller specialty practices. Includes all HDIM services pre-configured with NextGen-optimized defaults.',
    },
    {
      name: 'Kubernetes',
      description:
        'Production-grade orchestration with auto-scaling, rolling updates, and high availability. Recommended for large multi-specialty group practices or organizations running NextGen Enterprise across multiple clinic locations.',
    },
  ],
};
