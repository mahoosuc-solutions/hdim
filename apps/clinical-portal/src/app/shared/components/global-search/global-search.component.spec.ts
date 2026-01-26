import { ComponentFixture, TestBed, fakeAsync, tick } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { MatDialogRef } from '@angular/material/dialog';
import { Router } from '@angular/router';
import { GlobalSearchComponent, SearchResult } from './global-search.component';
import { LoggerService } from '../../../services/logger.service';
import { PatientService } from '../../../services/patient.service';
import { MeasureService } from '../../../services/measure.service';
import { createMockLoggerService } from '../../../testing/mocks';
import { createMockMatDialogRef } from '../../testing/mocks';
import { createMockRouter } from '../../testing/mocks';

describe('GlobalSearchComponent', () => {
  let fixture: ComponentFixture<GlobalSearchComponent>;
  let component: GlobalSearchComponent;
  let patientService: { searchPatients: jest.Mock };
  let measureService: { searchMeasures: jest.Mock };
  let dialogRef: { close: jest.Mock };
  let router: { navigate: jest.Mock };

  beforeEach(async () => {
    patientService = { searchPatients: jest.fn() };
    measureService = { searchMeasures: jest.fn() };
    dialogRef = { close: jest.fn() };
    router = { navigate: jest.fn() };

    await TestBed.configureTestingModule({
      imports: [GlobalSearchComponent],
      providers: [{ provide: LoggerService, useValue: createMockLoggerService() },
        { provide: PatientService, useValue: patientService },
        { provide: MeasureService, useValue: measureService },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: Router, useValue: router },
        { provide: MatDialogRef, useValue: createMockMatDialogRef() },
        { provide: Router, useValue: createMockRouter() }],
    }).compileComponents();

    fixture = TestBed.createComponent(GlobalSearchComponent);
    component = fixture.componentInstance;
    component.searchInput = { nativeElement: { focus: jest.fn() } } as any;
  });

  it('initializes quick pages and focuses input', fakeAsync(() => {
    component.ngOnInit();
    tick(100);

    expect(component.popularPages.length).toBe(4);
    expect(component.searchInput.nativeElement.focus).toHaveBeenCalled();
  }));

  it('handles search changes and clears results', () => {
    component.searchQuery = '';
    component.searchResults = [{ id: '1' } as SearchResult];

    component.onSearchChange();
    expect(component.searchResults).toEqual([]);
  });

  it('emits search queries when input is not empty', () => {
    const nextSpy = jest.spyOn((component as any).searchSubject, 'next');
    component.searchQuery = 'patient';

    component.onSearchChange();

    expect(nextSpy).toHaveBeenCalledWith('patient');
  });

  it('performs search and builds results', fakeAsync(() => {
    patientService.searchPatients.mockReturnValue(of([
      { id: 'p1', name: [{ family: 'Doe', given: ['John'] }], identifier: [{ value: 'MRN1', type: { coding: [{ code: 'MR' }] } }] },
    ]));
    measureService.searchMeasures.mockReturnValue(of([
      { id: 'm1', name: 'Measure 1', description: 'Desc' },
    ]));

    (component as any).performSearch('Doe');
    tick();

    expect(component.searchResults.length).toBeGreaterThan(0);
    expect(component.loading).toBe(false);
  }));

  it('derives patient names and MRNs', () => {
    expect((component as any).getPatientName({})).toBe('Unknown Patient');

    const name = (component as any).getPatientName({
      name: [{ family: 'Doe', given: ['Jane'] }],
    });
    expect(name).toBe('Doe, Jane');

    const mrn = (component as any).getPatientMRN({
      identifier: [{ value: '123', type: { coding: [{ code: 'MR' }] } }],
    });
    expect(mrn).toBe('123');
  });

  it('navigates results and saves recents', () => {
    const result: SearchResult = {
      id: 'reports',
      type: 'page',
      title: 'Reports',
      icon: 'description',
      route: ['/reports'],
    };

    component.navigateToResult(result);
    expect(router.navigate).toHaveBeenCalledWith(result.route, { queryParams: undefined });
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('executes action results without navigation', () => {
    const action = jest.fn();
    const result: SearchResult = {
      id: 'action-1',
      type: 'action',
      title: 'Run action',
      icon: 'bolt',
      action,
    };

    component.navigateToResult(result);

    expect(action).toHaveBeenCalled();
    expect(router.navigate).not.toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('maps result types to labels and colors', () => {
    expect(component.getResultIconColor('patient')).toBe('#1976d2');
    expect(component.getResultTypeLabel('measure')).toBe('Measure');
  });

  it('falls back to default labels and colors', () => {
    expect(component.getResultIconColor('other')).toBe('#757575');
    expect(component.getResultTypeLabel('other')).toBe('');
  });

  it('returns visible results based on search state', () => {
    component.popularPages = [{ id: '1' } as SearchResult];
    component.searchResults = [];

    expect(component.getVisibleResults()).toEqual(component.popularPages);

    component.searchResults = [{ id: '2' } as SearchResult];
    expect(component.getVisibleResults()).toEqual(component.searchResults);
  });

  it('navigates selection boundaries', () => {
    component.popularPages = [{ id: '1' } as SearchResult];

    (component as any).navigateResults(1);
    expect(component.selectedIndex).toBe(0);

    (component as any).navigateResults(-1);
    expect(component.selectedIndex).toBe(0);
  });

  it('clears search and refocuses input', () => {
    component.searchQuery = 'test';
    component.searchResults = [{ id: '1' } as SearchResult];

    component.clearSearch();

    expect(component.searchQuery).toBe('');
    expect(component.searchResults).toEqual([]);
    expect(component.searchInput.nativeElement.focus).toHaveBeenCalled();
  });

  it('loads and saves recent searches', () => {
    const stored = JSON.stringify([{ id: 'recent-1', title: 'Recent' }]);
    const storageProto = Object.getPrototypeOf(window.localStorage) as Storage;
    const getItemSpy = jest.spyOn(storageProto, 'getItem').mockReturnValue(stored);
    const setItemSpy = jest.spyOn(storageProto, 'setItem').mockImplementation(() => undefined);

    (component as any).loadRecentSearches();
    expect(component.recentSearches).toHaveLength(1);

    const result: SearchResult = {
      id: 'recent-2',
      type: 'page',
      title: 'Reports',
      icon: 'description',
      route: ['/reports'],
    };
    (component as any).saveRecentSearch(result);

    expect(component.recentSearches[0].id).toBe('recent-2');
    expect(setItemSpy).toHaveBeenCalled();

    getItemSpy.mockRestore();
    setItemSpy.mockRestore();
  });

  it('handles keyboard shortcuts', () => {
    const navigateSpy = jest.spyOn(component, 'navigateToResult').mockImplementation(() => undefined);
    component.searchResults = [
      { id: '1', type: 'page', title: 'Dashboard', icon: 'dashboard', route: ['/dashboard'] },
    ];

    component.handleKeyboard({ key: 'ArrowDown', preventDefault: jest.fn() } as any);
    component.handleKeyboard({ key: 'ArrowUp', preventDefault: jest.fn() } as any);
    component.handleKeyboard({ key: 'Enter', preventDefault: jest.fn() } as any);
    component.handleKeyboard({ key: 'Escape' } as any);

    expect(navigateSpy).toHaveBeenCalled();
    expect(dialogRef.close).toHaveBeenCalled();
  });
});
