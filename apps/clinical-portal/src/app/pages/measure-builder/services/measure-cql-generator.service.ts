import { Injectable } from '@angular/core';
import {
  SliderConfig,
  MeasureAlgorithm,
  PopulationBlock,
  RangeSliderConfig,
  ThresholdSliderConfig,
} from '../models/measure-builder.model';

/**
 * Service to generate CQL code from slider configurations
 * Translates visual builder slider values into valid CQL conditions
 */
@Injectable({
  providedIn: 'root',
})
export class MeasureCqlGeneratorService {
  /**
   * Generate complete CQL from algorithm and slider configurations
   */
  generateCompleteCql(
    algorithm: MeasureAlgorithm,
    sliderConfigs: SliderConfig[],
    measureName: string
  ): string {
    const cqlLines: string[] = [];

    // Header
    cqlLines.push('// Clinical Quality Language (CQL) for: ' + measureName);
    cqlLines.push('library ' + this.sanitizeLibraryName(measureName) + ' version \'1.0.0\'');
    cqlLines.push('');

    // Using statements
    cqlLines.push('using FHIR version \'4.0.1\'');
    cqlLines.push('');

    // Generate slider-based definitions
    cqlLines.push('// Slider-Based Definitions');
    cqlLines.push('// Generated from visual builder configurations');
    cqlLines.push('');

    for (const slider of sliderConfigs) {
      // Access value based on slider type
      if (slider.type === 'range-dual' || slider.type === 'range-single' || slider.type === 'threshold' || slider.type === 'period-selector') {
        cqlLines.push(slider.cqlGenerator(slider.value));
      } else if (slider.type === 'distribution') {
        cqlLines.push(slider.cqlGenerator(slider.components));
      }
      cqlLines.push('');
    }

    // Generate population definitions
    cqlLines.push('// Population Criteria Definitions');
    cqlLines.push('');

    cqlLines.push(this.generatePopulationDefinition(algorithm.initialPopulation));
    cqlLines.push('');

    cqlLines.push(this.generatePopulationDefinition(algorithm.denominator));
    cqlLines.push('');

    cqlLines.push(this.generatePopulationDefinition(algorithm.numerator));
    cqlLines.push('');

    if (algorithm.exclusions && algorithm.exclusions.length > 0) {
      for (let i = 0; i < algorithm.exclusions.length; i++) {
        cqlLines.push(this.generatePopulationDefinition(algorithm.exclusions[i]));
        cqlLines.push('');
      }
    }

    if (algorithm.exceptions && algorithm.exceptions.length > 0) {
      for (let i = 0; i < algorithm.exceptions.length; i++) {
        cqlLines.push(this.generatePopulationDefinition(algorithm.exceptions[i]));
        cqlLines.push('');
      }
    }

    // Measure definition
    cqlLines.push('// Measure Definition');
    cqlLines.push('define "Measure Score":');
    cqlLines.push(
      '  Count("Numerator") / Count("Denominator") * 100'
    );

    return cqlLines.join('\n');
  }

  /**
   * Generate CQL definition for a population block
   */
  private generatePopulationDefinition(block: PopulationBlock): string {
    const defType = this.getDefinitionType(block.type);
    return `define "${defType}":\n  ${block.condition}`;
  }

  /**
   * Get CQL definition type name from population type
   */
  private getDefinitionType(type: string): string {
    const typeMap: Record<string, string> = {
      initial: 'Initial Population',
      denominator: 'Denominator',
      numerator: 'Numerator',
      exclusion: 'Denominator Exclusion',
      exception: 'Denominator Exception',
    };
    return typeMap[type] || 'Custom Definition';
  }

  /**
   * Generate CQL for age range slider
   */
  generateAgeRangeCql(value: number | number[]): string {
    if (Array.isArray(value)) {
      const [min, max] = value;
      return `define "Age Range":\n  AgeInYears() >= ${min} AND AgeInYears() <= ${max}`;
    }
    return `define "Age Range":\n  AgeInYears() >= ${value}`;
  }

  /**
   * Generate CQL for threshold slider (clinical parameter)
   */
  generateThresholdCql(label: string, value: number, unit: string = ''): string {
    // Map common threshold parameters to CQL functions
    const thresholdMap: Record<string, (val: number) => string> = {
      'HbA1c Target': (v) => `define "HbA1c Control":\n  MostRecentObservation("2345-7") < ${v}`,
      'Blood Pressure': (v) => `define "BP Control":\n  MostRecentBP() < ${v}/80`,
      BMI: (v) => `define "BMI Target":\n  MostRecentBMI() < ${v}`,
      'Cholesterol Level': (v) => `define "Cholesterol Control":\n  MostRecentLDL() < ${v}`,
    };

    if (thresholdMap[label]) {
      return thresholdMap[label](value);
    }

    // Fallback generic threshold
    return `define "${label}":\n  LastValue("${label}") < ${value}${unit ? ' ' + unit : ''}`;
  }

  /**
   * Generate CQL for period selector (timing configuration)
   */
  generatePeriodCql(periodDays: number): string {
    return `define "Measurement Period":\n  Interval[Today() - ${periodDays} days, Today()]`;
  }

  /**
   * Generate CQL for observation requirement
   */
  generateObservationRequiredCql(
    observationType: string,
    periodDays: number,
    resultRangeMin?: number,
    resultRangeMax?: number
  ): string {
    const cqlLines: string[] = [];
    cqlLines.push(`define "${observationType} Recorded":`);
    cqlLines.push(
      `  exists([Observation] O where O.status in {'final', 'amended', 'corrected'}`
    );
    cqlLines.push(`    and O.code.coding ~ '${observationType}'`);
    cqlLines.push(
      `    and O.effective.toInterval() during "Measurement Period"`
    );

    if (resultRangeMin !== undefined && resultRangeMax !== undefined) {
      cqlLines.push(
        `    and O.value.value >= ${resultRangeMin} and O.value.value <= ${resultRangeMax}`
      );
    }

    cqlLines.push(`  )`);

    return cqlLines.join('\n');
  }

  /**
   * Generate CQL for medication presence
   */
  generateMedicationCql(medicationCode: string, periodDays: number): string {
    return `define "On ${medicationCode}":\n  exists([MedicationRequest] MR where MR.medication.coding ~ '${medicationCode}'\n    and MR.authoredOn during "Measurement Period")`;
  }

  /**
   * Generate CQL for encounter requirement
   */
  generateEncounterCql(
    encounterType: string,
    minCount: number = 1,
    periodDays: number = 365
  ): string {
    return `define "${encounterType} Encounter":\n  Count([Encounter] E where E.type.coding ~ '${encounterType}'\n    and E.period overlaps "Measurement Period") >= ${minCount}`;
  }

  /**
   * Generate CQL for composite measure with weighted components
   */
  generateCompositeWeightsCql(components: Array<{ name: string; weight: number }>): string {
    const cqlLines: string[] = [];
    cqlLines.push('define "Composite Score":');
    cqlLines.push('  (');

    const conditions = components
      .map((c) => `"${c.name}" * ${c.weight / 100}`)
      .join(' + ');

    cqlLines.push(`    ${conditions}`);
    cqlLines.push('  ) * 100');

    return cqlLines.join('\n');
  }

  /**
   * Sanitize library name for CQL (remove spaces, special chars)
   */
  private sanitizeLibraryName(name: string): string {
    return name
      .replace(/[^a-zA-Z0-9_]/g, '_')
      .replace(/^[0-9]/, '_')
      .substring(0, 50);
  }

  /**
   * Validate CQL syntax (basic validation)
   */
  validateCql(cql: string): { valid: boolean; errors: string[] } {
    const errors: string[] = [];

    // Check for library declaration
    if (!cql.includes('library')) {
      errors.push('Missing library declaration');
    }

    // Check for using statement
    if (!cql.includes('using')) {
      errors.push('Missing using statement');
    }

    // Check for balanced braces and parentheses
    const openBraces = (cql.match(/{/g) || []).length;
    const closeBraces = (cql.match(/}/g) || []).length;
    const openParens = (cql.match(/\(/g) || []).length;
    const closeParens = (cql.match(/\)/g) || []).length;

    if (openBraces !== closeBraces) {
      errors.push('Unbalanced braces { }');
    }

    if (openParens !== closeParens) {
      errors.push('Unbalanced parentheses ( )');
    }

    return {
      valid: errors.length === 0,
      errors,
    };
  }

  /**
   * Extract function calls from CQL to identify dependencies
   */
  extractFunctionCalls(cql: string): string[] {
    const functionPattern = /([a-zA-Z_][a-zA-Z0-9_]*)\s*\(/g;
    const matches = cql.match(functionPattern) || [];
    return [...new Set(matches.map((m) => m.replace(/\s*\($/, '')))];
  }

  /**
   * Format CQL with proper indentation and syntax highlighting hints
   */
  formatCql(cql: string): string {
    let formatted = cql;
    let indentLevel = 0;

    const lines = formatted.split('\n');
    const formattedLines = lines.map((line) => {
      const trimmed = line.trim();

      if (trimmed.startsWith('}')) {
        indentLevel--;
      }

      const result = '  '.repeat(Math.max(0, indentLevel)) + trimmed;

      if (trimmed.endsWith('{')) {
        indentLevel++;
      }

      return result;
    });

    return formattedLines.join('\n');
  }

  /**
   * Generate CQL documentation comments
   */
  generateDocumentation(
    measureName: string,
    description?: string,
    populationCriteria?: Record<string, string>
  ): string {
    const doc: string[] = [];
    doc.push(`/**`);
    doc.push(` * Measure: ${measureName}`);
    if (description) {
      doc.push(` * Description: ${description}`);
    }
    doc.push(` * Generated by Visual Measure Builder`);
    doc.push(` * Generated at: ${new Date().toISOString()}`);
    doc.push(` */`);
    doc.push('');

    if (populationCriteria) {
      doc.push('// Population Definitions:');
      for (const [key, value] of Object.entries(populationCriteria)) {
        doc.push(`// ${key}: ${value}`);
      }
      doc.push('');
    }

    return doc.join('\n');
  }
}
