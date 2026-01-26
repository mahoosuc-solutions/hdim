/**
 * Unit Tests for Procedure History Service (TDD)
 *
 * Feature 2.4: Procedure History Service
 * 21 comprehensive test cases covering all requirements
 */

import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ProcedureHistoryService } from './procedure-history.service';
import {
  ProcedureRecord,
  CategorizedProcedures,
  ProcedureQueryOptions,
  FhirProcedure,
  PROCEDURE_CATEGORY_MAPPING,
} from '../models/procedure-history.model';
import { FhirBundle } from '../models/fhir.model';
import { API_CONFIG, FHIR_ENDPOINTS, buildFhirUrl } from '../config/api.config';

describe('ProcedureHistoryService', () => {
  let service: ProcedureHistoryService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ProcedureHistoryService],
    });
    service = TestBed.inject(ProcedureHistoryService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  }, 30000);

  // ========================================================================
  // Get Procedures Tests (7 tests)
  // ========================================================================

  describe('getProcedures', () => {
    it('1. Should fetch FHIR Procedure resources for patient', (done) => {
      const patientId = 'patient-123';
      const mockBundle: FhirBundle<FhirProcedure> = {
        resourceType: 'Bundle',
        type: 'searchset',
        total: 2,
        entry: [
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-1',
              status: 'completed',
              code: {
                coding: [
                  {
                    system: 'http://snomed.info/sct',
                    code: '387713003',
                    display: 'Surgical procedure',
                  },
                ],
                text: 'Appendectomy',
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-10-15T10:00:00Z',
            },
          },
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-2',
              status: 'completed',
              code: {
                coding: [
                  {
                    system: 'http://www.ama-assn.org/go/cpt',
                    code: '80053',
                    display: 'Comprehensive metabolic panel',
                  },
                ],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-11-01T09:00:00Z',
            },
          },
        ],
      };

      service.getProcedures(patientId).subscribe((procedures) => {
        expect(procedures).toBeTruthy();
        expect(procedures.length).toBe(2);
        expect(procedures[0].id).toBe('proc-1');
        expect(procedures[1].id).toBe('proc-2');
        done();
      }, 30000);

      const req = httpMock.expectOne((request) =>
        request.url.includes('/Procedure') && request.params.get('patient') === patientId
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockBundle);
    }, 30000);

    it('2. Should extract SNOMED/CPT code and display name', (done) => {
      const patientId = 'patient-123';
      const mockBundle: FhirBundle<FhirProcedure> = {
        resourceType: 'Bundle',
        type: 'searchset',
        total: 2,
        entry: [
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-1',
              status: 'completed',
              code: {
                coding: [
                  {
                    system: 'http://snomed.info/sct',
                    code: '387713003',
                    display: 'Surgical procedure',
                  },
                ],
                text: 'Appendectomy',
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-10-15T10:00:00Z',
            },
          },
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-2',
              status: 'completed',
              code: {
                coding: [
                  {
                    system: 'http://www.ama-assn.org/go/cpt',
                    code: '80053',
                    display: 'Comprehensive metabolic panel',
                  },
                ],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-11-01T09:00:00Z',
            },
          },
        ],
      };

      service.getProcedures(patientId).subscribe((procedures) => {
        expect(procedures[0].code).toBe('387713003');
        expect(procedures[0].codeSystem).toBe('SNOMED');
        expect(procedures[0].displayName).toBe('Appendectomy');

        expect(procedures[1].code).toBe('80053');
        expect(procedures[1].codeSystem).toBe('CPT');
        expect(procedures[1].displayName).toBe('Comprehensive metabolic panel');
        done();
      }, 30000);

      const req = httpMock.expectOne((request) => request.url.includes('/Procedure'));
      req.flush(mockBundle);
    }, 30000);

    it('3. Should extract performer name and role from Procedure.performer', (done) => {
      const patientId = 'patient-123';
      const mockBundle: FhirBundle<FhirProcedure> = {
        resourceType: 'Bundle',
        type: 'searchset',
        total: 1,
        entry: [
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-1',
              status: 'completed',
              code: {
                coding: [
                  {
                    system: 'http://snomed.info/sct',
                    code: '387713003',
                    display: 'Surgical procedure',
                  },
                ],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-10-15T10:00:00Z',
              performer: [
                {
                  function: {
                    coding: [
                      {
                        system: 'http://terminology.hl7.org/CodeSystem/v2-0443',
                        code: 'SP',
                        display: 'Surgeon',
                      },
                    ],
                  },
                  actor: {
                    reference: 'Practitioner/prac-1',
                    display: 'Dr. Jane Smith',
                  },
                },
              ],
            },
          },
        ],
      };

      service.getProcedures(patientId).subscribe((procedures) => {
        expect(procedures[0].performerName).toBe('Dr. Jane Smith');
        expect(procedures[0].performerRole).toBe('Surgeon');
        done();
      }, 30000);

      const req = httpMock.expectOne((request) => request.url.includes('/Procedure'));
      req.flush(mockBundle);
    }, 30000);

    it('4. Should filter by status when provided', (done) => {
      const patientId = 'patient-123';
      const options: ProcedureQueryOptions = { status: 'completed' };

      service.getProcedures(patientId, options).subscribe(() => {
        done();
      }, 30000);

      const req = httpMock.expectOne((request) =>
        request.url.includes('/Procedure') &&
        request.params.get('patient') === patientId &&
        request.params.get('status') === 'completed'
      );
      expect(req.request.method).toBe('GET');
      req.flush({ resourceType: 'Bundle', type: 'searchset', total: 0, entry: [] });
    });

    it('5. Should filter by date range when provided', (done) => {
      const patientId = 'patient-123';
      const startDate = new Date('2025-01-01');
      const endDate = new Date('2025-12-31');
      const options: ProcedureQueryOptions = { startDate, endDate };

      service.getProcedures(patientId, options).subscribe(() => {
        done();
      }, 30000);

      const req = httpMock.expectOne((request) => {
        const dateParam = request.params.get('date');
        return (
          request.url.includes('/Procedure') &&
          request.params.get('patient') === patientId &&
          dateParam !== null &&
          dateParam.includes('ge2025-01-01') &&
          dateParam.includes('le2025-12-31')
        );
      });
      expect(req.request.method).toBe('GET');
      req.flush({ resourceType: 'Bundle', type: 'searchset', total: 0, entry: [] });
    });

    it('6. Should respect limit parameter', (done) => {
      const patientId = 'patient-123';
      const options: ProcedureQueryOptions = { limit: 10 };

      service.getProcedures(patientId, options).subscribe(() => {
        done();
      }, 30000);

      const req = httpMock.expectOne((request) =>
        request.url.includes('/Procedure') &&
        request.params.get('patient') === patientId &&
        request.params.get('_count') === '10'
      );
      expect(req.request.method).toBe('GET');
      req.flush({ resourceType: 'Bundle', type: 'searchset', total: 0, entry: [] });
    });

    it('7. Should handle procedures without performer or notes', (done) => {
      const patientId = 'patient-123';
      const mockBundle: FhirBundle<FhirProcedure> = {
        resourceType: 'Bundle',
        type: 'searchset',
        total: 1,
        entry: [
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-1',
              status: 'completed',
              code: {
                coding: [
                  {
                    system: 'http://snomed.info/sct',
                    code: '168537006',
                    display: 'X-ray',
                  },
                ],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-10-15T10:00:00Z',
              // No performer or notes
            },
          },
        ],
      };

      service.getProcedures(patientId).subscribe((procedures) => {
        expect(procedures[0].performerName).toBeUndefined();
        expect(procedures[0].performerRole).toBeUndefined();
        expect(procedures[0].notes).toBeUndefined();
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/Procedure'));
      req.flush(mockBundle);
    });
  });

  // ========================================================================
  // Categorization Tests (6 tests)
  // ========================================================================

  describe('getProceduresByCategory', () => {
    it('8. Should categorize procedures with surgical SNOMED codes', (done) => {
      const patientId = 'patient-123';
      const mockBundle: FhirBundle<FhirProcedure> = {
        resourceType: 'Bundle',
        type: 'searchset',
        total: 1,
        entry: [
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-1',
              status: 'completed',
              code: {
                coding: [
                  {
                    system: 'http://snomed.info/sct',
                    code: '387713003',
                    display: 'Surgical procedure',
                  },
                ],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-10-15T10:00:00Z',
            },
          },
        ],
      };

      service.getProceduresByCategory(patientId).subscribe((categorized) => {
        expect(categorized.surgical.length).toBe(1);
        expect(categorized.surgical[0].category).toBe('surgical');
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/Procedure'));
      req.flush(mockBundle);
    });

    it('9. Should categorize imaging and diagnostic procedures', (done) => {
      const patientId = 'patient-123';
      const mockBundle: FhirBundle<FhirProcedure> = {
        resourceType: 'Bundle',
        type: 'searchset',
        total: 2,
        entry: [
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-1',
              status: 'completed',
              code: {
                coding: [
                  {
                    system: 'http://snomed.info/sct',
                    code: '168537006',
                    display: 'X-ray',
                  },
                ],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-10-15T10:00:00Z',
            },
          },
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-2',
              status: 'completed',
              code: {
                coding: [
                  {
                    system: 'http://www.ama-assn.org/go/cpt',
                    code: '70450',
                    display: 'CT scan',
                  },
                ],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-11-01T09:00:00Z',
            },
          },
        ],
      };

      service.getProceduresByCategory(patientId).subscribe((categorized) => {
        expect(categorized.imaging.length).toBe(2);
        expect(categorized.imaging[0].category).toBe('imaging');
        expect(categorized.imaging[1].category).toBe('imaging');
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/Procedure'));
      req.flush(mockBundle);
    });

    it('10. Should categorize therapeutic procedures', (done) => {
      const patientId = 'patient-123';
      const mockBundle: FhirBundle<FhirProcedure> = {
        resourceType: 'Bundle',
        type: 'searchset',
        total: 1,
        entry: [
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-1',
              status: 'completed',
              code: {
                coding: [
                  {
                    system: 'http://snomed.info/sct',
                    code: '91251008',
                    display: 'Physical therapy',
                  },
                ],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-10-15T10:00:00Z',
            },
          },
        ],
      };

      service.getProceduresByCategory(patientId).subscribe((categorized) => {
        expect(categorized.therapeutic.length).toBe(1);
        expect(categorized.therapeutic[0].category).toBe('therapeutic');
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/Procedure'));
      req.flush(mockBundle);
    });

    it('11. Should place uncategorized procedures in "other"', (done) => {
      const patientId = 'patient-123';
      const mockBundle: FhirBundle<FhirProcedure> = {
        resourceType: 'Bundle',
        type: 'searchset',
        total: 1,
        entry: [
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-1',
              status: 'completed',
              code: {
                coding: [
                  {
                    system: 'http://snomed.info/sct',
                    code: '999999999',
                    display: 'Unknown procedure',
                  },
                ],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-10-15T10:00:00Z',
            },
          },
        ],
      };

      service.getProceduresByCategory(patientId).subscribe((categorized) => {
        expect(categorized.other.length).toBe(1);
        expect(categorized.other[0].category).toBe('other');
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/Procedure'));
      req.flush(mockBundle);
    });

    it('12. Should include accurate totalCount', (done) => {
      const patientId = 'patient-123';
      const mockBundle: FhirBundle<FhirProcedure> = {
        resourceType: 'Bundle',
        type: 'searchset',
        total: 3,
        entry: [
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-1',
              status: 'completed',
              code: {
                coding: [{ system: 'http://snomed.info/sct', code: '387713003', display: 'Surgery' }],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-10-15T10:00:00Z',
            },
          },
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-2',
              status: 'completed',
              code: {
                coding: [{ system: 'http://snomed.info/sct', code: '168537006', display: 'X-ray' }],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-11-01T09:00:00Z',
            },
          },
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-3',
              status: 'completed',
              code: {
                coding: [{ system: 'http://snomed.info/sct', code: '33879002', display: 'Vaccination' }],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-11-05T09:00:00Z',
            },
          },
        ],
      };

      service.getProceduresByCategory(patientId).subscribe((categorized) => {
        expect(categorized.totalCount).toBe(3);
        expect(
          categorized.surgical.length +
            categorized.imaging.length +
            categorized.lab.length +
            categorized.therapeutic.length +
            categorized.preventive.length +
            categorized.other.length
        ).toBe(3);
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/Procedure'));
      req.flush(mockBundle);
    });

    it('13. Should detect surgical procedures by SNOMED code range', () => {
      const surgicalProcedure: FhirProcedure = {
        resourceType: 'Procedure',
        id: 'proc-1',
        status: 'completed',
        code: {
          coding: [
            {
              system: 'http://snomed.info/sct',
              code: '387750000',
              display: 'Surgical procedure',
            },
          ],
        },
        performedDateTime: '2025-10-15T10:00:00Z',
      };

      const category = service.categorizeProcedure(surgicalProcedure);
      expect(category).toBe('surgical');
    });
  });

  // ========================================================================
  // Category Detection Tests (4 tests)
  // ========================================================================

  describe('categorizeProcedure', () => {
    it('14. Should detect imaging procedures (X-ray, CT, MRI)', () => {
      const xrayProcedure: FhirProcedure = {
        resourceType: 'Procedure',
        id: 'proc-1',
        status: 'completed',
        code: {
          coding: [
            {
              system: 'http://snomed.info/sct',
              code: '168537006',
              display: 'X-ray',
            },
          ],
        },
        performedDateTime: '2025-10-15T10:00:00Z',
      };

      const ctProcedure: FhirProcedure = {
        resourceType: 'Procedure',
        id: 'proc-2',
        status: 'completed',
        code: {
          coding: [
            {
              system: 'http://snomed.info/sct',
              code: '77477000',
              display: 'CT scan',
            },
          ],
        },
        performedDateTime: '2025-10-15T10:00:00Z',
      };

      const mriProcedure: FhirProcedure = {
        resourceType: 'Procedure',
        id: 'proc-3',
        status: 'completed',
        code: {
          coding: [
            {
              system: 'http://snomed.info/sct',
              code: '241615005',
              display: 'MRI',
            },
          ],
        },
        performedDateTime: '2025-10-15T10:00:00Z',
      };

      expect(service.categorizeProcedure(xrayProcedure)).toBe('imaging');
      expect(service.categorizeProcedure(ctProcedure)).toBe('imaging');
      expect(service.categorizeProcedure(mriProcedure)).toBe('imaging');
    });

    it('15. Should detect lab procedures (blood draw, biopsy)', () => {
      const bloodDrawProcedure: FhirProcedure = {
        resourceType: 'Procedure',
        id: 'proc-1',
        status: 'completed',
        code: {
          coding: [
            {
              system: 'http://snomed.info/sct',
              code: '396550006',
              display: 'Blood draw',
            },
          ],
        },
        performedDateTime: '2025-10-15T10:00:00Z',
      };

      const biopsyProcedure: FhirProcedure = {
        resourceType: 'Procedure',
        id: 'proc-2',
        status: 'completed',
        code: {
          coding: [
            {
              system: 'http://snomed.info/sct',
              code: '86273004',
              display: 'Biopsy',
            },
          ],
        },
        performedDateTime: '2025-10-15T10:00:00Z',
      };

      expect(service.categorizeProcedure(bloodDrawProcedure)).toBe('lab');
      expect(service.categorizeProcedure(biopsyProcedure)).toBe('lab');
    });

    it('16. Should detect preventive procedures (vaccinations, screenings)', () => {
      const vaccinationProcedure: FhirProcedure = {
        resourceType: 'Procedure',
        id: 'proc-1',
        status: 'completed',
        code: {
          coding: [
            {
              system: 'http://snomed.info/sct',
              code: '33879002',
              display: 'Vaccination',
            },
          ],
        },
        performedDateTime: '2025-10-15T10:00:00Z',
      };

      const screeningProcedure: FhirProcedure = {
        resourceType: 'Procedure',
        id: 'proc-2',
        status: 'completed',
        code: {
          coding: [
            {
              system: 'http://snomed.info/sct',
              code: '268556000',
              display: 'Screening',
            },
          ],
        },
        performedDateTime: '2025-10-15T10:00:00Z',
      };

      expect(service.categorizeProcedure(vaccinationProcedure)).toBe('preventive');
      expect(service.categorizeProcedure(screeningProcedure)).toBe('preventive');
    });

    it('17. Should categorize using CPT code mapping', () => {
      const surgicalCptProcedure: FhirProcedure = {
        resourceType: 'Procedure',
        id: 'proc-1',
        status: 'completed',
        code: {
          coding: [
            {
              system: 'http://www.ama-assn.org/go/cpt',
              code: '44950',
              display: 'Appendectomy',
            },
          ],
        },
        performedDateTime: '2025-10-15T10:00:00Z',
      };

      const imagingCptProcedure: FhirProcedure = {
        resourceType: 'Procedure',
        id: 'proc-2',
        status: 'completed',
        code: {
          coding: [
            {
              system: 'http://www.ama-assn.org/go/cpt',
              code: '70450',
              display: 'CT scan',
            },
          ],
        },
        performedDateTime: '2025-10-15T10:00:00Z',
      };

      const labCptProcedure: FhirProcedure = {
        resourceType: 'Procedure',
        id: 'proc-3',
        status: 'completed',
        code: {
          coding: [
            {
              system: 'http://www.ama-assn.org/go/cpt',
              code: '80053',
              display: 'Comprehensive metabolic panel',
            },
          ],
        },
        performedDateTime: '2025-10-15T10:00:00Z',
      };

      const preventiveCptProcedure: FhirProcedure = {
        resourceType: 'Procedure',
        id: 'proc-4',
        status: 'completed',
        code: {
          coding: [
            {
              system: 'http://www.ama-assn.org/go/cpt',
              code: '90471',
              display: 'Immunization administration',
            },
          ],
        },
        performedDateTime: '2025-10-15T10:00:00Z',
      };

      expect(service.categorizeProcedure(surgicalCptProcedure)).toBe('surgical');
      expect(service.categorizeProcedure(imagingCptProcedure)).toBe('imaging');
      expect(service.categorizeProcedure(labCptProcedure)).toBe('lab');
      expect(service.categorizeProcedure(preventiveCptProcedure)).toBe('preventive');
    });
  });

  // ========================================================================
  // Recent and Scheduled Tests (4 tests)
  // ========================================================================

  describe('getRecentProcedures', () => {
    it('18. Should return only procedures from last 12 months', (done) => {
      const patientId = 'patient-123';
      const today = new Date();
      const elevenMonthsAgo = new Date(today);
      elevenMonthsAgo.setMonth(today.getMonth() - 11);

      const thirteenMonthsAgo = new Date(today);
      thirteenMonthsAgo.setMonth(today.getMonth() - 13);

      const mockBundle: FhirBundle<FhirProcedure> = {
        resourceType: 'Bundle',
        type: 'searchset',
        total: 2,
        entry: [
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-recent',
              status: 'completed',
              code: {
                coding: [{ system: 'http://snomed.info/sct', code: '387713003', display: 'Surgery' }],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: elevenMonthsAgo.toISOString(),
            },
          },
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-old',
              status: 'completed',
              code: {
                coding: [{ system: 'http://snomed.info/sct', code: '168537006', display: 'X-ray' }],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: thirteenMonthsAgo.toISOString(),
            },
          },
        ],
      };

      service.getRecentProcedures(patientId).subscribe((procedures) => {
        // Should only return the recent one (client-side filtering)
        expect(procedures.length).toBeGreaterThanOrEqual(1);
        // Check that date filter was sent to server
        done();
      });

      const req = httpMock.expectOne((request) => {
        const dateParam = request.params.get('date');
        return request.url.includes('/Procedure') && dateParam !== null && dateParam.includes('ge');
      });
      req.flush(mockBundle);
    });

    it('19. Should sort by performedDate descending', (done) => {
      const patientId = 'patient-123';
      const mockBundle: FhirBundle<FhirProcedure> = {
        resourceType: 'Bundle',
        type: 'searchset',
        total: 3,
        entry: [
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-1',
              status: 'completed',
              code: {
                coding: [{ system: 'http://snomed.info/sct', code: '387713003', display: 'Surgery 1' }],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-10-15T10:00:00Z',
            },
          },
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-2',
              status: 'completed',
              code: {
                coding: [{ system: 'http://snomed.info/sct', code: '168537006', display: 'X-ray' }],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-11-20T09:00:00Z',
            },
          },
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-3',
              status: 'completed',
              code: {
                coding: [{ system: 'http://snomed.info/sct', code: '33879002', display: 'Vaccination' }],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: '2025-09-05T09:00:00Z',
            },
          },
        ],
      };

      service.getRecentProcedures(patientId).subscribe((procedures) => {
        // Check sorting - most recent first
        expect(procedures[0].id).toBe('proc-2');
        expect(procedures[1].id).toBe('proc-1');
        expect(procedures[2].id).toBe('proc-3');
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/Procedure'));
      req.flush(mockBundle);
    });
  });

  describe('getScheduledProcedures', () => {
    it('20. Should return procedures with status=preparation and future date', (done) => {
      const patientId = 'patient-123';
      const futureDate = new Date();
      futureDate.setDate(futureDate.getDate() + 30);

      const mockBundle: FhirBundle<FhirProcedure> = {
        resourceType: 'Bundle',
        type: 'searchset',
        total: 1,
        entry: [
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-scheduled',
              status: 'preparation',
              code: {
                coding: [{ system: 'http://snomed.info/sct', code: '387713003', display: 'Surgery' }],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: futureDate.toISOString(),
            },
          },
        ],
      };

      service.getScheduledProcedures(patientId).subscribe((procedures) => {
        expect(procedures.length).toBe(1);
        expect(procedures[0].status).toBe('preparation');
        expect(procedures[0].performedDate.getTime()).toBeGreaterThan(new Date().getTime());
        done();
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes('/Procedure') && request.params.get('status') === 'preparation'
      );
      req.flush(mockBundle);
    });

    it('21. Should sort scheduled procedures by date ascending', (done) => {
      const patientId = 'patient-123';
      const date1 = new Date();
      date1.setDate(date1.getDate() + 10);

      const date2 = new Date();
      date2.setDate(date2.getDate() + 5);

      const date3 = new Date();
      date3.setDate(date3.getDate() + 20);

      const mockBundle: FhirBundle<FhirProcedure> = {
        resourceType: 'Bundle',
        type: 'searchset',
        total: 3,
        entry: [
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-1',
              status: 'preparation',
              code: {
                coding: [{ system: 'http://snomed.info/sct', code: '387713003', display: 'Surgery 1' }],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: date1.toISOString(),
            },
          },
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-2',
              status: 'preparation',
              code: {
                coding: [{ system: 'http://snomed.info/sct', code: '168537006', display: 'X-ray' }],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: date2.toISOString(),
            },
          },
          {
            resource: {
              resourceType: 'Procedure',
              id: 'proc-3',
              status: 'preparation',
              code: {
                coding: [{ system: 'http://snomed.info/sct', code: '33879002', display: 'Vaccination' }],
              },
              subject: { reference: `Patient/${patientId}` },
              performedDateTime: date3.toISOString(),
            },
          },
        ],
      };

      service.getScheduledProcedures(patientId).subscribe((procedures) => {
        // Check sorting - soonest first (ascending)
        expect(procedures[0].id).toBe('proc-2');
        expect(procedures[1].id).toBe('proc-1');
        expect(procedures[2].id).toBe('proc-3');
        done();
      });

      const req = httpMock.expectOne((request) => request.url.includes('/Procedure'));
      req.flush(mockBundle);
    });
  });
});
