import { ChangeDetectionStrategy, Component, computed, inject, signal, viewChild } from '@angular/core';
import { CdkDrag, CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { ThumbnailComponent } from '../../shared/thumbnail/thumbnail.component';
import { FileUploaderComponent } from '../../shared/file-uploader/file-uploader.component';
import { FileUploadService } from '../../../services/file-upload.service';
import { MatIconModule } from '@angular/material/icon';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { Thumbnail } from '../../../models/thumbnail.model';
import { ActionButtonComponent } from '../../shared/action-button/action-button.component';
import { PdfService } from '../../../services/pdf.service';
import { PdfMetadata, ThumbnailGeneratorService } from '../../../services/thumbnail-generator.service';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { SnackbarService } from '../../../services/snackbar.service';
import { SeoService } from '../../../seo/seo.service';

@Component({
  selector: 'app-merge-pdf',
  imports: [
    ThumbnailComponent,
    FileUploaderComponent,
    MatIconModule,
    DragDropModule,
    CdkDrag,
    ActionButtonComponent,
    MatTooltipModule,
    MatButtonModule
  ],
  templateUrl: './merge-pdf.component.html',
  styleUrl: './merge-pdf.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MergePdfComponent {
  private readonly fileUploadService = inject(FileUploadService);
  private readonly pdfService = inject(PdfService);
  private readonly thumbnailGeneratorService = inject(ThumbnailGeneratorService);
  private readonly snackbarService = inject(SnackbarService);
  private seoService = inject(SeoService);

  thumbnails = signal<Thumbnail[]>([]);
  isMerging = signal(false);
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

  canMerge = computed(() =>
    this.thumbnails().length > 1 &&
    !this.isMerging() &&
    !this.isWaitingForUploads()
  );

  mergeButtonLabel = computed(() => {
    if (this.isMerging()) return 'Merging...';
    if (this.isWaitingForUploads()) return 'Uploading...';
    return 'Merge PDF';
  });

  fileUploader = viewChild(FileUploaderComponent);

  ngOnInit() {
    this.seoService.applySEO('merge-pdf');
  }

  async onFilesUploaded(files: File[] | null) {
    if (!files || files.length === 0) return;

    for (const file of files) {
      // Generate unique temporary ID per file
      const tempId = `temp-${Date.now()}-${Math.random()}`;

      this.addPlaceholderThumbnail(file, tempId);
      this.generateThumbnail(file, tempId);
      this.uploadFileInBackground(file, tempId);
    }

    // Clear file input once after all files are processed
    this.fileUploader()?.removeFile();
  }

  /**
   * Step 1: Add placeholder thumbnail immediately
   */
  private addPlaceholderThumbnail(file: File, tempId: string): void {
    const placeholder: Thumbnail = {
      fileId: null,
      fileName: file.name,
      pageCount: 0,
      thumbnailUrl: '', // Empty for now
      uploadStatus: 'pending',
      file,
      tempId
    };

    this.thumbnails.update(list => [...list, placeholder]);
  }

  /**
   * Step 2: Generate thumbnail and update the placeholder
   */
  private async generateThumbnail(file: File, tempId: string): Promise<void> {
    try {
      const { thumbnailUrl, pageCount }: PdfMetadata =
        await this.thumbnailGeneratorService.getPdfInfo(file);

      // Update the existing placeholder with thumbnail data
      this.thumbnails.update(list =>
        list.map(t =>
          t.tempId === tempId
            ? { ...t, thumbnailUrl, pageCount }
            : t
        )
      );
    } catch (error) {
      console.error(`Failed to generate thumbnail for ${file.name}`, error);

      // Mark thumbnail generation as failed (but keep trying upload)
      this.thumbnails.update(list =>
        list.map(t =>
          t.tempId === tempId
            ? {
              ...t,
              error: 'Thumbnail generation failed',
              // Don't mark as 'failed' yet, upload might succeed
            }
            : t
        )
      );
    }
  }

  /**
   * Step 3: Upload file and update the placeholder
   */
  private uploadFileInBackground(file: File, tempId: string): void {
    // Update status to uploading
    this.thumbnails.update(list =>
      list.map(t =>
        t.tempId === tempId
          ? { ...t, uploadStatus: 'uploading' as const }
          : t
      )
    );

    this.fileUploadService.uploadPdf(file).subscribe({
      next: (res) => {
        // Update with backend response
        this.thumbnails.update(list =>
          list.map(t =>
            t.tempId === tempId
              ? {
                ...t,
                fileId: res.data.fileId,
                pageCount: res.data.pageCount, // Backend page count (more reliable)
                uploadStatus: 'completed'
              }
              : t
          )
        );
      },
      error: (err) => {
        this.thumbnails.update(list =>
          list.map(t =>
            t.tempId === tempId
              ? {
                ...t,
                uploadStatus: 'failed',
                error: err.message || 'Upload failed'
              }
              : t
          )
        );
      }
    });
  }

  removePdf(id: string) {
    const thumbnail = this.thumbnails().find(t =>
      t.fileId === id || t.thumbnailUrl === id
    );

    if (thumbnail?.thumbnailUrl && thumbnail.thumbnailUrl.startsWith('blob:')) {
      this.thumbnailGeneratorService.revokeThumbnailUrl(thumbnail.thumbnailUrl);
    }

    this.thumbnails.update(list =>
      list.filter(t => t.fileId !== id && t.thumbnailUrl !== id)
    );
  }

  retryUpload(id: string | null) {
    if (!id) return;

    const thumbnail = this.thumbnails().find(t => t.fileId === id || t.tempId === id);

    if (thumbnail && thumbnail.file && thumbnail.uploadStatus === 'failed') {
      // Generate new tempId for retry
      const newTempId = thumbnail.tempId || `temp-${Date.now()}-${Math.random()}`;

      // Retry both thumbnail generation and upload
      this.generateThumbnail(thumbnail.file, newTempId);
      this.uploadFileInBackground(thumbnail.file, newTempId);
    }
  }

  onDrop(event: CdkDragDrop<Thumbnail[]>) {
    this.thumbnails.update(list => {
      const updated = [...list];
      moveItemInArray(updated, event.previousIndex, event.currentIndex);
      return updated;
    });
  }

  async merge() {
    if (this.thumbnails().length < 2) {
      return;
    }

    // Check if any uploads are still in progress
    if (this.isAnyUploading()) {
      this.isWaitingForUploads.set(true);

      // Wait for all uploads to complete
      await this.waitForUploadsToComplete();

      this.isWaitingForUploads.set(false);
    }

    // Check if any uploads failed
    if (this.hasFailedUploads()) {
      this.snackbarService.error('One or more uploads failed. Please re-upload and try again.');
      return;
    }

    const fileIds = this.thumbnails()
      .filter(t => t.fileId !== null)
      .map(t => t.fileId!);

    if (fileIds.length < 2) {
      return;
    }

    this.isMerging.set(true);
    this.pdfService.mergePdfs({ fileIds }).subscribe({
      next: response => {
        this.isMerging.set(false);
        const blob = response.body as Blob;
        const contentDisposition = response.headers.get('content-disposition');
        let fileName = 'ConvertLab_Merge.pdf';

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
        this.isMerging.set(false);
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
      }, 100); // Check every 100ms
    });
  }

  private revokeThumbnailUrls(): void {
    this.thumbnails().forEach(thumbnail => {
      if (thumbnail.thumbnailUrl.startsWith('blob:')) {
        this.thumbnailGeneratorService.revokeThumbnailUrl(thumbnail.thumbnailUrl);
      }
    });
  }

  ngOnDestroy(): void {
    this.revokeThumbnailUrls();
    this.seoService.cleanup();
  }
}
