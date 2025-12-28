import { Component, inject, signal } from '@angular/core';
import { FileUploaderComponent } from '../../shared/file-uploader/file-uploader.component';
import { PageRangeInputComponent } from '../../shared/page-range-input/page-range-input.component';
import { ActionButtonComponent } from '../../shared/action-button/action-button.component';
import { FormsModule } from '@angular/forms';
import { FileUploadService } from '../../../services/file-upload.service';
import { PdfService } from '../../../services/pdf.service';
import { MatButtonToggleChange, MatButtonToggleModule } from '@angular/material/button-toggle';
import { ThumbnailComponent } from '../../shared/thumbnail/thumbnail.component';
import { ThumbnailGeneratorService } from '../../../services/thumbnail-generator.service';
import { MatIconModule } from '@angular/material/icon';
import { SplitType } from '../../../models/split-pdf.model';

@Component({
  selector: 'app-split-pdf',
  imports: [
    FileUploaderComponent,
    PageRangeInputComponent,
    ActionButtonComponent,
    FormsModule,
    MatButtonToggleModule,
    ThumbnailComponent,
    MatIconModule
  ],
  templateUrl: './split-pdf.component.html',
  styleUrl: './split-pdf.component.scss',
})
export class SplitPdfComponent {
  private readonly fileUploadService = inject(FileUploadService);
  private readonly pdfService = inject(PdfService);
  private readonly thumbnailGeneratorService = inject(ThumbnailGeneratorService);

  public uploadedFileId = signal<string | null>(null);
  public pageRange = signal<string>('');
  isUploading = signal(false);
  selectedFile = signal<File | null>(null);
  splitType = signal<SplitType>(SplitType.EACH_PAGE);
  isSplitting = signal(false);

  // Thumbnail data
  thumbnailUrl = signal<string>('');
  pageCount = signal<number>(0);
  fileName = signal<string>('');

  // Show/hide page range input based on split type
  showPageRangeInput = signal(false);

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

  split(): void {
    if (this.uploadedFileId() == null || this.selectedFile() == null) return;

    // Validate page range if split by range
    if (this.splitType() === SplitType.BY_RANGE && !this.pageRange()) {
      return;
    }

    this.isSplitting.set(true);
    this.pdfService
      .splitPdf({
        fileId: this.uploadedFileId()!,
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

  splitTypeChange($event: MatButtonToggleChange) {
    this.splitType.set($event.value);
    // Show/hide page range input based on split type
    this.showPageRangeInput.set($event.value === SplitType.BY_RANGE);
    // Clear page range when switching to "each page"
    if ($event.value === SplitType.EACH_PAGE) {
      this.pageRange.set('');
    }
  }

  ngOnDestroy(): void {
    // Clean up thumbnail URL on component destroy
    if (this.thumbnailUrl()) {
      this.thumbnailGeneratorService.revokeThumbnailUrl(this.thumbnailUrl());
    }
  }
}
