import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MeasureMetadataDialogComponent } from './measure-metadata-dialog.component';
import { CustomMeasureService } from '../../../services/custom-measure.service';
import {
  expectAlertBanner,
  expectInlineValidationText,
} from '../../../../testing/dialog-a11y.helper';

describe('MeasureMetadataDialogComponent', () => {
  let fixture: ComponentFixture<MeasureMetadataDialogComponent>;
  let component: MeasureMetadataDialogComponent;
  let dialogRef: { close: jest.Mock };
  let customMeasureService: { update: jest.Mock };

  beforeEach(async () => {
    dialogRef = { close: jest.fn() };
    customMeasureService = { update: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [MeasureMetadataDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: CustomMeasureService, useValue: customMeasureService },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            measure: {
              id: 'm-1',
              name: 'Measure One',
              description: 'desc',
              category: 'CUSTOM',
              year: 2026,
              owner: 'Team A',
              clinicalFocus: 'Diabetes',
              reportingCadence: 'MONTHLY',
              targetThreshold: '75%',
              priority: 'HIGH',
              implementationNotes: 'Pilot',
              tags: 'diabetes,quality',
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(MeasureMetadataDialogComponent);
    component = fixture.componentInstance;
  });

  it('initializes form fields from input measure', () => {
    expect(component.name).toBe('Measure One');
    expect(component.owner).toBe('Team A');
    expect(component.priority).toBe('HIGH');
  });

  it('returns update payload on save', () => {
    customMeasureService.update.mockReturnValue(of({ id: 'm-1', name: 'Updated Name' }));
    component.name = 'Updated Name';
    component.reportingCadence = 'QUARTERLY';
    component.showClientValidation = true;

    component.save();

    expect(component.showClientValidation).toBe(false);
    expect(customMeasureService.update).toHaveBeenCalledWith(
      'm-1',
      expect.objectContaining({
        name: 'Updated Name',
        reportingCadence: 'QUARTERLY',
      })
    );
    expect(dialogRef.close).toHaveBeenCalledWith(
      expect.objectContaining({
        id: 'm-1',
        name: 'Updated Name',
      })
    );
  });

  it('does not save when name is blank', () => {
    component.name = '   ';
    component.save();

    expect(component.showClientValidation).toBe(true);
    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(customMeasureService.update).not.toHaveBeenCalled();
  });

  it('does not save when year is out of valid range', () => {
    component.year = 1800;
    component.save();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(customMeasureService.update).not.toHaveBeenCalled();
  });

  it('does not save when reporting cadence is invalid', () => {
    component.reportingCadence = 'WEEKLY';
    component.save();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(customMeasureService.update).not.toHaveBeenCalled();
  });

  it('does not save when priority is invalid', () => {
    component.priority = 'URGENT';
    component.save();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(customMeasureService.update).not.toHaveBeenCalled();
  });

  it('maps backend field errors inline', () => {
    customMeasureService.update.mockReturnValue(
      throwError(() => ({
        error: {
          fieldErrors: [
            { field: 'priority', defaultMessage: 'Priority must be LOW, MEDIUM, or HIGH' },
            { field: 'year', defaultMessage: 'Year must be >= 2000' },
          ],
        },
      }))
    );

    component.save();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.apiFieldErrors['priority']).toBe(
      'Priority must be LOW, MEDIUM, or HIGH'
    );
    expect(component.apiFieldErrors['year']).toBe('Year must be >= 2000');
  });

  it('shows non-field api error message when no field errors are present', () => {
    customMeasureService.update.mockReturnValue(
      throwError(() => ({
        status: 500,
        error: {
          message: 'Service unavailable',
        },
      }))
    );

    component.save();

    expect(dialogRef.close).not.toHaveBeenCalled();
    expect(component.apiErrorMessage).toBe('Service unavailable');
  });

  it('retries save after transient api failure', () => {
    customMeasureService.update
      .mockReturnValueOnce(
        throwError(() => ({
          status: 500,
          error: { message: 'Service unavailable' },
        }))
      )
      .mockReturnValueOnce(of({ id: 'm-1', name: 'Measure One' }));

    component.save();
    expect(component.apiErrorMessage).toBe('Service unavailable');
    expect(customMeasureService.update).toHaveBeenCalledTimes(1);

    component.retrySave();

    expect(customMeasureService.update).toHaveBeenCalledTimes(2);
    expect(component.apiErrorMessage).toBe('');
    expect(dialogRef.close).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'm-1', name: 'Measure One' })
    );
  });

  it('renders non-field error banner with alert role', () => {
    component.apiErrorMessage = 'Service unavailable';
    fixture.detectChanges();

    expectAlertBanner(fixture, 'Service unavailable');
  });

  it('renders inline client validation message for missing name', () => {
    component.name = ' ';
    component.showClientValidation = true;
    fixture.detectChanges();

    expectInlineValidationText(fixture, 'Measure name is required');
  });

  it('renders client validation message after touching invalid field', () => {
    component.name = ' ';
    component.showClientValidation = false;
    component.touchField('name');
    fixture.detectChanges();

    expectInlineValidationText(fixture, 'Measure name is required');
  });
});
