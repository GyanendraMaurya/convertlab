import { Component, computed, inject, signal } from '@angular/core';
import { FileUploaderComponent } from '../../shared/file-uploader/file-uploader.component';
import { PageRangeInputComponent } from '../../shared/page-range-input/page-range-input.component';
import { ActionButtonComponent } from '../../shared/action-button/action-button.component';
import { FormsModule } from '@angular/forms';
import { FileUploadService } from '../../../services/file-upload.service';
import { PdfService } from '../../../services/pdf.service';
import { MatButtonToggleChange, MatButtonToggleModule } from '@angular/material/button-toggle';
import { SplitType } from '../../../models/split-pdf.model';
import { ThumbnailComponent } from '../../shared/thumbnail/thumbnail.component';
import { PdfMetadata, ThumbnailGeneratorService } from '../../../services/thumbnail-generator.service';
import { MatIconModule } from '@angular/material/icon';
import { SeoService } from '../../../seo/seo.service';
import { Thumbnail } from '../../../models/thumbnail.model';
import { SnackbarService } from '../../../services/snackbar.service';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-split-pdf',
  imports: [
    FileUploaderComponent,
    PageRangeInputComponent,
    ActionButtonComponent,
    FormsModule,
    MatButtonToggleModule,
    ThumbnailComponent,
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './split-pdf.component.html',
  styleUrl: './split-pdf.component.scss',
})
export class SplitPdfComponent {
  private readonly fileUploadService = inject(FileUploadService);
  private readonly pdfService = inject(PdfService);
  private readonly thumbnailGeneratorService = inject(ThumbnailGeneratorService);
  private seoService = inject(SeoService);
  private readonly snackbarService = inject(SnackbarService);

  public pageRange = signal<string>('');
  splitType = signal<SplitType>(SplitType.EACH_PAGE);
  isSplitting = signal(false);
  isWaitingForUpload = signal(false);
  isUploading = computed(() => this.thumbnail()?.uploadStatus === 'uploading' || this.thumbnail()?.uploadStatus === 'pending');

  thumbnail = signal<Thumbnail | null>(null);

  // Show/hide page range input based on split type
  showPageRangeInput = signal(false);


  canSplit = computed(() => {
    const uploadReady = this.thumbnail() && !this.isSplitting() && !this.isWaitingForUpload();

    // If split by range, also check if page range is provided
    if (this.splitType() === SplitType.BY_RANGE) {
      return uploadReady && this.pageRange().trim() !== '';
    }

    return uploadReady;
  });

  splitButtonLabel = computed(() => {
    if (this.isSplitting()) return 'Splitting...';
    if (this.isWaitingForUpload()) return 'Uploading...';
    return 'Split PDF';
  });

  hasFailedUploads = computed(() => this.thumbnail()?.uploadStatus === 'failed')

  ngOnInit() {
    this.seoService.applySEO('split-pdf');
  }


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

    this.thumbnail.set(placeholder);
  }

  private async generateThumbnail(file: File, tempId: string): Promise<void> {
    try {
      const { thumbnailUrl, pageCount }: PdfMetadata =
        await this.thumbnailGeneratorService.getPdfInfo(file);

      // Update the existing placeholder with thumbnail data
      this.thumbnail.update(t => (t && { ...t, thumbnailUrl, pageCount }));
    } catch (error) {
      console.error(`Failed to generate thumbnail for ${file.name}`, error);

      // Mark thumbnail generation as failed (but keep trying upload)
      this.thumbnail.update(t => (t && { ...t, error: 'Thumbnail generation failed' }));
    }
  }

  async onFileUploaded(file: File | null) {
    if (!file) return;

    const tempId = `temp-${Date.now()}-${Math.random()}`;

    this.addPlaceholderThumbnail(file, tempId);
    this.generateThumbnail(file, tempId);
    this.uploadFileInBackground(file, tempId);

  }

  private uploadFileInBackground(file: File, tempId: string): void {
    // Update status to uploading
    this.thumbnail.update(t => (t && { ...t, uploadStatus: 'uploading' as const }));

    this.fileUploadService.uploadPdf(file).subscribe({
      next: (res) => {
        // Update with backend response
        this.thumbnail.update(t => (t && {
          ...t,
          fileId: res.data.fileId,
          uploadStatus: 'completed'
        }));
      },
      error: (err) => {
        this.thumbnail.update(t => (t && {
          ...t,
          uploadStatus: 'failed',
          error: err.message || 'Upload failed'
        }));
      }
    });
  }


  onFileRemoved() {
    // Revoke thumbnail URL
    const thumbnail = this.thumbnail();

    if (thumbnail?.thumbnailUrl && thumbnail.thumbnailUrl.startsWith('blob:')) {
      this.thumbnailGeneratorService.revokeThumbnailUrl(thumbnail.thumbnailUrl);
    }

    this.thumbnail.set(null);
  }

  removePdf() {
    this.onFileRemoved();
  }

  async split(): Promise<void> {
    if (!this.thumbnail()) return;

    // Validate page range if split by range
    if (this.splitType() === SplitType.BY_RANGE && !this.pageRange()) {
      return;
    }

    // Check if upload is still in progress
    if (this.isUploading()) {
      this.isWaitingForUpload.set(true);

      // Wait for upload to complete
      await this.waitForUploadToComplete();

      this.isWaitingForUpload.set(false);
    }

    // Check if any uploads failed
    if (this.hasFailedUploads()) {
      this.snackbarService.error('Upload failed. Please re-upload and try again.');
      return;
    }

    this.isSplitting.set(true);
    this.pdfService
      .splitPdf({
        fileId: this.thumbnail()!.fileId!,
        pageRange: this.pageRange(),
        splitType: this.splitType(),
      })
      .subscribe({
        next: response => {
          this.isSplitting.set(false);
          const blob = response.body! as Blob;
          const contentDisposition = response.headers.get('content-disposition');
          let fileName = 'split-pdfs.zip';
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
        error: err => {
          this.isSplitting.set(false);
        },
      });
  }

  private waitForUploadToComplete(): Promise<void> {
    return new Promise((resolve) => {
      const checkInterval = setInterval(() => {
        if (!this.isUploading()) {
          clearInterval(checkInterval);
          resolve();
        }
      }, 100); // Check every 100ms
    });
  }

  splitTypeChange($event: MatButtonToggleChange) {
    this.splitType.set($event.value);
    // Show/hide page range input based on split type
    this.showPageRangeInput.set($event.value === SplitType.BY_RANGE);
    // Clear page range when switching to "each page"
    if ($event.value === SplitType.EACH_PAGE) {
      this.pageRange.set('');
    }
  }

  retryUpload(id: string | undefined) {
    if (!id) return;
    const thumbnail = this.thumbnail();
    if (thumbnail && thumbnail.file && thumbnail.uploadStatus === 'failed') {
      // Generate new tempId for retry
      const newTempId = thumbnail.tempId || `temp-${Date.now()}-${Math.random()}`;
      this.generateThumbnail(thumbnail.file, newTempId);
      this.uploadFileInBackground(thumbnail.file, newTempId);
    }
  }

  ngOnDestroy(): void {
    // Clean up thumbnail URL on component destroy
    if (this.thumbnail()) {
      this.thumbnailGeneratorService.revokeThumbnailUrl(this.thumbnail()!.thumbnailUrl);
    }
    this.seoService.cleanup();
  }
}
