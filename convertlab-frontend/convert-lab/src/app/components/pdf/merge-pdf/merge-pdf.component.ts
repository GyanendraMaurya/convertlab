import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ThumbnailComponent } from '../../shared/thumbnail/thumbnail.component';
import { Thumbnail } from '../../../models/thumbnail.model';
import { FileUploaderComponent } from '../../shared/file-uploader/file-uploader.component';
import { FileUploadService } from '../../../services/file-upload.service';

@Component({
  selector: 'app-merge-pdf',
  imports: [ThumbnailComponent, FileUploaderComponent],
  templateUrl: './merge-pdf.component.html',
  styleUrl: './merge-pdf.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MergePdfComponent {

  private readonly fileUploadService = inject(FileUploadService);

  thumbnails = signal<Thumbnail[]>([]);
  isUploading = signal(false); // Set to true when a file is being uploaded
  selectedFile = signal<File | null>(null);
  isExtracting = signal(false);


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
    this.thumbnails.update(thumbnails => thumbnails.filter(thumbnail => thumbnail.fileId !== id));
  }
}
