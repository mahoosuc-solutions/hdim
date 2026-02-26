import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { CreateMeasurePageComponent } from './create-measure-page.component';
import { CustomMeasureService } from '../../services/custom-measure.service';
import { ToastService } from '../../services/toast.service';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('CreateMeasurePageComponent', () => {
  let component: CreateMeasurePageComponent;
  let fixture: ComponentFixture<CreateMeasurePageComponent>;
  let mockCustomMeasureService: jest.Mocked<CustomMeasureService>;
  let mockToast: jest.Mocked<ToastService>;
  let router: Router;

  beforeEach(async () => {
    mockCustomMeasureService = {
      createDraft: jest.fn(),
      updateCql: jest.fn(),
    } as unknown as jest.Mocked<CustomMeasureService>;

    mockToast = {
      success: jest.fn(),
      error: jest.fn(),
      warning: jest.fn(),
      info: jest.fn(),
    } as unknown as jest.Mocked<ToastService>;

    await TestBed.configureTestingModule({
      imports: [CreateMeasurePageComponent, NoopAnimationsModule],
      providers: [
        provideRouter([]),
        { provide: CustomMeasureService, useValue: mockCustomMeasureService },
        { provide: ToastService, useValue: mockToast },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateMeasurePageComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
    jest.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  it('creates a draft from template and saves CQL', () => {
    component.ngOnInit();
    const firstTemplate = component.templates[0];
    component.selectTemplate(firstTemplate);

    mockCustomMeasureService.createDraft.mockReturnValue(
      of({ id: 'm-1' } as any)
    );
    mockCustomMeasureService.updateCql.mockReturnValue(of({ id: 'm-1' } as any));

    component.save();

    expect(mockCustomMeasureService.createDraft).toHaveBeenCalled();
    expect(mockCustomMeasureService.updateCql).toHaveBeenCalledWith(
      'm-1',
      firstTemplate.cqlTemplate
    );
    expect(mockToast.success).toHaveBeenCalledWith(
      'Measure draft created from template'
    );
    expect(router.navigate).toHaveBeenCalledWith(['/measure-builder']);
  });

  it('creates a draft from scratch without CQL update', () => {
    component.onTabChange(1);
    component.name = 'Custom Measure';
    component.description = 'Manual configuration';
    component.owner = 'Population Health Team';
    component.reportingCadence = 'QUARTERLY';
    mockCustomMeasureService.createDraft.mockReturnValue(
      of({ id: 'm-2' } as any)
    );

    component.save();

    expect(mockCustomMeasureService.createDraft).toHaveBeenCalled();
    expect(mockCustomMeasureService.createDraft).toHaveBeenCalledWith(
      expect.objectContaining({
        owner: 'Population Health Team',
        reportingCadence: 'QUARTERLY',
      })
    );
    expect(mockCustomMeasureService.updateCql).not.toHaveBeenCalled();
    expect(mockToast.success).toHaveBeenCalledWith('Measure draft created');
  });

  it('shows error toast when draft creation fails', () => {
    component.onTabChange(1);
    component.name = 'Custom Measure';
    mockCustomMeasureService.createDraft.mockReturnValue(
      throwError(() => new Error('failure'))
    );

    component.save();

    expect(mockToast.error).toHaveBeenCalledWith('Failed to create measure');
  });

  it('shows backend validation message when available', () => {
    component.onTabChange(1);
    component.name = 'Custom Measure';
    mockCustomMeasureService.createDraft.mockReturnValue(
      throwError(() => ({
        status: 400,
        error: {
          errors: [{ defaultMessage: 'Priority must be one of: LOW, MEDIUM, HIGH' }],
        },
      }))
    );

    component.save();

    expect(mockToast.error).toHaveBeenCalledWith(
      'Priority must be one of: LOW, MEDIUM, HIGH'
    );
  });

  it('maps backend field errors for inline display', () => {
    component.onTabChange(1);
    component.name = 'Custom Measure';
    mockCustomMeasureService.createDraft.mockReturnValue(
      throwError(() => ({
        status: 400,
        error: {
          fieldErrors: [
            { field: 'priority', defaultMessage: 'Priority must be one of: LOW, MEDIUM, HIGH' },
            { field: 'year', defaultMessage: 'Year must be >= 2000' },
          ],
        },
      }))
    );

    component.save();

    expect(component.apiFieldErrors['priority']).toBe(
      'Priority must be one of: LOW, MEDIUM, HIGH'
    );
    expect(component.apiFieldErrors['year']).toBe('Year must be >= 2000');
  });

  it('does not save when year is outside valid range', () => {
    component.onTabChange(1);
    component.name = 'Custom Measure';
    component.year = 1800;

    component.save();

    expect(mockCustomMeasureService.createDraft).not.toHaveBeenCalled();
  });
});
