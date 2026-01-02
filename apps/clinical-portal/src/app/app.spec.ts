import { TestBed, ComponentFixture, fakeAsync, tick } from '@angular/core/testing';
import { App } from './app';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { provideRouter, Router } from '@angular/router';
import { AuthService } from './services/auth.service';
import { ThemeService } from './services/theme.service';
import { GlobalSearchService } from './shared/services/global-search.service';
import { BreadcrumbService } from './shared/services/breadcrumb.service';
import { BehaviorSubject, of } from 'rxjs';

describe('App', () => {
  let component: App;
  let fixture: ComponentFixture<App>;
  let authService: jest.Mocked<AuthService>;
  let router: jest.Mocked<Router>;
  let themeService: jest.Mocked<ThemeService>;
  let globalSearchService: jest.Mocked<GlobalSearchService>;
  let breadcrumbService: jest.Mocked<BreadcrumbService>;
  let isAuthenticatedSubject: BehaviorSubject<boolean>;

  beforeEach(async () => {
    isAuthenticatedSubject = new BehaviorSubject<boolean>(false);

    const authServiceMock = {
      isAuthenticated$: isAuthenticatedSubject.asObservable(),
      isAuthenticated: jest.fn().mockReturnValue(true),
      logout: jest.fn(),
    };

    const routerMock = {
      navigate: jest.fn(),
      url: '/dashboard',
    };

    const themeServiceMock = {
      initialize: jest.fn(),
      toggleTheme: jest.fn(),
      currentTheme: jest.fn().mockReturnValue('light'),
    };

    const globalSearchServiceMock = {
      openSearch: jest.fn(),
    };

    const breadcrumbServiceMock = {
      breadcrumbs$: new BehaviorSubject([]).asObservable(),
      setBreadcrumbs: jest.fn(),
    };

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
        { provide: ThemeService, useValue: themeServiceMock },
        { provide: GlobalSearchService, useValue: globalSearchServiceMock },
        { provide: BreadcrumbService, useValue: breadcrumbServiceMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).compileComponents();

    fixture = TestBed.createComponent(App);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService) as jest.Mocked<AuthService>;
    router = TestBed.inject(Router) as jest.Mocked<Router>;
    themeService = TestBed.inject(ThemeService) as jest.Mocked<ThemeService>;
    globalSearchService = TestBed.inject(GlobalSearchService) as jest.Mocked<GlobalSearchService>;
    breadcrumbService = TestBed.inject(BreadcrumbService) as jest.Mocked<BreadcrumbService>;
  });

  afterEach(() => {
    jest.clearAllTimers();
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize theme service on init', () => {
      component.ngOnInit();
      expect(themeService.initialize).toHaveBeenCalled();
    });

    it('should subscribe to authentication state on init', () => {
      component.ngOnInit();
      expect(authService.isAuthenticated$).toBeTruthy();
    });

    it('should start session timeout monitoring when user is authenticated', fakeAsync(() => {
      component.ngOnInit();
      isAuthenticatedSubject.next(true);
      tick();

      // Session timeout should be set
      expect(component['sessionTimeoutId']).toBeTruthy();
    }));

    it('should not start session timeout when user is not authenticated', fakeAsync(() => {
      component.ngOnInit();
      isAuthenticatedSubject.next(false);
      tick();

      // Session timeout should not be set
      expect(component['sessionTimeoutId']).toBeFalsy();
    }));
  });

  describe('Logout Functionality', () => {
    it('should call authService.logout when logout is called', () => {
      component.logout();
      expect(authService.logout).toHaveBeenCalled();
    });

    it('should clear session timeout on logout', () => {
      component.ngOnInit();
      isAuthenticatedSubject.next(true);

      const clearSpy = jest.spyOn(component as any, 'clearSessionTimeout');
      component.logout();

      expect(clearSpy).toHaveBeenCalled();
    });

    it('should clear all timers when logging out', fakeAsync(() => {
      component.ngOnInit();
      isAuthenticatedSubject.next(true);
      tick();

      component.logout();

      expect(component['sessionTimeoutId']).toBeNull();
      expect(component['sessionWarningId']).toBeNull();
      expect(component['sessionCountdownId']).toBeNull();
    }));
  });

  describe('Session Timeout Management', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('should show warning before session timeout', () => {
      component.ngOnInit();
      isAuthenticatedSubject.next(true);

      // Fast forward to warning time (13 minutes, 2 minutes before 15 min timeout)
      jest.advanceTimersByTime(13 * 60 * 1000);

      expect(component['showSessionWarning']).toBe(true);
      expect(component['sessionTimeRemaining']).toBeGreaterThan(0);
    });

    it('should logout user after session timeout', () => {
      component.ngOnInit();
      isAuthenticatedSubject.next(true);

      // Fast forward past timeout (15 minutes)
      jest.advanceTimersByTime(15 * 60 * 1000);

      expect(authService.logout).toHaveBeenCalled();
    });

    it('should reset session timeout on user activity', () => {
      component.ngOnInit();
      isAuthenticatedSubject.next(true);

      // Fast forward some time
      jest.advanceTimersByTime(5 * 60 * 1000);

      // Simulate user activity
      component.onUserActivity();

      // Timer should be reset
      expect(component['lastActivityTime']).toBeCloseTo(Date.now(), -2);
    });

    it('should hide warning on user activity', () => {
      component.ngOnInit();
      isAuthenticatedSubject.next(true);

      // Fast forward to warning time
      jest.advanceTimersByTime(13 * 60 * 1000);
      expect(component['showSessionWarning']).toBe(true);

      // Simulate user activity
      component.onUserActivity();

      expect(component['showSessionWarning']).toBe(false);
    });

    it('should extend session when user clicks Stay Logged In', () => {
      component.ngOnInit();
      isAuthenticatedSubject.next(true);

      // Fast forward to warning time
      jest.advanceTimersByTime(13 * 60 * 1000);
      expect(component['showSessionWarning']).toBe(true);

      // User extends session
      component.extendSession();

      expect(component['showSessionWarning']).toBe(false);
      // Timers should be reset
      expect(component['sessionTimeoutId']).toBeTruthy();
    });

    it('should countdown remaining time during warning', () => {
      component.ngOnInit();
      isAuthenticatedSubject.next(true);

      // Fast forward to warning time
      jest.advanceTimersByTime(13 * 60 * 1000);

      const initialTime = component['sessionTimeRemaining'];

      // Fast forward 1 second
      jest.advanceTimersByTime(1000);

      expect(component['sessionTimeRemaining']).toBe(initialTime - 1);
    });

    it('should clear countdown when time runs out', () => {
      component.ngOnInit();
      isAuthenticatedSubject.next(true);

      // Fast forward to warning time
      jest.advanceTimersByTime(13 * 60 * 1000);

      // Fast forward to end of warning period
      jest.advanceTimersByTime(2 * 60 * 1000);

      expect(component['sessionCountdownId']).toBeNull();
    });

    it('should not reset timeout if user is not authenticated', () => {
      authService.isAuthenticated.mockReturnValue(false);
      const resetSpy = jest.spyOn(component as any, 'resetSessionTimeout');

      component.onUserActivity();

      expect(resetSpy).not.toHaveBeenCalled();
    });

    it('should clear timers on component destroy', () => {
      component.ngOnInit();
      isAuthenticatedSubject.next(true);

      component.ngOnDestroy();

      expect(component['sessionTimeoutId']).toBeNull();
      expect(component['sessionWarningId']).toBeNull();
    });

    it('should unsubscribe from auth state on destroy', () => {
      component.ngOnInit();
      const subscription = component['authSubscription'];

      const unsubscribeSpy = jest.spyOn(subscription!, 'unsubscribe');
      component.ngOnDestroy();

      expect(unsubscribeSpy).toHaveBeenCalled();
    });
  });

  describe('User Activity Tracking', () => {
    it('should track click events', () => {
      authService.isAuthenticated.mockReturnValue(true);
      const resetSpy = jest.spyOn(component as any, 'resetSessionTimeout');

      component.onUserActivity();

      expect(resetSpy).toHaveBeenCalled();
    });

    it('should update last activity time on activity', () => {
      authService.isAuthenticated.mockReturnValue(true);
      const beforeTime = Date.now();

      component.onUserActivity();

      expect(component['lastActivityTime']).toBeGreaterThanOrEqual(beforeTime);
    });
  });

  describe('UI Interactions', () => {
    it('should toggle sidenav', () => {
      const initialState = component['sidenavOpened'];
      component.toggleSidenav();
      expect(component['sidenavOpened']).toBe(!initialState);
    });

    it('should open global search', () => {
      component.openGlobalSearch();
      expect(globalSearchService.openSearch).toHaveBeenCalled();
    });

    it('should toggle theme', () => {
      component.toggleTheme();
      expect(themeService.toggleTheme).toHaveBeenCalled();
    });

    it('should return dark mode status', () => {
      themeService.currentTheme.mockReturnValue('dark');
      expect(component.isDarkMode).toBe(true);

      themeService.currentTheme.mockReturnValue('light');
      expect(component.isDarkMode).toBe(false);
    });
  });

  describe('Navigation Items', () => {
    it('should have defined navigation items', () => {
      expect(component['navItems']).toBeDefined();
      expect(component['navItems'].length).toBeGreaterThan(0);
    });

    it('should have valid navigation structure', () => {
      component['navItems'].forEach((item) => {
        expect(item).toHaveProperty('path');
        expect(item).toHaveProperty('icon');
        expect(item).toHaveProperty('label');
      });
    });
  });

  describe('Session Timeout Configuration', () => {
    it('should have correct session timeout duration', () => {
      expect(component['SESSION_TIMEOUT_MS']).toBe(15 * 60 * 1000); // 15 minutes
    });

    it('should have correct warning duration', () => {
      expect(component['SESSION_WARNING_MS']).toBe(2 * 60 * 1000); // 2 minutes
    });

    it('should show warning at correct time before timeout', () => {
      const warningTime = component['SESSION_TIMEOUT_MS'] - component['SESSION_WARNING_MS'];
      expect(warningTime).toBe(13 * 60 * 1000); // 13 minutes
    });
  });
});
