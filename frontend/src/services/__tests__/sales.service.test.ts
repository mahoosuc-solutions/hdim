/**
 * Tests for sales service
 */

import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { SalesService } from '../sales.service';
import type { Lead, LeadStatus, LeadSource } from '../../types/sales';

describe('sales.service', () => {
  let salesService: SalesService;
  const mockApiOptions = { tenantId: 'TENANT001', userId: 'USER001' };

  beforeEach(() => {
    salesService = new SalesService('http://localhost:8106');
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe('getLeads', () => {
    it('fetches leads with correct headers', async () => {
      const mockLeads = {
        content: [
          {
            id: 'lead-1',
            tenantId: 'TENANT001',
            firstName: 'John',
            lastName: 'Doe',
            email: 'john@example.com',
            source: 'WEBSITE' as LeadSource,
            status: 'NEW' as LeadStatus,
            score: 50,
            createdAt: '2024-01-15T10:00:00Z',
            updatedAt: '2024-01-15T10:00:00Z',
          },
        ],
        totalElements: 1,
        totalPages: 1,
        size: 20,
        number: 0,
        first: true,
        last: true,
        empty: false,
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockLeads),
      });

      const result = await salesService.getLeads(mockApiOptions);

      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8106/api/sales/leads?page=0&size=20',
        expect.objectContaining({
          method: 'GET',
          headers: {
            'Content-Type': 'application/json',
            'X-Tenant-ID': 'TENANT001',
            'X-User-ID': 'USER001',
          },
        })
      );

      expect(result.content).toHaveLength(1);
      expect(result.content[0].firstName).toBe('John');
    });

    it('applies filters to query params', async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve({ content: [], totalElements: 0 }),
      });

      await salesService.getLeads(
        mockApiOptions,
        { status: 'QUALIFIED', source: 'WEBSITE', search: 'test' },
        1,
        10
      );

      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('page=1'),
        expect.anything()
      );
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('size=10'),
        expect.anything()
      );
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('status=QUALIFIED'),
        expect.anything()
      );
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('source=WEBSITE'),
        expect.anything()
      );
      expect(fetch).toHaveBeenCalledWith(
        expect.stringContaining('search=test'),
        expect.anything()
      );
    });

    it('throws error on failed request', async () => {
      global.fetch = vi.fn().mockResolvedValue({
        ok: false,
        statusText: 'Internal Server Error',
      });

      await expect(salesService.getLeads(mockApiOptions)).rejects.toThrow(
        'Failed to fetch leads: Internal Server Error'
      );
    });
  });

  describe('getLead', () => {
    it('fetches single lead by ID', async () => {
      const mockLead: Lead = {
        id: 'lead-1',
        tenantId: 'TENANT001',
        firstName: 'John',
        lastName: 'Doe',
        email: 'john@example.com',
        source: 'WEBSITE',
        status: 'NEW',
        score: 50,
        createdAt: '2024-01-15T10:00:00Z',
        updatedAt: '2024-01-15T10:00:00Z',
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockLead),
      });

      const result = await salesService.getLead(mockApiOptions, 'lead-1');

      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8106/api/sales/leads/lead-1',
        expect.anything()
      );
      expect(result.id).toBe('lead-1');
    });
  });

  describe('createLead', () => {
    it('creates lead with POST request', async () => {
      const newLead = {
        firstName: 'Jane',
        lastName: 'Smith',
        email: 'jane@example.com',
        source: 'ROI_CALCULATOR' as LeadSource,
      };

      const mockResponse = {
        id: 'lead-2',
        ...newLead,
        status: 'NEW' as LeadStatus,
        score: 0,
        tenantId: 'TENANT001',
        createdAt: '2024-01-15T11:00:00Z',
        updatedAt: '2024-01-15T11:00:00Z',
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockResponse),
      });

      const result = await salesService.createLead(mockApiOptions, newLead);

      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8106/api/sales/leads',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify(newLead),
        })
      );
      expect(result.id).toBe('lead-2');
      expect(result.firstName).toBe('Jane');
    });
  });

  describe('convertLead', () => {
    it('converts lead to opportunity', async () => {
      const mockOpportunity = {
        id: 'opp-1',
        name: 'New Opportunity',
        amount: 50000,
        stage: 'DISCOVERY',
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockOpportunity),
      });

      const result = await salesService.convertLead(mockApiOptions, 'lead-1');

      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8106/api/sales/leads/lead-1/convert',
        expect.objectContaining({
          method: 'POST',
        })
      );
      expect(result.id).toBe('opp-1');
    });
  });

  describe('getPipelineKanban', () => {
    it('fetches pipeline kanban data', async () => {
      const mockPipeline = {
        columns: [
          {
            stage: 'DISCOVERY',
            stageLabel: 'Discovery',
            cards: [],
            totalValue: 0,
            count: 0,
          },
        ],
        summary: {
          totalValue: 100000,
          weightedValue: 50000,
          opportunityCount: 5,
          avgDealSize: 20000,
          avgProbability: 50,
        },
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockPipeline),
      });

      const result = await salesService.getPipelineKanban(mockApiOptions);

      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8106/api/sales/pipeline/kanban',
        expect.anything()
      );
      expect(result.summary.totalValue).toBe(100000);
    });
  });

  describe('getDashboard', () => {
    it('fetches dashboard metrics', async () => {
      // Mock the NESTED API response structure (not the transformed flat DTO)
      const mockApiResponse = {
        leads: {
          totalLeads: 100,
          newLeadsThisMonth: 25,
          qualifiedLeads: 40,
          conversionRate: 15.5,
          leadsBySource: { WEBSITE: 40, REFERRAL: 30 },
        },
        pipeline: {
          totalOpenOpportunities: 50,
          totalPipelineValue: 500000,
          weightedPipelineValue: 250000,
          averageDealSize: 10000,
          wonThisMonth: 5,
          wonValueThisMonth: 50000,
          lostThisMonth: 2,
          winRate: 71.4,
          averageSalesCycleDays: 45,
          opportunitiesByStage: { DISCOVERY: 20, DEMO: 15 },
        },
        activities: {
          callsThisWeek: 40,
          emailsThisWeek: 50,
          meetingsThisWeek: 30,
          overdueActivities: 8,
        },
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockApiResponse),
      });

      const result = await salesService.getDashboard(mockApiOptions);

      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8106/api/sales/dashboard',
        expect.anything()
      );
      // Verify the service correctly transforms nested API response to flat DTO
      expect(result.totalLeads).toBe(100);
      expect(result.winRate).toBe(71.4);
      expect(result.activitiesThisWeek).toBe(120); // 40 + 50 + 30
      expect(result.leadsBySource).toEqual({ WEBSITE: 40, REFERRAL: 30 });
    });
  });

  describe('email sequences', () => {
    it('fetches sequences', async () => {
      const mockSequences = {
        content: [
          {
            id: 'seq-1',
            name: 'Welcome Sequence',
            type: 'WELCOME',
            active: true,
            steps: [],
          },
        ],
        totalElements: 1,
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockSequences),
      });

      const result = await salesService.getSequences(mockApiOptions);

      expect(result.content).toHaveLength(1);
      expect(result.content[0].name).toBe('Welcome Sequence');
    });

    it('enrolls lead in sequence', async () => {
      const mockEnrollment = {
        id: 'enroll-1',
        sequenceId: 'seq-1',
        leadId: 'lead-1',
        status: 'ACTIVE',
        currentStep: 0,
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockEnrollment),
      });

      const result = await salesService.enrollLeadInSequence(
        mockApiOptions,
        'seq-1',
        'lead-1'
      );

      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8106/api/sales/sequences/seq-1/enroll/lead/lead-1',
        expect.objectContaining({
          method: 'POST',
        })
      );
      expect(result.id).toBe('enroll-1');
    });
  });

  describe('captureLead', () => {
    it('captures lead without auth headers (public endpoint)', async () => {
      const captureRequest = {
        firstName: 'Test',
        lastName: 'User',
        email: 'test@example.com',
        source: 'WEBSITE' as LeadSource,
      };

      const mockResponse = {
        leadId: 'lead-new',
        message: 'Lead captured successfully',
        score: 45,
      };

      global.fetch = vi.fn().mockResolvedValue({
        ok: true,
        json: () => Promise.resolve(mockResponse),
      });

      const result = await salesService.captureLead(captureRequest);

      expect(fetch).toHaveBeenCalledWith(
        'http://localhost:8106/api/sales/public/capture',
        expect.objectContaining({
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
        })
      );
      expect(result.leadId).toBe('lead-new');
    });
  });
});
