import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { CxApiService } from '../shared/services/cx-api.service';
import { catchError, forkJoin, of } from 'rxjs';

type PrincipalType = 'staff' | 'customer' | 'investor' | 'agent';

interface CustomerAccessRecord {
  email: string;
  customer_role: string;
  customer_ids: string[];
}

interface IdentityAccessRecord {
  email: string;
  principal_type: PrincipalType;
  role?: string;
  customer_role?: string;
  investor_role?: string;
  customer_ids?: string[];
  active?: boolean;
}

@Component({
  selector: 'cx-access-admin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="access-admin-container">
      <header class="page-header">
        <h1>Access Administration</h1>
        <button class="btn-primary" [disabled]="loading" (click)="reload()">
          {{ loading ? 'Loading...' : 'Refresh' }}
        </button>
      </header>

      <p class="subtitle">
        Manage dynamic access for customer-scoped users and principal identity overrides.
      </p>

      <div class="status success" *ngIf="successMessage">{{ successMessage }}</div>
      <div class="status error" *ngIf="errorMessage">
        {{ errorMessage }}
        <button
          *ngIf="isNotAuthenticatedError()"
          class="btn-link"
          type="button"
          (click)="signIn()"
        >
          Sign in
        </button>
      </div>

      <section class="card">
        <h2>Customer Access</h2>
        <p class="section-help">
          Map customer users to one or more customer IDs and assign customer role.
        </p>

        <form class="form-grid" (ngSubmit)="saveCustomerAccess()">
          <label>
            Email
            <input
              type="email"
              [(ngModel)]="customerForm.email"
              name="customer_email"
              placeholder="user@customer.com"
              required
            />
          </label>
          <label>
            Customer Role
            <select [(ngModel)]="customerForm.customer_role" name="customer_role" required>
              <option value="customer_viewer">customer_viewer</option>
              <option value="customer_admin">customer_admin</option>
            </select>
          </label>
          <label class="wide">
            Customer IDs (comma separated)
            <input
              type="text"
              [(ngModel)]="customerForm.customer_ids"
              name="customer_ids"
              placeholder="cust-001,cust-002"
              required
            />
          </label>
          <div class="wide actions">
            <button class="btn-primary" type="submit">Save Customer Access</button>
            <button class="btn-outline" type="button" (click)="resetCustomerForm()">Clear</button>
          </div>
        </form>

        <table *ngIf="customerAccess.length" class="records-table">
          <thead>
            <tr>
              <th>Email</th>
              <th>Role</th>
              <th>Customer IDs</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let row of customerAccess">
              <td>{{ row.email }}</td>
              <td>{{ row.customer_role || '-' }}</td>
              <td>{{ (row.customer_ids || []).join(', ') }}</td>
              <td class="table-actions">
                <button class="btn-link" (click)="editCustomerAccess(row)">Edit</button>
                <button class="btn-link danger" (click)="removeCustomerAccess(row.email)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p class="empty" *ngIf="!customerAccess.length">No customer access records found.</p>
      </section>

      <section class="card">
        <h2>Identity Access</h2>
        <p class="section-help">
          Define principal type overrides for staff, investor, customer, or agent identities.
        </p>

        <form class="form-grid" (ngSubmit)="saveIdentityAccess()">
          <label>
            Email
            <input
              type="email"
              [(ngModel)]="identityForm.email"
              name="identity_email"
              placeholder="person@domain.com"
              required
            />
          </label>
          <label>
            Principal Type
            <select [(ngModel)]="identityForm.principal_type" name="principal_type" required>
              <option value="staff">staff</option>
              <option value="customer">customer</option>
              <option value="investor">investor</option>
              <option value="agent">agent</option>
            </select>
          </label>
          <label>
            Staff/Principal Role
            <input
              type="text"
              [(ngModel)]="identityForm.role"
              name="identity_role"
              placeholder="internal"
            />
          </label>
          <label>
            Customer Role
            <input
              type="text"
              [(ngModel)]="identityForm.customer_role"
              name="identity_customer_role"
              placeholder="customer_admin"
            />
          </label>
          <label>
            Investor Role
            <input
              type="text"
              [(ngModel)]="identityForm.investor_role"
              name="identity_investor_role"
              placeholder="investor"
            />
          </label>
          <label>
            Active
            <select [(ngModel)]="identityForm.active" name="identity_active">
              <option [ngValue]="true">true</option>
              <option [ngValue]="false">false</option>
            </select>
          </label>
          <label class="wide">
            Customer IDs (comma separated, optional)
            <input
              type="text"
              [(ngModel)]="identityForm.customer_ids"
              name="identity_customer_ids"
              placeholder="cust-001,cust-002"
            />
          </label>
          <div class="wide actions">
            <button class="btn-primary" type="submit">Save Identity Access</button>
            <button class="btn-outline" type="button" (click)="resetIdentityForm()">Clear</button>
          </div>
        </form>

        <table *ngIf="identityAccess.length" class="records-table">
          <thead>
            <tr>
              <th>Email</th>
              <th>Principal</th>
              <th>Roles</th>
              <th>Customer IDs</th>
              <th>Active</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let row of identityAccess">
              <td>{{ row.email }}</td>
              <td>{{ row.principal_type }}</td>
              <td>
                {{ row.role || '-' }} / {{ row.customer_role || '-' }} / {{ row.investor_role || '-' }}
              </td>
              <td>{{ (row.customer_ids || []).join(', ') }}</td>
              <td>{{ row.active === false ? 'false' : 'true' }}</td>
              <td class="table-actions">
                <button class="btn-link" (click)="editIdentityAccess(row)">Edit</button>
                <button class="btn-link danger" (click)="removeIdentityAccess(row.email)">Delete</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p class="empty" *ngIf="!identityAccess.length">No identity access records found.</p>
      </section>
    </div>
  `,
  styles: [`
    .access-admin-container { padding: 24px; }
    .page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 8px; }
    .page-header h1 { margin: 0; color: #1a1a2e; }
    .subtitle { margin: 0 0 20px; color: #666; }
    .status { padding: 10px 12px; border-radius: 8px; margin-bottom: 12px; font-size: 13px; }
    .status.success { background: #e8f8f0; color: #145c32; }
    .status.error { background: #fdecec; color: #8b1d1d; }
    .card {
      background: #fff;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      padding: 20px;
      margin-bottom: 20px;
    }
    .card h2 { margin: 0 0 6px; color: #1a1a2e; }
    .section-help { margin: 0 0 14px; color: #666; font-size: 13px; }
    .form-grid {
      display: grid;
      grid-template-columns: repeat(2, minmax(220px, 1fr));
      gap: 12px;
      margin-bottom: 14px;
    }
    .form-grid label { display: flex; flex-direction: column; gap: 6px; font-size: 13px; color: #333; }
    .form-grid input, .form-grid select {
      border: 1px solid #d6d9e0;
      border-radius: 8px;
      padding: 9px 10px;
      font-size: 14px;
      outline: none;
    }
    .form-grid input:focus, .form-grid select:focus { border-color: #4361ee; }
    .wide { grid-column: 1 / -1; }
    .actions { display: flex; gap: 10px; }
    .btn-primary {
      background: #4361ee;
      color: #fff;
      border: 0;
      border-radius: 8px;
      padding: 10px 14px;
      cursor: pointer;
    }
    .btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }
    .btn-outline {
      background: #fff;
      color: #1a1a2e;
      border: 1px solid #d6d9e0;
      border-radius: 8px;
      padding: 10px 14px;
      cursor: pointer;
    }
    .records-table { width: 100%; border-collapse: collapse; }
    .records-table th, .records-table td {
      text-align: left;
      border-top: 1px solid #eef1f5;
      padding: 10px 8px;
      font-size: 13px;
      vertical-align: top;
    }
    .records-table th { color: #666; font-weight: 600; }
    .table-actions { white-space: nowrap; }
    .btn-link {
      border: 0;
      background: transparent;
      color: #2f56d3;
      cursor: pointer;
      padding: 0;
      margin-right: 10px;
      font-size: 13px;
    }
    .btn-link.danger { color: #b12626; }
    .empty { margin: 8px 0 0; color: #777; font-size: 13px; }
    @media (max-width: 900px) {
      .form-grid { grid-template-columns: 1fr; }
    }
  `],
})
export class AccessAdminComponent implements OnInit {
  loading = false;
  successMessage = '';
  errorMessage = '';

  customerAccess: CustomerAccessRecord[] = [];
  identityAccess: IdentityAccessRecord[] = [];

  customerForm: { email: string; customer_role: string; customer_ids: string } = {
    email: '',
    customer_role: 'customer_viewer',
    customer_ids: '',
  };

  identityForm: {
    email: string;
    principal_type: PrincipalType;
    role: string;
    customer_role: string;
    investor_role: string;
    customer_ids: string;
    active: boolean;
  } = {
    email: '',
    principal_type: 'staff',
    role: '',
    customer_role: '',
    investor_role: '',
    customer_ids: '',
    active: true,
  };

  constructor(private api: CxApiService) {}

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.loading = true;
    this.clearMessages();

    forkJoin({
      customer: this.api.getCustomerAccess().pipe(
        catchError((error) => {
          this.setErrorFromHttp(error);
          return of([]);
        })
      ),
      identity: this.api.getIdentityAccess().pipe(
        catchError((error) => {
          this.setErrorFromHttp(error);
          return of([]);
        })
      ),
    }).subscribe({
      next: ({ customer, identity }) => {
        this.customerAccess = this.normalizeRecords<CustomerAccessRecord>(customer);
        this.identityAccess = this.normalizeRecords<IdentityAccessRecord>(identity);
        this.loading = false;
      },
      error: (error) => this.handleError(error),
    });
  }

  saveCustomerAccess(): void {
    this.clearMessages();
    const payload: CustomerAccessRecord = {
      email: this.customerForm.email.trim(),
      customer_role: this.customerForm.customer_role,
      customer_ids: this.parseCsv(this.customerForm.customer_ids),
    };
    if (!payload.email || payload.customer_ids.length === 0) {
      this.errorMessage = 'Customer access requires email and at least one customer ID.';
      return;
    }
    this.api.upsertCustomerAccess(payload).subscribe({
      next: () => {
        this.successMessage = `Saved customer access for ${payload.email}.`;
        this.resetCustomerForm();
        this.reload();
      },
      error: (error) => this.handleError(error),
    });
  }

  removeCustomerAccess(email: string): void {
    this.clearMessages();
    this.api.deleteCustomerAccess(email).subscribe({
      next: () => {
        this.successMessage = `Deleted customer access for ${email}.`;
        this.reload();
      },
      error: (error) => this.handleError(error),
    });
  }

  editCustomerAccess(record: CustomerAccessRecord): void {
    this.customerForm = {
      email: record.email,
      customer_role: record.customer_role || 'customer_viewer',
      customer_ids: (record.customer_ids || []).join(','),
    };
  }

  saveIdentityAccess(): void {
    this.clearMessages();
    const payload: IdentityAccessRecord = {
      email: this.identityForm.email.trim(),
      principal_type: this.identityForm.principal_type,
      role: this.identityForm.role.trim() || undefined,
      customer_role: this.identityForm.customer_role.trim() || undefined,
      investor_role: this.identityForm.investor_role.trim() || undefined,
      customer_ids: this.parseCsv(this.identityForm.customer_ids),
      active: this.identityForm.active,
    };
    if (!payload.email) {
      this.errorMessage = 'Identity access requires an email.';
      return;
    }
    this.api.upsertIdentityAccess(payload).subscribe({
      next: () => {
        this.successMessage = `Saved identity access for ${payload.email}.`;
        this.resetIdentityForm();
        this.reload();
      },
      error: (error) => this.handleError(error),
    });
  }

  removeIdentityAccess(email: string): void {
    this.clearMessages();
    this.api.deleteIdentityAccess(email).subscribe({
      next: () => {
        this.successMessage = `Deleted identity access for ${email}.`;
        this.reload();
      },
      error: (error) => this.handleError(error),
    });
  }

  isNotAuthenticatedError(): boolean {
    return (this.errorMessage || '').toLowerCase().includes('not authenticated');
  }

  signIn(): void {
    // Use same-origin auth endpoint so any session cookies land on the portal domain.
    window.location.assign('/api/auth/login');
  }

  editIdentityAccess(record: IdentityAccessRecord): void {
    this.identityForm = {
      email: record.email,
      principal_type: record.principal_type || 'staff',
      role: record.role || '',
      customer_role: record.customer_role || '',
      investor_role: record.investor_role || '',
      customer_ids: (record.customer_ids || []).join(','),
      active: record.active !== false,
    };
  }

  resetCustomerForm(): void {
    this.customerForm = {
      email: '',
      customer_role: 'customer_viewer',
      customer_ids: '',
    };
  }

  resetIdentityForm(): void {
    this.identityForm = {
      email: '',
      principal_type: 'staff',
      role: '',
      customer_role: '',
      investor_role: '',
      customer_ids: '',
      active: true,
    };
  }

  private parseCsv(value: string): string[] {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter((item) => !!item);
  }

  private normalizeRecords<T>(response: unknown): T[] {
    if (Array.isArray(response)) return response as T[];
    if (response && typeof response === 'object' && Array.isArray((response as any).items)) {
      return (response as any).items as T[];
    }
    return [];
  }

  private clearMessages(): void {
    this.successMessage = '';
    this.errorMessage = '';
  }

  private handleError(error: any): void {
    this.loading = false;
    this.setErrorFromHttp(error);
  }

  private setErrorFromHttp(error: any): void {
    const detail =
      error?.error?.detail ||
      error?.message ||
      'Access admin request failed. Verify API auth and admin policy configuration.';
    // Preserve the first meaningful error (don’t spam the UI with multiple overwrites).
    if (!this.errorMessage) this.errorMessage = String(detail);
  }
}
