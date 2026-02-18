import { Injectable } from "@angular/core";
import { AuthService } from "./auth.service";
import { AuthStateService } from "./auth-state.service";

@Injectable({ providedIn: 'root' })
export class AuthInitService {

  constructor(private authService: AuthService,
    private authState: AuthStateService) { }

  init(): Promise<void> {
    return new Promise((resolve) => {
      // If access token already exists, skip
      if (this.authState.getAccessToken()) {
        resolve();
        return;
      }

      // Try silent refresh using HttpOnly cookie
      this.authService.refreshToken().subscribe({
        next: (res) => {
          this.authState.setTokens(res.data);
          resolve();
        },
        error: () => {
          // no refresh cookie or expired
          resolve();
        }
      });
    });
  }
}
