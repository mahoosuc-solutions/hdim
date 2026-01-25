import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { AlertService } from '../../services/alert.service';
import { LoggerService } from '../../services/logger.service';
import {
  AlertConfig,
  CreateAlertConfigRequest,
  UpdateAlertConfigRequest,
  AlertType,
  AlertSeverity,
  NotificationChannel,
  ALERT_THRESHOLD_PRESETS,
  ALERT_TYPE_LABELS,
  SEVERITY_LABELS,
  NOTIFICATION_CHANNEL_LABELS,
} from '../../models/alert-config.model';
import { SERVICE_DEFINITIONS, ServiceDefinitionMetadata } from '../../models/service-definitions';

/**
 * Alert Configuration Component
 *
 * Manages alert rules for service monitoring.
 * Allows administrators to create, edit, and delete alert configurations.
 *
 * Features:
 * - Create alert rules with custom thresholds
 * - Configure notification channels (EMAIL, SLACK, WEBHOOK, SMS)
 * - Enable/disable alerts
 * - View recent alert events
 *
 * HIPAA Compliance: No PHI is handled in this component.
 */
@Component({
  selector: 'app-alert-config',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './alert-config.component.html',
  styleUrls: ['./alert-config.component.scss'],
})
export class AlertConfigComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private logger = this.loggerService.withContext('AlertConfigComponent');

  // State
  alertConfigs: AlertConfig[] = [];
  loading = true;
  showCreateModal = false;
  showEditModal = false;
  showDeleteConfirmModal = false;
  selectedAlertConfig: AlertConfig | null = null;

  // Form state
  formData: Partial<CreateAlertConfigRequest> = {};
  editFormData: Partial<UpdateAlertConfigRequest> = {};
  formErrors: Record<string, string> = {};

  // Available services (with ports, can expose metrics)
  availableServices: ServiceDefinitionMetadata[] = SERVICE_DEFINITIONS.filter((s) => s.port !== null);

  // Constants exposed to template
  alertTypes: AlertType[] = ['CPU_USAGE', 'MEMORY_USAGE', 'ERROR_RATE', 'LATENCY', 'REQUEST_RATE'];
  severityLevels: AlertSeverity[] = ['INFO', 'WARNING', 'CRITICAL'];
  notificationChannels: NotificationChannel[] = ['EMAIL', 'SLACK', 'WEBHOOK', 'SMS'];

  // Label maps
  alertTypeLabels = ALERT_TYPE_LABELS;
  severityLabels = SEVERITY_LABELS;
  channelLabels = NOTIFICATION_CHANNEL_LABELS;
  thresholdPresets = ALERT_THRESHOLD_PRESETS;

  constructor(
    private alertService: AlertService,
    private loggerService: LoggerService
  ) {}

  ngOnInit(): void {
    this.logger.info('Initializing Alert Configuration');
    this.loadAlertConfigs();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Load all alert configurations
   */
  loadAlertConfigs(): void {
    this.loading = true;
    this.alertService
      .getAllAlertConfigs()
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (configs) => {
          this.alertConfigs = configs;
          this.loading = false;
          this.logger.info('Loaded alert configurations', { count: configs.length });
        },
        error: (error) => {
          this.logger.error('Failed to load alert configurations', error);
          this.loading = false;
        },
      });
  }

  /**
   * Open create alert modal
   */
  openCreateModal(): void {
    this.formData = {
      enabled: true,
      notificationChannels: ['EMAIL'],
      durationMinutes: 5,
    };
    this.formErrors = {};
    this.showCreateModal = true;
  }

  /**
   * Close create alert modal
   */
  closeCreateModal(): void {
    this.showCreateModal = false;
    this.formData = {};
    this.formErrors = {};
  }

  /**
   * Open edit alert modal
   */
  openEditModal(config: AlertConfig): void {
    this.selectedAlertConfig = config;
    this.editFormData = {
      threshold: config.threshold,
      durationMinutes: config.durationMinutes,
      severity: config.severity,
      enabled: config.enabled,
      notificationChannels: [...config.notificationChannels],
    };
    this.formErrors = {};
    this.showEditModal = true;
  }

  /**
   * Close edit alert modal
   */
  closeEditModal(): void {
    this.showEditModal = false;
    this.selectedAlertConfig = null;
    this.editFormData = {};
    this.formErrors = {};
  }

  /**
   * Open delete confirmation modal
   */
  openDeleteConfirmModal(config: AlertConfig): void {
    this.selectedAlertConfig = config;
    this.showDeleteConfirmModal = true;
  }

  /**
   * Close delete confirmation modal
   */
  closeDeleteConfirmModal(): void {
    this.showDeleteConfirmModal = false;
    this.selectedAlertConfig = null;
  }

  /**
   * Handle alert type selection
   * Auto-populate threshold with recommended value
   */
  onAlertTypeChange(): void {
    if (this.formData.alertType) {
      const preset = this.thresholdPresets[this.formData.alertType];
      this.formData.threshold = preset.recommended;
    }
  }

  /**
   * Toggle notification channel selection
   */
  toggleChannel(channel: NotificationChannel, isCreate: boolean = true): void {
    const channels = isCreate
      ? (this.formData.notificationChannels || [])
      : (this.editFormData.notificationChannels || []);

    const index = channels.indexOf(channel);
    if (index > -1) {
      channels.splice(index, 1);
    } else {
      channels.push(channel);
    }

    if (isCreate) {
      this.formData.notificationChannels = channels;
    } else {
      this.editFormData.notificationChannels = channels;
    }
  }

  /**
   * Check if channel is selected
   */
  isChannelSelected(channel: NotificationChannel, isCreate: boolean = true): boolean {
    const channels = isCreate
      ? (this.formData.notificationChannels || [])
      : (this.editFormData.notificationChannels || []);
    return channels.includes(channel);
  }

  /**
   * Validate create form
   */
  validateCreateForm(): boolean {
    this.formErrors = {};
    let isValid = true;

    if (!this.formData.serviceName) {
      this.formErrors['serviceName'] = 'Service is required';
      isValid = false;
    }

    if (!this.formData.displayName) {
      this.formErrors['displayName'] = 'Display name is required';
      isValid = false;
    }

    if (!this.formData.alertType) {
      this.formErrors['alertType'] = 'Alert type is required';
      isValid = false;
    }

    if (this.formData.threshold === undefined || this.formData.threshold <= 0) {
      this.formErrors['threshold'] = 'Threshold must be greater than 0';
      isValid = false;
    }

    if (!this.formData.durationMinutes || this.formData.durationMinutes < 1) {
      this.formErrors['durationMinutes'] = 'Duration must be at least 1 minute';
      isValid = false;
    }

    if (!this.formData.severity) {
      this.formErrors['severity'] = 'Severity is required';
      isValid = false;
    }

    if (!this.formData.notificationChannels || this.formData.notificationChannels.length === 0) {
      this.formErrors['notificationChannels'] = 'At least one notification channel is required';
      isValid = false;
    }

    return isValid;
  }

  /**
   * Create alert configuration
   */
  createAlert(): void {
    if (!this.validateCreateForm()) {
      this.logger.warn('Form validation failed');
      return;
    }

    this.logger.info('Creating alert configuration');

    this.alertService.createAlertConfig(this.formData as CreateAlertConfigRequest)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (config) => {
          this.logger.info('Alert configuration created', { id: config.id });
          this.alertConfigs.push(config);
          this.closeCreateModal();
        },
        error: (error) => {
          this.logger.error('Failed to create alert configuration', error);
          this.formErrors['submit'] = 'Failed to create alert configuration. Please try again.';
        },
      });
  }

  /**
   * Update alert configuration
   */
  updateAlert(): void {
    if (!this.selectedAlertConfig) return;

    this.logger.info('Updating alert configuration', { id: this.selectedAlertConfig.id });

    this.alertService
      .updateAlertConfig(this.selectedAlertConfig.id, this.editFormData)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (config) => {
          this.logger.info('Alert configuration updated', { id: config.id });
          const index = this.alertConfigs.findIndex((c) => c.id === config.id);
          if (index > -1) {
            this.alertConfigs[index] = config;
          }
          this.closeEditModal();
        },
        error: (error) => {
          this.logger.error('Failed to update alert configuration', error);
          this.formErrors['submit'] = 'Failed to update alert configuration. Please try again.';
        },
      });
  }

  /**
   * Delete alert configuration
   */
  deleteAlert(): void {
    if (!this.selectedAlertConfig) return;

    this.logger.info('Deleting alert configuration', { id: this.selectedAlertConfig.id });

    this.alertService
      .deleteAlertConfig(this.selectedAlertConfig.id)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: () => {
          this.logger.info('Alert configuration deleted', { id: this.selectedAlertConfig!.id });
          this.alertConfigs = this.alertConfigs.filter((c) => c.id !== this.selectedAlertConfig!.id);
          this.closeDeleteConfirmModal();
        },
        error: (error) => {
          this.logger.error('Failed to delete alert configuration', error);
        },
      });
  }

  /**
   * Toggle alert enabled/disabled
   */
  toggleAlert(config: AlertConfig): void {
    this.logger.info('Toggling alert configuration', { id: config.id, enabled: !config.enabled });

    this.alertService
      .toggleAlertConfig(config.id, !config.enabled)
      .pipe(takeUntil(this.destroy$))
      .subscribe({
        next: (updatedConfig) => {
          this.logger.info('Alert configuration toggled', { id: updatedConfig.id });
          const index = this.alertConfigs.findIndex((c) => c.id === config.id);
          if (index > -1) {
            this.alertConfigs[index] = updatedConfig;
          }
        },
        error: (error) => {
          this.logger.error('Failed to toggle alert configuration', error);
        },
      });
  }

  /**
   * Get service display name by service ID
   */
  getServiceDisplayName(serviceName: string): string {
    const service = this.availableServices.find((s) => s.id === serviceName);
    return service ? service.displayName : serviceName;
  }

  /**
   * Get threshold unit based on alert type
   */
  getThresholdUnit(alertType: AlertType): string {
    return this.thresholdPresets[alertType]?.unit || '';
  }
}
