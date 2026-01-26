import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { ConfirmDialogComponent, ConfirmDialogData } from './confirm-dialog.component';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { createMockMatDialogRef } from '../../testing/mocks';

/**
 * TDD Test Suite for ConfirmDialog Component
 *
 * This component provides a reusable confirmation dialog with customizable
 * title, message, buttons, icon, and colors.
 */
describe('ConfirmDialogComponent (TDD)', () => {
  let component: ConfirmDialogComponent;
  let fixture: ComponentFixture<ConfirmDialogComponent>;
  let mockDialogRef: jest.Mocked<MatDialogRef<ConfirmDialogComponent>>;
  let dialogData: ConfirmDialogData;

  beforeEach(async () => {
    dialogData = {
      title: 'Confirm Action',
      message: 'Are you sure you want to proceed?',
    };

    mockDialogRef = {
      close: jest.fn(),
    } as jest.Mocked<MatDialogRef<ConfirmDialogComponent>>;

    await TestBed.configureTestingModule({
      imports: [ConfirmDialogComponent, NoopAnimationsModule],
      providers: [{ provide: MatDialogRef, useValue: mockDialogRef },
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
        { provide: MatDialogRef, useValue: createMockMatDialogRef() }],
    }).compileComponents();

    fixture = TestBed.createComponent(ConfirmDialogComponent);
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

    it('should receive dialog data', () => {
      expect(component.data).toBeDefined();
      expect(component.data.title).toBe('Confirm Action');
      expect(component.data.message).toBe('Are you sure you want to proceed?');
    });

    it('should display dialog title', () => {
      const title = fixture.debugElement.query(By.css('[mat-dialog-title]'));
      expect(title.nativeElement.textContent.trim()).toBe('Confirm Action');
    });

    it('should display dialog message', () => {
      const content = fixture.debugElement.query(By.css('mat-dialog-content p'));
      expect(content.nativeElement.textContent.trim()).toBe('Are you sure you want to proceed?');
    });
  });

  // ============================================================================
  // 2. Button Rendering (4 tests)
  // ============================================================================
  describe('Button Rendering', () => {
    it('should render cancel and confirm buttons', () => {
      const buttons = fixture.debugElement.queryAll(By.css('button'));
      expect(buttons.length).toBe(2);
    });

    it('should display default button texts', () => {
      const buttons = fixture.debugElement.queryAll(By.css('button'));
      expect(buttons[0].nativeElement.textContent.trim()).toBe('Cancel');
      expect(buttons[1].nativeElement.textContent.trim()).toBe('Confirm');
    });

    it('should display custom button texts', () => {
      component.data.cancelText = 'No';
      component.data.confirmText = 'Yes';
      fixture.detectChanges();

      const buttons = fixture.debugElement.queryAll(By.css('button'));
      expect(buttons[0].nativeElement.textContent.trim()).toBe('No');
      expect(buttons[1].nativeElement.textContent.trim()).toBe('Yes');
    });

    it('should apply confirm button color', () => {
      component.data.confirmColor = 'warn';
      fixture.detectChanges();

      const confirmBtn = fixture.debugElement.query(By.css('button[mat-raised-button]'));
      expect(confirmBtn.componentInstance.color).toBe('warn');
    });
  });

  // ============================================================================
  // 3. Icon Display (4 tests)
  // ============================================================================
  describe('Icon Display', () => {
    it('should not display icon by default', () => {
      const icon = fixture.debugElement.query(By.css('.dialog-icon'));
      expect(icon).toBeNull();
    });

    it('should display icon when provided', () => {
      component.data.icon = 'warning';
      fixture.detectChanges();

      const icon = fixture.debugElement.query(By.css('.dialog-icon mat-icon'));
      expect(icon).toBeTruthy();
      expect(icon.nativeElement.textContent.trim()).toBe('warning');
    });

    it('should apply default icon color', () => {
      component.data.icon = 'warning';
      fixture.detectChanges();

      const iconContainer = fixture.debugElement.query(By.css('.dialog-icon'));
      const color = iconContainer.nativeElement.style.color;
      expect(color).toBe('rgb(255, 152, 0)'); // #ff9800 in RGB
    });

    it('should apply custom icon color', () => {
      component.data.icon = 'error';
      component.data.iconColor = '#f44336';
      fixture.detectChanges();

      const iconContainer = fixture.debugElement.query(By.css('.dialog-icon'));
      const color = iconContainer.nativeElement.style.color;
      expect(color).toBe('rgb(244, 67, 54)'); // #f44336 in RGB
    });
  });

  // ============================================================================
  // 4. User Interactions (4 tests)
  // ============================================================================
  describe('User Interactions', () => {
    it('should close dialog with false on cancel', () => {
      const cancelBtn = fixture.debugElement.query(By.css('button[mat-button]'));
      cancelBtn.nativeElement.click();

      expect(mockDialogRef.close).toHaveBeenCalledWith(false);
    });

    it('should close dialog with true on confirm', () => {
      const confirmBtn = fixture.debugElement.query(By.css('button[mat-raised-button]'));
      confirmBtn.nativeElement.click();

      expect(mockDialogRef.close).toHaveBeenCalledWith(true);
    });

    it('should call onCancel method', () => {
      const cancelSpy = jest.spyOn(component, 'onCancel');
      const cancelBtn = fixture.debugElement.query(By.css('button[mat-button]'));
      cancelBtn.nativeElement.click();

      expect(cancelSpy).toHaveBeenCalled();
    });

    it('should call onConfirm method', () => {
      const confirmSpy = jest.spyOn(component, 'onConfirm');
      const confirmBtn = fixture.debugElement.query(By.css('button[mat-raised-button]'));
      confirmBtn.nativeElement.click();

      expect(confirmSpy).toHaveBeenCalled();
    });
  });

  // ============================================================================
  // 5. HTML Content (2 tests)
  // ============================================================================
  describe('HTML Content', () => {
    it('should render HTML in message', () => {
      component.data.message = '<strong>Important:</strong> This action cannot be undone.';
      fixture.detectChanges();

      const content = fixture.debugElement.query(By.css('mat-dialog-content p'));
      const strong = content.nativeElement.querySelector('strong');
      expect(strong).toBeTruthy();
      expect(strong.textContent).toBe('Important:');
    });

    it('should display plain text message', () => {
      component.data.message = 'Simple message without HTML';
      fixture.detectChanges();

      const content = fixture.debugElement.query(By.css('mat-dialog-content p'));
      expect(content.nativeElement.textContent.trim()).toBe('Simple message without HTML');
    });
  });

  // ============================================================================
  // 6. Integration Scenarios (3 tests)
  // ============================================================================
  describe('Integration Scenarios', () => {
    it('should work as delete confirmation', () => {
      component.data = {
        title: 'Delete Report?',
        message: 'This action cannot be undone.',
        confirmText: 'Delete',
        cancelText: 'Keep',
        confirmColor: 'warn',
        icon: 'delete',
        iconColor: '#f44336',
      };
      fixture.detectChanges();

      const title = fixture.debugElement.query(By.css('[mat-dialog-title]'));
      const icon = fixture.debugElement.query(By.css('.dialog-icon mat-icon'));
      const buttons = fixture.debugElement.queryAll(By.css('button'));

      expect(title.nativeElement.textContent.trim()).toBe('Delete Report?');
      expect(icon.nativeElement.textContent.trim()).toBe('delete');
      expect(buttons[0].nativeElement.textContent.trim()).toBe('Keep');
      expect(buttons[1].nativeElement.textContent.trim()).toBe('Delete');
    });

    it('should work as simple confirmation', () => {
      component.data = {
        title: 'Save Changes?',
        message: 'Do you want to save your changes?',
      };
      fixture.detectChanges();

      const title = fixture.debugElement.query(By.css('[mat-dialog-title]'));
      const icon = fixture.debugElement.query(By.css('.dialog-icon'));
      const buttons = fixture.debugElement.queryAll(By.css('button'));

      expect(title.nativeElement.textContent.trim()).toBe('Save Changes?');
      expect(icon).toBeNull();
      expect(buttons[0].nativeElement.textContent.trim()).toBe('Cancel');
      expect(buttons[1].nativeElement.textContent.trim()).toBe('Confirm');
    });

    it('should work with all custom properties', () => {
      component.data = {
        title: 'Warning',
        message: '<p><strong>Caution:</strong> This is a critical action.</p>',
        confirmText: 'I Understand',
        cancelText: 'Go Back',
        confirmColor: 'accent',
        icon: 'warning',
        iconColor: '#ffa726',
      };
      fixture.detectChanges();

      const title = fixture.debugElement.query(By.css('[mat-dialog-title]'));
      const icon = fixture.debugElement.query(By.css('.dialog-icon mat-icon'));
      const buttons = fixture.debugElement.queryAll(By.css('button'));
      const confirmBtn = fixture.debugElement.query(By.css('button[mat-raised-button]'));

      expect(title.nativeElement.textContent.trim()).toBe('Warning');
      expect(icon.nativeElement.textContent.trim()).toBe('warning');
      expect(buttons[0].nativeElement.textContent.trim()).toBe('Go Back');
      expect(buttons[1].nativeElement.textContent.trim()).toBe('I Understand');
      expect(confirmBtn.componentInstance.color).toBe('accent');
    });
  });
});
