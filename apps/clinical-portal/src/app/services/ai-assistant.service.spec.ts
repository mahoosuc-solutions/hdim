import { AIAssistantService, UserInteraction } from './ai-assistant.service';

const createInteraction = (overrides: Partial<UserInteraction> = {}): UserInteraction => ({
  timestamp: new Date('2024-01-01T00:00:00Z'),
  component: 'dashboard',
  action: 'click',
  duration: 1500,
  success: true,
  ...overrides,
});

describe('AIAssistantService', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  it('initializes with stored interactions and a system message', () => {
    localStorage.setItem('ai_interactions', JSON.stringify([createInteraction()]));

    const service = new AIAssistantService({} as any, {} as any);
    const interactions = (service as any).interactions as UserInteraction[];

    expect(interactions.length).toBe(1);
    expect((service as any).messagesSubject.value.length).toBeGreaterThan(0);
  });

  it('tracks interactions only when active and triggers auto analysis', async () => {
    const service = new AIAssistantService({} as any, {} as any);
    const analyzeSpy = jest.spyOn(service, 'analyzeInteractions').mockResolvedValue([]);

    service.trackInteraction({ component: 'dashboard', action: 'view', success: true });
    expect((service as any).interactions.length).toBe(0);

    service.activateAgent();
    service.enableAutoAnalysis();

    for (let i = 0; i < 50; i += 1) {
      service.trackInteraction({ component: 'dashboard', action: 'view', success: true });
    }

    expect((service as any).interactions.length).toBe(50);
    expect(analyzeSpy).toHaveBeenCalled();
  });

  it('does not auto-analyze when disabled and respects max interactions', () => {
    const service = new AIAssistantService({} as any, {} as any);
    service.activateAgent();

    for (let i = 0; i < 1100; i += 1) {
      service.trackInteraction({ component: 'dashboard', action: 'view', success: true });
    }

    const interactions = (service as any).interactions as UserInteraction[];
    expect(interactions.length).toBeLessThanOrEqual(1000);
  });

  it('deactivates agent and logs system message', () => {
    const service = new AIAssistantService({} as any, {} as any);
    service.activateAgent();
    service.deactivateAgent();

    const messages = (service as any).messagesSubject.value as any[];
    expect(messages[messages.length - 1].content).toContain('deactivated');
  });

  it('sends messages and falls back on error', async () => {
    const service = new AIAssistantService({} as any, {} as any);

    jest.spyOn(service as any, 'callAIBackend').mockResolvedValue('Hello');
    const response = await service.sendMessage('Hello');
    expect(response.role).toBe('assistant');
    expect(response.content).toBe('Hello');

    jest.spyOn(service as any, 'callAIBackend').mockRejectedValue(new Error('Boom'));
    const fallback = await service.sendMessage('Test failure');
    expect(fallback.role).toBe('assistant');
    expect(fallback.content.length).toBeGreaterThan(0);
  });

  it('returns fallback responses for specific prompts', () => {
    const service = new AIAssistantService({} as any, {} as any);

    const uiResponse = (service as any).getFallbackResponse('Improve UI');
    expect(uiResponse).toContain('Add Loading States');

    const a11yResponse = (service as any).getFallbackResponse('Accessibility guidance');
    expect(a11yResponse).toContain('ARIA');

    const testResponse = (service as any).getFallbackResponse('Testing help');
    expect(testResponse).toContain('E2E');
  });

  it('analyzes interactions and returns recommendations', async () => {
    const service = new AIAssistantService({} as any, {} as any);

    (service as any).interactions = [
      createInteraction({ component: 'reports', success: false, duration: 4500 }),
      createInteraction({ component: 'reports', success: false, duration: 5200 }),
      createInteraction({ component: 'dashboard', success: true, duration: 1000 }),
      createInteraction({ component: 'patients', success: true, duration: 1200 }),
      createInteraction({ component: 'rare-component', success: true, duration: 900 }),
    ];

    const analyses = await service.analyzeInteractions();
    expect(analyses.length).toBeGreaterThan(0);
    expect((service as any).analysisSubject.value.length).toBeGreaterThan(0);
  });

  it('returns empty analysis when no interactions', async () => {
    const service = new AIAssistantService({} as any, {} as any);
    (service as any).interactions = [];

    const analyses = await service.analyzeInteractions();

    expect(analyses).toEqual([]);
  });

  it('generates component suggestions based on interaction history', async () => {
    const service = new AIAssistantService({} as any, {} as any);

    (service as any).interactions = Array.from({ length: 12 }, (_, index) =>
      createInteraction({
        component: 'measure-builder',
        success: index % 3 !== 0,
        duration: 6000,
      })
    );

    const suggestions = await service.getComponentSuggestions('measure-builder');
    expect(suggestions).toContain('AI Analysis for measure-builder');
  });

  it('returns default suggestions when no component interactions exist', async () => {
    const service = new AIAssistantService({} as any, {} as any);

    const suggestions = await service.getComponentSuggestions('unknown-component');
    expect(suggestions).toContain('No interaction data available');
  });

  it('clears and exports interactions', () => {
    const service = new AIAssistantService({} as any, {} as any);
    (service as any).interactions = [createInteraction()];

    const exported = service.exportInteractions();
    expect(exported).toContain('dashboard');

    service.clearInteractions();
    expect((service as any).interactions.length).toBe(0);
  });

  it('imports interactions with error handling', () => {
    const service = new AIAssistantService({} as any, {} as any);

    service.importInteractions('[{"component":"dashboard","action":"view","success":true}]');
    expect((service as any).interactions.length).toBe(1);

    service.importInteractions('not-json');
    expect((service as any).messagesSubject.value.length).toBeGreaterThan(0);
  });

  it('calculates average duration for interactions', () => {
    const service = new AIAssistantService({} as any, {} as any);
    const avg = (service as any).calculateAverageDuration([
      createInteraction({ duration: 2000 }),
      createInteraction({ duration: 4000 }),
    ]);

    expect(avg).toBe(3);
  });

  it('handles average duration with no valid durations', () => {
    const service = new AIAssistantService({} as any, {} as any);
    const avg = (service as any).calculateAverageDuration([
      createInteraction({ duration: 0 }),
    ]);

    expect(avg).toBe(0);
  });
});
