/**
 * AI Code Review Service
 *
 * Provides automated code analysis, pattern detection, and auto-fix suggestions
 * for improving code quality, performance, and maintainability.
 */

import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface CodeIssue {
  id: string;
  severity: 'error' | 'warning' | 'info' | 'suggestion';
  category: 'performance' | 'security' | 'maintainability' | 'accessibility' | 'best-practice';
  title: string;
  description: string;
  file: string;
  line?: number;
  column?: number;
  codeSnippet?: string;
  suggestedFix?: string;
  autoFixAvailable: boolean;
  references?: string[];
}

export interface CodeReviewResult {
  timestamp: Date;
  filesAnalyzed: number;
  issuesFound: number;
  issues: CodeIssue[];
  score: number; // 0-100
  summary: string;
}

export interface AutoFixResult {
  issueId: string;
  success: boolean;
  message: string;
  changes?: {
    file: string;
    oldCode: string;
    newCode: string;
  };
}

@Injectable({
  providedIn: 'root'
})
export class AICodeReviewService {
  private reviewResults = new BehaviorSubject<CodeReviewResult | null>(null);
  public reviewResults$ = this.reviewResults.asObservable();

  private isAnalyzing = new BehaviorSubject<boolean>(false);
  public isAnalyzing$ = this.isAnalyzing.asObservable();

  // Rule-based patterns for common issues
  private codePatterns = {
    // Performance issues
    performance: [
      {
        pattern: /subscribe\(\).*subscribe\(/,
        title: 'Nested Subscriptions Detected',
        description: 'Nested subscriptions can lead to memory leaks and poor performance',
        category: 'performance' as const,
        severity: 'warning' as const,
        fix: 'Use switchMap, mergeMap, or concatMap operators instead',
        autoFixAvailable: false,
      },
      {
        pattern: /\*ngFor.*trackBy/,
        title: 'Missing trackBy Function',
        description: '*ngFor without trackBy can cause unnecessary re-renders',
        category: 'performance' as const,
        severity: 'suggestion' as const,
        fix: 'Add trackBy function to optimize rendering',
        autoFixAvailable: true,
      },
      {
        pattern: /setInterval|setTimeout/,
        title: 'Potential Memory Leak',
        description: 'setInterval/setTimeout without cleanup in ngOnDestroy',
        category: 'performance' as const,
        severity: 'warning' as const,
        fix: 'Store reference and clear in ngOnDestroy',
        autoFixAvailable: false,
      },
    ],

    // Security issues
    security: [
      {
        pattern: /innerHTML\s*=|\.innerHTML/,
        title: 'Potential XSS Vulnerability',
        description: 'Direct innerHTML assignment can introduce XSS attacks',
        category: 'security' as const,
        severity: 'error' as const,
        fix: 'Use Angular\'s DomSanitizer or textContent',
        autoFixAvailable: false,
      },
      {
        pattern: /eval\(/,
        title: 'Dangerous eval() Usage',
        description: 'eval() is dangerous and should never be used',
        category: 'security' as const,
        severity: 'error' as const,
        fix: 'Remove eval() and use safer alternatives',
        autoFixAvailable: false,
      },
    ],

    // Accessibility issues
    accessibility: [
      {
        pattern: /<button[^>]*onclick/,
        title: 'onClick Instead of (click)',
        description: 'Use Angular (click) syntax instead of onclick',
        category: 'accessibility' as const,
        severity: 'suggestion' as const,
        fix: 'Replace onclick with (click)',
        autoFixAvailable: true,
      },
      {
        pattern: /<img(?![^>]*alt=)/,
        title: 'Image Missing alt Attribute',
        description: 'Images should have alt text for screen readers',
        category: 'accessibility' as const,
        severity: 'warning' as const,
        fix: 'Add descriptive alt attribute',
        autoFixAvailable: false,
      },
    ],

    // Best practices
    bestPractice: [
      {
        pattern: /console\.(log|debug|info|warn|error)/,
        title: 'Console Statement in Production Code',
        description: 'Console statements should be removed before production',
        category: 'best-practice' as const,
        severity: 'info' as const,
        fix: 'Remove console statement or use proper logging service',
        autoFixAvailable: true,
      },
      {
        pattern: /any\s*\)/,
        title: 'Weak Type Usage (any)',
        description: 'Using "any" defeats TypeScript\'s type safety',
        category: 'best-practice' as const,
        severity: 'suggestion' as const,
        fix: 'Define proper interface or type',
        autoFixAvailable: false,
      },
      {
        pattern: /subscribe\(\)(?!.*takeUntil|take\()/,
        title: 'Unmanaged Subscription',
        description: 'Subscription without unsubscribe mechanism',
        category: 'best-practice' as const,
        severity: 'warning' as const,
        fix: 'Use takeUntil pattern or async pipe',
        autoFixAvailable: false,
      },
    ],

    // Maintainability
    maintainability: [
      {
        pattern: /function\s+\w+\s*\([^)]*\)\s*{[^}]{500,}/,
        title: 'Large Function Detected',
        description: 'Function is too large and should be split',
        category: 'maintainability' as const,
        severity: 'suggestion' as const,
        fix: 'Extract smaller functions',
        autoFixAvailable: false,
      },
      {
        pattern: /\/\/\s*TODO|\/\/\s*FIXME|\/\/\s*HACK/,
        title: 'TODO/FIXME Comment',
        description: 'Unresolved TODO or FIXME comment',
        category: 'maintainability' as const,
        severity: 'info' as const,
        fix: 'Address the TODO or create a ticket',
        autoFixAvailable: false,
      },
    ],
  };

  constructor() {}

  /**
   * Analyze code files for issues
   */
  async analyzeCode(files: { path: string; content: string }[]): Promise<CodeReviewResult> {
    this.isAnalyzing.next(true);

    try {
      const issues: CodeIssue[] = [];
      let issueIdCounter = 0;

      // Analyze each file
      for (const file of files) {
        const fileIssues = this.analyzeFile(file.path, file.content, issueIdCounter);
        issues.push(...fileIssues);
        issueIdCounter += fileIssues.length;
      }

      // Calculate code quality score
      const score = this.calculateQualityScore(issues);

      const result: CodeReviewResult = {
        timestamp: new Date(),
        filesAnalyzed: files.length,
        issuesFound: issues.length,
        issues,
        score,
        summary: this.generateSummary(issues, score),
      };

      this.reviewResults.next(result);
      return result;
    } finally {
      this.isAnalyzing.next(false);
    }
  }

  /**
   * Analyze a single file
   */
  private analyzeFile(filePath: string, content: string, startId: number): CodeIssue[] {
    const issues: CodeIssue[] = [];
    let issueId = startId;

    // Check all pattern categories
    const allPatterns = [
      ...this.codePatterns.performance,
      ...this.codePatterns.security,
      ...this.codePatterns.accessibility,
      ...this.codePatterns.bestPractice,
      ...this.codePatterns.maintainability,
    ];

    // Analyze each line
    const lines = content.split('\n');
    lines.forEach((line, lineNumber) => {
      allPatterns.forEach((pattern) => {
        if (pattern.pattern.test(line)) {
          issues.push({
            id: `issue-${issueId++}`,
            severity: pattern.severity,
            category: pattern.category,
            title: pattern.title,
            description: pattern.description,
            file: filePath,
            line: lineNumber + 1,
            codeSnippet: line.trim(),
            suggestedFix: pattern.fix,
            autoFixAvailable: pattern.autoFixAvailable,
          });
        }
      });
    });

    return issues;
  }

  /**
   * Calculate code quality score (0-100)
   */
  private calculateQualityScore(issues: CodeIssue[]): number {
    const weights = {
      error: 10,
      warning: 5,
      info: 2,
      suggestion: 1,
    };

    const totalDeductions = issues.reduce((sum, issue) => {
      return sum + weights[issue.severity];
    }, 0);

    // Start at 100, deduct points for issues
    const score = Math.max(0, 100 - totalDeductions);
    return Math.round(score);
  }

  /**
   * Generate summary text
   */
  private generateSummary(issues: CodeIssue[], score: number): string {
    const errorCount = issues.filter(i => i.severity === 'error').length;
    const warningCount = issues.filter(i => i.severity === 'warning').length;
    const suggestionCount = issues.filter(i => i.severity === 'suggestion').length;

    let summary = `Code Quality Score: ${score}/100. `;

    if (errorCount > 0) {
      summary += `Found ${errorCount} critical error(s). `;
    }
    if (warningCount > 0) {
      summary += `${warningCount} warning(s). `;
    }
    if (suggestionCount > 0) {
      summary += `${suggestionCount} suggestion(s) for improvement.`;
    }

    if (issues.length === 0) {
      summary = 'Excellent! No issues found. Code quality score: 100/100.';
    }

    return summary;
  }

  /**
   * Apply automatic fix for an issue
   */
  async applyAutoFix(issue: CodeIssue): Promise<AutoFixResult> {
    if (!issue.autoFixAvailable) {
      return {
        issueId: issue.id,
        success: false,
        message: 'Auto-fix not available for this issue',
      };
    }

    try {
      // Implement specific auto-fixes based on issue type
      const fix = await this.generateAutoFix(issue);

      return {
        issueId: issue.id,
        success: true,
        message: 'Auto-fix applied successfully',
        changes: fix,
      };
    } catch (error: any) {
      return {
        issueId: issue.id,
        success: false,
        message: `Auto-fix failed: ${error.message}`,
      };
    }
  }

  /**
   * Generate auto-fix code
   */
  private async generateAutoFix(issue: CodeIssue): Promise<{
    file: string;
    oldCode: string;
    newCode: string;
  }> {
    const oldCode = issue.codeSnippet || '';
    let newCode = oldCode;

    // Apply specific fixes based on issue title
    switch (issue.title) {
      case 'Console Statement in Production Code':
        newCode = oldCode.replace(/console\.(log|debug|info|warn|error)\([^)]*\);?/, '');
        break;

      case 'onClick Instead of (click)':
        newCode = oldCode.replace(/onclick="([^"]*)"/, '(click)="$1"');
        break;

      case 'Missing trackBy Function':
        // Extract the *ngFor expression
        const ngForMatch = oldCode.match(/\*ngFor="let (\w+) of (\w+)"/);
        if (ngForMatch) {
          const itemName = ngForMatch[1];
          newCode = oldCode.replace(
            /\*ngFor="([^"]*)"/,
            `*ngFor="$1; trackBy: track${itemName.charAt(0).toUpperCase() + itemName.slice(1)}"`
          );
        }
        break;

      default:
        // No auto-fix available
        break;
    }

    return {
      file: issue.file,
      oldCode,
      newCode,
    };
  }

  /**
   * Get issues by severity
   */
  getIssuesBySeverity(severity: CodeIssue['severity']): CodeIssue[] {
    const currentResult = this.reviewResults.value;
    if (!currentResult) return [];
    return currentResult.issues.filter(issue => issue.severity === severity);
  }

  /**
   * Get issues by category
   */
  getIssuesByCategory(category: CodeIssue['category']): CodeIssue[] {
    const currentResult = this.reviewResults.value;
    if (!currentResult) return [];
    return currentResult.issues.filter(issue => issue.category === category);
  }

  /**
   * Get auto-fixable issues
   */
  getAutoFixableIssues(): CodeIssue[] {
    const currentResult = this.reviewResults.value;
    if (!currentResult) return [];
    return currentResult.issues.filter(issue => issue.autoFixAvailable);
  }

  /**
   * Clear review results
   */
  clearResults(): void {
    this.reviewResults.next(null);
  }

  /**
   * Export issues as JSON
   */
  exportIssues(): string {
    const currentResult = this.reviewResults.value;
    if (!currentResult) return '[]';
    return JSON.stringify(currentResult.issues, null, 2);
  }

  /**
   * Export issues as CSV
   */
  exportIssuesCSV(): string {
    const currentResult = this.reviewResults.value;
    if (!currentResult) return '';

    const headers = ['Severity', 'Category', 'Title', 'File', 'Line', 'Description', 'Suggested Fix'];
    const rows = currentResult.issues.map(issue => [
      issue.severity,
      issue.category,
      issue.title,
      issue.file,
      issue.line?.toString() || '',
      issue.description,
      issue.suggestedFix || '',
    ]);

    const csv = [
      headers.join(','),
      ...rows.map(row => row.map(cell => `"${cell}"`).join(',')),
    ].join('\n');

    return csv;
  }
}
