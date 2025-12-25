/**
 * Unit Tests for Patient Health Facade
 *
 * Tests the unified API that maintains backward compatibility
 * while delegating to specialized domain services.
 */
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { PatientHealthFacade, PatientHealthService } from './patient-health.facade';
import { PhysicalHealthService } from './physical-health.service';
import { MentalHealthService } from './mental-health.service';
import { SDOHService } from './sdoh.service';
import { RiskStratificationService } from './risk-stratification.service';
import { HealthScoringService } from './health-scoring.service';
import { LoggerService } from '../logger.service';
import {
  PhysicalHealthSummary,
  MentalHealthSummary,
  SDOHSummary,
  RiskStratification,
  HealthScore,
} from '../../models/patient-health.model';

describe('PatientHealthFacade', () => {
  let facade: PatientHealthFacade;

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
    functionalStatus: { adlScore: 10, iadlScore: 8, mobilityLevel: 'Independent', painLevel: 2 },
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
    screeningDate: new Date(),
    interventions: [],
    referrals: [],
    strengths: [],
  };

  const mockRiskStratification: RiskStratification = {
    overallRisk: 'low',
    scores: {
      clinicalComplexity: 20,
      socialComplexity: 10,
      mentalHealthRisk: 15,
      utilizationRisk: 10,
      costRisk: 10,
    },
    predictions: {
      hospitalizationRisk30Day: 0.05,
      hospitalizationRisk90Day: 0.08,
      edVisitRisk30Day: 0.03,
      readmissionRisk: 0.02,
    },
    categories: {
      diabetes: 'low',
      cardiovascular: 'low',
      respiratory: 'low',
      mentalHealth: 'low',
      fallRisk: 'low',
    },
  };

  const mockHealthScore: HealthScore = {
    patientId: 'patient-1',
    overallScore: 85,
    score: 85,
    status: 'excellent',
    trend: 'stable',
    components: {
      physical: 90,
      mental: 80,
      social: 85,
      preventive: 80,
      chronicDisease: 90,
    },
    calculatedAt: new Date(),
    lastCalculated: new Date(),
  };

  const mockPhysicalHealthService = {
    getPhysicalHealthSummary: jest.fn().mockReturnValue(of(mockPhysicalHealthSummary)),
    getLabResultsGroupedByPanel: jest.fn().mockReturnValue(of([])),
    getLabHistory: jest.fn().mockReturnValue(of([])),
    analyzeLabTrend: jest.fn().mockReturnValue({ trend: 'stable', percentChange: 0 }),
    getChronicConditions: jest.fn().mockReturnValue(of([])),
    getVitalSignHistory: jest.fn().mockReturnValue(of([])),
    invalidatePatientPhysicalHealth: jest.fn(),
  };

  const mockMentalHealthService = {
    getMentalHealthSummary: jest.fn().mockReturnValue(of(mockMentalHealthSummary)),
    getAssessmentHistory: jest.fn().mockReturnValue(of([])),
    submitMentalHealthAssessment: jest.fn().mockReturnValue(of({ type: 'PHQ-9', score: 10 })),
    getMentalHealthAssessmentHistory: jest.fn().mockReturnValue(of([])),
    calculateMentalHealthTrend: jest.fn().mockReturnValue('stable'),
    calculateDetailedMentalHealthTrend: jest.fn().mockReturnValue({ direction: 'stable' }),
    scorePHQ9: jest.fn().mockReturnValue({ type: 'PHQ-9', score: 10, severity: 'moderate' }),
    scoreGAD7: jest.fn().mockReturnValue({ type: 'GAD-7', score: 8, severity: 'mild' }),
    scorePHQ2: jest.fn().mockReturnValue({ type: 'PHQ-2', score: 2, severity: 'minimal' }),
    invalidatePatientMentalHealth: jest.fn(),
  };

  const mockSDOHService = {
    getSDOHSummary: jest.fn().mockReturnValue(of(mockSDOHSummary)),
    getSocialDeterminants: jest.fn().mockReturnValue(of({})),
    getSDOHScreeningFromFhir: jest.fn().mockReturnValue(of({})),
    calculateSDOHRiskScore: jest.fn().mockReturnValue(of({ score: 10, level: 'low' })),
    identifySDOHInterventionNeeds: jest.fn().mockReturnValue(of([])),
    mapSDOHCategoryToZCode: jest.fn().mockReturnValue('Z59.9'),
    invalidatePatientSDOH: jest.fn(),
  };

  const mockRiskStratificationService = {
    getRiskStratification: jest.fn().mockReturnValue(of(mockRiskStratification)),
    calculateMultiFactorRiskScore: jest.fn().mockReturnValue(of({ overallScore: 25, overallRisk: 'low' })),
    calculateClinicalComplexityScore: jest.fn().mockReturnValue(of({ total: 20 })),
    getCategoryRiskAssessments: jest.fn().mockReturnValue(of([])),
    calculateDiabetesRisk: jest.fn().mockReturnValue(of({ category: 'diabetes', riskLevel: 'low' })),
    calculateCardiovascularRisk: jest.fn().mockReturnValue(of({ category: 'cardiovascular', riskLevel: 'low' })),
    calculateMentalHealthCrisisRisk: jest.fn().mockReturnValue(of({ category: 'mental-health', riskLevel: 'low' })),
    calculateRiskTrend: jest.fn().mockReturnValue({ direction: 'stable' }),
    invalidatePatientRisk: jest.fn(),
  };

  const mockHealthScoringService = {
    getHealthScore: jest.fn().mockReturnValue(of(mockHealthScore)),
    getHealthScoreHistory: jest.fn().mockReturnValue(of([])),
    getHealthScoreHistoryDetailed: jest.fn().mockReturnValue(of([])),
    calculateHealthScoreTrend: jest.fn().mockReturnValue({ direction: 'stable' }),
    calculateWeightedHealthScore: jest.fn().mockReturnValue(85),
    determineHealthStatus: jest.fn().mockReturnValue('excellent'),
    invalidateHealthScoreCache: jest.fn(),
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
        PatientHealthFacade,
        { provide: PhysicalHealthService, useValue: mockPhysicalHealthService },
        { provide: MentalHealthService, useValue: mockMentalHealthService },
        { provide: SDOHService, useValue: mockSDOHService },
        { provide: RiskStratificationService, useValue: mockRiskStratificationService },
        { provide: HealthScoringService, useValue: mockHealthScoringService },
        { provide: LoggerService, useValue: mockLoggerService },
      ],
    });

    facade = TestBed.inject(PatientHealthFacade);

    // Reset mocks
    jest.clearAllMocks();
  });

  describe('Service Creation', () => {
    it('should be created', () => {
      expect(facade).toBeTruthy();
    });

    it('should be available as PatientHealthService for backward compatibility', () => {
      const service = TestBed.inject(PatientHealthService);
      expect(service).toBeTruthy();
    });
  });

  describe('getPatientHealthOverview', () => {
    it('should aggregate data from all health domains', (done) => {
      facade.getPatientHealthOverview('patient-1').subscribe((overview) => {
        expect(overview).toBeDefined();
        expect(overview.patientId).toBe('patient-1');
        expect(overview.physicalHealth).toBeDefined();
        expect(overview.mentalHealth).toBeDefined();
        expect(overview.socialDeterminants).toBeDefined();
        expect(overview.riskStratification).toBeDefined();
        expect(overview.overallHealthScore).toBeDefined();
        done();
      });
    });

    it('should call all specialized services', (done) => {
      facade.getPatientHealthOverview('patient-1').subscribe(() => {
        expect(mockPhysicalHealthService.getPhysicalHealthSummary).toHaveBeenCalledWith('patient-1');
        expect(mockMentalHealthService.getMentalHealthSummary).toHaveBeenCalledWith('patient-1');
        expect(mockSDOHService.getSDOHSummary).toHaveBeenCalledWith('patient-1');
        expect(mockRiskStratificationService.getRiskStratification).toHaveBeenCalledWith('patient-1');
        expect(mockHealthScoringService.getHealthScore).toHaveBeenCalledWith('patient-1');
        done();
      });
    });
  });

  describe('Physical Health Delegations', () => {
    it('should delegate getPhysicalHealthSummary', (done) => {
      facade.getPhysicalHealthSummary('patient-1').subscribe((result) => {
        expect(result).toBeDefined();
        expect(mockPhysicalHealthService.getPhysicalHealthSummary).toHaveBeenCalledWith('patient-1');
        done();
      });
    });

    it('should delegate getLabResultsGroupedByPanel', (done) => {
      facade.getLabResultsGroupedByPanel('patient-1').subscribe(() => {
        expect(mockPhysicalHealthService.getLabResultsGroupedByPanel).toHaveBeenCalledWith('patient-1');
        done();
      });
    });

    it('should delegate getLabHistory', (done) => {
      facade.getLabHistory('patient-1', '4548-4', 10).subscribe(() => {
        expect(mockPhysicalHealthService.getLabHistory).toHaveBeenCalledWith('patient-1', '4548-4', 10);
        done();
      });
    });

    it('should delegate analyzeLabTrend', () => {
      const results = [{ id: '1', name: 'HbA1c', code: '4548-4', value: 6.5, unit: '%', date: new Date(), status: 'normal' as const }];
      facade.analyzeLabTrend(results);
      expect(mockPhysicalHealthService.analyzeLabTrend).toHaveBeenCalledWith(results);
    });

    it('should delegate getChronicConditions', (done) => {
      facade.getChronicConditions('patient-1').subscribe(() => {
        expect(mockPhysicalHealthService.getChronicConditions).toHaveBeenCalledWith('patient-1');
        done();
      });
    });

    it('should delegate getVitalSignHistory', (done) => {
      facade.getVitalSignHistory('patient-1', '8480-6', 30).subscribe(() => {
        expect(mockPhysicalHealthService.getVitalSignHistory).toHaveBeenCalledWith('patient-1', '8480-6', 30);
        done();
      });
    });
  });

  describe('Mental Health Delegations', () => {
    it('should delegate getMentalHealthSummary', (done) => {
      facade.getMentalHealthSummary('patient-1').subscribe(() => {
        expect(mockMentalHealthService.getMentalHealthSummary).toHaveBeenCalledWith('patient-1');
        done();
      });
    });

    it('should delegate getAssessmentHistory', (done) => {
      facade.getAssessmentHistory('patient-1', 'PHQ-9').subscribe(() => {
        expect(mockMentalHealthService.getAssessmentHistory).toHaveBeenCalledWith('patient-1', 'PHQ-9');
        done();
      });
    });

    it('should delegate submitMentalHealthAssessment', (done) => {
      const responses = { q1: 1, q2: 2 };
      facade.submitMentalHealthAssessment('patient-1', 'PHQ-9', responses).subscribe(() => {
        expect(mockMentalHealthService.submitMentalHealthAssessment).toHaveBeenCalledWith(
          'patient-1',
          'PHQ-9',
          responses,
          undefined,
          undefined
        );
        done();
      });
    });

    it('should delegate scorePHQ9', () => {
      const responses = { q1: 1, q2: 2 };
      facade.scorePHQ9(responses);
      expect(mockMentalHealthService.scorePHQ9).toHaveBeenCalledWith(responses);
    });

    it('should delegate scoreGAD7', () => {
      const responses = { q1: 1, q2: 2 };
      facade.scoreGAD7(responses);
      expect(mockMentalHealthService.scoreGAD7).toHaveBeenCalledWith(responses);
    });

    it('should delegate scorePHQ2', () => {
      const responses = { q1: 1, q2: 2 };
      facade.scorePHQ2(responses);
      expect(mockMentalHealthService.scorePHQ2).toHaveBeenCalledWith(responses);
    });
  });

  describe('SDOH Delegations', () => {
    it('should delegate getSDOHSummary', (done) => {
      facade.getSDOHSummary('patient-1').subscribe(() => {
        expect(mockSDOHService.getSDOHSummary).toHaveBeenCalledWith('patient-1');
        done();
      });
    });

    it('should delegate getSocialDeterminants', (done) => {
      facade.getSocialDeterminants('patient-1').subscribe(() => {
        expect(mockSDOHService.getSocialDeterminants).toHaveBeenCalledWith('patient-1');
        done();
      });
    });

    it('should delegate calculateSDOHRiskScore', (done) => {
      facade.calculateSDOHRiskScore('patient-1').subscribe(() => {
        expect(mockSDOHService.calculateSDOHRiskScore).toHaveBeenCalledWith('patient-1');
        done();
      });
    });

    it('should delegate mapSDOHCategoryToZCode', () => {
      facade.mapSDOHCategoryToZCode('food-insecurity');
      expect(mockSDOHService.mapSDOHCategoryToZCode).toHaveBeenCalledWith('food-insecurity');
    });
  });

  describe('Risk Stratification Delegations', () => {
    it('should delegate getRiskStratification', (done) => {
      facade.getRiskStratification('patient-1').subscribe(() => {
        expect(mockRiskStratificationService.getRiskStratification).toHaveBeenCalledWith('patient-1');
        done();
      });
    });

    it('should delegate calculateMultiFactorRiskScore', (done) => {
      facade.calculateMultiFactorRiskScore('patient-1').subscribe(() => {
        expect(mockRiskStratificationService.calculateMultiFactorRiskScore).toHaveBeenCalledWith('patient-1');
        done();
      });
    });

    it('should delegate getCategoryRiskAssessments', (done) => {
      facade.getCategoryRiskAssessments('patient-1').subscribe(() => {
        expect(mockRiskStratificationService.getCategoryRiskAssessments).toHaveBeenCalledWith('patient-1');
        done();
      });
    });
  });

  describe('Health Scoring Delegations', () => {
    it('should delegate getHealthScore', (done) => {
      facade.getHealthScore('patient-1').subscribe(() => {
        expect(mockHealthScoringService.getHealthScore).toHaveBeenCalledWith('patient-1');
        done();
      });
    });

    it('should delegate getHealthScoreHistory', (done) => {
      facade.getHealthScoreHistory('patient-1').subscribe(() => {
        expect(mockHealthScoringService.getHealthScoreHistory).toHaveBeenCalledWith('patient-1');
        done();
      });
    });

    it('should delegate calculateWeightedHealthScore', () => {
      const components = { physical: 80, mental: 70, social: 60, preventive: 90 };
      facade.calculateWeightedHealthScore(components);
      expect(mockHealthScoringService.calculateWeightedHealthScore).toHaveBeenCalledWith(components);
    });

    it('should delegate determineHealthStatus', () => {
      facade.determineHealthStatus(85);
      expect(mockHealthScoringService.determineHealthStatus).toHaveBeenCalledWith(85);
    });
  });

  describe('Cache Invalidation', () => {
    it('should invalidate all caches for a patient', () => {
      facade.invalidatePatientCache('patient-1');

      expect(mockPhysicalHealthService.invalidatePatientPhysicalHealth).toHaveBeenCalledWith('patient-1');
      expect(mockMentalHealthService.invalidatePatientMentalHealth).toHaveBeenCalledWith('patient-1');
      expect(mockSDOHService.invalidatePatientSDOH).toHaveBeenCalledWith('patient-1');
      expect(mockRiskStratificationService.invalidatePatientRisk).toHaveBeenCalledWith('patient-1');
      expect(mockHealthScoringService.invalidateHealthScoreCache).toHaveBeenCalledWith('patient-1');
    });

    it('should delegate invalidateHealthScoreCache', () => {
      facade.invalidateHealthScoreCache('patient-1');
      expect(mockHealthScoringService.invalidateHealthScoreCache).toHaveBeenCalledWith('patient-1');
    });
  });
});
