// src/app/interceptors/error.interceptor.ts (Recommended file location)

import { HttpInterceptorFn, HttpRequest, HttpHandlerFn, HttpEvent, HttpErrorResponse } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { IS_BLOB_REQUEST, SUPPRESS_ERROR } from './http-context';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';

// Define the functional interceptor using HttpInterceptorFn
export const errorInterceptor: HttpInterceptorFn = (
  req,
  next
) => {

  const snackBar = inject(MatSnackBar);

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
        snackBar.open(message, "Close", { duration: 5000, verticalPosition: 'top' });
      }

      return throwError(() => ({
        status: error.status,
        message,
        code
      }));
    })
  );
};


