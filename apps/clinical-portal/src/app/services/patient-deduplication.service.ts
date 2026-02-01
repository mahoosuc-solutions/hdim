import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';
import { LoggerService } from './logger.service';
import {
  PatientSummaryWithLinks,
  PatientLink,
  PatientLinkType,
  PotentialDuplicateMatch,
  PatientMergeRequest,
  PatientMergeResult,
  DeduplicationStatistics
} from '../models/patient-link.model';
import { PatientSummary } from '../models/patient.model';

/**
 * Service for patient deduplication and Master Patient Index (MPI) management
 *
 * This service handles:
 * - Identifying potential duplicate patient records
 * - Linking duplicate records to master records
 * - Merging duplicate records
 * - Filtering views to show only master records
 */
@Injectable({
  providedIn: 'root'
})
export class PatientDeduplicationService {
  // In-memory storage for patient links (would be backend API in production)
  private patientLinks: Map<string, PatientLink[]> = new Map();
  private masterPatientIds: Set<string> = new Set();

  constructor(private logger: LoggerService) {
    // Initialize with some sample duplicate relationships for demonstration
    this.initializeSampleDuplicates();
  }

  /**
   * Convert regular patient summary to one with MPI information
   */
  enhanceWithLinkInfo(patient: PatientSummary): PatientSummaryWithLinks {
    const links = this.patientLinks.get(patient.id) || [];
    const isMaster = this.masterPatientIds.has(patient.id);

    // Find master patient ID if this is a duplicate
    let masterPatientId: string | undefined;
    const replacedByLink = links.find(l => l.type === PatientLinkType.REPLACED_BY);
    if (replacedByLink) {
      masterPatientId = replacedByLink.targetPatientId;
    }

    // Count duplicates if this is a master
    const duplicateIds: string[] = [];
    if (isMaster) {
      // Find all patients that link to this one as master
      this.patientLinks.forEach((patientLinks, patientId) => {
        const linkToThis = patientLinks.find(
          l => l.targetPatientId === patient.id && l.type === PatientLinkType.REPLACED_BY
        );
        if (linkToThis) {
          duplicateIds.push(patientId);
        }
      });
    }

    return {
      ...patient,
      isMaster,
      masterPatientId,
      links,
      duplicateIds,
      duplicateCount: duplicateIds.length,
      isPotentialDuplicate: false // Would be set by matching algorithm
    };
  }

  /**
   * Get all patients with MPI information
   */
  enhancePatientList(patients: PatientSummary[]): PatientSummaryWithLinks[] {
    return patients.map(p => this.enhanceWithLinkInfo(p));
  }

  /**
   * Filter to show only master records
   */
  filterMasterRecordsOnly(patients: PatientSummaryWithLinks[]): PatientSummaryWithLinks[] {
    return patients.filter(p => p.isMaster || (!p.masterPatientId && !p.links?.length));
  }

  /**
   * Link a duplicate patient to a master patient
   */
  linkPatient(duplicateId: string, masterId: string, verified = false): Observable<boolean> {
    // Add link from duplicate to master
    const duplicateLinks = this.patientLinks.get(duplicateId) || [];
    duplicateLinks.push({
      targetPatientId: masterId,
      type: PatientLinkType.REPLACED_BY,
      verified,
      createdAt: new Date().toISOString(),
      createdBy: 'current-user'
    });
    this.patientLinks.set(duplicateId, duplicateLinks);

    // Add reverse link from master to duplicate
    const masterLinks = this.patientLinks.get(masterId) || [];
    masterLinks.push({
      targetPatientId: duplicateId,
      type: PatientLinkType.REPLACES,
      verified,
      createdAt: new Date().toISOString(),
      createdBy: 'current-user'
    });
    this.patientLinks.set(masterId, masterLinks);

    // Mark master as master record
    this.masterPatientIds.add(masterId);

    return of(true);
  }

  /**
   * Unlink a duplicate from its master
   */
  unlinkPatient(duplicateId: string): Observable<boolean> {
    const links = this.patientLinks.get(duplicateId) || [];
    const masterLink = links.find(l => l.type === PatientLinkType.REPLACED_BY);

    if (masterLink) {
      // Remove link from duplicate
      this.patientLinks.set(
        duplicateId,
        links.filter(l => l.type !== PatientLinkType.REPLACED_BY)
      );

      // Remove reverse link from master
      const masterLinks = this.patientLinks.get(masterLink.targetPatientId) || [];
      this.patientLinks.set(
        masterLink.targetPatientId,
        masterLinks.filter(l => l.targetPatientId !== duplicateId)
      );

      // Check if master still has duplicates, if not remove from master set
      const remainingMasterLinks = this.patientLinks.get(masterLink.targetPatientId) || [];
      if (!remainingMasterLinks.some(l => l.type === PatientLinkType.REPLACES)) {
        this.masterPatientIds.delete(masterLink.targetPatientId);
      }
    }

    return of(true);
  }

  /**
   * Set a patient as the master record
   */
  setAsMaster(patientId: string): Observable<boolean> {
    this.masterPatientIds.add(patientId);
    return of(true);
  }

  /**
   * Find potential duplicate matches for a patient
   */
  findPotentialDuplicates(
    patient: PatientSummaryWithLinks,
    allPatients: PatientSummaryWithLinks[]
  ): Observable<PotentialDuplicateMatch[]> {
    const matches: PotentialDuplicateMatch[] = [];

    allPatients.forEach(otherPatient => {
      if (otherPatient.id === patient.id) return;
      if (otherPatient.masterPatientId === patient.id) return; // Already linked

      const matchScore = this.calculateMatchScore(patient, otherPatient);

      if (matchScore >= 70) {
        matches.push({
          patient1: patient,
          patient2: otherPatient,
          matchScore,
          matchingFields: {
            name: this.namesMatch(patient.fullName, otherPatient.fullName),
            dateOfBirth: patient.dateOfBirth === otherPatient.dateOfBirth,
            gender: patient.gender === otherPatient.gender,
            mrn: patient.mrn === otherPatient.mrn
          },
          suggestedAction: matchScore >= 90 ? 'merge' : matchScore >= 80 ? 'link' : 'review'
        });
      }
    });

    // Sort by match score descending
    matches.sort((a, b) => b.matchScore - a.matchScore);

    return of(matches);
  }

  /**
   * Merge duplicate patients into a master record
   */
  mergePatients(request: PatientMergeRequest): Observable<PatientMergeResult> {
    try {
      // Link all duplicates to the master
      request.duplicatePatientIds.forEach(dupId => {
        this.linkPatient(dupId, request.masterPatientId, true).subscribe();
      });

      return of({
        masterPatientId: request.masterPatientId,
        mergedPatientIds: request.duplicatePatientIds,
        success: true,
        mergedAt: new Date().toISOString()
      });
    } catch (error) {
      return of({
        masterPatientId: request.masterPatientId,
        mergedPatientIds: [],
        success: false,
        error: error instanceof Error ? error.message : 'Unknown error'
      });
    }
  }

  /**
   * Get deduplication statistics
   */
  getStatistics(patients: PatientSummaryWithLinks[]): Observable<DeduplicationStatistics> {
    const masterRecords = patients.filter(p => p.isMaster).length;
    const duplicateRecords = patients.filter(p => p.masterPatientId).length;
    const unlinkedRecords = patients.filter(p => !p.isMaster && !p.masterPatientId).length;

    const mastersWithDuplicates = patients.filter(p => p.isMaster && (p.duplicateCount || 0) > 0);
    const totalDuplicates = mastersWithDuplicates.reduce((sum, p) => sum + (p.duplicateCount || 0), 0);
    const averageDuplicatesPerMaster =
      mastersWithDuplicates.length > 0 ? totalDuplicates / mastersWithDuplicates.length : 0;

    return of({
      totalPatients: patients.length,
      masterRecords,
      duplicateRecords,
      unlinkedRecords,
      potentialDuplicates: 0, // Would be calculated by matching algorithm
      averageDuplicatesPerMaster
    });
  }

  /**
   * Calculate match score between two patients (0-100)
   */
  private calculateMatchScore(p1: PatientSummaryWithLinks, p2: PatientSummaryWithLinks): number {
    let score = 0;
    let maxScore = 0;

    // Name matching (40 points)
    maxScore += 40;
    if (this.namesMatch(p1.fullName, p2.fullName)) {
      score += 40;
    } else if (this.namesSimilar(p1.fullName, p2.fullName)) {
      score += 25;
    }

    // Date of birth (30 points)
    maxScore += 30;
    if (p1.dateOfBirth && p2.dateOfBirth) {
      if (p1.dateOfBirth === p2.dateOfBirth) {
        score += 30;
      }
    }

    // Gender (10 points)
    maxScore += 10;
    if (p1.gender && p2.gender && p1.gender === p2.gender) {
      score += 10;
    }

    // MRN (20 points)
    maxScore += 20;
    if (p1.mrn && p2.mrn && p1.mrn === p2.mrn) {
      score += 20;
    }

    return Math.round((score / maxScore) * 100);
  }

  /**
   * Check if two names match exactly (case-insensitive, normalized)
   */
  private namesMatch(name1: string, name2: string): boolean {
    const normalize = (name: string) =>
      name
        .toLowerCase()
        .replace(/[^a-z\s]/g, '')
        .trim();
    return normalize(name1) === normalize(name2);
  }

  /**
   * Check if two names are similar (allows for minor variations)
   */
  private namesSimilar(name1: string, name2: string): boolean {
    const normalize = (name: string) =>
      name
        .toLowerCase()
        .replace(/[^a-z]/g, '')
        .trim();

    const n1 = normalize(name1);
    const n2 = normalize(name2);

    // Check if one name contains the other
    if (n1.includes(n2) || n2.includes(n1)) return true;

    // Calculate Levenshtein distance
    const distance = this.levenshteinDistance(n1, n2);
    const maxLength = Math.max(n1.length, n2.length);

    // Allow 20% difference
    return distance / maxLength <= 0.2;
  }

  /**
   * Calculate Levenshtein distance between two strings
   */
  private levenshteinDistance(str1: string, str2: string): number {
    const matrix: number[][] = [];

    for (let i = 0; i <= str2.length; i++) {
      matrix[i] = [i];
    }

    for (let j = 0; j <= str1.length; j++) {
      matrix[0][j] = j;
    }

    for (let i = 1; i <= str2.length; i++) {
      for (let j = 1; j <= str1.length; j++) {
        if (str2.charAt(i - 1) === str1.charAt(j - 1)) {
          matrix[i][j] = matrix[i - 1][j - 1];
        } else {
          matrix[i][j] = Math.min(
            matrix[i - 1][j - 1] + 1, // substitution
            matrix[i][j - 1] + 1, // insertion
            matrix[i - 1][j] + 1 // deletion
          );
        }
      }
    }

    return matrix[str2.length][str1.length];
  }

  /**
   * Automatically detect and link duplicate patients
   * This runs the matching algorithm and creates links for high-confidence matches
   */
  autoDetectAndLinkDuplicates(patients: PatientSummary[]): Observable<{
    mastersCreated: number;
    duplicatesLinked: number;
    matches: PotentialDuplicateMatch[];
  }> {
    const patientsWithLinks = this.enhancePatientList(patients);
    const processedPairs = new Set<string>(); // Track processed pairs to avoid duplicates
    const linkedPatients = new Set<string>(); // Track patients that have been linked
    let duplicatesLinked = 0;

    // Find all potential duplicates synchronously
    for (let i = 0; i < patientsWithLinks.length; i++) {
      const patient1 = patientsWithLinks[i];

      // Skip if already linked as a duplicate
      if (linkedPatients.has(patient1.id)) {
        continue;
      }

      for (let j = i + 1; j < patientsWithLinks.length; j++) {
        const patient2 = patientsWithLinks[j];

        // Skip if already linked as a duplicate
        if (linkedPatients.has(patient2.id)) {
          continue;
        }

        // Create unique pair ID to avoid processing same pair twice
        const pairId = [patient1.id, patient2.id].sort().join('-');
        if (processedPairs.has(pairId)) {
          continue;
        }
        processedPairs.add(pairId);

        // Calculate match score
        const matchScore = this.calculateMatchScore(patient1, patient2);

        // Only auto-link if match score >= 85 (high confidence)
        if (matchScore >= 85) {
          // Determine which should be master (prefer earlier MRN or ID)
          const shouldP1BeMaster = this.shouldBeMaster(patient1, patient2);
          const masterId = shouldP1BeMaster ? patient1.id : patient2.id;
          const duplicateId = shouldP1BeMaster ? patient2.id : patient1.id;

          // Check if either is already a master or duplicate
          const p1IsMaster = this.masterPatientIds.has(patient1.id);
          const p2IsMaster = this.masterPatientIds.has(patient2.id);

          // If both are already masters with different duplicates, don't link
          if (p1IsMaster && p2IsMaster) {
            continue;
          }

          // Link the duplicate to the master (synchronous operation)
          this.linkPatientSync(duplicateId, masterId);
          linkedPatients.add(duplicateId);
          duplicatesLinked++;

          this.logger.info('Linked duplicate patient to master', { duplicateId, masterId, matchScore });
        }
      }
    }

    // Count unique masters created
    const mastersCreated = this.masterPatientIds.size;

    this.logger.info('Patient deduplication detection complete', { mastersCreated, duplicatesLinked });

    return of({
      mastersCreated,
      duplicatesLinked,
      matches: [] // Could populate this if needed for detailed results
    });
  }

  /**
   * Synchronous version of linkPatient for use in batch operations
   */
  private linkPatientSync(duplicateId: string, masterId: string): void {
    // Add link from duplicate to master
    const duplicateLinks = this.patientLinks.get(duplicateId) || [];
    duplicateLinks.push({
      targetPatientId: masterId,
      type: PatientLinkType.REPLACED_BY,
      verified: true,
      createdAt: new Date().toISOString(),
      createdBy: 'auto-detect'
    });
    this.patientLinks.set(duplicateId, duplicateLinks);

    // Add reverse link from master to duplicate
    const masterLinks = this.patientLinks.get(masterId) || [];
    masterLinks.push({
      targetPatientId: duplicateId,
      type: PatientLinkType.REPLACES,
      verified: true,
      createdAt: new Date().toISOString(),
      createdBy: 'auto-detect'
    });
    this.patientLinks.set(masterId, masterLinks);

    // Mark master as master record
    this.masterPatientIds.add(masterId);
  }

  /**
   * Determine which patient should be the master record
   * Prefers: lower MRN number, then lower ID
   */
  private shouldBeMaster(p1: PatientSummaryWithLinks, p2: PatientSummaryWithLinks): boolean {
    // If one already has duplicates, it should be master
    if ((p1.duplicateCount || 0) > 0 && (p2.duplicateCount || 0) === 0) {
      return true;
    }
    if ((p2.duplicateCount || 0) > 0 && (p1.duplicateCount || 0) === 0) {
      return false;
    }

    // Compare MRNs if both have them
    if (p1.mrn && p2.mrn) {
      const mrn1Num = parseInt(p1.mrn.replace(/\D/g, '')) || 0;
      const mrn2Num = parseInt(p2.mrn.replace(/\D/g, '')) || 0;
      if (mrn1Num !== mrn2Num) {
        return mrn1Num < mrn2Num;
      }
    }

    // Fall back to ID comparison
    const id1Num = parseInt(p1.id) || 0;
    const id2Num = parseInt(p2.id) || 0;
    return id1Num < id2Num;
  }

  /**
   * Clear all duplicate links (for testing/reset)
   */
  clearAllLinks(): void {
    this.patientLinks.clear();
    this.masterPatientIds.clear();
  }

  /**
   * Initialize sample duplicate relationships for demonstration
   */
  private initializeSampleDuplicates(): void {
    // Sample duplicates will be auto-detected via autoDetectAndLinkDuplicates()
  }
}
