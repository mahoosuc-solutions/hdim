import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators, AbstractControl } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Router } from '@angular/router';
import { PasswordService } from '../../services/password.service';
import { LoggerService } from '../../services/logger.service';

@Component({
  selector: 'app-change-password',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatButtonModule, MatIconModule, MatProgressBarModule,
  ],
  template: `
    <div class="change-password-container">
      <mat-card class="change-password-card">
        <mat-card-header>
          <mat-card-title>Change Your Password</mat-card-title>
          <mat-card-subtitle>Your password must be changed before continuing.</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          <form [formGroup]="form" (ngSubmit)="onSubmit()">
            <mat-form-field appearance="outline" class="full-width">
              <mat-label>New Password</mat-label>
              <input matInput [type]="hidePassword ? 'password' : 'text'" formControlName="newPassword"
                     data-test-id="new-password-input"
                     aria-label="New password">
              <button mat-icon-button matSuffix type="button" (click)="hidePassword = !hidePassword"
                      [attr.aria-label]="hidePassword ? 'Show password' : 'Hide password'">
                <mat-icon aria-hidden="true">{{hidePassword ? 'visibility_off' : 'visibility'}}</mat-icon>
              </button>
              @if (form.get('newPassword')?.hasError('required') && form.get('newPassword')?.touched) {
                <mat-error>Password is required</mat-error>
              }
              @if (form.get('newPassword')?.hasError('minlength')) {
                <mat-error>Minimum 8 characters</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline" class="full-width">
              <mat-label>Confirm Password</mat-label>
              <input matInput [type]="hideConfirm ? 'password' : 'text'" formControlName="confirmPassword"
                     data-test-id="confirm-password-input"
                     aria-label="Confirm new password">
              <button mat-icon-button matSuffix type="button" (click)="hideConfirm = !hideConfirm"
                      [attr.aria-label]="hideConfirm ? 'Show password' : 'Hide password'">
                <mat-icon aria-hidden="true">{{hideConfirm ? 'visibility_off' : 'visibility'}}</mat-icon>
              </button>
              @if (form.get('confirmPassword')?.hasError('passwordMismatch')) {
                <mat-error>Passwords do not match</mat-error>
              }
            </mat-form-field>

            @if (form.get('newPassword')?.value) {
              <div class="password-strength">
                <mat-progress-bar [value]="passwordStrength" [color]="strengthColor"></mat-progress-bar>
                <span class="strength-label">{{strengthLabel}}</span>
              </div>
            }

            @if (errorMessage) {
              <div class="error-message" data-test-id="error-message">{{errorMessage}}</div>
            }

            <button mat-raised-button color="primary" type="submit" class="full-width"
                    data-test-id="change-password-submit"
                    [disabled]="form.invalid || loading" aria-label="Change password">
              {{loading ? 'Changing...' : 'Change Password'}}
            </button>
          </form>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .change-password-container { display: flex; justify-content: center; align-items: center; min-height: 80vh; }
    .change-password-card { max-width: 400px; width: 100%; }
    .full-width { width: 100%; margin-bottom: 16px; }
    .password-strength { margin-bottom: 16px; }
    .strength-label { font-size: 12px; margin-top: 4px; display: block; }
    .error-message { color: #f44336; margin-bottom: 16px; font-size: 14px; }
  `],
})
export class ChangePasswordComponent {
  form: FormGroup;
  hidePassword = true;
  hideConfirm = true;
  loading = false;
  errorMessage = '';
  private logger;

  constructor(
    private fb: FormBuilder,
    private passwordService: PasswordService,
    private router: Router,
    private loggerService: LoggerService,
  ) {
    this.logger = this.loggerService.withContext('ChangePasswordComponent');
    this.form = this.fb.group({
      newPassword: ['', [Validators.required, Validators.minLength(8)]],
      confirmPassword: ['', [Validators.required]],
    }, { validators: this.passwordMatchValidator });
  }

  passwordMatchValidator(control: AbstractControl) {
    const password = control.get('newPassword')?.value;
    const confirm = control.get('confirmPassword')?.value;
    if (password && confirm && password !== confirm) {
      control.get('confirmPassword')?.setErrors({ passwordMismatch: true });
    }
    return null;
  }

  get passwordStrength(): number {
    const pw = this.form.get('newPassword')?.value || '';
    let score = 0;
    if (pw.length >= 8) score += 25;
    if (pw.length >= 12) score += 25;
    if (/[A-Z]/.test(pw) && /[a-z]/.test(pw)) score += 25;
    if (/[0-9!@#$%^&*]/.test(pw)) score += 25;
    return score;
  }

  get strengthColor(): string {
    return this.passwordStrength >= 75 ? 'primary' : this.passwordStrength >= 50 ? 'accent' : 'warn';
  }

  get strengthLabel(): string {
    const s = this.passwordStrength;
    if (s >= 75) return 'Strong';
    if (s >= 50) return 'Good';
    if (s >= 25) return 'Weak';
    return 'Very weak';
  }

  onSubmit(): void {
    if (this.form.invalid) return;
    this.loading = true;
    this.errorMessage = '';

    this.passwordService.forceChangePassword({ newPassword: this.form.value.newPassword }).subscribe({
      next: () => {
        this.loading = false;
        this.logger.info('Password changed successfully');
        this.router.navigate(['/dashboard']);
      },
      error: (err: { error?: { message?: string } }) => {
        this.loading = false;
        this.errorMessage = err.error?.message || 'Failed to change password. Please try again.';
        this.logger.error('Password change failed');
      },
    });
  }
}
