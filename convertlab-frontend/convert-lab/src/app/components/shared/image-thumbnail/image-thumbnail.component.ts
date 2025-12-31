import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';

@Component({
  selector: 'app-image-thumbnail',
  imports: [MatIconModule, MatButtonModule, MatTooltipModule],
  templateUrl: './image-thumbnail.component.html',
  styleUrl: './image-thumbnail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageThumbnailComponent {
  id = input.required<string>();
  thumbnailUrl = input.required<string>();
  fileName = input<string>('');
  dimensions = input<string>('');
  rotation = input<0 | 90 | 180 | 270>(0);
  uploadStatus = input<'pending' | 'uploading' | 'completed' | 'failed'>('completed');
  disabled = input(false);

  remove = output<string>();
  rotate = output<string>();
  retry = output<void>();

  isUploading = computed(() =>
    this.uploadStatus() === 'uploading' || this.uploadStatus() === 'pending'
  );

  isFailed = computed(() => this.uploadStatus() === 'failed');

  // Compute rotation transform
  rotationTransform = computed(() => `rotate(${this.rotation()}deg)`);

  onRemoveClick(event: MouseEvent) {
    event.stopPropagation();
    if (!this.disabled()) {
      this.remove.emit(this.id());
    }
  }

  onRotateClick(event: MouseEvent) {
    event.stopPropagation();
    if (!this.disabled()) {
      this.rotate.emit(this.id());
    }
  }

  onRetryClick(event: MouseEvent) {
    event.stopPropagation();
    this.retry.emit();
  }
}
