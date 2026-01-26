import { ComponentFixture, TestBed } from '@angular/core/testing';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MONACO_PATH } from '@materia-ui/ngx-monaco-editor';
import { CqlEditorDialogComponent, CqlEditorDialogData } from './cql-editor-dialog.component';
import { createMockMatDialogRef } from '../../testing/mocks';

describe('CqlEditorDialogComponent', () => {
  let fixture: ComponentFixture<CqlEditorDialogComponent>;
  let component: CqlEditorDialogComponent;
  let dialogRef: { close: jest.Mock };
  let data: CqlEditorDialogData;

  beforeEach(async () => {
    dialogRef = { close: jest.fn() };
    data = {
      measureId: 'm1',
      measureName: 'My Measure',
      cqlText: '',
      readOnly: false,
    };

    await TestBed.configureTestingModule({
      imports: [CqlEditorDialogComponent],
      providers: [{ provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MONACO_PATH, useValue: 'assets/monaco-editor/min/vs' },
        { provide: MatDialogRef, useValue: createMockMatDialogRef() }],
    }).compileComponents();

    fixture = TestBed.createComponent(CqlEditorDialogComponent);
    component = fixture.componentInstance;
  });

  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('initializes with default template when no CQL provided', () => {
    expect(component.cqlText).toContain('library MyMeasure version');
    expect(component.originalCqlText).toBe(component.cqlText);
  });

  it('sets read-only editor options when data is read-only', () => {
    component.data.readOnly = true;

    component.ngOnInit();

    expect(component.editorOptions.readOnly).toBe(true);
  });

  it('updates editor options based on toggle state', () => {
    component.showLineNumbers = false;
    component.wordWrap = true;
    component.minimap = false;

    component.updateEditorOptions();

    expect(component.editorOptions.lineNumbers).toBe('off');
    expect(component.editorOptions.wordWrap).toBe('on');
    expect(component.editorOptions.minimap).toEqual({ enabled: false });
  });

  it('toggles full screen styles on the dialog container', () => {
    const container = document.createElement('div');
    container.className = 'mat-mdc-dialog-container';
    document.body.appendChild(container);

    component.toggleFullScreen();

    expect(component.isFullScreen).toBe(true);
    expect(container.style.width).toBe('100vw');

    component.toggleFullScreen();

    expect(component.isFullScreen).toBe(false);
    expect(container.style.width).toBe('90vw');

    document.body.removeChild(container);
  });

  it('detects changes between current and original CQL text', () => {
    component.cqlText = 'new content';
    component.originalCqlText = 'old content';

    expect(component.hasChanges()).toBe(true);
  });

  it('saves when there are changes and not read-only', () => {
    component.cqlText = 'updated';
    component.originalCqlText = 'original';

    component.save();

    expect(dialogRef.close).toHaveBeenCalledWith('updated');
  });

  it('does not save when read-only or unchanged', () => {
    component.cqlText = 'same';
    component.originalCqlText = 'same';

    component.save();

    expect(dialogRef.close).not.toHaveBeenCalled();

    component.data.readOnly = true;
    component.cqlText = 'changed';
    component.originalCqlText = 'original';

    component.save();

    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('confirms before canceling with unsaved changes', () => {
    const confirmSpy = jest.spyOn(window, 'confirm').mockReturnValue(false);
    component.cqlText = 'changed';
    component.originalCqlText = 'original';

    component.cancel();

    expect(confirmSpy).toHaveBeenCalled();
    expect(dialogRef.close).not.toHaveBeenCalled();

    confirmSpy.mockReturnValue(true);

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('closes immediately when canceling without changes', () => {
    component.cqlText = 'same';
    component.originalCqlText = 'same';

    component.cancel();

    expect(dialogRef.close).toHaveBeenCalled();
  });
});
