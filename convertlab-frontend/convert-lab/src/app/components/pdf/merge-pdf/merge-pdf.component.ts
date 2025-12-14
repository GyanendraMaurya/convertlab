import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { CdkDrag, CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { ThumbnailComponent } from '../../shared/thumbnail/thumbnail.component';
import { FileUploaderComponent } from '../../shared/file-uploader/file-uploader.component';
import { FileUploadService } from '../../../services/file-upload.service';
import { MatIconModule } from '@angular/material/icon';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { Thumbnail } from '../../../models/thumbnail.model';

@Component({
  selector: 'app-merge-pdf',
  imports: [
    ThumbnailComponent,
    FileUploaderComponent,
    MatIconModule,
    DragDropModule,
    CdkDrag
  ],
  templateUrl: './merge-pdf.component.html',
  styleUrl: './merge-pdf.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MergePdfComponent {

  private readonly fileUploadService = inject(FileUploadService);

  thumbnails = signal<Thumbnail[]>([]);
  isUploading = signal(false);

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
}
