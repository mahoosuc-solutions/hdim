# Shared Components Library - Usage Examples

Complete usage examples for all shared components in the Clinical Portal.

---

## 1. StatCardComponent - Statistics Display

### Basic Usage
```html
<app-stat-card
  title="Total Patients"
  value="1,234"
  icon="people"
  color="primary">
</app-stat-card>
```

### With Trend Indicator
```html
<app-stat-card
  title="Monthly Revenue"
  value="$45,678"
  subtitle="+12.5% from last month"
  icon="attach_money"
  color="success"
  trend="up">
</app-stat-card>
```

### Dashboard Grid Example
```html
<div class="stats-grid" style="display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 16px;">
  <app-stat-card
    title="Total Evaluations"
    [value]="stats.totalEvaluations | number"
    subtitle="All time"
    icon="assessment"
    color="primary">
  </app-stat-card>

  <app-stat-card
    title="Active Patients"
    [value]="stats.activePatients | number"
    subtitle="Currently enrolled"
    icon="people"
    color="accent"
    [clickable]="true"
    (click)="navigateToPatients()">
  </app-stat-card>

  <app-stat-card
    title="Compliance Rate"
    [value]="stats.complianceRate + '%'"
    [subtitle]="stats.complianceChange + '% vs last month'"
    icon="verified"
    color="success"
    [trend]="stats.complianceChange > 0 ? 'up' : stats.complianceChange < 0 ? 'down' : 'stable'">
  </app-stat-card>

  <app-stat-card
    title="Pending Reviews"
    [value]="stats.pendingReviews | number"
    subtitle="Requires attention"
    icon="schedule"
    color="warn"
    tooltip="Items awaiting review">
  </app-stat-card>
</div>
```

---

## 2. EmptyStateComponent - Empty States

### Basic Empty State
```html
<app-empty-state
  icon="inbox"
  title="No Items Found"
  message="There are no items to display at this time.">
</app-empty-state>
```

### With Action Button
```html
<app-empty-state
  icon="people_outline"
  title="No Patients Yet"
  message="Get started by adding your first patient to the system."
  actionText="Add Patient"
  actionIcon="add"
  actionColor="primary"
  (action)="openAddPatientDialog()">
</app-empty-state>
```

### Search Results Empty State
```html
<app-empty-state
  *ngIf="filteredResults.length === 0 && searchTerm"
  icon="search_off"
  title="No Results Found"
  [message]="'No results found for &quot;' + searchTerm + '&quot;. Try different search terms.'"
  actionText="Clear Search"
  actionIcon="clear"
  (action)="clearSearch()">
</app-empty-state>
```

### Loading State with Empty State
```typescript
// Component
isLoading = false;
hasData = false;
loadingText = 'Loading data...';

// Template
<app-loading-overlay
  [isLoading]="isLoading"
  [message]="loadingText">
</app-loading-overlay>

<app-empty-state
  *ngIf="!isLoading && !hasData"
  icon="folder_open"
  title="No Data Available"
  message="Start by creating your first evaluation to see dashboard metrics."
  actionText="Create Evaluation"
  actionIcon="add"
  [actionLoading]="isCreating"
  (action)="createEvaluation()">
</app-empty-state>
```

---

## 3. ErrorBannerComponent - Error & Info Messages

### Error Message
```html
<app-error-banner
  message="Failed to load patient data. Please try again."
  type="error"
  [dismissible]="true"
  [retryButton]="true"
  (retry)="loadPatients()"
  (dismiss)="clearError()">
</app-error-banner>
```

### Success Message with Auto-Dismiss
```html
<app-error-banner
  message="Patient record saved successfully!"
  type="success"
  [dismissible]="true"
  [autoDismissTimeout]="3000"
  (dismiss)="clearSuccessMessage()">
</app-error-banner>
```

### Warning Message
```html
<app-error-banner
  message="Some measures have not been evaluated in the last 30 days."
  type="warning"
  [dismissible]="true">
</app-error-banner>
```

### Info Message
```html
<app-error-banner
  message="System maintenance scheduled for tonight at 11 PM EST."
  type="info"
  [dismissible]="true">
</app-error-banner>
```

### Multiple Banners Stack
```html
<!-- Component -->
errors: string[] = [];
warnings: string[] = [];

<!-- Template -->
<div class="banner-stack">
  <app-error-banner
    *ngFor="let error of errors; trackBy: trackByIndex"
    [message]="error"
    type="error"
    [dismissible]="true"
    [retryButton]="true"
    (retry)="retryOperation()"
    (dismiss)="removeError(error)">
  </app-error-banner>

  <app-error-banner
    *ngFor="let warning of warnings; trackBy: trackByIndex"
    [message]="warning"
    type="warning"
    [dismissible]="true"
    (dismiss)="removeWarning(warning)">
  </app-error-banner>
</div>
```

---

## 4. FilterPanelComponent - Advanced Filtering

### Basic Filter Panel
```typescript
// Component
filterConfig: FilterConfig[] = [
  {
    key: 'name',
    label: 'Patient Name',
    type: 'text',
    placeholder: 'Enter patient name'
  },
  {
    key: 'status',
    label: 'Status',
    type: 'select',
    options: [
      { label: 'Active', value: 'active' },
      { label: 'Inactive', value: 'inactive' }
    ]
  }
];

onFilterChange(filters: FilterValues) {
  console.log('Active filters:', filters);
  this.applyFilters(filters);
}
```

```html
<app-filter-panel
  [filters]="filterConfig"
  [expanded]="false"
  (filterChange)="onFilterChange($event)">
</app-filter-panel>
```

### Complex Filter Example
```typescript
filterConfig: FilterConfig[] = [
  // Text search
  {
    key: 'search',
    label: 'Search',
    type: 'text',
    placeholder: 'Search by name, MRN, or ID'
  },

  // Select dropdown
  {
    key: 'gender',
    label: 'Gender',
    type: 'select',
    options: [
      { label: 'Male', value: 'male' },
      { label: 'Female', value: 'female' },
      { label: 'Other', value: 'other' }
    ]
  },

  // Multiple select
  {
    key: 'status',
    label: 'Patient Status',
    type: 'select',
    options: [
      { label: 'Active', value: 'active' },
      { label: 'Inactive', value: 'inactive' },
      { label: 'Pending', value: 'pending' }
    ],
    defaultValue: 'active'
  },

  // Number range
  {
    key: 'age',
    label: 'Age',
    type: 'number-range'
  },

  // Date range
  {
    key: 'lastVisit',
    label: 'Last Visit',
    type: 'date-range'
  }
];

// Handle filter changes
onFilterChange(filters: FilterValues) {
  this.filteredData = this.data.filter(item => {
    // Text search
    if (filters['search']) {
      const search = filters['search'].toLowerCase();
      if (!item.name.toLowerCase().includes(search) &&
          !item.mrn.toLowerCase().includes(search)) {
        return false;
      }
    }

    // Select filters
    if (filters['gender'] && item.gender !== filters['gender']) {
      return false;
    }

    if (filters['status'] && item.status !== filters['status']) {
      return false;
    }

    // Number range
    if (filters['age_min'] && item.age < filters['age_min']) {
      return false;
    }
    if (filters['age_max'] && item.age > filters['age_max']) {
      return false;
    }

    // Date range
    if (filters['lastVisit_start'] && item.lastVisit < filters['lastVisit_start']) {
      return false;
    }
    if (filters['lastVisit_end'] && item.lastVisit > filters['lastVisit_end']) {
      return false;
    }

    return true;
  });
}
```

---

## 5. DateRangePickerComponent - Date Selection

### Basic Date Range Picker
```html
<app-date-range-picker
  [startDate]="startDate"
  [endDate]="endDate"
  (rangeChange)="onDateRangeChange($event)">
</app-date-range-picker>
```

### With Custom Labels and Presets
```html
<app-date-range-picker
  [startDate]="reportStartDate"
  [endDate]="reportEndDate"
  startLabel="Report Start Date"
  endLabel="Report End Date"
  [showPresets]="true"
  (rangeChange)="updateReportDates($event)">
</app-date-range-picker>
```

### With Date Constraints
```typescript
// Component
today = new Date();
oneYearAgo = new Date(this.today.getFullYear() - 1, this.today.getMonth(), this.today.getDate());
startDate: Date | null = null;
endDate: Date | null = null;

onDateRangeChange(range: DateRange) {
  this.startDate = range.start;
  this.endDate = range.end;
  this.loadDataForDateRange(range.start, range.end);
}
```

```html
<app-date-range-picker
  [startDate]="startDate"
  [endDate]="endDate"
  [minDate]="oneYearAgo"
  [maxDate]="today"
  startLabel="From"
  endLabel="To"
  (rangeChange)="onDateRangeChange($event)">
</app-date-range-picker>
```

### Filter Panel Integration
```html
<mat-card class="filters-card">
  <mat-card-content>
    <div class="filter-row">
      <app-date-range-picker
        [startDate]="filters.startDate"
        [endDate]="filters.endDate"
        (rangeChange)="onDateFilterChange($event)">
      </app-date-range-picker>

      <button mat-raised-button color="primary" (click)="applyFilters()">
        Apply Filters
      </button>
    </div>
  </mat-card-content>
</mat-card>
```

---

## 6. StatusBadgeComponent - Status Indicators

### Basic Status Badges
```html
<!-- Success status -->
<app-status-badge
  status="Active"
  type="success">
</app-status-badge>

<!-- Warning status -->
<app-status-badge
  status="Pending"
  type="warning">
</app-status-badge>

<!-- Error status -->
<app-status-badge
  status="Failed"
  type="error">
</app-status-badge>

<!-- Info status -->
<app-status-badge
  status="In Progress"
  type="info">
</app-status-badge>

<!-- Neutral status -->
<app-status-badge
  status="Draft"
  type="neutral">
</app-status-badge>
```

### With Custom Icons
```html
<app-status-badge
  status="Verified"
  type="success"
  icon="verified_user"
  tooltip="This patient record has been verified">
</app-status-badge>

<app-status-badge
  status="Priority"
  type="warning"
  icon="priority_high"
  tooltip="High priority patient">
</app-status-badge>
```

### Compact Badges in Tables
```html
<table mat-table [dataSource]="patients">
  <ng-container matColumnDef="status">
    <th mat-header-cell *matHeaderCellDef>Status</th>
    <td mat-cell *matCellDef="let patient">
      <app-status-badge
        [status]="patient.status"
        [type]="getStatusType(patient.status)"
        [compact]="true">
      </app-status-badge>
    </td>
  </ng-container>
</table>
```

```typescript
// Component
getStatusType(status: string): StatusType {
  switch(status.toLowerCase()) {
    case 'active': return 'success';
    case 'inactive': return 'neutral';
    case 'pending': return 'warning';
    case 'suspended': return 'error';
    default: return 'info';
  }
}
```

### Dynamic Status Badge
```html
<app-status-badge
  [status]="evaluation.outcome | titlecase"
  [type]="getOutcomeType(evaluation.outcome)"
  [icon]="getOutcomeIcon(evaluation.outcome)"
  [tooltip]="'Evaluated on ' + (evaluation.date | date)">
</app-status-badge>
```

```typescript
// Component
getOutcomeType(outcome: string): StatusType {
  const outcomeMap: Record<string, StatusType> = {
    'compliant': 'success',
    'non-compliant': 'error',
    'not-eligible': 'info',
    'pending': 'warning'
  };
  return outcomeMap[outcome] || 'neutral';
}

getOutcomeIcon(outcome: string): string {
  const iconMap: Record<string, string> = {
    'compliant': 'check_circle',
    'non-compliant': 'cancel',
    'not-eligible': 'info',
    'pending': 'schedule'
  };
  return iconMap[outcome] || 'help';
}
```

---

## 7. PageHeaderComponent - Consistent Page Headers

### Basic Page Header
```html
<app-page-header
  title="Patients"
  subtitle="Manage patient records and information">
</app-page-header>
```

### With Breadcrumbs
```typescript
// Component
breadcrumbs: Breadcrumb[] = [
  { label: 'Home', route: '/', icon: 'home' },
  { label: 'Patients', route: '/patients' },
  { label: 'John Doe', route: '/patients/123' },
  { label: 'Edit' }
];
```

```html
<app-page-header
  title="Edit Patient"
  subtitle="Update patient information"
  [breadcrumbs]="breadcrumbs">
</app-page-header>
```

### With Action Buttons
```html
<app-page-header
  title="Patient Details"
  subtitle="View and manage patient information"
  [breadcrumbs]="breadcrumbs">
  <!-- Action buttons via content projection -->
  <button mat-raised-button color="primary" (click)="editPatient()">
    <mat-icon>edit</mat-icon>
    Edit Patient
  </button>

  <button mat-stroked-button (click)="printPatient()">
    <mat-icon>print</mat-icon>
    Print
  </button>

  <button mat-icon-button [matMenuTriggerFor]="menu">
    <mat-icon>more_vert</mat-icon>
  </button>
  <mat-menu #menu="matMenu">
    <button mat-menu-item (click)="exportPatient()">
      <mat-icon>download</mat-icon>
      Export
    </button>
    <button mat-menu-item (click)="deletePatient()">
      <mat-icon>delete</mat-icon>
      Delete
    </button>
  </mat-menu>
</app-page-header>
```

### With Back Button
```html
<app-page-header
  title="Patient Details"
  subtitle="John Doe (MRN: 12345)"
  [breadcrumbs]="breadcrumbs"
  [showBackButton]="true">
  <button mat-raised-button color="primary">
    <mat-icon>save</mat-icon>
    Save Changes
  </button>
</app-page-header>
```

### Complete Page Example
```typescript
// Component
breadcrumbs: Breadcrumb[] = [
  { label: 'Dashboard', route: '/dashboard', icon: 'dashboard' },
  { label: 'Evaluations', route: '/evaluations' },
  { label: 'Results' }
];

isLoading = false;
error: string | null = null;
results: any[] = [];
```

```html
<!-- Page Header -->
<app-page-header
  title="Evaluation Results"
  subtitle="Quality measure evaluation outcomes"
  [breadcrumbs]="breadcrumbs">
  <button mat-raised-button color="primary" (click)="newEvaluation()">
    <mat-icon>add</mat-icon>
    New Evaluation
  </button>

  <button mat-stroked-button (click)="exportResults()">
    <mat-icon>download</mat-icon>
    Export
  </button>
</app-page-header>

<!-- Error Banner -->
<app-error-banner
  *ngIf="error"
  [message]="error"
  type="error"
  [retryButton]="true"
  (retry)="loadResults()"
  (dismiss)="clearError()">
</app-error-banner>

<!-- Loading Overlay -->
<app-loading-overlay
  [isLoading]="isLoading"
  message="Loading evaluation results...">
</app-loading-overlay>

<!-- Empty State -->
<app-empty-state
  *ngIf="!isLoading && !error && results.length === 0"
  icon="assessment"
  title="No Evaluation Results"
  message="Start by running your first quality measure evaluation."
  actionText="Run Evaluation"
  actionIcon="play_arrow"
  (action)="runEvaluation()">
</app-empty-state>

<!-- Results Content -->
<div *ngIf="!isLoading && !error && results.length > 0">
  <!-- Your results content here -->
</div>
```

---

## Combined Real-World Examples

### Dashboard Page
```html
<!-- Page Header -->
<app-page-header
  title="Clinical Portal Dashboard"
  [breadcrumbs]="[{ label: 'Home', icon: 'home' }]">
  <app-loading-button
    text="Refresh"
    loadingText="Refreshing..."
    successText="Updated!"
    icon="refresh"
    color="primary"
    [loading]="isRefreshing"
    [success]="refreshSuccess"
    (buttonClick)="refreshDashboard()">
  </app-loading-button>
</app-page-header>

<!-- Statistics Cards -->
<div class="stats-grid">
  <app-stat-card
    title="Total Evaluations"
    [value]="stats.totalEvaluations | number"
    subtitle="All time"
    icon="assessment"
    color="primary">
  </app-stat-card>

  <app-stat-card
    title="Total Patients"
    [value]="stats.totalPatients | number"
    subtitle="Active patients"
    icon="people"
    color="accent">
  </app-stat-card>

  <app-stat-card
    title="Overall Compliance"
    [value]="stats.complianceRate + '%'"
    [subtitle]="stats.complianceChange + '% vs last period'"
    icon="verified"
    color="success"
    [trend]="getTrend(stats.complianceChange)">
  </app-stat-card>

  <app-stat-card
    title="Recent Evaluations"
    [value]="stats.recentEvaluations | number"
    subtitle="Last 30 days"
    icon="schedule"
    color="primary">
  </app-stat-card>
</div>

<!-- Error Message -->
<app-error-banner
  *ngIf="dashboardError"
  [message]="dashboardError"
  type="error"
  [retryButton]="true"
  (retry)="loadDashboard()"
  (dismiss)="clearError()">
</app-error-banner>

<!-- Empty State -->
<app-empty-state
  *ngIf="!isLoading && !dashboardError && stats.totalEvaluations === 0"
  icon="inbox"
  title="No Data Available"
  message="Start by creating your first evaluation to see dashboard metrics."
  actionText="New Evaluation"
  actionIcon="add"
  (action)="navigateToEvaluations()">
</app-empty-state>
```

### Patient List Page with Filters
```html
<!-- Page Header -->
<app-page-header
  title="Patient Management"
  subtitle="View and manage patient records"
  [breadcrumbs]="[
    { label: 'Home', route: '/', icon: 'home' },
    { label: 'Patients' }
  ]">
  <button mat-raised-button color="primary" (click)="addPatient()">
    <mat-icon>add</mat-icon>
    Add Patient
  </button>
</app-page-header>

<!-- Statistics -->
<div class="stats-row">
  <app-stat-card
    title="Total Patients"
    [value]="patientStats.total | number"
    icon="people"
    color="primary">
  </app-stat-card>

  <app-stat-card
    title="Active Patients"
    [value]="patientStats.active | number"
    icon="person"
    color="success">
  </app-stat-card>

  <app-stat-card
    title="Average Age"
    [value]="(patientStats.averageAge | number: '1.0-0') + ' yrs'"
    icon="cake"
    color="accent">
  </app-stat-card>
</div>

<!-- Filters -->
<app-filter-panel
  [filters]="filterConfig"
  title="Patient Filters"
  (filterChange)="onFilterChange($event)">
</app-filter-panel>

<!-- Results -->
<mat-card>
  <mat-card-content>
    <table mat-table [dataSource]="filteredPatients">
      <!-- Name Column -->
      <ng-container matColumnDef="name">
        <th mat-header-cell *matHeaderCellDef>Name</th>
        <td mat-cell *matCellDef="let patient">{{ patient.name }}</td>
      </ng-container>

      <!-- Status Column -->
      <ng-container matColumnDef="status">
        <th mat-header-cell *matHeaderCellDef>Status</th>
        <td mat-cell *matCellDef="let patient">
          <app-status-badge
            [status]="patient.status"
            [type]="getStatusType(patient.status)"
            [compact]="true">
          </app-status-badge>
        </td>
      </ng-container>

      <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
      <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
    </table>

    <!-- Empty State -->
    <app-empty-state
      *ngIf="filteredPatients.length === 0"
      icon="people_outline"
      title="No Patients Found"
      message="No patients match your current filters. Try adjusting your search criteria."
      actionText="Clear Filters"
      actionIcon="clear"
      (action)="clearFilters()">
    </app-empty-state>
  </mat-card-content>
</mat-card>
```

---

## Best Practices

### 1. Component Composition
Combine components for complete page layouts:
```html
<app-page-header>...</app-page-header>
<app-error-banner>...</app-error-banner>
<app-filter-panel>...</app-filter-panel>
<app-empty-state>...</app-empty-state>
```

### 2. Conditional Rendering
Use *ngIf to show components conditionally:
```html
<app-error-banner *ngIf="hasError">...</app-error-banner>
<app-loading-overlay *ngIf="isLoading">...</app-loading-overlay>
<app-empty-state *ngIf="isEmpty()">...</app-empty-state>
```

### 3. Data Binding
Use property binding for dynamic values:
```html
<app-stat-card
  [value]="statistics.count | number"
  [trend]="getTrendDirection(statistics.change)">
</app-stat-card>
```

### 4. Event Handling
Connect component events to methods:
```html
<app-filter-panel (filterChange)="applyFilters($event)">
</app-filter-panel>
```

### 5. Type Safety
Use TypeScript interfaces for type safety:
```typescript
import { FilterConfig, FilterValues, DateRange, Breadcrumb } from '@app/shared/components';
```

---

**Last Updated:** November 18, 2024
