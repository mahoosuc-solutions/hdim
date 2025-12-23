import { Renderer2, RendererFactory2 } from '@angular/core';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;
  let renderer: Renderer2;
  let rendererFactory: RendererFactory2;
  let mediaQuery: MediaQueryList;
  let mediaListener: ((event: MediaQueryListEvent) => void) | null = null;

  beforeEach(() => {
    renderer = {
      addClass: jest.fn(),
      removeClass: jest.fn(),
      setAttribute: jest.fn(),
    } as unknown as Renderer2;

    rendererFactory = {
      createRenderer: jest.fn(() => renderer),
    } as unknown as RendererFactory2;

    mediaQuery = {
      matches: false,
      addEventListener: jest.fn((_, cb) => {
        mediaListener = cb as (event: MediaQueryListEvent) => void;
      }),
    } as unknown as MediaQueryList;

    Object.defineProperty(window, 'matchMedia', {
      writable: true,
      value: jest.fn().mockReturnValue(mediaQuery),
    });
    localStorage.clear();

    service = new ThemeService(rendererFactory);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('initializes with saved mode and applies theme class', () => {
    localStorage.setItem('healthdata-theme-preference', 'dark');
    service.initialize();

    expect(service.themeMode()).toBe('dark');
    expect(service.currentTheme()).toBe('dark');
    expect(renderer.addClass).toHaveBeenCalledWith(document.body, 'dark-theme');
  });

  it('applies auto mode based on system preference changes', () => {
    mediaQuery.matches = true;
    localStorage.setItem('healthdata-theme-preference', 'auto');
    service.initialize();

    expect(service.currentTheme()).toBe('dark');
    mediaListener?.({ matches: false } as MediaQueryListEvent);
    expect(service.currentTheme()).toBe('light');
  });

  it('toggles theme and persists preference', () => {
    service.initialize();
    service.toggleTheme();

    expect(service.themeMode()).toBe('dark');
    expect(localStorage.getItem('healthdata-theme-preference')).toBe('dark');
  });

  it('returns effective theme and system preference', () => {
    expect(service.systemPrefersDark()).toBe(false);
    service.setThemeMode('dark');
    expect(service.getEffectiveTheme()).toBe('dark');
  });
});
