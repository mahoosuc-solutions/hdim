/**
 * HDIM Page Object Models Index
 *
 * Central export for all page objects used in E2E testing.
 */

// Base page
export { BasePage } from './base.page';

// Authentication
export { LoginPage } from './login.page';

// Main navigation
export { DashboardPage } from './dashboard.page';

// Patient management
export { PatientSearchPage } from './patient-search.page';

// Quality evaluation
export { EvaluationPage } from './evaluation.page';

// Care gap management
export { CareGapPage } from './care-gap.page';

// Re-export types
export type { PHIMaskingOptions, MaskingResult } from '../utils/phi-masking';
