import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { SalesLoginComponent } from './sales-login.component';
import { SalesAuthService } from '../../../services/sales-auth.service';

// Helper to create mock service with Jest
function createMockAuthService() {
  return {
    login: jest.fn(),
  };
}

function createMockRouter() {
  return {
    navigate: jest.fn(),
  };
}

type MockAuthService = ReturnType<typeof createMockAuthService>;
type MockRouter = ReturnType<typeof createMockRouter>;

describe('SalesLoginComponent', () => {
  let component: SalesLoginComponent;
  let fixture: ComponentFixture<SalesLoginComponent>;
  let mockAuthService: MockAuthService;
  let mockRouter: MockRouter;

  beforeEach(async () => {
    mockAuthService = createMockAuthService();
    mockRouter = createMockRouter();

    // Default mock return
    mockAuthService.login.mockReturnValue(of({ token: 'test-token' }));

    await TestBed.configureTestingModule({
      imports: [SalesLoginComponent, FormsModule],
      providers: [
        { provide: SalesAuthService, useValue: mockAuthService },
        { provide: Router, useValue: mockRouter },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(SalesLoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('Initial State', () => {
    it('should have empty email and password', () => {
      expect(component.email).toBe('');
      expect(component.password).toBe('');
    });

    it('should not be loading initially', () => {
      expect(component.isLoading()).toBe(false);
    });

    it('should have no error message initially', () => {
      expect(component.errorMessage()).toBeNull();
    });
  });

  describe('Form Display', () => {
    it('should render login form', () => {
      const form = fixture.nativeElement.querySelector('.login-form');
      expect(form).toBeTruthy();
    });

    it('should render email input', () => {
      const emailInput = fixture.nativeElement.querySelector('input[type="email"]');
      expect(emailInput).toBeTruthy();
    });

    it('should render password input', () => {
      const passwordInput = fixture.nativeElement.querySelector('input[type="password"]');
      expect(passwordInput).toBeTruthy();
    });

    it('should render sign in button', () => {
      const button = fixture.nativeElement.querySelector('.btn-login');
      expect(button).toBeTruthy();
      expect(button.textContent).toContain('Sign In');
    });

    it('should display demo credentials', () => {
      const footer = fixture.nativeElement.querySelector('.demo-credentials');
      expect(footer).toBeTruthy();
      expect(footer.textContent).toContain('sales@hdim.health');
    });
  });

  describe('Form Validation', () => {
    it('should show error when email is empty', fakeAsync(() => {
      component.email = '';
      component.password = 'password123';

      component.onSubmit();
      tick();

      expect(component.errorMessage()).toBe('Please enter email and password');
      expect(mockAuthService.login).not.toHaveBeenCalled();
    }));

    it('should show error when password is empty', fakeAsync(() => {
      component.email = 'test@example.com';
      component.password = '';

      component.onSubmit();
      tick();

      expect(component.errorMessage()).toBe('Please enter email and password');
      expect(mockAuthService.login).not.toHaveBeenCalled();
    }));

    it('should show error when both fields are empty', fakeAsync(() => {
      component.email = '';
      component.password = '';

      component.onSubmit();
      tick();

      expect(component.errorMessage()).toBe('Please enter email and password');
      expect(mockAuthService.login).not.toHaveBeenCalled();
    }));
  });

  describe('Successful Login', () => {
    beforeEach(() => {
      component.email = 'sales@hdim.health';
      component.password = 'sales2026!';
    });

    it('should call login service with credentials', fakeAsync(() => {
      component.onSubmit();
      tick();

      expect(mockAuthService.login).toHaveBeenCalledWith({
        email: 'sales@hdim.health',
        password: 'sales2026!',
      });
    }));

    it('should set loading state during login', fakeAsync(() => {
      // Create a subject to control the observable timing
      const loginSubject = of({ token: 'test-token' });
      mockAuthService.login.mockReturnValue(loginSubject);

      component.onSubmit();

      // During the login process
      expect(component.isLoading()).toBe(true);

      tick();
    }));

    it('should navigate to dashboard on success', fakeAsync(() => {
      component.onSubmit();
      tick();

      expect(mockRouter.navigate).toHaveBeenCalledWith(['/sales/dashboard']);
    }));

    it('should clear error message on submit', fakeAsync(() => {
      component.errorMessage.set('Previous error');

      component.onSubmit();
      tick();

      // Error should be cleared before attempting login
      expect(component.errorMessage()).toBeNull();
    }));
  });

  describe('Failed Login', () => {
    beforeEach(() => {
      component.email = 'wrong@example.com';
      component.password = 'wrongpassword';
    });

    it('should display error message on login failure', fakeAsync(() => {
      mockAuthService.login.mockReturnValue(
        throwError(() => new Error('Invalid credentials'))
      );

      component.onSubmit();
      tick();

      expect(component.errorMessage()).toBe('Invalid credentials');
    }));

    it('should display default error when no message provided', fakeAsync(() => {
      mockAuthService.login.mockReturnValue(
        throwError(() => ({}))
      );

      component.onSubmit();
      tick();

      expect(component.errorMessage()).toBe('Login failed. Please try again.');
    }));

    it('should stop loading on error', fakeAsync(() => {
      mockAuthService.login.mockReturnValue(
        throwError(() => new Error('Invalid credentials'))
      );

      component.onSubmit();
      tick();

      expect(component.isLoading()).toBe(false);
    }));

    it('should not navigate on error', fakeAsync(() => {
      mockAuthService.login.mockReturnValue(
        throwError(() => new Error('Invalid credentials'))
      );

      component.onSubmit();
      tick();

      expect(mockRouter.navigate).not.toHaveBeenCalled();
    }));
  });

  describe('UI State Changes', () => {
    it('should display error message in DOM when set', fakeAsync(() => {
      component.errorMessage.set('Test error');
      fixture.detectChanges();

      const errorEl = fixture.nativeElement.querySelector('.error-message');
      expect(errorEl).toBeTruthy();
      expect(errorEl.textContent).toContain('Test error');
    }));

    it('should not display error message when null', () => {
      component.errorMessage.set(null);
      fixture.detectChanges();

      const errorEl = fixture.nativeElement.querySelector('.error-message');
      expect(errorEl).toBeFalsy();
    });

    it('should disable inputs when loading', fakeAsync(() => {
      component.isLoading.set(true);
      fixture.detectChanges();
      tick();
      fixture.detectChanges();

      const emailInput = fixture.nativeElement.querySelector('input[type="email"]');
      const passwordInput = fixture.nativeElement.querySelector('input[type="password"]');

      // Check for disabled attribute (Angular binding may use attribute)
      expect(emailInput.hasAttribute('disabled') || emailInput.disabled).toBe(true);
      expect(passwordInput.hasAttribute('disabled') || passwordInput.disabled).toBe(true);
    }));

    it('should disable submit button when loading', fakeAsync(() => {
      component.isLoading.set(true);
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector('.btn-login');
      expect(button.disabled).toBe(true);
    }));

    it('should show spinner when loading', fakeAsync(() => {
      component.isLoading.set(true);
      fixture.detectChanges();

      const spinner = fixture.nativeElement.querySelector('.loading-spinner');
      expect(spinner).toBeTruthy();
    }));
  });
});
