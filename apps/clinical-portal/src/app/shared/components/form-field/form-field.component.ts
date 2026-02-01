/**
 * Form Field Component
 *
 * Wrapper around Material mat-form-field with consistent styling and validation.
 * Displays validation errors inline.
 *
 * @example
 * <app-form-field
 *   [control]="emailControl"
 *   [label]="'Email Address'"
 *   [type]="'email'"
 *   [hint]="'Enter your work email'"
 *   [icon]="'email'">
 * </app-form-field>
 */
import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormControl, ReactiveFormsModule, ValidationErrors } from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { LoggerService } from '../../../services/logger.service';

/**
 * Validation error definition
 */
export interface ValidationError {
  /** Error key (e.g., 'required', 'email') */
  key: string;
  /** Error message to display */
  message: string;
}

@Component({
  selector: 'app-form-field',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
    MatIconModule,
    MatDatepickerModule,
    MatNativeDateModule
  ],
  template: `
    <mat-form-field [appearance]="appearance" class="form-field">
      <!-- Prefix icon -->
      <mat-icon *ngIf="icon" matPrefix class="field-icon" aria-hidden="true">{{ icon }}</mat-icon>

      <!-- Label -->
      <mat-label>{{ label }}@if (required) { <span aria-label="required"> *</span>}</mat-label>

      <!-- Input field -->
      <input
        *ngIf="type !== 'date'"
        matInput
        [type]="type"
        [formControl]="control"
        [placeholder]="placeholder"
        [required]="required"
        [readonly]="readonly"
        [attr.autocomplete]="autocomplete"
        [attr.aria-required]="required"
        [attr.aria-invalid]="hasError"
        [attr.aria-describedby]="hasError ? 'error-message-' + label : (hint ? 'hint-text-' + label : null)">

      <!-- Date picker -->
      <input
        *ngIf="type === 'date'"
        matInput
        [matDatepicker]="picker"
        [formControl]="control"
        [placeholder]="placeholder"
        [required]="required"
        [readonly]="readonly"
        [attr.aria-required]="required"
        [attr.aria-invalid]="hasError"
        [attr.aria-describedby]="hasError ? 'error-message-' + label : (hint ? 'hint-text-' + label : null)">
      <mat-datepicker-toggle *ngIf="type === 'date'" matSuffix [for]="picker" [attr.aria-label]="'Choose ' + label"></mat-datepicker-toggle>
      <mat-datepicker #picker></mat-datepicker>

      <!-- Hint text -->
      <mat-hint *ngIf="hint && !hasError" [id]="'hint-text-' + label">{{ hint }}</mat-hint>

      <!-- Error messages -->
      <mat-error *ngIf="hasError" [id]="'error-message-' + label" role="alert">
        {{ getErrorMessage() }}
      </mat-error>
    </mat-form-field>
  `,
  styles: [`
    .form-field {
      width: 100%;
    }

    .field-icon {
      color: rgba(0, 0, 0, 0.54);
      margin-right: 8px;
    }

    /* Dark theme */
    @media (prefers-color-scheme: dark) {
      .field-icon {
        color: rgba(255, 255, 255, 0.54);
      }
    }
  `]
})
export class FormFieldComponent implements OnInit {
  /** Form control */
  @Input() control!: FormControl;

  /** Field label */
  @Input() label = '';

  /** Input type */
  @Input() type: 'text' | 'email' | 'password' | 'number' | 'tel' | 'date' = 'text';

  /** Placeholder text */
  @Input() placeholder = '';

  /** Hint text */
  @Input() hint?: string;

  /** Icon name */
  @Input() icon?: string;

  /** Is field required */
  @Input() required = false;

  /** Is field readonly */
  @Input() readonly = false;

  /** Autocomplete attribute */
  @Input() autocomplete = 'off';

  /** Material appearance */
  @Input() appearance: 'fill' | 'outline' = 'outline';

  /** Custom error messages */
  @Input() errors: ValidationError[] = [];

  /** Default error messages */
  private defaultErrors: { [key: string]: string } = {
    required: 'This field is required',
    email: 'Please enter a valid email address',
    min: 'Value is too low',
    max: 'Value is too high',
    minlength: 'Input is too short',
    maxlength: 'Input is too long',
    pattern: 'Invalid format'
  };

  private get logger() {
    return this.loggerService.withContext('FormFieldComponent');
  }

  constructor(private loggerService: LoggerService) {}

  ngOnInit(): void {
    if (!this.control) {
      this.loggerService.error('FormFieldComponent: control input is required');
    }
  }

  get hasError(): boolean {
    return this.control && this.control.invalid && (this.control.dirty || this.control.touched);
  }

  getErrorMessage(): string {
    if (!this.control || !this.control.errors) {
      return '';
    }

    const errors = this.control.errors;
    const errorKey = Object.keys(errors)[0];

    // Check for custom error message
    const customError = this.errors.find(e => e.key === errorKey);
    if (customError) {
      return customError.message;
    }

    // Use default error message
    if (this.defaultErrors[errorKey]) {
      // Handle errors with parameters
      if (errorKey === 'minlength') {
        return `Minimum length is ${errors[errorKey].requiredLength} characters`;
      }
      if (errorKey === 'maxlength') {
        return `Maximum length is ${errors[errorKey].requiredLength} characters`;
      }
      if (errorKey === 'min') {
        return `Minimum value is ${errors[errorKey].min}`;
      }
      if (errorKey === 'max') {
        return `Maximum value is ${errors[errorKey].max}`;
      }
      return this.defaultErrors[errorKey];
    }

    return 'Invalid input';
  }
}
