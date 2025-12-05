import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { provideNativeDateAdapter } from '@angular/material/core';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BatchEvaluationDialogComponent } from './batch-evaluation-dialog.component';

describe('BatchEvaluationDialogComponent', () => {
  let component: BatchEvaluationDialogComponent;
  let fixture: ComponentFixture<BatchEvaluationDialogComponent>;

  beforeEach(async () => {
    const dialogRefSpy = { close: jest.fn() } as unknown as MatDialogRef<any>;

    await TestBed.configureTestingModule({
      imports: [BatchEvaluationDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        provideNativeDateAdapter(),
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BatchEvaluationDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load mock patients', () => {
    expect(component.dataSource.data.length).toBeGreaterThan(0);
  });

  it('should select and deselect all patients', () => {
    component.selectAll();
    expect(component.selection.selected.length).toBe(component.dataSource.data.length);

    component.deselectAll();
    expect(component.selection.selected.length).toBe(0);
  });

  it('should not allow run without selections', () => {
    component.deselectAll();
    expect(component.canRun()).toBe(false);
  });
});
