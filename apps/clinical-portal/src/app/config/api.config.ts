/**
 * API Configuration for Backend Services
 *
 * Supports two deployment modes:
 * 1. Direct Mode: Frontend connects directly to backend services (development)
 * 2. Gateway Mode: Frontend connects through Kong API Gateway (production/HIE)
 *
 * Set USE_API_GATEWAY=true to enable Kong routing
 */

// Environment detection
const USE_API_GATEWAY = false; // Direct mode with Angular proxy for CORS

// API Gateway configuration (our Gateway service, not Kong)
const API_GATEWAY_URL = 'http://localhost:8080'; // Gateway service URL

export const API_CONFIG = {
  // API Gateway Settings
  USE_API_GATEWAY,
  API_GATEWAY_URL,

  // Base URLs - Proxied through Angular dev server (see proxy.conf.json)
  // In development: Angular dev server proxies these to backend services
  // In production: These should be absolute URLs or configured via environment
  CQL_ENGINE_URL: USE_API_GATEWAY
    ? `${API_GATEWAY_URL}/api/cql`
    : '/cql-engine',  // Proxied to localhost:8081

  QUALITY_MEASURE_URL: USE_API_GATEWAY
    ? `${API_GATEWAY_URL}/api/quality/quality-measure`
    : '/quality-measure',  // Proxied to localhost:8087

  FHIR_SERVER_URL: USE_API_GATEWAY
    ? `${API_GATEWAY_URL}/api/fhir`
    : '/fhir',  // Proxied to localhost:8085

  CARE_GAP_URL: USE_API_GATEWAY
    ? `${API_GATEWAY_URL}/api/care-gap`
    : '/care-gap',  // Proxied to localhost:8086

  PATIENT_URL: USE_API_GATEWAY
    ? `${API_GATEWAY_URL}/api/patient`
    : '/patient',  // Proxied to localhost:8084

  QRDA_EXPORT_URL: USE_API_GATEWAY
    ? `${API_GATEWAY_URL}/api/qrda`
    : '/qrda',  // Proxied to localhost:8104

  AI_ASSISTANT_URL: USE_API_GATEWAY
    ? `${API_GATEWAY_URL}/api/ai-assistant`
    : '/api/ai-assistant',  // Proxied or direct to AI service

  // Tenant Configuration - must match backend test data
  DEFAULT_TENANT_ID: 'DEMO001',  // Demo tenant for clinical portal

  // HTTP Settings
  TIMEOUT_MS: 30000, // 30 seconds
  RETRY_ATTEMPTS: 3,
  RETRY_DELAY_MS: 1000,

  // Logging
  ENABLE_LOGGING: true, // Set to false in production
};

/**
 * CQL Engine API Endpoints
 */
export const CQL_ENGINE_ENDPOINTS = {
  // Library Endpoints
  LIBRARIES: '/api/v1/cql/libraries',
  LIBRARIES_ACTIVE: '/api/v1/cql/libraries/active',
  LIBRARY_BY_ID: (id: string) => `/api/v1/cql/libraries/${id}`,
  LIBRARY_BY_NAME: (name: string, version: string) =>
    `/api/v1/cql/libraries/by-name/${name}/version/${version}`,
  LIBRARY_LATEST: (name: string) => `/api/v1/cql/libraries/by-name/${name}/latest`,
  LIBRARY_VERSIONS: (name: string) => `/api/v1/cql/libraries/by-name/${name}/versions`,
  LIBRARIES_BY_STATUS: (status: string) => `/api/v1/cql/libraries/by-status/${status}`,
  LIBRARIES_SEARCH: '/api/v1/cql/libraries/search',
  LIBRARIES_COUNT: '/api/v1/cql/libraries/count',
  LIBRARY_EXISTS: '/api/v1/cql/libraries/exists',

  // Evaluation Endpoints
  EVALUATIONS: '/api/v1/cql/evaluations',
  EVALUATIONS_BATCH: '/api/v1/cql/evaluations/batch',
  EVALUATIONS_BY_PATIENT: (patientId: string) =>
    `/api/v1/cql/evaluations/patient/${patientId}`,
  EVALUATIONS_BY_LIBRARY: (libraryId: string) =>
    `/api/v1/cql/evaluations/library/${libraryId}`,
  EVALUATE_SIMPLE: '/evaluate', // Simplified endpoint

  // HEDIS Measure Discovery Endpoints (Phase 1)
  HEDIS_MEASURES: '/evaluate/measures', // List all registered HEDIS measures
  HEDIS_MEASURE_BY_ID: (measureId: string) => `/evaluate/measures/${measureId}`,
  HEDIS_MEASURE_EXISTS: (measureId: string) => `/evaluate/measures/${measureId}/exists`,
};

/**
 * Quality Measure Service Endpoints
 * Note: Controller now uses /api/v1 (servlet context-path /quality-measure is already in base URL)
 */
export const QUALITY_MEASURE_ENDPOINTS = {
  // Local Measure Registry (service discovery)
  LOCAL_MEASURES: '/measures/local',  // GET - returns available measures with metadata

  // Calculation Endpoints
  CALCULATE: '/calculate',
  CALCULATE_LOCAL: '/calculate-local',  // POST - local Java calculation (bypasses CQL)
  RESULTS_BY_PATIENT: '/results',
  QUALITY_SCORE: '/score',
  PATIENT_REPORT: '/report/patient',
  POPULATION_REPORT: '/report/population',
  HEALTH: '/_health',

  // Saved Reports Endpoints
  SAVE_PATIENT_REPORT: '/report/patient/save',
  SAVE_POPULATION_REPORT: '/report/population/save',
  SAVED_REPORTS: '/reports',
  SAVED_REPORT_BY_ID: (reportId: string) => `/reports/${reportId}`,
  EXPORT_CSV: (reportId: string) => `/reports/${reportId}/export/csv`,
  EXPORT_EXCEL: (reportId: string) => `/reports/${reportId}/export/excel`,

  // Patient Health Overview Endpoints
  PATIENT_HEALTH_OVERVIEW: (patientId: string) => `/patient-health/overview/${patientId}`,
  PATIENT_HEALTH_SCORE: (patientId: string) => `/patient-health/health-score/${patientId}`,
  PATIENT_HEALTH_SCORE_HISTORY: (patientId: string) => `/patient-health/health-score/${patientId}/history`,
  MENTAL_HEALTH_ASSESSMENTS: '/patient-health/mental-health/assessments',
  MENTAL_HEALTH_ASSESSMENTS_BY_PATIENT: (patientId: string) => `/patient-health/mental-health/assessments/${patientId}`,
  MENTAL_HEALTH_TREND: (patientId: string) => `/patient-health/mental-health/assessments/${patientId}/trend`,
  CARE_GAPS_BY_PATIENT: (patientId: string) => `/patient-health/care-gaps/${patientId}`,
  ADDRESS_CARE_GAP: (gapId: string) => `/patient-health/care-gaps/${gapId}/address`,
  UPDATE_CARE_GAP_STATUS: (gapId: string) => `/patient-health/care-gaps/${gapId}/status`,
  CARE_GAP_METRICS: (patientId: string) => `/patient-health/care-gaps/${patientId}/metrics`,
  RISK_STRATIFICATION_CALCULATE: (patientId: string) => `/patient-health/risk-stratification/${patientId}/calculate`,
  RISK_STRATIFICATION_GET: (patientId: string) => `/patient-health/risk-stratification/${patientId}`,
  RISK_HISTORY: (patientId: string) => `/patient-health/risk/${patientId}/history`,

  // Feature 4.2: Hospitalization Predictions
  HOSPITALIZATION_PREDICTION: (patientId: string) => `/patient-health/predictions/${patientId}/hospitalization`,

  // Feature 5.2: Care Recommendations Engine
  CARE_RECOMMENDATIONS: (patientId: string) => `/patient-health/recommendations/${patientId}`,
  GENERATE_RECOMMENDATIONS: (patientId: string) => `/patient-health/recommendations/${patientId}/generate`,
  UPDATE_RECOMMENDATION_STATUS: (recommendationId: string) => `/patient-health/recommendations/${recommendationId}/status`,
  RECOMMENDATION_OUTCOMES: (patientId: string) => `/patient-health/recommendations/${patientId}/outcomes`,

  // Feature 5.3: SDOH Referral Management
  COMMUNITY_RESOURCES: '/patient-health/sdoh/community-resources',
  COMMUNITY_RESOURCES_SEARCH: '/patient-health/sdoh/community-resources/search',
  SDOH_REFERRALS: '/patient-health/sdoh/referrals',
  SDOH_REFERRAL_BY_ID: (referralId: string) => `/patient-health/sdoh/referrals/${referralId}`,
  SDOH_REFERRAL_SEND: (referralId: string) => `/patient-health/sdoh/referrals/${referralId}/send`,
  SDOH_REFERRAL_STATUS: (referralId: string) => `/patient-health/sdoh/referrals/${referralId}/status`,
  SDOH_REFERRAL_HISTORY: (patientId: string) => `/patient-health/sdoh/referrals/patient/${patientId}`,
  SDOH_REFERRAL_METRICS: (patientId: string) => `/patient-health/sdoh/referrals/patient/${patientId}/metrics`,

  // Batch Population Calculation Endpoints
  POPULATION_CALCULATE: '/population/calculate',
  POPULATION_JOBS: '/population/jobs',
  POPULATION_JOB_BY_ID: (jobId: string) => `/population/jobs/${jobId}`,
  POPULATION_JOB_CANCEL: (jobId: string) => `/population/jobs/${jobId}/cancel`,

  // Clinical Decision Support (CDS) Endpoints
  CDS_RULES: '/cds/rules',
  CDS_RULES_BY_CATEGORY: (category: string) => `/cds/rules/category/${category}`,
  CDS_RULE_BY_CODE: (ruleCode: string) => `/cds/rules/${ruleCode}`,
  CDS_RECOMMENDATIONS: (patientId: string) => `/cds/recommendations/${patientId}`,
  CDS_RECOMMENDATIONS_COUNT: (patientId: string) => `/cds/recommendations/${patientId}/count`,
  CDS_RECOMMENDATIONS_OVERDUE: (patientId: string) => `/cds/recommendations/${patientId}/overdue`,
  CDS_EVALUATE: '/cds/evaluate',
  CDS_ACKNOWLEDGE: '/cds/acknowledge',
};

/**
 * QRDA Export Service Endpoints
 */
export const QRDA_EXPORT_ENDPOINTS = {
  // QRDA Category I (Patient-level)
  GENERATE_CATEGORY_I: '/api/v1/qrda/category-i/generate',

  // QRDA Category III (Aggregate)
  GENERATE_CATEGORY_III: '/api/v1/qrda/category-iii/generate',

  // Job Management
  JOBS: '/api/v1/qrda/jobs',
  JOB_BY_ID: (jobId: string) => `/api/v1/qrda/jobs/${jobId}`,
  JOB_DOWNLOAD: (jobId: string) => `/api/v1/qrda/jobs/${jobId}/download`,
  JOB_CANCEL: (jobId: string) => `/api/v1/qrda/jobs/${jobId}/cancel`,
};

/**
 * FHIR Server Endpoints
 */
export const FHIR_ENDPOINTS = {
  PATIENT: '/Patient',
  PATIENT_BY_ID: (id: string) => `/Patient/${id}`,
  OBSERVATION: '/Observation',
  DIAGNOSTIC_REPORT: '/DiagnosticReport',
  CONDITION: '/Condition',
  PROCEDURE: '/Procedure',
  MEDICATION: '/MedicationRequest',
  MEDICATION_STATEMENT: '/MedicationStatement',
  QUESTIONNAIRE_RESPONSE: '/QuestionnaireResponse',
  SERVICE_REQUEST: '/ServiceRequest',
};

/**
 * HTTP Headers
 */
export const HTTP_HEADERS = {
  TENANT_ID: 'X-Tenant-ID',
  CONTENT_TYPE: 'Content-Type',
  AUTHORIZATION: 'Authorization',
};

/**
 * Build full URL for CQL Engine endpoint
 */
export function buildCqlEngineUrl(endpoint: string, params?: Record<string, string>): string {
  let url = `${API_CONFIG.CQL_ENGINE_URL}${endpoint}`;

  if (params) {
    const queryString = Object.entries(params)
      .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
      .join('&');
    url += `?${queryString}`;
  }

  return url;
}

/**
 * Build full URL for Quality Measure endpoint
 */
export function buildQualityMeasureUrl(endpoint: string, params?: Record<string, string>): string {
  let url = `${API_CONFIG.QUALITY_MEASURE_URL}${endpoint}`;

  if (params && Object.keys(params).length > 0) {
    const queryString = Object.entries(params)
      .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
      .join('&');
    url += `?${queryString}`;
  }

  return url;
}

/**
 * Build full URL for FHIR endpoint
 */
export function buildFhirUrl(endpoint: string, params?: Record<string, string>): string {
  let url = `${API_CONFIG.FHIR_SERVER_URL}${endpoint}`;

  if (params) {
    const queryString = Object.entries(params)
      .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
      .join('&');
    url += `?${queryString}`;
  }

  return url;
}

/**
 * Build full URL for QRDA Export endpoint
 */
export function buildQrdaExportUrl(endpoint: string, params?: Record<string, string>): string {
  let url = `${API_CONFIG.QRDA_EXPORT_URL}${endpoint}`;

  if (params && Object.keys(params).length > 0) {
    const queryString = Object.entries(params)
      .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
      .join('&');
    url += `?${queryString}`;
  }

  return url;
}
