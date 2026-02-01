/**
 * Shared Notifications Library
 *
 * Provides toast and alert notification system for the application.
 *
 * Usage:
 * 1. Import NotificationContainerComponent in root component
 * 2. Inject NotificationService in any component
 * 3. Call service methods: success(), error(), warning(), info(), alert()
 *
 * Example:
 * ```typescript
 * constructor(private notifications: NotificationService) {}
 *
 * onSave() {
 *   this.notifications.success('Changes saved!');
 * }
 *
 * onDelete() {
 *   this.notifications.alert(
 *     'Confirm Delete',
 *     'Are you sure?',
 *     AlertSeverity.Warning,
 *     'Delete',
 *     'Cancel',
 *     () => this.performDelete(),
 *     () => console.log('Cancelled')
 *   );
 * }
 * ```
 */

export * from './lib/notification.service';
export * from './lib/toast.component';
export * from './lib/alert.component';
export * from './lib/notification-container.component';
