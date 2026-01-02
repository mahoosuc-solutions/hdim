import {
  Component,
  Input,
  Output,
  EventEmitter,
  OnInit,
  OnDestroy,
  forwardRef,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
  FormsModule,
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatMenuModule } from '@angular/material/menu';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MonacoEditorModule } from 'ngx-monaco-editor-v2';
import { Subject } from 'rxjs';

/**
 * Template variable detected in the prompt
 */
export interface PromptVariable {
  name: string;
  startIndex: number;
  endIndex: number;
}

/**
 * Prompt editor component with Monaco integration for AI agent system prompts.
 * Features template variable highlighting, snippets, and validation.
 */
@Component({
  selector: 'app-prompt-editor',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatTooltipModule,
    MatMenuModule,
    MatChipsModule,
    MatDividerModule,
    MonacoEditorModule,
  ],
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => PromptEditorComponent),
      multi: true,
    },
  ],
  template: `
    <div class="prompt-editor" [class.focused]="isFocused">
      <!-- Toolbar -->
      <div class="editor-toolbar">
        <div class="toolbar-left">
          <span class="editor-label">
            <mat-icon>{{ icon }}</mat-icon>
            {{ label }}
          </span>
          @if (required) {
            <span class="required-badge">Required</span>
          }
        </div>

        <div class="toolbar-actions">
          <!-- Variable Insert Menu -->
          <button
            mat-icon-button
            [matMenuTriggerFor]="variableMenu"
            matTooltip="Insert variable"
            aria-label="Insert template variable"
            [disabled]="disabled">
            <mat-icon>data_object</mat-icon>
          </button>
          <mat-menu #variableMenu="matMenu">
            <div class="menu-header">Template Variables</div>
            @for (variable of availableVariables; track variable.name) {
              <button mat-menu-item (click)="insertVariable(variable.name)">
                <code>{{ '{{' + variable.name + '}}' }}</code>
                <span class="variable-desc">{{ variable.description }}</span>
              </button>
            }
          </mat-menu>

          <!-- Snippet Insert Menu -->
          <button
            mat-icon-button
            [matMenuTriggerFor]="snippetMenu"
            matTooltip="Insert snippet"
            aria-label="Insert prompt snippet"
            [disabled]="disabled">
            <mat-icon>library_add</mat-icon>
          </button>
          <mat-menu #snippetMenu="matMenu">
            <div class="menu-header">Prompt Snippets</div>
            @for (snippet of promptSnippets; track snippet.name) {
              <button mat-menu-item (click)="insertSnippet(snippet.content)">
                <mat-icon>{{ snippet.icon }}</mat-icon>
                <span>{{ snippet.name }}</span>
              </button>
            }
          </mat-menu>

          <mat-divider vertical></mat-divider>

          <!-- Format/Clear -->
          <button
            mat-icon-button
            matTooltip="Clear content"
            aria-label="Clear all content"
            [disabled]="disabled || !value"
            (click)="clearContent()">
            <mat-icon>clear_all</mat-icon>
          </button>

          <!-- Fullscreen -->
          <button
            mat-icon-button
            [matTooltip]="isFullScreen ? 'Exit fullscreen' : 'Fullscreen'"
            [attr.aria-label]="isFullScreen ? 'Exit fullscreen mode' : 'Enter fullscreen mode'"
            (click)="toggleFullScreen()">
            <mat-icon>{{ isFullScreen ? 'fullscreen_exit' : 'fullscreen' }}</mat-icon>
          </button>
        </div>
      </div>

      <!-- Monaco Editor -->
      <div class="editor-container" [style.height]="editorHeight">
        <ngx-monaco-editor
          [(ngModel)]="value"
          [options]="editorOptions"
          (ngModelChange)="onValueChange($event)"
          (onInit)="onEditorInit($event)"
          class="monaco-editor">
        </ngx-monaco-editor>
      </div>

      <!-- Footer with stats -->
      <div class="editor-footer">
        <div class="footer-left">
          @if (detectedVariables.length > 0) {
            <div class="variables-preview">
              <mat-icon>data_object</mat-icon>
              <span>Variables:</span>
              @for (v of detectedVariables; track v.name) {
                <mat-chip size="small">{{ v.name }}</mat-chip>
              }
            </div>
          }
        </div>
        <div class="footer-right">
          <span class="char-count" [class.warning]="isOverLimit">
            {{ characterCount }} / {{ maxLength }} characters
          </span>
        </div>
      </div>

      <!-- Hint text -->
      @if (hint) {
        <div class="editor-hint">
          <mat-icon>info</mat-icon>
          {{ hint }}
        </div>
      }
    </div>
  `,
  styles: [`
    .prompt-editor {
      border: 1px solid var(--mat-sys-outline-variant);
      border-radius: 8px;
      overflow: hidden;
      transition: border-color 0.2s;

      &.focused {
        border-color: var(--mat-sys-primary);
      }
    }

    .editor-toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 12px;
      background: var(--mat-sys-surface-variant);
      border-bottom: 1px solid var(--mat-sys-outline-variant);
    }

    .toolbar-left {
      display: flex;
      align-items: center;
      gap: 8px;
    }

    .editor-label {
      display: flex;
      align-items: center;
      gap: 6px;
      font-weight: 500;
      color: var(--mat-sys-on-surface);

      mat-icon {
        font-size: 18px;
        width: 18px;
        height: 18px;
        color: var(--mat-sys-primary);
      }
    }

    .required-badge {
      font-size: 0.75rem;
      padding: 2px 6px;
      background: var(--mat-sys-error-container);
      color: var(--mat-sys-on-error-container);
      border-radius: 4px;
    }

    .toolbar-actions {
      display: flex;
      align-items: center;
      gap: 4px;

      mat-divider {
        height: 24px;
        margin: 0 8px;
      }
    }

    .menu-header {
      padding: 8px 16px;
      font-size: 0.75rem;
      font-weight: 600;
      color: var(--mat-sys-on-surface-variant);
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    .variable-desc {
      margin-left: 8px;
      font-size: 0.8125rem;
      color: var(--mat-sys-on-surface-variant);
    }

    .editor-container {
      position: relative;
      min-height: 200px;

      .monaco-editor {
        height: 100%;
        width: 100%;
      }
    }

    .editor-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 8px 12px;
      background: var(--mat-sys-surface-variant);
      border-top: 1px solid var(--mat-sys-outline-variant);
    }

    .footer-left {
      display: flex;
      align-items: center;
    }

    .variables-preview {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 0.8125rem;
      color: var(--mat-sys-on-surface-variant);

      mat-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
      }

      mat-chip {
        font-family: monospace;
      }
    }

    .footer-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .char-count {
      font-size: 0.75rem;
      color: var(--mat-sys-on-surface-variant);

      &.warning {
        color: var(--mat-sys-error);
        font-weight: 500;
      }
    }

    .editor-hint {
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 12px;
      font-size: 0.8125rem;
      color: var(--mat-sys-on-surface-variant);
      background: var(--mat-sys-surface);

      mat-icon {
        font-size: 16px;
        width: 16px;
        height: 16px;
      }
    }
  `],
})
export class PromptEditorComponent implements OnInit, OnDestroy, ControlValueAccessor {
  @Input() label = 'System Prompt';
  @Input() icon = 'psychology';
  @Input() hint = '';
  @Input() required = false;
  @Input() disabled = false;
  @Input() maxLength = 10000;
  @Input() height = '300px';

  @Output() variablesDetected = new EventEmitter<PromptVariable[]>();

  private destroy$ = new Subject<void>();
  private editor: any;

  value = '';
  isFocused = false;
  isFullScreen = false;
  detectedVariables: PromptVariable[] = [];

  editorOptions: any = {
    theme: 'vs',
    language: 'markdown',
    automaticLayout: true,
    fontSize: 14,
    lineNumbers: 'on',
    wordWrap: 'on',
    minimap: { enabled: false },
    scrollBeyondLastLine: false,
    renderWhitespace: 'none',
    tabSize: 2,
    insertSpaces: true,
    lineHeight: 22,
    padding: { top: 12, bottom: 12 },
    suggestOnTriggerCharacters: true,
    quickSuggestions: true,
  };

  // Available template variables for AI agents
  availableVariables = [
    { name: 'patient_name', description: 'Patient full name' },
    { name: 'patient_id', description: 'Patient identifier' },
    { name: 'patient_age', description: 'Patient age in years' },
    { name: 'patient_gender', description: 'Patient gender' },
    { name: 'current_date', description: 'Today\'s date' },
    { name: 'current_time', description: 'Current time' },
    { name: 'user_name', description: 'Current user name' },
    { name: 'user_role', description: 'Current user role' },
    { name: 'tenant_name', description: 'Organization name' },
    { name: 'context', description: 'Additional context' },
  ];

  // Pre-built prompt snippets for healthcare AI
  promptSnippets = [
    {
      name: 'Clinical Safety Disclaimer',
      icon: 'health_and_safety',
      content: `IMPORTANT CLINICAL SAFETY GUIDELINES:
- Always recommend consulting with a qualified healthcare professional
- Never provide definitive diagnoses
- Flag high-risk symptoms for immediate attention
- Include appropriate clinical disclaimers in responses`,
    },
    {
      name: 'HIPAA Compliance',
      icon: 'security',
      content: `HIPAA COMPLIANCE REQUIREMENTS:
- Do not store or log Protected Health Information (PHI)
- Redact any PHI from responses when not necessary
- Maintain patient privacy at all times
- Follow minimum necessary principle`,
    },
    {
      name: 'Evidence-Based Response',
      icon: 'science',
      content: `RESPONSE GUIDELINES:
- Base recommendations on clinical evidence and guidelines
- Cite relevant clinical sources when available
- Acknowledge limitations and uncertainty
- Provide balanced information about options`,
    },
    {
      name: 'Empathetic Communication',
      icon: 'favorite',
      content: `COMMUNICATION STYLE:
- Use empathetic and supportive language
- Acknowledge patient concerns and emotions
- Explain medical concepts in accessible terms
- Be patient and thorough in explanations`,
    },
    {
      name: 'Care Gap Analysis',
      icon: 'checklist',
      content: `CARE GAP ANALYSIS CAPABILITIES:
- Identify preventive care opportunities
- Prioritize based on clinical urgency and impact
- Suggest evidence-based interventions
- Consider patient preferences and barriers`,
    },
  ];

  // ControlValueAccessor implementation
  private onChange: (value: string) => void = () => {};
  private onTouched: () => void = () => {};

  get characterCount(): number {
    return this.value?.length || 0;
  }

  get isOverLimit(): boolean {
    return this.characterCount > this.maxLength;
  }

  get editorHeight(): string {
    return this.isFullScreen ? 'calc(100vh - 200px)' : this.height;
  }

  ngOnInit(): void {
    if (this.disabled) {
      this.editorOptions = { ...this.editorOptions, readOnly: true };
    }
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  writeValue(value: string): void {
    this.value = value || '';
    this.detectVariables();
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.disabled = isDisabled;
    this.editorOptions = { ...this.editorOptions, readOnly: isDisabled };
  }

  onEditorInit(editor: any): void {
    this.editor = editor;

    // Track focus
    editor.onDidFocusEditorText(() => {
      this.isFocused = true;
      this.onTouched();
    });

    editor.onDidBlurEditorText(() => {
      this.isFocused = false;
    });

    // Register custom language for prompt template syntax highlighting
    this.registerPromptLanguage();
  }

  onValueChange(value: string): void {
    this.value = value;
    this.onChange(value);
    this.detectVariables();
  }

  /**
   * Detect template variables in the prompt ({{variable_name}})
   */
  private detectVariables(): void {
    const regex = /\{\{(\w+)\}\}/g;
    const variables: PromptVariable[] = [];
    let match;

    while ((match = regex.exec(this.value)) !== null) {
      variables.push({
        name: match[1],
        startIndex: match.index,
        endIndex: match.index + match[0].length,
      });
    }

    // Deduplicate by name
    const uniqueVariables = variables.filter(
      (v, i, arr) => arr.findIndex((x) => x.name === v.name) === i
    );

    this.detectedVariables = uniqueVariables;
    this.variablesDetected.emit(uniqueVariables);
  }

  /**
   * Insert a template variable at cursor position
   */
  insertVariable(variableName: string): void {
    if (!this.editor) return;

    const selection = this.editor.getSelection();
    const text = `{{${variableName}}}`;

    this.editor.executeEdits('insert-variable', [
      {
        range: selection,
        text,
        forceMoveMarkers: true,
      },
    ]);

    this.editor.focus();
  }

  /**
   * Insert a prompt snippet at cursor position
   */
  insertSnippet(content: string): void {
    if (!this.editor) return;

    const selection = this.editor.getSelection();
    const prefix = this.value ? '\n\n' : '';

    this.editor.executeEdits('insert-snippet', [
      {
        range: selection,
        text: prefix + content,
        forceMoveMarkers: true,
      },
    ]);

    this.editor.focus();
  }

  /**
   * Clear editor content
   */
  clearContent(): void {
    if (confirm('Are you sure you want to clear all content?')) {
      this.value = '';
      this.onChange(this.value);
      this.detectVariables();
    }
  }

  /**
   * Toggle fullscreen mode
   */
  toggleFullScreen(): void {
    this.isFullScreen = !this.isFullScreen;
  }

  /**
   * Register custom prompt template language for Monaco
   */
  private registerPromptLanguage(): void {
    // This would register a custom language with highlighting for {{variables}}
    // For now, we use markdown which provides good readability
  }
}
