import { Component, OnInit, OnDestroy, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatChipsModule } from '@angular/material/chips';
import { MatListModule } from '@angular/material/list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatBadgeModule } from '@angular/material/badge';
import { MatDividerModule } from '@angular/material/divider';
import { Subject, takeUntil } from 'rxjs';
import { HelpService, HelpContent, HelpFeature, HelpTutorial, HelpFAQ, WhatsNewItem } from '../../services/help.service';

/**
 * Help Panel Component
 *
 * Provides contextual help for the current page with features, tutorials,
 * FAQs, and tips. Also displays What's New announcements.
 */
@Component({
  selector: 'app-help-panel',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RouterModule,
    MatSidenavModule,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatFormFieldModule,
    MatInputModule,
    MatExpansionModule,
    MatChipsModule,
    MatListModule,
    MatTooltipModule,
    MatBadgeModule,
    MatDividerModule,
  ],
  template: `
    <div class="help-panel" [class.open]="isOpen">
      <!-- Header -->
      <div class="panel-header">
        <div class="header-title">
          <mat-icon>help</mat-icon>
          <span>Help</span>
        </div>
        <button mat-icon-button (click)="close()" matTooltip="Close help">
          <mat-icon>close</mat-icon>
        </button>
      </div>

      <!-- Search -->
      <div class="search-section">
        <mat-form-field appearance="outline" class="search-field">
          <mat-label>Search help...</mat-label>
          <input matInput [(ngModel)]="searchQuery" (input)="onSearch()" placeholder="Type to search...">
          <mat-icon matPrefix>search</mat-icon>
          @if (searchQuery) {
            <button matSuffix mat-icon-button (click)="clearSearch()">
              <mat-icon>clear</mat-icon>
            </button>
          }
        </mat-form-field>
      </div>

      <!-- Search Results -->
      @if (searchQuery && searchResults.length > 0) {
        <div class="search-results">
          <h3>Search Results</h3>
          @for (result of searchResults; track result.pageId) {
            <mat-card class="result-card" (click)="navigateToPage(result.pageId)">
              <mat-card-header>
                <mat-icon mat-card-avatar>description</mat-icon>
                <mat-card-title>{{ getPageTitle(result.pageId) }}</mat-card-title>
              </mat-card-header>
              <mat-card-content>
                <ul class="match-list">
                  @for (match of result.matches.slice(0, 3); track match) {
                    <li>{{ match }}</li>
                  }
                </ul>
              </mat-card-content>
            </mat-card>
          }
        </div>
      } @else if (searchQuery) {
        <div class="no-results">
          <mat-icon>search_off</mat-icon>
          <p>No results found for "{{ searchQuery }}"</p>
        </div>
      }

      <!-- Main Content (when not searching) -->
      @if (!searchQuery) {
        <div class="panel-content">
          <!-- What's New Section -->
          @if (whatsNewItems.length > 0) {
            <div class="whats-new-section">
              <div class="section-header">
                <h3>
                  <mat-icon>new_releases</mat-icon>
                  What's New
                  <mat-chip class="new-badge">{{ whatsNewItems.length }}</mat-chip>
                </h3>
                <button mat-button size="small" (click)="dismissAllWhatsNew()">
                  Dismiss All
                </button>
              </div>
              <div class="whats-new-list">
                @for (item of whatsNewItems; track item.id) {
                  <div class="whats-new-item" [class]="item.type">
                    <div class="item-icon">
                      <mat-icon>{{ getWhatsNewIcon(item.type) }}</mat-icon>
                    </div>
                    <div class="item-content">
                      <span class="item-title">{{ item.title }}</span>
                      <span class="item-desc">{{ item.description }}</span>
                      @if (item.link) {
                        <a [routerLink]="item.link" class="item-link">
                          Learn more <mat-icon>arrow_forward</mat-icon>
                        </a>
                      }
                    </div>
                    <button mat-icon-button (click)="dismissWhatsNew(item.id)" matTooltip="Dismiss">
                      <mat-icon>close</mat-icon>
                    </button>
                  </div>
                }
              </div>
            </div>
            <mat-divider></mat-divider>
          }

          <!-- Page Help Content -->
          @if (currentHelp) {
            <div class="page-help">
              <div class="help-header">
                <h2>{{ currentHelp.title }}</h2>
                <p class="overview">{{ currentHelp.overview }}</p>
              </div>

              <!-- Features -->
              @if (currentHelp.features.length > 0) {
                <mat-expansion-panel expanded class="help-section">
                  <mat-expansion-panel-header>
                    <mat-panel-title>
                      <mat-icon>stars</mat-icon>
                      Features
                    </mat-panel-title>
                  </mat-expansion-panel-header>
                  <div class="features-list">
                    @for (feature of currentHelp.features; track feature.name) {
                      <div class="feature-item">
                        <mat-icon class="feature-icon">{{ feature.icon }}</mat-icon>
                        <div class="feature-content">
                          <span class="feature-name">{{ feature.name }}</span>
                          <span class="feature-desc">{{ feature.description }}</span>
                          <div class="feature-tip">
                            <mat-icon>lightbulb</mat-icon>
                            <span>{{ feature.tip }}</span>
                          </div>
                        </div>
                      </div>
                    }
                  </div>
                </mat-expansion-panel>
              }

              <!-- Tutorials -->
              @if (currentHelp.tutorials.length > 0) {
                <mat-expansion-panel class="help-section">
                  <mat-expansion-panel-header>
                    <mat-panel-title>
                      <mat-icon>school</mat-icon>
                      Tutorials & Guides
                    </mat-panel-title>
                  </mat-expansion-panel-header>
                  <mat-nav-list class="tutorials-list">
                    @for (tutorial of currentHelp.tutorials; track tutorial.url) {
                      <a mat-list-item [routerLink]="tutorial.url" (click)="close()">
                        <mat-icon matListItemIcon>{{ getTutorialIcon(tutorial.type) }}</mat-icon>
                        <div matListItemTitle>{{ tutorial.title }}</div>
                        <div matListItemLine>{{ tutorial.description }}</div>
                        <mat-chip matListItemMeta>{{ tutorial.duration }}</mat-chip>
                      </a>
                    }
                  </mat-nav-list>
                </mat-expansion-panel>
              }

              <!-- FAQ -->
              @if (currentHelp.faq.length > 0) {
                <mat-expansion-panel class="help-section">
                  <mat-expansion-panel-header>
                    <mat-panel-title>
                      <mat-icon>question_answer</mat-icon>
                      FAQ
                    </mat-panel-title>
                  </mat-expansion-panel-header>
                  <mat-accordion class="faq-accordion">
                    @for (faq of currentHelp.faq; track faq.question) {
                      <mat-expansion-panel class="faq-item">
                        <mat-expansion-panel-header>
                          <mat-panel-title>{{ faq.question }}</mat-panel-title>
                        </mat-expansion-panel-header>
                        <p>{{ faq.answer }}</p>
                        <div class="faq-tags">
                          @for (tag of faq.tags; track tag) {
                            <mat-chip size="small">{{ tag }}</mat-chip>
                          }
                        </div>
                      </mat-expansion-panel>
                    }
                  </mat-accordion>
                </mat-expansion-panel>
              }

              <!-- Tips -->
              @if (currentHelp.tips.length > 0) {
                <mat-expansion-panel class="help-section">
                  <mat-expansion-panel-header>
                    <mat-panel-title>
                      <mat-icon>tips_and_updates</mat-icon>
                      Pro Tips
                    </mat-panel-title>
                  </mat-expansion-panel-header>
                  <ul class="tips-list">
                    @for (tip of currentHelp.tips; track tip) {
                      <li>
                        <mat-icon>arrow_right</mat-icon>
                        {{ tip }}
                      </li>
                    }
                  </ul>
                </mat-expansion-panel>
              }
            </div>
          }

          <!-- Contact Support -->
          <div class="support-section">
            <mat-card class="support-card">
              <mat-card-content>
                <mat-icon>support_agent</mat-icon>
                <div class="support-text">
                  <span class="support-title">Need more help?</span>
                  <span class="support-desc">Contact our support team</span>
                </div>
                <button mat-stroked-button routerLink="/knowledge-base" (click)="close()">
                  Knowledge Base
                </button>
              </mat-card-content>
            </mat-card>
          </div>
        </div>
      }

      <!-- Keyboard Shortcut Hint -->
      <div class="panel-footer">
        <span>
          <kbd>?</kbd> Keyboard shortcuts
        </span>
      </div>
    </div>
  `,
  styles: [`
    .help-panel {
      position: fixed;
      top: 0;
      right: -400px;
      width: 400px;
      height: 100vh;
      background: white;
      box-shadow: -4px 0 20px rgba(0, 0, 0, 0.15);
      z-index: 1000;
      display: flex;
      flex-direction: column;
      transition: right 0.3s ease;

      &.open {
        right: 0;
      }
    }

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 16px;
      background: linear-gradient(135deg, #1976d2, #1565c0);
      color: white;
    }

    .header-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 18px;
      font-weight: 500;

      mat-icon {
        font-size: 24px;
      }
    }

    .search-section {
      padding: 16px;
      border-bottom: 1px solid #e0e0e0;
    }

    .search-field {
      width: 100%;

      ::ng-deep .mat-mdc-form-field-subscript-wrapper {
        display: none;
      }
    }

    .search-results {
      padding: 16px;
      overflow-y: auto;

      h3 {
        margin: 0 0 12px 0;
        font-size: 14px;
        color: #666;
      }
    }

    .result-card {
      margin-bottom: 12px;
      cursor: pointer;
      transition: box-shadow 0.2s;

      &:hover {
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
      }
    }

    .match-list {
      margin: 0;
      padding-left: 20px;
      font-size: 13px;
      color: #666;

      li {
        margin-bottom: 4px;
      }
    }

    .no-results {
      display: flex;
      flex-direction: column;
      align-items: center;
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
        text-align: center;
      }
    }

    .panel-content {
      flex: 1;
      overflow-y: auto;
      padding: 16px;
    }

    .whats-new-section {
      margin-bottom: 16px;
    }

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      h3 {
        display: flex;
        align-items: center;
        gap: 8px;
        margin: 0;
        font-size: 16px;

        mat-icon {
          color: #ff9800;
        }
      }

      .new-badge {
        background: #ff9800 !important;
        color: white !important;
        font-size: 11px;
        min-height: 20px;
      }
    }

    .whats-new-list {
      display: flex;
      flex-direction: column;
      gap: 8px;
    }

    .whats-new-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 12px;
      background: #f5f5f5;
      border-radius: 8px;
      border-left: 4px solid #ff9800;

      &.feature { border-color: #4caf50; }
      &.improvement { border-color: #2196f3; }
      &.fix { border-color: #f44336; }
    }

    .item-icon {
      padding: 8px;
      background: white;
      border-radius: 50%;

      mat-icon {
        font-size: 20px;
        width: 20px;
        height: 20px;
      }
    }

    .feature .item-icon mat-icon { color: #4caf50; }
    .improvement .item-icon mat-icon { color: #2196f3; }
    .fix .item-icon mat-icon { color: #f44336; }

    .item-content {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .item-title {
      font-weight: 500;
      color: #333;
    }

    .item-desc {
      font-size: 13px;
      color: #666;
    }

    .item-link {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 13px;
      color: #1976d2;
      text-decoration: none;

      mat-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
      }

      &:hover {
        text-decoration: underline;
      }
    }

    .page-help {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .help-header {
      h2 {
        margin: 0 0 8px 0;
        font-size: 20px;
        color: #333;
      }

      .overview {
        margin: 0;
        color: #666;
        font-size: 14px;
        line-height: 1.5;
      }
    }

    .help-section {
      ::ng-deep .mat-expansion-panel-header-title {
        display: flex;
        align-items: center;
        gap: 8px;

        mat-icon {
          color: #1976d2;
        }
      }
    }

    .features-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .feature-item {
      display: flex;
      gap: 12px;
    }

    .feature-icon {
      color: #1976d2;
      font-size: 24px;
      width: 24px;
      height: 24px;
    }

    .feature-content {
      display: flex;
      flex-direction: column;
      gap: 4px;
    }

    .feature-name {
      font-weight: 500;
      color: #333;
    }

    .feature-desc {
      font-size: 13px;
      color: #666;
    }

    .feature-tip {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-top: 4px;
      padding: 6px 10px;
      background: #fff8e1;
      border-radius: 4px;
      font-size: 12px;
      color: #f57c00;

      mat-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
      }
    }

    .tutorials-list {
      ::ng-deep .mat-mdc-list-item {
        height: auto !important;
        padding: 12px 0 !important;
      }
    }

    .faq-accordion {
      ::ng-deep .mat-expansion-panel {
        box-shadow: none !important;
        border-bottom: 1px solid #e0e0e0;

        &:last-child {
          border-bottom: none;
        }
      }
    }

    .faq-item {
      p {
        margin: 0 0 12px 0;
        font-size: 14px;
        line-height: 1.6;
      }
    }

    .faq-tags {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
    }

    .tips-list {
      margin: 0;
      padding: 0;
      list-style: none;

      li {
        display: flex;
        align-items: flex-start;
        gap: 8px;
        margin-bottom: 12px;
        font-size: 14px;
        color: #333;

        mat-icon {
          color: #4caf50;
          font-size: 18px;
          width: 18px;
          height: 18px;
          flex-shrink: 0;
        }
      }
    }

    .support-section {
      margin-top: 16px;
    }

    .support-card {
      background: linear-gradient(135deg, #e3f2fd, #e8f5e9);

      mat-card-content {
        display: flex;
        align-items: center;
        gap: 12px;

        mat-icon {
          font-size: 32px;
          width: 32px;
          height: 32px;
          color: #1976d2;
        }
      }
    }

    .support-text {
      flex: 1;
      display: flex;
      flex-direction: column;
    }

    .support-title {
      font-weight: 500;
      color: #333;
    }

    .support-desc {
      font-size: 13px;
      color: #666;
    }

    .panel-footer {
      padding: 12px 16px;
      background: #f5f5f5;
      border-top: 1px solid #e0e0e0;
      text-align: center;
      font-size: 13px;
      color: #666;

      kbd {
        display: inline-block;
        padding: 2px 6px;
        background: white;
        border: 1px solid #ccc;
        border-radius: 4px;
        font-family: monospace;
        font-size: 12px;
      }
    }

    @media (max-width: 480px) {
      .help-panel {
        width: 100%;
        right: -100%;

        &.open {
          right: 0;
        }
      }
    }
  `],
})
export class HelpPanelComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  @Input() isOpen = false;
  @Output() closed = new EventEmitter<void>();

  searchQuery = '';
  searchResults: { pageId: string; matches: string[] }[] = [];
  currentHelp: HelpContent | null = null;
  whatsNewItems: WhatsNewItem[] = [];

  constructor(
    private helpService: HelpService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.helpService.currentHelp$
      .pipe(takeUntil(this.destroy$))
      .subscribe(help => {
        this.currentHelp = help;
      });

    this.helpService.whatsNew$
      .pipe(takeUntil(this.destroy$))
      .subscribe(items => {
        this.whatsNewItems = items.filter(i => !i.dismissed);
      });
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  close(): void {
    this.closed.emit();
    this.helpService.hideHelpPanel();
  }

  onSearch(): void {
    if (this.searchQuery.trim()) {
      this.searchResults = this.helpService.searchHelp(this.searchQuery);
    } else {
      this.searchResults = [];
    }
  }

  clearSearch(): void {
    this.searchQuery = '';
    this.searchResults = [];
  }

  navigateToPage(pageId: string): void {
    this.router.navigate(['/' + pageId.replace('default', 'dashboard')]);
    this.close();
  }

  getPageTitle(pageId: string): string {
    const help = this.helpService.getHelpForPage(pageId);
    return help?.title || pageId;
  }

  dismissWhatsNew(id: string): void {
    this.helpService.dismissWhatsNewItem(id);
  }

  dismissAllWhatsNew(): void {
    this.helpService.dismissAllWhatsNew();
  }

  getWhatsNewIcon(type: string): string {
    switch (type) {
      case 'feature': return 'new_releases';
      case 'improvement': return 'trending_up';
      case 'fix': return 'build';
      default: return 'info';
    }
  }

  getTutorialIcon(type: string): string {
    switch (type) {
      case 'video': return 'play_circle';
      case 'article': return 'article';
      case 'guide': return 'menu_book';
      default: return 'description';
    }
  }
}
