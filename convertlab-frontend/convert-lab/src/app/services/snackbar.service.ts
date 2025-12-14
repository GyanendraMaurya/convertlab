import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

export type SnackbarType = 'success' | 'error' | 'info';

@Injectable({
  providedIn: 'root',
})
export class SnackbarService {
  private snackBar = inject(MatSnackBar);

  /**
   * Show success message (green)
   */
  success(message: string, duration: number = 5000) {
    this.snackBar.open(message, 'Close', {
      duration,
      verticalPosition: 'top',
      panelClass: ['snackbar-success'],
    });
  }

  /**
   * Show error message (red)
   */
  error(message: string, duration: number = 5000) {
    this.snackBar.open(message, 'Close', {
      duration,
      verticalPosition: 'top',
      panelClass: ['snackbar-error'],
    });
  }

  /**
   * Show info message (default/blue)
   */
  info(message: string, duration: number = 5000) {
    this.snackBar.open(message, 'Close', {
      duration,
      verticalPosition: 'top',
      panelClass: ['snackbar-info'],
    });
  }

  /**
   * Generic method
   */
  show(message: string, type: SnackbarType = 'info', duration: number = 5000) {
    const panelClass = `snackbar-${type}`;
    this.snackBar.open(message, 'Close', {
      duration,
      verticalPosition: 'top',
      panelClass: [panelClass],
    });
  }
}
