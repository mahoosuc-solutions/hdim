import { TestBed } from '@angular/core/testing';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { PatientService } from './patient.service';
import { Patient, Bundle } from '../models/patient.model';
import { PatientFactory } from '../../testing/factories/patient.factory';
import { API_CONFIG, buildFhirUrl, FHIR_ENDPOINTS } from '../config/api.config';

describe('PatientService', () => {
  let service: PatientService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PatientService],
    });

    service = TestBed.inject(PatientService);
    httpMock = TestBed.inject(HttpTestingController);
    PatientFactory.reset();
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('initialization', () => {
    it('should be created', () => {
      expect(service).toBeTruthy();
    });
  });

  describe('getPatients', () => {
    it('should fetch patients and extract from bundle', () => {
      const mockPatients = [
        PatientFactory.createJohnDoe(),
        PatientFactory.createJaneSmith(),
        PatientFactory.createRobertJohnson(),
      ];
      const bundle = PatientFactory.createBundle(mockPatients);
      const expectedUrl = buildFhirUrl(FHIR_ENDPOINTS.PATIENT, { _count: '100' });

      service.getPatients().subscribe((patients) => {
        expect(patients.length).toBe(3);
        expect(patients).toEqual(mockPatients);
        expect(patients[0].id).toBe('patient-001');
      });

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('GET');
      req.flush(bundle);
    });

    it('should use custom count parameter', () => {
      const mockPatients = PatientFactory.createMany(50);
      const bundle = PatientFactory.createBundle(mockPatients);
      const expectedUrl = buildFhirUrl(FHIR_ENDPOINTS.PATIENT, { _count: '50' });

      service.getPatients(50).subscribe((patients) => {
        expect(patients.length).toBe(50);
      });

      const req = httpMock.expectOne(expectedUrl);
      req.flush(bundle);
    });

    it('should handle empty bundle', () => {
      const emptyBundle = PatientFactory.createEmptyBundle();

      service.getPatients().subscribe((patients) => {
        expect(patients).toEqual([]);
        expect(patients.length).toBe(0);
      });

      const req = httpMock.expectOne(buildFhirUrl(FHIR_ENDPOINTS.PATIENT, { _count: '100' }));
      req.flush(emptyBundle);
    });
  });

  describe('getPatient', () => {
    it('should fetch single patient by ID', () => {
      const mockPatient = PatientFactory.createJohnDoe();
      const expectedUrl = buildFhirUrl(FHIR_ENDPOINTS.PATIENT_BY_ID('patient-001'));

      service.getPatient('patient-001').subscribe((patient) => {
        expect(patient).toEqual(mockPatient);
        expect(patient.id).toBe('patient-001');
      });

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('GET');
      req.flush(mockPatient);
    });

    it('should handle 404 when patient not found', () => {
      service.getPatient('non-existent').subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne(
        buildFhirUrl(FHIR_ENDPOINTS.PATIENT_BY_ID('non-existent'))
      );
      req.flush('Not Found', { status: 404, statusText: 'Not Found' });
    });
  });

  describe('searchPatientsByName', () => {
    it('should search patients by name', () => {
      const mockPatients = [PatientFactory.createJohnDoe()];
      const bundle = PatientFactory.createBundle(mockPatients);
      const expectedUrl = buildFhirUrl(FHIR_ENDPOINTS.PATIENT, { name: 'Doe' });

      service.searchPatientsByName('Doe').subscribe((patients) => {
        expect(patients.length).toBe(1);
        expect(patients[0].name?.[0].family).toBe('Doe');
      });

      const req = httpMock.expectOne(expectedUrl);
      req.flush(bundle);
    });

    it('should return empty array when no matches', () => {
      const emptyBundle = PatientFactory.createEmptyBundle();

      service.searchPatientsByName('NonExistent').subscribe((patients) => {
        expect(patients).toEqual([]);
      });

      const req = httpMock.expectOne(
        buildFhirUrl(FHIR_ENDPOINTS.PATIENT, { name: 'NonExistent' })
      );
      req.flush(emptyBundle);
    });
  });

  describe('searchPatientsByIdentifier', () => {
    it('should search patients by identifier', () => {
      const mockPatient = PatientFactory.createJohnDoe();
      const bundle = PatientFactory.createBundle([mockPatient]);
      const expectedUrl = buildFhirUrl(FHIR_ENDPOINTS.PATIENT, { identifier: 'MRN00001' });

      service.searchPatientsByIdentifier('MRN00001').subscribe((patients) => {
        expect(patients.length).toBe(1);
        expect(patients[0].identifier?.[0].value).toBe('MRN00001');
      });

      const req = httpMock.expectOne(expectedUrl);
      req.flush(bundle);
    });
  });

  describe('searchPatientsByBirthdate', () => {
    it('should search patients by birthdate', () => {
      const mockPatient = PatientFactory.createJohnDoe();
      const bundle = PatientFactory.createBundle([mockPatient]);
      const birthdate = '1980-01-15';
      const expectedUrl = buildFhirUrl(FHIR_ENDPOINTS.PATIENT, { birthdate });

      service.searchPatientsByBirthdate(birthdate).subscribe((patients) => {
        expect(patients.length).toBe(1);
        expect(patients[0].birthDate).toBe(birthdate);
      });

      const req = httpMock.expectOne(expectedUrl);
      req.flush(bundle);
    });
  });

  describe('searchPatients', () => {
    it('should search with multiple criteria', () => {
      const mockPatient = PatientFactory.createJaneSmith();
      const bundle = PatientFactory.createBundle([mockPatient]);
      const params = {
        name: 'Smith',
        gender: 'female',
        birthdate: '1975-06-20',
      };

      service.searchPatients(params).subscribe((patients) => {
        expect(patients.length).toBe(1);
        expect(patients[0].name?.[0].family).toBe('Smith');
        expect(patients[0].gender).toBe('female');
      });

      // Match request regardless of query parameter order
      const req = httpMock.expectOne((request) => {
        return request.url.includes('/Patient') &&
               request.url.includes('name=Smith') &&
               request.url.includes('gender=female') &&
               request.url.includes('birthdate=1975-06-20');
      });
      req.flush(bundle);
    });

    it('should handle empty search parameters', () => {
      const mockPatients = PatientFactory.createMany(3);
      const bundle = PatientFactory.createBundle(mockPatients);

      service.searchPatients({}).subscribe((patients) => {
        expect(patients.length).toBe(3);
      });

      const req = httpMock.expectOne(buildFhirUrl(FHIR_ENDPOINTS.PATIENT, {}));
      req.flush(bundle);
    });
  });

  describe('createPatient', () => {
    it('should create a new patient', () => {
      const newPatient = PatientFactory.create();
      const expectedUrl = buildFhirUrl(FHIR_ENDPOINTS.PATIENT);

      service.createPatient(newPatient).subscribe((patient) => {
        expect(patient).toEqual(newPatient);
      });

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(newPatient);
      req.flush(newPatient);
    });

    it('should invalidate cache on create', () => {
      const invalidateSpy = jest.spyOn(service, 'invalidateCache');
      const newPatient = PatientFactory.create();

      service.createPatient(newPatient).subscribe();

      const req = httpMock.expectOne(buildFhirUrl(FHIR_ENDPOINTS.PATIENT));
      req.flush(newPatient);

      expect(invalidateSpy).toHaveBeenCalled();
    });
  });

  describe('updatePatient', () => {
    it('should update an existing patient', () => {
      const updatedPatient = PatientFactory.createJohnDoe();
      updatedPatient.active = false;
      const expectedUrl = buildFhirUrl(FHIR_ENDPOINTS.PATIENT_BY_ID('patient-001'));

      service.updatePatient('patient-001', updatedPatient).subscribe((patient) => {
        expect(patient).toEqual(updatedPatient);
        expect(patient.active).toBe(false);
      });

      const req = httpMock.expectOne(expectedUrl);
      expect(req.request.method).toBe('PUT');
      expect(req.request.body).toEqual(updatedPatient);
      req.flush(updatedPatient);
    });

    it('should invalidate cache on update', () => {
      const invalidateSpy = jest.spyOn(service, 'invalidateCache');
      const updatedPatient = PatientFactory.createJohnDoe();

      service.updatePatient('patient-001', updatedPatient).subscribe();

      const req = httpMock.expectOne(buildFhirUrl(FHIR_ENDPOINTS.PATIENT_BY_ID('patient-001')));
      req.flush(updatedPatient);

      expect(invalidateSpy).toHaveBeenCalled();
    });
  });

  describe('deletePatient', () => {
    it('should delete a patient and invalidate cache', () => {
      const invalidateSpy = jest.spyOn(service, 'invalidateCache');

      service.deletePatient('patient-001').subscribe();

      const req = httpMock.expectOne(buildFhirUrl(FHIR_ENDPOINTS.PATIENT_BY_ID('patient-001')));
      expect(req.request.method).toBe('DELETE');
      req.flush({});

      expect(invalidateSpy).toHaveBeenCalled();
    });
  });

  describe('caching', () => {
    it('should return cached patients within TTL', () => {
      const nowSpy = jest.spyOn(Date, 'now').mockReturnValue(1000);
      const mockPatients = PatientFactory.createMany(2);
      const bundle = PatientFactory.createBundle(mockPatients);
      const expectedUrl = buildFhirUrl(FHIR_ENDPOINTS.PATIENT, { _count: '100' });

      service.getPatientsCached().subscribe((patients) => {
        expect(patients.length).toBe(2);
      });

      const req = httpMock.expectOne(expectedUrl);
      req.flush(bundle);

      service.getPatientsCached().subscribe((patients) => {
        expect(patients.length).toBe(2);
      });

      httpMock.expectNone(expectedUrl);
      nowSpy.mockRestore();
    });

    it('should report cache validity based on timestamp', () => {
      const nowSpy = jest.spyOn(Date, 'now').mockReturnValue(1000);
      service.getPatientsCached().subscribe();
      const req = httpMock.expectOne(buildFhirUrl(FHIR_ENDPOINTS.PATIENT, { _count: '100' }));
      req.flush(PatientFactory.createBundle([]));

      expect(service.isCacheValid()).toBe(true);

      nowSpy.mockReturnValue(1000 + (5 * 60 * 1000) + 1);
      expect(service.isCacheValid()).toBe(false);
      nowSpy.mockRestore();
    });
  });

  describe('getPatientsSummary', () => {
    it('should transform patients to PatientSummary', () => {
      const mockPatients = [
        PatientFactory.createJohnDoe(),
        PatientFactory.createJaneSmith(),
      ];
      const bundle = PatientFactory.createBundle(mockPatients);

      service.getPatientsSummary().subscribe((summaries) => {
        expect(summaries.length).toBe(2);
        expect(summaries[0]).toHaveProperty('fullName');
        expect(summaries[0]).toHaveProperty('age');
        expect(summaries[0]).toHaveProperty('status');
        expect(summaries[0].fullName).toBe('John Doe');
        expect(summaries[0].mrn).toBe('MRN00001');
      });

      const req = httpMock.expectOne(buildFhirUrl(FHIR_ENDPOINTS.PATIENT, { _count: '100' }));
      req.flush(bundle);
    });
  });

  describe('toPatientSummary', () => {
    it('should convert FHIR Patient to PatientSummary', () => {
      const patient = PatientFactory.createJohnDoe();
      const summary = service.toPatientSummary(patient);

      expect(summary.id).toBe('patient-001');
      expect(summary.fullName).toBe('John Doe');
      expect(summary.firstName).toBe('John');
      expect(summary.lastName).toBe('Doe');
      expect(summary.mrn).toBe('MRN00001');
      expect(summary.dateOfBirth).toBe('1980-01-15');
      expect(summary.gender).toBe('male');
      expect(summary.status).toBe('Active');
      expect(summary.age).toBeGreaterThan(40); // Should calculate current age
    });

    it('should handle patient without name', () => {
      const patient = PatientFactory.createPatientWithoutName();
      const summary = service.toPatientSummary(patient);

      expect(summary.fullName).toBe('Unknown');
      expect(summary.firstName).toBeUndefined();
      expect(summary.lastName).toBeUndefined();
    });

    it('should handle patient without identifier', () => {
      const patient = PatientFactory.createPatientWithoutIdentifier();
      const summary = service.toPatientSummary(patient);

      expect(summary.mrn).toBeUndefined();
    });

    it('should handle inactive patient', () => {
      const patient = PatientFactory.createInactivePatient();
      const summary = service.toPatientSummary(patient);

      expect(summary.status).toBe('Inactive');
    });

    it('should calculate age correctly for elderly patient', () => {
      const patient = PatientFactory.createElderlyPatient();
      const summary = service.toPatientSummary(patient);

      expect(summary.age).toBeGreaterThan(70);
      expect(summary.age).toBeLessThan(80);
    });

    it('should calculate age correctly for pediatric patient', () => {
      const patient = PatientFactory.createPediatricPatient();
      const summary = service.toPatientSummary(patient);

      expect(summary.age).toBeGreaterThan(5);
      expect(summary.age).toBeLessThan(15);
    });
  });

  describe('formatPatientName', () => {
    it('should format patient name correctly', () => {
      const patient = PatientFactory.createJohnDoe();
      const formattedName = service.formatPatientName(patient);

      expect(formattedName).toBe('John Doe');
    });

    it('should handle patient without name', () => {
      const patient = PatientFactory.createPatientWithoutName();
      const formattedName = service.formatPatientName(patient);

      expect(formattedName).toBe('Unknown');
    });
  });

  describe('getPatientMRN', () => {
    it('should extract MRN from patient identifiers', () => {
      const patient = PatientFactory.createJohnDoe();
      const mrn = service.getPatientMRN(patient);

      expect(mrn).toBe('MRN00001');
    });

    it('should return undefined when no MRN identifier exists', () => {
      const patient = PatientFactory.createPatientWithoutIdentifier();
      const mrn = service.getPatientMRN(patient);

      expect(mrn).toBeUndefined();
    });
  });

  describe('getPatientMRNAuthority', () => {
    it('should extract MRN assigning authority', () => {
      const patient = PatientFactory.createJohnDoe();
      const authority = service.getPatientMRNAuthority(patient);

      expect(authority).toBeDefined();
    });

    it('should return undefined when no MRN authority exists', () => {
      const patient = PatientFactory.createPatientWithoutIdentifier();
      const authority = service.getPatientMRNAuthority(patient);

      expect(authority).toBeUndefined();
    });
  });
});
