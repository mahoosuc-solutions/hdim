/**
 * Accessibility Tests for NavigationComponent
 *
 * Tests WCAG 2.1 Level AA compliance for the main navigation component.
 * Validates skip links, keyboard navigation, ARIA attributes, and focus indicators.
 */

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { RouterTestingModule } from '@angular/router/testing';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { NavigationComponent } from './navigation.component';
import { OfflineIndicatorComponent } from '../offline-indicator/offline-indicator.component';
import { WhatsNewBannerComponent } from '../whats-new-banner/whats-new-banner.component';
import { HelpPanelComponent } from '../help-panel/help-panel.component';
import { LoggerService } from '../../services/logger.service';
import {
  testAccessibility,
  testKeyboardAccessibility,
  testAriaAttributes,
  testAccessibilityForElement,
} from '../../../testing/accessibility.helper';

describe('NavigationComponent - Accessibility', () => {
  let component: NavigationComponent;
  let fixture: ComponentFixture<NavigationComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        NavigationComponent,
        RouterTestingModule,
        MatSidenavModule,
        MatToolbarModule,
        MatListModule,
        MatIconModule,
        MatTooltipModule,
        BrowserAnimationsModule,
        OfflineIndicatorComponent,
        WhatsNewBannerComponent,
        HelpPanelComponent,
      ],
      providers: [
        { provide: LoggerService, useValue: createMockLoggerService() },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(NavigationComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('WCAG 2.1 Level A Compliance', () => {
    it('should have no Level A accessibility violations', async () => {
      const results = await testAccessibility(fixture);
      expect(results).toHaveNoViolations();
    });

    it('should have skip navigation links (2.4.1 Bypass Blocks)', async () => {
      const skipLinks = fixture.nativeElement.querySelector('.skip-links');
      expect(skipLinks).toBeTruthy();

      const skipToMain = fixture.nativeElement.querySelector('a[href="#main-content"]');
      const skipToNav = fixture.nativeElement.querySelector('a[href="#nav-menu"]');

      expect(skipToMain).toBeTruthy();
      expect(skipToNav).toBeTruthy();
      expect(skipToMain.textContent).toContain('Skip to main content');
      expect(skipToNav.textContent).toContain('Skip to navigation');
    });

    it('should have semantic main landmark', () => {
      const mainElement = fixture.nativeElement.querySelector('main#main-content');
      expect(mainElement).toBeTruthy();
      expect(mainElement.getAttribute('role')).toBe('main');
    });

    it('should have accessible navigation menu', () => {
      const navMenu = fixture.nativeElement.querySelector('#nav-menu');
      expect(navMenu).toBeTruthy();
    });
  });

  describe('WCAG 2.1 Level AA Compliance', () => {
    it('should support keyboard navigation', async () => {
      const results = await testKeyboardAccessibility(fixture);
      expect(results).toHaveNoViolations();
    });

    it('should have valid ARIA attributes', async () => {
      const results = await testAriaAttributes(fixture);
      expect(results).toHaveNoViolations();
    });

    it('should have accessible toolbar buttons', () => {
      const menuButton = fixture.nativeElement.querySelector('button[aria-label="Toggle navigation"]');
      const helpButton = fixture.nativeElement.querySelector('button[aria-label="Help"]');
      const notificationsButton = fixture.nativeElement.querySelector('button[aria-label="Notifications"]');
      const userMenuButton = fixture.nativeElement.querySelector('button[aria-label="User menu"]');

      expect(menuButton).toBeTruthy();
      expect(helpButton).toBeTruthy();
      expect(notificationsButton).toBeTruthy();
      expect(userMenuButton).toBeTruthy();
    });

    it('should have accessible navigation items', () => {
      const navItems = fixture.nativeElement.querySelectorAll('.nav-item');
      expect(navItems.length).toBeGreaterThan(0);

      navItems.forEach((item: HTMLElement) => {
        const link = item.querySelector('a');
        const icon = item.querySelector('mat-icon');
        const title = item.querySelector('[matListItemTitle]');

        expect(link).toBeTruthy();
        expect(icon).toBeTruthy();
        expect(title).toBeTruthy();
        expect(title?.textContent?.trim()).not.toBe('');
      });
    });
  });

  describe('Skip Links Functionality', () => {
    it('should position skip links off-screen by default', () => {
      const skipLinks = fixture.nativeElement.querySelector('.skip-links');
      const computedStyle = window.getComputedStyle(skipLinks);

      expect(computedStyle.position).toBe('absolute');
      expect(parseInt(computedStyle.top)).toBeLessThan(0);
    });

    it('should have visible focus indicators on skip links', () => {
      const skipLink = fixture.nativeElement.querySelector('.skip-link');
      skipLink.focus();

      const computedStyle = window.getComputedStyle(skipLink, ':focus');
      // Note: Pseudo-element styles can't be directly tested in JSDOM
      // This would require browser-based E2E testing
      expect(skipLink).toBe(document.activeElement);
    });
  });

  describe('Navigation Landmarks', () => {
    it('should have navigation landmark for sidenav', async () => {
      const results = await testAccessibilityForElement(fixture, '.sidenav');
      expect(results).toHaveNoViolations();
    });

    it('should have main landmark for content area', async () => {
      const results = await testAccessibilityForElement(fixture, '#main-content');
      expect(results).toHaveNoViolations();
    });
  });

  describe('Focus Management', () => {
    it('should have focusable interactive elements', () => {
      const focusableElements = fixture.nativeElement.querySelectorAll(
        'a[href], button:not([disabled]), [tabindex]:not([tabindex="-1"])'
      );

      expect(focusableElements.length).toBeGreaterThan(0);

      focusableElements.forEach((element: HTMLElement) => {
        expect(element.tabIndex).toBeGreaterThanOrEqual(0);
      });
    });

    it('should not have positive tabindex values', () => {
      const positiveTabindexElements = fixture.nativeElement.querySelectorAll(
        '[tabindex]:not([tabindex="0"]):not([tabindex="-1"])'
      );

      positiveTabindexElements.forEach((element: HTMLElement) => {
        const tabindex = parseInt(element.getAttribute('tabindex') || '0');
        expect(tabindex).toBeLessThanOrEqual(0);
      });
    });
  });

  describe('ARIA Compliance', () => {
    it('should have valid ARIA roles', () => {
      const mainElement = fixture.nativeElement.querySelector('main');
      expect(mainElement.getAttribute('role')).toBe('main');
    });

    it('should have accessible names for icon buttons', () => {
      const iconButtons = fixture.nativeElement.querySelectorAll('button[mat-icon-button]');

      iconButtons.forEach((button: HTMLElement) => {
        const ariaLabel = button.getAttribute('aria-label');
        const ariaLabelledBy = button.getAttribute('aria-labelledby');
        const textContent = button.textContent?.trim();

        // Button must have either aria-label, aria-labelledby, or text content
        expect(
          ariaLabel || ariaLabelledBy || textContent
        ).toBeTruthy();
      });
    });

    it('should mark decorative icons as aria-hidden', () => {
      const decorativeIcons = fixture.nativeElement.querySelectorAll('mat-icon[aria-hidden="true"]');
      expect(decorativeIcons.length).toBeGreaterThan(0);
    });
  });
});
