import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  output,
  signal,
  viewChild,
} from '@angular/core';

@Component({
  selector: 'app-upload-zone',
  standalone: true,
  templateUrl: './upload-zone.component.html',
  styleUrl: './upload-zone.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UploadZoneComponent {
  /** Emits the selected File to the parent */
  fileSelected = output<File>();

  isDragging = signal(false);

  private fileInputRef = viewChild<ElementRef<HTMLInputElement>>('fileInput');

  onZoneClick() {
    this.fileInputRef()?.nativeElement.click();
  }

  onFileInputChange(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      this.fileSelected.emit(file);
      input.value = ''; // reset so same file can be re-uploaded
    }
  }

  @HostListener('dragover', ['$event'])
  onDragOver(e: DragEvent) {
    e.preventDefault();
    this.isDragging.set(true);
  }

  @HostListener('dragleave')
  onDragLeave() {
    this.isDragging.set(false);
  }

  @HostListener('drop', ['$event'])
  onDrop(e: DragEvent) {
    e.preventDefault();
    this.isDragging.set(false);
    const file = e.dataTransfer?.files[0];
    if (file) this.fileSelected.emit(file);
  }
}
