import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core';
import { FileUploaderComponent } from '../../shared/file-uploader/file-uploader.component';
import { FileUploadService } from '../../../services/file-upload.service';
import { MatIconModule } from '@angular/material/icon';
import { ActionButtonComponent } from '../../shared/action-button/action-button.component';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SnackbarService } from '../../../services/snackbar.service';
import { ImageThumbnailComponent } from '../../shared/image-thumbnail/image-thumbnail.component';
import { ImageThumbnail } from '../../../models/image-thumbnail.mode';
import { SeoService } from '../../../seo/seo.service';
import { FileValidationService } from '../../../services/file-validation.service';
import { ImageService } from '../../../services/image.service';
import { CompressImageRequest } from '../../../models/compress-image.model';
import { CompressionLevel } from '../../../models/compression-level.model';
import { MatButtonToggleChange, MatButtonToggleModule } from '@angular/material/button-toggle';

@Component({
  selector: 'app-compress-image',
  imports: [
    ImageThumbnailComponent,
    FileUploaderComponent,
    MatIconModule,
    ActionButtonComponent,
    MatTooltipModule,
    MatButtonModule,
    MatButtonToggleModule
  ],
  templateUrl: './compress-image.component.html',
  styleUrl: './compress-image.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CompressImageComponent {
  private readonly fileUploadService = inject(FileUploadService);
  private readonly snackbarService = inject(SnackbarService);
  private readonly imageService = inject(ImageService);
  private seoService = inject(SeoService);
  public readonly fileValidationService = inject(FileValidationService);

  thumbnails = signal<ImageThumbnail[]>([]);
  isCompressing = signal(false);
  isWaitingForUploads = signal(false);
  compressionLevel = signal<CompressionLevel>(CompressionLevel.MEDIUM);

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

  canCompress = computed(() =>
    this.thumbnails().length > 0 &&
    !this.isCompressing() &&
    !this.isWaitingForUploads()
  );

  compressButtonLabel = computed(() => {
    if (this.isCompressing()) return 'Compressing...';
    if (this.isWaitingForUploads()) return 'Uploading...';
    return 'Compress Image';
  });

  allowedTypes = computed(() => this.fileValidationService.getConstraints('image').allowedExtensions);

  fileUploader = viewChild(FileUploaderComponent);

  ngOnInit() {
    this.seoService.applySEO('compress-image');
  }

  onRawFilesReceived(files: File[] | null) {
    if (!files || files.length === 0) return;

    // Immediately add placeholders with skeleton loaders
    for (const file of files) {
      const tempId = `temp-${Date.now()}-${Math.random()}`;
      this.addPlaceholderThumbnail(file, tempId);
    }
  }

  // Simplified onFilesUploaded (receives converted files):
  onFilesUploaded(files: File[] | null) {
    if (!files || files.length === 0) return;

    // Files are already converted at this point
    for (const file of files) {
      // Find the placeholder by original filename pattern
      const tempId = this.findTempIdForFile(file);

      if (tempId) {
        // Update file reference and process in parallel
        this.thumbnails.update(list =>
          list.map(t =>
            t.tempId === tempId
              ? { ...t, file, fileName: file.name }
              : t
          )
        );

        // Run thumbnail generation and upload in parallel
        this.processImage(file, tempId);
        this.uploadFileInBackground(file, tempId);
      }
    }

    // Clear file input for next upload
    this.fileUploader()?.clearFileInput();
  }

  private findTempIdForFile(convertedFile: File): string | null {
    const thumbnails = this.thumbnails();

    // For HEIC -> JPG conversion, match by base name
    const baseName = convertedFile.name.replace(/\.(jpg|jpeg)$/i, '');

    const match = thumbnails.find(t => {
      if (!t.file) return false;
      const originalBase = t.file.name.replace(/\.(heic|jpg|jpeg|png|gif|bmp|webp)$/i, '');
      return originalBase === baseName || t.file.name === convertedFile.name;
    });

    return match?.tempId || null;
  }

  private addPlaceholderThumbnail(file: File, tempId: string): void {
    const placeholder: ImageThumbnail = {
      fileId: null,
      fileName: file.name,
      thumbnailUrl: '',
      rotation: 0,
      width: 0,
      height: 0,
      uploadStatus: 'pending',
      file,
      tempId: tempId
    };

    this.thumbnails.update(list => [...list, placeholder]);
  }

  private async processImage(file: File, tempId: string): Promise<void> {
    try {
      console.log("thumbnail generateion started")
      const { thumbnailUrl, size } = await this.generateImageThumbnail(file);
      console.log("thumbnail generateion ended")

      this.thumbnails.update(list =>
        list.map(t =>
          t.tempId === tempId
            ? { ...t, thumbnailUrl, size }
            : t
        ));
    } catch (error) {
      console.error('Failed to process image:', error);
      this.snackbarService.error(`Failed to process ${file.name}`);
    }
  }

  private async generateImageThumbnail(file: File): Promise<{ thumbnailUrl: string, size: number }> {
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
              size: file.size

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

  private uploadFileInBackground(file: File, tempId: string) {
    this.thumbnails.update(list =>
      list.map(t =>
        t.tempId === tempId
          ? { ...t, uploadStatus: 'uploading' as const }
          : t
      )
    );

    console.log("file upload started")
    this.fileUploadService.uploadImage(file).subscribe({
      next: (res) => {
        console.log("file upload ended")

        this.thumbnails.update(list =>
          list.map(t =>
            t.tempId === tempId
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
          t.tempId === tempId
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


  async compress() {
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

    this.isCompressing.set(true);

    const fileIds = this.thumbnails()
      .filter(t => t.fileId !== null)
      .map(t => t.fileId!);

    if (fileIds.length < 1) {
      return;
    }
    const request: CompressImageRequest = { fileIds, compressionLevel: this.compressionLevel() };
    this.imageService.compressImages(request).subscribe({
      next: response => {
        this.isCompressing.set(false);
        const blob = response.body as Blob;
        const contentDisposition = response.headers.get('content-disposition');
        let fileName = 'ConvertLab_CompressedImage';

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
        this.isCompressing.set(false);
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

  private async convertHeicToJpeg(file: File): Promise<File> {
    if (!file.name.toLowerCase().endsWith(".heic")) {
      return file; // already fine
    }

    const heic2any = (await import("heic2any")).default
    try {
      const jpegBlob = await heic2any({
        blob: file,
        toType: "image/jpeg",
        quality: 0.9
      });

      return new File(
        [jpegBlob as BlobPart],
        file.name.replace(/\.heic$/i, ".jpg"),
        { type: "image/jpeg" }
      );
    } catch (error) {
      console.error('HEIC conversion failed:', error);
      throw new Error('Failed to convert HEIC image');
    }
  }

  compressLevelChange($event: MatButtonToggleChange) {
    this.compressionLevel.set($event.value);
  }

  ngOnDestroy(): void {
    this.revokeThumbnailUrls();
    this.seoService.cleanup();
  }
}

