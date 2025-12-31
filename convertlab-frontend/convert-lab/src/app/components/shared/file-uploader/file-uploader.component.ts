import {
  Component,
  signal,
  input,
  computed,
  output,
  viewChild,
  ElementRef,
  inject,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { ActionButtonComponent } from '../action-button/action-button.component';
import { MatProgressBar } from '@angular/material/progress-bar';
import { FileValidationService } from '../../../services/file-validation.service';

@Component({
  selector: 'app-file-uploader',
  imports: [MatButtonModule, MatIconModule, CommonModule, ActionButtonComponent, MatProgressBar],
  templateUrl: './file-uploader.component.html',
  styleUrl: './file-uploader.component.scss',
})
export class FileUploaderComponent {
  private readonly validationService = inject(FileValidationService);

  /** Allowed file types e.g. ['pdf', 'png', 'jpeg'] */
  allowedTypes = input<string[]>(['pdf']);

  /** Enable multiple file selection */
  multiple = input<boolean>(false);

  allowedTypesAttr = computed(() =>
    this.allowedTypes().length > 0 ? this.allowedTypes().map((t) => '.' + t).join(',') : null
  );

  // Outputs
  onFileSelected = output<File | null>(); // For single file (backward compatibility)
  onFilesSelected = output<File[] | null>(); // For multiple files
  fileRemoved = output<void>();

  isDragging = signal(false);
  errorMessage = signal('');
  selectedFiles = signal<File[]>([]);
  selectedFileName = computed(() => {
    const files = this.selectedFiles();
    if (files.length === 0) return null;
    if (files.length === 1) return files[0].name;
    return `${files.length} files selected`;
  });
  isUploading = input(false);
  fileInput = viewChild<ElementRef>('fileInput');

  // Display validation constraints
  validationInfo = computed(() => this.validationService.getConstraintsDescription());

  /** Validate and emit file(s) */
  handleFiles(files: FileList | null) {
    if (!files || files.length === 0) return;

    // Clear previous errors
    this.errorMessage.set('');

    const fileArray = Array.from(files);

    // Validate all files
    // const validationResult = this.validationService.validateFiles(fileArray);

    // if (!validationResult.valid) {
    //   this.onFileSelected.emit(null);
    //   this.onFilesSelected.emit(null);
    //   this.clearFileInput();
    //   this.errorMessage.set(validationResult.errors.join('. '));
    //   return;
    // }

    // Additional extension check for all files
    const invalidFiles = fileArray.filter(file => {
      const extension = file.name.split('.').pop()?.toLowerCase();
      return this.allowedTypes().length > 0 && !this.allowedTypes().includes(extension!);
    });

    if (invalidFiles.length > 0) {
      this.errorMessage.set(`Only ${this.allowedTypes().join(', ')} files are allowed`);
      this.onFileSelected.emit(null);
      this.onFilesSelected.emit(null);
      this.clearFileInput();
      return;
    }

    this.selectedFiles.set(fileArray);

    // Emit based on mode
    if (this.multiple()) {
      this.onFilesSelected.emit(fileArray);
    } else {
      this.onFileSelected.emit(fileArray[0]);
    }
  }

  /** Trigger manually from input */
  onFileInput(event: any) {
    const files = event.target.files;
    if (files) this.handleFiles(files);
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

    const files = event.dataTransfer?.files;
    if (files) this.handleFiles(files);
  }

  removeFile() {
    this.clearFileInput();
    this.fileRemoved.emit();
  }

  clearFileInput() {
    if (this.fileInput()) {
      this.fileInput()!.nativeElement.value = '';
    }
    this.selectedFiles.set([]);
    this.errorMessage.set('');
  }
}
