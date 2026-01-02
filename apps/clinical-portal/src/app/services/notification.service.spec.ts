import { NotificationService } from './notification.service';
import { MatSnackBar } from '@angular/material/snack-bar';

describe('NotificationService', () => {
  it('shows notifications and dismisses', () => {
    const snackBar = { open: jest.fn(), dismiss: jest.fn() } as unknown as MatSnackBar;
    const service = new NotificationService(snackBar);

    service.success('ok');
    service.error('error');
    service.warning('warn');
    service.info('info');
    service.show('custom', 'custom-class', 1000, 'Action');
    service.dismiss();

    expect(snackBar.open).toHaveBeenCalledTimes(5);
    expect(snackBar.dismiss).toHaveBeenCalled();
  });
});
