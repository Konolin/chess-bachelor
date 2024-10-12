import { inject, Injectable } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { CustomToastrService } from '../../custom-toastr/services/custom-toastr.service';
import { TranslocoService } from '@jsverse/transloco';

@Injectable({
  providedIn: 'root',
})
export class HandleExceptionsService {
  private readonly customToastrService = inject(CustomToastrService);
  private readonly translocoService = inject(TranslocoService);

  public handleError(err: HttpErrorResponse) {
    let exceptionMessage: string;
    // TODO - extend with specific error handling
    if (err.error instanceof ErrorEvent) {
      // a client-side or network error occurred. Handle it accordingly.
      exceptionMessage = `An error occurred: ${err.error.message}`;
    } else {
      // the backend returned an unsuccessful response code.
      exceptionMessage = this.translocoService.translate('exceptions.unknownException');
    }

    // TODO - temporary code
    console.error(exceptionMessage);

    this.customToastrService.error(
      exceptionMessage,
      this.translocoService.translate('exceptions.errorTitle')
    );
  }
}
