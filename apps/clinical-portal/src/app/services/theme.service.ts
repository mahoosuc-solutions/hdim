import { Injectable, Renderer2, RendererFactory2, signal } from '@angular/core';

export type ThemeMode = 'light';

@Injectable({
  providedIn: 'root',
})
export class ThemeService {
  private renderer: Renderer2;
  private readonly THEME_KEY = 'healthdata-theme-preference';

  // Signal for reactive theme state
  public readonly currentTheme = signal<'light'>('light');
  public readonly themeMode = signal<ThemeMode>('light');

  constructor(rendererFactory: RendererFactory2) {
    this.renderer = rendererFactory.createRenderer(null, null);
  }

  /**
   * Initialize theme system - call this in app component
   */
  public initialize(): void {
    // Force light theme regardless of system preference.
    this.setThemeMode('light');
  }

  /**
   * Set theme mode: light only
   */
  public setThemeMode(mode: ThemeMode): void {
    this.themeMode.set(mode);
    localStorage.setItem(this.THEME_KEY, mode);
    this.setThemeClass('light');
  }

  /**
   * Toggle theme (no-op, light only)
   */
  public toggleTheme(): void {
    this.setThemeMode('light');
  }

  /**
   * Get the effective theme (resolves 'auto' to actual theme)
   */
  public getEffectiveTheme(): 'light' {
    return this.currentTheme();
  }

  /**
   * Check if system prefers dark mode
   */
  /**
   * Apply theme class to document
   */
  private setThemeClass(theme: 'light'): void {
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
