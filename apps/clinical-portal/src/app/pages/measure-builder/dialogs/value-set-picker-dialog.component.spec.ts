import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { ValueSetPickerDialogComponent } from './value-set-picker-dialog.component';
import { CqlEngineService } from '../../../services/cql-engine.service';

describe('ValueSetPickerDialogComponent', () => {
  let fixture: ComponentFixture<ValueSetPickerDialogComponent>;
  let component: ValueSetPickerDialogComponent;
  let dialogRef: { close: jest.Mock };
  let cqlEngineService: jest.Mocked<CqlEngineService>;

  const valueSets = [
    { id: 'v1', name: 'Diabetes', oid: '1.2.3', category: 'Diagnoses', version: '2024', codeCount: 12 },
    { id: 'v2', name: 'A1C Lab', oid: '2.3.4', category: 'Laboratory', version: '2023', codeCount: 7 },
  ];

  beforeEach(async () => {
    dialogRef = { close: jest.fn() };
    cqlEngineService = {
      listValueSets: jest.fn(),
    } as unknown as jest.Mocked<CqlEngineService>;

    await TestBed.configureTestingModule({
      imports: [ValueSetPickerDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { measureId: 'm1', currentValueSets: [{ id: 'v2' }] },
        },
        { provide: CqlEngineService, useValue: cqlEngineService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ValueSetPickerDialogComponent);
    component = fixture.componentInstance;
  });

  it('loads value sets and marks current selections', () => {
    cqlEngineService.listValueSets.mockReturnValue(of(valueSets));

    fixture.detectChanges();

    expect(component.valueSets).toHaveLength(2);
    expect(component.valueSets[1].selected).toBe(true);
    expect(component.dataSource.data).toHaveLength(2);
    expect(component.loading).toBe(false);
  });

  it('filters results by search text and category', () => {
    cqlEngineService.listValueSets.mockReturnValue(of(valueSets));

    fixture.detectChanges();

    component.searchText = 'a1c';
    component.selectedCategory = 'Laboratory';
    component.applyFilter();

    expect(component.dataSource.filteredData).toHaveLength(1);
    expect(component.dataSource.filteredData[0].id).toBe('v2');
  });

  it('toggles selection state and counts selected items', () => {
    component.valueSets = valueSets.map((vs) => ({ ...vs, selected: false }));

    component.toggleSelection(component.valueSets[0]);

    expect(component.valueSets[0].selected).toBe(true);
    expect(component.getSelectedCount()).toBe(1);
  });

  it('closes with selected value sets on save', () => {
    component.valueSets = valueSets.map((vs) => ({ ...vs, selected: vs.id === 'v1' }));

    component.save();

    expect(dialogRef.close).toHaveBeenCalledWith([
      expect.objectContaining({ id: 'v1' }),
    ]);
  });
});
