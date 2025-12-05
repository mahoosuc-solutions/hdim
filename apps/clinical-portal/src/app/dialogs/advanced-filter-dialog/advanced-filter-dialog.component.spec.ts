import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { AdvancedFilterDialogComponent } from './advanced-filter-dialog.component';

describe('AdvancedFilterDialogComponent', () => {
  let component: AdvancedFilterDialogComponent;
  let fixture: ComponentFixture<AdvancedFilterDialogComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [AdvancedFilterDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: { close: jest.fn() } },
        {
          provide: MAT_DIALOG_DATA,
          useValue: {
            availableFields: [
              { name: 'name', label: 'Name', type: 'text' },
              { name: 'age', label: 'Age', type: 'number' },
            ],
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AdvancedFilterDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with one filter row', () => {
    expect(component.filters.length).toBe(1);
  });

  it('should add and remove filter rows', () => {
    component.addFilter();
    expect(component.filters.length).toBe(2);

    component.removeFilter(1);
    expect(component.filters.length).toBe(1);
  });

  it('should get operators for field type', () => {
    const filterGroup = component.filters.at(0);
    filterGroup.patchValue({ field: 'name' });
    const operators = component.getOperatorsForField(0);
    expect(operators.length).toBeGreaterThan(0);
  });
});
