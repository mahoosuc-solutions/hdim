import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { SalesSequenceService } from './sales-sequence.service';
import {
  EmailSequence,
  SequenceCreateRequest,
  SequenceEnrollment,
  SequenceAnalytics,
  PageResponse,
} from '../models/sales.model';
import { environment } from '../../environments/environment';

describe('SalesSequenceService', () => {
  let service: SalesSequenceService;
  let httpMock: HttpTestingController;
  const apiBaseUrl = environment.apiConfig.salesApiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SalesSequenceService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(SalesSequenceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ==========================================
  // Signal Initial State Tests
  // ==========================================

  describe('Initial State', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });

    it('should have empty sequences signal initially', () => {
      expect(service.sequences()).toEqual([]);
    });

    it('should have null currentSequence signal initially', () => {
      expect(service.currentSequence()).toBeNull();
    });

    it('should have isLoading as false initially', () => {
      expect(service.isLoading()).toBe(false);
    });

    it('should have error as null initially', () => {
      expect(service.error()).toBeNull();
    });
  });

  // ==========================================
  // Sequence CRUD Tests
  // ==========================================

  describe('getSequences', () => {
    const mockSequencesResponse: PageResponse<EmailSequence> = {
      content: [
        {
          id: 'sequence-1',
          name: 'Welcome Sequence',
          description: 'New lead welcome',
          status: 'ACTIVE',
          sequenceType: 'DRIP',
          targetType: 'LEAD',
          steps: [],
          totalEnrollments: 50,
          activeEnrollments: 25,
          completedEnrollments: 20,
          createdAt: '2026-01-01T00:00:00Z',
        },
        {
          id: 'sequence-2',
          name: 'Follow-up Sequence',
          description: 'Post-demo follow-up',
          status: 'INACTIVE',
          sequenceType: 'NURTURE',
          targetType: 'CONTACT',
          steps: [],
          totalEnrollments: 100,
          activeEnrollments: 0,
          completedEnrollments: 80,
          createdAt: '2026-01-02T00:00:00Z',
        },
      ],
      totalElements: 2,
      totalPages: 1,
      size: 20,
      number: 0,
    };

    it('should fetch sequences and update signal', fakeAsync(() => {
      let result: PageResponse<EmailSequence> | undefined;

      service.getSequences().subscribe((res) => (result = res));

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/sequences`);
      expect(req.request.method).toBe('GET');
      req.flush(mockSequencesResponse);

      tick();

      expect(result).toEqual(mockSequencesResponse);
      expect(service.sequences()).toEqual(mockSequencesResponse.content);
      expect(service.isLoading()).toBe(false);
    }));

    it('should set isLoading to true while fetching', () => {
      service.getSequences().subscribe();
      expect(service.isLoading()).toBe(true);

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/sequences`);
      req.flush(mockSequencesResponse);
    });

    it('should include pagination parameters when provided', fakeAsync(() => {
      service.getSequences({ page: 1, size: 10 }).subscribe();

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/sequences?page=1&size=10`);
      expect(req.request.method).toBe('GET');
      req.flush(mockSequencesResponse);
    }));

    it('should handle error and set error signal', fakeAsync(() => {
      let error: Error | undefined;

      service.getSequences().subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/sequences`);
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to load sequences');
      expect(service.isLoading()).toBe(false);
    }));
  });

  describe('getSequence', () => {
    const mockSequence: EmailSequence = {
      id: 'sequence-1',
      name: 'Welcome Sequence',
      description: 'New lead welcome',
      status: 'ACTIVE',
      sequenceType: 'DRIP',
      targetType: 'LEAD',
      steps: [
        {
          id: 'step-1',
          stepNumber: 1,
          stepType: 'EMAIL',
          subject: 'Welcome!',
          templateContent: 'Hello {{firstName}}!',
          delayDays: 0,
        },
      ],
      totalEnrollments: 50,
      activeEnrollments: 25,
      completedEnrollments: 20,
      createdAt: '2026-01-01T00:00:00Z',
    };

    it('should fetch single sequence and update currentSequence signal', fakeAsync(() => {
      let result: EmailSequence | undefined;

      service.getSequence('sequence-1').subscribe((res) => (result = res));

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/sequences/sequence-1`);
      expect(req.request.method).toBe('GET');
      req.flush(mockSequence);

      tick();

      expect(result).toEqual(mockSequence);
      expect(service.currentSequence()).toEqual(mockSequence);
    }));

    it('should handle 404 error', fakeAsync(() => {
      let error: Error | undefined;

      service.getSequence('non-existent').subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/sequences/non-existent`);
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to load sequence');
    }));
  });

  describe('createSequence', () => {
    const newSequence: SequenceCreateRequest = {
      name: 'New Sequence',
      description: 'New description',
      sequenceType: 'DRIP',
      targetType: 'LEAD',
      steps: [
        {
          stepNumber: 1,
          stepType: 'EMAIL',
          subject: 'Step 1',
          templateContent: 'Hello!',
          delayDays: 0,
        },
      ],
    };

    const createdSequence: EmailSequence = {
      id: 'new-sequence-id',
      name: 'New Sequence',
      description: 'New description',
      status: 'INACTIVE',
      sequenceType: 'DRIP',
      targetType: 'LEAD',
      steps: newSequence.steps || [],
      totalEnrollments: 0,
      activeEnrollments: 0,
      completedEnrollments: 0,
      createdAt: '2026-01-01T00:00:00Z',
    };

    it('should create sequence and update sequences signal', fakeAsync(() => {
      let result: EmailSequence | undefined;

      service.createSequence(newSequence).subscribe((res) => (result = res));

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/sequences`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(newSequence);
      req.flush(createdSequence);

      tick();

      expect(result).toEqual(createdSequence);
      expect(service.sequences()).toContainEqual(createdSequence);
    }));

    it('should handle error', fakeAsync(() => {
      let error: Error | undefined;

      service.createSequence(newSequence).subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/sequences`);
      req.flush('Bad Request', { status: 400, statusText: 'Bad Request' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to create sequence');
    }));
  });

  describe('updateSequence', () => {
    const updateData: Partial<SequenceCreateRequest> = {
      name: 'Updated Sequence',
      description: 'Updated description',
    };

    const updatedSequence: EmailSequence = {
      id: 'sequence-1',
      name: 'Updated Sequence',
      description: 'Updated description',
      status: 'ACTIVE',
      sequenceType: 'DRIP',
      targetType: 'LEAD',
      steps: [],
      totalEnrollments: 50,
      activeEnrollments: 25,
      completedEnrollments: 20,
      createdAt: '2026-01-01T00:00:00Z',
    };

    it('should update sequence and update signals', fakeAsync(() => {
      // Set initial sequences
      const initialSequence: EmailSequence = { ...updatedSequence, name: 'Original' };
      service['_sequences'].set([initialSequence]);
      service['_currentSequence'].set(initialSequence);

      let result: EmailSequence | undefined;

      service.updateSequence('sequence-1', updateData).subscribe((res) => (result = res));

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/sequences/sequence-1`);
      expect(req.request.method).toBe('PUT');
      req.flush(updatedSequence);

      tick();

      expect(result).toEqual(updatedSequence);
      expect(service.sequences()[0].name).toBe('Updated Sequence');
      expect(service.currentSequence()?.name).toBe('Updated Sequence');
    }));

    it('should handle 404 error', fakeAsync(() => {
      let error: Error | undefined;

      service.updateSequence('non-existent', updateData).subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/sequences/non-existent`);
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to update sequence');
    }));
  });

  describe('deleteSequence', () => {
    it('should delete sequence and remove from signals', fakeAsync(() => {
      // Set initial sequences
      const sequences: EmailSequence[] = [
        {
          id: 'sequence-1',
          name: 'To Delete',
          status: 'INACTIVE',
          sequenceType: 'DRIP',
          targetType: 'LEAD',
          steps: [],
          totalEnrollments: 0,
          activeEnrollments: 0,
          completedEnrollments: 0,
          createdAt: '2026-01-01T00:00:00Z',
        },
        {
          id: 'sequence-2',
          name: 'Keep',
          status: 'ACTIVE',
          sequenceType: 'NURTURE',
          targetType: 'CONTACT',
          steps: [],
          totalEnrollments: 10,
          activeEnrollments: 5,
          completedEnrollments: 3,
          createdAt: '2026-01-02T00:00:00Z',
        },
      ];
      service['_sequences'].set(sequences);
      service['_currentSequence'].set(sequences[0]);

      let completed = false;
      service.deleteSequence('sequence-1').subscribe(() => (completed = true));

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/sequences/sequence-1`);
      expect(req.request.method).toBe('DELETE');
      req.flush(null);

      tick();

      expect(completed).toBe(true);
      expect(service.sequences().length).toBe(1);
      expect(service.sequences()[0].id).toBe('sequence-2');
      expect(service.currentSequence()).toBeNull();
    }));

    it('should handle error', fakeAsync(() => {
      let error: Error | undefined;

      service.deleteSequence('non-existent').subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/sequences/non-existent`);
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to delete sequence');
    }));
  });

  // ==========================================
  // Sequence Actions Tests
  // ==========================================

  describe('activateSequence', () => {
    const activatedSequence: EmailSequence = {
      id: 'sequence-1',
      name: 'Test Sequence',
      status: 'ACTIVE',
      sequenceType: 'DRIP',
      targetType: 'LEAD',
      steps: [],
      totalEnrollments: 0,
      activeEnrollments: 0,
      completedEnrollments: 0,
      createdAt: '2026-01-01T00:00:00Z',
    };

    it('should activate sequence and update status in signal', fakeAsync(() => {
      // Set initial inactive sequence
      const inactiveSequence: EmailSequence = { ...activatedSequence, status: 'INACTIVE' };
      service['_sequences'].set([inactiveSequence]);

      let result: EmailSequence | undefined;

      service.activateSequence('sequence-1').subscribe((res) => (result = res));

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/sequences/sequence-1/activate`);
      expect(req.request.method).toBe('POST');
      req.flush(activatedSequence);

      tick();

      expect(result).toEqual(activatedSequence);
      expect(service.sequences()[0].status).toBe('ACTIVE');
    }));

    it('should handle error', fakeAsync(() => {
      let error: Error | undefined;

      service.activateSequence('non-existent').subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/sequences/non-existent/activate`);
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to activate sequence');
    }));
  });

  describe('deactivateSequence', () => {
    const deactivatedSequence: EmailSequence = {
      id: 'sequence-1',
      name: 'Test Sequence',
      status: 'INACTIVE',
      sequenceType: 'DRIP',
      targetType: 'LEAD',
      steps: [],
      totalEnrollments: 50,
      activeEnrollments: 0,
      completedEnrollments: 45,
      createdAt: '2026-01-01T00:00:00Z',
    };

    it('should deactivate sequence and update status in signal', fakeAsync(() => {
      // Set initial active sequence
      const activeSequence: EmailSequence = { ...deactivatedSequence, status: 'ACTIVE' };
      service['_sequences'].set([activeSequence]);

      let result: EmailSequence | undefined;

      service.deactivateSequence('sequence-1').subscribe((res) => (result = res));

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/sequences/sequence-1/deactivate`);
      expect(req.request.method).toBe('POST');
      req.flush(deactivatedSequence);

      tick();

      expect(result).toEqual(deactivatedSequence);
      expect(service.sequences()[0].status).toBe('INACTIVE');
    }));
  });

  // ==========================================
  // Enrollment Management Tests
  // ==========================================

  describe('enrollLead', () => {
    const mockEnrollment: SequenceEnrollment = {
      id: 'enrollment-1',
      sequenceId: 'sequence-1',
      leadId: 'lead-1',
      status: 'ACTIVE',
      currentStep: 1,
      enrolledAt: '2026-01-01T00:00:00Z',
    };

    it('should enroll lead in sequence', fakeAsync(() => {
      let result: SequenceEnrollment | undefined;

      service.enrollLead('sequence-1', 'lead-1').subscribe((res) => (result = res));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/sequences/sequence-1/enroll/lead/lead-1`
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockEnrollment);

      tick();

      expect(result).toEqual(mockEnrollment);
    }));

    it('should handle error', fakeAsync(() => {
      let error: Error | undefined;

      service.enrollLead('sequence-1', 'lead-1').subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/sequences/sequence-1/enroll/lead/lead-1`
      );
      req.flush('Bad Request', { status: 400, statusText: 'Bad Request' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to enroll lead');
    }));
  });

  describe('enrollContact', () => {
    const mockEnrollment: SequenceEnrollment = {
      id: 'enrollment-1',
      sequenceId: 'sequence-1',
      contactId: 'contact-1',
      status: 'ACTIVE',
      currentStep: 1,
      enrolledAt: '2026-01-01T00:00:00Z',
    };

    it('should enroll contact in sequence', fakeAsync(() => {
      let result: SequenceEnrollment | undefined;

      service.enrollContact('sequence-1', 'contact-1').subscribe((res) => (result = res));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/sequences/sequence-1/enroll/contact/contact-1`
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockEnrollment);

      tick();

      expect(result).toEqual(mockEnrollment);
    }));
  });

  describe('getEnrollments', () => {
    const mockEnrollments: SequenceEnrollment[] = [
      {
        id: 'enrollment-1',
        sequenceId: 'sequence-1',
        leadId: 'lead-1',
        status: 'ACTIVE',
        currentStep: 2,
        enrolledAt: '2026-01-01T00:00:00Z',
      },
      {
        id: 'enrollment-2',
        sequenceId: 'sequence-1',
        contactId: 'contact-1',
        status: 'COMPLETED',
        currentStep: 5,
        enrolledAt: '2026-01-02T00:00:00Z',
        completedAt: '2026-01-10T00:00:00Z',
      },
    ];

    it('should fetch enrollments for sequence', fakeAsync(() => {
      let result: SequenceEnrollment[] | undefined;

      service.getEnrollments('sequence-1').subscribe((res) => (result = res));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/sequences/sequence-1/enrollments`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockEnrollments);

      tick();

      expect(result).toEqual(mockEnrollments);
    }));

    it('should return empty array on error', fakeAsync(() => {
      let result: SequenceEnrollment[] | undefined;

      service.getEnrollments('sequence-1').subscribe((res) => (result = res));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/sequences/sequence-1/enrollments`
      );
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

      tick();

      expect(result).toEqual([]);
    }));
  });

  describe('pauseEnrollment', () => {
    const pausedEnrollment: SequenceEnrollment = {
      id: 'enrollment-1',
      sequenceId: 'sequence-1',
      leadId: 'lead-1',
      status: 'PAUSED',
      currentStep: 2,
      enrolledAt: '2026-01-01T00:00:00Z',
    };

    it('should pause enrollment', fakeAsync(() => {
      let result: SequenceEnrollment | undefined;

      service.pauseEnrollment('enrollment-1').subscribe((res) => (result = res));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/sequences/enrollments/enrollment-1/pause`
      );
      expect(req.request.method).toBe('POST');
      req.flush(pausedEnrollment);

      tick();

      expect(result).toEqual(pausedEnrollment);
    }));

    it('should handle error', fakeAsync(() => {
      let error: Error | undefined;

      service.pauseEnrollment('non-existent').subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/sequences/enrollments/non-existent/pause`
      );
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to pause enrollment');
    }));
  });

  describe('resumeEnrollment', () => {
    const resumedEnrollment: SequenceEnrollment = {
      id: 'enrollment-1',
      sequenceId: 'sequence-1',
      leadId: 'lead-1',
      status: 'ACTIVE',
      currentStep: 2,
      enrolledAt: '2026-01-01T00:00:00Z',
    };

    it('should resume enrollment', fakeAsync(() => {
      let result: SequenceEnrollment | undefined;

      service.resumeEnrollment('enrollment-1').subscribe((res) => (result = res));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/sequences/enrollments/enrollment-1/resume`
      );
      expect(req.request.method).toBe('POST');
      req.flush(resumedEnrollment);

      tick();

      expect(result).toEqual(resumedEnrollment);
    }));
  });

  describe('removeEnrollment', () => {
    it('should remove enrollment', fakeAsync(() => {
      let completed = false;

      service.removeEnrollment('enrollment-1').subscribe(() => (completed = true));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/sequences/enrollments/enrollment-1`
      );
      expect(req.request.method).toBe('DELETE');
      req.flush(null);

      tick();

      expect(completed).toBe(true);
    }));

    it('should handle error', fakeAsync(() => {
      let error: Error | undefined;

      service.removeEnrollment('non-existent').subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/sequences/enrollments/non-existent`
      );
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to remove enrollment');
    }));
  });

  // ==========================================
  // Analytics Tests
  // ==========================================

  describe('getSequenceAnalytics', () => {
    const mockAnalytics: SequenceAnalytics = {
      sequenceId: 'sequence-1',
      totalEnrollments: 100,
      activeEnrollments: 45,
      completedEnrollments: 50,
      pausedEnrollments: 5,
      openRate: 65.5,
      clickRate: 23.2,
      replyRate: 12.5,
      unsubscribeRate: 2.1,
      stepBreakdown: [
        { stepNumber: 1, sent: 100, opened: 70, clicked: 30, replied: 15 },
        { stepNumber: 2, sent: 85, opened: 55, clicked: 22, replied: 10 },
      ],
    };

    it('should fetch sequence analytics', fakeAsync(() => {
      let result: SequenceAnalytics | undefined;

      service.getSequenceAnalytics('sequence-1').subscribe((res) => (result = res));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/sequences/sequence-1/analytics`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockAnalytics);

      tick();

      expect(result).toEqual(mockAnalytics);
    }));

    it('should handle error', fakeAsync(() => {
      let error: Error | undefined;

      service.getSequenceAnalytics('sequence-1').subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/sequences/sequence-1/analytics`
      );
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to load analytics');
    }));
  });
});
