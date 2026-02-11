import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { SalesLinkedInService } from './sales-linkedin.service';
import {
  LinkedInCampaign,
  LinkedInOutreach,
  LinkedInAnalytics,
  PageResponse,
} from '../models/sales.model';
import { environment } from '../../environments/environment';

describe('SalesLinkedInService', () => {
  let service: SalesLinkedInService;
  let httpMock: HttpTestingController;
  const apiBaseUrl = environment.apiConfig.salesApiUrl;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SalesLinkedInService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(SalesLinkedInService);
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

    it('should have empty campaigns signal initially', () => {
      expect(service.campaigns()).toEqual([]);
    });

    it('should have empty outreachList signal initially', () => {
      expect(service.outreachList()).toEqual([]);
    });

    it('should have null analytics signal initially', () => {
      expect(service.analytics()).toBeNull();
    });

    it('should have isLoading as false initially', () => {
      expect(service.isLoading()).toBe(false);
    });

    it('should have error as null initially', () => {
      expect(service.error()).toBeNull();
    });
  });

  // ==========================================
  // Campaign Management Tests
  // ==========================================

  describe('getCampaigns', () => {
    const mockCampaignsResponse: PageResponse<LinkedInCampaign> = {
      content: [
        {
          id: 'campaign-1',
          name: 'Test Campaign 1',
          description: 'Test description',
          status: 'DRAFT',
          targetCriteria: 'Healthcare executives',
          dailyLimit: 25,
          totalSent: 100,
          totalAccepted: 45,
          totalReplied: 20,
          acceptanceRate: 45.0,
          createdAt: '2026-01-01T00:00:00Z',
        },
        {
          id: 'campaign-2',
          name: 'Test Campaign 2',
          description: 'Another description',
          status: 'ACTIVE',
          targetCriteria: 'Tech executives',
          dailyLimit: 50,
          totalSent: 200,
          totalAccepted: 100,
          totalReplied: 50,
          acceptanceRate: 50.0,
          createdAt: '2026-01-02T00:00:00Z',
        },
      ],
      totalElements: 2,
      totalPages: 1,
      size: 20,
      number: 0,
    };

    it('should fetch campaigns and update signal', fakeAsync(() => {
      let result: PageResponse<LinkedInCampaign> | undefined;

      service.getCampaigns().subscribe((res) => (result = res));

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/linkedin/campaigns`);
      expect(req.request.method).toBe('GET');
      req.flush(mockCampaignsResponse);

      tick();

      expect(result).toEqual(mockCampaignsResponse);
      expect(service.campaigns()).toEqual(mockCampaignsResponse.content);
      expect(service.isLoading()).toBe(false);
    }));

    it('should set isLoading to true while fetching', () => {
      service.getCampaigns().subscribe();
      expect(service.isLoading()).toBe(true);

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/linkedin/campaigns`);
      req.flush(mockCampaignsResponse);
    });

    it('should include pagination parameters when provided', fakeAsync(() => {
      service.getCampaigns({ page: 1, size: 10 }).subscribe();

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/campaigns?page=1&size=10`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockCampaignsResponse);
    }));

    it('should include status filter when provided', fakeAsync(() => {
      service.getCampaigns(undefined, 'ACTIVE').subscribe();

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/campaigns?status=ACTIVE`
      );
      req.flush(mockCampaignsResponse);
    }));

    it('should include search parameter when provided', fakeAsync(() => {
      service.getCampaigns(undefined, undefined, 'Test').subscribe();

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/campaigns?search=Test`
      );
      req.flush(mockCampaignsResponse);
    }));

    it('should combine all query parameters', fakeAsync(() => {
      service.getCampaigns({ page: 0, size: 20 }, 'ACTIVE', 'Healthcare').subscribe();

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/campaigns?page=0&size=20&status=ACTIVE&search=Healthcare`
      );
      req.flush(mockCampaignsResponse);
    }));

    it('should handle error and set error signal', fakeAsync(() => {
      let error: Error | undefined;

      service.getCampaigns().subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/linkedin/campaigns`);
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to load campaigns');
      expect(service.isLoading()).toBe(false);
    }));
  });

  describe('getCampaign', () => {
    const mockCampaign: LinkedInCampaign = {
      id: 'campaign-1',
      name: 'Test Campaign',
      description: 'Test description',
      status: 'DRAFT',
      targetCriteria: 'Healthcare executives',
      dailyLimit: 25,
      totalSent: 100,
      totalAccepted: 45,
      totalReplied: 20,
      acceptanceRate: 45.0,
      createdAt: '2026-01-01T00:00:00Z',
    };

    it('should fetch single campaign by id', fakeAsync(() => {
      let result: LinkedInCampaign | undefined;

      service.getCampaign('campaign-1').subscribe((res) => (result = res));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/campaigns/campaign-1`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockCampaign);

      tick();

      expect(result).toEqual(mockCampaign);
    }));

    it('should handle 404 error', fakeAsync(() => {
      let error: Error | undefined;

      service.getCampaign('non-existent').subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/campaigns/non-existent`
      );
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to load campaign');
    }));
  });

  describe('createCampaign', () => {
    const newCampaign: Partial<LinkedInCampaign> = {
      name: 'New Campaign',
      description: 'New description',
      targetCriteria: 'Healthcare executives',
      dailyLimit: 30,
    };

    const createdCampaign: LinkedInCampaign = {
      id: 'new-campaign-id',
      name: 'New Campaign',
      description: 'New description',
      status: 'DRAFT',
      targetCriteria: 'Healthcare executives',
      dailyLimit: 30,
      totalSent: 0,
      totalAccepted: 0,
      totalReplied: 0,
      acceptanceRate: 0,
      createdAt: '2026-01-01T00:00:00Z',
    };

    it('should create campaign and update campaigns signal', fakeAsync(() => {
      let result: LinkedInCampaign | undefined;

      service.createCampaign(newCampaign).subscribe((res) => (result = res));

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/linkedin/campaigns`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(newCampaign);
      req.flush(createdCampaign);

      tick();

      expect(result).toEqual(createdCampaign);
      expect(service.campaigns()).toContainEqual(createdCampaign);
    }));

    it('should handle conflict error for duplicate name', fakeAsync(() => {
      let error: Error | undefined;

      service.createCampaign(newCampaign).subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/linkedin/campaigns`);
      req.flush('Campaign already exists', { status: 409, statusText: 'Conflict' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to create campaign');
    }));
  });

  describe('updateCampaign', () => {
    const updateData: Partial<LinkedInCampaign> = {
      name: 'Updated Campaign',
      description: 'Updated description',
    };

    const updatedCampaign: LinkedInCampaign = {
      id: 'campaign-1',
      name: 'Updated Campaign',
      description: 'Updated description',
      status: 'DRAFT',
      targetCriteria: 'Healthcare executives',
      dailyLimit: 25,
      totalSent: 100,
      totalAccepted: 45,
      totalReplied: 20,
      acceptanceRate: 45.0,
      createdAt: '2026-01-01T00:00:00Z',
    };

    it('should update campaign and update signal', fakeAsync(() => {
      // First, populate the campaigns signal
      const initialCampaign: LinkedInCampaign = {
        ...updatedCampaign,
        name: 'Original Campaign',
      };
      service['_campaigns'].set([initialCampaign]);

      let result: LinkedInCampaign | undefined;

      service.updateCampaign('campaign-1', updateData).subscribe((res) => (result = res));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/campaigns/campaign-1`
      );
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updateData);
      req.flush(updatedCampaign);

      tick();

      expect(result).toEqual(updatedCampaign);
      expect(service.campaigns()[0].name).toBe('Updated Campaign');
    }));

    it('should handle 404 error', fakeAsync(() => {
      let error: Error | undefined;

      service.updateCampaign('non-existent', updateData).subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/campaigns/non-existent`
      );
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to update campaign');
    }));
  });

  describe('deleteCampaign', () => {
    it('should delete campaign and remove from signal', fakeAsync(() => {
      // Populate the campaigns signal
      const campaigns: LinkedInCampaign[] = [
        {
          id: 'campaign-1',
          name: 'To Delete',
          status: 'DRAFT',
          dailyLimit: 25,
          totalSent: 0,
          totalAccepted: 0,
          totalReplied: 0,
          acceptanceRate: 0,
          createdAt: '2026-01-01T00:00:00Z',
        },
        {
          id: 'campaign-2',
          name: 'Keep',
          status: 'ACTIVE',
          dailyLimit: 30,
          totalSent: 10,
          totalAccepted: 5,
          totalReplied: 2,
          acceptanceRate: 50.0,
          createdAt: '2026-01-02T00:00:00Z',
        },
      ];
      service['_campaigns'].set(campaigns);

      let completed = false;
      service.deleteCampaign('campaign-1').subscribe(() => (completed = true));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/campaigns/campaign-1`
      );
      expect(req.request.method).toBe('DELETE');
      req.flush(null);

      tick();

      expect(completed).toBe(true);
      expect(service.campaigns().length).toBe(1);
      expect(service.campaigns()[0].id).toBe('campaign-2');
    }));

    it('should handle 404 error', fakeAsync(() => {
      let error: Error | undefined;

      service.deleteCampaign('non-existent').subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/campaigns/non-existent`
      );
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to delete campaign');
    }));
  });

  describe('activateCampaign', () => {
    const activatedCampaign: LinkedInCampaign = {
      id: 'campaign-1',
      name: 'Test Campaign',
      status: 'ACTIVE',
      dailyLimit: 25,
      totalSent: 0,
      totalAccepted: 0,
      totalReplied: 0,
      acceptanceRate: 0,
      createdAt: '2026-01-01T00:00:00Z',
    };

    it('should activate campaign and update status in signal', fakeAsync(() => {
      // Populate with a DRAFT campaign
      const draftCampaign: LinkedInCampaign = { ...activatedCampaign, status: 'DRAFT' };
      service['_campaigns'].set([draftCampaign]);

      let result: LinkedInCampaign | undefined;

      service.activateCampaign('campaign-1').subscribe((res) => (result = res));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/campaigns/campaign-1/activate`
      );
      expect(req.request.method).toBe('POST');
      req.flush(activatedCampaign);

      tick();

      expect(result).toEqual(activatedCampaign);
      expect(service.campaigns()[0].status).toBe('ACTIVE');
    }));

    it('should handle error', fakeAsync(() => {
      let error: Error | undefined;

      service.activateCampaign('non-existent').subscribe({
        error: (err) => (error = err),
      });

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/campaigns/non-existent/activate`
      );
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to activate campaign');
    }));
  });

  describe('pauseCampaign', () => {
    const pausedCampaign: LinkedInCampaign = {
      id: 'campaign-1',
      name: 'Test Campaign',
      status: 'PAUSED',
      dailyLimit: 25,
      totalSent: 50,
      totalAccepted: 25,
      totalReplied: 10,
      acceptanceRate: 50.0,
      createdAt: '2026-01-01T00:00:00Z',
    };

    it('should pause campaign and update status in signal', fakeAsync(() => {
      // Populate with an ACTIVE campaign
      const activeCampaign: LinkedInCampaign = { ...pausedCampaign, status: 'ACTIVE' };
      service['_campaigns'].set([activeCampaign]);

      let result: LinkedInCampaign | undefined;

      service.pauseCampaign('campaign-1').subscribe((res) => (result = res));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/campaigns/campaign-1/pause`
      );
      expect(req.request.method).toBe('POST');
      req.flush(pausedCampaign);

      tick();

      expect(result).toEqual(pausedCampaign);
      expect(service.campaigns()[0].status).toBe('PAUSED');
    }));
  });

  // ==========================================
  // Connection Request Tests
  // ==========================================

  describe('sendConnectionRequest', () => {
    const mockOutreach: LinkedInOutreach = {
      id: 'outreach-1',
      linkedInProfileUrl: 'https://linkedin.com/in/test',
      type: 'CONNECTION',
      status: 'PENDING',
      message: 'Hello!',
      createdAt: '2026-01-01T00:00:00Z',
    };

    it('should send connection request for lead', fakeAsync(() => {
      let result: LinkedInOutreach | undefined;

      service
        .sendConnectionRequest({
          leadId: 'lead-1',
          linkedInProfileUrl: 'https://linkedin.com/in/test',
          message: 'Hello!',
        })
        .subscribe((res) => (result = res));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/connect/lead/lead-1`
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockOutreach);

      tick();

      expect(result).toEqual(mockOutreach);
      expect(service.outreachList()).toContainEqual(mockOutreach);
    }));

    it('should send connection request for contact', fakeAsync(() => {
      service
        .sendConnectionRequest({
          contactId: 'contact-1',
          linkedInProfileUrl: 'https://linkedin.com/in/test',
          message: 'Hello!',
        })
        .subscribe();

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/connect/contact/contact-1`
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockOutreach);
    }));

    it('should handle error', fakeAsync(() => {
      let error: Error | undefined;

      service
        .sendConnectionRequest({
          leadId: 'lead-1',
          linkedInProfileUrl: 'https://linkedin.com/in/test',
          message: 'Hello!',
        })
        .subscribe({ error: (err) => (error = err) });

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/connect/lead/lead-1`
      );
      req.flush('Bad Request', { status: 400, statusText: 'Bad Request' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to send connection request');
    }));
  });

  // ==========================================
  // InMail Tests
  // ==========================================

  describe('sendInMail', () => {
    const mockOutreach: LinkedInOutreach = {
      id: 'outreach-1',
      linkedInProfileUrl: 'https://linkedin.com/in/test',
      type: 'INMAIL',
      status: 'PENDING',
      message: 'Hello!',
      subject: 'Subject',
      createdAt: '2026-01-01T00:00:00Z',
    };

    it('should send InMail for lead', fakeAsync(() => {
      let result: LinkedInOutreach | undefined;

      service
        .sendInMail({
          leadId: 'lead-1',
          linkedInProfileUrl: 'https://linkedin.com/in/test',
          subject: 'Subject',
          message: 'Hello!',
        })
        .subscribe((res) => (result = res));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/inmail/lead/lead-1`
      );
      expect(req.request.method).toBe('POST');
      req.flush(mockOutreach);

      tick();

      expect(result).toEqual(mockOutreach);
      expect(service.outreachList()).toContainEqual(mockOutreach);
    }));

    it('should send InMail for contact', fakeAsync(() => {
      service
        .sendInMail({
          contactId: 'contact-1',
          linkedInProfileUrl: 'https://linkedin.com/in/test',
          subject: 'Subject',
          message: 'Hello!',
        })
        .subscribe();

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/inmail/contact/contact-1`
      );
      req.flush(mockOutreach);
    }));
  });

  // ==========================================
  // Analytics Tests
  // ==========================================

  describe('getAnalytics', () => {
    const mockAnalytics: LinkedInAnalytics = {
      totalSent: 500,
      totalAccepted: 250,
      totalReplied: 100,
      acceptanceRate: 50.0,
      replyRate: 40.0,
      campaignBreakdown: [
        { campaignId: 'campaign-1', campaignName: 'Test', sent: 100, accepted: 50, replied: 20 },
      ],
    };

    it('should fetch analytics and update signal', fakeAsync(() => {
      let result: LinkedInAnalytics | undefined;

      service.getAnalytics().subscribe((res) => (result = res));

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/linkedin/analytics`);
      expect(req.request.method).toBe('GET');
      req.flush(mockAnalytics);

      tick();

      expect(result).toEqual(mockAnalytics);
      expect(service.analytics()).toEqual(mockAnalytics);
    }));

    it('should handle error', fakeAsync(() => {
      let error: Error | undefined;

      service.getAnalytics().subscribe({ error: (err) => (error = err) });

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/linkedin/analytics`);
      req.flush('Server Error', { status: 500, statusText: 'Internal Server Error' });

      tick();

      expect(error).toBeTruthy();
      expect(service.error()).toBe('Failed to load analytics');
    }));
  });

  describe('getCampaignAnalytics', () => {
    const mockAnalytics: LinkedInAnalytics = {
      totalSent: 100,
      totalAccepted: 50,
      totalReplied: 20,
      acceptanceRate: 50.0,
      replyRate: 40.0,
      campaignBreakdown: [],
    };

    it('should fetch campaign-specific analytics', fakeAsync(() => {
      let result: LinkedInAnalytics | undefined;

      service.getCampaignAnalytics('campaign-1').subscribe((res) => (result = res));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/campaign/campaign-1/analytics`
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockAnalytics);

      tick();

      expect(result).toEqual(mockAnalytics);
    }));
  });

  // ==========================================
  // Outreach Management Tests
  // ==========================================

  describe('getOutreach', () => {
    const mockOutreachResponse: PageResponse<LinkedInOutreach> = {
      content: [
        {
          id: 'outreach-1',
          type: 'CONNECTION',
          status: 'SENT',
          createdAt: '2026-01-01T00:00:00Z',
        },
      ],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
    };

    it('should fetch outreach list', fakeAsync(() => {
      let result: PageResponse<LinkedInOutreach> | undefined;

      service.getOutreach().subscribe((res) => (result = res));

      const req = httpMock.expectOne(`${apiBaseUrl}/api/sales/linkedin/outreach`);
      expect(req.request.method).toBe('GET');
      req.flush(mockOutreachResponse);

      tick();

      expect(result).toEqual(mockOutreachResponse);
      expect(service.outreachList()).toEqual(mockOutreachResponse.content);
    }));

    it('should filter by campaignId', fakeAsync(() => {
      service.getOutreach('campaign-1').subscribe();

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/outreach?campaignId=campaign-1`
      );
      req.flush(mockOutreachResponse);
    }));
  });

  describe('updateOutreachStatus', () => {
    const updatedOutreach: LinkedInOutreach = {
      id: 'outreach-1',
      type: 'CONNECTION',
      status: 'ACCEPTED',
      createdAt: '2026-01-01T00:00:00Z',
    };

    it('should update outreach status', fakeAsync(() => {
      // Populate with initial outreach
      const initialOutreach: LinkedInOutreach = { ...updatedOutreach, status: 'SENT' };
      service['_outreachList'].set([initialOutreach]);

      let result: LinkedInOutreach | undefined;

      service.updateOutreachStatus('outreach-1', 'ACCEPTED').subscribe((res) => (result = res));

      const req = httpMock.expectOne(
        `${apiBaseUrl}/api/sales/linkedin/outreach/outreach-1/status`
      );
      expect(req.request.method).toBe('PATCH');
      expect(req.request.body).toEqual({ status: 'ACCEPTED' });
      req.flush(updatedOutreach);

      tick();

      expect(result).toEqual(updatedOutreach);
      expect(service.outreachList()[0].status).toBe('ACCEPTED');
    }));
  });
});
