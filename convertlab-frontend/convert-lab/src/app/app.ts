import { Component, inject, PLATFORM_ID, signal } from '@angular/core';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { LayoutComponent } from './components/layout/layout.component';
import { MatToolbar, MatToolbarModule } from '@angular/material/toolbar';
import { SessionService } from './services/session.service';
import { PageVisitService } from './services/page-visit.service';
import { isPlatformBrowser } from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [MatSlideToggleModule, LayoutComponent, MatToolbarModule],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('convert-lab');
  private readonly session = inject(SessionService);
  private readonly pageVisit = inject(PageVisitService);
  private platformId = inject(PLATFORM_ID);

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.session.init();
      this.pageVisit.init();
    }
  }
}
