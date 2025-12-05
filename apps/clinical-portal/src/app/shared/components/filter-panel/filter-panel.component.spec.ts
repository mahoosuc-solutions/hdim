import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FilterPanelComponent, FilterConfig } from './filter-panel.component';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('FilterPanelComponent', () => {
  let component: FilterPanelComponent;
  let fixture: ComponentFixture<FilterPanelComponent>;

  const mockFilters: FilterConfig[] = [
    {
      key: 'name',
      label: 'Patient Name',
      type: 'text',
      placeholder: 'Enter name'
    },
    {
      key: 'status',
      label: 'Status',
      type: 'select',
      options: [
        { label: 'Active', value: 'active' },
        { label: 'Inactive', value: 'inactive' }
      ]
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FilterPanelComponent, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(FilterPanelComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initialize with provided filters', () => {
    component.filters = mockFilters;
    component.ngOnInit();

    expect(component.filters.length).toBe(2);
  });

  it('should initialize filter values with defaults', () => {
    component.filters = [
      { ...mockFilters[0], defaultValue: 'test' }
    ];
    component.ngOnInit();

    expect(component.filterValues['name']).toBe('test');
  });

  it('should display panel title', () => {
    component.title = 'Custom Filters';
    fixture.detectChanges();

    const title = fixture.nativeElement.querySelector('mat-panel-title');
    expect(title.textContent).toContain('Custom Filters');
  });

  describe('Filter fields', () => {
    beforeEach(() => {
      component.filters = mockFilters;
      fixture.detectChanges();
    });

    it('should render text input for text type', () => {
      const textInput = fixture.nativeElement.querySelector('input[matInput]');
      expect(textInput).toBeTruthy();
    });

    it('should render select for select type', () => {
      const select = fixture.nativeElement.querySelector('mat-select');
      expect(select).toBeTruthy();
    });

    it('should update filterValues when input changes', () => {
      component.filterValues['name'] = 'John Doe';
      fixture.detectChanges();

      expect(component.filterValues['name']).toBe('John Doe');
    });
  });

  describe('Apply filters', () => {
    beforeEach(() => {
      component.filters = mockFilters;
      component.ngOnInit();
    });

    it('should emit filterChange event when applied', () => {
      jest.spyOn(component.filterChange, 'emit');
      component.filterValues = { name: 'John', status: 'active' };
      component.onApply();

      expect(component.filterChange.emit).toHaveBeenCalledWith({
        name: 'John',
        status: 'active'
      });
    });

    it('should only emit non-empty values', () => {
      jest.spyOn(component.filterChange, 'emit');
      component.filterValues = { name: 'John', status: '' };
      component.onApply();

      expect(component.filterChange.emit).toHaveBeenCalledWith({
        name: 'John'
      });
    });

    it('should update appliedFilters', () => {
      component.filterValues = { name: 'John' };
      component.onApply();

      expect(component.appliedFilters['name']).toBe('John');
    });
  });

  describe('Reset filters', () => {
    beforeEach(() => {
      component.filters = mockFilters;
      component.ngOnInit();
    });

    it('should clear all filter values', () => {
      component.filterValues = { name: 'John', status: 'active' };
      component.onReset();

      expect(component.filterValues['name']).toBeUndefined();
      expect(component.filterValues['status']).toBeUndefined();
    });

    it('should clear applied filters', () => {
      component.appliedFilters = { name: 'John' };
      component.onReset();

      expect(Object.keys(component.appliedFilters).length).toBe(0);
    });

    it('should emit empty filterChange', () => {
      jest.spyOn(component.filterChange, 'emit');
      component.onReset();

      expect(component.filterChange.emit).toHaveBeenCalledWith({});
    });

    it('should restore default values', () => {
      component.filters = [
        { ...mockFilters[0], defaultValue: 'default' }
      ];
      component.ngOnInit();
      component.filterValues['name'] = 'changed';
      component.onReset();

      expect(component.filterValues['name']).toBe('default');
    });
  });

  describe('Active filter chips', () => {
    beforeEach(() => {
      component.filters = mockFilters;
      component.ngOnInit();
    });

    it('should return empty array when no filters applied', () => {
      const chips = component.getActiveFilterChips();
      expect(chips.length).toBe(0);
    });

    it('should return chips for applied filters', () => {
      component.appliedFilters = { name: 'John', status: 'active' };
      const chips = component.getActiveFilterChips();

      expect(chips.length).toBe(2);
      expect(chips[0].key).toBe('name');
      expect(chips[0].value).toBe('John');
    });

    it('should format select values using option labels', () => {
      component.appliedFilters = { status: 'active' };
      const chips = component.getActiveFilterChips();

      expect(chips[0].value).toBe('Active');
    });

    it('should display filter count in title', () => {
      component.appliedFilters = { name: 'John', status: 'active' };
      fixture.detectChanges();

      const count = fixture.nativeElement.querySelector('.filter-count');
      expect(count).toBeTruthy();
      expect(count.textContent).toContain('2');
    });
  });

  describe('Remove chip', () => {
    beforeEach(() => {
      component.filters = mockFilters;
      component.ngOnInit();
    });

    it('should remove filter when chip is removed', () => {
      component.filterValues = { name: 'John', status: 'active' };
      component.appliedFilters = { name: 'John', status: 'active' };

      component.onRemoveChip('name');

      expect(component.filterValues['name']).toBeUndefined();
      expect(component.appliedFilters['name']).toBeUndefined();
    });

    it('should emit filterChange when chip is removed', () => {
      jest.spyOn(component.filterChange, 'emit');
      component.appliedFilters = { name: 'John', status: 'active' };

      component.onRemoveChip('name');

      expect(component.filterChange.emit).toHaveBeenCalled();
    });
  });

  describe('Accessibility', () => {
    beforeEach(() => {
      component.filters = mockFilters;
      fixture.detectChanges();
    });

    it('should have aria-label on inputs', () => {
      const input = fixture.nativeElement.querySelector('input[matInput]');
      expect(input.getAttribute('aria-label')).toBeTruthy();
    });

    it('should have aria-label on apply button', () => {
      const applyButton = fixture.nativeElement.querySelector('button[color="primary"]');
      expect(applyButton.getAttribute('aria-label')).toBe('Apply filters');
    });

    it('should have aria-label on reset button', () => {
      const resetButton = fixture.nativeElement.querySelector('button[mat-stroked-button]');
      expect(resetButton.getAttribute('aria-label')).toBe('Reset all filters');
    });
  });
});
