import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { RouterTestingModule } from '@angular/router/testing';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { TenantRegistrationComponent } from './tenant-registration.component';

describe('TenantRegistrationComponent', () => {
  let component: TenantRegistrationComponent;
  let fixture: ComponentFixture<TenantRegistrationComponent>;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        TenantRegistrationComponent,
        ReactiveFormsModule,
        HttpClientTestingModule,
        RouterTestingModule.withRoutes([{ path: 'login', component: TenantRegistrationComponent }]),
        BrowserAnimationsModule,
        MatFormFieldModule,
        MatInputModule,
        MatButtonModule,
        MatCardModule,
        MatIconModule,
        MatProgressSpinnerModule,
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(TenantRegistrationComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    jest.spyOn(router, 'navigate');
    fixture.detectChanges();
  });

  afterEach(() => {
    if (httpMock) {
      httpMock.verify();
    }
  });

  describe('Form Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with an empty form', () => {
      expect(component.registrationForm.value).toEqual({
        tenantId: '',
        tenantName: '',
        firstName: '',
        lastName: '',
        username: '',
        email: '',
        password: '',
      });
    });

    it('should have form invalid when empty', () => {
      expect(component.registrationForm.valid).toBeFalsy();
    });

    it('should have loading set to false initially', () => {
      expect(component.isLoading()).toBeFalsy();
    });

    it('should have success set to false initially', () => {
      expect(component.successMessage()).toBeNull();
    });
  });

  describe('Form Validation', () => {
    describe('Tenant ID Validation', () => {
      it('should require tenant ID', () => {
        const tenantId = component.registrationForm.get('tenantId');
        tenantId?.setValue('');
        expect(tenantId?.hasError('required')).toBeTruthy();
      });

      it('should accept valid tenant ID with lowercase letters and numbers', () => {
        const tenantId = component.registrationForm.get('tenantId');
        tenantId?.setValue('acme-health-123');
        expect(tenantId?.valid).toBeTruthy();
      });

      it('should accept valid tenant ID at minimum length (3 chars)', () => {
        const tenantId = component.registrationForm.get('tenantId');
        tenantId?.setValue('abc');
        expect(tenantId?.valid).toBeTruthy();
      });

      it('should accept valid tenant ID at maximum length (63 chars)', () => {
        const tenantId = component.registrationForm.get('tenantId');
        const longId = 'a'.repeat(63);
        tenantId?.setValue(longId);
        expect(tenantId?.valid).toBeTruthy();
      });

      it('should reject tenant ID shorter than 3 characters', () => {
        const tenantId = component.registrationForm.get('tenantId');
        tenantId?.setValue('ab');
        expect(tenantId?.hasError('pattern')).toBeTruthy();
      });

      it('should reject tenant ID longer than 63 characters', () => {
        const tenantId = component.registrationForm.get('tenantId');
        const longId = 'a'.repeat(64);
        tenantId?.setValue(longId);
        expect(tenantId?.hasError('pattern')).toBeTruthy();
      });

      it('should reject tenant ID with uppercase letters', () => {
        const tenantId = component.registrationForm.get('tenantId');
        tenantId?.setValue('Acme-Health');
        expect(tenantId?.hasError('pattern')).toBeTruthy();
      });

      it('should reject tenant ID with spaces', () => {
        const tenantId = component.registrationForm.get('tenantId');
        tenantId?.setValue('acme health');
        expect(tenantId?.hasError('pattern')).toBeTruthy();
      });

      it('should reject tenant ID with special characters except hyphen', () => {
        const tenantId = component.registrationForm.get('tenantId');
        tenantId?.setValue('acme_health');
        expect(tenantId?.hasError('pattern')).toBeTruthy();
      });
    });

    describe('Tenant Name Validation', () => {
      it('should require tenant name', () => {
        const tenantName = component.registrationForm.get('tenantName');
        tenantName?.setValue('');
        expect(tenantName?.hasError('required')).toBeTruthy();
      });

      it('should accept valid tenant name', () => {
        const tenantName = component.registrationForm.get('tenantName');
        tenantName?.setValue('Acme Health Organization');
        expect(tenantName?.valid).toBeTruthy();
      });
    });

    describe('Email Validation', () => {
      it('should require email', () => {
        const email = component.registrationForm.get('email');
        email?.setValue('');
        expect(email?.hasError('required')).toBeTruthy();
      });

      it('should accept valid email format', () => {
        const email = component.registrationForm.get('email');
        email?.setValue('admin@acme-health.com');
        expect(email?.valid).toBeTruthy();
      });

      it('should reject invalid email format', () => {
        const email = component.registrationForm.get('email');
        email?.setValue('invalid-email');
        expect(email?.hasError('email')).toBeTruthy();
      });
    });

    describe('Password Validation', () => {
      it('should require password', () => {
        const password = component.registrationForm.get('password');
        password?.setValue('');
        expect(password?.hasError('required')).toBeTruthy();
      });

      it('should require minimum 8 characters', () => {
        const password = component.registrationForm.get('password');
        password?.setValue('Test1!');
        expect(password?.hasError('minlength')).toBeTruthy();
      });

      it('should require at least one uppercase letter', () => {
        const password = component.registrationForm.get('password');
        password?.setValue('test2026!');
        expect(password?.hasError('pattern')).toBeTruthy();
      });

      it('should require at least one lowercase letter', () => {
        const password = component.registrationForm.get('password');
        password?.setValue('TEST2026!');
        expect(password?.hasError('pattern')).toBeTruthy();
      });

      it('should require at least one number', () => {
        const password = component.registrationForm.get('password');
        password?.setValue('TestTest!');
        expect(password?.hasError('pattern')).toBeTruthy();
      });

      it('should require at least one special character', () => {
        const password = component.registrationForm.get('password');
        password?.setValue('Test2026');
        expect(password?.hasError('pattern')).toBeTruthy();
      });

      it('should accept valid password', () => {
        const password = component.registrationForm.get('password');
        password?.setValue('Test2026!');
        expect(password?.valid).toBeTruthy();
      });
    });


    describe('All Required Fields', () => {
      it('should require all fields', () => {
        const form = component.registrationForm;
        expect(form.get('tenantId')?.hasError('required')).toBeTruthy();
        expect(form.get('tenantName')?.hasError('required')).toBeTruthy();
        expect(form.get('firstName')?.hasError('required')).toBeTruthy();
        expect(form.get('lastName')?.hasError('required')).toBeTruthy();
        expect(form.get('username')?.hasError('required')).toBeTruthy();
        expect(form.get('email')?.hasError('required')).toBeTruthy();
        expect(form.get('password')?.hasError('required')).toBeTruthy();
      });
    });
  });

  describe('Registration Submission', () => {
    const validFormData = {
      tenantId: 'test-clinic',
      tenantName: 'Test Clinic',
      firstName: 'John',
      lastName: 'Doe',
      username: 'admin@test-clinic',
      email: 'admin@test-clinic.com',
      password: 'Test2026!',
    };

    it('should not submit when form is invalid', () => {
      component.onSubmit();
      expect(component.isLoading()).toBeFalsy();
      httpMock.expectNone('/tenants/register');
    });

    it('should submit valid form and handle success', fakeAsync(() => {
      component.registrationForm.patchValue(validFormData);
      expect(component.registrationForm.valid).toBeTruthy();

      component.onSubmit();
      expect(component.isLoading()).toBeTruthy();
      expect(component.errorMessage()).toBeNull();

      const req = httpMock.expectOne('/tenants/register');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({
        tenantId: validFormData.tenantId,
        tenantName: validFormData.tenantName,
        adminUser: {
          username: validFormData.username,
          email: validFormData.email,
          password: validFormData.password,
          firstName: validFormData.firstName,
          lastName: validFormData.lastName,
        },
      });

      const mockResponse = {
        tenantId: validFormData.tenantId,
        tenantName: validFormData.tenantName,
        status: 'ACTIVE',
        adminUser: {
          userId: '123',
          username: validFormData.username,
          email: validFormData.email,
          roles: ['ADMIN'],
          tenantIds: [validFormData.tenantId],
        },
      };

      req.flush(mockResponse);
      tick();

      expect(component.isLoading()).toBeFalsy();
      expect(component.successMessage()).toContain('registered successfully');
      expect(component.errorMessage()).toBeNull();
    }));

    it('should auto-redirect to login after successful registration', fakeAsync(() => {
      jest.spyOn(router, 'navigate');
      component.registrationForm.patchValue(validFormData);

      component.onSubmit();

      const req = httpMock.expectOne('/tenants/register');
      req.flush({
        tenantId: validFormData.tenantId,
        tenantName: validFormData.tenantName,
        status: 'ACTIVE',
        adminUser: {
          userId: '123',
          username: validFormData.username,
          email: validFormData.email,
          roles: ['ADMIN'],
          tenantIds: [validFormData.tenantId],
        },
      });
      tick();

      // Advance time by 3 seconds to trigger the redirect
      tick(3000);

      expect(router.navigate).toHaveBeenCalledWith(['/login'], {
        queryParams: {
          registered: 'true',
          tenantId: validFormData.tenantId,
          username: validFormData.username,
        },
      });
    }));

    it('should handle 409 Conflict error (duplicate tenant)', () => {
      component.registrationForm.patchValue(validFormData);

      component.onSubmit();

      const req = httpMock.expectOne('/tenants/register');
      req.flush(
        { message: 'Tenant ID already exists' },
        { status: 409, statusText: 'Conflict' }
      );

      expect(component.isLoading()).toBeFalsy();
      expect(component.successMessage()).toBeNull();
      expect(component.errorMessage()).toBe('Tenant ID already exists');
    });

    it('should handle 400 Bad Request error', () => {
      component.registrationForm.patchValue(validFormData);

      component.onSubmit();

      const req = httpMock.expectOne('/tenants/register');
      req.flush(
        { message: 'Invalid email format' },
        { status: 400, statusText: 'Bad Request' }
      );

      expect(component.isLoading()).toBeFalsy();
      expect(component.successMessage()).toBeNull();
      expect(component.errorMessage()).toBe('Invalid email format');
    });

    it('should handle 500 Internal Server Error', () => {
      component.registrationForm.patchValue(validFormData);

      component.onSubmit();

      const req = httpMock.expectOne('/tenants/register');
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });

      expect(component.isLoading()).toBeFalsy();
      expect(component.successMessage()).toBeNull();
      expect(component.errorMessage()).toContain('registration');
    });

    it('should handle network error', () => {
      component.registrationForm.patchValue(validFormData);

      component.onSubmit();

      const req = httpMock.expectOne('/tenants/register');
      req.error(new ProgressEvent('error'));

      expect(component.isLoading()).toBeFalsy();
      expect(component.successMessage()).toBeNull();
      expect(component.errorMessage()).toContain('server');
    });
  });

  describe('UI State Management', () => {
    it('should show loading state during submission', () => {
      const validFormData = {
        tenantId: 'test-clinic',
        tenantName: 'Test Clinic',
        firstName: 'John',
        lastName: 'Doe',
        username: 'admin@test-clinic',
        email: 'admin@test-clinic.com',
        password: 'Test2026!',
      };

      component.registrationForm.patchValue(validFormData);
      component.onSubmit();

      expect(component.isLoading()).toBeTruthy();

      const req = httpMock.expectOne('/tenants/register');
      req.flush({
        tenantId: validFormData.tenantId,
        tenantName: validFormData.tenantName,
        status: 'ACTIVE',
        adminUser: {
          userId: '123',
          username: validFormData.username,
          email: validFormData.email,
          roles: ['ADMIN'],
          tenantIds: [validFormData.tenantId],
        },
      });

      fixture.detectChanges();
    });

    it('should clear error message on new submission', () => {
      const validFormData = {
        tenantId: 'test-clinic',
        tenantName: 'Test Clinic',
        firstName: 'John',
        lastName: 'Doe',
        username: 'admin@test-clinic',
        email: 'admin@test-clinic.com',
        password: 'Test2026!',
      };

      component.errorMessage.set('Previous error');
      component.registrationForm.patchValue(validFormData);
      component.onSubmit();

      expect(component.errorMessage()).toBeNull();

      // Flush the HTTP request to prevent "Expected no open requests" error
      const req = httpMock.expectOne('/tenants/register');
      req.flush({
        tenantId: validFormData.tenantId,
        tenantName: validFormData.tenantName,
        status: 'ACTIVE',
        adminUser: {
          userId: '123',
          username: validFormData.username,
          email: validFormData.email,
          roles: ['ADMIN'],
          tenantIds: [validFormData.tenantId],
        },
      });
    });

    it('should toggle password visibility', () => {
      expect(component.hidePassword()).toBeTruthy();
      component.togglePasswordVisibility();
      expect(component.hidePassword()).toBeFalsy();
      component.togglePasswordVisibility();
      expect(component.hidePassword()).toBeTruthy();
    });
  });
});
