import { Component, computed, inject, signal } from '@angular/core';
import { FileUploaderComponent } from "../../shared/file-uploader/file-uploader.component";
import { PageRangeInputComponent } from '../../shared/page-range-input/page-range-input.component';
import { ActionButtonComponent } from '../../shared/action-button/action-button.component';
import { FormsModule } from '@angular/forms';
import { FileUploadService } from '../../../services/file-upload.service';
import { PdfService } from '../../../services/pdf.service';
import { MatButtonToggleChange, MatButtonToggleModule } from '@angular/material/button-toggle';
import { ActionType } from '../../../models/extract-pdf.model';
import { ThumbnailComponent } from '../../shared/thumbnail/thumbnail.component';
import { PdfMetadata, ThumbnailGeneratorService } from '../../../services/thumbnail-generator.service';
import { SeoService } from '../../../seo/seo.service';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { Thumbnail } from '../../../models/thumbnail.model';
import { SnackbarService } from '../../../services/snackbar.service';

@Component({
  selector: 'app-extract-page',
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
  templateUrl: './extract-page.component.html',
  styleUrl: './extract-page.component.scss',
})
export class ExtractPageComponent {

  private readonly fileUploadService = inject(FileUploadService);
  private readonly extractPdfService = inject(PdfService);
  private readonly thumbnailGeneratorService = inject(ThumbnailGeneratorService);
  private seoService = inject(SeoService);
  private readonly snackbarService = inject(SnackbarService);

  public uploadedFileId = signal<string | null>(null);
  public pageRange = signal<string>('');
  isUploading = signal(false);
  selectedFile = signal<File | null>(null);
  actionType = signal<ActionType>(ActionType.KEEP);
  isExtracting = signal(false);
  isWaitingForUpload = signal(false);

  thumbnail = signal<Thumbnail | null>(null);

  // Computed states
  uploadCompleted = computed(() =>
    this.uploadedFileId() !== null && !this.isUploading()
  );

  canExtract = computed(() => (
    this.thumbnail()
    && !this.isExtracting()
    && !this.isWaitingForUpload()
    && this.pageRange().trim() !== ''
  ));

  extractButtonLabel = computed(() => {
    if (this.isExtracting()) return 'Extracting...';
    if (this.isWaitingForUpload()) return 'Uploading...';
    return 'Extract2';
  });

  hasFailedUploads = computed(() => this.thumbnail()?.uploadStatus === 'failed')


  ngOnInit() {
    this.seoService.applySEO('extract-pdf');
  }


  async onFileUploaded(file: File | null) {
    if (!file) return;

    const tempId = `temp-${Date.now()}-${Math.random()}`;

    this.addPlaceholderThumbnail(file, tempId);
    this.generateThumbnail(file, tempId);
    this.uploadFileInBackground(file, tempId);

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

  async extract(): Promise<void> {
    if (!this.thumbnail()) return;

    // Validate page range if split by range
    if (!this.pageRange()) {
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

    this.isExtracting.set(true);
    this.extractPdfService.extractPdf({
      fileId: this.thumbnail()!.fileId!,
      pageRange: this.pageRange(),
      actionType: this.actionType()
    })
      .subscribe({
        next: response => {
          this.isExtracting.set(false);
          const blob = (response.body!) as Blob;
          const contentDisposition = response.headers.get('content-disposition');
          let fileName = 'downloaded-file';
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
          this.isExtracting.set(false);
        }
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

  actionTypeChange($event: MatButtonToggleChange) {
    this.actionType.set($event.value);
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
