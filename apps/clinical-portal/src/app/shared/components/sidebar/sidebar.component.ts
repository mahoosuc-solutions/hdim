/**
 * Sidebar Component
 *
 * Responsive sidebar navigation with Material sidenav.
 * Collapsible on desktop, modal on mobile.
 *
 * @example
 * <app-sidebar [menuItems]="navItems" [opened]="true">
 *   <!-- Main content here -->
 * </app-sidebar>
 */
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';

/**
 * Menu item definition
 */
export interface MenuItem {
  /** Menu item label */
  label: string;
  /** Router link */
  route?: string;
  /** Material icon */
  icon?: string;
  /** Is item active */
  active?: boolean;
  /** Child menu items */
  children?: MenuItem[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    MatButtonModule,
    MatToolbarModule
  ],
  template: `
    <mat-sidenav-container class="sidenav-container">
      <mat-sidenav
        #sidenav
        [mode]="mode"
        [opened]="opened"
        [fixedInViewport]="true"
        [fixedTopGap]="0"
        class="sidenav"
        (openedChange)="openedChange.emit($event)">

        <mat-toolbar class="sidenav-toolbar">
          <span class="sidebar-title">{{ title }}</span>
          <button
            *ngIf="mode === 'over'"
            mat-icon-button
            (click)="sidenav.close()">
            <mat-icon>close</mat-icon>
          </button>
        </mat-toolbar>

        <mat-nav-list class="menu-list">
          <ng-container *ngFor="let item of menuItems">
            <a
              mat-list-item
              [routerLink]="item.route"
              [class.active]="item.active"
              (click)="onItemClick(item)">
              <mat-icon matListItemIcon *ngIf="item.icon">{{ item.icon }}</mat-icon>
              <span matListItemTitle>{{ item.label }}</span>
            </a>
          </ng-container>
        </mat-nav-list>
      </mat-sidenav>

      <mat-sidenav-content class="sidenav-content">
        <ng-content></ng-content>
      </mat-sidenav-content>
    </mat-sidenav-container>
  `,
  styles: [`
    .sidenav-container {
      height: 100vh;
      width: 100%;
    }

    .sidenav {
      width: 250px;
      background: var(--sidebar-background);
      border-right: 1px solid var(--border-color);
      color: var(--text-primary);
    }

    .sidenav-toolbar {
      background: var(--toolbar-background);
      color: var(--toolbar-text);
      padding: 0 16px;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .sidebar-title {
      font-size: 18px;
      font-weight: 500;
    }

    .menu-list {
      padding-top: 8px;
    }

    .menu-list a {
      color: var(--text-primary);
      transition: background-color 0.2s, color 0.2s;
    }

    .menu-list a:hover {
      background-color: var(--sidebar-hover);
    }

    .menu-list a.active {
      background-color: var(--sidebar-selected);
      color: var(--primary-color);
      font-weight: 500;
    }

    .menu-list a.active mat-icon {
      color: var(--primary-color);
    }

    .menu-list a mat-icon {
      color: var(--text-secondary);
    }

    .sidenav-content {
      display: flex;
      flex-direction: column;
      height: 100%;
      overflow: auto;
    }

    /* Responsive */
    @media (max-width: 960px) {
      .sidenav {
        width: 220px;
      }
    }

    @media (max-width: 600px) {
      .sidenav {
        width: 200px;
      }

      .sidebar-title {
        font-size: 16px;
      }
    }
  `]
})
export class SidebarComponent {
  /** Menu items */
  @Input() menuItems: MenuItem[] = [];

  /** Sidebar title */
  @Input() title: string = 'Menu';

  /** Is sidebar opened */
  @Input() opened: boolean = true;

  /** Sidenav mode */
  @Input() mode: 'over' | 'push' | 'side' = 'side';

  /** Opened state changed */
  @Output() openedChange = new EventEmitter<boolean>();

  /** Menu item clicked */
  @Output() itemClicked = new EventEmitter<MenuItem>();

  onItemClick(item: MenuItem): void {
    this.itemClicked.emit(item);

    // Close sidebar on mobile when item clicked
    if (this.mode === 'over') {
      this.opened = false;
      this.openedChange.emit(false);
    }
  }
}
