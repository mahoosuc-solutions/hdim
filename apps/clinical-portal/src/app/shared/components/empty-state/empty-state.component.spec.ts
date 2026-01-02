import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EmptyStateComponent } from './empty-state.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('EmptyStateComponent', () => {
  let component: EmptyStateComponent;
  let fixture: ComponentFixture<EmptyStateComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EmptyStateComponent, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(EmptyStateComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display default icon', () => {
    fixture.detectChanges();

    const icon = fixture.nativeElement.querySelector('.empty-state-icon');
    expect(icon).toBeTruthy();
    expect(icon.textContent).toContain('inbox');
  });

  it('should display custom icon', () => {
    component.icon = 'people_outline';
    fixture.detectChanges();

    const icon = fixture.nativeElement.querySelector('.empty-state-icon');
    expect(icon.textContent).toContain('people_outline');
  });

  it('should display title when provided', () => {
    component.title = 'No Patients Found';
    fixture.detectChanges();

    const title = fixture.nativeElement.querySelector('.empty-state-title');
    expect(title).toBeTruthy();
    expect(title.textContent).toContain('No Patients Found');
  });

  it('should display message when provided', () => {
    component.message = 'There are no patients matching your criteria.';
    fixture.detectChanges();

    const message = fixture.nativeElement.querySelector('.empty-state-message');
    expect(message).toBeTruthy();
    expect(message.textContent).toContain('There are no patients matching your criteria.');
  });

  it('should not display title when not provided', () => {
    fixture.detectChanges();

    const title = fixture.nativeElement.querySelector('.empty-state-title');
    expect(title).toBeFalsy();
  });

  it('should not display message when not provided', () => {
    fixture.detectChanges();

    const message = fixture.nativeElement.querySelector('.empty-state-message');
    expect(message).toBeFalsy();
  });

  describe('Action button', () => {
    it('should display action button when actionText is provided', () => {
      component.actionText = 'Add Patient';
      fixture.detectChanges();

      const actionButton = fixture.nativeElement.querySelector('app-loading-button');
      expect(actionButton).toBeTruthy();
    });

    it('should not display action button when actionText is not provided', () => {
      fixture.detectChanges();

      const actionButton = fixture.nativeElement.querySelector('app-loading-button');
      expect(actionButton).toBeFalsy();
    });

    it('should emit action event when button is clicked', () => {
      component.actionText = 'Add Patient';
      fixture.detectChanges();

      jest.spyOn(component.action, 'emit');
      component.onActionClick();

      expect(component.action.emit).toHaveBeenCalledWith();
    });

    it('should pass loading state to button', () => {
      component.actionText = 'Add Patient';
      component.actionLoading = true;
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector('app-loading-button');
      expect(button).toBeTruthy();
    });

    it('should pass icon to button when provided', () => {
      component.actionText = 'Add Patient';
      component.actionIcon = 'add';
      fixture.detectChanges();

      const button = fixture.nativeElement.querySelector('app-loading-button');
      expect(button).toBeTruthy();
    });
  });

  describe('Accessibility', () => {
    it('should have role status', () => {
      fixture.detectChanges();

      const container = fixture.nativeElement.querySelector('.empty-state-container');
      expect(container.getAttribute('role')).toBe('status');
    });

    it('should have aria-live polite', () => {
      fixture.detectChanges();

      const container = fixture.nativeElement.querySelector('.empty-state-container');
      expect(container.getAttribute('aria-live')).toBe('polite');
    });

    it('should have aria-hidden on icon', () => {
      fixture.detectChanges();

      const icon = fixture.nativeElement.querySelector('.empty-state-icon');
      expect(icon.getAttribute('aria-hidden')).toBe('true');
    });
  });

  it('should apply custom class when provided', () => {
    component.customClass = 'custom-empty-state';
    fixture.detectChanges();

    const container = fixture.nativeElement.querySelector('.empty-state-container');
    expect(container.classList.contains('custom-empty-state')).toBe(true);
  });
});
