import { Component, inject, signal } from '@angular/core';
import { FileUploaderComponent } from "../../shared/file-uploader/file-uploader.component";
import { PageRangeInputComponent } from '../../shared/page-range-input/page-range-input.component';
import { ActionButtonComponent } from '../../shared/action-button/action-button.component';
import { HttpService } from '../../../services/http.service';
import { FormsModule } from '@angular/forms';
import { Observable } from 'rxjs';
import { HttpClient, HttpResponse } from '@angular/common/http';
import { FileUploadService } from '../../../services/file-upload.service';
import { ExtractPdfService } from '../../../services/extract-pdf.service';
import { MatButtonToggleChange, MatButtonToggleModule } from '@angular/material/button-toggle';
import { ActionType } from '../../../models/extract-pdf.model';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-extract-page',
  imports: [FileUploaderComponent, PageRangeInputComponent, ActionButtonComponent, FormsModule,
    MatButtonToggleModule
  ],
  templateUrl: './extract-page.component.html',
  styleUrl: './extract-page.component.scss',
})
export class ExtractPageComponent {

  private readonly http = inject(HttpService);
  private readonly httpClient = inject(HttpClient);
  private readonly fileUploadService = inject(FileUploadService);
  private readonly extractPdfService = inject(ExtractPdfService);
  private readonly _snackBar = inject(MatSnackBar);

  private uploadedFileId = signal<string | null>(null);
  public pageRange = signal<string>('');
  isUploading = signal(false); // Set to true when a file is being uploaded
  selectedFile = signal<File | null>(null);
  actionType = signal<ActionType>(ActionType.KEEP);



  onFileUploaded($event: File | null) {
    this.isUploading.set(true);
    this.selectedFile.set($event);
    this.fileUploadService.uploadPdf(this.selectedFile()!).subscribe({
      next: res => {
        this.uploadedFileId.set(res.fileId);
        this.isUploading.set(false);
      },
      error: err => {
        this.isUploading.set(false);
      }
    });
  }

  extract(): void {
    if (this.uploadedFileId() == null || this.selectedFile() == null) return;
    this.extractPdfService.extractPdf({ fileId: this.uploadedFileId()!, pageRange: this.pageRange(), actionType: this.actionType() })
      .subscribe({
        next: response => {
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
        }, error: err => {
          this._snackBar.open(err.error.message, 'Close', {
            duration: 5000,
            verticalPosition: 'top',
          });
        }
      });
  }

  actionTypeChange($event: MatButtonToggleChange) {
    console.log($event);
    this.actionType.set($event.value);
  }


}
