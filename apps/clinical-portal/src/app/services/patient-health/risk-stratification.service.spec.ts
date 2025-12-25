/**
 * Unit Tests for Risk Stratification Service
 */
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { RiskStratificationService } from './risk-stratification.service';
import { PhysicalHealthService } from './physical-health.service';
import { MentalHealthService } from './mental-health.service';
import { SDOHService } from './sdoh.service';
import { LoggerService } from '../logger.service';
import {
  RiskLevel,
  MultiFactorRiskScore,
  CategoryRiskAssessment,
  PhysicalHealthSummary,
  MentalHealthSummary,
  SDOHSummary,
} from '../../models/patient-health.model';

describe('RiskStratificationService', () => {
  let service: RiskStratificationService;

  const mockPhysicalHealthSummary: PhysicalHealthSummary = {
    vitals: {
      bloodPressure: { value: '120/80', systolic: 120, diastolic: 80, unit: 'mmHg', date: new Date(), status: 'normal' },
      heartRate: { value: 72, unit: 'bpm', date: new Date(), status: 'normal' },
      respiratoryRate: { value: 16, unit: '/min', date: new Date(), status: 'normal' },
      temperature: { value: 98.6, unit: '°F', date: new Date(), status: 'normal' },
      bmi: { value: 24.5, height: 70, weight: 170, unit: 'kg/m²', date: new Date(), status: 'normal' },
      oxygenSaturation: { value: 98, unit: '%', date: new Date(), status: 'normal' },
    },
    recentLabs: [],
    chronicConditions: [],
    medicationAdherence: { status: 'good', pdcScore: 0.85, lastFillDate: new Date() },
    functionalStatus: {
      adlScore: 10,
      iadlScore: 8,
      mobilityLevel: 'Independent',
      painLevel: 2,
    },
  };

  const mockMentalHealthSummary: MentalHealthSummary = {
    assessments: [],
    diagnoses: [],
    treatmentEngagement: { inTherapy: false },
    substanceUse: { overallRisk: 'low' },
    suicideRisk: { level: 'low', lastAssessed: new Date() },
    socialSupport: 'good',
    currentTreatments: [],
  };

  const mockSDOHSummary: SDOHSummary = {
    overallRisk: 'low',
    categories: {
      'food-insecurity': 'none',
      'housing-instability': 'none',
      transportation: 'none',
      employment: 'none',
      education: 'none',
      'social-isolation': 'none',
      'financial-strain': 'none',
      'healthcare-access': 'none',
    },
    activeNeeds: [],
    needs: [],
    screeningDate: new Date(),
    interventions: [],
    referrals: [],
    strengths: [],
  };

  const mockPhysicalHealthService = {
    getPhysicalHealthSummary: jest.fn().mockReturnValue(of(mockPhysicalHealthSummary)),
  };

  const mockMentalHealthService = {
    getMentalHealthSummary: jest.fn().mockReturnValue(of(mockMentalHealthSummary)),
    calculateMentalHealthRiskScore: jest.fn().mockReturnValue(20),
  };

  const mockSDOHService = {
    getSDOHSummary: jest.fn().mockReturnValue(of(mockSDOHSummary)),
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
        RiskStratificationService,
        { provide: PhysicalHealthService, useValue: mockPhysicalHealthService },
        { provide: MentalHealthService, useValue: mockMentalHealthService },
        { provide: SDOHService, useValue: mockSDOHService },
        { provide: LoggerService, useValue: mockLoggerService },
      ],
    });

    service = TestBed.inject(RiskStratificationService);

    // Reset mocks
    jest.clearAllMocks();
    mockPhysicalHealthService.getPhysicalHealthSummary.mockReturnValue(of(mockPhysicalHealthSummary));
    mockMentalHealthService.getMentalHealthSummary.mockReturnValue(of(mockMentalHealthSummary));
    mockMentalHealthService.calculateMentalHealthRiskScore.mockReturnValue(20);
    mockSDOHService.getSDOHSummary.mockReturnValue(of(mockSDOHSummary));
  });

  describe('Service Creation', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });

  describe('getRiskStratification', () => {
    it('should return risk stratification for low-risk patient', (done) => {
      service.getRiskStratification('patient-1').subscribe((result) => {
        expect(result).toBeDefined();
        expect(result.overallRisk).toBeDefined();
        expect(result.scores).toBeDefined();
        expect(result.predictions).toBeDefined();
        expect(result.categories).toBeDefined();
        done();
      });
    });

    it('should return cached result on second call', (done) => {
      // First call - populates cache
      service.getRiskStratification('patient-cache-test').subscribe(() => {
        // Reset call counts
        jest.clearAllMocks();

        // Second call - should use cache
        service.getRiskStratification('patient-cache-test').subscribe((result) => {
          expect(result).toBeDefined();
          // Should not have made new API calls
          expect(mockPhysicalHealthService.getPhysicalHealthSummary).not.toHaveBeenCalled();
          done();
        });
      });
    });

    it('should handle errors gracefully and return default stratification', (done) => {
      mockPhysicalHealthService.getPhysicalHealthSummary.mockReturnValue(
        throwError(() => new Error('API Error'))
      );

      service.getRiskStratification('patient-error').subscribe((result) => {
        expect(result).toBeDefined();
        expect(result.overallRisk).toBe('low');
        done();
      });
    });

    it('should include predictions in risk stratification', (done) => {
      service.getRiskStratification('patient-1').subscribe((result) => {
        expect(result.predictions).toBeDefined();
        expect(result.predictions.hospitalizationRisk30Day).toBeDefined();
        expect(result.predictions.hospitalizationRisk90Day).toBeDefined();
        expect(result.predictions.edVisitRisk30Day).toBeDefined();
        expect(result.predictions.readmissionRisk).toBeDefined();
        done();
      });
    });

    it('should include category-specific risks', (done) => {
      service.getRiskStratification('patient-1').subscribe((result) => {
        expect(result.categories).toBeDefined();
        expect(result.categories.diabetes).toBeDefined();
        expect(result.categories.cardiovascular).toBeDefined();
        expect(result.categories.mentalHealth).toBeDefined();
        done();
      });
    });
  });

  describe('calculateMultiFactorRiskScore', () => {
    it('should calculate multi-factor risk score with proper weights', (done) => {
      service.calculateMultiFactorRiskScore('patient-1').subscribe((result) => {
        expect(result).toBeDefined();
        expect(result.patientId).toBe('patient-1');
        expect(result.overallScore).toBeGreaterThanOrEqual(0);
        expect(result.overallScore).toBeLessThanOrEqual(100);
        expect(result.components).toBeDefined();
        expect(result.components.clinicalComplexity).toBeDefined();
        expect(result.components.sdohRisk).toBeDefined();
        expect(result.components.mentalHealthRisk).toBeDefined();
        done();
      });
    });

    it('should include weight information in result', (done) => {
      service.calculateMultiFactorRiskScore('patient-1').subscribe((result) => {
        expect(result.weights).toBeDefined();
        expect(result.weights.clinicalComplexity).toBe(0.4);
        expect(result.weights.sdohRisk).toBe(0.3);
        expect(result.weights.mentalHealthRisk).toBe(0.3);
        done();
      });
    });

    it('should include detailed breakdown in result', (done) => {
      service.calculateMultiFactorRiskScore('patient-1').subscribe((result) => {
        expect(result.details).toBeDefined();
        expect(result.details.conditionCount).toBeDefined();
        expect(result.details.uncontrolledConditionCount).toBeDefined();
        expect(result.details.sdohNeedCount).toBeDefined();
        expect(result.details.mentalHealthAssessmentCount).toBeDefined();
        done();
      });
    });

    it('should calculate higher risk for patient with chronic conditions', (done) => {
      const physicalWithConditions: PhysicalHealthSummary = {
        ...mockPhysicalHealthSummary,
        chronicConditions: [
          {
            id: '1',
            name: 'Type 2 Diabetes',
            code: 'E11',
            system: 'ICD-10',
            display: 'Type 2 diabetes mellitus',
            severity: 'moderate',
            onset: new Date(),
            isControlled: false,
            onsetDate: new Date(),
          },
          {
            id: '2',
            name: 'Hypertension',
            code: 'I10',
            system: 'ICD-10',
            display: 'Essential hypertension',
            severity: 'moderate',
            onset: new Date(),
            isControlled: false,
            onsetDate: new Date(),
          },
        ],
      };

      mockPhysicalHealthService.getPhysicalHealthSummary.mockReturnValue(of(physicalWithConditions));

      service.calculateMultiFactorRiskScore('patient-high-risk').subscribe((result) => {
        expect(result.details.conditionCount).toBe(2);
        expect(result.details.uncontrolledConditionCount).toBe(2);
        expect(result.components.clinicalComplexity).toBeGreaterThan(0);
        done();
      });
    });
  });

  describe('calculateClinicalComplexityScore', () => {
    it('should return clinical complexity components', (done) => {
      service.calculateClinicalComplexityScore('patient-1').subscribe((result) => {
        expect(result).toBeDefined();
        expect(result.total).toBeDefined();
        expect(result.comorbidityScore).toBeDefined();
        expect(result.medicationComplexity).toBeDefined();
        expect(result.functionalStatus).toBeDefined();
        done();
      });
    });

    it('should calculate higher score for patient with comorbidities', (done) => {
      const physicalWithConditions: PhysicalHealthSummary = {
        ...mockPhysicalHealthSummary,
        chronicConditions: [
          {
            id: '1',
            name: 'Type 2 Diabetes',
            code: 'E11',
            severity: 'severe',
            onset: new Date(),
            isControlled: false,
            onsetDate: new Date(),
          },
          {
            id: '2',
            name: 'Heart Failure',
            code: 'I50.9',
            severity: 'severe',
            onset: new Date(),
            isControlled: false,
            onsetDate: new Date(),
          },
        ],
      };

      mockPhysicalHealthService.getPhysicalHealthSummary.mockReturnValue(of(physicalWithConditions));

      service.calculateClinicalComplexityScore('patient-complex').subscribe((result) => {
        expect(result.comorbidityScore).toBeGreaterThan(0);
        done();
      });
    });
  });

  describe('getCategoryRiskAssessments', () => {
    it('should return category-specific risk assessments', (done) => {
      service.getCategoryRiskAssessments('patient-1').subscribe((assessments) => {
        expect(assessments).toBeDefined();
        expect(Array.isArray(assessments)).toBe(true);
        expect(assessments.length).toBeGreaterThan(0);
        done();
      });
    });

    it('should include diabetes risk assessment', (done) => {
      service.getCategoryRiskAssessments('patient-1').subscribe((assessments) => {
        const diabetesAssessment = assessments.find((a) => a.category === 'diabetes');
        expect(diabetesAssessment).toBeDefined();
        expect(diabetesAssessment?.riskLevel).toBeDefined();
        done();
      });
    });

    it('should include cardiovascular risk assessment', (done) => {
      service.getCategoryRiskAssessments('patient-1').subscribe((assessments) => {
        const cvAssessment = assessments.find((a) => a.category === 'cardiovascular');
        expect(cvAssessment).toBeDefined();
        expect(cvAssessment?.riskLevel).toBeDefined();
        done();
      });
    });

    it('should include mental health crisis risk assessment', (done) => {
      service.getCategoryRiskAssessments('patient-1').subscribe((assessments) => {
        const mhAssessment = assessments.find((a) => a.category === 'mental-health');
        expect(mhAssessment).toBeDefined();
        expect(mhAssessment?.riskLevel).toBeDefined();
        done();
      });
    });
  });

  describe('calculateDiabetesRisk', () => {
    it('should return diabetes risk assessment', (done) => {
      service.calculateDiabetesRisk('patient-1').subscribe((assessment) => {
        expect(assessment).toBeDefined();
        expect(assessment.category).toBe('diabetes');
        expect(assessment.riskLevel).toBeDefined();
        done();
      });
    });
  });

  describe('calculateRiskTrend', () => {
    it('should calculate risk trend from data points', () => {
      const dataPoints = [
        { date: new Date('2024-01-01'), value: 30, label: 'January' },
        { date: new Date('2024-06-01'), value: 40, label: 'June' },
      ];

      const trend = service.calculateRiskTrend('patient-1', 'overall-risk', dataPoints);

      expect(trend).toBeDefined();
      expect(trend.metric).toBe('overall-risk');
      expect(trend.patientId).toBe('patient-1');
    });

    it('should return stable trend for minimal data', () => {
      const dataPoints = [
        { date: new Date('2024-01-01'), value: 30, label: 'January' },
      ];

      const trend = service.calculateRiskTrend('patient-1', 'overall-risk', dataPoints);

      expect(trend.trend).toBe('stable');
      expect(trend.percentChange).toBe(0);
    });
  });

  describe('Cache Invalidation', () => {
    it('should invalidate risk cache for patient', (done) => {
      // First call - populates cache
      service.getRiskStratification('patient-invalidate').subscribe(() => {
        // Reset mocks
        jest.clearAllMocks();

        // Invalidate cache
        service.invalidatePatientRisk('patient-invalidate');

        // Third call - should make new API call
        service.getRiskStratification('patient-invalidate').subscribe(() => {
          expect(mockPhysicalHealthService.getPhysicalHealthSummary).toHaveBeenCalled();
          done();
        });
      });
    });
  });
});
