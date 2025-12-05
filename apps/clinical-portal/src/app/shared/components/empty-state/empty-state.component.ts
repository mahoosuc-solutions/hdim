import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { LoadingButtonComponent } from '../loading-button/loading-button.component';

/**
 * EmptyState Component
 *
 * A reusable component for displaying empty states across the application.
 * Shows a large icon, message, and optional action button.
 *
 * Features:
 * - Large icon display with customizable icon
 * - Centered layout optimized for empty state UX
 * - Optional action button with loading states
 * - Flexible message content
 * - Accessible with ARIA attributes
 *
 * @example
 * <app-empty-state
 *   icon="people_outline"
 *   title="No Patients Found"
 *   message="There are no patients matching your criteria. Try adjusting your filters."
 *   actionText="Add Patient"
 *   actionIcon="add"
 *   (action)="onAddPatient()">
 * </app-empty-state>
 */
@Component({
  selector: 'app-empty-state',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    LoadingButtonComponent
  ],
  templateUrl: './empty-state.component.html',
  styleUrls: ['./empty-state.component.scss']
})
export class EmptyStateComponent {
  /** Material icon name for the empty state */
  @Input() icon = 'inbox';

  /** Main title text */
  @Input() title = '';

  /** Descriptive message text */
  @Input() message = '';

  /** Optional action button text */
  @Input() actionText?: string;

  /** Optional action button icon */
  @Input() actionIcon?: string;

  /** Action button loading state */
  @Input() actionLoading = false;

  /** Action button color */
  @Input() actionColor: 'primary' | 'accent' | 'warn' = 'primary';

  /** Custom CSS class for the container */
  @Input() customClass?: string;

  /** Emits when action button is clicked */
  @Output() action = new EventEmitter<void>();

  /**
   * Handle action button click
   */
  onActionClick(): void {
    this.action.emit();
  }
}
