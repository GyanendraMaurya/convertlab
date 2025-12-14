import {
  HttpInterceptorFn,
  HttpRequest,
  HttpHandlerFn,
  HttpErrorResponse,
  HttpEvent
} from '@angular/common/http';
import { catchError, throwError, from, Observable } from 'rxjs';
import { inject } from '@angular/core';
import { IS_BLOB_REQUEST, SUPPRESS_ERROR } from './http-context';
import { SnackbarService } from '../services/snackbar.service';

export const blobErrorInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
): Observable<HttpEvent<unknown>> => {

  const snackbarService = inject(SnackbarService);

  // ONLY process blob requests
  const isBlobRequest = req.context.get(IS_BLOB_REQUEST);
  if (!isBlobRequest) {
    return next(req);
  }

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const suppress = req.context.get(SUPPRESS_ERROR);

      // Only attempt to parse Blob errors
      if (error.error instanceof Blob) {

        return from(error.error.text()).pipe(
          catchError(() => throwError(() => error)), // fallback if cannot read blob
          // Parse blob text → json
          (source$) =>
            new Observable(sub => {
              source$.subscribe({
                next: (text) => {
                  let parsed: any = null;
                  try {
                    parsed = JSON.parse(text);
                  } catch { }

                  const apiError = parsed?.error;
                  const message = apiError?.message || "Download failed.";
                  const code = apiError?.code;

                  if (!suppress) {
                    snackbarService.error(message);
                  }

                  sub.error({
                    status: error.status,
                    message,
                    code
                  });
                },
                error: (err) => sub.error(err)
              });
            })
        ) as unknown as Observable<HttpEvent<unknown>>; // Cast the error to Observable<HttpEvent<unknown>>;
      }

      return throwError(() => error);
    })
  );
};
