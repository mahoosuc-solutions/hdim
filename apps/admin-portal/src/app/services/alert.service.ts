import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError, tap } from 'rxjs/operators';
import { LoggerService } from './logger.service';
import {
  AlertConfig,
  CreateAlertConfigRequest,
  UpdateAlertConfigRequest,
  AlertEvent,
  AlertType,
  AlertSeverity,
  NotificationChannel,
} from '../models/alert-config.model';

/**
 * Alert Configuration Service
 *
 * Manages alert configurations and alert events.
 * Provides CRUD operations for alerting rules and query capabilities for alert history.
 *
 * Backend API: /api/v1/admin/alerts
 */
@Injectable({
  providedIn: 'root',
})
export class AlertService {
  private apiUrl = '/api/v1/admin/alerts';
  private logger = this.loggerService.withContext('AlertService');

  constructor(
    private http: HttpClient,
    private loggerService: LoggerService
  ) {}

  /**
   * Get all alert configurations
   * @returns Observable<AlertConfig[]>
   */
  getAllAlertConfigs(): Observable<AlertConfig[]> {
    this.logger.info('Fetching all alert configurations');

    return this.http.get<AlertConfig[]>(`${this.apiUrl}/configs`).pipe(
      tap((configs) => this.logger.info('Fetched alert configurations', { count: configs.length })),
      catchError((error) => {
        this.logger.error('Failed to fetch alert configurations', error);
        return of(this.getMockAlertConfigs());
      })
    );
  }

  /**
   * Get alert configuration by ID
   * @param id Alert config ID
   * @returns Observable<AlertConfig>
   */
  getAlertConfig(id: string): Observable<AlertConfig> {
    this.logger.info('Fetching alert configuration', { id });

    return this.http.get<AlertConfig>(`${this.apiUrl}/configs/${id}`).pipe(
      tap((config) => this.logger.info('Fetched alert configuration', { id: config.id })),
      catchError((error) => {
        this.logger.error('Failed to fetch alert configuration', error);
        const mockConfig = this.getMockAlertConfigs().find((c) => c.id === id);
        return of(mockConfig!);
      })
    );
  }

  /**
   * Create a new alert configuration
   * @param request Alert configuration data
   * @returns Observable<AlertConfig>
   */
  createAlertConfig(request: CreateAlertConfigRequest): Observable<AlertConfig> {
    this.logger.info('Creating alert configuration', { serviceName: request.serviceName });

    return this.http.post<AlertConfig>(`${this.apiUrl}/configs`, request).pipe(
      tap((config) => this.logger.info('Created alert configuration', { id: config.id })),
      catchError((error) => {
        this.logger.error('Failed to create alert configuration', error);
        // Return mock config for development
        const mockConfig: AlertConfig = {
          id: Math.random().toString(36).substring(7),
          ...request,
          createdAt: new Date(),
          updatedAt: new Date(),
          createdBy: 'current-user',
        };
        return of(mockConfig);
      })
    );
  }

  /**
   * Update an existing alert configuration
   * @param id Alert config ID
   * @param request Update data
   * @returns Observable<AlertConfig>
   */
  updateAlertConfig(id: string, request: UpdateAlertConfigRequest): Observable<AlertConfig> {
    this.logger.info('Updating alert configuration', { id });

    return this.http.put<AlertConfig>(`${this.apiUrl}/configs/${id}`, request).pipe(
      tap((config) => this.logger.info('Updated alert configuration', { id: config.id })),
      catchError((error) => {
        this.logger.error('Failed to update alert configuration', error);
        return of({} as AlertConfig);
      })
    );
  }

  /**
   * Delete an alert configuration
   * @param id Alert config ID
   * @returns Observable<void>
   */
  deleteAlertConfig(id: string): Observable<void> {
    this.logger.info('Deleting alert configuration', { id });

    return this.http.delete<void>(`${this.apiUrl}/configs/${id}`).pipe(
      tap(() => this.logger.info('Deleted alert configuration', { id })),
      catchError((error) => {
        this.logger.error('Failed to delete alert configuration', error);
        return of(undefined);
      })
    );
  }

  /**
   * Toggle alert configuration enabled/disabled
   * @param id Alert config ID
   * @param enabled New enabled state
   * @returns Observable<AlertConfig>
   */
  toggleAlertConfig(id: string, enabled: boolean): Observable<AlertConfig> {
    this.logger.info('Toggling alert configuration', { id, enabled });

    return this.updateAlertConfig(id, { enabled });
  }

  /**
   * Get recent alert events
   * @param limit Maximum number of events to return
   * @returns Observable<AlertEvent[]>
   */
  getRecentAlertEvents(limit: number = 50): Observable<AlertEvent[]> {
    this.logger.info('Fetching recent alert events', { limit });

    return this.http.get<AlertEvent[]>(`${this.apiUrl}/events?limit=${limit}`).pipe(
      tap((events) => this.logger.info('Fetched alert events', { count: events.length })),
      catchError((error) => {
        this.logger.error('Failed to fetch alert events', error);
        return of(this.getMockAlertEvents());
      })
    );
  }

  /**
   * Acknowledge an alert event
   * @param eventId Alert event ID
   * @returns Observable<AlertEvent>
   */
  acknowledgeAlertEvent(eventId: string): Observable<AlertEvent> {
    this.logger.info('Acknowledging alert event', { eventId });

    return this.http.post<AlertEvent>(`${this.apiUrl}/events/${eventId}/acknowledge`, {}).pipe(
      tap((event) => this.logger.info('Acknowledged alert event', { eventId: event.id })),
      catchError((error) => {
        this.logger.error('Failed to acknowledge alert event', error);
        return of({} as AlertEvent);
      })
    );
  }

  /**
   * Mock alert configurations for development
   */
  private getMockAlertConfigs(): AlertConfig[] {
    const now = new Date();
    return [
      {
        id: 'alert-1',
        serviceName: 'patient-service',
        displayName: 'Patient Service - High CPU',
        alertType: 'CPU_USAGE' as AlertType,
        threshold: 80,
        durationMinutes: 5,
        severity: 'WARNING' as AlertSeverity,
        enabled: true,
        notificationChannels: ['EMAIL', 'SLACK'] as NotificationChannel[],
        createdAt: now,
        updatedAt: now,
        createdBy: 'admin',
      },
      {
        id: 'alert-2',
        serviceName: 'quality-measure-service',
        displayName: 'Quality Measure - High Memory',
        alertType: 'MEMORY_USAGE' as AlertType,
        threshold: 850,
        durationMinutes: 10,
        severity: 'CRITICAL' as AlertSeverity,
        enabled: true,
        notificationChannels: ['EMAIL', 'SLACK', 'WEBHOOK'] as NotificationChannel[],
        createdAt: now,
        updatedAt: now,
        createdBy: 'admin',
      },
      {
        id: 'alert-3',
        serviceName: 'care-gap-service',
        displayName: 'Care Gap - High Error Rate',
        alertType: 'ERROR_RATE' as AlertType,
        threshold: 5,
        durationMinutes: 3,
        severity: 'CRITICAL' as AlertSeverity,
        enabled: true,
        notificationChannels: ['EMAIL', 'SLACK', 'SMS'] as NotificationChannel[],
        createdAt: now,
        updatedAt: now,
        createdBy: 'admin',
      },
      {
        id: 'alert-4',
        serviceName: 'fhir-service',
        displayName: 'FHIR Service - High Latency',
        alertType: 'LATENCY' as AlertType,
        threshold: 500,
        durationMinutes: 5,
        severity: 'WARNING' as AlertSeverity,
        enabled: false,
        notificationChannels: ['EMAIL'] as NotificationChannel[],
        createdAt: now,
        updatedAt: now,
        createdBy: 'admin',
      },
    ];
  }

  /**
   * Mock alert events for development
   */
  private getMockAlertEvents(): AlertEvent[] {
    const now = new Date();
    return [
      {
        id: 'event-1',
        alertConfigId: 'alert-1',
        serviceName: 'patient-service',
        alertType: 'CPU_USAGE' as AlertType,
        severity: 'WARNING' as AlertSeverity,
        currentValue: 85,
        threshold: 80,
        message: 'CPU usage exceeded 80% for patient-service',
        triggeredAt: new Date(now.getTime() - 15 * 60 * 1000), // 15 minutes ago
        acknowledged: false,
      },
      {
        id: 'event-2',
        alertConfigId: 'alert-2',
        serviceName: 'quality-measure-service',
        alertType: 'MEMORY_USAGE' as AlertType,
        severity: 'CRITICAL' as AlertSeverity,
        currentValue: 920,
        threshold: 850,
        message: 'Memory usage exceeded 850 MB for quality-measure-service',
        triggeredAt: new Date(now.getTime() - 30 * 60 * 1000), // 30 minutes ago
        resolvedAt: new Date(now.getTime() - 10 * 60 * 1000), // 10 minutes ago
        acknowledged: true,
        acknowledgedBy: 'admin',
        acknowledgedAt: new Date(now.getTime() - 25 * 60 * 1000), // 25 minutes ago
      },
    ];
  }
}
