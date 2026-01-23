import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { SkeletonLoaderComponent } from '../skeleton-loader/skeleton-loader.component';
import { DecimalPipe } from '@angular/common';

@Component({
  selector: 'app-thumbnail',
  imports: [MatIconModule, MatButtonModule, SkeletonLoaderComponent, DecimalPipe],
  templateUrl: './thumbnail.component.html',
  styleUrl: './thumbnail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ThumbnailComponent {
  id = input.required<string>();
  thumbnailUrl = input.required<string>();
  fileName = input<string>('');
  pageCount = input<number | undefined>(0);
  size = input<number>(0);
  uploadStatus = input<'pending' | 'uploading' | 'completed' | 'failed'>('completed');
  disabled = input(false);

  remove = output<string>();
  retry = output<void>();

  isUploading = computed(() =>
    this.uploadStatus() === 'uploading' || this.uploadStatus() === 'pending'
  );

  isFailed = computed(() => this.uploadStatus() === 'failed');

  sizeInMegaBytes = computed(() => this.size() / 1024 / 1024);

  onRemoveClick(event: MouseEvent) {
    event.stopPropagation();
    if (!this.disabled()) {
      this.remove.emit(this.id());
    }
  }

  onRetryClick(event: MouseEvent) {
    event.stopPropagation();
    this.retry.emit();
  }
}
