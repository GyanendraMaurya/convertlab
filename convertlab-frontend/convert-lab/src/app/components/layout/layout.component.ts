import { Component, inject, signal } from '@angular/core';
import { NavbarComponent } from '../shared/navbar/navbar.component';
import { Router, RouterOutlet } from '@angular/router';
import { VersionDisplayComponent } from '../shared/version-display';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';
import { AuthStateService } from '../../services/auth-state.service';

@Component({
  selector: 'app-layout',
  imports: [
    RouterOutlet,
    NavbarComponent,
    VersionDisplayComponent,
    MatSidenavModule,
    MatListModule,
    MatIconModule,
    RouterLink
  ],
  templateUrl: './layout.component.html',
  styleUrl: './layout.component.scss',
})
export class LayoutComponent {
  private authState = inject(AuthStateService);

  sidenavOpened = signal(false);
  adminMenuOpened = signal(false);
  isSuperAdmin = this.authState.isSuperAdmin;

  constructor(private router: Router) { }

  toggleSidenav() {
    const nextState = !this.sidenavOpened();
    this.sidenavOpened.set(nextState);

    if (!nextState) {
      this.adminMenuOpened.set(false);
    } else if (this.isAdminActive()) {
      this.adminMenuOpened.set(true);
    }
  }

  closeSidenav() {
    this.sidenavOpened.set(false);
    this.adminMenuOpened.set(false);
  }

  toggleAdminMenu() {
    this.adminMenuOpened.update(v => !v);
  }

  closeAdminMenu() {
    this.adminMenuOpened.set(false);
  }

  isActive(route: string): boolean {
    return this.router.url === route;
  }

  isAdminActive(): boolean {
    return this.router.url.startsWith('/admin/');
  }
}
