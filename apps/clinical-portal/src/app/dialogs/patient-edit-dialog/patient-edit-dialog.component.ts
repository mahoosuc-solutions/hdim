import { Component, Inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import {
  MatDialogModule,
  MatDialogRef,
  MAT_DIALOG_DATA,
} from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatStepperModule } from '@angular/material/stepper';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { LoadingButtonComponent } from '../../shared/components/loading-button/loading-button.component';
import { Patient } from '../../models/patient.model';

/**
 * Patient Edit Dialog Data Interface
 */
export interface PatientEditDialogData {
  patient?: Patient; // Optional, for edit mode
  mode: 'create' | 'edit';
}

/**
 * Patient Edit Dialog Component
 *
 * Multi-step form for adding or editing patient information.
 *
 * Features:
 * - Three-step form: Demographics, Contact, Insurance
 * - Full validation with error messages
 * - FHIR-compliant output format
 * - Loading states for save operation
 *
 * Usage:
 * ```typescript
 * const dialogRef = this.dialog.open(PatientEditDialogComponent, {
 *   data: { mode: 'create' } // or { mode: 'edit', patient: existingPatient }
 * });
 *
 * dialogRef.afterClosed().subscribe((patient: Patient | null) => {
 *   if (patient) {
 *     // Handle saved patient
 *   }
 * });
 * ```
 */
@Component({
  selector: 'app-patient-edit-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatStepperModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatDatepickerModule,
    MatNativeDateModule,
    LoadingButtonComponent,
  ],
  templateUrl: './patient-edit-dialog.component.html',
  styleUrls: ['./patient-edit-dialog.component.scss'],
})
export class PatientEditDialogComponent implements OnInit {
  demographicsForm!: FormGroup;
  contactForm!: FormGroup;
  insuranceForm!: FormGroup;

  isSaving = signal(false);
  isEditMode: boolean;

  genderOptions = [
    { value: 'male', label: 'Male' },
    { value: 'female', label: 'Female' },
    { value: 'other', label: 'Other' },
    { value: 'unknown', label: 'Unknown' },
  ];

  phoneTypeOptions = [
    { value: 'home', label: 'Home' },
    { value: 'work', label: 'Work' },
    { value: 'mobile', label: 'Mobile' },
  ];

  addressTypeOptions = [
    { value: 'home', label: 'Home' },
    { value: 'work', label: 'Work' },
  ];

  constructor(
    private fb: FormBuilder,
    private dialogRef: MatDialogRef<PatientEditDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: PatientEditDialogData
  ) {
    this.isEditMode = data.mode === 'edit';
  }

  ngOnInit(): void {
    this.initializeForms();
    if (this.isEditMode && this.data.patient) {
      this.populateFormsWithPatientData(this.data.patient);
    }
  }

  /**
   * Initialize all form groups with validation
   */
  private initializeForms(): void {
    // Demographics Form
    this.demographicsForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      dateOfBirth: ['', Validators.required],
      gender: ['', Validators.required],
      mrn: ['', [Validators.required, Validators.pattern(/^[A-Za-z0-9-]+$/)]],
    });

    // Contact Form
    this.contactForm = this.fb.group({
      phoneNumber: ['', [Validators.pattern(/^\+?[\d\s\-\(\)]+$/)]],
      phoneType: ['mobile'],
      email: ['', [Validators.email]],
      addressLine1: [''],
      addressLine2: [''],
      city: [''],
      state: [''],
      postalCode: ['', [Validators.pattern(/^\d{5}(-\d{4})?$/)]],
      country: ['US'],
      addressType: ['home'],
    });

    // Insurance Form
    this.insuranceForm = this.fb.group({
      insuranceProvider: [''],
      policyNumber: [''],
      groupNumber: [''],
      subscriberId: [''],
    });
  }

  /**
   * Populate forms with existing patient data
   */
  private populateFormsWithPatientData(patient: Patient): void {
    const name = patient.name?.[0];
    const phone = patient.telecom?.find((t) => t.system === 'phone');
    const email = patient.telecom?.find((t) => t.system === 'email');
    const address = patient.address?.[0];
    const mrnIdentifier = patient.identifier?.find(
      (id) => id.type?.text === 'Medical Record Number'
    );

    // Demographics
    this.demographicsForm.patchValue({
      firstName: name?.given?.[0] || '',
      lastName: name?.family || '',
      dateOfBirth: patient.birthDate ? new Date(patient.birthDate) : null,
      gender: patient.gender || '',
      mrn: mrnIdentifier?.value || '',
    });

    // Contact
    this.contactForm.patchValue({
      phoneNumber: phone?.value || '',
      phoneType: phone?.use || 'mobile',
      email: email?.value || '',
      addressLine1: address?.line?.[0] || '',
      addressLine2: address?.line?.[1] || '',
      city: address?.city || '',
      state: address?.state || '',
      postalCode: address?.postalCode || '',
      country: address?.country || 'US',
      addressType: address?.use || 'home',
    });

    // Insurance - would be extended with actual insurance data
    // For now, leaving empty as insurance is typically in Coverage resource
  }

  /**
   * Build FHIR Patient resource from form data
   */
  private buildPatientResource(): Patient {
    const demographics = this.demographicsForm.value;
    const contact = this.contactForm.value;

    const patient: Patient = {
      resourceType: 'Patient',
      id: this.data.patient?.id || `patient-${Date.now()}`,
      active: true,
      name: [
        {
          use: 'official',
          family: demographics.lastName,
          given: [demographics.firstName],
          text: `${demographics.firstName} ${demographics.lastName}`,
        },
      ],
      gender: demographics.gender,
      birthDate: this.formatDate(demographics.dateOfBirth),
      identifier: [
        {
          type: {
            text: 'Medical Record Number',
          },
          system: 'http://hospital.example.org/patients',
          value: demographics.mrn,
        },
      ],
    };

    // Add telecom if provided
    const telecom = [];
    if (contact.phoneNumber) {
      telecom.push({
        system: 'phone' as const,
        value: contact.phoneNumber,
        use: contact.phoneType,
      });
    }
    if (contact.email) {
      telecom.push({
        system: 'email' as const,
        value: contact.email,
      });
    }
    if (telecom.length > 0) {
      patient.telecom = telecom;
    }

    // Add address if provided
    if (contact.addressLine1 || contact.city || contact.state) {
      const addressLines = [contact.addressLine1, contact.addressLine2].filter(
        (line) => line
      );
      patient.address = [
        {
          use: contact.addressType,
          type: 'physical' as const,
          line: addressLines,
          city: contact.city,
          state: contact.state,
          postalCode: contact.postalCode,
          country: contact.country,
        },
      ];
    }

    return patient;
  }

  /**
   * Format date to YYYY-MM-DD
   */
  private formatDate(date: Date): string {
    if (!date) return '';
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}-${month}-${day}`;
  }

  /**
   * Save patient and close dialog
   */
  onSave(): void {
    if (!this.isFormValid()) {
      this.markAllAsTouched();
      return;
    }

    this.isSaving.set(true);

    // Simulate async save operation
    setTimeout(() => {
      const patient = this.buildPatientResource();
      this.isSaving.set(false);
      this.dialogRef.close(patient);
    }, 1000);
  }

  /**
   * Check if all forms are valid
   */
  private isFormValid(): boolean {
    return (
      this.demographicsForm.valid &&
      this.contactForm.valid &&
      this.insuranceForm.valid
    );
  }

  /**
   * Mark all form fields as touched to show validation errors
   */
  private markAllAsTouched(): void {
    this.demographicsForm.markAllAsTouched();
    this.contactForm.markAllAsTouched();
    this.insuranceForm.markAllAsTouched();
  }

  /**
   * Cancel and close dialog
   */
  onCancel(): void {
    this.dialogRef.close(null);
  }

  /**
   * Get error message for a form field
   */
  getErrorMessage(form: FormGroup, fieldName: string): string {
    const field = form.get(fieldName);
    if (!field || !field.errors) return '';

    if (field.errors['required']) {
      return 'This field is required';
    }
    if (field.errors['minlength']) {
      const minLength = field.errors['minlength'].requiredLength;
      return `Minimum ${minLength} characters required`;
    }
    if (field.errors['email']) {
      return 'Please enter a valid email address';
    }
    if (field.errors['pattern']) {
      if (fieldName === 'mrn') {
        return 'MRN can only contain letters, numbers, and hyphens';
      }
      if (fieldName === 'phoneNumber') {
        return 'Please enter a valid phone number';
      }
      if (fieldName === 'postalCode') {
        return 'Please enter a valid ZIP code (e.g., 12345 or 12345-6789)';
      }
    }
    return 'Invalid value';
  }
}
