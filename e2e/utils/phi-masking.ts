/**
 * PHI Masking Utility for HIPAA Compliance
 *
 * This utility ensures Protected Health Information (PHI) is properly
 * masked in test outputs, logs, screenshots, and error messages.
 *
 * HIPAA Identifiers (18 types):
 * 1. Names
 * 2. Geographic data (smaller than state)
 * 3. Dates (except year)
 * 4. Phone numbers
 * 5. Fax numbers
 * 6. Email addresses
 * 7. SSN
 * 8. Medical record numbers
 * 9. Health plan beneficiary numbers
 * 10. Account numbers
 * 11. Certificate/license numbers
 * 12. Vehicle identifiers
 * 13. Device identifiers
 * 14. URLs
 * 15. IP addresses
 * 16. Biometric identifiers
 * 17. Photos
 * 18. Any unique identifying number
 */

export interface PHIMaskingOptions {
  enabled: boolean;
  maskChar?: string;
  preserveLength?: boolean;
  logMaskedContent?: boolean;
}

export interface MaskingResult {
  masked: string;
  containedPHI: boolean;
  maskedFields: string[];
}

export class PHIMasking {
  private options: Required<PHIMaskingOptions>;

  // Regex patterns for PHI detection
  private static readonly PATTERNS = {
    // Social Security Number
    ssn: /\b\d{3}[-.\s]?\d{2}[-.\s]?\d{4}\b/g,

    // Phone numbers
    phone: /\b(?:\+?1[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}\b/g,

    // Email addresses
    email: /\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b/g,

    // Medical Record Numbers (common formats)
    mrn: /\b(?:MRN|MR#|Medical Record|Patient ID)[:\s#]*([A-Z0-9]{5,15})\b/gi,

    // Dates (MM/DD/YYYY, YYYY-MM-DD, etc.) - preserve year
    dateWithDay: /\b(?:0[1-9]|1[0-2])[-/](?:0[1-9]|[12]\d|3[01])[-/](?:19|20)\d{2}\b/g,
    isoDate: /\b(?:19|20)\d{2}[-/](?:0[1-9]|1[0-2])[-/](?:0[1-9]|[12]\d|3[01])\b/g,

    // IP addresses
    ipv4: /\b(?:(?:25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(?:25[0-5]|2[0-4]\d|[01]?\d\d?)\b/g,

    // Credit card numbers
    creditCard: /\b(?:\d{4}[-.\s]?){3}\d{4}\b/g,

    // Account numbers
    accountNumber: /\b(?:Account|Acct)[:\s#]*(\d{8,16})\b/gi,

    // Driver's license (various state formats)
    driversLicense: /\b[A-Z]{1,2}\d{6,8}\b/g,

    // Health plan IDs
    healthPlanId: /\b(?:Member|Subscriber|Group|Policy)[:\s#]*([A-Z0-9]{6,15})\b/gi,

    // ZIP codes (5 or 9 digit)
    zipCode: /\b\d{5}(?:-\d{4})?\b/g,

    // Street addresses
    streetAddress: /\b\d{1,5}\s+(?:[A-Za-z]+\s+){1,3}(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Drive|Dr|Lane|Ln|Way|Court|Ct|Circle|Cir)\b/gi,
  };

  constructor(options: PHIMaskingOptions) {
    this.options = {
      enabled: options.enabled,
      maskChar: options.maskChar || '*',
      preserveLength: options.preserveLength ?? true,
      logMaskedContent: options.logMaskedContent ?? false,
    };
  }

  /**
   * Mask PHI in a string
   */
  mask(content: string): MaskingResult {
    if (!this.options.enabled) {
      return { masked: content, containedPHI: false, maskedFields: [] };
    }

    let masked = content;
    const maskedFields: string[] = [];

    // Apply each pattern
    for (const [fieldType, pattern] of Object.entries(PHIMasking.PATTERNS)) {
      const matches = content.match(pattern);
      if (matches) {
        maskedFields.push(fieldType);
        masked = masked.replace(pattern, (match) => this.maskValue(match, fieldType));
      }
    }

    // Log masked content if enabled
    if (this.options.logMaskedContent && maskedFields.length > 0) {
      console.log(`PHI Masking: Masked ${maskedFields.length} field types: ${maskedFields.join(', ')}`);
    }

    return {
      masked,
      containedPHI: maskedFields.length > 0,
      maskedFields,
    };
  }

  /**
   * Mask a specific value
   */
  private maskValue(value: string, fieldType: string): string {
    const maskChar = this.options.maskChar;

    if (this.options.preserveLength) {
      // Preserve format characters for certain types
      switch (fieldType) {
        case 'ssn':
          return value.replace(/\d/g, maskChar);
        case 'phone':
          return value.replace(/\d/g, maskChar);
        case 'email':
          const [localPart, domain] = value.split('@');
          return `${maskChar.repeat(localPart.length)}@${maskChar.repeat(domain.length)}`;
        case 'dateWithDay':
        case 'isoDate':
          // Preserve year, mask month and day
          return value.replace(/\d{2}(?=[-/])/g, maskChar.repeat(2));
        case 'creditCard':
          return value.replace(/\d/g, maskChar);
        default:
          return maskChar.repeat(value.length);
      }
    }

    return `[${fieldType.toUpperCase()}_MASKED]`;
  }

  /**
   * Mask PHI in an object recursively
   */
  maskObject<T extends object>(obj: T): T {
    if (!this.options.enabled) {
      return obj;
    }

    const masked = { ...obj } as T;

    for (const key of Object.keys(masked)) {
      const value = (masked as any)[key];

      if (typeof value === 'string') {
        (masked as any)[key] = this.mask(value).masked;
      } else if (typeof value === 'object' && value !== null) {
        if (Array.isArray(value)) {
          (masked as any)[key] = value.map((item) =>
            typeof item === 'string'
              ? this.mask(item).masked
              : typeof item === 'object'
              ? this.maskObject(item)
              : item
          );
        } else {
          (masked as any)[key] = this.maskObject(value);
        }
      }
    }

    return masked;
  }

  /**
   * Check if content contains PHI
   */
  containsPHI(content: string): boolean {
    for (const pattern of Object.values(PHIMasking.PATTERNS)) {
      if (pattern.test(content)) {
        return true;
      }
      // Reset regex state
      pattern.lastIndex = 0;
    }
    return false;
  }

  /**
   * Get list of PHI fields found in content
   */
  detectPHI(content: string): string[] {
    const detectedFields: string[] = [];

    for (const [fieldType, pattern] of Object.entries(PHIMasking.PATTERNS)) {
      if (pattern.test(content)) {
        detectedFields.push(fieldType);
      }
      // Reset regex state
      pattern.lastIndex = 0;
    }

    return detectedFields;
  }

  /**
   * Create a safe error message by masking PHI
   */
  safeError(error: Error | string): string {
    const message = typeof error === 'string' ? error : error.message;
    return this.mask(message).masked;
  }

  /**
   * Mask PHI in console output
   */
  safeLog(...args: any[]): void {
    const maskedArgs = args.map((arg) => {
      if (typeof arg === 'string') {
        return this.mask(arg).masked;
      } else if (typeof arg === 'object' && arg !== null) {
        return this.maskObject(arg);
      }
      return arg;
    });

    console.log(...maskedArgs);
  }
}

/**
 * Default PHI masking instance for tests
 */
export const defaultPHIMasking = new PHIMasking({
  enabled: process.env.PHI_MASKING !== 'false',
  maskChar: '*',
  preserveLength: true,
  logMaskedContent: process.env.DEBUG_PHI_MASKING === 'true',
});
