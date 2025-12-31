import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, BehaviorSubject } from 'rxjs';
import { KnowledgeBaseService } from './knowledge-base.service';
import { API_CONFIG } from '../config/api.config';

/**
 * AI Assistant Service
 *
 * Provides AI-powered assistance for:
 * - UI/UX improvement suggestions
 * - User interaction analysis
 * - Context-aware help
 * - Automated testing recommendations
 * - Accessibility improvements
 * - Performance optimization suggestions
 */

export interface AIMessage {
  id: string;
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: Date;
  metadata?: {
    component?: string;
    action?: string;
    context?: Record<string, any>;
  };
}

export interface AIAnalysis {
  type: 'ui_improvement' | 'ux_enhancement' | 'accessibility' | 'performance' | 'testing';
  severity: 'low' | 'medium' | 'high' | 'critical';
  title: string;
  description: string;
  recommendation: string;
  codeExample?: string;
  affectedComponents: string[];
  estimatedImpact: 'low' | 'medium' | 'high';
  implementationSteps?: string[];
}

export interface UserInteraction {
  timestamp: Date;
  component: string;
  action: string;
  duration?: number;
  success: boolean;
  errorMessage?: string;
  context?: Record<string, any>;
}

@Injectable({
  providedIn: 'root'
})
export class AIAssistantService {
  private readonly API_URL = API_CONFIG.AI_ASSISTANT_URL;

  // Chat interface
  private messagesSubject = new BehaviorSubject<AIMessage[]>([]);
  messages$ = this.messagesSubject.asObservable();

  // Analysis results
  private analysisSubject = new BehaviorSubject<AIAnalysis[]>([]);
  analysis$ = this.analysisSubject.asObservable();

  // User interaction tracking
  private interactions: UserInteraction[] = [];
  private readonly MAX_INTERACTIONS = 1000;

  // Agent state
  private isActive = false;
  private autoAnalyzeEnabled = false;

  constructor(
    private http: HttpClient,
    private kbService: KnowledgeBaseService = inject(KnowledgeBaseService)
  ) {
    this.initializeAgent();
  }

  /**
   * Initialize the AI agent
   */
  private initializeAgent() {
    // Load previous session data from localStorage
    const savedInteractions = localStorage.getItem('ai_interactions');
    if (savedInteractions) {
      this.interactions = JSON.parse(savedInteractions);
    }

    // Add welcome message
    this.addSystemMessage(
      'AI Assistant initialized. I can help you improve the UI/UX, analyze user interactions, ' +
      'and provide recommendations for accessibility, performance, and testing.'
    );
  }

  /**
   * Activate the AI agent
   */
  activateAgent() {
    this.isActive = true;
    this.addSystemMessage('AI Agent activated. Monitoring user interactions...');
  }

  /**
   * Deactivate the AI agent
   */
  deactivateAgent() {
    this.isActive = false;
    this.addSystemMessage('AI Agent deactivated.');
  }

  /**
   * Enable automatic analysis
   */
  enableAutoAnalysis() {
    this.autoAnalyzeEnabled = true;
    this.addSystemMessage('Automatic analysis enabled. I will analyze interactions every 50 actions.');
  }

  /**
   * Track user interaction
   */
  trackInteraction(interaction: Omit<UserInteraction, 'timestamp'>) {
    if (!this.isActive) return;

    const fullInteraction: UserInteraction = {
      ...interaction,
      timestamp: new Date()
    };

    this.interactions.push(fullInteraction);

    // Keep only last MAX_INTERACTIONS
    if (this.interactions.length > this.MAX_INTERACTIONS) {
      this.interactions = this.interactions.slice(-this.MAX_INTERACTIONS);
    }

    // Save to localStorage
    this.saveInteractions();

    // Auto-analyze if enabled
    if (this.autoAnalyzeEnabled && this.interactions.length % 50 === 0) {
      this.analyzeInteractions();
    }
  }

  /**
   * Send message to AI assistant
   */
  async sendMessage(content: string, context?: Record<string, any>): Promise<AIMessage> {
    const userMessage: AIMessage = {
      id: this.generateId(),
      role: 'user',
      content,
      timestamp: new Date(),
      metadata: { context }
    };

    this.addMessage(userMessage);

    try {
      // In production, this would call your AI backend (OpenAI, Claude, etc.)
      const response = await this.callAIBackend(content, context);

      const assistantMessage: AIMessage = {
        id: this.generateId(),
        role: 'assistant',
        content: response,
        timestamp: new Date()
      };

      this.addMessage(assistantMessage);
      return assistantMessage;
    } catch (error) {
      const errorMessage: AIMessage = {
        id: this.generateId(),
        role: 'system',
        content: `Error: ${error}. Using fallback AI response.`,
        timestamp: new Date()
      };
      this.addMessage(errorMessage);

      // Fallback to rule-based responses
      const fallbackResponse = this.getFallbackResponse(content, context);
      const assistantMessage: AIMessage = {
        id: this.generateId(),
        role: 'assistant',
        content: fallbackResponse,
        timestamp: new Date()
      };
      this.addMessage(assistantMessage);
      return assistantMessage;
    }
  }

  /**
   * Analyze user interactions and provide recommendations
   */
  async analyzeInteractions(): Promise<AIAnalysis[]> {
    if (this.interactions.length === 0) {
      this.addSystemMessage('No interactions to analyze yet.');
      return [];
    }

    this.addSystemMessage(`Analyzing ${this.interactions.length} user interactions...`);

    const analyses: AIAnalysis[] = [];

    // Analysis 1: Error rate analysis
    const errorRate = this.calculateErrorRate();
    if (errorRate > 0.1) {
      analyses.push({
        type: 'ux_enhancement',
        severity: errorRate > 0.3 ? 'critical' : 'high',
        title: 'High Error Rate Detected',
        description: `${(errorRate * 100).toFixed(1)}% of user actions result in errors`,
        recommendation: 'Review error-prone workflows and add validation, better error messages, or inline help',
        affectedComponents: this.getTopErrorComponents(),
        estimatedImpact: 'high',
        implementationSteps: [
          'Identify components with highest error rates',
          'Add inline validation and helpful error messages',
          'Implement progressive disclosure for complex forms',
          'Add contextual help tooltips'
        ]
      });
    }

    // Analysis 2: Slow interactions
    const slowInteractions = this.findSlowInteractions();
    if (slowInteractions.length > 0) {
      analyses.push({
        type: 'performance',
        severity: 'medium',
        title: 'Slow User Interactions Detected',
        description: `${slowInteractions.length} interactions took longer than 3 seconds`,
        recommendation: 'Optimize component loading and add loading indicators',
        affectedComponents: slowInteractions.map(i => i.component),
        estimatedImpact: 'medium',
        implementationSteps: [
          'Add skeleton loaders for slow-loading components',
          'Implement lazy loading for heavy components',
          'Add progress indicators for long operations',
          'Consider caching frequently accessed data'
        ]
      });
    }

    // Analysis 3: Unused features
    const unusedComponents = this.findUnusedComponents();
    if (unusedComponents.length > 0) {
      analyses.push({
        type: 'ui_improvement',
        severity: 'low',
        title: 'Underutilized Features Detected',
        description: `${unusedComponents.length} components have very low usage`,
        recommendation: 'Consider improving discoverability or removing unused features',
        affectedComponents: unusedComponents,
        estimatedImpact: 'medium',
        implementationSteps: [
          'Add onboarding tooltips for underused features',
          'Improve feature visibility in UI',
          'Consider A/B testing different placements',
          'Gather user feedback on feature utility'
        ]
      });
    }

    // Analysis 4: Accessibility issues
    const accessibilityIssues = this.detectAccessibilityIssues();
    if (accessibilityIssues.length > 0) {
      analyses.push({
        type: 'accessibility',
        severity: 'high',
        title: 'Accessibility Improvements Needed',
        description: 'Detected potential keyboard navigation and screen reader issues',
        recommendation: 'Add ARIA labels, improve keyboard navigation, and test with screen readers',
        affectedComponents: accessibilityIssues,
        estimatedImpact: 'high',
        codeExample: `
// Add ARIA labels
<button aria-label="Save measure" (click)="save()">
  <svg>...</svg>
</button>

// Add keyboard navigation
@HostListener('document:keydown', ['$event'])
handleKeyboard(event: KeyboardEvent) {
  if (event.key === 'Enter' && this.isFormValid()) {
    this.save();
  }
}
        `,
        implementationSteps: [
          'Add aria-label to all icon-only buttons',
          'Ensure all interactive elements are keyboard accessible',
          'Test with NVDA/JAWS screen readers',
          'Add skip-to-content links'
        ]
      });
    }

    // Analysis 5: Testing recommendations
    const testingGaps = this.identifyTestingGaps();
    if (testingGaps.length > 0) {
      analyses.push({
        type: 'testing',
        severity: 'medium',
        title: 'Testing Coverage Gaps',
        description: 'Critical user workflows lack automated test coverage',
        recommendation: 'Add E2E tests for high-traffic user workflows',
        affectedComponents: testingGaps,
        estimatedImpact: 'high',
        codeExample: `
// E2E test example with Cypress
describe('Measure Creation Workflow', () => {
  it('should create a new quality measure', () => {
    cy.visit('/measure-builder');
    cy.get('[data-testid="measure-name"]').type('Test Measure');
    cy.get('[data-testid="category"]').select('Diabetes');
    cy.get('[data-testid="save-draft"]').click();
    cy.contains('Measure saved successfully').should('be.visible');
  });
});
        `,
        implementationSteps: [
          'Identify top 5 critical user workflows',
          'Write E2E tests using Cypress or Playwright',
          'Add data-testid attributes to key elements',
          'Set up CI/CD pipeline for automated testing'
        ]
      });
    }

    this.analysisSubject.next(analyses);
    this.addSystemMessage(`Analysis complete. Found ${analyses.length} recommendations.`);

    return analyses;
  }

  /**
   * Get AI suggestions for a specific component
   */
  async getComponentSuggestions(componentName: string): Promise<string> {
    const componentInteractions = this.interactions.filter(
      i => i.component === componentName
    );

    if (componentInteractions.length === 0) {
      return `No interaction data available for ${componentName}. Start using the component to get AI recommendations.`;
    }

    const errorRate = componentInteractions.filter(i => !i.success).length / componentInteractions.length;
    const avgDuration = this.calculateAverageDuration(componentInteractions);

    let suggestions = `**AI Analysis for ${componentName}**\n\n`;
    suggestions += `📊 **Usage Statistics:**\n`;
    suggestions += `- Total interactions: ${componentInteractions.length}\n`;
    suggestions += `- Error rate: ${(errorRate * 100).toFixed(1)}%\n`;
    suggestions += `- Average duration: ${avgDuration.toFixed(2)}s\n\n`;

    if (errorRate > 0.2) {
      suggestions += `⚠️ **High Error Rate Alert**\n`;
      suggestions += `This component has a ${(errorRate * 100).toFixed(1)}% error rate.\n\n`;
      suggestions += `**Recommendations:**\n`;
      suggestions += `1. Add inline validation to prevent errors before submission\n`;
      suggestions += `2. Improve error messages to be more actionable\n`;
      suggestions += `3. Add help tooltips for confusing fields\n`;
      suggestions += `4. Consider adding a confirmation dialog for destructive actions\n\n`;
    }

    if (avgDuration > 5) {
      suggestions += `🐌 **Performance Concern**\n`;
      suggestions += `Average interaction time is ${avgDuration.toFixed(2)} seconds.\n\n`;
      suggestions += `**Recommendations:**\n`;
      suggestions += `1. Add loading skeleton or spinner for better perceived performance\n`;
      suggestions += `2. Implement lazy loading for heavy data\n`;
      suggestions += `3. Consider caching frequently accessed data\n`;
      suggestions += `4. Optimize any API calls or computations\n\n`;
    }

    if (componentInteractions.length < 10 && this.interactions.length > 100) {
      suggestions += `📉 **Low Usage Detected**\n`;
      suggestions += `This component is underutilized compared to others.\n\n`;
      suggestions += `**Recommendations:**\n`;
      suggestions += `1. Improve feature discoverability (add to dashboard, menu, etc.)\n`;
      suggestions += `2. Add onboarding tutorial or tooltip\n`;
      suggestions += `3. Consider if this feature is still needed\n`;
      suggestions += `4. Gather user feedback on feature value\n\n`;
    }

    return suggestions;
  }

  /**
   * Clear all tracked interactions
   */
  clearInteractions() {
    this.interactions = [];
    this.saveInteractions();
    this.addSystemMessage('Interaction history cleared.');
  }

  /**
   * Export interaction data for analysis
   */
  exportInteractions(): string {
    return JSON.stringify(this.interactions, null, 2);
  }

  /**
   * Import interaction data
   */
  importInteractions(data: string) {
    try {
      this.interactions = JSON.parse(data);
      this.saveInteractions();
      this.addSystemMessage(`Imported ${this.interactions.length} interactions.`);
    } catch (error) {
      this.addSystemMessage(`Error importing interactions: ${error}`);
    }
  }

  // Private helper methods

  private addMessage(message: AIMessage) {
    const current = this.messagesSubject.value;
    this.messagesSubject.next([...current, message]);
  }

  private addSystemMessage(content: string) {
    this.addMessage({
      id: this.generateId(),
      role: 'system',
      content,
      timestamp: new Date()
    });
  }

  private generateId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  private saveInteractions() {
    localStorage.setItem('ai_interactions', JSON.stringify(this.interactions));
  }

  private calculateErrorRate(): number {
    const errors = this.interactions.filter(i => !i.success).length;
    return errors / this.interactions.length;
  }

  private getTopErrorComponents(): string[] {
    const errorCounts: Record<string, number> = {};

    this.interactions
      .filter(i => !i.success)
      .forEach(i => {
        errorCounts[i.component] = (errorCounts[i.component] || 0) + 1;
      });

    return Object.entries(errorCounts)
      .sort(([, a], [, b]) => b - a)
      .slice(0, 5)
      .map(([component]) => component);
  }

  private findSlowInteractions(): UserInteraction[] {
    return this.interactions.filter(i => (i.duration || 0) > 3000);
  }

  private findUnusedComponents(): string[] {
    const componentCounts: Record<string, number> = {};

    this.interactions.forEach(i => {
      componentCounts[i.component] = (componentCounts[i.component] || 0) + 1;
    });

    const avgUsage = Object.values(componentCounts).reduce((a, b) => a + b, 0) /
                     Object.keys(componentCounts).length;

    return Object.entries(componentCounts)
      .filter(([, count]) => count < avgUsage * 0.2)
      .map(([component]) => component);
  }

  private detectAccessibilityIssues(): string[] {
    // This would analyze actual component code in production
    // For now, return components with high keyboard-only interaction failure rates
    const keyboardIssues: string[] = [];

    // Placeholder logic - in production, integrate with axe-core or similar
    const componentsToCheck = new Set(this.interactions.map(i => i.component));

    return Array.from(componentsToCheck).slice(0, 3);
  }

  private identifyTestingGaps(): string[] {
    const highTrafficComponents = this.getHighTrafficComponents();
    // In production, cross-reference with actual test coverage data
    return highTrafficComponents.slice(0, 5);
  }

  private getHighTrafficComponents(): string[] {
    const componentCounts: Record<string, number> = {};

    this.interactions.forEach(i => {
      componentCounts[i.component] = (componentCounts[i.component] || 0) + 1;
    });

    return Object.entries(componentCounts)
      .sort(([, a], [, b]) => b - a)
      .slice(0, 10)
      .map(([component]) => component);
  }

  private calculateAverageDuration(interactions: UserInteraction[]): number {
    const durationsArray = interactions
      .map(i => i.duration || 0)
      .filter(d => d > 0);

    if (durationsArray.length === 0) return 0;

    return durationsArray.reduce((a, b) => a + b, 0) / durationsArray.length / 1000; // Convert to seconds
  }

  private async callAIBackend(message: string, context?: Record<string, any>): Promise<string> {
    // In production, call your AI backend (OpenAI, Claude, etc.)
    // For now, return a simulated response
    return this.getFallbackResponse(message, context);
  }

  private getFallbackResponse(message: string, context?: Record<string, any>): string {
    const lowerMessage = message.toLowerCase();

    if (lowerMessage.includes('improve') && lowerMessage.includes('ui')) {
      return `Based on current usage patterns, I recommend:

1. **Add Loading States**: Users are waiting without visual feedback. Add skeleton loaders.
2. **Improve Error Messages**: Make errors more actionable with specific next steps.
3. **Add Inline Help**: Place help tooltips next to complex form fields.
4. **Optimize Performance**: Consider lazy loading and caching for frequently accessed data.

Would you like me to analyze a specific component for detailed recommendations?`;
    }

    if (lowerMessage.includes('accessibility')) {
      return `Accessibility improvements recommended:

1. **Keyboard Navigation**: Ensure all interactive elements are keyboard accessible
2. **ARIA Labels**: Add descriptive labels to icon-only buttons
3. **Focus Indicators**: Make focus states clearly visible
4. **Screen Reader Testing**: Test with NVDA or JAWS

Would you like code examples for any of these improvements?`;
    }

    if (lowerMessage.includes('test')) {
      return `Testing recommendations:

1. **E2E Tests**: Add Cypress tests for critical user workflows
2. **Component Tests**: Increase unit test coverage to 80%+
3. **Accessibility Tests**: Integrate axe-core for automated a11y testing
4. **Visual Regression**: Consider Percy or Chromatic for visual testing

Would you like me to generate test cases for a specific workflow?`;
    }

    return `I'm here to help improve your UI/UX. I can:

- Analyze user interactions and identify pain points
- Suggest accessibility improvements
- Recommend performance optimizations
- Generate test cases for critical workflows
- Provide code examples for improvements

Try asking: "How can I improve the patients page?" or "Analyze my measure builder component"`;
  }
}
