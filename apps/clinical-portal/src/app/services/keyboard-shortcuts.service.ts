import { Injectable, OnDestroy, Inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { BehaviorSubject, Observable, Subject, fromEvent, filter, takeUntil } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { Router } from '@angular/router';

/**
 * Keyboard Shortcut Definition
 */
export interface KeyboardShortcut {
  id: string;
  key: string;
  modifiers: ('ctrl' | 'shift' | 'alt' | 'meta')[];
  description: string;
  category: ShortcutCategory;
  action: () => void;
  enabled: boolean;
  customizable: boolean;
}

/**
 * Shortcut Categories for organization
 */
export type ShortcutCategory =
  | 'navigation'
  | 'actions'
  | 'dialogs'
  | 'data'
  | 'help';

/**
 * Shortcut Category metadata
 */
export interface ShortcutCategoryInfo {
  id: ShortcutCategory;
  label: string;
  icon: string;
}

/**
 * User Shortcut Preferences
 */
interface ShortcutPreferences {
  enabled: boolean;
  customBindings: Record<string, { key: string; modifiers: string[] }>;
}

/**
 * Keyboard Shortcuts Service
 *
 * Provides global keyboard shortcut management for the clinical portal.
 *
 * Features:
 * - Global keyboard event handling
 * - Customizable shortcut bindings
 * - Category-based organization
 * - Enable/disable per shortcut
 * - Persistence via localStorage
 * - Conflict detection
 * - Help modal integration
 */
@Injectable({
  providedIn: 'root',
})
export class KeyboardShortcutsService implements OnDestroy {
  private readonly STORAGE_KEY = 'hdim_keyboard_shortcuts';
  private readonly destroy$ = new Subject<void>();
  private readonly isBrowser: boolean;

  private shortcuts: Map<string, KeyboardShortcut> = new Map();
  private shortcutsSubject = new BehaviorSubject<KeyboardShortcut[]>([]);
  readonly shortcuts$ = this.shortcutsSubject.asObservable();

  private enabledSubject = new BehaviorSubject<boolean>(true);
  readonly enabled$ = this.enabledSubject.asObservable();

  private helpDialogOpen = false;

  readonly categories: ShortcutCategoryInfo[] = [
    { id: 'navigation', label: 'Navigation', icon: 'navigation' },
    { id: 'actions', label: 'Quick Actions', icon: 'flash_on' },
    { id: 'dialogs', label: 'Dialogs', icon: 'open_in_new' },
    { id: 'data', label: 'Data & Refresh', icon: 'refresh' },
    { id: 'help', label: 'Help', icon: 'help' },
  ];

  constructor(
    @Inject(PLATFORM_ID) platformId: object,
    private router: Router,
    private dialog: MatDialog,
  ) {
    this.isBrowser = isPlatformBrowser(platformId);
    this.initializeDefaultShortcuts();
    this.loadPreferences();
    if (this.isBrowser) {
      this.setupGlobalListener();
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  /**
   * Initialize default keyboard shortcuts
   */
  private initializeDefaultShortcuts(): void {
    // Help shortcuts
    this.registerShortcut({
      id: 'help',
      key: '?',
      modifiers: [],
      description: 'Open keyboard shortcuts help',
      category: 'help',
      action: () => this.openHelpDialog(),
      enabled: true,
      customizable: false,
    });

    // Navigation shortcuts
    this.registerShortcut({
      id: 'nav-dashboard',
      key: 'd',
      modifiers: ['alt'],
      description: 'Go to Dashboard',
      category: 'navigation',
      action: () => this.router.navigate(['/dashboard']),
      enabled: true,
      customizable: true,
    });

    this.registerShortcut({
      id: 'nav-patients',
      key: 'p',
      modifiers: ['alt'],
      description: 'Go to Patients',
      category: 'navigation',
      action: () => this.router.navigate(['/patients']),
      enabled: true,
      customizable: true,
    });

    this.registerShortcut({
      id: 'nav-evaluations',
      key: 'e',
      modifiers: ['alt'],
      description: 'Go to Evaluations',
      category: 'navigation',
      action: () => this.router.navigate(['/evaluations']),
      enabled: true,
      customizable: true,
    });

    this.registerShortcut({
      id: 'nav-reports',
      key: 'r',
      modifiers: ['alt'],
      description: 'Go to Reports',
      category: 'navigation',
      action: () => this.router.navigate(['/reports']),
      enabled: true,
      customizable: true,
    });

    this.registerShortcut({
      id: 'nav-insights',
      key: 'i',
      modifiers: ['alt'],
      description: 'Go to Insights',
      category: 'navigation',
      action: () => this.router.navigate(['/insights']),
      enabled: true,
      customizable: true,
    });

    // Data shortcuts
    this.registerShortcut({
      id: 'refresh',
      key: 'r',
      modifiers: ['ctrl'],
      description: 'Refresh current page data',
      category: 'data',
      action: () => this.emitRefresh(),
      enabled: true,
      customizable: true,
    });

    this.registerShortcut({
      id: 'focus-search',
      key: 'f',
      modifiers: ['ctrl'],
      description: 'Focus search input',
      category: 'data',
      action: () => this.focusSearch(),
      enabled: true,
      customizable: true,
    });

    // Quick Actions
    this.registerShortcut({
      id: 'quick-action-1',
      key: '1',
      modifiers: ['ctrl'],
      description: 'Quick Action: Order Lab',
      category: 'actions',
      action: () => this.emitQuickAction('order-lab'),
      enabled: true,
      customizable: true,
    });

    this.registerShortcut({
      id: 'quick-action-2',
      key: '2',
      modifiers: ['ctrl'],
      description: 'Quick Action: Schedule Visit',
      category: 'actions',
      action: () => this.emitQuickAction('schedule-visit'),
      enabled: true,
      customizable: true,
    });

    this.registerShortcut({
      id: 'quick-action-3',
      key: '3',
      modifiers: ['ctrl'],
      description: 'Quick Action: Send Message',
      category: 'actions',
      action: () => this.emitQuickAction('send-message'),
      enabled: true,
      customizable: true,
    });

    this.registerShortcut({
      id: 'sign-result',
      key: 's',
      modifiers: ['ctrl'],
      description: 'Sign selected result',
      category: 'actions',
      action: () => this.emitAction('sign-result'),
      enabled: true,
      customizable: true,
    });

    this.registerShortcut({
      id: 'sign-all-normal',
      key: 'a',
      modifiers: ['ctrl', 'shift'],
      description: 'Sign all normal results',
      category: 'actions',
      action: () => this.emitAction('sign-all-normal'),
      enabled: true,
      customizable: true,
    });

    // Dialog shortcuts
    this.registerShortcut({
      id: 'close-dialog',
      key: 'Escape',
      modifiers: [],
      description: 'Close current dialog',
      category: 'dialogs',
      action: () => this.closeCurrentDialog(),
      enabled: true,
      customizable: false,
    });

    this.registerShortcut({
      id: 'new-report',
      key: 'n',
      modifiers: ['ctrl'],
      description: 'Create new report',
      category: 'dialogs',
      action: () => this.router.navigate(['/report-builder']),
      enabled: true,
      customizable: true,
    });

    this.emitShortcuts();
  }

  /**
   * Register a new keyboard shortcut
   */
  registerShortcut(shortcut: KeyboardShortcut): void {
    this.shortcuts.set(shortcut.id, shortcut);
    this.emitShortcuts();
  }

  /**
   * Unregister a keyboard shortcut
   */
  unregisterShortcut(id: string): void {
    this.shortcuts.delete(id);
    this.emitShortcuts();
  }

  /**
   * Get all shortcuts
   */
  getShortcuts(): KeyboardShortcut[] {
    return Array.from(this.shortcuts.values());
  }

  /**
   * Get shortcuts by category
   */
  getShortcutsByCategory(category: ShortcutCategory): KeyboardShortcut[] {
    return this.getShortcuts().filter(s => s.category === category);
  }

  /**
   * Get shortcut by ID
   */
  getShortcut(id: string): KeyboardShortcut | undefined {
    return this.shortcuts.get(id);
  }

  /**
   * Enable/disable all shortcuts
   */
  setEnabled(enabled: boolean): void {
    this.enabledSubject.next(enabled);
  }

  /**
   * Enable/disable a specific shortcut
   */
  setShortcutEnabled(id: string, enabled: boolean): void {
    const shortcut = this.shortcuts.get(id);
    if (shortcut) {
      shortcut.enabled = enabled;
      this.emitShortcuts();
      this.savePreferences();
    }
  }

  /**
   * Update shortcut binding
   */
  updateBinding(id: string, key: string, modifiers: ('ctrl' | 'shift' | 'alt' | 'meta')[]): boolean {
    const shortcut = this.shortcuts.get(id);
    if (!shortcut || !shortcut.customizable) {
      return false;
    }

    // Check for conflicts
    const conflict = this.findConflict(key, modifiers, id);
    if (conflict) {
      return false;
    }

    shortcut.key = key;
    shortcut.modifiers = modifiers;
    this.emitShortcuts();
    this.savePreferences();
    return true;
  }

  /**
   * Reset shortcut to default
   */
  resetToDefault(id: string): void {
    // Re-initialize the specific shortcut
    this.initializeDefaultShortcuts();
    this.savePreferences();
  }

  /**
   * Reset all shortcuts to defaults
   */
  resetAllToDefaults(): void {
    this.shortcuts.clear();
    this.initializeDefaultShortcuts();
    localStorage.removeItem(this.STORAGE_KEY);
  }

  /**
   * Format shortcut for display
   */
  formatShortcut(shortcut: KeyboardShortcut): string {
    const parts: string[] = [];

    if (shortcut.modifiers.includes('ctrl')) parts.push('Ctrl');
    if (shortcut.modifiers.includes('alt')) parts.push('Alt');
    if (shortcut.modifiers.includes('shift')) parts.push('Shift');
    if (shortcut.modifiers.includes('meta')) parts.push('Cmd');

    // Format key
    let keyDisplay = shortcut.key;
    if (keyDisplay === ' ') keyDisplay = 'Space';
    else if (keyDisplay === 'Escape') keyDisplay = 'Esc';
    else if (keyDisplay.length === 1) keyDisplay = keyDisplay.toUpperCase();

    parts.push(keyDisplay);

    return parts.join('+');
  }

  /**
   * Check if shortcuts are globally enabled
   */
  isEnabled(): boolean {
    return this.enabledSubject.getValue();
  }

  /**
   * Open help dialog - to be implemented by components
   */
  private helpDialogCallback: (() => void) | null = null;

  setHelpDialogCallback(callback: () => void): void {
    this.helpDialogCallback = callback;
  }

  private openHelpDialog(): void {
    if (this.helpDialogCallback) {
      this.helpDialogCallback();
    }
  }

  // Event emitters for component integration
  private refreshSubject = new Subject<void>();
  readonly refresh$ = this.refreshSubject.asObservable();

  private quickActionSubject = new Subject<string>();
  readonly quickAction$ = this.quickActionSubject.asObservable();

  private actionSubject = new Subject<string>();
  readonly action$ = this.actionSubject.asObservable();

  private emitRefresh(): void {
    this.refreshSubject.next();
  }

  private emitQuickAction(action: string): void {
    this.quickActionSubject.next(action);
  }

  private emitAction(action: string): void {
    this.actionSubject.next(action);
  }

  private focusSearch(): void {
    const searchInput = document.querySelector('input[type="search"], input[placeholder*="Search"]') as HTMLInputElement;
    if (searchInput) {
      searchInput.focus();
    }
  }

  private closeCurrentDialog(): void {
    this.dialog.closeAll();
  }

  /**
   * Setup global keyboard event listener
   */
  private setupGlobalListener(): void {
    fromEvent<KeyboardEvent>(document, 'keydown')
      .pipe(
        takeUntil(this.destroy$),
        filter(() => this.enabledSubject.getValue()),
        filter(event => !this.isInputFocused(event)),
      )
      .subscribe(event => this.handleKeyDown(event));
  }

  /**
   * Check if an input element is focused
   */
  private isInputFocused(event: KeyboardEvent): boolean {
    const target = event.target as HTMLElement;
    const tagName = target.tagName.toLowerCase();

    // Allow shortcuts in inputs if modifier key is pressed (except for text editing shortcuts)
    if (tagName === 'input' || tagName === 'textarea' || target.isContentEditable) {
      // Allow help shortcut even in inputs
      if (event.key === '?' && !event.ctrlKey && !event.altKey) {
        return false;
      }
      // Allow Escape even in inputs
      if (event.key === 'Escape') {
        return false;
      }
      // Block other shortcuts in inputs
      return true;
    }

    return false;
  }

  /**
   * Handle keyboard event
   */
  private handleKeyDown(event: KeyboardEvent): void {
    const matchingShortcut = this.findMatchingShortcut(event);

    if (matchingShortcut && matchingShortcut.enabled) {
      event.preventDefault();
      event.stopPropagation();
      matchingShortcut.action();
    }
  }

  /**
   * Find matching shortcut for event
   */
  private findMatchingShortcut(event: KeyboardEvent): KeyboardShortcut | undefined {
    for (const shortcut of this.shortcuts.values()) {
      if (this.eventMatchesShortcut(event, shortcut)) {
        return shortcut;
      }
    }
    return undefined;
  }

  /**
   * Check if event matches shortcut
   */
  private eventMatchesShortcut(event: KeyboardEvent, shortcut: KeyboardShortcut): boolean {
    // Check key
    const keyMatches =
      event.key.toLowerCase() === shortcut.key.toLowerCase() ||
      event.key === shortcut.key;

    if (!keyMatches) return false;

    // Check modifiers
    const ctrlRequired = shortcut.modifiers.includes('ctrl');
    const altRequired = shortcut.modifiers.includes('alt');
    const shiftRequired = shortcut.modifiers.includes('shift');
    const metaRequired = shortcut.modifiers.includes('meta');

    const ctrlPressed = event.ctrlKey || event.metaKey; // Treat Cmd as Ctrl on Mac
    const altPressed = event.altKey;
    const shiftPressed = event.shiftKey;

    return (
      ctrlRequired === ctrlPressed &&
      altRequired === altPressed &&
      shiftRequired === shiftPressed &&
      (!metaRequired || event.metaKey)
    );
  }

  /**
   * Find conflicting shortcut
   */
  private findConflict(key: string, modifiers: string[], excludeId?: string): KeyboardShortcut | undefined {
    for (const shortcut of this.shortcuts.values()) {
      if (excludeId && shortcut.id === excludeId) continue;

      const keyMatches = shortcut.key.toLowerCase() === key.toLowerCase();
      const modifiersMatch =
        shortcut.modifiers.length === modifiers.length &&
        shortcut.modifiers.every(m => modifiers.includes(m));

      if (keyMatches && modifiersMatch) {
        return shortcut;
      }
    }
    return undefined;
  }

  /**
   * Emit shortcuts update
   */
  private emitShortcuts(): void {
    this.shortcutsSubject.next(Array.from(this.shortcuts.values()));
  }

  /**
   * Load preferences from localStorage
   */
  private loadPreferences(): void {
    if (!this.isBrowser) return;

    try {
      const stored = localStorage.getItem(this.STORAGE_KEY);
      if (!stored) return;

      const prefs: ShortcutPreferences = JSON.parse(stored);
      this.enabledSubject.next(prefs.enabled);

      // Apply custom bindings
      for (const [id, binding] of Object.entries(prefs.customBindings)) {
        const shortcut = this.shortcuts.get(id);
        if (shortcut && shortcut.customizable) {
          shortcut.key = binding.key;
          shortcut.modifiers = binding.modifiers as ('ctrl' | 'shift' | 'alt' | 'meta')[];
        }
      }

      this.emitShortcuts();
    } catch {
      // Ignore parse errors
    }
  }

  /**
   * Save preferences to localStorage
   */
  private savePreferences(): void {
    if (!this.isBrowser) return;

    const customBindings: Record<string, { key: string; modifiers: string[] }> = {};

    for (const [id, shortcut] of this.shortcuts.entries()) {
      if (shortcut.customizable) {
        customBindings[id] = {
          key: shortcut.key,
          modifiers: shortcut.modifiers,
        };
      }
    }

    const prefs: ShortcutPreferences = {
      enabled: this.enabledSubject.getValue(),
      customBindings,
    };

    localStorage.setItem(this.STORAGE_KEY, JSON.stringify(prefs));
  }
}
