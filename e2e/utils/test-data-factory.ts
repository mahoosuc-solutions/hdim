/**
 * Test Data Factory for Synthetic FHIR Data Generation
 *
 * Generates realistic but synthetic healthcare data for testing.
 * All data is clearly marked as test data to prevent confusion
 * with real patient information.
 *
 * Based on Synthea-style data generation patterns.
 */

import { PHIMasking } from './phi-masking';

// Synthetic name pools
const FIRST_NAMES = [
  'Test_Alice', 'Test_Bob', 'Test_Carol', 'Test_David', 'Test_Emma',
  'Test_Frank', 'Test_Grace', 'Test_Henry', 'Test_Iris', 'Test_Jack',
  'Test_Karen', 'Test_Leo', 'Test_Maria', 'Test_Nathan', 'Test_Olivia',
  'Test_Peter', 'Test_Quinn', 'Test_Rachel', 'Test_Samuel', 'Test_Tina',
];

const LAST_NAMES = [
  'Synthetic', 'TestPatient', 'FakeData', 'DemoUser', 'SampleCase',
  'MockRecord', 'TestCase', 'FauxPatient', 'SimData', 'DummyRecord',
];

// Quality measure codes
const QUALITY_MEASURES = [
  { code: 'CMS122v12', name: 'Diabetes: Hemoglobin A1c Poor Control', category: 'Chronic Disease' },
  { code: 'CMS130v12', name: 'Colorectal Cancer Screening', category: 'Preventive' },
  { code: 'CMS165v12', name: 'Controlling High Blood Pressure', category: 'Chronic Disease' },
  { code: 'CMS138v12', name: 'Tobacco Use Screening and Cessation', category: 'Preventive' },
  { code: 'CMS125v12', name: 'Breast Cancer Screening', category: 'Women\'s Health' },
  { code: 'CMS127v12', name: 'Pneumococcal Vaccination', category: 'Preventive' },
  { code: 'CMS139v12', name: 'Falls Screening', category: 'Geriatric' },
  { code: 'CMS156v12', name: 'High-Risk Medication Use', category: 'Medication' },
  { code: 'CMS347v7', name: 'Statin Therapy for CVD Prevention', category: 'Medication' },
  { code: 'CMS349v6', name: 'HIV Screening', category: 'Preventive' },
];

// Condition codes (SNOMED CT)
const CONDITIONS = [
  { code: '44054006', display: 'Type 2 Diabetes Mellitus' },
  { code: '38341003', display: 'Hypertensive disorder' },
  { code: '195967001', display: 'Asthma' },
  { code: '13645005', display: 'Chronic obstructive lung disease' },
  { code: '49436004', display: 'Atrial fibrillation' },
  { code: '22298006', display: 'Myocardial infarction' },
  { code: '73211009', display: 'Diabetes mellitus' },
  { code: '370143000', display: 'Major depression' },
  { code: '197480006', display: 'Anxiety disorder' },
  { code: '414545008', display: 'Ischemic heart disease' },
];

// Care gap types
const CARE_GAP_TYPES = [
  'SCREENING', 'MEDICATION', 'LAB', 'ASSESSMENT', 'FOLLOW_UP',
  'IMMUNIZATION', 'REFERRAL', 'PROCEDURE', 'COUNSELING',
];

export interface TestDataFactoryOptions {
  phiMasking: PHIMasking | null;
  tenantId?: string;
  testPrefix?: string;
}

export interface TestPatient {
  id?: string;
  fhirId: string;
  tenantId: string;
  firstName: string;
  lastName: string;
  birthDate: string;
  gender: 'male' | 'female' | 'other';
  mrn: string;
  address?: TestAddress;
  phone?: string;
  email?: string;
  conditions?: TestCondition[];
}

export interface TestAddress {
  line: string[];
  city: string;
  state: string;
  postalCode: string;
  country: string;
}

export interface TestCondition {
  code: string;
  display: string;
  onsetDate: string;
  status: 'active' | 'resolved' | 'inactive';
}

export interface TestQualityMeasure {
  id?: string;
  code: string;
  name: string;
  version: string;
  category: string;
  status: 'active' | 'draft' | 'retired';
  measurePeriodStart: string;
  measurePeriodEnd: string;
}

export interface TestCareGap {
  id?: string;
  patientId: string;
  measureCode: string;
  gapType: string;
  urgency: 'HIGH' | 'MEDIUM' | 'LOW';
  status: 'OPEN' | 'CLOSED' | 'IN_PROGRESS';
  identifiedDate: string;
  dueDate: string;
  recommendation?: string;
}

export interface TestEvaluation {
  id?: string;
  patientId: string;
  measureCode: string;
  result: 'COMPLIANT' | 'NON_COMPLIANT' | 'NOT_ELIGIBLE';
  evaluationDate: string;
  numerator: boolean;
  denominator: boolean;
  source: string;
}

export class TestDataFactory {
  private options: Required<TestDataFactoryOptions>;
  private idCounter: number = 0;

  constructor(options: TestDataFactoryOptions) {
    this.options = {
      phiMasking: options.phiMasking,
      tenantId: options.tenantId || 'TENANT001',
      testPrefix: options.testPrefix || 'TEST_',
    };
  }

  /**
   * Generate unique test ID
   */
  private generateId(): string {
    return `${this.options.testPrefix}${Date.now()}_${++this.idCounter}`;
  }

  /**
   * Random element from array
   */
  private randomElement<T>(arr: T[]): T {
    return arr[Math.floor(Math.random() * arr.length)];
  }

  /**
   * Random date within range
   */
  private randomDate(startYear: number, endYear: number): string {
    const start = new Date(startYear, 0, 1);
    const end = new Date(endYear, 11, 31);
    const date = new Date(start.getTime() + Math.random() * (end.getTime() - start.getTime()));
    return date.toISOString().split('T')[0];
  }

  /**
   * Generate synthetic MRN
   */
  private generateMRN(): string {
    const num = Math.floor(Math.random() * 9000000) + 1000000;
    return `${this.options.testPrefix}MRN${num}`;
  }

  /**
   * Generate synthetic phone number
   */
  private generatePhone(): string {
    return `555-${String(Math.floor(Math.random() * 900) + 100)}-${String(Math.floor(Math.random() * 9000) + 1000)}`;
  }

  /**
   * Create a single test patient
   */
  createPatient(overrides?: Partial<TestPatient>): TestPatient {
    const firstName = this.randomElement(FIRST_NAMES);
    const lastName = this.randomElement(LAST_NAMES);
    const gender = this.randomElement(['male', 'female', 'other']) as TestPatient['gender'];

    const patient: TestPatient = {
      fhirId: this.generateId(),
      tenantId: this.options.tenantId,
      firstName,
      lastName,
      birthDate: this.randomDate(1940, 2005),
      gender,
      mrn: this.generateMRN(),
      address: {
        line: ['123 Test Street'],
        city: 'TestCity',
        state: 'TS',
        postalCode: '00000',
        country: 'US',
      },
      phone: this.generatePhone(),
      email: `${firstName.toLowerCase()}.${lastName.toLowerCase()}@test.example.com`,
      ...overrides,
    };

    return patient;
  }

  /**
   * Create multiple test patients
   */
  createPatients(count: number, overrides?: Partial<TestPatient>): TestPatient[] {
    return Array.from({ length: count }, () => this.createPatient(overrides));
  }

  /**
   * Create a patient with specific conditions
   */
  createPatientWithConditions(
    conditionCount: number,
    patientOverrides?: Partial<TestPatient>
  ): TestPatient {
    const conditions: TestCondition[] = Array.from({ length: conditionCount }, () => {
      const condition = this.randomElement(CONDITIONS);
      return {
        code: condition.code,
        display: condition.display,
        onsetDate: this.randomDate(2015, 2023),
        status: this.randomElement(['active', 'resolved', 'inactive']) as TestCondition['status'],
      };
    });

    return this.createPatient({
      ...patientOverrides,
      conditions,
    });
  }

  /**
   * Create a test quality measure
   */
  createQualityMeasure(overrides?: Partial<TestQualityMeasure>): TestQualityMeasure {
    const measureDef = this.randomElement(QUALITY_MEASURES);
    const currentYear = new Date().getFullYear();

    return {
      code: measureDef.code,
      name: measureDef.name,
      version: '2024',
      category: measureDef.category,
      status: 'active',
      measurePeriodStart: `${currentYear}-01-01`,
      measurePeriodEnd: `${currentYear}-12-31`,
      ...overrides,
    };
  }

  /**
   * Create multiple quality measures
   */
  createQualityMeasures(count: number): TestQualityMeasure[] {
    const measures: TestQualityMeasure[] = [];
    const usedCodes = new Set<string>();

    while (measures.length < count && usedCodes.size < QUALITY_MEASURES.length) {
      const measureDef = QUALITY_MEASURES[measures.length % QUALITY_MEASURES.length];
      if (!usedCodes.has(measureDef.code)) {
        usedCodes.add(measureDef.code);
        measures.push(this.createQualityMeasure({ code: measureDef.code, name: measureDef.name, category: measureDef.category }));
      }
    }

    return measures;
  }

  /**
   * Create a test care gap
   */
  createCareGap(patientId: string, overrides?: Partial<TestCareGap>): TestCareGap {
    const measureDef = this.randomElement(QUALITY_MEASURES);
    const identifiedDate = this.randomDate(2023, 2024);
    const dueDate = new Date(identifiedDate);
    dueDate.setMonth(dueDate.getMonth() + 3);

    return {
      patientId,
      measureCode: measureDef.code,
      gapType: this.randomElement(CARE_GAP_TYPES),
      urgency: this.randomElement(['HIGH', 'MEDIUM', 'LOW']) as TestCareGap['urgency'],
      status: 'OPEN',
      identifiedDate,
      dueDate: dueDate.toISOString().split('T')[0],
      recommendation: `Complete ${measureDef.name} for patient`,
      ...overrides,
    };
  }

  /**
   * Create multiple care gaps for a patient
   */
  createCareGaps(patientId: string, count: number): TestCareGap[] {
    return Array.from({ length: count }, () => this.createCareGap(patientId));
  }

  /**
   * Create a test evaluation result
   */
  createEvaluation(
    patientId: string,
    measureCode: string,
    overrides?: Partial<TestEvaluation>
  ): TestEvaluation {
    const result = this.randomElement(['COMPLIANT', 'NON_COMPLIANT', 'NOT_ELIGIBLE']) as TestEvaluation['result'];

    return {
      patientId,
      measureCode,
      result,
      evaluationDate: new Date().toISOString().split('T')[0],
      numerator: result === 'COMPLIANT',
      denominator: result !== 'NOT_ELIGIBLE',
      source: 'e2e_test',
      ...overrides,
    };
  }

  /**
   * Create a complete test scenario with patients, conditions, and care gaps
   */
  createTestScenario(config: {
    patientCount: number;
    conditionsPerPatient: number;
    careGapsPerPatient: number;
  }): {
    patients: TestPatient[];
    careGaps: TestCareGap[];
  } {
    const patients = Array.from({ length: config.patientCount }, () =>
      this.createPatientWithConditions(config.conditionsPerPatient)
    );

    const careGaps = patients.flatMap((patient) =>
      this.createCareGaps(patient.fhirId, config.careGapsPerPatient)
    );

    return { patients, careGaps };
  }

  /**
   * Create FHIR Bundle from test data
   */
  createFHIRBundle(patients: TestPatient[]): object {
    return {
      resourceType: 'Bundle',
      type: 'collection',
      entry: patients.map((patient) => ({
        resource: {
          resourceType: 'Patient',
          id: patient.fhirId,
          identifier: [
            {
              system: 'http://test.example.com/mrn',
              value: patient.mrn,
            },
          ],
          name: [
            {
              family: patient.lastName,
              given: [patient.firstName],
            },
          ],
          gender: patient.gender,
          birthDate: patient.birthDate,
          address: patient.address
            ? [
                {
                  line: patient.address.line,
                  city: patient.address.city,
                  state: patient.address.state,
                  postalCode: patient.address.postalCode,
                  country: patient.address.country,
                },
              ]
            : undefined,
          telecom: [
            patient.phone && { system: 'phone', value: patient.phone },
            patient.email && { system: 'email', value: patient.email },
          ].filter(Boolean),
        },
      })),
    };
  }
}
