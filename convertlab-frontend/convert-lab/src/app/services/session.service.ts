import { inject, Injectable, PLATFORM_ID, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class SessionService {
  private readonly KEY = 'cl_session_id';

  sessionId = signal<string | null>(null);

  init() {
    let id = sessionStorage.getItem(this.KEY);
    if (!id) {
      id = crypto.randomUUID();
      sessionStorage.setItem(this.KEY, id);
    }
    this.sessionId.set(id);
  }
}

