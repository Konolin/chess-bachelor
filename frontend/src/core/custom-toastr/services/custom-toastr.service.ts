import { inject, Injectable } from '@angular/core';
import { ToastrService } from 'ngx-toastr';

/**
 * A service to provide customizable toast notifications throughout the application.
 * This service wraps the ngx-toastr library for displaying informational and error messages.
 */
@Injectable({
  providedIn: 'root',
})
export class CustomToastrService {
  private readonly toastrService = inject(ToastrService);

  /**
   * Displays an informational toast notification with a specified message and title.
   *
   * @param message - The message to display in the toast.
   * @param title - The title of the toast notification.
   *
   * @example
   * ```typescript
   * customToastrService.info('Data saved successfully', 'Success');
   * ```
   */
  info(message: string, title: string) {
    this.toastrService.info(message, title, { timeOut: 3000 });
  }

  /**
   * Displays an error toast notification with a specified message and title.
   * The error toast has no timeout, requiring manual dismissal by the user.
   *
   * @param message - The message to display in the toast.
   * @param title - The title of the toast notification.
   *
   * @example
   * ```typescript
   * customToastrService.error('Failed to save data', 'Error');
   * ```
   */
  error(message: string, title: string) {
    this.toastrService.error(message, title, { disableTimeOut: true });
  }
}
