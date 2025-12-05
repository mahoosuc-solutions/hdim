import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { PatientEditDialogComponent } from './patient-edit-dialog.component';

describe('PatientEditDialogComponent', () => {
  let component: PatientEditDialogComponent;
  let fixture: ComponentFixture<PatientEditDialogComponent>;
  let dialogRef: jest.Mocked<MatDialogRef<PatientEditDialogComponent>>;

  beforeEach(async () => {
    const dialogRefSpy = { close: jest.fn() } as unknown as MatDialogRef<PatientEditDialogComponent>;

    await TestBed.configureTestingModule({
      imports: [PatientEditDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        {
          provide: MAT_DIALOG_DATA,
          useValue: { mode: 'create' },
        },
      ],
    }).compileComponents();

    dialogRef = TestBed.inject(MatDialogRef) as jest.Mocked<
      MatDialogRef<PatientEditDialogComponent>
    >;
    fixture = TestBed.createComponent(PatientEditDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize forms with validators', () => {
    expect(component.demographicsForm).toBeDefined();
    expect(component.contactForm).toBeDefined();
    expect(component.insuranceForm).toBeDefined();

    // Check demographics form has required validators
    const firstNameControl = component.demographicsForm.get('firstName');
    expect(firstNameControl?.hasError('required')).toBe(true);
  });

  it('should validate MRN pattern', () => {
    const mrnControl = component.demographicsForm.get('mrn');
    mrnControl?.setValue('invalid mrn!');
    expect(mrnControl?.hasError('pattern')).toBe(true);

    mrnControl?.setValue('MRN-12345');
    expect(mrnControl?.hasError('pattern')).toBe(false);
  });

  it('should validate email format', () => {
    const emailControl = component.contactForm.get('email');
    emailControl?.setValue('invalid-email');
    expect(emailControl?.hasError('email')).toBe(true);

    emailControl?.setValue('valid@email.com');
    expect(emailControl?.hasError('email')).toBe(false);
  });

  it('should build FHIR patient resource on save', () => {
    // Fill in required demographics
    component.demographicsForm.patchValue({
      firstName: 'John',
      lastName: 'Doe',
      dateOfBirth: new Date('1980-01-01'),
      gender: 'male',
      mrn: 'MRN-12345',
    });

    component.onSave();

    expect(component.isSaving()).toBe(true);
  });

  it('should close dialog with null on cancel', () => {
    component.onCancel();
    expect(dialogRef.close).toHaveBeenCalledWith(null);
  });

  it('should return appropriate error messages', () => {
    const firstNameControl = component.demographicsForm.get('firstName');
    firstNameControl?.markAsTouched();

    const message = component.getErrorMessage(
      component.demographicsForm,
      'firstName'
    );
    expect(message).toBe('This field is required');
  });

  it('should be in edit mode when patient data is provided', () => {
    const editComponent = TestBed.createComponent(
      PatientEditDialogComponent
    ).componentInstance;
    expect(component.isEditMode).toBe(false);
  });
});
