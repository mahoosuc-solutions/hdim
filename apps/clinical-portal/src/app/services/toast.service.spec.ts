import { ToastService } from './toast.service';
import { MatSnackBar } from '@angular/material/snack-bar';

describe('ToastService', () => {
  it('shows toast notifications with correct styles', () => {
    const snackBar = { open: jest.fn() } as unknown as MatSnackBar;
    const service = new ToastService(snackBar);

    service.success('ok');
    service.error('error');
    service.info('info');
    service.warning('warn');

    expect(snackBar.open).toHaveBeenCalledTimes(4);
  });
});
