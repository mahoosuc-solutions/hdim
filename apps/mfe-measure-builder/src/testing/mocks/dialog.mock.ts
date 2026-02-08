/**
 * Angular Material Dialog Mock Implementation for Testing
 *
 * Provides mock implementations for MatDialogRef and MatDialog used in component tests.
 */

import { of } from 'rxjs';

/**
 * Creates a mock MatDialogRef instance
 * Used by dialog components that need to close or update the dialog
 */
export function createMockMatDialogRef(resultData: any = null) {
  return {
    close: jest.fn(),
    afterClosed: jest.fn(() => of(resultData)),
    updateSize: jest.fn(),
    updatePosition: jest.fn(),
    disableClose: false,
    keydownEvents: jest.fn(() => of({})),
    backdropClick: jest.fn(() => of({})),
    componentInstance: {},
    result: Promise.resolve(resultData),
  };
}

/**
 * Creates a mock MatDialog service
 * Used by components that open dialogs
 */
export function createMockMatDialog() {
  return {
    open: jest.fn(() => createMockMatDialogRef()),
    closeAll: jest.fn(),
    getDialogById: jest.fn(),
    afterAllClosed: jest.fn(() => of({})),
    afterOpened: jest.fn(() => of({})),
  };
}

export const MOCK_MAT_DIALOG_REF = createMockMatDialogRef();
export const MOCK_MAT_DIALOG = createMockMatDialog();
