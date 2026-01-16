import { Renderer2, RendererFactory2 } from '@angular/core';
import { ThemeService } from './theme.service';

describe('ThemeService', () => {
  let service: ThemeService;
  let renderer: Renderer2;
  let rendererFactory: RendererFactory2;

  beforeEach(() => {
    renderer = {
      addClass: jest.fn(),
      removeClass: jest.fn(),
      setAttribute: jest.fn(),
    } as unknown as Renderer2;

    rendererFactory = {
      createRenderer: jest.fn(() => renderer),
    } as unknown as RendererFactory2;

    localStorage.clear();

    service = new ThemeService(rendererFactory);
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('initializes with saved mode and applies theme class', () => {
    localStorage.setItem('healthdata-theme-preference', 'light');
    service.initialize();

    expect(service.themeMode()).toBe('light');
    expect(service.currentTheme()).toBe('light');
    expect(renderer.addClass).toHaveBeenCalledWith(document.body, 'light-theme');
  });

  it('toggles theme and persists preference', () => {
    service.initialize();
    service.toggleTheme();

    expect(service.themeMode()).toBe('light');
    expect(localStorage.getItem('healthdata-theme-preference')).toBe('light');
  });

  it('returns effective theme and system preference', () => {
    service.setThemeMode('light');
    expect(service.getEffectiveTheme()).toBe('light');
  });
});
