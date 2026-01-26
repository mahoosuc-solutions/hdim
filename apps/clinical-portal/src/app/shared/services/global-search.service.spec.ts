import { Subject } from 'rxjs';
import { GlobalSearchService } from './global-search.service';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { createMockMatDialog } from '../../testing/mocks';
import { createMockMatDialogRef } from '../../testing/mocks';

const createDialogRef = () => ({
  afterClosedSubject: new Subject<void>(),
  afterClosed() {
    return (this as any).afterClosedSubject.asObservable();
  },
  close: jest.fn(),
}) as unknown as MatDialogRef<any>;

describe('GlobalSearchService', () => {
  it('opens search on Ctrl+K keyboard shortcut', () => {
    const dialogRef = createDialogRef();
    const dialog = { open: jest.fn(() => dialogRef) } as unknown as MatDialog;

    new GlobalSearchService(dialog);

    const event = new KeyboardEvent('keydown', { key: 'k', ctrlKey: true });
    const preventSpy = jest.spyOn(event, 'preventDefault');
    document.dispatchEvent(event);

    expect(dialog.open).toHaveBeenCalled();
    expect(preventSpy).toHaveBeenCalled();
  });

  it('opens search on Cmd+K and ignores other keys', () => {
    const dialogRef = createDialogRef();
    const dialog = { open: jest.fn(() => dialogRef) } as unknown as MatDialog;

    new GlobalSearchService(dialog);

    const cmdEvent = new KeyboardEvent('keydown', { key: 'k', metaKey: true });
    const cmdPrevent = jest.spyOn(cmdEvent, 'preventDefault');
    document.dispatchEvent(cmdEvent);

    const otherEvent = new KeyboardEvent('keydown', { key: 'x', ctrlKey: true });
    document.dispatchEvent(otherEvent);

    expect(dialog.open).toHaveBeenCalledTimes(1);
    expect(cmdPrevent).toHaveBeenCalled();
  });

  it('opens and closes the search dialog', () => {
    const dialogRef = createDialogRef();
    const dialog = { open: jest.fn(() => dialogRef) } as unknown as MatDialog;

    const service = new GlobalSearchService(dialog);

    service.openSearch();
    expect(service.isSearchOpen()).toBe(true);

    service.closeSearch();
    expect(service.isSearchOpen()).toBe(false);
  });

  it('does nothing when closing without an open dialog', () => {
    const dialog = { open: jest.fn() } as unknown as MatDialog;
    const service = new GlobalSearchService(dialog);

    service.closeSearch();

    expect(service.isSearchOpen()).toBe(false);
  });

  it('clears dialog reference after close', () => {
    const dialogRef = createDialogRef();
    const dialog = { open: jest.fn(() => dialogRef) } as unknown as MatDialog;

    const service = new GlobalSearchService(dialog);
    service.openSearch();
    expect(service.isSearchOpen()).toBe(true);

    (dialogRef as any).afterClosedSubject.next();
    expect(service.isSearchOpen()).toBe(false);
  });

  it('prevents opening twice', () => {
    const dialogRef = createDialogRef();
    const dialog = { open: jest.fn(() => dialogRef) } as unknown as MatDialog;

    const service = new GlobalSearchService(dialog);
    service.openSearch();
    service.openSearch();

    expect(dialog.open).toHaveBeenCalledTimes(1);
  });
});
