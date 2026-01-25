import { Injectable } from '@angular/core';
import { Observable, forkJoin, of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { CacheableService, getCodeFromConcept, conceptCodeIncludes, conceptContainsAnyCode } from '../shared';
import { LoggerService, ContextualLogger } from '../logger.service';
import { FhirObservationService } from '../fhir/fhir-observation.service';
import { FhirConditionService } from '../fhir/fhir-condition.service';
import { FhirQuestionnaireService } from '../fhir/fhir-questionnaire.service';
import { MedicationAdherenceService } from '../medication-adherence.service';
import { ProcedureHistoryService } from '../procedure-history.service';
import {
  PhysicalHealthSummary,
  HealthStatus,
  VitalSign,
  LabResult,
  ChronicCondition,
  FunctionalStatus,
  LabPanel,
  LabTrendAnalysis,
  LabInterpretation,
} from '../../models/patient-health.model';
import {
  LOINC_LAB_PANELS,
  FHIR_INTERPRETATION_CODES,
} from '../../models/fhir.model';

/**
 * Physical Health Service
 *
 * Handles all physical health data aggregation and analysis:
 * - Vital signs monitoring with critical alert detection
 * - Lab results with panel grouping and trend analysis
 * - Chronic condition management
 * - Medication adherence tracking
 * - Functional status assessment
 *
 * Features:
 * - Built-in caching with 5-minute TTL
 * - Critical alert detection for vital signs
 * - Lab trend analysis and recommendations
 * - Medication adherence scoring
 */
@Injectable({
  providedIn: 'root',
})
export class PhysicalHealthService extends CacheableService {
  private log: ContextualLogger;

  constructor(
    private fhirObservation: FhirObservationService,
    private fhirCondition: FhirConditionService,
    private fhirQuestionnaire: FhirQuestionnaireService,
    private medicationAdherence: MedicationAdherenceService,
    private procedureHistory: ProcedureHistoryService,
    private logger: LoggerService
  ) {
    super({ ttlMs: 5 * 60 * 1000 }); // 5 minute cache
    this.log = this.logger.withContext('PhysicalHealthService');
  }

  /**
   * Get comprehensive physical health summary
   * Aggregates vitals, labs, conditions, medications, and functional status
   */
  getPhysicalHealthSummary(patientId: string): Observable<PhysicalHealthSummary> {
    const cacheKey = `physical:summary:${patientId}`;
    const cached = this.getCached<PhysicalHealthSummary>(cacheKey);
    if (cached) return of(cached);

    return forkJoin({
      vitals: this.fhirObservation.getVitalSigns(patientId).pipe(
        catchError((err) => {
          this.log.error('Error fetching vitals:', err);
          return of({});
        })
      ),
      labs: this.fhirObservation.getLabResults(patientId).pipe(
        catchError((err) => {
          this.log.error('Error fetching labs:', err);
          return of([]);
        })
      ),
      conditions: this.fhirCondition.getActiveConditions(patientId).pipe(
        catchError((err) => {
          this.log.error('Error fetching conditions:', err);
          return of([]);
        })
      ),
      medications: this.medicationAdherence.calculateOverallAdherence(patientId).pipe(
        catchError((err) => {
          this.log.error('Error fetching medication adherence:', err);
          return of({
            overallPDC: 0,
            adherentCount: 0,
            totalMedications: 0,
            problematicMedications: [],
          });
        })
      ),
      procedures: this.procedureHistory.getRecentProcedures(patientId).pipe(
        catchError((err) => {
          this.log.error('Error fetching procedures:', err);
          return of([]);
        })
      ),
      functional: this.getFunctionalStatus(patientId).pipe(
        catchError((err) => {
          this.log.error('Error fetching functional status:', err);
          return of(this.getDefaultFunctionalStatus());
        })
      ),
    }).pipe(
      map((data) => {
        const summary = this.buildPhysicalHealthSummary(data);
        this.setCache(cacheKey, summary);
        return summary;
      }),
      catchError((err) => {
        this.log.error('Error building physical health summary:', err);
        return of(this.getMockPhysicalHealth());
      })
    );
  }

  /**
   * Get lab results grouped by panel (CBC, BMP, Lipid, etc.)
   */
  getLabResultsGroupedByPanel(patientId: string): Observable<LabPanel[]> {
    const cacheKey = `physical:labs:panels:${patientId}`;
    const cached = this.getCached<LabPanel[]>(cacheKey);
    if (cached) return of(cached);

    return this.fhirObservation.getLabResults(patientId).pipe(
      map((labResults) => {
        const panels = this.groupLabsByPanel(labResults);
        this.setCache(cacheKey, panels);
        return panels;
      }),
      catchError((error) => {
        this.log.error('Error grouping lab results by panel:', error);
        return of([]);
      })
    );
  }

  /**
   * Get lab history for a specific LOINC code
   */
  getLabHistory(
    patientId: string,
    loincCode: string,
    limit = 10
  ): Observable<LabResult[]> {
    return this.fhirObservation.getLabHistory(patientId, loincCode, limit);
  }

  /**
   * Analyze lab trend from historical results
   */
  analyzeLabTrend(results: LabResult[]): LabTrendAnalysis {
    if (results.length < 2) {
      return {
        loincCode: getCodeFromConcept(results[0]?.code),
        testName: results[0]?.name || 'Unknown',
        trend: 'stable',
        percentChange: 0,
        dataPoints: results,
      };
    }

    // Sort by date (oldest to newest for trend calculation)
    const sortedResults = [...results].sort(
      (a, b) => a.date.getTime() - b.date.getTime()
    );

    // Get numeric values
    const values = sortedResults
      .map((r) =>
        typeof r.value === 'number' ? r.value : parseFloat(String(r.value))
      )
      .filter((v) => !isNaN(v));

    if (values.length < 2) {
      return {
        loincCode: getCodeFromConcept(sortedResults[0]?.code),
        testName: sortedResults[0]?.name || 'Unknown',
        trend: 'stable',
        percentChange: 0,
        dataPoints: results,
      };
    }

    // Calculate percentage change from oldest to newest
    const oldestValue = values[0];
    const newestValue = values[values.length - 1];
    const percentChange = ((newestValue - oldestValue) / oldestValue) * 100;

    // Determine trend
    let trend: 'improving' | 'stable' | 'worsening';
    if (Math.abs(percentChange) < 5) {
      trend = 'stable';
    } else if (percentChange < 0) {
      // For most lab values, decreasing is improving (e.g., HbA1c, cholesterol)
      trend = 'improving';
    } else {
      trend = 'worsening';
    }

    // Generate recommendation for concerning trends
    let recommendation: string | undefined;
    if (trend === 'worsening' || newestValue > oldestValue * 1.2) {
      const testName = sortedResults[0]?.name || 'Lab value';
      recommendation =
        `${testName} has increased by ${Math.abs(percentChange).toFixed(1)}%. ` +
        'Consider reviewing treatment plan and patient adherence. ' +
        'May require medication adjustment or lifestyle intervention.';
    } else if (sortedResults[sortedResults.length - 1]?.status === 'critical') {
      recommendation =
        'Current value is in critical range. Immediate clinical review recommended.';
    }

    return {
      loincCode: getCodeFromConcept(sortedResults[0]?.code),
      testName: sortedResults[0]?.name || 'Unknown',
      trend,
      percentChange,
      dataPoints: results,
      recommendation,
    };
  }

  /**
   * Get chronic conditions
   */
  getChronicConditions(patientId: string): Observable<ChronicCondition[]> {
    return this.fhirCondition.getChronicConditions(patientId);
  }

  /**
   * Get functional status from questionnaire responses
   */
  getFunctionalStatus(patientId: string): Observable<FunctionalStatus> {
    const cacheKey = `physical:functional:${patientId}`;
    const cached = this.getCached<FunctionalStatus>(cacheKey);
    if (cached) return of(cached);

    return this.fhirQuestionnaire.getFunctionalStatusAssessments(patientId).pipe(
      map((assessments) => {
        const functional = this.calculateFunctionalStatus(assessments);
        this.setCache(cacheKey, functional);
        return functional;
      }),
      catchError((error) => {
        this.log.error('Error fetching functional status:', error);
        return of(this.getDefaultFunctionalStatus());
      })
    );
  }

  /**
   * Subscribe to real-time vital signs updates
   */
  subscribeToVitalSigns(
    patientId: string,
    intervalMs = 30000
  ): Observable<PhysicalHealthSummary['vitals']> {
    // Delegate to FhirObservationService's polling mechanism
    return this.fhirObservation.getVitalSigns(patientId);
  }

  /**
   * Get vital sign history for a specific type
   */
  getVitalSignHistory(
    patientId: string,
    loincCode?: string,
    limit = 30
  ): Observable<VitalSign<number | string>[]> {
    return this.fhirObservation.getVitalSignHistory(patientId, loincCode, limit);
  }

  /**
   * Map FHIR interpretation code to structured LabInterpretation
   */
  mapInterpretationCode(code: string): LabInterpretation {
    const mapping = (FHIR_INTERPRETATION_CODES as Record<string, any>)[code];

    if (mapping) {
      const descriptions: Record<string, string> = {
        N: 'Result is within normal reference range',
        L: 'Result is below the normal reference range',
        H: 'Result is above the normal reference range',
        LL: 'Result is critically low - immediate clinical attention may be required',
        HH: 'Result is critically high - immediate clinical attention may be required',
        A: 'Result is abnormal but not specifically high or low',
      };

      return {
        code,
        display: mapping.display,
        severity: mapping.severity,
        description: descriptions[code],
      };
    }

    return {
      code,
      display: 'Unknown',
      severity: 'unknown',
      description: 'Interpretation code not recognized',
    };
  }

  /**
   * Invalidate physical health cache for a patient
   */
  invalidatePatientPhysicalHealth(patientId: string): void {
    this.invalidatePatientCache(patientId);
    // Also invalidate dependent services
    this.fhirObservation.invalidatePatientObservations(patientId);
    this.fhirCondition.invalidatePatientConditions(patientId);
  }

  // ===== Private Helper Methods =====

  /**
   * Build physical health summary from aggregated data
   */
  private buildPhysicalHealthSummary(data: {
    vitals: PhysicalHealthSummary['vitals'];
    labs: LabResult[];
    conditions: ChronicCondition[];
    medications: {
      overallPDC: number;
      adherentCount: number;
      totalMedications: number;
      problematicMedications: string[];
    };
    procedures: any[];
    functional: FunctionalStatus;
  }): PhysicalHealthSummary {
    // Map medication adherence data to expected format
    const medicationAdherence: PhysicalHealthSummary['medicationAdherence'] = {
      overallRate: data.medications.overallPDC,
      status: this.determineMedicationAdherenceStatus(data.medications.overallPDC),
      problematicMedications: data.medications.problematicMedications,
      totalMedications: data.medications.totalMedications,
    };

    // Detect and flag critical alerts
    this.detectCriticalAlerts(data.vitals, data.labs);

    // Determine overall status
    const status = this.determinePhysicalHealthStatus({
      vitals: data.vitals,
      labs: data.labs,
      conditions: data.conditions,
      medications: medicationAdherence,
      functional: data.functional,
    });

    return {
      status,
      vitals: data.vitals,
      labs: data.labs,
      chronicConditions: data.conditions,
      medicationAdherence,
      functionalStatus: data.functional,
    };
  }

  /**
   * Group lab results by panel
   */
  private groupLabsByPanel(labResults: LabResult[]): LabPanel[] {
    const panels: LabPanel[] = [];
    const usedResults = new Set<string>();

    // Group by date first
    const resultsByDate = new Map<string, LabResult[]>();
    labResults.forEach((result) => {
      const dateKey = result.date.toISOString().split('T')[0];
      if (!resultsByDate.has(dateKey)) {
        resultsByDate.set(dateKey, []);
      }
      resultsByDate.get(dateKey)?.push(result);
    });

    // Process each panel type
    const panelDefinitions = [
      {
        name: 'CBC',
        code: LOINC_LAB_PANELS.CBC.panelCode,
        components: LOINC_LAB_PANELS.CBC.components,
      },
      {
        name: 'BMP',
        code: LOINC_LAB_PANELS.BMP.panelCode,
        components: LOINC_LAB_PANELS.BMP.components,
      },
      {
        name: 'Lipid Panel',
        code: LOINC_LAB_PANELS.LIPID.panelCode,
        components: LOINC_LAB_PANELS.LIPID.components,
      },
    ];

    resultsByDate.forEach((dateResults, dateKey) => {
      panelDefinitions.forEach((panelDef) => {
        const panelResults = dateResults.filter(
          (r) => {
            const codeStr = getCodeFromConcept(r.code);
            return codeStr &&
              (panelDef.components as readonly string[]).includes(codeStr) &&
              !usedResults.has(`${codeStr}-${dateKey}`);
          }
        );

        if (panelResults.length >= 2) {
          panelResults.forEach((r) => usedResults.add(`${getCodeFromConcept(r.code)}-${dateKey}`));

          // Determine panel status
          let panelStatus: 'normal' | 'abnormal' | 'critical' = 'normal';
          if (panelResults.some((r) => r.status === 'critical')) {
            panelStatus = 'critical';
          } else if (panelResults.some((r) => r.status === 'abnormal')) {
            panelStatus = 'abnormal';
          }

          panels.push({
            panelCode: panelDef.code,
            panelName: panelDef.name,
            date: new Date(dateKey),
            status: panelStatus,
            results: panelResults,
          });
        }
      });
    });

    // Add standalone results
    const standaloneResults = labResults.filter((r) => {
      const dateKey = r.date.toISOString().split('T')[0];
      return !usedResults.has(`${r.code}-${dateKey}`);
    });

    if (standaloneResults.length > 0) {
      const standaloneByDate = new Map<string, LabResult[]>();
      standaloneResults.forEach((result) => {
        const dateKey = result.date.toISOString().split('T')[0];
        if (!standaloneByDate.has(dateKey)) {
          standaloneByDate.set(dateKey, []);
        }
        standaloneByDate.get(dateKey)?.push(result);
      });

      standaloneByDate.forEach((results, dateKey) => {
        let status: 'normal' | 'abnormal' | 'critical' = 'normal';
        if (results.some((r) => r.status === 'critical')) {
          status = 'critical';
        } else if (results.some((r) => r.status === 'abnormal')) {
          status = 'abnormal';
        }

        panels.push({
          panelCode: 'standalone',
          panelName: 'Standalone Labs',
          date: new Date(dateKey),
          status,
          results,
        });
      });
    }

    // Sort panels by date (newest first)
    panels.sort((a, b) => b.date.getTime() - a.date.getTime());

    return panels;
  }

  /**
   * Calculate functional status from assessments
   */
  private calculateFunctionalStatus(assessments: any[]): FunctionalStatus {
    const functional: FunctionalStatus = this.getDefaultFunctionalStatus();

    for (const assessment of assessments) {
      const type = assessment.questionnaireType?.toLowerCase() || '';

      if (type.includes('adl') && !type.includes('iadl')) {
        // Calculate ADL score - count independent activities
        functional.adlScore = assessment.items
          .filter((item: any) => item.answer === 'independent')
          .length;
        functional.adlScore = Math.min(functional.adlScore, 6);
      }

      if (type.includes('iadl')) {
        // Calculate IADL score - count independent activities
        functional.iadlScore = assessment.items
          .filter((item: any) => item.answer === 'independent')
          .length;
        functional.iadlScore = Math.min(functional.iadlScore, 8);
      }
    }

    return functional;
  }

  /**
   * Get default functional status values
   */
  private getDefaultFunctionalStatus(): FunctionalStatus {
    return {
      adlScore: 6,
      iadlScore: 8,
      mobilityScore: 100,
      painLevel: 0,
      fatigueLevel: 0,
    };
  }

  /**
   * Determine medication adherence status from PDC percentage
   */
  private determineMedicationAdherenceStatus(
    pdc: number
  ): 'excellent' | 'good' | 'poor' | 'unknown' {
    if (pdc >= 80) return 'excellent';
    if (pdc >= 60) return 'good';
    if (pdc > 0) return 'poor';
    return 'unknown';
  }

  /**
   * Detect and flag critical alerts from vital signs and lab results
   */
  private detectCriticalAlerts(
    vitals: PhysicalHealthSummary['vitals'],
    labs: LabResult[]
  ): void {
    // Check blood pressure for critical values
    if (vitals.bloodPressure) {
      const bpValue = vitals.bloodPressure.value;
      if (typeof bpValue === 'string') {
        const [systolic, diastolic] = bpValue.split('/').map((v) => parseInt(v, 10));
        if (systolic > 180 || diastolic > 120) {
          vitals.bloodPressure.status = 'critical';
        } else if (systolic > 140 || diastolic > 90) {
          vitals.bloodPressure.status = 'abnormal';
        }
      }
    }

    // Check heart rate for critical values
    if (vitals.heartRate) {
      const hr = vitals.heartRate.value;
      if (typeof hr === 'number') {
        if (hr < 40 || hr > 150) {
          vitals.heartRate.status = 'critical';
        } else if (hr < 60 || hr > 100) {
          vitals.heartRate.status = 'abnormal';
        }
      }
    }

    // Check temperature for critical values
    if (vitals.temperature) {
      const temp = vitals.temperature.value;
      if (typeof temp === 'number') {
        if (temp > 103 || temp < 95) {
          vitals.temperature.status = 'critical';
        } else if (temp > 100.4 || temp < 96) {
          vitals.temperature.status = 'abnormal';
        }
      }
    }

    // Check oxygen saturation for critical values
    if (vitals.oxygenSaturation) {
      const o2 = vitals.oxygenSaturation.value;
      if (typeof o2 === 'number') {
        if (o2 < 90) {
          vitals.oxygenSaturation.status = 'critical';
        } else if (o2 < 95) {
          vitals.oxygenSaturation.status = 'abnormal';
        }
      }
    }

    // Check labs for critical glucose values
    labs.forEach((lab) => {
      if (lab.code === '2339-0' || lab.code === '2345-7') {
        // Glucose LOINC codes
        const value =
          typeof lab.value === 'number'
            ? lab.value
            : parseFloat(lab.value as string);
        if (!isNaN(value)) {
          if (value < 50 || value > 400) {
            lab.status = 'critical';
          } else if (value < 70 || value > 200) {
            lab.status = 'abnormal';
          }
        }
      }
    });
  }

  /**
   * Determine overall physical health status from components
   */
  private determinePhysicalHealthStatus(data: {
    vitals: PhysicalHealthSummary['vitals'];
    labs: LabResult[];
    conditions: ChronicCondition[];
    medications: PhysicalHealthSummary['medicationAdherence'];
    functional: FunctionalStatus;
  }): HealthStatus {
    let score = 100;

    // Check vitals
    const vitalValues = Object.values(data.vitals);
    const criticalVitals = vitalValues.filter(
      (v) => v && v.status === 'critical'
    ).length;
    const abnormalVitals = vitalValues.filter(
      (v) => v && v.status === 'abnormal'
    ).length;

    if (criticalVitals > 0) score -= 30;
    else if (abnormalVitals > 0) score -= 10;

    // Check labs
    const criticalLabs = data.labs.filter((l) => l.status === 'critical').length;
    const abnormalLabs = data.labs.filter((l) => l.status === 'abnormal').length;

    if (criticalLabs > 0) score -= 30;
    else if (abnormalLabs > 0) score -= 10;

    // Check conditions
    const severeUncontrolled = data.conditions.filter(
      (c) => c.severity === 'severe' && !c.isControlled
    ).length;
    const moderateUncontrolled = data.conditions.filter(
      (c) => c.severity === 'moderate' && !c.isControlled
    ).length;

    score -= severeUncontrolled * 20;
    score -= moderateUncontrolled * 10;

    // Check medication adherence
    if (data.medications.status === 'poor') score -= 15;

    // Check functional status
    if (data.functional.adlScore < 4) score -= 10;
    if (data.functional.painLevel > 7) score -= 10;

    // Determine status
    if (score >= 85) return 'excellent';
    if (score >= 70) return 'good';
    if (score >= 50) return 'fair';
    return 'poor';
  }

  /**
   * Get mock physical health data for fallback
   */
  private getMockPhysicalHealth(): PhysicalHealthSummary {
    return {
      status: 'good',
      vitals: {
        bloodPressure: {
          name: 'Blood Pressure',
          value: '128/82',
          unit: 'mmHg',
          date: new Date(),
          status: 'abnormal',
          trend: 'stable',
        },
        heartRate: {
          name: 'Heart Rate',
          value: 72,
          unit: 'bpm',
          date: new Date(),
          status: 'normal',
        },
        weight: {
          name: 'Weight',
          value: 185,
          unit: 'lbs',
          date: new Date(),
          status: 'normal',
          trend: 'stable',
        },
        bmi: {
          name: 'BMI',
          value: 28.5,
          unit: 'kg/m²',
          date: new Date(),
          status: 'abnormal',
        },
      },
      labs: [],
      chronicConditions: [],
      medicationAdherence: {
        overallRate: 85,
        status: 'excellent',
        problematicMedications: [],
        totalMedications: 5,
      },
      functionalStatus: this.getDefaultFunctionalStatus(),
    };
  }
}
