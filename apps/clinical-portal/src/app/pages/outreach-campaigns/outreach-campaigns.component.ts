import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatRadioModule } from '@angular/material/radio';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatTableModule } from '@angular/material/table';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';

interface InterventionType {
  id: string;
  name: string;
  description: string;
  icon: string;
  unitCost: number;
  successRate: number;
  enabled: boolean;
  count: number;
}

interface Campaign {
  id: string;
  name: string;
  measureCode: string;
  measureName: string;
  targetPatients: number;
  status: 'Draft' | 'Scheduled' | 'In Progress' | 'Completed';
  createdDate: string;
  scheduledDate?: string;
  completedDate?: string;
  interventions: {
    type: string;
    count: number;
    cost: number;
  }[];
  projectedCloseRate: number;
  actualCloseRate?: number;
  totalCost: number;
}

@Component({
  selector: 'app-outreach-campaigns',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatInputModule,
    MatFormFieldModule,
    MatSelectModule,
    MatCheckboxModule,
    MatRadioModule,
    MatProgressBarModule,
    MatDividerModule,
    MatChipsModule,
    MatTableModule,
    MatDialogModule,
    MatSnackBarModule,
  ],
  templateUrl: './outreach-campaigns.component.html',
  styleUrl: './outreach-campaigns.component.scss',
})
export class OutreachCampaignsComponent implements OnInit {
  // Campaign creation state
  isCreatingCampaign = signal(false);
  campaignName = signal('');
  selectedMeasure = signal<string | null>(null);
  targetPatientCount = signal(247);
  exportFormat = signal<'csv' | 'hl7' | 'pdf'>('hl7');

  // Intervention types
  interventions = signal<InterventionType[]>([
    {
      id: 'letter',
      name: 'Member Outreach Letter',
      description: 'Personalized letter with care gap information and nearby facility locations',
      icon: 'mail',
      unitCost: 12,
      successRate: 32,
      enabled: true,
      count: 247,
    },
    {
      id: 'provider',
      name: 'Provider Alert',
      description: 'Alert to primary care provider for next patient visit',
      icon: 'local_hospital',
      unitCost: 0,
      successRate: 48,
      enabled: true,
      count: 189,
    },
    {
      id: 'call',
      name: 'Care Coordinator Call',
      description: 'Personal call from care coordinator to schedule appointment',
      icon: 'phone',
      unitCost: 45,
      successRate: 67,
      enabled: true,
      count: 62,
    },
    {
      id: 'sms',
      name: 'SMS Reminder',
      description: 'Text message reminder with appointment scheduling link',
      icon: 'sms',
      unitCost: 2,
      successRate: 28,
      enabled: false,
      count: 0,
    },
    {
      id: 'portal',
      name: 'Patient Portal Message',
      description: 'Secure message through patient portal',
      icon: 'computer',
      unitCost: 1,
      successRate: 22,
      enabled: false,
      count: 0,
    },
  ]);

  // Computed values
  totalCost = computed(() => {
    return this.interventions().reduce((sum, i) => {
      return sum + (i.enabled ? i.unitCost * i.count : 0);
    }, 0);
  });

  estimatedCloseRate = computed(() => {
    const enabledInterventions = this.interventions().filter(i => i.enabled);
    if (enabledInterventions.length === 0) return 0;

    // Weighted average based on counts
    const totalCount = enabledInterventions.reduce((sum, i) => sum + i.count, 0);
    const weightedSum = enabledInterventions.reduce((sum, i) => sum + (i.successRate * i.count), 0);

    return Math.round(weightedSum / totalCount);
  });

  estimatedGapsClosed = computed(() => {
    return Math.round(this.targetPatientCount() * (this.estimatedCloseRate() / 100));
  });

  estimatedHedisImprovement = computed(() => {
    // Assuming 873 eligible patients (denominator)
    const denominator = 873;
    const gapsClosed = this.estimatedGapsClosed();
    return Math.round((gapsClosed / denominator) * 1000) / 10; // One decimal place
  });

  estimatedQualityBonus = computed(() => {
    // $500 per care gap closed (simplified ROI model)
    return this.estimatedGapsClosed() * 500;
  });

  roi = computed(() => {
    const cost = this.totalCost();
    if (cost === 0) return 0;
    return Math.round(this.estimatedQualityBonus() / cost);
  });

  // Computed for template (avoid arrow functions in templates)
  totalPatientsTargeted = computed(() => {
    return this.campaigns().reduce((sum, c) => sum + c.targetPatients, 0);
  });

  completedCampaignsCount = computed(() => {
    return this.campaigns().filter(c => c.status === 'Completed').length;
  });

  // Existing campaigns
  campaigns = signal<Campaign[]>([
    {
      id: '1',
      name: 'BCS Q4 2025 Outreach',
      measureCode: 'BCS',
      measureName: 'Breast Cancer Screening',
      targetPatients: 312,
      status: 'Completed',
      createdDate: '2025-10-01',
      completedDate: '2025-12-15',
      interventions: [
        { type: 'Member Letters', count: 312, cost: 3744 },
        { type: 'Provider Alerts', count: 245, cost: 0 },
        { type: 'Care Coordinator Calls', count: 89, cost: 4005 },
      ],
      projectedCloseRate: 45,
      actualCloseRate: 52,
      totalCost: 7749,
    },
    {
      id: '2',
      name: 'COL Q1 2026 Campaign',
      measureCode: 'COL',
      measureName: 'Colorectal Cancer Screening',
      targetPatients: 198,
      status: 'In Progress',
      createdDate: '2026-01-02',
      interventions: [
        { type: 'Member Letters', count: 198, cost: 2376 },
        { type: 'Provider Alerts', count: 156, cost: 0 },
      ],
      projectedCloseRate: 38,
      totalCost: 2376,
    },
  ]);

  displayedColumns = ['name', 'measure', 'targetPatients', 'status', 'closeRate', 'roi', 'actions'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private dialog: MatDialog,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    // Check for query params from Quality Measures page
    this.route.queryParams.subscribe(params => {
      if (params['measureCode']) {
        this.isCreatingCampaign.set(true);
        this.selectedMeasure.set(params['measureCode']);
        this.campaignName.set(`${params['measureCode']} Q1 2026 Outreach`);

        if (params['careGapsCount']) {
          this.targetPatientCount.set(parseInt(params['careGapsCount'], 10));
          this.updateInterventionCounts(parseInt(params['careGapsCount'], 10));
        }
      }
    });
  }

  private updateInterventionCounts(total: number): void {
    const updated = this.interventions().map(i => {
      if (i.id === 'letter') return { ...i, count: total };
      if (i.id === 'provider') return { ...i, count: Math.floor(total * 0.77) };
      if (i.id === 'call') return { ...i, count: Math.floor(total * 0.25) };
      return i;
    });
    this.interventions.set(updated);
  }

  toggleIntervention(id: string): void {
    const updated = this.interventions().map(i => {
      if (i.id === id) {
        return { ...i, enabled: !i.enabled };
      }
      return i;
    });
    this.interventions.set(updated);
  }

  updateInterventionCount(id: string, count: number): void {
    const updated = this.interventions().map(i => {
      if (i.id === id) {
        return { ...i, count: Math.max(0, Math.min(count, this.targetPatientCount())) };
      }
      return i;
    });
    this.interventions.set(updated);
  }

  startNewCampaign(): void {
    this.isCreatingCampaign.set(true);
    this.campaignName.set('');
    this.selectedMeasure.set(null);
  }

  cancelCampaign(): void {
    this.isCreatingCampaign.set(false);
    this.router.navigate(['/outreach-campaigns']);
  }

  generateCampaign(): void {
    if (!this.campaignName() || !this.selectedMeasure()) {
      this.snackBar.open('Please provide campaign name and select a measure', 'Close', { duration: 3000 });
      return;
    }

    // Create the campaign
    const newCampaign: Campaign = {
      id: Date.now().toString(),
      name: this.campaignName(),
      measureCode: this.selectedMeasure()!,
      measureName: this.getMeasureName(this.selectedMeasure()!),
      targetPatients: this.targetPatientCount(),
      status: 'Draft',
      createdDate: new Date().toISOString().split('T')[0],
      interventions: this.interventions()
        .filter(i => i.enabled)
        .map(i => ({
          type: i.name,
          count: i.count,
          cost: i.unitCost * i.count,
        })),
      projectedCloseRate: this.estimatedCloseRate(),
      totalCost: this.totalCost(),
    };

    this.campaigns.update(c => [newCampaign, ...c]);

    this.snackBar.open(
      `Campaign "${newCampaign.name}" created successfully! ${this.targetPatientCount()} patients targeted.`,
      'Close',
      { duration: 5000 }
    );

    this.isCreatingCampaign.set(false);
  }

  scheduleCampaign(): void {
    // Would open a date picker dialog
    this.snackBar.open('Campaign scheduled for next available date', 'Close', { duration: 3000 });
  }

  viewCampaign(campaign: Campaign): void {
    // Would navigate to campaign detail view
    this.snackBar.open(`Viewing campaign: ${campaign.name}`, 'Close', { duration: 2000 });
  }

  deleteCampaign(campaign: Campaign): void {
    this.campaigns.update(c => c.filter(item => item.id !== campaign.id));
    this.snackBar.open(`Campaign "${campaign.name}" deleted`, 'Undo', { duration: 3000 });
  }

  getMeasureName(code: string): string {
    const measures: Record<string, string> = {
      'BCS': 'Breast Cancer Screening',
      'COL': 'Colorectal Cancer Screening',
      'CBP': 'Controlling High Blood Pressure',
      'CDC': 'Comprehensive Diabetes Care',
      'EED': 'Eye Exam for Diabetics',
      'SPC': 'Statin Therapy',
    };
    return measures[code] || code;
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'Completed': return 'primary';
      case 'In Progress': return 'accent';
      case 'Scheduled': return 'warn';
      default: return '';
    }
  }

  formatCurrency(value: number): string {
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency: 'USD',
      minimumFractionDigits: 0,
      maximumFractionDigits: 0,
    }).format(value);
  }
}
