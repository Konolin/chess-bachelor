import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { HandleExceptionsService } from '../services/handle-exceptions.service';
import { catchError } from 'rxjs';

export const ServerErrorsInterceptor: HttpInterceptorFn = (req, next) => {
  const handleExceptionService = inject(HandleExceptionsService);

  return next(req).pipe(
    catchError((err) => {
      handleExceptionService.handleError(err);
      throw err;
    })
  );
};
