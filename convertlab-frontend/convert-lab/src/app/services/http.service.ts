import { Injectable } from '@angular/core';
import { HttpClient, HttpContext, HttpHeaders, HttpParams } from '@angular/common/http';

export interface RequestOptions {
  params?: Record<string, any>;
  headers?: Record<string, any>;
  responseType?: 'json' | 'blob';
  observe?: 'response' | 'body';
  context?: HttpContext
}

@Injectable({ providedIn: 'root' })
export class HttpService {

  constructor(private http: HttpClient) { }

  /** Helper: create an AbortController only when needed */
  createController(): AbortController {
    return new AbortController();
  }

  /** GET */
  get<T>(url: string, options?: RequestOptions, ctrl?: AbortController) {
    return this.http.get<T>(url, {
      // signal: ctrl?.signal,    // OPTIONAL
      params: new HttpParams({ fromObject: options?.params }),
      headers: new HttpHeaders(options?.headers),
    });
  }

  /** POST */
  post<T>(url: string, body: any, options?: RequestOptions, ctrl?: AbortController) {
    return this.http.post<T>(url, body, {
      // signal: ctrl?.signal,
      params: new HttpParams({ fromObject: options?.params }),
      headers: new HttpHeaders(options?.headers),
      responseType: options?.responseType,
      observe: options?.observe,
      context: options?.context
    } as object);
  }

  /** PUT */
  put<T>(url: string, body: any, options?: RequestOptions, ctrl?: AbortController) {
    return this.http.put<T>(url, body, {
      // signal: ctrl?.signal,
      params: new HttpParams({ fromObject: options?.params }),
      headers: new HttpHeaders(options?.headers)
    });
  }

  /** DELETE */
  delete<T>(url: string, options?: RequestOptions, ctrl?: AbortController) {
    return this.http.delete<T>(url, {
      // signal: ctrl?.signal,
      params: new HttpParams({ fromObject: options?.params }),
      headers: new HttpHeaders(options?.headers)
    });
  }
}
