import {
  Patient,
  PatientSummary,
  HumanName,
  Identifier,
  Bundle,
  BundleEntry,
} from '../../app/models/patient.model';

/**
 * Factory for creating mock Patient objects for testing
 */
export class PatientFactory {
  private static counter = 1;

  /**
   * Create a full FHIR Patient resource
   */
  static create(overrides?: Partial<Patient>): Patient {
    const id = `patient-${this.counter++}`;
    const given = overrides?.name?.[0]?.given || ['John'];
    const family = overrides?.name?.[0]?.family || 'Doe';

    return {
      resourceType: 'Patient',
      id: overrides?.id || id,
      identifier: overrides?.identifier || [
        {
          system: 'MRN',
          value: `MRN${this.counter.toString().padStart(5, '0')}`,
        },
      ],
      name: overrides?.name || [
        {
          use: 'official',
          given,
          family,
        },
      ],
      gender: overrides?.gender || 'male',
      birthDate: overrides?.birthDate || '1980-01-15',
      active: overrides?.active !== undefined ? overrides.active : true,
    };
  }

  /**
   * Create multiple patients
   */
  static createMany(count: number, overrides?: Partial<Patient>): Patient[] {
    return Array.from({ length: count }, () => this.create(overrides));
  }

  /**
   * Create John Doe patient
   */
  static createJohnDoe(): Patient {
    return this.create({
      id: 'patient-001',
      identifier: [{ system: 'MRN', value: 'MRN00001', type: { text: 'Medical Record Number' } }],
      name: [{ use: 'official', given: ['John'], family: 'Doe' }],
      gender: 'male',
      birthDate: '1980-01-15',
    });
  }

  /**
   * Create Jane Smith patient
   */
  static createJaneSmith(): Patient {
    return this.create({
      id: 'patient-002',
      identifier: [{ system: 'MRN', value: 'MRN00002', type: { text: 'Medical Record Number' } }],
      name: [{ use: 'official', given: ['Jane'], family: 'Smith' }],
      gender: 'female',
      birthDate: '1975-06-20',
    });
  }

  /**
   * Create Robert Johnson patient
   */
  static createRobertJohnson(): Patient {
    return this.create({
      id: 'patient-003',
      identifier: [{ system: 'MRN', value: 'MRN00003', type: { text: 'Medical Record Number' } }],
      name: [{ use: 'official', given: ['Robert'], family: 'Johnson' }],
      gender: 'male',
      birthDate: '1965-03-10',
    });
  }

  /**
   * Create elderly patient (for age calculations)
   */
  static createElderlyPatient(): Patient {
    return this.create({
      birthDate: '1950-05-15', // ~74 years old
    });
  }

  /**
   * Create pediatric patient
   */
  static createPediatricPatient(): Patient {
    return this.create({
      birthDate: '2015-08-20', // ~9 years old
    });
  }

  /**
   * Create inactive patient
   */
  static createInactivePatient(): Patient {
    return this.create({
      active: false,
    });
  }

  /**
   * Create patient with no name
   */
  static createPatientWithoutName(): Patient {
    return this.create({
      name: [],
    });
  }

  /**
   * Create patient with no identifier
   */
  static createPatientWithoutIdentifier(): Patient {
    return this.create({
      identifier: [],
    });
  }

  /**
   * Create PatientSummary (UI model)
   */
  static createSummary(overrides?: Partial<PatientSummary>): PatientSummary {
    const id = `patient-${this.counter++}`;
    const hasMrnOverride = overrides ? Object.prototype.hasOwnProperty.call(overrides, 'mrn') : false;
    return {
      id: overrides?.id || id,
      mrn: hasMrnOverride ? overrides?.mrn : `MRN${this.counter.toString().padStart(5, '0')}`,
      fullName: overrides?.fullName || 'John Doe',
      firstName: overrides?.firstName || 'John',
      lastName: overrides?.lastName || 'Doe',
      dateOfBirth: overrides?.dateOfBirth || '1980-01-15',
      age: overrides?.age || 44,
      gender: overrides?.gender || 'male',
      status: overrides?.status || 'Active',
    };
  }

  /**
   * Create list of PatientSummary objects
   */
  static createSummaryList(): PatientSummary[] {
    return [
      {
        id: 'patient-001',
        mrn: 'MRN00001',
        fullName: 'John Doe',
        firstName: 'John',
        lastName: 'Doe',
        dateOfBirth: '1980-01-15',
        age: 44,
        gender: 'male',
        status: 'Active',
      },
      {
        id: 'patient-002',
        mrn: 'MRN00002',
        fullName: 'Jane Smith',
        firstName: 'Jane',
        lastName: 'Smith',
        dateOfBirth: '1975-06-20',
        age: 49,
        gender: 'female',
        status: 'Active',
      },
      {
        id: 'patient-003',
        mrn: 'MRN00003',
        fullName: 'Robert Johnson',
        firstName: 'Robert',
        lastName: 'Johnson',
        dateOfBirth: '1965-03-10',
        age: 59,
        gender: 'male',
        status: 'Active',
      },
    ];
  }

  /**
   * Create FHIR Bundle with patients
   */
  static createBundle(patients: Patient[]): Bundle<Patient> {
    return {
      resourceType: 'Bundle',
      type: 'searchset',
      total: patients.length,
      entry: patients.map((patient) => ({
        fullUrl: `http://localhost:8085/Patient/${patient.id}`,
        resource: patient,
      })),
    };
  }

  /**
   * Create empty FHIR Bundle
   */
  static createEmptyBundle(): Bundle<Patient> {
    return {
      resourceType: 'Bundle',
      type: 'searchset',
      total: 0,
      entry: [],
    };
  }

  /**
   * Reset counter for tests
   */
  static reset(): void {
    this.counter = 1;
  }
}
