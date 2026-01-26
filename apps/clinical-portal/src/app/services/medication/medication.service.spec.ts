/**
 * Medication Service - Unit Tests
 *
 * Test-Driven Development approach: Tests define the contract
 * that the service implementation must satisfy.
 *
 * Coverage areas:
 * - Tenant context management and header validation
 * - Medication catalog operations (CRUD)
 * - Medication order lifecycle (create, update, cancel)
 * - Pharmacy fulfillment tracking
 * - Adverse event/allergy management
 * - Medication administration records
 * - Interaction checking and drug interaction detection
 * - Metrics and adherence tracking
 * - Error handling and edge cases
 * - Caching behavior with TTL
 */

import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import {
  MedicationService,
  MEDICATION_BASE_URL,
} from './medication.service';
import {
  Medication,
  MedicationOrder,
  PharmacyFulfillment,
  MedicationAdverseEvent,
  MedicationAdministration,
  MedicationInteractionCheck,
  MedicationAdherenceMetrics,
  MedicationTherapyMetrics,
  Pharmacy,
  PharmacyCoordination,
  MedicationContext,
  PaginatedResponse,
  MedicationForm,
  DosageUnit,
  MedicationRoute,
  MedicationFrequency,
  PrescriptionStatus,
  PriorityLevel,
  FulfillmentStatus,
  AdverseEventSeverity,
  AdverseEventType,
  AdministrationStatus,
  InteractionSeverity,
} from './medication.models';

describe('MedicationService', () => {
  let service: MedicationService;
  let httpMock: HttpTestingController;

  const tenantId = 'TENANT001';
  const patientId = '550e8400-e29b-41d4-a716-446655440000';
  const clinicianId = '660e8400-e29b-41d4-a716-446655440001';

  // Mock data
  const mockMedication: Medication = {
    id: 'med-001',
    tenantId,
    name: 'Metformin',
    genericName: 'Metformin Hydrochloride',
    therapeuticClass: 'Antidiabetic',
    form: MedicationForm.TABLET,
    strength: '500',
    strengthUnit: DosageUnit.MG,
    ndc: '0378-0076-99',
    rxNorm: '6809',
    manufacturer: 'Merck',
    activeIngredients: ['Metformin HCl'],
    isControlledSubstance: false,
  };

  const mockMedicationOrder: MedicationOrder = {
    id: 'order-001',
    patientId,
    tenantId,
    prescriberId: clinicianId,
    medicationId: 'med-001',
    dosage: 500,
    dosageUnit: DosageUnit.MG,
    route: MedicationRoute.ORAL,
    frequency: MedicationFrequency.TWICE_DAILY,
    startDate: new Date('2024-01-01'),
    quantity: 60,
    refills: 5,
    prescriptionStatus: PrescriptionStatus.FILLED,
    priorityLevel: PriorityLevel.ROUTINE,
    indication: 'Type 2 Diabetes',
  };

  const mockPharmacyFulfillment: PharmacyFulfillment = {
    id: 'fulfill-001',
    prescriptionId: 'order-001',
    tenantId,
    pharmacyId: 'pharm-001',
    pharmacyName: 'CVS Pharmacy',
    fulfillmentStatus: FulfillmentStatus.READY_FOR_PICKUP,
    quantityRequested: 60,
    quantityFulfilled: 60,
    estimatedReadyDate: new Date('2024-01-05'),
    actualReadyDate: new Date('2024-01-04'),
  };

  const mockAdverseEvent: MedicationAdverseEvent = {
    id: 'adverse-001',
    patientId,
    tenantId,
    medicationId: 'med-001',
    medicationName: 'Metformin',
    eventType: AdverseEventType.ALLERGY,
    severity: AdverseEventSeverity.MODERATE,
    reactionDescription: 'Nausea and gastrointestinal upset',
    onsetDate: new Date('2024-01-15'),
    verificationStatus: 'VERIFIED',
  };

  const mockAdministration: MedicationAdministration = {
    id: 'admin-001',
    orderId: 'order-001',
    patientId,
    tenantId,
    administeredBy: clinicianId,
    status: AdministrationStatus.ADMINISTERED,
    scheduledTime: new Date('2024-01-10T08:00:00'),
    administeredTime: new Date('2024-01-10T08:15:00'),
    dosageAdministered: 500,
    dosageUnit: DosageUnit.MG,
    route: MedicationRoute.ORAL,
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MedicationService],
    });
    service = TestBed.inject(MedicationService);
    httpMock = TestBed.inject(HttpTestingController);
    service.setTenantContext(tenantId);
  });

  afterEach(() => {
    httpMock.verify();
  });

  // ==================== Tenant Context Tests ====================

  describe('Tenant Context Management', () => {
    it('should set and retrieve tenant context', () => {
      service.setTenantContext('NEW_TENANT');
      expect(service.getTenantContext()).toBe('NEW_TENANT');
    });

    it('should throw error when making requests without tenant context', () => {
      const unsetService = new MedicationService(TestBed.inject(HttpClientTestingModule) as any);
      expect(() =>
        unsetService.getMedicationById('med-001').subscribe()
      ).toThrowError('Tenant context not set');
    });
  });

  // ==================== Medication Catalog Tests ====================

  describe('Medication Catalog Operations', () => {
    it('should create medication in catalog', (done) => {
      service.createMedication(mockMedication).subscribe((result) => {
        expect(result.id).toBeDefined();
        expect(result.name).toBe('Metformin');
        expect(result.tenantId).toBe(tenantId);
        done();
      }, 30000);

      const req = httpMock.expectOne((r) =>
        r.url.includes('medications') && r.method === 'POST'
      );
      expect(req.request.headers.get('X-Tenant-ID')).toBe(tenantId);
      req.flush(mockMedication);
    });

    it('should retrieve medication by ID', (done) => {
      service.getMedicationById('med-001').subscribe((result) => {
        expect(result.id).toBe('med-001');
        expect(result.name).toBe('Metformin');
        done();
      }, 30000);

      const req = httpMock.expectOne((r) =>
        r.url.includes('medications/med-001')
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockMedication);
    });

    it('should search medications by name', (done) => {
      const mockResponse: PaginatedResponse<Medication> = {
        content: [mockMedication],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.searchMedications('Metformin', 0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        expect(result.content[0].name).toBe('Metformin');
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('medications/search')
      );
      expect(req.request.params.get('query')).toBe('Metformin');
      req.flush(mockResponse);
    });

    it('should get medications by therapeutic class', (done) => {
      const mockResponse: PaginatedResponse<Medication> = {
        content: [mockMedication],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service
        .getMedicationsByTherapeuticClass('Antidiabetic', 0, 10)
        .subscribe((result) => {
          expect(result.content[0].therapeuticClass).toBe('Antidiabetic');
          done();
        });

      const req = httpMock.expectOne((r) =>
        r.url.includes('medications/class')
      );
      req.flush(mockResponse);
    });

    it('should update medication in catalog', (done) => {
      const updated = { ...mockMedication, strength: '750' };
      service.updateMedication('med-001', updated).subscribe((result) => {
        expect(result.strength).toBe('750');
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('medications/med-001') && r.method === 'PUT'
      );
      req.flush(updated);
    });
  });

  // ==================== Medication Order Tests ====================

  describe('Medication Order Lifecycle', () => {
    it('should create medication order', (done) => {
      service.createMedicationOrder(mockMedicationOrder).subscribe((result) => {
        expect(result.id).toBeDefined();
        expect(result.patientId).toBe(patientId);
        expect(result.prescriptionStatus).toBe(PrescriptionStatus.FILLED);
        done();
      }, 30000);

      const req = httpMock.expectOne((r) =>
        r.url.includes('orders') && r.method === 'POST'
      );
      expect(req.request.headers.get('X-Tenant-ID')).toBe(tenantId);
      req.flush(mockMedicationOrder);
    });

    it('should retrieve medication order by ID', (done) => {
      service.getMedicationOrderById('order-001').subscribe((result) => {
        expect(result.id).toBe('order-001');
        expect(result.medicationId).toBe('med-001');
        done();
      }, 30000);

      const req = httpMock.expectOne((r) =>
        r.url.includes('orders/order-001')
      );
      req.flush(mockMedicationOrder);
    });

    it('should get active orders for patient', (done) => {
      const mockResponse: PaginatedResponse<MedicationOrder> = {
        content: [mockMedicationOrder],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getActiveOrdersForPatient(patientId, 0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        expect(result.content[0].patientId).toBe(patientId);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('orders/patient') && r.url.includes('active')
      );
      req.flush(mockResponse);
    });

    it('should get pending orders awaiting pharmacy', (done) => {
      const mockResponse: PaginatedResponse<MedicationOrder> = {
        content: [{ ...mockMedicationOrder, prescriptionStatus: PrescriptionStatus.SENT_TO_PHARMACY }],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getPendingOrdersAwaitingPharmacy(0, 10).subscribe((result) => {
        expect(result.content[0].prescriptionStatus).toBe(PrescriptionStatus.SENT_TO_PHARMACY);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('orders/pending')
      );
      req.flush(mockResponse);
    });

    it('should update medication order', (done) => {
      const updated = { ...mockMedicationOrder, quantity: 90 };
      service.updateMedicationOrder('order-001', updated).subscribe((result) => {
        expect(result.quantity).toBe(90);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('orders/order-001') && r.method === 'PUT'
      );
      req.flush(updated);
    });

    it('should refill medication order', (done) => {
      service.refillMedicationOrder('order-001').subscribe((result) => {
        expect(result.refillsRemaining).toBeLessThan(mockMedicationOrder.refills!);
        done();
      }, 30000);

      const req = httpMock.expectOne((r) =>
        r.url.includes('orders/order-001/refill')
      );
      req.flush({ ...mockMedicationOrder, refillsRemaining: 4 });
    });

    it('should cancel medication order', (done) => {
      service.cancelMedicationOrder('order-001', 'Patient request').subscribe((result) => {
        expect(result.prescriptionStatus).toBe(PrescriptionStatus.CANCELLED);
        done();
      }, 30000);

      const req = httpMock.expectOne((r) =>
        r.url.includes('orders/order-001/cancel')
      );
      req.flush({ ...mockMedicationOrder, prescriptionStatus: PrescriptionStatus.CANCELLED });
    });

    it('should send order to pharmacy', (done) => {
      service.sendOrderToPharmacy('order-001').subscribe((result) => {
        expect(result.prescriptionStatus).toBe(PrescriptionStatus.SENT_TO_PHARMACY);
        done();
      }, 30000);

      const req = httpMock.expectOne((r) =>
        r.url.includes('orders/order-001/send-pharmacy')
      );
      req.flush({ ...mockMedicationOrder, prescriptionStatus: PrescriptionStatus.SENT_TO_PHARMACY });
    });
  });

  // ==================== Pharmacy Fulfillment Tests ====================

  describe('Pharmacy Fulfillment Tracking', () => {
    it('should get fulfillment status for order', (done) => {
      service.getFulfillmentStatus('order-001').subscribe((result) => {
        expect(result.prescriptionId).toBe('order-001');
        expect(result.fulfillmentStatus).toBe(FulfillmentStatus.READY_FOR_PICKUP);
        done();
      }, 30000);

      const req = httpMock.expectOne((r) =>
        r.url.includes('fulfillments/order-001')
      );
      req.flush(mockPharmacyFulfillment);
    });

    it('should get pending fulfillments', (done) => {
      const mockResponse: PaginatedResponse<PharmacyFulfillment> = {
        content: [mockPharmacyFulfillment],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getPendingFulfillments(0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('fulfillments/pending')
      );
      req.flush(mockResponse);
    });

    it('should track fulfillment ready for pickup', (done) => {
      service.getFulfillmentsReadyForPickup(patientId, 0, 10).subscribe((result) => {
        expect(result.content[0].fulfillmentStatus).toBe(FulfillmentStatus.READY_FOR_PICKUP);
        done();
      }, 30000);

      const req = httpMock.expectOne((r) =>
        r.url.includes('fulfillments/patient') && r.url.includes('ready')
      );
      req.flush({
        content: [mockPharmacyFulfillment],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      });
    });

    it('should update fulfillment status', (done) => {
      const updated = { ...mockPharmacyFulfillment, fulfillmentStatus: FulfillmentStatus.DELIVERED };
      service.updateFulfillmentStatus('fulfill-001', updated).subscribe((result) => {
        expect(result.fulfillmentStatus).toBe(FulfillmentStatus.DELIVERED);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('fulfillments/fulfill-001') && r.method === 'PUT'
      );
      req.flush(updated);
    });
  });

  // ==================== Adverse Event/Allergy Tests ====================

  describe('Adverse Event Management', () => {
    it('should record medication adverse event', (done) => {
      service.recordAdverseEvent(mockAdverseEvent).subscribe((result) => {
        expect(result.id).toBeDefined();
        expect(result.eventType).toBe(AdverseEventType.ALLERGY);
        expect(result.severity).toBe(AdverseEventSeverity.MODERATE);
        done();
      }, 30000);

      const req = httpMock.expectOne((r) =>
        r.url.includes('adverse-events') && r.method === 'POST'
      );
      expect(req.request.headers.get('X-Tenant-ID')).toBe(tenantId);
      req.flush(mockAdverseEvent);
    });

    it('should retrieve patient allergies', (done) => {
      const mockResponse: PaginatedResponse<MedicationAdverseEvent> = {
        content: [mockAdverseEvent],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getPatientAllergies(patientId, 0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        expect(result.content[0].eventType).toBe(AdverseEventType.ALLERGY);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('adverse-events/patient') && r.url.includes('allergies')
      );
      req.flush(mockResponse);
    });

    it('should retrieve patient adverse events', (done) => {
      const mockResponse: PaginatedResponse<MedicationAdverseEvent> = {
        content: [mockAdverseEvent],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getPatientAdverseEvents(patientId, 0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('adverse-events/patient')
      );
      req.flush(mockResponse);
    });

    it('should update adverse event', (done) => {
      const updated = { ...mockAdverseEvent, verificationStatus: 'VERIFIED' as const };
      service.updateAdverseEvent('adverse-001', updated).subscribe((result) => {
        expect(result.verificationStatus).toBe('VERIFIED');
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('adverse-events/adverse-001') && r.method === 'PUT'
      );
      req.flush(updated);
    });
  });

  // ==================== Medication Administration Tests ====================

  describe('Medication Administration', () => {
    it('should record medication administration', (done) => {
      service.recordMedicationAdministration(mockAdministration).subscribe((result) => {
        expect(result.id).toBeDefined();
        expect(result.status).toBe(AdministrationStatus.ADMINISTERED);
        expect(result.administeredTime).toBeDefined();
        done();
      }, 30000);

      const req = httpMock.expectOne((r) =>
        r.url.includes('administration') && r.method === 'POST'
      );
      expect(req.request.headers.get('X-Tenant-ID')).toBe(tenantId);
      req.flush(mockAdministration);
    });

    it('should get scheduled administrations for patient', (done) => {
      const mockResponse: PaginatedResponse<MedicationAdministration> = {
        content: [mockAdministration],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getScheduledAdministrations(patientId, 0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('administration/patient') && r.url.includes('scheduled')
      );
      req.flush(mockResponse);
    });

    it('should get administered medications for today', (done) => {
      const mockResponse: MedicationAdministration[] = [mockAdministration];

      service.getAdministeredMedicationsForToday(patientId).subscribe((result) => {
        expect(result.length).toBeGreaterThan(0);
        expect(result[0].status).toBe(AdministrationStatus.ADMINISTERED);
        done();
      }, 30000);

      const req = httpMock.expectOne((r) =>
        r.url.includes('administration/patient') && r.url.includes('today')
      );
      req.flush(mockResponse);
    });

    it('should update administration status', (done) => {
      const updated = { ...mockAdministration, status: AdministrationStatus.NOT_ADMINISTERED, reasonNotAdministered: 'Patient refused' };
      service.updateAdministrationStatus('admin-001', updated).subscribe((result) => {
        expect(result.status).toBe(AdministrationStatus.NOT_ADMINISTERED);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('administration/admin-001') && r.method === 'PUT'
      );
      req.flush(updated);
    });
  });

  // ==================== Drug Interaction Tests ====================

  describe('Drug Interaction Checking', () => {
    it('should check drug interactions for new medication', (done) => {
      const mockInteractionCheck: MedicationInteractionCheck = {
        patientId,
        tenantId,
        currentMedications: ['med-001'],
        newMedicationId: 'med-002',
        interactions: [],
        hasSignificantInteractions: false,
      };

      service.checkDrugInteractions(patientId, ['med-001'], 'med-002').subscribe((result) => {
        expect(result.patientId).toBe(patientId);
        expect(result.hasSignificantInteractions).toBe(false);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('interactions/check')
      );
      expect(req.request.body.currentMedications).toContain('med-001');
      expect(req.request.body.newMedicationId).toBe('med-002');
      req.flush(mockInteractionCheck);
    });

    it('should detect significant drug interactions', (done) => {
      const mockInteractionCheck: MedicationInteractionCheck = {
        patientId,
        tenantId,
        currentMedications: ['med-001'],
        newMedicationId: 'med-003',
        interactions: [
          {
            medicationId1: 'med-001',
            medicationId2: 'med-003',
            severity: InteractionSeverity.MAJOR,
            description: 'Significant interaction detected',
          },
        ],
        hasSignificantInteractions: true,
        recommendations: 'Do not combine or use alternative medication',
      };

      service.checkDrugInteractions(patientId, ['med-001'], 'med-003').subscribe((result) => {
        expect(result.hasSignificantInteractions).toBe(true);
        expect(result.interactions.length).toBeGreaterThan(0);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('interactions/check')
      );
      req.flush(mockInteractionCheck);
    });
  });

  // ==================== Metrics Tests ====================

  describe('Medication Metrics', () => {
    it('should get medication adherence metrics', (done) => {
      const mockMetrics: MedicationAdherenceMetrics = {
        patientId,
        totalOrders: 10,
        filledOrders: 9,
        adherenceRate: 90,
        missedDoses: 1,
        refillsOnTime: 8,
        refillsLate: 1,
        refillsEarly: 0,
      };

      service.getMedicationAdherenceMetrics(patientId).subscribe((result) => {
        expect(result.adherenceRate).toBe(90);
        expect(result.filledOrders).toBe(9);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('metrics/adherence')
      );
      req.flush(mockMetrics);
    });

    it('should get medication therapy metrics', (done) => {
      const mockMetrics: MedicationTherapyMetrics = {
        patientId,
        totalActiveMedications: 5,
        highRiskMedications: 1,
        potentialDrugInteractions: 0,
        medicationsWithDuplicateTherapy: 0,
        medicationsAwaitingReview: 1,
      };

      service.getMedicationTherapyMetrics(patientId).subscribe((result) => {
        expect(result.totalActiveMedications).toBe(5);
        expect(result.highRiskMedications).toBe(1);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('metrics/therapy')
      );
      req.flush(mockMetrics);
    });
  });

  // ==================== Pharmacy Management Tests ====================

  describe('Pharmacy Management', () => {
    it('should retrieve pharmacy information', (done) => {
      const mockPharmacy: Pharmacy = {
        id: 'pharm-001',
        tenantId,
        name: 'CVS Pharmacy',
        type: 'RETAIL',
        npi: '1234567890',
        accepts24HourOrders: true,
        acceptsDelivery: true,
        isPreferred: true,
      };

      service.getPharmacy('pharm-001').subscribe((result) => {
        expect(result.name).toBe('CVS Pharmacy');
        expect(result.accepts24HourOrders).toBe(true);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('pharmacies/pharm-001')
      );
      req.flush(mockPharmacy);
    });

    it('should get preferred pharmacies for tenant', (done) => {
      const mockPharmacyList = [
        {
          id: 'pharm-001',
          name: 'CVS Pharmacy',
          type: 'RETAIL',
          isPreferred: true,
        },
      ];

      service.getPreferredPharmacies(0, 10).subscribe((result) => {
        expect(result.content.length).toBeGreaterThan(0);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('pharmacies/preferred')
      );
      req.flush({
        content: mockPharmacyList,
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      });
    });

    it('should create pharmacy coordination request', (done) => {
      const mockCoordination: PharmacyCoordination = {
        id: 'coord-001',
        prescriptionId: 'order-001',
        patientId,
        tenantId,
        coordinatorId: clinicianId,
        pharmacyId: 'pharm-001',
        status: 'PENDING',
        requestType: 'PRIOR_AUTH',
        requestDetails: 'Prior authorization for specialty medication',
      };

      service.createPharmacyCoordination(mockCoordination).subscribe((result) => {
        expect(result.id).toBeDefined();
        expect(result.status).toBe('PENDING');
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('pharmacy-coordination') && r.method === 'POST'
      );
      expect(req.request.headers.get('X-Tenant-ID')).toBe(tenantId);
      req.flush(mockCoordination);
    });

    it('should get active pharmacy coordinations', (done) => {
      const mockCoordination: PharmacyCoordination = {
        id: 'coord-001',
        prescriptionId: 'order-001',
        patientId,
        tenantId,
        coordinatorId: clinicianId,
        pharmacyId: 'pharm-001',
        status: 'IN_PROGRESS',
        requestType: 'PRIOR_AUTH',
      };

      const mockResponse: PaginatedResponse<PharmacyCoordination> = {
        content: [mockCoordination],
        totalElements: 1,
        totalPages: 1,
        currentPage: 0,
        pageSize: 10,
        hasNext: false,
        hasPrevious: false,
      };

      service.getActivePharmacyCoordinations(0, 10).subscribe((result) => {
        expect(result.content.length).toBe(1);
        done();
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('pharmacy-coordination/active')
      );
      req.flush(mockResponse);
    });
  });

  // ==================== Error Handling Tests ====================

  describe('Error Handling', () => {
    it('should handle 404 errors gracefully', (done) => {
      service.getMedicationById('nonexistent').subscribe(
        () => fail('should not succeed'),
        (error) => {
          expect(error.status).toBe(404);
          expect(error.context).toBe('getMedicationById');
          done();
        }
      );

      const req = httpMock.expectOne((r) =>
        r.url.includes('medications/nonexistent')
      );
      req.flush('Not found', { status: 404, statusText: 'Not Found' });
    });

    it('should handle 401 Unauthorized errors', (done) => {
      service.createMedicationOrder(mockMedicationOrder).subscribe(
        () => fail('should not succeed'),
        (error) => {
          expect(error.status).toBe(401);
          done();
        }
      );

      const req = httpMock.expectOne((r) =>
        r.url.includes('orders') && r.method === 'POST'
      );
      req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    });

    it('should handle 500 server errors', (done) => {
      service.getMedicationAdherenceMetrics(patientId).subscribe(
        () => fail('should not succeed'),
        (error) => {
          expect(error.status).toBe(500);
          done();
        }
      );

      const req = httpMock.expectOne((r) =>
        r.url.includes('metrics/adherence')
      );
      req.flush('Internal Server Error', {
        status: 500,
        statusText: 'Internal Server Error',
      });
    });
  });

  // ==================== Caching Tests ====================

  describe('Caching Behavior', () => {
    it('should cache medication catalog results with TTL', (done) => {
      service.getMedicationById('med-001').subscribe(() => {
        // Second call should use cache
        service.getMedicationById('med-001').subscribe(() => {
          // Verify only one HTTP call was made
          const requests = httpMock.match((r) =>
            r.url.includes('medications/med-001')
          );
          expect(requests.length).toBe(1);
          done();
        }, 30000);
      });

      const req = httpMock.expectOne((r) =>
        r.url.includes('medications/med-001')
      );
      req.flush(mockMedication);
    });

    it('should invalidate cache when order is created', (done) => {
      service.createMedicationOrder(mockMedicationOrder).subscribe(() => {
        // Cache should be invalidated, next fetch should hit backend
        service.getActiveOrdersForPatient(patientId, 0, 10).subscribe(() => {
          done();
        }, 30000);

        const secondReq = httpMock.expectOne((r) =>
          r.url.includes('orders/patient')
        );
        secondReq.flush({
          content: [mockMedicationOrder],
          totalElements: 1,
          totalPages: 1,
          currentPage: 0,
          pageSize: 10,
          hasNext: false,
          hasPrevious: false,
        });
      });

      const firstReq = httpMock.expectOne((r) =>
        r.url.includes('orders') && r.method === 'POST'
      );
      firstReq.flush(mockMedicationOrder);
    });

    it('should invalidate cache when adverse event is recorded', (done) => {
      service.recordAdverseEvent(mockAdverseEvent).subscribe(() => {
        // Cache should be invalidated
        service.getPatientAllergies(patientId, 0, 10).subscribe(() => {
          done();
        }, 30000);

        const secondReq = httpMock.expectOne((r) =>
          r.url.includes('adverse-events/patient')
        );
        secondReq.flush({
          content: [mockAdverseEvent],
          totalElements: 1,
          totalPages: 1,
          currentPage: 0,
          pageSize: 10,
          hasNext: false,
          hasPrevious: false,
        });
      });

      const firstReq = httpMock.expectOne((r) =>
        r.url.includes('adverse-events') && r.method === 'POST'
      );
      firstReq.flush(mockAdverseEvent);
    });
  });
});
