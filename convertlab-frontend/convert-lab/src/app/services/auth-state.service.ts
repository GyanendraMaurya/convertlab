// convertlab-frontend/convert-lab/src/app/services/auth-state.service.ts
import { Injectable, signal, computed } from '@angular/core';

export interface AuthTokens {
  accessToken: string;
  accessTokenExpiresInSeconds: number;
  email: string;
  role?: 'USER' | 'SUPER_ADMIN';
}

@Injectable({
  providedIn: 'root'
})
export class AuthStateService {
  // Store only in memory (RAM) - signals act as reactive variables
  private accessToken = signal<string | null>(null);
  private tokenExpiry = signal<number | null>(null);
  private email = signal<string | null>(null);

  isAuthenticated = computed(() => {
    const token = this.accessToken();
    const expiry = this.tokenExpiry();

    if (!token || !expiry) return false;

    // Check if token is expired
    return Date.now() < expiry;
  });

  setTokens(tokens: AuthTokens) {
    const expiryTime = Date.now() + (tokens.accessTokenExpiresInSeconds * 1000);

    // Store only in RAM
    this.accessToken.set(tokens.accessToken);
    this.tokenExpiry.set(expiryTime);
    this.email.set(tokens.email);
  }

  getAccessToken(): string | null {
    return this.accessToken();
  }

  getEmail(): string | null {
    return this.email();
  }

  clearTokens() {
    this.accessToken.set(null);
    this.tokenExpiry.set(null);
    this.email.set(null);
  }

  isTokenExpiringSoon(): boolean {
    const expiry = this.tokenExpiry();
    if (!expiry) return false;

    // Consider token expiring if less than 30 seconds remaining
    return (expiry - Date.now()) < 30000;
  }
}
