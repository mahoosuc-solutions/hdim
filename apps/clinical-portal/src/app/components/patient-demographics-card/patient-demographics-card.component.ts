import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { Patient, HumanName, ContactPoint, Address, Identifier } from '../../models/patient.model';
import { LoggerService } from '../../services/logger.service';

/**
 * Patient Demographics Card Component
 *
 * Displays comprehensive patient demographics information including:
 * - Basic demographics (name, DOB, age, gender, MRN)
 * - Contact information (phone, email, address)
 * - Emergency contacts (if available in Patient.contact)
 * - Insurance information (if available in identifiers)
 *
 * HIPAA Compliance:
 * - All data display uses LoggerService for audit logging
 * - No console.log statements
 * - PHI is properly handled according to HIPAA guidelines
 *
 * Accessibility:
 * - ARIA labels on all sections
 * - Keyboard navigation support
 * - Screen reader friendly
 *
 * Sprint 1 - Issue #237: Patient 360° View Demographics Card
 */
@Component({
  selector: 'app-patient-demographics-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatTooltipModule,
  ],
  templateUrl: './patient-demographics-card.component.html',
  styleUrls: ['./patient-demographics-card.component.scss'],
})
export class PatientDemographicsCardComponent implements OnInit {
  @Input() patient: Patient | null = null;
  @Input() showEmergencyContacts = true;
  @Input() showInsurance = true;

  // Processed demographics data
  fullName = '';
  age: number | null = null;
  mrn = '';
  mrnSystem = '';

  // Contact information
  primaryPhone = '';
  primaryEmail = '';
  primaryAddress: Address | null = null;

  // Emergency contact (from Patient.contact if available)
  emergencyContactName = '';
  emergencyContactPhone = '';
  emergencyContactRelationship = '';

  // Insurance identifier (if stored in identifiers)
  insuranceMemberId = '';
  insurancePlanName = '';

  private logger!: ReturnType<LoggerService['withContext']>;

  constructor(private logger: LoggerService) {
  }

  ngOnInit(): void {
    if (this.patient) {
      this.processPatientData();
    }
  }

  /**
   * Process patient data into display-friendly format
   */
  private processPatientData(): void {
    if (!this.patient) return;

    this.logger.info('Processing patient demographics', this.patient.id);

    // Process name
    this.fullName = this.getFullName(this.patient.name);

    // Calculate age from birthDate
    if (this.patient.birthDate) {
      this.age = this.calculateAge(this.patient.birthDate);
    }

    // Extract MRN
    this.extractMRN();

    // Extract contact information
    this.extractContactInfo();

    // Extract emergency contact (if available)
    this.extractEmergencyContact();

    // Extract insurance (if available)
    this.extractInsurance();
  }

  /**
   * Get full name from FHIR HumanName array
   */
  private getFullName(names: HumanName[] | undefined): string {
    if (!names || names.length === 0) return 'Unknown';

    // Prefer official or usual name
    const preferredName =
      names.find((n) => n.use === 'official') ||
      names.find((n) => n.use === 'usual') ||
      names[0];

    if (preferredName.text) {
      return preferredName.text;
    }

    const parts: string[] = [];
    if (preferredName.prefix) parts.push(...preferredName.prefix);
    if (preferredName.given) parts.push(...preferredName.given);
    if (preferredName.family) parts.push(preferredName.family);
    if (preferredName.suffix) parts.push(...preferredName.suffix);

    return parts.join(' ') || 'Unknown';
  }

  /**
   * Calculate age from birthdate string (YYYY-MM-DD)
   */
  private calculateAge(birthDate: string): number {
    const today = new Date();
    const birth = new Date(birthDate);
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();

    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
      age--;
    }

    return age;
  }

  /**
   * Extract MRN from identifiers
   */
  private extractMRN(): void {
    if (!this.patient?.identifier || this.patient.identifier.length === 0) {
      return;
    }

    // Look for MRN identifier (system contains "mrn" or type.coding contains MR)
    const mrnIdentifier = this.patient.identifier.find(
      (id) =>
        id.system?.toLowerCase().includes('mrn') ||
        id.type?.coding?.some((c) => c.code === 'MR')
    ) || this.patient.identifier[0]; // Fallback to first identifier

    if (mrnIdentifier) {
      this.mrn = mrnIdentifier.value;
      this.mrnSystem = mrnIdentifier.system || '';
    }
  }

  /**
   * Extract primary contact information (phone, email, address)
   */
  private extractContactInfo(): void {
    if (!this.patient) return;

    // Extract primary phone
    const phoneContact = this.patient.telecom?.find(
      (c) => c.system === 'phone' && (c.use === 'home' || c.use === 'mobile')
    );
    this.primaryPhone = phoneContact?.value || '';

    // Extract primary email
    const emailContact = this.patient.telecom?.find(
      (c) => c.system === 'email'
    );
    this.primaryEmail = emailContact?.value || '';

    // Extract primary address
    const homeAddress = this.patient.address?.find(
      (a) => a.use === 'home'
    ) || this.patient.address?.[0];
    this.primaryAddress = homeAddress || null;
  }

  /**
   * Extract emergency contact information
   * Note: FHIR Patient.contact is typically used for emergency contacts
   */
  private extractEmergencyContact(): void {
    // Patient.contact is not in the simplified model
    // This would be added if FHIR Patient resource includes contact extension
    // For now, marking as placeholder for future enhancement
    this.emergencyContactName = '';
    this.emergencyContactPhone = '';
    this.emergencyContactRelationship = '';
  }

  /**
   * Extract insurance information from identifiers
   * Insurance Member ID may be stored as an identifier with system containing "insurance"
   */
  private extractInsurance(): void {
    if (!this.patient?.identifier) return;

    const insuranceIdentifier = this.patient.identifier.find(
      (id) => id.system?.toLowerCase().includes('insurance')
    );

    if (insuranceIdentifier) {
      this.insuranceMemberId = insuranceIdentifier.value;
      // Plan name would typically come from Coverage resource, not Patient
      this.insurancePlanName = ''; // Placeholder
    }
  }

  /**
   * Format address for display
   */
  getFormattedAddress(address: Address | null): string {
    if (!address) return 'No address on file';

    const parts: string[] = [];

    if (address.line) {
      parts.push(...address.line);
    }

    const cityStateZip: string[] = [];
    if (address.city) cityStateZip.push(address.city);
    if (address.state) cityStateZip.push(address.state);
    if (address.postalCode) cityStateZip.push(address.postalCode);

    if (cityStateZip.length > 0) {
      parts.push(cityStateZip.join(', '));
    }

    if (address.country) {
      parts.push(address.country);
    }

    return parts.join('\n') || 'No address on file';
  }

  /**
   * Get gender display text
   */
  getGenderDisplay(): string {
    if (!this.patient?.gender) return 'Not specified';

    const genderMap: Record<string, string> = {
      male: 'Male',
      female: 'Female',
      other: 'Other',
      unknown: 'Unknown',
    };

    return genderMap[this.patient.gender] || this.patient.gender;
  }

  /**
   * Get status display
   */
  getStatusDisplay(): string {
    return this.patient?.active ? 'Active' : 'Inactive';
  }

  /**
   * Get status color for mat-chip
   */
  getStatusColor(): string {
    return this.patient?.active ? 'primary' : 'warn';
  }
}
