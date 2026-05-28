import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { FeatureFlag, FeatureFlagService } from '../../../services/feature-flag.service';
import { SnackbarService } from '../../../services/snackbar.service';

@Component({
  selector: 'app-admin-features',
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSlideToggleModule,
  ],
  templateUrl: './admin-features.component.html',
  styleUrl: './admin-features.component.scss',
})
export class AdminFeaturesComponent {
  private readonly featureFlagService = inject(FeatureFlagService);
  private readonly snackbar = inject(SnackbarService);

  readonly features = signal<FeatureFlag[]>([]);
  readonly loading = signal(true);
  readonly saving = signal(false);
  readonly dirty = signal(false);
  readonly loadError = signal<string | null>(null);

  ngOnInit(): void {
    this.loadFeatures();
  }

  loadFeatures(): void {
    this.loading.set(true);
    this.loadError.set(null);

    this.featureFlagService.getAdminFeatures().subscribe({
      next: (response) => {
        this.features.set(response.data ?? []);
        this.dirty.set(false);
        this.loading.set(false);
      },
      error: (error) => {
        this.loadError.set(error.message || 'Failed to load features.');
        this.loading.set(false);
      },
    });
  }

  setFeatureEnabled(code: string, enabled: boolean): void {
    this.features.update(features =>
      features.map(feature =>
        feature.code === code ? { ...feature, enabled } : feature
      )
    );
    this.dirty.set(true);
  }

  saveFeatures(): void {
    if (this.saving() || !this.dirty()) {
      return;
    }

    this.saving.set(true);

    this.featureFlagService.updateAdminFeatures(
      this.features().map(feature => ({
        code: feature.code,
        enabled: feature.enabled,
      })),
    ).subscribe({
      next: (response) => {
        const savedFeatures = response.data ?? [];
        this.features.set(savedFeatures);
        this.featureFlagService.applyPublicFeatures(savedFeatures);
        this.dirty.set(false);
        this.saving.set(false);
        this.snackbar.success('Features saved successfully.');
      },
      error: () => {
        this.saving.set(false);
      },
    });
  }
}
