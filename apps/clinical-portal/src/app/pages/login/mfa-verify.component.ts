import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatCheckboxModule } from '@angular/material/checkbox';

/**
 * MFA Verification Component
 * Displayed during login when user has MFA enabled
 */
@Component({
  selector: 'app-mfa-verify',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatCheckboxModule,
  ],
  template: `
    <div class="mfa-container">
      <div class="mfa-header">
        <mat-icon class="mfa-icon">security</mat-icon>
        <h2>Two-Factor Authentication</h2>
        <p>Enter the 6-digit code from your authenticator app</p>
      </div>

      <form [formGroup]="mfaForm" (ngSubmit)="onSubmit()">
        <mat-form-field appearance="outline" class="full-width code-field">
          <mat-label>{{ useRecoveryCode ? 'Recovery Code' : 'Authentication Code' }}</mat-label>
          <input
            matInput
            formControlName="code"
            [placeholder]="useRecoveryCode ? 'XXXXXXXX' : '000000'"
            [maxlength]="useRecoveryCode ? 10 : 6"
            autocomplete="one-time-code"
            autofocus
          />
          <mat-icon matPrefix>{{ useRecoveryCode ? 'vpn_key' : 'pin' }}</mat-icon>
          @if (mfaForm.get('code')?.hasError('required') && mfaForm.get('code')?.touched) {
            <mat-error>Code is required</mat-error>
          }
          @if (mfaForm.get('code')?.hasError('pattern') && !useRecoveryCode) {
            <mat-error>Code must be 6 digits</mat-error>
          }
        </mat-form-field>

        <mat-checkbox
          formControlName="useRecoveryCode"
          (change)="onRecoveryCodeToggle()"
          class="recovery-checkbox"
        >
          Use recovery code instead
        </mat-checkbox>

        <div class="button-row">
          <button
            mat-stroked-button
            type="button"
            (click)="onCancel()"
            [disabled]="isLoading"
          >
            <mat-icon>arrow_back</mat-icon>
            Back
          </button>

          <button
            mat-raised-button
            color="primary"
            type="submit"
            [disabled]="mfaForm.invalid || isLoading"
          >
            @if (isLoading) {
              <mat-spinner diameter="20"></mat-spinner>
              <span>Verifying...</span>
            } @else {
              <mat-icon>check</mat-icon>
              <span>Verify</span>
            }
          </button>
        </div>
      </form>

      @if (errorMessage) {
        <div class="error-message">
          <mat-icon>error</mat-icon>
          <span>{{ errorMessage }}</span>
        </div>
      }

      <div class="help-text">
        <mat-icon>help_outline</mat-icon>
        <span>Can't access your authenticator? Use a recovery code.</span>
      </div>
    </div>
  `,
  styles: [`
    .mfa-container {
      padding: 16px 0;
    }

    .mfa-header {
      text-align: center;
      margin-bottom: 24px;
    }

    .mfa-icon {
      font-size: 48px;
      width: 48px;
      height: 48px;
      color: #667eea;
      margin-bottom: 12px;
    }

    .mfa-header h2 {
      margin: 0 0 8px;
      font-size: 20px;
      font-weight: 500;
    }

    .mfa-header p {
      margin: 0;
      color: #666;
      font-size: 14px;
    }

    .full-width {
      width: 100%;
    }

    .code-field input {
      text-align: center;
      font-size: 24px;
      letter-spacing: 8px;
      font-family: monospace;
    }

    .recovery-checkbox {
      display: block;
      margin: 16px 0;
    }

    .button-row {
      display: flex;
      gap: 12px;
      margin-top: 16px;
    }

    .button-row button {
      flex: 1;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
    }

    .error-message {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px;
      margin-top: 16px;
      background: #ffebee;
      border-radius: 8px;
      color: #c62828;
    }

    .error-message mat-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    .help-text {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      margin-top: 24px;
      font-size: 13px;
      color: #666;
    }

    .help-text mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }
  `],
})
export class MfaVerifyComponent {
  @Input() isLoading = false;
  @Input() errorMessage = '';
  @Output() verify = new EventEmitter<{ code: string; useRecoveryCode: boolean }>();
  @Output() cancel = new EventEmitter<void>();

  mfaForm: FormGroup;
  useRecoveryCode = false;

  constructor(private fb: FormBuilder) {
    this.mfaForm = this.fb.group({
      code: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
      useRecoveryCode: [false],
    });
  }

  onRecoveryCodeToggle(): void {
    this.useRecoveryCode = this.mfaForm.get('useRecoveryCode')?.value;
    const codeControl = this.mfaForm.get('code');

    if (this.useRecoveryCode) {
      codeControl?.setValidators([Validators.required]);
    } else {
      codeControl?.setValidators([Validators.required, Validators.pattern(/^\d{6}$/)]);
    }

    codeControl?.updateValueAndValidity();
    codeControl?.setValue('');
  }

  onSubmit(): void {
    if (this.mfaForm.valid) {
      this.verify.emit({
        code: this.mfaForm.get('code')?.value,
        useRecoveryCode: this.useRecoveryCode,
      });
    }
  }

  onCancel(): void {
    this.cancel.emit();
  }
}
