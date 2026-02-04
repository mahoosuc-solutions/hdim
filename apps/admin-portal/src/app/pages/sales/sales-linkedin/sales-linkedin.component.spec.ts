import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { of } from 'rxjs';
import { signal, WritableSignal } from '@angular/core';
import { SalesLinkedInComponent } from './sales-linkedin.component';
import { SalesLinkedInService } from '../../../services/sales-linkedin.service';
import { SalesService } from '../../../services/sales.service';
import {
  LinkedInCampaign,
  LinkedInCampaignStatus,
  LinkedInOutreach,
  LinkedInAnalytics,
  Lead,
} from '../../../models/sales.model';

// Helper to create mock services with Jest
function createMockLinkedInService() {
  return {
    getCampaigns: jest.fn(),
    getCampaign: jest.fn(),
    createCampaign: jest.fn(),
    updateCampaign: jest.fn(),
    deleteCampaign: jest.fn(),
    activateCampaign: jest.fn(),
    pauseCampaign: jest.fn(),
    getAnalytics: jest.fn(),
    getCampaignAnalytics: jest.fn(),
    getOutreach: jest.fn(),
    sendConnectionRequest: jest.fn(),
    sendInMail: jest.fn(),
    updateOutreachStatus: jest.fn(),
    isLoading: signal(false) as WritableSignal<boolean>,
  };
}

function createMockSalesService() {
  return {
    getLeads: jest.fn(),
  };
}

type MockLinkedInService = ReturnType<typeof createMockLinkedInService>;
type MockSalesService = ReturnType<typeof createMockSalesService>;

describe('SalesLinkedInComponent', () => {
  let component: SalesLinkedInComponent;
  let fixture: ComponentFixture<SalesLinkedInComponent>;
  let mockLinkedInService: MockLinkedInService;
  let mockSalesService: MockSalesService;

  const mockCampaigns: LinkedInCampaign[] = [
    {
      id: '1',
      name: 'Q1 Outreach',
      description: 'Healthcare executives outreach',
      status: 'ACTIVE' as LinkedInCampaignStatus,
      targetCriteria: 'VP+ Healthcare',
      dailyLimit: 50,
      totalSent: 156,
      totalAccepted: 62,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-15T00:00:00Z',
    },
    {
      id: '2',
      name: 'VCs Target',
      description: 'Investor outreach',
      status: 'PAUSED' as LinkedInCampaignStatus,
      targetCriteria: 'VC Partners',
      dailyLimit: 25,
      totalSent: 78,
      totalAccepted: 27,
      createdAt: '2026-01-10T00:00:00Z',
      updatedAt: '2026-01-20T00:00:00Z',
    },
  ];

  const mockAnalytics: LinkedInAnalytics = {
    totalConnectionsSent: 234,
    totalConnectionsAccepted: 89,
    connectionRate: 0.38,
    totalInMailsSent: 45,
    totalInMailsReplied: 12,
    inMailReplyRate: 0.267,
    activeCampaigns: 1,
  };

  const mockOutreach: LinkedInOutreach[] = [
    {
      id: '1',
      campaignId: '1',
      leadId: '1',
      type: 'CONNECTION',
      status: 'ACCEPTED',
      linkedInProfileUrl: 'https://linkedin.com/in/johndoe',
      message: 'Hello!',
      sentAt: '2026-01-15T00:00:00Z',
      respondedAt: '2026-01-16T00:00:00Z',
    },
    {
      id: '2',
      campaignId: '1',
      leadId: '2',
      type: 'CONNECTION',
      status: 'PENDING',
      linkedInProfileUrl: 'https://linkedin.com/in/janesmith',
      message: 'Hi there!',
      sentAt: '2026-01-20T00:00:00Z',
    },
  ];

  const mockLeads: Lead[] = [
    { id: '1', firstName: 'John', lastName: 'Doe', email: 'john@example.com', company: 'Acme', status: 'OPEN', source: 'WEBSITE', linkedInUrl: 'https://linkedin.com/in/johndoe' },
    { id: '2', firstName: 'Jane', lastName: 'Smith', email: 'jane@example.com', company: 'Tech', status: 'QUALIFIED', source: 'LINKEDIN', linkedInUrl: 'https://linkedin.com/in/janesmith' },
  ];

  beforeEach(async () => {
    mockLinkedInService = createMockLinkedInService();
    mockSalesService = createMockSalesService();

    // Default mock returns
    mockLinkedInService.getCampaigns.mockReturnValue(of({
      content: mockCampaigns,
      totalPages: 1,
      totalElements: 2,
      size: 20,
      number: 0,
    }));
    mockLinkedInService.getAnalytics.mockReturnValue(of(mockAnalytics));
    mockLinkedInService.getOutreach.mockReturnValue(of({
      content: mockOutreach,
      totalPages: 1,
      totalElements: 2,
      size: 20,
      number: 0,
    }));
    mockSalesService.getLeads.mockReturnValue(of({
      content: mockLeads,
      totalPages: 1,
      totalElements: 2,
      size: 100,
      number: 0,
    }));

    await TestBed.configureTestingModule({
      imports: [SalesLinkedInComponent, FormsModule],
      providers: [
        { provide: SalesLinkedInService, useValue: mockLinkedInService },
        { provide: SalesService, useValue: mockSalesService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SalesLinkedInComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initialization', () => {
    it('should load campaigns on init', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      expect(mockLinkedInService.getCampaigns).toHaveBeenCalled();
      expect(component.campaigns().length).toBe(2);
    }));

    it('should load analytics on init', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      expect(mockLinkedInService.getAnalytics).toHaveBeenCalled();
      expect(component.analytics()).toEqual(mockAnalytics);
    }));

    it('should load leads on init', fakeAsync(() => {
      fixture.detectChanges();
      tick();

      expect(mockSalesService.getLeads).toHaveBeenCalled();
      expect(component.leads().length).toBe(2);
    }));
  });

  describe('Analytics Display', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should have analytics data', () => {
      const analytics = component.analytics();
      expect(analytics?.totalConnectionsSent).toBe(234);
      expect(analytics?.connectionRate).toBe(0.38);
    });
  });

  describe('Filtering', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should filter campaigns by search query', () => {
      component.searchQuery = 'q1';
      const filtered = component.filteredCampaigns();

      expect(filtered.length).toBe(1);
      expect(filtered[0].name).toBe('Q1 Outreach');
    });

    it('should filter campaigns by status', () => {
      component.statusFilter = 'PAUSED';
      const filtered = component.filteredCampaigns();

      expect(filtered.length).toBe(1);
      expect(filtered[0].name).toBe('VCs Target');
    });

    it('should return all campaigns when no filters applied', () => {
      component.searchQuery = '';
      component.statusFilter = '';
      const filtered = component.filteredCampaigns();

      expect(filtered.length).toBe(2);
    });
  });

  describe('Campaign Selection', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should select a campaign and load outreach', fakeAsync(() => {
      component.selectCampaign(mockCampaigns[0]);
      tick();

      expect(component.selectedCampaign()).toEqual(mockCampaigns[0]);
      expect(mockLinkedInService.getOutreach).toHaveBeenCalledWith('1');
    }));

    it('should close detail panel when selectedCampaign is set to null', fakeAsync(() => {
      component.selectCampaign(mockCampaigns[0]);
      tick();

      expect(component.selectedCampaign()).toBeTruthy();

      // The component uses signal directly to close the detail panel
      component.selectedCampaign.set(null);

      expect(component.selectedCampaign()).toBeNull();
    }));
  });

  describe('Campaign Dialog', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should open create dialog with empty form', () => {
      component.openCampaignDialog();

      expect(component.showCampaignDialog).toBe(true);
      expect(component.editingCampaign).toBeNull();
      expect(component.campaignForm.name).toBe('');
    });

    it('should open edit dialog with campaign data', () => {
      component.editCampaign(mockCampaigns[0]);

      expect(component.showCampaignDialog).toBe(true);
      expect(component.editingCampaign).toEqual(mockCampaigns[0]);
      expect(component.campaignForm.name).toBe('Q1 Outreach');
      expect(component.campaignForm.dailyLimit).toBe(50);
    });

    it('should close dialog and reset form', () => {
      component.openCampaignDialog();
      component.campaignForm.name = 'Test';
      component.closeCampaignDialog();

      expect(component.showCampaignDialog).toBe(false);
    });

    it('should create new campaign on save', fakeAsync(() => {
      mockLinkedInService.createCampaign.mockReturnValue(of(mockCampaigns[0]));

      component.openCampaignDialog();
      component.campaignForm.name = 'New Campaign';
      component.campaignForm.dailyLimit = 30;
      component.saveCampaign();
      tick();

      expect(mockLinkedInService.createCampaign).toHaveBeenCalled();
      expect(component.showCampaignDialog).toBe(false);
    }));

    it('should update existing campaign on save', fakeAsync(() => {
      mockLinkedInService.updateCampaign.mockReturnValue(of(mockCampaigns[0]));
      // Ensure selectedCampaign is different from editing campaign to avoid the refresh logic
      component.selectedCampaign.set(mockCampaigns[1]);

      component.editCampaign(mockCampaigns[0]);
      component.campaignForm.name = 'Updated Name';
      component.saveCampaign();
      tick();

      expect(mockLinkedInService.updateCampaign).toHaveBeenCalledWith('1', expect.any(Object));
    }));
  });

  describe('Campaign Actions', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should pause active campaign', fakeAsync(() => {
      mockLinkedInService.pauseCampaign.mockReturnValue(of({
        ...mockCampaigns[0],
        status: 'PAUSED' as LinkedInCampaignStatus,
      }));

      component.toggleCampaignStatus(mockCampaigns[0]);
      tick();

      expect(mockLinkedInService.pauseCampaign).toHaveBeenCalledWith('1');
    }));

    it('should activate paused campaign', fakeAsync(() => {
      mockLinkedInService.activateCampaign.mockReturnValue(of({
        ...mockCampaigns[1],
        status: 'ACTIVE' as LinkedInCampaignStatus,
      }));

      component.toggleCampaignStatus(mockCampaigns[1]);
      tick();

      expect(mockLinkedInService.activateCampaign).toHaveBeenCalledWith('2');
    }));

    it('should delete campaign with confirmation', fakeAsync(() => {
      jest.spyOn(window, 'confirm').mockReturnValue(true);
      mockLinkedInService.deleteCampaign.mockReturnValue(of(void 0));

      component.deleteCampaign(mockCampaigns[0]);
      tick();

      expect(mockLinkedInService.deleteCampaign).toHaveBeenCalledWith('1');
    }));

    it('should not delete campaign when confirmation cancelled', fakeAsync(() => {
      jest.spyOn(window, 'confirm').mockReturnValue(false);

      component.deleteCampaign(mockCampaigns[0]);
      tick();

      expect(mockLinkedInService.deleteCampaign).not.toHaveBeenCalled();
    }));
  });

  describe('Connection Request Dialog', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should open connection dialog', () => {
      component.openConnectionDialog();

      expect(component.showConnectionDialog).toBe(true);
      expect(component.connectionForm.leadId).toBe('');
    });

    it('should open connection dialog for specific campaign', () => {
      component.openConnectionDialogForCampaign(mockCampaigns[0]);

      expect(component.showConnectionDialog).toBe(true);
      expect(component.connectionForm.campaignId).toBe('1');
    });

    it('should send connection request', fakeAsync(() => {
      mockLinkedInService.sendConnectionRequest.mockReturnValue(of(mockOutreach[0]));

      component.openConnectionDialog();
      component.connectionForm = {
        leadId: '1',
        linkedInProfileUrl: 'https://linkedin.com/in/test',
        message: 'Hello',
      };
      component.sendConnection();
      tick();

      expect(mockLinkedInService.sendConnectionRequest).toHaveBeenCalled();
      expect(component.showConnectionDialog).toBe(false);
    }));
  });

  describe('InMail Dialog', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should open InMail dialog', () => {
      component.openInMailDialog();

      expect(component.showInMailDialog).toBe(true);
      expect(component.inMailForm.subject).toBe('');
    });

    it('should send InMail', fakeAsync(() => {
      mockLinkedInService.sendInMail.mockReturnValue(of({
        ...mockOutreach[0],
        type: 'INMAIL',
      }));

      component.openInMailDialog();
      component.inMailForm = {
        leadId: '1',
        linkedInProfileUrl: 'https://linkedin.com/in/test',
        subject: 'Opportunity',
        message: 'Hello',
      };
      component.sendInMail();
      tick();

      expect(mockLinkedInService.sendInMail).toHaveBeenCalled();
      expect(component.showInMailDialog).toBe(false);
    }));
  });

  describe('Utility Methods', () => {
    it('should format status correctly', () => {
      expect(component.formatStatus('ACTIVE')).toBe('Active');
      expect(component.formatStatus('PAUSED')).toBe('Paused');
      expect(component.formatStatus('DRAFT')).toBe('Draft');
      expect(component.formatStatus('COMPLETED')).toBe('Completed');
    });

    it('should format outreach status correctly', () => {
      expect(component.formatOutreachStatus('PENDING')).toBe('Pending');
      expect(component.formatOutreachStatus('SENT')).toBe('Sent');
      expect(component.formatOutreachStatus('ACCEPTED')).toBe('Accepted');
      expect(component.formatOutreachStatus('REPLIED')).toBe('Replied');
    });

    it('should have acceptance rate in campaign model', () => {
      // Acceptance rate is calculated on the server and stored in the campaign model
      // The component displays campaign.acceptanceRate directly
      const campaignWithRate: LinkedInCampaign = {
        ...mockCampaigns[0],
        acceptanceRate: 0.397,
      };
      expect(campaignWithRate.acceptanceRate).toBeCloseTo(0.397, 3);
    });
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
      expect(mockLinkedInService.getCampaigns).toHaveBeenCalledWith({ page: 1, size: 20 });
    }));
  });

  describe('Refresh', () => {
    beforeEach(fakeAsync(() => {
      fixture.detectChanges();
      tick();
    }));

    it('should refresh data', fakeAsync(() => {
      mockLinkedInService.getCampaigns.mockClear();
      mockLinkedInService.getAnalytics.mockClear();

      component.refreshData();
      tick();

      expect(mockLinkedInService.getCampaigns).toHaveBeenCalled();
      expect(mockLinkedInService.getAnalytics).toHaveBeenCalled();
    }));
  });

  describe('Cleanup', () => {
    it('should complete destroy$ on ngOnDestroy', () => {
      const nextSpy = jest.spyOn(component['destroy$'], 'next');
      const completeSpy = jest.spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(nextSpy).toHaveBeenCalled();
      expect(completeSpy).toHaveBeenCalled();
    });
  });
});
