import { TestBed } from '@angular/core/testing';
import { MultiTenantService, TenantInfo } from './multi-tenant.service';

describe('MultiTenantService', () => {
  let service: MultiTenantService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [MultiTenantService]
    });
    service = TestBed.inject(MultiTenantService);
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  describe('Tenant Context Management', () => {
    it('should set and get current tenant', () => {
      service.setCurrentTenant('tenant-123');
      expect(service.getCurrentTenant()).toBe('tenant-123');
    });

    it('should persist tenant context to localStorage', () => {
      service.setCurrentTenant('tenant-456');
      const stored = localStorage.getItem('current_tenant');
      expect(stored).toBe('tenant-456');
    });

    it('should load tenant context from localStorage', () => {
      localStorage.setItem('current_tenant', 'tenant-789');
      const newService = new MultiTenantService();
      expect(newService.getCurrentTenant()).toBe('tenant-789');
    });

    it('should emit tenant change observable', (done) => {
      let tenantChanged = false;
      let newTenant = '';

      service.currentTenant$.subscribe(tenant => {
        if (tenant !== 'default-tenant') {
          tenantChanged = true;
          newTenant = tenant;
        }
      });

      service.setCurrentTenant('tenant-new');

      setTimeout(() => {
        expect(tenantChanged).toBe(true);
        expect(newTenant).toBe('tenant-new');
        done();
      }, 50);
    });
  });

  describe('Data Filtering', () => {
    it('should filter array by current tenant', () => {
      const data = [
        { id: 1, tenantId: 'tenant-A', name: 'Item A1' },
        { id: 2, tenantId: 'tenant-B', name: 'Item B1' },
        { id: 3, tenantId: 'tenant-A', name: 'Item A2' }
      ];

      service.setCurrentTenant('tenant-A');
      const filtered = service.filterByTenant(data);

      expect(filtered.length).toBe(2);
      expect(filtered[0].name).toBe('Item A1');
      expect(filtered[1].name).toBe('Item A2');
    });

    it('should filter with custom tenant field', () => {
      const data = [
        { id: 1, customTenantId: 'tenant-A' },
        { id: 2, customTenantId: 'tenant-B' }
      ];

      service.setCurrentTenant('tenant-A');
      const filtered = service.filterByTenant(data, 'customTenantId');

      expect(filtered.length).toBe(1);
      expect(filtered[0].customTenantId).toBe('tenant-A');
    });

    it('should check if object belongs to current tenant', () => {
      const item = { id: 1, tenantId: 'tenant-A' };
      service.setCurrentTenant('tenant-A');

      expect(service.belongsToCurrentTenant(item)).toBe(true);

      service.setCurrentTenant('tenant-B');
      expect(service.belongsToCurrentTenant(item)).toBe(false);
    });

    it('should check tenant access', () => {
      expect(service.hasAccessToTenant('tenant-123')).toBe(true);
      expect(service.hasAccessToTenant('')).toBe(false);
    });
  });

  describe('Tenant Preferences', () => {
    it('should set and get tenant preference', () => {
      service.setCurrentTenant('tenant-A');
      service.setTenantPreference('theme', 'dark');

      expect(service.getTenantPreference('theme')).toBe('dark');
    });

    it('should return default value when preference not set', () => {
      const result = service.getTenantPreference('unknown', 'default-value');
      expect(result).toBe('default-value');
    });

    it('should maintain separate preferences per tenant', () => {
      service.setCurrentTenant('tenant-A');
      service.setTenantPreference('theme', 'dark');

      service.setCurrentTenant('tenant-B');
      service.setTenantPreference('theme', 'light');

      expect(service.getTenantPreference('theme')).toBe('light');

      service.setCurrentTenant('tenant-A');
      expect(service.getTenantPreference('theme')).toBe('dark');
    });

    it('should persist preferences to localStorage', () => {
      service.setCurrentTenant('tenant-A');
      service.setTenantPreference('color', 'blue');

      const stored = JSON.parse(localStorage.getItem('tenant_preferences') || '{}');
      expect(stored['tenant-A'].color).toBe('blue');
    });

    it('should load preferences from localStorage', () => {
      const prefs = {
        'tenant-X': { font: 'large', lang: 'en' }
      };
      localStorage.setItem('tenant_preferences', JSON.stringify(prefs));

      const newService = new MultiTenantService();
      newService.setCurrentTenant('tenant-X');
      expect(newService.getTenantPreference('font')).toBe('large');
      expect(newService.getTenantPreference('lang')).toBe('en');
    });

    it('should get all tenant preferences', () => {
      service.setCurrentTenant('tenant-A');
      service.setTenantPreference('pref1', 'value1');
      service.setTenantPreference('pref2', 'value2');

      const all = service.getAllTenantPreferences();
      expect(all.pref1).toBe('value1');
      expect(all.pref2).toBe('value2');
    });

    it('should clear tenant preferences', () => {
      service.setCurrentTenant('tenant-A');
      service.setTenantPreference('key', 'value');

      service.clearTenantPreferences();

      const all = service.getAllTenantPreferences();
      expect(Object.keys(all).length).toBe(0);
    });
  });

  describe('Tenant Information', () => {
    it('should set and get tenant info', () => {
      const info: TenantInfo = {
        id: 'tenant-123',
        name: 'Acme Corp',
        createdAt: Date.now(),
        region: 'us-west'
      };

      service.setTenantInfo('tenant-123', info);
      const retrieved = service.getTenantInfo('tenant-123');

      expect(retrieved).toEqual(info);
    });

    it('should get current tenant info', () => {
      const info: TenantInfo = {
        id: 'tenant-456',
        name: 'Test Org',
        createdAt: Date.now()
      };

      service.setCurrentTenant('tenant-456');
      service.setTenantInfo('tenant-456', info);

      const current = service.getCurrentTenantInfo();
      expect(current).toEqual(info);
    });

    it('should return undefined for unknown tenant', () => {
      const info = service.getTenantInfo('unknown-tenant');
      expect(info).toBeUndefined();
    });
  });

  describe('Cross-Tenant Validation', () => {
    it('should validate cross-tenant access match', () => {
      expect(() => {
        service.validateCrossTenantAccess('tenant-A', 'tenant-A');
      }).not.toThrow();
    });

    it('should throw on cross-tenant access mismatch', () => {
      expect(() => {
        service.validateCrossTenantAccess('tenant-A', 'tenant-B');
      }).toThrowError('Cross-tenant access denied');
    });

    it('should validate current tenant context', () => {
      service.setCurrentTenant('tenant-X');
      expect(() => {
        service.validateTenantContext('tenant-X');
      }).not.toThrow();
    });

    it('should throw on tenant context mismatch', () => {
      service.setCurrentTenant('tenant-X');
      expect(() => {
        service.validateTenantContext('tenant-Y');
      }).toThrowError('Tenant context mismatch');
    });
  });

  describe('Authorized Tenants', () => {
    it('should get authorized tenants', () => {
      service.setCurrentTenant('tenant-main');
      const authorized = service.getAuthorizedTenants();

      expect(authorized).toContain('tenant-main');
      expect(Array.isArray(authorized)).toBe(true);
    });
  });

  describe('Tenant Isolation', () => {
    it('should isolate data between tenants', () => {
      const tenantAData = { id: 1, tenantId: 'tenant-A', secret: 'secret-a' };
      const tenantBData = { id: 2, tenantId: 'tenant-B', secret: 'secret-b' };

      service.setCurrentTenant('tenant-A');
      let result = service.filterByTenant([tenantAData, tenantBData]);
      expect(result.length).toBe(1);
      expect(result[0].secret).toBe('secret-a');

      service.setCurrentTenant('tenant-B');
      result = service.filterByTenant([tenantAData, tenantBData]);
      expect(result.length).toBe(1);
      expect(result[0].secret).toBe('secret-b');
    });

    it('should prevent cross-tenant preference access', () => {
      service.setCurrentTenant('tenant-A');
      service.setTenantPreference('api-key', 'key-a');

      service.setCurrentTenant('tenant-B');
      const key = service.getTenantPreference('api-key');

      expect(key).toBeUndefined();
    });

    it('should maintain separate preference maps', () => {
      service.setCurrentTenant('tenant-1');
      service.setTenantPreference('setting1', 'value1');

      service.setCurrentTenant('tenant-2');
      service.setTenantPreference('setting1', 'value2');

      const prefs = service.getAllTenantPreferences();
      expect(prefs.setting1).toBe('value2');

      service.setCurrentTenant('tenant-1');
      const prefs1 = service.getAllTenantPreferences();
      expect(prefs1.setting1).toBe('value1');
    });
  });

  describe('Default Tenant', () => {
    it('should initialize with default tenant', () => {
      const tenant = service.getCurrentTenant();
      expect(tenant).toBeTruthy();
    });

    it('should allow switching from default tenant', () => {
      service.setCurrentTenant('custom-tenant');
      expect(service.getCurrentTenant()).toBe('custom-tenant');
    });
  });

  describe('Complex Scenarios', () => {
    it('should handle multiple tenants with different configurations', () => {
      // Tenant A
      service.setCurrentTenant('tenant-A');
      service.setTenantPreference('locale', 'en-US');
      service.setTenantPreference('timezone', 'UTC');

      // Tenant B
      service.setCurrentTenant('tenant-B');
      service.setTenantPreference('locale', 'fr-FR');
      service.setTenantPreference('timezone', 'CET');

      // Verify isolation
      expect(service.getTenantPreference('locale')).toBe('fr-FR');
      expect(service.getTenantPreference('timezone')).toBe('CET');

      // Switch back to A
      service.setCurrentTenant('tenant-A');
      expect(service.getTenantPreference('locale')).toBe('en-US');
      expect(service.getTenantPreference('timezone')).toBe('UTC');
    });

    it('should handle large data filtering efficiently', () => {
      const largeData = [];
      for (let i = 0; i < 1000; i++) {
        largeData.push({
          id: i,
          tenantId: i % 2 === 0 ? 'tenant-A' : 'tenant-B',
          value: Math.random()
        });
      }

      service.setCurrentTenant('tenant-A');
      const start = performance.now();
      const filtered = service.filterByTenant(largeData);
      const duration = performance.now() - start;

      expect(filtered.length).toBe(500);
      expect(duration).toBeLessThan(100); // Should be fast
    });
  });
});
