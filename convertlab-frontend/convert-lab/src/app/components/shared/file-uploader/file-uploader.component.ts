import { Component, Input, Output, EventEmitter, signal, input, computed, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActionButtonComponent } from '../action-button/action-button.component';
import { MatProgressBar } from '@angular/material/progress-bar';

@Component({
  selector: 'app-file-uploader',
  imports: [MatButtonModule, MatIconModule, CommonModule, ActionButtonComponent,
    MatProgressBar
  ],
  templateUrl: './file-uploader.component.html',
  styleUrl: './file-uploader.component.scss',
})
export class FileUploaderComponent {

  // /** Allowed file types e.g. ['pdf', 'png', 'jpeg'] */
  allowedTypes = input<string[]>(['pdf']);

  allowedTypesAttr = computed(() => this.allowedTypes().length > 0 ? this.allowedTypes().map(t => '.' + t).join(',') : null)


  onFileSelected = output<File | null>();
  fileRemoved = output<void>();

  isDragging = signal(false);
  errorMessage = signal('');
  selectedFile = signal<File | null>(null);
  selectedFileName = computed(() => this.selectedFile()?.name);
  isUploading = input(false);

  /** Validate and emit file */
  handleFile(file: File) {
    ;
    const extension = file.name.split('.').pop()?.toLowerCase();
    if (this.allowedTypes().length > 0 && !this.allowedTypes().includes(extension!)) {
      this.errorMessage.set(`Only ${this.allowedTypes().join(', ')} files are allowed`);
      this.onFileSelected.emit(null);
      this.selectedFile.set(null);
      return;
    }

    this.errorMessage.set('');
    this.selectedFile.set(file);
    this.onFileSelected.emit(file);
  }

  /** Trigger manually from input */
  onFileInput(event: any) {
    const file = event.target.files?.[0];
    if (file) this.handleFile(file);
  }

  /** Drag events */
  onDragOver(event: DragEvent) {
    event.preventDefault();
    this.isDragging.set(true);
  }

  onDragLeave() {
    this.isDragging.set(false);
  }

  onDrop(event: DragEvent) {
    event.preventDefault();
    this.isDragging.set(false);

    const file = event.dataTransfer?.files?.[0];
    if (file) this.handleFile(file);
  }

  removeFile() {
    this.selectedFile.set(null);
    // this.uploadProgress.set(0);
    // this.isUploading.set(false);
    this.fileRemoved.emit();
  }
}
