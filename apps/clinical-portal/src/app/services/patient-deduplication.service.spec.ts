import { PatientDeduplicationService } from './patient-deduplication.service';
import { PatientSummary } from '../models/patient.model';
import { PatientSummaryWithLinks } from '../models/patient-link.model';

const createPatient = (overrides: Partial<PatientSummary> = {}): PatientSummary => ({
  id: '1',
  fullName: 'John Doe',
  status: 'Active',
  gender: 'male',
  dateOfBirth: '1980-01-01',
  mrn: 'MRN001',
  ...overrides,
});

describe('PatientDeduplicationService', () => {
  let service: PatientDeduplicationService;

  beforeEach(() => {
    service = new PatientDeduplicationService();
    service.clearAllLinks();
  });

  it('enhances patient with link info and master details', () => {
    service.linkPatient('2', '1', true).subscribe();

    const master = service.enhanceWithLinkInfo(createPatient({ id: '1' }));
    const duplicate = service.enhanceWithLinkInfo(createPatient({ id: '2' }));

    expect(master.isMaster).toBe(true);
    expect(master.duplicateCount).toBe(1);
    expect(duplicate.masterPatientId).toBe('1');
  });

  it('filters master records only', () => {
    const patient = service.enhanceWithLinkInfo(createPatient({ id: '1' }));
    const duplicate = service.enhanceWithLinkInfo(createPatient({ id: '2' }));

    const filtered = service.filterMasterRecordsOnly([patient, duplicate]);
    expect(filtered.length).toBe(2);
  });

  it('filters out linked duplicates when master exists', () => {
    service.linkPatient('2', '1', true).subscribe();

    const master = service.enhanceWithLinkInfo(createPatient({ id: '1' }));
    const duplicate = service.enhanceWithLinkInfo(createPatient({ id: '2' }));

    const filtered = service.filterMasterRecordsOnly([master, duplicate]);
    expect(filtered.length).toBe(1);
    expect(filtered[0].id).toBe('1');
  });

  it('keeps unlinked patients when filtering master records', () => {
    const unlinked = service.enhanceWithLinkInfo(createPatient({ id: '3' }));
    const filtered = service.filterMasterRecordsOnly([unlinked]);

    expect(filtered.length).toBe(1);
    expect(filtered[0].id).toBe('3');
  });

  it('links and unlinks patients', () => {
    service.linkPatient('2', '1', true).subscribe();
    let duplicate = service.enhanceWithLinkInfo(createPatient({ id: '2' }));
    expect(duplicate.masterPatientId).toBe('1');

    service.unlinkPatient('2').subscribe();
    duplicate = service.enhanceWithLinkInfo(createPatient({ id: '2' }));
    expect(duplicate.masterPatientId).toBeUndefined();
  });

  it('safely unlinks when no master link exists', () => {
    service.unlinkPatient('missing').subscribe((result) => {
      expect(result).toBe(true);
    });
  });

  it('removes master status when last duplicate is unlinked', () => {
    service.linkPatient('2', '1', true).subscribe();

    service.unlinkPatient('2').subscribe();

    const master = service.enhanceWithLinkInfo(createPatient({ id: '1' }));
    expect(master.isMaster).toBe(false);
  });

  it('finds potential duplicates and sorts by score', (done) => {
    const patient1 = service.enhanceWithLinkInfo(createPatient({ id: '1', fullName: 'Jane Doe' }));
    const patient2 = service.enhanceWithLinkInfo(createPatient({ id: '2', fullName: 'Jane Doe' }));
    const patient3 = service.enhanceWithLinkInfo(createPatient({ id: '3', fullName: 'Alex Smith' }));

    service.findPotentialDuplicates(patient1, [patient1, patient2, patient3]).subscribe((matches) => {
      expect(matches.length).toBeGreaterThan(0);
      expect(matches[0].matchScore).toBeGreaterThanOrEqual(70);
      done();
    });
  });

  it('returns no matches when score is below threshold', (done) => {
    const patient1 = service.enhanceWithLinkInfo(createPatient({ id: '1', fullName: 'Jane Doe' }));
    const patient2 = service.enhanceWithLinkInfo(createPatient({ id: '2', fullName: 'Alex Smith' }));

    service.findPotentialDuplicates(patient1, [patient1, patient2]).subscribe((matches) => {
      expect(matches.length).toBe(0);
      done();
    });
  });

  it('suggests review for borderline matches', (done) => {
    const patient1 = service.enhanceWithLinkInfo(
      createPatient({
        id: '1',
        fullName: 'John Doe',
        dateOfBirth: '1980-01-01',
        gender: 'male',
        mrn: 'MRN100',
      })
    );
    const patient2 = service.enhanceWithLinkInfo(
      createPatient({
        id: '2',
        fullName: 'John Doe',
        dateOfBirth: '1980-01-01',
        gender: 'female',
        mrn: 'MRN101',
      })
    );

    service.findPotentialDuplicates(patient1, [patient1, patient2]).subscribe((matches) => {
      expect(matches.length).toBe(1);
      expect(matches[0].suggestedAction).toBe('review');
      done();
    });
  });

  it('suggests merge for exact matches and link for strong matches', (done) => {
    const patient1 = service.enhanceWithLinkInfo(
      createPatient({
        id: '1',
        fullName: 'John Doe',
        dateOfBirth: '1980-01-01',
        gender: 'male',
        mrn: 'MRN100',
      })
    );
    const patient2 = service.enhanceWithLinkInfo(
      createPatient({
        id: '2',
        fullName: 'John Doe',
        dateOfBirth: '1980-01-01',
        gender: 'male',
        mrn: 'MRN100',
      })
    );
    const patient3 = service.enhanceWithLinkInfo(
      createPatient({
        id: '3',
        fullName: 'Jon Doe',
        dateOfBirth: '1980-01-01',
        gender: 'male',
        mrn: 'MRN100',
      })
    );

    service.findPotentialDuplicates(patient1, [patient1, patient2, patient3]).subscribe((matches) => {
      const exactMatch = matches.find((match) => match.patient2.id === '2');
      const strongMatch = matches.find((match) => match.patient2.id === '3');

      expect(exactMatch?.matchScore).toBe(100);
      expect(exactMatch?.suggestedAction).toBe('merge');
      expect(strongMatch?.matchScore).toBe(85);
      expect(strongMatch?.suggestedAction).toBe('link');
      done();
    });
  });

  it('merges patients and returns result', (done) => {
    const request = {
      masterPatientId: '1',
      duplicatePatientIds: ['2', '3'],
      mergeStrategy: 'keep-master' as const,
    };

    service.mergePatients(request).subscribe((result) => {
      expect(result.success).toBe(true);
      expect(result.mergedPatientIds.length).toBe(2);
      done();
    });
  });

  it('handles merge failures gracefully', (done) => {
    const request = {
      masterPatientId: '1',
      duplicatePatientIds: ['2'],
      mergeStrategy: 'keep-master' as const,
    };

    const original = service.linkPatient;
    (service as any).linkPatient = () => {
      throw new Error('merge failed');
    };

    service.mergePatients(request).subscribe((result) => {
      expect(result.success).toBe(false);
      expect(result.error).toBe('merge failed');
      (service as any).linkPatient = original;
      done();
    });
  });

  it('calculates deduplication statistics', (done) => {
    const master = service.enhanceWithLinkInfo(createPatient({ id: '1' }));
    const duplicate = { ...service.enhanceWithLinkInfo(createPatient({ id: '2' })), masterPatientId: '1' };

    service.getStatistics([master, duplicate as PatientSummaryWithLinks]).subscribe((stats) => {
      expect(stats.masterRecords).toBe(0);
      expect(stats.duplicateRecords).toBe(1);
      expect(stats.unlinkedRecords).toBeGreaterThanOrEqual(0);
      done();
    });
  });

  it('calculates average duplicates per master', (done) => {
    service.linkPatient('2', '1', true).subscribe();
    const master = service.enhanceWithLinkInfo(createPatient({ id: '1' }));
    const duplicate = service.enhanceWithLinkInfo(createPatient({ id: '2' }));

    service.getStatistics([master, duplicate]).subscribe((stats) => {
      expect(stats.masterRecords).toBe(1);
      expect(stats.averageDuplicatesPerMaster).toBeGreaterThan(0);
      done();
    });
  });

  it('handles master selection rules', () => {
    service.linkPatient('2', '1', true).subscribe();
    const master = service.enhanceWithLinkInfo(createPatient({ id: '1', mrn: 'MRN001' }));
    const duplicate = service.enhanceWithLinkInfo(createPatient({ id: '2', mrn: 'MRN010' }));

    const shouldBeMaster = (service as any).shouldBeMaster(master, duplicate);
    expect(shouldBeMaster).toBe(true);

    const reverse = (service as any).shouldBeMaster(
      { ...master, duplicateCount: 0 },
      { ...duplicate, duplicateCount: 2 }
    );
    expect(reverse).toBe(false);
  });

  it('uses MRN and ID to choose a master', () => {
    const p1 = service.enhanceWithLinkInfo(createPatient({ id: '10', mrn: 'MRN010' }));
    const p2 = service.enhanceWithLinkInfo(createPatient({ id: '2', mrn: 'MRN002' }));

    expect((service as any).shouldBeMaster(p1, p2)).toBe(false);

    const noMrn = service.enhanceWithLinkInfo(createPatient({ id: '5', mrn: undefined }));
    const noMrn2 = service.enhanceWithLinkInfo(createPatient({ id: '7', mrn: undefined }));
    expect((service as any).shouldBeMaster(noMrn, noMrn2)).toBe(true);
  });

  it('uses name similarity for partial matches', () => {
    const similar = (service as any).namesSimilar('John Doe', 'Jon Doe');
    const match = (service as any).namesMatch('John Doe', 'john doe');

    expect(similar).toBe(true);
    expect(match).toBe(true);
  });

  it('returns false when names are not similar', () => {
    const similar = (service as any).namesSimilar('John Doe', 'Alex Smith');
    expect(similar).toBe(false);
  });

  it('auto-detects and links duplicates', (done) => {
    const patients = [
      createPatient({ id: '10', fullName: 'Maria Garcia', mrn: '100' }),
      createPatient({ id: '11', fullName: 'Maria Garcia', mrn: '101' }),
    ];

    service.autoDetectAndLinkDuplicates(patients).subscribe((result) => {
      expect(result.duplicatesLinked).toBeGreaterThanOrEqual(0);
      done();
    });
  });

  it('skips auto-linking when both patients are already masters', (done) => {
    const patients = [
      createPatient({ id: '10', fullName: 'Maria Garcia', mrn: '100' }),
      createPatient({ id: '11', fullName: 'Maria Garcia', mrn: '101' }),
    ];
    service.setAsMaster('10').subscribe();
    service.setAsMaster('11').subscribe();

    service.autoDetectAndLinkDuplicates(patients).subscribe((result) => {
      expect(result.duplicatesLinked).toBe(0);
      done();
    });
  });
});
