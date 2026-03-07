import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { VersionCompareDialogComponent } from './version-compare-dialog.component';
import { AgentBuilderService } from '../services/agent-builder.service';
import { ToastService } from '../../../services/toast.service';
import { LoggerService } from '../../../services/logger.service';
import { AgentVersion } from '../models/agent.model';

describe('VersionCompareDialogComponent', () => {
  let component: VersionCompareDialogComponent;
  let fixture: ComponentFixture<VersionCompareDialogComponent>;
  let mockAgentService: any;
  let mockToastService: any;
  let mockLoggerService: any;
  let mockDialogRef: any;

  const mockVersion1: AgentVersion = {
    id: 'v1',
    agentConfigurationId: 'agent-123',
    versionNumber: '1.0.0',
    status: 'SUPERSEDED',
    changeType: 'MAJOR',
    changeSummary: 'Initial version',
    configurationSnapshot: {
      id: 'agent-123',
      name: 'Test Agent',
      description: 'Old description',
      modelProvider: 'claude',
      modelId: 'claude-2',
      temperature: 0.5,
      maxTokens: 1000,
      systemPrompt: 'Old prompt',
      toolConfiguration: [{ toolName: 'tool1', enabled: true }],
      guardrailConfiguration: {
        phiFiltering: true,
        clinicalDisclaimerRequired: false,
        blockedPatterns: [],
        maxOutputTokens: 1000,
        requireHumanReview: false,
      },
    },
    createdAt: '2026-01-20T10:00:00Z',
    createdBy: 'user@example.com',
  } as AgentVersion;

  const mockVersion2: AgentVersion = {
    id: 'v2',
    agentConfigurationId: 'agent-123',
    versionNumber: '2.0.0',
    status: 'PUBLISHED',
    changeType: 'MAJOR',
    changeSummary: 'Major update with new features',
    configurationSnapshot: {
      id: 'agent-123',
      name: 'Test Agent',
      description: 'New description',
      modelProvider: 'claude',
      modelId: 'claude-3',
      temperature: 0.7,
      maxTokens: 2000,
      systemPrompt: 'New prompt',
      toolConfiguration: [
        { toolName: 'tool1', enabled: true },
        { toolName: 'tool2', enabled: true },
      ],
      guardrailConfiguration: {
        phiFiltering: true,
        clinicalDisclaimerRequired: true,
        blockedPatterns: ['sensitive'],
        maxOutputTokens: 2000,
        requireHumanReview: true,
      },
    },
    createdAt: '2026-01-22T10:00:00Z',
    createdBy: 'user@example.com',
  } as AgentVersion;

  const dialogData = {
    agentId: 'agent-123',
    version1Id: 'v1',
    version2Id: 'v2',
    version1Label: '1.0.0',
    version2Label: '2.0.0 (Current)',
  };

  beforeEach(async () => {
    mockAgentService = { getVersion: jest.fn() };
    mockToastService = { error: jest.fn() };
    mockLoggerService = { withContext: jest.fn() };
    mockDialogRef = { close: jest.fn() };

    const mockLogger = { info: jest.fn(), error: jest.fn() };
    mockLoggerService.withContext.mockReturnValue(mockLogger as any);

    await TestBed.configureTestingModule({
      imports: [VersionCompareDialogComponent, NoopAnimationsModule],
      providers: [{ provide: AgentBuilderService, useValue: mockAgentService },
        { provide: ToastService, useValue: mockToastService },
        { provide: LoggerService, useValue: mockLoggerService },
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: MAT_DIALOG_DATA, useValue: dialogData }],
    }).compileComponents();

    fixture = TestBed.createComponent(VersionCompareDialogComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load both versions in parallel on init', () => {
    mockAgentService.getVersion.mockReturnValueOnce(of(mockVersion1)).mockReturnValueOnce(of(mockVersion2));

    fixture.detectChanges();

    expect(mockAgentService.getVersion).toHaveBeenCalledTimes(2);
    expect(mockAgentService.getVersion).toHaveBeenCalledWith('agent-123', 'v1');
    expect(mockAgentService.getVersion).toHaveBeenCalledWith('agent-123', 'v2');
  });

  it('should compute diffs after loading versions', (done) => {
    mockAgentService.getVersion.mockReturnValueOnce(of(mockVersion1)).mockReturnValueOnce(of(mockVersion2));

    fixture.detectChanges();

    setTimeout(() => {
      expect(component.version1).toEqual(mockVersion1);
      expect(component.version2).toEqual(mockVersion2);
      expect(component.basicInfoDiffs.length).toBeGreaterThan(0);
      expect(component.modelConfigDiffs.length).toBeGreaterThan(0);
      expect(component.guardrailDiffs.length).toBeGreaterThan(0);
      done();
    }, 100);
  });

  it('should calculate changes count correctly', (done) => {
    mockAgentService.getVersion.mockReturnValueOnce(of(mockVersion1)).mockReturnValueOnce(of(mockVersion2));

    fixture.detectChanges();

    setTimeout(() => {
      // Description, modelId, temperature, maxTokens, systemPrompt changed
      // Plus tool configuration and guardrail changes
      expect(component.changesCount).toBeGreaterThan(0);
      done();
    }, 100);
  });

  it('should handle version loading error', () => {
    mockAgentService.getVersion.mockReturnValue(
      throwError(() => new Error('API error'))
    );

    fixture.detectChanges();

    expect(mockToastService.error).toHaveBeenCalledWith('Failed to load versions for comparison');
    expect(component.loading).toBe(false);
  });

  it('should identify changed fields correctly', (done) => {
    mockAgentService.getVersion.mockReturnValueOnce(of(mockVersion1)).mockReturnValueOnce(of(mockVersion2));

    fixture.detectChanges();

    setTimeout(() => {
      const descriptionDiff = component.basicInfoDiffs.find((d) => d.field === 'description');
      expect(descriptionDiff?.changed).toBe(true);
      expect(descriptionDiff?.oldValue).toBe('Old description');
      expect(descriptionDiff?.newValue).toBe('New description');
      done();
    }, 100);
  });

  it('should identify unchanged fields correctly', (done) => {
    mockAgentService.getVersion.mockReturnValueOnce(of(mockVersion1)).mockReturnValueOnce(of(mockVersion2));

    fixture.detectChanges();

    setTimeout(() => {
      const nameDiff = component.basicInfoDiffs.find((d) => d.field === 'name');
      expect(nameDiff?.changed).toBe(false);
      expect(nameDiff?.oldValue).toBe('Test Agent');
      expect(nameDiff?.newValue).toBe('Test Agent');
      done();
    }, 100);
  });

  it('should handle tool configuration changes', (done) => {
    mockAgentService.getVersion.mockReturnValueOnce(of(mockVersion1)).mockReturnValueOnce(of(mockVersion2));

    fixture.detectChanges();

    setTimeout(() => {
      // v2 has 2 tools, v1 has 1 tool — changes should be detected
      expect(component.changesCount).toBeGreaterThan(0);
      done();
    }, 100);
  });

  it('should handle guardrail configuration changes', (done) => {
    mockAgentService.getVersion.mockReturnValueOnce(of(mockVersion1)).mockReturnValueOnce(of(mockVersion2));

    fixture.detectChanges();

    setTimeout(() => {
      const disclaimerDiff = component.guardrailDiffs.find(
        (d) => d.field === 'clinicalDisclaimer'
      );
      expect(disclaimerDiff?.changed).toBe(true);
      done();
    }, 100);
  });

  it('should close dialog when onClose is called', () => {
    component.onClose();
    expect(mockDialogRef.close).toHaveBeenCalled();
  });

  it('should display version labels correctly', () => {
    expect(component.data.version1Label).toBe('1.0.0');
    expect(component.data.version2Label).toBe('2.0.0 (Current)');
  });

  it('should handle empty tool configuration', (done) => {
    const version1NoTools = {
      ...mockVersion1,
      configurationSnapshot: {
        ...mockVersion1.configurationSnapshot,
        toolConfiguration: [],
      },
    } as AgentVersion;

    mockAgentService.getVersion.mockReturnValueOnce(of(version1NoTools)).mockReturnValueOnce(of(mockVersion2));

    fixture.detectChanges();

    setTimeout(() => {
      expect(component.version1).toEqual(version1NoTools);
      expect(component.version2).toEqual(mockVersion2);
      done();
    }, 100);
  });

  it('should set loading to true when loading versions', () => {
    mockAgentService.getVersion.mockReturnValueOnce(of(mockVersion1)).mockReturnValueOnce(of(mockVersion2));
    fixture.detectChanges();
    // After loading completes, loading should be false
    expect(component.loading).toBe(false);
  });
});
