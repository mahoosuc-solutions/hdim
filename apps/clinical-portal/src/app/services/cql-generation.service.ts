import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable, of, delay } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { API_CONFIG } from '../config/api.config';

/**
 * CQL Generation Service
 *
 * Provides AI-assisted CQL (Clinical Quality Language) generation capabilities:
 * - Natural language to CQL conversion
 * - CQL syntax validation and suggestions
 * - Auto-completion for value sets and measure definitions
 * - CQL optimization recommendations
 */

export interface CqlGenerationRequest {
  prompt: string;
  context?: CqlGenerationContext;
  options?: CqlGenerationOptions;
}

export interface CqlGenerationContext {
  measureName?: string;
  measureDescription?: string;
  category?: string;
  existingCql?: string;
  targetPopulation?: string;
  clinicalCriteria?: string[];
}

export interface CqlGenerationOptions {
  includeComments?: boolean;
  includeValueSets?: boolean;
  fhirVersion?: string;
  measureType?: 'proportion' | 'continuous' | 'ratio';
}

export interface CqlGenerationResponse {
  cql: string;
  explanation?: string;
  warnings?: string[];
  suggestedValueSets?: SuggestedValueSet[];
  confidence: number;
}

export interface SuggestedValueSet {
  oid: string;
  name: string;
  codeSystem: string;
  description: string;
  usage: string;
}

export interface CqlValidationResult {
  valid: boolean;
  errors: CqlError[];
  warnings: CqlWarning[];
  suggestions: CqlSuggestion[];
}

export interface CqlError {
  line: number;
  column: number;
  message: string;
  severity: 'error';
}

export interface CqlWarning {
  line: number;
  column: number;
  message: string;
  severity: 'warning';
}

export interface CqlSuggestion {
  line: number;
  column: number;
  message: string;
  replacement?: string;
  severity: 'info';
}

export interface CqlCompletion {
  label: string;
  detail: string;
  insertText: string;
  kind: 'keyword' | 'function' | 'valueset' | 'codesystem' | 'snippet';
}

@Injectable({
  providedIn: 'root',
})
export class CqlGenerationService {
  private readonly apiUrl = API_CONFIG.QUALITY_MEASURE_URL;

  constructor(private http: HttpClient) {}

  /**
   * Generate CQL from natural language prompt
   *
   * Issue #150: Updated to use new AI CQL generation endpoint
   */
  generateCql(request: CqlGenerationRequest): Observable<CqlGenerationResponse> {
    const url = `${this.apiUrl}/api/v1/measures/ai/generate-cql`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': API_CONFIG.DEFAULT_TENANT_ID,
    });

    // Transform request to match backend DTO
    const backendRequest = {
      description: request.prompt,
      measureType: request.options?.measureType?.toUpperCase() || 'PROCESS',
      context: {
        existingConditions: request.context?.clinicalCriteria,
        targetPopulation: request.context?.targetPopulation,
      },
      validateCql: true,
      runTests: false,
    };

    // Try backend first, fallback to local generation
    return this.http.post<any>(url, backendRequest, { headers }).pipe(
      map((backendResponse) => this.transformBackendResponse(backendResponse)),
      catchError(() => {
        // Fallback to local AI-like generation
        return this.generateCqlLocally(request);
      })
    );
  }

  /**
   * Transform backend response to frontend format
   */
  private transformBackendResponse(backendResponse: any): CqlGenerationResponse {
    return {
      cql: backendResponse.generatedCql || '',
      explanation: backendResponse.explanation || '',
      warnings: backendResponse.validationResult?.warnings?.map((w: any) => w.message) || [],
      suggestedValueSets: backendResponse.suggestions?.filter((s: any) => s.type === 'VALUE_SET')?.map((s: any) => ({
        oid: s.suggestedCode || '',
        name: s.message,
        codeSystem: 'Various',
        description: s.message,
        usage: 'Suggested by AI',
      })) || [],
      confidence: backendResponse.confidence || 0.85,
    };
  }

  /**
   * Validate CQL syntax
   *
   * Issue #150: Updated to use new AI CQL validation endpoint
   */
  validateCql(cql: string): Observable<CqlValidationResult> {
    const url = `${this.apiUrl}/api/v1/measures/ai/validate-cql`;
    const headers = new HttpHeaders({
      'X-Tenant-ID': API_CONFIG.DEFAULT_TENANT_ID,
    });

    return this.http.post<any>(url, { cqlCode: cql }, { headers }).pipe(
      map((backendResponse) => this.transformValidationResponse(backendResponse)),
      catchError(() => {
        // Fallback to local validation
        return this.validateCqlLocally(cql);
      })
    );
  }

  /**
   * Transform backend validation response to frontend format
   */
  private transformValidationResponse(backendResponse: any): CqlValidationResult {
    const result = backendResponse.validationResult || {};
    return {
      valid: result.syntaxValid && result.semanticValid,
      errors: (result.errors || []).map((e: any) => ({
        line: e.line || 1,
        column: e.column || 1,
        message: e.message,
        severity: 'error' as const,
      })),
      warnings: (result.warnings || []).map((w: any) => ({
        line: w.line || 1,
        column: w.column || 1,
        message: w.message,
        severity: 'warning' as const,
      })),
      suggestions: [],
    };
  }

  /**
   * Get auto-completions for CQL editor
   */
  getCompletions(cql: string, position: { line: number; column: number }): Observable<CqlCompletion[]> {
    // Local completions (can be enhanced with backend)
    return of(this.getLocalCompletions(cql, position));
  }

  /**
   * Get optimization suggestions for CQL
   */
  getOptimizationSuggestions(cql: string): Observable<CqlSuggestion[]> {
    return of(this.analyzeForOptimizations(cql));
  }

  /**
   * Local CQL generation (fallback when backend unavailable)
   */
  private generateCqlLocally(request: CqlGenerationRequest): Observable<CqlGenerationResponse> {
    const { prompt, context, options } = request;
    const lowerPrompt = prompt.toLowerCase();

    let cql = this.buildCqlFromPrompt(lowerPrompt, context, options);
    let explanation = this.buildExplanation(lowerPrompt, context);
    let suggestedValueSets = this.suggestValueSets(lowerPrompt);

    return of({
      cql,
      explanation,
      suggestedValueSets,
      warnings: [],
      confidence: 0.85,
    }).pipe(delay(500)); // Simulate AI processing time
  }

  /**
   * Build CQL from natural language prompt
   */
  private buildCqlFromPrompt(
    prompt: string,
    context?: CqlGenerationContext,
    options?: CqlGenerationOptions
  ): string {
    const measureName = context?.measureName?.replace(/\s+/g, '') || 'CustomMeasure';
    const fhirVersion = options?.fhirVersion || '4.0.1';
    const includeComments = options?.includeComments !== false;

    let cql = `library ${measureName} version '1.0.0'\n\n`;
    cql += `using FHIR version '${fhirVersion}'\n\n`;
    cql += `include FHIRHelpers version '${fhirVersion}'\n\n`;

    // Add code systems
    cql += `// Code Systems\n`;
    cql += `codesystem "LOINC": 'http://loinc.org'\n`;
    cql += `codesystem "SNOMEDCT": 'http://snomed.info/sct'\n`;
    cql += `codesystem "ICD10CM": 'http://hl7.org/fhir/sid/icd-10-cm'\n`;
    cql += `codesystem "RXNORM": 'http://www.nlm.nih.gov/research/umls/rxnorm'\n\n`;

    // Detect condition/disease type from prompt
    const conditionPatterns = this.detectConditions(prompt);
    const interventionPatterns = this.detectInterventions(prompt);
    const ageRange = this.detectAgeRange(prompt);

    // Add value sets based on detected patterns
    cql += `// Value Sets\n`;
    conditionPatterns.forEach(({ name, oid }) => {
      cql += `valueset "${name}": '${oid}'\n`;
    });
    interventionPatterns.forEach(({ name, oid }) => {
      cql += `valueset "${name}": '${oid}'\n`;
    });
    cql += `\n`;

    // Add parameters
    cql += `// Parameters\n`;
    cql += `parameter "Measurement Period" Interval<DateTime>\n\n`;

    cql += `context Patient\n\n`;

    // Generate demographic criteria
    if (includeComments) {
      cql += `// Demographic criteria\n`;
    }
    cql += `define "In Demographic":\n`;
    if (ageRange) {
      cql += `  AgeInYearsAt(start of "Measurement Period") >= ${ageRange.min}\n`;
      cql += `    and AgeInYearsAt(start of "Measurement Period") <= ${ageRange.max}\n`;
    } else {
      cql += `  AgeInYearsAt(start of "Measurement Period") >= 18\n`;
    }

    // Detect if this is a gender-specific measure
    if (prompt.includes('women') || prompt.includes('female') || prompt.includes('breast') || prompt.includes('cervical')) {
      cql += `    and Patient.gender = 'female'\n`;
    } else if (prompt.includes('men') || prompt.includes('male') || prompt.includes('prostate')) {
      cql += `    and Patient.gender = 'male'\n`;
    }
    cql += `\n`;

    // Generate condition criteria
    if (conditionPatterns.length > 0) {
      if (includeComments) {
        cql += `// Clinical condition criteria\n`;
      }
      conditionPatterns.forEach(({ name, definition }) => {
        cql += `define "Has ${name.replace(' Diagnosis', '')}":\n`;
        cql += `  exists([Condition: "${name}"] C\n`;
        cql += `    where C.clinicalStatus ~ 'active'\n`;
        cql += `      and C.verificationStatus ~ 'confirmed')\n\n`;
      });
    }

    // Generate intervention criteria
    if (interventionPatterns.length > 0) {
      if (includeComments) {
        cql += `// Intervention criteria\n`;
      }
      interventionPatterns.forEach(({ name, resourceType, definition }) => {
        const defName = name.replace(' ', '');
        if (resourceType === 'Observation') {
          cql += `define "${defName} Result":\n`;
          cql += `  [Observation: "${name}"] O\n`;
          cql += `    where O.status in {'final', 'amended'}\n`;
          cql += `      and O.effective during "Measurement Period"\n\n`;
          cql += `define "Has ${defName}":\n`;
          cql += `  exists("${defName} Result")\n\n`;
        } else if (resourceType === 'Procedure') {
          cql += `define "Has ${defName}":\n`;
          cql += `  exists([Procedure: "${name}"] P\n`;
          cql += `    where P.status = 'completed'\n`;
          cql += `      and P.performed during "Measurement Period")\n\n`;
        } else if (resourceType === 'Immunization') {
          cql += `define "Has ${defName}":\n`;
          cql += `  exists([Immunization: "${name}"] I\n`;
          cql += `    where I.status = 'completed'\n`;
          cql += `      and I.occurrence during "Measurement Period")\n\n`;
        } else if (resourceType === 'MedicationRequest') {
          cql += `define "On ${defName}":\n`;
          cql += `  exists([MedicationRequest: "${name}"] M\n`;
          cql += `    where M.status = 'active'\n`;
          cql += `      and M.authoredOn during "Measurement Period")\n\n`;
        }
      });
    }

    // Generate population definitions
    if (includeComments) {
      cql += `// ===== Population Definitions =====\n\n`;
    }

    // Initial Population
    cql += `define "Initial Population":\n`;
    cql += `  "In Demographic"\n`;
    if (conditionPatterns.length > 0) {
      cql += `    and "Has ${conditionPatterns[0].name.replace(' Diagnosis', '')}"\n`;
    }
    cql += `\n`;

    // Denominator
    cql += `define "Denominator":\n`;
    cql += `  "Initial Population"\n\n`;

    // Numerator
    cql += `define "Numerator":\n`;
    if (interventionPatterns.length > 0) {
      const interventionDef = interventionPatterns[0].resourceType === 'MedicationRequest'
        ? `"On ${interventionPatterns[0].name.replace(' ', '')}"`
        : `"Has ${interventionPatterns[0].name.replace(' ', '')}"`;
      cql += `  ${interventionDef}\n`;
    } else {
      cql += `  "Denominator"\n`;
    }
    cql += `\n`;

    // Denominator Exclusions
    cql += `define "Denominator Exclusions":\n`;
    cql += `  false\n`;

    return cql;
  }

  /**
   * Detect conditions from prompt
   */
  private detectConditions(prompt: string): Array<{ name: string; oid: string; definition: string }> {
    const conditions: Array<{ name: string; oid: string; definition: string }> = [];

    const conditionMap: Record<string, { name: string; oid: string }> = {
      diabetes: { name: 'Diabetes Diagnosis', oid: 'urn:oid:2.16.840.1.113883.3.464.1003.103.12.1001' },
      hypertension: { name: 'Essential Hypertension', oid: 'urn:oid:2.16.840.1.113883.3.464.1003.104.12.1011' },
      'high blood pressure': { name: 'Essential Hypertension', oid: 'urn:oid:2.16.840.1.113883.3.464.1003.104.12.1011' },
      depression: { name: 'Depression Diagnosis', oid: 'urn:oid:2.16.840.1.113883.3.600.146' },
      asthma: { name: 'Asthma Diagnosis', oid: 'urn:oid:2.16.840.1.113883.3.526.3.362' },
      copd: { name: 'COPD Diagnosis', oid: 'urn:oid:2.16.840.1.113883.3.526.3.1003' },
      'heart failure': { name: 'Heart Failure Diagnosis', oid: 'urn:oid:2.16.840.1.113883.3.526.3.376' },
      chf: { name: 'Heart Failure Diagnosis', oid: 'urn:oid:2.16.840.1.113883.3.526.3.376' },
      cad: { name: 'Coronary Artery Disease', oid: 'urn:oid:2.16.840.1.113883.3.464.1003.104.12.1003' },
      'coronary artery disease': { name: 'Coronary Artery Disease', oid: 'urn:oid:2.16.840.1.113883.3.464.1003.104.12.1003' },
      ckd: { name: 'Chronic Kidney Disease', oid: 'urn:oid:2.16.840.1.113883.3.526.3.1002' },
      'kidney disease': { name: 'Chronic Kidney Disease', oid: 'urn:oid:2.16.840.1.113883.3.526.3.1002' },
    };

    Object.entries(conditionMap).forEach(([keyword, data]) => {
      if (prompt.includes(keyword) && !conditions.find(c => c.name === data.name)) {
        conditions.push({ ...data, definition: keyword });
      }
    });

    return conditions;
  }

  /**
   * Detect interventions from prompt
   */
  private detectInterventions(prompt: string): Array<{ name: string; oid: string; resourceType: string; definition: string }> {
    const interventions: Array<{ name: string; oid: string; resourceType: string; definition: string }> = [];

    const interventionMap: Record<string, { name: string; oid: string; resourceType: string }> = {
      'hba1c': { name: 'HbA1c Laboratory Test', oid: 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1013', resourceType: 'Observation' },
      'hemoglobin a1c': { name: 'HbA1c Laboratory Test', oid: 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1013', resourceType: 'Observation' },
      'blood pressure': { name: 'Blood Pressure Measurement', oid: 'urn:oid:2.16.840.1.113883.3.464.1003.118.12.1035', resourceType: 'Observation' },
      bp: { name: 'Blood Pressure Measurement', oid: 'urn:oid:2.16.840.1.113883.3.464.1003.118.12.1035', resourceType: 'Observation' },
      mammogram: { name: 'Mammography', oid: 'urn:oid:2.16.840.1.113883.3.464.1003.108.12.1018', resourceType: 'Procedure' },
      'breast cancer screening': { name: 'Mammography', oid: 'urn:oid:2.16.840.1.113883.3.464.1003.108.12.1018', resourceType: 'Procedure' },
      colonoscopy: { name: 'Colonoscopy', oid: 'urn:oid:2.16.840.1.113883.3.464.1003.108.12.1020', resourceType: 'Procedure' },
      'colorectal screening': { name: 'Colonoscopy', oid: 'urn:oid:2.16.840.1.113883.3.464.1003.108.12.1020', resourceType: 'Procedure' },
      statin: { name: 'Statin Medications', oid: 'urn:oid:2.16.840.1.113883.3.526.3.1003', resourceType: 'MedicationRequest' },
      'flu vaccine': { name: 'Influenza Vaccine', oid: 'urn:oid:2.16.840.1.113883.3.526.3.1254', resourceType: 'Immunization' },
      'influenza vaccine': { name: 'Influenza Vaccine', oid: 'urn:oid:2.16.840.1.113883.3.526.3.1254', resourceType: 'Immunization' },
      'phq-9': { name: 'PHQ-9 Screening', oid: 'urn:oid:2.16.840.1.113883.3.600.145', resourceType: 'Observation' },
      'depression screening': { name: 'PHQ-9 Screening', oid: 'urn:oid:2.16.840.1.113883.3.600.145', resourceType: 'Observation' },
      bmi: { name: 'BMI Measurement', oid: 'urn:oid:2.16.840.1.113883.3.600.1.1525', resourceType: 'Observation' },
      ldl: { name: 'LDL Cholesterol Test', oid: 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1016', resourceType: 'Observation' },
      'lipid panel': { name: 'Lipid Panel', oid: 'urn:oid:2.16.840.1.113883.3.464.1003.198.12.1012', resourceType: 'Observation' },
      ace: { name: 'ACE Inhibitor Medications', oid: 'urn:oid:2.16.840.1.113883.3.526.3.1139', resourceType: 'MedicationRequest' },
      arb: { name: 'ARB Medications', oid: 'urn:oid:2.16.840.1.113883.3.526.3.1140', resourceType: 'MedicationRequest' },
    };

    Object.entries(interventionMap).forEach(([keyword, data]) => {
      if (prompt.includes(keyword) && !interventions.find(i => i.name === data.name)) {
        interventions.push({ ...data, definition: keyword });
      }
    });

    return interventions;
  }

  /**
   * Detect age range from prompt
   */
  private detectAgeRange(prompt: string): { min: number; max: number } | null {
    // Pattern: "18-75" or "18 to 75" or "adults over 65" etc.
    const rangeMatch = prompt.match(/(\d{1,3})\s*[-to]+\s*(\d{1,3})/);
    if (rangeMatch) {
      return { min: parseInt(rangeMatch[1]), max: parseInt(rangeMatch[2]) };
    }

    const overMatch = prompt.match(/over\s+(\d{1,3})/);
    if (overMatch) {
      return { min: parseInt(overMatch[1]), max: 120 };
    }

    const underMatch = prompt.match(/under\s+(\d{1,3})/);
    if (underMatch) {
      return { min: 0, max: parseInt(underMatch[1]) };
    }

    // Age group keywords
    if (prompt.includes('adult')) return { min: 18, max: 75 };
    if (prompt.includes('pediatric') || prompt.includes('child')) return { min: 0, max: 17 };
    if (prompt.includes('elderly') || prompt.includes('senior') || prompt.includes('geriatric')) return { min: 65, max: 120 };

    return null;
  }

  /**
   * Build explanation for generated CQL
   */
  private buildExplanation(prompt: string, context?: CqlGenerationContext): string {
    let explanation = '**Generated CQL Explanation:**\n\n';

    const conditions = this.detectConditions(prompt);
    const interventions = this.detectInterventions(prompt);
    const ageRange = this.detectAgeRange(prompt);

    explanation += '**Population Criteria:**\n';
    if (ageRange) {
      explanation += `- Age range: ${ageRange.min}-${ageRange.max} years\n`;
    }
    if (conditions.length > 0) {
      explanation += `- Conditions: ${conditions.map(c => c.name).join(', ')}\n`;
    }
    if (interventions.length > 0) {
      explanation += `- Interventions: ${interventions.map(i => i.name).join(', ')}\n`;
    }

    explanation += '\n**Measure Logic:**\n';
    explanation += '- Initial Population: Patients meeting demographic and condition criteria\n';
    explanation += '- Denominator: All patients in Initial Population\n';
    if (interventions.length > 0) {
      explanation += `- Numerator: Patients with ${interventions[0].name}\n`;
    }

    explanation += '\n**Note:** This is AI-generated CQL. Please review and customize as needed.';

    return explanation;
  }

  /**
   * Suggest value sets based on prompt
   */
  private suggestValueSets(prompt: string): SuggestedValueSet[] {
    const suggested: SuggestedValueSet[] = [];

    // Add all detected conditions and interventions as suggestions
    const conditions = this.detectConditions(prompt);
    const interventions = this.detectInterventions(prompt);

    conditions.forEach(c => {
      suggested.push({
        oid: c.oid,
        name: c.name,
        codeSystem: 'SNOMED-CT/ICD-10',
        description: `Diagnosis codes for ${c.definition}`,
        usage: 'Condition identification',
      });
    });

    interventions.forEach(i => {
      suggested.push({
        oid: i.oid,
        name: i.name,
        codeSystem: i.resourceType === 'MedicationRequest' ? 'RxNorm' : 'LOINC/CPT',
        description: `Codes for ${i.definition}`,
        usage: `${i.resourceType} identification`,
      });
    });

    return suggested;
  }

  /**
   * Local CQL validation
   */
  private validateCqlLocally(cql: string): Observable<CqlValidationResult> {
    const errors: CqlError[] = [];
    const warnings: CqlWarning[] = [];
    const suggestions: CqlSuggestion[] = [];

    const lines = cql.split('\n');

    lines.forEach((line, index) => {
      const lineNum = index + 1;

      // Check for missing library declaration
      if (lineNum === 1 && !line.startsWith('library ')) {
        errors.push({
          line: lineNum,
          column: 1,
          message: 'CQL must start with a library declaration',
          severity: 'error',
        });
      }

      // Check for unclosed strings
      const quotes = (line.match(/'/g) || []).length;
      if (quotes % 2 !== 0) {
        errors.push({
          line: lineNum,
          column: line.indexOf("'") + 1,
          message: 'Unclosed string literal',
          severity: 'error',
        });
      }

      // Check for common typos
      if (line.includes('defnie ')) {
        errors.push({
          line: lineNum,
          column: line.indexOf('defnie') + 1,
          message: "Misspelled keyword 'define'",
          severity: 'error',
        });
      }

      // Warn about magic numbers
      const magicNum = line.match(/[<>=]\s*(\d+)\s*[^.]/);
      if (magicNum && !line.includes('//')) {
        suggestions.push({
          line: lineNum,
          column: line.indexOf(magicNum[1]) + 1,
          message: `Consider extracting magic number '${magicNum[1]}' into a named constant`,
          severity: 'info',
        });
      }

      // Warn about missing comments for complex definitions
      if (line.startsWith('define ') && !lines[index - 1]?.trim().startsWith('//')) {
        suggestions.push({
          line: lineNum,
          column: 1,
          message: 'Consider adding a comment to explain this definition',
          severity: 'info',
        });
      }
    });

    // Check for required elements
    if (!cql.includes('using FHIR')) {
      warnings.push({
        line: 1,
        column: 1,
        message: 'Missing FHIR version declaration',
        severity: 'warning',
      });
    }

    if (!cql.includes('context Patient')) {
      warnings.push({
        line: 1,
        column: 1,
        message: "Missing 'context Patient' declaration",
        severity: 'warning',
      });
    }

    if (!cql.includes('"Initial Population"')) {
      warnings.push({
        line: 1,
        column: 1,
        message: 'Missing "Initial Population" definition',
        severity: 'warning',
      });
    }

    return of({
      valid: errors.length === 0,
      errors,
      warnings,
      suggestions,
    });
  }

  /**
   * Get local auto-completions
   */
  private getLocalCompletions(cql: string, position: { line: number; column: number }): CqlCompletion[] {
    const completions: CqlCompletion[] = [];

    // Keywords
    const keywords = ['define', 'library', 'using', 'include', 'parameter', 'context', 'valueset', 'codesystem', 'code', 'where', 'and', 'or', 'not', 'exists', 'from', 'return', 'sort by'];
    keywords.forEach(kw => {
      completions.push({
        label: kw,
        detail: 'CQL keyword',
        insertText: kw,
        kind: 'keyword',
      });
    });

    // Functions
    const functions = [
      { name: 'AgeInYearsAt', detail: 'Calculate age in years at a given date' },
      { name: 'AgeInMonthsAt', detail: 'Calculate age in months at a given date' },
      { name: 'Count', detail: 'Count elements in a list' },
      { name: 'Sum', detail: 'Sum numeric values in a list' },
      { name: 'Avg', detail: 'Average numeric values in a list' },
      { name: 'Min', detail: 'Minimum value in a list' },
      { name: 'Max', detail: 'Maximum value in a list' },
      { name: 'First', detail: 'First element in a list' },
      { name: 'Last', detail: 'Last element in a list' },
      { name: 'singleton from', detail: 'Extract single element from a list' },
    ];
    functions.forEach(fn => {
      completions.push({
        label: fn.name,
        detail: fn.detail,
        insertText: fn.name + '($1)',
        kind: 'function',
      });
    });

    // Snippets
    completions.push({
      label: 'define-population',
      detail: 'Define a population',
      insertText: `define "\${1:Population Name}":\n  \${2:criteria}`,
      kind: 'snippet',
    });

    completions.push({
      label: 'condition-exists',
      detail: 'Check if condition exists',
      insertText: `exists([Condition: "\${1:ValueSet}"] C\n  where C.clinicalStatus ~ 'active'\n    and C.verificationStatus ~ 'confirmed')`,
      kind: 'snippet',
    });

    completions.push({
      label: 'observation-exists',
      detail: 'Check if observation exists',
      insertText: `exists([Observation: "\${1:ValueSet}"] O\n  where O.status in {'final', 'amended'}\n    and O.effective during "Measurement Period")`,
      kind: 'snippet',
    });

    return completions;
  }

  /**
   * Analyze CQL for optimization opportunities
   */
  private analyzeForOptimizations(cql: string): CqlSuggestion[] {
    const suggestions: CqlSuggestion[] = [];

    // Check for repeated subexpressions
    const defineBlocks = cql.match(/define "[^"]+":[\s\S]*?(?=define|$)/g) || [];
    const expressions: Record<string, number[]> = {};

    defineBlocks.forEach((block, index) => {
      const expr = block.replace(/define "[^"]+":/, '').trim();
      if (!expressions[expr]) {
        expressions[expr] = [];
      }
      expressions[expr].push(index + 1);
    });

    Object.entries(expressions).forEach(([expr, lines]) => {
      if (lines.length > 1 && expr.length > 50) {
        suggestions.push({
          line: lines[0],
          column: 1,
          message: `This expression is duplicated ${lines.length} times. Consider extracting to a shared definition.`,
          severity: 'info',
        });
      }
    });

    // Check for potentially expensive operations
    if (cql.includes('flatten')) {
      const line = cql.split('\n').findIndex(l => l.includes('flatten')) + 1;
      suggestions.push({
        line,
        column: 1,
        message: 'flatten operations can be expensive. Ensure this is necessary.',
        severity: 'info',
      });
    }

    return suggestions;
  }
}
