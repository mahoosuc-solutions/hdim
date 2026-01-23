/**
 * User Role Service
 *
 * Manages user roles and provides role-specific configurations
 * Supports Medical Assistants (MA), Registered Nurses (RN), and Providers (MD/DO/PA/NP)
 */

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';
import { LoggerService } from '../../services/logger.service';

export enum UserRole {
  MEDICAL_ASSISTANT = 'MA',
  REGISTERED_NURSE = 'RN',
  PROVIDER = 'PROVIDER',
  ADMIN = 'ADMIN'
}

export interface RoleConfig {
  role: UserRole;
  displayName: string;
  icon: string;
  color: string;
  permissions: string[];
  defaultView: string;
  quickActions: QuickAction[];
}

export interface QuickAction {
  id: string;
  label: string;
  icon: string;
  action: string;
  color?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserRoleService {
  private currentRoleSubject = new BehaviorSubject<UserRole>(UserRole.PROVIDER);
  public currentRole$ = this.currentRoleSubject.asObservable();

  private readonly ROLE_CONFIGS: Record<UserRole, RoleConfig> = {
    [UserRole.MEDICAL_ASSISTANT]: {
      role: UserRole.MEDICAL_ASSISTANT,
      displayName: 'Medical Assistant',
      icon: 'medical_services',
      color: '#7b1fa2',
      permissions: ['view_patients', 'schedule_appointments', 'update_vitals', 'view_care_gaps'],
      defaultView: '/dashboard/ma',
      quickActions: [
        { id: 'check_in', label: 'Patient Check-in', icon: 'how_to_reg', action: 'CHECK_IN', color: '#4caf50' },
        { id: 'vitals', label: 'Record Vitals', icon: 'favorite', action: 'RECORD_VITALS', color: '#f44336' },
        { id: 'schedule', label: 'Schedule Follow-up', icon: 'event', action: 'SCHEDULE', color: '#2196f3' },
        { id: 'prep_room', label: 'Prep Exam Room', icon: 'meeting_room', action: 'PREP_ROOM', color: '#ff9800' }
      ]
    },
    [UserRole.REGISTERED_NURSE]: {
      role: UserRole.REGISTERED_NURSE,
      displayName: 'Registered Nurse',
      icon: 'local_hospital',
      color: '#1976d2',
      permissions: ['view_patients', 'update_care_plans', 'manage_medications', 'view_care_gaps', 'coordinate_care'],
      defaultView: '/dashboard/rn',
      quickActions: [
        { id: 'care_plan', label: 'Update Care Plan', icon: 'assignment', action: 'CARE_PLAN', color: '#1976d2' },
        { id: 'patient_ed', label: 'Patient Education', icon: 'school', action: 'PATIENT_ED', color: '#4caf50' },
        { id: 'medication', label: 'Med Reconciliation', icon: 'medication', action: 'MED_REVIEW', color: '#f44336' },
        { id: 'referral', label: 'Coordinate Referral', icon: 'send', action: 'REFERRAL', color: '#ff9800' },
        { id: 'followup', label: 'Schedule Follow-up', icon: 'phone', action: 'FOLLOWUP_CALL', color: '#9c27b0' }
      ]
    },
    [UserRole.PROVIDER]: {
      role: UserRole.PROVIDER,
      displayName: 'Provider',
      icon: 'stethoscope',
      color: '#388e3c',
      permissions: ['full_access'],
      defaultView: '/dashboard/provider',
      quickActions: [
        { id: 'review', label: 'Review Results', icon: 'analytics', action: 'REVIEW_RESULTS', color: '#1976d2' },
        { id: 'prescribe', label: 'E-Prescribe', icon: 'medication', action: 'PRESCRIBE', color: '#f44336' },
        { id: 'order', label: 'Order Tests', icon: 'science', action: 'ORDER_TESTS', color: '#4caf50' },
        { id: 'referral', label: 'Create Referral', icon: 'send', action: 'CREATE_REFERRAL', color: '#ff9800' },
        { id: 'note', label: 'Clinical Note', icon: 'note_add', action: 'CLINICAL_NOTE', color: '#9c27b0' }
      ]
    },
    [UserRole.ADMIN]: {
      role: UserRole.ADMIN,
      displayName: 'Administrator',
      icon: 'admin_panel_settings',
      color: '#f57c00',
      permissions: ['full_access', 'manage_users', 'system_config'],
      defaultView: '/dashboard',
      quickActions: [
        { id: 'users', label: 'Manage Users', icon: 'people', action: 'MANAGE_USERS', color: '#1976d2' },
        { id: 'reports', label: 'Generate Reports', icon: 'assessment', action: 'REPORTS', color: '#4caf50' },
        { id: 'settings', label: 'System Settings', icon: 'settings', action: 'SETTINGS', color: '#ff9800' }
      ]
    }
  };

  private logger = this.loggerService.withContext('UserRoleService');

  constructor(private loggerService: LoggerService) {
    // Load saved role from localStorage
    this.loadSavedRole();
  }

  /**
   * Get current user role
   */
  getCurrentRole(): UserRole {
    return this.currentRoleSubject.value;
  }

  /**
   * Set current user role
   */
  setRole(role: UserRole): void {
    this.currentRoleSubject.next(role);
    this.saveRole(role);
  }

  /**
   * Get configuration for specific role
   */
  getRoleConfig(role: UserRole): RoleConfig {
    return this.ROLE_CONFIGS[role];
  }

  /**
   * Get current role configuration
   */
  getCurrentRoleConfig(): RoleConfig {
    return this.ROLE_CONFIGS[this.getCurrentRole()];
  }

  /**
   * Check if current user has specific permission
   */
  hasPermission(permission: string): boolean {
    const config = this.getCurrentRoleConfig();
    return config.permissions.includes('full_access') || config.permissions.includes(permission);
  }

  /**
   * Get all available roles
   */
  getAllRoles(): UserRole[] {
    return Object.values(UserRole);
  }

  /**
   * Get all role configurations
   */
  getAllRoleConfigs(): RoleConfig[] {
    return Object.values(this.ROLE_CONFIGS);
  }

  /**
   * Save role to localStorage
   */
  private saveRole(role: UserRole): void {
    try {
      localStorage.setItem('userRole', role);
    } catch (err) {
      this.logger.error('Error saving user role:', err);
    }
  }

  /**
   * Load saved role from localStorage
   */
  private loadSavedRole(): void {
    try {
      const saved = localStorage.getItem('userRole');
      if (saved && Object.values(UserRole).includes(saved as UserRole)) {
        this.currentRoleSubject.next(saved as UserRole);
      }
    } catch (err) {
      this.logger.error('Error loading user role:', err);
    }
  }

  /**
   * Get role-specific dashboard metrics
   */
  getRoleDashboardMetrics(role: UserRole): string[] {
    switch (role) {
      case UserRole.MEDICAL_ASSISTANT:
        return [
          'patients_scheduled_today',
          'patients_checked_in',
          'vitals_pending',
          'rooms_ready'
        ];
      case UserRole.REGISTERED_NURSE:
        return [
          'care_gaps_assigned',
          'patient_calls_pending',
          'med_reconciliations_needed',
          'patient_education_due'
        ];
      case UserRole.PROVIDER:
        return [
          'patients_scheduled_today',
          'results_to_review',
          'care_gaps_high_priority',
          'quality_score'
        ];
      case UserRole.ADMIN:
        return [
          'total_patients',
          'overall_quality_score',
          'staff_productivity',
          'system_health'
        ];
      default:
        return [];
    }
  }

  /**
   * Get role-specific care gap filters
   */
  getRoleCareGapFilters(role: UserRole): any {
    switch (role) {
      case UserRole.MEDICAL_ASSISTANT:
        return {
          assignedTo: 'MA',
          categories: ['vitals', 'screening', 'immunization']
        };
      case UserRole.REGISTERED_NURSE:
        return {
          assignedTo: 'RN',
          categories: ['education', 'care_coordination', 'medication_management']
        };
      case UserRole.PROVIDER:
        return {
          priority: 'high',
          requiresProviderAction: true
        };
      default:
        return {};
    }
  }
}
