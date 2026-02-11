import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { Contact } from '../../../../models/investor.model';

export interface ContactDialogData {
  mode: 'create' | 'edit';
  contact?: Contact;
}

export interface ContactDialogResult {
  action: 'save' | 'cancel';
  contact?: Partial<Contact>;
}

/**
 * Material dialog for creating or editing an investor contact.
 */
@Component({
  selector: 'app-contact-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatIconModule,
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>{{ data.mode === 'create' ? 'person_add' : 'edit' }}</mat-icon>
      {{ data.mode === 'create' ? 'Add Contact' : 'Edit Contact' }}
    </h2>

    <mat-dialog-content>
      <form [formGroup]="form" class="contact-form">
        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Name</mat-label>
            <input matInput formControlName="name" placeholder="John Smith" />
            @if (form.get('name')?.hasError('required') && form.get('name')?.touched) {
              <mat-error>Name is required</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Title</mat-label>
            <input matInput formControlName="title" placeholder="VP of Quality" />
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Organization</mat-label>
          <input matInput formControlName="organization" placeholder="Kaiser Permanente" />
          @if (form.get('organization')?.hasError('required') && form.get('organization')?.touched) {
            <mat-error>Organization is required</mat-error>
          }
        </mat-form-field>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Email</mat-label>
            <input matInput type="email" formControlName="email" placeholder="john@example.com" />
            @if (form.get('email')?.hasError('email')) {
              <mat-error>Invalid email format</mat-error>
            }
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Phone</mat-label>
            <input matInput formControlName="phone" placeholder="+1 (555) 123-4567" />
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>LinkedIn URL</mat-label>
          <input matInput formControlName="linkedInUrl" placeholder="https://linkedin.com/in/username" />
          <mat-icon matSuffix>link</mat-icon>
        </mat-form-field>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Category</mat-label>
            <mat-select formControlName="category">
              <mat-option value="quality_leader">Quality Leader</mat-option>
              <mat-option value="investor">Investor (VC)</mat-option>
              <mat-option value="angel">Angel Investor</mat-option>
              <mat-option value="partner">Strategic Partner</mat-option>
              <mat-option value="customer">Potential Customer</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Tier</mat-label>
            <mat-select formControlName="tier">
              <mat-option value="1">Tier 1 (High Priority)</mat-option>
              <mat-option value="2">Tier 2 (Medium)</mat-option>
              <mat-option value="3">Tier 3 (Low)</mat-option>
            </mat-select>
          </mat-form-field>
        </div>

        @if (data.mode === 'edit') {
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Status</mat-label>
            <mat-select formControlName="status">
              <mat-option value="not_contacted">Not Contacted</mat-option>
              <mat-option value="connection_sent">Connection Sent</mat-option>
              <mat-option value="connected">Connected</mat-option>
              <mat-option value="in_conversation">In Conversation</mat-option>
              <mat-option value="meeting_scheduled">Meeting Scheduled</mat-option>
              <mat-option value="warm_lead">Warm Lead</mat-option>
              <mat-option value="cold">Cold/No Response</mat-option>
            </mat-select>
          </mat-form-field>
        }

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Notes</mat-label>
          <textarea matInput formControlName="notes" rows="3" placeholder="Any additional notes..."></textarea>
        </mat-form-field>
      </form>
    </mat-dialog-content>

    <mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button
        mat-raised-button
        color="primary"
        (click)="onSave()"
        [disabled]="!form.valid"
      >
        {{ data.mode === 'create' ? 'Add Contact' : 'Save Changes' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    mat-dialog-content {
      min-width: 500px;
    }

    h2[mat-dialog-title] {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .contact-form {
      display: flex;
      flex-direction: column;
      gap: 8px;
      padding-top: 8px;
    }

    .form-row {
      display: flex;
      gap: 16px;
    }

    .form-row mat-form-field {
      flex: 1;
    }

    .full-width {
      width: 100%;
    }

    mat-dialog-actions {
      padding: 16px 24px;
    }
  `],
})
export class ContactDialogComponent {
  readonly dialogRef = inject(MatDialogRef<ContactDialogComponent>);
  readonly data = inject<ContactDialogData>(MAT_DIALOG_DATA);
  private fb = inject(FormBuilder);

  form = this.fb.group({
    name: [this.data.contact?.name || '', Validators.required],
    title: [this.data.contact?.title || ''],
    organization: [this.data.contact?.organization || '', Validators.required],
    email: [this.data.contact?.email || '', Validators.email],
    phone: [this.data.contact?.phone || ''],
    linkedInUrl: [this.data.contact?.linkedInUrl || ''],
    category: [this.data.contact?.category || 'quality_leader', Validators.required],
    tier: [this.data.contact?.tier?.toString() || '2'],
    status: [this.data.contact?.status || 'not_contacted'],
    notes: [this.data.contact?.notes || ''],
  });

  onCancel(): void {
    this.dialogRef.close({ action: 'cancel' });
  }

  onSave(): void {
    if (this.form.valid) {
      const contact: Partial<Contact> = {
        ...this.form.value,
        tier: parseInt(this.form.value.tier || '2', 10) as 1 | 2 | 3,
      } as Partial<Contact>;

      this.dialogRef.close({ action: 'save', contact });
    }
  }
}
