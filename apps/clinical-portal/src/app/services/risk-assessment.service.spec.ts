/**
 * Unit Tests for Risk Assessment Service
 * Testing risk assessment functionality including fetching current assessment,
 * risk history, category-specific assessments, recalculation, and population stats.
 */

import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RiskAssessmentService } from './risk-assessment.service';
import { ApiService } from './api.service';
import {
  RiskAssessment,
  RiskFactor,
  PredictedOutcome,
  PopulationStats,
} from '../models/risk-assessment.model';
import { of, throwError } from 'rxjs';

describe('RiskAssessmentService', () => {
  let service: RiskAssessmentService;
  let httpMock: HttpTestingController;
  let apiService: jest.Mocked<ApiService>;

  // Mock data
  const mockRiskFactor: RiskFactor = {
    factor: 'Hypertension',
    category: 'CARDIOVASCULAR',
    weight: 25,
    severity: 'moderate',
    evidence: 'BP readings consistently >140/90',
  };

  const mockPredictedOutcome: PredictedOutcome = {
    outcome: 'Heart Attack',
    probability: 0.15,
    timeframe: '5 years',
  };

  const mockRiskAssessment: RiskAssessment = {
    id: 'risk-1',
    patientId: 'patient-123',
    riskCategory: 'CARDIOVASCULAR',
    riskScore: 72,
    riskLevel: 'high',
    riskFactors: [mockRiskFactor],
    predictedOutcomes: [mockPredictedOutcome],
    recommendations: ['Monitor blood pressure daily', 'Schedule cardiology appointment'],
    assessmentDate: new Date('2025-12-01T10:00:00Z'),
    createdAt: new Date('2025-12-01T10:00:00Z'),
  };

  const mockRiskAssessment2: RiskAssessment = {
    id: 'risk-2',
    patientId: 'patient-123',
    riskCategory: 'DIABETES',
    riskScore: 45,
    riskLevel: 'moderate',
    riskFactors: [
      {
        factor: 'Elevated HbA1c',
        category: 'DIABETES',
        weight: 20,
        severity: 'mild',
        evidence: 'HbA1c: 6.2%',
      },
    ],
    predictedOutcomes: [
      {
        outcome: 'Type 2 Diabetes',
        probability: 0.35,
        timeframe: '3 years',
      },
    ],
    recommendations: ['Dietary counseling', 'Increase physical activity'],
    assessmentDate: new Date('2025-11-15T10:00:00Z'),
    createdAt: new Date('2025-11-15T10:00:00Z'),
  };

  const mockPopulationStats: PopulationStats = {
    totalPatients: 1000,
    riskLevelDistribution: {
      low: 300,
      moderate: 400,
      high: 250,
      'very-high': 50,
    },
  };

  beforeEach(() => {
    const apiServiceMock = {
      get: jest.fn(),
      post: jest.fn(),
      put: jest.fn(),
      delete: jest.fn(),
    };

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        RiskAssessmentService,
        { provide: ApiService, useValue: apiServiceMock },
      ],
    });

    service = TestBed.inject(RiskAssessmentService);
    httpMock = TestBed.inject(HttpTestingController);
    apiService = TestBed.inject(ApiService) as jest.Mocked<ApiService>;
  });

  afterEach(() => {
    httpMock.verify();
    jest.clearAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  }, 30000);

  // ==========================================================================
  // Get Risk Assessment Tests
  // ==========================================================================

  describe('getRiskAssessment', () => {
    it('should fetch current risk assessment for a patient successfully', (done) => {
      apiService.get.mockReturnValue(of(mockRiskAssessment));

      service.getRiskAssessment('patient-123').subscribe({
        next: (assessment) => {
          expect(assessment).toBeDefined();
          expect(assessment.id).toBe('risk-1');
          expect(assessment.patientId).toBe('patient-123');
          expect(assessment.riskCategory).toBe('CARDIOVASCULAR');
          expect(assessment.riskScore).toBe(72);
          expect(assessment.riskLevel).toBe('high');
          expect(assessment.riskFactors.length).toBe(1);
          expect(assessment.predictedOutcomes.length).toBe(1);
          expect(assessment.recommendations.length).toBe(2);
          expect(apiService.get).toHaveBeenCalledWith('/api/patients/patient-123/risk-assessment');
          done();
        },
        error: () => fail('should not fail'),
      }, 30000);
    });

    it('should transform date strings to Date objects', (done) => {
      const mockData = {
        ...mockRiskAssessment,
        assessmentDate: '2025-12-01T10:00:00Z',
        createdAt: '2025-12-01T10:00:00Z',
      };
      apiService.get.mockReturnValue(of(mockData));

      service.getRiskAssessment('patient-123').subscribe({
        next: (assessment) => {
          expect(assessment.assessmentDate).toBeInstanceOf(Date);
          expect(assessment.createdAt).toBeInstanceOf(Date);
          done();
        },
      }, 30000);
    });

    it('should return null when patient has no risk assessment', (done) => {
      apiService.get.mockReturnValue(throwError(() => ({ status: 404 })));

      service.getRiskAssessment('patient-456').subscribe({
        next: (assessment) => {
          expect(assessment).toBeNull();
          done();
        },
      }, 30000);
    });

    it('should handle server errors gracefully', (done) => {
      const error = { status: 500, message: 'Server error' };
      apiService.get.mockReturnValue(throwError(() => error));

      service.getRiskAssessment('patient-123').subscribe({
        next: (assessment) => {
          expect(assessment).toBeNull();
          done();
        },
      }, 30000);
    });

    it('should handle missing patient ID', (done) => {
      service.getRiskAssessment('').subscribe({
        next: () => fail('should not succeed'),
        error: (err) => {
          expect(err).toBeDefined();
          done();
        },
      }, 30000);
    });
  });

  // ==========================================================================
  // Get Risk History Tests
  // ==========================================================================

  describe('getRiskHistory', () => {
    it('should fetch risk history for a patient successfully', (done) => {
      const mockHistory = [mockRiskAssessment, mockRiskAssessment2];
      apiService.get.mockReturnValue(of(mockHistory));

      service.getRiskHistory('patient-123').subscribe({
        next: (history) => {
          expect(history).toBeDefined();
          expect(history.length).toBe(2);
          expect(history[0].id).toBe('risk-1');
          expect(history[1].id).toBe('risk-2');
          expect(apiService.get).toHaveBeenCalledWith('/api/patients/patient-123/risk-history');
          done();
        },
        error: () => fail('should not fail'),
      }, 30000);
    });

    it('should transform all dates in history to Date objects', (done) => {
      const mockData = [
        {
          ...mockRiskAssessment,
          assessmentDate: '2025-12-01T10:00:00Z',
          createdAt: '2025-12-01T10:00:00Z',
        },
        {
          ...mockRiskAssessment2,
          assessmentDate: '2025-11-15T10:00:00Z',
          createdAt: '2025-11-15T10:00:00Z',
        },
      ];
      apiService.get.mockReturnValue(of(mockData));

      service.getRiskHistory('patient-123').subscribe({
        next: (history) => {
          expect(history[0].assessmentDate).toBeInstanceOf(Date);
          expect(history[0].createdAt).toBeInstanceOf(Date);
          expect(history[1].assessmentDate).toBeInstanceOf(Date);
          expect(history[1].createdAt).toBeInstanceOf(Date);
          done();
        },
      }, 30000);
    });

    it('should return empty array when patient has no history', (done) => {
      apiService.get.mockReturnValue(of([]));

      service.getRiskHistory('patient-456').subscribe({
        next: (history) => {
          expect(history).toEqual([]);
          done();
        },
      }, 30000);
    });

    it('should return empty array on error', (done) => {
      const error = { status: 500, message: 'Server error' };
      apiService.get.mockReturnValue(throwError(() => error));

      service.getRiskHistory('patient-123').subscribe({
        next: (history) => {
          expect(history).toEqual([]);
          done();
        },
      }, 30000);
    });

    it('should handle missing patient ID', (done) => {
      service.getRiskHistory('').subscribe({
        next: () => fail('should not succeed'),
        error: (err) => {
          expect(err).toBeDefined();
          done();
        },
      }, 30000);
    });
  });

  // ==========================================================================
  // Get Risk By Category Tests
  // ==========================================================================

  describe('getRiskByCategory', () => {
    it('should fetch risk assessment for a specific category successfully', (done) => {
      apiService.get.mockReturnValue(of(mockRiskAssessment));

      service.getRiskByCategory('patient-123', 'CARDIOVASCULAR').subscribe({
        next: (assessment) => {
          expect(assessment).toBeDefined();
          expect(assessment.id).toBe('risk-1');
          expect(assessment.riskCategory).toBe('CARDIOVASCULAR');
          expect(apiService.get).toHaveBeenCalledWith(
            '/api/patients/patient-123/risk-by-category/CARDIOVASCULAR'
          );
          done();
        },
        error: () => fail('should not fail'),
      }, 30000);
    });

    it('should return null when category assessment not found', (done) => {
      apiService.get.mockReturnValue(throwError(() => ({ status: 404 })));

      service.getRiskByCategory('patient-123', 'MENTAL_HEALTH').subscribe({
        next: (assessment) => {
          expect(assessment).toBeNull();
          done();
        },
      }, 30000);
    });

    it('should handle multiple categories', (done) => {
      const diabetesAssessment = { ...mockRiskAssessment2 };
      apiService.get.mockReturnValue(of(diabetesAssessment));

      service.getRiskByCategory('patient-123', 'DIABETES').subscribe({
        next: (assessment) => {
          expect(assessment).toBeDefined();
          expect(assessment.riskCategory).toBe('DIABETES');
          expect(apiService.get).toHaveBeenCalledWith(
            '/api/patients/patient-123/risk-by-category/DIABETES'
          );
          done();
        },
      }, 30000);
    });

    it('should handle server errors gracefully', (done) => {
      const error = { status: 500, message: 'Server error' };
      apiService.get.mockReturnValue(throwError(() => error));

      service.getRiskByCategory('patient-123', 'CARDIOVASCULAR').subscribe({
        next: (assessment) => {
          expect(assessment).toBeNull();
          done();
        },
      }, 30000);
    });

    it('should handle missing parameters', (done) => {
      service.getRiskByCategory('', '').subscribe({
        next: () => fail('should not succeed'),
        error: (err) => {
          expect(err).toBeDefined();
          done();
        },
      }, 30000);
    });
  });

  // ==========================================================================
  // Recalculate Risk Tests
  // ==========================================================================

  describe('recalculateRisk', () => {
    it('should trigger risk recalculation successfully', (done) => {
      const recalculatedAssessment = {
        ...mockRiskAssessment,
        riskScore: 68,
        assessmentDate: new Date(),
      };
      apiService.post.mockReturnValue(of(recalculatedAssessment));

      service.recalculateRisk('patient-123').subscribe({
        next: (assessment) => {
          expect(assessment).toBeDefined();
          expect(assessment.patientId).toBe('patient-123');
          expect(assessment.riskScore).toBe(68);
          expect(apiService.post).toHaveBeenCalledWith(
            '/api/patients/patient-123/recalculate-risk',
            {}
          );
          done();
        },
        error: () => fail('should not fail'),
      });
    });

    it('should transform dates in recalculated assessment', (done) => {
      const mockData = {
        ...mockRiskAssessment,
        assessmentDate: '2025-12-04T15:30:00Z',
        createdAt: '2025-12-04T15:30:00Z',
      };
      apiService.post.mockReturnValue(of(mockData));

      service.recalculateRisk('patient-123').subscribe({
        next: (assessment) => {
          expect(assessment.assessmentDate).toBeInstanceOf(Date);
          expect(assessment.createdAt).toBeInstanceOf(Date);
          done();
        },
      }, 30000);
    });

    it('should handle recalculation errors', (done) => {
      const error = { status: 400, message: 'Cannot recalculate risk' };
      apiService.post.mockReturnValue(throwError(() => error));

      service.recalculateRisk('patient-123').subscribe({
        next: () => fail('should not succeed'),
        error: (err) => {
          expect(err).toBeDefined();
          done();
        },
      }, 30000);
    });

    it('should handle missing patient ID', (done) => {
      service.recalculateRisk('').subscribe({
        next: () => fail('should not succeed'),
        error: (err) => {
          expect(err).toBeDefined();
          done();
        },
      }, 30000);
    });

    it('should return updated risk level after recalculation', (done) => {
      const updatedAssessment = {
        ...mockRiskAssessment,
        riskScore: 55,
        riskLevel: 'moderate',
      };
      apiService.post.mockReturnValue(of(updatedAssessment));

      service.recalculateRisk('patient-123').subscribe({
        next: (assessment) => {
          expect(assessment.riskScore).toBe(55);
          expect(assessment.riskLevel).toBe('moderate');
          done();
        },
      }, 30000);
    });
  });

  // ==========================================================================
  // Get Population Stats Tests
  // ==========================================================================

  describe('getPopulationStats', () => {
    it('should fetch population statistics successfully', (done) => {
      apiService.get.mockReturnValue(of(mockPopulationStats));

      service.getPopulationStats().subscribe({
        next: (stats) => {
          expect(stats).toBeDefined();
          expect(stats.totalPatients).toBe(1000);
          expect(stats.riskLevelDistribution.low).toBe(300);
          expect(stats.riskLevelDistribution.moderate).toBe(400);
          expect(stats.riskLevelDistribution.high).toBe(250);
          expect(stats.riskLevelDistribution['very-high']).toBe(50);
          expect(apiService.get).toHaveBeenCalledWith('/api/risk/population-stats');
          done();
        },
        error: () => fail('should not fail'),
      }, 30000);
    });

    it('should handle empty population', (done) => {
      const emptyStats: PopulationStats = {
        totalPatients: 0,
        riskLevelDistribution: {
          low: 0,
          moderate: 0,
          high: 0,
          'very-high': 0,
        },
      };
      apiService.get.mockReturnValue(of(emptyStats));

      service.getPopulationStats().subscribe({
        next: (stats) => {
          expect(stats.totalPatients).toBe(0);
          expect(stats.riskLevelDistribution.low).toBe(0);
          done();
        },
      }, 30000);
    }, 30000);

    it('should return empty stats on error', (done) => {
      const error = { status: 500, message: 'Server error' };
      apiService.get.mockReturnValue(throwError(() => error));

      service.getPopulationStats().subscribe({
        next: (stats) => {
          expect(stats.totalPatients).toBe(0);
          expect(stats.riskLevelDistribution.low).toBe(0);
          expect(stats.riskLevelDistribution.moderate).toBe(0);
          expect(stats.riskLevelDistribution.high).toBe(0);
          expect(stats.riskLevelDistribution['very-high']).toBe(0);
          done();
        },
      }, 30000);
    });

    it('should validate distribution totals match total patients', (done) => {
      apiService.get.mockReturnValue(of(mockPopulationStats));

      service.getPopulationStats().subscribe({
        next: (stats) => {
          const sum =
            stats.riskLevelDistribution.low +
            stats.riskLevelDistribution.moderate +
            stats.riskLevelDistribution.high +
            stats.riskLevelDistribution['very-high'];
          expect(sum).toBe(stats.totalPatients);
          done();
        },
      }, 30000);
    });
  });

  // ==========================================================================
  // Data Transformation Tests
  // ==========================================================================

  describe('Data Transformation', () => {
    it('should correctly transform risk factors', (done) => {
      apiService.get.mockReturnValue(of(mockRiskAssessment));

      service.getRiskAssessment('patient-123').subscribe({
        next: (assessment) => {
          const factor = assessment.riskFactors[0];
          expect(factor.factor).toBe('Hypertension');
          expect(factor.category).toBe('CARDIOVASCULAR');
          expect(factor.weight).toBe(25);
          expect(factor.severity).toBe('moderate');
          expect(factor.evidence).toBe('BP readings consistently >140/90');
          done();
        },
      }, 30000);
    });

    it('should correctly transform predicted outcomes', (done) => {
      apiService.get.mockReturnValue(of(mockRiskAssessment));

      service.getRiskAssessment('patient-123').subscribe({
        next: (assessment) => {
          const outcome = assessment.predictedOutcomes[0];
          expect(outcome.outcome).toBe('Heart Attack');
          expect(outcome.probability).toBe(0.15);
          expect(outcome.timeframe).toBe('5 years');
          done();
        },
      }, 30000);
    });

    it('should handle multiple risk factors', (done) => {
      const multiFactorAssessment = {
        ...mockRiskAssessment,
        riskFactors: [
          mockRiskFactor,
          {
            factor: 'High Cholesterol',
            category: 'CARDIOVASCULAR',
            weight: 20,
            severity: 'moderate',
            evidence: 'LDL >130',
          },
          {
            factor: 'Smoking',
            category: 'CARDIOVASCULAR',
            weight: 30,
            severity: 'high',
            evidence: 'Current smoker',
          },
        ],
      };
      apiService.get.mockReturnValue(of(multiFactorAssessment));

      service.getRiskAssessment('patient-123').subscribe({
        next: (assessment) => {
          expect(assessment.riskFactors.length).toBe(3);
          expect(assessment.riskFactors[0].factor).toBe('Hypertension');
          expect(assessment.riskFactors[1].factor).toBe('High Cholesterol');
          expect(assessment.riskFactors[2].factor).toBe('Smoking');
          done();
        },
      }, 30000);
    }, 30000);

    it('should handle multiple predicted outcomes', (done) => {
      const multiOutcomeAssessment = {
        ...mockRiskAssessment,
        predictedOutcomes: [
          mockPredictedOutcome,
          {
            outcome: 'Stroke',
            probability: 0.08,
            timeframe: '10 years',
          },
        ],
      };
      apiService.get.mockReturnValue(of(multiOutcomeAssessment));

      service.getRiskAssessment('patient-123').subscribe({
        next: (assessment) => {
          expect(assessment.predictedOutcomes.length).toBe(2);
          expect(assessment.predictedOutcomes[0].outcome).toBe('Heart Attack');
          expect(assessment.predictedOutcomes[1].outcome).toBe('Stroke');
          done();
        },
      }, 30000);
    }, 30000);
  }, 30000);

  // ==========================================================================
  // Error Handling Tests
  // ==========================================================================

  describe('Error Handling', () => {
    it('should log errors to console', (done) => {
      const consoleSpy = jest.spyOn(console, 'error').mockImplementation();
      const error = { status: 500, message: 'Server error' };
      apiService.get.mockReturnValue(throwError(() => error));

      service.getRiskAssessment('patient-123').subscribe({
        next: () => {
          expect(consoleSpy).toHaveBeenCalled();
          consoleSpy.mockRestore();
          done();
        },
      }, 30000);
    });

    it('should handle network errors', (done) => {
      const networkError = new Error('Network error');
      apiService.get.mockReturnValue(throwError(() => networkError));

      service.getRiskAssessment('patient-123').subscribe({
        next: (assessment) => {
          expect(assessment).toBeNull();
          done();
        },
      }, 30000);
    });

    it('should handle malformed response data', (done) => {
      const malformedData = { invalid: 'data' };
      apiService.get.mockReturnValue(of(malformedData));

      service.getRiskAssessment('patient-123').subscribe({
        next: (assessment) => {
          // Service should still return the data even if malformed
          expect(assessment).toBeDefined();
          done();
        },
      }, 30000);
    });
  });

  // ==========================================================================
  // Integration Tests
  // ==========================================================================

  describe('Integration Scenarios', () => {
    it('should fetch assessment and then recalculate', (done) => {
      apiService.get.mockReturnValue(of(mockRiskAssessment));
      const recalculated = { ...mockRiskAssessment, riskScore: 65 };
      apiService.post.mockReturnValue(of(recalculated));

      service.getRiskAssessment('patient-123').subscribe({
        next: (assessment) => {
          expect(assessment.riskScore).toBe(72);

          service.recalculateRisk('patient-123').subscribe({
            next: (newAssessment) => {
              expect(newAssessment.riskScore).toBe(65);
              done();
            },
          }, 30000);
        },
      });
    });

    it('should handle category-specific and overall assessments', (done) => {
      apiService.get.mockReturnValueOnce(of(mockRiskAssessment));
      apiService.get.mockReturnValueOnce(of(mockRiskAssessment2));

      service.getRiskByCategory('patient-123', 'CARDIOVASCULAR').subscribe({
        next: (cvAssessment) => {
          expect(cvAssessment.riskCategory).toBe('CARDIOVASCULAR');

          service.getRiskByCategory('patient-123', 'DIABETES').subscribe({
            next: (diabetesAssessment) => {
              expect(diabetesAssessment.riskCategory).toBe('DIABETES');
              done();
            },
          }, 30000);
        },
      });
    });
  });
});
