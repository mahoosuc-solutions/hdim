/**
 * Agent Builder TypeScript Models
 * Matches backend entities from agent-builder-service
 */

// Agent Status enum matching backend
export type AgentStatus = 'DRAFT' | 'TESTING' | 'ACTIVE' | 'DEPRECATED' | 'ARCHIVED';

// Model providers
export type ModelProvider = 'claude' | 'azure-openai' | 'bedrock';

// Template categories
export type TemplateCategory =
  | 'SYSTEM_PROMPT'
  | 'CAPABILITIES'
  | 'CONSTRAINTS'
  | 'RESPONSE_FORMAT'
  | 'CLINICAL_SAFETY'
  | 'TOOL_USAGE'
  | 'PERSONA'
  | 'CUSTOM';

// Tool configuration for an agent
export interface ToolConfig {
  toolName: string;
  enabled: boolean;
  configuration?: Record<string, unknown>;
}

// Guardrail configuration
export interface GuardrailConfig {
  phiFiltering: boolean;
  clinicalDisclaimerRequired: boolean;
  blockedPatterns: string[];
  maxOutputTokens: number;
  requireHumanReview: boolean;
  riskThreshold?: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
}

// UI configuration for agent widget
export interface UiConfig {
  position?: 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left';
  primaryColor?: string;
  headerTitle?: string;
  showAvatar?: boolean;
}

// Main Agent Configuration entity
export interface AgentConfiguration {
  id: string;
  tenantId: string;
  name: string;
  slug: string;
  description?: string;
  version: string;
  status: AgentStatus;

  // Persona
  personaName?: string;
  personaRole?: string;
  personaAvatarUrl?: string;

  // Model settings
  modelProvider: ModelProvider;
  modelId?: string;
  maxTokens?: number;
  temperature?: number;

  // Prompts
  systemPrompt: string;
  welcomeMessage?: string;

  // Configuration
  toolConfiguration?: ToolConfig[];
  guardrailConfiguration?: GuardrailConfig;
  uiConfiguration?: UiConfig;

  // Access control
  allowedRoles?: string[];
  requiresPatientContext?: boolean;

  // Metadata
  tags?: string[];
  createdBy: string;
  updatedBy?: string;
  createdAt: string;
  updatedAt?: string;
  publishedAt?: string;
  archivedAt?: string;
}

// Version status
export type VersionStatus = 'DRAFT' | 'PUBLISHED' | 'ROLLED_BACK' | 'SUPERSEDED';

// Change type for versions
export type ChangeType = 'MAJOR' | 'MINOR' | 'PATCH';

// Agent Version entity
export interface AgentVersion {
  id: string;
  agentConfigurationId: string;
  versionNumber: string;
  configurationSnapshot: Record<string, unknown>;
  status: VersionStatus;
  changeSummary?: string;
  changeType: ChangeType;
  createdBy: string;
  createdAt: string;
  publishedAt?: string;
  publishedBy?: string;
  rolledBackAt?: string;
  rolledBackBy?: string;
  rollbackReason?: string;
}

// Template variable
export interface TemplateVariable {
  name: string;
  description?: string;
  defaultValue?: string;
  required: boolean;
}

// Prompt Template entity
export interface PromptTemplate {
  id: string;
  tenantId: string;
  name: string;
  description?: string;
  category: TemplateCategory;
  content: string;
  variables?: TemplateVariable[];
  usageCount?: number;
  isSystem: boolean;
  createdBy: string;
  createdAt: string;
  updatedAt?: string;
}

// Test session status
export type TestStatus = 'IN_PROGRESS' | 'COMPLETED' | 'FAILED' | 'CANCELLED';

// Test message
export interface TestMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
  timestamp: string;
  latencyMs?: number;
}

// Tool invocation during test
export interface ToolInvocation {
  name: string;
  arguments: Record<string, unknown>;
  result?: string;
  success: boolean;
  durationMs: number;
}

// Test metrics
export interface TestMetrics {
  totalMessages: number;
  toolInvocations: number;
  totalTokens: number;
  avgLatencyMs: number;
  guardrailTriggers: number;
}

// Agent Test Session entity
export interface AgentTestSession {
  id: string;
  agentConfigurationId: string;
  tenantId: string;
  testType: 'INTERACTIVE' | 'AUTOMATED' | 'SCENARIO';
  testScenario?: string;
  startedAt: string;
  completedAt?: string;
  status: TestStatus;
  messages: TestMessage[];
  toolInvocations: ToolInvocation[];
  metrics?: TestMetrics;
  testerFeedback?: string;
  testerRating?: number;
  createdBy: string;
}

// Tool definition from runtime
export interface ToolInfo {
  name: string;
  displayName: string;
  description: string;
  category: string;
  inputSchema: Record<string, unknown>;
  requiresApproval: boolean;
  requiresPatientContext: boolean;
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
}

// Provider info from runtime
export interface ProviderInfo {
  name: string;
  displayName: string;
  available: boolean;
  hipaaCompliant: boolean;
  regions: string[];
  models: ModelInfo[];
}

// Model info
export interface ModelInfo {
  id: string;
  name: string;
  contextWindow: number;
  maxOutputTokens: number;
  costPerMillionTokens?: number;
}

// API Request/Response types
export interface CreateAgentRequest {
  name: string;
  description?: string;
  personaName?: string;
  personaRole?: string;
  modelProvider: ModelProvider;
  modelId?: string;
  systemPrompt: string;
  welcomeMessage?: string;
  toolConfiguration?: ToolConfig[];
  guardrailConfiguration?: GuardrailConfig;
  tags?: string[];
}

export interface UpdateAgentRequest {
  name?: string;
  description?: string;
  personaName?: string;
  personaRole?: string;
  modelProvider?: ModelProvider;
  modelId?: string;
  maxTokens?: number;
  temperature?: number;
  systemPrompt?: string;
  welcomeMessage?: string;
  toolConfiguration?: ToolConfig[];
  guardrailConfiguration?: GuardrailConfig;
  uiConfiguration?: UiConfig;
  allowedRoles?: string[];
  tags?: string[];
}

export interface TestMessageRequest {
  message: string;
}

export interface TestMessageResult {
  success: boolean;
  content?: string;
  latencyMs: number;
  error?: string;
}

// Paginated response
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

// Template validation result
export interface TemplateValidationResult {
  valid: boolean;
  variables: string[];
  errors: string[];
  warnings: string[];
}
