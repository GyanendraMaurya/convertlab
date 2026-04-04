// convertlab-frontend/convert-lab/src/app/components/shared/navbar/navbar.component.ts
import { Component, computed, inject, output, signal } from '@angular/core';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { RouterLink, RouterModule, Router } from '@angular/router';
import { AuthStateService } from '../../../services/auth-state.service';
import { AuthService } from '../../../services/auth.service';
import { SnackbarService } from '../../../services/snackbar.service';
import { MatDivider } from '@angular/material/divider';
import { UserService } from '../../../services/user.service';

@Component({
  selector: 'app-navbar',
  imports: [
    RouterModule,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatMenuModule,
    MatDivider,
    RouterLink
  ],
  templateUrl: './navbar.component.html',
  styleUrl: './navbar.component.scss',
})
export class NavbarComponent {
  private authState = inject(AuthStateService);
  private authService = inject(AuthService);
  private userService = inject(UserService);
  private snackbar = inject(SnackbarService);
  private router = inject(Router);

  menuToggle = output<void>();
  isAuthenticated = this.authState.isAuthenticated;
  userEmail = computed(() => this.authState.getEmail());

  userInitial = computed(() => {
    const email = this.userEmail();
    return email ? email.charAt(0).toUpperCase() : 'U';
  });

  onMenuClick() {
    this.menuToggle.emit();
  }

  onLogout() {
    this.authService.logout().subscribe({
      next: () => {
        this.authState.clearTokens();
        this.snackbar.success('Logged out successfully');
        this.router.navigate(['/login']);
      },
      error: () => {
        this.authState.clearTokens();
        this.router.navigate(['/login']);
      }
    });
  }

  deleteAccount() {
    if (confirm('Are you sure you want to delete your account? This action cannot be undone.')) {
      this.userService.deleteAccount().subscribe({
        next: () => {
          this.authState.clearTokens();
          this.snackbar.success('Account deleted successfully');
          this.router.navigate(['/signup']);
        },
        error: () => {
          this.snackbar.error('Failed to delete account. Please try again later.');
        }
      });
    }
  }
}
