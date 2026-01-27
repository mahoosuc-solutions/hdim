/**
 * Unit Tests for SDOH (Social Determinants of Health) Service
 */
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { of, throwError } from 'rxjs';
import { SDOHService } from './sdoh.service';
import { FhirQuestionnaireService } from '../fhir/fhir-questionnaire.service';
import { LoggerService } from '../logger.service';
import {
  SDOHSummary,
  SDOHCategory,
  SDOHSeverity,
  RiskLevel,
} from '../../models/patient-health.model';

describe('SDOHService', () => {
  let service: SDOHService;
  let httpMock: HttpTestingController;

  const mockLoggerService = {
    withContext: jest.fn().mockReturnValue({
      info: jest.fn(),
      debug: jest.fn(),
      error: jest.fn(),
      warn: jest.fn(),
    }),
  };

  const mockFhirQuestionnaireService = {
    getQuestionnaireResponses: jest.fn().mockReturnValue(of({ entry: [] })),
    getSDOHScreenings: jest.fn().mockReturnValue(of([])),
    invalidatePatientQuestionnaires: jest.fn(),
  };

  const createMockSDOHSummary = (needs: Array<{ category: SDOHCategory; severity: SDOHSeverity; addressed: boolean }>): SDOHSummary => ({
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
    needs: needs.map((n, i) => ({
      id: `need-${i}`,
      category: n.category,
      severity: n.severity,
      addressed: n.addressed,
      screeningDate: new Date(),
      identifiedDate: new Date(),
    })),
    screeningDate: new Date(),
    interventions: [],
    referrals: [],
    strengths: [],
    zCodes: needs.map(n => service['SDOH_CATEGORY_ZCODES'][n.category]).filter(Boolean),
  });

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        SDOHService,
        { provide: LoggerService, useValue: mockLoggerService },
        { provide: FhirQuestionnaireService, useValue: mockFhirQuestionnaireService },
      ],
    });

    service = TestBed.inject(SDOHService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    try {
      const pendingReqs = httpMock.match((req) => true);
      pendingReqs.forEach((req) => {
        if (!req.cancelled) {
          req.flush({ resourceType: 'Bundle', type: 'searchset', total: 0, entry: [] });
        }
      });
    } catch {
      // No pending requests
    }
    httpMock.verify();
  });

  describe('Service Creation', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });

  describe('mapSDOHCategoryToZCode', () => {
    it('should map food-insecurity to Z59.4', () => {
      expect(service.mapSDOHCategoryToZCode('food-insecurity')).toBe('Z59.4');
    });

    it('should map housing-instability to Z59.0', () => {
      expect(service.mapSDOHCategoryToZCode('housing-instability')).toBe('Z59.0');
    });

    it('should map transportation to Z59.82', () => {
      expect(service.mapSDOHCategoryToZCode('transportation')).toBe('Z59.82');
    });

    it('should map financial-strain to Z59.9', () => {
      expect(service.mapSDOHCategoryToZCode('financial-strain')).toBe('Z59.9');
    });

    it('should map employment to Z56.0', () => {
      expect(service.mapSDOHCategoryToZCode('employment')).toBe('Z56.0');
    });

    it('should map education to Z55.9', () => {
      expect(service.mapSDOHCategoryToZCode('education')).toBe('Z55.9');
    });

    it('should map social-isolation to Z60.4', () => {
      expect(service.mapSDOHCategoryToZCode('social-isolation')).toBe('Z60.4');
    });

    it('should return default Z59.9 for unknown category', () => {
      expect(service.mapSDOHCategoryToZCode('unknown' as SDOHCategory)).toBe('Z59.9');
    });
  });

  describe('calculateSDOHRiskScore', () => {
    it('should return low risk for no unaddressed needs', (done) => {
      const mockSummary = createMockSDOHSummary([]);

      jest.spyOn(service, 'getSDOHSummary').mockReturnValue(of(mockSummary));

      service.calculateSDOHRiskScore('patient-1').subscribe((result) => {
        expect(result.score).toBe(0);
        expect(result.level).toBe('low');
        done();
      };
    });

    it('should calculate score for severe needs (25 points each)', (done) => {
      const mockSummary = createMockSDOHSummary([
        { category: 'food-insecurity', severity: 'severe', addressed: false },
        { category: 'housing-instability', severity: 'severe', addressed: false },
      ]);

      jest.spyOn(service, 'getSDOHSummary').mockReturnValue(of(mockSummary));

      service.calculateSDOHRiskScore('patient-1').subscribe((result) => {
        expect(result.score).toBe(50);
        expect(result.level).toBe('high');
        done();
      };
    });

    it('should calculate score for moderate needs (15 points each)', (done) => {
      const mockSummary = createMockSDOHSummary([
        { category: 'food-insecurity', severity: 'moderate', addressed: false },
        { category: 'transportation', severity: 'moderate', addressed: false },
      ]);

      jest.spyOn(service, 'getSDOHSummary').mockReturnValue(of(mockSummary));

      service.calculateSDOHRiskScore('patient-1').subscribe((result) => {
        expect(result.score).toBe(30);
        expect(result.level).toBe('moderate');
        done();
      };
    });

    it('should calculate score for mild needs (5 points each)', (done) => {
      const mockSummary = createMockSDOHSummary([
        { category: 'food-insecurity', severity: 'mild', addressed: false },
        { category: 'transportation', severity: 'mild', addressed: false },
      ]);

      jest.spyOn(service, 'getSDOHSummary').mockReturnValue(of(mockSummary));

      service.calculateSDOHRiskScore('patient-1').subscribe((result) => {
        expect(result.score).toBe(10);
        expect(result.level).toBe('low');
        done();
      };
    });

    it('should not count addressed needs', (done) => {
      const mockSummary = createMockSDOHSummary([
        { category: 'food-insecurity', severity: 'severe', addressed: true },
        { category: 'housing-instability', severity: 'severe', addressed: false },
      ]);

      jest.spyOn(service, 'getSDOHSummary').mockReturnValue(of(mockSummary));

      service.calculateSDOHRiskScore('patient-1').subscribe((result) => {
        expect(result.score).toBe(25); // Only one unaddressed severe need
        expect(result.level).toBe('moderate');
        done();
      };
    });

    it('should return critical level for scores >= 75', (done) => {
      const mockSummary = createMockSDOHSummary([
        { category: 'food-insecurity', severity: 'severe', addressed: false },
        { category: 'housing-instability', severity: 'severe', addressed: false },
        { category: 'transportation', severity: 'severe', addressed: false },
      ]);

      jest.spyOn(service, 'getSDOHSummary').mockReturnValue(of(mockSummary));

      service.calculateSDOHRiskScore('patient-1').subscribe((result) => {
        expect(result.score).toBe(75);
        expect(result.level).toBe('critical');
        done();
      };
    });

    it('should cap score at 100', (done) => {
      const mockSummary = createMockSDOHSummary([
        { category: 'food-insecurity', severity: 'severe', addressed: false },
        { category: 'housing-instability', severity: 'severe', addressed: false },
        { category: 'transportation', severity: 'severe', addressed: false },
        { category: 'employment', severity: 'severe', addressed: false },
        { category: 'education', severity: 'severe', addressed: false },
      ]);

      jest.spyOn(service, 'getSDOHSummary').mockReturnValue(of(mockSummary));

      service.calculateSDOHRiskScore('patient-1').subscribe((result) => {
        expect(result.score).toBe(100); // Capped at 100
        expect(result.level).toBe('critical');
        done();
      };
    });
  });

  describe('identifySDOHInterventionNeeds', () => {
    it('should return empty array when no needs', (done) => {
      const mockSummary = createMockSDOHSummary([]);

      jest.spyOn(service, 'getSDOHSummary').mockReturnValue(of(mockSummary));

      service.identifySDOHInterventionNeeds('patient-1').subscribe((interventions) => {
        expect(interventions.length).toBe(0);
        done();
      };
    });

    it('should identify interventions for unaddressed needs', (done) => {
      const mockSummary = createMockSDOHSummary([
        { category: 'food-insecurity', severity: 'severe', addressed: false },
        { category: 'transportation', severity: 'moderate', addressed: false },
      ]);

      jest.spyOn(service, 'getSDOHSummary').mockReturnValue(of(mockSummary));

      service.identifySDOHInterventionNeeds('patient-1').subscribe((interventions) => {
        expect(interventions.length).toBe(2);
        expect(interventions[0].category).toBe('food-insecurity');
        expect(interventions[0].priority).toBe('high');
        expect(interventions[1].category).toBe('transportation');
        expect(interventions[1].priority).toBe('medium');
        done();
      };
    });

    it('should not include addressed needs', (done) => {
      const mockSummary = createMockSDOHSummary([
        { category: 'food-insecurity', severity: 'severe', addressed: true },
        { category: 'transportation', severity: 'moderate', addressed: false },
      ]);

      jest.spyOn(service, 'getSDOHSummary').mockReturnValue(of(mockSummary));

      service.identifySDOHInterventionNeeds('patient-1').subscribe((interventions) => {
        expect(interventions.length).toBe(1);
        expect(interventions[0].category).toBe('transportation');
        done();
      };
    });

    it('should set correct priority based on severity', (done) => {
      const mockSummary = createMockSDOHSummary([
        { category: 'food-insecurity', severity: 'severe', addressed: false },
        { category: 'transportation', severity: 'moderate', addressed: false },
        { category: 'education', severity: 'mild', addressed: false },
      ]);

      jest.spyOn(service, 'getSDOHSummary').mockReturnValue(of(mockSummary));

      service.identifySDOHInterventionNeeds('patient-1').subscribe((interventions) => {
        expect(interventions.find(i => i.category === 'food-insecurity')?.priority).toBe('high');
        expect(interventions.find(i => i.category === 'transportation')?.priority).toBe('medium');
        expect(interventions.find(i => i.category === 'education')?.priority).toBe('low');
        done();
      };
    });
  });

  describe('getSDOHSummary', () => {
    it('should return SDOH summary from fhir questionnaire service', (done) => {
      // Mock the fhirQuestionnaire.getSDOHScreenings to return mock data
      mockFhirQuestionnaireService.getSDOHScreenings.mockReturnValue(of([{
        questionnaireType: 'PRAPARE',
        screeningDate: new Date(),
        items: [],
      }]));

      service.getSDOHSummary('patient-1').subscribe((result) => {
        expect(result).toBeDefined();
        expect(result.overallRisk).toBeDefined();
        done();
      };
    });

    it('should return default summary when no screenings exist', (done) => {
      mockFhirQuestionnaireService.getSDOHScreenings.mockReturnValue(of([]));

      service.getSDOHSummary('patient-no-screenings').subscribe((result) => {
        expect(result).toBeDefined();
        expect(result.overallRisk).toBe('low');
        done();
      };
    });

    it('should return cached result on second call', (done) => {
      mockFhirQuestionnaireService.getSDOHScreenings.mockReturnValue(of([]));

      // First call - populates cache
      service.getSDOHSummary('patient-cached').subscribe(() => {
        // Reset mock call count
        mockFhirQuestionnaireService.getSDOHScreenings.mockClear();

        // Second call - should use cache
        service.getSDOHSummary('patient-cached').subscribe((result) => {
          expect(result).toBeDefined();
          // Should not have made new API call
          expect(mockFhirQuestionnaireService.getSDOHScreenings).not.toHaveBeenCalled();
          done();
        };
      });
    });
  });

  describe('Cache Invalidation', () => {
    it('should invalidate SDOH cache and questionnaire cache', (done) => {
      mockFhirQuestionnaireService.getSDOHScreenings.mockReturnValue(of([]));

      // First call - populates cache
      service.getSDOHSummary('patient-invalidate').subscribe(() => {
        // Clear call count
        mockFhirQuestionnaireService.getSDOHScreenings.mockClear();

        // Invalidate cache
        service.invalidatePatientSDOH('patient-invalidate');

        // Verify questionnaire cache was also invalidated
        expect(mockFhirQuestionnaireService.invalidatePatientQuestionnaires).toHaveBeenCalledWith('patient-invalidate');

        // Third call - should make new call since cache was invalidated
        service.getSDOHSummary('patient-invalidate').subscribe(() => {
          expect(mockFhirQuestionnaireService.getSDOHScreenings).toHaveBeenCalled();
          done();
        };
      });
    });
  });
});
