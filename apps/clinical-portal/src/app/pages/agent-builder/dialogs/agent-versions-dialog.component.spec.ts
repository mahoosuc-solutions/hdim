import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef, MatDialog } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AgentVersionsDialogComponent } from './agent-versions-dialog.component';
import { AgentBuilderService } from '../services/agent-builder.service';
import { ToastService } from '../../../services/toast.service';
import { LoggerService } from '../../../services/logger.service';
import { AgentConfiguration, AgentVersion, Page } from '../models/agent.model';
import { createMockMatDialog } from '../../testing/mocks';
import { createMockMatDialogRef } from '../../testing/mocks';

describe('AgentVersionsDialogComponent', () => {
  let component: AgentVersionsDialogComponent;
  let fixture: ComponentFixture<AgentVersionsDialogComponent>;
  let mockAgentService: any;
  let mockToastService: any;
  let mockLoggerService: any;
  let mockDialogRef: any;
  let mockDialog: any;

  const mockAgent: AgentConfiguration = {
    id: 'agent-123',
    tenantId: 'tenant1',
    name: 'Test Agent',
    description: 'Test description',
    modelProvider: 'claude',
    status: 'ACTIVE',
    version: 'v2',
    createdAt: '2026-01-20T10:00:00Z',
    createdBy: 'user@example.com',
  } as AgentConfiguration;

  const mockVersions: AgentVersion[] = [
    {
      id: 'v2',
      agentConfigurationId: 'agent-123',
      versionNumber: '2.0.0',
      status: 'PUBLISHED',
      changeType: 'MINOR',
      changeSummary: 'Added new guardrails',
      configurationSnapshot: mockAgent,
      createdAt: '2026-01-22T10:00:00Z',
      createdBy: 'user@example.com',
    },
    {
      id: 'v1',
      agentConfigurationId: 'agent-123',
      versionNumber: '1.0.0',
      status: 'SUPERSEDED',
      changeType: 'MAJOR',
      changeSummary: 'Initial version',
      configurationSnapshot: mockAgent,
      createdAt: '2026-01-20T10:00:00Z',
      createdBy: 'user@example.com',
    },
  ];

  const mockVersionPage: Page<AgentVersion> = {
    content: mockVersions,
    totalElements: 2,
    totalPages: 1,
    number: 0,
    size: 100,
  };

  beforeEach(async () => {
    mockAgentService = { listVersions: jest.fn(), rollbackToVersion: jest.fn() };
    mockToastService = { success: jest.fn(), error: jest.fn(), info: jest.fn() };
    mockLoggerService = { withContext: jest.fn() };
    mockDialogRef = { close: jest.fn() };
    mockDialog = { open: jest.fn() };

    const mockLogger = { info: jest.fn(), error: jest.fn() };
    mockLoggerService.withContext.mockReturnValue(mockLogger as any);

    await TestBed.configureTestingModule({
      imports: [AgentVersionsDialogComponent, NoopAnimationsModule],
      providers: [{ provide: AgentBuilderService, useValue: mockAgentService },
        { provide: ToastService, useValue: mockToastService },
        { provide: LoggerService, useValue: mockLoggerService },
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: MatDialog, useValue: mockDialog },
        { provide: MAT_DIALOG_DATA, useValue: { agent: mockAgent } },
        { provide: MatDialog, useValue: createMockMatDialog() },
        { provide: MatDialogRef, useValue: createMockMatDialogRef() }],
    }).compileComponents();

    fixture = TestBed.createComponent(AgentVersionsDialogComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load versions on init', () => {
    mockAgentService.listVersions.mockReturnValue(of(mockVersionPage));

    fixture.detectChanges(); // Triggers ngOnInit

    expect(mockAgentService.listVersions).toHaveBeenCalledWith('agent-123', 0, 100);
    expect(component.versions.length).toBe(2);
    expect(component.dataSource.data.length).toBe(2);
  });

  it('should handle version loading error', () => {
    mockAgentService.listVersions.mockReturnValue(
      throwError(() => new Error('API error'))
    );

    fixture.detectChanges();

    expect(mockToastService.error).toHaveBeenCalledWith('Failed to load version history');
    expect(component.loading).toBe(false);
  });

  it('should format version status correctly', () => {
    expect(component.formatStatus('PUBLISHED')).toBe('Published');
    expect(component.formatStatus('ROLLED_BACK')).toBe('Rolled Back');
    expect(component.formatStatus('SUPERSEDED')).toBe('Superseded');
  });

  it('should show version detail coming soon message', () => {
    const version = mockVersions[0];
    component.viewVersion(version);

    expect(mockToastService.info).toHaveBeenCalledWith('Version detail view coming soon');
  });

  it('should open comparison dialog with correct data', () => {
    mockAgentService.listVersions.mockReturnValue(of(mockVersionPage));
    const mockDialogInstance = { afterClosed: jest.fn() };
    mockDialogInstance.afterClosed.mockReturnValue(of(null));
    mockDialog.open.mockReturnValue(mockDialogInstance);

    fixture.detectChanges();

    component.compareVersions(mockVersions[1]);

    expect(mockDialog.open).toHaveBeenCalled();
  });

  it('should show rollback confirmation dialog', () => {
    const mockConfirmDialogRef = { afterClosed: jest.fn() };
    mockConfirmDialogRef.afterClosed.mockReturnValue(of(false)); // User cancels
    mockDialog.open.mockReturnValue(mockConfirmDialogRef);

    component.rollbackToVersion(mockVersions[1]);

    expect(mockDialog.open).toHaveBeenCalled();
  });

  it('should perform rollback when confirmed', () => {
    const mockConfirmDialogRef = { afterClosed: jest.fn() };
    mockConfirmDialogRef.afterClosed.mockReturnValue(of(true)); // User confirms
    mockDialog.open.mockReturnValue(mockConfirmDialogRef);

    const updatedAgent = { ...mockAgent, version: 'v1' };
    mockAgentService.rollbackToVersion.mockReturnValue(of(updatedAgent));

    component.rollbackToVersion(mockVersions[1]);

    mockConfirmDialogRef.afterClosed().subscribe(() => {
      expect(mockAgentService.rollbackToVersion).toHaveBeenCalledWith(
        'agent-123',
        'v1'
      );
      expect(mockToastService.success).toHaveBeenCalledWith(
        'Rolled back to version 1.0.0'
      );
      expect(mockDialogRef.close).toHaveBeenCalledWith(true);
    });
  });

  it('should handle rollback error', () => {
    const mockConfirmDialogRef = { afterClosed: jest.fn() };
    mockConfirmDialogRef.afterClosed.mockReturnValue(of(true));
    mockDialog.open.mockReturnValue(mockConfirmDialogRef);

    mockAgentService.rollbackToVersion.mockReturnValue(
      throwError(() => new Error('Rollback failed'))
    );

    component.rollbackToVersion(mockVersions[1]);

    mockConfirmDialogRef.afterClosed().subscribe(() => {
      expect(mockToastService.error).toHaveBeenCalledWith('Failed to rollback version');
      expect(component.rollingBack).toBe(false);
    });
  });

  it('should close dialog when onClose is called', () => {
    component.onClose();
    expect(mockDialogRef.close).toHaveBeenCalled();
  });

  it('should display current version badge correctly', () => {
    mockAgentService.listVersions.mockReturnValue(of(mockVersionPage));
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const versionRows = compiled.querySelectorAll('.version-row');

    // First row should have "Current" badge
    expect(versionRows[0].textContent).toContain('Current');
  });

  it('should disable rollback button for current version', () => {
    mockAgentService.listVersions.mockReturnValue(of(mockVersionPage));
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const rollbackButtons = compiled.querySelectorAll(
      'button[matTooltip="Rollback to this version"]'
    );

    // Current version (first row) should not have rollback button
    expect(rollbackButtons.length).toBe(1); // Only one rollback button (for v1)
  });
});
