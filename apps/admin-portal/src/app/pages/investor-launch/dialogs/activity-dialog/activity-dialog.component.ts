import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatDialogModule, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { Contact, OutreachActivity } from '../../../../models/investor.model';

export interface ActivityDialogData {
  mode: 'create' | 'edit';
  activity?: OutreachActivity;
  contacts: Contact[];
  preselectedContactId?: string;
}

export interface ActivityDialogResult {
  action: 'save' | 'cancel';
  activity?: Partial<OutreachActivity>;
}

/**
 * Material dialog for logging an outreach activity.
 */
@Component({
  selector: 'app-activity-dialog',
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
    MatDatepickerModule,
    MatNativeDateModule,
    MatAutocompleteModule,
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>{{ getActivityIcon() }}</mat-icon>
      {{ data.mode === 'create' ? 'Log Activity' : 'Edit Activity' }}
    </h2>

    <mat-dialog-content>
      <form [formGroup]="form" class="activity-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Contact</mat-label>
          <mat-select formControlName="contactId">
            @for (contact of data.contacts; track contact.id) {
              <mat-option [value]="contact.id">
                {{ contact.name }} - {{ contact.organization }}
              </mat-option>
            }
          </mat-select>
          @if (form.get('contactId')?.hasError('required') && form.get('contactId')?.touched) {
            <mat-error>Contact is required</mat-error>
          }
        </mat-form-field>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Activity Type</mat-label>
            <mat-select formControlName="type">
              <mat-option value="linkedin_request">
                <mat-icon>link</mat-icon> LinkedIn Request
              </mat-option>
              <mat-option value="linkedin_message">
                <mat-icon>chat</mat-icon> LinkedIn Message
              </mat-option>
              <mat-option value="email">
                <mat-icon>email</mat-icon> Email
              </mat-option>
              <mat-option value="call">
                <mat-icon>phone</mat-icon> Phone Call
              </mat-option>
              <mat-option value="meeting">
                <mat-icon>event</mat-icon> Meeting
              </mat-option>
              <mat-option value="follow_up">
                <mat-icon>replay</mat-icon> Follow-up
              </mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Date</mat-label>
            <input matInput [matDatepicker]="picker" formControlName="date" />
            <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
            <mat-datepicker #picker></mat-datepicker>
          </mat-form-field>
        </div>

        @if (data.mode === 'edit') {
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Status</mat-label>
            <mat-select formControlName="status">
              <mat-option value="sent">Sent</mat-option>
              <mat-option value="responded">Responded</mat-option>
              <mat-option value="no_response">No Response</mat-option>
              <mat-option value="scheduled">Scheduled</mat-option>
              <mat-option value="completed">Completed</mat-option>
            </mat-select>
          </mat-form-field>
        }

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Subject</mat-label>
          <input matInput formControlName="subject" placeholder="Subject or topic" />
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Notes</mat-label>
          <textarea matInput formControlName="notes" rows="3" placeholder="Activity details and notes..."></textarea>
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
        {{ data.mode === 'create' ? 'Log Activity' : 'Save Changes' }}
      </button>
    </mat-dialog-actions>
  `,
  styles: [`
    mat-dialog-content {
      min-width: 450px;
    }

    h2[mat-dialog-title] {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .activity-form {
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

    mat-option mat-icon {
      margin-right: 8px;
      font-size: 18px;
      height: 18px;
      width: 18px;
      vertical-align: middle;
    }
  `],
})
export class ActivityDialogComponent {
  readonly dialogRef = inject(MatDialogRef<ActivityDialogComponent>);
  readonly data = inject<ActivityDialogData>(MAT_DIALOG_DATA);
  private fb = inject(FormBuilder);

  form = this.fb.group({
    contactId: [
      this.data.activity?.contactId || this.data.preselectedContactId || '',
      Validators.required,
    ],
    type: [this.data.activity?.type || 'linkedin_request', Validators.required],
    date: [
      this.data.activity?.date ? new Date(this.data.activity.date) : new Date(),
      Validators.required,
    ],
    status: [this.data.activity?.status || 'sent'],
    subject: [this.data.activity?.subject || ''],
    notes: [this.data.activity?.notes || ''],
  });

  getActivityIcon(): string {
    const type = this.form.get('type')?.value;
    const icons: Record<string, string> = {
      linkedin_request: 'link',
      linkedin_message: 'chat',
      email: 'email',
      call: 'phone',
      meeting: 'event',
      follow_up: 'replay',
    };
    return icons[type || 'linkedin_request'] || 'note';
  }

  onCancel(): void {
    this.dialogRef.close({ action: 'cancel' });
  }

  onSave(): void {
    if (this.form.valid) {
      const formValue = this.form.value;
      const contact = this.data.contacts.find((c) => c.id === formValue.contactId);

      const activity: Partial<OutreachActivity> = {
        contactId: formValue.contactId || '',
        contactName: contact?.name || '',
        type: formValue.type as OutreachActivity['type'],
        status: formValue.status as OutreachActivity['status'],
        date: formValue.date
          ? (formValue.date as Date).toISOString().split('T')[0]
          : new Date().toISOString().split('T')[0],
        subject: formValue.subject || undefined,
        notes: formValue.notes || undefined,
      };

      this.dialogRef.close({ action: 'save', activity });
    }
  }
}
