import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { NavigationComponent, NavigationItem } from './navigation.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { LoggerService } from '../../services/logger.service';
import { createMockRouter } from '../../testing/mocks';

describe('NavigationComponent (TDD)', () => {
  let component: NavigationComponent;
  let fixture: ComponentFixture<NavigationComponent>;
  let routerUrl: string;

  beforeEach(async () => {
    routerUrl = '/';

    const mockRouter = {
      get url() {
        return routerUrl;
      },
      navigate: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [NavigationComponent, NoopAnimationsModule],
      providers: [{ provide: Router, useValue: mockRouter },
        { provide: LoggerService, useValue: createMockLoggerService() },
        { provide: Router, useValue: createMockRouter() }],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(NavigationComponent);
    component = fixture.componentInstance;
    // Do NOT call fixture.detectChanges() to avoid template rendering issues
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with sidenav opened', () => {
      expect(component.sidenavOpened).toBe(true);
    });

    it('should have navigation items defined', () => {
      expect(component.navigationItems).toBeDefined();
      expect(component.navigationItems.length).toBeGreaterThan(0);
    });

    it('should have exactly 7 navigation items', () => {
      expect(component.navigationItems).toHaveLength(7);
    });
  });

  describe('Navigation Items Structure', () => {
    it('should have Dashboard as first item', () => {
      const dashboard = component.navigationItems[0];
      expect(dashboard.label).toBe('Dashboard');
      expect(dashboard.route).toBe('/');
      expect(dashboard.icon).toBe('dashboard');
    });

    it('should have Patients item', () => {
      const patients = component.navigationItems.find((item) => item.label === 'Patients');
      expect(patients).toBeDefined();
      expect(patients?.route).toBe('/patients');
      expect(patients?.icon).toBe('people');
    });

    it('should have Evaluations item', () => {
      const evaluations = component.navigationItems.find((item) => item.label === 'Evaluations');
      expect(evaluations).toBeDefined();
      expect(evaluations?.route).toBe('/evaluations');
      expect(evaluations?.icon).toBe('assessment');
    });

    it('should have Results item', () => {
      const results = component.navigationItems.find((item) => item.label === 'Results');
      expect(results).toBeDefined();
      expect(results?.route).toBe('/results');
      expect(results?.icon).toBe('bar_chart');
    });

    it('should have Reports item', () => {
      const reports = component.navigationItems.find((item) => item.label === 'Reports');
      expect(reports).toBeDefined();
      expect(reports?.route).toBe('/reports');
      expect(reports?.icon).toBe('description');
    });

    it('should have Visualizations item', () => {
      const viz = component.navigationItems.find((item) => item.label === 'Visualizations');
      expect(viz).toBeDefined();
      expect(viz?.route).toBe('/visualization');
      expect(viz?.icon).toBe('3d_rotation');
    });

    it('should have Agent Builder item', () => {
      const agentBuilder = component.navigationItems.find((item) => item.label === 'Agent Builder');
      expect(agentBuilder).toBeDefined();
      expect(agentBuilder?.route).toBe('/agent-builder');
      expect(agentBuilder?.icon).toBe('smart_toy');
    });

    it('should have description for each navigation item', () => {
      component.navigationItems.forEach((item) => {
        expect(item.description).toBeDefined();
        expect(item.description.length).toBeGreaterThan(0);
      });
    });

    it('should have unique routes for each navigation item', () => {
      const routes = component.navigationItems.map((item) => item.route);
      const uniqueRoutes = new Set(routes);
      expect(routes.length).toBe(uniqueRoutes.size);
    });
  });

  describe('Active Route Detection', () => {
    it('should identify dashboard as active when on root route', () => {
      routerUrl = '/';

      const isActive = component.isActive('/');

      expect(isActive).toBe(true);
    });

    it('should identify patients page as active', () => {
      routerUrl = '/patients';

      const isActive = component.isActive('/patients');

      expect(isActive).toBe(true);
    });

    it('should identify evaluations page as active', () => {
      routerUrl = '/evaluations';

      const isActive = component.isActive('/evaluations');

      expect(isActive).toBe(true);
    });

    it('should identify results page as active', () => {
      routerUrl = '/results';

      const isActive = component.isActive('/results');

      expect(isActive).toBe(true);
    });

    it('should identify reports page as active', () => {
      routerUrl = '/reports';

      const isActive = component.isActive('/reports');

      expect(isActive).toBe(true);
    });

    it('should identify visualizations page as active', () => {
      routerUrl = '/visualization';

      const isActive = component.isActive('/visualization');

      expect(isActive).toBe(true);
    });

    it('should return false for inactive routes', () => {
      routerUrl = '/patients';

      const isActive = component.isActive('/reports');

      expect(isActive).toBe(false);
    });

    it('should handle dashboard route when on root', () => {
      routerUrl = '/';

      const isDashboardActive = component.isActive('/');

      expect(isDashboardActive).toBe(true);
    });

    it('should not identify partial route matches as active', () => {
      routerUrl = '/patients/123';

      const isActive = component.isActive('/patients');

      expect(isActive).toBe(false);
    });
  });

  describe('Sidenav Toggle', () => {
    it('should toggle sidenav from opened to closed', () => {
      component.sidenavOpened = true;

      component.toggleSidenav();

      expect(component.sidenavOpened).toBe(false);
    });

    it('should toggle sidenav from closed to opened', () => {
      component.sidenavOpened = false;

      component.toggleSidenav();

      expect(component.sidenavOpened).toBe(true);
    });

    it('should toggle sidenav multiple times', () => {
      const initialState = component.sidenavOpened;

      component.toggleSidenav();
      expect(component.sidenavOpened).toBe(!initialState);

      component.toggleSidenav();
      expect(component.sidenavOpened).toBe(initialState);

      component.toggleSidenav();
      expect(component.sidenavOpened).toBe(!initialState);
    });
  });

  describe('Navigation Item Properties', () => {
    it('should have NavigationItem type with correct properties', () => {
      const item: NavigationItem = {
        label: 'Test',
        icon: 'test_icon',
        route: '/test',
        description: 'Test description',
      };

      expect(item.label).toBe('Test');
      expect(item.icon).toBe('test_icon');
      expect(item.route).toBe('/test');
      expect(item.description).toBe('Test description');
    });

    it('should have all required properties for each item', () => {
      component.navigationItems.forEach((item) => {
        expect(item.label).toBeDefined();
        expect(item.icon).toBeDefined();
        expect(item.route).toBeDefined();
        expect(item.description).toBeDefined();

        expect(typeof item.label).toBe('string');
        expect(typeof item.icon).toBe('string');
        expect(typeof item.route).toBe('string');
        expect(typeof item.description).toBe('string');
      });
    });

    it('should have Material icon names for all items', () => {
      const validIconNames = [
        'dashboard',
        'people',
        'assessment',
        'bar_chart',
        'description',
        '3d_rotation',
        'smart_toy',
      ];

      component.navigationItems.forEach((item) => {
        expect(validIconNames).toContain(item.icon);
      });
    });

    it('should have routes starting with forward slash', () => {
      component.navigationItems.forEach((item) => {
        expect(item.route).toMatch(/^\//);
      });
    });
  });

  describe('Router Integration', () => {
    it('should have Router injected', () => {
      expect(component['router']).toBeDefined();
    });

    it('should use router.url for active route detection', () => {
      routerUrl = '/patients';

      const isActive = component.isActive('/patients');

      expect(isActive).toBe(true);
    });

    it('should handle different route patterns', () => {
      const routes = [
        '/',
        '/patients',
        '/evaluations',
        '/results',
        '/reports',
        '/visualization',
        '/agent-builder',
      ];

      routes.forEach((route) => {
        routerUrl = route;
        expect(component.isActive(route)).toBe(true);
        expect(component.isActive('/other')).toBe(false);
      });
    });
  });

  describe('Edge Cases', () => {
    it('should handle empty router URL', () => {
      routerUrl = '';

      const isActive = component.isActive('/');

      expect(isActive).toBe(false);
    });

    it('should handle null router URL gracefully', () => {
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      routerUrl = null as any;

      expect(() => component.isActive('/')).not.toThrow();
    });

    it('should maintain sidenav state across route changes', () => {
      component.sidenavOpened = false;
      routerUrl = '/patients';

      expect(component.sidenavOpened).toBe(false);

      routerUrl = '/reports';

      expect(component.sidenavOpened).toBe(false);
    });

    it('should handle route with query parameters', () => {
      routerUrl = '/patients?id=123';

      const isPatientsActive = component.isActive('/patients');

      expect(isPatientsActive).toBe(false); // Exact match only
    });

    it('should handle route with hash fragments', () => {
      routerUrl = '/reports#section1';

      const isReportsActive = component.isActive('/reports');

      expect(isReportsActive).toBe(false); // Exact match only
    });
  });

  describe('Component State', () => {
    it('should maintain navigation items immutability', () => {
      const originalItems = [...component.navigationItems];

      component.toggleSidenav();

      expect(component.navigationItems).toEqual(originalItems);
    });

    it('should have consistent navigation items count', () => {
      const count1 = component.navigationItems.length;

      component.toggleSidenav();
      routerUrl = '/patients';

      const count2 = component.navigationItems.length;

      expect(count1).toBe(count2);
    });
  });
});
