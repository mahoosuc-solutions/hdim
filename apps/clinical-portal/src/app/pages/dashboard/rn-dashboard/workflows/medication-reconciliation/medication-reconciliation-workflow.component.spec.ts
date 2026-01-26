/**
 * Medication Reconciliation Workflow Component - Unit Tests
 *
 * Tests for verifying medication accuracy, identifying discrepancies,
 * and checking for drug interactions.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule, FormBuilder } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { MedicationReconciliationWorkflowComponent } from './medication-reconciliation-workflow.component';
import { MedicationService, MedicationOrder } from '../../../../services/medication/medication.service';
import { ToastService } from '../../../../services/toast.service';
import { LoggerService } from '../../../../services/logger.service';
import { createMockMatDialogRef } from '../../testing/mocks';

describe('MedicationReconciliationWorkflowComponent', () => {
  let component: MedicationReconciliationWorkflowComponent;
  let fixture: ComponentFixture<MedicationReconciliationWorkflowComponent>;
  let medicationService: jasmine.SpyObj<MedicationService>;
  let toastService: jasmine.SpyObj<ToastService>;
  let dialogRef: jasmine.SpyObj<MatDialogRef<MedicationReconciliationWorkflowComponent>>;

  const mockDialogData = {
    reconciliationId: 'MED_RECON001',
    patientId: 'PATIENT001',
    patientName: 'Jane Doe',
  };

  const mockMedicationOrders: MedicationOrder[] = [
    {
      id: 'ORDER001',
      patientId: 'PATIENT001',
      prescriberId: 'DOC001',
      medicationId: 'MED001',
      dosage: 10,
      dosageUnit: 'MG',
      route: 'ORAL',
      frequency: 'ONCE_DAILY',
      startDate: new Date(),
      prescriptionStatus: 'FILLED',
      priorityLevel: 'ROUTINE',
    },
    {
      id: 'ORDER002',
      patientId: 'PATIENT001',
      prescriberId: 'DOC001',
      medicationId: 'MED002',
      dosage: 5,
      dosageUnit: 'MG',
      route: 'ORAL',
      frequency: 'TWICE_DAILY',
      startDate: new Date(),
      prescriptionStatus: 'FILLED',
      priorityLevel: 'ROUTINE',
    },
  ];

  beforeEach(async () => {
    const medicationSpy = jasmine.createSpyObj('MedicationService', [
      'getActiveOrdersForPatient',
      'checkDrugInteractions',
      'updateMedicationOrder',
      'createMedicationOrder',
      'completeMedicationReconciliation',
      'setTenantContext',
    ]);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error', 'warning']);
    const dialogRefSpy = jasmine.createSpyObj('MatDialogRef', ['close']);
    const loggerSpy = jasmine.createSpyObj('LoggerService', ['withContext']);
    loggerSpy.withContext.and.returnValue({
      log: jasmine.createSpy(),
      debug: jasmine.createSpy(),
      info: jasmine.createSpy(),
      warn: jasmine.createSpy(),
      error: jasmine.createSpy(),
    });

    await TestBed.configureTestingModule({
      declarations: [],
      imports: [ReactiveFormsModule],
      providers: [FormBuilder,
        { provide: MedicationService, useValue: medicationSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: MatDialogRef, useValue: dialogRefSpy },
        { provide: MAT_DIALOG_DATA, useValue: mockDialogData },
        { provide: LoggerService, useValue: loggerSpy },
        { provide: MatDialogRef, useValue: createMockMatDialogRef() }],
    }).compileComponents();

    medicationService = TestBed.inject(MedicationService) as jasmine.SpyObj<MedicationService>;
    toastService = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    dialogRef = TestBed.inject(MatDialogRef) as jasmine.SpyObj<MatDialogRef<MedicationReconciliationWorkflowComponent>>;

    fixture = TestBed.createComponent(MedicationReconciliationWorkflowComponent);
    component = fixture.componentInstance;
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with dialog data', () => {
      expect(component.reconciliationId).toBe('MED_RECON001');
      expect(component.patientId).toBe('PATIENT001');
      expect(component.patientName).toBe('Jane Doe');
    });

    it('should set current step to 0 (load medications)', () => {
      expect(component.currentStep).toBe(0);
      expect(component.totalSteps).toBe(6);
    });

    it('should load system medications on initialization', () => {
      medicationService.getActiveOrdersForPatient.and.returnValue(
        of({ content: mockMedicationOrders, totalElements: 2 })
      );

      component.ngOnInit();

      expect(medicationService.getActiveOrdersForPatient).toHaveBeenCalledWith('PATIENT001', 0, 100);
    });
  });

  describe('Step 0: Load Current Medications', () => {
    beforeEach(() => {
      medicationService.getActiveOrdersForPatient.and.returnValue(
        of({ content: mockMedicationOrders, totalElements: 2 })
      );
    });

    it('should display system medications', (done) => {
      component.ngOnInit();

      setTimeout(() => {
        expect(component.systemMedications.length).toBe(2);
        done();
      }, 100);
    });

    it('should show medication details in table', () => {
      component.ngOnInit();
      expect(component.medicationColumns).toContain('medication');
      expect(component.medicationColumns).toContain('dosage');
      expect(component.medicationColumns).toContain('frequency');
      expect(component.medicationColumns).toContain('status');
    });

    it('should advance to step 1 when medications loaded', (done) => {
      component.ngOnInit();

      setTimeout(() => {
        component.nextStep();
        expect(component.currentStep).toBe(1);
        done();
      }, 100);
    });
  });

  describe('Step 1: Compare with Patient Report', () => {
    beforeEach(() => {
      component.currentStep = 1;
      component.systemMedications = mockMedicationOrders;
    });

    it('should initialize patient reported medications form', () => {
      expect(component.form).toBeDefined();
      expect(component.form.get('patientMedications')).toBeDefined();
    });

    it('should add patient reported medication', () => {
      component.addPatientMedication();
      const medicationsArray = component.form.get('patientMedications') as any;
      expect(medicationsArray.length).toBeGreaterThan(0);
    });

    it('should remove patient reported medication', () => {
      component.addPatientMedication();
      const medicationsArray = component.form.get('patientMedications') as any;
      const initialLength = medicationsArray.length;

      component.removePatientMedication(0);

      expect(medicationsArray.length).toBe(initialLength - 1);
    });

    it('should identify discrepancies between lists', () => {
      component.systemMedications = [
        { ...mockMedicationOrders[0], id: 'MED1' },
        { ...mockMedicationOrders[1], id: 'MED2' },
      ];
      component.patientReportedMedications = [
        { name: 'Medication1', dosage: '10 MG', frequency: 'Once daily' },
        // MED2 is missing from patient report
      ];

      component.compareWithPatientReport();

      expect(component.discrepancies.length).toBeGreaterThan(0);
    });
  });

  describe('Step 2: Identify Discrepancies', () => {
    beforeEach(() => {
      component.currentStep = 2;
      component.systemMedications = mockMedicationOrders;
    });

    it('should flag discontinued medications', () => {
      component.systemMedications[0] = {
        ...component.systemMedications[0],
        prescriptionStatus: 'CANCELLED',
      };

      component.identifyDiscrepancies();

      expect(component.discrepancies).toContain(jasmine.objectContaining({
        type: 'DISCONTINUED',
      }));
    });

    it('should flag duplicate therapy', () => {
      component.systemMedications = [
        { ...mockMedicationOrders[0], id: 'MED1', medicationId: 'SAME_MED' },
        { ...mockMedicationOrders[1], id: 'MED2', medicationId: 'SAME_MED' },
      ];

      component.identifyDuplicateTherapy();

      expect(component.discrepancies).toContain(jasmine.objectContaining({
        type: 'DUPLICATE_THERAPY',
      }));
    });

    it('should flag missing medications', () => {
      component.patientReportedMedications = [
        { name: 'Medication3', dosage: '20 MG', frequency: 'Twice daily' },
      ];

      component.identifyMissingMedications();

      expect(component.discrepancies).toContain(jasmine.objectContaining({
        type: 'MISSING_MEDICATION',
      }));
    });

    it('should flag dose discrepancies', () => {
      component.patientReportedMedications = [
        { name: 'Medication1', dosage: '20 MG', frequency: 'Once daily' }, // Different dose
      ];

      component.identifyDoseDiscrepancies();

      expect(component.discrepancies).toContain(jasmine.objectContaining({
        type: 'DOSE_DISCREPANCY',
      }));
    });
  });

  describe('Step 3: Mark Duplicates/Discontinued', () => {
    beforeEach(() => {
      component.currentStep = 3;
    });

    it('should mark medication for discontinuation', () => {
      const medication = { ...mockMedicationOrders[0] };
      component.markForDiscontinuation(medication);

      expect(medication.prescriptionStatus).toBe('CANCELLED');
    });

    it('should mark duplicate medications', () => {
      component.systemMedications = [
        { ...mockMedicationOrders[0], id: 'MED1', medicationId: 'SAME' },
        { ...mockMedicationOrders[1], id: 'MED2', medicationId: 'SAME' },
      ];

      component.markDuplicateTherapy(0, 1);

      expect(component.medicationMarking).toContain(jasmine.objectContaining({
        id: 'MED1',
        status: 'DUPLICATE',
      }));
    });

    it('should allow unmarking medications', () => {
      const medication = mockMedicationOrders[0];
      component.markForDiscontinuation(medication);
      component.unmarkMedication(medication.id);

      expect(component.medicationMarking.find((m) => m.id === medication.id)).toBeUndefined();
    });
  });

  describe('Step 4: Add Missing Medications', () => {
    beforeEach(() => {
      component.currentStep = 4;
    });

    it('should initialize form for new medication', () => {
      component.initializeNewMedicationForm();

      expect(component.newMedicationForm).toBeDefined();
      expect(component.newMedicationForm.get('medicationName')).toBeDefined();
      expect(component.newMedicationForm.get('dosage')).toBeDefined();
    });

    it('should add new medication to list', () => {
      component.newMedicationForm = new FormBuilder().group({
        medicationName: 'New Med',
        dosage: '15 MG',
        frequency: 'Once daily',
        indication: 'Pain relief',
      });

      component.addNewMedication();

      expect(component.newMedications.length).toBeGreaterThan(0);
    });

    it('should validate new medication form', () => {
      component.newMedicationForm = new FormBuilder().group({
        medicationName: ['', Validators.required],
        dosage: ['', Validators.required],
      });

      component.newMedicationForm.patchValue({
        medicationName: '',
        dosage: '',
      });

      expect(component.newMedicationForm.valid).toBe(false);

      component.newMedicationForm.patchValue({
        medicationName: 'Aspirin',
        dosage: '100 MG',
      });

      expect(component.newMedicationForm.valid).toBe(true);
    });
  });

  describe('Step 5: Check Drug Interactions', () => {
    beforeEach(() => {
      component.currentStep = 5;
      component.systemMedications = mockMedicationOrders;
    });

    it('should check for drug interactions', (done) => {
      const mockInteractions = {
        interactions: [],
        hasSignificantInteractions: false,
      };
      medicationService.checkDrugInteractions.and.returnValue(of(mockInteractions));

      component.checkDrugInteractions();

      setTimeout(() => {
        expect(medicationService.checkDrugInteractions).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should display significant interactions as warnings', (done) => {
      const mockInteractions = {
        interactions: [
          {
            severity: 'MAJOR',
            description: 'Significant interaction detected',
          },
        ],
        hasSignificantInteractions: true,
      };
      medicationService.checkDrugInteractions.and.returnValue(of(mockInteractions));

      component.checkDrugInteractions();

      setTimeout(() => {
        expect(toastService.warning).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should prevent submission if significant interactions exist without acknowledgment', () => {
      component.hasSignificantInteractions = true;
      component.acknowledgedInteractions = false;

      expect(component.canCompleteReconciliation()).toBe(false);
    });

    it('should allow submission if interactions are acknowledged', () => {
      component.hasSignificantInteractions = true;
      component.acknowledgedInteractions = true;

      expect(component.canCompleteReconciliation()).toBe(true);
    });
  });

  describe('Workflow Submission', () => {
    beforeEach(() => {
      component.currentStep = 5;
    });

    it('should save medication reconciliation', (done) => {
      const mockResponse = { id: 'MED_RECON001', status: 'completed' };
      medicationService.completeMedicationReconciliation.and.returnValue(of(mockResponse));

      component.completeReconciliation();

      setTimeout(() => {
        expect(medicationService.completeMedicationReconciliation).toHaveBeenCalled();
        expect(toastService.success).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should close dialog on successful completion', (done) => {
      const mockResponse = { id: 'MED_RECON001', status: 'completed' };
      medicationService.completeMedicationReconciliation.and.returnValue(of(mockResponse));

      component.completeReconciliation();

      setTimeout(() => {
        expect(dialogRef.close).toHaveBeenCalledWith({ success: true, result: mockResponse });
        done();
      }, 100);
    });

    it('should handle save errors gracefully', (done) => {
      medicationService.completeMedicationReconciliation.and.returnValue(
        throwError(() => new Error('Save failed'))
      );

      component.completeReconciliation();

      setTimeout(() => {
        expect(toastService.error).toHaveBeenCalled();
        done();
      }, 100);
    });
  });

  describe('Form Navigation', () => {
    it('should advance to next step when conditions met', () => {
      component.currentStep = 0;
      component.nextStep();

      expect(component.currentStep).toBeGreaterThan(0);
    });

    it('should go to previous step', () => {
      component.currentStep = 2;
      component.previousStep();

      expect(component.currentStep).toBe(1);
    });

    it('should not go below step 0', () => {
      component.currentStep = 0;
      component.previousStep();

      expect(component.currentStep).toBe(0);
    });
  });

  describe('Error Handling', () => {
    it('should handle medication loading errors', (done) => {
      medicationService.getActiveOrdersForPatient.and.returnValue(
        throwError(() => new Error('Failed to load'))
      );

      component.ngOnInit();

      setTimeout(() => {
        expect(toastService.error).toHaveBeenCalled();
        done();
      }, 100);
    });
  });

  describe('Component Cleanup', () => {
    it('should unsubscribe on destroy', () => {
      spyOn(component['destroy$'], 'next');
      spyOn(component['destroy$'], 'complete');

      component.ngOnDestroy();

      expect(component['destroy$'].next).toHaveBeenCalled();
      expect(component['destroy$'].complete).toHaveBeenCalled();
    });
  });
});
