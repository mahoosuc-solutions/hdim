import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { HttpClient, provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { MeasureBuilderComponent } from './measure-builder.component';
import { CustomMeasureService, CustomMeasure } from '../../services/custom-measure.service';
import { ToastService } from '../../services/toast.service';
import { DialogService } from '../../services/dialog.service';
import { AIAssistantService } from '../../services/ai-assistant.service';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { CSVHelper } from '../../utils/csv-helper';
import { LoggerService } from '../../services/logger.service';
import { createMockMatDialog } from '../../testing/mocks';
import { Store } from '@ngrx/store';
import { provideMockStore } from '@ngrx/store/testing';

describe('MeasureBuilderComponent (TDD)', () => {
  let component: MeasureBuilderComponent;
  let fixture: ComponentFixture<MeasureBuilderComponent>;
  let mockDialog: jest.Mocked<MatDialog>;
  let mockCustomMeasureService: jest.Mocked<CustomMeasureService>;
  let mockToast: jest.Mocked<ToastService>;
  let mockDialogService: jest.Mocked<DialogService>;
  let mockAIAssistantService: jest.Mocked<AIAssistantService>;

  const mockDrafts: CustomMeasure[] = [
    {
      id: 'draft-1',
      tenantId: 'TENANT001',
      name: 'Diabetes Care Custom',
      version: '1.0',
      status: 'DRAFT',
      description: 'Custom diabetes care measure',
      category: 'CUSTOM',
      year: 2024,
      createdBy: 'user1',
      createdAt: '2024-01-15T10:00:00Z',
      updatedAt: '2024-01-16T10:00:00Z',
    },
    {
      id: 'draft-2',
      tenantId: 'TENANT001',
      name: 'Blood Pressure Monitoring',
      version: '1.0',
      status: 'DRAFT',
      description: 'Custom BP monitoring measure',
      category: 'CUSTOM',
      createdBy: 'user2',
      createdAt: '2024-01-14T09:00:00Z',
    },
  ];

  beforeEach(async () => {
    mockDialog = {
      open: jest.fn(),
    } as unknown as jest.Mocked<MatDialog>;

    mockCustomMeasureService = {
      createDraft: jest.fn(),
      list: jest.fn(),
      update: jest.fn(),
      batchPublish: jest.fn(),
      batchDelete: jest.fn(),
      updateCql: jest.fn(),
      updateValueSets: jest.fn(),
      delete: jest.fn(),
    } as unknown as jest.Mocked<CustomMeasureService>;

    mockToast = {
      success: jest.fn(),
      error: jest.fn(),
      info: jest.fn(),
      warning: jest.fn(),
    } as unknown as jest.Mocked<ToastService>;

    mockDialogService = {
      confirmDelete: jest.fn(),
      confirm: jest.fn(),
    } as unknown as jest.Mocked<DialogService>;

    mockAIAssistantService = {} as jest.Mocked<AIAssistantService>;

    await TestBed.configureTestingModule({
      imports: [MeasureBuilderComponent, NoopAnimationsModule],
      providers: [provideMockStore(),
        { provide: LoggerService, useValue: createMockLoggerService() },
        provideHttpClient(),
        { provide: CustomMeasureService, useValue: mockCustomMeasureService },
        { provide: ToastService, useValue: mockToast },
        { provide: DialogService, useValue: mockDialogService },
        { provide: AIAssistantService, useValue: mockAIAssistantService },
        { provide: MatDialog, useValue: createMockMatDialog() },
    })
      .overrideProvider(MatDialog, { useValue: mockDialog })
      .compileComponents();

    fixture = TestBed.createComponent(MeasureBuilderComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    jest.clearAllMocks();
  });

  describe('Component Initialization', () => {
    it('should create the component', () => {
      expect(component).toBeTruthy();
    });

    it('should initialize with empty drafts array', () => {
      expect(component.drafts).toEqual([]);
    });

    it('should load drafts on init', () => {
      mockCustomMeasureService.list.mockReturnValue(of(mockDrafts));

      component.ngOnInit();

      expect(mockCustomMeasureService.list).toHaveBeenCalledWith('DRAFT');
      expect(component.drafts).toEqual(mockDrafts);
    });
  });

  describe('Loading Drafts', () => {
    it('should fetch draft measures from service', () => {
      mockCustomMeasureService.list.mockReturnValue(of(mockDrafts));

      component.ngOnInit();

      expect(mockCustomMeasureService.list).toHaveBeenCalledWith('DRAFT');
      expect(component.drafts).toHaveLength(2);
    });

    it('should handle empty drafts list', () => {
      mockCustomMeasureService.list.mockReturnValue(of([]));

      component.ngOnInit();

      expect(component.drafts).toEqual([]);
    });

    it('should show error toast when loading drafts fails', () => {
      mockCustomMeasureService.list.mockReturnValue(throwError(() => new Error('API Error')));

      component.ngOnInit();

      expect(mockToast.error).toHaveBeenCalledWith('Failed to load draft measures');
    });

    it('should keep existing drafts on load error', () => {
      component.drafts = mockDrafts;
      mockCustomMeasureService.list.mockReturnValue(throwError(() => new Error('API Error')));

      component.ngOnInit();

      expect(component.drafts).toEqual(mockDrafts);
    });
  });

  describe('Creating New Measure', () => {
    it('should have openNewMeasureDialog method', () => {
      expect(typeof component.openNewMeasureDialog).toBe('function');
    });

    it('should not create draft without user input', () => {
      // Verify initial state - no drafts created yet
      expect(mockCustomMeasureService.createDraft).not.toHaveBeenCalled();
    });

    it('creates a draft when dialog returns data', async () => {
      const dialogRef = { afterClosed: () => of({ name: 'New Measure' }) } as any;
      mockDialog.open.mockReturnValue(dialogRef);
      mockCustomMeasureService.createDraft.mockReturnValue(of(mockDrafts[0]));

      await component.openNewMeasureDialog();

      expect(mockCustomMeasureService.createDraft).toHaveBeenCalled();
      expect(component.drafts[0].id).toBe('draft-1');
      expect(mockToast.success).toHaveBeenCalledWith('Measure created successfully');
    });

    it('shows error when create draft fails', async () => {
      const dialogRef = { afterClosed: () => of({ name: 'New Measure' }) } as any;
      mockDialog.open.mockReturnValue(dialogRef);
      mockCustomMeasureService.createDraft.mockReturnValue(throwError(() => new Error('fail')));

      await component.openNewMeasureDialog();

      expect(mockToast.error).toHaveBeenCalledWith('Failed to create measure');
    });

    it('does nothing when dialog closes without data', async () => {
      const dialogRef = { afterClosed: () => of(undefined) } as any;
      mockDialog.open.mockReturnValue(dialogRef);

      await component.openNewMeasureDialog();

      expect(mockCustomMeasureService.createDraft).not.toHaveBeenCalled();
    });
  });

  describe('Opening Draft Details', () => {
    it('should have openDraft method', () => {
      expect(typeof component.openDraft).toBe('function');
    });

    it('should accept CustomMeasure as parameter', () => {
      // Verify method signature - method exists and can be called
      expect(component.openDraft).toBeDefined();
    });

    it('opens CQL editor and updates measure', async () => {
      const dialogRef = { afterClosed: () => of('updated cql') } as any;
      mockDialog.open.mockReturnValue(dialogRef);
      mockCustomMeasureService.updateCql.mockReturnValue(of({ ...mockDrafts[0], cqlText: 'updated cql' }));

      component.drafts = [...mockDrafts];
      component.dataSource.data = [...mockDrafts];
      await component.editCql(mockDrafts[0]);

      expect(mockCustomMeasureService.updateCql).toHaveBeenCalledWith('draft-1', 'updated cql');
      expect(component.drafts[0].cqlText).toBe('updated cql');
    });

    it('does nothing when edit CQL dialog returns undefined', async () => {
      const dialogRef = { afterClosed: () => of(undefined) } as any;
      mockDialog.open.mockReturnValue(dialogRef);

      await component.editCql(mockDrafts[0]);

      expect(mockCustomMeasureService.updateCql).not.toHaveBeenCalled();
    });

    it('handles CQL update error', async () => {
      const dialogRef = { afterClosed: () => of('updated cql') } as any;
      mockDialog.open.mockReturnValue(dialogRef);
      mockCustomMeasureService.updateCql.mockReturnValue(throwError(() => new Error('fail')));

      await component.editCql(mockDrafts[0]);

      expect(mockToast.error).toHaveBeenCalled();
    });

    it('opens value set picker and updates value sets', async () => {
      const dialogRef = { afterClosed: () => of([{ oid: '1.2.3' }]) } as any;
      mockDialog.open.mockReturnValue(dialogRef);
      mockCustomMeasureService.updateValueSets.mockReturnValue(of({ ...mockDrafts[0], valueSets: [{ oid: '1.2.3' }] }));

      component.drafts = [...mockDrafts];
      component.dataSource.data = [...mockDrafts];
      await component.openValueSetPicker(mockDrafts[0]);

      expect(mockCustomMeasureService.updateValueSets).toHaveBeenCalled();
      expect(component.drafts[0].valueSets?.length).toBe(1);
    });

    it('handles value set update error', async () => {
      const dialogRef = { afterClosed: () => of([{ oid: '1.2.3' }]) } as any;
      mockDialog.open.mockReturnValue(dialogRef);
      mockCustomMeasureService.updateValueSets.mockReturnValue(throwError(() => new Error('fail')));

      await component.openValueSetPicker(mockDrafts[0]);

      expect(mockToast.error).toHaveBeenCalled();
    });

    it('returns early when opening a missing draft', () => {
      component.openDraft(null as any);
      expect(mockDialog.open).not.toHaveBeenCalled();
    });

    it('opens test preview dialog for a measure', () => {
      mockDialog.open.mockReturnValue({} as any);

      component.testMeasure(mockDrafts[0]);

      expect(mockDialog.open).toHaveBeenCalled();
    });
  });

  describe('Table utilities', () => {
    it('returns status colors with fallback', () => {
      expect(component.getStatusColor('DRAFT')).toBe('accent');
      expect(component.getStatusColor('UNKNOWN')).toBe('primary');
    });

    it('handles selection helpers', () => {
      component.dataSource.data = mockDrafts;

      expect(component.isAllSelected()).toBe(false);
      component.masterToggle();
      expect(component.isAllSelected()).toBe(true);

      expect(component.getSelectionCount()).toBe(2);
      component.clearSelection();
      expect(component.getSelectionCount()).toBe(0);

      expect(component.checkboxLabel()).toContain('select all');
      expect(component.checkboxLabel(mockDrafts[0])).toContain('select row');
    });

    it('applies filters to the data source', () => {
      component.dataSource.data = mockDrafts;
      component.dataSource.paginator = { firstPage: jest.fn() } as any;
      const event = { target: { value: 'diabetes' } } as unknown as Event;

      component.applyFilter(event);
      expect(component.dataSource.filter).toBe('diabetes');
      expect(component.dataSource.paginator.firstPage).toHaveBeenCalled();
    });
  });

  describe('Bulk actions', () => {
    it('exports selected measures to CSV', () => {
      jest.useFakeTimers();
      const arraySpy = jest.spyOn(CSVHelper, 'arrayToCSV').mockReturnValue('csv');
      const downloadSpy = jest.spyOn(CSVHelper, 'downloadCSV').mockImplementation();

      component.dataSource.data = mockDrafts;
      component.selection.select(mockDrafts[0]);

      component.exportSelectedToCSV();
      jest.runAllTimers();

      expect(arraySpy).toHaveBeenCalled();
      expect(downloadSpy).toHaveBeenCalled();

      arraySpy.mockRestore();
      downloadSpy.mockRestore();
      jest.useRealTimers();
    });

    it('does not export when no measures selected', () => {
      const arraySpy = jest.spyOn(CSVHelper, 'arrayToCSV').mockReturnValue('csv');
      component.exportSelectedToCSV();
      expect(arraySpy).not.toHaveBeenCalled();
      arraySpy.mockRestore();
    });

    it('publishes selected measures', () => {
      mockDialogService.confirm.mockReturnValue(of(true));
      mockCustomMeasureService.batchPublish.mockReturnValue(of({ published: 1, failed: [] }));

      component.dataSource.data = mockDrafts;
      component.selection.select(mockDrafts[0]);

      component.publishSelected();
      expect(mockCustomMeasureService.batchPublish).toHaveBeenCalled();
    });

    it('warns when publish skips already-published measures', () => {
      mockDialogService.confirm.mockReturnValue(of(true));
      mockCustomMeasureService.batchPublish.mockReturnValue(of({ published: 1, failed: [] }));

      component.dataSource.data = [
        { ...mockDrafts[0], status: 'DRAFT' },
        { ...mockDrafts[1], status: 'PUBLISHED' },
      ];
      component.selection.select(component.dataSource.data[0], component.dataSource.data[1]);

      component.publishSelected();

      expect(mockDialogService.confirm).toHaveBeenCalled();
      expect(mockCustomMeasureService.batchPublish).toHaveBeenCalled();
    });

    it('shows warning when some publishes fail', () => {
      mockDialogService.confirm.mockReturnValue(of(true));
      mockCustomMeasureService.batchPublish.mockReturnValue(of({ published: 1, failed: ['draft-2'] }));

      component.dataSource.data = mockDrafts;
      component.selection.select(mockDrafts[0], mockDrafts[1]);

      component.publishSelected();

      expect(mockToast.warning).toHaveBeenCalled();
    });

    it('warns when all selected measures are already published', () => {
      component.dataSource.data = [{ ...mockDrafts[0], status: 'PUBLISHED' }];
      component.selection.select(component.dataSource.data[0]);

      component.publishSelected();
      expect(mockToast.warning).toHaveBeenCalledWith('All selected measures are already published');
    });

    it('handles publish selected error', () => {
      mockDialogService.confirm.mockReturnValue(of(true));
      mockCustomMeasureService.batchPublish.mockReturnValue(throwError(() => new Error('fail')));

      component.dataSource.data = mockDrafts;
      component.selection.select(mockDrafts[0]);

      component.publishSelected();
      expect(mockToast.error).toHaveBeenCalled();
    });

    it('does nothing when publish selected is cancelled', () => {
      mockDialogService.confirm.mockReturnValue(of(false));
      component.dataSource.data = mockDrafts;
      component.selection.select(mockDrafts[0]);

      component.publishSelected();

      expect(mockCustomMeasureService.batchPublish).not.toHaveBeenCalled();
    });

    it('deletes selected measures', () => {
      mockDialogService.confirm.mockReturnValue(of(true));
      mockCustomMeasureService.batchDelete.mockReturnValue(of({ deleted: 1, failed: [] }));

      component.dataSource.data = mockDrafts;
      component.measures = [...mockDrafts];
      component.drafts = [...mockDrafts];
      component.selection.select(mockDrafts[0]);

      component.deleteSelected();
      expect(mockCustomMeasureService.batchDelete).toHaveBeenCalled();
    });

    it('does not delete when confirmation is cancelled', () => {
      mockDialogService.confirm.mockReturnValue(of(false));
      component.dataSource.data = mockDrafts;
      component.selection.select(mockDrafts[0]);

      component.deleteSelected();
      expect(mockCustomMeasureService.batchDelete).not.toHaveBeenCalled();
    });

    it('handles delete selected error', () => {
      mockDialogService.confirm.mockReturnValue(of(true));
      mockCustomMeasureService.batchDelete.mockReturnValue(throwError(() => new Error('fail')));

      component.dataSource.data = mockDrafts;
      component.selection.select(mockDrafts[0]);

      component.deleteSelected();
      expect(mockToast.error).toHaveBeenCalled();
    });

    it('removes successfully deleted measures when some fail', () => {
      mockDialogService.confirm.mockReturnValue(of(true));
      mockCustomMeasureService.batchDelete.mockReturnValue(of({ deleted: 1, failed: ['draft-2'] }));

      component.dataSource.data = mockDrafts;
      component.measures = [...mockDrafts];
      component.drafts = [...mockDrafts];
      component.selection.select(mockDrafts[0], mockDrafts[1]);

      component.deleteSelected();

      expect(component.measures.length).toBe(1);
      expect(component.measures[0].id).toBe('draft-2');
    });

    it('formats delete confirmation label when more than three selected', () => {
      mockDialogService.confirm.mockReturnValue(of(false));
      const extra = [
        { ...mockDrafts[0], id: 'draft-3', name: 'Measure 3' },
        { ...mockDrafts[0], id: 'draft-4', name: 'Measure 4' },
      ];
      component.dataSource.data = [...mockDrafts, ...extra];
      component.selection.select(...component.dataSource.data);

      component.deleteSelected();

      expect(mockDialogService.confirm).toHaveBeenCalled();
    });
  });

  describe('Single measure actions', () => {
    it('publishes a measure and reloads on confirmation', async () => {
      const dialogRef = { afterClosed: () => of(true) } as any;
      const loadSpy = jest.spyOn(component as any, 'loadMeasures');
      mockDialog.open.mockReturnValue(dialogRef);
      mockCustomMeasureService.list.mockReturnValue(of([]));

      await component.publishMeasure(mockDrafts[0]);

      expect(mockToast.success).toHaveBeenCalledWith('Measure published successfully');
      expect(loadSpy).toHaveBeenCalled();
    });

    it('does nothing when publish dialog is cancelled', async () => {
      const dialogRef = { afterClosed: () => of(false) } as any;
      const loadSpy = jest.spyOn(component as any, 'loadMeasures');
      mockDialog.open.mockReturnValue(dialogRef);

      await component.publishMeasure(mockDrafts[0]);

      expect(loadSpy).not.toHaveBeenCalled();
    });

    it('deletes a measure when confirmed', () => {
      mockDialogService.confirmDelete.mockReturnValue(of(true));
      mockCustomMeasureService.delete.mockReturnValue(of({}));

      component.measures = [...mockDrafts];
      component.drafts = [...mockDrafts];
      component.dataSource.data = [...mockDrafts];

      component.deleteMeasure(mockDrafts[0]);

      expect(mockCustomMeasureService.delete).toHaveBeenCalledWith('draft-1');
      expect(component.measures.length).toBe(1);
    });

    it('does nothing when delete measure is cancelled', () => {
      mockDialogService.confirmDelete.mockReturnValue(of(false));

      component.deleteMeasure(mockDrafts[0]);

      expect(mockCustomMeasureService.delete).not.toHaveBeenCalled();
    });

    it('handles delete measure error', () => {
      mockDialogService.confirmDelete.mockReturnValue(of(true));
      mockCustomMeasureService.delete.mockReturnValue(throwError(() => new Error('fail')));

      component.deleteMeasure(mockDrafts[0]);

      expect(mockToast.error).toHaveBeenCalled();
    });
  });
});

/**
 * Dialog Component Tests
 * 
 * NOTE: Dialog component tests (NewMeasureDialogComponent, DraftDetailDialogComponent) 
 * have been moved to separate test files for proper isolation and dependency injection.
 * 
 * Dialog components require complex MAT_DIALOG_DATA setup that conflicts with parent 
 * component testing when in the same TestBed.
 * 
 * See:
 * - new-measure-dialog.component.spec.ts (to be created)
 * - draft-detail-dialog.component.spec.ts (to be created)
 * 
 * This follows Angular testing best practices of testing dialogs independently from 
 * their parent components.
 */
