import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatDividerModule } from '@angular/material/divider';
import { Subject, takeUntil } from 'rxjs';
import {
  KeyboardShortcutsService,
  KeyboardShortcut,
  ShortcutCategory,
  ShortcutCategoryInfo,
} from '../../services/keyboard-shortcuts.service';

/**
 * Keyboard Shortcuts Help Dialog
 *
 * Displays all available keyboard shortcuts organized by category.
 * Allows users to enable/disable individual shortcuts and see their bindings.
 */
@Component({
  selector: 'app-keyboard-shortcuts-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatButtonModule,
    MatIconModule,
    MatTabsModule,
    MatSlideToggleModule,
    MatChipsModule,
    MatTooltipModule,
    MatDividerModule,
  ],
  template: `
    <div class="shortcuts-dialog">
      <div class="dialog-header">
        <div class="header-title">
          <mat-icon>keyboard</mat-icon>
          <h2>Keyboard Shortcuts</h2>
        </div>
        <div class="header-toggle">
          <mat-slide-toggle
            [(ngModel)]="shortcutsEnabled"
            (change)="toggleShortcuts()"
            color="primary">
            {{ shortcutsEnabled ? 'Enabled' : 'Disabled' }}
          </mat-slide-toggle>
        </div>
      </div>

      <mat-dialog-content>
        <mat-tab-group class="shortcuts-tabs" animationDuration="200ms">
          @for (category of categories; track category.id) {
            <mat-tab>
              <ng-template mat-tab-label>
                <mat-icon>{{ category.icon }}</mat-icon>
                <span class="tab-label">{{ category.label }}</span>
              </ng-template>

              <div class="tab-content">
                <div class="shortcuts-list">
                  @for (shortcut of getShortcutsByCategory(category.id); track shortcut.id) {
                    <div class="shortcut-item" [class.disabled]="!shortcut.enabled">
                      <div class="shortcut-info">
                        <span class="shortcut-description">{{ shortcut.description }}</span>
                      </div>
                      <div class="shortcut-binding">
                        <kbd class="keyboard-key">{{ formatShortcut(shortcut) }}</kbd>
                      </div>
                      @if (shortcut.customizable) {
                        <mat-slide-toggle
                          [checked]="shortcut.enabled"
                          (change)="toggleShortcut(shortcut.id, $event.checked)"
                          color="primary"
                          class="shortcut-toggle"
                          matTooltip="Enable/disable this shortcut">
                        </mat-slide-toggle>
                      }
                    </div>
                  } @empty {
                    <div class="empty-category">
                      <mat-icon>keyboard_hide</mat-icon>
                      <p>No shortcuts in this category</p>
                    </div>
                  }
                </div>
              </div>
            </mat-tab>
          }

          <!-- All Shortcuts Tab -->
          <mat-tab>
            <ng-template mat-tab-label>
              <mat-icon>list</mat-icon>
              <span class="tab-label">All</span>
            </ng-template>

            <div class="tab-content">
              <div class="shortcuts-list compact">
                @for (shortcut of allShortcuts; track shortcut.id) {
                  <div class="shortcut-item compact" [class.disabled]="!shortcut.enabled">
                    <div class="shortcut-info">
                      <mat-chip [class]="shortcut.category">
                        {{ getCategoryLabel(shortcut.category) }}
                      </mat-chip>
                      <span class="shortcut-description">{{ shortcut.description }}</span>
                    </div>
                    <div class="shortcut-binding">
                      <kbd class="keyboard-key">{{ formatShortcut(shortcut) }}</kbd>
                    </div>
                  </div>
                }
              </div>
            </div>
          </mat-tab>
        </mat-tab-group>

        <div class="shortcuts-footer">
          <div class="footer-tip">
            <mat-icon>lightbulb</mat-icon>
            <span>Press <kbd>?</kbd> anywhere to open this dialog</span>
          </div>
          <button mat-stroked-button (click)="resetAllDefaults()" class="reset-button">
            <mat-icon>restore</mat-icon>
            Reset to Defaults
          </button>
        </div>
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button mat-dialog-close>Close</button>
      </mat-dialog-actions>
    </div>
  `,
  styles: [`
    .shortcuts-dialog {
      min-width: 600px;
      max-width: 800px;
    }

    .dialog-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px 24px;
      border-bottom: 1px solid #e0e0e0;
      margin: -24px -24px 0;
      background: linear-gradient(135deg, #1976d2, #1565c0);
      color: white;
    }

    .header-title {
      display: flex;
      align-items: center;
      gap: 12px;

      mat-icon {
        font-size: 32px;
        width: 32px;
        height: 32px;
      }

      h2 {
        margin: 0;
        font-weight: 500;
      }
    }

    .header-toggle {
      ::ng-deep .mat-mdc-slide-toggle .mdc-label {
        color: white;
      }
    }

    .shortcuts-tabs {
      ::ng-deep .mat-mdc-tab-labels {
        border-bottom: 1px solid #e0e0e0;
      }

      ::ng-deep .mat-mdc-tab {
        min-width: 100px;
      }
    }

    .tab-label {
      margin-left: 8px;
    }

    .tab-content {
      padding: 16px 0;
      max-height: 400px;
      overflow-y: auto;
    }

    .shortcuts-list {
      display: flex;
      flex-direction: column;
      gap: 8px;

      &.compact {
        gap: 4px;
      }
    }

    .shortcut-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 12px 16px;
      background: #fafafa;
      border-radius: 8px;
      transition: all 0.2s;

      &:hover {
        background: #f0f0f0;
      }

      &.disabled {
        opacity: 0.5;

        .keyboard-key {
          background: #e0e0e0;
          color: #999;
        }
      }

      &.compact {
        padding: 8px 12px;
      }
    }

    .shortcut-info {
      flex: 1;
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .shortcut-description {
      font-size: 14px;
      color: #333;
    }

    .shortcut-binding {
      display: flex;
      align-items: center;
    }

    .keyboard-key {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      padding: 6px 12px;
      min-width: 32px;
      background: linear-gradient(180deg, #f5f5f5, #e0e0e0);
      border: 1px solid #ccc;
      border-radius: 6px;
      box-shadow: 0 2px 0 #999, inset 0 1px 0 white;
      font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Fira Mono', monospace;
      font-size: 12px;
      font-weight: 600;
      color: #333;
      white-space: nowrap;
    }

    .shortcut-toggle {
      margin-left: 12px;
    }

    mat-chip {
      font-size: 11px;
      min-height: 24px;

      &.navigation {
        background: #e3f2fd !important;
        color: #1565c0 !important;
      }

      &.actions {
        background: #fff3e0 !important;
        color: #e65100 !important;
      }

      &.dialogs {
        background: #f3e5f5 !important;
        color: #7b1fa2 !important;
      }

      &.data {
        background: #e8f5e9 !important;
        color: #2e7d32 !important;
      }

      &.help {
        background: #fce4ec !important;
        color: #c2185b !important;
      }
    }

    .empty-category {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      padding: 48px;
      color: #999;

      mat-icon {
        font-size: 48px;
        width: 48px;
        height: 48px;
        margin-bottom: 12px;
      }

      p {
        margin: 0;
        font-size: 14px;
      }
    }

    .shortcuts-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 16px;
      margin-top: 16px;
      border-top: 1px solid #e0e0e0;
    }

    .footer-tip {
      display: flex;
      align-items: center;
      gap: 8px;
      color: #666;
      font-size: 13px;

      mat-icon {
        color: #ffc107;
        font-size: 20px;
        width: 20px;
        height: 20px;
      }

      kbd {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        padding: 2px 6px;
        min-width: 20px;
        background: #f5f5f5;
        border: 1px solid #ccc;
        border-radius: 4px;
        font-family: monospace;
        font-size: 11px;
        font-weight: 600;
      }
    }

    .reset-button {
      mat-icon {
        margin-right: 4px;
      }
    }

    @media (max-width: 600px) {
      .shortcuts-dialog {
        min-width: auto;
      }

      .shortcut-item {
        flex-wrap: wrap;
      }

      .shortcut-info {
        flex-basis: 100%;
        margin-bottom: 8px;
      }
    }
  `],
})
export class KeyboardShortcutsDialogComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  allShortcuts: KeyboardShortcut[] = [];
  shortcutsEnabled = true;
  categories: ShortcutCategoryInfo[] = [];

  constructor(
    private dialogRef: MatDialogRef<KeyboardShortcutsDialogComponent>,
    private shortcutsService: KeyboardShortcutsService,
  ) {}

  ngOnInit(): void {
    this.categories = this.shortcutsService.categories;

    this.shortcutsService.shortcuts$
      .pipe(takeUntil(this.destroy$))
      .subscribe(shortcuts => {
        this.allShortcuts = shortcuts;
      });

    this.shortcutsService.enabled$
      .pipe(takeUntil(this.destroy$))
      .subscribe(enabled => {
        this.shortcutsEnabled = enabled;
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  getShortcutsByCategory(category: ShortcutCategory): KeyboardShortcut[] {
    return this.allShortcuts.filter(s => s.category === category);
  }

  getCategoryLabel(category: ShortcutCategory): string {
    const info = this.categories.find(c => c.id === category);
    return info?.label || category;
  }

  formatShortcut(shortcut: KeyboardShortcut): string {
    return this.shortcutsService.formatShortcut(shortcut);
  }

  toggleShortcuts(): void {
    this.shortcutsService.setEnabled(this.shortcutsEnabled);
  }

  toggleShortcut(id: string, enabled: boolean): void {
    this.shortcutsService.setShortcutEnabled(id, enabled);
  }

  resetAllDefaults(): void {
    this.shortcutsService.resetAllToDefaults();
  }
}
