import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import {
  AuthService,
  MfaSetupResponse,
  MfaStatusResponse,
  MfaRecoveryCodesResponse
} from '../../services/auth.service';

/**
 * MFA Settings Component
 * Allows users to set up, manage, and disable MFA
 */
@Component({
  selector: 'app-mfa-settings',
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
    MatDialogModule,
    MatSnackBarModule,
    MatDividerModule,
    MatChipsModule,
  ],
  template: `
    <mat-card class="mfa-settings-card">
      <mat-card-header>
        <mat-icon mat-card-avatar>security</mat-icon>
        <mat-card-title>Two-Factor Authentication</mat-card-title>
        <mat-card-subtitle>
          Add an extra layer of security to your account
        </mat-card-subtitle>
      </mat-card-header>

      <mat-card-content>
        @if (isLoading) {
          <div class="loading-container">
            <mat-spinner diameter="40"></mat-spinner>
            <p>Loading MFA settings...</p>
          </div>
        } @else if (mfaStatus?.mfaEnabled) {
          <!-- MFA Enabled State -->
          <div class="mfa-enabled">
            <div class="status-badge enabled">
              <mat-icon>verified_user</mat-icon>
              <span>MFA Enabled</span>
            </div>

            <div class="mfa-info">
              <div class="info-item">
                <mat-icon>schedule</mat-icon>
                <span>Enabled: {{ mfaStatus?.enabledAt | date:'medium' }}</span>
              </div>
              <div class="info-item">
                <mat-icon>vpn_key</mat-icon>
                <span>Recovery codes remaining: {{ mfaStatus?.remainingRecoveryCodes }}</span>
              </div>
            </div>

            <mat-divider></mat-divider>

            <div class="action-section">
              <h3>Recovery Codes</h3>
              <p class="help-text">
                Recovery codes can be used if you lose access to your authenticator app.
                Keep them in a safe place.
              </p>

              @if (showRecoveryCodes && recoveryCodes.length > 0) {
                <div class="recovery-codes-container">
                  <div class="codes-grid">
                    @for (code of recoveryCodes; track code) {
                      <code class="recovery-code">{{ code }}</code>
                    }
                  </div>
                  <button
                    mat-stroked-button
                    (click)="copyRecoveryCodes()"
                    class="copy-button"
                  >
                    <mat-icon>content_copy</mat-icon>
                    Copy All
                  </button>
                </div>
              }

              <form [formGroup]="verifyForm" (ngSubmit)="regenerateRecoveryCodes()">
                <mat-form-field appearance="outline" class="code-input">
                  <mat-label>Enter authenticator code</mat-label>
                  <input
                    matInput
                    formControlName="code"
                    placeholder="000000"
                    maxlength="6"
                  />
                </mat-form-field>
                <button
                  mat-raised-button
                  color="accent"
                  type="submit"
                  [disabled]="verifyForm.invalid || isProcessing"
                >
                  @if (isProcessing && processingAction === 'regenerate') {
                    <mat-spinner diameter="20"></mat-spinner>
                  } @else {
                    <mat-icon>refresh</mat-icon>
                  }
                  Regenerate Codes
                </button>
              </form>
            </div>

            <mat-divider></mat-divider>

            <div class="action-section danger-zone">
              <h3>Disable MFA</h3>
              <p class="help-text warning">
                Disabling MFA will make your account less secure. Only do this if necessary.
              </p>

              <form [formGroup]="disableForm" (ngSubmit)="disableMfa()">
                <mat-form-field appearance="outline" class="code-input">
                  <mat-label>Enter authenticator code to disable</mat-label>
                  <input
                    matInput
                    formControlName="code"
                    placeholder="000000"
                    maxlength="6"
                  />
                </mat-form-field>
                <button
                  mat-raised-button
                  color="warn"
                  type="submit"
                  [disabled]="disableForm.invalid || isProcessing"
                >
                  @if (isProcessing && processingAction === 'disable') {
                    <mat-spinner diameter="20"></mat-spinner>
                  } @else {
                    <mat-icon>no_encryption</mat-icon>
                  }
                  Disable MFA
                </button>
              </form>
            </div>
          </div>
        } @else {
          <!-- MFA Not Enabled State -->
          <div class="mfa-disabled">
            <div class="status-badge disabled">
              <mat-icon>shield</mat-icon>
              <span>MFA Not Enabled</span>
            </div>

            <div class="benefits">
              <h3>Why enable MFA?</h3>
              <ul>
                <li><mat-icon>check_circle</mat-icon> Protects against password theft</li>
                <li><mat-icon>check_circle</mat-icon> Required for healthcare compliance</li>
                <li><mat-icon>check_circle</mat-icon> Prevents unauthorized access to PHI</li>
              </ul>
            </div>

            @if (setupData) {
              <!-- Setup in progress -->
              <div class="setup-container">
                <h3>Scan QR Code</h3>
                <p class="help-text">
                  Scan this QR code with your authenticator app
                  (Google Authenticator, Authy, etc.)
                </p>

                <div class="qr-container">
                  <img [src]="setupData.qrCodeDataUri" alt="MFA QR Code" class="qr-code" />
                </div>

                <div class="secret-container">
                  <p class="help-text">Or enter this code manually:</p>
                  <code class="secret-code">{{ setupData.secret }}</code>
                  <button mat-icon-button (click)="copySecret()">
                    <mat-icon>content_copy</mat-icon>
                  </button>
                </div>

                <mat-divider></mat-divider>

                <form [formGroup]="confirmForm" (ngSubmit)="confirmSetup()">
                  <mat-form-field appearance="outline" class="full-width">
                    <mat-label>Enter code from authenticator</mat-label>
                    <input
                      matInput
                      formControlName="code"
                      placeholder="000000"
                      maxlength="6"
                    />
                  </mat-form-field>
                  <div class="setup-buttons">
                    <button
                      mat-stroked-button
                      type="button"
                      (click)="cancelSetup()"
                    >
                      Cancel
                    </button>
                    <button
                      mat-raised-button
                      color="primary"
                      type="submit"
                      [disabled]="confirmForm.invalid || isProcessing"
                    >
                      @if (isProcessing && processingAction === 'confirm') {
                        <mat-spinner diameter="20"></mat-spinner>
                      } @else {
                        <mat-icon>check</mat-icon>
                      }
                      Verify & Enable
                    </button>
                  </div>
                </form>
              </div>
            } @else {
              <!-- Initial state - enable button -->
              <button
                mat-raised-button
                color="primary"
                (click)="startSetup()"
                [disabled]="isProcessing"
                class="enable-button"
              >
                @if (isProcessing && processingAction === 'setup') {
                  <mat-spinner diameter="20"></mat-spinner>
                } @else {
                  <mat-icon>add_moderator</mat-icon>
                }
                Enable Two-Factor Authentication
              </button>
            }
          </div>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: [`
    .mfa-settings-card {
      max-width: 600px;
      margin: 24px auto;
    }

    mat-card-header {
      margin-bottom: 16px;
    }

    mat-card-header mat-icon {
      font-size: 40px;
      width: 40px;
      height: 40px;
      color: #667eea;
    }

    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;
      gap: 16px;
    }

    .status-badge {
      display: inline-flex;
      align-items: center;
      gap: 8px;
      padding: 8px 16px;
      border-radius: 20px;
      font-weight: 500;
      margin-bottom: 16px;
    }

    .status-badge.enabled {
      background: #e8f5e9;
      color: #2e7d32;
    }

    .status-badge.disabled {
      background: #fff3e0;
      color: #e65100;
    }

    .mfa-info {
      display: flex;
      flex-direction: column;
      gap: 8px;
      margin-bottom: 24px;
    }

    .info-item {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #666;
    }

    .info-item mat-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
    }

    .action-section {
      padding: 24px 0;
    }

    .action-section h3 {
      margin: 0 0 8px;
      font-size: 16px;
      font-weight: 500;
    }

    .help-text {
      color: #666;
      font-size: 14px;
      margin-bottom: 16px;
    }

    .help-text.warning {
      color: #f57c00;
    }

    .code-input {
      width: 200px;
      margin-right: 16px;
    }

    .danger-zone {
      background: #fff8f8;
      margin: 0 -16px;
      padding: 24px 16px !important;
      border-radius: 8px;
    }

    .benefits ul {
      list-style: none;
      padding: 0;
      margin: 16px 0;
    }

    .benefits li {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 8px 0;
    }

    .benefits li mat-icon {
      color: #4caf50;
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    .enable-button {
      height: 48px;
      font-size: 16px;
    }

    .setup-container {
      margin-top: 24px;
    }

    .qr-container {
      display: flex;
      justify-content: center;
      padding: 24px;
      background: #f5f5f5;
      border-radius: 8px;
      margin: 16px 0;
    }

    .qr-code {
      width: 200px;
      height: 200px;
    }

    .secret-container {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;
      margin-bottom: 24px;
    }

    .secret-code {
      padding: 8px 16px;
      background: #f5f5f5;
      border-radius: 4px;
      font-family: monospace;
      font-size: 14px;
      letter-spacing: 2px;
    }

    .setup-buttons {
      display: flex;
      gap: 16px;
      justify-content: flex-end;
      margin-top: 16px;
    }

    .full-width {
      width: 100%;
    }

    .recovery-codes-container {
      background: #f5f5f5;
      padding: 16px;
      border-radius: 8px;
      margin-bottom: 24px;
    }

    .codes-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 8px;
      margin-bottom: 16px;
    }

    .recovery-code {
      padding: 8px;
      background: white;
      border-radius: 4px;
      font-family: monospace;
      text-align: center;
      font-size: 12px;
    }

    .copy-button {
      width: 100%;
    }

    mat-divider {
      margin: 24px 0;
    }
  `],
})
export class MfaSettingsComponent implements OnInit, OnDestroy {
  mfaStatus: MfaStatusResponse | null = null;
  setupData: MfaSetupResponse | null = null;
  recoveryCodes: string[] = [];
  showRecoveryCodes = false;

  isLoading = true;
  isProcessing = false;
  processingAction = '';

  verifyForm: FormGroup;
  disableForm: FormGroup;
  confirmForm: FormGroup;

  private destroy$ = new Subject<void>();

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private snackBar: MatSnackBar
  ) {
    const codeValidators = [Validators.required, Validators.pattern(/^\d{6}$/)];

    this.verifyForm = this.fb.group({ code: ['', codeValidators] });
    this.disableForm = this.fb.group({ code: ['', codeValidators] });
    this.confirmForm = this.fb.group({ code: ['', codeValidators] });
  }

  ngOnInit(): void {
    this.loadMfaStatus();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadMfaStatus(): void {
    this.isLoading = true;
    this.authService
      .getMfaStatus()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (status) => {
          this.mfaStatus = status;
          this.isLoading = false;
        },
        error: (error) => {
          console.error('Failed to load MFA status:', error);
          this.isLoading = false;
          this.snackBar.open('Failed to load MFA settings', 'Close', { duration: 5000 });
        },
      });
  }

  startSetup(): void {
    this.isProcessing = true;
    this.processingAction = 'setup';

    this.authService
      .setupMfa()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (data) => {
          this.setupData = data;
          this.isProcessing = false;
        },
        error: (error) => {
          console.error('Failed to start MFA setup:', error);
          this.isProcessing = false;
          this.snackBar.open('Failed to start MFA setup', 'Close', { duration: 5000 });
        },
      });
  }

  confirmSetup(): void {
    if (this.confirmForm.invalid) return;

    this.isProcessing = true;
    this.processingAction = 'confirm';

    this.authService
      .confirmMfaSetup(this.confirmForm.get('code')?.value)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.recoveryCodes = response.recoveryCodes;
          this.showRecoveryCodes = true;
          this.setupData = null;
          this.isProcessing = false;
          this.loadMfaStatus();
          this.snackBar.open('MFA enabled successfully! Save your recovery codes.', 'Close', {
            duration: 10000,
          });
        },
        error: (error) => {
          console.error('Failed to confirm MFA setup:', error);
          this.isProcessing = false;
          this.snackBar.open('Invalid code. Please try again.', 'Close', { duration: 5000 });
        },
      });
  }

  cancelSetup(): void {
    this.setupData = null;
    this.confirmForm.reset();
  }

  regenerateRecoveryCodes(): void {
    if (this.verifyForm.invalid) return;

    this.isProcessing = true;
    this.processingAction = 'regenerate';

    this.authService
      .regenerateRecoveryCodes(this.verifyForm.get('code')?.value)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.recoveryCodes = response.recoveryCodes;
          this.showRecoveryCodes = true;
          this.isProcessing = false;
          this.verifyForm.reset();
          this.loadMfaStatus();
          this.snackBar.open('Recovery codes regenerated. Save them now!', 'Close', {
            duration: 10000,
          });
        },
        error: (error) => {
          console.error('Failed to regenerate recovery codes:', error);
          this.isProcessing = false;
          this.snackBar.open('Invalid code. Please try again.', 'Close', { duration: 5000 });
        },
      });
  }

  disableMfa(): void {
    if (this.disableForm.invalid) return;

    this.isProcessing = true;
    this.processingAction = 'disable';

    this.authService
      .disableMfa(this.disableForm.get('code')?.value)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.isProcessing = false;
          this.disableForm.reset();
          this.recoveryCodes = [];
          this.showRecoveryCodes = false;
          this.loadMfaStatus();
          this.snackBar.open('MFA has been disabled', 'Close', { duration: 5000 });
        },
        error: (error) => {
          console.error('Failed to disable MFA:', error);
          this.isProcessing = false;
          this.snackBar.open('Invalid code. Please try again.', 'Close', { duration: 5000 });
        },
      });
  }

  copySecret(): void {
    if (this.setupData?.secret) {
      navigator.clipboard.writeText(this.setupData.secret);
      this.snackBar.open('Secret copied to clipboard', 'Close', { duration: 3000 });
    }
  }

  copyRecoveryCodes(): void {
    const codes = this.recoveryCodes.join('\n');
    navigator.clipboard.writeText(codes);
    this.snackBar.open('Recovery codes copied to clipboard', 'Close', { duration: 3000 });
  }
}
