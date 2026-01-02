import { CqlLibrary, LibraryStatus, MeasureInfo } from '../../app/models/cql-library.model';

/**
 * Factory for creating mock CQL Library objects for testing
 */
export class CqlLibraryFactory {
  private static counter = 1;

  /**
   * Create a full CQL Library with all fields
   */
  static create(overrides?: Partial<CqlLibrary>): CqlLibrary {
    const id = `lib-${this.counter++}`;
    const name = overrides?.name || `HEDIS-CDC`;
    const version = overrides?.version || '1.0.0';

    return {
      id: overrides?.id || id,
      tenantId: overrides?.tenantId || 'tenant-1',
      name,
      libraryName: name,
      version,
      status: overrides?.status || 'ACTIVE',
      cqlContent: overrides?.cqlContent || this.generateCqlContent(name),
      elmJson: overrides?.elmJson || '{}',
      elmXml: overrides?.elmXml || '<library/>',
      description: overrides?.description || `${name} - Diabetes HbA1c Control`,
      publisher: overrides?.publisher || 'NCQA',
      fhirLibraryId: overrides?.fhirLibraryId || `fhir-${id}`,
      active: overrides?.active !== undefined ? overrides.active : true,
      createdAt: overrides?.createdAt || '2024-01-01T10:00:00Z',
      updatedAt: overrides?.updatedAt || '2024-01-01T10:00:00Z',
      createdBy: overrides?.createdBy || 'system',
    };
  }

  /**
   * Create multiple libraries
   */
  static createMany(count: number, overrides?: Partial<CqlLibrary>): CqlLibrary[] {
    return Array.from({ length: count }, () => this.create(overrides));
  }

  /**
   * Create HEDIS CDC measure
   */
  static createHedisCdc(): CqlLibrary {
    return this.create({
      name: 'HEDIS-CDC',
      version: '1.0.0',
      description: 'Comprehensive Diabetes Care - HbA1c Control',
      status: 'ACTIVE',
    });
  }

  /**
   * Create HEDIS CBP measure
   */
  static createHedisCbp(): CqlLibrary {
    return this.create({
      name: 'HEDIS-CBP',
      version: '1.0.0',
      description: 'Controlling High Blood Pressure',
      status: 'ACTIVE',
    });
  }

  /**
   * Create CMS measure
   */
  static createCms134(): CqlLibrary {
    return this.create({
      name: 'CMS-134',
      version: '2.0.0',
      description: 'Preventive Care and Screening',
      status: 'ACTIVE',
    });
  }

  /**
   * Create draft library
   */
  static createDraft(): CqlLibrary {
    return this.create({
      status: 'DRAFT',
      active: false,
    });
  }

  /**
   * Create retired library
   */
  static createRetired(): CqlLibrary {
    return this.create({
      status: 'RETIRED',
      active: false,
    });
  }

  /**
   * Create MeasureInfo (simplified)
   */
  static createMeasureInfo(overrides?: Partial<MeasureInfo>): MeasureInfo {
    const lib = this.create();
    return {
      id: overrides?.id || lib.id,
      name: overrides?.name || lib.name,
      version: overrides?.version || lib.version,
      description: overrides?.description || lib.description,
      category: overrides?.category || 'HEDIS',
      displayName: overrides?.displayName || `${lib.name} v${lib.version} - ${lib.description}`,
    };
  }

  /**
   * Create multiple MeasureInfo objects
   */
  static createMeasureInfoList(): MeasureInfo[] {
    return [
      {
        id: 'lib-1',
        name: 'HEDIS-CDC',
        version: '1.0.0',
        description: 'Comprehensive Diabetes Care',
        category: 'HEDIS',
        displayName: 'HEDIS-CDC v1.0.0 - Comprehensive Diabetes Care',
      },
      {
        id: 'lib-2',
        name: 'HEDIS-CBP',
        version: '1.0.0',
        description: 'Controlling High Blood Pressure',
        category: 'HEDIS',
        displayName: 'HEDIS-CBP v1.0.0 - Controlling High Blood Pressure',
      },
      {
        id: 'lib-3',
        name: 'CMS-134',
        version: '2.0.0',
        description: 'Preventive Care and Screening',
        category: 'CMS',
        displayName: 'CMS-134 v2.0.0 - Preventive Care and Screening',
      },
    ];
  }

  /**
   * Reset counter for tests
   */
  static reset(): void {
    this.counter = 1;
  }

  /**
   * Generate simple CQL content
   */
  private static generateCqlContent(name: string): string {
    return `library ${name} version '1.0.0'
using FHIR version '4.0.1'

define InDenominator: true
define InNumerator: true
`;
  }
}
