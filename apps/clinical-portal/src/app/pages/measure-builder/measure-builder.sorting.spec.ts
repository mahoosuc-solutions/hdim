import { of } from 'rxjs';
import { MatDialog } from '@angular/material/dialog';
import { Store } from '@ngrx/store';
import { MeasureBuilderComponent } from './measure-builder.component';
import { CustomMeasureService } from '../../services/custom-measure.service';
import { ToastService } from '../../services/toast.service';
import { DialogService } from '../../services/dialog.service';
import { AIAssistantService } from '../../services/ai-assistant.service';

describe('MeasureBuilderComponent sorting accessor', () => {
  let component: MeasureBuilderComponent;

  beforeEach(() => {
    const dialog = { open: jest.fn() } as unknown as MatDialog;
    const customMeasureService = {} as CustomMeasureService;
    const toast = {} as ToastService;
    const dialogService = {} as DialogService;
    const aiAssistant = {} as AIAssistantService;
    const store = {
      select: jest.fn().mockReturnValue(of(null)),
    } as unknown as Store;
    const router = { navigate: jest.fn() } as any;

    component = new MeasureBuilderComponent(
      dialog,
      customMeasureService,
      toast,
      dialogService,
      aiAssistant,
      store,
      router
    );

    component.paginator = {} as any;
    component.sort = {} as any;
    component.ngAfterViewInit();
  });

  it('sorts ownerCadence by owner then cadence text', () => {
    const first = {
      owner: 'Population Health',
      reportingCadence: 'MONTHLY',
    } as any;
    const second = {
      owner: 'Care Management',
      reportingCadence: 'QUARTERLY',
    } as any;

    const accessor = component.dataSource.sortingDataAccessor;
    const firstValue = accessor(first, 'ownerCadence');
    const secondValue = accessor(second, 'ownerCadence');

    expect(firstValue).toBe('population health monthly');
    expect(secondValue).toBe('care management quarterly');
    expect(String(firstValue) > String(secondValue)).toBe(true);
  });

  it('sorts priority by rank LOW < MEDIUM < HIGH', () => {
    const accessor = component.dataSource.sortingDataAccessor;

    const low = accessor({ priority: 'LOW' } as any, 'priority');
    const medium = accessor({ priority: 'MEDIUM' } as any, 'priority');
    const high = accessor({ priority: 'HIGH' } as any, 'priority');
    const unspecified = accessor({} as any, 'priority');

    expect(low).toBe(1);
    expect(medium).toBe(2);
    expect(high).toBe(3);
    expect(unspecified).toBe(0);
  });
});
