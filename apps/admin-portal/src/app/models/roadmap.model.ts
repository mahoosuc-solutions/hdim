/**
 * Product Roadmap Models
 *
 * Data structures for the visual product roadmap feature including
 * milestones, progress tracking, and category management.
 */

// ==========================================
// Type Definitions
// ==========================================

/**
 * Status of a milestone in the roadmap
 */
export type MilestoneStatus = 'planned' | 'in_progress' | 'completed' | 'blocked';

/**
 * Category for grouping milestones
 */
export type MilestoneCategory = 'gtm' | 'product' | 'infrastructure' | 'compliance';

/**
 * Quarter designation for roadmap timeline
 */
export type Quarter = 'Q1 2026' | 'Q2 2026' | 'Q3 2026' | 'Q4 2026';

// ==========================================
// Core Milestone Models
// ==========================================

/**
 * Main milestone interface representing a single roadmap item
 */
export interface RoadmapMilestone {
  id: string;
  name: string;
  description: string;
  status: MilestoneStatus;
  category: MilestoneCategory;
  quarter: Quarter;
  startDate: string;  // ISO date string
  targetDate: string; // ISO date string
  completedDate?: string; // ISO date string, optional
  completionPercent: number; // 0-100
  dependencies?: string[]; // IDs of other milestones this depends on
  owner: string;
  metrics?: MilestoneMetric[];
  tags?: string[];
}

/**
 * Metrics associated with a milestone for tracking KPIs
 */
export interface MilestoneMetric {
  label: string;
  value: string;
  target?: string;
  unit?: string;
}

// ==========================================
// Statistics & Analytics
// ==========================================

/**
 * Summary statistics for the entire roadmap
 */
export interface RoadmapStats {
  totalMilestones: number;
  completedMilestones: number;
  inProgressMilestones: number;
  plannedMilestones: number;
  blockedMilestones: number;
  overallProgress: number; // percentage
  byCategory: CategoryStats[];
  byQuarter: QuarterStats[];
}

/**
 * Statistics grouped by category
 */
export interface CategoryStats {
  category: MilestoneCategory;
  total: number;
  completed: number;
  progress: number;
}

/**
 * Statistics grouped by quarter
 */
export interface QuarterStats {
  quarter: Quarter;
  total: number;
  completed: number;
  progress: number;
}

// ==========================================
// Display Configuration
// ==========================================

/**
 * Display configuration for milestone categories
 */
export interface CategoryConfig {
  category: MilestoneCategory;
  label: string;
  color: string;
  icon: string;
}

/**
 * Default category configurations with display properties
 */
export const CATEGORY_CONFIGS: CategoryConfig[] = [
  { category: 'gtm', label: 'Go-to-Market', color: '#2196F3', icon: 'rocket_launch' },
  { category: 'product', label: 'Product', color: '#4CAF50', icon: 'inventory_2' },
  { category: 'infrastructure', label: 'Infrastructure', color: '#9C27B0', icon: 'dns' },
  { category: 'compliance', label: 'Compliance', color: '#FF9800', icon: 'verified_user' }
];

// ==========================================
// Filtering & Search
// ==========================================

/**
 * Filter options for roadmap view
 */
export interface RoadmapFilter {
  categories?: MilestoneCategory[];
  statuses?: MilestoneStatus[];
  quarters?: Quarter[];
  searchTerm?: string;
}
