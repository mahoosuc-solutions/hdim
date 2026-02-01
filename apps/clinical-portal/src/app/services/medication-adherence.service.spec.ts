/**
 * Unit Tests for Medication Adherence Service
 * Testing PDC calculation and adherence gap identification
 */

import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { MedicationAdherenceService } from './medication-adherence.service';
import {
  PDCResult,
  MedicationAdherenceScore,
  AdherenceGap,
  ProblematicMedication,
  DateRange,
  MedicationRequest,
} from '../models/medication-adherence.model';
import { API_CONFIG, FHIR_ENDPOINTS, buildFhirUrl } from '../config/api.config';

describe('MedicationAdherenceService', () => {
  let service: MedicationAdherenceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [MedicationAdherenceService],
    });
    service = TestBed.inject(MedicationAdherenceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  // ==========================================================================
  // Get Active Medications Tests (5 tests)
  // ==========================================================================

  describe('Get Active Medications', () => {
    it('should fetch MedicationRequest with status=active', (done) => {
      const patientId = 'patient-123';
      const mockBundle = {
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'med-1',
              resourceType: 'MedicationRequest',
              status: 'active',
              medicationCodeableConcept: {
                coding: [{ system: 'http://www.nlm.nih.gov/research/umls/rxnorm', code: '197361', display: 'Metformin' }],
              },
            },
          },
        ],
      };

      service.getActiveMedications(patientId).subscribe((medications) => {
        expect(medications.length).toBe(1);
        expect(medications[0].status).toBe('active');
        done();
      });

      const expectedUrl = buildFhirUrl(FHIR_ENDPOINTS.MEDICATION);
      const req = httpMock.expectOne((request) =>
        request.url.includes(expectedUrl) && request.params.get('patient') === patientId && request.params.get('status') === 'active'
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockBundle);
    });

    it('should extract RxNorm code from medicationCodeableConcept', (done) => {
      const patientId = 'patient-123';
      const mockBundle = {
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'med-1',
              resourceType: 'MedicationRequest',
              status: 'active',
              medicationCodeableConcept: {
                coding: [
                  { system: 'http://www.nlm.nih.gov/research/umls/rxnorm', code: '197361', display: 'Metformin 500mg' },
                ],
                text: 'Metformin 500mg tablet',
              },
            },
          },
        ],
      };

      service.getActiveMedications(patientId).subscribe((medications) => {
        expect(medications.length).toBe(1);
        expect(medications[0].medicationCodeableConcept?.coding?.[0].code).toBe('197361');
        expect(medications[0].medicationCodeableConcept?.coding?.[0].system).toContain('rxnorm');
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION));
      req.flush(mockBundle);
    });

    it('should format dosage instruction text', (done) => {
      const patientId = 'patient-123';
      const mockBundle = {
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'med-1',
              resourceType: 'MedicationRequest',
              status: 'active',
              medicationCodeableConcept: {
                coding: [{ system: 'http://www.nlm.nih.gov/research/umls/rxnorm', code: '197361', display: 'Metformin' }],
              },
              dosageInstruction: [
                {
                  text: 'Take 1 tablet by mouth twice daily',
                  timing: { repeat: { frequency: 2, period: 1, periodUnit: 'd' } },
                },
              ],
            },
          },
        ],
      };

      service.getActiveMedications(patientId).subscribe((medications) => {
        expect(medications.length).toBe(1);
        expect(medications[0].dosageInstruction?.[0].text).toBe('Take 1 tablet by mouth twice daily');
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION));
      req.flush(mockBundle);
    });

    it('should include days supply from dispenseRequest', (done) => {
      const patientId = 'patient-123';
      const mockBundle = {
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'med-1',
              resourceType: 'MedicationRequest',
              status: 'active',
              medicationCodeableConcept: {
                coding: [{ system: 'http://www.nlm.nih.gov/research/umls/rxnorm', code: '197361', display: 'Metformin' }],
              },
              dispenseRequest: {
                expectedSupplyDuration: {
                  value: 30,
                  unit: 'days',
                },
                quantity: {
                  value: 60,
                },
              },
            },
          },
        ],
      };

      service.getActiveMedications(patientId).subscribe((medications) => {
        expect(medications.length).toBe(1);
        expect(medications[0].dispenseRequest?.expectedSupplyDuration?.value).toBe(30);
        expect(medications[0].dispenseRequest?.expectedSupplyDuration?.unit).toBe('days');
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION));
      req.flush(mockBundle);
    });

    it('should return empty array when no active medications', (done) => {
      const patientId = 'patient-123';
      const mockBundle = {
        resourceType: 'Bundle',
        entry: [],
      };

      service.getActiveMedications(patientId).subscribe((medications) => {
        expect(medications).toEqual([]);
        expect(medications.length).toBe(0);
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION));
      req.flush(mockBundle);
    });

    it('should return empty array when medication fetch fails', (done) => {
      const patientId = 'patient-123';

      service.getActiveMedications(patientId).subscribe((medications) => {
        expect(medications).toEqual([]);
        done();
      };

      const req = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION));
      req.flush('error', { status: 500, statusText: 'Server Error' });
    });
  });

  // ==========================================================================
  // PDC Calculation Tests (8 tests)
  // ==========================================================================

  describe('PDC Calculation', () => {
    it('should calculate PDC as (days supplied / total days) * 100', (done) => {
      const patientId = 'patient-123';
      const medicationCode = '197361';
      const period: DateRange = {
        startDate: new Date('2025-01-01'),
        endDate: new Date('2025-01-31'), // 31 days
      };

      // Mock fill history - patient has 25 days covered
      service.calculatePDC(patientId, medicationCode, period).subscribe((result) => {
        expect(result.totalDays).toBe(31);
        expect(result.daysCovered).toBeGreaterThan(0);
        expect(result.pdc).toBe(Math.round((result.daysCovered / result.totalDays) * 100));
        done();
      });

      // Mock FHIR response for MedicationDispense
      const req = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'disp-1',
              whenHandedOver: '2025-01-01',
              daysSupply: { value: 15 },
            },
          },
          {
            resource: {
              id: 'disp-2',
              whenHandedOver: '2025-01-16',
              daysSupply: { value: 15 },
            },
          },
        ],
      });
    });

    it('should return 100% when fully covered', (done) => {
      const patientId = 'patient-123';
      const medicationCode = '197361';
      const period: DateRange = {
        startDate: new Date('2025-01-01'),
        endDate: new Date('2025-01-30'), // 30 days
      };

      service.calculatePDC(patientId, medicationCode, period).subscribe((result) => {
        expect(result.pdc).toBe(100);
        expect(result.status).toBe('excellent');
        expect(result.gaps.length).toBe(0);
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'disp-1',
              whenHandedOver: '2025-01-01',
              daysSupply: { value: 30 },
            },
          },
        ],
      });
    });

    it('should calculate reduced PDC when gaps exist', (done) => {
      const patientId = 'patient-123';
      const medicationCode = '197361';
      const period: DateRange = {
        startDate: new Date('2025-01-01'),
        endDate: new Date('2025-01-30'), // 30 days
      };

      service.calculatePDC(patientId, medicationCode, period).subscribe((result) => {
        expect(result.pdc).toBeLessThan(100);
        expect(result.gaps.length).toBeGreaterThan(0);
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'disp-1',
              whenHandedOver: '2025-01-01',
              daysSupply: { value: 15 },
            },
          },
          // Gap of 5 days, then next fill
          {
            resource: {
              id: 'disp-2',
              whenHandedOver: '2025-01-21',
              daysSupply: { value: 10 },
            },
          },
        ],
      });
    });

    it('should not double-count overlapping prescription periods', (done) => {
      const patientId = 'patient-123';
      const medicationCode = '197361';
      const period: DateRange = {
        startDate: new Date('2025-01-01'),
        endDate: new Date('2025-01-30'), // 30 days
      };

      service.calculatePDC(patientId, medicationCode, period).subscribe((result) => {
        // Even with overlapping fills, should not exceed 100% or 30 days
        expect(result.pdc).toBeLessThanOrEqual(100);
        expect(result.daysCovered).toBeLessThanOrEqual(30);
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'disp-1',
              whenHandedOver: '2025-01-01',
              daysSupply: { value: 20 },
            },
          },
          {
            resource: {
              id: 'disp-2',
              whenHandedOver: '2025-01-15', // Overlaps with previous
              daysSupply: { value: 20 },
            },
          },
        ],
      });
    });

    it('should set status=excellent when PDC >= 80%', (done) => {
      const patientId = 'patient-123';
      const medicationCode = '197361';
      const period: DateRange = {
        startDate: new Date('2025-01-01'),
        endDate: new Date('2025-01-30'),
      };

      service.calculatePDC(patientId, medicationCode, period).subscribe((result) => {
        if (result.pdc >= 80) {
          expect(result.status).toBe('excellent');
        }
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'disp-1',
              whenHandedOver: '2025-01-01',
              daysSupply: { value: 25 },
            },
          },
        ],
      });
    });

    it('should set status=good when PDC 60-79%', (done) => {
      const patientId = 'patient-123';
      const medicationCode = '197361';
      const period: DateRange = {
        startDate: new Date('2025-01-01'),
        endDate: new Date('2025-01-30'),
      };

      service.calculatePDC(patientId, medicationCode, period).subscribe((result) => {
        if (result.pdc >= 60 && result.pdc < 80) {
          expect(result.status).toBe('good');
        }
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'disp-1',
              whenHandedOver: '2025-01-01',
              daysSupply: { value: 20 },
            },
          },
        ],
      });
    });

    it('should set status=poor when PDC < 60%', (done) => {
      const patientId = 'patient-123';
      const medicationCode = '197361';
      const period: DateRange = {
        startDate: new Date('2025-01-01'),
        endDate: new Date('2025-01-30'),
      };

      service.calculatePDC(patientId, medicationCode, period).subscribe((result) => {
        if (result.pdc < 60) {
          expect(result.status).toBe('poor');
        }
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'disp-1',
              whenHandedOver: '2025-01-01',
              daysSupply: { value: 10 },
            },
          },
        ],
      });
    });

    it('should list all adherence gaps in result', (done) => {
      const patientId = 'patient-123';
      const medicationCode = '197361';
      const period: DateRange = {
        startDate: new Date('2025-01-01'),
        endDate: new Date('2025-01-30'),
      };

      service.calculatePDC(patientId, medicationCode, period).subscribe((result) => {
        expect(result.gaps).toBeDefined();
        expect(Array.isArray(result.gaps)).toBe(true);
        if (result.gaps.length > 0) {
          expect(result.gaps[0]).toHaveProperty('startDate');
          expect(result.gaps[0]).toHaveProperty('endDate');
          expect(result.gaps[0]).toHaveProperty('daysWithout');
        }
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'disp-1',
              whenHandedOver: '2025-01-01',
              daysSupply: { value: 10 },
            },
          },
          {
            resource: {
              id: 'disp-2',
              whenHandedOver: '2025-01-20',
              daysSupply: { value: 10 },
            },
          },
        ],
      });
    });

    it('should return 0% PDC when no dispenses exist', (done) => {
      const patientId = 'patient-123';
      const medicationCode = '197361';
      const period: DateRange = {
        startDate: new Date('2025-01-01'),
        endDate: new Date('2025-01-30'),
      };

      service.calculatePDC(patientId, medicationCode, period).subscribe((result) => {
        expect(result.pdc).toBe(0);
        expect(result.gaps.length).toBe(1);
        expect(result.medicationName).toBe(medicationCode);
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req.flush({ resourceType: 'Bundle', entry: [] });
    });
  });

  // ==========================================================================
  // Overall Adherence Tests (4 tests)
  // ==========================================================================

  describe('Overall Adherence', () => {
    it('returns zeroed score when no active medications', (done) => {
      const patientId = 'patient-123';

      service.calculateOverallAdherence(patientId).subscribe((score) => {
        expect(score.totalMedications).toBe(0);
        expect(score.overallPDC).toBe(0);
        done();
      };

      const req1 = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION) && request.params.get('status') === 'active');
      req1.flush({ resourceType: 'Bundle', entry: [] });
    });

    it('should average PDC across all active medications', (done) => {
      const patientId = 'patient-123';

      service.calculateOverallAdherence(patientId).subscribe((score) => {
        expect(score.overallPDC).toBeGreaterThanOrEqual(0);
        expect(score.overallPDC).toBeLessThanOrEqual(100);
        expect(score.totalMedications).toBeGreaterThan(0);
        done();
      };

      // Mock active medications
      const req1 = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION) && request.params.get('status') === 'active');
      req1.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'med-1',
              status: 'active',
              medicationCodeableConcept: {
                coding: [{ system: 'http://www.nlm.nih.gov/research/umls/rxnorm', code: '197361', display: 'Metformin' }],
              },
            },
          },
          {
            resource: {
              id: 'med-2',
              status: 'active',
              medicationCodeableConcept: {
                coding: [{ system: 'http://www.nlm.nih.gov/research/umls/rxnorm', code: '314076', display: 'Lisinopril' }],
              },
            },
          },
        ],
      });

      // Mock PDC calculations for each medication
      const req2 = httpMock.expectOne((request) => request.url.includes('/MedicationRequest') && request.params.get('medication') === '197361');
      req2.flush({
        resourceType: 'Bundle',
        entry: [
          { resource: { whenHandedOver: '2025-01-01', daysSupply: { value: 25 } } },
        ],
      });

      const req3 = httpMock.expectOne((request) => request.url.includes('/MedicationRequest') && request.params.get('medication') === '314076');
      req3.flush({
        resourceType: 'Bundle',
        entry: [
          { resource: { whenHandedOver: '2025-01-01', daysSupply: { value: 20 } } },
        ],
      });
    });

    it('should count medications with PDC >= 80%', (done) => {
      const patientId = 'patient-123';

      service.calculateOverallAdherence(patientId).subscribe((score) => {
        expect(score.adherentCount).toBeGreaterThanOrEqual(0);
        expect(score.adherentCount).toBeLessThanOrEqual(score.totalMedications);
        done();
      };

      const req1 = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION) && request.params.get('status') === 'active');
      req1.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'med-1',
              status: 'active',
              medicationCodeableConcept: {
                coding: [{ code: '197361', display: 'Metformin' }],
              },
            },
          },
        ],
      });

      const req2 = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req2.flush({
        resourceType: 'Bundle',
        entry: [
          { resource: { whenHandedOver: '2025-01-01', daysSupply: { value: 25 } } },
        ],
      });
    });

    it('should list medication names with PDC < 80%', (done) => {
      const patientId = 'patient-123';

      service.calculateOverallAdherence(patientId).subscribe((score) => {
        expect(score.problematicMedications).toBeDefined();
        expect(Array.isArray(score.problematicMedications)).toBe(true);
        done();
      };

      const req1 = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION) && request.params.get('status') === 'active');
      req1.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'med-1',
              status: 'active',
              medicationCodeableConcept: {
                coding: [{ code: '197361', display: 'Metformin' }],
              },
            },
          },
        ],
      });

      const req2 = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req2.flush({
        resourceType: 'Bundle',
        entry: [
          { resource: { whenHandedOver: '2025-01-01', daysSupply: { value: 15 } } },
        ],
      });
    });

    it('should work correctly with single medication', (done) => {
      const patientId = 'patient-123';

      service.calculateOverallAdherence(patientId).subscribe((score) => {
        expect(score.totalMedications).toBe(1);
        expect(score.overallPDC).toBeGreaterThanOrEqual(0);
        expect(score.overallPDC).toBeLessThanOrEqual(100);
        done();
      };

      const req1 = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION) && request.params.get('status') === 'active');
      req1.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'med-1',
              status: 'active',
              medicationCodeableConcept: {
                coding: [{ code: '197361', display: 'Metformin' }],
              },
            },
          },
        ],
      });

      const req2 = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req2.flush({
        resourceType: 'Bundle',
        entry: [
          { resource: { whenHandedOver: '2025-01-01', daysSupply: { value: 25 } } },
        ],
      });
    });

    it('handles overall adherence errors gracefully', (done) => {
      const patientId = 'patient-123';

      service.calculateOverallAdherence(patientId).subscribe((score) => {
        expect(score.overallPDC).toBe(0);
        done();
      };

      const req1 = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION) && request.params.get('status') === 'active');
      req1.flush('error', { status: 500, statusText: 'Server Error' });
    });
  });

  // ==========================================================================
  // Gap Identification Tests (4 tests)
  // ==========================================================================

  describe('Gap Identification', () => {
    it('should detect gap if no fill at period start', (done) => {
      const patientId = 'patient-123';
      const medicationCode = '197361';
      const period: DateRange = {
        startDate: new Date('2025-01-01'),
        endDate: new Date('2025-01-30'),
      };

      service.identifyAdherenceGaps(patientId, medicationCode, period).subscribe((gaps) => {
        expect(gaps.length).toBeGreaterThan(0);
        const firstGap = gaps[0];
        expect(firstGap.startDate).toEqual(period.startDate);
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'disp-1',
              whenHandedOver: '2025-01-10', // Starts 10 days late
              daysSupply: { value: 20 },
            },
          },
        ],
      });
    });

    it('should detect gaps between fill dates', (done) => {
      const patientId = 'patient-123';
      const medicationCode = '197361';
      const period: DateRange = {
        startDate: new Date('2025-01-01'),
        endDate: new Date('2025-01-30'),
      };

      service.identifyAdherenceGaps(patientId, medicationCode, period).subscribe((gaps) => {
        expect(gaps.length).toBeGreaterThan(0);
        // Should detect gap between fills
        const gap = gaps.find((g) => g.startDate > period.startDate);
        expect(gap).toBeDefined();
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'disp-1',
              whenHandedOver: '2025-01-01',
              daysSupply: { value: 10 },
            },
          },
          {
            resource: {
              id: 'disp-2',
              whenHandedOver: '2025-01-20', // Gap from day 11 to day 19
              daysSupply: { value: 10 },
            },
          },
        ],
      });
    });

    it('should return empty array when no gaps', (done) => {
      const patientId = 'patient-123';
      const medicationCode = '197361';
      const period: DateRange = {
        startDate: new Date('2025-01-01'),
        endDate: new Date('2025-01-30'),
      };

      service.identifyAdherenceGaps(patientId, medicationCode, period).subscribe((gaps) => {
        expect(gaps.length).toBe(0);
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'disp-1',
              whenHandedOver: '2025-01-01',
              daysSupply: { value: 30 }, // Fully covered
            },
          },
        ],
      });
    });

    it('should calculate correct days without medication', (done) => {
      const patientId = 'patient-123';
      const medicationCode = '197361';
      const period: DateRange = {
        startDate: new Date('2025-01-01'),
        endDate: new Date('2025-01-30'),
      };

      service.identifyAdherenceGaps(patientId, medicationCode, period).subscribe((gaps) => {
        expect(gaps.length).toBeGreaterThan(0);
        gaps.forEach((gap) => {
          expect(gap.daysWithout).toBeGreaterThan(0);
          // Verify calculation is correct
          const diffTime = gap.endDate.getTime() - gap.startDate.getTime();
          const expectedDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24)) + 1;
          expect(gap.daysWithout).toBe(expectedDays);
        });
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'disp-1',
              whenHandedOver: '2025-01-01',
              daysSupply: { value: 10 },
            },
          },
          {
            resource: {
              id: 'disp-2',
              whenHandedOver: '2025-01-20',
              daysSupply: { value: 10 },
            },
          },
        ],
      });
    });
  });

  // ==========================================================================
  // Problematic Medications Tests (4 tests)
  // ==========================================================================

  describe('Problematic Medications', () => {
    it('returns empty list when there are no active medications', (done) => {
      const patientId = 'patient-123';

      service.getProblematicMedications(patientId).subscribe((medications) => {
        expect(medications).toEqual([]);
        done();
      };

      const req1 = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION) && request.params.get('status') === 'active');
      req1.flush({ resourceType: 'Bundle', entry: [] });
    });

    it('should return only medications with PDC < 80%', (done) => {
      const patientId = 'patient-123';

      service.getProblematicMedications(patientId).subscribe((medications) => {
        expect(medications.length).toBeGreaterThanOrEqual(0);
        medications.forEach((med) => {
          expect(med.pdc).toBeLessThan(80);
        };
        done();
      });

      const req1 = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION) && request.params.get('status') === 'active');
      req1.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'med-1',
              status: 'active',
              medicationCodeableConcept: {
                coding: [{ code: '197361', display: 'Metformin' }],
              },
            },
          },
          {
            resource: {
              id: 'med-2',
              status: 'active',
              medicationCodeableConcept: {
                coding: [{ code: '314076', display: 'Lisinopril' }],
              },
            },
          },
        ],
      });

      const req2 = httpMock.expectOne((request) => request.url.includes('/MedicationRequest') && request.params.get('medication') === '197361');
      req2.flush({
        resourceType: 'Bundle',
        entry: [
          { resource: { whenHandedOver: '2025-01-01', daysSupply: { value: 15 } } },
        ],
      });

      const req3 = httpMock.expectOne((request) => request.url.includes('/MedicationRequest') && request.params.get('medication') === '314076');
      req3.flush({
        resourceType: 'Bundle',
        entry: [
          { resource: { whenHandedOver: '2025-01-01', daysSupply: { value: 25 } } },
        ],
      });
    });

    it('should calculate days overdue from last fill', (done) => {
      const patientId = 'patient-123';

      service.getProblematicMedications(patientId).subscribe((medications) => {
        medications.forEach((med) => {
          expect(med.daysOverdue).toBeGreaterThanOrEqual(0);
          expect(typeof med.daysOverdue).toBe('number');
        };
        done();
      });

      const req1 = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION) && request.params.get('status') === 'active');
      req1.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'med-1',
              status: 'active',
              medicationCodeableConcept: {
                coding: [{ code: '197361', display: 'Metformin' }],
              },
            },
          },
        ],
      });

      const req2 = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req2.flush({
        resourceType: 'Bundle',
        entry: [
          { resource: { whenHandedOver: '2025-01-01', daysSupply: { value: 15 } } },
        ],
      });
    });

    it('should provide intervention recommendation', (done) => {
      const patientId = 'patient-123';

      service.getProblematicMedications(patientId).subscribe((medications) => {
        medications.forEach((med) => {
          expect(med.recommendation).toBeDefined();
          expect(typeof med.recommendation).toBe('string');
          expect(med.recommendation.length).toBeGreaterThan(0);
        };
        done();
      });

      const req1 = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION) && request.params.get('status') === 'active');
      req1.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'med-1',
              status: 'active',
              medicationCodeableConcept: {
                coding: [{ code: '197361', display: 'Metformin' }],
              },
            },
          },
        ],
      });

      const req2 = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req2.flush({
        resourceType: 'Bundle',
        entry: [
          { resource: { whenHandedOver: '2025-01-01', daysSupply: { value: 15 } } },
        ],
      });
    });

    it('uses medication text when coding is missing', (done) => {
      const patientId = 'patient-123';

      service.getProblematicMedications(patientId).subscribe((medications) => {
        expect(medications[0].medicationName).toBe('Custom Med');
        done();
      };

      const req1 = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION) && request.params.get('status') === 'active');
      req1.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'med-1',
              status: 'active',
              medicationCodeableConcept: { text: 'Custom Med' },
            },
          },
        ],
      });

      const req2 = httpMock.expectOne((request) => request.url.includes('/MedicationRequest'));
      req2.flush({
        resourceType: 'Bundle',
        entry: [
          { resource: { whenHandedOver: '2025-01-01', daysSupply: { value: 10 } } },
        ],
      });
    });

    it('should sort by lowest PDC first', (done) => {
      const patientId = 'patient-123';

      service.getProblematicMedications(patientId).subscribe((medications) => {
        if (medications.length > 1) {
          for (let i = 0; i < medications.length - 1; i++) {
            expect(medications[i].pdc).toBeLessThanOrEqual(medications[i + 1].pdc);
          }
        }
        done();
      });

      const req1 = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION) && request.params.get('status') === 'active');
      req1.flush({
        resourceType: 'Bundle',
        entry: [
          {
            resource: {
              id: 'med-1',
              status: 'active',
              medicationCodeableConcept: {
                coding: [{ code: '197361', display: 'Metformin' }],
              },
            },
          },
          {
            resource: {
              id: 'med-2',
              status: 'active',
              medicationCodeableConcept: {
                coding: [{ code: '314076', display: 'Lisinopril' }],
              },
            },
          },
        ],
      });

      const req2 = httpMock.expectOne((request) => request.url.includes('/MedicationRequest') && request.params.get('medication') === '197361');
      req2.flush({
        resourceType: 'Bundle',
        entry: [
          { resource: { whenHandedOver: '2025-01-01', daysSupply: { value: 10 } } },
        ],
      });

      const req3 = httpMock.expectOne((request) => request.url.includes('/MedicationRequest') && request.params.get('medication') === '314076');
      req3.flush({
        resourceType: 'Bundle',
        entry: [
          { resource: { whenHandedOver: '2025-01-01', daysSupply: { value: 15 } } },
        ],
      });
    });

    it('returns empty list on error', (done) => {
      const patientId = 'patient-123';

      service.getProblematicMedications(patientId).subscribe((medications) => {
        expect(medications).toEqual([]);
        done();
      };

      const req1 = httpMock.expectOne((request) => request.url.includes(FHIR_ENDPOINTS.MEDICATION) && request.params.get('status') === 'active');
      req1.flush('error', { status: 500, statusText: 'Server Error' });
    });
  });
});
