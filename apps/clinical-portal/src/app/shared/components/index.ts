/**
 * Shared Components Library Index
 *
 * This file exports all shared components for easy importing throughout the application.
 *
 * Usage:
 * import { StatCardComponent, EmptyStateComponent } from '@app/shared/components';
 */

// Existing components
export * from './loading-button/loading-button.component';
export * from './loading-overlay/loading-overlay.component';

// New Phase 4 components
export * from './stat-card/stat-card.component';
export * from './empty-state/empty-state.component';
export * from './error-banner/error-banner.component';
export * from './filter-panel/filter-panel.component';
export * from './date-range-picker/date-range-picker.component';
export * from './status-badge/status-badge.component';
export * from './page-header/page-header.component';

// Help System components
export * from './help-tooltip/help-tooltip.component';
export * from './help-panel/help-panel.component';

// Safety components
export * from './critical-alert-banner/critical-alert-banner.component';

// Navigation components
export * from './breadcrumb/breadcrumb.component';
export * from './global-search/global-search.component';

// Agent 3C: TDD Swarm Shared Components Library

// Loading & Feedback
export * from './loading-spinner/loading-spinner.component';
export * from './success-banner/success-banner.component';

// Data Display
export * from './data-table/data-table.component';

// Charts & Visualizations
export * from './chart-line/chart-line.component';
export * from './chart-gauge/chart-gauge.component';
export * from './chart-bar/chart-bar.component';

// User Actions
export * from './action-buttons/action-buttons.component';
export * from './confirmation-dialog/confirmation-dialog.component';

// Forms
export * from './form-field/form-field.component';

// Layout Components
export * from './container/container.component';
export * from './card/card.component';
export * from './grid/grid.component';
export * from './sidebar/sidebar.component';
