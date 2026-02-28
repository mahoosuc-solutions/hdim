import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, delay } from 'rxjs/operators';
import { API_CONFIG } from '../config/api.config';

export interface CmoOnboardingKpiCard {
  label: string;
  value: string;
  trend: string;
  status: 'improving' | 'stable' | 'at-risk';
}

export interface CmoOnboardingDashboardView {
  kpis: CmoOnboardingKpiCard[];
  topActions: string[];
  governanceSignals: string[];
}

@Injectable({
  providedIn: 'root',
})
export class CmoOnboardingService {
  private readonly endpoint = `${API_CONFIG.API_GATEWAY_URL}/api/executive/cmo-onboarding/summary`;

  constructor(private readonly http: HttpClient) {}

  getDashboardSummary(): Observable<CmoOnboardingDashboardView> {
    return this.http.get<CmoOnboardingDashboardView>(this.endpoint).pipe(
      catchError(() => of(this.getMockSummary()).pipe(delay(200)))
    );
  }

  private getMockSummary(): CmoOnboardingDashboardView {
    return {
      kpis: [
        { label: 'Care Gap Closure Rate', value: '68%', trend: '+6.2 pts', status: 'improving' },
        { label: 'High-Risk Intervention Completion', value: '74%', trend: '+4.8 pts', status: 'improving' },
        { label: 'Data Freshness SLA', value: '99.1%', trend: '+0.7 pts', status: 'stable' },
        { label: 'Compliance Evidence Completion', value: '92%', trend: '+12.0 pts', status: 'improving' },
      ],
      topActions: [
        'Escalate outreach for high-risk diabetic cohort above 30-day threshold.',
        'Increase RN staffing on care-gap follow-up queue for Tuesdays and Wednesdays.',
        'Approve metric definitions for quality composite before monthly QBR.',
      ],
      governanceSignals: [
        'Weekly active quality users: 87%',
        'Workflow SLA adherence: 91%',
        'Last data quality audit: Passed (Feb 27, 2026)',
        'Open high-risk escalations: 3',
      ],
    };
  }
}
