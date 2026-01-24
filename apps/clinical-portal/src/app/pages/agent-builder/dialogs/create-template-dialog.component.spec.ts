import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { CreateTemplateDialogComponent } from './create-template-dialog.component';
import { AgentBuilderService } from '../services/agent-builder.service';
import { ToastService } from '../../../services/toast.service';
import { LoggerService } from '../../../services/logger.service';
import { PromptTemplate, PromptVariable } from '../models/agent.model';

describe('CreateTemplateDialogComponent', () => {
  let component: CreateTemplateDialogComponent;
  let fixture: ComponentFixture<CreateTemplateDialogComponent>;
  let mockAgentService: jasmine.SpyObj<AgentBuilderService>;
  let mockToastService: jasmine.SpyObj<ToastService>;
  let mockLoggerService: jasmine.SpyObj<LoggerService>;
  let mockDialogRef: jasmine.SpyObj<MatDialogRef<CreateTemplateDialogComponent>>;

  const mockCreatedTemplate: PromptTemplate = {
    id: 'new-template-id',
    tenantId: 'tenant1',
    name: 'Test Template',
    description: 'Test description',
    category: 'CUSTOM',
    content: 'Hello {{patient_name}}',
    variables: [{ name: 'patient_name', description: '', required: true }],
    usageCount: 0,
    isSystem: false,
    createdBy: 'user@example.com',
    createdAt: '2026-01-24T12:00:00Z',
  };

  beforeEach(async () => {
    mockAgentService = jasmine.createSpyObj('AgentBuilderService', ['createTemplate']);
    mockToastService = jasmine.createSpyObj('ToastService', ['success', 'error']);
    mockLoggerService = jasmine.createSpyObj('LoggerService', ['withContext']);
    mockDialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);

    const mockLogger = jasmine.createSpyObj('Logger', ['info', 'error']);
    mockLoggerService.withContext.and.returnValue(mockLogger as any);

    await TestBed.configureTestingModule({
      imports: [CreateTemplateDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: AgentBuilderService, useValue: mockAgentService },
        { provide: ToastService, useValue: mockToastService },
        { provide: LoggerService, useValue: mockLoggerService },
        { provide: MatDialogRef, useValue: mockDialogRef },
        { provide: MAT_DIALOG_DATA, useValue: { isEdit: false } },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateTemplateDialogComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize form with default values', () => {
    expect(component.templateForm).toBeDefined();
    expect(component.templateForm.get('name')?.value).toBe('');
    expect(component.templateForm.get('category')?.value).toBe('CUSTOM');
  });

  it('should mark form as invalid when name is empty', () => {
    const nameControl = component.templateForm.get('name');
    nameControl?.setValue('');
    nameControl?.markAsTouched();

    expect(component.templateForm.valid).toBe(false);
    expect(nameControl?.hasError('required')).toBe(true);
  });

  it('should mark form as invalid when content is empty', () => {
    const contentControl = component.templateForm.get('content');
    contentControl?.setValue('');
    contentControl?.markAsTouched();

    expect(component.templateForm.valid).toBe(false);
    expect(contentControl?.hasError('required')).toBe(true);
  });

  it('should detect variables from content', () => {
    const variables: PromptVariable[] = [
      { name: 'patient_name', startIndex: 6, endIndex: 22 },
      { name: 'user_role', startIndex: 30, endIndex: 43 },
    ];

    component.onVariablesDetected(variables);

    expect(component.detectedVariables.length).toBe(2);
    expect(component.detectedVariables[0].name).toBe('patient_name');
    expect(component.detectedVariables[1].name).toBe('user_role');
  });

  it('should create template successfully', () => {
    mockAgentService.createTemplate.and.returnValue(of(mockCreatedTemplate));

    component.templateForm.patchValue({
      name: 'Test Template',
      description: 'Test description',
      category: 'CUSTOM',
      content: 'Hello {{patient_name}}',
    });

    component.detectedVariables = [
      { name: 'patient_name', startIndex: 6, endIndex: 22 },
    ];

    component.onSave();

    expect(mockAgentService.createTemplate).toHaveBeenCalledWith(
      jasmine.objectContaining({
        name: 'Test Template',
        description: 'Test description',
        category: 'CUSTOM',
        content: 'Hello {{patient_name}}',
        variables: [{ name: 'patient_name', description: '', required: true }],
      })
    );

    expect(mockToastService.success).toHaveBeenCalledWith(
      'Template created successfully'
    );
    expect(mockDialogRef.close).toHaveBeenCalledWith(mockCreatedTemplate);
  });

  it('should handle create template error', () => {
    mockAgentService.createTemplate.and.returnValue(
      throwError(() => new Error('API error'))
    );

    component.templateForm.patchValue({
      name: 'Test Template',
      content: 'Test content',
    });

    component.onSave();

    expect(mockToastService.error).toHaveBeenCalledWith('Failed to create template');
    expect(component.saving).toBe(false);
  });

  it('should not save when form is invalid', () => {
    component.templateForm.patchValue({
      name: '',
      content: '',
    });

    component.onSave();

    expect(mockAgentService.createTemplate).not.toHaveBeenCalled();
  });

  it('should close dialog on cancel', () => {
    component.onCancel();

    expect(mockDialogRef.close).toHaveBeenCalled();
  });

  it('should load existing template in edit mode', () => {
    const existingTemplate: PromptTemplate = {
      id: 'existing-id',
      tenantId: 'tenant1',
      name: 'Existing Template',
      description: 'Existing description',
      category: 'CLINICAL_SAFETY',
      content: 'Existing content with {{variable}}',
      variables: [{ name: 'variable', description: 'Test var', required: true }],
      usageCount: 5,
      isSystem: false,
      createdBy: 'user@example.com',
      createdAt: '2026-01-20T10:00:00Z',
    };

    const editFixture = TestBed.createComponent(CreateTemplateDialogComponent);
    const editComponent = editFixture.componentInstance;

    // Override data for edit mode
    (editComponent as any).data = {
      isEdit: true,
      template: existingTemplate,
    };

    editComponent.ngOnInit();

    expect(editComponent.templateForm.get('name')?.value).toBe('Existing Template');
    expect(editComponent.templateForm.get('description')?.value).toBe(
      'Existing description'
    );
    expect(editComponent.templateForm.get('category')?.value).toBe('CLINICAL_SAFETY');
    expect(editComponent.templateForm.get('content')?.value).toBe(
      'Existing content with {{variable}}'
    );
    expect(editComponent.detectedVariables.length).toBe(1);
    expect(editComponent.detectedVariables[0].name).toBe('variable');
  });
});
