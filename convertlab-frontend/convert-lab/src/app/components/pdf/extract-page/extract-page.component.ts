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
import { ThumbnailGeneratorService } from '../../../services/thumbnail-generator.service';

@Component({
  selector: 'app-extract-page',
  imports: [
    FileUploaderComponent,
    PageRangeInputComponent,
    ActionButtonComponent,
    FormsModule,
    MatButtonToggleModule,
    ThumbnailComponent
  ],
  templateUrl: './extract-page.component.html',
  styleUrl: './extract-page.component.scss',
})
export class ExtractPageComponent {

  private readonly fileUploadService = inject(FileUploadService);
  private readonly extractPdfService = inject(PdfService);
  private readonly thumbnailGeneratorService = inject(ThumbnailGeneratorService);

  public uploadedFileId = signal<string | null>(null);
  public pageRange = signal<string>('');
  isUploading = signal(false);
  selectedFile = signal<File | null>(null);
  actionType = signal<ActionType>(ActionType.KEEP);
  isExtracting = signal(false);
  isWaitingForUpload = signal(false);

  // Thumbnail data
  thumbnailUrl = signal<string>('');
  pageCount = signal<number>(0);
  fileName = signal<string>('');

  // Computed states
  uploadCompleted = computed(() =>
    this.uploadedFileId() !== null && !this.isUploading()
  );

  canExtract = computed(() =>
    !this.isExtracting() && !this.isWaitingForUpload()
  );

  extractButtonLabel = computed(() => {
    if (this.isExtracting()) return 'Extracting...';
    if (this.isWaitingForUpload()) return 'Uploading...';
    return 'Extract';
  });

  async onFileUploaded($event: File | null) {
    if (!$event) return;

    this.isUploading.set(true);
    this.selectedFile.set($event);

    try {
      // Generate thumbnail and get page count
      const { thumbnailUrl, pageCount } = await this.thumbnailGeneratorService.getPdfInfo($event);
      this.thumbnailUrl.set(thumbnailUrl);
      this.pageCount.set(pageCount);
      this.fileName.set($event.name);

      // Upload file to backend
      this.fileUploadService.uploadPdf($event).subscribe({
        next: res => {
          this.uploadedFileId.set(res.data.fileId);
          this.isUploading.set(false);
        },
        error: err => {
          this.isUploading.set(false);
          // Revoke thumbnail URL on error
          if (this.thumbnailUrl()) {
            this.thumbnailGeneratorService.revokeThumbnailUrl(this.thumbnailUrl());
          }
        }
      });
    } catch (error) {
      console.error('Failed to process PDF:', error);
      this.isUploading.set(false);
    }
  }

  onFileRemoved() {
    // Revoke thumbnail URL
    if (this.thumbnailUrl()) {
      this.thumbnailGeneratorService.revokeThumbnailUrl(this.thumbnailUrl());
    }

    // Reset all state
    this.selectedFile.set(null);
    this.uploadedFileId.set(null);
    this.thumbnailUrl.set('');
    this.pageCount.set(0);
    this.fileName.set('');
    this.pageRange.set('');
  }

  removeThumbnail() {
    this.onFileRemoved();
  }

  async extract(): Promise<void> {
    if (this.selectedFile() == null) return;

    // Check if upload is still in progress
    if (this.isUploading()) {
      this.isWaitingForUpload.set(true);

      // Wait for upload to complete
      await this.waitForUploadToComplete();

      this.isWaitingForUpload.set(false);
    }

    // Check if upload completed successfully
    if (this.uploadedFileId() == null) {
      return;
    }

    this.isExtracting.set(true);
    this.extractPdfService.extractPdf({
      fileId: this.uploadedFileId()!,
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

  ngOnDestroy(): void {
    // Clean up thumbnail URL on component destroy
    if (this.thumbnailUrl()) {
      this.thumbnailGeneratorService.revokeThumbnailUrl(this.thumbnailUrl());
    }
  }
}
