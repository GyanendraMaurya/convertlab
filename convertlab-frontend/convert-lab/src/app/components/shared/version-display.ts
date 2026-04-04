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
          <div class="version-item">
            <a href="https://www.linkedin.com/in/gyanendramaurya/"
              target="_blank"
              rel="noopener noreferrer"
              class="author-link">
              <svg class="linkedin-icon"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 24 24"
                  fill="#0077b5">
                <path d="M20.447 20.452h-3.554v-5.569c0-1.328-.027-3.037-1.852-3.037-1.853 0-2.136 1.445-2.136 2.939v5.667H9.351V9h3.414v1.561h.046c.477-.9 1.637-1.85 3.37-1.85 3.601 0 4.267 2.37 4.267 5.455v6.286zM5.337 7.433a2.062 2.062 0 0 1-2.063-2.065 2.064 2.064 0 1 1 2.063 2.065zm1.782 13.019H3.555V9h3.564v11.452zM22.225 0H1.771C.792 0 0 .774 0 1.729v20.542C0 23.227.792 24 1.771 24h20.451C23.2 24 24 23.227 24 22.271V1.729C24 .774 23.2 0 22.222 0h.003z"/>
              </svg>
              Gyanendra Maurya
            </a>
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

    .author-link {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      color: #0077b5;
      text-decoration: none;
      font-size: 12px;
      font-weight: 500;
      transition: opacity 0.2s;
    }

    .author-link:hover {
      opacity: 0.75;
      text-decoration: underline;
    }

    .linkedin-icon {
      width: 14px;
      height: 14px;
      flex-shrink: 0;
    }`
  ]
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
