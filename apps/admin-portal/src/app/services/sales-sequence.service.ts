import { Injectable, signal, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of, throwError } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { environment } from '../../environments/environment';
import {
  EmailSequence,
  SequenceCreateRequest,
  SequenceEnrollment,
  SequenceAnalytics,
  PageRequest,
  PageResponse,
} from '../models/sales.model';

/**
 * Email Sequence Management Service
 *
 * Manages email sequences for automated outreach campaigns.
 */
@Injectable({
  providedIn: 'root',
})
export class SalesSequenceService {
  private readonly http = inject(HttpClient);
  private readonly apiBaseUrl = environment.apiConfig.salesApiUrl;

  // State signals
  private _sequences = signal<EmailSequence[]>([]);
  private _currentSequence = signal<EmailSequence | null>(null);
  private _isLoading = signal<boolean>(false);
  private _error = signal<string | null>(null);

  // Public read-only signals
  readonly sequences = this._sequences.asReadonly();
  readonly currentSequence = this._currentSequence.asReadonly();
  readonly isLoading = this._isLoading.asReadonly();
  readonly error = this._error.asReadonly();

  // ==========================================
  // Sequence CRUD
  // ==========================================

  getSequences(page?: PageRequest): Observable<PageResponse<EmailSequence>> {
    this._isLoading.set(true);
    let url = `${this.apiBaseUrl}/api/sales/sequences`;
    if (page) {
      url += `?page=${page.page}&size=${page.size}`;
    }

    return this.http.get<PageResponse<EmailSequence>>(url).pipe(
      tap((response) => {
        this._sequences.set(response.content);
        this._isLoading.set(false);
      }),
      catchError((error) => {
        this._isLoading.set(false);
        this._error.set('Failed to load sequences');
        return throwError(() => error);
      })
    );
  }

  getSequence(id: string): Observable<EmailSequence> {
    return this.http.get<EmailSequence>(`${this.apiBaseUrl}/api/sales/sequences/${id}`).pipe(
      tap((sequence) => this._currentSequence.set(sequence)),
      catchError((error) => {
        this._error.set('Failed to load sequence');
        return throwError(() => error);
      })
    );
  }

  createSequence(sequence: SequenceCreateRequest): Observable<EmailSequence> {
    return this.http.post<EmailSequence>(`${this.apiBaseUrl}/api/sales/sequences`, sequence).pipe(
      tap((created) => {
        this._sequences.update((list) => [...list, created]);
      }),
      catchError((error) => {
        this._error.set('Failed to create sequence');
        return throwError(() => error);
      })
    );
  }

  updateSequence(id: string, sequence: Partial<SequenceCreateRequest>): Observable<EmailSequence> {
    return this.http.put<EmailSequence>(`${this.apiBaseUrl}/api/sales/sequences/${id}`, sequence).pipe(
      tap((updated) => {
        this._sequences.update((list) =>
          list.map((s) => (s.id === id ? updated : s))
        );
        if (this._currentSequence()?.id === id) {
          this._currentSequence.set(updated);
        }
      }),
      catchError((error) => {
        this._error.set('Failed to update sequence');
        return throwError(() => error);
      })
    );
  }

  deleteSequence(id: string): Observable<void> {
    return this.http.delete<void>(`${this.apiBaseUrl}/api/sales/sequences/${id}`).pipe(
      tap(() => {
        this._sequences.update((list) => list.filter((s) => s.id !== id));
        if (this._currentSequence()?.id === id) {
          this._currentSequence.set(null);
        }
      }),
      catchError((error) => {
        this._error.set('Failed to delete sequence');
        return throwError(() => error);
      })
    );
  }

  // ==========================================
  // Sequence Actions
  // ==========================================

  activateSequence(id: string): Observable<EmailSequence> {
    return this.http.post<EmailSequence>(`${this.apiBaseUrl}/api/sales/sequences/${id}/activate`, {}).pipe(
      tap((updated) => {
        this._sequences.update((list) =>
          list.map((s) => (s.id === id ? { ...s, status: 'ACTIVE' as const } : s))
        );
      }),
      catchError((error) => {
        this._error.set('Failed to activate sequence');
        return throwError(() => error);
      })
    );
  }

  deactivateSequence(id: string): Observable<EmailSequence> {
    return this.http.post<EmailSequence>(`${this.apiBaseUrl}/api/sales/sequences/${id}/deactivate`, {}).pipe(
      tap((updated) => {
        this._sequences.update((list) =>
          list.map((s) => (s.id === id ? { ...s, status: 'INACTIVE' as const } : s))
        );
      }),
      catchError((error) => {
        this._error.set('Failed to deactivate sequence');
        return throwError(() => error);
      })
    );
  }

  // ==========================================
  // Enrollment Management
  // ==========================================

  enrollLead(sequenceId: string, leadId: string): Observable<SequenceEnrollment> {
    return this.http.post<SequenceEnrollment>(
      `${this.apiBaseUrl}/api/sales/sequences/${sequenceId}/enroll/lead/${leadId}`,
      {}
    ).pipe(
      catchError((error) => {
        this._error.set('Failed to enroll lead');
        return throwError(() => error);
      })
    );
  }

  enrollContact(sequenceId: string, contactId: string): Observable<SequenceEnrollment> {
    return this.http.post<SequenceEnrollment>(
      `${this.apiBaseUrl}/api/sales/sequences/${sequenceId}/enroll/contact/${contactId}`,
      {}
    ).pipe(
      catchError((error) => {
        this._error.set('Failed to enroll contact');
        return throwError(() => error);
      })
    );
  }

  getEnrollments(sequenceId: string): Observable<SequenceEnrollment[]> {
    return this.http.get<SequenceEnrollment[]>(
      `${this.apiBaseUrl}/api/sales/sequences/${sequenceId}/enrollments`
    ).pipe(
      catchError(() => of([]))
    );
  }

  pauseEnrollment(enrollmentId: string): Observable<SequenceEnrollment> {
    return this.http.post<SequenceEnrollment>(
      `${this.apiBaseUrl}/api/sales/sequences/enrollments/${enrollmentId}/pause`,
      {}
    ).pipe(
      catchError((error) => {
        this._error.set('Failed to pause enrollment');
        return throwError(() => error);
      })
    );
  }

  resumeEnrollment(enrollmentId: string): Observable<SequenceEnrollment> {
    return this.http.post<SequenceEnrollment>(
      `${this.apiBaseUrl}/api/sales/sequences/enrollments/${enrollmentId}/resume`,
      {}
    ).pipe(
      catchError((error) => {
        this._error.set('Failed to resume enrollment');
        return throwError(() => error);
      })
    );
  }

  removeEnrollment(enrollmentId: string): Observable<void> {
    return this.http.delete<void>(
      `${this.apiBaseUrl}/api/sales/sequences/enrollments/${enrollmentId}`
    ).pipe(
      catchError((error) => {
        this._error.set('Failed to remove enrollment');
        return throwError(() => error);
      })
    );
  }

  // ==========================================
  // Analytics
  // ==========================================

  getSequenceAnalytics(sequenceId: string): Observable<SequenceAnalytics> {
    return this.http.get<SequenceAnalytics>(
      `${this.apiBaseUrl}/api/sales/sequences/${sequenceId}/analytics`
    ).pipe(
      catchError((error) => {
        this._error.set('Failed to load analytics');
        return throwError(() => error);
      })
    );
  }
}
