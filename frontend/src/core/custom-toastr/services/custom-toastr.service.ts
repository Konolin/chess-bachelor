import { inject, Injectable } from '@angular/core';
import { MessageService } from 'primeng/api';

/**
 * A service to provide customizable toast notifications throughout the application.
 * This service wraps the ngx-toastr library for displaying informational and error messages.
 */
@Injectable({
  providedIn: 'root',
})
export class CustomToastrService {
  private readonly toastr = inject(MessageService);

  /**
   * Displays an informational toast notification with a specified message and title.
   *
   * @param message - The message to display in the toast.
   *
   * @example
   * ```typescript
   * customToastrService.info('Data saved successfully');
   * ```
   */
  info(message: string): void {
    this.toastr.add({ summary: message, severity: 'info' });
  }

  /**
   * Displays an error toast notification with a specified message and title.
   * The error toast has no timeout, requiring manual dismissal by the user.
   *
   * @param message - The message to display in the toast.
   *
   * @example
   * ```typescript
   * customToastrService.error('Failed to save data');
   * ```
   */
  error(message: string): void {
    this.toastr.add({ summary: message, severity: 'error', closable: true, sticky: true });
  }
}
