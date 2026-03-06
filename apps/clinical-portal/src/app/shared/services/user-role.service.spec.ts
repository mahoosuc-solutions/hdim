import { UserRoleService, UserRole } from './user-role.service';
import { createMockLoggerService } from '../../testing/mocks';

describe('UserRoleService', () => {
  let service: UserRoleService;
  let mockLoggerService: ReturnType<typeof createMockLoggerService>;

  beforeEach(() => {
    localStorage.clear();
    mockLoggerService = createMockLoggerService();
    service = new UserRoleService(mockLoggerService as any);
  });

  it('sets and gets role with persistence', () => {
    service.setRole(UserRole.ADMIN);
    expect(service.getCurrentRole()).toBe(UserRole.ADMIN);

    const reloaded = new UserRoleService(mockLoggerService as any);
    expect(reloaded.getCurrentRole()).toBe(UserRole.ADMIN);
  });

  it('returns role configs and permissions', () => {
    const config = service.getRoleConfig(UserRole.PROVIDER);
    expect(config.permissions.length).toBeGreaterThan(0);

    service.setRole(UserRole.PROVIDER);
    expect(service.hasPermission('anything')).toBe(true);
  });

  it('returns false when permission is missing', () => {
    service.setRole(UserRole.MEDICAL_ASSISTANT);
    expect(service.hasPermission('manage_users')).toBe(false);
  });

  it('returns dashboards and care gap filters', () => {
    const metrics = service.getRoleDashboardMetrics(UserRole.MEDICAL_ASSISTANT);
    expect(metrics.length).toBeGreaterThan(0);

    const filters = service.getRoleCareGapFilters(UserRole.REGISTERED_NURSE);
    expect(filters.assignedTo).toBe('RN');
  });

  it('returns RN and provider metrics', () => {
    const rnMetrics = service.getRoleDashboardMetrics(UserRole.REGISTERED_NURSE);
    expect(rnMetrics).toContain('patient_calls_pending');

    const providerMetrics = service.getRoleDashboardMetrics(UserRole.PROVIDER);
    expect(providerMetrics).toContain('results_to_review');
  });

  it('returns admin metrics and empty defaults', () => {
    const metrics = service.getRoleDashboardMetrics(UserRole.ADMIN);
    expect(metrics).toContain('system_health');

    const filters = service.getRoleCareGapFilters('UNKNOWN' as UserRole);
    expect(filters).toEqual({});
  });

  it('returns MA and provider care gap filters', () => {
    const maFilters = service.getRoleCareGapFilters(UserRole.MEDICAL_ASSISTANT);
    expect(maFilters.assignedTo).toBe('MA');

    const providerFilters = service.getRoleCareGapFilters(UserRole.PROVIDER);
    expect(providerFilters.priority).toBe('high');
  });

  it('returns available roles and configs', () => {
    expect(service.getAllRoles().length).toBeGreaterThan(0);
    expect(service.getAllRoleConfigs().length).toBeGreaterThan(0);
  });

  it('returns empty metrics for unknown roles', () => {
    const metrics = service.getRoleDashboardMetrics('UNKNOWN' as UserRole);
    expect(metrics).toEqual([]);
  });

  it('handles localStorage errors when saving or loading', () => {
    const storageProto = Object.getPrototypeOf(window.localStorage) as Storage;
    const getItemSpy = jest.spyOn(storageProto, 'getItem').mockImplementation(() => {
      throw new Error('fail');
    });
    const setItemSpy = jest.spyOn(storageProto, 'setItem').mockImplementation(() => {
      throw new Error('fail');
    });

    const freshLogger = createMockLoggerService();
    // withContext returns a contextual logger whose error method should be called
    const contextualLogger = freshLogger.withContext('UserRoleService');

    new UserRoleService(freshLogger as any);
    service.setRole(UserRole.ADMIN);

    // The service uses this.logger.error (via withContext) for localStorage errors
    expect(contextualLogger.error).toHaveBeenCalled();

    getItemSpy.mockRestore();
    setItemSpy.mockRestore();
  });
});
