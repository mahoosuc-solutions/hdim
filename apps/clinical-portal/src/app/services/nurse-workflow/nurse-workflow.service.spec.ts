/**
 * Unit tests for NurseWorkflowService
 *
 * Tests:
 * - HTTP client interactions with backend API
 * - Observable emissions and transformations
 * - Error handling and recovery
 * - Multi-tenant context enforcement
 * - Caching behavior
 * - Pagination support
 */

import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NurseWorkflowService } from './nurse-workflow.service';
import {
  OutreachLog,
  MedicationReconciliation,
  PatientEducationLog,
  ReferralCoordination,
  NurseWorkflowContext,
  ContactMethod,
  OutcomeType,
  OutreachReason,
  MedicationReconciliationStatus,
  PatientUnderstanding,
  EducationMaterialType,
  EducationDeliveryMethod,
  ReferralStatus,
  ReferralPriority,
} from './nurse-workflow.models';

describe('NurseWorkflowService', () => {
  let service: NurseWorkflowService;
  let httpMock: HttpTestingController;

  const tenantId = 'TENANT001';
  const patientId = '550e8400-e29b-41d4-a716-446655440000';
  const nurseId = '660e8400-e29b-41d4-a716-446655440001';

  const mockOutreachLog: OutreachLog = {
    id: '550e8400-e29b-41d4-a716-446655440010',
    patientId,
    tenantId,
    nurseId,
    contactMethod: ContactMethod.PHONE,
    outcomeType: OutcomeType.SUCCESSFUL_CONTACT,
    reason: OutreachReason.CARE_GAP,
    contactedAt: new Date().toISOString(),
    notes: 'Patient informed about care gap',
  };

  const mockMedicationReconciliation: MedicationReconciliation = {
    id: '550e8400-e29b-41d4-a716-446655440020',
    patientId,
    tenantId,
    reconcilerId: nurseId,
    status: MedicationReconciliationStatus.IN_PROGRESS,
    triggerType: 'HOSPITAL_DISCHARGE',
    medicationCount: 5,
    discrepancyCount: 1,
    patientEducationProvided: false,
    patientUnderstanding: PatientUnderstanding.GOOD,
    authorizationStatus: 'APPROVED',
    startedAt: new Date().toISOString(),
  };

  const mockPatientEducationLog: PatientEducationLog = {
    id: '550e8400-e29b-41d4-a716-446655440030',
    patientId,
    tenantId,
    educatorId: nurseId,
    materialType: EducationMaterialType.DIABETES_MANAGEMENT,
    deliveryMethod: EducationDeliveryMethod.IN_PERSON,
    patientUnderstanding: PatientUnderstanding.GOOD,
    interpreterUsed: false,
    caregiverInvolved: false,
    followUpNeeded: false,
    deliveredAt: new Date().toISOString(),
  };

  const mockReferralCoordination: ReferralCoordination = {
    id: '550e8400-e29b-41d4-a716-446655440040',
    patientId,
    tenantId,
    coordinatorId: nurseId,
    specialtyType: 'Cardiology',
    status: ReferralStatus.PENDING_AUTHORIZATION,
    priority: ReferralPriority.ROUTINE,
    authorizationStatus: 'PENDING',
    appointmentStatus: 'NOT_SCHEDULED',
    resultsStatus: 'NOT_RECEIVED',
    medicalRecordsTransmitted: false,
    requestedAt: new Date().toISOString(),
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [NurseWorkflowService],
    });

    service = TestBed.inject(NurseWorkflowService);
    httpMock = TestBed.inject(HttpTestingController);
    service.setTenantContext(tenantId);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('Tenant Context Management', () => {
    it('should set and retrieve tenant context', () => {
      service.setTenantContext('NEW_TENANT');
      expect(service.getTenantContext()).toBe('NEW_TENANT');
    });

    it('should throw error when making requests without tenant context', () => {
      const unsetService = new NurseWorkflowService(TestBed.inject(HttpClientTestingModule) as any);
      expect(() => unsetService.getPatientOutreachLogs(patientId, 0, 10)).toThrowError('Tenant context not set');
    });
  });

  describe('Outreach Log Service', () => {
    it('should create outreach log', (done) => {
      service.createOutreachLog(mockOutreachLog).subscribe((result) => {
        expect(result.id).toBeDefined();
        expect(result.patientId).toBe(patientId);
        expect(result.outcomeType).toBe(OutcomeType.SUCCESSFUL_CONTACT);
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes('outreach-logs'));
      expect(req.request.method).toBe('POST');
      expect(req.request.headers.get('X-Tenant-ID')).toBe(tenantId);
      req.flush(mockOutreachLog);
    });

    it('should fetch patient outreach logs with pagination', (done) => {
      const mockLogs = [mockOutreachLog];
      const mockResponse = {
        content: mockLogs,
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getPatientOutreachLogs(patientId, 0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        expect(result.content[0].patientId).toBe(patientId);
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes('outreach-logs/patient'));
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');
      req.flush(mockResponse);
    });

    it('should fetch outreach logs by outcome type', (done) => {
      const mockLogs = [mockOutreachLog];
      service.getOutreachLogsByOutcome(OutcomeType.SUCCESSFUL_CONTACT, 0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        expect(result.content[0].outcomeType).toBe(OutcomeType.SUCCESSFUL_CONTACT);
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes('outreach-logs/outcome'));
      req.flush({ content: mockLogs, totalElements: 1 });
    });

    it('should update outreach log', (done) => {
      const updatedLog = { ...mockOutreachLog, notes: 'Updated notes' };
      service.updateOutreachLog(mockOutreachLog.id!, updatedLog).subscribe((result) => {
        expect(result.notes).toBe('Updated notes');
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes(`outreach-logs/${mockOutreachLog.id}`));
      expect(req.request.method).toBe('PUT');
      req.flush(updatedLog);
    });
  });

  describe('Medication Reconciliation Service', () => {
    it('should start medication reconciliation', (done) => {
      service.startMedicationReconciliation(mockMedicationReconciliation).subscribe((result) => {
        expect(result.status).toBe(MedicationReconciliationStatus.IN_PROGRESS);
        expect(result.medicationCount).toBe(5);
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes('medication-reconciliations'));
      expect(req.request.method).toBe('POST');
      req.flush(mockMedicationReconciliation);
    });

    it('should complete medication reconciliation', (done) => {
      const completedMedRec = {
        ...mockMedicationReconciliation,
        status: MedicationReconciliationStatus.COMPLETED,
        completedAt: new Date().toISOString(),
      };

      service.completeMedicationReconciliation(completedMedRec).subscribe((result) => {
        expect(result.status).toBe(MedicationReconciliationStatus.COMPLETED);
        expect(result.completedAt).toBeDefined();
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes('medication-reconciliations/complete'));
      expect(req.request.method).toBe('PUT');
      req.flush(completedMedRec);
    });

    it('should fetch pending medication reconciliations', (done) => {
      const mockPending = [mockMedicationReconciliation];
      service.getPendingMedicationReconciliations(0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        expect(result.content[0].status).toBe(MedicationReconciliationStatus.IN_PROGRESS);
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes('medication-reconciliations/pending'));
      req.flush({
        content: mockPending,
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      });
    });

    it('should fetch medication reconciliation metrics', (done) => {
      const mockMetrics = {
        totalReconciliations: 10,
        pendingReconciliations: 3,
        completionRate: 70,
      };

      service.getMedicationReconciliationMetrics().subscribe((metrics) => {
        expect(metrics.totalReconciliations).toBe(10);
        expect(metrics.completionRate).toBe(70);
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes('medication-reconciliations/metrics/summary'));
      req.flush(mockMetrics);
    });
  });

  describe('Patient Education Service', () => {
    it('should log patient education delivery', (done) => {
      service.logPatientEducation(mockPatientEducationLog).subscribe((result) => {
        expect(result.materialType).toBe(EducationMaterialType.DIABETES_MANAGEMENT);
        expect(result.deliveryMethod).toBe(EducationDeliveryMethod.IN_PERSON);
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes('patient-education'));
      expect(req.request.method).toBe('POST');
      req.flush(mockPatientEducationLog);
    });

    it('should fetch patient education history', (done) => {
      const mockLogs = [mockPatientEducationLog];
      service.getPatientEducationHistory(patientId, 0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        expect(result.content[0].patientId).toBe(patientId);
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes('patient-education/patient'));
      req.flush({ content: mockLogs, totalElements: 1 });
    });

    it('should fetch sessions with poor understanding', (done) => {
      const poorSession = {
        ...mockPatientEducationLog,
        patientUnderstanding: PatientUnderstanding.POOR,
      };

      service.getEducationSessionsWithPoorUnderstanding().subscribe((result) => {
        expect(result.length).toBe(1);
        expect(result[0].patientUnderstanding).toBe(PatientUnderstanding.POOR);
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes('patient-education/poor-understanding'));
      req.flush([poorSession]);
    });

    it('should fetch patient education metrics', (done) => {
      const mockMetrics = {
        totalEducationSessions: 5,
        sessionsWithPoorUnderstanding: 1,
        materialTypesCovered: [EducationMaterialType.DIABETES_MANAGEMENT],
      };

      service.getPatientEducationMetrics(patientId).subscribe((metrics) => {
        expect(metrics.totalEducationSessions).toBe(5);
        expect(metrics.sessionsWithPoorUnderstanding).toBe(1);
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes(`patient-education/metrics/${patientId}`));
      req.flush(mockMetrics);
    });
  });

  describe('Referral Coordination Service', () => {
    it('should create referral', (done) => {
      service.createReferral(mockReferralCoordination).subscribe((result) => {
        expect(result.specialtyType).toBe('Cardiology');
        expect(result.status).toBe(ReferralStatus.PENDING_AUTHORIZATION);
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes('referral-coordinations'));
      expect(req.request.method).toBe('POST');
      req.flush(mockReferralCoordination);
    });

    it('should fetch pending referrals', (done) => {
      const mockReferrals = [mockReferralCoordination];
      service.getPendingReferrals(0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        expect(result.content[0].status).toBe(ReferralStatus.PENDING_AUTHORIZATION);
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes('referral-coordinations/pending'));
      req.flush({ content: mockReferrals, totalElements: 1 });
    });

    it('should fetch urgent referrals awaiting scheduling', (done) => {
      const urgentReferral = { ...mockReferralCoordination, priority: ReferralPriority.URGENT };
      service.getUrgentReferralsAwaitingScheduling().subscribe((result) => {
        expect(result.length).toBe(1);
        expect(result[0].priority).toBe(ReferralPriority.URGENT);
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes('referral-coordinations/urgent-awaiting-scheduling'));
      req.flush([urgentReferral]);
    });

    it('should update referral', (done) => {
      const updatedReferral = {
        ...mockReferralCoordination,
        status: ReferralStatus.AUTHORIZED,
      };

      service.updateReferral(mockReferralCoordination.id!, updatedReferral).subscribe((result) => {
        expect(result.status).toBe(ReferralStatus.AUTHORIZED);
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes(`referral-coordinations/${mockReferralCoordination.id}`));
      expect(req.request.method).toBe('PUT');
      req.flush(updatedReferral);
    });

    it('should fetch referral metrics', (done) => {
      const mockMetrics = {
        totalReferrals: 15,
        pendingReferrals: 5,
        completionRate: 67,
        awaitingScheduling: 3,
        awaitingResults: 2,
      };

      service.getReferralMetrics().subscribe((metrics) => {
        expect(metrics.totalReferrals).toBe(15);
        expect(metrics.completionRate).toBe(67);
        done();
      });

      const req = httpMock.expectOne((r) => r.url.includes('referral-coordinations/metrics/summary'));
      req.flush(mockMetrics);
    });
  });

  describe('Error Handling', () => {
    it('should handle 404 errors gracefully', (done) => {
      service.getPatientOutreachLogs(patientId, 0, 10).subscribe(
        () => fail('should not succeed'),
        (error) => {
          expect(error.status).toBe(404);
          done();
        }
      );

      const req = httpMock.expectOne((r) => r.url.includes('outreach-logs/patient'));
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });

    it('should handle 401 Unauthorized errors', (done) => {
      service.createOutreachLog(mockOutreachLog).subscribe(
        () => fail('should not succeed'),
        (error) => {
          expect(error.status).toBe(401);
          done();
        }
      );

      const req = httpMock.expectOne((r) => r.url.includes('outreach-logs'));
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle 500 server errors', (done) => {
      service.getMedicationReconciliationMetrics().subscribe(
        () => fail('should not succeed'),
        (error) => {
          expect(error.status).toBe(500);
          done();
        }
      );

      const req = httpMock.expectOne((r) => r.url.includes('medication-reconciliations/metrics'));
      req.flush('Internal Server Error', { status: 500, statusText: 'Internal Server Error' });
    });
  });

  describe('Caching Behavior', () => {
    it('should cache metrics with TTL', (done) => {
      const mockMetrics = {
        totalReconciliations: 10,
        pendingReconciliations: 3,
        completionRate: 70,
      };

      // First call - should hit backend
      service.getMedicationReconciliationMetrics().subscribe(() => {
        // Second call - should use cache
        service.getMedicationReconciliationMetrics().subscribe(() => {
          // Verify only one HTTP call was made
          const requests = httpMock.match((r) => r.url.includes('medication-reconciliations/metrics'));
          expect(requests.length).toBe(1);
          done();
        });
      });

      const req = httpMock.expectOne((r) => r.url.includes('medication-reconciliations/metrics'));
      req.flush(mockMetrics);
    });

    it('should invalidate cache when operations modify data', (done) => {
      // Create an outreach log (modifies data)
      service.createOutreachLog(mockOutreachLog).subscribe(() => {
        // Cache should be invalidated, next fetch should hit backend
        service.getPatientOutreachLogs(patientId, 0, 10).subscribe(() => {
          done();
        });

        const secondReq = httpMock.expectOne((r) => r.url.includes('outreach-logs/patient'));
        secondReq.flush({ content: [mockOutreachLog], totalElements: 1 });
      });

      const firstReq = httpMock.expectOne((r) => r.url.includes('outreach-logs'));
      firstReq.flush(mockOutreachLog);
    });
  });
});
