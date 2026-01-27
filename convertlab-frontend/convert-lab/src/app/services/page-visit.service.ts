import { Injectable } from '@angular/core';
import { NavigationEnd, Router } from '@angular/router';
import { SessionService } from './session.service';
import { filter } from 'rxjs';
import { HttpService } from './http.service';
import { environment } from '../../environments/environment';

@Injectable({ providedIn: 'root' })
export class PageVisitService {
  private readonly apiUrl = environment.apiUrl;
  private firstNavigation = true;

  constructor(
    private router: Router,
    private http: HttpService,
    private session: SessionService
  ) { }

  init() {
    this.router.events
      .pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: NavigationEnd) => {
        this.recordVisit(e.urlAfterRedirects);
      });
  }

  private recordVisit(path: string) {
    const payload = {
      sessionId: this.session.sessionId(),
      path,
      entry: this.firstNavigation
    };

    this.firstNavigation = false;
    this.http.post(`${this.apiUrl}/analytics/page-visit`, payload).subscribe();
  }
}
