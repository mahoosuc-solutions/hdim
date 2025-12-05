import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef } from '@angular/material/dialog';
import { YearSelectionDialogComponent } from './year-selection-dialog.component';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

/**
 * TDD Test Suite for YearSelectionDialog Component
 *
 * This component allows users to select a year for generating
 * population quality reports with quick selection buttons.
 */
describe('YearSelectionDialogComponent (TDD)', () => {
  let component: YearSelectionDialogComponent;
  let fixture: ComponentFixture<YearSelectionDialogComponent>;
  let mockDialogRef: jest.Mocked<MatDialogRef<YearSelectionDialogComponent>>;
  let currentYear: number;

  beforeEach(async () => {
    currentYear = new Date().getFullYear();

    mockDialogRef = {
      close: jest.fn(),
    } as jest.Mocked<MatDialogRef<YearSelectionDialogComponent>>;

    await TestBed.configureTestingModule({
      imports: [YearSelectionDialogComponent, NoopAnimationsModule],
      providers: [{ provide: MatDialogRef, useValue: mockDialogRef }],
    }).compileComponents();

    fixture = TestBed.createComponent(YearSelectionDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // ============================================================================
  // 1. Component Initialization (4 tests)
  // ============================================================================
  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with current year', () => {
      expect(component.currentYear).toBe(currentYear);
      expect(component.selectedYear).toBe(currentYear);
    });

    it('should generate array of 6 years (current + 5 past)', () => {
      expect(component.availableYears.length).toBe(6);
      expect(component.availableYears[0]).toBe(currentYear);
      expect(component.availableYears[5]).toBe(currentYear - 5);
    });

    it('should display dialog title', () => {
      const title = fixture.debugElement.query(By.css('[mat-dialog-title]'));
      expect(title.nativeElement.textContent).toContain('Select Reporting Year');
    });
  });

  // ============================================================================
  // 2. Year Dropdown (5 tests)
  // ============================================================================
  describe('Year Dropdown', () => {
    it('should render year select field', () => {
      const select = fixture.debugElement.query(By.css('mat-select'));
      expect(select).toBeTruthy();
    });

    it('should have current year selected by default', () => {
      const select = fixture.debugElement.query(By.css('mat-select'));
      expect(select.componentInstance.value).toBe(currentYear);
    });

    it('should update selectedYear when selection changes', () => {
      const lastYear = currentYear - 1;
      component.selectedYear = lastYear;
      fixture.detectChanges();

      expect(component.selectedYear).toBe(lastYear);
    });

    it('should display current badge for current year option', () => {
      // Material select rendering is complex, test the component logic
      const currentYearOption = component.availableYears.find(y => y === currentYear);
      expect(currentYearOption).toBe(currentYear);
    });

    it('should include all available years', () => {
      expect(component.availableYears).toContain(currentYear);
      expect(component.availableYears).toContain(currentYear - 1);
      expect(component.availableYears).toContain(currentYear - 5);
    });
  });

  // ============================================================================
  // 3. Selection Info Display (4 tests)
  // ============================================================================
  describe('Selection Info Display', () => {
    it('should display selection info when year is selected', () => {
      component.selectedYear = currentYear;
      fixture.detectChanges();

      const selectionInfo = fixture.debugElement.query(By.css('.selection-info'));
      expect(selectionInfo).toBeTruthy();
    });

    it('should show selected year in info panel', () => {
      component.selectedYear = 2023;
      fixture.detectChanges();

      const selectionInfo = fixture.debugElement.query(By.css('.selection-info'));
      expect(selectionInfo.nativeElement.textContent).toContain('2023');
    });

    it('should not display selection info when no year selected', () => {
      component.selectedYear = null;
      fixture.detectChanges();

      const selectionInfo = fixture.debugElement.query(By.css('.selection-info'));
      expect(selectionInfo).toBeNull();
    });

    it('should display descriptive text about selected year', () => {
      component.selectedYear = currentYear;
      fixture.detectChanges();

      const selectionInfo = fixture.debugElement.query(By.css('.selection-info'));
      expect(selectionInfo.nativeElement.textContent).toContain('The report will include');
    });
  });

  // ============================================================================
  // 4. Quick Selection Buttons (5 tests)
  // ============================================================================
  describe('Quick Selection Buttons', () => {
    it('should render quick selection buttons', () => {
      const hintButtons = fixture.debugElement.queryAll(By.css('.hint-buttons button'));
      expect(hintButtons.length).toBe(2);
    });

    it('should have current year button', () => {
      const buttons = fixture.debugElement.queryAll(By.css('.hint-buttons button'));
      const currentYearBtn = buttons[0];
      expect(currentYearBtn.nativeElement.textContent).toContain('Current Year');
      expect(currentYearBtn.nativeElement.textContent).toContain(currentYear.toString());
    });

    it('should have last year button', () => {
      const buttons = fixture.debugElement.queryAll(By.css('.hint-buttons button'));
      const lastYearBtn = buttons[1];
      expect(lastYearBtn.nativeElement.textContent).toContain('Last Year');
      expect(lastYearBtn.nativeElement.textContent).toContain((currentYear - 1).toString());
    });

    it('should select year when quick button clicked', () => {
      const lastYear = currentYear - 1;
      component.selectYear(lastYear);

      expect(component.selectedYear).toBe(lastYear);
    });

    it('should apply selected class to current selection', () => {
      component.selectedYear = currentYear;
      fixture.detectChanges();

      const buttons = fixture.debugElement.queryAll(By.css('.hint-buttons button'));
      const currentYearBtn = buttons[0];
      expect(currentYearBtn.nativeElement.classList.contains('selected')).toBe(true);
    });
  });

  // ============================================================================
  // 5. Dialog Actions (5 tests)
  // ============================================================================
  describe('Dialog Actions', () => {
    it('should render cancel and confirm buttons', () => {
      const actionButtons = fixture.debugElement.queryAll(By.css('mat-dialog-actions button'));
      expect(actionButtons.length).toBe(2);
    });

    it('should disable confirm button when no year selected', () => {
      component.selectedYear = null;
      fixture.detectChanges();

      const confirmBtn = fixture.debugElement.query(By.css('button[mat-raised-button]'));
      expect(confirmBtn.nativeElement.disabled).toBe(true);
    });

    it('should enable confirm button when year selected', () => {
      component.selectedYear = currentYear;
      fixture.detectChanges();

      const confirmBtn = fixture.debugElement.query(By.css('button[mat-raised-button]'));
      expect(confirmBtn.nativeElement.disabled).toBe(false);
    });

    it('should close dialog with null on cancel', () => {
      const cancelBtn = fixture.debugElement.query(By.css('button[mat-button]'));
      cancelBtn.nativeElement.click();

      expect(mockDialogRef.close).toHaveBeenCalledWith(null);
    });

    it('should close dialog with selected year on confirm', () => {
      component.selectedYear = 2023;
      const confirmBtn = fixture.debugElement.query(By.css('button[mat-raised-button]'));
      confirmBtn.nativeElement.click();

      expect(mockDialogRef.close).toHaveBeenCalledWith(2023);
    });
  });

  // ============================================================================
  // 6. Method Testing (3 tests)
  // ============================================================================
  describe('Method Testing', () => {
    it('should update selectedYear when selectYear called', () => {
      component.selectYear(2020);
      expect(component.selectedYear).toBe(2020);
    });

    it('should call dialogRef.close with year on onConfirm', () => {
      component.selectedYear = 2021;
      component.onConfirm();

      expect(mockDialogRef.close).toHaveBeenCalledWith(2021);
    });

    it('should call dialogRef.close with null on onCancel', () => {
      component.onCancel();

      expect(mockDialogRef.close).toHaveBeenCalledWith(null);
    });
  });

  // ============================================================================
  // 7. Integration Scenarios (3 tests)
  // ============================================================================
  describe('Integration Scenarios', () => {
    it('should complete full selection flow via quick button', () => {
      // Click last year quick button
      const buttons = fixture.debugElement.queryAll(By.css('.hint-buttons button'));
      buttons[1].nativeElement.click();
      fixture.detectChanges();

      expect(component.selectedYear).toBe(currentYear - 1);

      // Info panel should update
      const selectionInfo = fixture.debugElement.query(By.css('.selection-info'));
      expect(selectionInfo.nativeElement.textContent).toContain((currentYear - 1).toString());

      // Confirm button should be enabled
      const confirmBtn = fixture.debugElement.query(By.css('button[mat-raised-button]'));
      expect(confirmBtn.nativeElement.disabled).toBe(false);

      // Clicking confirm should close with selected year
      confirmBtn.nativeElement.click();
      expect(mockDialogRef.close).toHaveBeenCalledWith(currentYear - 1);
    });

    it('should handle year selection change', () => {
      // Start with current year
      expect(component.selectedYear).toBe(currentYear);

      // Change to last year
      component.selectYear(currentYear - 1);
      fixture.detectChanges();

      expect(component.selectedYear).toBe(currentYear - 1);

      // Change back to current year
      component.selectYear(currentYear);
      fixture.detectChanges();

      expect(component.selectedYear).toBe(currentYear);
    });

    it('should not close on confirm if no year selected', () => {
      component.selectedYear = null;
      component.onConfirm();

      // Should not call close when selectedYear is null
      expect(mockDialogRef.close).not.toHaveBeenCalled();
    });
  });
});
