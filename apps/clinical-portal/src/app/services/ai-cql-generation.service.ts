import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, delay } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * AI CQL Generation Service
 *
 * Provides AI-assisted CQL generation and explanation capabilities.
 * Uses mock implementations for demo mode.
 *
 * Issue #150: Implement AI-Assisted CQL Generation
 */
@Injectable({
  providedIn: 'root'
})
export class AiCqlGenerationService {

  private readonly baseUrl = `${environment.apiUrl}/quality-measure/api/v1/measures/ai`;

  // Demo mode - use mock responses for demonstration
  private readonly demoMode = true;

  constructor(private http: HttpClient) {}

  /**
   * Generate CQL from natural language description.
   */
  generateCql(request: CqlGenerationRequest): Observable<CqlGenerationResponse> {
    if (this.demoMode) {
      return this.mockGenerateCql(request);
    }
    return this.http.post<CqlGenerationResponse>(`${this.baseUrl}/generate-cql`, request);
  }

  /**
   * Explain existing CQL code in plain English.
   */
  explainCql(request: CqlExplainRequest): Observable<CqlExplainResponse> {
    if (this.demoMode) {
      return this.mockExplainCql(request);
    }
    return this.http.post<CqlExplainResponse>(`${this.baseUrl}/explain-cql`, request);
  }

  /**
   * Validate CQL syntax and semantics.
   */
  validateCql(cqlCode: string): Observable<CqlGenerationResponse> {
    if (this.demoMode) {
      return this.mockValidateCql(cqlCode);
    }
    return this.http.post<CqlGenerationResponse>(`${this.baseUrl}/validate-cql`, { cqlCode });
  }

  /**
   * Get available CQL templates.
   */
  getTemplates(category?: string): Observable<CqlTemplate[]> {
    if (this.demoMode) {
      return this.mockGetTemplates(category);
    }
    const params = category ? { category } : {};
    return this.http.get<CqlTemplate[]>(`${this.baseUrl}/templates`, { params });
  }

  /**
   * Get a specific CQL template with full details.
   */
  getTemplate(templateId: string): Observable<CqlTemplateDetail> {
    if (this.demoMode) {
      return this.mockGetTemplate(templateId);
    }
    return this.http.get<CqlTemplateDetail>(`${this.baseUrl}/templates/${templateId}`);
  }

  // ===== Mock Implementations for Demo =====

  private mockGenerateCql(request: CqlGenerationRequest): Observable<CqlGenerationResponse> {
    const description = request.description.toLowerCase();

    let generatedCql: string;
    let explanation: string;
    let confidence: number;

    // Generate appropriate CQL based on description
    if (description.includes('diabetes') || description.includes('hba1c') || description.includes('a1c')) {
      generatedCql = this.getDiabetesA1cCql();
      explanation = 'This measure identifies patients with diabetes who had HbA1c testing during the measurement period. It follows HEDIS CDC measure specifications.';
      confidence = 0.92;
    } else if (description.includes('blood pressure') || description.includes('hypertension') || description.includes('bp')) {
      generatedCql = this.getBloodPressureCql();
      explanation = 'This measure evaluates blood pressure control in hypertensive patients. It checks if the most recent BP reading is below 140/90 mmHg.';
      confidence = 0.89;
    } else if (description.includes('breast') || description.includes('mammogram') || description.includes('bcs')) {
      generatedCql = this.getBreastCancerScreeningCql();
      explanation = 'This measure identifies women aged 50-74 who had a mammogram to screen for breast cancer within the measurement period.';
      confidence = 0.94;
    } else if (description.includes('colon') || description.includes('colorectal') || description.includes('col')) {
      generatedCql = this.getColorectalScreeningCql();
      explanation = 'This measure identifies adults 50-75 who had appropriate screening for colorectal cancer. Multiple screening methods are acceptable.';
      confidence = 0.91;
    } else {
      generatedCql = this.getGenericMeasureCql(request.description);
      explanation = 'Generated a basic quality measure template based on your description. Please review and customize the population criteria.';
      confidence = 0.75;
    }

    const response: CqlGenerationResponse = {
      id: this.generateId(),
      generatedCql,
      explanation,
      confidence,
      validationStatus: 'VALID',
      validationResult: {
        syntaxValid: true,
        semanticValid: true,
        errors: [],
        warnings: [],
        errorCount: 0,
        warningCount: 0
      },
      suggestions: [
        {
          type: 'OPTIMIZATION',
          message: 'Consider adding exclusions for hospice patients',
          priority: 'MEDIUM'
        },
        {
          type: 'BEST_PRACTICE',
          message: 'Add measurement period bounds checking',
          priority: 'LOW'
        }
      ],
      generatedAt: new Date().toISOString(),
      modelVersion: '1.0.0'
    };

    return of(response).pipe(delay(1500)); // Simulate AI processing time
  }

  private mockExplainCql(request: CqlExplainRequest): Observable<CqlExplainResponse> {
    const response: CqlExplainResponse = {
      id: this.generateId(),
      summary: 'This CQL library defines a quality measure that identifies patients meeting specific clinical criteria and evaluates whether they received appropriate care or achieved desired outcomes.',
      sections: [
        {
          sectionName: 'Library Declaration',
          cqlSnippet: 'library MeasureName version \'1.0.0\'',
          explanation: 'Declares the CQL library name and version for identification and versioning.',
          purpose: 'Library Identification',
          lineStart: 1,
          lineEnd: 1
        },
        {
          sectionName: 'FHIR Model',
          cqlSnippet: 'using FHIR version \'4.0.1\'',
          explanation: 'Specifies that this library uses FHIR R4 data model for accessing patient data.',
          purpose: 'Data Model Definition',
          lineStart: 3,
          lineEnd: 4
        },
        {
          sectionName: 'Initial Population',
          cqlSnippet: 'define "Initial Population": ...',
          explanation: 'Defines the base population of patients eligible for this measure based on age, conditions, and enrollment criteria.',
          purpose: 'Population Definition',
          lineStart: 10,
          lineEnd: 15
        },
        {
          sectionName: 'Denominator',
          cqlSnippet: 'define "Denominator": "Initial Population"',
          explanation: 'The denominator typically equals the initial population, representing all eligible patients.',
          purpose: 'Denominator Criteria',
          lineStart: 17,
          lineEnd: 18
        },
        {
          sectionName: 'Numerator',
          cqlSnippet: 'define "Numerator": ...',
          explanation: 'Identifies patients who received the recommended care or achieved the desired outcome.',
          purpose: 'Outcome Measurement',
          lineStart: 20,
          lineEnd: 25
        }
      ],
      clinicalConcepts: [
        {
          name: 'Diabetes',
          description: 'Type 2 Diabetes Mellitus diagnosis',
          codeSystem: 'ICD-10-CM',
          valueSetOid: '2.16.840.1.113883.3.464.1003.103.12.1001',
          usage: 'Used to identify patients with diabetes in the initial population'
        },
        {
          name: 'HbA1c Test',
          description: 'Hemoglobin A1c laboratory test',
          codeSystem: 'LOINC',
          valueSetOid: '2.16.840.1.113883.3.464.1003.198.12.1013',
          usage: 'Used to determine if patient had HbA1c testing in numerator'
        }
      ],
      dataElements: [
        {
          resourceType: 'Patient',
          element: 'birthDate',
          purpose: 'Calculate patient age for eligibility',
          required: true
        },
        {
          resourceType: 'Condition',
          element: 'code, clinicalStatus',
          purpose: 'Identify active diagnoses',
          required: true
        },
        {
          resourceType: 'Observation',
          element: 'code, value, effective',
          purpose: 'Check for qualifying test results',
          required: true
        }
      ],
      potentialIssues: [
        'Consider adding exclusions for patients in hospice care',
        'May need to handle patients with multiple qualifying conditions'
      ],
      suggestions: [
        {
          type: 'BEST_PRACTICE',
          message: 'Add stratification by age group for reporting',
          priority: 'MEDIUM'
        }
      ],
      complexityRating: 6,
      performanceAssessment: {
        rating: 'MEDIUM',
        concerns: [
          'Multiple FHIR resource queries may impact performance'
        ],
        recommendations: [
          'Consider pre-computing condition status',
          'Use batch processing for large populations'
        ],
        estimatedDataQueries: 4
      },
      explainedAt: new Date().toISOString()
    };

    return of(response).pipe(delay(1000));
  }

  private mockValidateCql(cqlCode: string): Observable<CqlGenerationResponse> {
    const hasLibrary = cqlCode.includes('library');
    const hasUsing = cqlCode.includes('using FHIR');
    const hasContext = cqlCode.includes('context Patient');
    const hasDefine = cqlCode.includes('define');

    const errors: ValidationError[] = [];
    const warnings: ValidationError[] = [];

    if (!hasLibrary) {
      errors.push({
        line: 1,
        column: 1,
        message: 'Missing library declaration',
        severity: 'ERROR'
      });
    }

    if (!hasUsing) {
      errors.push({
        line: 3,
        column: 1,
        message: 'Missing FHIR model declaration (using FHIR version \'4.0.1\')',
        severity: 'ERROR'
      });
    }

    if (!hasContext) {
      warnings.push({
        line: 5,
        column: 1,
        message: 'Consider adding explicit context declaration',
        severity: 'WARNING'
      });
    }

    if (!hasDefine) {
      errors.push({
        line: 10,
        column: 1,
        message: 'No define statements found - CQL must have at least one definition',
        severity: 'ERROR'
      });
    }

    const response: CqlGenerationResponse = {
      id: this.generateId(),
      generatedCql: cqlCode,
      validationStatus: errors.length === 0 ? 'VALID' : 'INVALID',
      validationResult: {
        syntaxValid: errors.length === 0,
        semanticValid: errors.length === 0,
        errors,
        warnings,
        errorCount: errors.length,
        warningCount: warnings.length
      },
      generatedAt: new Date().toISOString(),
      modelVersion: '1.0.0'
    };

    return of(response).pipe(delay(500));
  }

  private mockGetTemplates(category?: string): Observable<CqlTemplate[]> {
    const templates: CqlTemplate[] = [
      {
        id: 'diabetes-a1c',
        name: 'Diabetes HbA1c Control',
        description: 'Patients with diabetes who had HbA1c testing',
        category: 'diabetes',
        measureType: 'PROCESS',
        tags: ['HEDIS', 'CDC', 'diabetes', 'lab-test']
      },
      {
        id: 'bp-control',
        name: 'Blood Pressure Control',
        description: 'Hypertension patients with controlled blood pressure',
        category: 'cardiovascular',
        measureType: 'OUTCOME',
        tags: ['HEDIS', 'CBP', 'hypertension']
      },
      {
        id: 'breast-cancer-screening',
        name: 'Breast Cancer Screening',
        description: 'Women with mammography screening',
        category: 'preventive',
        measureType: 'PROCESS',
        tags: ['HEDIS', 'BCS', 'screening', 'mammogram']
      },
      {
        id: 'colorectal-screening',
        name: 'Colorectal Cancer Screening',
        description: 'Adults with colorectal cancer screening',
        category: 'preventive',
        measureType: 'PROCESS',
        tags: ['HEDIS', 'COL', 'screening', 'colonoscopy']
      },
      {
        id: 'depression-screening',
        name: 'Depression Screening PHQ-9',
        description: 'Patients with depression screening and follow-up',
        category: 'behavioral',
        measureType: 'PROCESS',
        tags: ['HEDIS', 'DSF', 'mental-health', 'PHQ-9']
      },
      {
        id: 'statin-therapy',
        name: 'Statin Therapy for CVD',
        description: 'Cardiovascular patients on statin therapy',
        category: 'cardiovascular',
        measureType: 'PROCESS',
        tags: ['HEDIS', 'SPC', 'medication', 'statin']
      },
      {
        id: 'flu-vaccine',
        name: 'Influenza Immunization',
        description: 'Adults with annual flu vaccination',
        category: 'preventive',
        measureType: 'PROCESS',
        tags: ['immunization', 'flu', 'preventive']
      },
      {
        id: 'eye-exam-diabetes',
        name: 'Diabetic Eye Exam',
        description: 'Diabetic patients with annual retinal exam',
        category: 'diabetes',
        measureType: 'PROCESS',
        tags: ['HEDIS', 'EED', 'diabetes', 'eye-exam']
      }
    ];

    let filtered = templates;
    if (category) {
      filtered = templates.filter(t => t.category.toLowerCase() === category.toLowerCase());
    }

    return of(filtered).pipe(delay(300));
  }

  private mockGetTemplate(templateId: string): Observable<CqlTemplateDetail> {
    const templates: { [key: string]: CqlTemplateDetail } = {
      'diabetes-a1c': {
        id: 'diabetes-a1c',
        name: 'Diabetes HbA1c Control',
        description: 'Patients with diabetes who had HbA1c testing',
        category: 'diabetes',
        measureType: 'PROCESS',
        tags: ['HEDIS', 'CDC', 'diabetes', 'lab-test'],
        cqlCode: this.getDiabetesA1cCql(),
        explanation: 'This measure identifies patients with diabetes who had HbA1c testing during the measurement period. It follows HEDIS CDC measure specifications.',
        requiredValueSets: [
          '2.16.840.1.113883.3.464.1003.103.12.1001',
          '2.16.840.1.113883.3.464.1003.198.12.1013'
        ]
      },
      'bp-control': {
        id: 'bp-control',
        name: 'Blood Pressure Control',
        description: 'Hypertension patients with controlled blood pressure',
        category: 'cardiovascular',
        measureType: 'OUTCOME',
        tags: ['HEDIS', 'CBP', 'hypertension'],
        cqlCode: this.getBloodPressureCql(),
        explanation: 'This measure evaluates blood pressure control in hypertensive patients.',
        requiredValueSets: [
          '2.16.840.1.113883.3.464.1003.104.12.1011'
        ]
      },
      'breast-cancer-screening': {
        id: 'breast-cancer-screening',
        name: 'Breast Cancer Screening',
        description: 'Women with mammography screening',
        category: 'preventive',
        measureType: 'PROCESS',
        tags: ['HEDIS', 'BCS', 'screening', 'mammogram'],
        cqlCode: this.getBreastCancerScreeningCql(),
        explanation: 'This measure identifies women who had mammography screening for breast cancer.',
        requiredValueSets: [
          '2.16.840.1.113883.3.464.1003.108.12.1017'
        ]
      },
      'colorectal-screening': {
        id: 'colorectal-screening',
        name: 'Colorectal Cancer Screening',
        description: 'Adults with colorectal cancer screening',
        category: 'preventive',
        measureType: 'PROCESS',
        tags: ['HEDIS', 'COL', 'screening', 'colonoscopy'],
        cqlCode: this.getColorectalScreeningCql(),
        explanation: 'This measure identifies adults who had appropriate colorectal cancer screening.',
        requiredValueSets: [
          '2.16.840.1.113883.3.464.1003.108.12.1020',
          '2.16.840.1.113883.3.464.1003.108.12.1038'
        ]
      }
    };

    const template = templates[templateId];
    if (template) {
      return of(template).pipe(delay(300));
    }

    // Default template
    return of({
      id: templateId,
      name: 'Custom Template',
      description: 'A custom quality measure template',
      category: 'general',
      measureType: 'PROCESS',
      tags: ['custom'],
      cqlCode: this.getGenericMeasureCql('Custom measure'),
      explanation: 'A generic quality measure template for customization.',
      requiredValueSets: []
    }).pipe(delay(300));
  }

  // ===== Template CQL =====

  private getDiabetesA1cCql(): string {
    return `library DiabetesHbA1cControl version '1.0.0'

using FHIR version '4.0.1'
include FHIRHelpers version '4.0.1'

valueset "Diabetes": '2.16.840.1.113883.3.464.1003.103.12.1001'
valueset "HbA1c Laboratory Test": '2.16.840.1.113883.3.464.1003.198.12.1013'

parameter "Measurement Period" Interval<DateTime>

context Patient

define "Initial Population":
  AgeInYearsAt(start of "Measurement Period") >= 18
    and AgeInYearsAt(start of "Measurement Period") <= 75
    and exists([Condition: "Diabetes"] D where D.clinicalStatus ~ 'active')

define "Denominator":
  "Initial Population"

define "Numerator":
  exists([Observation: "HbA1c Laboratory Test"] O
    where O.status in {'final', 'amended'}
      and O.effective during "Measurement Period")

define "Denominator Exclusions":
  exists([Condition] C
    where C.code.coding.code contains 'Z51.5'
      and C.clinicalStatus ~ 'active')`;
  }

  private getBloodPressureCql(): string {
    return `library BloodPressureControl version '1.0.0'

using FHIR version '4.0.1'
include FHIRHelpers version '4.0.1'

valueset "Essential Hypertension": '2.16.840.1.113883.3.464.1003.104.12.1011'

parameter "Measurement Period" Interval<DateTime>

context Patient

define "Initial Population":
  AgeInYearsAt(start of "Measurement Period") >= 18
    and AgeInYearsAt(start of "Measurement Period") <= 85
    and exists([Condition: "Essential Hypertension"] H where H.clinicalStatus ~ 'active')

define "Denominator":
  "Initial Population"

define "Most Recent BP":
  Last([Observation] O
    where O.code.coding.code contains '85354-9'
      and O.status in {'final', 'amended'}
      and O.effective during "Measurement Period"
    sort by effective)

define "Systolic BP":
  ("Most Recent BP".component C where C.code.coding.code contains '8480-6').value

define "Diastolic BP":
  ("Most Recent BP".component C where C.code.coding.code contains '8462-4').value

define "Numerator":
  "Systolic BP" < 140 'mm[Hg]'
    and "Diastolic BP" < 90 'mm[Hg]'`;
  }

  private getBreastCancerScreeningCql(): string {
    return `library BreastCancerScreening version '1.0.0'

using FHIR version '4.0.1'
include FHIRHelpers version '4.0.1'

valueset "Mammography": '2.16.840.1.113883.3.464.1003.108.12.1017'

parameter "Measurement Period" Interval<DateTime>

context Patient

define "Initial Population":
  Patient.gender = 'female'
    and AgeInYearsAt(end of "Measurement Period") >= 50
    and AgeInYearsAt(end of "Measurement Period") <= 74

define "Denominator":
  "Initial Population"

define "Numerator":
  exists([Procedure: "Mammography"] M
    where M.status = 'completed'
      and M.performed during Interval[start of "Measurement Period" - 27 months, end of "Measurement Period"])
  or exists([DiagnosticReport: "Mammography"] D
    where D.status in {'final', 'amended'}
      and D.effective during Interval[start of "Measurement Period" - 27 months, end of "Measurement Period"])`;
  }

  private getColorectalScreeningCql(): string {
    return `library ColorectalCancerScreening version '1.0.0'

using FHIR version '4.0.1'
include FHIRHelpers version '4.0.1'

valueset "Colonoscopy": '2.16.840.1.113883.3.464.1003.108.12.1020'
valueset "Fecal Occult Blood Test": '2.16.840.1.113883.3.464.1003.108.12.1038'
valueset "FIT-DNA Test": '2.16.840.1.113883.3.464.1003.108.12.1039'

parameter "Measurement Period" Interval<DateTime>

context Patient

define "Initial Population":
  AgeInYearsAt(end of "Measurement Period") >= 50
    and AgeInYearsAt(end of "Measurement Period") <= 75

define "Denominator":
  "Initial Population"

define "Colonoscopy in Last 10 Years":
  exists([Procedure: "Colonoscopy"] C
    where C.status = 'completed'
      and C.performed during Interval[start of "Measurement Period" - 10 years, end of "Measurement Period"])

define "FOBT in Last Year":
  exists([Observation: "Fecal Occult Blood Test"] F
    where F.status in {'final', 'amended'}
      and F.effective during "Measurement Period")

define "FIT-DNA in Last 3 Years":
  exists([Observation: "FIT-DNA Test"] F
    where F.status in {'final', 'amended'}
      and F.effective during Interval[start of "Measurement Period" - 3 years, end of "Measurement Period"])

define "Numerator":
  "Colonoscopy in Last 10 Years"
    or "FOBT in Last Year"
    or "FIT-DNA in Last 3 Years"`;
  }

  private getGenericMeasureCql(description: string): string {
    const measureName = description
      .replace(/[^a-zA-Z0-9 ]/g, '')
      .split(' ')
      .map(w => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
      .join('')
      .substring(0, 30);

    return `library ${measureName || 'CustomMeasure'} version '1.0.0'

using FHIR version '4.0.1'
include FHIRHelpers version '4.0.1'

// TODO: Define value sets for your measure
// valueset "Condition": 'OID-HERE'
// valueset "Intervention": 'OID-HERE'

parameter "Measurement Period" Interval<DateTime>

context Patient

/*
 * Initial Population
 * Define the patients eligible for this measure.
 * Based on: ${description}
 */
define "Initial Population":
  AgeInYearsAt(start of "Measurement Period") >= 18
  // TODO: Add condition criteria

/*
 * Denominator
 * Usually equals Initial Population for most measures.
 */
define "Denominator":
  "Initial Population"

/*
 * Numerator
 * Patients who received the recommended care or achieved the outcome.
 */
define "Numerator":
  // TODO: Define numerator criteria based on your measure requirements
  true

/*
 * Denominator Exclusions (optional)
 * Patients who should be excluded from the measure.
 */
define "Denominator Exclusions":
  false`;
  }

  private generateId(): string {
    return 'ai-' + Math.random().toString(36).substring(2, 11);
  }
}

// ===== TypeScript Interfaces =====

export interface CqlGenerationRequest {
  description: string;
  measureType?: string;
  context?: MeasureContext;
  validateCql?: boolean;
  runTests?: boolean;
}

export interface MeasureContext {
  existingConditions?: string[];
  relevantValueSets?: string[];
  targetPopulation?: string;
  measurePeriodType?: string;
}

export interface CqlGenerationResponse {
  id: string;
  generatedCql: string;
  explanation?: string;
  confidence?: number;
  validationStatus: 'VALID' | 'INVALID' | 'WARNINGS' | 'NOT_VALIDATED';
  validationResult?: ValidationResult;
  testResults?: TestResult[];
  suggestions?: Suggestion[];
  generatedAt: string;
  modelVersion: string;
}

export interface ValidationResult {
  syntaxValid: boolean;
  semanticValid: boolean;
  errors: ValidationError[];
  warnings: ValidationError[];
  errorCount: number;
  warningCount: number;
}

export interface ValidationError {
  line?: number;
  column?: number;
  message: string;
  severity: 'ERROR' | 'WARNING' | 'INFO';
}

export interface TestResult {
  testName: string;
  passed: boolean;
  message?: string;
  expectedValue?: string;
  actualValue?: string;
}

export interface Suggestion {
  type: string;
  message: string;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  suggestedCode?: string;
}

export interface CqlExplainRequest {
  cqlCode: string;
  detailLevel?: 'BRIEF' | 'STANDARD' | 'DETAILED';
  includePerformanceAnalysis?: boolean;
}

export interface CqlExplainResponse {
  id: string;
  summary: string;
  sections: SectionExplanation[];
  clinicalConcepts: ClinicalConcept[];
  dataElements: DataElement[];
  potentialIssues: string[];
  suggestions: Suggestion[];
  complexityRating: number;
  performanceAssessment?: PerformanceAssessment;
  explainedAt: string;
}

export interface SectionExplanation {
  sectionName: string;
  cqlSnippet: string;
  explanation: string;
  purpose: string;
  lineStart: number;
  lineEnd: number;
}

export interface ClinicalConcept {
  name: string;
  description: string;
  codeSystem: string;
  valueSetOid: string;
  usage: string;
}

export interface DataElement {
  resourceType: string;
  element: string;
  purpose: string;
  required: boolean;
}

export interface PerformanceAssessment {
  rating: string;
  concerns: string[];
  recommendations: string[];
  estimatedDataQueries: number;
}

export interface CqlTemplate {
  id: string;
  name: string;
  description: string;
  category: string;
  measureType: string;
  tags: string[];
}

export interface CqlTemplateDetail extends CqlTemplate {
  cqlCode: string;
  explanation: string;
  requiredValueSets: string[];
}
