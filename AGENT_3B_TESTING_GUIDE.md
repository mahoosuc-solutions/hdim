# Agent 3B Testing Guide

## 🧪 Comprehensive Testing Strategy for Angular Services

This guide provides testing patterns for all services, guards, interceptors, and NgRx state created by Agent 3B.

---

## 📦 Test Setup

### Install Testing Dependencies

```bash
npm install --save-dev @ngrx/store-devtools karma jasmine-core karma-jasmine karma-chrome-launcher
```

### TestBed Configuration

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { provideMockStore, MockStore } from '@ngrx/store/testing';

beforeEach(() => {
  TestBed.configureTestingModule({
    imports: [HttpClientTestingModule],
    providers: [
      YourService,
      provideMockStore({ initialState: {} })
    ]
  });
});
```

---

## 1. API Service Tests

### api.service.spec.ts

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { ApiService } from './api.service';

describe('ApiService', () => {
  let service: ApiService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [ApiService]
    });
    service = TestBed.inject(ApiService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('GET requests', () => {
    it('should make a successful GET request', () => {
      const mockData = { id: 1, name: 'Test' };
      const url = '/api/test';

      service.get<any>(url).subscribe({
        next: (data) => {
          expect(data).toEqual(mockData);
        }
      });

      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('GET');
      req.flush(mockData);
    });

    it('should handle GET request errors', () => {
      const url = '/api/test';
      const errorMessage = 'Resource not found.';

      service.get<any>(url).subscribe({
        error: (error) => {
          expect(error.message).toContain(errorMessage);
          expect(error.status).toBe(404);
        }
      });

      const req = httpMock.expectOne(url);
      req.flush({ message: errorMessage }, { status: 404, statusText: 'Not Found' });
    });

    it('should retry failed requests', (done) => {
      const url = '/api/test';
      const mockData = { success: true };
      let requestCount = 0;

      service.getWithRetry<any>(url).subscribe({
        next: (data) => {
          expect(data).toEqual(mockData);
          expect(requestCount).toBeGreaterThan(1); // Verify retry occurred
          done();
        }
      });

      // First request fails
      httpMock.expectOne(url).flush(
        { error: 'Server error' },
        { status: 500, statusText: 'Internal Server Error' }
      );
      requestCount++;

      // Second request succeeds
      setTimeout(() => {
        httpMock.expectOne(url).flush(mockData);
        requestCount++;
      }, 1000);
    });
  });

  describe('POST requests', () => {
    it('should make a successful POST request', () => {
      const url = '/api/test';
      const payload = { name: 'Test' };
      const mockResponse = { id: 1, ...payload };

      service.post<any>(url, payload).subscribe({
        next: (data) => {
          expect(data).toEqual(mockResponse);
        }
      });

      const req = httpMock.expectOne(url);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(payload);
      req.flush(mockResponse);
    });
  });
});
```

---

## 2. Care Gap Service Tests

### care-gap.service.spec.ts

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { CareGapService, CareGap, CareGapStatus } from './care-gap.service';
import { API_CONFIG } from '../config/api.config';

describe('CareGapService', () => {
  let service: CareGapService;
  let httpMock: HttpTestingController;

  const mockCareGap: CareGap = {
    id: 'gap-1',
    patientId: 'patient-123',
    measureId: 'measure-1',
    measureName: 'Colorectal Cancer Screening',
    gapType: 'PREVENTIVE_SCREENING',
    status: CareGapStatus.OPEN,
    priority: 'HIGH',
    priorityScore: 85,
    description: 'Patient needs colorectal cancer screening',
    recommendation: 'Schedule colonoscopy',
    detectedDate: '2024-01-01',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CareGapService]
    });
    service = TestBed.inject(CareGapService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    service.clearCache();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getPatientCareGaps', () => {
    it('should fetch care gaps for a patient', () => {
      const patientId = 'patient-123';
      const mockGaps = [mockCareGap];

      service.getPatientCareGaps(patientId).subscribe({
        next: (gaps) => {
          expect(gaps).toEqual(mockGaps);
          expect(gaps.length).toBe(1);
        }
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes(`care-gaps/${patientId}`)
      );
      expect(req.request.method).toBe('GET');
      req.flush(mockGaps);
    });

    it('should use cached data if available', () => {
      const patientId = 'patient-123';
      const mockGaps = [mockCareGap];

      // First call - fetches from API
      service.getPatientCareGaps(patientId).subscribe();
      httpMock.expectOne((req) => req.url.includes(`care-gaps/${patientId}`)).flush(mockGaps);

      // Second call - uses cache (no HTTP request)
      service.getPatientCareGaps(patientId).subscribe({
        next: (gaps) => {
          expect(gaps).toEqual(mockGaps);
        }
      });

      httpMock.expectNone((req) => req.url.includes(`care-gaps/${patientId}`));
    });
  });

  describe('closeGap', () => {
    it('should close a care gap', () => {
      const gapId = 'gap-1';
      const closure = {
        reason: 'Service completed',
        notes: 'Patient received service',
        closedBy: 'dr-smith'
      };

      service.closeGap(gapId, closure).subscribe({
        next: (result) => {
          expect(result.gapId).toBe(gapId);
        }
      });

      const req = httpMock.expectOne((request) =>
        request.url.includes(`care-gaps/${gapId}/address`)
      );
      expect(req.request.method).toBe('POST');
      req.flush({ gapId, patientId: 'patient-123', ...closure });
    });

    it('should invalidate cache after closing gap', () => {
      const patientId = 'patient-123';
      const gapId = 'gap-1';
      const closure = { reason: 'Completed', closedBy: 'dr-smith' };

      spyOn(service, 'invalidatePatientCache');

      service.closeGap(gapId, closure).subscribe();

      const req = httpMock.expectOne((request) =>
        request.url.includes(`care-gaps/${gapId}/address`)
      );
      req.flush({ gapId, patientId, ...closure });

      expect(service.invalidatePatientCache).toHaveBeenCalledWith(patientId);
    });
  });

  describe('gap updates', () => {
    it('should emit gap updates', (done) => {
      service.gapUpdates$.subscribe({
        next: (update) => {
          if (update) {
            expect(update.type).toBe('detected');
            expect(update.patientId).toBe('patient-123');
            done();
          }
        }
      });

      service.detectGapsForPatient('patient-123').subscribe();
      httpMock.expectOne((req) => req.url.includes('detect')).flush([mockCareGap]);
    });
  });
});
```

---

## 3. Auth Service Tests

### auth.service.spec.ts

```typescript
import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService, User, LoginResponse } from './auth.service';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let router: jasmine.SpyObj<Router>;

  const mockUser: User = {
    id: '1',
    username: 'testuser',
    email: 'test@example.com',
    firstName: 'Test',
    lastName: 'User',
    fullName: 'Test User',
    roles: [
      {
        id: 'role-1',
        name: 'CLINICIAN',
        permissions: [{ id: 'perm-1', name: 'patients:read' }]
      }
    ],
    active: true
  };

  const mockLoginResponse: LoginResponse = {
    accessToken: 'mock-access-token',
    refreshToken: 'mock-refresh-token',
    tokenType: 'Bearer',
    expiresIn: 3600,
    user: mockUser
  };

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [
        AuthService,
        { provide: Router, useValue: routerSpy }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;

    localStorage.clear();
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('login', () => {
    it('should login successfully', () => {
      service.login('testuser', 'password').subscribe({
        next: (response) => {
          expect(response).toEqual(mockLoginResponse);
          expect(service.isAuthenticated()).toBeTruthy();
        }
      });

      const req = httpMock.expectOne('/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ username: 'testuser', password: 'password' });
      req.flush(mockLoginResponse);
    });

    it('should store tokens on successful login', () => {
      service.login('testuser', 'password').subscribe();

      const req = httpMock.expectOne('/auth/login');
      req.flush(mockLoginResponse);

      expect(service.getToken()).toBe('mock-access-token');
      expect(service.getRefreshToken()).toBe('mock-refresh-token');
    });

    it('should handle login errors', () => {
      service.login('testuser', 'wrongpassword').subscribe({
        error: (error) => {
          expect(error).toBeTruthy();
        }
      });

      const req = httpMock.expectOne('/auth/login');
      req.flush({ message: 'Invalid credentials' }, { status: 401, statusText: 'Unauthorized' });
    });
  });

  describe('logout', () => {
    it('should clear authentication data', () => {
      // Set up authenticated state
      service.setToken('token');
      service.setRefreshToken('refresh-token');

      service.logout();

      expect(service.getToken()).toBeNull();
      expect(service.getRefreshToken()).toBeNull();
      expect(service.isAuthenticated()).toBeFalsy();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('role and permission checks', () => {
    beforeEach(() => {
      service['currentUserSubject'].next(mockUser);
    });

    it('should check if user has role', () => {
      expect(service.hasRole('CLINICIAN')).toBeTruthy();
      expect(service.hasRole('ADMIN')).toBeFalsy();
    });

    it('should check if user has any role', () => {
      expect(service.hasAnyRole(['ADMIN', 'CLINICIAN'])).toBeTruthy();
      expect(service.hasAnyRole(['ADMIN', 'MANAGER'])).toBeFalsy();
    });

    it('should check if user has permission', () => {
      expect(service.hasPermission('patients:read')).toBeTruthy();
      expect(service.hasPermission('patients:delete')).toBeFalsy();
    });

    it('should grant all permissions to ADMIN', () => {
      const adminUser: User = {
        ...mockUser,
        roles: [{ id: 'role-admin', name: 'ADMIN' }]
      };
      service['currentUserSubject'].next(adminUser);

      expect(service.hasPermission('any:permission')).toBeTruthy();
    });
  });
});
```

---

## 4. NgRx Store Tests

### patient.reducer.spec.ts

```typescript
import { patientReducer, initialState } from './patient.reducer';
import * as PatientActions from '../actions/patient.actions';
import { Patient } from '../../models/patient.model';

describe('Patient Reducer', () => {
  const mockPatient: Patient = {
    id: '1',
    resourceType: 'Patient',
    name: [{ family: 'Smith', given: ['John'] }],
    gender: 'male',
    active: true
  };

  describe('unknown action', () => {
    it('should return the default state', () => {
      const action = { type: 'Unknown' } as any;
      const state = patientReducer(initialState, action);

      expect(state).toBe(initialState);
    });
  });

  describe('loadPatients actions', () => {
    it('should set loading to true', () => {
      const action = PatientActions.loadPatients({ count: 100 });
      const state = patientReducer(initialState, action);

      expect(state.loading).toBeTruthy();
      expect(state.error).toBeNull();
    });

    it('should set patients on success', () => {
      const patients = [mockPatient];
      const action = PatientActions.loadPatientsSuccess({ patients });
      const state = patientReducer(initialState, action);

      expect(state.patients).toEqual(patients);
      expect(state.loading).toBeFalsy();
      expect(state.error).toBeNull();
    });

    it('should set error on failure', () => {
      const error = 'Failed to load patients';
      const action = PatientActions.loadPatientsFailure({ error });
      const state = patientReducer(initialState, action);

      expect(state.error).toBe(error);
      expect(state.loading).toBeFalsy();
    });
  });

  describe('createPatient actions', () => {
    it('should add patient on success', () => {
      const action = PatientActions.createPatientSuccess({ patient: mockPatient });
      const state = patientReducer(initialState, action);

      expect(state.patients).toContain(mockPatient);
      expect(state.currentPatient).toEqual(mockPatient);
      expect(state.loading).toBeFalsy();
    });
  });

  describe('updatePatient actions', () => {
    it('should update patient on success', () => {
      const existingState = {
        ...initialState,
        patients: [mockPatient]
      };
      const updatedPatient = { ...mockPatient, active: false };
      const action = PatientActions.updatePatientSuccess({ patient: updatedPatient });
      const state = patientReducer(existingState, action);

      expect(state.patients[0]).toEqual(updatedPatient);
    });
  });

  describe('deletePatient actions', () => {
    it('should remove patient on success', () => {
      const existingState = {
        ...initialState,
        patients: [mockPatient]
      };
      const action = PatientActions.deletePatientSuccess({ id: '1' });
      const state = patientReducer(existingState, action);

      expect(state.patients.length).toBe(0);
    });
  });
});
```

### patient.effects.spec.ts

```typescript
import { TestBed } from '@angular/core/testing';
import { provideMockActions } from '@ngrx/effects/testing';
import { Observable, of, throwError } from 'rxjs';
import { PatientEffects } from './patient.effects';
import { PatientService } from '../../services/patient.service';
import { NotificationService } from '../../services/notification.service';
import { Router } from '@angular/router';
import * as PatientActions from '../actions/patient.actions';
import { Patient } from '../../models/patient.model';

describe('PatientEffects', () => {
  let actions$: Observable<any>;
  let effects: PatientEffects;
  let patientService: jasmine.SpyObj<PatientService>;
  let notificationService: jasmine.SpyObj<NotificationService>;
  let router: jasmine.SpyObj<Router>;

  const mockPatient: Patient = {
    id: '1',
    resourceType: 'Patient',
    name: [{ family: 'Smith', given: ['John'] }]
  };

  beforeEach(() => {
    const patientServiceSpy = jasmine.createSpyObj('PatientService', [
      'getPatients',
      'getPatient',
      'createPatient',
      'updatePatient',
      'deletePatient'
    ]);
    const notificationServiceSpy = jasmine.createSpyObj('NotificationService', [
      'success',
      'error',
      'info'
    ]);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      providers: [
        PatientEffects,
        provideMockActions(() => actions$),
        { provide: PatientService, useValue: patientServiceSpy },
        { provide: NotificationService, useValue: notificationServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });

    effects = TestBed.inject(PatientEffects);
    patientService = TestBed.inject(PatientService) as jasmine.SpyObj<PatientService>;
    notificationService = TestBed.inject(NotificationService) as jasmine.SpyObj<NotificationService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  describe('loadPatients$', () => {
    it('should return loadPatientsSuccess on success', (done) => {
      const patients = [mockPatient];
      patientService.getPatients.and.returnValue(of(patients));

      actions$ = of(PatientActions.loadPatients({ count: 100 }));

      effects.loadPatients$.subscribe({
        next: (action) => {
          expect(action).toEqual(PatientActions.loadPatientsSuccess({ patients }));
          done();
        }
      });
    });

    it('should return loadPatientsFailure on error', (done) => {
      const error = new Error('Failed');
      patientService.getPatients.and.returnValue(throwError(() => error));

      actions$ = of(PatientActions.loadPatients({ count: 100 }));

      effects.loadPatients$.subscribe({
        next: (action) => {
          expect(action.type).toBe(PatientActions.loadPatientsFailure.type);
          expect(notificationService.error).toHaveBeenCalled();
          done();
        }
      });
    });
  });

  describe('createPatient$', () => {
    it('should return createPatientSuccess on success', (done) => {
      patientService.createPatient.and.returnValue(of(mockPatient));

      actions$ = of(PatientActions.createPatient({ patient: mockPatient }));

      effects.createPatient$.subscribe({
        next: (action) => {
          expect(action).toEqual(PatientActions.createPatientSuccess({ patient: mockPatient }));
          expect(notificationService.success).toHaveBeenCalled();
          done();
        }
      });
    });
  });

  describe('createPatientSuccess$', () => {
    it('should navigate to patient detail', (done) => {
      actions$ = of(PatientActions.createPatientSuccess({ patient: mockPatient }));

      effects.createPatientSuccess$.subscribe({
        complete: () => {
          expect(router.navigate).toHaveBeenCalledWith(['/patients', '1']);
          done();
        }
      });
    });
  });
});
```

---

## 5. Guard Tests

### auth.guard.spec.ts

```typescript
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const authServiceSpy = jasmine.createSpyObj('AuthService', ['isAuthenticated']);
    const routerSpy = jasmine.createSpyObj('Router', ['createUrlTree']);

    TestBed.configureTestingModule({
      providers: [
        AuthGuard,
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });

    guard = TestBed.inject(AuthGuard);
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  it('should allow access if authenticated', () => {
    authService.isAuthenticated.and.returnValue(true);

    const result = guard.canActivate(null as any, null as any);

    expect(result).toBeTruthy();
  });

  it('should redirect to login if not authenticated', () => {
    authService.isAuthenticated.and.returnValue(false);
    const mockUrlTree = {} as any;
    router.createUrlTree.and.returnValue(mockUrlTree);

    const result = guard.canActivate(null as any, { url: '/dashboard' } as any);

    expect(router.createUrlTree).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/dashboard' }
    });
    expect(result).toBe(mockUrlTree);
  });
});
```

---

## 📊 Test Coverage Goals

| Category | Target Coverage |
|----------|----------------|
| Services | 85%+ |
| Guards | 90%+ |
| Interceptors | 85%+ |
| NgRx Reducers | 95%+ |
| NgRx Effects | 85%+ |
| NgRx Selectors | 95%+ |

---

## 🚀 Running Tests

```bash
# Run all tests
npm test

# Run tests with coverage
npm run test:coverage

# Run tests in headless mode (CI)
npm run test:ci

# Watch mode for development
npm run test:watch
```

---

## 📝 Testing Best Practices

1. **Use TestBed for dependency injection**
2. **Mock HTTP calls with HttpClientTestingModule**
3. **Test both success and error cases**
4. **Verify side effects (notifications, navigation)**
5. **Use spies to verify method calls**
6. **Clean up after each test (httpMock.verify(), localStorage.clear())**
7. **Test edge cases and boundary conditions**
8. **Keep tests focused and independent**

---

**Created by Agent 3B | Part of TDD Swarm Wave 3**
