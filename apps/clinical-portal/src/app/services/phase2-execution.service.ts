import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * Phase 2 Execution Service
 *
 * Provides HTTP client methods for Phase 2 task management API.
 */
@Injectable({
  providedIn: 'root',
})
export class Phase2ExecutionService {
  private apiUrl = 'http://localhost:8098/api/v1/payer/phase2-execution';

  constructor(private http: HttpClient) {}

  /**
   * Create a new Phase 2 execution task
   */
  createTask(request: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/tasks`, request);
  }

  /**
   * Update an existing Phase 2 execution task
   */
  updateTask(taskId: string, request: any): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/tasks/${taskId}/status`, request);
  }

  /**
   * Get Phase 2 execution dashboard summary
   */
  getDashboard(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/dashboard`);
  }

  /**
   * Get Phase 2 tasks by category
   */
  getTasksByCategory(category: string, page: number = 0, size: number = 50): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<any>(
      `${this.apiUrl}/tasks/category/${category}`,
      { params }
    );
  }

  /**
   * Get Phase 2 tasks by status
   */
  getTasksByStatus(status: string, page: number = 0, size: number = 50): Observable<any> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http.get<any>(
      `${this.apiUrl}/tasks/status/${status}`,
      { params }
    );
  }

  /**
   * Get Phase 2 tasks for specific week
   */
  getTasksByWeek(week: number): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/tasks/week/${week}`
    );
  }

  /**
   * Get all open Phase 2 tasks (not completed or cancelled)
   */
  getOpenTasks(): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/tasks/open`
    );
  }

  /**
   * Update task status and progress
   */
  updateTaskStatus(taskId: string, status: string, progressPercentage?: number): Observable<any> {
    return this.http.patch<any>(
      `${this.apiUrl}/tasks/${taskId}/status`,
      { status, progressPercentage }
    );
  }

  /**
   * Complete a task with outcomes
   */
  completeTask(taskId: string, actualOutcomes: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/tasks/${taskId}/complete`,
      { actualOutcomes }
    );
  }

  /**
   * Block a task with unblock date
   */
  blockTask(taskId: string, blockReason: string, unblockDate: Date): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/tasks/${taskId}/block`,
      { blockReason, unblockDate }
    );
  }

  /**
   * Unblock a task
   */
  unblockTask(taskId: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/tasks/${taskId}/unblock`,
      {}
    );
  }

  /**
   * Add a note to a task
   */
  addNote(taskId: string, note: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/tasks/${taskId}/notes`,
      { note }
    );
  }

  /**
   * Get tasks blocked by a specific task
   */
  getBlockedByTask(taskId: string): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/tasks/${taskId}/blocked-by`
    );
  }

  /**
   * Get tasks blocking a specific task
   */
  getBlockingTasks(taskId: string): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/tasks/${taskId}/blocking`
    );
  }

  /**
   * Get financial dashboard metrics
   */
  getFinancialDashboard(): Observable<any> {
    return this.http.get<any>(
      `${this.apiUrl}/financial/dashboard`
    );
  }

  /**
   * Get ROI breakdown by HEDIS measure
   */
  getMeasureROI(): Observable<any[]> {
    return this.http.get<any[]>(
      `${this.apiUrl}/financial/by-measure`
    );
  }

  /**
   * Get case studies (optionally filter by published status)
   */
  getCaseStudies(published: boolean = false): Observable<any[]> {
    const params = new HttpParams().set('published', published.toString());
    return this.http.get<any[]>(
      `${this.apiUrl}/case-studies`,
      { params }
    );
  }

  /**
   * Publish a case study
   */
  publishCaseStudy(caseStudyId: string): Observable<any> {
    return this.http.post<any>(
      `${this.apiUrl}/case-studies/${caseStudyId}/publish`,
      {}
    );
  }
}
