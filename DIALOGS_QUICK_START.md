# Advanced Dialogs - Quick Start Guide

**For developers integrating Phase 5 Advanced Dialogs**

---

## 🚀 Quick Start (5 minutes)

### 1. Import the Dialog Service

```typescript
// In your component
import { DialogService } from './services/dialog.service';

@Component({...})
export class MyComponent {
  constructor(private dialogService: DialogService) {}
}
```

### 2. Open a Dialog

```typescript
// Patient Edit Dialog
openPatientDialog(): void {
  this.dialogService.openPatientEdit().subscribe(patient => {
    if (patient) {
      console.log('Saved patient:', patient);
    }
  });
}

// Evaluation Details Dialog
viewEvaluation(evalId: string): void {
  this.dialogService.openEvaluationDetails(evalId);
}

// Advanced Filter Dialog
applyFilters(): void {
  const fields = [
    { name: 'name', label: 'Name', type: 'text' as const }
  ];

  this.dialogService.openAdvancedFilter(fields).subscribe(config => {
    if (config) {
      // Apply filters
    }
  });
}
```

---

## 📋 Available Dialogs

| Dialog | Method | Returns |
|--------|--------|---------|
| Patient Edit | `openPatientEdit(patient?)` | `Observable<Patient \| null>` |
| Evaluation Details | `openEvaluationDetails(id, name?, measure?)` | `void` |
| Advanced Filter | `openAdvancedFilter(fields, current?)` | `Observable<FilterConfig \| null>` |
| Batch Evaluation | `openBatchEvaluation()` | `Observable<BatchResult \| null>` |
| Confirmation | `confirm(title, message)` | `Observable<boolean>` |
| Delete Confirm | `confirmDelete(name, type?)` | `Observable<boolean>` |

---

## 💡 Common Patterns

### Create New Patient
```typescript
addPatient(): void {
  this.dialogService.openPatientEdit().subscribe(patient => {
    if (patient) {
      this.patientService.create(patient).subscribe({
        next: () => this.toast.success('Created'),
        error: (e) => this.dialogService.openErrorDetails(e)
      });
    }
  });
}
```

### Edit Existing Patient
```typescript
editPatient(patient: Patient): void {
  this.dialogService.openPatientEdit(patient).subscribe(updated => {
    if (updated) {
      this.patientService.update(updated).subscribe({
        next: () => this.toast.success('Updated'),
        error: (e) => this.toast.error('Failed')
      });
    }
  });
}
```

### Confirm Before Delete
```typescript
deletePatient(patient: Patient): void {
  this.dialogService
    .confirmDelete(patient.name[0].text, 'patient')
    .subscribe(confirmed => {
      if (confirmed) {
        this.patientService.delete(patient.id).subscribe({
          next: () => this.toast.success('Deleted'),
          error: (e) => this.toast.error('Failed')
        });
      }
    });
}
```

### Apply Advanced Filters
```typescript
filterResults(): void {
  const fields: FilterField[] = [
    { name: 'name', label: 'Patient Name', type: 'text' },
    { name: 'score', label: 'Score', type: 'number' },
    { name: 'date', label: 'Date', type: 'date' }
  ];

  this.dialogService
    .openAdvancedFilter(fields, this.currentFilters)
    .subscribe(config => {
      if (config) {
        this.currentFilters = config;
        this.loadResults(this.buildQuery(config));
      }
    });
}
```

---

## 🎯 Dialog Data Interfaces

### Patient Edit Dialog
```typescript
interface PatientEditDialogData {
  mode: 'create' | 'edit';
  patient?: Patient;
}
```

### Evaluation Details Dialog
```typescript
interface EvaluationDetailsDialogData {
  evaluationId: string;
  patientName?: string;
  measureName?: string;
}
```

### Advanced Filter Dialog
```typescript
interface AdvancedFilterDialogData {
  availableFields: FilterField[];
  currentFilters?: FilterConfig;
}

interface FilterField {
  name: string;
  label: string;
  type: 'text' | 'number' | 'date' | 'select';
  options?: { value: any; label: string }[];
}

interface FilterConfig {
  logic: 'AND' | 'OR';
  filters: FilterCriteria[];
  name?: string;
}
```

---

## 🛠️ Testing Your Integration

### Unit Test
```typescript
describe('MyComponent', () => {
  let dialogService: jasmine.SpyObj<DialogService>;

  beforeEach(() => {
    dialogService = jasmine.createSpyObj('DialogService', [
      'openPatientEdit',
      'confirm'
    ]);

    TestBed.configureTestingModule({
      providers: [{ provide: DialogService, useValue: dialogService }]
    });
  });

  it('should open patient edit dialog', () => {
    dialogService.openPatientEdit.and.returnValue(of(mockPatient));
    component.addPatient();
    expect(dialogService.openPatientEdit).toHaveBeenCalled();
  });
});
```

---

## ⚠️ Common Issues

### Issue: Dialog not showing
**Solution:** Ensure MatDialogModule is imported in your module

### Issue: Dialog closes immediately
**Solution:** Check that you're subscribing to the Observable returned

### Issue: Form validation not working
**Solution:** Make sure ReactiveFormsModule is imported

### Issue: Mobile dialog cut off
**Solution:** Dialog service handles this automatically - check viewport meta tag

---

## 📚 Full Documentation

- **Detailed Guide:** `PHASE_5_ADVANCED_DIALOGS_IMPLEMENTATION.md`
- **Complete Report:** `PHASE_5_DIALOGS_FINAL_REPORT.md`
- **API Reference:** See inline JSDoc in `/dialogs/` components

---

## 🆘 Need Help?

1. Check the comprehensive documentation files
2. Review the implementation code in `/dialogs/`
3. Look at test files for usage examples
4. Check the Dialog Service for all available methods

---

**Ready to go!** Start with `dialogService.openPatientEdit()` and explore from there.
