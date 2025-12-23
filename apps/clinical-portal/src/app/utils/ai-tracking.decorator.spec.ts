import { TestBed } from '@angular/core/testing';
import { TrackInteraction, createInteractionTracker, trackClick, WithAITracking } from './ai-tracking.decorator';
import { AIAssistantService } from '../services/ai-assistant.service';

class TestComponent {
  aiAssistant = { trackInteraction: jest.fn() };

  @TrackInteraction('test-component', 'sync-action')
  doSync() {
    return 'ok';
  }

  @TrackInteraction('test-component', 'async-action')
  async doAsync() {
    return 'async-ok';
  }

  @TrackInteraction('test-component', 'error-action')
  doError() {
    throw new Error('fail');
  }

  @TrackInteraction('test-component', 'async-error')
  async doAsyncError() {
    throw new Error('async-fail');
  }
}

describe('AI tracking decorators', () => {
  it('tracks sync and async interactions', async () => {
    const component = new TestComponent();

    await expect(component.doSync('arg1', 'arg2')).resolves.toBe('ok');
    await expect(component.doAsync()).resolves.toBe('async-ok');

    expect(component.aiAssistant.trackInteraction).toHaveBeenCalled();
    const hasArgCount = component.aiAssistant.trackInteraction.mock.calls.some(
      ([payload]) => payload?.context?.args === 2
    );
    expect(hasArgCount).toBe(true);
  });

  it('tracks error interactions and rethrows', async () => {
    const component = new TestComponent();

    await expect(component.doError()).rejects.toThrow('fail');
    expect(component.aiAssistant.trackInteraction).toHaveBeenCalled();
  });

  it('tracks async errors with failure metadata', async () => {
    const component = new TestComponent();

    await expect(component.doAsyncError()).rejects.toThrow('async-fail');

    const lastCall = component.aiAssistant.trackInteraction.mock.calls.at(-1)?.[0];
    expect(lastCall?.success).toBe(false);
    expect(lastCall?.errorMessage).toBe('async-fail');
  });

  it('tracks rejected promise results', async () => {
    class PromiseRejector {
      aiAssistant = { trackInteraction: jest.fn() };

      @TrackInteraction('test-component', 'reject-action')
      doReject() {
        return Promise.reject(new Error('reject'));
      }
    }

    const component = new PromiseRejector();

    await expect(component.doReject()).rejects.toThrow('reject');

    const lastCall = component.aiAssistant.trackInteraction.mock.calls.at(-1)?.[0];
    expect(lastCall?.success).toBe(false);
    expect(lastCall?.errorMessage).toBe('reject');
  });

  it('uses Unknown error message when error lacks message', async () => {
    class NoMessageComponent {
      aiAssistant = { trackInteraction: jest.fn() };

      @TrackInteraction('test-component', 'sync-no-message')
      doSyncNoMessage() {
        throw {};
      }

      @TrackInteraction('test-component', 'async-no-message')
      async doAsyncNoMessage() {
        throw {};
      }
    }

    const component = new NoMessageComponent();

    await expect(component.doSyncNoMessage()).rejects.toBeDefined();
    await expect(component.doAsyncNoMessage()).rejects.toBeDefined();

    const lastCall = component.aiAssistant.trackInteraction.mock.calls.at(-1)?.[0];
    expect(lastCall?.errorMessage).toBe('Unknown error');
    expect(lastCall?.success).toBe(false);
  });

  it('handles missing ai assistant gracefully', async () => {
    const component = new TestComponent();
    (component as any).aiAssistant = undefined;

    await expect(component.doSync()).resolves.toBe('ok');
  });

  it('ignores ai assistant without trackInteraction method', async () => {
    const component = new TestComponent();
    (component as any).aiAssistant = {};

    await expect(component.doSync()).resolves.toBe('ok');
  });

  it('handles tracking errors without breaking flow', async () => {
    const component = new TestComponent();
    (component as any).aiAssistant = {
      trackInteraction: jest.fn(() => {
        throw new Error('tracking-fail');
      }),
    };

    await expect(component.doSync()).resolves.toBe('ok');
  });

  it('injects ai assistant with WithAITracking decorator', () => {
    const mockService = { trackInteraction: jest.fn() };
    TestBed.configureTestingModule({
      providers: [{ provide: AIAssistantService, useValue: mockService }],
    });

    const Decorated = WithAITracking(class {});
    const instance = TestBed.runInInjectionContext(() => new Decorated());

    expect(instance.aiAssistant).toBe(mockService);
  });

  it('creates interaction trackers and tracks clicks', () => {
    const aiService = { trackInteraction: jest.fn() } as any;
    const tracker = createInteractionTracker(aiService, 'component', 'action');

    tracker.complete(false, 'oops');
    trackClick(aiService, 'component', 'click');

    expect(aiService.trackInteraction).toHaveBeenCalledTimes(2);
    expect(aiService.trackInteraction.mock.calls[0][0]).toEqual(
      expect.objectContaining({ success: false, errorMessage: 'oops' })
    );
  });
});
