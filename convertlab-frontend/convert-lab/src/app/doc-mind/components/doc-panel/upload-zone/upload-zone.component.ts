import {
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  HostListener,
  inject,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { FileType, FileValidationService, ValidationResult } from '../../../../services/file-validation.service';
import { SnackbarService } from '../../../../services/snackbar.service';

@Component({
  selector: 'app-upload-zone',
  standalone: true,
  templateUrl: './upload-zone.component.html',
  styleUrl: './upload-zone.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UploadZoneComponent {

  private readonly validationService = inject(FileValidationService);
  private readonly snackBarService = inject(SnackbarService);

  /** Emits the selected File to the parent */
  fileSelected = output<File>();

  isDragging = signal(false);

  private fileInputRef = viewChild<ElementRef<HTMLInputElement>>('fileInput');

  private fileType: FileType = 'pdf';
  readonly validationInfo = computed(() =>
    this.validationService.getConstraintsDescription(this.fileType)
  );

  onZoneClick() {
    this.fileInputRef()?.nativeElement.click();
  }

  async onFileInputChange(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    await this.emitValidFile(file);
    input.value = ''; // reset so same file can be re-uploaded
  }

  private async emitValidFile(file: File): Promise<void> {
    const validationResult: ValidationResult = await this.validationService.validateFiles([file], this.fileType);
    if (!validationResult.valid) {
      this.snackBarService.error(validationResult.errors.join('\n'));
      return;
    }

    this.fileSelected.emit(file);
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
  async onDrop(e: DragEvent) {
    e.preventDefault();
    this.isDragging.set(false);
    const file = e.dataTransfer?.files[0];
    if (file) await this.emitValidFile(file);
  }
}
