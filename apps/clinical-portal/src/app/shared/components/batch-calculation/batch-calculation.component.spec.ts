import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { BatchCalculationComponent } from './batch-calculation.component';
import { BatchCalculationService } from '../../../services/batch-calculation.service';
import { of } from 'rxjs';

describe('BatchCalculationComponent', () => {
  let component: BatchCalculationComponent;
  let fixture: ComponentFixture<BatchCalculationComponent>;
  let batchCalculationService: BatchCalculationService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        BatchCalculationComponent,
        HttpClientTestingModule
      ],
      providers: [BatchCalculationService]
    }).compileComponents();

    fixture = TestBed.createComponent(BatchCalculationComponent);
    component = fixture.componentInstance;
    batchCalculationService = TestBed.inject(BatchCalculationService);

    // Mock the getAllJobs method to return empty array
    jest.spyOn(batchCalculationService, 'getAllJobs').mockReturnValue(of([]));

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with no active job', () => {
    expect(component.activeJob).toBeNull();
    expect(component.isCalculating).toBe(false);
  });

  it('should format duration correctly', () => {
    expect(component.formatDuration('PT1.906862298S')).toBe('1.9s');
    expect(component.formatDuration('PT60S')).toBe('60.0s');
  });

  it('should calculate success rate correctly', () => {
    const mockJob: any = {
      totalCalculations: 100,
      successfulCalculations: 75
    };
    expect(component.getSuccessRate(mockJob)).toBe(75);
  });

  it('should handle zero total calculations', () => {
    const mockJob: any = {
      totalCalculations: 0,
      successfulCalculations: 0
    };
    expect(component.getSuccessRate(mockJob)).toBe(0);
  });
});
