import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideRouter, Router, ActivatedRoute } from '@angular/router';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { AuthService } from '../../services/auth.service';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authService: jest.Mocked<AuthService>;
  let router: Router;

  const mockLoginResponse = {
    accessToken: 'mock-token',
    refreshToken: 'mock-refresh',
    tokenType: 'Bearer',
    expiresIn: 3600,
    user: {
      id: 'user-1',
      username: 'testuser',
      email: 'test@example.com',
      firstName: 'Test',
      lastName: 'User',
      fullName: 'Test User',
      roles: [{ id: 'role-1', name: 'USER' }],
      tenantId: 'tenant-1',
      active: true,
    },
  };

  beforeEach(async () => {
    const authServiceMock = {
      login: jest.fn(),
      isAuthenticated: jest.fn().mockReturnValue(false),
      isMfaRequired: jest.fn().mockReturnValue(false),
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent, NoopAnimationsModule],
      providers: [
        provideRouter([
          { path: 'login', component: LoginComponent },
          { path: 'dashboard', component: LoginComponent },
        ]),
        { provide: AuthService, useValue: authServiceMock },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParams: {},
            },
          },
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService) as jest.Mocked<AuthService>;
    router = TestBed.inject(Router);

    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have login form with username and password', () => {
    expect(component.loginForm.get('username')).toBeTruthy();
    expect(component.loginForm.get('password')).toBeTruthy();
    expect(component.loginForm.get('rememberMe')).toBeTruthy();
  });

  it('should require username and password', () => {
    const form = component.loginForm;
    expect(form.valid).toBe(false);

    form.patchValue({ username: 'test', password: 'password' });
    expect(form.valid).toBe(true);
  });

  it('should not submit invalid form', () => {
    component.onSubmit();
    expect(authService.login).not.toHaveBeenCalled();
  });

  it('should call login service on submit', fakeAsync(() => {
    const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);
    authService.login.mockReturnValue(of(mockLoginResponse));

    component.loginForm.patchValue({
      username: 'testuser',
      password: 'testpass',
    });
    component.onSubmit();

    tick();

    expect(authService.login).toHaveBeenCalledWith('testuser', 'testpass');
    expect(navigateSpy).toHaveBeenCalledWith(['/dashboard']);
  }));

  it('should handle login failure', fakeAsync(() => {
    const error = { status: 401 };
    authService.login.mockReturnValue(throwError(() => error));

    component.loginForm.patchValue({
      username: 'testuser',
      password: 'wrongpass',
    });
    component.onSubmit();

    tick();

    expect(component.isLoading).toBe(false);
  }));

  it('should toggle password visibility', () => {
    expect(component.hidePassword).toBe(true);
    component.hidePassword = !component.hidePassword;
    expect(component.hidePassword).toBe(false);
  });

  it('should redirect if already authenticated', () => {
    authService.isAuthenticated.mockReturnValue(true);
    const navigateSpy = jest.spyOn(router, 'navigate').mockResolvedValue(true);

    component.ngOnInit();

    expect(navigateSpy).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should set loading state during login', fakeAsync(() => {
    authService.login.mockReturnValue(of(mockLoginResponse));
    jest.spyOn(router, 'navigate').mockResolvedValue(true);

    component.loginForm.patchValue({
      username: 'testuser',
      password: 'testpass',
    });

    expect(component.isLoading).toBe(false);
    component.onSubmit();
    expect(component.isLoading).toBe(true);

    tick();
  }));

  it('should have demo mode enabled', () => {
    expect(component.isDemoMode).toBe(true);
  });

  it('should set return URL from query params', () => {
    expect(component.returnUrl).toBe('/dashboard');
  });
});
