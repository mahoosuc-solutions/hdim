import { Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

/**
 * Base service with common functionality for audit services
 */
@Injectable({
  providedIn: 'root'
})
export class AuditBaseService {
  
  /**
   * Handle HTTP errors with user-friendly messages
   */
  protected handleError(error: HttpErrorResponse): Observable<never> {
    let errorMessage = 'An error occurred';

    if (error.error instanceof ErrorEvent) {
      // Client-side error
      errorMessage = `Client Error: ${error.error.message}`;
    } else {
      // Server-side error
      switch (error.status) {
        case 400:
          errorMessage = 'Invalid request. Please check your input.';
          break;
        case 401:
          errorMessage = 'Unauthorized. Please log in again.';
          break;
        case 403:
          errorMessage = 'You do not have permission to perform this action.';
          break;
        case 404:
          errorMessage = 'The requested resource was not found.';
          break;
        case 500:
          errorMessage = 'Server error. Please try again later.';
          break;
        default:
          errorMessage = `Server Error Code: ${error.status}\nMessage: ${error.message}`;
      }
    }

    console.error('Audit API Error:', errorMessage, error);
    return throwError(() => new Error(errorMessage));
  }

  /**
   * Format date to ISO string for API
   */
  protected formatDate(date: Date | string | null | undefined): string | undefined {
    if (!date) return undefined;
    
    if (typeof date === 'string') {
      return date;
    }
    
    return date.toISOString().split('T')[0];
  }

  /**
   * Calculate priority based on confidence score and severity
   */
  protected determinePriority(confidenceScore?: number, severity?: string): string {
    if (severity === 'CRITICAL' || (confidenceScore !== undefined && confidenceScore < 0.7)) {
      return 'CRITICAL';
    } else if (severity === 'HIGH' || (confidenceScore !== undefined && confidenceScore < 0.8)) {
      return 'HIGH';
    } else if (severity === 'MODERATE' || (confidenceScore !== undefined && confidenceScore < 0.9)) {
      return 'MEDIUM';
    }
    return 'LOW';
  }

  /**
   * Get priority badge color class
   */
  getPriorityColorClass(priority: string): string {
    switch (priority) {
      case 'CRITICAL':
        return 'badge-danger';
      case 'HIGH':
        return 'badge-warning';
      case 'MEDIUM':
        return 'badge-info';
      case 'LOW':
        return 'badge-secondary';
      default:
        return 'badge-light';
    }
  }

  /**
   * Get status badge color class
   */
  getStatusColorClass(status: string): string {
    switch (status) {
      case 'APPROVED':
      case 'VALID':
      case 'RESOLVED':
      case 'CLOSED':
        return 'badge-success';
      case 'REJECTED':
      case 'INVALID':
      case 'ROLLED_BACK':
        return 'badge-danger';
      case 'PENDING':
      case 'IN_PROGRESS':
        return 'badge-warning';
      case 'NEEDS_REVIEW':
      case 'NEEDS_REVISION':
        return 'badge-info';
      default:
        return 'badge-secondary';
    }
  }

  /**
   * Get evidence grade badge color class
   */
  getEvidenceGradeColorClass(grade: string): string {
    switch (grade) {
      case 'A':
        return 'badge-success'; // Strong evidence
      case 'B':
        return 'badge-primary'; // Moderate evidence
      case 'C':
        return 'badge-warning'; // Weak evidence
      case 'D':
        return 'badge-secondary'; // Expert opinion
      default:
        return 'badge-light';
    }
  }

  /**
   * Format confidence score as percentage
   */
  formatConfidenceScore(score: number): string {
    return `${(score * 100).toFixed(1)}%`;
  }

  /**
   * Get confidence score color class
   */
  getConfidenceColorClass(score: number): string {
    if (score >= 0.9) return 'text-success';
    if (score >= 0.8) return 'text-primary';
    if (score >= 0.7) return 'text-warning';
    return 'text-danger';
  }
}
