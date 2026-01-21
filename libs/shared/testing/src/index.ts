/**
 * TDD Testing Library for Clinical MFE Platform
 *
 * Provides:
 * - TDD Swarm testing infrastructure
 * - Mock data and services
 * - Test scenarios for all MFEs
 * - Coordinated testing utilities
 */

// Test harness and infrastructure
export * from './lib/tdd-harness';

// Test scenarios
export * from './lib/mfe-quality.test-scenarios';
export * from './lib/mfe-care-gaps.test-scenarios';
export * from './lib/mfe-reports.test-scenarios';
