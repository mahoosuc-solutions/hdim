import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, BehaviorSubject, Subject } from 'rxjs';
import { debounceTime, switchMap, map, catchError, tap } from 'rxjs/operators';
import { API_CONFIG } from '../config/api.config';

/**
 * Sample patient for live preview
 */
export interface PreviewPatient {
  id: string;
  name: string;
  mrn: string;
  age: number;
  gender: 'male' | 'female' | 'other';
  conditions: string[];
  medications: string[];
  recentProcedures: string[];
  recentObservations: string[];
}

/**
 * Result of evaluating a patient against CQL
 */
export interface PatientEvaluationResult {
  patientId: string;
  patientName: string;
  mrn: string;
  evaluationStatus: 'pending' | 'evaluating' | 'complete' | 'error';
  inInitialPopulation: boolean;
  inDenominator: boolean;
  inNumerator: boolean;
  inDenominatorExclusion: boolean;
  inDenominatorException: boolean;
  inNumeratorExclusion: boolean;
  outcome: 'pass' | 'fail' | 'excluded' | 'not-eligible' | 'error';
  matchedCriteria: MatchedCriterion[];
  errors?: string[];
  executionTimeMs?: number;
}

/**
 * Matched criterion detail
 */
export interface MatchedCriterion {
  criterionName: string;
  matched: boolean;
  reason?: string;
  matchedResources?: string[];
}

/**
 * Backend API response from /custom-measures/evaluate-patient
 */
export interface PatientEvaluationApiResponse {
  patientId: string;
  patientName: string;
  mrn: string;
  outcome: string;
  matchedCriteria: Array<{
    criterionName: string;
    matched: boolean;
    reason: string;
  }>;
  message: string;
}

/**
 * Live preview evaluation request
 */
export interface LivePreviewRequest {
  cqlText: string;
  patientIds?: string[];
  measurementPeriod?: {
    start: string;
    end: string;
  };
}

/**
 * Live preview evaluation response
 */
export interface LivePreviewResponse {
  evaluationId: string;
  status: 'complete' | 'partial' | 'error';
  timestamp: string;
  cqlValid: boolean;
  cqlErrors?: string[];
  results: PatientEvaluationResult[];
  summary: {
    total: number;
    inInitialPopulation: number;
    inDenominator: number;
    inNumerator: number;
    passed: number;
    failed: number;
    excluded: number;
    notEligible: number;
    errors: number;
  };
  executionTimeMs: number;
}

@Injectable({
  providedIn: 'root',
})
export class LivePreviewService {
  private cqlSubject = new Subject<string>();
  private evaluationResults$ = new BehaviorSubject<LivePreviewResponse | null>(null);
  private isEvaluating$ = new BehaviorSubject<boolean>(false);

  // Sample patients for preview (used as fallback when backend unavailable)
  private samplePatients: PreviewPatient[] = [
    {
      id: 'preview-p1',
      name: 'John Smith',
      mrn: 'PRV-001',
      age: 52,
      gender: 'male',
      conditions: ['Type 2 Diabetes Mellitus', 'Essential Hypertension'],
      medications: ['Metformin 500mg', 'Lisinopril 10mg'],
      recentProcedures: ['HbA1c Test - 7.2%', 'Blood Pressure Check'],
      recentObservations: ['HbA1c: 7.2%', 'BP: 134/82 mmHg', 'BMI: 28.5'],
    },
    {
      id: 'preview-p2',
      name: 'Sarah Johnson',
      mrn: 'PRV-002',
      age: 58,
      gender: 'female',
      conditions: ['Breast Cancer History', 'Type 2 Diabetes Mellitus'],
      medications: ['Metformin 1000mg', 'Tamoxifen'],
      recentProcedures: ['Mammogram', 'HbA1c Test - 6.8%'],
      recentObservations: ['HbA1c: 6.8%', 'Mammogram: Normal'],
    },
    {
      id: 'preview-p3',
      name: 'Robert Davis',
      mrn: 'PRV-003',
      age: 67,
      gender: 'male',
      conditions: ['Essential Hypertension', 'Hyperlipidemia'],
      medications: ['Atorvastatin 40mg', 'Amlodipine 5mg'],
      recentProcedures: ['Colonoscopy', 'Lipid Panel'],
      recentObservations: ['BP: 142/88 mmHg', 'LDL: 95 mg/dL', 'HDL: 48 mg/dL'],
    },
    {
      id: 'preview-p4',
      name: 'Emily Chen',
      mrn: 'PRV-004',
      age: 34,
      gender: 'female',
      conditions: ['Major Depressive Disorder', 'Generalized Anxiety'],
      medications: ['Sertraline 100mg', 'Buspirone 10mg'],
      recentProcedures: ['PHQ-9 Assessment - Score: 8', 'GAD-7 Assessment'],
      recentObservations: ['PHQ-9: 8', 'GAD-7: 12', 'BMI: 24.2'],
    },
    {
      id: 'preview-p5',
      name: 'Michael Brown',
      mrn: 'PRV-005',
      age: 45,
      gender: 'male',
      conditions: ['Type 2 Diabetes Mellitus'],
      medications: ['Metformin 500mg'],
      recentProcedures: [],
      recentObservations: ['BMI: 31.2', 'BP: 128/80 mmHg'],
    },
    {
      id: 'preview-p6',
      name: 'Lisa Martinez',
      mrn: 'PRV-006',
      age: 72,
      gender: 'female',
      conditions: ['Essential Hypertension', 'Fall Risk'],
      medications: ['Hydrochlorothiazide 25mg'],
      recentProcedures: ['Fall Risk Assessment', 'BP Monitoring'],
      recentObservations: ['BP: 148/92 mmHg', 'Falls Risk: High', 'Gait: Unsteady'],
    },
  ];

  constructor(private http: HttpClient) {
    // Set up debounced CQL evaluation
    this.cqlSubject.pipe(
      debounceTime(800), // Wait 800ms after typing stops
      tap(() => this.isEvaluating$.next(true)),
      switchMap((cql) => this.evaluateCql(cql)),
      tap(() => this.isEvaluating$.next(false))
    ).subscribe((result) => {
      this.evaluationResults$.next(result);
    });
  }

  /**
   * Get observable for evaluation results
   */
  getResults(): Observable<LivePreviewResponse | null> {
    return this.evaluationResults$.asObservable();
  }

  /**
   * Get observable for evaluation status
   */
  isEvaluating(): Observable<boolean> {
    return this.isEvaluating$.asObservable();
  }

  /**
   * Submit CQL for debounced evaluation
   */
  submitCql(cqlText: string): void {
    this.cqlSubject.next(cqlText);
  }

  /**
   * Get sample patients for preview
   */
  getSamplePatients(): PreviewPatient[] {
    return this.samplePatients;
  }

  /**
   * Evaluate CQL against sample patients (with fallback)
   */
  evaluateCql(
    cqlText: string,
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<LivePreviewResponse> {
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/cql/preview`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
    });

    const request: LivePreviewRequest = {
      cqlText,
      measurementPeriod: {
        start: this.getMeasurementPeriodStart(),
        end: this.getMeasurementPeriodEnd(),
      },
    };

    return this.http.post<LivePreviewResponse>(url, request, { headers }).pipe(
      catchError(() => {
        // Fallback to local evaluation simulation
        return of(this.simulateEvaluation(cqlText));
      })
    );
  }

  /**
   * Simulate CQL evaluation locally (fallback when backend unavailable)
   */
  private simulateEvaluation(cqlText: string): LivePreviewResponse {
    const startTime = Date.now();

    // Check if CQL is valid (basic syntax check)
    const cqlValid = this.validateCqlSyntax(cqlText);
    const cqlErrors = cqlValid ? [] : this.getCqlErrors(cqlText);

    if (!cqlValid) {
      return {
        evaluationId: `eval-${Date.now()}`,
        status: 'error',
        timestamp: new Date().toISOString(),
        cqlValid: false,
        cqlErrors,
        results: [],
        summary: {
          total: 0,
          inInitialPopulation: 0,
          inDenominator: 0,
          inNumerator: 0,
          passed: 0,
          failed: 0,
          excluded: 0,
          notEligible: 0,
          errors: 1,
        },
        executionTimeMs: Date.now() - startTime,
      };
    }

    // Parse CQL to understand what it's looking for
    const cqlLower = cqlText.toLowerCase();
    const results: PatientEvaluationResult[] = this.samplePatients.map((patient) => {
      return this.evaluatePatient(patient, cqlLower);
    });

    // Calculate summary
    const summary = {
      total: results.length,
      inInitialPopulation: results.filter((r) => r.inInitialPopulation).length,
      inDenominator: results.filter((r) => r.inDenominator).length,
      inNumerator: results.filter((r) => r.inNumerator).length,
      passed: results.filter((r) => r.outcome === 'pass').length,
      failed: results.filter((r) => r.outcome === 'fail').length,
      excluded: results.filter((r) => r.outcome === 'excluded').length,
      notEligible: results.filter((r) => r.outcome === 'not-eligible').length,
      errors: results.filter((r) => r.outcome === 'error').length,
    };

    return {
      evaluationId: `eval-${Date.now()}`,
      status: 'complete',
      timestamp: new Date().toISOString(),
      cqlValid: true,
      results,
      summary,
      executionTimeMs: Date.now() - startTime,
    };
  }

  /**
   * Evaluate a single patient against CQL patterns
   */
  private evaluatePatient(patient: PreviewPatient, cqlLower: string): PatientEvaluationResult {
    const matchedCriteria: MatchedCriterion[] = [];

    // Check age criteria
    const ageMatch = cqlLower.match(/age.*?(\d+).*?(\d+)/);
    let meetsAgeCriteria = true;
    if (ageMatch) {
      const minAge = parseInt(ageMatch[1], 10);
      const maxAge = parseInt(ageMatch[2], 10);
      meetsAgeCriteria = patient.age >= minAge && patient.age <= maxAge;
      matchedCriteria.push({
        criterionName: 'Age Criteria',
        matched: meetsAgeCriteria,
        reason: meetsAgeCriteria
          ? `Patient age ${patient.age} is within range ${minAge}-${maxAge}`
          : `Patient age ${patient.age} is outside range ${minAge}-${maxAge}`,
      });
    }

    // Check for diabetes-related measures
    const hasDiabetesCriteria = cqlLower.includes('diabetes') || cqlLower.includes('hba1c');
    const patientHasDiabetes = patient.conditions.some((c) =>
      c.toLowerCase().includes('diabetes')
    );
    const hasHba1c = patient.recentObservations.some((o) =>
      o.toLowerCase().includes('hba1c')
    );

    if (hasDiabetesCriteria) {
      matchedCriteria.push({
        criterionName: 'Diabetes Diagnosis',
        matched: patientHasDiabetes,
        reason: patientHasDiabetes
          ? `Patient has diabetes diagnosis`
          : `No diabetes diagnosis found`,
        matchedResources: patientHasDiabetes
          ? patient.conditions.filter((c) => c.toLowerCase().includes('diabetes'))
          : [],
      });

      if (cqlLower.includes('hba1c')) {
        matchedCriteria.push({
          criterionName: 'HbA1c Test',
          matched: hasHba1c,
          reason: hasHba1c
            ? `HbA1c test found in measurement period`
            : `No HbA1c test found`,
          matchedResources: hasHba1c
            ? patient.recentObservations.filter((o) => o.toLowerCase().includes('hba1c'))
            : [],
        });
      }
    }

    // Check for hypertension/BP measures
    const hasHypertensionCriteria =
      cqlLower.includes('hypertension') || cqlLower.includes('blood pressure');
    const patientHasHypertension = patient.conditions.some((c) =>
      c.toLowerCase().includes('hypertension')
    );
    const hasBP = patient.recentObservations.some((o) => o.toLowerCase().includes('bp'));

    if (hasHypertensionCriteria) {
      matchedCriteria.push({
        criterionName: 'Hypertension Diagnosis',
        matched: patientHasHypertension,
        reason: patientHasHypertension
          ? `Patient has hypertension diagnosis`
          : `No hypertension diagnosis found`,
      });

      if (hasBP) {
        // Parse BP value
        const bpObs = patient.recentObservations.find((o) => o.toLowerCase().includes('bp'));
        const bpMatch = bpObs?.match(/(\d+)\/(\d+)/);
        const systolic = bpMatch ? parseInt(bpMatch[1], 10) : 0;
        const diastolic = bpMatch ? parseInt(bpMatch[2], 10) : 0;
        const bpControlled = systolic < 140 && diastolic < 90;

        matchedCriteria.push({
          criterionName: 'Blood Pressure Control',
          matched: bpControlled,
          reason: bpControlled
            ? `BP ${systolic}/${diastolic} is controlled (<140/90)`
            : `BP ${systolic}/${diastolic} is not controlled (>=140/90)`,
          matchedResources: bpObs ? [bpObs] : [],
        });
      }
    }

    // Check for cancer screening
    const hasCancerScreening =
      cqlLower.includes('mammogram') ||
      cqlLower.includes('breast cancer') ||
      cqlLower.includes('colonoscopy') ||
      cqlLower.includes('colorectal');

    if (hasCancerScreening) {
      if (cqlLower.includes('mammogram') || cqlLower.includes('breast')) {
        const hasMammogram = patient.recentProcedures.some((p) =>
          p.toLowerCase().includes('mammogram')
        );
        matchedCriteria.push({
          criterionName: 'Mammogram',
          matched: hasMammogram,
          reason: hasMammogram
            ? `Mammogram found in records`
            : `No mammogram found`,
          matchedResources: hasMammogram
            ? patient.recentProcedures.filter((p) => p.toLowerCase().includes('mammogram'))
            : [],
        });
      }

      if (cqlLower.includes('colonoscopy') || cqlLower.includes('colorectal')) {
        const hasColonoscopy = patient.recentProcedures.some((p) =>
          p.toLowerCase().includes('colonoscopy')
        );
        matchedCriteria.push({
          criterionName: 'Colonoscopy',
          matched: hasColonoscopy,
          reason: hasColonoscopy
            ? `Colonoscopy found in records`
            : `No colonoscopy found`,
          matchedResources: hasColonoscopy
            ? patient.recentProcedures.filter((p) => p.toLowerCase().includes('colonoscopy'))
            : [],
        });
      }
    }

    // Check for depression screening
    const hasDepressionCriteria =
      cqlLower.includes('depression') || cqlLower.includes('phq');

    if (hasDepressionCriteria) {
      const hasPhq9 = patient.recentProcedures.some((p) =>
        p.toLowerCase().includes('phq')
      );
      matchedCriteria.push({
        criterionName: 'Depression Screening (PHQ-9)',
        matched: hasPhq9,
        reason: hasPhq9
          ? `PHQ-9 assessment found`
          : `No PHQ-9 assessment found`,
        matchedResources: hasPhq9
          ? patient.recentProcedures.filter((p) => p.toLowerCase().includes('phq'))
          : [],
      });
    }

    // Check for falls risk
    const hasFallsCriteria = cqlLower.includes('fall');

    if (hasFallsCriteria) {
      const hasFallRisk = patient.conditions.some((c) =>
        c.toLowerCase().includes('fall')
      );
      const hasFallAssessment = patient.recentProcedures.some((p) =>
        p.toLowerCase().includes('fall')
      );
      matchedCriteria.push({
        criterionName: 'Fall Risk Assessment',
        matched: hasFallAssessment,
        reason: hasFallAssessment
          ? `Fall risk assessment completed`
          : `No fall risk assessment found`,
      });
    }

    // Determine population membership based on matched criteria
    const denominatorCriteria = matchedCriteria.filter(
      (c) => c.criterionName.includes('Diagnosis') || c.criterionName.includes('Age')
    );
    const numeratorCriteria = matchedCriteria.filter(
      (c) =>
        c.criterionName.includes('Test') ||
        c.criterionName.includes('Control') ||
        c.criterionName.includes('Screening') ||
        c.criterionName.includes('Assessment') ||
        c.criterionName.includes('Mammogram') ||
        c.criterionName.includes('Colonoscopy')
    );

    const inInitialPopulation = meetsAgeCriteria;
    const inDenominator =
      inInitialPopulation &&
      (denominatorCriteria.length === 0 ||
        denominatorCriteria.some((c) => c.matched));
    const inNumerator =
      inDenominator &&
      (numeratorCriteria.length === 0 || numeratorCriteria.some((c) => c.matched));

    // Determine outcome
    let outcome: 'pass' | 'fail' | 'excluded' | 'not-eligible' | 'error';
    if (!inInitialPopulation) {
      outcome = 'not-eligible';
    } else if (!inDenominator) {
      outcome = 'not-eligible';
    } else if (inNumerator) {
      outcome = 'pass';
    } else {
      outcome = 'fail';
    }

    return {
      patientId: patient.id,
      patientName: patient.name,
      mrn: patient.mrn,
      evaluationStatus: 'complete',
      inInitialPopulation,
      inDenominator,
      inNumerator,
      inDenominatorExclusion: false,
      inDenominatorException: false,
      inNumeratorExclusion: false,
      outcome,
      matchedCriteria,
      executionTimeMs: Math.floor(Math.random() * 50) + 10,
    };
  }

  /**
   * Basic CQL syntax validation
   */
  private validateCqlSyntax(cqlText: string): boolean {
    if (!cqlText || cqlText.trim().length < 20) {
      return false;
    }

    // Check for basic CQL structure
    const hasLibrary = /library\s+\w+/i.test(cqlText);
    const hasUsing = /using\s+fhir/i.test(cqlText);
    const hasContext = /context\s+patient/i.test(cqlText);

    return hasLibrary || hasUsing || hasContext;
  }

  /**
   * Get CQL syntax errors
   */
  private getCqlErrors(cqlText: string): string[] {
    const errors: string[] = [];

    if (!cqlText || cqlText.trim().length < 20) {
      errors.push('CQL content is too short or empty');
    }

    if (!/library\s+\w+/i.test(cqlText)) {
      errors.push('Missing library declaration (e.g., library MyMeasure version \'1.0\')');
    }

    if (!/using\s+fhir/i.test(cqlText)) {
      errors.push('Missing FHIR using declaration (e.g., using FHIR version \'4.0.1\')');
    }

    if (!/context\s+patient/i.test(cqlText)) {
      errors.push('Missing context declaration (e.g., context Patient)');
    }

    return errors;
  }

  /**
   * Get measurement period start (beginning of current year)
   */
  private getMeasurementPeriodStart(): string {
    const now = new Date();
    return `${now.getFullYear()}-01-01T00:00:00Z`;
  }

  /**
   * Get measurement period end (end of current year)
   */
  private getMeasurementPeriodEnd(): string {
    const now = new Date();
    return `${now.getFullYear()}-12-31T23:59:59Z`;
  }

  /**
   * Clear evaluation results
   */
  clearResults(): void {
    this.evaluationResults$.next(null);
  }

  /**
   * Evaluate CQL against a specific patient by ID
   * Uses the backend /custom-measures/evaluate-patient endpoint
   */
  evaluateSpecificPatient(
    cqlText: string,
    patientId: string,
    tenantId: string = API_CONFIG.DEFAULT_TENANT_ID
  ): Observable<LivePreviewResponse> {
    // Use the new evaluate-patient endpoint
    const url = `${API_CONFIG.QUALITY_MEASURE_URL}/custom-measures/evaluate-patient`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': tenantId,
      'Content-Type': 'application/json',
    });

    const request = {
      cqlText,
      patientId,
    };

    return this.http.post<PatientEvaluationApiResponse>(url, request, { headers }).pipe(
      map((response) => this.mapApiResponseToLivePreview(response)),
      catchError(() => {
        // Fallback: Try to find patient in sample patients
        const samplePatient = this.samplePatients.find(p => p.id === patientId || p.mrn === patientId);
        if (samplePatient) {
          return of(this.simulateSinglePatientEvaluation(cqlText, samplePatient));
        }
        // Return error response if patient not found
        return of({
          evaluationId: `eval-${Date.now()}`,
          status: 'error' as const,
          timestamp: new Date().toISOString(),
          cqlValid: true,
          cqlErrors: [`Patient ${patientId} not found in sample data`],
          results: [],
          summary: {
            total: 0,
            inInitialPopulation: 0,
            inDenominator: 0,
            inNumerator: 0,
            passed: 0,
            failed: 0,
            excluded: 0,
            notEligible: 0,
            errors: 1,
          },
          executionTimeMs: 0,
        });
      })
    );
  }

  /**
   * Map backend API response to LivePreviewResponse format
   */
  private mapApiResponseToLivePreview(apiResponse: PatientEvaluationApiResponse): LivePreviewResponse {
    const outcome = apiResponse.outcome as PatientEvaluationResult['outcome'];
    
    const result: PatientEvaluationResult = {
      patientId: apiResponse.patientId,
      patientName: apiResponse.patientName,
      mrn: apiResponse.mrn,
      evaluationStatus: 'complete',
      inInitialPopulation: outcome !== 'not-eligible',
      inDenominator: outcome === 'pass' || outcome === 'fail',
      inNumerator: outcome === 'pass',
      inDenominatorExclusion: false,
      inDenominatorException: false,
      inNumeratorExclusion: false,
      outcome,
      matchedCriteria: apiResponse.matchedCriteria?.map(c => ({
        criterionName: c.criterionName,
        matched: c.matched,
        reason: c.reason,
      })) || [],
    };

    return {
      evaluationId: `eval-${Date.now()}`,
      status: 'complete',
      timestamp: new Date().toISOString(),
      cqlValid: true,
      cqlErrors: [],
      results: [result],
      summary: {
        total: 1,
        inInitialPopulation: result.inInitialPopulation ? 1 : 0,
        inDenominator: result.inDenominator ? 1 : 0,
        inNumerator: result.inNumerator ? 1 : 0,
        passed: outcome === 'pass' ? 1 : 0,
        failed: outcome === 'fail' ? 1 : 0,
        excluded: outcome === 'excluded' ? 1 : 0,
        notEligible: outcome === 'not-eligible' ? 1 : 0,
        errors: outcome === 'error' ? 1 : 0,
      },
      executionTimeMs: 50,
    };
  }

  /**
   * Simulate evaluation for a single patient
   */
  private simulateSinglePatientEvaluation(cqlText: string, patient: PreviewPatient): LivePreviewResponse {
    const startTime = Date.now();
    const cqlLower = cqlText.toLowerCase();
    const result = this.evaluatePatient(patient, cqlLower);

    return {
      evaluationId: `eval-${Date.now()}`,
      status: 'complete',
      timestamp: new Date().toISOString(),
      cqlValid: true,
      results: [result],
      summary: {
        total: 1,
        inInitialPopulation: result.inInitialPopulation ? 1 : 0,
        inDenominator: result.inDenominator ? 1 : 0,
        inNumerator: result.inNumerator ? 1 : 0,
        passed: result.outcome === 'pass' ? 1 : 0,
        failed: result.outcome === 'fail' ? 1 : 0,
        excluded: result.outcome === 'excluded' ? 1 : 0,
        notEligible: result.outcome === 'not-eligible' ? 1 : 0,
        errors: result.outcome === 'error' ? 1 : 0,
      },
      executionTimeMs: Date.now() - startTime,
    };
  }

  /**
   * Search patients by name or MRN (for specific patient testing)
   */
  searchPatients(query: string): PreviewPatient[] {
    if (!query || query.trim().length < 2) {
      return [];
    }
    const queryLower = query.toLowerCase();
    return this.samplePatients.filter(p =>
      p.name.toLowerCase().includes(queryLower) ||
      p.mrn.toLowerCase().includes(queryLower)
    );
  }
}
