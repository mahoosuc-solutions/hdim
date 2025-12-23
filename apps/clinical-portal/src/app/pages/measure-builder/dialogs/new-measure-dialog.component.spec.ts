import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NewMeasureDialogComponent } from './new-measure-dialog.component';

describe('NewMeasureDialogComponent', () => {
  let fixture: ComponentFixture<NewMeasureDialogComponent>;
  let component: NewMeasureDialogComponent;
  let dialogRef: { close: jest.Mock };

  beforeEach(async () => {
    dialogRef = { close: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [NewMeasureDialogComponent],
      providers: [
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: {} },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NewMeasureDialogComponent);
    component = fixture.componentInstance;
  });

  it('does not save when name is empty', () => {
    component.name = '   ';

    component.save();

    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('trims fields and closes with a draft payload', () => {
    component.name = '  My Measure  ';
    component.description = '  Description  ';
    component.category = 'CUSTOM';
    component.year = 2024;

    component.save();

    expect(dialogRef.close).toHaveBeenCalledWith({
      name: 'My Measure',
      description: 'Description',
      category: 'CUSTOM',
      year: 2024,
      createdBy: 'clinical-portal',
    });
  });
});
