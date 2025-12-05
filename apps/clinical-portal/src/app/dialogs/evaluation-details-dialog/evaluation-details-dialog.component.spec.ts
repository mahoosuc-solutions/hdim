import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { EvaluationDetailsDialogComponent } from './evaluation-details-dialog.component';
import { EvaluationService } from '../../services/evaluation.service';

describe('EvaluationDetailsDialogComponent', () => {
  let component: EvaluationDetailsDialogComponent;
  let fixture: ComponentFixture<EvaluationDetailsDialogComponent>;
  let dialogRef: jest.Mocked<MatDialogRef<EvaluationDetailsDialogComponent>>;
  let evaluationService: jest.Mocked<EvaluationService>;

  beforeEach(async () => {
    const dialogRefSpy = { close: jest.fn() } as unknown as MatDialogRef<EvaluationDetailsDialogComponent>;
    const evaluationServiceSpy = {
      getEvaluationDetails: jest.fn(),
    } as unknown as jest.Mocked<EvaluationService>;

    await TestBed.configureTestingModule({
      imports: [EvaluationDetailsDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: EvaluationService, useValue: evaluationServiceSpy },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            evaluationId: 'eval-123',
            patientName: 'John Doe',
            measureName: 'CMS125',
          },
        },
      ],
    }).compileComponents();

    dialogRef = TestBed.inject(MatDialogRef) as jest.Mocked<
      MatDialogRef<EvaluationDetailsDialogComponent>
    >;
    evaluationService = TestBed.inject(
      EvaluationService
    ) as jest.Mocked<EvaluationService>;
    fixture = TestBed.createComponent(EvaluationDetailsDialogComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load evaluation details on init', () => {
    fixture.detectChanges();
    expect(component.isLoading()).toBe(true);
  });

  it('should format dates correctly', () => {
    const date = '2024-01-15T10:30:00Z';
    const formatted = component.formatDate(date);
    expect(formatted).toContain('Jan');
    expect(formatted).toContain('15');
  });

  it('should get correct result colors', () => {
    expect(component.getResultColor('NUMERATOR')).toBe('#4caf50');
    expect(component.getResultColor('DENOMINATOR')).toBe('#ff9800');
    expect(component.getResultColor('EXCLUSION')).toBe('#f44336');
  });

  it('should get correct result icons', () => {
    expect(component.getResultIcon('NUMERATOR')).toBe('check_circle');
    expect(component.getResultIcon('DENOMINATOR')).toBe('pending');
    expect(component.getResultIcon('EXCLUSION')).toBe('cancel');
  });

  it('should get correct resource icons', () => {
    expect(component.getResourceIcon('Patient')).toBe('person');
    expect(component.getResourceIcon('Procedure')).toBe('medical_services');
    expect(component.getResourceIcon('Condition')).toBe('sick');
  });

  it('should close dialog on close button', () => {
    component.onClose();
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('should format JSON correctly', () => {
    const obj = { key: 'value', nested: { prop: 123 } };
    const formatted = component.formatJson(obj);
    expect(formatted).toContain('key');
    expect(formatted).toContain('value');
  });
});
