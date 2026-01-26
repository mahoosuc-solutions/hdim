import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { TemplateLibraryDialogComponent } from './template-library-dialog.component';
import { AgentBuilderService } from '../services/agent-builder.service';
import { ToastService } from '../../../services/toast.service';
import { LoggerService } from '../../../services/logger.service';
import { PromptTemplate } from '../models/agent.model';
import { createMockMatDialogRef } from '../../testing/mocks';

describe('TemplateLibraryDialogComponent', () => {
  let component: TemplateLibraryDialogComponent;
  let fixture: ComponentFixture<TemplateLibraryDialogComponent>;
  let mockAgentService: jasmine.SpyObj<AgentBuilderService>;
  let mockToastService: jasmine.SpyObj<ToastService>;
  let mockLoggerService: jasmine.SpyObj<LoggerService>;
  let mockDialogRef: jasmine.SpyObj<MatDialogRef<TemplateLibraryDialogComponent>>;

  const mockTemplates: PromptTemplate[] = [
    {
      id: '1',
      tenantId: 'tenant1',
      name: 'Clinical Safety Template',
      description: 'Standard clinical safety disclaimers',
      category: 'CLINICAL_SAFETY',
      content: 'Always recommend consulting a healthcare professional...',
      variables: [{ name: 'patient_name', description: 'Patient name', required: true }],
      usageCount: 10,
      isSystem: true,
      createdBy: 'system',
      createdAt: '2026-01-20T10:00:00Z',
    },
    {
      id: '2',
      tenantId: 'tenant1',
      name: 'Care Gap Analysis',
      description: 'Template for care gap identification',
      category: 'CAPABILITIES',
      content: 'Identify preventive care opportunities...',
      variables: [],
      usageCount: 5,
      isSystem: false,
      createdBy: 'user@example.com',
      createdAt: '2026-01-22T14:30:00Z',
    },
  ];

  beforeEach(async () => {
    mockAgentService = jasmine.createSpyObj('AgentBuilderService', ['listTemplates']);
    mockToastService = jasmine.createSpyObj('ToastService', ['success', 'error', 'info']);
    mockLoggerService = jasmine.createSpyObj('LoggerService', ['withContext']);
    mockDialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);

    const mockLogger = jasmine.createSpyObj('Logger', ['info', 'error']);
    mockLoggerService.withContext.and.returnValue(mockLogger as any);

    await TestBed.configureTestingModule({
      imports: [TemplateLibraryDialogComponent, NoopAnimationsModule],
      providers: [{ provide: AgentBuilderService, useValue: mockAgentService },
        { provide: ToastService, useValue: mockToastService },
        { provide: LoggerService, useValue: mockLoggerService },
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { mode: 'select' } },
        { provide: MatDialogRef, useValue: createMockMatDialogRef() }],
    }).compileComponents();

    mockAgentService.listTemplates.and.returnValue(of(mockTemplates));

    fixture = TestBed.createComponent(TemplateLibraryDialogComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should load templates on init', () => {
    fixture.detectChanges();

    expect(mockAgentService.listTemplates).toHaveBeenCalled();
    expect(component.templates.length).toBe(2);
    expect(component.dataSource.data.length).toBe(2);
  });

  it('should handle template loading error', () => {
    mockAgentService.listTemplates.and.returnValue(
      throwError(() => new Error('API error'))
    );

    fixture.detectChanges();

    expect(component.loading).toBe(false);
    expect(mockToastService.error).toHaveBeenCalledWith('Failed to load templates');
  });

  it('should filter templates by category', () => {
    fixture.detectChanges();

    component.selectedCategory = 'CLINICAL_SAFETY';
    component.onCategoryChange();

    expect(component.dataSource.data.length).toBe(1);
    expect(component.dataSource.data[0].category).toBe('CLINICAL_SAFETY');
  });

  it('should filter templates by search text', (done) => {
    fixture.detectChanges();

    component.searchControl.setValue('care gap');

    // Wait for debounce
    setTimeout(() => {
      expect(component.dataSource.data.length).toBe(1);
      expect(component.dataSource.data[0].name).toBe('Care Gap Analysis');
      done();
    }, 350);
  });

  it('should select a template', () => {
    fixture.detectChanges();

    const template = mockTemplates[0];
    component.selectTemplate(template);

    expect(component.selectedTemplate).toBe(template);
  });

  it('should close dialog with selected template when using', () => {
    fixture.detectChanges();

    component.selectedTemplate = mockTemplates[0];
    component.useTemplate();

    expect(mockDialogRef.close).toHaveBeenCalledWith(mockTemplates[0]);
  });

  it('should format category labels correctly', () => {
    expect(component.formatCategory('CLINICAL_SAFETY')).toBe('Clinical Safety');
    expect(component.formatCategory('SYSTEM_PROMPT')).toBe('System Prompt');
    expect(component.formatCategory('CUSTOM')).toBe('Custom');
  });

  it('should show empty state when no templates', () => {
    mockAgentService.listTemplates.and.returnValue(of([]));
    fixture.detectChanges();

    expect(component.dataSource.data.length).toBe(0);
  });

  it('should apply both category and search filters', (done) => {
    fixture.detectChanges();

    component.selectedCategory = 'CLINICAL_SAFETY';
    component.searchControl.setValue('clinical');

    setTimeout(() => {
      expect(component.dataSource.data.length).toBe(1);
      expect(component.dataSource.data[0].category).toBe('CLINICAL_SAFETY');
      done();
    }, 350);
  });
});
