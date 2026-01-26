import { of, throwError } from 'rxjs';
import { FhirClinicalService, Observation, Condition, Procedure } from './fhir-clinical.service';

const createBundle = <T>(resources: T[]) => ({
  resourceType: 'Bundle' as const,
  type: 'searchset',
  entry: resources.map((resource) => ({ resource })),
});

describe('FhirClinicalService', () => {
  let service: FhirClinicalService;
  let http: { get: jest.Mock };

  beforeEach(() => {
    http = { get: jest.fn() };
    service = new FhirClinicalService(http as any);
  });

  it('fetches observations and maps bundle resources', (done) => {
    const obs: Observation = {
      resourceType: 'Observation',
      status: 'final',
      code: { text: 'HbA1c' },
      subject: { reference: 'Patient/1' },
    };

    http.get.mockReturnValueOnce(of(createBundle([obs])));

    service.getObservations('1', 10).subscribe((results) => {
      expect(results.length).toBe(1);
      expect(http.get).toHaveBeenCalled();
      done();
    });
  });

  it('returns empty lists on errors', (done) => {
    http.get.mockReturnValueOnce(throwError(() => new Error('fail')));

    service.getConditions('1').subscribe((results) => {
      expect(results).toEqual([]);
      done();
    }, 30000);
  });

  it('fetches all clinical data via forkJoin', (done) => {
    http.get
      .mockReturnValueOnce(of(createBundle([])))
      .mockReturnValueOnce(of(createBundle([])))
      .mockReturnValueOnce(of(createBundle([])));

    service.getPatientClinicalData('1').subscribe((results) => {
      expect(results.observations).toEqual([]);
      expect(results.conditions).toEqual([]);
      expect(results.procedures).toEqual([]);
      done();
    }, 30000);
  });

  it('returns empty bundle entries as empty arrays', (done) => {
    http.get.mockReturnValueOnce(of({ resourceType: 'Bundle', type: 'searchset' }));

    service.getProcedures('1').subscribe((results) => {
      expect(results).toEqual([]);
      done();
    }, 30000);
  });

  it('formats observation and codes for display', () => {
    const observation: Observation = {
      resourceType: 'Observation',
      status: 'final',
      code: { coding: [{ code: '123', display: 'Display' }] },
      subject: { reference: 'Patient/1' },
      valueQuantity: { value: 5.5, unit: '%' },
    };

    const condition: Condition = {
      resourceType: 'Condition',
      code: { coding: [{ code: 'C1', display: 'Condition' }] },
      subject: { reference: 'Patient/1' },
    };

    const procedure: Procedure = {
      resourceType: 'Procedure',
      status: 'completed',
      code: { coding: [{ code: 'P1', display: 'Procedure' }] },
      subject: { reference: 'Patient/1' },
    };

    expect(service.formatObservationValue(observation)).toBe('5.5 %');
    expect(service.getObservationCodeDisplay(observation)).toBe('Display');
    expect(service.getConditionCodeDisplay(condition)).toBe('Condition');
    expect(service.getProcedureCodeDisplay(procedure)).toBe('Procedure');
  });

  it('uses fallback display values', () => {
    const observation: Observation = {
      resourceType: 'Observation',
      status: 'final',
      code: { text: '' },
      subject: { reference: 'Patient/1' },
    };

    const condition: Condition = {
      resourceType: 'Condition',
      code: { coding: [] },
      subject: { reference: 'Patient/1' },
    };

    expect(service.formatObservationValue(observation)).toBe('N/A');
    expect(service.getObservationCodeDisplay(observation)).toBe('Unknown');
    expect(service.getConditionStatus(condition)).toBe('unknown');
  });

  it('formats value strings and codeable concepts', () => {
    const observation: Observation = {
      resourceType: 'Observation',
      status: 'final',
      code: { text: 'Note' },
      subject: { reference: 'Patient/1' },
      valueString: 'Normal',
    };

    const codedObservation: Observation = {
      resourceType: 'Observation',
      status: 'final',
      code: { text: 'Code' },
      subject: { reference: 'Patient/1' },
      valueCodeableConcept: { text: 'High' },
    };

    expect(service.formatObservationValue(observation)).toBe('Normal');
    expect(service.formatObservationValue(codedObservation)).toBe('High');
  });

  it('prefers text values for code display and status', () => {
    const observation: Observation = {
      resourceType: 'Observation',
      status: 'final',
      code: { text: 'BP', coding: [{ code: 'bp' }] },
      subject: { reference: 'Patient/1' },
    };

    const condition: Condition = {
      resourceType: 'Condition',
      code: { text: 'Hypertension', coding: [{ code: 'C1' }] },
      clinicalStatus: { coding: [{ code: 'active' }] },
      subject: { reference: 'Patient/1' },
    };

    expect(service.getObservationCodeDisplay(observation)).toBe('BP');
    expect(service.getConditionCodeDisplay(condition)).toBe('Hypertension');
    expect(service.getConditionStatus(condition)).toBe('active');
  });

  it('fetches observations by code', (done) => {
    http.get.mockReturnValueOnce(of(createBundle([])));

    service.getObservationsByCode('1', 'code-1').subscribe((results) => {
      expect(results).toEqual([]);
      done();
    }, 30000);
  });
});
