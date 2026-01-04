import { Component, inject, signal, OnInit, afterNextRender } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import { VersionService } from '../../services/version.service';

@Component({
  selector: 'app-version-display',
  imports: [MatIconModule, MatButtonModule, CommonModule],
  template: `
    <div class="version-container">
      <button mat-icon-button (click)="toggleExpanded()" [title]="expanded() ? 'Hide version info' : 'Show version info'">
        <mat-icon>{{ expanded() ? 'expand_more' : 'info' }}</mat-icon>
      </button>

      @if (expanded()) {
        <div class="version-details">
          <div class="version-item">
            <strong>Frontend:</strong> v{{ frontendVersion().version }}
          </div>
          <div class="version-item">
            <strong>Backend:</strong>
            @if (backendVersion()) {
              v{{ backendVersion()!.version }}
            } @else if (loadingBackend()) {
              <span class="loading">Loading...</span>
            } @else {
              <span class="error">Failed to load</span>
            }
          </div>
          <div class="version-item small">
            Built: {{ frontendVersion().buildTime | date:'short' }}
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .version-container {
      position: fixed;
      bottom: 16px;
      right: 16px;
      background: var(--mat-sys-surface-container);
      border-radius: 28px;
      box-shadow: 0 2px 8px rgba(0,0,0,0.15);
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 4px;
      z-index: 1000;
      transition: all 0.3s ease;
    }

    .version-details {
      padding: 8px 16px 8px 8px;
      display: flex;
      flex-direction: column;
      gap: 4px;
      font-size: 13px;
      color: var(--mat-sys-on-surface);
      max-width: 250px;
      animation: slideIn 0.3s ease;
    }

    @keyframes slideIn {
      from {
        opacity: 0;
        transform: translateX(20px);
      }
      to {
        opacity: 1;
        transform: translateX(0);
      }
    }

    .version-item {
      display: flex;
      gap: 6px;
      white-space: nowrap;
    }

    .version-item.small {
      font-size: 11px;
      color: var(--mat-sys-on-surface-variant);
      margin-top: 4px;
    }

    .loading {
      color: var(--mat-sys-primary);
    }

    .error {
      color: var(--mat-sys-error);
    }

    strong {
      font-weight: 500;
    }
  `]
})
export class VersionDisplayComponent implements OnInit {
  private versionService = inject(VersionService);

  frontendVersion = signal(this.versionService.getFrontendVersion());
  backendVersion = signal<{ version: string; buildTime: string } | null>(null);
  loadingBackend = signal(true);
  expanded = signal(false);

  constructor() {
    afterNextRender(() => {
      this.loadBackendVersion();
    });
  }
  ngOnInit() {

  }

  loadBackendVersion() {
    this.versionService.getBackendVersion().subscribe({
      next: (response) => {
        this.backendVersion.set(response.data);
        this.loadingBackend.set(false);
      },
      error: () => {
        this.loadingBackend.set(false);
      }
    });
  }

  toggleExpanded() {
    this.expanded.update(v => !v);
  }
}
