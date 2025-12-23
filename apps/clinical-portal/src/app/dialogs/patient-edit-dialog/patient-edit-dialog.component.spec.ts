import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { PatientEditDialogComponent, PatientEditDialogData } from './patient-edit-dialog.component';

describe('PatientEditDialogComponent', () => {
  let component: PatientEditDialogComponent;
  let fixture: ComponentFixture<PatientEditDialogComponent>;
  let dialogRef: jest.Mocked<MatDialogRef<PatientEditDialogComponent>>;

  let dialogData: PatientEditDialogData;

  beforeEach(async () => {
    dialogData = { mode: 'create' };
    const dialogRefSpy = { close: jest.fn() } as unknown as MatDialogRef<PatientEditDialogComponent>;

    await TestBed.configureTestingModule({
      imports: [PatientEditDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: dialogRefSpy },
        {
          provide: MAT_DIALOG_DATA,
          useValue: dialogData,
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

  it('saves a valid patient and closes dialog', () => {
    jest.useFakeTimers();

    component.demographicsForm.patchValue({
      firstName: 'Jane',
      lastName: 'Smith',
      dateOfBirth: new Date('1990-05-10'),
      gender: 'female',
      mrn: 'MRN-999',
    });
    component.contactForm.patchValue({
      phoneNumber: '+15551234567',
      phoneType: 'mobile',
      email: 'jane@example.com',
      addressLine1: '123 Main St',
      city: 'Boston',
      state: 'MA',
      postalCode: '02110',
      country: 'US',
      addressType: 'home',
    });

    component.onSave();
    jest.advanceTimersByTime(1000);

    const savedPatient = dialogRef.close.mock.calls[0][0];
    expect(savedPatient).toEqual(
      expect.objectContaining({
        resourceType: 'Patient',
        gender: 'female',
        telecom: expect.arrayContaining([
          expect.objectContaining({ system: 'phone', value: '+15551234567' }),
          expect.objectContaining({ system: 'email', value: 'jane@example.com' }),
        ]),
        address: expect.arrayContaining([
          expect.objectContaining({ city: 'Boston', state: 'MA' }),
        ]),
      })
    );
    expect(savedPatient.birthDate).toMatch(/^1990-05/);
    jest.useRealTimers();
  });

  it('marks all controls as touched when save is invalid', () => {
    component.demographicsForm.patchValue({
      firstName: '',
      lastName: '',
      dateOfBirth: '',
      gender: '',
      mrn: '',
    });

    component.onSave();

    expect(component.demographicsForm.touched).toBe(true);
    expect(component.contactForm.touched).toBe(true);
    expect(component.insuranceForm.touched).toBe(true);
    expect(dialogRef.close).not.toHaveBeenCalled();
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

  it('should return error messages for other validation errors', () => {
    const mrnControl = component.demographicsForm.get('mrn');
    mrnControl?.setValue('bad value!');
    expect(component.getErrorMessage(component.demographicsForm, 'mrn')).toBe(
      'MRN can only contain letters, numbers, and hyphens'
    );

    const phoneControl = component.contactForm.get('phoneNumber');
    phoneControl?.setValue('bad phone');
    expect(component.getErrorMessage(component.contactForm, 'phoneNumber')).toBe(
      'Please enter a valid phone number'
    );

    const postalControl = component.contactForm.get('postalCode');
    postalControl?.setValue('bad zip');
    expect(component.getErrorMessage(component.contactForm, 'postalCode')).toBe(
      'Please enter a valid ZIP code (e.g., 12345 or 12345-6789)'
    );
  });

  it('returns minlength and default validation messages', () => {
    const firstNameControl = component.demographicsForm.get('firstName');
    firstNameControl?.setValue('A');
    expect(component.getErrorMessage(component.demographicsForm, 'firstName')).toBe(
      'Minimum 2 characters required'
    );

    const lastNameControl = component.demographicsForm.get('lastName');
    lastNameControl?.setErrors({ custom: true });
    expect(component.getErrorMessage(component.demographicsForm, 'lastName')).toBe(
      'Invalid value'
    );
  });

  it('returns empty string for unknown field', () => {
    expect(component.getErrorMessage(component.demographicsForm, 'unknown')).toBe('');
  });

  it('builds patient resource without optional contact data', () => {
    jest.useFakeTimers();
    component.demographicsForm.patchValue({
      firstName: 'Alex',
      lastName: 'Green',
      dateOfBirth: new Date('1970-01-01'),
      gender: 'other',
      mrn: 'MRN-000',
    });

    component.onSave();
    jest.advanceTimersByTime(1000);

    const savedPatient = dialogRef.close.mock.calls[0][0];
    expect(savedPatient.telecom).toBeUndefined();
    expect(savedPatient.address).toBeUndefined();
    jest.useRealTimers();
  });

  it('formats empty dates as blank strings', () => {
    expect((component as any).formatDate(undefined)).toBe('');
  });

  it('populates forms in edit mode', async () => {
    const patient = {
      id: 'p1',
      birthDate: '1985-03-04',
      gender: 'male',
      name: [{ family: 'Doe', given: ['John'] }],
      telecom: [
        { system: 'phone', value: '555-1234', use: 'home' },
        { system: 'email', value: 'john@demo.test' },
      ],
      address: [
        {
          line: ['123 Ave', 'Unit 2'],
          city: 'Portland',
          state: 'ME',
          postalCode: '04101',
          country: 'US',
          use: 'home',
        },
      ],
      identifier: [
        { type: { text: 'Medical Record Number' }, value: 'MRN-1' },
      ],
    } as any;

    TestBed.resetTestingModule();
    await TestBed.configureTestingModule({
      imports: [PatientEditDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: MatDialogRef, useValue: { close: jest.fn() } },
        { provide: MAT_DIALOG_DATA, useValue: { mode: 'edit', patient } },
      ],
    }).compileComponents();

    const editFixture = TestBed.createComponent(PatientEditDialogComponent);
    const editComponent = editFixture.componentInstance;
    editFixture.detectChanges();

    expect(editComponent.isEditMode).toBe(true);
    expect(editComponent.demographicsForm.get('firstName')?.value).toBe('John');
    expect(editComponent.contactForm.get('phoneNumber')?.value).toBe('555-1234');
    expect(editComponent.contactForm.get('email')?.value).toBe('john@demo.test');
  });
});
