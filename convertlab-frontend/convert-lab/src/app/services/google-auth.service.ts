import { Injectable, inject, signal, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from './http.service';
import { AuthTokens } from './auth-state.service';

declare const google: any;

@Injectable({ providedIn: 'root' })
export class GoogleAuthService {
  private readonly platformId = inject(PLATFORM_ID);
  private readonly http = inject(HttpClient);
  private readonly apiUrl = environment.apiUrl;

  isScriptLoaded = signal(false);

  /** Load the Google Identity Services script once */
  loadScript(): Promise<void> {
    if (!isPlatformBrowser(this.platformId)) return Promise.resolve();
    if (this.isScriptLoaded()) return Promise.resolve();
    if (typeof google !== 'undefined') {
      this.isScriptLoaded.set(true);
      return Promise.resolve();
    }

    return new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.defer = true;
      script.onload = () => {
        this.isScriptLoaded.set(true);
        resolve();
      };
      script.onerror = reject;
      document.head.appendChild(script);
    });
  }

  /**
   * Render a Google Sign-In button inside the given element.
   * @param elementId  id of the container div
   * @param onToken    callback receiving the credential (id_token) string
   */
  renderButton(elementId: string, onToken: (idToken: string) => void): void {
    if (!isPlatformBrowser(this.platformId)) return;

    const clientId = environment.googleClientId;
    if (!clientId) {
      console.warn('GOOGLE_CLIENT_ID not set in environment');
      return;
    }

    google.accounts.id.initialize({
      client_id: clientId,
      callback: (response: { credential: string }) => {
        onToken(response.credential);
      },
    });

    google.accounts.id.renderButton(document.getElementById(elementId), {
      theme: 'outline',
      size: 'large',
      type: 'standard',
      shape: 'rectangular',
      logo_alignment: 'left',
      width: '100%',
      text: 'continue_with',
    });
  }

  /** Send the id_token to your backend */
  loginWithGoogle(idToken: string): Observable<ApiResponse<AuthTokens>> {
    return this.http.post<ApiResponse<AuthTokens>>(
      `${this.apiUrl}/auth/google`,
      { idToken },
      { withCredentials: true }
    );
  }
}