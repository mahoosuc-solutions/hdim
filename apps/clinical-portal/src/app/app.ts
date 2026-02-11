import { Component, OnInit, OnDestroy, HostListener, effect } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatBadgeModule } from '@angular/material/badge';
import { MatMenuModule } from '@angular/material/menu';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { BreadcrumbComponent } from './shared/components/breadcrumb/breadcrumb.component';
import { GlobalSearchService } from './shared/services/global-search.service';
import { ThemeService } from './services/theme.service';
import { AuthService } from './services/auth.service';
import { LoggerService } from './services/logger.service';
import { AuditService } from './services/audit.service';
import { DemoControlBarComponent } from './demo-mode/components/demo-control-bar/demo-control-bar.component';
import { DemoModeService } from './demo-mode/services/demo-mode.service';
import { DemoStoryboardOverlayComponent } from './demo-mode/components/demo-storyboard-overlay/demo-storyboard-overlay.component';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-root',
  imports: [
    CommonModule,
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    MatListModule,
    MatBadgeModule,
    MatMenuModule,
    MatTooltipModule,
    MatSelectModule,
    MatFormFieldModule,
    BreadcrumbComponent,
    DemoControlBarComponent,
    DemoStoryboardOverlayComponent,
  ],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class App implements OnInit, OnDestroy {
  protected title = 'Clinical Portal';
  protected practiceName = 'Main Street Clinic';
  protected sidenavOpened = true;

  // Session timeout configuration (15 minutes of inactivity)
  private readonly SESSION_TIMEOUT_MS = 15 * 60 * 1000;
  private readonly SESSION_WARNING_MS = 2 * 60 * 1000; // Warn 2 minutes before timeout
  private sessionTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private sessionWarningId: ReturnType<typeof setTimeout> | null = null;
  private lastActivityTime = Date.now();
  protected showSessionWarning = false;
  protected sessionTimeRemaining = 0;
  private sessionCountdownId: ReturnType<typeof setInterval> | null = null;
  private authSubscription: Subscription | null = null;
  private tenantSubscription: Subscription | null = null;
  private selectedTenantSubscription: Subscription | null = null;

  protected tenantOptions: string[] = [];
  protected selectedTenantId: string | null = null;

  /**
   * Navigation items - conditionally includes demo/testing routes when demo mode is enabled
   */
  protected get navItems() {
    const baseItems = [
      { path: '/dashboard', icon: 'dashboard', label: 'Dashboard' },
      { path: '/patients', icon: 'people', label: 'Patients' },
      { path: '/quality-measures', icon: 'library_books', label: 'Quality Measures' },
      { path: '/evaluations', icon: 'assessment', label: 'Evaluations' },
      { path: '/results', icon: 'analytics', label: 'Results' },
      { path: '/care-gaps', icon: 'warning', label: 'Care Gaps' },
      { path: '/risk-stratification', icon: 'speed', label: 'Risk Stratification' },
      { path: '/outreach-campaigns', icon: 'campaign', label: 'Outreach' },
      { path: '/reports', icon: 'description', label: 'Reports' },
      { path: '/measure-builder', icon: 'build_circle', label: 'Measure Builder' },
      { path: '/visualization/live-monitor', icon: '3d_rotation', label: 'Live Monitor' },
      { path: '/ai-assistant', icon: 'smart_toy', label: 'AI Assistant' },
      { path: '/knowledge-base', icon: 'menu_book', label: 'Knowledge Base' },
    ];

    // Add demo/testing routes when demo mode is enabled
    if (this.demoModeService.isDemoMode()) {
      return [
        ...baseItems,
        { path: '/demo-startup', icon: 'power', label: 'Demo Startup' },
        { path: '/testing', icon: 'bug_report', label: 'Testing' },
        { path: '/compliance', icon: 'verified_user', label: 'Compliance' },
      ];
    }

    return baseItems;
  }

  getNavTestId(item: { path: string }): string {
    const slug = item.path.replace(/^\//, '').replace(/[^a-zA-Z0-9]+/g, '-').replace(/^-|-$/g, '');
    return `nav-${slug || 'dashboard'}`;
  }

  constructor(
    private globalSearchService: GlobalSearchService,
    protected themeService: ThemeService,
    private authService: AuthService,
    protected router: Router,
    private demoModeService: DemoModeService,
    private logger: LoggerService,
    private auditService: AuditService
  ) {
    // React to demo mode changes to add/remove body class
    effect(() => {
      if (this.demoModeService.isDemoMode()) {
        document.body.classList.add('demo-mode-active');
      } else {
        document.body.classList.remove('demo-mode-active');
      }
    });
  }

  ngOnInit(): void {
    // Initialize theme system - automatically detects browser preference
    this.themeService.initialize();
    this.initTestIdBridge();

    // Start session timeout monitoring if user is authenticated
    this.authSubscription = this.authService.isAuthenticated$.subscribe((isAuth) => {
      if (isAuth) {
        this.resetSessionTimeout();
      } else {
        this.clearSessionTimeout();
      }
    });

    this.tenantSubscription = this.authService.currentUser$.subscribe((user) => {
      this.tenantOptions = user?.tenantIds?.length ? user.tenantIds : (user?.tenantId ? [user.tenantId] : []);
      this.selectedTenantId = this.authService.getTenantId();
    });

    this.selectedTenantSubscription = this.authService.selectedTenant$.subscribe((tenantId) => {
      this.selectedTenantId = tenantId;
    });
  }

  private initTestIdBridge(): void {
    if (typeof window === 'undefined') return;

    const syncAttr = (el: Element) => {
      if (el.hasAttribute('data-testid') && !el.hasAttribute('data-test-id')) {
        const value = el.getAttribute('data-testid');
        if (value) {
          el.setAttribute('data-test-id', value);
        }
      }
    };

    document.querySelectorAll('[data-testid]').forEach(syncAttr);

    const observer = new MutationObserver((mutations) => {
      for (const mutation of mutations) {
        if (mutation.type === 'attributes' && mutation.target instanceof Element) {
          syncAttr(mutation.target);
        }
        if (mutation.type === 'childList') {
          mutation.addedNodes.forEach((node) => {
            if (node instanceof Element) {
              syncAttr(node);
              node.querySelectorAll?.('[data-testid]').forEach(syncAttr);
            }
          });
        }
      }
    });

    observer.observe(document.body, {
      attributes: true,
      attributeFilter: ['data-testid'],
      childList: true,
      subtree: true,
    });
  }

  ngOnDestroy(): void {
    this.clearSessionTimeout();
    if (this.authSubscription) {
      this.authSubscription.unsubscribe();
    }
    if (this.tenantSubscription) {
      this.tenantSubscription.unsubscribe();
    }
    if (this.selectedTenantSubscription) {
      this.selectedTenantSubscription.unsubscribe();
    }
  }

  onTenantChange(nextTenantId: string): void {
    if (!nextTenantId || nextTenantId === this.selectedTenantId) {
      return;
    }
    this.authService.setTenantId(nextTenantId);
    this.selectedTenantId = nextTenantId;
  }

  // Listen for user activity to reset session timeout
  @HostListener('document:click')
  @HostListener('document:keypress')
  @HostListener('document:mousemove')
  @HostListener('document:scroll')
  onUserActivity(): void {
    if (this.authService.isAuthenticated()) {
      this.resetSessionTimeout();
    }
  }

  /**
   * Reset the session timeout on user activity
   */
  private resetSessionTimeout(): void {
    this.lastActivityTime = Date.now();
    this.showSessionWarning = false;
    this.clearSessionTimeout();

    // Set warning timer (fires 2 minutes before timeout)
    this.sessionWarningId = setTimeout(() => {
      this.showSessionWarning = true;
      this.sessionTimeRemaining = Math.floor(this.SESSION_WARNING_MS / 1000);
      // Start countdown
      this.sessionCountdownId = setInterval(() => {
        this.sessionTimeRemaining--;
        if (this.sessionTimeRemaining <= 0) {
          this.clearSessionCountdown();
        }
      }, 1000);
    }, this.SESSION_TIMEOUT_MS - this.SESSION_WARNING_MS);

    // Set actual timeout
    this.sessionTimeoutId = setTimeout(() => {
      this.onSessionTimeout();
    }, this.SESSION_TIMEOUT_MS);
  }

  /**
   * Clear session timeout timers
   */
  private clearSessionTimeout(): void {
    if (this.sessionTimeoutId) {
      clearTimeout(this.sessionTimeoutId);
      this.sessionTimeoutId = null;
    }
    if (this.sessionWarningId) {
      clearTimeout(this.sessionWarningId);
      this.sessionWarningId = null;
    }
    this.clearSessionCountdown();
  }

  private clearSessionCountdown(): void {
    if (this.sessionCountdownId) {
      clearInterval(this.sessionCountdownId);
      this.sessionCountdownId = null;
    }
  }

  /**
   * Handle session timeout - log out the user
   * HIPAA §164.312(a)(2)(iii) - Automatic Logoff with Audit Trail
   */
  private onSessionTimeout(): void {
    this.showSessionWarning = false;
    this.clearSessionTimeout();

    // HIPAA-compliant audit logging before logout
    this.auditService.logSessionTimeout({
      reason: 'IDLE_TIMEOUT',
      idleDurationMinutes: this.SESSION_TIMEOUT_MS / (60 * 1000),
      warningShown: true, // Warning was shown 2 minutes before timeout
    });

    this.logger.warn('Session timeout - logging out user due to inactivity');
    this.authService.logout();
  }

  /**
   * Extend the session (called when user clicks "Stay Logged In")
   */
  extendSession(): void {
    this.showSessionWarning = false;
    this.resetSessionTimeout();
  }

  toggleSidenav(): void {
    this.sidenavOpened = !this.sidenavOpened;
  }

  openGlobalSearch(): void {
    this.globalSearchService.openSearch();
  }

  toggleTheme(): void {
    this.themeService.toggleTheme();
  }

  get isDarkMode(): boolean {
    return false;
  }

  logout(): void {
    this.clearSessionTimeout();

    // Log explicit user logout (not automatic timeout)
    this.auditService.logSessionTimeout({
      reason: 'EXPLICIT_LOGOUT',
      warningShown: false,
    });

    this.authService.logout();
  }
}
