import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { CmoOnboardingService } from './cmo-onboarding.service';
import { API_CONFIG } from '../config/api.config';

describe('CmoOnboardingService', () => {
  let service: CmoOnboardingService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        CmoOnboardingService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(CmoOnboardingService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should request dashboard summary from executive endpoint', () => {
    service.getDashboardSummary().subscribe((summary) => {
      expect(summary.kpis.length).toBeGreaterThan(0);
      expect(summary.topActions.length).toBeGreaterThan(0);
      expect(summary.governanceSignals.length).toBeGreaterThan(0);
    });

    const req = httpMock.expectOne(`${API_CONFIG.API_GATEWAY_URL}/api/executive/cmo-onboarding/summary`);
    expect(req.request.method).toBe('GET');
    req.flush({
      kpis: [{ label: 'x', value: '1', trend: '+1', status: 'stable' }],
      topActions: ['a'],
      governanceSignals: ['g'],
    });
  });
});
