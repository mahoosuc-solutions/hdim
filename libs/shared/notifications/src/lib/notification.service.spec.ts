import { TestBed } from '@angular/core/testing';
import {
  NotificationService,
  NotificationType,
  AlertSeverity,
  Toast,
  Alert,
} from './notification.service';

/**
 * Notification Service Tests
 *
 * Comprehensive test coverage for:
 * - Toast creation and auto-dismiss
 * - Alert creation and user interaction
 * - Notification history tracking
 * - User preferences management
 * - Sound playback
 */
describe('NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [NotificationService],
    });
    service = TestBed.inject(NotificationService);

    // Clear localStorage and sessionStorage
    localStorage.clear();
    sessionStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
    sessionStorage.clear();
  });

  describe('Toast Notifications', () => {
    it('should create a success toast', (done) => {
      let toastReceived = false;

      service.toast$.subscribe((toast) => {
        expect(toast.type).toBe(NotificationType.Success);
        expect(toast.message).toBe('Success message');
        expect(toast.duration).toBe(3000); // Default success duration
        toastReceived = true;
      });

      service.success('Success message');

      setTimeout(() => {
        expect(toastReceived).toBe(true);
        done();
      }, 100);
    });

    it('should create an error toast with longer duration', (done) => {
      service.toast$.subscribe((toast) => {
        expect(toast.type).toBe(NotificationType.Error);
        expect(toast.duration).toBe(5000); // Longer for errors
      });

      service.error('Error message');
      done();
    });

    it('should create a warning toast', (done) => {
      service.toast$.subscribe((toast) => {
        expect(toast.type).toBe(NotificationType.Warning);
        expect(toast.duration).toBe(4000);
      });

      service.warning('Warning message');
      done();
    });

    it('should create an info toast', (done) => {
      service.toast$.subscribe((toast) => {
        expect(toast.type).toBe(NotificationType.Info);
        expect(toast.duration).toBe(3000);
      });

      service.info('Info message');
      done();
    });

    it('should support custom duration', (done) => {
      service.toast$.subscribe((toast) => {
        expect(toast.duration).toBe(6000);
      });

      service.success('Custom duration', 6000);
      done();
    });

    it('should support action button', (done) => {
      const onAction = jasmine.createSpy('onAction');

      service.toast$.subscribe((toast) => {
        expect(toast.actionLabel).toBe('Undo');
        expect(toast.onAction).toBe(onAction);
      });

      service.success('Action toast', 3000, 'Undo', onAction);
      done();
    });

    it('should generate unique IDs for toasts', () => {
      const id1 = service.success('Toast 1');
      const id2 = service.success('Toast 2');

      expect(id1).toBeTruthy();
      expect(id2).toBeTruthy();
      expect(id1).not.toBe(id2);
    });

    it('should add toast to history', () => {
      service.success('History test');

      const history = service.getHistory();
      expect(history.length).toBe(1);
      expect(history[0].type).toBe('toast');
      expect(history[0].message).toBe('History test');
    });

    it('should dismiss toast', () => {
      const id = service.success('Dismiss test');

      let history = service.getHistory();
      expect(history[0].dismissed).toBe(false);

      service.dismissToast(id);

      history = service.getHistory();
      expect(history[0].dismissed).toBe(true);
    });
  });

  describe('Alert Notifications', () => {
    it('should create an info alert', (done) => {
      service.alert$.subscribe((alert) => {
        expect(alert.severity).toBe(AlertSeverity.Info);
        expect(alert.title).toBe('Information');
        expect(alert.message).toBe('Alert message');
        expect(alert.confirmLabel).toBe('OK');
      });

      service.alert('Information', 'Alert message');
      done();
    });

    it('should create a warning alert', (done) => {
      service.alert$.subscribe((alert) => {
        expect(alert.severity).toBe(AlertSeverity.Warning);
      });

      service.alert('Warning', 'Warning message', AlertSeverity.Warning);
      done();
    });

    it('should create an error alert', (done) => {
      service.alert$.subscribe((alert) => {
        expect(alert.severity).toBe(AlertSeverity.Error);
      });

      service.alert('Error', 'Error message', AlertSeverity.Error);
      done();
    });

    it('should create a critical alert', (done) => {
      service.alert$.subscribe((alert) => {
        expect(alert.severity).toBe(AlertSeverity.Critical);
      });

      service.alert('Critical', 'Critical message', AlertSeverity.Critical);
      done();
    });

    it('should support custom button labels', (done) => {
      service.alert$.subscribe((alert) => {
        expect(alert.confirmLabel).toBe('Yes');
        expect(alert.cancelLabel).toBe('No');
      });

      service.alert('Question', 'Continue?', AlertSeverity.Info, 'Yes', 'No');
      done();
    });

    it('should support confirm callback', (done) => {
      const onConfirm = jasmine.createSpy('onConfirm');

      service.alert$.subscribe((alert) => {
        expect(alert.onConfirm).toBe(onConfirm);
      });

      service.alert('Question', 'Confirm?', AlertSeverity.Info, 'OK', undefined, onConfirm);
      done();
    });

    it('should support cancel callback', (done) => {
      const onCancel = jasmine.createSpy('onCancel');

      service.alert$.subscribe((alert) => {
        expect(alert.onCancel).toBe(onCancel);
      });

      service.alert(
        'Question',
        'Confirm?',
        AlertSeverity.Info,
        'OK',
        'Cancel',
        undefined,
        onCancel
      );
      done();
    });

    it('should generate unique IDs for alerts', () => {
      const id1 = service.alert('Alert 1', 'Message 1');
      const id2 = service.alert('Alert 2', 'Message 2');

      expect(id1).toBeTruthy();
      expect(id2).toBeTruthy();
      expect(id1).not.toBe(id2);
    });

    it('should add alert to history', () => {
      service.alert('History Alert', 'Message');

      const history = service.getHistory();
      expect(history.length).toBe(1);
      expect(history[0].type).toBe('alert');
      expect(history[0].title).toBe('History Alert');
    });

    it('should confirm alert', () => {
      const id = service.alert('Confirm Test', 'Message');

      let history = service.getHistory();
      expect(history[0].dismissed).toBe(false);

      service.confirmAlert(id);

      history = service.getHistory();
      expect(history[0].dismissed).toBe(true);
    });

    it('should cancel alert', () => {
      const id = service.alert('Cancel Test', 'Message');

      let history = service.getHistory();
      expect(history[0].dismissed).toBe(false);

      service.cancelAlert(id);

      history = service.getHistory();
      expect(history[0].dismissed).toBe(true);
    });
  });

  describe('Notification History', () => {
    it('should track notification history', () => {
      service.success('Message 1');
      service.info('Message 2');
      service.alert('Title', 'Message 3');

      const history = service.getHistory();
      expect(history.length).toBe(3);
      expect(history[0].message).toBe('Message 3'); // Most recent first
      expect(history[1].message).toBe('Message 2');
      expect(history[2].message).toBe('Message 1');
    });

    it('should limit history to max size', () => {
      service.setPreferences({ maxHistorySize: 5 });

      for (let i = 0; i < 10; i++) {
        service.success(`Message ${i}`);
      }

      const history = service.getHistory();
      expect(history.length).toBe(5);
    });

    it('should clear history', () => {
      service.success('Message 1');
      service.success('Message 2');

      let history = service.getHistory();
      expect(history.length).toBe(2);

      service.clearHistory();

      history = service.getHistory();
      expect(history.length).toBe(0);
    });

    it('should emit history updates', (done) => {
      service.history$.subscribe((history) => {
        if (history.length > 0) {
          expect(history[0].message).toBe('Test');
          done();
        }
      });

      service.success('Test');
    });

    it('should persist history to sessionStorage', () => {
      service.success('Persistent message');

      const stored = sessionStorage.getItem('app-notification-history');
      expect(stored).toBeTruthy();

      const parsed = JSON.parse(stored!);
      expect(parsed.length).toBe(1);
      expect(parsed[0].message).toBe('Persistent message');
    });
  });

  describe('Notification Preferences', () => {
    it('should load default preferences', () => {
      const prefs = service.getPreferences();

      expect(prefs.enableSuccess).toBe(true);
      expect(prefs.enableError).toBe(true);
      expect(prefs.enableWarning).toBe(true);
      expect(prefs.enableInfo).toBe(true);
      expect(prefs.enableSound).toBe(true);
      expect(prefs.maxHistorySize).toBe(50);
    });

    it('should update preferences', () => {
      service.setPreferences({ enableSuccess: false });

      const prefs = service.getPreferences();
      expect(prefs.enableSuccess).toBe(false);
      expect(prefs.enableError).toBe(true); // Others unchanged
    });

    it('should respect success notification preference', () => {
      service.setPreferences({ enableSuccess: false });

      let toastReceived = false;
      service.toast$.subscribe(() => {
        toastReceived = true;
      });

      service.success('Should not appear');

      expect(toastReceived).toBe(false);
    });

    it('should respect error notification preference', () => {
      service.setPreferences({ enableError: false });

      let toastReceived = false;
      service.toast$.subscribe(() => {
        toastReceived = true;
      });

      service.error('Should not appear');

      expect(toastReceived).toBe(false);
    });

    it('should respect warning notification preference', () => {
      service.setPreferences({ enableWarning: false });

      let toastReceived = false;
      service.toast$.subscribe(() => {
        toastReceived = true;
      });

      service.warning('Should not appear');

      expect(toastReceived).toBe(false);
    });

    it('should respect info notification preference', () => {
      service.setPreferences({ enableInfo: false });

      let toastReceived = false;
      service.toast$.subscribe(() => {
        toastReceived = true;
      });

      service.info('Should not appear');

      expect(toastReceived).toBe(false);
    });

    it('should persist preferences to localStorage', () => {
      service.setPreferences({ enableSuccess: false, enableSound: false });

      const stored = localStorage.getItem('app-notification-preferences');
      expect(stored).toBeTruthy();

      const parsed = JSON.parse(stored!);
      expect(parsed.enableSuccess).toBe(false);
      expect(parsed.enableSound).toBe(false);
    });

    it('should emit preference updates', (done) => {
      service.preferences$.subscribe((prefs) => {
        if (!prefs.enableSuccess) {
          expect(prefs.enableSuccess).toBe(false);
          done();
        }
      });

      service.setPreferences({ enableSuccess: false });
    });
  });

  describe('Sound Playback', () => {
    it('should attempt to play sound for success toast', (done) => {
      spyOn(window, 'AudioContext' as any).and.returnValue({
        createOscillator: jasmine.createSpy('createOscillator').and.returnValue({
          connect: jasmine.createSpy('connect'),
          frequency: { value: 0 },
          type: 'sine',
          start: jasmine.createSpy('start'),
          stop: jasmine.createSpy('stop'),
        }),
        createGain: jasmine.createSpy('createGain').and.returnValue({
          connect: jasmine.createSpy('connect'),
          gain: { setValueAtTime: jasmine.createSpy(), exponentialRampToValueAtTime: jasmine.createSpy() },
        }),
        destination: {},
        currentTime: 0,
      });

      service.success('With sound');
      setTimeout(() => {
        expect(window.AudioContext).toHaveBeenCalled();
        done();
      }, 100);
    });

    it('should respect sound preference', (done) => {
      service.setPreferences({ enableSound: false });

      spyOn(window, 'AudioContext' as any).and.returnValue({
        createOscillator: jasmine.createSpy(),
        createGain: jasmine.createSpy(),
      });

      service.success('No sound');

      setTimeout(() => {
        expect(window.AudioContext).not.toHaveBeenCalled();
        done();
      }, 100);
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty message gracefully', () => {
      const id = service.success('');
      expect(id).toBeTruthy();
    });

    it('should handle very long messages', () => {
      const longMessage = 'x'.repeat(1000);
      service.success(longMessage);

      const history = service.getHistory();
      expect(history[0].message).toBe(longMessage);
    });

    it('should handle rapid notification creation', () => {
      for (let i = 0; i < 100; i++) {
        service.success(`Message ${i}`);
      }

      const history = service.getHistory();
      expect(history.length).toBe(50); // Limited by maxHistorySize
    });

    it('should handle null callbacks gracefully', () => {
      const id = service.success('No callback', 3000, 'Action');
      expect(id).toBeTruthy();
    });
  });
});
