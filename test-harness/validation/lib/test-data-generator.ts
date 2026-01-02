import { randomUUID } from 'crypto';

export class TestDataGenerator {
  generateFHIRPatient(overrides: any = {}): any {
    const id = randomUUID();

    return {
      resourceType: 'Patient',
      id,
      identifier: overrides.identifier || [{
        system: 'http://example.org/mrn',
        value: `MRN-${Date.now()}-${Math.random().toString(36).substr(2, 9)}`,
      }],
      name: [{
        family: overrides.family || 'Doe',
        given: overrides.given || ['John'],
      }],
      gender: overrides.gender || 'male',
      birthDate: overrides.birthDate || '1980-01-15',
      ...overrides,
    };
  }

  generatePatient(config: { tenantId: string; [key: string]: any }): any {
    return {
      ...this.generateFHIRPatient(config),
      tenantId: config.tenantId,
    };
  }

  generateDiabetesPatient(config: { tenantId: string }): any {
    const patient = this.generatePatient(config);

    // Add diabetes condition
    patient.conditions = [{
      resourceType: 'Condition',
      code: {
        coding: [{
          system: 'http://snomed.info/sct',
          code: '73211009',
          display: 'Diabetes mellitus',
        }],
      },
      subject: { reference: `Patient/${patient.id}` },
    }];

    return patient;
  }

  generateObservation(config: { patientId: string; code: string; value: number; unit: string; date?: string }): any {
    return {
      resourceType: 'Observation',
      id: randomUUID(),
      status: 'final',
      code: {
        coding: [{
          system: 'http://loinc.org',
          code: config.code,
        }],
      },
      subject: {
        reference: `Patient/${config.patientId}`,
      },
      effectiveDateTime: config.date || new Date().toISOString(),
      valueQuantity: {
        value: config.value,
        unit: config.unit,
      },
    };
  }

  generateCondition(config: { patientId: string; code: string; display: string }): any {
    return {
      resourceType: 'Condition',
      id: randomUUID(),
      clinicalStatus: {
        coding: [{
          system: 'http://terminology.hl7.org/CodeSystem/condition-clinical',
          code: 'active',
        }],
      },
      code: {
        coding: [{
          system: 'http://snomed.info/sct',
          code: config.code,
          display: config.display,
        }],
      },
      subject: {
        reference: `Patient/${config.patientId}`,
      },
      recordedDate: new Date().toISOString(),
    };
  }

  generateBulkPatients(count: number, tenantId: string): any[] {
    const patients: any[] = [];

    for (let i = 0; i < count; i++) {
      patients.push(this.generatePatient({
        tenantId,
        family: `TestFamily${i}`,
        given: [`TestGiven${i}`],
      }));
    }

    return patients;
  }
}
