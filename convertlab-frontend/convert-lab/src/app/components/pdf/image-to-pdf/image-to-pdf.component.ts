import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core';
import { CdkDrag, CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { FileUploaderComponent } from '../../shared/file-uploader/file-uploader.component';
import { FileUploadService } from '../../../services/file-upload.service';
import { MatIconModule } from '@angular/material/icon';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { ActionButtonComponent } from '../../shared/action-button/action-button.component';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SnackbarService } from '../../../services/snackbar.service';
import { ImageThumbnailComponent } from '../../shared/image-thumbnail/image-thumbnail.component';
import { PdfService } from '../../../services/pdf.service';
import { ImageThumbnail } from '../../../models/image-thumbnail.mode';

@Component({
  selector: 'app-image-to-pdf',
  imports: [
    ImageThumbnailComponent,
    FileUploaderComponent,
    MatIconModule,
    DragDropModule,
    CdkDrag,
    ActionButtonComponent,
    MatTooltipModule,
    MatButtonModule
  ],
  templateUrl: './image-to-pdf.component.html',
  styleUrl: './image-to-pdf.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImageToPdfComponent {
  private readonly fileUploadService = inject(FileUploadService);
  private readonly snackbarService = inject(SnackbarService);
  private readonly pdfService = inject(PdfService);

  thumbnails = signal<ImageThumbnail[]>([]);
  isConverting = signal(false);
  isWaitingForUploads = signal(false);

  // Computed states
  isAnyUploading = computed(() =>
    this.thumbnails().some(t => t.uploadStatus === 'uploading' || t.uploadStatus === 'pending')
  );

  hasFailedUploads = computed(() =>
    this.thumbnails().some(t => t.uploadStatus === 'failed')
  );

  allUploadsCompleted = computed(() =>
    this.thumbnails().length > 0 &&
    this.thumbnails().every(t => t.uploadStatus === 'completed')
  );

  canConvert = computed(() =>
    this.thumbnails().length > 0 &&
    !this.isConverting() &&
    !this.isWaitingForUploads()
  );

  convertButtonLabel = computed(() => {
    if (this.isConverting()) return 'Converting...';
    if (this.isWaitingForUploads()) return 'Uploading...';
    return 'Convert to PDF';
  });

  fileUploader = viewChild(FileUploaderComponent);

  onFilesUploaded(files: File[] | null) {
    if (!files || files.length === 0) return;

    for (const file of files) {
      const tempId = `temp-${Date.now()}-${Math.random()}`;
      this.processImage(file, tempId);
      this.uploadFileInBackground(file, tempId);
    }

    // Clear file input for next upload
    this.fileUploader()?.clearFileInput();
  }

  private async processImage(file: File, tempId: string) {

    try {
      const { thumbnailUrl, width, height } = await this.generateImageThumbnail(file);

      const thumbnail: ImageThumbnail = {
        fileId: tempId,
        fileName: file.name,
        thumbnailUrl,
        rotation: 0,
        width,
        height,
        uploadStatus: 'pending',
        file,
        trackId: tempId
      };
      this.thumbnails.update(list => [...list, thumbnail]);
    } catch (error) {
      console.error('Failed to process image:', error);
      this.snackbarService.error(`Failed to process ${file.name}`);
    }
  }

  private async generateImageThumbnail(file: File): Promise<{ thumbnailUrl: string; width: number; height: number }> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      const url = URL.createObjectURL(file);

      img.onload = () => {
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d')!;

        // Create thumbnail with max dimension of 300px
        const maxDimension = 300;
        let width = img.width;
        let height = img.height;

        if (width > height) {
          if (width > maxDimension) {
            height = (height * maxDimension) / width;
            width = maxDimension;
          }
        } else {
          if (height > maxDimension) {
            width = (width * maxDimension) / height;
            height = maxDimension;
          }
        }

        canvas.width = width;
        canvas.height = height;
        ctx.drawImage(img, 0, 0, width, height);

        canvas.toBlob((blob) => {
          URL.revokeObjectURL(url);
          if (blob) {
            const thumbnailUrl = URL.createObjectURL(blob);
            resolve({
              thumbnailUrl,
              width: img.width,
              height: img.height
            });
          } else {
            reject(new Error('Failed to create thumbnail'));
          }
        }, 'image/jpeg', 0.8);
      };

      img.onerror = () => {
        URL.revokeObjectURL(url);
        reject(new Error('Failed to load image'));
      };

      img.src = url;
    });
  }

  private uploadFileInBackground(file: File, id: string) {
    this.thumbnails.update(list =>
      list.map(t =>
        t.fileId === id
          ? { ...t, uploadStatus: 'uploading' as const }
          : t
      )
    );

    this.fileUploadService.uploadImage(file).subscribe({
      next: (res) => {
        this.thumbnails.update(list =>
          list.map(t =>
            t.fileId === id
              ? {
                ...t,
                fileId: res.data.fileId,
                uploadStatus: 'completed'
              }
              : t
          )
        );
      },
      error: (err) => {
        this.thumbnails.update(list => list.map(t =>
          t.fileId === id
            ? {
              ...t,
              uploadStatus: 'failed',
              error: err.message || 'Upload failed'
            }
            : t
        ));
      }
    });
  }

  removeImage(id: string) {
    const thumbnail = this.thumbnails().find(t =>
      t.fileId === id || t.thumbnailUrl === id
    );

    if (thumbnail?.thumbnailUrl.startsWith('blob:')) {
      URL.revokeObjectURL(thumbnail.thumbnailUrl);
    }

    this.thumbnails.update(list =>
      list.filter(t => t.fileId !== id && t.thumbnailUrl !== id)
    );
  }

  retryUpload(id: string | null) {
    if (!id) return;

    const thumbnail = this.thumbnails().find(t => t.fileId === id);

    if (thumbnail && thumbnail.file && thumbnail.uploadStatus === 'failed') {
      this.uploadFileInBackground(thumbnail.file, id);
    }
  }

  rotateImage(id: string) {
    this.thumbnails.update(list =>
      list.map(t => {
        if (t.fileId === id) {
          const newRotation = ((t.rotation + 90) % 360) as 0 | 90 | 180 | 270;
          return { ...t, rotation: newRotation };
        }
        return t;
      })
    );
  }

  onDrop(event: CdkDragDrop<ImageThumbnail[]>) {
    this.thumbnails.update(list => {
      const updated = [...list];
      moveItemInArray(updated, event.previousIndex, event.currentIndex);
      return updated;
    });
  }

  async convert() {
    if (this.thumbnails().length === 0) return;

    if (this.isAnyUploading()) {
      this.isWaitingForUploads.set(true);
      await this.waitForUploadsToComplete();
      this.isWaitingForUploads.set(false);
    }

    if (this.hasFailedUploads()) {
      this.snackbarService.error('Some uploads failed. Please remove or retry them.');
      return;
    }

    const imageData = this.thumbnails()
      .filter(t => t.fileId !== null)
      .map(t => ({
        fileId: t.fileId!,
        rotation: t.rotation
      }));

    if (imageData.length === 0) return;

    this.isConverting.set(true);

    const imageToPdfRequest = {
      images: this.thumbnails().map(thumbnail => ({
        fileId: thumbnail.fileId,
        rotation: thumbnail.rotation
      }))
    };

    this.pdfService.convertImagesToPdf(imageToPdfRequest).subscribe({
      next: response => {
        this.isConverting.set(false);
        const blob = response.body as Blob;
        const contentDisposition = response.headers.get('content-disposition');
        let fileName = 'ConvertLab_ImageToPdf.pdf';

        if (contentDisposition) {
          const match = contentDisposition.match(/filename="([^"]+)"/);
          if (match?.[1]) fileName = match[1];
        }

        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.isConverting.set(false);
      }
    });
  }

  private waitForUploadsToComplete(): Promise<void> {
    return new Promise((resolve) => {
      const checkInterval = setInterval(() => {
        if (!this.isAnyUploading()) {
          clearInterval(checkInterval);
          resolve();
        }
      }, 100);
    });
  }

  private revokeThumbnailUrls(): void {
    this.thumbnails().forEach(thumbnail => {
      if (thumbnail.thumbnailUrl.startsWith('blob:')) {
        URL.revokeObjectURL(thumbnail.thumbnailUrl);
      }
    });
  }

  ngOnDestroy(): void {
    this.revokeThumbnailUrls();
  }
}
