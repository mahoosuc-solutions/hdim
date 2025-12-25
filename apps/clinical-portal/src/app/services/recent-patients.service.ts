import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

/**
 * Recent patient entry with access metadata
 */
export interface RecentPatientEntry {
  patientId: string;
  fullName: string;
  mrn?: string;
  dateOfBirth?: string;
  gender?: string;
  accessedAt: Date;
  accessCount: number;
}

/**
 * Recent Patients Service
 *
 * Tracks recently accessed patients using localStorage.
 * Features:
 * - Stores last 20 recently accessed patients
 * - Tracks access count for "frequently accessed" patients
 * - Provides quick access list for the patients page
 * - Persists across sessions
 */
@Injectable({
  providedIn: 'root',
})
export class RecentPatientsService {
  private readonly STORAGE_KEY = 'hdim_recent_patients';
  private readonly MAX_RECENT_PATIENTS = 20;

  private recentPatientsSubject = new BehaviorSubject<RecentPatientEntry[]>([]);
  readonly recentPatients$ = this.recentPatientsSubject.asObservable();

  constructor() {
    this.loadFromStorage();
  }

  /**
   * Get current recent patients list
   */
  getRecentPatients(): RecentPatientEntry[] {
    return this.recentPatientsSubject.getValue();
  }

  /**
   * Get top N frequently accessed patients
   */
  getFrequentPatients(limit: number = 5): RecentPatientEntry[] {
    return [...this.getRecentPatients()]
      .sort((a, b) => b.accessCount - a.accessCount)
      .slice(0, limit);
  }

  /**
   * Get most recently accessed patients
   */
  getMostRecentPatients(limit: number = 5): RecentPatientEntry[] {
    return [...this.getRecentPatients()]
      .sort((a, b) => new Date(b.accessedAt).getTime() - new Date(a.accessedAt).getTime())
      .slice(0, limit);
  }

  /**
   * Record patient access
   * Called when a patient is viewed
   */
  recordPatientAccess(patient: {
    id: string;
    fullName: string;
    mrn?: string;
    dateOfBirth?: string;
    gender?: string;
  }): void {
    const recentPatients = this.getRecentPatients();
    const existingIndex = recentPatients.findIndex((p) => p.patientId === patient.id);

    if (existingIndex !== -1) {
      // Update existing entry
      const existing = recentPatients[existingIndex];
      existing.accessedAt = new Date();
      existing.accessCount++;
      // Update patient info in case it changed
      existing.fullName = patient.fullName;
      existing.mrn = patient.mrn;
      existing.dateOfBirth = patient.dateOfBirth;
      existing.gender = patient.gender;
    } else {
      // Add new entry
      const newEntry: RecentPatientEntry = {
        patientId: patient.id,
        fullName: patient.fullName,
        mrn: patient.mrn,
        dateOfBirth: patient.dateOfBirth,
        gender: patient.gender,
        accessedAt: new Date(),
        accessCount: 1,
      };
      recentPatients.unshift(newEntry);

      // Limit to max recent patients
      if (recentPatients.length > this.MAX_RECENT_PATIENTS) {
        recentPatients.pop();
      }
    }

    // Sort by most recent and save
    const sorted = recentPatients.sort(
      (a, b) => new Date(b.accessedAt).getTime() - new Date(a.accessedAt).getTime()
    );
    this.saveToStorage(sorted);
    this.recentPatientsSubject.next(sorted);
  }

  /**
   * Remove a patient from recent list
   */
  removePatient(patientId: string): void {
    const filtered = this.getRecentPatients().filter((p) => p.patientId !== patientId);
    this.saveToStorage(filtered);
    this.recentPatientsSubject.next(filtered);
  }

  /**
   * Clear all recent patients
   */
  clearRecentPatients(): void {
    this.saveToStorage([]);
    this.recentPatientsSubject.next([]);
  }

  /**
   * Check if a patient was recently accessed
   */
  isRecentPatient(patientId: string): boolean {
    return this.getRecentPatients().some((p) => p.patientId === patientId);
  }

  /**
   * Get access count for a patient
   */
  getAccessCount(patientId: string): number {
    const entry = this.getRecentPatients().find((p) => p.patientId === patientId);
    return entry?.accessCount || 0;
  }

  /**
   * Load recent patients from localStorage
   */
  private loadFromStorage(): void {
    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored) as RecentPatientEntry[];
        // Convert date strings back to Date objects
        const withDates = parsed.map((p) => ({
          ...p,
          accessedAt: new Date(p.accessedAt),
        }));
        this.recentPatientsSubject.next(withDates);
      }
    } catch (error) {
      console.error('[RecentPatientsService] Error loading from storage:', error);
      // Clear corrupted data
      localStorage.removeItem(this.STORAGE_KEY);
    }
  }

  /**
   * Save recent patients to localStorage
   */
  private saveToStorage(patients: RecentPatientEntry[]): void {
    try {
      localStorage.setItem(this.STORAGE_KEY, JSON.stringify(patients));
    } catch (error) {
      console.error('[RecentPatientsService] Error saving to storage:', error);
    }
  }
}
