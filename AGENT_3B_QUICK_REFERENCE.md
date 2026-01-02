# Agent 3B Quick Reference Guide

## 🚀 Quick Start for Developers

### Import Services in Your Component

```typescript
import { Component, OnInit } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable } from 'rxjs';

// Services
import { PatientService } from './services/patient.service';
import { CareGapService } from './services/care-gap.service';
import { FhirService } from './services/fhir.service';
import { AuthService } from './services/auth.service';
import { NotificationService } from './services/notification.service';
import { LoadingService } from './services/loading.service';

// Store
import { AppState } from './store';
import * as PatientActions from './store/actions/patient.actions';
import { selectAllPatients, selectLoading } from './store/selectors/patient.selectors';

// Models
import { Patient } from './models/patient.model';
import { CareGap } from './services/care-gap.service';
```

---

## 📦 Service Usage Examples

### 1. Patient Service

```typescript
// Load all patients
this.patientService.getPatients(100).subscribe({
  next: (patients) => console.log('Patients:', patients),
  error: (error) => console.error('Error:', error)
});

// Get single patient
this.patientService.getPatient('patient-123').subscribe({
  next: (patient) => this.currentPatient = patient,
  error: (error) => this.notificationService.error('Failed to load patient')
});

// Search patients
this.patientService.searchPatients({
  name: 'Smith',
  gender: 'female'
}).subscribe({
  next: (results) => this.searchResults = results
});

// Create patient
this.patientService.createPatient(newPatient).subscribe({
  next: (created) => this.notificationService.success('Patient created!'),
  error: (error) => this.notificationService.error('Creation failed')
});
```

### 2. Care Gap Service

```typescript
// Get patient care gaps (with caching)
this.careGapService.getPatientCareGaps('patient-123').subscribe({
  next: (gaps) => this.careGaps = gaps
});

// Detect new gaps
this.careGapService.detectGapsForPatient('patient-123').subscribe({
  next: (gaps) => console.log('Detected gaps:', gaps)
});

// Close a gap
this.careGapService.closeGap('gap-456', {
  reason: 'Service completed',
  notes: 'Patient received mammogram',
  closedBy: 'dr-smith'
}).subscribe({
  next: (result) => this.notificationService.success('Gap closed')
});

// Get high priority gaps
this.careGapService.getHighPriorityGaps(20).subscribe({
  next: (gaps) => this.priorityGaps = gaps
});

// Listen to gap updates
this.careGapService.gapUpdates$.subscribe({
  next: (update) => {
    if (update) {
      console.log('Gap update:', update);
    }
  }
});
```

### 3. FHIR Service

```typescript
// Get patient observations
this.fhirService.getObservations('patient-123', {
  category: 'vital-signs',
  _count: 50
}).subscribe({
  next: (observations) => this.observations = observations
});

// Get conditions
this.fhirService.getConditions('patient-123').subscribe({
  next: (conditions) => this.conditions = conditions
});

// Get medications
this.fhirService.getMedicationRequests('patient-123').subscribe({
  next: (medications) => this.medications = medications
});

// Export patient as bundle
this.fhirService.exportPatientAsBundle('patient-123').subscribe({
  next: (bundle) => this.downloadBundle(bundle)
});

// Format coding for display
const display = this.fhirService.formatCoding(coding);
```

### 4. Auth Service

```typescript
// Login
this.authService.login(username, password).subscribe({
  next: (response) => {
    this.notificationService.success('Login successful');
    this.router.navigate(['/dashboard']);
  },
  error: (error) => {
    this.notificationService.error('Login failed');
  }
});

// Check authentication
if (this.authService.isAuthenticated()) {
  // User is logged in
}

// Check roles
if (this.authService.hasRole('ADMIN')) {
  // User is admin
}

if (this.authService.hasAnyRole(['ADMIN', 'CLINICIAN'])) {
  // User has one of these roles
}

// Check permissions
if (this.authService.hasPermission('patients:write')) {
  // User can write patients
}

// Get current user
this.authService.currentUser$.subscribe({
  next: (user) => this.currentUser = user
});

// Logout
this.authService.logout();
```

### 5. Notification Service

```typescript
// Success notification
this.notificationService.success('Operation completed successfully');

// Error notification (stays longer)
this.notificationService.error('Failed to save data');

// Warning notification
this.notificationService.warning('This action cannot be undone');

// Info notification
this.notificationService.info('New features available');

// Custom duration and action
this.notificationService.success('Saved!', 5000, 'View');
```

### 6. Loading Service

```typescript
// Show loading
this.loadingService.show();

// Hide loading
this.loadingService.hide();

// Observable loading state (for UI)
this.isLoading$ = this.loadingService.isLoading$;

// In template:
<div *ngIf="isLoading$ | async">Loading...</div>
```

### 7. Logger Service

```typescript
// Debug (only in development)
this.logger.debug('Debug info', { data: someData });

// Info
this.logger.log('Operation completed', { id: 123 });

// Warning
this.logger.warn('Deprecated API used');

// Error
this.logger.error('Failed to load data', error);

// With context
this.logger.logWithContext('PatientComponent', 'Patient loaded', patient);
```

---

## 🏪 NgRx Store Usage

### Dispatch Actions

```typescript
export class PatientListComponent implements OnInit {
  patients$: Observable<Patient[]>;
  loading$: Observable<boolean>;

  constructor(private store: Store<AppState>) {}

  ngOnInit() {
    // Load patients
    this.store.dispatch(loadPatients({ count: 100 }));

    // Select patients from store
    this.patients$ = this.store.select(selectAllPatients);
    this.loading$ = this.store.select(selectLoading);
  }

  createPatient(patient: Patient) {
    this.store.dispatch(createPatient({ patient }));
  }

  updatePatient(id: string, patient: Patient) {
    this.store.dispatch(updatePatient({ id, patient }));
  }

  deletePatient(id: string) {
    this.store.dispatch(deletePatient({ id }));
  }

  searchPatients(name: string) {
    this.store.dispatch(searchPatients({
      params: { name }
    }));
  }
}
```

### In Template

```html
<!-- Show loading -->
<div *ngIf="loading$ | async">Loading patients...</div>

<!-- Display patients -->
<div *ngFor="let patient of patients$ | async">
  <h3>{{ patient.name[0].family }}, {{ patient.name[0].given[0] }}</h3>
  <button (click)="deletePatient(patient.id)">Delete</button>
</div>
```

---

## 🛡️ Route Guards

### Protect Routes

```typescript
// app.routes.ts
import { AuthGuard } from './guards/auth.guard';
import { RoleGuard } from './guards/role.guard';
import { PermissionGuard } from './guards/permission.guard';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [AuthGuard]  // Requires authentication
  },
  {
    path: 'admin',
    component: AdminComponent,
    canActivate: [AuthGuard, RoleGuard],
    data: { roles: ['ADMIN'] }  // Requires ADMIN role
  },
  {
    path: 'reports',
    component: ReportsComponent,
    canActivate: [AuthGuard, PermissionGuard],
    data: { permissions: ['reports:read', 'reports:create'] }
  }
];
```

---

## 🔌 HTTP Interceptors

### Configure in App Config

```typescript
// app.config.ts
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { authInterceptor } from './interceptors/auth.interceptor';
import { jwtInterceptor } from './interceptors/jwt.interceptor';
import { errorInterceptor } from './interceptors/error.interceptor';
import { loadingInterceptor } from './interceptors/loading.interceptor';
import { tenantInterceptor } from './interceptors/tenant.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideHttpClient(
      withInterceptors([
        authInterceptor,      // Adds Basic Auth for backend services
        jwtInterceptor,       // Adds JWT token, handles 401
        errorInterceptor,     // Transforms errors
        loadingInterceptor,   // Tracks loading state
        tenantInterceptor     // Adds tenant header
      ])
    ),
    // ... other providers
  ]
};
```

---

## 📝 Module Configuration

### App Module Setup (if using NgModule)

```typescript
// app.module.ts
import { StoreModule } from '@ngrx/store';
import { EffectsModule } from '@ngrx/effects';
import { StoreDevtoolsModule } from '@ngrx/store-devtools';
import { reducers } from './store';
import { PatientEffects } from './store/effects/patient.effects';

@NgModule({
  imports: [
    // ... other imports
    StoreModule.forRoot(reducers),
    EffectsModule.forRoot([PatientEffects]),
    StoreDevtoolsModule.instrument({
      maxAge: 25,
      logOnly: environment.production
    })
  ],
  // ... providers, declarations, bootstrap
})
export class AppModule {}
```

### Standalone App Configuration

```typescript
// app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideStore } from '@ngrx/store';
import { provideEffects } from '@ngrx/effects';
import { reducers } from './store';
import { PatientEffects } from './store/effects/patient.effects';

export const appConfig: ApplicationConfig = {
  providers: [
    provideStore(reducers),
    provideEffects([PatientEffects]),
    // ... other providers
  ]
};
```

---

## 🎨 UI Integration Examples

### Component with Full Integration

```typescript
import { Component, OnInit, OnDestroy } from '@angular/core';
import { Store } from '@ngrx/store';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { AppState } from './store';
import * as PatientActions from './store/actions/patient.actions';
import {
  selectAllPatients,
  selectLoading,
  selectError
} from './store/selectors/patient.selectors';
import { Patient } from './models/patient.model';
import { NotificationService } from './services/notification.service';
import { CareGapService, CareGap } from './services/care-gap.service';

@Component({
  selector: 'app-patient-dashboard',
  template: `
    <div class="dashboard">
      <h1>Patient Dashboard</h1>

      <!-- Loading indicator -->
      <mat-spinner *ngIf="loading$ | async"></mat-spinner>

      <!-- Error display -->
      <mat-error *ngIf="error$ | async as error">
        {{ error }}
      </mat-error>

      <!-- Patient list -->
      <div class="patient-list">
        <mat-card *ngFor="let patient of patients$ | async" (click)="selectPatient(patient)">
          <mat-card-header>
            <mat-card-title>{{ getPatientName(patient) }}</mat-card-title>
            <mat-card-subtitle>
              Care Gaps: {{ getCareGapCount(patient.id) }}
            </mat-card-subtitle>
          </mat-card-header>
          <mat-card-actions>
            <button mat-button (click)="viewPatient(patient.id)">View</button>
            <button mat-button (click)="editPatient(patient.id)">Edit</button>
          </mat-card-actions>
        </mat-card>
      </div>
    </div>
  `
})
export class PatientDashboardComponent implements OnInit, OnDestroy {
  patients$: Observable<Patient[]>;
  loading$: Observable<boolean>;
  error$: Observable<string | null>;

  careGapCounts = new Map<string, number>();
  private destroy$ = new Subject<void>();

  constructor(
    private store: Store<AppState>,
    private careGapService: CareGapService,
    private notificationService: NotificationService
  ) {
    this.patients$ = this.store.select(selectAllPatients);
    this.loading$ = this.store.select(selectLoading);
    this.error$ = this.store.select(selectError);
  }

  ngOnInit() {
    // Load patients
    this.store.dispatch(loadPatients({ count: 100 }));

    // Subscribe to patients and load care gaps
    this.patients$.pipe(takeUntil(this.destroy$)).subscribe({
      next: (patients) => {
        patients.forEach((patient) => {
          this.loadCareGaps(patient.id!);
        });
      }
    });
  }

  ngOnDestroy() {
    this.destroy$.next();
    this.destroy$.complete();
  }

  private loadCareGaps(patientId: string) {
    this.careGapService.getPatientCareGaps(patientId).subscribe({
      next: (gaps) => {
        this.careGapCounts.set(patientId, gaps.length);
      },
      error: (error) => {
        console.error('Error loading care gaps:', error);
      }
    });
  }

  selectPatient(patient: Patient) {
    this.store.dispatch(PatientActions.selectPatient({ patient }));
    this.notificationService.info(`Selected: ${this.getPatientName(patient)}`);
  }

  getPatientName(patient: Patient): string {
    if (!patient.name || patient.name.length === 0) return 'Unknown';
    const name = patient.name[0];
    return `${name.family || ''}, ${name.given?.join(' ') || ''}`.trim();
  }

  getCareGapCount(patientId: string): number {
    return this.careGapCounts.get(patientId) || 0;
  }

  viewPatient(id: string) {
    // Navigate to patient detail
  }

  editPatient(id: string) {
    // Navigate to patient edit
  }
}
```

---

## 📚 Type Definitions Reference

### Patient (from FHIR)
```typescript
interface Patient {
  id?: string;
  resourceType: 'Patient';
  active?: boolean;
  name?: HumanName[];
  telecom?: ContactPoint[];
  gender?: 'male' | 'female' | 'other' | 'unknown';
  birthDate?: string;
  address?: Address[];
  identifier?: Identifier[];
}
```

### Care Gap
```typescript
interface CareGap {
  id: string;
  patientId: string;
  measureId: string;
  measureName: string;
  gapType: CareGapType;
  status: CareGapStatus;
  priority: GapPriority;
  priorityScore: number;
  description: string;
  recommendation: string;
  dueDate?: string;
  detectedDate: string;
  closedDate?: string;
  closureReason?: string;
}
```

### User (from Auth)
```typescript
interface User {
  id: string;
  username: string;
  email: string;
  firstName: string;
  lastName: string;
  fullName: string;
  roles: Role[];
  tenantId?: string;
  active: boolean;
}
```

---

## 🔧 Troubleshooting

### Common Issues

**1. "Cannot find module '@ngrx/store'"**
```bash
npm install @ngrx/store @ngrx/effects @ngrx/store-devtools --save
```

**2. "No provider for MatSnackBar"**
```typescript
// Import MatSnackBarModule in your module
import { MatSnackBarModule } from '@angular/material/snack-bar';
```

**3. "Token refresh failed"**
- Check if refresh token is valid
- Verify backend refresh endpoint is working
- Check network connectivity

**4. "Guards not working"**
- Ensure guards are added to canActivate array
- Verify AuthService is properly configured
- Check if user is authenticated

---

## 📖 Additional Resources

- **NgRx Documentation**: https://ngrx.io/
- **Angular HTTP Client**: https://angular.io/guide/http
- **FHIR R4 Specification**: https://hl7.org/fhir/R4/
- **JWT Best Practices**: https://tools.ietf.org/html/rfc7519

---

**Created by Agent 3B | Part of TDD Swarm Wave 3**
