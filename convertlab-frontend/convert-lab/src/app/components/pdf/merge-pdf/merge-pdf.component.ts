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
    return 'Merge2';
  });

  fileUploader = viewChild(FileUploaderComponent);

  constructor() {

  }

  async onFilesUploaded(files: File[] | null) {
    if (!files || files.length === 0) return;

    for (const file of files) {
      // Generate unique temporary ID per file
      const tempId = `temp-${Date.now()}-${Math.random()}`;
      this.generateThumbnail(file, tempId);
      // Start background upload for this file
      this.uploadFileInBackground(file, tempId);
    }
    // Clear file input once after all files are processed
    this.fileUploader()?.removeFile();
  }

  private async generateThumbnail(file: File, tempId: string) {
    try {
      const { thumbnailUrl, pageCount }: PdfMetadata = await this.thumbnailGeneratorService.getPdfInfo(file);
      console.log("after thumbnail generation")

      const thumbnail: Thumbnail = {
        fileId: tempId,
        pageCount,
        fileName: file.name,
        thumbnailUrl,
        uploadStatus: 'pending',
        file,
        tempId: tempId
      };

      // Add thumbnail immediately
      this.thumbnails.update(list => [...list, thumbnail]);
    } catch (error) {
      console.error(`Failed to generate thumbnail for ${file.name}`, error);

      // Optional: show failed thumbnail entry
      this.thumbnails.update(list => [
        ...list,
        {
          fileId: tempId,
          fileName: file.name,
          uploadStatus: 'failed',
          error: 'Thumbnail generation failed'
        } as Thumbnail
      ]);
    }
  }

  private uploadFileInBackground(file: File, tempId: string) {
    // Update status to uploading
    this.thumbnails.update(list =>
      list.map(t =>
        t.tempId === tempId
          ? { ...t, uploadStatus: 'uploading' }
          : t
      )
    );

    this.fileUploadService.uploadPdf(file).subscribe({
      next: (res) => {
        this.thumbnails.update(list =>
          list.map(t =>
            t.tempId === tempId
              ? {
                ...t,
                fileId: res.data.fileId,
                pageCount: res.data.pageCount,
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

  removePdf(id: string) {
    const thumbnail = this.thumbnails().find(t =>
      t.fileId === id || t.thumbnailUrl === id
    );

    if (thumbnail?.thumbnailUrl.startsWith('blob:')) {
      this.thumbnailGeneratorService.revokeThumbnailUrl(thumbnail.thumbnailUrl);
    }

    this.thumbnails.update(list =>
      list.filter(t => t.fileId !== id && t.thumbnailUrl !== id)
    );
  }

  retryUpload(id: string | null) {
    if (!id) {
      return;
    }
    const thumbnail = this.thumbnails().find(t => t.fileId === id);

    if (thumbnail && thumbnail.file && thumbnail.uploadStatus === 'failed') {
      this.uploadFileInBackground(thumbnail.file, id);
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
  }
}
