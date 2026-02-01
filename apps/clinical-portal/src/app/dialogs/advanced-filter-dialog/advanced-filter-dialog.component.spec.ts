import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { AdvancedFilterDialogComponent, AdvancedFilterDialogData } from './advanced-filter-dialog.component';
import { createMockMatDialogRef } from '../../testing/mocks';

describe('AdvancedFilterDialogComponent', () => {
  let fixture: ComponentFixture<AdvancedFilterDialogComponent>;
  let component: AdvancedFilterDialogComponent;
  let dialogRef: { close: jest.Mock };
  let dialogData: AdvancedFilterDialogData;

  beforeEach(async () => {
    dialogRef = { close: jest.fn() };
    dialogData = {
      availableFields: [
        { name: 'status', label: 'Status', type: 'select', options: [{ value: 'Active', label: 'Active' }] },
        { name: 'name', label: 'Name', type: 'text' },
        { name: 'score', label: 'Score', type: 'number' },
        { name: 'createdAt', label: 'Created', type: 'date' },
      ],
    };

    await TestBed.configureTestingModule({
      imports: [AdvancedFilterDialogComponent, NoopAnimationsModule],
      providers: [{ provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
        { provide: MatDialogRef, useValue: createMockMatDialogRef() }],
    }).compileComponents();

    fixture = TestBed.createComponent(AdvancedFilterDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  afterEach(() => {
    jest.useRealTimers();
  });

  it('initializes with one filter row', () => {
    expect(component.filters.length).toBe(1);
  });

  it('loads existing filters when provided', async () => {
    const existingData: AdvancedFilterDialogData = {
      availableFields: dialogData.availableFields,
      currentFilters: {
        logic: 'OR',
        name: 'Existing',
        filters: [
          { field: 'name', operator: 'contains', value: 'Alice' },
          { field: 'status', operator: 'equals', value: 'Active' },
        ],
      },
    };

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [AdvancedFilterDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: existingData },
      ],
    }).compileComponents();

    const loadedFixture = TestBed.createComponent(AdvancedFilterDialogComponent);
    const loadedComponent = loadedFixture.componentInstance;
    loadedFixture.detectChanges();

    expect(loadedComponent.filters.length).toBe(2);
    expect(loadedComponent.filterForm.get('logic')?.value).toBe('OR');
  });

  it('resets operator and values on field change', () => {
    const group = component.filters.at(0);
    group.patchValue({ field: 'name', operator: 'equals', value: 'x' });

    component.onFieldChange(0);

    expect(group.get('operator')?.value).toBe('contains');
    expect(group.get('value')?.value).toBe('');
  });

  it('detects field types and options', () => {
    const group = component.filters.at(0);
    group.patchValue({ field: 'createdAt', operator: 'equals' });
    expect(component.isDateField(0)).toBe(true);

    group.patchValue({ field: 'status' });
    expect(component.isSelectField(0)).toBe(true);
    expect(component.getSelectOptions(0)).toHaveLength(1);
  });

  it('handles between operator requirement', () => {
    const group = component.filters.at(0);
    group.patchValue({ operator: 'between' });
    expect(component.requiresSecondValue(0)).toBe(true);
  });

  it('updates preview when form is valid', () => {
    jest.useFakeTimers();
    const group = component.filters.at(0);
    group.patchValue({ field: 'name', operator: 'contains', value: 'Bob' });

    component.updatePreview();
    expect(component.isLoadingPreview()).toBe(true);

    jest.advanceTimersByTime(500);

    expect(component.isLoadingPreview()).toBe(false);
    expect(component.previewCount()).not.toBeNull();
  });

  it('does not update preview when form is invalid', () => {
    component.updatePreview();
    expect(component.previewCount()).toBeNull();
  });

  it('saves current filter configuration when named', () => {
    component.filterForm.patchValue({ name: 'My Filter' });
    const count = component.savedFilters.length;

    component.saveFilter();

    expect(component.savedFilters.length).toBe(count + 1);
  });

  it('resets filters', () => {
    component.filterForm.patchValue({ logic: 'OR' });
    component.addFilter();

    component.onReset();

    expect(component.filters.length).toBe(1);
    expect(component.filterForm.get('logic')?.value).toBe('AND');
    expect(component.previewCount()).toBeNull();
  });

  it('applies valid filters and closes', () => {
    const group = component.filters.at(0);
    group.patchValue({ field: 'name', operator: 'contains', value: 'Bob' });

    component.onApply();

    expect(dialogRef.close).toHaveBeenCalledWith(
      expect.objectContaining({ logic: 'AND' })
    );
  });

  it('blocks apply when invalid', () => {
    component.onApply();
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('closes on cancel', () => {
    component.onCancel();
    expect(dialogRef.close).toHaveBeenCalledWith(null);
  });
});
