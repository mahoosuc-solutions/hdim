/**
 * Care Plan Service
 *
 * Provides comprehensive care plan management capabilities:
 * - Care plan creation, updates, and lifecycle management
 * - Problem/diagnosis tracking and resolution
 * - Goal setting, progress tracking, and achievement
 * - Intervention planning and completion management
 * - Multidisciplinary team coordination
 * - Patient and caregiver engagement
 * - Care plan reviews and quality metrics
 * - Care transition coordination
 *
 * Features:
 * - Multi-tenant isolation via X-Tenant-ID header
 * - Intelligent caching with configurable TTL
 * - Typed error responses with context
 * - RxJS Observable patterns with proper operators
 */

import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, BehaviorSubject, throwError, of } from 'rxjs';
import { tap, map, switchMap, catchError } from 'rxjs/operators';
import { LoggerService } from '../logger.service';
import {
  CarePlan,
  CarePlanProblem,
  CarePlanGoal,
  CarePlanIntervention,
  CarePlanTeamMember,
  PatientEngagement,
  CarePlanReview,
  CareTransition,
  CarePlanMetrics,
  CarePlanContext,
  PaginatedResponse,
} from './care-plan.models';

export const CARE_PLAN_BASE_URL = '/care-plan-service/api/v1';

interface CacheEntry<T> {
  data: T;
  timestamp: number;
  ttlMs: number;
}

/**
 * CarePlanService provides comprehensive care plan management functionality
 * including plan lifecycle, problems, goals, interventions, team coordination, and metrics.
 *
 * @injectable
 */
@Injectable({
  providedIn: 'root',
})
export class CarePlanService {
  private tenantContext$ = new BehaviorSubject<string | null>(null);
  private cache = new Map<string, CacheEntry<any>>();
  private readonly DEFAULT_CACHE_TTL = 5 * 60 * 1000; // 5 minutes
  private readonly METRICS_CACHE_TTL = 10 * 60 * 1000; // 10 minutes

  constructor(
    private logger: LoggerService,
    private http: HttpClient
  ) {}

  // ==================== Context Management ====================

  /**
   * Set the tenant context for all subsequent requests
   * @param tenantId Unique identifier for the tenant
   */
  setTenantContext(tenantId: string): void {
    this.tenantContext$.next(tenantId);
  }

  /**
   * Get the current tenant context
   * @returns Current tenant ID
   * @throws Error if tenant context not set
   */
  getTenantContext(): string {
    const tenant = this.tenantContext$.value;
    if (!tenant) {
      throw new Error('Tenant context not set. Call setTenantContext() first.');
    }
    return tenant;
  }

  /**
   * Invalidate cache entries matching pattern
   * @param pattern Optional pattern to match cache keys
   */
  invalidateCache(pattern?: string): void {
    if (!pattern) {
      this.cache.clear();
      return;
    }
    const keysToDelete = Array.from(this.cache.keys()).filter((key) =>
      key.includes(pattern)
    );
    keysToDelete.forEach((key) => this.cache.delete(key));
  }

  // ==================== Care Plan CRUD Operations ====================

  /**
   * Get available care plan templates
   * @returns Observable of care plan templates
   */
  getCarePlanTemplates(): Observable<any[]> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/templates`;
    const cacheKey = 'templates';

    const cached = this.getFromCache<any[]>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<any[]>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((templates) => this.setInCache(cacheKey, templates, 30 * 60 * 1000)), // 30 min cache
        catchError((error: unknown) => this.handleError(error, 'getCarePlanTemplates'))
      );
  }

  /**
   * Create a new care plan
   * @param carePlan Care plan to create (can be partial with just patientId and templateId)
   * @returns Observable of created care plan
   */
  createCarePlan(carePlan: Partial<CarePlan> | CarePlan): Observable<CarePlan> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans`;

    return this.http
      .post<CarePlan>(url, { ...carePlan, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('plan')),
        catchError((error) => this.handleError(error, 'createCarePlan'))
      );
  }

  /**
   * Retrieve care plan by ID
   * @param planId ID of care plan to retrieve
   * @returns Observable of care plan
   */
  getCarePlanById(planId: string): Observable<CarePlan> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}`;
    const cacheKey = `plan:${planId}`;

    const cached = this.getFromCache<CarePlan>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<CarePlan>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((plan) => this.setInCache(cacheKey, plan)),
        catchError((error) => this.handleError(error, 'getCarePlanById'))
      );
  }

  /**
   * Get all active care plans for a patient
   * @param patientId Patient ID
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated active care plans
   */
  getActiveCarePlansForPatient(
    patientId: string,
    page: number,
    size: number
  ): Observable<PaginatedResponse<CarePlan>> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/patient/${patientId}/active`;
    const cacheKey = `plan:patient:${patientId}:active:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<CarePlan>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<CarePlan>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getActiveCarePlansForPatient')
        )
      );
  }

  /**
   * Update care plan
   * @param planId ID of plan to update
   * @param carePlan Updated care plan data
   * @returns Observable of updated care plan
   */
  updateCarePlan(planId: string, carePlan: CarePlan): Observable<CarePlan> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}`;

    return this.http
      .put<CarePlan>(url, { ...carePlan, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('plan')),
        catchError((error) => this.handleError(error, 'updateCarePlan'))
      );
  }

  /**
   * Complete a care plan with finalization data
   * @param planId ID of plan to complete
   * @param completionData Data about the completion
   * @returns Observable of completed care plan
   */
  completeCarePlan(planId: string, completionData: any): Observable<any> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/complete`;

    return this.http
      .post<any>(url, { ...completionData, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('plan')),
        catchError((error: unknown) => this.handleError(error, 'completeCarePlan'))
      );
  }

  /**
   * Close/complete a care plan
   * @param planId ID of plan to close
   * @param reason Reason for closure
   * @returns Observable of closed care plan
   */
  closeCarePlan(planId: string, reason?: string): Observable<CarePlan> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/close`;

    return this.http
      .post<CarePlan>(url, { reason }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('plan')),
        catchError((error) => this.handleError(error, 'closeCarePlan'))
      );
  }

  /**
   * Get care plans due for review
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated care plans due for review
   */
  getCarePlansDueForReview(
    page: number,
    size: number
  ): Observable<PaginatedResponse<CarePlan>> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/due-for-review`;
    const cacheKey = `plan:due-for-review:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<CarePlan>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<CarePlan>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getCarePlansDueForReview')
        )
      );
  }

  // ==================== Problem Management ====================

  /**
   * Add a problem/diagnosis to care plan
   * @param planId Care plan ID
   * @param problem Problem to add
   * @returns Observable of added problem
   */
  addProblem(planId: string, problem: CarePlanProblem): Observable<CarePlanProblem> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/problems`;

    return this.http
      .post<CarePlanProblem>(url, { ...problem, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('problem')),
        catchError((error) => this.handleError(error, 'addProblem'))
      );
  }

  /**
   * Get problems for a care plan
   * @param planId Care plan ID
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated problems
   */
  getProblemsForCarePlan(
    planId: string,
    page: number,
    size: number
  ): Observable<PaginatedResponse<CarePlanProblem>> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/problems`;
    const cacheKey = `problem:plan:${planId}:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<CarePlanProblem>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<CarePlanProblem>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getProblemsForCarePlan')
        )
      );
  }

  /**
   * Update problem
   * @param problemId ID of problem to update
   * @param problem Updated problem data
   * @returns Observable of updated problem
   */
  updateProblem(
    problemId: string,
    problem: CarePlanProblem
  ): Observable<CarePlanProblem> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/problems/${problemId}`;

    return this.http
      .put<CarePlanProblem>(url, { ...problem, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('problem')),
        catchError((error) => this.handleError(error, 'updateProblem'))
      );
  }

  /**
   * Resolve a problem
   * @param problemId ID of problem to resolve
   * @param resolutionNotes Notes about resolution
   * @returns Observable of resolved problem
   */
  resolveProblem(
    problemId: string,
    resolutionNotes?: string
  ): Observable<CarePlanProblem> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/problems/${problemId}/resolve`;

    return this.http
      .post<CarePlanProblem>(url, { resolutionNotes }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('problem')),
        catchError((error) => this.handleError(error, 'resolveProblem'))
      );
  }

  // ==================== Goal Management ====================

  /**
   * Add a goal to care plan
   * @param planId Care plan ID
   * @param goal Goal to add
   * @returns Observable of added goal
   */
  addGoal(planId: string, goal: CarePlanGoal): Observable<CarePlanGoal> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/goals`;

    return this.http
      .post<CarePlanGoal>(url, { ...goal, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('goal')),
        catchError((error) => this.handleError(error, 'addGoal'))
      );
  }

  /**
   * Get goals for a care plan
   * @param planId Care plan ID
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated goals
   */
  getGoalsForCarePlan(
    planId: string,
    page: number,
    size: number
  ): Observable<PaginatedResponse<CarePlanGoal>> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/goals`;
    const cacheKey = `goal:plan:${planId}:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<CarePlanGoal>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<CarePlanGoal>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) => this.handleError(error, 'getGoalsForCarePlan'))
      );
  }

  /**
   * Update goal
   * @param goalId ID of goal to update
   * @param goal Updated goal data
   * @returns Observable of updated goal
   */
  updateGoal(goalId: string, goal: CarePlanGoal): Observable<CarePlanGoal> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/goals/${goalId}`;

    return this.http
      .put<CarePlanGoal>(url, { ...goal, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('goal')),
        catchError((error) => this.handleError(error, 'updateGoal'))
      );
  }

  /**
   * Mark goal as achieved
   * @param goalId ID of goal to achieve
   * @returns Observable of achieved goal
   */
  achieveGoal(goalId: string): Observable<CarePlanGoal> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/goals/${goalId}/achieve`;

    return this.http
      .post<CarePlanGoal>(url, {}, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('goal')),
        catchError((error) => this.handleError(error, 'achieveGoal'))
      );
  }

  /**
   * Get goals nearing their target date
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated goals nearing target date
   */
  getGoalsNearingTargetDate(
    page: number,
    size: number
  ): Observable<PaginatedResponse<CarePlanGoal>> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/goals/nearing-target`;
    const cacheKey = `goal:nearing-target:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<CarePlanGoal>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<CarePlanGoal>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getGoalsNearingTargetDate')
        )
      );
  }

  // ==================== Intervention Management ====================

  /**
   * Add an intervention to care plan
   * @param planId Care plan ID
   * @param intervention Intervention to add
   * @returns Observable of added intervention
   */
  addIntervention(
    planId: string,
    intervention: CarePlanIntervention
  ): Observable<CarePlanIntervention> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/interventions`;

    return this.http
      .post<CarePlanIntervention>(url, { ...intervention, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('intervention')),
        catchError((error) => this.handleError(error, 'addIntervention'))
      );
  }

  /**
   * Get interventions for a care plan
   * @param planId Care plan ID
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated interventions
   */
  getInterventionsForCarePlan(
    planId: string,
    page: number,
    size: number
  ): Observable<PaginatedResponse<CarePlanIntervention>> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/interventions`;
    const cacheKey = `intervention:plan:${planId}:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<CarePlanIntervention>>(
      cacheKey
    );
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<CarePlanIntervention>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getInterventionsForCarePlan')
        )
      );
  }

  /**
   * Update intervention
   * @param interventionId ID of intervention to update
   * @param intervention Updated intervention data
   * @returns Observable of updated intervention
   */
  updateIntervention(
    interventionId: string,
    intervention: CarePlanIntervention
  ): Observable<CarePlanIntervention> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/interventions/${interventionId}`;

    return this.http
      .put<CarePlanIntervention>(url, { ...intervention, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('intervention')),
        catchError((error) => this.handleError(error, 'updateIntervention'))
      );
  }

  /**
   * Complete an intervention
   * @param interventionId ID of intervention to complete
   * @returns Observable of completed intervention
   */
  completeIntervention(
    interventionId: string
  ): Observable<CarePlanIntervention> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/interventions/${interventionId}/complete`;

    return this.http
      .post<CarePlanIntervention>(url, {}, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('intervention')),
        catchError((error) => this.handleError(error, 'completeIntervention'))
      );
  }

  /**
   * Get pending interventions
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated pending interventions
   */
  getPendingInterventions(
    page: number,
    size: number
  ): Observable<PaginatedResponse<CarePlanIntervention>> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/interventions/pending`;
    const cacheKey = `intervention:pending:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<CarePlanIntervention>>(
      cacheKey
    );
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<CarePlanIntervention>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getPendingInterventions')
        )
      );
  }

  // ==================== Team Management ====================

  /**
   * Add team member to care plan
   * @param planId Care plan ID
   * @param teamMember Team member to add
   * @returns Observable of added team member
   */
  addTeamMember(
    planId: string,
    teamMember: CarePlanTeamMember
  ): Observable<CarePlanTeamMember> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/team`;

    return this.http
      .post<CarePlanTeamMember>(url, { ...teamMember, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('team')),
        catchError((error) => this.handleError(error, 'addTeamMember'))
      );
  }

  /**
   * Get team members for care plan
   * @param planId Care plan ID
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated team members
   */
  getTeamMembersForCarePlan(
    planId: string,
    page: number,
    size: number
  ): Observable<PaginatedResponse<CarePlanTeamMember>> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/team`;
    const cacheKey = `team:plan:${planId}:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<CarePlanTeamMember>>(
      cacheKey
    );
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<CarePlanTeamMember>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getTeamMembersForCarePlan')
        )
      );
  }

  /**
   * Remove team member from care plan
   * @param planId Care plan ID
   * @param teamMemberId Team member ID to remove
   * @returns Observable of success
   */
  removeTeamMember(planId: string, teamMemberId: string): Observable<any> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/team/${teamMemberId}`;

    return this.http
      .delete(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('team')),
        catchError((error) => this.handleError(error, 'removeTeamMember'))
      );
  }

  // ==================== Patient Engagement ====================

  /**
   * Record patient engagement
   * @param planId Care plan ID
   * @param engagement Engagement data
   * @returns Observable of recorded engagement
   */
  recordPatientEngagement(
    planId: string,
    engagement: PatientEngagement
  ): Observable<PatientEngagement> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/engagement`;

    return this.http
      .post<PatientEngagement>(url, { ...engagement, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('engagement')),
        catchError((error) => this.handleError(error, 'recordPatientEngagement'))
      );
  }

  /**
   * Get patient engagement for care plan
   * @param planId Care plan ID
   * @returns Observable of patient engagement
   */
  getPatientEngagement(planId: string): Observable<PatientEngagement> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/engagement`;
    const cacheKey = `engagement:plan:${planId}`;

    const cached = this.getFromCache<PatientEngagement>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<PatientEngagement>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((engagement) => this.setInCache(cacheKey, engagement)),
        catchError((error) => this.handleError(error, 'getPatientEngagement'))
      );
  }

  /**
   * Update patient engagement
   * @param planId Care plan ID
   * @param engagement Updated engagement data
   * @returns Observable of updated engagement
   */
  updatePatientEngagement(
    planId: string,
    engagement: PatientEngagement
  ): Observable<PatientEngagement> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/engagement`;

    return this.http
      .put<PatientEngagement>(url, { ...engagement, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('engagement')),
        catchError((error) => this.handleError(error, 'updatePatientEngagement'))
      );
  }

  // ==================== Care Plan Review ====================

  /**
   * Create care plan review
   * @param planId Care plan ID
   * @param review Review data
   * @returns Observable of created review
   */
  createCarePlanReview(
    planId: string,
    review: CarePlanReview
  ): Observable<CarePlanReview> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/reviews`;

    return this.http
      .post<CarePlanReview>(url, { ...review, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('review')),
        catchError((error) => this.handleError(error, 'createCarePlanReview'))
      );
  }

  /**
   * Get reviews for care plan
   * @param planId Care plan ID
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated reviews
   */
  getReviewsForCarePlan(
    planId: string,
    page: number,
    size: number
  ): Observable<PaginatedResponse<CarePlanReview>> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/reviews`;
    const cacheKey = `review:plan:${planId}:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<CarePlanReview>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<CarePlanReview>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getReviewsForCarePlan')
        )
      );
  }

  // ==================== Care Transition ====================

  /**
   * Create care transition
   * @param planId Care plan ID
   * @param transition Transition data
   * @returns Observable of created transition
   */
  createCareTransition(
    planId: string,
    transition: CareTransition
  ): Observable<CareTransition> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/transitions`;

    return this.http
      .post<CareTransition>(url, { ...transition, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('transition')),
        catchError((error) => this.handleError(error, 'createCareTransition'))
      );
  }

  /**
   * Get transitions for care plan
   * @param planId Care plan ID
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated transitions
   */
  getTransitionsForCarePlan(
    planId: string,
    page: number,
    size: number
  ): Observable<PaginatedResponse<CareTransition>> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/care-plans/${planId}/transitions`;
    const cacheKey = `transition:plan:${planId}:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<CareTransition>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<CareTransition>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getTransitionsForCarePlan')
        )
      );
  }

  /**
   * Update care transition
   * @param transitionId Transition ID
   * @param transition Updated transition data
   * @returns Observable of updated transition
   */
  updateCareTransition(
    transitionId: string,
    transition: CareTransition
  ): Observable<CareTransition> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/transitions/${transitionId}`;

    return this.http
      .put<CareTransition>(url, { ...transition, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('transition')),
        catchError((error) => this.handleError(error, 'updateCareTransition'))
      );
  }

  // ==================== Metrics ====================

  /**
   * Get care plan metrics for patient
   * @param patientId Patient ID
   * @returns Observable of care plan metrics
   */
  getCarePlanMetrics(patientId: string): Observable<CarePlanMetrics> {
    const tenantId = this.getTenantContext();
    const url = `${CARE_PLAN_BASE_URL}/metrics/patient/${patientId}`;
    const cacheKey = `metrics:patient:${patientId}`;

    const cached = this.getFromCache<CarePlanMetrics>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<CarePlanMetrics>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((metrics) =>
          this.setInCache(cacheKey, metrics, this.METRICS_CACHE_TTL)
        ),
        catchError((error) => this.handleError(error, 'getCarePlanMetrics'))
      );
  }

  // ==================== Private Helper Methods ====================

  /**
   * Retrieve value from cache if not expired
   * @private
   */
  private getFromCache<T>(key: string): T | null {
    const entry = this.cache.get(key);
    if (!entry) return null;

    const now = Date.now();
    if (now - entry.timestamp > entry.ttlMs) {
      this.cache.delete(key);
      return null;
    }
    return entry.data as T;
  }

  /**
   * Store value in cache with TTL
   * @private
   */
  private setInCache<T>(
    key: string,
    data: T,
    ttlMs: number = this.DEFAULT_CACHE_TTL
  ): void {
    this.cache.set(key, {
      data,
      timestamp: Date.now(),
      ttlMs,
    });
  }

  /**
   * Handle HTTP errors with typed response
   * @private
   */
  private handleError(error: any, context: string): Observable<never> {
    this.logger.error(`[CarePlanService] Error in ${context}:`, { error });

    return throwError(() => ({
      status: error.status || 0,
      statusText: error.statusText || 'Unknown Error',
      message: error.error?.message || error.message || 'An unknown error occurred',
      context,
      error,
    }));
  }
}
