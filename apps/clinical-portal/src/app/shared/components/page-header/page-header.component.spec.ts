import { ComponentFixture, TestBed } from '@angular/core/testing';
import { PageHeaderComponent, Breadcrumb } from './page-header.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { RouterTestingModule } from '@angular/router/testing';

describe('PageHeaderComponent', () => {
  let component: PageHeaderComponent;
  let fixture: ComponentFixture<PageHeaderComponent>;

  const mockBreadcrumbs: Breadcrumb[] = [
    { label: 'Home', route: '/', icon: 'home' },
    { label: 'Patients', route: '/patients' },
    { label: 'John Doe' }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        PageHeaderComponent,
        NoopAnimationsModule,
        RouterTestingModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PageHeaderComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display title', () => {
    component.title = 'Patient Details';
    fixture.detectChanges();

    const title = fixture.nativeElement.querySelector('.page-title');
    expect(title.textContent).toContain('Patient Details');
  });

  it('should display subtitle when provided', () => {
    component.title = 'Patients';
    component.subtitle = 'Manage patient records';
    fixture.detectChanges();

    const subtitle = fixture.nativeElement.querySelector('.page-subtitle');
    expect(subtitle).toBeTruthy();
    expect(subtitle.textContent).toContain('Manage patient records');
  });

  it('should not display subtitle when not provided', () => {
    component.title = 'Patients';
    fixture.detectChanges();

    const subtitle = fixture.nativeElement.querySelector('.page-subtitle');
    expect(subtitle).toBeFalsy();
  });

  describe('Breadcrumbs', () => {
    beforeEach(() => {
      component.breadcrumbs = mockBreadcrumbs;
      fixture.detectChanges();
    });

    it('should display breadcrumbs', () => {
      const breadcrumbNav = fixture.nativeElement.querySelector('.breadcrumbs');
      expect(breadcrumbNav).toBeTruthy();
    });

    it('should not display breadcrumbs when empty', () => {
      component.breadcrumbs = [];
      fixture.detectChanges();

      const breadcrumbNav = fixture.nativeElement.querySelector('.breadcrumbs');
      expect(breadcrumbNav).toBeFalsy();
    });

    it('should render all breadcrumb items', () => {
      const items = fixture.nativeElement.querySelectorAll('.breadcrumb-item');
      expect(items.length).toBe(3);
    });

    it('should render links for breadcrumbs with routes', () => {
      const links = fixture.nativeElement.querySelectorAll('.breadcrumb-link');
      expect(links.length).toBe(2); // Home and Patients have routes
    });

    it('should render text for breadcrumbs without routes', () => {
      const currentCrumb = fixture.nativeElement.querySelector('.breadcrumb-current .breadcrumb-text');
      expect(currentCrumb).toBeTruthy();
      expect(currentCrumb.textContent).toContain('John Doe');
    });

    it('should display icons when provided', () => {
      const icons = fixture.nativeElement.querySelectorAll('.breadcrumb-icon');
      expect(icons.length).toBeGreaterThan(0);
      expect(icons[0].textContent).toContain('home');
    });

    it('should display separators between breadcrumbs', () => {
      const separators = fixture.nativeElement.querySelectorAll('.breadcrumb-separator');
      expect(separators.length).toBe(2); // 3 items = 2 separators
    });

    it('should mark last breadcrumb as current', () => {
      const currentCrumb = fixture.nativeElement.querySelector('.breadcrumb-current');
      expect(currentCrumb).toBeTruthy();
      expect(currentCrumb.getAttribute('aria-current')).toBeFalsy(); // aria-current is on span inside

      const currentSpan = currentCrumb.querySelector('span[aria-current="page"]');
      expect(currentSpan).toBeTruthy();
    });
  });

  describe('Helper methods', () => {
    beforeEach(() => {
      component.breadcrumbs = mockBreadcrumbs;
    });

    it('should return last breadcrumb', () => {
      const last = component.getLastBreadcrumb();
      expect(last?.label).toBe('John Doe');
    });

    it('should return parent breadcrumbs', () => {
      const parents = component.getParentBreadcrumbs();
      expect(parents.length).toBe(2);
      expect(parents[0].label).toBe('Home');
      expect(parents[1].label).toBe('Patients');
    });

    it('should check if breadcrumb has route', () => {
      expect(component.hasRoute(mockBreadcrumbs[0])).toBe(true);
      expect(component.hasRoute(mockBreadcrumbs[2])).toBe(false);
    });
  });

  describe('Back button', () => {
    it('should show back button when showBackButton is true', () => {
      component.showBackButton = true;
      fixture.detectChanges();

      const backButton = fixture.nativeElement.querySelector('.back-button');
      expect(backButton).toBeTruthy();
    });

    it('should not show back button when showBackButton is false', () => {
      component.showBackButton = false;
      fixture.detectChanges();

      const backButton = fixture.nativeElement.querySelector('.back-button');
      expect(backButton).toBeFalsy();
    });

    it('should have aria-label on back button', () => {
      component.showBackButton = true;
      fixture.detectChanges();

      const backButton = fixture.nativeElement.querySelector('.back-button');
      expect(backButton.getAttribute('aria-label')).toBe('Go back');
    });
  });

  describe('Actions (Content Projection)', () => {
    it('should render projected content in actions area', () => {
      const testFixture = TestBed.createComponent(PageHeaderComponent);
      const testComponent = testFixture.componentInstance;
      testComponent.title = 'Test';

      // Create a button element to project
      const button = document.createElement('button');
      button.textContent = 'Action Button';
      button.className = 'test-action-button';

      testFixture.detectChanges();

      const actionsArea = testFixture.nativeElement.querySelector('.page-header-actions');
      expect(actionsArea).toBeTruthy();
    });
  });

  describe('Accessibility', () => {
    beforeEach(() => {
      component.breadcrumbs = mockBreadcrumbs;
      fixture.detectChanges();
    });

    it('should have aria-label on breadcrumb navigation', () => {
      const nav = fixture.nativeElement.querySelector('nav.breadcrumbs');
      expect(nav.getAttribute('aria-label')).toBe('Breadcrumb navigation');
    });

    it('should have aria-current on current page breadcrumb', () => {
      const currentSpan = fixture.nativeElement.querySelector('.breadcrumb-current span[aria-current]');
      expect(currentSpan.getAttribute('aria-current')).toBe('page');
    });

    it('should have aria-hidden on separators', () => {
      const separators = fixture.nativeElement.querySelectorAll('.breadcrumb-separator');
      separators.forEach((separator: HTMLElement) => {
        expect(separator.getAttribute('aria-hidden')).toBe('true');
      });
    });

    it('should have descriptive aria-label on breadcrumb links', () => {
      const links = fixture.nativeElement.querySelectorAll('.breadcrumb-link');
      expect(links[0].getAttribute('aria-label')).toBe('Navigate to Home');
    });
  });

  it('should apply custom class when provided', () => {
    component.customClass = 'custom-header';
    fixture.detectChanges();

    const header = fixture.nativeElement.querySelector('.page-header');
    expect(header.classList.contains('custom-header')).toBe(true);
  });
});
