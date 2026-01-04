import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { SkeletonLoaderComponent } from '../skeleton-loader/skeleton-loader.component';

@Component({
  selector: 'app-thumbnail',
  imports: [MatIconModule, MatButtonModule, SkeletonLoaderComponent],
  templateUrl: './thumbnail.component.html',
  styleUrl: './thumbnail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ThumbnailComponent {
  id = input.required<string>();
  thumbnailUrl = input.required<string>();
  fileName = input<string>('');
  pageCount = input<number>(0);
  uploadStatus = input<'pending' | 'uploading' | 'completed' | 'failed'>('completed');
  disabled = input(false);

  remove = output<string>();
  retry = output<void>();

  isUploading = computed(() =>
    this.uploadStatus() === 'uploading' || this.uploadStatus() === 'pending'
  );

  isFailed = computed(() => this.uploadStatus() === 'failed');

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
