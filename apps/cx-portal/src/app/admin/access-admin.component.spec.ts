import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { AccessAdminComponent } from './access-admin.component';
import { CxApiService } from '../shared/services/cx-api.service';

describe('AccessAdminComponent', () => {
  const apiMock = {
    getCustomerAccess: jest.fn(),
    getIdentityAccess: jest.fn(),
    upsertCustomerAccess: jest.fn(),
    deleteCustomerAccess: jest.fn(),
    upsertIdentityAccess: jest.fn(),
    deleteIdentityAccess: jest.fn(),
  };

  beforeEach(async () => {
    apiMock.getCustomerAccess.mockReset();
    apiMock.getIdentityAccess.mockReset();
    apiMock.upsertCustomerAccess.mockReset();
    apiMock.deleteCustomerAccess.mockReset();
    apiMock.upsertIdentityAccess.mockReset();
    apiMock.deleteIdentityAccess.mockReset();

    apiMock.getCustomerAccess.mockReturnValue(of([]));
    apiMock.getIdentityAccess.mockReturnValue(of([]));
    apiMock.upsertCustomerAccess.mockReturnValue(of({ ok: true }));
    apiMock.deleteCustomerAccess.mockReturnValue(of({ ok: true }));
    apiMock.upsertIdentityAccess.mockReturnValue(of({ ok: true }));
    apiMock.deleteIdentityAccess.mockReturnValue(of({ ok: true }));

    await TestBed.configureTestingModule({
      imports: [AccessAdminComponent],
      providers: [{ provide: CxApiService, useValue: apiMock }],
    }).compileComponents();
  });

  it('loads customer and identity access records on init', () => {
    apiMock.getCustomerAccess.mockReturnValue(
      of([{ email: 'cust@example.com', customer_role: 'customer_admin', customer_ids: ['c1'] }])
    );
    apiMock.getIdentityAccess.mockReturnValue(
      of([{ email: 'ops@example.com', principal_type: 'staff', role: 'internal' }])
    );

    const fixture = TestBed.createComponent(AccessAdminComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(component.customerAccess).toHaveLength(1);
    expect(component.identityAccess).toHaveLength(1);
    expect(component.loading).toBe(false);
  });

  it('saves customer access with parsed customer IDs', () => {
    const fixture = TestBed.createComponent(AccessAdminComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component.customerForm = {
      email: 'viewer@example.com',
      customer_role: 'customer_viewer',
      customer_ids: 'cust-1, cust-2 ,',
    };

    component.saveCustomerAccess();

    expect(apiMock.upsertCustomerAccess).toHaveBeenCalledWith({
      email: 'viewer@example.com',
      customer_role: 'customer_viewer',
      customer_ids: ['cust-1', 'cust-2'],
    });
    expect(component.customerForm.email).toBe('');
    expect(apiMock.getCustomerAccess).toHaveBeenCalled();
  });

  it('shows validation error for missing customer IDs', () => {
    const fixture = TestBed.createComponent(AccessAdminComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component.customerForm = {
      email: 'viewer@example.com',
      customer_role: 'customer_viewer',
      customer_ids: '',
    };
    component.saveCustomerAccess();

    expect(apiMock.upsertCustomerAccess).not.toHaveBeenCalled();
    expect(component.errorMessage).toContain('requires email and at least one customer ID');
  });

  it('deletes identity access record', () => {
    const fixture = TestBed.createComponent(AccessAdminComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component.removeIdentityAccess('agent@example.com');

    expect(apiMock.deleteIdentityAccess).toHaveBeenCalledWith('agent@example.com');
    expect(apiMock.getIdentityAccess).toHaveBeenCalled();
  });

  it('surfaces backend error details', () => {
    apiMock.getCustomerAccess.mockReturnValue(
      throwError(() => ({ error: { detail: 'forbidden' } }))
    );
    apiMock.getIdentityAccess.mockReturnValue(of([]));

    const fixture = TestBed.createComponent(AccessAdminComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    expect(component.errorMessage).toBe('forbidden');
    expect(component.loading).toBe(false);
  });
});
