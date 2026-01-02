/**
 * Global Search Service
 *
 * Manages the global search dialog and keyboard shortcuts
 */

import { Injectable, HostListener } from '@angular/core';
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { GlobalSearchComponent } from '../components/global-search/global-search.component';

@Injectable({
  providedIn: 'root',
})
export class GlobalSearchService {
  private dialogRef: MatDialogRef<GlobalSearchComponent> | null = null;

  constructor(private dialog: MatDialog) {
    // Listen for Ctrl+K globally
    this.setupKeyboardShortcut();
  }

  /**
   * Setup global keyboard shortcut (Ctrl+K or Cmd+K)
   */
  private setupKeyboardShortcut(): void {
    document.addEventListener('keydown', (event: KeyboardEvent) => {
      // Check for Ctrl+K (Windows/Linux) or Cmd+K (Mac)
      if ((event.ctrlKey || event.metaKey) && event.key === 'k') {
        event.preventDefault();
        this.openSearch();
      }
    });
  }

  /**
   * Open global search dialog
   */
  openSearch(): void {
    // Don't open if already open
    if (this.dialogRef) {
      return;
    }

    this.dialogRef = this.dialog.open(GlobalSearchComponent, {
      width: '600px',
      maxWidth: '90vw',
      maxHeight: '70vh',
      panelClass: 'global-search-dialog-container',
      hasBackdrop: true,
      backdropClass: 'global-search-backdrop',
      disableClose: false,
      autoFocus: true,
    });

    this.dialogRef.afterClosed().subscribe(() => {
      this.dialogRef = null;
    });
  }

  /**
   * Close global search dialog
   */
  closeSearch(): void {
    if (this.dialogRef) {
      this.dialogRef.close();
      this.dialogRef = null;
    }
  }

  /**
   * Check if search dialog is open
   */
  isSearchOpen(): boolean {
    return this.dialogRef !== null;
  }
}
