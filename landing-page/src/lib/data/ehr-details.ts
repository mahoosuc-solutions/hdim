export interface EHRDetail {
  id: string;
  name: string;
  marketShare: string;
  fhirSupport: string;
  authComplexity: string;
  integrationTimeline: string;
  typicalACV: string;
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
    typicalACV: '$80-150K',
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
  cerner: {
    id: 'cerner',
    name: 'Cerner Corporation',
    marketShare: '27% US market share',
    fhirSupport: 'Full FHIR R4 support',
    authComplexity: 'MEDIUM - OAuth2 with standard JWT',
    integrationTimeline: '4-6 weeks',
    typicalACV: '$50-100K',
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
    typicalACV: '$30-60K',
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
  fhir: {
    id: 'fhir',
    name: 'Generic FHIR R4 Server',
    marketShare: '25% (Allscripts, NextGen, others)',
    fhirSupport: 'Native FHIR R4',
    authComplexity: 'LOW - Standard OAuth2 or basic auth',
    integrationTimeline: '1-3 weeks',
    typicalACV: '$20-50K',
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
