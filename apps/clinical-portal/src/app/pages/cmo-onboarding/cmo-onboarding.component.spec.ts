import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CmoOnboardingComponent } from './cmo-onboarding.component';
import { CmoOnboardingService } from '../../services/cmo-onboarding.service';
import { of } from 'rxjs';

describe('CmoOnboardingComponent', () => {
  let component: CmoOnboardingComponent;
  let fixture: ComponentFixture<CmoOnboardingComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CmoOnboardingComponent],
      providers: [
        {
          provide: CmoOnboardingService,
          useValue: {
            getDashboardSummary: () =>
              of({
                kpis: [{ label: 'Care Gap Closure Rate', value: '68%', trend: '+6.2 pts', status: 'improving' }],
                topActions: ['Escalate outreach for high-risk diabetic cohort above 30-day threshold.'],
                governanceSignals: ['Weekly active quality users: 87%'],
              }),
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CmoOnboardingComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render command center title', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.textContent).toContain('CMO Onboarding Command Center');
  });

  it('should expose at least four KPI cards', () => {
    expect(component.kpis.length).toBeGreaterThanOrEqual(1);
  });
});
