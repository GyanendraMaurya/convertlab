import { inject, Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { ApiResponse, HttpService } from './http.service';
import { Observable } from 'rxjs';
import { AuthTokens } from './auth-state.service';
import { HttpContext } from '@angular/common/http';
import { SUPPRESS_ERROR } from '../interceptors/http-context';

export interface SignupRequest {
  email: string;
  password: string;
}

export interface VerifyOtpRequest {
  email: string;
  otp: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly apiUrl = environment.apiUrl;
  private readonly httpService = inject(HttpService);

  signup(request: SignupRequest): Observable<ApiResponse<string>> {
    return this.httpService.post<ApiResponse<string>>(
      `${this.apiUrl}/auth/signup`,
      request
    );
  }

  verifyOtp(request: VerifyOtpRequest): Observable<ApiResponse<string>> {
    return this.httpService.post<ApiResponse<string>>(
      `${this.apiUrl}/auth/verify-otp`,
      request
    );
  }

  login(request: LoginRequest): Observable<ApiResponse<AuthTokens>> {
    return this.httpService.post<ApiResponse<AuthTokens>>(
      `${this.apiUrl}/auth/login`,
      request
    );
  }

  refreshToken(): Observable<ApiResponse<AuthTokens>> {
    return this.httpService.post<ApiResponse<AuthTokens>>(
      `${this.apiUrl}/auth/refresh`,
      {},
      { context: new HttpContext().set(SUPPRESS_ERROR, true) }
    );
  }

  logout(): Observable<ApiResponse<string>> {
    return this.httpService.post<ApiResponse<string>>(
      `${this.apiUrl}/auth/logout`,
      {}
    );
  }
}
