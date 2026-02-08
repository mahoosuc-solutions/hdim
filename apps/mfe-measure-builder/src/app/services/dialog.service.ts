import { Injectable } from '@angular/core';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { Observable, map } from 'rxjs';
import { ConfirmDialogComponent, ConfirmDialogData } from '../components/dialogs/confirm-dialog.component';

/**
 * Minimal Dialog Service for Measure Builder MFE
 * Provides confirm and confirmDelete flows without pulling full clinical portal dialogs.
 */
@Injectable({
  providedIn: 'root',
})
export class DialogService {
  private readonly defaultConfig: MatDialogConfig = {
    disableClose: false,
    autoFocus: true,
    restoreFocus: true,
    panelClass: 'custom-dialog-container',
  };

  constructor(private dialog: MatDialog) {}

  confirm(
    title: string,
    message: string,
    confirmText = 'Confirm',
    cancelText = 'Cancel',
    confirmColor: 'primary' | 'accent' | 'warn' = 'primary'
  ): Observable<boolean> {
    const data: ConfirmDialogData = {
      title,
      message,
      confirmText,
      cancelText,
      confirmColor,
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      ...this.defaultConfig,
      data,
      width: '450px',
      maxWidth: '90vw',
    });

    return dialogRef.afterClosed().pipe(map((result) => !!result));
  }

  confirmDelete(itemName: string, itemType = 'item'): Observable<boolean> {
    const data: ConfirmDialogData = {
      title: `Delete ${itemType}?`,
      message: `Are you sure you want to delete "<strong>${itemName}</strong>"?<br><br>This action cannot be undone.`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      confirmColor: 'warn',
      icon: 'warning',
      iconColor: '#f44336',
    };

    const dialogRef = this.dialog.open(ConfirmDialogComponent, {
      ...this.defaultConfig,
      data,
      width: '450px',
      maxWidth: '90vw',
    });

    return dialogRef.afterClosed().pipe(map((result) => !!result));
  }
}
