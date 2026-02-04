import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of, BehaviorSubject } from 'rxjs';
import { signal } from '@angular/core';
import { SalesSequencesComponent } from './sales-sequences.component';
import { SalesSequenceService } from '../../../services/sales-sequence.service';
import { SalesService } from '../../../services/sales.service';
import { EmailSequence, SequenceStatus, Lead } from '../../../models/sales.model';

describe('SalesSequencesComponent', () => {
  let component: SalesSequencesComponent;
  let fixture: ComponentFixture<SalesSequencesComponent>;
  let mockSequenceService: jasmine.SpyObj<SalesSequenceService>;
  let mockSalesService: jasmine.SpyObj<SalesService>;

  const mockSequences: EmailSequence[] = [
    {
      id: '1',
      name: 'Welcome Sequence',
      description: 'Onboarding emails for new leads',
      status: 'ACTIVE' as SequenceStatus,
      steps: [
        { id: '1', stepNumber: 1, type: 'EMAIL', subject: 'Welcome!', content: 'Hello', delayDays: 0, delayHours: 0 },
        { id: '2', stepNumber: 2, type: 'WAIT', delayDays: 3, delayHours: 0 },
        { id: '3', stepNumber: 3, type: 'EMAIL', subject: 'Follow up', content: 'Checking in', delayDays: 0, delayHours: 0 },
      ],
      enrollmentCount: 42,
      completedCount: 15,
      replyRate: 0.125,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-15T00:00:00Z',
    },
    {
      id: '2',
      name: 'Re-engagement',
      description: 'Win back inactive leads',
      status: 'DRAFT' as SequenceStatus,
      steps: [],
      enrollmentCount: 0,
      completedCount: 0,
      replyRate: 0,
      createdAt: '2026-01-10T00:00:00Z',
      updatedAt: '2026-01-10T00:00:00Z',
    },
  ];

  const mockLeads: Lead[] = [
    { id: '1', firstName: 'John', lastName: 'Doe', email: 'john@example.com', company: 'Acme', status: 'OPEN', source: 'WEBSITE' },
    { id: '2', firstName: 'Jane', lastName: 'Smith', email: 'jane@example.com', company: 'Tech', status: 'QUALIFIED', source: 'LINKEDIN' },
  ];

  beforeEach(async () => {
    mockSequenceService = jasmine.createSpyObj('SalesSequenceService', [
      'getSequences',
      'getSequence',
      'createSequence',
      'updateSequence',
      'deleteSequence',
      'activateSequence',
      'deactivateSequence',
      'getSequenceAnalytics',
      'getEnrollments',
      'enrollLead',
      'pauseEnrollment',
      'resumeEnrollment',
    ]);
    mockSequenceService.isLoading = signal(false);

    mockSalesService = jasmine.createSpyObj('SalesService', ['getLeads']);

    // Default mock returns
    mockSequenceService.getSequences.and.returnValue(of({
      content: mockSequences,
      totalPages: 1,
      totalElements: 2,
      size: 20,
      number: 0,
    }));
    mockSequenceService.getSequenceAnalytics.and.returnValue(of({
      sequenceId: '1',
      totalEnrolled: 42,
      active: 27,
      completed: 15,
      paused: 0,
      emailsSent: 100,
      emailsOpened: 60,
      emailsClicked: 20,
      replies: 12,
      openRate: 0.6,
      clickRate: 0.2,
      replyRate: 0.12,
    }));
    mockSequenceService.getEnrollments.and.returnValue(of([]));
    mockSalesService.getLeads.and.returnValue(of({
      content: mockLeads,
      totalPages: 1,
      totalElements: 2,
      size: 100,
      number: 0,
    }));

    await TestBed.configureTestingModule({
      imports: [SalesSequencesComponent, FormsModule],
      providers: [
        { provide: SalesSequenceService, useValue: mockSequenceService },
        { provide: SalesService, useValue: mockSalesService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SalesSequencesComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should load sequences on init', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      expect(mockSequenceService.getSequences).toHaveBeenCalled();
      expect(component.sequences().length).toBe(2);
    }));

    it('should load leads on init', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      expect(mockSalesService.getLeads).toHaveBeenCalled();
      expect(component.leads().length).toBe(2);
    }));
  });

  describe('Filtering', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should filter sequences by search query', () => {
      component.searchQuery = 'welcome';
      const filtered = component.filteredSequences();

      expect(filtered.length).toBe(1);
      expect(filtered[0].name).toBe('Welcome Sequence');
    });

    it('should filter sequences by status', () => {
      component.statusFilter = 'DRAFT';
      const filtered = component.filteredSequences();

      expect(filtered.length).toBe(1);
      expect(filtered[0].name).toBe('Re-engagement');
    });

    it('should return all sequences when no filters applied', () => {
      component.searchQuery = '';
      component.statusFilter = '';
      const filtered = component.filteredSequences();

      expect(filtered.length).toBe(2);
    });

    it('should combine search and status filters', () => {
      component.searchQuery = 'welcome';
      component.statusFilter = 'ACTIVE';
      const filtered = component.filteredSequences();

      expect(filtered.length).toBe(1);
      expect(filtered[0].name).toBe('Welcome Sequence');
    });

    it('should return empty when filters match nothing', () => {
      component.searchQuery = 'nonexistent';
      const filtered = component.filteredSequences();

      expect(filtered.length).toBe(0);
    });
  });

  describe('Sequence Selection', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should select a sequence and load details', fakeAsync(() => {
      component.selectSequence(mockSequences[0]);
      tick();

      expect(component.selectedSequence()).toEqual(mockSequences[0]);
      expect(mockSequenceService.getSequenceAnalytics).toHaveBeenCalledWith('1');
      expect(mockSequenceService.getEnrollments).toHaveBeenCalledWith('1');
    }));

    it('should close detail panel when closeDetail is called', fakeAsync(() => {
      component.selectSequence(mockSequences[0]);
      tick();

      expect(component.selectedSequence()).toBeTruthy();

      component.closeDetail();

      expect(component.selectedSequence()).toBeNull();
    }));
  });

  describe('Create/Edit Dialog', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should open create dialog with empty form', () => {
      component.openCreateDialog();

      expect(component.showCreateDialog).toBeTrue();
      expect(component.editingSequence).toBeNull();
      expect(component.sequenceForm.name).toBe('');
      expect(component.sequenceForm.steps.length).toBe(0);
    });

    it('should open edit dialog with sequence data', () => {
      component.editSequence(mockSequences[0]);

      expect(component.showCreateDialog).toBeTrue();
      expect(component.editingSequence).toEqual(mockSequences[0]);
      expect(component.sequenceForm.name).toBe('Welcome Sequence');
      expect(component.sequenceForm.steps.length).toBe(3);
    });

    it('should close dialog and reset form', () => {
      component.openCreateDialog();
      component.sequenceForm.name = 'Test';
      component.closeDialog();

      expect(component.showCreateDialog).toBeFalse();
    });

    it('should create new sequence on save', fakeAsync(() => {
      mockSequenceService.createSequence.and.returnValue(of(mockSequences[0]));

      component.openCreateDialog();
      component.sequenceForm.name = 'New Sequence';
      component.sequenceForm.description = 'Test';
      component.saveSequence();
      tick();

      expect(mockSequenceService.createSequence).toHaveBeenCalled();
      expect(component.showCreateDialog).toBeFalse();
    }));

    it('should update existing sequence on save', fakeAsync(() => {
      mockSequenceService.updateSequence.and.returnValue(of(mockSequences[0]));

      component.editSequence(mockSequences[0]);
      component.sequenceForm.name = 'Updated Name';
      component.saveSequence();
      tick();

      expect(mockSequenceService.updateSequence).toHaveBeenCalledWith('1', jasmine.any(Object));
    }));
  });

  describe('Sequence Actions', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should toggle sequence status to inactive', fakeAsync(() => {
      mockSequenceService.deactivateSequence.and.returnValue(of({
        ...mockSequences[0],
        status: 'INACTIVE' as SequenceStatus,
      }));

      component.toggleStatus(mockSequences[0]);
      tick();

      expect(mockSequenceService.deactivateSequence).toHaveBeenCalledWith('1');
    }));

    it('should toggle sequence status to active', fakeAsync(() => {
      const inactiveSequence = { ...mockSequences[1], status: 'INACTIVE' as SequenceStatus };
      mockSequenceService.activateSequence.and.returnValue(of({
        ...inactiveSequence,
        status: 'ACTIVE' as SequenceStatus,
      }));

      component.toggleStatus(inactiveSequence);
      tick();

      expect(mockSequenceService.activateSequence).toHaveBeenCalledWith('2');
    }));

    it('should delete sequence with confirmation', fakeAsync(() => {
      spyOn(window, 'confirm').and.returnValue(true);
      mockSequenceService.deleteSequence.and.returnValue(of(void 0));

      component.deleteSequence(mockSequences[0]);
      tick();

      expect(mockSequenceService.deleteSequence).toHaveBeenCalledWith('1');
    }));

    it('should not delete sequence when confirmation cancelled', fakeAsync(() => {
      spyOn(window, 'confirm').and.returnValue(false);

      component.deleteSequence(mockSequences[0]);
      tick();

      expect(mockSequenceService.deleteSequence).not.toHaveBeenCalled();
    }));
  });

  describe('Step Management', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
      component.openCreateDialog();
    }));

    it('should add a new step', () => {
      expect(component.sequenceForm.steps.length).toBe(0);

      component.addStep();

      expect(component.sequenceForm.steps.length).toBe(1);
      expect(component.sequenceForm.steps[0].type).toBe('EMAIL');
      expect(component.sequenceForm.steps[0].stepNumber).toBe(1);
    });

    it('should remove a step', () => {
      component.addStep();
      component.addStep();
      expect(component.sequenceForm.steps.length).toBe(2);

      component.removeStep(0);

      expect(component.sequenceForm.steps.length).toBe(1);
    });

    it('should move step up', () => {
      component.addStep();
      component.addStep();
      component.sequenceForm.steps[0].subject = 'First';
      component.sequenceForm.steps[1].subject = 'Second';

      component.moveStepUp(1);

      expect(component.sequenceForm.steps[0].subject).toBe('Second');
      expect(component.sequenceForm.steps[1].subject).toBe('First');
    });

    it('should move step down', () => {
      component.addStep();
      component.addStep();
      component.sequenceForm.steps[0].subject = 'First';
      component.sequenceForm.steps[1].subject = 'Second';

      component.moveStepDown(0);

      expect(component.sequenceForm.steps[0].subject).toBe('Second');
      expect(component.sequenceForm.steps[1].subject).toBe('First');
    });
  });

  describe('Enrollment', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should open enroll dialog', () => {
      component.openEnrollDialog(mockSequences[0]);

      expect(component.showEnrollDialog).toBeTrue();
      expect(component.enrollingSequence).toEqual(mockSequences[0]);
    });

    it('should enroll a lead', fakeAsync(() => {
      mockSequenceService.enrollLead.and.returnValue(of({
        id: 'enrollment-1',
        sequenceId: '1',
        leadId: '1',
        status: 'ACTIVE',
        currentStep: 1,
        enrolledAt: '2026-02-04T00:00:00Z',
      }));

      component.openEnrollDialog(mockSequences[0]);
      component.selectedLeadId = '1';
      component.enrollLead();
      tick();

      expect(mockSequenceService.enrollLead).toHaveBeenCalledWith('1', '1');
      expect(component.showEnrollDialog).toBeFalse();
    }));
  });

  describe('Pagination', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should change page', fakeAsync(() => {
      component.changePage(1);
      tick();

      expect(component.currentPage).toBe(1);
      expect(mockSequenceService.getSequences).toHaveBeenCalledWith({ page: 1, size: 20 });
    }));
  });

  describe('Utility Methods', () => {
    it('should format status correctly', () => {
      expect(component.formatStatus('ACTIVE')).toBe('Active');
      expect(component.formatStatus('INACTIVE')).toBe('Inactive');
      expect(component.formatStatus('DRAFT')).toBe('Draft');
    });

    it('should get step icon correctly', () => {
      expect(component.getStepIcon('EMAIL')).toBe('📧');
      expect(component.getStepIcon('WAIT')).toBe('⏰');
      expect(component.getStepIcon('TASK')).toBe('✅');
      expect(component.getStepIcon('LINKEDIN')).toBe('🔗');
    });

    it('should format delay correctly', () => {
      expect(component.formatDelay(1, 0)).toBe('After 1 day');
      expect(component.formatDelay(3, 0)).toBe('After 3 days');
      expect(component.formatDelay(0, 5)).toBe('After 5 hours');
      expect(component.formatDelay(2, 12)).toBe('After 2 days, 12 hours');
      expect(component.formatDelay(0, 0)).toBe('Immediately');
    });
  });

  describe('Cleanup', () => {
    it('should complete destroy$ on ngOnDestroy', () => {
      spyOn(component['destroy$'], 'next');
      spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });
  });
});
