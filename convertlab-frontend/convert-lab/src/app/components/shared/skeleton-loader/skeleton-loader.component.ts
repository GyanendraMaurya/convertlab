import { Component, input } from '@angular/core';

export type SkeletonVariant = 'text' | 'circular' | 'rectangular' | 'thumbnail';

@Component({
  selector: 'app-skeleton-loader',
  imports: [],
  templateUrl: './skeleton-loader.component.html',
  styleUrl: './skeleton-loader.component.scss',
})
export class SkeletonLoaderComponent {
  /** Skeleton variant type */
  variant = input<SkeletonVariant>('rectangular');

  /** Width of the skeleton (CSS units: px, %, em, etc.) */
  width = input<string>('100%');

  /** Height of the skeleton (CSS units: px, %, em, etc.) */
  height = input<string>('20px');

  /** Border radius (CSS units) */
  borderRadius = input<string | null>(null);

  /** Animation type */
  animation = input<'wave' | 'pulse' | 'none'>('wave');
}
