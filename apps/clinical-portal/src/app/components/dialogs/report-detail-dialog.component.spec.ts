import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { of, throwError } from 'rxjs';
import { ReportDetailDialogComponent } from './report-detail-dialog.component';
import { EvaluationService } from '../../services/evaluation.service';

describe('ReportDetailDialogComponent', () => {
  let fixture: ComponentFixture<ReportDetailDialogComponent>;
  let component: ReportDetailDialogComponent;
  let dialogRef: { close: jest.Mock };
  let evaluationService: jest.Mocked<EvaluationService>;

  const report = {
    id: 'r1',
    reportName: 'Quality Report',
    reportType: 'PATIENT',
    status: 'COMPLETED',
    createdAt: '2024-01-02T10:00:00Z',
    createdBy: 'tester',
    tenantId: 't1',
    reportData: JSON.stringify({
      qualityScore: 88,
      totalMeasures: 4,
      compliantMeasures: 3,
      measureResults: [],
    }),
  } as any;

  beforeEach(async () => {
    dialogRef = { close: jest.fn() };
    evaluationService = {
      exportAndDownloadReport: jest.fn(),
    } as unknown as jest.Mocked<EvaluationService>;

    await TestBed.configureTestingModule({
      imports: [ReportDetailDialogComponent],
      providers: [
        { provide: MAT_DIALOG_DATA, useValue: report },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: EvaluationService, useValue: evaluationService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(ReportDetailDialogComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('parses report data on init', () => {
    component.ngOnInit();

    expect(component.parsedData).toEqual(
      expect.objectContaining({ qualityScore: 88 })
    );
  });

  it('handles invalid report data JSON', () => {
    const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    component.report.reportData = '{bad json';

    component.ngOnInit();

    expect(component.parsedData).toBeNull();
    expect(errorSpy).toHaveBeenCalled();
  });

  it('formats date time strings', () => {
    const value = component.formatDateTime('2024-01-02T10:00:00Z');
    expect(value).toContain('2024');
  });

  it('exports report to CSV and Excel', () => {
    evaluationService.exportAndDownloadReport.mockReturnValue(of(void 0));

    component.onExportCsv();
    component.onExportExcel();

    expect(evaluationService.exportAndDownloadReport).toHaveBeenCalledWith(
      report.id,
      report.reportName,
      'csv'
    );
    expect(evaluationService.exportAndDownloadReport).toHaveBeenCalledWith(
      report.id,
      report.reportName,
      'excel'
    );
  });

  it('logs export errors', () => {
    const errorSpy = jest.spyOn(console, 'error').mockImplementation(() => undefined);
    evaluationService.exportAndDownloadReport.mockReturnValue(
      throwError(() => new Error('export failed'))
    );

    component.onExportCsv();

    expect(errorSpy).toHaveBeenCalled();
  });

  it('closes the dialog', () => {
    component.onClose();

    expect(dialogRef.close).toHaveBeenCalled();
  });
});
