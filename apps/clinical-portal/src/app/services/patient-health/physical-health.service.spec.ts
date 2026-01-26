/**
 * Unit Tests for Physical Health Service
 */
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { PhysicalHealthService } from './physical-health.service';
import { FhirObservationService } from '../fhir/fhir-observation.service';
import { FhirConditionService } from '../fhir/fhir-condition.service';
import { FhirQuestionnaireService } from '../fhir/fhir-questionnaire.service';
import { MedicationAdherenceService } from '../medication-adherence.service';
import { ProcedureHistoryService } from '../procedure-history.service';
import { LoggerService } from '../logger.service';
import {
  LabResult,
  LabTrendAnalysis,
  PhysicalHealthSummary,
  ChronicCondition,
} from '../../models/patient-health.model';

describe('PhysicalHealthService', () => {
  let service: PhysicalHealthService;

  const mockVitals = {
    bloodPressure: { value: '120/80', systolic: 120, diastolic: 80, unit: 'mmHg', date: new Date(), status: 'normal' as const },
    heartRate: { value: 72, unit: 'bpm', date: new Date(), status: 'normal' as const },
    respiratoryRate: { value: 16, unit: '/min', date: new Date(), status: 'normal' as const },
    temperature: { value: 98.6, unit: '°F', date: new Date(), status: 'normal' as const },
    bmi: { value: 24.5, height: 70, weight: 170, unit: 'kg/m²', date: new Date(), status: 'normal' as const },
    oxygenSaturation: { value: 98, unit: '%', date: new Date(), status: 'normal' as const },
  };

  const mockFhirObservationService = {
    getVitalSigns: jest.fn().mockReturnValue(of(mockVitals)),
    getLabResults: jest.fn().mockReturnValue(of([])),
    getLabHistory: jest.fn().mockReturnValue(of([])),
    getVitalSignHistory: jest.fn().mockReturnValue(of([])),
    invalidatePatientObservations: jest.fn(),
  };

  const mockFhirConditionService = {
    getActiveConditions: jest.fn().mockReturnValue(of([])),
    getChronicConditions: jest.fn().mockReturnValue(of([])),
    getMentalHealthConditions: jest.fn().mockReturnValue(of([])),
    invalidatePatientConditions: jest.fn(),
  };

  const mockFhirQuestionnaireService = {
    getFunctionalStatusAssessments: jest.fn().mockReturnValue(of([])),
  };

  const mockMedicationAdherenceService = {
    calculateOverallAdherence: jest.fn().mockReturnValue(
      of({ overallPDC: 0.85, adherentCount: 5, totalMedications: 6, problematicMedications: [] })
    ),
  };

  const mockProcedureHistoryService = {
    getRecentProcedures: jest.fn().mockReturnValue(of([])),
  };

  const mockLoggerService = {
    withContext: jest.fn().mockReturnValue({
      info: jest.fn(),
      debug: jest.fn(),
      error: jest.fn(),
      warn: jest.fn(),
    }),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PhysicalHealthService,
        { provide: FhirObservationService, useValue: mockFhirObservationService },
        { provide: FhirConditionService, useValue: mockFhirConditionService },
        { provide: FhirQuestionnaireService, useValue: mockFhirQuestionnaireService },
        { provide: MedicationAdherenceService, useValue: mockMedicationAdherenceService },
        { provide: ProcedureHistoryService, useValue: mockProcedureHistoryService },
        { provide: LoggerService, useValue: mockLoggerService },
      ],
    });

    service = TestBed.inject(PhysicalHealthService);

    // Reset mocks
    jest.clearAllMocks();
    mockFhirObservationService.getVitalSigns.mockReturnValue(of(mockVitals));
    mockFhirObservationService.getLabResults.mockReturnValue(of([]));
    mockFhirConditionService.getActiveConditions.mockReturnValue(of([]));
    mockMedicationAdherenceService.calculateOverallAdherence.mockReturnValue(
      of({ overallPDC: 0.85, adherentCount: 5, totalMedications: 6, problematicMedications: [] })
    );
    mockProcedureHistoryService.getRecentProcedures.mockReturnValue(of([]));
  });

  describe('Service Creation', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });

  describe('analyzeLabTrend', () => {
    it('should return stable trend for single result', () => {
      const results: LabResult[] = [
        {
          id: '1',
          name: 'HbA1c',
          code: '4548-4',
          value: 6.5,
          unit: '%',
          date: new Date(),
          status: 'normal',
        },
      ];

      const trend = service.analyzeLabTrend(results);

      expect(trend.trend).toBe('stable');
      expect(trend.percentChange).toBe(0);
      expect(trend.testName).toBe('HbA1c');
    });

    it('should detect improving trend when values decrease', () => {
      const results: LabResult[] = [
        { id: '1', name: 'HbA1c', code: '4548-4', value: 8.0, unit: '%', date: new Date('2024-01-01'), status: 'abnormal' },
        { id: '2', name: 'HbA1c', code: '4548-4', value: 6.5, unit: '%', date: new Date('2024-06-01'), status: 'normal' },
      ];

      const trend = service.analyzeLabTrend(results);

      expect(trend.trend).toBe('improving');
      expect(trend.percentChange).toBeCloseTo(-18.75, 1);
    });

    it('should detect worsening trend when values increase', () => {
      const results: LabResult[] = [
        { id: '1', name: 'HbA1c', code: '4548-4', value: 6.0, unit: '%', date: new Date('2024-01-01'), status: 'normal' },
        { id: '2', name: 'HbA1c', code: '4548-4', value: 8.0, unit: '%', date: new Date('2024-06-01'), status: 'abnormal' },
      ];

      const trend = service.analyzeLabTrend(results);

      expect(trend.trend).toBe('worsening');
      expect(trend.percentChange).toBeCloseTo(33.33, 1);
      expect(trend.recommendation).toBeDefined();
    });

    it('should return stable trend for small changes', () => {
      const results: LabResult[] = [
        { id: '1', name: 'Glucose', code: '2345-7', value: 100, unit: 'mg/dL', date: new Date('2024-01-01'), status: 'normal' },
        { id: '2', name: 'Glucose', code: '2345-7', value: 102, unit: 'mg/dL', date: new Date('2024-06-01'), status: 'normal' },
      ];

      const trend = service.analyzeLabTrend(results);

      expect(trend.trend).toBe('stable');
      expect(Math.abs(trend.percentChange)).toBeLessThan(5);
    });

    it('should handle unsorted data correctly', () => {
      const results: LabResult[] = [
        { id: '2', name: 'HbA1c', code: '4548-4', value: 6.0, unit: '%', date: new Date('2024-06-01'), status: 'normal' },
        { id: '1', name: 'HbA1c', code: '4548-4', value: 8.0, unit: '%', date: new Date('2024-01-01'), status: 'abnormal' },
      ];

      const trend = service.analyzeLabTrend(results);

      // Should sort and show improving since value went from 8.0 to 6.0
      expect(trend.trend).toBe('improving');
    });

    it('should generate recommendation for critical values', () => {
      const results: LabResult[] = [
        { id: '1', name: 'Glucose', code: '2345-7', value: 400, unit: 'mg/dL', date: new Date('2024-01-01'), status: 'abnormal' },
        { id: '2', name: 'Glucose', code: '2345-7', value: 500, unit: 'mg/dL', date: new Date('2024-06-01'), status: 'critical' },
      ];

      const trend = service.analyzeLabTrend(results);

      expect(trend.trend).toBe('worsening');
      expect(trend.recommendation).toBeDefined();
    });

    it('should handle string values by parsing them', () => {
      const results: LabResult[] = [
        { id: '1', name: 'Glucose', code: '2345-7', value: '100' as any, unit: 'mg/dL', date: new Date('2024-01-01'), status: 'normal' },
        { id: '2', name: 'Glucose', code: '2345-7', value: '120' as any, unit: 'mg/dL', date: new Date('2024-06-01'), status: 'normal' },
      ];

      const trend = service.analyzeLabTrend(results);

      expect(trend.trend).toBe('worsening');
      expect(trend.percentChange).toBeCloseTo(20, 1);
    });
  });

  describe('getPhysicalHealthSummary', () => {
    it('should aggregate physical health data', (done) => {
      service.getPhysicalHealthSummary('patient-1').subscribe((summary) => {
        expect(summary).toBeDefined();
        expect(summary.vitals).toBeDefined();
        expect(mockFhirObservationService.getVitalSigns).toHaveBeenCalledWith('patient-1');
        expect(mockFhirObservationService.getLabResults).toHaveBeenCalledWith('patient-1');
        expect(mockFhirConditionService.getActiveConditions).toHaveBeenCalledWith('patient-1');
        done();
      }, 30000);
    });

    it('should return cached result on second call', (done) => {
      // First call - populates cache
      service.getPhysicalHealthSummary('patient-cache-test').subscribe(() => {
        // Reset call counts
        jest.clearAllMocks();

        // Second call - should use cache
        service.getPhysicalHealthSummary('patient-cache-test').subscribe((summary) => {
          expect(summary).toBeDefined();
          // Should not have made new API calls
          expect(mockFhirObservationService.getVitalSigns).not.toHaveBeenCalled();
          done();
        }, 30000);
      });
    });

    it('should handle errors gracefully', (done) => {
      mockFhirObservationService.getVitalSigns.mockReturnValue(throwError(() => new Error('API Error')));

      service.getPhysicalHealthSummary('patient-error').subscribe((summary) => {
        // Should still return a summary with empty/default vitals
        expect(summary).toBeDefined();
        done();
      }, 30000);
    });
  });

  describe('getLabHistory', () => {
    it('should delegate to FHIR observation service', (done) => {
      const mockLabs: LabResult[] = [
        { id: '1', name: 'HbA1c', code: '4548-4', value: 6.5, unit: '%', date: new Date(), status: 'normal' },
        { id: '2', name: 'HbA1c', code: '4548-4', value: 6.8, unit: '%', date: new Date(), status: 'normal' },
      ];
      mockFhirObservationService.getLabHistory.mockReturnValue(of(mockLabs));

      service.getLabHistory('patient-1', '4548-4', 10).subscribe((labs) => {
        expect(labs).toEqual(mockLabs);
        expect(mockFhirObservationService.getLabHistory).toHaveBeenCalledWith('patient-1', '4548-4', 10);
        done();
      }, 30000);
    });
  });

  describe('getChronicConditions', () => {
    it('should delegate to FHIR condition service', (done) => {
      const mockConditions: ChronicCondition[] = [
        {
          id: '1',
          name: 'Type 2 Diabetes',
          code: 'E11',
          system: 'ICD-10',
          display: 'Type 2 diabetes mellitus',
          severity: 'moderate',
          onset: new Date('2020-01-01'),
          isControlled: true,
          onsetDate: new Date('2020-01-01'),
        },
      ];
      mockFhirConditionService.getChronicConditions.mockReturnValue(of(mockConditions));

      service.getChronicConditions('patient-1').subscribe((conditions) => {
        expect(conditions).toEqual(mockConditions);
        expect(mockFhirConditionService.getChronicConditions).toHaveBeenCalledWith('patient-1');
        done();
      }, 30000);
    });
  });

  describe('getVitalSignHistory', () => {
    it('should delegate to FHIR observation service', (done) => {
      const mockVitalHistory = [
        { value: 120, unit: 'mmHg', date: new Date(), status: 'normal' as const },
        { value: 118, unit: 'mmHg', date: new Date(), status: 'normal' as const },
      ];
      mockFhirObservationService.getVitalSignHistory.mockReturnValue(of(mockVitalHistory));

      service.getVitalSignHistory('patient-1', '8480-6', 20).subscribe((history) => {
        expect(history).toEqual(mockVitalHistory);
        expect(mockFhirObservationService.getVitalSignHistory).toHaveBeenCalledWith('patient-1', '8480-6', 20);
        done();
      }, 30000);
    });

    it('should use default limit of 30', (done) => {
      mockFhirObservationService.getVitalSignHistory.mockReturnValue(of([]));

      service.getVitalSignHistory('patient-1').subscribe(() => {
        expect(mockFhirObservationService.getVitalSignHistory).toHaveBeenCalledWith('patient-1', undefined, 30);
        done();
      }, 30000);
    });
  });

  describe('mapInterpretationCode', () => {
    it('should map normal interpretation code', () => {
      const result = service.mapInterpretationCode('N');

      expect(result.code).toBe('N');
      expect(result.display).toBeDefined();
    });

    it('should map high interpretation code', () => {
      const result = service.mapInterpretationCode('H');

      expect(result.code).toBe('H');
    });

    it('should map low interpretation code', () => {
      const result = service.mapInterpretationCode('L');

      expect(result.code).toBe('L');
    });

    it('should map critically high interpretation code', () => {
      const result = service.mapInterpretationCode('HH');

      expect(result.code).toBe('HH');
    });

    it('should handle unknown code', () => {
      const result = service.mapInterpretationCode('UNKNOWN');

      expect(result).toBeDefined();
    });
  });

  describe('Cache Invalidation', () => {
    it('should invalidate physical health cache for patient', (done) => {
      // First call - populates cache
      service.getPhysicalHealthSummary('patient-invalidate').subscribe(() => {
        // Reset mocks
        jest.clearAllMocks();

        // Invalidate cache
        service.invalidatePatientPhysicalHealth('patient-invalidate');

        // Third call - should make new API call
        service.getPhysicalHealthSummary('patient-invalidate').subscribe(() => {
          expect(mockFhirObservationService.getVitalSigns).toHaveBeenCalled();
          done();
        }, 30000);
      });
    });
  });
});
