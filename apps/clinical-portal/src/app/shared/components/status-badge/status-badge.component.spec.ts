import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatusBadgeComponent } from './status-badge.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('StatusBadgeComponent', () => {
  let component: StatusBadgeComponent;
  let fixture: ComponentFixture<StatusBadgeComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatusBadgeComponent, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(StatusBadgeComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display status text', () => {
    component.status = 'Active';
    fixture.detectChanges();

    const text = fixture.nativeElement.querySelector('.status-badge-text');
    expect(text.textContent).toContain('Active');
  });

  describe('Status types', () => {
    it('should apply success class for success type', () => {
      component.type = 'success';
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-badge');
      expect(badge.classList.contains('status-badge-success')).toBe(true);
    });

    it('should apply warning class for warning type', () => {
      component.type = 'warning';
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-badge');
      expect(badge.classList.contains('status-badge-warning')).toBe(true);
    });

    it('should apply error class for error type', () => {
      component.type = 'error';
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-badge');
      expect(badge.classList.contains('status-badge-error')).toBe(true);
    });

    it('should apply info class for info type', () => {
      component.type = 'info';
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-badge');
      expect(badge.classList.contains('status-badge-info')).toBe(true);
    });

    it('should apply neutral class for neutral type', () => {
      component.type = 'neutral';
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-badge');
      expect(badge.classList.contains('status-badge-neutral')).toBe(true);
    });
  });

  describe('Icons', () => {
    it('should display custom icon when provided', () => {
      component.icon = 'star';
      fixture.detectChanges();

      const icon = fixture.nativeElement.querySelector('.status-badge-icon');
      expect(icon).toBeTruthy();
      expect(icon.textContent).toContain('star');
    });

    it('should display default icon for success type', () => {
      component.type = 'success';
      fixture.detectChanges();

      expect(component.getDefaultIcon()).toBe('check_circle');
    });

    it('should display default icon for warning type', () => {
      component.type = 'warning';
      fixture.detectChanges();

      expect(component.getDefaultIcon()).toBe('warning');
    });

    it('should display default icon for error type', () => {
      component.type = 'error';
      fixture.detectChanges();

      expect(component.getDefaultIcon()).toBe('error');
    });

    it('should display default icon for info type', () => {
      component.type = 'info';
      fixture.detectChanges();

      expect(component.getDefaultIcon()).toBe('info');
    });

    it('should not display default icon for neutral type', () => {
      component.type = 'neutral';
      fixture.detectChanges();

      expect(component.getDefaultIcon()).toBeUndefined();
      expect(component.shouldShowIcon()).toBe(false);
    });

    it('should prefer custom icon over default', () => {
      component.type = 'success';
      component.icon = 'custom_icon';

      expect(component.getDefaultIcon()).toBe('custom_icon');
    });

    it('should not display icon when shouldShowIcon is false', () => {
      component.type = 'neutral';
      component.icon = undefined;
      fixture.detectChanges();

      const icon = fixture.nativeElement.querySelector('.status-badge-icon');
      expect(icon).toBeFalsy();
    });
  });

  describe('Compact mode', () => {
    it('should apply compact class when compact is true', () => {
      component.compact = true;
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-badge');
      expect(badge.classList.contains('status-badge-compact')).toBe(true);
    });

    it('should not apply compact class when compact is false', () => {
      component.compact = false;
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-badge');
      expect(badge.classList.contains('status-badge-compact')).toBe(false);
    });
  });

  describe('Tooltip', () => {
    it('should display tooltip when provided', () => {
      component.tooltip = 'This is a tooltip';
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-badge');
      expect(badge.getAttribute('mattooltip')).toBe('This is a tooltip');
    });

    it('should not display tooltip when not provided', () => {
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-badge');
      expect(badge.getAttribute('mattooltip')).toBeFalsy();
    });
  });

  describe('Accessibility', () => {
    it('should have role status', () => {
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-badge');
      expect(badge.getAttribute('role')).toBe('status');
    });

    it('should have aria-label with status text', () => {
      component.status = 'Active';
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-badge');
      expect(badge.getAttribute('aria-label')).toBe('Active');
    });

    it('should include tooltip in aria-label when provided', () => {
      component.status = 'Active';
      component.tooltip = 'Patient is active';
      fixture.detectChanges();

      const badge = fixture.nativeElement.querySelector('.status-badge');
      expect(badge.getAttribute('aria-label')).toBe('Active: Patient is active');
    });

    it('should have aria-hidden on icon', () => {
      component.icon = 'star';
      fixture.detectChanges();

      const icon = fixture.nativeElement.querySelector('.status-badge-icon');
      expect(icon.getAttribute('aria-hidden')).toBe('true');
    });
  });

  it('should apply custom class when provided', () => {
    component.customClass = 'custom-badge';
    fixture.detectChanges();

    const badge = fixture.nativeElement.querySelector('.status-badge');
    expect(badge.classList.contains('custom-badge')).toBe(true);
  });
});
