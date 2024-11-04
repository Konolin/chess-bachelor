import {
  HttpErrorResponse,
  HttpEvent,
  HttpHandler,
  HttpInterceptor,
  HttpRequest,
} from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, EMPTY, Observable } from 'rxjs';
import { CustomToastrService } from '../../custom-toastr/services/custom-toastr.service';
import { TranslocoService } from '@jsverse/transloco';

/**
 * Interceptor to handle server errors for HTTP requests.
 * This interceptor catches errors from HTTP responses and displays appropriate
 * toast notifications based on the error status and message.
 */
export class ServerErrorsInterceptor implements HttpInterceptor {
  private readonly toastr = inject(CustomToastrService);
  private readonly translocoService = inject(TranslocoService);

  /**
   * Intercepts HTTP requests and handles errors returned from the server.
   *
   * @param request - The HTTP request to handle.
   * @param next - The next handler in the chain that processes the request.
   * @returns An observable of the HTTP event or an empty observable in case of error.
   */
  intercept(request: HttpRequest<unknown>, next: HttpHandler): Observable<HttpEvent<unknown>> {
    return next.handle(request).pipe(
      catchError((error: HttpErrorResponse) => {
        // Define an error message based on the error status
        const errorMessage = this.getErrorMessage(error);

        // Display the error message using CustomToastrService
        this.toastr.error(
          this.translocoService.translate(errorMessage),
          this.translocoService.translate('exceptions.errorTitle')
        );

        // Return an empty observable since the error has been handled
        return EMPTY;
      })
    );
  }

  /**
   * Determines the appropriate error message based on the HttpErrorResponse.
   *
   * @param error - The HttpErrorResponse object received from the HTTP request.
   * @returns A string representing the translated error message key.
   */
  private getErrorMessage(error: HttpErrorResponse): string {
    if (error.status === 0) {
      return 'exceptions.noResponse'; // no response from the server
    } else if (error.error) {
      return `exceptions.${error.error}`; // server-side error with specific message
    } else {
      return 'exceptions.unknownError'; // fallback for unknown errors
    }
  }
}
