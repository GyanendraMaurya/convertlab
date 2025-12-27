import { Component, signal } from '@angular/core';
import { NavbarComponent } from '../shared/navbar/navbar.component';
import { Router, RouterOutlet } from '@angular/router';
import { VersionDisplayComponent } from '../shared/version-display';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { RouterLink } from '@angular/router';

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
  sidenavOpened = signal(false);

  constructor(private router: Router) { }

  toggleSidenav() {
    this.sidenavOpened.update(v => !v);
  }

  closeSidenav() {
    this.sidenavOpened.set(false);
  }

  isActive(route: string): boolean {
    return this.router.url === route;
  }
}
