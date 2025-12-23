import { ComponentFixture, TestBed } from '@angular/core/testing';
import { BulkActionsToolbarComponent } from './bulk-actions-toolbar.component';

describe('BulkActionsToolbarComponent', () => {
  let fixture: ComponentFixture<BulkActionsToolbarComponent>;
  let component: BulkActionsToolbarComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BulkActionsToolbarComponent],
    }).compileComponents();

    fixture = TestBed.createComponent(BulkActionsToolbarComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('renders the selected count', () => {
    component.selectedCount = 3;
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('3 selected');
  });

  it('emits bulk action events', () => {
    const acceptSpy = jest.spyOn(component.accept, 'emit');
    const declineSpy = jest.spyOn(component.decline, 'emit');
    const completeSpy = jest.spyOn(component.complete, 'emit');
    const clearSpy = jest.spyOn(component.clearSelection, 'emit');

    component.accept.emit();
    component.decline.emit();
    component.complete.emit();
    component.clearSelection.emit();

    expect(acceptSpy).toHaveBeenCalled();
    expect(declineSpy).toHaveBeenCalled();
    expect(completeSpy).toHaveBeenCalled();
    expect(clearSpy).toHaveBeenCalled();
  });
});
