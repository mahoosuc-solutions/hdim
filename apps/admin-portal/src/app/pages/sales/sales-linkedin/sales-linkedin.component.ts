import { Component, OnInit, OnDestroy, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject, takeUntil } from 'rxjs';
import { SalesLinkedInService } from '../../../services/sales-linkedin.service';
import { SalesService } from '../../../services/sales.service';
import {
  LinkedInCampaign,
  LinkedInOutreach,
  LinkedInAnalytics,
  LinkedInCampaignStatus,
  LinkedInConnectionRequest,
  LinkedInInMailRequest,
  Lead,
} from '../../../models/sales.model';

@Component({
  selector: 'app-sales-linkedin',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="linkedin-page">
      <div class="page-header">
        <div class="header-content">
          <h2>LinkedIn Campaigns</h2>
          <p class="subtitle">Manage your LinkedIn outreach and connections</p>
        </div>
        <div class="header-actions">
          <button class="btn btn-secondary" (click)="openConnectionDialog()">
            🔗 Send Connection
          </button>
          <button class="btn btn-secondary" (click)="openInMailDialog()">
            ✉️ Send InMail
          </button>
          <button class="btn btn-primary" (click)="openCampaignDialog()">
            + New Campaign
          </button>
        </div>
      </div>

      <!-- Analytics Summary -->
      <div class="analytics-summary" *ngIf="analytics()">
        <div class="analytics-card">
          <div class="analytics-icon sent">📤</div>
          <div class="analytics-content">
            <span class="analytics-value">{{ analytics()!.totalConnections }}</span>
            <span class="analytics-label">Connections Sent</span>
          </div>
        </div>
        <div class="analytics-card">
          <div class="analytics-icon accepted">✅</div>
          <div class="analytics-content">
            <span class="analytics-value">{{ analytics()!.acceptedConnections }}</span>
            <span class="analytics-label">Accepted</span>
          </div>
        </div>
        <div class="analytics-card">
          <div class="analytics-icon rate">📊</div>
          <div class="analytics-content">
            <span class="analytics-value">{{ (analytics()!.connectionRate * 100) | number:'1.1-1' }}%</span>
            <span class="analytics-label">Accept Rate</span>
          </div>
        </div>
        <div class="analytics-card">
          <div class="analytics-icon inmail">💬</div>
          <div class="analytics-content">
            <span class="analytics-value">{{ analytics()!.totalInMails }}</span>
            <span class="analytics-label">InMails Sent</span>
          </div>
        </div>
        <div class="analytics-card">
          <div class="analytics-icon response">📩</div>
          <div class="analytics-content">
            <span class="analytics-value">{{ (analytics()!.responseRate * 100) | number:'1.1-1' }}%</span>
            <span class="analytics-label">Response Rate</span>
          </div>
        </div>
      </div>

      <!-- Filters -->
      <div class="filters-bar">
        <div class="search-box">
          <input
            type="text"
            [(ngModel)]="searchQuery"
            placeholder="Search campaigns..."
            (input)="onSearch()"
          />
        </div>
        <div class="filter-group">
          <select [(ngModel)]="statusFilter" (change)="applyFilters()">
            <option value="">All Statuses</option>
            <option *ngFor="let status of statuses" [value]="status">
              {{ formatStatus(status) }}
            </option>
          </select>
          <button class="btn btn-secondary" (click)="refreshData()">
            Refresh
          </button>
        </div>
      </div>

      <!-- Loading State -->
      <div class="loading" *ngIf="isLoading()">
        <div class="spinner"></div>
        <span>Loading campaigns...</span>
      </div>

      <!-- Campaigns Table -->
      <div class="table-container" *ngIf="!isLoading()">
        <table class="campaigns-table">
          <thead>
            <tr>
              <th>Campaign</th>
              <th>Status</th>
              <th>Daily Limit</th>
              <th>Sent</th>
              <th>Accepted</th>
              <th>Rate</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let campaign of filteredCampaigns()" (click)="selectCampaign(campaign)">
              <td class="name-cell">
                <div class="campaign-icon">🔗</div>
                <div class="campaign-name">
                  <span class="name">{{ campaign.name }}</span>
                  <span class="description" *ngIf="campaign.description">{{ campaign.description }}</span>
                </div>
              </td>
              <td>
                <span class="status-badge" [class]="campaign.status.toLowerCase()">
                  {{ formatStatus(campaign.status) }}
                </span>
              </td>
              <td>{{ campaign.dailyLimit }}/day</td>
              <td>{{ campaign.totalSent }}</td>
              <td>{{ campaign.totalAccepted }}</td>
              <td>
                <span class="rate-value">{{ (campaign.acceptanceRate * 100) | number:'1.1-1' }}%</span>
              </td>
              <td class="actions-cell" (click)="$event.stopPropagation()">
                <button
                  class="action-btn"
                  [title]="campaign.status === 'ACTIVE' ? 'Pause' : 'Activate'"
                  (click)="toggleCampaignStatus(campaign)"
                  *ngIf="campaign.status !== 'COMPLETED'"
                >
                  {{ campaign.status === 'ACTIVE' ? '⏸️' : '▶️' }}
                </button>
                <button class="action-btn" title="Edit" (click)="editCampaign(campaign)">✏️</button>
                <button class="action-btn delete" title="Delete" (click)="deleteCampaign(campaign)">🗑️</button>
              </td>
            </tr>
            <tr *ngIf="!filteredCampaigns().length">
              <td colspan="7" class="empty-state">
                No campaigns found. Create your first LinkedIn campaign to get started.
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div class="pagination" *ngIf="totalPages > 1">
        <button [disabled]="currentPage === 0" (click)="changePage(currentPage - 1)">Previous</button>
        <span>Page {{ currentPage + 1 }} of {{ totalPages }}</span>
        <button [disabled]="currentPage >= totalPages - 1" (click)="changePage(currentPage + 1)">Next</button>
      </div>

      <!-- Campaign Create/Edit Dialog -->
      <div class="dialog-overlay" *ngIf="showCampaignDialog" (click)="closeCampaignDialog()">
        <div class="dialog" (click)="$event.stopPropagation()">
          <div class="dialog-header">
            <h3>{{ editingCampaign ? 'Edit Campaign' : 'Create New Campaign' }}</h3>
            <button class="close-btn" (click)="closeCampaignDialog()">×</button>
          </div>
          <form (ngSubmit)="saveCampaign()" class="dialog-form">
            <div class="form-group">
              <label>Campaign Name *</label>
              <input type="text" [(ngModel)]="campaignForm.name" name="name" required />
            </div>
            <div class="form-group">
              <label>Description</label>
              <textarea [(ngModel)]="campaignForm.description" name="description" rows="2"></textarea>
            </div>
            <div class="form-group">
              <label>Target Criteria</label>
              <textarea
                [(ngModel)]="campaignForm.targetCriteria"
                name="targetCriteria"
                rows="2"
                placeholder="e.g., Healthcare executives, VP+ level, 500+ employees"
              ></textarea>
            </div>
            <div class="form-group">
              <label>Daily Connection Limit</label>
              <input
                type="number"
                [(ngModel)]="campaignForm.dailyLimit"
                name="dailyLimit"
                min="1"
                max="100"
              />
              <span class="hint">Recommended: 20-50 per day to avoid LinkedIn restrictions</span>
            </div>
            <div class="dialog-actions">
              <button type="button" class="btn btn-secondary" (click)="closeCampaignDialog()">Cancel</button>
              <button type="submit" class="btn btn-primary">{{ editingCampaign ? 'Save Changes' : 'Create Campaign' }}</button>
            </div>
          </form>
        </div>
      </div>

      <!-- Send Connection Dialog -->
      <div class="dialog-overlay" *ngIf="showConnectionDialog" (click)="closeConnectionDialog()">
        <div class="dialog" (click)="$event.stopPropagation()">
          <div class="dialog-header">
            <h3>Send Connection Request</h3>
            <button class="close-btn" (click)="closeConnectionDialog()">×</button>
          </div>
          <form (ngSubmit)="sendConnection()" class="dialog-form">
            <div class="form-group">
              <label>Select Lead</label>
              <select [(ngModel)]="connectionForm.leadId" name="leadId">
                <option value="">-- Select a Lead --</option>
                <option *ngFor="let lead of leads()" [value]="lead.id">
                  {{ lead.firstName }} {{ lead.lastName }} ({{ lead.company }})
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>LinkedIn Profile URL *</label>
              <input
                type="url"
                [(ngModel)]="connectionForm.linkedInProfileUrl"
                name="linkedInProfileUrl"
                placeholder="https://linkedin.com/in/..."
                required
              />
            </div>
            <div class="form-group">
              <label>Connection Message</label>
              <textarea
                [(ngModel)]="connectionForm.message"
                name="message"
                rows="4"
                placeholder="Hi, I'd like to connect..."
                maxlength="300"
              ></textarea>
              <span class="hint">{{ (connectionForm.message?.length || 0) }}/300 characters</span>
            </div>
            <div class="form-group">
              <label>Associate with Campaign (Optional)</label>
              <select [(ngModel)]="connectionForm.campaignId" name="campaignId">
                <option value="">-- No Campaign --</option>
                <option *ngFor="let campaign of campaigns()" [value]="campaign.id">
                  {{ campaign.name }}
                </option>
              </select>
            </div>
            <div class="dialog-actions">
              <button type="button" class="btn btn-secondary" (click)="closeConnectionDialog()">Cancel</button>
              <button type="submit" class="btn btn-primary" [disabled]="!connectionForm.linkedInProfileUrl">
                Send Connection
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Send InMail Dialog -->
      <div class="dialog-overlay" *ngIf="showInMailDialog" (click)="closeInMailDialog()">
        <div class="dialog" (click)="$event.stopPropagation()">
          <div class="dialog-header">
            <h3>Send InMail</h3>
            <button class="close-btn" (click)="closeInMailDialog()">×</button>
          </div>
          <form (ngSubmit)="sendInMail()" class="dialog-form">
            <div class="form-group">
              <label>Select Lead</label>
              <select [(ngModel)]="inMailForm.leadId" name="leadId">
                <option value="">-- Select a Lead --</option>
                <option *ngFor="let lead of leads()" [value]="lead.id">
                  {{ lead.firstName }} {{ lead.lastName }} ({{ lead.company }})
                </option>
              </select>
            </div>
            <div class="form-group">
              <label>LinkedIn Profile URL *</label>
              <input
                type="url"
                [(ngModel)]="inMailForm.linkedInProfileUrl"
                name="linkedInProfileUrl"
                placeholder="https://linkedin.com/in/..."
                required
              />
            </div>
            <div class="form-group">
              <label>Subject *</label>
              <input
                type="text"
                [(ngModel)]="inMailForm.subject"
                name="subject"
                placeholder="Subject line..."
                required
              />
            </div>
            <div class="form-group">
              <label>Message *</label>
              <textarea
                [(ngModel)]="inMailForm.message"
                name="message"
                rows="6"
                placeholder="Your InMail message..."
                required
              ></textarea>
            </div>
            <div class="form-group">
              <label>Associate with Campaign (Optional)</label>
              <select [(ngModel)]="inMailForm.campaignId" name="campaignId">
                <option value="">-- No Campaign --</option>
                <option *ngFor="let campaign of campaigns()" [value]="campaign.id">
                  {{ campaign.name }}
                </option>
              </select>
            </div>
            <div class="dialog-actions">
              <button type="button" class="btn btn-secondary" (click)="closeInMailDialog()">Cancel</button>
              <button
                type="submit"
                class="btn btn-primary"
                [disabled]="!inMailForm.linkedInProfileUrl || !inMailForm.subject || !inMailForm.message"
              >
                Send InMail
              </button>
            </div>
          </form>
        </div>
      </div>

      <!-- Campaign Detail Panel -->
      <div class="detail-panel" *ngIf="selectedCampaign()" (click)="selectedCampaign.set(null)">
        <div class="detail-content" (click)="$event.stopPropagation()">
          <div class="detail-header">
            <div class="campaign-info">
              <div class="campaign-icon large">🔗</div>
              <div>
                <h3>{{ selectedCampaign()!.name }}</h3>
                <p *ngIf="selectedCampaign()!.description">{{ selectedCampaign()!.description }}</p>
              </div>
            </div>
            <button class="close-btn" (click)="selectedCampaign.set(null)">×</button>
          </div>

          <div class="detail-body">
            <div class="detail-section">
              <h4>Status</h4>
              <div class="status-toggle">
                <span class="status-badge large" [class]="selectedCampaign()!.status.toLowerCase()">
                  {{ formatStatus(selectedCampaign()!.status) }}
                </span>
                <button
                  class="btn btn-secondary btn-sm"
                  (click)="toggleCampaignStatus(selectedCampaign()!)"
                  *ngIf="selectedCampaign()!.status !== 'COMPLETED'"
                >
                  {{ selectedCampaign()!.status === 'ACTIVE' ? 'Pause' : 'Activate' }}
                </button>
              </div>
            </div>

            <div class="detail-section">
              <h4>Settings</h4>
              <div class="detail-row">
                <span class="label">Daily Limit:</span>
                <span>{{ selectedCampaign()!.dailyLimit }} connections/day</span>
              </div>
              <div class="detail-row" *ngIf="selectedCampaign()!.targetCriteria">
                <span class="label">Target:</span>
                <span>{{ selectedCampaign()!.targetCriteria }}</span>
              </div>
            </div>

            <div class="detail-section">
              <h4>Performance</h4>
              <div class="performance-grid">
                <div class="performance-item">
                  <span class="performance-value">{{ selectedCampaign()!.totalSent }}</span>
                  <span class="performance-label">Total Sent</span>
                </div>
                <div class="performance-item">
                  <span class="performance-value">{{ selectedCampaign()!.totalAccepted }}</span>
                  <span class="performance-label">Accepted</span>
                </div>
                <div class="performance-item">
                  <span class="performance-value">{{ selectedCampaign()!.totalSent - selectedCampaign()!.totalAccepted }}</span>
                  <span class="performance-label">Pending</span>
                </div>
                <div class="performance-item highlight">
                  <span class="performance-value">{{ (selectedCampaign()!.acceptanceRate * 100) | number:'1.1-1' }}%</span>
                  <span class="performance-label">Accept Rate</span>
                </div>
              </div>
            </div>

            <div class="detail-section">
              <h4>Recent Outreach ({{ outreachList().length }})</h4>
              <div class="outreach-list">
                <div class="outreach-item" *ngFor="let outreach of outreachList()">
                  <div class="outreach-icon" [class]="outreach.type.toLowerCase()">
                    {{ outreach.type === 'CONNECTION' ? '🔗' : '✉️' }}
                  </div>
                  <div class="outreach-content">
                    <span class="outreach-type">{{ formatOutreachType(outreach.type) }}</span>
                    <span class="outreach-date">{{ outreach.sentAt | date:'short' }}</span>
                  </div>
                  <span class="outreach-status" [class]="outreach.status.toLowerCase()">
                    {{ formatOutreachStatus(outreach.status) }}
                  </span>
                </div>
                <div class="empty-outreach" *ngIf="!outreachList().length">
                  No outreach sent yet
                </div>
              </div>
            </div>
          </div>

          <div class="detail-actions">
            <button class="btn btn-secondary" (click)="editCampaign(selectedCampaign()!)">Edit Campaign</button>
            <button class="btn btn-primary" (click)="openConnectionDialogForCampaign(selectedCampaign()!)">
              Send Connection
            </button>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .linkedin-page {
      max-width: 1400px;
      margin: 0 auto;
    }

    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;
    }

    .header-content h2 {
      margin: 0;
      color: #1a237e;
    }

    .subtitle {
      margin: 4px 0 0;
      color: #666;
      font-size: 14px;
    }

    .header-actions {
      display: flex;
      gap: 12px;
    }

    .btn {
      padding: 10px 20px;
      border-radius: 8px;
      font-weight: 500;
      cursor: pointer;
      border: none;
      transition: all 0.2s ease;
    }

    .btn-primary {
      background: #1a237e;
      color: white;
    }

    .btn-primary:hover {
      background: #0d47a1;
    }

    .btn-secondary {
      background: #f5f5f5;
      color: #333;
      border: 1px solid #ddd;
    }

    .btn-sm {
      padding: 6px 12px;
      font-size: 12px;
    }

    /* Analytics Summary */
    .analytics-summary {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }

    .analytics-card {
      background: white;
      border-radius: 12px;
      padding: 20px;
      display: flex;
      align-items: center;
      gap: 16px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .analytics-icon {
      width: 48px;
      height: 48px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 24px;
    }

    .analytics-icon.sent { background: #e3f2fd; }
    .analytics-icon.accepted { background: #e8f5e9; }
    .analytics-icon.rate { background: #f3e5f5; }
    .analytics-icon.inmail { background: #fff3e0; }
    .analytics-icon.response { background: #e0f2f1; }

    .analytics-content {
      display: flex;
      flex-direction: column;
    }

    .analytics-value {
      font-size: 24px;
      font-weight: 700;
      color: #1a237e;
    }

    .analytics-label {
      font-size: 12px;
      color: #666;
    }

    .filters-bar {
      display: flex;
      gap: 16px;
      margin-bottom: 24px;
      flex-wrap: wrap;
    }

    .search-box {
      flex: 1;
      min-width: 250px;
    }

    .search-box input {
      width: 100%;
      padding: 12px 16px;
      border: 1px solid #ddd;
      border-radius: 8px;
      font-size: 14px;
    }

    .filter-group {
      display: flex;
      gap: 12px;
    }

    .filter-group select {
      padding: 12px 16px;
      border: 1px solid #ddd;
      border-radius: 8px;
      background: white;
      font-size: 14px;
      min-width: 150px;
    }

    .table-container {
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      overflow: hidden;
    }

    .campaigns-table {
      width: 100%;
      border-collapse: collapse;
    }

    .campaigns-table th {
      background: #f8f9fa;
      padding: 16px;
      text-align: left;
      font-weight: 600;
      color: #333;
      font-size: 13px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .campaigns-table td {
      padding: 16px;
      border-top: 1px solid #eee;
      font-size: 14px;
    }

    .campaigns-table tbody tr {
      cursor: pointer;
      transition: background 0.2s ease;
    }

    .campaigns-table tbody tr:hover {
      background: #f8f9fa;
    }

    .name-cell {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .campaign-icon {
      width: 40px;
      height: 40px;
      border-radius: 8px;
      background: #e0f2f1;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 20px;
    }

    .campaign-icon.large {
      width: 60px;
      height: 60px;
      font-size: 28px;
    }

    .campaign-name {
      display: flex;
      flex-direction: column;
    }

    .campaign-name .name {
      font-weight: 500;
      color: #333;
    }

    .campaign-name .description {
      font-size: 12px;
      color: #666;
      max-width: 200px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }

    .status-badge {
      padding: 4px 12px;
      border-radius: 12px;
      font-size: 12px;
      font-weight: 500;
    }

    .status-badge.draft { background: #f5f5f5; color: #757575; }
    .status-badge.active { background: #e8f5e9; color: #2e7d32; }
    .status-badge.paused { background: #fff3e0; color: #ef6c00; }
    .status-badge.completed { background: #e3f2fd; color: #1565c0; }
    .status-badge.large { padding: 8px 16px; font-size: 14px; }

    .rate-value {
      font-weight: 600;
      color: #1a237e;
    }

    .actions-cell {
      display: flex;
      gap: 8px;
    }

    .action-btn {
      background: none;
      border: none;
      cursor: pointer;
      padding: 4px;
      font-size: 16px;
      opacity: 0.7;
      transition: opacity 0.2s ease;
    }

    .action-btn:hover {
      opacity: 1;
    }

    .action-btn.delete:hover {
      color: #c62828;
    }

    .empty-state {
      text-align: center;
      padding: 48px !important;
      color: #999;
    }

    .pagination {
      display: flex;
      justify-content: center;
      align-items: center;
      gap: 16px;
      margin-top: 24px;
    }

    .pagination button {
      padding: 8px 16px;
      border: 1px solid #ddd;
      background: white;
      border-radius: 6px;
      cursor: pointer;
    }

    .pagination button:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    /* Dialog Styles */
    .dialog-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }

    .dialog {
      background: white;
      border-radius: 12px;
      width: 90%;
      max-width: 500px;
      max-height: 90vh;
      overflow-y: auto;
    }

    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 20px 24px;
      border-bottom: 1px solid #eee;
    }

    .dialog-header h3 {
      margin: 0;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 24px;
      cursor: pointer;
      color: #666;
    }

    .dialog-form {
      padding: 24px;
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .form-group {
      display: flex;
      flex-direction: column;
      gap: 6px;
    }

    .form-group label {
      font-size: 13px;
      font-weight: 500;
      color: #333;
    }

    .form-group input,
    .form-group select,
    .form-group textarea {
      padding: 10px 12px;
      border: 1px solid #ddd;
      border-radius: 6px;
      font-size: 14px;
    }

    .form-group .hint {
      font-size: 12px;
      color: #999;
    }

    .dialog-actions {
      display: flex;
      justify-content: flex-end;
      gap: 12px;
      margin-top: 8px;
    }

    /* Detail Panel */
    .detail-panel {
      position: fixed;
      top: 0;
      right: 0;
      bottom: 0;
      width: 100%;
      background: rgba(0, 0, 0, 0.3);
      display: flex;
      justify-content: flex-end;
      z-index: 1000;
    }

    .detail-content {
      width: 520px;
      background: white;
      height: 100%;
      overflow-y: auto;
      box-shadow: -4px 0 20px rgba(0, 0, 0, 0.1);
    }

    .detail-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      padding: 24px;
      border-bottom: 1px solid #eee;
    }

    .campaign-info {
      display: flex;
      gap: 16px;
      align-items: center;
    }

    .campaign-info h3 {
      margin: 0;
      color: #333;
    }

    .campaign-info p {
      margin: 4px 0 0;
      color: #666;
      font-size: 14px;
    }

    .detail-body {
      padding: 24px;
    }

    .detail-section {
      margin-bottom: 24px;
    }

    .detail-section h4 {
      margin: 0 0 12px 0;
      color: #1a237e;
      font-size: 14px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .status-toggle {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .detail-row {
      display: flex;
      justify-content: space-between;
      padding: 8px 0;
      border-bottom: 1px solid #f5f5f5;
    }

    .detail-row .label {
      color: #666;
    }

    /* Performance Grid */
    .performance-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;
    }

    .performance-item {
      text-align: center;
      padding: 16px;
      background: #f8f9fa;
      border-radius: 8px;
    }

    .performance-item.highlight {
      background: #e3f2fd;
    }

    .performance-value {
      display: block;
      font-size: 24px;
      font-weight: 700;
      color: #1a237e;
    }

    .performance-label {
      font-size: 12px;
      color: #666;
    }

    /* Outreach List */
    .outreach-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
      max-height: 300px;
      overflow-y: auto;
    }

    .outreach-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      background: #f8f9fa;
      border-radius: 8px;
    }

    .outreach-icon {
      width: 32px;
      height: 32px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 14px;
    }

    .outreach-icon.connection { background: #e0f2f1; }
    .outreach-icon.inmail { background: #fff3e0; }

    .outreach-content {
      flex: 1;
      display: flex;
      flex-direction: column;
    }

    .outreach-type {
      font-weight: 500;
      font-size: 13px;
    }

    .outreach-date {
      font-size: 11px;
      color: #999;
    }

    .outreach-status {
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 11px;
      font-weight: 500;
    }

    .outreach-status.pending { background: #f5f5f5; color: #757575; }
    .outreach-status.sent { background: #e3f2fd; color: #1565c0; }
    .outreach-status.accepted { background: #e8f5e9; color: #2e7d32; }
    .outreach-status.declined { background: #ffebee; color: #c62828; }
    .outreach-status.no_response { background: #fff3e0; color: #ef6c00; }

    .empty-outreach {
      padding: 24px;
      text-align: center;
      color: #999;
    }

    .detail-actions {
      padding: 24px;
      border-top: 1px solid #eee;
      display: flex;
      gap: 12px;
    }

    .loading {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 48px;
      color: #666;
    }

    .spinner {
      width: 40px;
      height: 40px;
      border: 3px solid #e0e0e0;
      border-top-color: #1a237e;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin-bottom: 16px;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    @media (max-width: 768px) {
      .header-actions {
        flex-wrap: wrap;
      }

      .detail-content {
        width: 100%;
      }

      .analytics-summary {
        grid-template-columns: repeat(2, 1fr);
      }
    }
  `],
})
export class SalesLinkedInComponent implements OnInit, OnDestroy {
  private readonly linkedInService = inject(SalesLinkedInService);
  private readonly salesService = inject(SalesService);
  private destroy$ = new Subject<void>();

  // State
  campaigns = signal<LinkedInCampaign[]>([]);
  selectedCampaign = signal<LinkedInCampaign | null>(null);
  outreachList = signal<LinkedInOutreach[]>([]);
  analytics = signal<LinkedInAnalytics | null>(null);
  leads = signal<Lead[]>([]);
  isLoading = this.linkedInService.isLoading;

  // Dialog state
  showCampaignDialog = false;
  showConnectionDialog = false;
  showInMailDialog = false;
  editingCampaign: LinkedInCampaign | null = null;

  // Filters
  searchQuery = '';
  statusFilter = '';

  // Pagination
  currentPage = 0;
  totalPages = 1;
  pageSize = 20;

  // Forms
  campaignForm: Partial<LinkedInCampaign> = this.getEmptyCampaignForm();
  connectionForm: LinkedInConnectionRequest = this.getEmptyConnectionForm();
  inMailForm: LinkedInInMailRequest = this.getEmptyInMailForm();

  statuses: LinkedInCampaignStatus[] = ['DRAFT', 'ACTIVE', 'PAUSED', 'COMPLETED'];

  ngOnInit(): void {
    this.loadCampaigns();
    this.loadAnalytics();
    this.loadLeads();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  loadCampaigns(): void {
    this.linkedInService.getCampaigns({ page: this.currentPage, size: this.pageSize })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => {
          this.campaigns.set(response.content);
          this.totalPages = response.totalPages;
        },
      });
  }

  loadAnalytics(): void {
    this.linkedInService.getAnalytics()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (analytics) => this.analytics.set(analytics),
        error: () => this.analytics.set(null),
      });
  }

  loadLeads(): void {
    this.salesService.getLeads({}, { page: 0, size: 100 })
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => this.leads.set(response.content),
      });
  }

  refreshData(): void {
    this.loadCampaigns();
    this.loadAnalytics();
  }

  filteredCampaigns(): LinkedInCampaign[] {
    let filtered = this.campaigns();

    if (this.searchQuery) {
      const query = this.searchQuery.toLowerCase();
      filtered = filtered.filter(
        (c) => c.name.toLowerCase().includes(query) ||
               c.description?.toLowerCase().includes(query)
      );
    }

    if (this.statusFilter) {
      filtered = filtered.filter((c) => c.status === this.statusFilter);
    }

    return filtered;
  }

  onSearch(): void {
    // Client-side filtering
  }

  applyFilters(): void {
    // Client-side filtering
  }

  changePage(page: number): void {
    this.currentPage = page;
    this.loadCampaigns();
  }

  selectCampaign(campaign: LinkedInCampaign): void {
    this.selectedCampaign.set(campaign);
    this.loadCampaignOutreach(campaign.id);
  }

  loadCampaignOutreach(campaignId: string): void {
    this.linkedInService.getOutreach(campaignId)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (response) => this.outreachList.set(response.content),
        error: () => this.outreachList.set([]),
      });
  }

  // Campaign Dialog
  openCampaignDialog(): void {
    this.editingCampaign = null;
    this.campaignForm = this.getEmptyCampaignForm();
    this.showCampaignDialog = true;
  }

  editCampaign(campaign: LinkedInCampaign): void {
    this.editingCampaign = campaign;
    this.campaignForm = {
      name: campaign.name,
      description: campaign.description,
      targetCriteria: campaign.targetCriteria,
      dailyLimit: campaign.dailyLimit,
    };
    this.showCampaignDialog = true;
  }

  closeCampaignDialog(): void {
    this.showCampaignDialog = false;
    this.editingCampaign = null;
    this.campaignForm = this.getEmptyCampaignForm();
  }

  saveCampaign(): void {
    if (this.editingCampaign) {
      this.linkedInService.updateCampaign(this.editingCampaign.id, this.campaignForm)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.closeCampaignDialog();
            this.loadCampaigns();
            if (this.selectedCampaign()?.id === this.editingCampaign?.id) {
              this.linkedInService.getCampaign(this.editingCampaign!.id)
                .pipe(takeUntil(this.destroy$))
                .subscribe((c) => this.selectedCampaign.set(c));
            }
          },
        });
    } else {
      this.linkedInService.createCampaign(this.campaignForm)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            this.closeCampaignDialog();
            this.loadCampaigns();
          },
        });
    }
  }

  deleteCampaign(campaign: LinkedInCampaign): void {
    if (confirm(`Delete campaign "${campaign.name}"?`)) {
      this.linkedInService.deleteCampaign(campaign.id)
        .pipe(takeUntil(this.destroy$))
        .subscribe({
          next: () => {
            if (this.selectedCampaign()?.id === campaign.id) {
              this.selectedCampaign.set(null);
            }
            this.loadCampaigns();
          },
        });
    }
  }

  toggleCampaignStatus(campaign: LinkedInCampaign): void {
    const action = campaign.status === 'ACTIVE'
      ? this.linkedInService.pauseCampaign(campaign.id)
      : this.linkedInService.activateCampaign(campaign.id);

    action.pipe(takeUntil(this.destroy$)).subscribe({
      next: () => {
        this.loadCampaigns();
        this.loadAnalytics();
      },
    });
  }

  // Connection Dialog
  openConnectionDialog(): void {
    this.connectionForm = this.getEmptyConnectionForm();
    this.showConnectionDialog = true;
  }

  openConnectionDialogForCampaign(campaign: LinkedInCampaign): void {
    this.connectionForm = {
      ...this.getEmptyConnectionForm(),
      campaignId: campaign.id,
    };
    this.showConnectionDialog = true;
  }

  closeConnectionDialog(): void {
    this.showConnectionDialog = false;
    this.connectionForm = this.getEmptyConnectionForm();
  }

  sendConnection(): void {
    this.linkedInService.sendConnectionRequest(this.connectionForm)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.closeConnectionDialog();
          this.loadAnalytics();
          if (this.selectedCampaign() && this.connectionForm.campaignId === this.selectedCampaign()?.id) {
            this.loadCampaignOutreach(this.selectedCampaign()!.id);
          }
        },
      });
  }

  // InMail Dialog
  openInMailDialog(): void {
    this.inMailForm = this.getEmptyInMailForm();
    this.showInMailDialog = true;
  }

  closeInMailDialog(): void {
    this.showInMailDialog = false;
    this.inMailForm = this.getEmptyInMailForm();
  }

  sendInMail(): void {
    this.linkedInService.sendInMail(this.inMailForm)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.closeInMailDialog();
          this.loadAnalytics();
        },
      });
  }

  // Form helpers
  getEmptyCampaignForm(): Partial<LinkedInCampaign> {
    return {
      name: '',
      description: '',
      targetCriteria: '',
      dailyLimit: 25,
    };
  }

  getEmptyConnectionForm(): LinkedInConnectionRequest {
    return {
      leadId: '',
      linkedInProfileUrl: '',
      message: '',
      campaignId: '',
    };
  }

  getEmptyInMailForm(): LinkedInInMailRequest {
    return {
      leadId: '',
      linkedInProfileUrl: '',
      subject: '',
      message: '',
      campaignId: '',
    };
  }

  // Formatters
  formatStatus(status: string): string {
    return status.replace('_', ' ').toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase());
  }

  formatOutreachType(type: string): string {
    const types: Record<string, string> = {
      CONNECTION: 'Connection Request',
      INMAIL: 'InMail Message',
      MESSAGE: 'Direct Message',
    };
    return types[type] || type;
  }

  formatOutreachStatus(status: string): string {
    return status.replace('_', ' ').toLowerCase()
      .replace(/\b\w/g, (l) => l.toUpperCase());
  }
}
