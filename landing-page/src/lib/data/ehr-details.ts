export interface EHRDetail {
  id: string;
  name: string;
  marketShare: string;
  fhirSupport: string;
  authComplexity: string;
  integrationTimeline: string;

  queryMethods: string[];
  supportedResources: string[];
  commonIntegrationPatterns: string[];
  implementationNotes: string;
}

export const EHR_DETAILS: Record<string, EHRDetail> = {
  epic: {
    id: 'epic',
    name: 'Epic Systems',
    marketShare: '36% US market share',
    fhirSupport: 'Full FHIR R4 support',
    authComplexity: 'HIGH - RS384 JWT signing required',
    integrationTimeline: '6-8 weeks',
    queryMethods: [
      'Direct FHIR REST queries (R4)',
      'Epic FHIR DSTU2 (legacy support)',
      'HL7 v2 interface (ETL)',
      'Bulk Export API (HL7 FHIR Bulk Data)',
    ],
    supportedResources: [
      'Patient',
      'Encounter',
      'Condition',
      'Observation',
      'Procedure',
      'MedicationRequest',
      'Immunization',
      'DiagnosticReport',
      'AllergyIntolerance',
      'DocumentReference',
    ],
    commonIntegrationPatterns: [
      'Real-time query pattern (recommended)',
      'Scheduled bulk import (nightly)',
      'Hybrid: Real-time with bulk fallback',
      'Direct EHR alerts via secure web service',
    ],
    implementationNotes:
      'Epic requires OAuth2 with RS384 JWT. Typical integration: establish service account, configure FHIR endpoint URL, set up JWT signing certificate, test with sample data, deploy to production.',
  },
  'oracle-health': {
    id: 'oracle-health',
    name: 'Oracle Health (Cerner)',
    marketShare: '27% US market share',
    fhirSupport: 'Full FHIR R4 support',
    authComplexity: 'MEDIUM - OAuth2 with standard JWT',
    integrationTimeline: '4-6 weeks',
    queryMethods: [
      'Direct FHIR REST queries (R4)',
      'CDS Connect (clinical workflow)',
      'HL7 v2 HL7 interface',
      'Bulk Export API',
    ],
    supportedResources: [
      'Patient',
      'Encounter',
      'Condition',
      'Observation',
      'Procedure',
      'MedicationRequest',
      'MedicationStatement',
      'Immunization',
      'DiagnosticReport',
      'AllergyIntolerance',
    ],
    commonIntegrationPatterns: [
      'Real-time query pattern with caching',
      'Scheduled polling (hourly)',
      'Webhook notifications (preferred)',
      'CDS Hooks for clinical alerts',
    ],
    implementationNotes:
      'Cerner OAuth2 is more standard than Epic. Setup: Register app in Cerner code console, configure FHIR endpoint, implement OAuth flow, handle token refresh, deploy.',
  },
  athena: {
    id: 'athena',
    name: 'Athena Health',
    marketShare: '8% US market share',
    fhirSupport: 'Full FHIR R4 support',
    authComplexity: 'MEDIUM - OAuth2',
    integrationTimeline: '3-5 weeks',
    queryMethods: [
      'Direct FHIR REST queries (R4)',
      'Athena API (proprietary)',
      'Bulk Export (SFTP)',
    ],
    supportedResources: [
      'Patient',
      'Encounter',
      'Condition',
      'Observation',
      'Procedure',
      'MedicationRequest',
      'Immunization',
      'DiagnosticReport',
    ],
    commonIntegrationPatterns: [
      'Real-time FHIR queries',
      'Daily bulk export via SFTP',
      'Hybrid: Real-time + daily refresh',
    ],
    implementationNotes:
      'Athena has simpler OAuth2 flow. Setup: Create API application, get credentials, implement token exchange, configure FHIR endpoint URL, test integration.',
  },
  intersystems: {
    id: 'intersystems',
    name: 'InterSystems HealthShare / IRIS',
    marketShare: '70%+ US HIEs (dominant in health information exchanges)',
    fhirSupport: 'Full FHIR R4 via IRIS for Health',
    authComplexity: 'MEDIUM - OAuth2 or API key',
    integrationTimeline: '4-8 weeks',
    queryMethods: [
      'FHIR REST API (R4 via IRIS for Health)',
      'HL7 FHIR Bulk Data Access',
      'CDA/C-CDA document exchange',
      'ADT feed ingestion (real-time)',
      'HealthShare Health Connect routing',
    ],
    supportedResources: [
      'Patient',
      'Encounter',
      'Condition',
      'Observation',
      'Procedure',
      'MedicationRequest',
      'Immunization',
      'DiagnosticReport',
      'AllergyIntolerance',
      'DocumentReference',
      'Coverage',

    ],
    commonIntegrationPatterns: [
      'Real-time FHIR queries via IRIS for Health',
      'Bulk data export (NDJSON) for population health',
      'ADT event-driven ingestion (admission/discharge/transfer)',
      'CDA document import with FHIR conversion',
      'Hybrid: Real-time queries + scheduled bulk refresh',
    ],
    implementationNotes:
      'InterSystems HealthShare powers 70%+ of US HIEs. HDIM connects via FHIR R4 endpoint on IRIS for Health, architected for HIE-scale workloads of 16M+ patients. Setup: configure HealthShare FHIR endpoint URL, establish OAuth2 or API key auth, map FHIR resources to HDIM patient model, configure real-time + bulk ingestion, deploy with SPRING_PROFILES_ACTIVE=production and tune connection pooling, Kafka partitions, and batch sizes for HIE-scale volumes.',
  },
  meditech: {
    id: 'meditech',
    name: 'Meditech',
    marketShare: '25% US community hospitals',
    fhirSupport: 'FHIR R4 via Expanse platform',
    authComplexity: 'MEDIUM - OAuth2',
    integrationTimeline: '4-6 weeks',
    queryMethods: [
      'FHIR REST API (R4 via Expanse)',
      'HL7 v2 ADT feeds',
      'CDA/C-CDA document exchange',
      'Bulk Export (NDJSON)',
    ],
    supportedResources: [
      'Patient',
      'Encounter',
      'Condition',
      'Observation',
      'Procedure',
      'MedicationRequest',
      'Immunization',
      'DiagnosticReport',
      'AllergyIntolerance',
      'DocumentReference',
    ],
    commonIntegrationPatterns: [
      'Real-time FHIR queries via Expanse',
      'ADT event-driven ingestion',
      'CDA document import with FHIR conversion',
      'Scheduled bulk export for population health',
    ],
    implementationNotes:
      'Meditech Expanse provides FHIR R4 APIs for community hospitals. Setup: configure Expanse FHIR endpoint, establish OAuth2 auth, map resources, configure ingestion mode, deploy with community-hospital profile.',
  },
  allscripts: {
    id: 'allscripts',
    name: 'Allscripts / Veradigm',
    marketShare: 'Ambulatory + acute care',
    fhirSupport: 'FHIR R4 support',
    authComplexity: 'MEDIUM - OAuth2',
    integrationTimeline: '4-6 weeks',
    queryMethods: [
      'FHIR REST API (R4)',
      'Allscripts Unity API',
      'HL7 v2 interface',
      'CDA/C-CDA document exchange',
    ],
    supportedResources: [
      'Patient',
      'Encounter',
      'Condition',
      'Observation',
      'Procedure',
      'MedicationRequest',
      'Immunization',
      'DiagnosticReport',
      'AllergyIntolerance',
      'DocumentReference',
    ],
    commonIntegrationPatterns: [
      'Unified FHIR interface for Sunrise + TouchWorks',
      'Real-time FHIR queries',
      'Scheduled bulk import',
      'CDA document exchange',
    ],
    implementationNotes:
      'Allscripts supports both Sunrise Clinical Manager (inpatient) and TouchWorks (ambulatory) via FHIR R4. Setup: register in Allscripts Developer Program, configure OAuth2, set up FHIR endpoint, deploy.',
  },
  eclinicalworks: {
    id: 'eclinicalworks',
    name: 'eClinicalWorks',
    marketShare: 'Large ambulatory market',
    fhirSupport: 'FHIR R4 support',
    authComplexity: 'MEDIUM - OAuth2',
    integrationTimeline: '3-5 weeks',
    queryMethods: [
      'FHIR REST API (R4)',
      'eCW V11+ API',
      'HL7 v2 interface',
      'Direct Messaging',
    ],
    supportedResources: [
      'Patient',
      'Encounter',
      'Condition',
      'Observation',
      'Procedure',
      'MedicationRequest',
      'Immunization',
      'DiagnosticReport',
      'AllergyIntolerance',
      'Coverage',
    ],
    commonIntegrationPatterns: [
      'Real-time FHIR R4 queries',
      'Cloud-to-cloud API integration',
      'HL7 v2 for legacy workflows',
      'Bulk data export for population health',
    ],
    implementationNotes:
      'eClinicalWorks is cloud-based with FHIR R4 APIs. Setup: register in eCW Developer Program, configure OAuth2, set FHIR endpoint URL, configure resource mappings, deploy.',
  },
  nextgen: {
    id: 'nextgen',
    name: 'NextGen Healthcare',
    marketShare: 'Ambulatory + specialty practices',
    fhirSupport: 'FHIR R4 support',
    authComplexity: 'MEDIUM - OAuth2',
    integrationTimeline: '3-5 weeks',
    queryMethods: [
      'FHIR REST API (R4)',
      'Mirth Connect integration engine',
      'HL7 v2 interface',
      'CCDA document exchange',
      'Direct Messaging',
    ],
    supportedResources: [
      'Patient',
      'Encounter',
      'Condition',
      'Observation',
      'Procedure',
      'MedicationRequest',
      'Immunization',
      'DiagnosticReport',
      'AllergyIntolerance',
      'DocumentReference',
    ],
    commonIntegrationPatterns: [
      'Direct FHIR R4 REST queries',
      'Mirth Connect channel routing',
      'Specialty-specific quality measure workflows',
      'Ambulatory practice network integration',
    ],
    implementationNotes:
      'NextGen supports FHIR R4 with optional Mirth Connect for advanced routing. Setup: configure NextGen FHIR endpoint, set up OAuth2, optionally configure Mirth Connect, deploy.',
  },
  fhir: {
    id: 'fhir',
    name: 'Generic FHIR R4 Server',
    marketShare: '25% (Allscripts, NextGen, others)',
    fhirSupport: 'Native FHIR R4',
    authComplexity: 'LOW - Standard OAuth2 or basic auth',
    integrationTimeline: '1-3 weeks',
    queryMethods: [
      'Standard FHIR REST (GET/POST)',
      'FHIR Search API',
      'Bulk Export (NDJSON)',
      'Custom API if FHIR not available',
    ],
    supportedResources: [
      'Patient',
      'Encounter',
      'Condition',
      'Observation',
      'Procedure',
      'MedicationRequest',
      'Immunization',
      'DiagnosticReport',
      'Any FHIR R4 resource',
    ],
    commonIntegrationPatterns: [
      'Direct FHIR R4 REST queries',
      'Token-based authentication',
      'Patient-centric queries',
      'Condition/Observation bulk retrieval',
    ],
    implementationNotes:
      'Generic FHIR servers follow standard. Setup: Get API credentials, authenticate with bearer token, query FHIR endpoints, handle pagination, deploy.',
  },
}

export function getEHRDetail(ehrId: string): EHRDetail | null {
  return EHR_DETAILS[ehrId] || null;
}

export function getAllEHRs(): EHRDetail[] {
  return Object.values(EHR_DETAILS);
}
