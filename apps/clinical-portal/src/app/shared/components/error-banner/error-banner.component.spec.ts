import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { ErrorBannerComponent } from './error-banner.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('ErrorBannerComponent', () => {
  let component: ErrorBannerComponent;
  let fixture: ComponentFixture<ErrorBannerComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorBannerComponent, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(ErrorBannerComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display message', () => {
    component.message = 'Test error message';
    fixture.detectChanges();

    const message = fixture.nativeElement.querySelector('.error-banner-message');
    expect(message.textContent).toContain('Test error message');
  });

  it('should be visible by default', () => {
    fixture.detectChanges();

    const banner = fixture.nativeElement.querySelector('.error-banner');
    expect(banner).toBeTruthy();
    expect(component.visible).toBe(true);
  });

  describe('Banner types', () => {
    it('should display error icon for error type', () => {
      component.type = 'error';
      fixture.detectChanges();

      expect(component.getIcon()).toBe('error');
      const banner = fixture.nativeElement.querySelector('.error-banner');
      expect(banner.classList.contains('error-banner-error')).toBe(true);
    });

    it('should display warning icon for warning type', () => {
      component.type = 'warning';
      fixture.detectChanges();

      expect(component.getIcon()).toBe('warning');
      const banner = fixture.nativeElement.querySelector('.error-banner');
      expect(banner.classList.contains('error-banner-warning')).toBe(true);
    });

    it('should display info icon for info type', () => {
      component.type = 'info';
      fixture.detectChanges();

      expect(component.getIcon()).toBe('info');
      const banner = fixture.nativeElement.querySelector('.error-banner');
      expect(banner.classList.contains('error-banner-info')).toBe(true);
    });

    it('should display success icon for success type', () => {
      component.type = 'success';
      fixture.detectChanges();

      expect(component.getIcon()).toBe('check_circle');
      const banner = fixture.nativeElement.querySelector('.error-banner');
      expect(banner.classList.contains('error-banner-success')).toBe(true);
    });
  });

  describe('Dismiss functionality', () => {
    it('should show close button when dismissible is true', () => {
      component.dismissible = true;
      fixture.detectChanges();

      const closeButton = fixture.nativeElement.querySelector('.error-banner-close');
      expect(closeButton).toBeTruthy();
    });

    it('should not show close button when dismissible is false', () => {
      component.dismissible = false;
      fixture.detectChanges();

      const closeButton = fixture.nativeElement.querySelector('.error-banner-close');
      expect(closeButton).toBeFalsy();
    });

    it('should emit dismiss event when close button is clicked', () => {
      component.dismissible = true;
      fixture.detectChanges();

      jest.spyOn(component.dismiss, 'emit');
      component.onDismiss();

      expect(component.dismiss.emit).toHaveBeenCalledWith();
      expect(component.visible).toBe(false);
    });

    it('should hide banner when dismissed', () => {
      fixture.detectChanges();

      component.onDismiss();
      fixture.detectChanges();

      const banner = fixture.nativeElement.querySelector('.error-banner');
      expect(banner).toBeFalsy();
    });
  });

  describe('Retry functionality', () => {
    it('should show retry button when retryButton is true', () => {
      component.retryButton = true;
      fixture.detectChanges();

      const retryButton = fixture.nativeElement.querySelector('.error-banner-retry');
      expect(retryButton).toBeTruthy();
    });

    it('should not show retry button when retryButton is false', () => {
      component.retryButton = false;
      fixture.detectChanges();

      const retryButton = fixture.nativeElement.querySelector('.error-banner-retry');
      expect(retryButton).toBeFalsy();
    });

    it('should emit retry event when retry button is clicked', () => {
      component.retryButton = true;
      fixture.detectChanges();

      jest.spyOn(component.retry, 'emit');
      component.onRetry();

      expect(component.retry.emit).toHaveBeenCalledWith();
    });
  });

  describe('Auto-dismiss', () => {
    it('should auto-dismiss after timeout', fakeAsync(() => {
      component.autoDismissTimeout = 3000;
      jest.spyOn(component.dismiss, 'emit');

      component.ngOnInit();
      expect(component.visible).toBe(true);

      tick(3000);

      expect(component.visible).toBe(false);
      expect(component.dismiss.emit).toHaveBeenCalledWith();
    }));

    it('should not auto-dismiss when timeout is 0', fakeAsync(() => {
      component.autoDismissTimeout = 0;
      jest.spyOn(component.dismiss, 'emit');

      component.ngOnInit();
      tick(5000);

      expect(component.visible).toBe(true);
      expect(component.dismiss.emit).not.toHaveBeenCalled();
    }));

    it('should clear timeout on destroy', fakeAsync(() => {
      component.autoDismissTimeout = 3000;
      component.ngOnInit();

      jest.spyOn(window, 'clearTimeout');
      component.ngOnDestroy();

      expect(window.clearTimeout).toHaveBeenCalled();
    }));
  });

  describe('Accessibility', () => {
    it('should have alert role for error type', () => {
      component.type = 'error';
      fixture.detectChanges();

      const banner = fixture.nativeElement.querySelector('.error-banner');
      expect(banner.getAttribute('role')).toBe('alert');
    });

    it('should have status role for non-error types', () => {
      component.type = 'info';
      fixture.detectChanges();

      const banner = fixture.nativeElement.querySelector('.error-banner');
      expect(banner.getAttribute('role')).toBe('status');
    });

    it('should have assertive aria-live for error type', () => {
      component.type = 'error';
      fixture.detectChanges();

      const banner = fixture.nativeElement.querySelector('.error-banner');
      expect(banner.getAttribute('aria-live')).toBe('assertive');
    });

    it('should have polite aria-live for non-error types', () => {
      component.type = 'info';
      fixture.detectChanges();

      const banner = fixture.nativeElement.querySelector('.error-banner');
      expect(banner.getAttribute('aria-live')).toBe('polite');
    });

    it('should have aria-hidden on icon', () => {
      fixture.detectChanges();

      const icon = fixture.nativeElement.querySelector('.error-banner-icon');
      expect(icon.getAttribute('aria-hidden')).toBe('true');
    });
  });
});
