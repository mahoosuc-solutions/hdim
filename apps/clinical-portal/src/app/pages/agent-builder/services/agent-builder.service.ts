import { Injectable, inject } from '@angular/core';
import { LoggerService } from '../../../services/logger.service';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable, catchError, throwError } from 'rxjs';
import {
  AgentConfiguration,
  AgentVersion,
  AgentTestSession,
  PromptTemplate,
  ToolInfo,
  ProviderInfo,
  CreateAgentRequest,
  UpdateAgentRequest,
  TestMessageRequest,
  TestMessageResult,
  TemplateValidationResult,
  Page,
  AgentStatus,
} from '../models/agent.model';

/**
 * Service for interacting with the Agent Builder API.
 * Provides CRUD operations for agents, versions, templates, and testing.
 */
@Injectable({
  providedIn: 'root',
})
export class AgentBuilderService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1/agent-builder';
  private readonly logger = inject(LoggerService);

  constructor() {}

  // ============================================================================
  // AGENT CRUD OPERATIONS
  // ============================================================================

  /**
   * List agents with optional filtering and pagination.
   */
  listAgents(
    status?: AgentStatus,
    page = 0,
    size = 20
  ): Observable<Page<AgentConfiguration>> {
    let params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    if (status) {
      params = params.set('status', status);
    }

    return this.http
      .get<Page<AgentConfiguration>>(`${this.baseUrl}/agents`, { params })
      .pipe(catchError(this.handleError));
  }

  /**
   * Search agents by name or description.
   */
  searchAgents(
    query: string,
    page = 0,
    size = 20
  ): Observable<Page<AgentConfiguration>> {
    const params = new HttpParams()
      .set('q', query)
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<Page<AgentConfiguration>>(`${this.baseUrl}/agents/search`, { params })
      .pipe(catchError(this.handleError));
  }

  /**
   * Get a single agent by ID.
   */
  getAgent(id: string): Observable<AgentConfiguration> {
    return this.http
      .get<AgentConfiguration>(`${this.baseUrl}/agents/${id}`)
      .pipe(catchError(this.handleError));
  }

  /**
   * Get a single agent by slug.
   */
  getAgentBySlug(slug: string): Observable<AgentConfiguration> {
    return this.http
      .get<AgentConfiguration>(`${this.baseUrl}/agents/slug/${slug}`)
      .pipe(catchError(this.handleError));
  }

  /**
   * Create a new agent.
   */
  createAgent(request: CreateAgentRequest): Observable<AgentConfiguration> {
    return this.http
      .post<AgentConfiguration>(`${this.baseUrl}/agents`, request)
      .pipe(catchError(this.handleError));
  }

  /**
   * Update an existing agent.
   */
  updateAgent(
    id: string,
    request: UpdateAgentRequest,
    changeSummary?: string
  ): Observable<AgentConfiguration> {
    let params = new HttpParams();
    if (changeSummary) {
      params = params.set('changeSummary', changeSummary);
    }

    return this.http
      .put<AgentConfiguration>(`${this.baseUrl}/agents/${id}`, request, { params })
      .pipe(catchError(this.handleError));
  }

  /**
   * Delete (archive) an agent.
   */
  deleteAgent(id: string): Observable<void> {
    return this.http
      .delete<void>(`${this.baseUrl}/agents/${id}`)
      .pipe(catchError(this.handleError));
  }

  /**
   * Clone an existing agent.
   */
  cloneAgent(id: string, newName: string): Observable<AgentConfiguration> {
    const params = new HttpParams().set('newName', newName);

    return this.http
      .post<AgentConfiguration>(`${this.baseUrl}/agents/${id}/clone`, null, { params })
      .pipe(catchError(this.handleError));
  }

  /**
   * Publish an agent to production.
   */
  publishAgent(id: string): Observable<AgentConfiguration> {
    return this.http
      .post<AgentConfiguration>(`${this.baseUrl}/agents/${id}/publish`, null)
      .pipe(catchError(this.handleError));
  }

  /**
   * Deprecate an agent.
   */
  deprecateAgent(id: string): Observable<AgentConfiguration> {
    return this.http
      .post<AgentConfiguration>(`${this.baseUrl}/agents/${id}/deprecate`, null)
      .pipe(catchError(this.handleError));
  }

  /**
   * Get all active agents.
   */
  getActiveAgents(): Observable<AgentConfiguration[]> {
    return this.http
      .get<AgentConfiguration[]>(`${this.baseUrl}/agents/active`)
      .pipe(catchError(this.handleError));
  }

  // ============================================================================
  // VERSION MANAGEMENT
  // ============================================================================

  /**
   * List versions for an agent.
   */
  listVersions(agentId: string, page = 0, size = 20): Observable<Page<AgentVersion>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<Page<AgentVersion>>(`${this.baseUrl}/agents/${agentId}/versions`, { params })
      .pipe(catchError(this.handleError));
  }

  /**
   * Get a specific version.
   */
  getVersion(agentId: string, versionId: string): Observable<AgentVersion> {
    return this.http
      .get<AgentVersion>(`${this.baseUrl}/agents/${agentId}/versions/${versionId}`)
      .pipe(catchError(this.handleError));
  }

  /**
   * Rollback to a specific version.
   */
  rollbackToVersion(agentId: string, versionId: string): Observable<AgentConfiguration> {
    return this.http
      .post<AgentConfiguration>(
        `${this.baseUrl}/agents/${agentId}/rollback`,
        null,
        { params: new HttpParams().set('versionId', versionId) }
      )
      .pipe(catchError(this.handleError));
  }

  // ============================================================================
  // TESTING
  // ============================================================================

  /**
   * Start a new test session for an agent.
   */
  startTestSession(
    agentId: string,
    testType: 'INTERACTIVE' | 'AUTOMATED' | 'SCENARIO' = 'INTERACTIVE',
    scenario?: string
  ): Observable<AgentTestSession> {
    const body = { testType, scenario };

    return this.http
      .post<AgentTestSession>(`${this.baseUrl}/agents/${agentId}/test`, body)
      .pipe(catchError(this.handleError));
  }

  /**
   * Send a test message in a session.
   */
  sendTestMessage(sessionId: string, message: string): Observable<TestMessageResult> {
    const body: TestMessageRequest = { message };

    return this.http
      .post<TestMessageResult>(`${this.baseUrl}/test-sessions/${sessionId}/message`, body)
      .pipe(catchError(this.handleError));
  }

  /**
   * Complete a test session with feedback.
   */
  completeTestSession(
    sessionId: string,
    feedback?: string,
    rating?: number
  ): Observable<AgentTestSession> {
    const body = { feedback, rating };

    return this.http
      .post<AgentTestSession>(`${this.baseUrl}/test-sessions/${sessionId}/complete`, body)
      .pipe(catchError(this.handleError));
  }

  /**
   * Cancel a test session.
   */
  cancelTestSession(sessionId: string): Observable<AgentTestSession> {
    return this.http
      .post<AgentTestSession>(`${this.baseUrl}/test-sessions/${sessionId}/cancel`, null)
      .pipe(catchError(this.handleError));
  }

  /**
   * Get test session details.
   */
  getTestSession(sessionId: string): Observable<AgentTestSession> {
    return this.http
      .get<AgentTestSession>(`${this.baseUrl}/test-sessions/${sessionId}`)
      .pipe(catchError(this.handleError));
  }

  /**
   * List test sessions for an agent.
   */
  listTestSessions(agentId: string, page = 0, size = 10): Observable<Page<AgentTestSession>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());

    return this.http
      .get<Page<AgentTestSession>>(`${this.baseUrl}/agents/${agentId}/test-sessions`, { params })
      .pipe(catchError(this.handleError));
  }

  // ============================================================================
  // TEMPLATES
  // ============================================================================

  /**
   * List available templates (tenant + system).
   */
  listTemplates(): Observable<PromptTemplate[]> {
    return this.http
      .get<PromptTemplate[]>(`${this.baseUrl}/templates/available`)
      .pipe(catchError(this.handleError));
  }

  /**
   * Get a template by ID.
   */
  getTemplate(id: string): Observable<PromptTemplate> {
    return this.http
      .get<PromptTemplate>(`${this.baseUrl}/templates/${id}`)
      .pipe(catchError(this.handleError));
  }

  /**
   * Create a new template.
   */
  createTemplate(template: Partial<PromptTemplate>): Observable<PromptTemplate> {
    return this.http
      .post<PromptTemplate>(`${this.baseUrl}/templates`, template)
      .pipe(catchError(this.handleError));
  }

  /**
   * Validate template syntax.
   */
  validateTemplate(content: string): Observable<TemplateValidationResult> {
    return this.http
      .post<TemplateValidationResult>(`${this.baseUrl}/templates/validate`, { content })
      .pipe(catchError(this.handleError));
  }

  /**
   * Render a template with variables.
   */
  renderTemplate(
    templateId: string,
    variables: Record<string, string>
  ): Observable<{ renderedContent: string }> {
    return this.http
      .post<{ renderedContent: string }>(
        `${this.baseUrl}/templates/${templateId}/render`,
        { variables }
      )
      .pipe(catchError(this.handleError));
  }

  // ============================================================================
  // RUNTIME METADATA (routes through gateway to agent-runtime-service)
  // ============================================================================

  /**
   * Get available tools from agent runtime.
   * Note: Uses /api/v1/tools which routes to agent-runtime-service
   */
  getAvailableTools(): Observable<ToolInfo[]> {
    return this.http
      .get<ToolInfo[]>('/api/v1/tools')
      .pipe(catchError(this.handleError));
  }

  /**
   * Get supported LLM providers.
   * Note: Uses /api/v1/providers which routes to agent-runtime-service
   */
  getSupportedProviders(): Observable<ProviderInfo[]> {
    return this.http
      .get<ProviderInfo[]>('/api/v1/providers')
      .pipe(catchError(this.handleError));
  }

  /**
   * Check agent runtime health.
   * Note: Uses /api/v1/runtime which routes to agent-runtime-service
   */
  checkRuntimeHealth(): Observable<Record<string, unknown>> {
    return this.http
      .get<Record<string, unknown>>('/api/v1/runtime/health')
      .pipe(catchError(this.handleError));
  }

  // ============================================================================
  // ERROR HANDLING
  // ============================================================================

  private handleError(error: unknown): Observable<never> {
    this.logger.error('AgentBuilderService error:', { error });
    return throwError(() => error);
  }
}
