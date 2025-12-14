// src/app/components/pdf/merge-pdf/merge-pdf.component.ts
import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ThumbnailComponent } from '../../shared/thumbnail/thumbnail.component';
import { Thumbnail } from '../../../models/thumbnail.model';
import { FileUploaderComponent } from '../../shared/file-uploader/file-uploader.component';
import { FileUploadService } from '../../../services/file-upload.service';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-merge-pdf',
  imports: [ThumbnailComponent, FileUploaderComponent, MatIconModule],
  templateUrl: './merge-pdf.component.html',
  styleUrl: './merge-pdf.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MergePdfComponent {
  private readonly fileUploadService = inject(FileUploadService);

  thumbnails = signal<Thumbnail[]>([]);
  isUploading = signal(false);
  selectedFile = signal<File | null>(null);
  isExtracting = signal(false);

  // Drag & Drop state
  draggedIndex = signal<number | null>(null);
  dragOverIndex = signal<number | null>(null);

  // Touch support
  touchStartY = 0;
  touchStartX = 0;
  isDraggingTouch = signal(false);

  onFileUploaded($event: File | null) {
    this.isUploading.set(true);
    this.selectedFile.set($event);
    this.fileUploadService.uploadPdf(this.selectedFile()!).subscribe({
      next: res => {
        this.thumbnails.update(thumbnails => [...thumbnails, res.data]);
        this.isUploading.set(false);
      },
      error: err => {
        this.isUploading.set(false);
      }
    });
  }

  removePdf(id: string) {
    this.thumbnails.update(thumbnails =>
      thumbnails.filter(thumbnail => thumbnail.fileId !== id)
    );
  }

  // Drag & Drop handlers
  onDragStart(index: number) {
    this.draggedIndex.set(index);
  }

  onDragOver(event: DragEvent, index: number) {
    event.preventDefault();
    this.dragOverIndex.set(index);
  }

  onDragLeave() {
    this.dragOverIndex.set(null);
  }

  onDrop(event: DragEvent, dropIndex: number) {
    event.preventDefault();

    const dragIndex = this.draggedIndex();
    if (dragIndex === null || dragIndex === dropIndex) {
      this.resetDragState();
      return;
    }

    // Reorder the array
    this.thumbnails.update(thumbnails => {
      const newThumbnails = [...thumbnails];
      const [draggedItem] = newThumbnails.splice(dragIndex, 1);
      newThumbnails.splice(dropIndex, 0, draggedItem);
      return newThumbnails;
    });

    this.resetDragState();
  }

  onDragEnd() {
    this.resetDragState();
  }

  private resetDragState() {
    this.draggedIndex.set(null);
    this.dragOverIndex.set(null);
    this.isDraggingTouch.set(false);
  }

  // Touch event handlers for mobile
  onTouchStart(event: TouchEvent, index: number) {
    const touch = event.touches[0];
    this.touchStartX = touch.clientX;
    this.touchStartY = touch.clientY;
    this.draggedIndex.set(index);
    this.isDraggingTouch.set(true);
  }

  onTouchMove(event: TouchEvent) {
    if (!this.isDraggingTouch() || this.draggedIndex() === null) return;

    event.preventDefault(); // Prevent scrolling while dragging

    const touch = event.touches[0];
    const element = document.elementFromPoint(touch.clientX, touch.clientY);

    // Find the thumbnail wrapper element
    const thumbnailWrapper = element?.closest('.thumbnail-list > .thumbnail-wrapper');
    if (thumbnailWrapper) {
      const allWrappers = Array.from(document.querySelectorAll('.thumbnail-list > .thumbnail-wrapper'));
      const dropIndex = allWrappers.indexOf(thumbnailWrapper);

      if (dropIndex !== -1 && dropIndex !== this.draggedIndex()) {
        this.dragOverIndex.set(dropIndex);
      }
    }
  }

  onTouchEnd(event: TouchEvent) {
    if (!this.isDraggingTouch() || this.draggedIndex() === null) {
      this.resetDragState();
      return;
    }

    const touch = event.changedTouches[0];
    const element = document.elementFromPoint(touch.clientX, touch.clientY);
    const thumbnailWrapper = element?.closest('.thumbnail-list > .thumbnail-wrapper');

    if (thumbnailWrapper) {
      const allWrappers = Array.from(document.querySelectorAll('.thumbnail-list > .thumbnail-wrapper'));
      const dropIndex = allWrappers.indexOf(thumbnailWrapper);

      if (dropIndex !== -1 && dropIndex !== this.draggedIndex()) {
        const dragIndex = this.draggedIndex()!;

        // Reorder the array
        this.thumbnails.update(thumbnails => {
          const newThumbnails = [...thumbnails];
          const [draggedItem] = newThumbnails.splice(dragIndex, 1);
          newThumbnails.splice(dropIndex, 0, draggedItem);
          return newThumbnails;
        });
      }
    }

    this.resetDragState();
  }
}
