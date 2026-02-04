import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  RoadmapMilestone,
  RoadmapStats,
  RoadmapFilter,
  MilestoneCategory,
  MilestoneStatus,
  Quarter,
  CATEGORY_CONFIGS,
} from '../../models/roadmap.model';

/**
 * Mock data for the roadmap - will be replaced by RoadmapService when available
 */
const MOCK_MILESTONES: RoadmapMilestone[] = [
  {
    id: 'M001',
    name: 'Series A Documentation Package',
    description: 'Complete investor deck, financial projections, and due diligence materials for Series A fundraise.',
    status: 'completed',
    category: 'gtm',
    quarter: 'Q1 2026',
    startDate: '2026-01-01',
    targetDate: '2026-01-31',
    completedDate: '2026-01-28',
    completionPercent: 100,
    owner: 'CEO',
    metrics: [
      { label: 'Deck Version', value: '3.2', target: 'Final' },
      { label: 'Investor Meetings', value: '12', target: '15' },
    ],
    tags: ['fundraise', 'priority'],
  },
  {
    id: 'M002',
    name: 'HIPAA Certification Audit',
    description: 'Complete SOC 2 Type II and HIPAA compliance audit with third-party auditor.',
    status: 'in_progress',
    category: 'compliance',
    quarter: 'Q1 2026',
    startDate: '2026-01-15',
    targetDate: '2026-03-15',
    completionPercent: 65,
    owner: 'CTO',
    metrics: [
      { label: 'Controls Validated', value: '45', target: '68' },
      { label: 'Findings', value: '3', target: '0' },
    ],
    tags: ['compliance', 'critical'],
  },
  {
    id: 'M003',
    name: 'CQL Engine v2.0 Release',
    description: 'Major release of the CQL evaluation engine with performance improvements and new measure support.',
    status: 'in_progress',
    category: 'product',
    quarter: 'Q1 2026',
    startDate: '2025-12-01',
    targetDate: '2026-02-28',
    completionPercent: 80,
    owner: 'Engineering Lead',
    dependencies: ['M002'],
    metrics: [
      { label: 'Measures Supported', value: '48', target: '55' },
      { label: 'Performance Gain', value: '3.2x', target: '3x' },
    ],
    tags: ['core-product'],
  },
  {
    id: 'M004',
    name: 'Kubernetes Migration',
    description: 'Migrate from Docker Compose to Kubernetes for production deployment scalability.',
    status: 'planned',
    category: 'infrastructure',
    quarter: 'Q2 2026',
    startDate: '2026-04-01',
    targetDate: '2026-05-31',
    completionPercent: 0,
    owner: 'DevOps Lead',
    dependencies: ['M003'],
    tags: ['infrastructure', 'scalability'],
  },
  {
    id: 'M005',
    name: 'First Enterprise Customer Launch',
    description: 'Successfully onboard and go-live with first enterprise healthcare payer customer.',
    status: 'planned',
    category: 'gtm',
    quarter: 'Q2 2026',
    startDate: '2026-04-01',
    targetDate: '2026-06-30',
    completionPercent: 0,
    owner: 'VP Sales',
    dependencies: ['M002', 'M003'],
    metrics: [
      { label: 'Contract Value', value: 'TBD', target: '$500K ARR' },
    ],
    tags: ['revenue', 'milestone'],
  },
  {
    id: 'M006',
    name: 'Real-Time Analytics Dashboard',
    description: 'Build executive dashboard with real-time quality measure analytics and population health insights.',
    status: 'planned',
    category: 'product',
    quarter: 'Q2 2026',
    startDate: '2026-05-01',
    targetDate: '2026-06-30',
    completionPercent: 0,
    owner: 'Product Manager',
    tags: ['feature', 'analytics'],
  },
  {
    id: 'M007',
    name: 'FHIR R5 Support',
    description: 'Add support for FHIR R5 specification alongside existing R4 support.',
    status: 'planned',
    category: 'product',
    quarter: 'Q3 2026',
    startDate: '2026-07-01',
    targetDate: '2026-09-30',
    completionPercent: 0,
    owner: 'Engineering Lead',
    tags: ['interoperability', 'fhir'],
  },
  {
    id: 'M008',
    name: 'Multi-Region Deployment',
    description: 'Deploy infrastructure across multiple AWS regions for disaster recovery and latency optimization.',
    status: 'planned',
    category: 'infrastructure',
    quarter: 'Q3 2026',
    startDate: '2026-08-01',
    targetDate: '2026-09-30',
    completionPercent: 0,
    owner: 'DevOps Lead',
    dependencies: ['M004'],
    tags: ['infrastructure', 'dr'],
  },
  {
    id: 'M009',
    name: 'AI/ML Care Gap Prediction',
    description: 'Implement machine learning model to predict and prioritize care gaps based on patient risk factors.',
    status: 'planned',
    category: 'product',
    quarter: 'Q4 2026',
    startDate: '2026-10-01',
    targetDate: '2026-12-31',
    completionPercent: 0,
    owner: 'Data Science Lead',
    dependencies: ['M006'],
    tags: ['ai', 'innovation'],
  },
  {
    id: 'M010',
    name: 'FDA Pre-Submission (if applicable)',
    description: 'Prepare and submit FDA pre-submission package if clinical decision support features require clearance.',
    status: 'planned',
    category: 'compliance',
    quarter: 'Q4 2026',
    startDate: '2026-10-15',
    targetDate: '2026-12-15',
    completionPercent: 0,
    owner: 'Regulatory Affairs',
    tags: ['regulatory', 'fda'],
  },
];

@Component({
  selector: 'app-roadmap',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="roadmap-page">
      <!-- Header Section -->
      <div class="page-header">
        <div class="header-content">
          <h1>Product Roadmap</h1>
          <p class="subtitle">HDIM Strategic Milestones & Execution Timeline</p>
        </div>
        <div class="header-stats">
          <div class="overall-progress">
            <div class="progress-label">
              <span>Overall Progress</span>
              <span class="progress-value">{{ stats().overallProgress }}%</span>
            </div>
            <div class="progress-bar-large">
              <div class="progress-fill" [style.width.%]="stats().overallProgress"></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Filter Chips -->
      <div class="filter-section">
        <div class="filter-group">
          <span class="filter-label">Category:</span>
          <div class="filter-chips">
            <button
              class="filter-chip"
              [class.active]="!activeFilters().categories?.length"
              (click)="clearCategoryFilter()"
            >
              All
            </button>
            @for (config of categoryConfigs; track config.category) {
              <button
                class="filter-chip"
                [class.active]="isCategoryActive(config.category)"
                [style.--chip-color]="config.color"
                (click)="toggleCategoryFilter(config.category)"
              >
                <span class="chip-dot" [style.background]="config.color"></span>
                {{ config.label }}
              </button>
            }
          </div>
        </div>
        <div class="filter-group">
          <span class="filter-label">Status:</span>
          <div class="filter-chips">
            <button
              class="filter-chip"
              [class.active]="!activeFilters().statuses?.length"
              (click)="clearStatusFilter()"
            >
              All
            </button>
            <button
              class="filter-chip"
              [class.active]="isStatusActive('planned')"
              (click)="toggleStatusFilter('planned')"
            >
              <span class="status-icon planned-icon"></span> Planned
            </button>
            <button
              class="filter-chip"
              [class.active]="isStatusActive('in_progress')"
              (click)="toggleStatusFilter('in_progress')"
            >
              <span class="status-icon progress-icon"></span> In Progress
            </button>
            <button
              class="filter-chip"
              [class.active]="isStatusActive('completed')"
              (click)="toggleStatusFilter('completed')"
            >
              <span class="status-icon completed-icon"></span> Completed
            </button>
            <button
              class="filter-chip"
              [class.active]="isStatusActive('blocked')"
              (click)="toggleStatusFilter('blocked')"
            >
              <span class="status-icon blocked-icon"></span> Blocked
            </button>
          </div>
        </div>
      </div>

      <!-- Statistics Cards Row -->
      <div class="stats-grid">
        <div class="stat-card total">
          <div class="stat-icon total-icon"></div>
          <div class="stat-content">
            <span class="stat-value">{{ stats().totalMilestones }}</span>
            <span class="stat-label">Total Milestones</span>
          </div>
        </div>
        <div class="stat-card completed">
          <div class="stat-icon completed-icon"></div>
          <div class="stat-content">
            <span class="stat-value">{{ stats().completedMilestones }}</span>
            <span class="stat-label">Completed</span>
          </div>
        </div>
        <div class="stat-card in-progress">
          <div class="stat-icon progress-icon"></div>
          <div class="stat-content">
            <span class="stat-value">{{ stats().inProgressMilestones }}</span>
            <span class="stat-label">In Progress</span>
          </div>
        </div>
        @if (stats().blockedMilestones > 0) {
          <div class="stat-card blocked">
            <div class="stat-icon blocked-icon"></div>
            <div class="stat-content">
              <span class="stat-value">{{ stats().blockedMilestones }}</span>
              <span class="stat-label">Blocked</span>
            </div>
          </div>
        }
        <div class="stat-card planned">
          <div class="stat-icon planned-icon"></div>
          <div class="stat-content">
            <span class="stat-value">{{ stats().plannedMilestones }}</span>
            <span class="stat-label">Planned</span>
          </div>
        </div>
      </div>

      <!-- Main Content: Timeline + Detail Panel -->
      <div class="main-content" [class.panel-open]="selectedMilestone()">
        <!-- Timeline View -->
        <div class="timeline-section">
          <!-- Quarter Headers -->
          <div class="quarter-headers">
            @for (quarter of quarters; track quarter) {
              <div class="quarter-header">
                <span class="quarter-name">{{ quarter }}</span>
                <span class="quarter-count">{{ getMilestoneCountByQuarter(quarter) }} milestones</span>
              </div>
            }
          </div>

          <!-- Timeline Grid -->
          <div class="timeline-grid">
            @for (quarter of quarters; track quarter) {
              <div class="quarter-column">
                @for (milestone of getMilestonesByQuarter(quarter); track milestone.id) {
                  <div
                    class="milestone-card"
                    [class.selected]="selectedMilestone()?.id === milestone.id"
                    [class.completed]="milestone.status === 'completed'"
                    [class.in-progress]="milestone.status === 'in_progress'"
                    [class.blocked]="milestone.status === 'blocked'"
                    (click)="selectMilestone(milestone)"
                  >
                    <div class="milestone-header">
                      <span class="milestone-status-icon" [class]="milestone.status + '-icon'"></span>
                      <span
                        class="milestone-category"
                        [style.background]="getCategoryColor(milestone.category)"
                      >
                        {{ getCategoryLabel(milestone.category) }}
                      </span>
                    </div>
                    <h4 class="milestone-name">{{ milestone.name }}</h4>
                    @if (milestone.status === 'in_progress') {
                      <div class="milestone-progress">
                        <div class="progress-bar-small">
                          <div
                            class="progress-fill"
                            [style.width.%]="milestone.completionPercent"
                          ></div>
                        </div>
                        <span class="progress-text">{{ milestone.completionPercent }}%</span>
                      </div>
                    }
                    <div class="milestone-footer">
                      <span class="milestone-date">
                        <span class="date-icon"></span>
                        {{ formatDate(milestone.targetDate) }}
                      </span>
                      @if (milestone.dependencies?.length) {
                        <span class="dependency-badge" title="Has dependencies">
                          {{ milestone.dependencies.length }} dep
                        </span>
                      }
                    </div>
                  </div>
                }
                @if (getMilestonesByQuarter(quarter).length === 0) {
                  <div class="empty-quarter">
                    <span class="empty-icon"></span>
                    <span>No milestones</span>
                  </div>
                }
              </div>
            }
          </div>
        </div>

        <!-- Detail Panel (Right Sidebar) -->
        @if (selectedMilestone(); as milestone) {
          <div class="detail-panel">
            <div class="panel-header">
              <h3>Milestone Details</h3>
              <button class="close-btn" (click)="closeMilestonePanel()" aria-label="Close panel">X</button>
            </div>

            <div class="panel-content">
              <!-- Status Badge -->
              <div class="detail-status">
                <span class="status-badge" [class]="milestone.status">
                  {{ formatStatus(milestone.status) }}
                </span>
                <span
                  class="category-badge"
                  [style.background]="getCategoryColor(milestone.category)"
                >
                  {{ getCategoryLabel(milestone.category) }}
                </span>
              </div>

              <!-- Title & Description -->
              <h2 class="detail-title">{{ milestone.name }}</h2>
              <p class="detail-description">{{ milestone.description }}</p>

              <!-- Progress (if in progress) -->
              @if (milestone.status === 'in_progress') {
                <div class="detail-progress">
                  <div class="progress-header">
                    <span>Progress</span>
                    <span class="progress-percent">{{ milestone.completionPercent }}%</span>
                  </div>
                  <div class="progress-bar-detail">
                    <div
                      class="progress-fill"
                      [style.width.%]="milestone.completionPercent"
                    ></div>
                  </div>
                </div>
              }

              <!-- Key Info -->
              <div class="detail-info-grid">
                <div class="info-item">
                  <span class="info-label">Owner</span>
                  <span class="info-value">{{ milestone.owner }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">Quarter</span>
                  <span class="info-value">{{ milestone.quarter }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">Start Date</span>
                  <span class="info-value">{{ formatDate(milestone.startDate) }}</span>
                </div>
                <div class="info-item">
                  <span class="info-label">Target Date</span>
                  <span class="info-value">{{ formatDate(milestone.targetDate) }}</span>
                </div>
                @if (milestone.completedDate) {
                  <div class="info-item completed-date">
                    <span class="info-label">Completed</span>
                    <span class="info-value success">{{ formatDate(milestone.completedDate) }}</span>
                  </div>
                }
              </div>

              <!-- Dependencies -->
              @if (milestone.dependencies?.length) {
                <div class="detail-section">
                  <h4>Dependencies</h4>
                  <div class="dependencies-list">
                    @for (depId of milestone.dependencies; track depId) {
                      <div
                        class="dependency-item"
                        [class.resolved]="isDependencyResolved(depId)"
                        (click)="selectMilestoneById(depId)"
                      >
                        <span class="dep-status" [class.resolved]="isDependencyResolved(depId)"></span>
                        <span class="dep-name">{{ getMilestoneName(depId) }}</span>
                      </div>
                    }
                  </div>
                </div>
              }

              <!-- Metrics -->
              @if (milestone.metrics?.length) {
                <div class="detail-section">
                  <h4>Metrics</h4>
                  <div class="metrics-list">
                    @for (metric of milestone.metrics; track metric.label) {
                      <div class="metric-item">
                        <span class="metric-label">{{ metric.label }}</span>
                        <div class="metric-values">
                          <span class="metric-current">{{ metric.value }}</span>
                          @if (metric.target) {
                            <span class="metric-target">/ {{ metric.target }}</span>
                          }
                        </div>
                      </div>
                    }
                  </div>
                </div>
              }

              <!-- Tags -->
              @if (milestone.tags?.length) {
                <div class="detail-section">
                  <h4>Tags</h4>
                  <div class="tags-list">
                    @for (tag of milestone.tags; track tag) {
                      <span class="tag">{{ tag }}</span>
                    }
                  </div>
                </div>
              }

              <!-- Status Update Buttons -->
              <div class="detail-actions">
                <h4>Update Status</h4>
                <div class="status-buttons">
                  <button
                    class="status-btn planned"
                    [class.active]="milestone.status === 'planned'"
                    (click)="updateMilestoneStatus(milestone.id, 'planned')"
                  >
                    Planned
                  </button>
                  <button
                    class="status-btn in-progress"
                    [class.active]="milestone.status === 'in_progress'"
                    (click)="updateMilestoneStatus(milestone.id, 'in_progress')"
                  >
                    In Progress
                  </button>
                  <button
                    class="status-btn completed"
                    [class.active]="milestone.status === 'completed'"
                    (click)="updateMilestoneStatus(milestone.id, 'completed')"
                  >
                    Completed
                  </button>
                  <button
                    class="status-btn blocked"
                    [class.active]="milestone.status === 'blocked'"
                    (click)="updateMilestoneStatus(milestone.id, 'blocked')"
                  >
                    Blocked
                  </button>
                </div>
              </div>
            </div>
          </div>
        }
      </div>

      <!-- Category Progress Cards -->
      <div class="category-progress-section">
        <h3>Progress by Category</h3>
        <div class="category-cards">
          @for (config of categoryConfigs; track config.category) {
            <div class="category-card" [style.--card-color]="config.color">
              <div class="category-header">
                <span class="category-icon" [style.background]="config.color"></span>
                <span class="category-name">{{ config.label }}</span>
              </div>
              <div class="category-stats">
                <span class="category-count">
                  {{ getCategoryCompleted(config.category) }}/{{ getCategoryTotal(config.category) }}
                </span>
                <span class="category-label">completed</span>
              </div>
              <div class="category-progress-bar">
                <div
                  class="progress-fill"
                  [style.width.%]="getCategoryProgress(config.category)"
                  [style.background]="config.color"
                ></div>
              </div>
            </div>
          }
        </div>
      </div>

      <!-- Loading State -->
      @if (isLoading()) {
        <div class="loading-overlay">
          <div class="spinner"></div>
          <span>Loading roadmap...</span>
        </div>
      }
    </div>
  `,
  styles: [`
    .roadmap-page {
      max-width: 1600px;
      margin: 0 auto;
      padding: 24px;
    }

    /* Header Section */
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 24px;
      flex-wrap: wrap;
      gap: 24px;
    }

    .header-content h1 {
      margin: 0;
      color: #1a237e;
      font-size: 32px;
      font-weight: 700;
    }

    .subtitle {
      margin: 4px 0 0 0;
      color: #666;
      font-size: 14px;
    }

    .header-stats {
      flex: 1;
      max-width: 400px;
      min-width: 280px;
    }

    .overall-progress {
      background: white;
      border-radius: 12px;
      padding: 16px 20px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .progress-label {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      font-size: 14px;
      color: #666;
    }

    .progress-value {
      font-size: 24px;
      font-weight: 700;
      color: #1a237e;
    }

    .progress-bar-large {
      height: 12px;
      background: #e0e0e0;
      border-radius: 6px;
      overflow: hidden;
    }

    .progress-bar-large .progress-fill {
      height: 100%;
      background: linear-gradient(90deg, #1a237e, #3949ab);
      border-radius: 6px;
      transition: width 0.3s ease;
    }

    /* Filter Section */
    .filter-section {
      background: white;
      border-radius: 12px;
      padding: 16px 20px;
      margin-bottom: 24px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .filter-group {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }

    .filter-label {
      font-size: 13px;
      color: #666;
      font-weight: 600;
      min-width: 70px;
    }

    .filter-chips {
      display: flex;
      gap: 8px;
      flex-wrap: wrap;
    }

    .filter-chip {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 6px 12px;
      background: #f5f5f5;
      border: 1px solid #e0e0e0;
      border-radius: 20px;
      font-size: 13px;
      cursor: pointer;
      transition: all 0.2s;
    }

    .filter-chip:hover {
      background: #eeeeee;
      border-color: #bdbdbd;
    }

    .filter-chip.active {
      background: #1a237e;
      color: white;
      border-color: #1a237e;
    }

    .chip-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
    }

    .status-icon {
      width: 14px;
      height: 14px;
      display: inline-block;
    }

    .planned-icon::before { content: "[ ]"; font-size: 10px; }
    .progress-icon::before { content: "~"; font-size: 14px; }
    .completed-icon::before { content: "[x]"; font-size: 10px; color: #4caf50; }
    .blocked-icon::before { content: "[!]"; font-size: 10px; color: #f44336; }
    .total-icon::before { content: "#"; font-size: 16px; }

    /* Statistics Cards */
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
      gap: 16px;
      margin-bottom: 24px;
    }

    .stat-card {
      background: white;
      border-radius: 12px;
      padding: 20px;
      display: flex;
      align-items: center;
      gap: 16px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      border-left: 4px solid #e0e0e0;
    }

    .stat-card.total { border-left-color: #1a237e; }
    .stat-card.completed { border-left-color: #4caf50; }
    .stat-card.in-progress { border-left-color: #2196f3; }
    .stat-card.blocked { border-left-color: #f44336; }
    .stat-card.planned { border-left-color: #9e9e9e; }

    .stat-icon {
      font-size: 28px;
    }

    .stat-content {
      display: flex;
      flex-direction: column;
    }

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: #1a237e;
    }

    .stat-label {
      font-size: 13px;
      color: #666;
    }

    /* Main Content Layout */
    .main-content {
      display: flex;
      gap: 24px;
      margin-bottom: 24px;
    }

    .main-content.panel-open .timeline-section {
      flex: 1;
    }

    /* Timeline Section */
    .timeline-section {
      flex: 1;
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      overflow: hidden;
    }

    .quarter-headers {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      background: #f5f7fa;
      border-bottom: 1px solid #e0e0e0;
    }

    .quarter-header {
      padding: 16px 20px;
      text-align: center;
      border-right: 1px solid #e0e0e0;
    }

    .quarter-header:last-child {
      border-right: none;
    }

    .quarter-name {
      display: block;
      font-size: 16px;
      font-weight: 700;
      color: #1a237e;
    }

    .quarter-count {
      display: block;
      font-size: 12px;
      color: #666;
      margin-top: 2px;
    }

    .timeline-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      min-height: 400px;
    }

    .quarter-column {
      padding: 16px;
      border-right: 1px solid #f0f0f0;
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .quarter-column:last-child {
      border-right: none;
    }

    /* Milestone Card */
    .milestone-card {
      background: #f9fafb;
      border-radius: 8px;
      padding: 14px;
      cursor: pointer;
      transition: all 0.2s;
      border: 2px solid transparent;
    }

    .milestone-card:hover {
      background: #f3f4f6;
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    }

    .milestone-card.selected {
      border-color: #1a237e;
      background: #e8eaf6;
    }

    .milestone-card.completed {
      border-left: 3px solid #4caf50;
    }

    .milestone-card.in-progress {
      border-left: 3px solid #2196f3;
    }

    .milestone-card.blocked {
      border-left: 3px solid #f44336;
      background: #fff5f5;
    }

    .milestone-header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
    }

    .milestone-status-icon {
      font-size: 14px;
    }

    .milestone-category {
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 10px;
      font-weight: 600;
      color: white;
      text-transform: uppercase;
    }

    .milestone-name {
      margin: 0 0 8px 0;
      font-size: 14px;
      font-weight: 600;
      color: #333;
      line-height: 1.3;
    }

    .milestone-progress {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
    }

    .progress-bar-small {
      flex: 1;
      height: 6px;
      background: #e0e0e0;
      border-radius: 3px;
      overflow: hidden;
    }

    .progress-bar-small .progress-fill {
      height: 100%;
      background: #2196f3;
      border-radius: 3px;
    }

    .progress-text {
      font-size: 12px;
      font-weight: 600;
      color: #2196f3;
    }

    .milestone-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .milestone-date {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: #666;
    }

    .date-icon::before {
      content: ">";
      font-size: 10px;
    }

    .dependency-badge {
      font-size: 11px;
      color: #9c27b0;
      background: #f3e5f5;
      padding: 2px 6px;
      border-radius: 4px;
    }

    .empty-quarter {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 40px 20px;
      color: #999;
      font-size: 13px;
    }

    .empty-icon::before {
      content: "[ ]";
      font-size: 24px;
      margin-bottom: 8px;
      opacity: 0.5;
    }

    /* Detail Panel */
    .detail-panel {
      width: 400px;
      background: white;
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      overflow: hidden;
      flex-shrink: 0;
    }

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 20px;
      background: #f5f7fa;
      border-bottom: 1px solid #e0e0e0;
    }

    .panel-header h3 {
      margin: 0;
      font-size: 16px;
      color: #333;
    }

    .close-btn {
      width: 32px;
      height: 32px;
      border: none;
      background: transparent;
      font-size: 18px;
      cursor: pointer;
      border-radius: 4px;
      color: #666;
    }

    .close-btn:hover {
      background: #e0e0e0;
    }

    .panel-content {
      padding: 20px;
      max-height: calc(100vh - 300px);
      overflow-y: auto;
    }

    .detail-status {
      display: flex;
      gap: 8px;
      margin-bottom: 16px;
    }

    .status-badge {
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
    }

    .status-badge.planned { background: #f5f5f5; color: #666; }
    .status-badge.in_progress { background: #e3f2fd; color: #1565c0; }
    .status-badge.completed { background: #e8f5e9; color: #2e7d32; }
    .status-badge.blocked { background: #ffebee; color: #c62828; }

    .category-badge {
      padding: 4px 12px;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
      color: white;
    }

    .detail-title {
      margin: 0 0 12px 0;
      font-size: 20px;
      font-weight: 700;
      color: #1a237e;
    }

    .detail-description {
      margin: 0 0 20px 0;
      font-size: 14px;
      color: #666;
      line-height: 1.6;
    }

    .detail-progress {
      margin-bottom: 20px;
      padding: 16px;
      background: #f5f7fa;
      border-radius: 8px;
    }

    .progress-header {
      display: flex;
      justify-content: space-between;
      margin-bottom: 8px;
      font-size: 13px;
      color: #666;
    }

    .progress-percent {
      font-weight: 700;
      color: #2196f3;
    }

    .progress-bar-detail {
      height: 10px;
      background: #e0e0e0;
      border-radius: 5px;
      overflow: hidden;
    }

    .progress-bar-detail .progress-fill {
      height: 100%;
      background: #2196f3;
      border-radius: 5px;
    }

    .detail-info-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 12px;
      margin-bottom: 20px;
    }

    .info-item {
      display: flex;
      flex-direction: column;
    }

    .info-label {
      font-size: 11px;
      color: #999;
      text-transform: uppercase;
      margin-bottom: 2px;
    }

    .info-value {
      font-size: 14px;
      font-weight: 600;
      color: #333;
    }

    .info-value.success {
      color: #2e7d32;
    }

    .detail-section {
      margin-bottom: 20px;
    }

    .detail-section h4 {
      margin: 0 0 12px 0;
      font-size: 14px;
      color: #333;
      font-weight: 600;
    }

    .dependencies-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .dependency-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 12px;
      background: #f5f5f5;
      border-radius: 6px;
      cursor: pointer;
      transition: background 0.2s;
    }

    .dependency-item:hover {
      background: #eeeeee;
    }

    .dependency-item.resolved {
      background: #e8f5e9;
    }

    .dep-status {
      width: 16px;
      height: 16px;
      border: 2px solid #999;
      border-radius: 50%;
    }

    .dep-status.resolved {
      background: #4caf50;
      border-color: #4caf50;
    }

    .dep-name {
      font-size: 13px;
      color: #333;
    }

    .metrics-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .metric-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 12px;
      background: #f5f5f5;
      border-radius: 6px;
    }

    .metric-label {
      font-size: 13px;
      color: #666;
    }

    .metric-values {
      display: flex;
      align-items: baseline;
      gap: 4px;
    }

    .metric-current {
      font-size: 16px;
      font-weight: 700;
      color: #1a237e;
    }

    .metric-target {
      font-size: 12px;
      color: #999;
    }

    .tags-list {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }

    .tag {
      padding: 4px 10px;
      background: #e3f2fd;
      color: #1565c0;
      border-radius: 4px;
      font-size: 12px;
    }

    .detail-actions {
      padding-top: 20px;
      border-top: 1px solid #e0e0e0;
    }

    .detail-actions h4 {
      margin: 0 0 12px 0;
      font-size: 14px;
      color: #333;
    }

    .status-buttons {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 8px;
    }

    .status-btn {
      padding: 10px 12px;
      border: 1px solid #e0e0e0;
      border-radius: 6px;
      background: white;
      font-size: 13px;
      cursor: pointer;
      transition: all 0.2s;
    }

    .status-btn:hover {
      background: #f5f5f5;
    }

    .status-btn.active {
      border-width: 2px;
    }

    .status-btn.planned.active {
      border-color: #9e9e9e;
      background: #f5f5f5;
    }

    .status-btn.in-progress.active {
      border-color: #2196f3;
      background: #e3f2fd;
    }

    .status-btn.completed.active {
      border-color: #4caf50;
      background: #e8f5e9;
    }

    .status-btn.blocked.active {
      border-color: #f44336;
      background: #ffebee;
    }

    /* Category Progress Section */
    .category-progress-section {
      background: white;
      border-radius: 12px;
      padding: 24px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    }

    .category-progress-section h3 {
      margin: 0 0 20px 0;
      font-size: 18px;
      color: #333;
    }

    .category-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
      gap: 16px;
    }

    .category-card {
      padding: 20px;
      background: #f9fafb;
      border-radius: 8px;
      border-left: 4px solid var(--card-color, #ccc);
    }

    .category-header {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 12px;
    }

    .category-icon {
      width: 32px;
      height: 32px;
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 16px;
      color: white;
    }

    .category-name {
      font-size: 15px;
      font-weight: 600;
      color: #333;
    }

    .category-stats {
      margin-bottom: 8px;
    }

    .category-count {
      font-size: 24px;
      font-weight: 700;
      color: #1a237e;
    }

    .category-label {
      font-size: 13px;
      color: #666;
      margin-left: 4px;
    }

    .category-progress-bar {
      height: 8px;
      background: #e0e0e0;
      border-radius: 4px;
      overflow: hidden;
    }

    .category-progress-bar .progress-fill {
      height: 100%;
      border-radius: 4px;
      transition: width 0.3s ease;
    }

    /* Loading State */
    .loading-overlay {
      position: fixed;
      inset: 0;
      background: rgba(255, 255, 255, 0.9);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }

    .spinner {
      width: 48px;
      height: 48px;
      border: 4px solid #e0e0e0;
      border-top-color: #1a237e;
      border-radius: 50%;
      animation: spin 1s linear infinite;
      margin-bottom: 16px;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    /* Responsive */
    @media (max-width: 1200px) {
      .detail-panel {
        width: 350px;
      }
    }

    @media (max-width: 1024px) {
      .timeline-grid,
      .quarter-headers {
        grid-template-columns: repeat(2, 1fr);
      }

      .main-content {
        flex-direction: column;
      }

      .detail-panel {
        width: 100%;
      }
    }

    @media (max-width: 768px) {
      .timeline-grid,
      .quarter-headers {
        grid-template-columns: 1fr;
      }

      .stats-grid {
        grid-template-columns: repeat(2, 1fr);
      }

      .status-buttons {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class RoadmapComponent implements OnInit {
  // Category configurations
  categoryConfigs = CATEGORY_CONFIGS;
  quarters: Quarter[] = ['Q1 2026', 'Q2 2026', 'Q3 2026', 'Q4 2026'];

  // State signals
  milestones = signal<RoadmapMilestone[]>([]);
  selectedMilestone = signal<RoadmapMilestone | null>(null);
  activeFilters = signal<RoadmapFilter>({});
  isLoading = signal(false);

  // Computed stats
  stats = computed<RoadmapStats>(() => {
    const all = this.milestones();
    const completed = all.filter(m => m.status === 'completed');
    const inProgress = all.filter(m => m.status === 'in_progress');
    const planned = all.filter(m => m.status === 'planned');
    const blocked = all.filter(m => m.status === 'blocked');

    const totalProgress = all.length > 0
      ? Math.round(all.reduce((sum, m) => sum + m.completionPercent, 0) / all.length)
      : 0;

    const byCategory: { category: MilestoneCategory; total: number; completed: number; progress: number }[] =
      (['gtm', 'product', 'infrastructure', 'compliance'] as MilestoneCategory[]).map(cat => {
        const catMilestones = all.filter(m => m.category === cat);
        const catCompleted = catMilestones.filter(m => m.status === 'completed').length;
        return {
          category: cat,
          total: catMilestones.length,
          completed: catCompleted,
          progress: catMilestones.length > 0 ? Math.round((catCompleted / catMilestones.length) * 100) : 0
        };
      });

    const byQuarter: { quarter: Quarter; total: number; completed: number; progress: number }[] =
      this.quarters.map(q => {
        const qMilestones = all.filter(m => m.quarter === q);
        const qCompleted = qMilestones.filter(m => m.status === 'completed').length;
        return {
          quarter: q,
          total: qMilestones.length,
          completed: qCompleted,
          progress: qMilestones.length > 0 ? Math.round((qCompleted / qMilestones.length) * 100) : 0
        };
      });

    return {
      totalMilestones: all.length,
      completedMilestones: completed.length,
      inProgressMilestones: inProgress.length,
      plannedMilestones: planned.length,
      blockedMilestones: blocked.length,
      overallProgress: totalProgress,
      byCategory,
      byQuarter
    };
  });

  // Filtered milestones
  filteredMilestones = computed(() => {
    const all = this.milestones();
    const filters = this.activeFilters();

    return all.filter(m => {
      if (filters.categories?.length && !filters.categories.includes(m.category)) {
        return false;
      }
      if (filters.statuses?.length && !filters.statuses.includes(m.status)) {
        return false;
      }
      if (filters.quarters?.length && !filters.quarters.includes(m.quarter)) {
        return false;
      }
      return true;
    });
  });

  ngOnInit(): void {
    // Load mock data - will be replaced with RoadmapService call
    this.loadMilestones();
  }

  private loadMilestones(): void {
    this.isLoading.set(true);
    // Simulate API call
    setTimeout(() => {
      this.milestones.set(MOCK_MILESTONES);
      this.isLoading.set(false);
    }, 500);
  }

  // Filtering methods
  toggleCategoryFilter(category: MilestoneCategory): void {
    const current = this.activeFilters();
    const categories = current.categories || [];
    const index = categories.indexOf(category);

    if (index > -1) {
      categories.splice(index, 1);
    } else {
      categories.push(category);
    }

    this.activeFilters.set({ ...current, categories: categories.length ? categories : undefined });
  }

  clearCategoryFilter(): void {
    const current = this.activeFilters();
    this.activeFilters.set({ ...current, categories: undefined });
  }

  isCategoryActive(category: MilestoneCategory): boolean {
    return this.activeFilters().categories?.includes(category) || false;
  }

  toggleStatusFilter(status: MilestoneStatus): void {
    const current = this.activeFilters();
    const statuses = current.statuses || [];
    const index = statuses.indexOf(status);

    if (index > -1) {
      statuses.splice(index, 1);
    } else {
      statuses.push(status);
    }

    this.activeFilters.set({ ...current, statuses: statuses.length ? statuses : undefined });
  }

  clearStatusFilter(): void {
    const current = this.activeFilters();
    this.activeFilters.set({ ...current, statuses: undefined });
  }

  isStatusActive(status: MilestoneStatus): boolean {
    return this.activeFilters().statuses?.includes(status) || false;
  }

  // Milestone selection
  selectMilestone(milestone: RoadmapMilestone): void {
    this.selectedMilestone.set(milestone);
  }

  selectMilestoneById(id: string): void {
    const milestone = this.milestones().find(m => m.id === id);
    if (milestone) {
      this.selectedMilestone.set(milestone);
    }
  }

  closeMilestonePanel(): void {
    this.selectedMilestone.set(null);
  }

  // Data retrieval methods
  getMilestonesByQuarter(quarter: Quarter): RoadmapMilestone[] {
    return this.filteredMilestones().filter(m => m.quarter === quarter);
  }

  getMilestoneCountByQuarter(quarter: Quarter): number {
    return this.filteredMilestones().filter(m => m.quarter === quarter).length;
  }

  getMilestoneName(id: string): string {
    const milestone = this.milestones().find(m => m.id === id);
    return milestone?.name || 'Unknown';
  }

  isDependencyResolved(id: string): boolean {
    const milestone = this.milestones().find(m => m.id === id);
    return milestone?.status === 'completed';
  }

  // Category helpers
  getCategoryColor(category: MilestoneCategory): string {
    const config = this.categoryConfigs.find(c => c.category === category);
    return config?.color || '#999';
  }

  getCategoryLabel(category: MilestoneCategory): string {
    const config = this.categoryConfigs.find(c => c.category === category);
    return config?.label || category;
  }

  getCategoryTotal(category: MilestoneCategory): number {
    return this.milestones().filter(m => m.category === category).length;
  }

  getCategoryCompleted(category: MilestoneCategory): number {
    return this.milestones().filter(m => m.category === category && m.status === 'completed').length;
  }

  getCategoryProgress(category: MilestoneCategory): number {
    const total = this.getCategoryTotal(category);
    const completed = this.getCategoryCompleted(category);
    return total > 0 ? Math.round((completed / total) * 100) : 0;
  }

  // Status helpers
  formatStatus(status: MilestoneStatus): string {
    const labels: Record<MilestoneStatus, string> = {
      planned: 'Planned',
      in_progress: 'In Progress',
      completed: 'Completed',
      blocked: 'Blocked'
    };
    return labels[status];
  }

  // Date formatting
  formatDate(dateString: string): string {
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  }

  // Status update
  updateMilestoneStatus(id: string, status: MilestoneStatus): void {
    const milestones = this.milestones();
    const index = milestones.findIndex(m => m.id === id);

    if (index > -1) {
      const updated = [...milestones];
      updated[index] = {
        ...updated[index],
        status,
        completionPercent: status === 'completed' ? 100 : updated[index].completionPercent,
        completedDate: status === 'completed' ? new Date().toISOString().split('T')[0] : undefined
      };
      this.milestones.set(updated);

      // Update selected milestone if it's the same
      if (this.selectedMilestone()?.id === id) {
        this.selectedMilestone.set(updated[index]);
      }
    }
  }
}
