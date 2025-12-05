import { ComponentFixture, TestBed } from '@angular/core/testing';
import { StatCardComponent } from './stat-card.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('StatCardComponent', () => {
  let component: StatCardComponent;
  let fixture: ComponentFixture<StatCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [StatCardComponent, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(StatCardComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display title and value', () => {
    component.title = 'Total Patients';
    component.value = '1,234';
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('Total Patients');
    expect(compiled.textContent).toContain('1,234');
  });

  it('should display subtitle when provided', () => {
    component.title = 'Total Patients';
    component.value = '1,234';
    component.subtitle = '+12% from last month';
    fixture.detectChanges();

    const compiled = fixture.nativeElement;
    expect(compiled.textContent).toContain('+12% from last month');
  });

  it('should display icon when provided', () => {
    component.icon = 'people';
    fixture.detectChanges();

    const icon = fixture.nativeElement.querySelector('mat-icon.stat-icon');
    expect(icon).toBeTruthy();
    expect(icon.textContent).toContain('people');
  });

  it('should not display icon when not provided', () => {
    fixture.detectChanges();

    const icon = fixture.nativeElement.querySelector('mat-icon.stat-icon');
    expect(icon).toBeFalsy();
  });

  describe('Trend indicators', () => {
    it('should display trending up icon', () => {
      component.trend = 'up';
      fixture.detectChanges();

      expect(component.getTrendIcon()).toBe('trending_up');
      const trendIcon = fixture.nativeElement.querySelector('.trend-icon');
      expect(trendIcon).toBeTruthy();
      expect(trendIcon.classList.contains('trend-up')).toBe(true);
    });

    it('should display trending down icon', () => {
      component.trend = 'down';
      fixture.detectChanges();

      expect(component.getTrendIcon()).toBe('trending_down');
      const trendIcon = fixture.nativeElement.querySelector('.trend-icon');
      expect(trendIcon.classList.contains('trend-down')).toBe(true);
    });

    it('should display trending stable icon', () => {
      component.trend = 'stable';
      fixture.detectChanges();

      expect(component.getTrendIcon()).toBe('trending_flat');
      const trendIcon = fixture.nativeElement.querySelector('.trend-icon');
      expect(trendIcon.classList.contains('trend-stable')).toBe(true);
    });

    it('should not display trend icon when none', () => {
      component.trend = 'none';
      fixture.detectChanges();

      const trendIcon = fixture.nativeElement.querySelector('.trend-icon');
      expect(trendIcon).toBeFalsy();
    });
  });

  describe('Color variants', () => {
    it('should apply primary color class', () => {
      component.color = 'primary';
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('.stat-card');
      expect(card.classList.contains('stat-card-primary')).toBe(true);
    });

    it('should apply accent color class', () => {
      component.color = 'accent';
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('.stat-card');
      expect(card.classList.contains('stat-card-accent')).toBe(true);
    });

    it('should apply warn color class', () => {
      component.color = 'warn';
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('.stat-card');
      expect(card.classList.contains('stat-card-warn')).toBe(true);
    });

    it('should apply success color class', () => {
      component.color = 'success';
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('.stat-card');
      expect(card.classList.contains('stat-card-success')).toBe(true);
    });
  });

  describe('Accessibility', () => {
    it('should have proper aria-label', () => {
      component.title = 'Total Patients';
      component.value = '1,234';
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('.stat-card');
      expect(card.getAttribute('aria-label')).toBe('Total Patients: 1,234');
    });

    it('should have role region', () => {
      fixture.detectChanges();

      const card = fixture.nativeElement.querySelector('.stat-card');
      expect(card.getAttribute('role')).toBe('region');
    });

    it('should have aria-hidden on icon', () => {
      component.icon = 'people';
      fixture.detectChanges();

      const icon = fixture.nativeElement.querySelector('mat-icon.stat-icon');
      expect(icon.getAttribute('aria-hidden')).toBe('true');
    });
  });

  it('should apply clickable class when clickable is true', () => {
    component.clickable = true;
    fixture.detectChanges();

    const card = fixture.nativeElement.querySelector('.stat-card');
    expect(card.classList.contains('clickable')).toBe(true);
  });
});
