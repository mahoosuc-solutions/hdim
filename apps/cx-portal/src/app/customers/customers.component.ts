import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CxApiService } from '../shared/services/cx-api.service';
import { Customer } from '../shared/models';

/**
 * Customers Component
 *
 * Customer list with health scores and deployment status.
 */
@Component({
  selector: 'cx-customers',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="customers-container">
      <header class="page-header">
        <h1>Customer Success</h1>
        <button class="btn-primary" (click)="addCustomer()">+ Add Customer</button>
      </header>

      <div class="stats-bar" *ngIf="stats">
        <div class="stat">
          <span class="stat-value">{{ stats.total }}</span>
          <span class="stat-label">Total Customers</span>
        </div>
        <div class="stat">
          <span class="stat-value success">{{ stats.active }}</span>
          <span class="stat-label">Active</span>
        </div>
        <div class="stat">
          <span class="stat-value">{{ stats.onboarding }}</span>
          <span class="stat-label">Onboarding</span>
        </div>
        <div class="stat">
          <span class="stat-value money">\${{ stats.total_contract_value | number:'1.0-0' }}</span>
          <span class="stat-label">Total Contract Value</span>
        </div>
        <div class="stat">
          <span class="stat-value">{{ stats.total_patients | number:'1.0-0' }}</span>
          <span class="stat-label">Total Patients</span>
        </div>
      </div>

      <div class="customers-grid" *ngIf="customers.length">
        <div class="customer-card" *ngFor="let customer of customers">
          <div class="customer-header">
            <div class="customer-info">
              <h3>{{ customer.name }}</h3>
              <span class="customer-type">{{ customer.organization_type }}</span>
            </div>
            <span class="status-badge status-{{ customer.status }}">
              {{ customer.status }}
            </span>
          </div>

          <div class="customer-details">
            <div class="detail-row">
              <span class="detail-label">Primary Contact</span>
              <span class="detail-value">{{ customer.primary_contact_name || 'N/A' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">Patients</span>
              <span class="detail-value">{{ customer.patient_count | number:'1.0-0' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">Contract Value</span>
              <span class="detail-value">\${{ customer.contract_value | number:'1.0-0' }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">Integration</span>
              <span class="detail-value">{{ customer.integration_method | uppercase }}</span>
            </div>
          </div>

          <div class="health-score" *ngIf="customer.health_score !== null">
            <div class="health-bar">
              <div
                class="health-fill"
                [style.width.%]="customer.health_score"
                [ngClass]="getHealthClass(customer.health_score)"
              ></div>
            </div>
            <span class="health-value">{{ customer.health_score }}% Health</span>
          </div>

          <div class="customer-footer">
            <button class="btn-outline" (click)="viewCustomer(customer)">View Details</button>
          </div>
        </div>
      </div>

      <div class="empty-state" *ngIf="!customers.length && !error">
        No customers found
      </div>

      <div class="loading" *ngIf="loading">
        Loading customers...
      </div>

      <div class="error" *ngIf="error">
        {{ error }}
      </div>
    </div>
  `,
  styles: [`
    .customers-container {
      padding: 24px;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }

    .page-header h1 {
      margin: 0;
      color: #1a1a2e;
    }

    .btn-primary {
      background: #4361ee;
      color: white;
      border: none;
      padding: 10px 20px;
      border-radius: 8px;
      cursor: pointer;
      font-size: 14px;
    }

    .stats-bar {
      display: flex;
      gap: 32px;
      margin-bottom: 24px;
      padding: 20px 24px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .stat {
      display: flex;
      flex-direction: column;
    }

    .stat-value {
      font-size: 24px;
      font-weight: 600;
      color: #1a1a2e;
    }

    .stat-value.success {
      color: #2ecc71;
    }

    .stat-value.money {
      color: #4361ee;
    }

    .stat-label {
      font-size: 12px;
      color: #666;
    }

    .customers-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
      gap: 24px;
    }

    .customer-card {
      background: white;
      border-radius: 12px;
      padding: 20px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .customer-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 16px;
    }

    .customer-info h3 {
      margin: 0 0 4px 0;
      color: #1a1a2e;
      font-size: 18px;
    }

    .customer-type {
      font-size: 12px;
      color: #666;
      text-transform: uppercase;
    }

    .status-badge {
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }

    .status-active {
      background: #d4edda;
      color: #155724;
    }

    .status-onboarding {
      background: #fff3cd;
      color: #856404;
    }

    .status-prospect {
      background: #cce5ff;
      color: #004085;
    }

    .status-churned {
      background: #f8d7da;
      color: #721c24;
    }

    .customer-details {
      margin-bottom: 16px;
    }

    .detail-row {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid #f0f0f0;
    }

    .detail-row:last-child {
      border-bottom: none;
    }

    .detail-label {
      font-size: 13px;
      color: #666;
    }

    .detail-value {
      font-size: 13px;
      color: #1a1a2e;
      font-weight: 500;
    }

    .health-score {
      margin-bottom: 16px;
    }

    .health-bar {
      height: 8px;
      background: #e0e0e0;
      border-radius: 4px;
      overflow: hidden;
      margin-bottom: 4px;
    }

    .health-fill {
      height: 100%;
      border-radius: 4px;
      transition: width 0.3s;
    }

    .health-fill.healthy {
      background: #2ecc71;
    }

    .health-fill.at-risk {
      background: #f39c12;
    }

    .health-fill.critical {
      background: #e74c3c;
    }

    .health-value {
      font-size: 12px;
      color: #666;
    }

    .customer-footer {
      padding-top: 16px;
      border-top: 1px solid #f0f0f0;
    }

    .btn-outline {
      background: transparent;
      color: #4361ee;
      border: 1px solid #4361ee;
      padding: 8px 16px;
      border-radius: 6px;
      cursor: pointer;
      font-size: 13px;
      width: 100%;
    }

    .btn-outline:hover {
      background: #f0f4ff;
    }

    .empty-state {
      text-align: center;
      padding: 48px;
      color: #999;
    }

    .loading, .error {
      text-align: center;
      padding: 48px;
      color: #666;
    }

    .error {
      color: #dc3545;
    }
  `],
})
export class CustomersComponent implements OnInit {
  customers: Customer[] = [];
  stats: any = null;
  loading = false;
  error: string | null = null;

  constructor(private cxApi: CxApiService) {}

  ngOnInit(): void {
    this.loadCustomers();
    this.loadStats();
  }

  loadCustomers(): void {
    this.loading = true;
    this.cxApi.getCustomers({ limit: 50 }).subscribe({
      next: (result) => {
        this.customers = result.items;
        this.loading = false;
        this.error = null;
      },
      error: (err) => {
        this.error = 'Failed to load customers. Is the CX API running?';
        this.loading = false;
        console.error('Customers error:', err);
      },
    });
  }

  loadStats(): void {
    this.cxApi.getCustomerStats().subscribe({
      next: (stats) => {
        this.stats = stats;
      },
      error: (err) => {
        console.error('Failed to load customer stats:', err);
      },
    });
  }

  getHealthClass(score: number): string {
    if (score >= 80) return 'healthy';
    if (score >= 50) return 'at-risk';
    return 'critical';
  }

  addCustomer(): void {
    // TODO: Open add customer modal
    console.log('Add customer clicked');
  }

  viewCustomer(customer: Customer): void {
    // TODO: Navigate to customer details
    console.log('View customer:', customer.id);
  }
}
