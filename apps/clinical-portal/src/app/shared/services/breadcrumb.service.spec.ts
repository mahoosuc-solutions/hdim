import { Subject } from 'rxjs';
import { NavigationEnd, ActivatedRoute, Params, Router } from '@angular/router';
import { BreadcrumbService } from './breadcrumb.service';

function createRoute(
  path: string,
  data: Record<string, any> = {},
  params: Params = {},
  queryParams: Params = {},
  children: ActivatedRoute[] = []
): ActivatedRoute {
  const urlSegments = path
    ? path.split('/').map((segment) => ({ path: segment }))
    : [];

  return {
    snapshot: {
      url: urlSegments,
      data,
      params,
      queryParams,
    },
    children,
  } as unknown as ActivatedRoute;
}

describe('BreadcrumbService', () => {
  let service: BreadcrumbService;
  let routerEvents$: Subject<any>;
  let router: Router;
  let activatedRoute: ActivatedRoute;

  beforeEach(() => {
    routerEvents$ = new Subject();
    router = { events: routerEvents$.asObservable() } as Router;

    const child = createRoute('patients', { breadcrumb: 'Patients' });
    activatedRoute = createRoute('', {}, {}, {}, [child]);
    (activatedRoute as any).root = activatedRoute;

    service = new BreadcrumbService(router, activatedRoute);
  });

  it('builds breadcrumbs on navigation end', (done) => {
    service.breadcrumbs$.subscribe((crumbs) => {
      if (crumbs.length > 0) {
        expect(crumbs[0]).toEqual({ label: 'Patients', url: '/patients', icon: undefined, queryParams: undefined });
        done();
      }
    });

    routerEvents$.next(new NavigationEnd(1, '/patients', '/patients'));
  });

  it('supports manual breadcrumb operations', () => {
    service.setBreadcrumbs([{ label: 'Home', url: '/' }]);
    service.addBreadcrumb({ label: 'Reports', url: '/reports' });
    expect(service.getCurrentBreadcrumbs().length).toBe(2);

    service.clearBreadcrumbs();
    expect(service.getCurrentBreadcrumbs()).toEqual([]);
  });

  it('resolves label fallback and ignores hidden breadcrumbs', () => {
    const hiddenRoute = createRoute('patient-detail', { breadcrumb: false });
    const root = createRoute('', {}, {}, {}, [hiddenRoute]);

    const crumbs = (service as any).buildBreadcrumbs(root);
    expect(crumbs.length).toBe(0);

    const label = (service as any).getLabelFromUrl('/knowledge-base');
    expect(label).toBe('Knowledge Base');
  });

  it('includes params and query params in breadcrumbs', () => {
    const detailRoute = createRoute(
      'patient/:id',
      { breadcrumb: 'Patient :id' },
      { id: '123' },
      { tab: 'overview' }
    );
    const root = createRoute('', {}, {}, {}, [detailRoute]);

    const crumbs = (service as any).buildBreadcrumbs(root);
    expect(crumbs[0].label).toBe('Patient 123');
    expect(crumbs[0].queryParams).toEqual({ tab: 'overview' });
  });

  it('skips empty path segments while building breadcrumbs', () => {
    const nested = createRoute('reports', { breadcrumb: 'Reports' });
    const emptyPath = createRoute('', {}, {}, {}, [nested]);
    const root = createRoute('', {}, {}, {}, [emptyPath]);

    const crumbs = (service as any).buildBreadcrumbs(root);
    expect(crumbs[0].label).toBe('Reports');
    expect(crumbs[0].url).toBe('/reports');
  });

  it('generates labels for custom routes', () => {
    const label = (service as any).getLabelFromUrl('/custom-page?tab=1');
    expect(label).toBe('Custom Page');
  });
});
