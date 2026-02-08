import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { CxApiService } from '../shared/services/cx-api.service';

interface WizardStep {
  number: number;
  label: string;
  icon: string;
  completed: boolean;
}

interface CampaignGoal {
  id: string;
  name: string;
  description: string;
  icon: string;
  type: 'investor' | 'customer' | 'partner';
  recommendedFormula: string;
}

interface TargetFilter {
  tiers: string[];
  sources: string[];
  orgTypes: string[];
  investorTypes: string[];
  states: string[];
}

@Component({
  selector: 'cx-campaign-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="wizard-container">
      <!-- Progress Steps -->
      <div class="wizard-steps">
        <div
          *ngFor="let step of steps"
          class="step"
          [class.active]="step.number === currentStep"
          [class.completed]="step.completed"
        >
          <div class="step-number">
            <span *ngIf="!step.completed">{{ step.number }}</span>
            <span *ngIf="step.completed">✓</span>
          </div>
          <div class="step-label">{{ step.label }}</div>
        </div>
      </div>

      <!-- Step Content -->
      <div class="wizard-content">
        <!-- Step 1: Campaign Goal -->
        <div *ngIf="currentStep === 1" class="step-content">
          <h2>What's your campaign goal?</h2>
          <p class="step-description">Choose the type of campaign you want to run</p>

          <div class="goals-grid">
            <div
              *ngFor="let goal of campaignGoals"
              class="goal-card"
              [class.selected]="selectedGoal?.id === goal.id"
              (click)="selectGoal(goal)"
            >
              <div class="goal-icon">{{ goal.icon }}</div>
              <h3>{{ goal.name }}</h3>
              <p>{{ goal.description }}</p>
              <span class="goal-type">{{ goal.type }}</span>
            </div>
          </div>

          <div *ngIf="selectedGoal" class="ai-recommendation">
            <strong>💡 AI Recommendation:</strong>
            <p>
              Based on your goal, I recommend the
              <strong>{{ selectedGoal.recommendedFormula }}</strong> formula.
              This includes {{ getFormulaSteps(selectedGoal.recommendedFormula) }} steps
              with built-in approval checkpoints.
            </p>
          </div>
        </div>

        <!-- Step 2: Target Audience -->
        <div *ngIf="currentStep === 2" class="step-content">
          <h2>Who should we target?</h2>
          <p class="step-description">Define your target audience segment</p>

          <div class="filter-section">
            <h3>Tier Selection</h3>
            <div class="checkbox-group">
              <label *ngFor="let tier of ['A', 'B', 'C']">
                <input
                  type="checkbox"
                  [checked]="targetFilters.tiers.includes(tier)"
                  (change)="toggleFilter('tiers', tier)"
                />
                <span>Tier {{ tier }}</span>
                <span class="tier-count">({{ getTierCount(tier) }} prospects)</span>
              </label>
            </div>
          </div>

          <div class="filter-section" *ngIf="selectedGoal?.type === 'investor'">
            <h3>Investor Type</h3>
            <div class="checkbox-group">
              <label *ngFor="let type of ['vc', 'angel', 'strategic', 'family_office']">
                <input
                  type="checkbox"
                  [checked]="targetFilters.investorTypes.includes(type)"
                  (change)="toggleFilter('investorTypes', type)"
                />
                <span>{{ formatInvestorType(type) }}</span>
              </label>
            </div>
          </div>

          <div class="filter-section" *ngIf="selectedGoal?.type === 'customer'">
            <h3>Organization Type</h3>
            <div class="checkbox-group">
              <label *ngFor="let type of ['ACO', 'HIE', 'Payer', 'Health System', 'FQHC']">
                <input
                  type="checkbox"
                  [checked]="targetFilters.orgTypes.includes(type)"
                  (change)="toggleFilter('orgTypes', type)"
                />
                <span>{{ type }}</span>
              </label>
            </div>
          </div>

          <div class="filter-section">
            <h3>Source</h3>
            <div class="checkbox-group">
              <label *ngFor="let source of ['warm_intro', 'cold_outreach', 'inbound', 'referral']">
                <input
                  type="checkbox"
                  [checked]="targetFilters.sources.includes(source)"
                  (change)="toggleFilter('sources', source)"
                />
                <span>{{ formatSource(source) }}</span>
              </label>
            </div>
          </div>

          <div class="target-summary">
            <strong>Target Size:</strong> {{ getTargetCount() }} prospects
            <div class="target-breakdown">
              <div *ngFor="let tier of targetFilters.tiers">
                Tier {{ tier }}: {{ getTierCount(tier) }}
              </div>
            </div>
          </div>
        </div>

        <!-- Step 3: Formula & Content -->
        <div *ngIf="currentStep === 3" class="step-content">
          <h2>Configure campaign workflow</h2>
          <p class="step-description">Select formula and content sources</p>

          <div class="formula-selection">
            <h3>Workflow Formula</h3>
            <select [(ngModel)]="campaignData.formula_id" class="formula-select">
              <option value="investor-outreach">Investor Outreach (5-day sequence)</option>
              <option value="customer-outreach">Customer Outreach (7-day sequence)</option>
              <option value="warm-reconnect">Warm Reconnect (3-day sequence)</option>
            </select>

            <div class="formula-preview">
              <h4>Formula Steps:</h4>
              <ol>
                <li *ngFor="let step of getFormulaStepsList()">{{ step }}</li>
              </ol>
            </div>
          </div>

          <div class="content-sources">
            <h3>Content Sources</h3>
            <p>These content files will be used by AI agents:</p>

            <div class="content-item" *ngFor="let source of getContentSources()">
              <span class="content-icon">📄</span>
              <div>
                <strong>{{ source.label }}</strong>
                <div class="content-path">{{ source.path }}</div>
              </div>
            </div>
          </div>

          <div class="approval-settings">
            <h3>Approval Settings</h3>
            <label>
              <input
                type="checkbox"
                [(ngModel)]="requireApprovalForEmails"
                checked
              />
              Require human approval before sending emails
            </label>
            <label>
              <input
                type="checkbox"
                [(ngModel)]="requireApprovalForLinkedIn"
                checked
              />
              Require human approval for LinkedIn messages
            </label>
            <div class="urgency-select">
              <label>Approval Urgency:</label>
              <select [(ngModel)]="campaignData.approval_urgency">
                <option value="urgent">Urgent (2 hours)</option>
                <option value="normal">Normal (24 hours)</option>
                <option value="low">Low (7 days)</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Step 4: Schedule & Timing -->
        <div *ngIf="currentStep === 4" class="step-content">
          <h2>Schedule your campaign</h2>
          <p class="step-description">Configure timing and business hours</p>

          <div class="schedule-settings">
            <div class="setting-group">
              <h3>Send Window</h3>
              <div class="time-range">
                <label>
                  Start Time:
                  <input
                    type="time"
                    [(ngModel)]="campaignData.send_window_start"
                    value="09:00"
                  />
                </label>
                <label>
                  End Time:
                  <input
                    type="time"
                    [(ngModel)]="campaignData.send_window_end"
                    value="17:00"
                  />
                </label>
              </div>
            </div>

            <div class="setting-group">
              <h3>Send Days</h3>
              <div class="days-grid">
                <label *ngFor="let day of allDays">
                  <input
                    type="checkbox"
                    [checked]="campaignData.send_days?.includes(day)"
                    (change)="toggleDay(day)"
                  />
                  <span>{{ day }}</span>
                </label>
              </div>
            </div>

            <div class="setting-group">
              <h3>Timezone</h3>
              <select [(ngModel)]="campaignData.timezone">
                <option value="America/Los_Angeles">Pacific Time (PT)</option>
                <option value="America/Denver">Mountain Time (MT)</option>
                <option value="America/Chicago">Central Time (CT)</option>
                <option value="America/New_York">Eastern Time (ET)</option>
              </select>
            </div>

            <div class="setting-group">
              <label>
                <input
                  type="checkbox"
                  [(ngModel)]="campaignData.business_hours_only"
                  checked
                />
                Only send during business hours
              </label>
            </div>
          </div>
        </div>

        <!-- Step 5: Review & Launch -->
        <div *ngIf="currentStep === 5" class="step-content">
          <h2>Review and launch</h2>
          <p class="step-description">Verify your campaign configuration</p>

          <div class="review-section">
            <div class="review-item">
              <h3>Campaign Name</h3>
              <input
                type="text"
                [(ngModel)]="campaignData.name"
                placeholder="Q1 2026 Tier A Investor Outreach"
                class="campaign-name-input"
              />
            </div>

            <div class="review-item">
              <h3>Description</h3>
              <textarea
                [(ngModel)]="campaignData.description"
                placeholder="Brief description of campaign goals and approach..."
                rows="3"
                class="campaign-description-input"
              ></textarea>
            </div>

            <div class="review-summary">
              <div class="summary-card">
                <h4>Goal</h4>
                <p>{{ selectedGoal?.name }}</p>
              </div>

              <div class="summary-card">
                <h4>Target Audience</h4>
                <p>{{ getTargetCount() }} prospects</p>
                <ul>
                  <li *ngFor="let tier of targetFilters.tiers">
                    Tier {{ tier }}: {{ getTierCount(tier) }}
                  </li>
                </ul>
              </div>

              <div class="summary-card">
                <h4>Formula</h4>
                <p>{{ campaignData.formula_id }}</p>
                <p class="text-sm">{{ getFormulaSteps(campaignData.formula_id) }} steps</p>
              </div>

              <div class="summary-card">
                <h4>Schedule</h4>
                <p>{{ campaignData.send_window_start }} - {{ campaignData.send_window_end }}</p>
                <p class="text-sm">{{ campaignData.send_days?.join(', ') }}</p>
              </div>
            </div>

            <div class="launch-options">
              <label>
                <input type="radio" name="launch" value="draft" [(ngModel)]="launchMode" checked />
                <strong>Save as Draft</strong> - Review and launch later
              </label>
              <label>
                <input type="radio" name="launch" value="start" [(ngModel)]="launchMode" />
                <strong>Launch Immediately</strong> - Start campaign now
              </label>
            </div>
          </div>
        </div>
      </div>

      <!-- Navigation Buttons -->
      <div class="wizard-nav">
        <button
          *ngIf="currentStep > 1"
          (click)="previousStep()"
          class="btn btn-secondary"
        >
          ← Back
        </button>

        <button
          *ngIf="currentStep < 5"
          (click)="nextStep()"
          [disabled]="!canProceed()"
          class="btn btn-primary"
        >
          Next →
        </button>

        <button
          *ngIf="currentStep === 5"
          (click)="createCampaign()"
          [disabled]="creating || !canProceed()"
          class="btn btn-success"
        >
          <span *ngIf="!creating">{{ launchMode === 'start' ? '🚀 Launch Campaign' : '💾 Save Draft' }}</span>
          <span *ngIf="creating">Creating...</span>
        </button>
      </div>
    </div>
  `,
  styles: [`
    .wizard-container {
      max-width: 900px;
      margin: 0 auto;
      padding: 2rem;
    }

    .wizard-steps {
      display: flex;
      justify-content: space-between;
      margin-bottom: 3rem;
      position: relative;
    }

    .wizard-steps::before {
      content: '';
      position: absolute;
      top: 20px;
      left: 40px;
      right: 40px;
      height: 2px;
      background: #ddd;
      z-index: 0;
    }

    .step {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.5rem;
      position: relative;
      z-index: 1;
    }

    .step-number {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      background: #f5f5f5;
      border: 2px solid #ddd;
      display: flex;
      align-items: center;
      justify-content: center;
      font-weight: bold;
      transition: all 0.3s;
    }

    .step.active .step-number {
      background: #007bff;
      border-color: #007bff;
      color: white;
    }

    .step.completed .step-number {
      background: #28a745;
      border-color: #28a745;
      color: white;
    }

    .step-label {
      font-size: 0.875rem;
      color: #666;
      text-align: center;
    }

    .step.active .step-label {
      color: #007bff;
      font-weight: 600;
    }

    .wizard-content {
      min-height: 400px;
      background: white;
      border-radius: 8px;
      padding: 2rem;
      box-shadow: 0 2px 4px rgba(0,0,0,0.1);
    }

    .step-content h2 {
      margin: 0 0 0.5rem 0;
      color: #333;
    }

    .step-description {
      color: #666;
      margin-bottom: 2rem;
    }

    .goals-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
      margin-bottom: 2rem;
    }

    .goal-card {
      border: 2px solid #e0e0e0;
      border-radius: 8px;
      padding: 1.5rem;
      cursor: pointer;
      transition: all 0.3s;
      text-align: center;
    }

    .goal-card:hover {
      border-color: #007bff;
      transform: translateY(-2px);
      box-shadow: 0 4px 8px rgba(0,0,0,0.1);
    }

    .goal-card.selected {
      border-color: #007bff;
      background: #f0f7ff;
    }

    .goal-icon {
      font-size: 3rem;
      margin-bottom: 1rem;
    }

    .goal-card h3 {
      margin: 0.5rem 0;
      font-size: 1.125rem;
    }

    .goal-card p {
      font-size: 0.875rem;
      color: #666;
      margin-bottom: 0.5rem;
    }

    .goal-type {
      display: inline-block;
      padding: 0.25rem 0.75rem;
      background: #007bff;
      color: white;
      border-radius: 12px;
      font-size: 0.75rem;
      text-transform: uppercase;
    }

    .ai-recommendation {
      background: #f0f7ff;
      border-left: 4px solid #007bff;
      padding: 1rem;
      border-radius: 4px;
    }

    .filter-section {
      margin-bottom: 2rem;
    }

    .filter-section h3 {
      margin: 0 0 1rem 0;
      font-size: 1rem;
      color: #333;
    }

    .checkbox-group {
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
    }

    .checkbox-group label {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      cursor: pointer;
    }

    .checkbox-group input[type="checkbox"] {
      width: 18px;
      height: 18px;
      cursor: pointer;
    }

    .tier-count {
      color: #666;
      font-size: 0.875rem;
    }

    .target-summary {
      background: #f8f9fa;
      padding: 1rem;
      border-radius: 4px;
      margin-top: 2rem;
    }

    .target-breakdown {
      margin-top: 0.5rem;
      font-size: 0.875rem;
      color: #666;
    }

    .formula-select {
      width: 100%;
      padding: 0.5rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
    }

    .formula-preview {
      margin-top: 1rem;
      background: #f8f9fa;
      padding: 1rem;
      border-radius: 4px;
    }

    .formula-preview ol {
      margin: 0.5rem 0 0 0;
      padding-left: 1.5rem;
    }

    .formula-preview li {
      margin-bottom: 0.25rem;
    }

    .content-sources {
      margin-top: 2rem;
    }

    .content-item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.75rem;
      background: #f8f9fa;
      border-radius: 4px;
      margin-bottom: 0.5rem;
    }

    .content-icon {
      font-size: 1.5rem;
    }

    .content-path {
      font-size: 0.75rem;
      color: #666;
      font-family: monospace;
    }

    .approval-settings {
      margin-top: 2rem;
      padding: 1rem;
      background: #fff8e1;
      border-radius: 4px;
    }

    .approval-settings label {
      display: block;
      margin-bottom: 0.75rem;
    }

    .urgency-select {
      margin-top: 1rem;
      display: flex;
      align-items: center;
      gap: 1rem;
    }

    .urgency-select select {
      flex: 1;
      padding: 0.5rem;
      border: 1px solid #ddd;
      border-radius: 4px;
    }

    .schedule-settings {
      display: flex;
      flex-direction: column;
      gap: 2rem;
    }

    .setting-group h3 {
      margin: 0 0 1rem 0;
      font-size: 1rem;
    }

    .time-range {
      display: flex;
      gap: 1rem;
    }

    .time-range label {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
    }

    .time-range input {
      padding: 0.5rem;
      border: 1px solid #ddd;
      border-radius: 4px;
    }

    .days-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
      gap: 0.75rem;
    }

    .days-grid label {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      cursor: pointer;
    }

    .setting-group select {
      width: 100%;
      padding: 0.5rem;
      border: 1px solid #ddd;
      border-radius: 4px;
    }

    .review-section {
      display: flex;
      flex-direction: column;
      gap: 2rem;
    }

    .campaign-name-input,
    .campaign-description-input {
      width: 100%;
      padding: 0.75rem;
      border: 1px solid #ddd;
      border-radius: 4px;
      font-size: 1rem;
    }

    .review-summary {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
    }

    .summary-card {
      background: #f8f9fa;
      padding: 1rem;
      border-radius: 4px;
    }

    .summary-card h4 {
      margin: 0 0 0.5rem 0;
      font-size: 0.875rem;
      color: #666;
      text-transform: uppercase;
    }

    .summary-card p {
      margin: 0.25rem 0;
    }

    .summary-card ul {
      margin: 0.5rem 0 0 0;
      padding-left: 1.5rem;
    }

    .summary-card .text-sm {
      font-size: 0.875rem;
      color: #666;
    }

    .launch-options {
      display: flex;
      flex-direction: column;
      gap: 1rem;
      padding: 1rem;
      background: #f0f7ff;
      border-radius: 4px;
    }

    .launch-options label {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      cursor: pointer;
      padding: 0.75rem;
      border-radius: 4px;
      transition: background 0.2s;
    }

    .launch-options label:hover {
      background: rgba(0,123,255,0.1);
    }

    .launch-options input[type="radio"] {
      width: 18px;
      height: 18px;
    }

    .wizard-nav {
      display: flex;
      justify-content: space-between;
      margin-top: 2rem;
    }

    .btn {
      padding: 0.75rem 2rem;
      border: none;
      border-radius: 4px;
      font-size: 1rem;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s;
    }

    .btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .btn-primary {
      background: #007bff;
      color: white;
    }

    .btn-primary:hover:not(:disabled) {
      background: #0056b3;
    }

    .btn-secondary {
      background: #6c757d;
      color: white;
    }

    .btn-secondary:hover {
      background: #545b62;
    }

    .btn-success {
      background: #28a745;
      color: white;
    }

    .btn-success:hover:not(:disabled) {
      background: #218838;
    }
  `]
})
export class CampaignWizardComponent implements OnInit {
  currentStep = 1;
  creating = false;
  launchMode: 'draft' | 'start' = 'draft';

  steps: WizardStep[] = [
    { number: 1, label: 'Goal', icon: '🎯', completed: false },
    { number: 2, label: 'Audience', icon: '👥', completed: false },
    { number: 3, label: 'Workflow', icon: '⚙️', completed: false },
    { number: 4, label: 'Schedule', icon: '📅', completed: false },
    { number: 5, label: 'Review', icon: '✅', completed: false },
  ];

  campaignGoals: CampaignGoal[] = [
    {
      id: 'investor-seed',
      name: 'Raise Seed Round',
      description: 'Reach out to VCs and angels for seed funding',
      icon: '💰',
      type: 'investor',
      recommendedFormula: 'investor-outreach'
    },
    {
      id: 'customer-acquisition',
      name: 'Acquire Customers',
      description: 'Reach quality leaders at healthcare organizations',
      icon: '🏥',
      type: 'customer',
      recommendedFormula: 'customer-outreach'
    },
    {
      id: 'network-reconnect',
      name: 'Reconnect Network',
      description: 'Re-engage warm contacts and past relationships',
      icon: '🤝',
      type: 'partner',
      recommendedFormula: 'warm-reconnect'
    }
  ];

  selectedGoal: CampaignGoal | null = null;

  targetFilters: TargetFilter = {
    tiers: [],
    sources: [],
    orgTypes: [],
    investorTypes: [],
    states: []
  };

  requireApprovalForEmails = true;
  requireApprovalForLinkedIn = true;

  allDays = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];

  campaignData: any = {
    name: '',
    description: '',
    campaign_type: 'investor',
    formula_id: 'investor-outreach',
    priority: 'high',
    approval_urgency: 'normal',
    business_hours_only: true,
    send_window_start: '09:00',
    send_window_end: '17:00',
    send_days: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri'],
    timezone: 'America/Los_Angeles',
    sync_to_hdim: true,
    created_by: 'user@example.com',
    target_segment: {},
    content_sources: {},
    approval_required_steps: []
  };

  constructor(
    private cxApi: CxApiService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Initialize wizard
  }

  selectGoal(goal: CampaignGoal): void {
    this.selectedGoal = goal;
    this.campaignData.campaign_type = goal.type;
    this.campaignData.formula_id = goal.recommendedFormula;
  }

  toggleFilter(filterType: keyof TargetFilter, value: string): void {
    const filter = this.targetFilters[filterType] as string[];
    const index = filter.indexOf(value);
    if (index > -1) {
      filter.splice(index, 1);
    } else {
      filter.push(value);
    }
  }

  toggleDay(day: string): void {
    if (!this.campaignData.send_days) {
      this.campaignData.send_days = [];
    }
    const index = this.campaignData.send_days.indexOf(day);
    if (index > -1) {
      this.campaignData.send_days.splice(index, 1);
    } else {
      this.campaignData.send_days.push(day);
    }
  }

  getTierCount(tier: string): number {
    // TODO: Get actual count from API
    return tier === 'A' ? 12 : tier === 'B' ? 25 : 18;
  }

  getTargetCount(): number {
    // TODO: Calculate from filters
    return this.targetFilters.tiers.reduce((sum, tier) => sum + this.getTierCount(tier), 0);
  }

  formatInvestorType(type: string): string {
    const labels: Record<string, string> = {
      'vc': 'Venture Capital',
      'angel': 'Angel Investor',
      'strategic': 'Strategic Investor',
      'family_office': 'Family Office'
    };
    return labels[type] || type;
  }

  formatSource(source: string): string {
    return source.split('_').map(w => w.charAt(0).toUpperCase() + w.slice(1)).join(' ');
  }

  getFormulaSteps(formulaId: string): string {
    const steps: Record<string, string> = {
      'investor-outreach': '7',
      'customer-outreach': '9',
      'warm-reconnect': '5'
    };
    return steps[formulaId] || '5';
  }

  getFormulaStepsList(): string[] {
    if (this.campaignData.formula_id === 'investor-outreach') {
      return [
        'Research investor background and portfolio',
        'Personalize pitch deck for investor focus',
        'Draft intro email with warm hook',
        'Send intro email (requires approval)',
        'LinkedIn connection request',
        'Follow-up email (day 3)',
        'Meeting ask (day 5)'
      ];
    }
    return [];
  }

  getContentSources(): Array<{label: string, path: string}> {
    if (this.campaignData.campaign_type === 'investor') {
      return [
        { label: 'Investor Target List', path: 'investor/outreach/investor-target-list.md' },
        { label: 'Email Templates', path: 'investor/outreach/outreach-templates.md' },
        { label: 'Pitch Deck', path: 'investor/content/pitch-deck/saas-technical-pitch-deck.md' },
        { label: 'Battle Cards', path: 'investor/content/battle-cards/saas-battle-cards.md' },
        { label: 'One-Pager', path: 'investor/content/one-pagers/executive-one-pager-saas.md' }
      ];
    }
    return [];
  }

  canProceed(): boolean {
    switch (this.currentStep) {
      case 1:
        return !!this.selectedGoal;
      case 2:
        return this.targetFilters.tiers.length > 0;
      case 3:
        return !!this.campaignData.formula_id;
      case 4:
        return this.campaignData.send_days?.length > 0;
      case 5:
        return !!this.campaignData.name;
      default:
        return false;
    }
  }

  nextStep(): void {
    if (this.canProceed()) {
      this.steps[this.currentStep - 1].completed = true;
      this.currentStep++;
    }
  }

  previousStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
    }
  }

  async createCampaign(): Promise<void> {
    this.creating = true;

    try {
      // Prepare campaign data
      const campaign = {
        ...this.campaignData,
        target_segment: this.targetFilters,
        content_sources: this.getContentSources().reduce((acc, src) => {
          acc[src.label.toLowerCase().replace(/\s+/g, '_')] = src.path;
          return acc;
        }, {} as Record<string, string>),
        approval_required_steps: this.requireApprovalForEmails ? ['draft-intro-email', 'send-intro'] : [],
        target_count: this.getTargetCount()
      };

      // Create campaign via API
      const created = await this.cxApi.createCampaign(campaign).toPromise();

      // If launch immediately, start the campaign
      if (this.launchMode === 'start' && created) {
        await this.cxApi.startCampaign(created.id).toPromise();
      }

      // Navigate to campaign detail page
      if (created) {
        this.router.navigate(['/campaigns', created.id]);
      }
    } catch (error) {
      console.error('Failed to create campaign:', error);
      alert('Failed to create campaign. Please try again.');
    } finally {
      this.creating = false;
    }
  }
}
