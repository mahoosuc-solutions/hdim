#!/usr/bin/env ts-node
/**
 * UI Implementation Validation System
 * Validates Angular UI implementations against customer success criteria,
 * industry best practices, and HIPAA compliance requirements.
 *
 * Usage: npx ts-node scripts/validate-ui-implementation.ts [--report] [--json]
 */

import * as fs from 'fs';
import * as path from 'path';
import { glob } from 'glob';

// ============================================================================
// VALIDATION CRITERIA DEFINITIONS
// ============================================================================

interface ValidationCriterion {
  id: string;
  category: string;
  name: string;
  description: string;
  weight: number; // 1-10 (importance)
  validator: (context: ValidationContext) => ValidationResult;
}

interface ValidationContext {
  componentFiles: string[];
  serviceFiles: string[];
  moduleFiles: string[];
  templateFiles: string[];
  styleFiles: string[];
  featureList: Record<string, boolean>;
  codebase: Map<string, string>;
}

interface ValidationResult {
  passed: boolean;
  score: number; // 0-100
  findings: string[];
  recommendations: string[];
  criticalIssues: string[];
}

interface CategoryScore {
  category: string;
  totalWeight: number;
  achievedScore: number;
  maxScore: number;
  percentage: number;
  grade: string;
  criteria: Array<{
    id: string;
    name: string;
    passed: boolean;
    score: number;
    weight: number;
  }>;
}

// ============================================================================
// CUSTOMER SUCCESS CRITERIA
// ============================================================================

const customerSuccessCriteria: ValidationCriterion[] = [
  {
    id: 'CS-001',
    category: 'Customer Success',
    name: 'Care Gap Management UI',
    description: 'Complete UI for identifying, prioritizing, and closing care gaps',
    weight: 10,
    validator: (ctx) => {
      const careGapComponent = ctx.componentFiles.find(f => f.includes('care-gap'));
      const hasFilters = ctx.codebase.get(careGapComponent || '')?.includes('filter') || false;
      const hasPrioritization = ctx.codebase.get(careGapComponent || '')?.includes('priority') || false;

      const passed = !!careGapComponent && hasFilters && hasPrioritization;
      return {
        passed,
        score: passed ? 100 : 50,
        findings: [
          careGapComponent ? '✓ Care gap component exists' : '✗ Care gap component missing',
          hasFilters ? '✓ Filtering capability present' : '✗ Filtering missing',
          hasPrioritization ? '✓ Prioritization logic present' : '✗ Prioritization missing'
        ],
        recommendations: passed ? [] : [
          'Implement care gap filtering by measure, urgency, and patient',
          'Add priority scoring visualization',
          'Include bulk action capabilities for care teams'
        ],
        criticalIssues: passed ? [] : ['Care gap management is a core customer requirement']
      };
    }
  },
  {
    id: 'CS-002',
    category: 'Customer Success',
    name: 'Patient Health Overview Dashboard',
    description: '360-degree patient view with comprehensive health metrics',
    weight: 10,
    validator: (ctx) => {
      const patientHealthComponent = ctx.componentFiles.find(f =>
        f.includes('patient-health-overview') || f.includes('patient-detail')
      );
      const hasTimeline = ctx.codebase.get(patientHealthComponent || '')?.includes('timeline') || false;
      const hasRiskScore = ctx.codebase.get(patientHealthComponent || '')?.includes('risk') || false;

      const passed = !!patientHealthComponent && (hasTimeline || hasRiskScore);
      return {
        passed,
        score: passed ? 90 : 40,
        findings: [
          patientHealthComponent ? '✓ Patient health overview exists' : '✗ Patient health overview missing',
          hasTimeline ? '✓ Timeline view implemented' : '• Timeline view could enhance UX',
          hasRiskScore ? '✓ Risk stratification present' : '• Risk scoring missing'
        ],
        recommendations: passed ? [
          'Consider adding social determinants of health (SDOH) panel',
          'Integrate mental health screening results (PHQ-9, GAD-7)'
        ] : [
          'Implement comprehensive patient dashboard per PRODUCT_FEATURES.md',
          'Add 360° health view with chronic conditions, medications, allergies'
        ],
        criticalIssues: passed ? [] : ['Patient health overview is essential for clinical workflows']
      };
    }
  },
  {
    id: 'CS-003',
    category: 'Customer Success',
    name: 'Quality Measure Calculation UI',
    description: 'Interface for running and viewing HEDIS measure evaluations',
    weight: 9,
    validator: (ctx) => {
      const evaluationComponent = ctx.componentFiles.find(f => f.includes('evaluation'));
      const measureComponent = ctx.componentFiles.find(f => f.includes('measure'));
      const hasResults = ctx.componentFiles.some(f => f.includes('result'));

      const passed = !!(evaluationComponent && hasResults);
      return {
        passed,
        score: passed ? 95 : 30,
        findings: [
          evaluationComponent ? '✓ Evaluation UI exists' : '✗ Evaluation UI missing',
          hasResults ? '✓ Results display implemented' : '✗ Results display missing',
          measureComponent ? '✓ Measure builder present' : '• Measure builder could enable custom measures'
        ],
        recommendations: passed ? [
          'Add batch evaluation progress tracking',
          'Implement real-time WebSocket updates for long-running evaluations'
        ] : [
          'Create UI for measure selection and patient cohort definition',
          'Display evaluation results with drill-down capabilities'
        ],
        criticalIssues: passed ? [] : ['Quality measure evaluation is the core platform value proposition']
      };
    }
  },
  {
    id: 'CS-004',
    category: 'Customer Success',
    name: 'Role-Based Dashboards',
    description: 'Dedicated dashboards for Medical Assistant, RN, and Provider roles',
    weight: 8,
    validator: (ctx) => {
      const maDashboard = ctx.componentFiles.find(f => f.includes('ma-dashboard'));
      const rnDashboard = ctx.componentFiles.find(f => f.includes('rn-dashboard'));
      const providerDashboard = ctx.componentFiles.find(f => f.includes('provider-dashboard'));

      const count = [maDashboard, rnDashboard, providerDashboard].filter(Boolean).length;
      const passed = count >= 2;

      return {
        passed,
        score: (count / 3) * 100,
        findings: [
          maDashboard ? '✓ Medical Assistant dashboard exists' : '✗ MA dashboard missing',
          rnDashboard ? '✓ Registered Nurse dashboard exists' : '✗ RN dashboard missing',
          providerDashboard ? '✓ Provider dashboard exists' : '✗ Provider dashboard missing'
        ],
        recommendations: count < 3 ? [
          'Implement all role-specific dashboards to optimize workflows',
          'Tailor KPIs and actions to each role\'s responsibilities'
        ] : [
          'Ensure dashboards show role-appropriate metrics',
          'Add quick-action buttons for common tasks'
        ],
        criticalIssues: count === 0 ? ['Role-based workflows are essential for user adoption'] : []
      };
    }
  },
  {
    id: 'CS-005',
    category: 'Customer Success',
    name: 'Care Recommendations & Interventions',
    description: 'Actionable care recommendations with outcome tracking',
    weight: 7,
    validator: (ctx) => {
      const careRecComponent = ctx.componentFiles.find(f => f.includes('care-recommendation'));
      const hasBulkActions = ctx.componentFiles.some(f => f.includes('bulk-action'));
      const hasStats = ctx.componentFiles.some(f => f.includes('stats') || f.includes('metric'));

      const passed = !!(careRecComponent && hasBulkActions);
      return {
        passed,
        score: passed ? 85 : 45,
        findings: [
          careRecComponent ? '✓ Care recommendations UI exists' : '✗ Care recommendations missing',
          hasBulkActions ? '✓ Bulk action toolbar present' : '✗ Bulk actions missing',
          hasStats ? '✓ Statistics panel implemented' : '• Stats panel would improve visibility'
        ],
        recommendations: passed ? [
          'Add ML-powered closure probability predictions',
          'Integrate appointment scheduling for recommended interventions'
        ] : [
          'Build care recommendation list with filtering and prioritization',
          'Enable care team assignment and outcome tracking'
        ],
        criticalIssues: passed ? [] : ['Care coordination features drive gap closure rates']
      };
    }
  }
];

// ============================================================================
// INDUSTRY BEST PRACTICES CRITERIA
// ============================================================================

const industryBestPractices: ValidationCriterion[] = [
  {
    id: 'BP-001',
    category: 'Best Practices',
    name: 'Component Architecture',
    description: 'Proper separation of concerns with smart/dumb components',
    weight: 6,
    validator: (ctx) => {
      const totalComponents = ctx.componentFiles.length;
      const hasServices = ctx.serviceFiles.length > 0;
      const avgLinesPerComponent = 150; // Estimate

      // Check for proper module structure
      const hasSharedModule = ctx.moduleFiles.some(f => f.includes('shared'));
      const hasCoreModule = ctx.moduleFiles.some(f => f.includes('core'));

      const score = (hasServices ? 40 : 0) + (hasSharedModule ? 30 : 0) + (hasCoreModule ? 30 : 0);
      const passed = score >= 70;

      return {
        passed,
        score,
        findings: [
          hasServices ? '✓ Services layer exists' : '✗ Services layer missing',
          hasSharedModule ? '✓ Shared module for reusable components' : '• Consider shared module for DRY principle',
          hasCoreModule ? '✓ Core module for singletons' : '• Core module recommended for app-wide services'
        ],
        recommendations: [
          'Follow Angular style guide for component organization',
          'Keep components under 300 lines for maintainability',
          'Use OnPush change detection for performance'
        ],
        criticalIssues: passed ? [] : ['Poor architecture increases technical debt']
      };
    }
  },
  {
    id: 'BP-002',
    category: 'Best Practices',
    name: 'Responsive Design',
    description: 'Mobile-friendly, responsive layouts for all screen sizes',
    weight: 7,
    validator: (ctx) => {
      const hasResponsiveStyles = ctx.styleFiles.some(f => {
        const content = ctx.codebase.get(f) || '';
        return content.includes('@media') || content.includes('flex');
      });

      const usesMaterialGrid = Array.from(ctx.codebase.values()).some(content =>
        content.includes('fxLayout') || content.includes('mat-grid')
      );

      const passed = hasResponsiveStyles || usesMaterialGrid;
      return {
        passed,
        score: passed ? 80 : 35,
        findings: [
          hasResponsiveStyles ? '✓ Responsive CSS media queries found' : '✗ No responsive styles detected',
          usesMaterialGrid ? '✓ Angular Material flex layout in use' : '• Consider Angular Material Grid for responsive design'
        ],
        recommendations: [
          'Test on mobile devices (phones, tablets)',
          'Ensure touch-friendly hit targets (min 44x44px)',
          'Use responsive breakpoints: xs, sm, md, lg, xl'
        ],
        criticalIssues: passed ? [] : ['Healthcare workers often use tablets at point-of-care']
      };
    }
  },
  {
    id: 'BP-003',
    category: 'Best Practices',
    name: 'Accessibility (WCAG 2.1 AA)',
    description: 'Compliance with accessibility standards for inclusive design',
    weight: 8,
    validator: (ctx) => {
      const hasAriaLabels = Array.from(ctx.codebase.values()).some(content =>
        content.includes('aria-label') || content.includes('aria-labelledby')
      );

      const hasSemanticHtml = Array.from(ctx.codebase.values()).some(content =>
        content.includes('<nav') || content.includes('<main') || content.includes('<section')
      );

      const hasKeyboardNav = Array.from(ctx.codebase.values()).some(content =>
        content.includes('(keydown)') || content.includes('tabindex')
      );

      const score = (hasAriaLabels ? 40 : 0) + (hasSemanticHtml ? 30 : 0) + (hasKeyboardNav ? 30 : 0);
      const passed = score >= 70;

      return {
        passed,
        score,
        findings: [
          hasAriaLabels ? '✓ ARIA labels present' : '✗ ARIA labels missing',
          hasSemanticHtml ? '✓ Semantic HTML5 elements used' : '✗ Semantic HTML missing',
          hasKeyboardNav ? '✓ Keyboard navigation support' : '✗ Keyboard navigation missing'
        ],
        recommendations: [
          'Run automated accessibility audit (axe, Lighthouse)',
          'Ensure minimum contrast ratio 4.5:1 for text',
          'Test with screen reader (NVDA, JAWS)',
          'Provide focus indicators for keyboard users'
        ],
        criticalIssues: passed ? [] : ['Healthcare systems often require Section 508/WCAG compliance']
      };
    }
  },
  {
    id: 'BP-004',
    category: 'Best Practices',
    name: 'Error Handling & User Feedback',
    description: 'Graceful error handling with informative user messages',
    weight: 6,
    validator: (ctx) => {
      const hasErrorHandling = ctx.serviceFiles.some(f => {
        const content = ctx.codebase.get(f) || '';
        return content.includes('catchError') || content.includes('ErrorHandler');
      });

      const hasLoadingStates = ctx.componentFiles.some(f => {
        const content = ctx.codebase.get(f) || '';
        return content.includes('loading') || content.includes('spinner');
      });

      const hasToasts = Array.from(ctx.codebase.values()).some(content =>
        content.includes('snackBar') || content.includes('toast') || content.includes('notification')
      );

      const score = (hasErrorHandling ? 40 : 0) + (hasLoadingStates ? 30 : 0) + (hasToasts ? 30 : 0);
      const passed = score >= 70;

      return {
        passed,
        score,
        findings: [
          hasErrorHandling ? '✓ RxJS error handling present' : '✗ Error handling missing',
          hasLoadingStates ? '✓ Loading states implemented' : '✗ Loading states missing',
          hasToasts ? '✓ User notifications (toasts/snackbar)' : '✗ User feedback mechanism missing'
        ],
        recommendations: [
          'Implement global error handler for uncaught exceptions',
          'Show user-friendly messages (not stack traces)',
          'Log errors to monitoring service (Sentry, LogRocket)',
          'Provide actionable next steps in error messages'
        ],
        criticalIssues: passed ? [] : ['Poor error handling frustrates users and hides bugs']
      };
    }
  },
  {
    id: 'BP-005',
    category: 'Best Practices',
    name: 'Performance Optimization',
    description: 'Lazy loading, OnPush, virtual scrolling for large datasets',
    weight: 7,
    validator: (ctx) => {
      const hasLazyLoading = ctx.moduleFiles.some(f => {
        const content = ctx.codebase.get(f) || '';
        return content.includes('loadChildren');
      });

      const hasOnPush = Array.from(ctx.codebase.values()).some(content =>
        content.includes('ChangeDetectionStrategy.OnPush')
      );

      const hasVirtualScroll = Array.from(ctx.codebase.values()).some(content =>
        content.includes('cdk-virtual-scroll') || content.includes('VirtualScroll')
      );

      const score = (hasLazyLoading ? 50 : 0) + (hasOnPush ? 30 : 0) + (hasVirtualScroll ? 20 : 0);
      const passed = score >= 60;

      return {
        passed,
        score,
        findings: [
          hasLazyLoading ? '✓ Lazy-loaded routes for code splitting' : '✗ Lazy loading missing - consider for large app',
          hasOnPush ? '✓ OnPush change detection used' : '• OnPush can improve performance',
          hasVirtualScroll ? '✓ Virtual scrolling for large lists' : '• Virtual scroll recommended for 100+ items'
        ],
        recommendations: [
          'Use trackBy functions in *ngFor for better rendering',
          'Implement pagination for tables with 50+ rows',
          'Optimize bundle size with Ivy and tree-shaking',
          'Cache API responses where appropriate'
        ],
        criticalIssues: passed ? [] : ['Performance issues lead to poor user experience']
      };
    }
  }
];

// ============================================================================
// HIPAA COMPLIANCE CRITERIA
// ============================================================================

const hipaaComplianceCriteria: ValidationCriterion[] = [
  {
    id: 'HIPAA-001',
    category: 'HIPAA Compliance',
    name: 'Authentication & Authorization',
    description: 'JWT-based auth with role-based access control',
    weight: 10,
    validator: (ctx) => {
      const hasAuthGuard = ctx.serviceFiles.some(f => f.includes('auth.guard') || f.includes('AuthGuard'));
      const hasAuthService = ctx.serviceFiles.some(f => f.includes('auth.service') || f.includes('AuthService'));
      const hasRoleCheck = Array.from(ctx.codebase.values()).some(content =>
        content.includes('canActivate') || content.includes('hasRole')
      );

      const passed = hasAuthGuard && hasAuthService;
      return {
        passed,
        score: passed ? 100 : 20,
        findings: [
          hasAuthGuard ? '✓ Auth guard protects routes' : '✗ CRITICAL: No auth guard found',
          hasAuthService ? '✓ Auth service manages tokens' : '✗ CRITICAL: No auth service',
          hasRoleCheck ? '✓ Role-based authorization logic' : '• Role checks recommended for granular access'
        ],
        recommendations: passed ? [
          'Verify all PHI routes are guarded',
          'Implement MFA for administrative users',
          'Set session timeout (15 min recommended for HIPAA)'
        ] : [
          'URGENT: Implement authentication before handling PHI',
          'Use JWT tokens with short expiration (15 min)',
          'Store tokens in httpOnly cookies (not localStorage)'
        ],
        criticalIssues: passed ? [] : ['HIPAA requires authentication and access controls for PHI systems']
      };
    }
  },
  {
    id: 'HIPAA-002',
    category: 'HIPAA Compliance',
    name: 'Audit Logging',
    description: 'Comprehensive audit trail for PHI access and modifications',
    weight: 9,
    validator: (ctx) => {
      const hasAuditService = ctx.serviceFiles.some(f => f.includes('audit'));
      const logsUserActions = Array.from(ctx.codebase.values()).some(content =>
        content.includes('audit.log') || content.includes('trackAction') || content.includes('logAccess')
      );

      const passed = hasAuditService && logsUserActions;
      return {
        passed,
        score: passed ? 90 : 30,
        findings: [
          hasAuditService ? '✓ Audit service exists' : '✗ CRITICAL: No audit service',
          logsUserActions ? '✓ User actions logged' : '✗ User action logging missing'
        ],
        recommendations: passed ? [
          'Log: user ID, timestamp, action, resource ID, IP address',
          'Ensure logs are tamper-proof (write-only, signed)',
          'Retain audit logs for 6 years per HIPAA'
        ] : [
          'URGENT: Implement audit logging before production',
          'Log all PHI access (view, create, update, delete)',
          'Send audit events to backend service (don\'t rely on client logs)'
        ],
        criticalIssues: passed ? [] : ['HIPAA §164.312(b) requires audit controls']
      };
    }
  },
  {
    id: 'HIPAA-003',
    category: 'HIPAA Compliance',
    name: 'Data Encryption in Transit',
    description: 'All API calls over HTTPS/TLS 1.2+',
    weight: 10,
    validator: (ctx) => {
      const usesHttpClient = ctx.serviceFiles.some(f => {
        const content = ctx.codebase.get(f) || '';
        return content.includes('HttpClient');
      });

      const noInsecureHttp = !Array.from(ctx.codebase.values()).some(content =>
        content.match(/http:\/\/(?!localhost)/)
      );

      const passed = usesHttpClient && noInsecureHttp;
      return {
        passed,
        score: passed ? 100 : 0,
        findings: [
          usesHttpClient ? '✓ Angular HttpClient in use' : '✗ HTTP client missing',
          noInsecureHttp ? '✓ No insecure HTTP endpoints detected' : '✗ CRITICAL: HTTP (not HTTPS) endpoints found'
        ],
        recommendations: passed ? [
          'Verify production deployment uses HTTPS',
          'Configure HSTS headers on server',
          'Use TLS 1.2 or higher (TLS 1.3 preferred)'
        ] : [
          'URGENT: Replace all HTTP calls with HTTPS',
          'Configure API baseUrl to use https://',
          'Enforce TLS 1.2+ at load balancer/gateway'
        ],
        criticalIssues: passed ? [] : ['HIPAA §164.312(e)(1) requires encryption in transit']
      };
    }
  },
  {
    id: 'HIPAA-004',
    category: 'HIPAA Compliance',
    name: 'Session Management',
    description: 'Automatic session timeout and secure token handling',
    weight: 8,
    validator: (ctx) => {
      const hasIdleDetection = Array.from(ctx.codebase.values()).some(content =>
        content.includes('idle') || content.includes('inactivity') || content.includes('timeout')
      );

      const hasLogoutFunction = ctx.serviceFiles.some(f => {
        const content = ctx.codebase.get(f) || '';
        return content.includes('logout') || content.includes('signOut');
      });

      const passed = hasIdleDetection && hasLogoutFunction;
      return {
        passed,
        score: passed ? 85 : 40,
        findings: [
          hasIdleDetection ? '✓ Idle/inactivity detection present' : '✗ Session timeout missing',
          hasLogoutFunction ? '✓ Logout functionality exists' : '✗ Logout function missing'
        ],
        recommendations: passed ? [
          'Set idle timeout to 15 minutes (HIPAA recommendation)',
          'Warn user 2 minutes before timeout',
          'Clear all tokens and state on logout',
          'Implement "Continue Session" button for active users'
        ] : [
          'Implement idle detection library (ng-idle, idle-js)',
          'Auto-logout after 15 minutes of inactivity',
          'Revoke refresh tokens on logout'
        ],
        criticalIssues: passed ? [] : ['Unattended sessions pose PHI exposure risk']
      };
    }
  },
  {
    id: 'HIPAA-005',
    category: 'HIPAA Compliance',
    name: 'PHI Display Controls',
    description: 'No PHI in browser console, localStorage, or cache',
    weight: 9,
    validator: (ctx) => {
      const hasConsoleLog = Array.from(ctx.codebase.values()).some(content =>
        content.includes('console.log')
      );

      const usesLocalStorage = Array.from(ctx.codebase.values()).some(content =>
        content.includes('localStorage.setItem') && content.match(/patient|phi|ssn|mrn/i)
      );

      const score = (!hasConsoleLog ? 50 : 20) + (!usesLocalStorage ? 50 : 0);
      const passed = score >= 70;

      return {
        passed,
        score,
        findings: [
          hasConsoleLog ? '⚠ console.log() found - may leak PHI' : '✓ No console.log() statements',
          usesLocalStorage ? '✗ CRITICAL: PHI in localStorage detected' : '✓ No PHI in localStorage'
        ],
        recommendations: [
          'Remove all console.log() before production build',
          'Use Angular production mode to strip logs',
          'Store PHI only in memory or secure httpOnly cookies',
          'Mask SSN/MRN with XXX-XX-1234 format',
          'Implement print prevention or watermarking'
        ],
        criticalIssues: usesLocalStorage ? ['Storing PHI in localStorage violates HIPAA encryption requirements'] : []
      };
    }
  },
  {
    id: 'HIPAA-006',
    category: 'HIPAA Compliance',
    name: 'Patient Consent Management',
    description: 'UI for managing patient consent and data sharing preferences',
    weight: 7,
    validator: (ctx) => {
      const hasConsentComponent = ctx.componentFiles.some(f => f.includes('consent'));
      const hasConsentService = ctx.serviceFiles.some(f => f.includes('consent'));

      const passed = hasConsentComponent || hasConsentService;
      return {
        passed,
        score: passed ? 75 : 30,
        findings: [
          hasConsentComponent ? '✓ Consent UI component exists' : '• Consent UI recommended',
          hasConsentService ? '✓ Consent service for API integration' : '• Consent service missing'
        ],
        recommendations: [
          'Allow patients to view/revoke data sharing consent',
          'Display consent status on patient dashboard',
          'Audit all consent changes with timestamp and user',
          'Support granular consent (per provider, per purpose)'
        ],
        criticalIssues: []
      };
    }
  }
];

// ============================================================================
// VALIDATION ENGINE
// ============================================================================

async function loadCodebase(rootDir: string): Promise<Map<string, string>> {
  const codebase = new Map<string, string>();

  const patterns = [
    `${rootDir}/**/*.ts`,
    `${rootDir}/**/*.html`,
    `${rootDir}/**/*.scss`,
    `${rootDir}/**/*.css`
  ];

  for (const pattern of patterns) {
    const files = glob.sync(pattern, {
      ignore: ['**/node_modules/**', '**/dist/**', '**/*.spec.ts']
    });

    for (const file of files) {
      try {
        const content = fs.readFileSync(file, 'utf-8');
        codebase.set(file, content);
      } catch (err) {
        console.warn(`Failed to read ${file}`);
      }
    }
  }

  return codebase;
}

function categorizeFiles(files: string[]) {
  return {
    componentFiles: files.filter(f => f.includes('.component.ts')),
    serviceFiles: files.filter(f => f.includes('.service.ts')),
    moduleFiles: files.filter(f => f.includes('.module.ts')),
    templateFiles: files.filter(f => f.endsWith('.html')),
    styleFiles: files.filter(f => f.endsWith('.scss') || f.endsWith('.css'))
  };
}

function calculateGrade(percentage: number): string {
  if (percentage >= 95) return 'A+';
  if (percentage >= 90) return 'A';
  if (percentage >= 85) return 'A-';
  if (percentage >= 80) return 'B+';
  if (percentage >= 75) return 'B';
  if (percentage >= 70) return 'B-';
  if (percentage >= 65) return 'C+';
  if (percentage >= 60) return 'C';
  if (percentage >= 55) return 'C-';
  if (percentage >= 50) return 'D';
  return 'F';
}

async function runValidation(rootDir: string): Promise<{
  categories: CategoryScore[];
  overallScore: number;
  overallGrade: string;
  criticalIssues: string[];
  topRecommendations: string[];
}> {
  console.log('🔍 Loading codebase...');
  const codebase = await loadCodebase(rootDir);
  const files = Array.from(codebase.keys());
  const categorized = categorizeFiles(files);

  const context: ValidationContext = {
    ...categorized,
    featureList: {},
    codebase
  };

  console.log(`📊 Analyzing ${files.length} files...`);
  console.log(`   - ${context.componentFiles.length} components`);
  console.log(`   - ${context.serviceFiles.length} services`);
  console.log(`   - ${context.moduleFiles.length} modules`);
  console.log('');

  const allCriteria = [
    ...customerSuccessCriteria,
    ...industryBestPractices,
    ...hipaaComplianceCriteria
  ];

  const categoryMap = new Map<string, CategoryScore>();
  const allCriticalIssues: string[] = [];
  const allRecommendations: string[] = [];

  for (const criterion of allCriteria) {
    const result = criterion.validator(context);

    if (!categoryMap.has(criterion.category)) {
      categoryMap.set(criterion.category, {
        category: criterion.category,
        totalWeight: 0,
        achievedScore: 0,
        maxScore: 0,
        percentage: 0,
        grade: 'F',
        criteria: []
      });
    }

    const cat = categoryMap.get(criterion.category)!;
    cat.totalWeight += criterion.weight;
    cat.achievedScore += (result.score / 100) * criterion.weight;
    cat.maxScore += criterion.weight;
    cat.criteria.push({
      id: criterion.id,
      name: criterion.name,
      passed: result.passed,
      score: result.score,
      weight: criterion.weight
    });

    allCriticalIssues.push(...result.criticalIssues);
    allRecommendations.push(...result.recommendations);
  }

  // Calculate percentages and grades
  const categories: CategoryScore[] = [];
  let totalWeightedScore = 0;
  let totalMaxScore = 0;

  for (const cat of categoryMap.values()) {
    cat.percentage = (cat.achievedScore / cat.maxScore) * 100;
    cat.grade = calculateGrade(cat.percentage);
    categories.push(cat);

    totalWeightedScore += cat.achievedScore;
    totalMaxScore += cat.maxScore;
  }

  const overallScore = (totalWeightedScore / totalMaxScore) * 100;
  const overallGrade = calculateGrade(overallScore);

  // Prioritize recommendations
  const topRecommendations = allRecommendations
    .filter((r, i, arr) => arr.indexOf(r) === i) // Unique
    .slice(0, 10);

  return {
    categories,
    overallScore,
    overallGrade,
    criticalIssues: [...new Set(allCriticalIssues)],
    topRecommendations
  };
}

// ============================================================================
// REPORT GENERATION
// ============================================================================

function generateTextReport(results: Awaited<ReturnType<typeof runValidation>>): string {
  let report = '';

  report += '═'.repeat(80) + '\n';
  report += '  HDIM UI IMPLEMENTATION VALIDATION REPORT\n';
  report += '═'.repeat(80) + '\n\n';

  report += `Overall Score: ${results.overallScore.toFixed(1)}% (Grade: ${results.overallGrade})\n`;
  report += `Generated: ${new Date().toISOString()}\n\n`;

  // Category Breakdown
  report += '─'.repeat(80) + '\n';
  report += 'CATEGORY SCORES\n';
  report += '─'.repeat(80) + '\n\n';

  for (const cat of results.categories) {
    report += `${cat.category}: ${cat.percentage.toFixed(1)}% (${cat.grade})\n`;
    report += `  Max Score: ${cat.maxScore} | Achieved: ${cat.achievedScore.toFixed(1)}\n`;
    report += `  Criteria:\n`;

    for (const crit of cat.criteria) {
      const icon = crit.passed ? '✓' : '✗';
      report += `    ${icon} [${crit.id}] ${crit.name} - ${crit.score}%\n`;
    }
    report += '\n';
  }

  // Critical Issues
  if (results.criticalIssues.length > 0) {
    report += '─'.repeat(80) + '\n';
    report += '⚠️  CRITICAL ISSUES (Must Address Before Production)\n';
    report += '─'.repeat(80) + '\n\n';

    results.criticalIssues.forEach((issue, i) => {
      report += `${i + 1}. ${issue}\n`;
    });
    report += '\n';
  }

  // Top Recommendations
  report += '─'.repeat(80) + '\n';
  report += '💡 TOP RECOMMENDATIONS\n';
  report += '─'.repeat(80) + '\n\n';

  results.topRecommendations.forEach((rec, i) => {
    report += `${i + 1}. ${rec}\n`;
  });
  report += '\n';

  report += '═'.repeat(80) + '\n';

  return report;
}

// ============================================================================
// MAIN EXECUTION
// ============================================================================

async function main() {
  const args = process.argv.slice(2);
  const rootDir = args.find(a => !a.startsWith('--')) ||
    path.join(__dirname, '../hdim-backend-tests/apps/clinical-portal');

  const results = await runValidation(rootDir);

  const report = generateTextReport(results);
  console.log(report);

  if (args.includes('--report')) {
    const reportPath = path.join(__dirname, '../ui-validation-report.txt');
    fs.writeFileSync(reportPath, report);
    console.log(`📄 Report saved to: ${reportPath}`);
  }

  if (args.includes('--json')) {
    const jsonPath = path.join(__dirname, '../ui-validation-results.json');
    fs.writeFileSync(jsonPath, JSON.stringify(results, null, 2));
    console.log(`📊 JSON results saved to: ${jsonPath}`);
  }

  // Exit code based on grade
  if (results.criticalIssues.length > 0) {
    process.exit(1);
  } else if (results.overallScore < 70) {
    process.exit(2);
  } else {
    process.exit(0);
  }
}

if (require.main === module) {
  main().catch(console.error);
}

export { runValidation, ValidationCriterion, ValidationContext, ValidationResult };
