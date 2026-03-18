import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FileUploaderComponent } from '../../shared/file-uploader/file-uploader.component';
import { ActionButtonComponent } from '../../shared/action-button/action-button.component';
import { FileUploadService } from '../../../services/file-upload.service';
import { ImageService } from '../../../services/image.service';
import { SnackbarService } from '../../../services/snackbar.service';
import { FileValidationService } from '../../../services/file-validation.service';
import { SeoService } from '../../../seo/seo.service';
import { CropImageRequest, CropRect } from '../../../models/crop-image.model';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ImageCropEditorComponent } from '../image-crop-editor/image-crop-editor.component';

type PageState = 'idle' | 'uploading' | 'editor' | 'processing' | 'done';

@Component({
  selector: 'app-crop-image',
  imports: [
    FileUploaderComponent,
    ImageCropEditorComponent,
    ActionButtonComponent,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './crop-image.component.html',
  styleUrl:    './crop-image.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class CropImageComponent {
  private readonly fileUploadService  = inject(FileUploadService);
  private readonly imageService       = inject(ImageService);
  private readonly snackbarService    = inject(SnackbarService);
  private readonly seoService         = inject(SeoService);
  public  readonly fileValidationService = inject(FileValidationService);

  // ── State ──────────────────────────────────────────────────────────────────
  pageState     = signal<PageState>('idle');
  uploadedFileId = signal<string | null>(null);
  previewUrl    = signal<string | null>(null);

  // Latest crop data emitted by the editor
  private currentCrop      = signal<CropRect>({ x: 0, y: 0, width: 0, height: 0 });
  private currentRotation  = signal(0);
  private currentFlipH     = signal(false);
  private currentFlipV     = signal(false);
  private naturalWidth     = signal(0);
  private naturalHeight    = signal(0);

  // ── Computed ───────────────────────────────────────────────────────────────
  isUploading  = computed(() => this.pageState() === 'uploading');
  isProcessing = computed(() => this.pageState() === 'processing');
  showEditor   = computed(() => this.pageState() === 'editor' || this.pageState() === 'processing');

  canProcess = computed(() =>
    this.pageState() === 'editor' &&
    !!this.uploadedFileId() &&
    this.currentCrop().width > 0 &&
    this.currentCrop().height > 0
  );

  actionLabel = computed(() => {
    if (this.isProcessing()) return 'Processing…';
    if (this.isUploading())  return 'Uploading…';
    return 'Crop & Download';
  });

  allowedTypes = computed(() =>
    this.fileValidationService.getConstraints('image').allowedExtensions
  );

  private cropEditor = viewChild(ImageCropEditorComponent);

  ngOnInit() {
    // SEO will be added to seo.config when merging
  }

  // ── Upload flow ────────────────────────────────────────────────────────────

  onFileSelected(file: File | null) {
    if (!file) return;
    this.resetEditor();
    this.pageState.set('uploading');

    // Create preview URL immediately for snappy UI
    const objectUrl = URL.createObjectURL(file);
    this.previewUrl.set(objectUrl);

    this.fileUploadService.uploadImage(file).subscribe({
      next: res => {
        this.uploadedFileId.set(res.data.fileId);
        this.pageState.set('editor');
      },
      error: () => {
        this.pageState.set('idle');
        URL.revokeObjectURL(objectUrl);
        this.previewUrl.set(null);
      },
    });
  }

  // ── Crop change from editor ────────────────────────────────────────────────

  onCropChanged(event: {
    cropRect: CropRect;
    rotation: number;
    flipH: boolean;
    flipV: boolean;
    naturalWidth: number;
    naturalHeight: number;
  }) {
    this.currentCrop.set(event.cropRect);
    this.currentRotation.set(event.rotation);
    this.currentFlipH.set(event.flipH);
    this.currentFlipV.set(event.flipV);
    this.naturalWidth.set(event.naturalWidth);
    this.naturalHeight.set(event.naturalHeight);
  }

  // ── Process ────────────────────────────────────────────────────────────────

  process() {
    if (!this.canProcess()) return;

    const crop = this.currentCrop();
    const request: CropImageRequest = {
      fileId:          this.uploadedFileId()!,
      x:               crop.x,
      y:               crop.y,
      width:           crop.width,
      height:          crop.height,
      rotation:        this.currentRotation(),
      flipHorizontal:  this.currentFlipH(),
      flipVertical:    this.currentFlipV(),
      outputFormat:    'JPEG',
      quality:         92,
    };

    this.pageState.set('processing');

    this.imageService.cropImage(request).subscribe({
      next: response => {
        this.pageState.set('editor'); // stay in editor so user can re-crop

        const blob = response.body as Blob;
        const contentDisposition = response.headers.get('content-disposition');
        let fileName = 'cropped_image.jpg';
        if (contentDisposition) {
          const match = contentDisposition.match(/filename="([^"]+)"/);
          if (match?.[1]) fileName = match[1];
        }

        const url = window.URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href     = url;
        a.download = fileName;
        a.click();
        URL.revokeObjectURL(url);

        this.snackbarService.success('Image cropped and downloaded!');
      },
      error: () => {
        this.pageState.set('editor');
      },
    });
  }

  // ── Reset ──────────────────────────────────────────────────────────────────

  resetEditor() {
    const prev = this.previewUrl();
    if (prev) URL.revokeObjectURL(prev);
    this.previewUrl.set(null);
    this.uploadedFileId.set(null);
    this.currentCrop.set({ x: 0, y: 0, width: 0, height: 0 });
    this.currentRotation.set(0);
    this.currentFlipH.set(false);
    this.currentFlipV.set(false);
  }

  changeImage() {
    this.resetEditor();
    this.pageState.set('idle');
  }

  ngOnDestroy() {
    this.resetEditor();
    this.seoService.cleanup();
  }
}