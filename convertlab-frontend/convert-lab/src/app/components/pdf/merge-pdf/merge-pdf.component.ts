import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CdkDrag, CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { ThumbnailComponent } from '../../shared/thumbnail/thumbnail.component';
import { FileUploaderComponent } from '../../shared/file-uploader/file-uploader.component';
import { FileUploadService } from '../../../services/file-upload.service';
import { MatIconModule } from '@angular/material/icon';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { Thumbnail } from '../../../models/thumbnail.model';
import { ActionButtonComponent } from '../../shared/action-button/action-button.component';
import { PdfService } from '../../../services/pdf.service';

@Component({
  selector: 'app-merge-pdf',
  imports: [
    ThumbnailComponent,
    FileUploaderComponent,
    MatIconModule,
    DragDropModule,
    CdkDrag,
    ActionButtonComponent
  ],
  templateUrl: './merge-pdf.component.html',
  styleUrl: './merge-pdf.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MergePdfComponent {

  private readonly fileUploadService = inject(FileUploadService);
  private readonly pdfService = inject(PdfService);

  thumbnails = signal<Thumbnail[]>([]);
  isUploading = signal(false);
  isMerging = signal(false);

  onFileUploaded(file: File | null) {
    if (!file) return;

    this.isUploading.set(true);
    this.fileUploadService.uploadPdf(file).subscribe({
      next: res => {
        this.thumbnails.update(list => [...list, res.data]);
        this.isUploading.set(false);
      },
      error: () => this.isUploading.set(false)
    });
  }

  removePdf(id: string) {
    this.thumbnails.update(list =>
      list.filter(t => t.fileId !== id)
    );
  }

  onDrop(event: CdkDragDrop<Thumbnail[]>) {
    this.thumbnails.update(list => {
      const updated = [...list];
      moveItemInArray(updated, event.previousIndex, event.currentIndex);
      return updated;
    });
  }

  merge() {
    const fileIds = this.thumbnails().map(t => t.fileId);

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
}
