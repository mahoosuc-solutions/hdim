import { Injectable } from '@angular/core';
import { BehaviorSubject, Subject } from 'rxjs';
import { v4 as uuidv4 } from 'uuid';

/**
 * Notification types supported by the system
 */
export enum NotificationType {
  Success = 'success',
  Error = 'error',
  Warning = 'warning',
  Info = 'info',
}

/**
 * Alert severity levels
 */
export enum AlertSeverity {
  Info = 'info',
  Warning = 'warning',
  Error = 'error',
  Critical = 'critical',
}

/**
 * Toast notification data structure
 */
export interface Toast {
  id: string;
  type: NotificationType;
  message: string;
  duration?: number; // milliseconds, 0 = no auto-dismiss
  actionLabel?: string;
  onAction?: () => void;
  timestamp: number;
}

/**
 * Alert notification data structure
 */
export interface Alert {
  id: string;
  severity: AlertSeverity;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  onConfirm?: () => void;
  onCancel?: () => void;
  timestamp: number;
}

/**
 * Notification history entry
 */
export interface NotificationHistoryEntry {
  id: string;
  type: 'toast' | 'alert';
  severity?: AlertSeverity;
  notificationType?: NotificationType;
  message: string;
  title?: string;
  timestamp: number;
  dismissed: boolean;
}

/**
 * Notification preferences
 */
export interface NotificationPreferences {
  enableSuccess: boolean;
  enableError: boolean;
  enableWarning: boolean;
  enableInfo: boolean;
  enableSound: boolean;
  maxHistorySize: number;
}

/**
 * Notification Service
 *
 * Manages toast and alert notifications for the application.
 * Provides:
 * - Toast notifications with auto-dismiss
 * - Alert notifications requiring user action
 * - Notification history tracking
 * - User preferences management
 * - WebSocket alert integration
 *
 * ★ Insight ─────────────────────────────────────
 * This service uses RxJS Subjects for reactive notification flow:
 * - toast$ Observable for toast display
 * - alert$ Observable for alert display
 * - history$ Observable for notification history updates
 * Components subscribe to these observables to display notifications.
 * Using Subjects allows decoupled notification triggers throughout the app.
 * ─────────────────────────────────────────────────
 */
@Injectable({
  providedIn: 'root',
})
export class NotificationService {
  // Public observables for components to subscribe
  private toastSubject = new Subject<Toast>();
  public toast$ = this.toastSubject.asObservable();

  private alertSubject = new Subject<Alert>();
  public alert$ = this.alertSubject.asObservable();

  private historySubject = new BehaviorSubject<NotificationHistoryEntry[]>([]);
  public history$ = this.historySubject.asObservable();

  private preferencesSubject = new BehaviorSubject<NotificationPreferences>({
    enableSuccess: true,
    enableError: true,
    enableWarning: true,
    enableInfo: true,
    enableSound: true,
    maxHistorySize: 50,
  });
  public preferences$ = this.preferencesSubject.asObservable();

  // Track active toasts for stacking
  private activeToasts = new Map<string, Toast>();

  constructor() {
    this.loadPreferences();
  }

  /**
   * Show a success toast
   */
  success(message: string, duration = 3000, actionLabel?: string, onAction?: () => void): string {
    return this.showToast(NotificationType.Success, message, duration, actionLabel, onAction);
  }

  /**
   * Show an error toast
   */
  error(message: string, duration = 5000, actionLabel?: string, onAction?: () => void): string {
    return this.showToast(NotificationType.Error, message, duration, actionLabel, onAction);
  }

  /**
   * Show a warning toast
   */
  warning(message: string, duration = 4000, actionLabel?: string, onAction?: () => void): string {
    return this.showToast(NotificationType.Warning, message, duration, actionLabel, onAction);
  }

  /**
   * Show an info toast
   */
  info(message: string, duration = 3000, actionLabel?: string, onAction?: () => void): string {
    return this.showToast(NotificationType.Info, message, duration, actionLabel, onAction);
  }

  /**
   * Internal method to show a toast
   */
  private showToast(
    type: NotificationType,
    message: string,
    duration: number,
    actionLabel?: string,
    onAction?: () => void
  ): string {
    // Check preferences
    const prefs = this.preferencesSubject.value;
    if (!this.isTypeEnabled(type, prefs)) {
      return '';
    }

    const id = uuidv4();
    const toast: Toast = {
      id,
      type,
      message,
      duration,
      actionLabel,
      onAction,
      timestamp: Date.now(),
    };

    // Track active toast
    this.activeToasts.set(id, toast);

    // Emit toast
    this.toastSubject.next(toast);

    // Add to history
    this.addToHistory({
      id,
      type: 'toast',
      notificationType: type,
      message,
      timestamp: Date.now(),
      dismissed: false,
    });

    // Auto-dismiss if duration > 0
    if (duration > 0) {
      setTimeout(() => {
        this.dismissToast(id);
      }, duration);
    }

    // Play sound if enabled
    if (prefs.enableSound) {
      this.playNotificationSound();
    }

    return id;
  }

  /**
   * Dismiss a specific toast
   */
  dismissToast(id: string): void {
    this.activeToasts.delete(id);

    // Update history
    const history = this.historySubject.value;
    const entry = history.find((h) => h.id === id && h.type === 'toast');
    if (entry) {
      entry.dismissed = true;
    }
    this.historySubject.next([...history]);
  }

  /**
   * Show an alert (requires user action)
   */
  alert(
    title: string,
    message: string,
    severity: AlertSeverity = AlertSeverity.Info,
    confirmLabel = 'OK',
    cancelLabel?: string,
    onConfirm?: () => void,
    onCancel?: () => void
  ): string {
    const id = uuidv4();
    const alert: Alert = {
      id,
      severity,
      title,
      message,
      confirmLabel,
      cancelLabel,
      onConfirm,
      onCancel,
      timestamp: Date.now(),
    };

    // Emit alert
    this.alertSubject.next(alert);

    // Add to history
    this.addToHistory({
      id,
      type: 'alert',
      severity,
      title,
      message,
      timestamp: Date.now(),
      dismissed: false,
    });

    // Play sound if critical
    const prefs = this.preferencesSubject.value;
    if (severity === AlertSeverity.Critical && prefs.enableSound) {
      this.playNotificationSound(true);
    }

    return id;
  }

  /**
   * Confirm an alert (user confirmed)
   */
  confirmAlert(id: string): void {
    const history = this.historySubject.value;
    const entry = history.find((h) => h.id === id && h.type === 'alert');
    if (entry) {
      entry.dismissed = true;
    }
    this.historySubject.next([...history]);
  }

  /**
   * Cancel an alert (user cancelled)
   */
  cancelAlert(id: string): void {
    const history = this.historySubject.value;
    const entry = history.find((h) => h.id === id && h.type === 'alert');
    if (entry) {
      entry.dismissed = true;
    }
    this.historySubject.next([...history]);
  }

  /**
   * Get notification history
   */
  getHistory(): NotificationHistoryEntry[] {
    return this.historySubject.value;
  }

  /**
   * Clear notification history
   */
  clearHistory(): void {
    this.historySubject.next([]);
    this.saveHistory([]);
  }

  /**
   * Update notification preferences
   */
  setPreferences(prefs: Partial<NotificationPreferences>): void {
    const current = this.preferencesSubject.value;
    const updated = { ...current, ...prefs };
    this.preferencesSubject.next(updated);
    this.savePreferences(updated);
  }

  /**
   * Get current preferences
   */
  getPreferences(): NotificationPreferences {
    return this.preferencesSubject.value;
  }

  /**
   * Check if a notification type is enabled
   */
  private isTypeEnabled(type: NotificationType, prefs: NotificationPreferences): boolean {
    switch (type) {
      case NotificationType.Success:
        return prefs.enableSuccess;
      case NotificationType.Error:
        return prefs.enableError;
      case NotificationType.Warning:
        return prefs.enableWarning;
      case NotificationType.Info:
        return prefs.enableInfo;
      default:
        return true;
    }
  }

  /**
   * Add entry to notification history
   */
  private addToHistory(entry: NotificationHistoryEntry): void {
    const history = this.historySubject.value;
    const prefs = this.preferencesSubject.value;

    // Add new entry
    const updated = [entry, ...history];

    // Trim to max size
    if (updated.length > prefs.maxHistorySize) {
      updated.splice(prefs.maxHistorySize);
    }

    this.historySubject.next(updated);
    this.saveHistory(updated);
  }

  /**
   * Play notification sound
   */
  private playNotificationSound(critical = false): void {
    try {
      // Use Web Audio API for cross-browser support
      const audioContext = new (window as any).AudioContext || (window as any).webkitAudioContext();

      const oscillator = audioContext.createOscillator();
      const gainNode = audioContext.createGain();

      oscillator.connect(gainNode);
      gainNode.connect(audioContext.destination);

      // Frequency: 800Hz for normal, 1000Hz for critical
      oscillator.frequency.value = critical ? 1000 : 800;
      oscillator.type = 'sine';

      // Duration: 150ms for normal, 300ms for critical
      const duration = critical ? 0.3 : 0.15;
      gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + duration);

      oscillator.start(audioContext.currentTime);
      oscillator.stop(audioContext.currentTime + duration);
    } catch (error) {
      // Web Audio API not available, silently fail
    }
  }

  /**
   * Save preferences to localStorage
   */
  private savePreferences(prefs: NotificationPreferences): void {
    try {
      localStorage.setItem('app-notification-preferences', JSON.stringify(prefs));
    } catch (error) {
      // localStorage not available or full, silently fail
    }
  }

  /**
   * Load preferences from localStorage
   */
  private loadPreferences(): void {
    try {
      const stored = localStorage.getItem('app-notification-preferences');
      if (stored) {
        const prefs = JSON.parse(stored);
        this.preferencesSubject.next({ ...this.preferencesSubject.value, ...prefs });
      }
    } catch (error) {
      // localStorage not available, use defaults
    }
  }

  /**
   * Save history to sessionStorage
   */
  private saveHistory(history: NotificationHistoryEntry[]): void {
    try {
      sessionStorage.setItem('app-notification-history', JSON.stringify(history));
    } catch (error) {
      // sessionStorage not available, silently fail
    }
  }

  /**
   * Load history from sessionStorage
   */
  private loadHistory(): void {
    try {
      const stored = sessionStorage.getItem('app-notification-history');
      if (stored) {
        const history = JSON.parse(stored);
        this.historySubject.next(history);
      }
    } catch (error) {
      // sessionStorage not available, use defaults
    }
  }
}
