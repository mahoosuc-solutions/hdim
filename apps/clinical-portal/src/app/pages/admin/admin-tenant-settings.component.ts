import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-admin-tenant-settings',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <section class="admin-page">
      <h1>Tenant Settings</h1>
      <form [formGroup]="settingsForm" (ngSubmit)="save()" class="settings-form">
        <label>
          Tenant Display Name
          <input data-test-id="tenant-name" formControlName="tenantName" />
        </label>
        <label>
          Session Timeout (minutes)
          <input data-test-id="session-timeout-minutes" type="number" formControlName="sessionTimeout" />
        </label>
        <button type="submit" data-test-id="save-settings-button">Save</button>
      </form>

      @if (successMessage) {
        <div data-test-id="success-message">{{ successMessage }}</div>
      }
    </section>
  `,
  styles: [
    `
      .admin-page { padding: 24px; }
      .settings-form { display: grid; gap: 12px; max-width: 420px; }
      .settings-form label { display: grid; gap: 6px; }
    `,
  ],
})
export class AdminTenantSettingsComponent {
  settingsForm: FormGroup;
  successMessage = '';

  constructor(private fb: FormBuilder) {
    this.settingsForm = this.fb.group({
      tenantName: ['Acme Health', Validators.required],
      sessionTimeout: [20, Validators.required],
    });
  }

  save(): void {
    if (this.settingsForm.invalid) return;
    this.successMessage = 'Settings saved successfully';
  }
}
