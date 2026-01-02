import { TestBed } from '@angular/core/testing';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { of } from 'rxjs';
import { DialogService } from './dialog.service';
import { Patient } from '../models/patient.model';

describe('DialogService', () => {
  let service: DialogService;
  let dialog: jest.Mocked<MatDialog>;

  beforeEach(() => {
    const dialogSpy = {
      open: jest.fn(),
      closeAll: jest.fn(),
    } as unknown as jest.Mocked<MatDialog>;

    TestBed.configureTestingModule({
      providers: [DialogService, { provide: MatDialog, useValue: dialogSpy }],
    });

    service = TestBed.inject(DialogService);
    dialog = TestBed.inject(MatDialog) as jest.Mocked<MatDialog>;
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('openPatientEdit', () => {
    it('should open patient edit dialog in create mode', () => {
      const mockDialogRef = {
        afterClosed: () => of(null),
      } as MatDialogRef<any>;

      dialog.open.mockReturnValue(mockDialogRef);

      service.openPatientEdit().subscribe();

      expect(dialog.open).toHaveBeenCalled();
      const callArgs = dialog.open.mock.calls[dialog.open.mock.calls.length - 1];
      expect(callArgs[1]?.data?.mode).toBe('create');
    });

    it('should open patient edit dialog in edit mode with patient', () => {
      const mockPatient: Patient = {
        resourceType: 'Patient',
        id: 'patient-123',
        name: [{ given: ['John'], family: 'Doe' }],
      };

      const mockDialogRef = {
        afterClosed: () => of(mockPatient),
      } as MatDialogRef<any>;

      dialog.open.mockReturnValue(mockDialogRef);

      service.openPatientEdit(mockPatient).subscribe((result) => {
        expect(result).toEqual(mockPatient);
      });

      expect(dialog.open).toHaveBeenCalled();
      const callArgs = dialog.open.mock.calls[dialog.open.mock.calls.length - 1];
      expect(callArgs[1]?.data?.mode).toBe('edit');
      expect(callArgs[1]?.data?.patient).toEqual(mockPatient);
    });
  });

  describe('openEvaluationDetails', () => {
    it('should open evaluation details dialog', () => {
      const mockDialogRef = {
        afterClosed: () => of(null),
      } as MatDialogRef<any>;

      dialog.open.mockReturnValue(mockDialogRef);

      service.openEvaluationDetails('eval-123', 'John Doe', 'CMS125');

      expect(dialog.open).toHaveBeenCalled();
      const callArgs = dialog.open.mock.calls[dialog.open.mock.calls.length - 1];
      expect(callArgs[1]?.data?.evaluationId).toBe('eval-123');
      expect(callArgs[1]?.data?.patientName).toBe('John Doe');
      expect(callArgs[1]?.data?.measureName).toBe('CMS125');
    });
  });

  describe('openAdvancedFilter', () => {
    it('should open advanced filter dialog', () => {
      const fields = [
        { name: 'name', label: 'Name', type: 'text' as const },
      ];

      const mockDialogRef = {
        afterClosed: () => of(null),
      } as MatDialogRef<any>;

      dialog.open.mockReturnValue(mockDialogRef);

      service.openAdvancedFilter(fields).subscribe();

      expect(dialog.open).toHaveBeenCalled();
      const callArgs = dialog.open.mock.calls[dialog.open.mock.calls.length - 1];
      expect(callArgs[1]?.data?.availableFields).toEqual(fields);
    });
  });

  describe('openBatchEvaluation', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('should emit mock batch result after delay', () => {
      const results: any[] = [];

      service.openBatchEvaluation().subscribe((result) => results.push(result));

      jest.advanceTimersByTime(1000);

      expect(results).toHaveLength(1);
      expect(results[0]).toEqual({
        successCount: 10,
        errorCount: 0,
        results: [],
      });
    });
  });

  describe('openExportConfig', () => {
    beforeEach(() => {
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('should emit export config after delay', () => {
      const results: any[] = [];

      service.openExportConfig(['name', 'dob'], [{ id: 1 }]).subscribe((result) => results.push(result));

      jest.advanceTimersByTime(500);

      expect(results).toHaveLength(1);
      expect(results[0]).toEqual({
        format: 'csv',
        columns: ['name', 'dob'],
        fileName: 'export.csv',
      });
    });
  });

  describe('openErrorDetails', () => {
    it('should log error details from Error', () => {
      const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
      const error = new Error('Boom');

      service.openErrorDetails(error);

      expect(errorSpy).toHaveBeenCalled();
      const [, errorInfo] = errorSpy.mock.calls[0];
      expect(errorInfo.message).toBe('Boom');
      expect(errorInfo.severity).toBe('error');
      errorSpy.mockRestore();
    });

    it('should log provided error info', () => {
      const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
      const errorInfo = {
        message: 'Bad request',
        timestamp: new Date('2024-01-01T00:00:00Z'),
        severity: 'warning' as const,
      };

      service.openErrorDetails(errorInfo);

      expect(errorSpy).toHaveBeenCalledWith('Error Details:', errorInfo);
      errorSpy.mockRestore();
    });
  });

  describe('openHelp', () => {
    it('should log help topic', () => {
      const logSpy = jest.spyOn(console, 'log').mockImplementation(() => undefined);

      service.openHelp('patients');

      expect(logSpy).toHaveBeenCalledWith('Opening help for topic:', 'patients');
      logSpy.mockRestore();
    });
  });

  describe('confirm', () => {
    it('should open confirmation dialog and return true on confirm', (done) => {
      const mockDialogRef = {
        afterClosed: () => of(true),
      } as MatDialogRef<any>;

      dialog.open.mockReturnValue(mockDialogRef);

      service.confirm('Test Title', 'Test message').subscribe((result) => {
        expect(result).toBe(true);
        done();
      });

      expect(dialog.open).toHaveBeenCalled();
    });

    it('should return false on cancel', (done) => {
      const mockDialogRef = {
        afterClosed: () => of(false),
      } as MatDialogRef<any>;

      dialog.open.mockReturnValue(mockDialogRef);

      service.confirm('Test Title', 'Test message').subscribe((result) => {
        expect(result).toBe(false);
        done();
      });
    });
  });

  describe('confirmWarning', () => {
    it('should open warning confirmation with custom button text', () => {
      const mockDialogRef = {
        afterClosed: () => of(true),
      } as MatDialogRef<any>;

      dialog.open.mockReturnValue(mockDialogRef);

      service.confirmWarning('Warning', 'Proceed?').subscribe();

      expect(dialog.open).toHaveBeenCalled();
      const callArgs = dialog.open.mock.calls[dialog.open.mock.calls.length - 1];
      expect(callArgs[1]?.data?.confirmText).toBe('Continue');
      expect(callArgs[1]?.data?.confirmColor).toBe('warn');
    });
  });

  describe('confirmDelete', () => {
    it('should open delete confirmation with proper styling', () => {
      const mockDialogRef = {
        afterClosed: () => of(true),
      } as MatDialogRef<any>;

      dialog.open.mockReturnValue(mockDialogRef);

      service.confirmDelete('Test Item', 'patient').subscribe();

      expect(dialog.open).toHaveBeenCalled();
      const callArgs = dialog.open.mock.calls[dialog.open.mock.calls.length - 1];
      expect(callArgs[1]?.data?.confirmColor).toBe('warn');
      expect(callArgs[1]?.data?.icon).toBe('warning');
      expect(callArgs[1]?.data?.message).toContain('Test Item');
    });
  });

  describe('utility methods', () => {
    it('should close all dialogs', () => {
      service.closeAll();
      expect(dialog.closeAll).toHaveBeenCalled();
    });

    it('should check if dialogs are open', () => {
      (dialog as any).openDialogs = [{}];
      expect(service.hasOpenDialogs()).toBe(true);
    });

    it('should return false when no dialogs are open', () => {
      (dialog as any).openDialogs = [];
      expect(service.hasOpenDialogs()).toBe(false);
    });

    it('should get open dialog count', () => {
      (dialog as any).openDialogs = [{}, {}];
      expect(service.getOpenDialogCount()).toBe(2);
    });

    it('should return zero when no dialogs are open', () => {
      (dialog as any).openDialogs = [];
      expect(service.getOpenDialogCount()).toBe(0);
    });
  });
});
