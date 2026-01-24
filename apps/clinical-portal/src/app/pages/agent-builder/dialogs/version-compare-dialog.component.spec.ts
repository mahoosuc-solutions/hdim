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
  let mockAgentService: jasmine.SpyObj<AgentBuilderService>;
  let mockToastService: jasmine.SpyObj<ToastService>;
  let mockLoggerService: jasmine.SpyObj<LoggerService>;
  let mockDialogRef: jasmine.SpyObj<MatDialogRef<VersionCompareDialogComponent>>;

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
    mockAgentService = jasmine.createSpyObj('AgentBuilderService', ['getVersion']);
    mockToastService = jasmine.createSpyObj('ToastService', ['error']);
    mockLoggerService = jasmine.createSpyObj('LoggerService', ['withContext']);
    mockDialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);

    const mockLogger = jasmine.createSpyObj('Logger', ['info', 'error']);
    mockLoggerService.withContext.and.returnValue(mockLogger as any);

    await TestBed.configureTestingModule({
      imports: [VersionCompareDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: AgentBuilderService, useValue: mockAgentService },
        { provide: ToastService, useValue: mockToastService },
        { provide: LoggerService, useValue: mockLoggerService },
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: MAT_DIALOG_DATA, useValue: dialogData },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(VersionCompareDialogComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load both versions in parallel on init', () => {
    mockAgentService.getVersion.and.returnValues(
      of(mockVersion1),
      of(mockVersion2)
    );

    fixture.detectChanges();

    expect(mockAgentService.getVersion).toHaveBeenCalledTimes(2);
    expect(mockAgentService.getVersion).toHaveBeenCalledWith('agent-123', 'v1');
    expect(mockAgentService.getVersion).toHaveBeenCalledWith('agent-123', 'v2');
  });

  it('should compute diffs after loading versions', (done) => {
    mockAgentService.getVersion.and.returnValues(
      of(mockVersion1),
      of(mockVersion2)
    );

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
    mockAgentService.getVersion.and.returnValues(
      of(mockVersion1),
      of(mockVersion2)
    );

    fixture.detectChanges();

    setTimeout(() => {
      // Description, modelId, temperature, maxTokens, systemPrompt changed
      // Plus tool configuration and guardrail changes
      expect(component.changesCount).toBeGreaterThan(0);
      done();
    }, 100);
  });

  it('should handle version loading error', () => {
    mockAgentService.getVersion.and.returnValue(
      throwError(() => new Error('API error'))
    );

    fixture.detectChanges();

    expect(mockToastService.error).toHaveBeenCalledWith('Failed to load version details');
    expect(component.loading).toBe(false);
  });

  it('should identify changed fields correctly', (done) => {
    mockAgentService.getVersion.and.returnValues(
      of(mockVersion1),
      of(mockVersion2)
    );

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
    mockAgentService.getVersion.and.returnValues(
      of(mockVersion1),
      of(mockVersion2)
    );

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
    mockAgentService.getVersion.and.returnValues(
      of(mockVersion1),
      of(mockVersion2)
    );

    fixture.detectChanges();

    setTimeout(() => {
      // v2 has 2 tools enabled, v1 has 1 tool enabled
      expect(component.toolConfigDiffs).toBeDefined();
      done();
    }, 100);
  });

  it('should handle guardrail configuration changes', (done) => {
    mockAgentService.getVersion.and.returnValues(
      of(mockVersion1),
      of(mockVersion2)
    );

    fixture.detectChanges();

    setTimeout(() => {
      const disclaimerDiff = component.guardrailDiffs.find(
        (d) => d.field === 'clinicalDisclaimerRequired'
      );
      expect(disclaimerDiff?.changed).toBe(true);
      expect(disclaimerDiff?.oldValue).toBe('false');
      expect(disclaimerDiff?.newValue).toBe('true');
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

    mockAgentService.getVersion.and.returnValues(
      of(version1NoTools),
      of(mockVersion2)
    );

    fixture.detectChanges();

    setTimeout(() => {
      expect(component.toolConfigDiffs).toBeDefined();
      done();
    }, 100);
  });

  it('should show loading spinner while fetching versions', () => {
    mockAgentService.getVersion.and.returnValue(of(mockVersion1).pipe());

    component.loading = true;
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    const spinner = compiled.querySelector('mat-spinner');
    expect(spinner).toBeTruthy();
  });
});
