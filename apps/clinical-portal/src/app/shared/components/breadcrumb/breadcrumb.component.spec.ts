import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Subject } from 'rxjs';
import { BreadcrumbComponent } from './breadcrumb.component';
import { BreadcrumbService, Breadcrumb } from '../../services/breadcrumb.service';

describe('BreadcrumbComponent', () => {
  let fixture: ComponentFixture<BreadcrumbComponent>;
  let component: BreadcrumbComponent;
  let breadcrumbs$: Subject<Breadcrumb[]>;
  let breadcrumbService: { breadcrumbs$: Subject<Breadcrumb[]> };

  beforeEach(async () => {
    breadcrumbs$ = new Subject<Breadcrumb[]>();
    breadcrumbService = { breadcrumbs$ };

    await TestBed.configureTestingModule({
      imports: [BreadcrumbComponent],
      providers: [{ provide: BreadcrumbService, useValue: breadcrumbService }],
    }).compileComponents();

    fixture = TestBed.createComponent(BreadcrumbComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('subscribes to breadcrumb updates', () => {
    const data: Breadcrumb[] = [
      { label: 'Dashboard', url: '/dashboard' },
      { label: 'Reports', url: '/reports' },
    ];

    breadcrumbs$.next(data);

    expect(component.breadcrumbs).toEqual(data);
  });

  it('returns explicit icon when provided', () => {
    expect(component.getIcon({ label: 'Any', url: '/', icon: 'star' })).toBe('star');
  });

  it('auto-detects icons from labels', () => {
    expect(component.getIcon({ label: 'Home', url: '/' })).toBe('dashboard');
    expect(component.getIcon({ label: 'Patient List', url: '/' })).toBe('person');
    expect(component.getIcon({ label: 'Evaluation Results', url: '/' })).toBe('assessment');
    expect(component.getIcon({ label: 'Reports', url: '/' })).toBe('description');
    expect(component.getIcon({ label: 'Measure Builder', url: '/' })).toBe('rule');
    expect(component.getIcon({ label: 'Knowledge Base', url: '/' })).toBe('menu_book');
    expect(component.getIcon({ label: 'AI Assistant', url: '/' })).toBe('smart_toy');
    expect(component.getIcon({ label: 'Other', url: '/' })).toBe('chevron_right');
  });

  it('identifies the last breadcrumb', () => {
    component.breadcrumbs = [
      { label: 'One', url: '/one' },
      { label: 'Two', url: '/two' },
    ];

    expect(component.isLast(0)).toBe(false);
    expect(component.isLast(1)).toBe(true);
  });

  it('tracks by url', () => {
    expect(component.trackByUrl(0, { label: 'One', url: '/one' })).toBe('/one');
  });
});
