import { firstValueFrom, of, throwError } from 'rxjs';
import { SDOHReferralService } from './sdoh-referral.service';
import { ApiService } from './api.service';
import { LoggerService } from './logger.service';
import { SDOHReferralRequest } from '../models/sdoh-referral.model';

const createApiService = () => ({
  get: jest.fn(),
  post: jest.fn(),
}) as unknown as ApiService;

const mockLoggerService = {
  withContext: jest.fn().mockReturnValue({
    debug: jest.fn(),
    info: jest.fn(),
    warn: jest.fn(),
    error: jest.fn(),
  }),
  debug: jest.fn(),
  info: jest.fn(),
  warn: jest.fn(),
  error: jest.fn(),
};

describe('SDOHReferralService', () => {
  let apiService: ApiService;
  let service: SDOHReferralService;

  beforeEach(() => {
    apiService = createApiService();
    const mockHttpClient = {} as any;
    service = new SDOHReferralService(mockHttpClient, apiService, mockLoggerService as unknown as LoggerService);
  });

  it('returns staff list and fallback on error', (done) => {
    (apiService.get as jest.Mock)
      .mockReturnValueOnce(throwError(() => new Error('fail')))
      .mockReturnValueOnce(of([{ id: 'staff-1' }]));

    service.getAvailableStaff().subscribe((staff) => {
      expect(staff.length).toBeGreaterThan(0);

      service.getAvailableStaff('social-worker').subscribe((filtered) => {
        expect(filtered.length).toBeGreaterThan(0);
        done();
      };
    });
  });

  it('returns staff availability fallback on error', (done) => {
    (apiService.get as jest.Mock).mockReturnValueOnce(throwError(() => new Error('fail')));

    service.getStaffAvailability('staff-1').subscribe((availability) => {
      expect(availability.available).toBe(true);
      done();
    };
  });

  it('searches resources with fallback mapping', (done) => {
    (apiService.get as jest.Mock)
      .mockReturnValueOnce(throwError(() => new Error('fail')))
      .mockReturnValueOnce(of([{ id: 'r1', name: 'Resource' }]))
      .mockReturnValueOnce(of([{ id: 'r2', name: 'Resource 2' }]));

    service.searchCommunityResources('food').subscribe((resources) => {
      expect(resources).toEqual([]);

      service.search211Resources('food', '12345').subscribe((results) => {
        expect(results[0].source).toBe('211');

        service.searchFindHelpResources('food', '12345').subscribe((findHelp) => {
          expect(findHelp[0].source).toBe('findhelp');
          done();
        };
      });
    });
  });

  it('submits referrals and handles errors', (done) => {
    const request = { patientId: 'p1' } as SDOHReferralRequest;
    (apiService.post as jest.Mock)
      .mockReturnValueOnce(of({ id: 'ref-1' }))
      .mockReturnValueOnce(throwError(() => new Error('fail')));

    service.submitReferral(request).subscribe((referral) => {
      expect(referral.id).toBe('ref-1');

      service.saveDraft(request).subscribe({
        error: (error) => {
          expect(error).toBeInstanceOf(Error);
          done();
        },
      };
    });
  });

  it('updates status and gets metrics', (done) => {
    (apiService.post as jest.Mock)
      .mockReturnValueOnce(of({ id: 'ref-1' }))
      .mockReturnValueOnce(of({ id: 'ref-1' }))
      .mockReturnValueOnce(of({ id: 'ref-1' }));
    (apiService.get as jest.Mock)
      .mockReturnValueOnce(of({ id: 'ref-1' }))
      .mockReturnValueOnce(throwError(() => new Error('fail')));

    service.updateStatus('ref-1', 'completed').subscribe((referral) => {
      expect(referral.id).toBe('ref-1');

      service.sendReferral('ref-1').subscribe(() => {
        service.getReferral('ref-1').subscribe(() => {
          service.getReferralMetrics('patient-1').subscribe((metrics) => {
            expect(metrics.totalReferrals).toBe(0);
            done();
          };
        });
      });
    });
  });

  it('gets patient referrals with criteria and fallback', async () => {
    (apiService.get as jest.Mock)
      .mockReturnValueOnce(of([{ id: 'ref-1' }]))
      .mockReturnValueOnce(throwError(() => new Error('fail')));

    const criteria = {
      status: ['pending', 'completed'],
      urgency: ['urgent'],
      dateRange: { start: new Date('2024-01-01'), end: new Date('2024-01-10') },
    };

    await firstValueFrom(service.getPatientReferrals('patient-1', criteria));
    const fallback = await firstValueFrom(service.getPatientReferrals('patient-1', criteria));

    expect(fallback).toEqual([]);
  });

  it('handles update/send/document errors', async () => {
    (apiService.post as jest.Mock)
      .mockReturnValueOnce(throwError(() => new Error('fail')))
      .mockReturnValueOnce(throwError(() => new Error('fail')))
      .mockReturnValueOnce(throwError(() => new Error('fail')));

    await expect(firstValueFrom(service.updateStatus('ref-1', 'completed'))).rejects.toThrow('fail');
    await expect(firstValueFrom(service.sendReferral('ref-1'))).rejects.toThrow('fail');
    await expect(firstValueFrom(service.documentOutcome('ref-1', { summary: 'done' } as any))).rejects.toThrow('fail');
  });

  it('falls back to social-worker when category missing', () => {
    const destinations = service.getSuggestedDestinations('other' as any);
    expect(destinations).toEqual(['social-worker']);
  });

  it('returns urgency options and suggested destinations', () => {
    const options = service.getUrgencyOptions();
    expect(options.length).toBe(4);

    const destinations = service.getSuggestedDestinations('housing');
    expect(destinations.length).toBeGreaterThan(0);
  });
});
