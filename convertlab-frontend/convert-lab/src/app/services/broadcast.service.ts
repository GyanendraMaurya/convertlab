import { HttpContext } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Observable, Subscription } from 'rxjs';
import { environment } from '../../environments/environment';
import { SUPPRESS_ERROR } from '../interceptors/http-context';
import { ApiResponse, HttpService } from './http.service';
import { WebSocketService } from './websocket.service';

export interface BroadcastMessage {
  id: string;
  message: string;
  createdAt: string;
  expiresAt: string;
  active: boolean;
}

export interface CreateBroadcastRequest {
  message: string;
  expiresAt: string;
}

type DismissedBroadcasts = Record<string, string>;

const DISMISSED_BROADCASTS_KEY = 'convertlab.dismissedBroadcasts';

@Injectable({
  providedIn: 'root',
})
export class BroadcastService {
  private readonly apiUrl = environment.apiUrl;
  private readonly httpService = inject(HttpService);
  private readonly ws = inject(WebSocketService);
  private wsSubscription: Subscription | null = null;

  readonly visibleBroadcasts = signal<BroadcastMessage[]>([]);

  init(): void {
    this.cleanupDismissedBroadcasts();
    this.loadActiveBroadcasts();

    if (this.wsSubscription) {
      return;
    }

    this.wsSubscription = this.ws.on<BroadcastMessage>('BROADCAST_MESSAGE').subscribe(event => {
      this.addVisibleBroadcast(event.payload);
    });
  }

  loadActiveBroadcasts(): void {
    this.httpService.get<ApiResponse<BroadcastMessage[]>>(
      `${this.apiUrl}/broadcasts/active`,
      { context: new HttpContext().set(SUPPRESS_ERROR, true) },
    ).subscribe({
      next: (response) => {
        const broadcasts = (response.data ?? [])
          .filter(broadcast => this.shouldDisplay(broadcast))
          .sort((a, b) => this.toMillis(b.createdAt) - this.toMillis(a.createdAt));
        this.visibleBroadcasts.set(broadcasts);
      },
      error: () => {
        this.visibleBroadcasts.set([]);
      },
    });
  }

  dismissBroadcast(id: string): void {
    const broadcast = this.visibleBroadcasts().find(item => item.id === id);

    if (broadcast) {
      const dismissed = this.getDismissedBroadcasts();
      dismissed[id] = broadcast.expiresAt;
      this.setDismissedBroadcasts(dismissed);
    }

    this.visibleBroadcasts.update(broadcasts => broadcasts.filter(item => item.id !== id));
  }

  getAdminBroadcasts(): Observable<ApiResponse<BroadcastMessage[]>> {
    return this.httpService.get<ApiResponse<BroadcastMessage[]>>(`${this.apiUrl}/admin/broadcasts`);
  }

  createBroadcast(request: CreateBroadcastRequest): Observable<ApiResponse<BroadcastMessage>> {
    return this.httpService.post<ApiResponse<BroadcastMessage>>(`${this.apiUrl}/admin/broadcasts`, request);
  }

  deactivateBroadcast(id: string): Observable<ApiResponse<BroadcastMessage>> {
    return this.httpService.patch<ApiResponse<BroadcastMessage>>(`${this.apiUrl}/admin/broadcasts/${id}/deactivate`);
  }

  private addVisibleBroadcast(broadcast: BroadcastMessage): void {
    if (!this.shouldDisplay(broadcast)) {
      return;
    }

    this.visibleBroadcasts.update(broadcasts => {
      const withoutDuplicate = broadcasts.filter(item => item.id !== broadcast.id);
      return [broadcast, ...withoutDuplicate];
    });
  }

  private shouldDisplay(broadcast: BroadcastMessage): boolean {
    return broadcast.active
      && this.toMillis(broadcast.expiresAt) > Date.now()
      && !this.isDismissed(broadcast.id);
  }

  private isDismissed(id: string): boolean {
    return id in this.getDismissedBroadcasts();
  }

  private cleanupDismissedBroadcasts(): void {
    const dismissed = this.getDismissedBroadcasts();
    const now = Date.now();
    const nextDismissed: DismissedBroadcasts = {};

    for (const [id, expiresAt] of Object.entries(dismissed)) {
      if (this.toMillis(expiresAt) > now) {
        nextDismissed[id] = expiresAt;
      }
    }

    this.setDismissedBroadcasts(nextDismissed);
  }

  private getDismissedBroadcasts(): DismissedBroadcasts {
    try {
      const raw = localStorage.getItem(DISMISSED_BROADCASTS_KEY);
      return raw ? JSON.parse(raw) as DismissedBroadcasts : {};
    } catch {
      return {};
    }
  }

  private setDismissedBroadcasts(value: DismissedBroadcasts): void {
    try {
      localStorage.setItem(DISMISSED_BROADCASTS_KEY, JSON.stringify(value));
    } catch {
      // Ignore storage errors so broadcasts still work in privacy-restricted browsers.
    }
  }

  private toMillis(value: string): number {
    return new Date(value).getTime();
  }
}
