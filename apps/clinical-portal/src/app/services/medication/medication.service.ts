/**
 * Medication Service
 *
 * Provides complete medication management capabilities:
 * - Medication catalog operations (CRUD)
 * - Prescription and order lifecycle
 * - Pharmacy fulfillment tracking
 * - Adverse event/allergy management
 * - Medication administration records
 * - Drug interaction checking
 * - Adherence and therapy metrics
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
  Medication,
  MedicationOrder,
  PharmacyFulfillment,
  MedicationAdverseEvent,
  MedicationAdministration,
  MedicationInteractionCheck,
  MedicationAdherenceMetrics,
  MedicationTherapyMetrics,
  Pharmacy,
  PharmacyCoordination,
  MedicationContext,
  PaginatedResponse,
} from './medication.models';

export const MEDICATION_BASE_URL = '/medication-service/api/v1';

interface CacheEntry<T> {
  data: T;
  timestamp: number;
  ttlMs: number;
}

/**
 * MedicationService provides comprehensive medication management functionality
 * including catalog management, order fulfillment, adverse event tracking, and metrics.
 *
 * @injectable
 */
@Injectable({
  providedIn: 'root',
})
export class MedicationService {
  private readonly logger: any;
  private tenantContext$ = new BehaviorSubject<string | null>(null);
  private cache = new Map<string, CacheEntry<any>>();
  private readonly DEFAULT_CACHE_TTL = 5 * 60 * 1000; // 5 minutes
  private readonly METRICS_CACHE_TTL = 10 * 60 * 1000; // 10 minutes

  constructor(
    private loggerService: LoggerService,private http: HttpClient) {
    this.logger = this.loggerService.withContext(\'MedicationService');}

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
   * @param pattern Optional pattern to match cache keys (e.g., 'medication:' to clear all medication entries)
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

  // ==================== Medication Catalog Operations ====================

  /**
   * Create a new medication in the catalog
   * @param medication Medication to create
   * @returns Observable of created medication
   */
  createMedication(medication: Medication): Observable<Medication> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/medications`;

    return this.http
      .post<Medication>(url, { ...medication, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('medication')),
        catchError((error) => this.handleError(error, 'createMedication'))
      );
  }

  /**
   * Retrieve medication by ID
   * @param medicationId ID of medication to retrieve
   * @returns Observable of medication
   */
  getMedicationById(medicationId: string): Observable<Medication> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/medications/${medicationId}`;
    const cacheKey = `medication:${medicationId}`;

    const cached = this.getFromCache<Medication>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<Medication>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((medication) => this.setInCache(cacheKey, medication)),
        catchError((error) => this.handleError(error, 'getMedicationById'))
      );
  }

  /**
   * Search medications by name or criteria
   * @param query Search query (medication name)
   * @param page Page number for pagination
   * @param size Page size
   * @returns Observable of paginated medication results
   */
  searchMedications(
    query: string,
    page: number,
    size: number
  ): Observable<PaginatedResponse<Medication>> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/medications/search`;
    const cacheKey = `medication:search:${query}:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<Medication>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('query', query)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<Medication>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) => this.handleError(error, 'searchMedications'))
      );
  }

  /**
   * Get medications by therapeutic class
   * @param therapeuticClass Therapeutic classification (e.g., 'Antidiabetic')
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated medications
   */
  getMedicationsByTherapeuticClass(
    therapeuticClass: string,
    page: number,
    size: number
  ): Observable<PaginatedResponse<Medication>> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/medications/class/${therapeuticClass}`;
    const cacheKey = `medication:class:${therapeuticClass}:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<Medication>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<Medication>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getMedicationsByTherapeuticClass')
        )
      );
  }

  /**
   * Update medication in catalog
   * @param medicationId ID of medication to update
   * @param medication Updated medication data
   * @returns Observable of updated medication
   */
  updateMedication(
    medicationId: string,
    medication: Medication
  ): Observable<Medication> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/medications/${medicationId}`;

    return this.http
      .put<Medication>(url, { ...medication, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('medication')),
        catchError((error) => this.handleError(error, 'updateMedication'))
      );
  }

  // ==================== Medication Order Operations ====================

  /**
   * Create a new medication order/prescription
   * @param order Order to create
   * @returns Observable of created order
   */
  createMedicationOrder(order: MedicationOrder): Observable<MedicationOrder> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/orders`;

    return this.http
      .post<MedicationOrder>(url, { ...order, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('order')),
        catchError((error) => this.handleError(error, 'createMedicationOrder'))
      );
  }

  /**
   * Retrieve medication order by ID
   * @param orderId ID of order to retrieve
   * @returns Observable of medication order
   */
  getMedicationOrderById(orderId: string): Observable<MedicationOrder> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/orders/${orderId}`;
    const cacheKey = `order:${orderId}`;

    const cached = this.getFromCache<MedicationOrder>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<MedicationOrder>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((order) => this.setInCache(cacheKey, order)),
        catchError((error) => this.handleError(error, 'getMedicationOrderById'))
      );
  }

  /**
   * Get all active orders for a patient
   * @param patientId Patient ID
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated active orders
   */
  getActiveOrdersForPatient(
    patientId: string,
    page: number,
    size: number
  ): Observable<PaginatedResponse<MedicationOrder>> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/orders/patient/${patientId}/active`;
    const cacheKey = `order:patient:${patientId}:active:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<MedicationOrder>>(
      cacheKey
    );
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<MedicationOrder>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getActiveOrdersForPatient')
        )
      );
  }

  /**
   * Get pending medication orders awaiting pharmacy processing
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated pending orders
   */
  getPendingOrdersAwaitingPharmacy(
    page: number,
    size: number
  ): Observable<PaginatedResponse<MedicationOrder>> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/orders/pending`;
    const cacheKey = `order:pending:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<MedicationOrder>>(
      cacheKey
    );
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<MedicationOrder>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getPendingOrdersAwaitingPharmacy')
        )
      );
  }

  /**
   * Update medication order
   * @param orderId ID of order to update
   * @param order Updated order data
   * @returns Observable of updated order
   */
  updateMedicationOrder(
    orderId: string,
    order: MedicationOrder
  ): Observable<MedicationOrder> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/orders/${orderId}`;

    return this.http
      .put<MedicationOrder>(url, { ...order, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('order')),
        catchError((error) => this.handleError(error, 'updateMedicationOrder'))
      );
  }

  /**
   * Refill a medication order
   * @param orderId ID of order to refill
   * @returns Observable of refilled order
   */
  refillMedicationOrder(orderId: string): Observable<MedicationOrder> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/orders/${orderId}/refill`;

    return this.http
      .post<MedicationOrder>(url, {}, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('order')),
        catchError((error) => this.handleError(error, 'refillMedicationOrder'))
      );
  }

  /**
   * Cancel a medication order
   * @param orderId ID of order to cancel
   * @param reason Reason for cancellation
   * @returns Observable of cancelled order
   */
  cancelMedicationOrder(
    orderId: string,
    reason?: string
  ): Observable<MedicationOrder> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/orders/${orderId}/cancel`;

    return this.http
      .post<MedicationOrder>(url, { reason }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('order')),
        catchError((error) => this.handleError(error, 'cancelMedicationOrder'))
      );
  }

  /**
   * Send order to pharmacy for processing
   * @param orderId ID of order to send
   * @returns Observable of order sent to pharmacy
   */
  sendOrderToPharmacy(orderId: string): Observable<MedicationOrder> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/orders/${orderId}/send-pharmacy`;

    return this.http
      .post<MedicationOrder>(url, {}, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('order')),
        catchError((error) => this.handleError(error, 'sendOrderToPharmacy'))
      );
  }

  // ==================== Pharmacy Fulfillment Operations ====================

  /**
   * Get fulfillment status for an order
   * @param orderId Order ID
   * @returns Observable of pharmacy fulfillment
   */
  getFulfillmentStatus(orderId: string): Observable<PharmacyFulfillment> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/fulfillments/${orderId}`;
    const cacheKey = `fulfillment:${orderId}`;

    const cached = this.getFromCache<PharmacyFulfillment>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<PharmacyFulfillment>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((fulfillment) => this.setInCache(cacheKey, fulfillment)),
        catchError((error) => this.handleError(error, 'getFulfillmentStatus'))
      );
  }

  /**
   * Get pending fulfillments awaiting pharmacy action
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated pending fulfillments
   */
  getPendingFulfillments(
    page: number,
    size: number
  ): Observable<PaginatedResponse<PharmacyFulfillment>> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/fulfillments/pending`;
    const cacheKey = `fulfillment:pending:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<PharmacyFulfillment>>(
      cacheKey
    );
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<PharmacyFulfillment>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getPendingFulfillments')
        )
      );
  }

  /**
   * Get fulfillments ready for pickup by patient
   * @param patientId Patient ID
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated ready-for-pickup fulfillments
   */
  getFulfillmentsReadyForPickup(
    patientId: string,
    page: number,
    size: number
  ): Observable<PaginatedResponse<PharmacyFulfillment>> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/fulfillments/patient/${patientId}/ready`;
    const cacheKey = `fulfillment:patient:${patientId}:ready:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<PharmacyFulfillment>>(
      cacheKey
    );
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<PharmacyFulfillment>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getFulfillmentsReadyForPickup')
        )
      );
  }

  /**
   * Update fulfillment status
   * @param fulfillmentId Fulfillment ID
   * @param fulfillment Updated fulfillment data
   * @returns Observable of updated fulfillment
   */
  updateFulfillmentStatus(
    fulfillmentId: string,
    fulfillment: PharmacyFulfillment
  ): Observable<PharmacyFulfillment> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/fulfillments/${fulfillmentId}`;

    return this.http
      .put<PharmacyFulfillment>(url, { ...fulfillment, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('fulfillment')),
        catchError((error) =>
          this.handleError(error, 'updateFulfillmentStatus')
        )
      );
  }

  // ==================== Adverse Event Management ====================

  /**
   * Record a medication adverse event or allergy
   * @param event Adverse event to record
   * @returns Observable of recorded event
   */
  recordAdverseEvent(
    event: MedicationAdverseEvent
  ): Observable<MedicationAdverseEvent> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/adverse-events`;

    return this.http
      .post<MedicationAdverseEvent>(url, { ...event, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('adverse')),
        catchError((error) => this.handleError(error, 'recordAdverseEvent'))
      );
  }

  /**
   * Get patient allergies
   * @param patientId Patient ID
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated allergies
   */
  getPatientAllergies(
    patientId: string,
    page: number,
    size: number
  ): Observable<PaginatedResponse<MedicationAdverseEvent>> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/adverse-events/patient/${patientId}/allergies`;
    const cacheKey = `adverse:patient:${patientId}:allergies:${page}:${size}`;

    const cached = this.getFromCache<
      PaginatedResponse<MedicationAdverseEvent>
    >(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<MedicationAdverseEvent>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) => this.handleError(error, 'getPatientAllergies'))
      );
  }

  /**
   * Get all adverse events for patient
   * @param patientId Patient ID
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated adverse events
   */
  getPatientAdverseEvents(
    patientId: string,
    page: number,
    size: number
  ): Observable<PaginatedResponse<MedicationAdverseEvent>> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/adverse-events/patient/${patientId}`;
    const cacheKey = `adverse:patient:${patientId}:${page}:${size}`;

    const cached = this.getFromCache<
      PaginatedResponse<MedicationAdverseEvent>
    >(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<MedicationAdverseEvent>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getPatientAdverseEvents')
        )
      );
  }

  /**
   * Update adverse event record
   * @param eventId Event ID
   * @param event Updated event data
   * @returns Observable of updated event
   */
  updateAdverseEvent(
    eventId: string,
    event: MedicationAdverseEvent
  ): Observable<MedicationAdverseEvent> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/adverse-events/${eventId}`;

    return this.http
      .put<MedicationAdverseEvent>(url, { ...event, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('adverse')),
        catchError((error) => this.handleError(error, 'updateAdverseEvent'))
      );
  }

  // ==================== Medication Administration ====================

  /**
   * Record medication administration
   * @param administration Administration record
   * @returns Observable of recorded administration
   */
  recordMedicationAdministration(
    administration: MedicationAdministration
  ): Observable<MedicationAdministration> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/administration`;

    return this.http
      .post<MedicationAdministration>(url, { ...administration, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('administration')),
        catchError((error) =>
          this.handleError(error, 'recordMedicationAdministration')
        )
      );
  }

  /**
   * Get scheduled medication administrations for patient
   * @param patientId Patient ID
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated scheduled administrations
   */
  getScheduledAdministrations(
    patientId: string,
    page: number,
    size: number
  ): Observable<PaginatedResponse<MedicationAdministration>> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/administration/patient/${patientId}/scheduled`;
    const cacheKey = `administration:patient:${patientId}:scheduled:${page}:${size}`;

    const cached = this.getFromCache<
      PaginatedResponse<MedicationAdministration>
    >(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<MedicationAdministration>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getScheduledAdministrations')
        )
      );
  }

  /**
   * Get administered medications for today
   * @param patientId Patient ID
   * @returns Observable of administered medications
   */
  getAdministeredMedicationsForToday(
    patientId: string
  ): Observable<MedicationAdministration[]> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/administration/patient/${patientId}/today`;
    const cacheKey = `administration:patient:${patientId}:today`;

    const cached = this.getFromCache<MedicationAdministration[]>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<MedicationAdministration[]>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((medications) => this.setInCache(cacheKey, medications)),
        catchError((error) =>
          this.handleError(error, 'getAdministeredMedicationsForToday')
        )
      );
  }

  /**
   * Update administration status
   * @param administrationId Administration ID
   * @param administration Updated administration data
   * @returns Observable of updated administration
   */
  updateAdministrationStatus(
    administrationId: string,
    administration: MedicationAdministration
  ): Observable<MedicationAdministration> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/administration/${administrationId}`;

    return this.http
      .put<MedicationAdministration>(url, { ...administration, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('administration')),
        catchError((error) =>
          this.handleError(error, 'updateAdministrationStatus')
        )
      );
  }

  // ==================== Drug Interaction Checking ====================

  /**
   * Check for drug interactions among a set of medications (bulk check)
   * @param patientId Patient ID
   * @param medicationIds Array of medication IDs to check for interactions
   * @returns Observable of interaction check results
   */
  checkDrugInteractionsBulk(
    patientId: string,
    medicationIds: string[]
  ): Observable<MedicationInteractionCheck> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/interactions/check-bulk`;
    const cacheKey = `interactions:bulk:${patientId}:${medicationIds.join(',')}`;

    const cached = this.getFromCache<MedicationInteractionCheck>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const body = {
      patientId,
      medicationIds,
    };

    return this.http
      .post<MedicationInteractionCheck>(url, body, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((result) => this.setInCache(cacheKey, result)),
        catchError((error: unknown) => this.handleError(error, 'checkDrugInteractionsBulk'))
      );
  }

  /**
   * Check for drug interactions with new medication
   * @param patientId Patient ID
   * @param currentMedications Array of current medication IDs
   * @param newMedicationId New medication ID to check
   * @returns Observable of interaction check results
   */
  checkDrugInteractions(
    patientId: string,
    currentMedications: string[],
    newMedicationId: string
  ): Observable<MedicationInteractionCheck> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/interactions/check`;
    const cacheKey = `interactions:${patientId}:${currentMedications.join(',')}:${newMedicationId}`;

    const cached = this.getFromCache<MedicationInteractionCheck>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const body = {
      patientId,
      currentMedications,
      newMedicationId,
    };

    return this.http
      .post<MedicationInteractionCheck>(url, body, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((result) => this.setInCache(cacheKey, result)),
        catchError((error) => this.handleError(error, 'checkDrugInteractions'))
      );
  }

  // ==================== Medication Reconciliation Operations ====================

  /**
   * Complete a medication reconciliation session
   * @param reconciliationId Reconciliation session ID
   * @param reconciliationData Complete reconciliation data
   * @returns Observable of completion result
   */
  completeMedicationReconciliation(
    reconciliationId: string,
    reconciliationData: any
  ): Observable<any> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/reconciliations/${reconciliationId}/complete`;

    return this.http
      .post<any>(url, { ...reconciliationData, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('reconciliation')),
        catchError((error: unknown) => this.handleError(error, 'completeMedicationReconciliation'))
      );
  }

  // ==================== Metrics Operations ====================

  /**
   * Get medication adherence metrics for patient
   * @param patientId Patient ID
   * @returns Observable of adherence metrics
   */
  getMedicationAdherenceMetrics(
    patientId: string
  ): Observable<MedicationAdherenceMetrics> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/metrics/adherence/${patientId}`;
    const cacheKey = `metrics:adherence:${patientId}`;

    const cached = this.getFromCache<MedicationAdherenceMetrics>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<MedicationAdherenceMetrics>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((metrics) =>
          this.setInCache(cacheKey, metrics, this.METRICS_CACHE_TTL)
        ),
        catchError((error) =>
          this.handleError(error, 'getMedicationAdherenceMetrics')
        )
      );
  }

  /**
   * Get medication therapy management metrics for patient
   * @param patientId Patient ID
   * @returns Observable of therapy metrics
   */
  getMedicationTherapyMetrics(
    patientId: string
  ): Observable<MedicationTherapyMetrics> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/metrics/therapy/${patientId}`;
    const cacheKey = `metrics:therapy:${patientId}`;

    const cached = this.getFromCache<MedicationTherapyMetrics>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<MedicationTherapyMetrics>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((metrics) =>
          this.setInCache(cacheKey, metrics, this.METRICS_CACHE_TTL)
        ),
        catchError((error) =>
          this.handleError(error, 'getMedicationTherapyMetrics')
        )
      );
  }

  // ==================== Pharmacy Management ====================

  /**
   * Get pharmacy information
   * @param pharmacyId Pharmacy ID
   * @returns Observable of pharmacy data
   */
  getPharmacy(pharmacyId: string): Observable<Pharmacy> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/pharmacies/${pharmacyId}`;
    const cacheKey = `pharmacy:${pharmacyId}`;

    const cached = this.getFromCache<Pharmacy>(cacheKey);
    if (cached) {
      return of(cached);
    }

    return this.http
      .get<Pharmacy>(url, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap((pharmacy) => this.setInCache(cacheKey, pharmacy)),
        catchError((error) => this.handleError(error, 'getPharmacy'))
      );
  }

  /**
   * Get preferred pharmacies for tenant
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated preferred pharmacies
   */
  getPreferredPharmacies(
    page: number,
    size: number
  ): Observable<PaginatedResponse<Pharmacy>> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/pharmacies/preferred`;
    const cacheKey = `pharmacy:preferred:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<Pharmacy>>(cacheKey);
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<Pharmacy>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) => this.handleError(error, 'getPreferredPharmacies'))
      );
  }

  /**
   * Create pharmacy coordination request
   * @param coordination Coordination request
   * @returns Observable of created coordination
   */
  createPharmacyCoordination(
    coordination: PharmacyCoordination
  ): Observable<PharmacyCoordination> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/pharmacy-coordination`;

    return this.http
      .post<PharmacyCoordination>(url, { ...coordination, tenantId }, {
        headers: { 'X-Tenant-ID': tenantId },
      })
      .pipe(
        tap(() => this.invalidateCache('coordination')),
        catchError((error) =>
          this.handleError(error, 'createPharmacyCoordination')
        )
      );
  }

  /**
   * Get active pharmacy coordination requests
   * @param page Page number
   * @param size Page size
   * @returns Observable of paginated active coordinations
   */
  getActivePharmacyCoordinations(
    page: number,
    size: number
  ): Observable<PaginatedResponse<PharmacyCoordination>> {
    const tenantId = this.getTenantContext();
    const url = `${MEDICATION_BASE_URL}/pharmacy-coordination/active`;
    const cacheKey = `coordination:active:${page}:${size}`;

    const cached = this.getFromCache<PaginatedResponse<PharmacyCoordination>>(
      cacheKey
    );
    if (cached) {
      return of(cached);
    }

    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<PaginatedResponse<PharmacyCoordination>>(url, {
        headers: { 'X-Tenant-ID': tenantId },
        params,
      })
      .pipe(
        tap((response) => this.setInCache(cacheKey, response)),
        catchError((error) =>
          this.handleError(error, 'getActivePharmacyCoordinations')
        )
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
    this.logger.error(`[MedicationService] Error in ${context}:`, { error });

    return throwError(() => ({
      status: error.status || 0,
      statusText: error.statusText || 'Unknown Error',
      message: error.error?.message || error.message || 'An unknown error occurred',
      context,
      error,
    }));
  }
}
