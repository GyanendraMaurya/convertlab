import {
    AfterViewInit,
    ChangeDetectionStrategy,
    Component,
    inject,
    input,
    OnDestroy,
    output,
    PLATFORM_ID,
} from '@angular/core';
import { isPlatformBrowser } from '@angular/common';
import { GoogleAuthService } from '../../../services/google-auth.service';
import { AuthStateService } from '../../../services/auth-state.service';
import { SnackbarService } from '../../../services/snackbar.service';
import { Router } from '@angular/router';

@Component({
    selector: 'app-google-signin-button',
    template: `
    <div class="google-btn-wrap">
      <div class="divider">
        <span>or continue with</span>
      </div>
      <div id="google-signin-btn" class="google-btn-container"></div>
    </div>
  `,
    styles: [`
    .google-btn-wrap {
      display: flex;
      flex-direction: column;
      gap: 12px;
      width: 100%;
    }

    .divider {
      display: flex;
      align-items: center;
      color: var(--mat-sys-on-surface-variant);
      font-size: 13px;

      &::before, &::after {
        content: '';
        flex: 1;
        height: 1px;
        background: var(--mat-sys-outline-variant);
      }

      span { padding: 0 12px; }
    }

    .google-btn-container {
      display: flex;
      justify-content: center;
      min-height: 44px;
    }
  `],
    changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GoogleSigninButtonComponent implements AfterViewInit, OnDestroy {
    /** Where to navigate after success (default: '/') */
    redirectTo = input<string>('/');

    /** Emits the id_token if you want the parent to handle the API call */
    tokenReceived = output<string>();

    /** When true the component handles the full login flow internally */
    handleLogin = input<boolean>(true);

    private readonly googleAuth = inject(GoogleAuthService);
    private readonly authState = inject(AuthStateService);
    private readonly snackbar = inject(SnackbarService);
    private readonly router = inject(Router);
    private readonly platformId = inject(PLATFORM_ID);

    async ngAfterViewInit() {
        if (!isPlatformBrowser(this.platformId)) return;
        await this.googleAuth.loadScript();
        this.googleAuth.renderButton('google-signin-btn', (idToken) => this.onToken(idToken));
    }

    private onToken(idToken: string) {
        this.tokenReceived.emit(idToken);

        if (!this.handleLogin()) return;

        this.googleAuth.loginWithGoogle(idToken).subscribe({
            next: (res) => {
                this.authState.setTokens(res.data);
                this.snackbar.success('Signed in with Google!');
                this.router.navigate([this.redirectTo()]);
            },
            error: (err) => {
                this.snackbar.error(err.message || 'Google sign-in failed. Please try again.');
            },
        });
    }

    ngOnDestroy() { }
}