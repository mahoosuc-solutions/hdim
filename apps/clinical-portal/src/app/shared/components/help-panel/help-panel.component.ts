import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

/**
 * Help Panel Component
 *
 * Displays a sliding help panel with detailed information, FAQs, and quick tips
 *
 * Usage:
 * <app-help-panel
 *   [isOpen]="showHelp"
 *   [title]="'Measure Builder Help'"
 *   [sections]="helpSections"
 *   (close)="showHelp = false">
 * </app-help-panel>
 */

export interface HelpSection {
  title: string;
  content: string;
  type?: 'info' | 'warning' | 'tip' | 'video';
  icon?: string;
  link?: { text: string; url: string };
}

@Component({
  selector: 'app-help-panel',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="help-panel-overlay"
         *ngIf="isOpen"
         (click)="onClose()"
         [@fadeIn]></div>

    <aside
      class="help-panel"
      [class.open]="isOpen"
      [@slideIn]
      role="complementary"
      aria-label="Help panel">

      <div class="help-panel-header">
        <div class="header-content">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" class="help-icon">
            <circle cx="12" cy="12" r="10" stroke-width="2"/>
            <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3" stroke-width="2" stroke-linecap="round"/>
            <circle cx="12" cy="17" r="1" fill="currentColor"/>
          </svg>
          <h2>{{ title }}</h2>
        </div>
        <button
          type="button"
          class="close-button"
          (click)="onClose()"
          aria-label="Close help panel">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor">
            <path d="M18 6L6 18M6 6l12 12" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
      </div>

      <div class="help-panel-content">
        <!-- Search Box -->
        <div class="search-box" *ngIf="searchable">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor" class="search-icon">
            <path fill-rule="evenodd" d="M8 4a4 4 0 100 8 4 4 0 000-8zM2 8a6 6 0 1110.89 3.476l4.817 4.817a1 1 0 01-1.414 1.414l-4.816-4.816A6 6 0 012 8z" clip-rule="evenodd"/>
          </svg>
          <input
            type="text"
            placeholder="Search help topics..."
            [(ngModel)]="searchQuery"
            (input)="onSearch()"
            class="search-input">
        </div>

        <!-- Help Sections -->
        <div class="sections-container">
          <div
            *ngFor="let section of filteredSections; let i = index"
            class="help-section"
            [class]="'section-' + (section.type || 'info')">

            <div class="section-header" (click)="toggleSection(i)">
              <div class="section-title">
                <span class="section-icon" [innerHTML]="getSectionIcon(section.type)"></span>
                <h3>{{ section.title }}</h3>
              </div>
              <svg
                width="20"
                height="20"
                viewBox="0 0 20 20"
                fill="currentColor"
                class="chevron"
                [class.rotated]="expandedSections[i]">
                <path fill-rule="evenodd" d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z" clip-rule="evenodd"/>
              </svg>
            </div>

            <div class="section-content" *ngIf="expandedSections[i]">
              <div [innerHTML]="section.content"></div>
              <a
                *ngIf="section.link"
                [href]="section.link.url"
                target="_blank"
                class="section-link">
                {{ section.link.text }} →
              </a>
            </div>
          </div>
        </div>

        <!-- Quick Links -->
        <div class="quick-links" *ngIf="quickLinks.length > 0">
          <h3>Quick Links</h3>
          <div class="links-grid">
            <a
              *ngFor="let link of quickLinks"
              [href]="link.url"
              target="_blank"
              class="quick-link-item">
              <span class="link-icon">📚</span>
              <span class="link-text">{{ link.text }}</span>
            </a>
          </div>
        </div>

        <!-- Keyboard Shortcuts -->
        <div class="keyboard-shortcuts" *ngIf="showKeyboardShortcuts">
          <h3>Keyboard Shortcuts</h3>
          <div class="shortcuts-list">
            <div class="shortcut-item">
              <kbd>?</kbd>
              <span>Toggle this help panel</span>
            </div>
            <div class="shortcut-item">
              <kbd>Ctrl</kbd> + <kbd>S</kbd>
              <span>Save current work</span>
            </div>
            <div class="shortcut-item">
              <kbd>Ctrl</kbd> + <kbd>K</kbd>
              <span>Search</span>
            </div>
            <div class="shortcut-item">
              <kbd>Esc</kbd>
              <span>Close dialogs</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Support Contact -->
      <div class="help-panel-footer">
        <div class="support-info">
          <p class="support-text">Still need help?</p>
          <button
            type="button"
            class="contact-support-button"
            (click)="onContactSupport()">
            Contact Support
          </button>
        </div>
      </div>
    </aside>
  `,
  styles: [`
    .help-panel-overlay {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: rgba(0, 0, 0, 0.5);
      z-index: 1000;
      backdrop-filter: blur(2px);
    }

    .help-panel {
      position: fixed;
      top: 0;
      right: -480px;
      width: 480px;
      height: 100vh;
      background-color: white;
      box-shadow: -4px 0 24px rgba(0, 0, 0, 0.1);
      z-index: 1001;
      display: flex;
      flex-direction: column;
      transition: right 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    }

    .help-panel.open {
      right: 0;
    }

    .help-panel-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 24px;
      border-bottom: 1px solid #e5e7eb;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
    }

    .header-content {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .help-icon {
      flex-shrink: 0;
    }

    .help-panel-header h2 {
      margin: 0;
      font-size: 20px;
      font-weight: 600;
    }

    .close-button {
      padding: 8px;
      border: none;
      background: rgba(255, 255, 255, 0.2);
      color: white;
      cursor: pointer;
      border-radius: 6px;
      transition: background-color 0.2s;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .close-button:hover {
      background: rgba(255, 255, 255, 0.3);
    }

    .help-panel-content {
      flex: 1;
      overflow-y: auto;
      padding: 24px;
    }

    .search-box {
      position: relative;
      margin-bottom: 24px;
    }

    .search-icon {
      position: absolute;
      left: 12px;
      top: 50%;
      transform: translateY(-50%);
      color: #9ca3af;
    }

    .search-input {
      width: 100%;
      padding: 12px 12px 12px 40px;
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      font-size: 14px;
      transition: all 0.2s;
    }

    .search-input:focus {
      outline: none;
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }

    .sections-container {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .help-section {
      border: 1px solid #e5e7eb;
      border-radius: 8px;
      overflow: hidden;
      transition: all 0.2s;
    }

    .help-section:hover {
      border-color: #d1d5db;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    }

    .section-info { border-left: 3px solid #3b82f6; }
    .section-warning { border-left: 3px solid #f59e0b; }
    .section-tip { border-left: 3px solid #10b981; }
    .section-video { border-left: 3px solid #8b5cf6; }

    .section-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 16px;
      cursor: pointer;
      user-select: none;
      background-color: #f9fafb;
      transition: background-color 0.2s;
    }

    .section-header:hover {
      background-color: #f3f4f6;
    }

    .section-title {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .section-icon {
      font-size: 20px;
      flex-shrink: 0;
    }

    .section-header h3 {
      margin: 0;
      font-size: 15px;
      font-weight: 600;
      color: #1f2937;
    }

    .chevron {
      color: #9ca3af;
      transition: transform 0.2s;
      flex-shrink: 0;
    }

    .chevron.rotated {
      transform: rotate(180deg);
    }

    .section-content {
      padding: 16px;
      background-color: white;
      font-size: 14px;
      line-height: 1.6;
      color: #4b5563;
      animation: slideDown 0.2s ease;
    }

    @keyframes slideDown {
      from {
        opacity: 0;
        transform: translateY(-8px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }

    .section-link {
      display: inline-block;
      margin-top: 12px;
      color: #667eea;
      text-decoration: none;
      font-weight: 500;
      font-size: 13px;
    }

    .section-link:hover {
      text-decoration: underline;
    }

    .quick-links {
      margin-top: 32px;
      padding-top: 24px;
      border-top: 1px solid #e5e7eb;
    }

    .quick-links h3 {
      margin: 0 0 16px 0;
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
    }

    .links-grid {
      display: grid;
      gap: 8px;
    }

    .quick-link-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      background-color: #f9fafb;
      border-radius: 6px;
      text-decoration: none;
      color: #4b5563;
      transition: all 0.2s;
    }

    .quick-link-item:hover {
      background-color: #f3f4f6;
      color: #1f2937;
    }

    .link-icon {
      font-size: 18px;
    }

    .link-text {
      font-size: 14px;
      font-weight: 500;
    }

    .keyboard-shortcuts {
      margin-top: 32px;
      padding-top: 24px;
      border-top: 1px solid #e5e7eb;
    }

    .keyboard-shortcuts h3 {
      margin: 0 0 16px 0;
      font-size: 16px;
      font-weight: 600;
      color: #1f2937;
    }

    .shortcuts-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .shortcut-item {
      display: flex;
      align-items: center;
      gap: 12px;
      font-size: 14px;
    }

    kbd {
      padding: 4px 8px;
      background-color: #f3f4f6;
      border: 1px solid #d1d5db;
      border-radius: 4px;
      font-family: 'Monaco', 'Menlo', monospace;
      font-size: 12px;
      font-weight: 600;
      color: #374151;
      box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
    }

    .help-panel-footer {
      padding: 20px 24px;
      border-top: 1px solid #e5e7eb;
      background-color: #f9fafb;
    }

    .support-info {
      text-align: center;
    }

    .support-text {
      margin: 0 0 12px 0;
      font-size: 14px;
      color: #6b7280;
    }

    .contact-support-button {
      width: 100%;
      padding: 12px 24px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      border: none;
      border-radius: 8px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      transition: transform 0.2s, box-shadow 0.2s;
    }

    .contact-support-button:hover {
      transform: translateY(-1px);
      box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
    }

    /* Mobile responsive */
    @media (max-width: 640px) {
      .help-panel {
        width: 100%;
        right: -100%;
      }
    }
  `]
})
export class HelpPanelComponent {
  @Input() isOpen = false;
  @Input() title = 'Help & Support';
  @Input() sections: HelpSection[] = [];
  @Input() searchable = true;
  @Input() showKeyboardShortcuts = true;
  @Input() quickLinks: { text: string; url: string }[] = [];

  @Output() close = new EventEmitter<void>();
  @Output() contactSupport = new EventEmitter<void>();

  searchQuery = '';
  filteredSections: HelpSection[] = [];
  expandedSections: boolean[] = [];

  ngOnInit() {
    this.filteredSections = this.sections;
    this.expandedSections = new Array(this.sections.length).fill(false);
  }

  ngOnChanges() {
    if (this.sections) {
      this.filteredSections = this.sections;
      this.expandedSections = new Array(this.sections.length).fill(false);
    }
  }

  onClose() {
    this.close.emit();
  }

  onContactSupport() {
    this.contactSupport.emit();
  }

  onSearch() {
    if (!this.searchQuery.trim()) {
      this.filteredSections = this.sections;
      return;
    }

    const query = this.searchQuery.toLowerCase();
    this.filteredSections = this.sections.filter(section =>
      section.title.toLowerCase().includes(query) ||
      section.content.toLowerCase().includes(query)
    );
  }

  toggleSection(index: number) {
    this.expandedSections[index] = !this.expandedSections[index];
  }

  getSectionIcon(type?: string): string {
    switch (type) {
      case 'warning': return '⚠️';
      case 'tip': return '💡';
      case 'video': return '🎥';
      default: return 'ℹ️';
    }
  }
}
