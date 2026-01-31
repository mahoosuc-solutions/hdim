/**
 * Unit Tests for Care Recommendation Service
 * Testing care recommendations dashboard functionality including
 * fetching, filtering, sorting, and bulk actions.
 */

import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CareRecommendationService, RecommendationUpdate } from './care-recommendation.service';
import { ApiService } from './api.service';
import {
  DashboardRecommendation,
  RecommendationFilterConfig,
  RecommendationSortConfig,
  RecommendationDashboardStats,
  BulkActionRequest,
  BulkActionResult,
  RecommendationCategory,
  RecommendationStatus,
  RecommendationUrgency,
} from '../models/care-recommendation.model';
import { RiskLevel } from '../models/patient-health.model';
import { buildQualityMeasureUrl, QUALITY_MEASURE_ENDPOINTS } from '../config/api.config';
import { of, throwError } from 'rxjs';

describe('CareRecommendationService', () => {
  let service: CareRecommendationService;
  let httpMock: HttpTestingController;
  let apiService: jest.Mocked<ApiService>;

  // Mock data
  const mockRecommendation: DashboardRecommendation = {
    id: 'rec-1',
    type: 'care-gap',
    patientId: 'patient-123',
    patientName: 'John Doe',
    mrn: 'MRN-12345',
    patientRiskLevel: 'high' as RiskLevel,
    category: 'preventive' as RecommendationCategory,
    title: 'Annual Wellness Visit Overdue',
    description: 'Patient has not had an annual wellness visit in over 12 months',
    urgency: 'urgent' as RecommendationUrgency,
    priority: 8,
    status: 'pending' as RecommendationStatus,
    createdDate: new Date('2025-01-01'),
    dueDate: new Date('2025-12-01'),
    daysOverdue: 3,
    actionItems: ['Schedule annual wellness visit', 'Review preventive care needs'],
    suggestedIntervention: 'Contact patient to schedule appointment',
    measureId: 'AWV-001',
    measureName: 'Annual Wellness Visit',
    evidenceSource: 'CMS Quality Measures',
    clinicalGuideline: 'Medicare AWV Guidelines',
  };

  const mockRecommendation2: DashboardRecommendation = {
    id: 'rec-2',
    type: 'recommendation',
    patientId: 'patient-456',
    patientName: 'Jane Smith',
    mrn: 'MRN-67890',
    patientRiskLevel: 'moderate' as RiskLevel,
    category: 'chronic-disease' as RecommendationCategory,
    title: 'HbA1c Test Needed',
    description: 'Diabetic patient needs HbA1c test',
    urgency: 'soon' as RecommendationUrgency,
    priority: 6,
    status: 'pending' as RecommendationStatus,
    createdDate: new Date('2025-01-15'),
    dueDate: new Date('2025-02-15'),
    actionItems: ['Order HbA1c test', 'Review diabetes management plan'],
    suggestedIntervention: 'Schedule lab work',
    measureId: 'DM-001',
    measureName: 'Diabetes HbA1c Testing',
  };

  const mockStats: RecommendationDashboardStats = {
    totalRecommendations: 50,
    byUrgency: {
      emergent: 5,
      urgent: 15,
      soon: 20,
      routine: 10,
    },
    byCategory: {
      preventive: 15,
      chronicDisease: 20,
      medication: 8,
      mentalHealth: 4,
      sdoh: 3,
    },
    byPatientRisk: {
      critical: 10,
      high: 20,
      moderate: 15,
      low: 5,
    },
    byStatus: {
      pending: 30,
      inProgress: 10,
      completed: 8,
      declined: 2,
    },
    overdueSummary: {
      total: 25,
      critical: 8,
      warning: 10,
      approaching: 7,
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
        CareRecommendationService,
        { provide: ApiService, useValue: apiServiceMock },
      ],
    });

    service = TestBed.inject(CareRecommendationService);
    httpMock = TestBed.inject(HttpTestingController);
    apiService = TestBed.inject(ApiService) as jest.Mocked<ApiService>;
  });

  afterEach(() => {
    httpMock.verify();
    jest.clearAllMocks();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  };

  // ==========================================================================
  // Get Dashboard Recommendations Tests
  // ==========================================================================

  describe('getDashboardRecommendations', () => {
    it('should fetch all dashboard recommendations successfully', (done) => {
      const mockRecommendations = [mockRecommendation, mockRecommendation2];
      apiService.get.mockReturnValue(of(mockRecommendations));

      service.getDashboardRecommendations().subscribe({
        next: (recommendations) => {
          expect(recommendations).toBeDefined();
          expect(recommendations.length).toBe(2);
          expect(recommendations[0].id).toBe('rec-1');
          expect(recommendations[1].id).toBe('rec-2');
          expect(apiService.get).toHaveBeenCalledWith(
            buildQualityMeasureUrl('/patient-health/recommendations/dashboard')
          );
          done();
        },
        error: () => fail('should not fail'),
      };
    });

    it('should transform date strings to Date objects', (done) => {
      const mockData = [{
        ...mockRecommendation,
        createdDate: '2025-01-01T00:00:00Z',
        dueDate: '2025-12-01T00:00:00Z',
      }];
      apiService.get.mockReturnValue(of(mockData));

      service.getDashboardRecommendations().subscribe({
        next: (recommendations) => {
          expect(recommendations[0].createdDate).toBeInstanceOf(Date);
          expect(recommendations[0].dueDate).toBeInstanceOf(Date);
          done();
        },
      };
    });

    it('should calculate days overdue for overdue recommendations', (done) => {
      const pastDueDate = new Date();
      pastDueDate.setDate(pastDueDate.getDate() - 10);

      const mockData = [{
        ...mockRecommendation,
        dueDate: pastDueDate.toISOString(),
      }];
      apiService.get.mockReturnValue(of(mockData));

      service.getDashboardRecommendations().subscribe({
        next: (recommendations) => {
          expect(recommendations[0].daysOverdue).toBeGreaterThan(0);
          done();
        },
      };
    });

    it('should return empty array on error', (done) => {
      const error = { status: 500, message: 'Server error' };
      apiService.get.mockReturnValue(throwError(() => error));

      service.getDashboardRecommendations().subscribe({
        next: (recommendations) => {
          expect(recommendations).toEqual([]);
          done();
        },
      };
    });

    it('should emit update notification on successful load', (done) => {
      const mockRecommendations = [mockRecommendation];
      apiService.get.mockReturnValue(of(mockRecommendations));

      let updateEmitted = false;
      service.updates$.subscribe((update) => {
        if (update && update.type === 'loaded') {
          expect(update.count).toBe(1);
          expect(update.timestamp).toBeInstanceOf(Date);
          updateEmitted = true;
        }
      };

      service.getDashboardRecommendations().subscribe({
        next: () => {
          expect(updateEmitted).toBe(true);
          done();
        },
      });
    });

    it('should use cached data when not expired and refresh=false', (done) => {
      const mockRecommendations = [mockRecommendation];
      apiService.get.mockReturnValue(of(mockRecommendations));

      // First call to populate cache
      service.getDashboardRecommendations().subscribe({
        next: () => {
          // Second call should use cache
          service.getDashboardRecommendations(false).subscribe({
            next: (recommendations) => {
              expect(recommendations.length).toBe(1);
              // API should only be called once (for first request)
              expect(apiService.get).toHaveBeenCalledTimes(1);
              done();
            },
          };
        },
      });
    });

    it('should bypass cache when refresh=true', (done) => {
      const mockRecommendations = [mockRecommendation];
      apiService.get.mockReturnValue(of(mockRecommendations));

      // First call
      service.getDashboardRecommendations().subscribe({
        next: () => {
          // Second call with refresh=true
          service.getDashboardRecommendations(true).subscribe({
            next: () => {
              // API should be called twice
              expect(apiService.get).toHaveBeenCalledTimes(2);
              done();
            },
          };
        },
      });
    });
  });

  // ==========================================================================
  // Get Filtered Recommendations Tests
  // ==========================================================================

  describe('getFilteredRecommendations', () => {
    it('should fetch filtered recommendations with all filter parameters', (done) => {
      const mockRecommendations = [mockRecommendation];
      const filter: Partial<RecommendationFilterConfig> = {
        urgency: ['urgent', 'emergent'],
        category: ['preventive'],
        patientRiskLevel: ['high', 'critical'],
        status: ['pending'],
        patientSearch: 'John',
      };
      const sort: RecommendationSortConfig = {
        field: 'urgency',
        direction: 'desc',
      };

      apiService.get.mockReturnValue(of(mockRecommendations));

      service.getFilteredRecommendations(filter, sort).subscribe({
        next: (recommendations) => {
          expect(recommendations).toBeDefined();
          expect(recommendations.length).toBe(1);

          // Verify API was called with correct params
          const callArgs = apiService.get.mock.calls[0];
          expect(callArgs[0]).toBe(buildQualityMeasureUrl('/patient-health/recommendations/filter'));

          const params = callArgs[1];
          expect(params.get('urgency')).toBe('urgent,emergent');
          expect(params.get('category')).toBe('preventive');
          expect(params.get('riskLevel')).toBe('high,critical');
          expect(params.get('status')).toBe('pending');
          expect(params.get('patientSearch')).toBe('John');
          expect(params.get('sortField')).toBe('urgency');
          expect(params.get('sortDirection')).toBe('desc');
          done();
        },
      };
    });

    it('should handle filter with only urgency parameter', (done) => {
      const mockRecommendations = [mockRecommendation];
      const filter: Partial<RecommendationFilterConfig> = {
        urgency: ['urgent'],
      };

      apiService.get.mockReturnValue(of(mockRecommendations));

      service.getFilteredRecommendations(filter).subscribe({
        next: () => {
          const callArgs = apiService.get.mock.calls[0];
          const params = callArgs[1];
          expect(params.get('urgency')).toBe('urgent');
          expect(params.has('category')).toBe(false);
          done();
        },
      };
    });

    it('should handle empty filter object', (done) => {
      const mockRecommendations = [mockRecommendation];
      apiService.get.mockReturnValue(of(mockRecommendations));

      service.getFilteredRecommendations({}).subscribe({
        next: (recommendations) => {
          expect(recommendations).toBeDefined();
          done();
        },
      };
    });

    it('should emit filtered update notification', (done) => {
      const mockRecommendations = [mockRecommendation, mockRecommendation2];
      apiService.get.mockReturnValue(of(mockRecommendations));

      let updateEmitted = false;
      service.updates$.subscribe((update) => {
        if (update && update.type === 'filtered') {
          expect(update.count).toBe(2);
          updateEmitted = true;
        }
      };

      service.getFilteredRecommendations({ urgency: ['urgent'] }).subscribe({
        next: () => {
          expect(updateEmitted).toBe(true);
          done();
        },
      });
    });

    it('should return empty array on error', (done) => {
      const error = { status: 400, message: 'Bad request' };
      apiService.get.mockReturnValue(throwError(() => error));

      service.getFilteredRecommendations({ urgency: ['urgent'] }).subscribe({
        next: (recommendations) => {
          expect(recommendations).toEqual([]);
          done();
        },
      };
    });

    it('should handle sort without filter', (done) => {
      const mockRecommendations = [mockRecommendation];
      const sort: RecommendationSortConfig = {
        field: 'patientName',
        direction: 'asc',
      };

      apiService.get.mockReturnValue(of(mockRecommendations));

      service.getFilteredRecommendations({}, sort).subscribe({
        next: () => {
          const callArgs = apiService.get.mock.calls[0];
          const params = callArgs[1];
          expect(params.get('sortField')).toBe('patientName');
          expect(params.get('sortDirection')).toBe('asc');
          done();
        },
      };
    });
  });

  // ==========================================================================
  // Get Dashboard Stats Tests
  // ==========================================================================

  describe('getDashboardStats', () => {
    it('should fetch dashboard statistics successfully', (done) => {
      apiService.get.mockReturnValue(of(mockStats));

      service.getDashboardStats().subscribe({
        next: (stats) => {
          expect(stats).toBeDefined();
          expect(stats.totalRecommendations).toBe(50);
          expect(stats.byUrgency.urgent).toBe(15);
          expect(stats.byCategory.preventive).toBe(15);
          expect(stats.byPatientRisk.high).toBe(20);
          expect(stats.byStatus.pending).toBe(30);
          expect(stats.overdueSummary.total).toBe(25);
          expect(apiService.get).toHaveBeenCalledWith(
            buildQualityMeasureUrl('/patient-health/recommendations/stats')
          );
          done();
        },
      };
    });

    it('should cache stats and return cached value on subsequent calls', (done) => {
      apiService.get.mockReturnValue(of(mockStats));

      // First call
      service.getDashboardStats().subscribe({
        next: () => {
          // Second call should use cache
          service.getDashboardStats().subscribe({
            next: (stats) => {
              expect(stats.totalRecommendations).toBe(50);
              // API should only be called once
              expect(apiService.get).toHaveBeenCalledTimes(1);
              done();
            },
          };
        },
      });
    });

    it('should return empty stats on error', (done) => {
      const error = { status: 500, message: 'Server error' };
      apiService.get.mockReturnValue(throwError(() => error));

      service.getDashboardStats().subscribe({
        next: (stats) => {
          expect(stats.totalRecommendations).toBe(0);
          expect(stats.byUrgency.emergent).toBe(0);
          expect(stats.byCategory.preventive).toBe(0);
          expect(stats.byPatientRisk.critical).toBe(0);
          expect(stats.byStatus.pending).toBe(0);
          done();
        },
      };
    });
  });

  // ==========================================================================
  // Accept Recommendation Tests
  // ==========================================================================

  describe('acceptRecommendation', () => {
    it('should accept recommendation successfully', (done) => {
      const acceptedRec = {
        ...mockRecommendation,
        status: 'in-progress' as RecommendationStatus,
      };
      apiService.post.mockReturnValue(of(acceptedRec));

      service.acceptRecommendation('rec-1').subscribe({
        next: (recommendation) => {
          expect(recommendation).toBeDefined();
          expect(recommendation.status).toBe('in-progress');
          expect(apiService.post).toHaveBeenCalledWith(
            buildQualityMeasureUrl(
              QUALITY_MEASURE_ENDPOINTS.UPDATE_RECOMMENDATION_STATUS('rec-1')
            ),
            {
              status: 'in-progress',
              action: 'accept',
              notes: undefined,
            }
          );
          done();
        },
      });
    });

    it('should accept recommendation with notes', (done) => {
      const acceptedRec = {
        ...mockRecommendation,
        status: 'in-progress' as RecommendationStatus,
      };
      apiService.post.mockReturnValue(of(acceptedRec));

      service.acceptRecommendation('rec-1', 'Patient contacted').subscribe({
        next: () => {
          const callArgs = apiService.post.mock.calls[0];
          expect(callArgs[1]).toEqual({
            status: 'in-progress',
            action: 'accept',
            notes: 'Patient contacted',
          };
          done();
        },
      });
    });

    it('should emit accepted update notification', (done) => {
      const acceptedRec = {
        ...mockRecommendation,
        status: 'in-progress' as RecommendationStatus,
      };
      apiService.post.mockReturnValue(of(acceptedRec));

      let updateEmitted = false;
      service.updates$.subscribe((update) => {
        if (update && update.type === 'accepted') {
          expect(update.recommendationId).toBe('rec-1');
          expect(update.patientId).toBe('patient-123');
          updateEmitted = true;
        }
      };

      service.acceptRecommendation('rec-1').subscribe({
        next: () => {
          expect(updateEmitted).toBe(true);
          done();
        },
      });
    });

    it('should invalidate cache after accepting', (done) => {
      const acceptedRec = {
        ...mockRecommendation,
        status: 'in-progress' as RecommendationStatus,
      };
      apiService.post.mockReturnValue(of(acceptedRec));

      service.acceptRecommendation('rec-1').subscribe({
        next: () => {
          // Cache should be cleared
          service.getCachedRecommendations$().subscribe((cached) => {
            expect(cached).toEqual([]);
            done();
          };
        },
      });
    });

    it('should throw error on failure', (done) => {
      const error = { status: 404, message: 'Recommendation not found' };
      apiService.post.mockReturnValue(throwError(() => error));

      service.acceptRecommendation('invalid-id').subscribe({
        next: () => fail('should not succeed'),
        error: (err) => {
          expect(err).toBeDefined();
          done();
        },
      };
    });
  });

  // ==========================================================================
  // Decline Recommendation Tests
  // ==========================================================================

  describe('declineRecommendation', () => {
    it('should decline recommendation with reason', (done) => {
      const declinedRec = {
        ...mockRecommendation,
        status: 'declined' as RecommendationStatus,
      };
      apiService.post.mockReturnValue(of(declinedRec));

      service.declineRecommendation('rec-1', 'Not applicable').subscribe({
        next: (recommendation) => {
          expect(recommendation).toBeDefined();
          expect(recommendation.status).toBe('declined');
          expect(apiService.post).toHaveBeenCalledWith(
            buildQualityMeasureUrl(
              QUALITY_MEASURE_ENDPOINTS.UPDATE_RECOMMENDATION_STATUS('rec-1')
            ),
            {
              status: 'declined',
              action: 'decline',
              reason: 'Not applicable',
              notes: undefined,
            }
          );
          done();
        },
      });
    });

    it('should decline recommendation with reason and notes', (done) => {
      const declinedRec = {
        ...mockRecommendation,
        status: 'declined' as RecommendationStatus,
      };
      apiService.post.mockReturnValue(of(declinedRec));

      service.declineRecommendation('rec-1', 'Not applicable', 'Patient refused').subscribe({
        next: () => {
          const callArgs = apiService.post.mock.calls[0];
          expect(callArgs[1]).toEqual({
            status: 'declined',
            action: 'decline',
            reason: 'Not applicable',
            notes: 'Patient refused',
          };
          done();
        },
      });
    });

    it('should emit declined update notification', (done) => {
      const declinedRec = {
        ...mockRecommendation,
        status: 'declined' as RecommendationStatus,
      };
      apiService.post.mockReturnValue(of(declinedRec));

      let updateEmitted = false;
      service.updates$.subscribe((update) => {
        if (update && update.type === 'declined') {
          expect(update.recommendationId).toBe('rec-1');
          expect(update.patientId).toBe('patient-123');
          updateEmitted = true;
        }
      };

      service.declineRecommendation('rec-1', 'Not applicable').subscribe({
        next: () => {
          expect(updateEmitted).toBe(true);
          done();
        },
      });
    });

    it('should invalidate cache after declining', (done) => {
      const declinedRec = {
        ...mockRecommendation,
        status: 'declined' as RecommendationStatus,
      };
      apiService.post.mockReturnValue(of(declinedRec));

      service.declineRecommendation('rec-1', 'Not applicable').subscribe({
        next: () => {
          service.getCachedRecommendations$().subscribe((cached) => {
            expect(cached).toEqual([]);
            done();
          };
        },
      });
    });

    it('should throw error on failure', (done) => {
      const error = { status: 500, message: 'Server error' };
      apiService.post.mockReturnValue(throwError(() => error));

      service.declineRecommendation('rec-1', 'reason').subscribe({
        next: () => fail('should not succeed'),
        error: (err) => {
          expect(err).toBeDefined();
          done();
        },
      };
    });
  });

  // ==========================================================================
  // Complete Recommendation Tests
  // ==========================================================================

  describe('completeRecommendation', () => {
    it('should complete recommendation with outcome', (done) => {
      const completedRec = {
        ...mockRecommendation,
        status: 'completed' as RecommendationStatus,
        completedDate: new Date(),
      };
      apiService.post.mockReturnValue(of(completedRec));

      service.completeRecommendation('rec-1', 'Appointment scheduled').subscribe({
        next: (recommendation) => {
          expect(recommendation).toBeDefined();
          expect(recommendation.status).toBe('completed');
          expect(apiService.post).toHaveBeenCalledWith(
            buildQualityMeasureUrl(
              QUALITY_MEASURE_ENDPOINTS.UPDATE_RECOMMENDATION_STATUS('rec-1')
            ),
            {
              status: 'completed',
              action: 'complete',
              outcome: 'Appointment scheduled',
              notes: undefined,
            }
          );
          done();
        },
      });
    });

    it('should complete recommendation with outcome and notes', (done) => {
      const completedRec = {
        ...mockRecommendation,
        status: 'completed' as RecommendationStatus,
      };
      apiService.post.mockReturnValue(of(completedRec));

      service.completeRecommendation('rec-1', 'Completed', 'All tasks done').subscribe({
        next: () => {
          const callArgs = apiService.post.mock.calls[0];
          expect(callArgs[1]).toEqual({
            status: 'completed',
            action: 'complete',
            outcome: 'Completed',
            notes: 'All tasks done',
          };
          done();
        },
      });
    });

    it('should emit completed update notification', (done) => {
      const completedRec = {
        ...mockRecommendation,
        status: 'completed' as RecommendationStatus,
      };
      apiService.post.mockReturnValue(of(completedRec));

      let updateEmitted = false;
      service.updates$.subscribe((update) => {
        if (update && update.type === 'completed') {
          expect(update.recommendationId).toBe('rec-1');
          expect(update.patientId).toBe('patient-123');
          updateEmitted = true;
        }
      };

      service.completeRecommendation('rec-1', 'Done').subscribe({
        next: () => {
          expect(updateEmitted).toBe(true);
          done();
        },
      });
    });

    it('should invalidate cache after completing', (done) => {
      const completedRec = {
        ...mockRecommendation,
        status: 'completed' as RecommendationStatus,
      };
      apiService.post.mockReturnValue(of(completedRec));

      service.completeRecommendation('rec-1', 'Done').subscribe({
        next: () => {
          service.getCachedRecommendations$().subscribe((cached) => {
            expect(cached).toEqual([]);
            done();
          };
        },
      });
    });

    it('should throw error on failure', (done) => {
      const error = { status: 400, message: 'Invalid outcome' };
      apiService.post.mockReturnValue(throwError(() => error));

      service.completeRecommendation('rec-1', '').subscribe({
        next: () => fail('should not succeed'),
        error: (err) => {
          expect(err).toBeDefined();
          done();
        },
      };
    });
  });

  // ==========================================================================
  // Perform Bulk Action Tests
  // ==========================================================================

  describe('performBulkAction', () => {
    it('should perform bulk accept action successfully', (done) => {
      const bulkRequest: BulkActionRequest = {
        recommendationIds: ['rec-1', 'rec-2', 'rec-3'],
        action: 'accept',
        notes: 'Bulk accepted',
      };

      const bulkResult: BulkActionResult = {
        successCount: 3,
        failureCount: 0,
        processed: [
          { id: 'rec-1', success: true },
          { id: 'rec-2', success: true },
          { id: 'rec-3', success: true },
        ],
      };

      apiService.post.mockReturnValue(of(bulkResult));

      service.performBulkAction(bulkRequest).subscribe({
        next: (result) => {
          expect(result).toBeDefined();
          expect(result.successCount).toBe(3);
          expect(result.failureCount).toBe(0);
          expect(result.processed.length).toBe(3);
          expect(apiService.post).toHaveBeenCalledWith(
            buildQualityMeasureUrl('/patient-health/recommendations/bulk-action'),
            bulkRequest
          );
          done();
        },
      };
    };

    it('should handle bulk action with partial failures', (done) => {
      const bulkRequest: BulkActionRequest = {
        recommendationIds: ['rec-1', 'rec-2', 'rec-invalid'],
        action: 'decline',
        reason: 'Not applicable',
      };

      const bulkResult: BulkActionResult = {
        successCount: 2,
        failureCount: 1,
        processed: [
          { id: 'rec-1', success: true },
          { id: 'rec-2', success: true },
          { id: 'rec-invalid', success: false, error: 'Recommendation not found' },
        ],
      };

      apiService.post.mockReturnValue(of(bulkResult));

      service.performBulkAction(bulkRequest).subscribe({
        next: (result) => {
          expect(result.successCount).toBe(2);
          expect(result.failureCount).toBe(1);
          expect(result.processed[2].success).toBe(false);
          expect(result.processed[2].error).toBe('Recommendation not found');
          done();
        },
      };
    };

    it('should emit bulk-action update notification', (done) => {
      const bulkRequest: BulkActionRequest = {
        recommendationIds: ['rec-1', 'rec-2'],
        action: 'complete',
      };

      const bulkResult: BulkActionResult = {
        successCount: 2,
        failureCount: 0,
        processed: [
          { id: 'rec-1', success: true },
          { id: 'rec-2', success: true },
        ],
      };

      apiService.post.mockReturnValue(of(bulkResult));

      let updateEmitted = false;
      service.updates$.subscribe((update) => {
        if (update && update.type === 'bulk-action') {
          expect(update.count).toBe(2);
          updateEmitted = true;
        }
      };

      service.performBulkAction(bulkRequest).subscribe({
        next: () => {
          expect(updateEmitted).toBe(true);
          done();
        },
      };
    };

    it('should invalidate cache after bulk action', (done) => {
      const bulkRequest: BulkActionRequest = {
        recommendationIds: ['rec-1'],
        action: 'accept',
      };

      const bulkResult: BulkActionResult = {
        successCount: 1,
        failureCount: 0,
        processed: [{ id: 'rec-1', success: true }],
      };

      apiService.post.mockReturnValue(of(bulkResult));

      service.performBulkAction(bulkRequest).subscribe({
        next: () => {
          service.getCachedRecommendations$().subscribe((cached) => {
            expect(cached).toEqual([]);
            done();
          };
        },
      };
    };

    it('should throw error on bulk action failure', (done) => {
      const bulkRequest: BulkActionRequest = {
        recommendationIds: ['rec-1'],
        action: 'accept',
      };

      const error = { status: 500, message: 'Bulk action failed' };
      apiService.post.mockReturnValue(throwError(() => error));

      service.performBulkAction(bulkRequest).subscribe({
        next: () => fail('should not succeed'),
        error: (err) => {
          expect(err).toBeDefined();
          done();
        },
      };
    });

    it('should handle bulk action with assign action type', (done) => {
      const bulkRequest: BulkActionRequest = {
        recommendationIds: ['rec-1', 'rec-2'],
        action: 'assign',
        assignedTo: 'dr-smith',
        notes: 'Assigned to Dr. Smith',
      };

      const bulkResult: BulkActionResult = {
        successCount: 2,
        failureCount: 0,
        processed: [
          { id: 'rec-1', success: true },
          { id: 'rec-2', success: true },
        ],
      };

      apiService.post.mockReturnValue(of(bulkResult));

      service.performBulkAction(bulkRequest).subscribe({
        next: (result) => {
          expect(result.successCount).toBe(2);
          const callArgs = apiService.post.mock.calls[0];
          expect(callArgs[1]).toEqual(bulkRequest);
          done();
        },
      });
    });

    it('should handle bulk action with reschedule action type', (done) => {
      const newDueDate = new Date('2025-12-31');
      const bulkRequest: BulkActionRequest = {
        recommendationIds: ['rec-1'],
        action: 'reschedule',
        newDueDate,
        notes: 'Rescheduled to end of year',
      };

      const bulkResult: BulkActionResult = {
        successCount: 1,
        failureCount: 0,
        processed: [{ id: 'rec-1', success: true }],
      };

      apiService.post.mockReturnValue(of(bulkResult));

      service.performBulkAction(bulkRequest).subscribe({
        next: (result) => {
          expect(result.successCount).toBe(1);
          done();
        },
      });
    });
  });

  // ==========================================================================
  // Client-Side Filtering Tests
  // ==========================================================================

  describe('applyFilters', () => {
    const testRecommendations = [mockRecommendation, mockRecommendation2];

    it('should filter by urgency', () => {
      const filter: Partial<RecommendationFilterConfig> = {
        urgency: ['urgent'],
      };

      const filtered = service.applyFilters(testRecommendations, filter);
      expect(filtered.length).toBe(1);
      expect(filtered[0].urgency).toBe('urgent');
    });

    it('should filter by category', () => {
      const filter: Partial<RecommendationFilterConfig> = {
        category: ['preventive'],
      };

      const filtered = service.applyFilters(testRecommendations, filter);
      expect(filtered.length).toBe(1);
      expect(filtered[0].category).toBe('preventive');
    });

    it('should filter by patient risk level', () => {
      const filter: Partial<RecommendationFilterConfig> = {
        patientRiskLevel: ['high'],
      };

      const filtered = service.applyFilters(testRecommendations, filter);
      expect(filtered.length).toBe(1);
      expect(filtered[0].patientRiskLevel).toBe('high');
    });

    it('should filter by status', () => {
      const filter: Partial<RecommendationFilterConfig> = {
        status: ['pending'],
      };

      const filtered = service.applyFilters(testRecommendations, filter);
      expect(filtered.length).toBe(2);
    });

    it('should filter by patient search (name)', () => {
      const filter: Partial<RecommendationFilterConfig> = {
        patientSearch: 'john',
      };

      const filtered = service.applyFilters(testRecommendations, filter);
      expect(filtered.length).toBe(1);
      expect(filtered[0].patientName).toBe('John Doe');
    });

    it('should filter by patient search (MRN)', () => {
      const filter: Partial<RecommendationFilterConfig> = {
        patientSearch: '67890',
      };

      const filtered = service.applyFilters(testRecommendations, filter);
      expect(filtered.length).toBe(1);
      expect(filtered[0].mrn).toBe('MRN-67890');
    });

    it('should filter by days overdue minimum', () => {
      const filter: Partial<RecommendationFilterConfig> = {
        daysOverdueRange: { min: 3, max: null },
      };

      const filtered = service.applyFilters(testRecommendations, filter);
      expect(filtered.length).toBe(1);
      expect(filtered[0].daysOverdue).toBeGreaterThanOrEqual(3);
    });

    it('should filter by days overdue maximum', () => {
      const filter: Partial<RecommendationFilterConfig> = {
        daysOverdueRange: { min: null, max: 5 },
      };

      const filtered = service.applyFilters(testRecommendations, filter);
      filtered.forEach((rec) => {
        if (rec.daysOverdue) {
          expect(rec.daysOverdue).toBeLessThanOrEqual(5);
        }
      });
    });

    it('should apply multiple filters together', () => {
      const filter: Partial<RecommendationFilterConfig> = {
        urgency: ['urgent'],
        category: ['preventive'],
        patientRiskLevel: ['high'],
      };

      const filtered = service.applyFilters(testRecommendations, filter);
      expect(filtered.length).toBe(1);
      expect(filtered[0].urgency).toBe('urgent');
      expect(filtered[0].category).toBe('preventive');
      expect(filtered[0].patientRiskLevel).toBe('high');
    });

    it('should return all recommendations when no filters applied', () => {
      const filtered = service.applyFilters(testRecommendations, {});
      expect(filtered.length).toBe(2);
    });
  });

  // ==========================================================================
  // Client-Side Sorting Tests
  // ==========================================================================

  describe('applySort', () => {
    const testRecommendations = [mockRecommendation, mockRecommendation2];

    it('should sort by urgency ascending', () => {
      const sort: RecommendationSortConfig = {
        field: 'urgency',
        direction: 'asc',
      };

      const sorted = service.applySort(testRecommendations, sort);
      // Ascending urgency: emergent(0), urgent(1), soon(2), routine(3)
      // So 'urgent' comes before 'soon'
      expect(sorted[0].urgency).toBe('urgent');
      expect(sorted[1].urgency).toBe('soon');
    });

    it('should sort by urgency descending', () => {
      const sort: RecommendationSortConfig = {
        field: 'urgency',
        direction: 'desc',
      };

      const sorted = service.applySort(testRecommendations, sort);
      // Descending urgency: routine(3), soon(2), urgent(1), emergent(0)
      // So 'soon' comes before 'urgent'
      expect(sorted[0].urgency).toBe('soon');
      expect(sorted[1].urgency).toBe('urgent');
    });

    it('should sort by category ascending', () => {
      const sort: RecommendationSortConfig = {
        field: 'category',
        direction: 'asc',
      };

      const sorted = service.applySort(testRecommendations, sort);
      expect(sorted[0].category).toBe('chronic-disease');
      expect(sorted[1].category).toBe('preventive');
    });

    it('should sort by patient name ascending', () => {
      const sort: RecommendationSortConfig = {
        field: 'patientName',
        direction: 'asc',
      };

      const sorted = service.applySort(testRecommendations, sort);
      expect(sorted[0].patientName).toBe('Jane Smith');
      expect(sorted[1].patientName).toBe('John Doe');
    });

    it('should sort by risk level descending', () => {
      const sort: RecommendationSortConfig = {
        field: 'riskLevel',
        direction: 'desc',
      };

      const sorted = service.applySort(testRecommendations, sort);
      expect(sorted[0].patientRiskLevel).toBe('moderate');
      expect(sorted[1].patientRiskLevel).toBe('high');
    });

    it('should sort by due date ascending', () => {
      const sort: RecommendationSortConfig = {
        field: 'dueDate',
        direction: 'asc',
      };

      const sorted = service.applySort(testRecommendations, sort);
      expect(sorted[0].dueDate!.getTime()).toBeLessThan(sorted[1].dueDate!.getTime());
    });

    it('should sort by created date descending', () => {
      const sort: RecommendationSortConfig = {
        field: 'createdDate',
        direction: 'desc',
      };

      const sorted = service.applySort(testRecommendations, sort);
      expect(sorted[0].createdDate.getTime()).toBeGreaterThan(sorted[1].createdDate.getTime());
    });

    it('should handle recommendations without due dates', () => {
      const recsWithoutDates = [
        { ...mockRecommendation, dueDate: undefined },
        mockRecommendation2,
      ];

      const sort: RecommendationSortConfig = {
        field: 'dueDate',
        direction: 'asc',
      };

      const sorted = service.applySort(recsWithoutDates, sort);
      expect(sorted).toBeDefined();
      expect(sorted.length).toBe(2);
    });
  });

  // ==========================================================================
  // Cache Management Tests
  // ==========================================================================

  describe('Cache Management', () => {
    it('should invalidate all caches', (done) => {
      const mockRecommendations = [mockRecommendation];
      apiService.get.mockReturnValue(of(mockRecommendations));

      // Populate caches
      service.getDashboardRecommendations().subscribe(() => {
        service.getDashboardStats().subscribe(() => {
          // Invalidate
          service.invalidateCache();

          // Check caches are empty
          service.getCachedRecommendations$().subscribe((cached) => {
            expect(cached).toEqual([]);
            done();
          };
        });
      });
    });

    it('should return cached recommendations observable', (done) => {
      const mockRecommendations = [mockRecommendation];
      apiService.get.mockReturnValue(of(mockRecommendations));

      service.getDashboardRecommendations().subscribe(() => {
        service.getCachedRecommendations$().subscribe((cached) => {
          expect(cached.length).toBe(1);
          expect(cached[0].id).toBe('rec-1');
          done();
        };
      });
    });
  });

  // ==========================================================================
  // Update Notifications Tests
  // ==========================================================================

  describe('Update Notifications', () => {
    it('should emit updates via updates$ observable', (done) => {
      const mockRecommendations = [mockRecommendation];
      apiService.get.mockReturnValue(of(mockRecommendations));

      const updates: RecommendationUpdate[] = [];
      service.updates$.subscribe((update) => {
        if (update) {
          updates.push(update);
        }
      };

      service.getDashboardRecommendations().subscribe(() => {
        expect(updates.length).toBeGreaterThan(0);
        expect(updates[0].type).toBe('loaded');
        done();
      });
    });

    it('should include timestamp in all updates', (done) => {
      const mockRecommendations = [mockRecommendation];
      apiService.get.mockReturnValue(of(mockRecommendations));

      service.updates$.subscribe((update) => {
        if (update && update.type === 'loaded') {
          expect(update.timestamp).toBeInstanceOf(Date);
          done();
        }
      };

      service.getDashboardRecommendations().subscribe();
    });
  });
});
