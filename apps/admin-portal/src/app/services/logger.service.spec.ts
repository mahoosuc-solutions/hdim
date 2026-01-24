import { TestBed } from '@angular/core/testing';
import { HttpClient } from '@angular/common/http';
import { LoggerService, LogLevel } from './logger.service';

describe('LoggerService - Admin Portal', () => {
  let service: LoggerService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        LoggerService,
        { provide: HttpClient, useValue: null },
      ],
    });

    service = TestBed.inject(LoggerService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('Log Levels', () => {
    it('should filter logs below minimum level', () => {
      jest.spyOn(console, 'log').mockImplementation();
      service.setMinLevel(LogLevel.WARN);

      service.debug('debug message');
      service.info('info message');
      service.warn('warn message');

      expect(console.log).not.toHaveBeenCalled();
    });

    it('should allow logs at or above minimum level', () => {
      jest.spyOn(console, 'warn').mockImplementation();
      service.setMinLevel(LogLevel.WARN);

      service.warn('warn message');

      expect(console.warn).toHaveBeenCalled();
    });
  });

  describe('Contextual Logging', () => {
    it('should create contextual logger with context', () => {
      jest.spyOn(console, 'log').mockImplementation();
      const contextLogger = service.withContext('TestComponent');

      contextLogger.info('test message');

      expect(console.log).toHaveBeenCalledWith(
        jasmine.stringContaining('[TestComponent]'),
        jasmine.anything()
      );
    });

    it('should support admin operation logging', () => {
      jest.spyOn(console, 'log').mockImplementation();
      const contextLogger = service.withContext('AuditLogs');

      contextLogger.logAdminOperation('CREATE_TENANT', { tenantId: 'test-123' });

      expect(console.log).toHaveBeenCalledWith(
        jasmine.stringContaining('Admin Operation'),
        jasmine.anything()
      );
    });
  });

  describe('PHI Filtering', () => {
    it('should redact SSN from logs', () => {
      const testData = { ssn: '123-45-6789', userId: 'user-123' };
      const filtered = service['sanitizeData'](testData);
      const serialized = JSON.stringify(filtered);

      expect(serialized).toContain('[REDACTED]');
      expect(serialized).not.toContain('123-45-6789');
    });

    it('should redact SSN without dashes', () => {
      const testData = { ssn: '123456789' };
      const filtered = service['sanitizeData'](testData);
      const serialized = JSON.stringify(filtered);

      expect(serialized).toContain('[REDACTED]');
    });

    it('should redact patient names in JSON', () => {
      const testData = { firstName: 'John', lastName: 'Doe' };
      const filtered = service['sanitizeData'](testData);
      const serialized = JSON.stringify(filtered);

      expect(serialized).toContain('[REDACTED]');
      expect(serialized).not.toContain('John');
      expect(serialized).not.toContain('Doe');
    });

    it('should redact dates of birth', () => {
      const testData = { dob: '1980-01-15', patientId: 'PAT-123' };
      const filtered = service['sanitizeData'](testData);
      const serialized = JSON.stringify(filtered);

      expect(serialized).toContain('[REDACTED]');
      expect(serialized).not.toContain('1980-01-15');
      // Should preserve non-PHI data
      expect(serialized).toContain('PAT-123');
    });

    it('should redact email addresses (partial match)', () => {
      const message = 'User email: john.doe@example.com';
      const sanitized = service['sanitizeForProduction'](message);

      expect(sanitized).toContain('[REDACTED]');
      expect(sanitized).not.toContain('john.doe@example.com');
    });

    it('should redact phone numbers', () => {
      const testData = { phone: '555-123-4567' };
      const filtered = service['sanitizeData'](testData);
      const serialized = JSON.stringify(filtered);

      expect(serialized).toContain('[REDACTED]');
    });

    it('should redact MRN (Medical Record Number)', () => {
      const message = 'Patient MRN: 12345';
      const sanitized = service['sanitizeForProduction'](message);

      expect(sanitized).toContain('[REDACTED]');
      expect(sanitized).not.toContain('MRN: 12345');
    });

    it('should handle large data objects securely', () => {
      const largeData = { data: 'x'.repeat(2000) };
      const filtered = service['sanitizeData'](largeData);

      expect(filtered).toEqual({ _note: 'Data too large, omitted for security' });
    });

    it('should handle errors without exposing stack traces', () => {
      const error = new Error('Database connection failed');
      const filtered = service['sanitizeData'](error);

      expect(filtered).toEqual({
        errorType: 'Error',
        message: jasmine.any(String),
      });
      expect((filtered as any).stack).toBeUndefined();
    });

    it('should preserve safe identifiers', () => {
      const testData = {
        tenantId: 'tenant-123',
        userId: 'user-456',
        auditEventId: 'evt-789',
      };
      const filtered = service['sanitizeData'](testData);
      const serialized = JSON.stringify(filtered);

      // IDs should not be redacted
      expect(serialized).toContain('tenant-123');
      expect(serialized).toContain('user-456');
      expect(serialized).toContain('evt-789');
    });
  });

  describe('Admin Operation Logging', () => {
    it('should log admin operations with context', () => {
      jest.spyOn(console, 'log').mockImplementation();

      service.logAdminOperation('CREATE_TENANT', { tenantId: 'test-123' });

      expect(console.log).toHaveBeenCalledWith(
        jasmine.stringContaining('Admin Operation: CREATE_TENANT'),
        jasmine.anything()
      );
    });

    it('should sanitize PHI in admin operations', () => {
      jest.spyOn(console, 'log').mockImplementation();

      service.logAdminOperation('UPDATE_USER', {
        userId: 'user-123',
        email: 'user@example.com',
      });

      const logCall = (console.log as jasmine.Spy).calls.mostRecent();
      const dataArg = JSON.stringify(logCall.args[1]);

      expect(dataArg).toContain('[REDACTED]');
      expect(dataArg).not.toContain('user@example.com');
    });
  });

  describe('User ID Tracking', () => {
    it('should set and track user ID', () => {
      service.setUserId('admin-user-123');

      // Access private property for testing
      expect((service as any).userId).toBe('admin-user-123');
    });

    it('should clear user ID when set to null', () => {
      service.setUserId('admin-user-123');
      service.setUserId(null);

      expect((service as any).userId).toBeNull();
    });
  });

  describe('Session ID', () => {
    it('should generate unique session ID', () => {
      const sessionId1 = service['generateSessionId']();
      const sessionId2 = service['generateSessionId']();

      expect(sessionId1).toBeTruthy();
      expect(sessionId2).toBeTruthy();
      expect(sessionId1).not.toBe(sessionId2);
    });
  });
});
