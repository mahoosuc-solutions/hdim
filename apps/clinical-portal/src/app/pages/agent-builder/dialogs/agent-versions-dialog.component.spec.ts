import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialog, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { AgentVersionsDialogComponent } from './agent-versions-dialog.component';
import { AgentBuilderService } from '../services/agent-builder.service';
import { ToastService } from '../../../services/toast.service';
import { LoggerService } from '../../../services/logger.service';
import { AgentConfiguration, AgentVersion, Page } from '../models/agent.model';

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
      configurationSnapshot: { name: 'Test Agent v2' },
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
      configurationSnapshot: { name: 'Test Agent v1' },
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
    mockAgentService = { listVersions: jest.fn(), rollbackToVersion: jest.fn(), getVersion: jest.fn() };
    mockToastService = { success: jest.fn(), error: jest.fn(), info: jest.fn() };
    mockLoggerService = { withContext: jest.fn() };
    mockDialogRef = { close: jest.fn() };
    mockDialog = { open: jest.fn() };

    const mockLogger = { info: jest.fn(), error: jest.fn() };
    mockLoggerService.withContext.mockReturnValue(mockLogger as any);

    await TestBed.configureTestingModule({
      imports: [AgentVersionsDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: AgentBuilderService, useValue: mockAgentService },
        { provide: ToastService, useValue: mockToastService },
        { provide: LoggerService, useValue: mockLoggerService },
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { agent: mockAgent } }],
    })
    .overrideProvider(MatDialog, { useValue: mockDialog })
    .compileComponents();

    fixture = TestBed.createComponent(AgentVersionsDialogComponent);
    component = fixture.componentInstance;
  });

  it('should load versions on init', () => {
    mockAgentService.listVersions.mockReturnValue(of(mockVersionPage));

    fixture.detectChanges(); // Triggers ngOnInit

    expect(mockAgentService.listVersions).toHaveBeenCalledWith('agent-123', 0, 100);
    expect(component.versions.length).toBe(2);
  });

  it('should handle version loading error', () => {
    mockAgentService.listVersions.mockReturnValue(
      throwError(() => new Error('API error'))
    );

    fixture.detectChanges();

    expect(mockToastService.error).toHaveBeenCalledWith('Failed to load version history');
  });

  it('opens version details dialog when view action is used', () => {
    mockAgentService.getVersion.mockReturnValue(of(mockVersions[0]));

    component.viewVersion(mockVersions[0]);

    expect(mockAgentService.getVersion).toHaveBeenCalledWith('agent-123', 'v2');
    expect(mockDialog.open).toHaveBeenCalled();
  });

  it('shows error when version details fail to load', () => {
    mockAgentService.getVersion.mockReturnValue(throwError(() => new Error('API error')));

    component.viewVersion(mockVersions[0]);

    expect(mockToastService.error).toHaveBeenCalledWith('Failed to load version details');
  });

  it('should open comparison dialog with correct data', () => {
    mockAgentService.listVersions.mockReturnValue(of(mockVersionPage));
    const mockDialogInstance = { afterClosed: () => of(null) };
    mockDialog.open.mockReturnValue(mockDialogInstance);

    fixture.detectChanges();

    component.compareVersions(mockVersions[1]);

    expect(mockDialog.open).toHaveBeenCalled();
  });

  it('should show rollback confirmation dialog', () => {
    const mockConfirmDialogRef = { afterClosed: () => of(false) };
    mockDialog.open.mockReturnValue(mockConfirmDialogRef);

    component.rollbackToVersion(mockVersions[1]);

    expect(mockDialog.open).toHaveBeenCalled();
  });

  it('should perform rollback when confirmed', () => {
    const updatedAgent = { ...mockAgent, version: 'v1' };
    mockAgentService.rollbackToVersion.mockReturnValue(of(updatedAgent));
    const mockConfirmDialogRef = { afterClosed: () => of(true) };
    mockDialog.open.mockReturnValue(mockConfirmDialogRef);

    component.rollbackToVersion(mockVersions[1]);

    expect(mockAgentService.rollbackToVersion).toHaveBeenCalledWith(
      'agent-123',
      'v1'
    );
    expect(mockToastService.success).toHaveBeenCalledWith(
      'Rolled back to version 1.0.0'
    );
    expect(mockDialogRef.close).toHaveBeenCalledWith(true);
  });

  it('should handle rollback error', () => {
    mockAgentService.rollbackToVersion.mockReturnValue(
      throwError(() => new Error('Rollback failed'))
    );
    const mockConfirmDialogRef = { afterClosed: () => of(true) };
    mockDialog.open.mockReturnValue(mockConfirmDialogRef);

    component.rollbackToVersion(mockVersions[1]);

    expect(mockToastService.error).toHaveBeenCalledWith('Failed to rollback version');
    expect(component.rollingBack).toBe(false);
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
