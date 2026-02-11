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
import { InvestorTask } from '../../../../models/investor.model';

export interface TaskDialogData {
  mode: 'create' | 'edit';
  task?: InvestorTask;
}

export interface TaskDialogResult {
  action: 'save' | 'cancel';
  task?: Partial<InvestorTask>;
}

/**
 * Material dialog for creating or editing an investor task.
 */
@Component({
  selector: 'app-task-dialog',
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
  ],
  template: `
    <h2 mat-dialog-title>
      <mat-icon>{{ data.mode === 'create' ? 'add_task' : 'edit' }}</mat-icon>
      {{ data.mode === 'create' ? 'Add Task' : 'Edit Task' }}
    </h2>

    <mat-dialog-content>
      <form [formGroup]="form" class="task-form">
        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Subject</mat-label>
          <input matInput formControlName="subject" placeholder="Task subject" />
          @if (form.get('subject')?.hasError('required') && form.get('subject')?.touched) {
            <mat-error>Subject is required</mat-error>
          }
        </mat-form-field>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="2" placeholder="Task description"></textarea>
        </mat-form-field>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Category</mat-label>
            <mat-select formControlName="category">
              <mat-option value="investor">Investor</mat-option>
              <mat-option value="customer">Customer</mat-option>
              <mat-option value="content">Content</mat-option>
              <mat-option value="application">Application</mat-option>
              <mat-option value="manual">Manual</mat-option>
            </mat-select>
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Week</mat-label>
            <mat-select formControlName="week">
              <mat-option [value]="1">Week 1</mat-option>
              <mat-option [value]="2">Week 2</mat-option>
              <mat-option [value]="3">Week 3</mat-option>
              <mat-option [value]="4">Week 4</mat-option>
            </mat-select>
          </mat-form-field>
        </div>

        @if (data.mode === 'edit') {
          <mat-form-field appearance="outline" class="full-width">
            <mat-label>Status</mat-label>
            <mat-select formControlName="status">
              <mat-option value="pending">⬜ Pending</mat-option>
              <mat-option value="in_progress">🔄 In Progress</mat-option>
              <mat-option value="completed">✅ Completed</mat-option>
              <mat-option value="blocked">🔒 Blocked</mat-option>
            </mat-select>
          </mat-form-field>
        }

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Deliverable</mat-label>
          <input matInput formControlName="deliverable" placeholder="Link or file path to deliverable" />
          <mat-icon matSuffix>attachment</mat-icon>
        </mat-form-field>

        <div class="form-row">
          <mat-form-field appearance="outline">
            <mat-label>Assignee</mat-label>
            <input matInput formControlName="assignee" placeholder="Who is responsible" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>Due Date</mat-label>
            <input matInput [matDatepicker]="picker" formControlName="dueDate" />
            <mat-datepicker-toggle matSuffix [for]="picker"></mat-datepicker-toggle>
            <mat-datepicker #picker></mat-datepicker>
          </mat-form-field>
        </div>

        <mat-form-field appearance="outline" class="full-width">
          <mat-label>Notes</mat-label>
          <textarea matInput formControlName="notes" rows="2" placeholder="Any additional notes..."></textarea>
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
        {{ data.mode === 'create' ? 'Add Task' : 'Save Changes' }}
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

    .task-form {
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
export class TaskDialogComponent {
  readonly dialogRef = inject(MatDialogRef<TaskDialogComponent>);
  readonly data = inject<TaskDialogData>(MAT_DIALOG_DATA);
  private fb = inject(FormBuilder);

  form = this.fb.group({
    subject: [this.data.task?.subject || '', Validators.required],
    description: [this.data.task?.description || ''],
    category: [this.data.task?.category || 'manual', Validators.required],
    week: [this.data.task?.week || 1, Validators.required],
    status: [this.data.task?.status || 'pending'],
    deliverable: [this.data.task?.deliverable || ''],
    assignee: [this.data.task?.assignee || ''],
    dueDate: [this.data.task?.dueDate ? new Date(this.data.task.dueDate) : null],
    notes: [this.data.task?.notes || ''],
  });

  onCancel(): void {
    this.dialogRef.close({ action: 'cancel' });
  }

  onSave(): void {
    if (this.form.valid) {
      const formValue = this.form.value;
      const task: Partial<InvestorTask> = {
        subject: formValue.subject || '',
        description: formValue.description || '',
        category: formValue.category as InvestorTask['category'],
        week: formValue.week || 1,
        status: formValue.status as InvestorTask['status'],
        deliverable: formValue.deliverable || undefined,
        assignee: formValue.assignee || undefined,
        dueDate: formValue.dueDate
          ? (formValue.dueDate as Date).toISOString().split('T')[0]
          : undefined,
        notes: formValue.notes || undefined,
      };

      this.dialogRef.close({ action: 'save', task });
    }
  }
}
