import { Injectable, Renderer2, RendererFactory2, signal } from '@angular/core';

export type ThemeMode = 'light' | 'dark' | 'auto';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private renderer: Renderer2;
  private readonly THEME_KEY = 'healthdata-theme-preference';

  // Signal for reactive theme state
  public readonly currentTheme = signal<'light' | 'dark'>('light');
  public readonly themeMode = signal<ThemeMode>('auto');

  private mediaQuery: MediaQueryList;

  constructor(rendererFactory: RendererFactory2) {
    this.renderer = rendererFactory.createRenderer(null, null);
    this.mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
  }

  /**
   * Initialize theme system - call this in app component
   */
  public initialize(): void {
    // Load saved preference or default to 'light' for better readability
    const saved = localStorage.getItem(this.THEME_KEY) as ThemeMode | null;
    const mode = saved || 'light';
    this.themeMode.set(mode);

    // Apply theme based on preference
    this.applyTheme(mode);

    // Listen for system theme changes (for 'auto' mode)
    this.mediaQuery.addEventListener('change', (e) => {
      if (this.themeMode() === 'auto') {
        this.setThemeClass(e.matches ? 'dark' : 'light');
      }
    });
  }

  /**
   * Set theme mode: 'light', 'dark', or 'auto'
   */
  public setThemeMode(mode: ThemeMode): void {
    this.themeMode.set(mode);
    localStorage.setItem(this.THEME_KEY, mode);
    this.applyTheme(mode);
  }

  /**
   * Toggle between light and dark (switches to manual mode)
   */
  public toggleTheme(): void {
    const newTheme = this.currentTheme() === 'light' ? 'dark' : 'light';
    this.setThemeMode(newTheme);
  }

  /**
   * Get the effective theme (resolves 'auto' to actual theme)
   */
  public getEffectiveTheme(): 'light' | 'dark' {
    return this.currentTheme();
  }

  /**
   * Check if system prefers dark mode
   */
  public systemPrefersDark(): boolean {
    return this.mediaQuery.matches;
  }

  /**
   * Apply theme based on mode
   */
  private applyTheme(mode: ThemeMode): void {
    let effectiveTheme: 'light' | 'dark';

    if (mode === 'auto') {
      effectiveTheme = this.systemPrefersDark() ? 'dark' : 'light';
    } else {
      effectiveTheme = mode;
    }

    this.setThemeClass(effectiveTheme);
  }

  /**
   * Apply theme class to document
   */
  private setThemeClass(theme: 'light' | 'dark'): void {
    this.currentTheme.set(theme);

    // Remove both theme classes
    this.renderer.removeClass(document.body, 'light-theme');
    this.renderer.removeClass(document.body, 'dark-theme');

    // Add the active theme class
    this.renderer.addClass(document.body, `${theme}-theme`);

    // Also set data attribute for CSS targeting
    this.renderer.setAttribute(document.body, 'data-theme', theme);
  }
}
