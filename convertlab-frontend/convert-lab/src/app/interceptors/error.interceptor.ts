
import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { IS_BLOB_REQUEST, SUPPRESS_ERROR } from './http-context';
import { inject } from '@angular/core';
import { SnackbarService } from '../services/snackbar.service';

// Define the functional interceptor using HttpInterceptorFn
export const errorInterceptor: HttpInterceptorFn = (
  req,
  next
) => {

  const snackbarService = inject(SnackbarService);

  // Skip JSON error handling for blob requests
  const isBlobRequest = req.context.get(IS_BLOB_REQUEST);
  if (isBlobRequest) {
    return next(req);
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {

      const suppress = req.context.get(SUPPRESS_ERROR);
      const apiError = error.error?.error;
      const message = apiError?.message || "Something went wrong.";
      const code = apiError?.code || "UNKNOWN";

      if (!suppress) {
        snackbarService.error(message)
      }

      return throwError(() => ({
        status: error.status,
        message,
        code
      }));
    })
  );
};


