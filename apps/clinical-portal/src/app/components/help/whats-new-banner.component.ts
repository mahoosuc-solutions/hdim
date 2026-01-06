/**
 * Issue #24: Provider Dashboard Help System
 * What's New Banner Component
 *
 * Displays announcements for new features and updates.
 * Can be dismissed and remembers dismissal in localStorage.
 */
import { Component, OnInit, inject, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import {
  trigger,
  state,
  style,
  transition,
  animate,
} from '@angular/animations';

export interface WhatsNewItem {
  id: string;
  title: string;
  description: string;
  type: 'feature' | 'improvement' | 'fix';
  date: string;
  learnMoreUrl?: string;
}

// Current announcements - update this array with new features
const ANNOUNCEMENTS: WhatsNewItem[] = [
  {
    id: 'offline-mode-v1',
    title: 'Offline Mode Now Available',
    description: 'Continue working even without internet connection. Changes sync automatically when you\'re back online.',
    type: 'feature',
    date: '2026-01-06',
    learnMoreUrl: '/help/features/offline-mode',
  },
  {
    id: 'ai-insights-v2',
    title: 'Enhanced AI Population Insights',
    description: 'New predictive analytics help identify at-risk patients before care gaps occur.',
    type: 'feature',
    date: '2026-01-05',
    learnMoreUrl: '/help/features/ai-insights',
  },
  {
    id: 'keyboard-shortcuts-v1',
    title: 'Keyboard Shortcuts',
    description: 'Press ? anywhere to see available keyboard shortcuts for faster navigation.',
    type: 'improvement',
    date: '2026-01-04',
  },
  {
    id: 'provider-leaderboard-v1',
    title: 'Provider Performance Leaderboard',
    description: 'Compare your quality metrics with peers and track your improvement over time.',
    type: 'feature',
    date: '2026-01-03',
    learnMoreUrl: '/help/features/leaderboard',
  },
];

const STORAGE_KEY = 'hdim-dismissed-announcements';

@Component({
  selector: 'app-whats-new-banner',
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
  ],
  template: `
    @if (visibleAnnouncements.length > 0 && !collapsed) {
      <div class="whats-new-banner" [@slideDown]>
        <div class="banner-header">
          <div class="header-left">
            <mat-icon class="sparkle-icon">auto_awesome</mat-icon>
            <span class="header-title">What's New</span>
            <span class="badge">{{ visibleAnnouncements.length }}</span>
          </div>
          <div class="header-right">
            <button
              mat-icon-button
              (click)="collapse()"
              matTooltip="Collapse"
              aria-label="Collapse banner">
              <mat-icon>expand_less</mat-icon>
            </button>
            <button
              mat-icon-button
              (click)="dismissAll()"
              matTooltip="Dismiss all"
              aria-label="Dismiss all announcements">
              <mat-icon>close</mat-icon>
            </button>
          </div>
        </div>

        <div class="announcements-container">
          @for (item of visibleAnnouncements; track item.id) {
            <div class="announcement-card" [class]="item.type" [@fadeIn]>
              <div class="type-indicator">
                @switch (item.type) {
                  @case ('feature') {
                    <mat-icon>new_releases</mat-icon>
                  }
                  @case ('improvement') {
                    <mat-icon>trending_up</mat-icon>
                  }
                  @case ('fix') {
                    <mat-icon>build</mat-icon>
                  }
                }
              </div>
              <div class="announcement-content">
                <h4>{{ item.title }}</h4>
                <p>{{ item.description }}</p>
                @if (item.learnMoreUrl) {
                  <a [href]="item.learnMoreUrl" class="learn-more" target="_blank">
                    Learn more
                    <mat-icon>arrow_forward</mat-icon>
                  </a>
                }
              </div>
              <button
                mat-icon-button
                class="dismiss-btn"
                (click)="dismissItem(item.id)"
                matTooltip="Dismiss"
                aria-label="Dismiss announcement">
                <mat-icon>close</mat-icon>
              </button>
            </div>
          }
        </div>
      </div>
    }

    <!-- Collapsed state indicator -->
    @if (visibleAnnouncements.length > 0 && collapsed) {
      <button
        class="collapsed-indicator"
        (click)="expand()"
        matTooltip="Show what's new">
        <mat-icon>auto_awesome</mat-icon>
        <span class="indicator-badge">{{ visibleAnnouncements.length }}</span>
      </button>
    }
  `,
  styles: [`
    :host {
      display: block;
    }

    .whats-new-banner {
      background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
      border-bottom: 1px solid #a5d6a7;
      overflow: hidden;
    }

    .banner-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 16px;
      background: rgba(255, 255, 255, 0.5);
    }

    .header-left {
      display: flex;
      align-items: center;
      gap: 8px;

      .sparkle-icon {
        color: #4caf50;
        animation: sparkle 2s ease-in-out infinite;
      }

      .header-title {
        font-weight: 600;
        color: #2e7d32;
      }

      .badge {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        min-width: 20px;
        height: 20px;
        padding: 0 6px;
        background: #4caf50;
        color: white;
        border-radius: 10px;
        font-size: 12px;
        font-weight: 600;
      }
    }

    .header-right {
      display: flex;
      gap: 4px;

      button {
        color: rgba(0, 0, 0, 0.6);
      }
    }

    @keyframes sparkle {
      0%, 100% { opacity: 1; transform: scale(1); }
      50% { opacity: 0.7; transform: scale(1.1); }
    }

    .announcements-container {
      display: flex;
      gap: 12px;
      padding: 12px 16px 16px;
      overflow-x: auto;
      scroll-snap-type: x mandatory;

      &::-webkit-scrollbar {
        height: 6px;
      }

      &::-webkit-scrollbar-track {
        background: rgba(0, 0, 0, 0.1);
        border-radius: 3px;
      }

      &::-webkit-scrollbar-thumb {
        background: rgba(0, 0, 0, 0.2);
        border-radius: 3px;
      }
    }

    .announcement-card {
      display: flex;
      gap: 12px;
      min-width: 280px;
      max-width: 320px;
      padding: 12px;
      background: white;
      border-radius: 8px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      scroll-snap-align: start;
      position: relative;

      &.feature .type-indicator {
        background: linear-gradient(135deg, #4caf50 0%, #66bb6a 100%);
      }

      &.improvement .type-indicator {
        background: linear-gradient(135deg, #2196f3 0%, #42a5f5 100%);
      }

      &.fix .type-indicator {
        background: linear-gradient(135deg, #ff9800 0%, #ffa726 100%);
      }
    }

    .type-indicator {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 36px;
      height: 36px;
      border-radius: 8px;
      color: white;
      flex-shrink: 0;

      mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }
    }

    .announcement-content {
      flex: 1;
      min-width: 0;

      h4 {
        margin: 0 0 4px;
        font-size: 14px;
        font-weight: 600;
        color: rgba(0, 0, 0, 0.87);
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
        padding-right: 24px;
      }

      p {
        margin: 0 0 8px;
        font-size: 12px;
        color: rgba(0, 0, 0, 0.6);
        line-height: 1.4;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      .learn-more {
        display: inline-flex;
        align-items: center;
        gap: 4px;
        font-size: 12px;
        color: #1976d2;
        text-decoration: none;
        font-weight: 500;

        mat-icon {
          font-size: 14px;
          width: 14px;
          height: 14px;
        }

        &:hover {
          text-decoration: underline;
        }
      }
    }

    .dismiss-btn {
      position: absolute;
      top: 4px;
      right: 4px;
      width: 24px;
      height: 24px;
      line-height: 24px;

      mat-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
      }
    }

    /* Collapsed indicator */
    .collapsed-indicator {
      position: fixed;
      bottom: 80px;
      right: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 48px;
      height: 48px;
      background: linear-gradient(135deg, #4caf50 0%, #66bb6a 100%);
      color: white;
      border: none;
      border-radius: 50%;
      box-shadow: 0 4px 12px rgba(76, 175, 80, 0.4);
      cursor: pointer;
      transition: all 0.2s ease;
      z-index: 100;

      &:hover {
        transform: scale(1.1);
        box-shadow: 0 6px 16px rgba(76, 175, 80, 0.5);
      }

      mat-icon {
        animation: sparkle 2s ease-in-out infinite;
      }

      .indicator-badge {
        position: absolute;
        top: -4px;
        right: -4px;
        display: flex;
        align-items: center;
        justify-content: center;
        min-width: 20px;
        height: 20px;
        padding: 0 6px;
        background: #f44336;
        color: white;
        border-radius: 10px;
        font-size: 11px;
        font-weight: 600;
      }
    }

    /* Responsive */
    @media (max-width: 600px) {
      .announcement-card {
        min-width: 260px;
      }
    }
  `],
  animations: [
    trigger('slideDown', [
      state('void', style({ height: 0, opacity: 0 })),
      state('*', style({ height: '*', opacity: 1 })),
      transition('void => *', animate('300ms ease-out')),
      transition('* => void', animate('200ms ease-in')),
    ]),
    trigger('fadeIn', [
      state('void', style({ opacity: 0, transform: 'translateY(-10px)' })),
      state('*', style({ opacity: 1, transform: 'translateY(0)' })),
      transition('void => *', animate('200ms ease-out')),
    ]),
  ],
})
export class WhatsNewBannerComponent implements OnInit {
  @Output() openHelp = new EventEmitter<void>();

  visibleAnnouncements: WhatsNewItem[] = [];
  collapsed = false;

  private dismissedIds: Set<string> = new Set();

  ngOnInit(): void {
    this.loadDismissedItems();
    this.updateVisibleAnnouncements();
  }

  dismissItem(id: string): void {
    this.dismissedIds.add(id);
    this.saveDismissedItems();
    this.updateVisibleAnnouncements();
  }

  dismissAll(): void {
    ANNOUNCEMENTS.forEach((item) => this.dismissedIds.add(item.id));
    this.saveDismissedItems();
    this.updateVisibleAnnouncements();
  }

  collapse(): void {
    this.collapsed = true;
  }

  expand(): void {
    this.collapsed = false;
  }

  private updateVisibleAnnouncements(): void {
    // Show announcements from the last 30 days that haven't been dismissed
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

    this.visibleAnnouncements = ANNOUNCEMENTS.filter(
      (item) =>
        !this.dismissedIds.has(item.id) &&
        new Date(item.date) >= thirtyDaysAgo
    ).sort((a, b) => new Date(b.date).getTime() - new Date(a.date).getTime());
  }

  private loadDismissedItems(): void {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        this.dismissedIds = new Set(parsed);
      }
    } catch {
      // Ignore localStorage errors
    }
  }

  private saveDismissedItems(): void {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify([...this.dismissedIds]));
    } catch {
      // Ignore localStorage errors
    }
  }
}
