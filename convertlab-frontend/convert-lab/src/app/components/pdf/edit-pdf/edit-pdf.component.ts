import { CommonModule, isPlatformBrowser } from '@angular/common';
import {
  AfterViewInit,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  HostListener,
  OnDestroy,
  PLATFORM_ID,
  computed,
  inject,
  signal,
  viewChild,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import * as pdfjsLib from 'pdfjs-dist/legacy/build/pdf.mjs';
import { ActionButtonComponent } from '../../shared/action-button/action-button.component';
import { FileUploaderComponent } from '../../shared/file-uploader/file-uploader.component';
import { PdfEditAlignment, PdfEditFontFamily, PdfEditOperation, PdfEditRequest } from '../../../models/pdf-edit.model';
import { FileUploadService } from '../../../services/file-upload.service';
import { PdfService } from '../../../services/pdf.service';
import { SnackbarService } from '../../../services/snackbar.service';
import { SeoService } from '../../../seo/seo.service';

pdfjsLib.GlobalWorkerOptions.workerSrc = '/assets/pdfjs/pdf.worker.min.mjs';

interface EditorOperation extends PdfEditOperation {
  id: string;
}

interface TextProbe {
  x: number;
  y: number;
  width: number;
  height: number;
  fontSize: number;
  fontFamily: PdfEditFontFamily;
}

type DragMode = 'move' | 'resize';

@Component({
  selector: 'app-edit-pdf',
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatIconModule,
    MatTooltipModule,
    ActionButtonComponent,
    FileUploaderComponent,
  ],
  templateUrl: './edit-pdf.component.html',
  styleUrl: './edit-pdf.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EditPdfComponent implements AfterViewInit, OnDestroy {
  private readonly fileUploadService = inject(FileUploadService);
  private readonly pdfService = inject(PdfService);
  private readonly snackbarService = inject(SnackbarService);
  private readonly seoService = inject(SeoService);
  private readonly platformId = inject(PLATFORM_ID);

  readonly canvas = viewChild<ElementRef<HTMLCanvasElement>>('pdfCanvas');
  readonly fileUploader = viewChild(FileUploaderComponent);

  readonly fileName = signal('');
  readonly uploadedFileId = signal<string | null>(null);
  readonly uploadStatus = signal<'idle' | 'uploading' | 'completed' | 'failed'>('idle');
  readonly isRendering = signal(false);
  readonly isExporting = signal(false);
  readonly currentPage = signal(1);
  readonly pageCount = signal(0);
  readonly zoom = signal(1.2);
  readonly pageWidth = signal(0);
  readonly pageHeight = signal(0);
  readonly operations = signal<EditorOperation[]>([]);
  readonly selectedId = signal<string | null>(null);
  readonly currentText = signal('Replacement text');
  readonly fontFamily = signal<PdfEditFontFamily>('Helvetica');
  readonly fontSize = signal(14);
  readonly textColor = signal('#111111');
  readonly coverColor = signal('#ffffff');
  readonly coverEnabled = signal(true);
  readonly bold = signal(false);
  readonly italic = signal(false);
  readonly alignment = signal<PdfEditAlignment>('left');

  readonly canExport = computed(() =>
    this.uploadStatus() === 'completed' &&
    this.operations().length > 0 &&
    !this.isExporting()
  );

  readonly selectedOperation = computed(() =>
    this.operations().find(operation => operation.id === this.selectedId()) ?? null
  );

  readonly visibleOperations = computed(() =>
    this.operations().filter(operation => operation.pageNumber === this.currentPage())
  );

  readonly exportLabel = computed(() => {
    if (this.isExporting()) return 'Exporting...';
    if (this.uploadStatus() === 'uploading') return 'Uploading...';
    return 'Export PDF';
  });

  private pdfDocument: any = null;
  private pageTextProbes = new Map<number, TextProbe[]>();
  private undoStack: EditorOperation[][] = [];
  private redoStack: EditorOperation[][] = [];
  private dragState: {
    id: string;
    mode: DragMode;
    startX: number;
    startY: number;
    original: EditorOperation;
  } | null = null;

  ngAfterViewInit(): void {
    this.seoService.applySEO('edit-pdf');
  }

  async onFileSelected(file: File | null): Promise<void> {
    if (!file || !isPlatformBrowser(this.platformId)) return;

    this.resetEditor(file.name);
    this.fileUploader()?.removeFile();

    try {
      await this.loadPdf(file);
      this.uploadFile(file);
    } catch (error) {
      console.error('Unable to load PDF for editing', error);
      this.snackbarService.error('Unable to load this PDF for editing.');
      this.uploadStatus.set('failed');
    }
  }

  onQuickFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.onFileSelected(file);
    input.value = '';
  }

  async goToPage(pageNumber: number): Promise<void> {
    const nextPage = Math.min(Math.max(1, pageNumber), this.pageCount());
    if (nextPage === this.currentPage()) return;
    this.currentPage.set(nextPage);
    this.selectedId.set(null);
    await this.renderCurrentPage();
  }

  async zoomIn(): Promise<void> {
    this.zoom.set(Math.min(2.5, Number((this.zoom() + 0.1).toFixed(2))));
    await this.renderCurrentPage();
  }

  async zoomOut(): Promise<void> {
    this.zoom.set(Math.max(0.6, Number((this.zoom() - 0.1).toFixed(2))));
    await this.renderCurrentPage();
  }

  createEditFromPointer(event: MouseEvent): void {
    if (!this.pdfDocument || this.dragState || !this.isPageSurface(event.target)) return;

    const point = this.toPdfPoint(event);
    if (!point) return;

    const nearbyText = this.findNearestText(point.x, point.y);
    this.applyStyleDefaults(nearbyText);

    const width = nearbyText ? Math.max(nearbyText.width + 16, 120) : 180;
    const height = Math.max((nearbyText?.height ?? this.fontSize()) * 1.7, 32);

    this.pushUndo();
    const operation: EditorOperation = {
      id: this.createId(),
      pageNumber: this.currentPage(),
      x: point.x,
      y: point.y,
      width,
      height,
      text: this.currentText(),
      fontFamily: this.fontFamily(),
      fontSize: this.fontSize(),
      bold: this.bold(),
      italic: this.italic(),
      textColor: this.textColor(),
      coverColor: this.coverColor(),
      coverEnabled: this.coverEnabled(),
      alignment: this.alignment(),
    };

    this.operations.update(list => [...list, operation]);
    this.selectedId.set(operation.id);
  }

  selectOperation(event: MouseEvent, id: string): void {
    event.stopPropagation();
    this.selectedId.set(id);
    const operation = this.selectedOperation();
    if (!operation) return;
    this.currentText.set(operation.text);
    this.fontFamily.set(operation.fontFamily);
    this.fontSize.set(operation.fontSize);
    this.bold.set(operation.bold);
    this.italic.set(operation.italic);
    this.textColor.set(operation.textColor);
    this.coverColor.set(operation.coverColor);
    this.coverEnabled.set(operation.coverEnabled);
    this.alignment.set(operation.alignment);
  }

  startDrag(event: MouseEvent, operation: EditorOperation, mode: DragMode): void {
    event.stopPropagation();
    event.preventDefault();
    this.selectedId.set(operation.id);
    this.dragState = {
      id: operation.id,
      mode,
      startX: event.clientX,
      startY: event.clientY,
      original: { ...operation },
    };
    this.pushUndo();
  }

  @HostListener('window:mousemove', ['$event'])
  onWindowMouseMove(event: MouseEvent): void {
    if (!this.dragState) return;

    const dx = (event.clientX - this.dragState.startX) / this.displayScale();
    const dy = (event.clientY - this.dragState.startY) / this.displayScale();

    this.operations.update(list => list.map(operation => {
      if (operation.id !== this.dragState?.id) return operation;

      if (this.dragState.mode === 'resize') {
        return {
          ...operation,
          width: Math.max(24, this.dragState.original.width + dx),
          height: Math.max(18, this.dragState.original.height + dy),
        };
      }

      return {
        ...operation,
        x: this.clamp(this.dragState.original.x + dx, 0, this.pageWidth() - operation.width),
        y: this.clamp(this.dragState.original.y + dy, 0, this.pageHeight() - operation.height),
      };
    }));
  }

  @HostListener('window:mouseup')
  onWindowMouseUp(): void {
    this.dragState = null;
  }

  updateSelectedStyle(): void {
    const selectedId = this.selectedId();
    if (!selectedId) return;

    this.operations.update(list => list.map(operation =>
      operation.id === selectedId
        ? {
            ...operation,
            text: this.currentText(),
            fontFamily: this.fontFamily(),
            fontSize: this.fontSize(),
            bold: this.bold(),
            italic: this.italic(),
            textColor: this.textColor(),
            coverColor: this.coverColor(),
            coverEnabled: this.coverEnabled(),
            alignment: this.alignment(),
          }
        : operation
    ));
  }

  setFontFamily(value: string): void {
    if (value === 'Times' || value === 'Courier') {
      this.fontFamily.set(value);
    } else {
      this.fontFamily.set('Helvetica');
    }
    this.updateSelectedStyle();
  }

  deleteSelected(): void {
    const selectedId = this.selectedId();
    if (!selectedId) return;

    this.pushUndo();
    this.operations.update(list => list.filter(operation => operation.id !== selectedId));
    this.selectedId.set(null);
  }

  undo(): void {
    const previous = this.undoStack.pop();
    if (!previous) return;

    this.redoStack.push(this.cloneOperations(this.operations()));
    this.operations.set(previous);
    this.selectedId.set(null);
  }

  redo(): void {
    const next = this.redoStack.pop();
    if (!next) return;

    this.undoStack.push(this.cloneOperations(this.operations()));
    this.operations.set(next);
    this.selectedId.set(null);
  }

  async exportPdf(): Promise<void> {
    if (!this.canExport() || !this.uploadedFileId()) return;

    this.isExporting.set(true);
    const request: PdfEditRequest = {
      fileId: this.uploadedFileId()!,
      operations: this.operations().map(({ id, ...operation }) => operation),
    };

    this.pdfService.editPdf(request).subscribe({
      next: response => {
        this.isExporting.set(false);
        const blob = response.body as Blob;
        const contentDisposition = response.headers.get('content-disposition');
        let fileName = 'ConvertLab_Edited.pdf';
        const match = contentDisposition?.match(/filename="([^"]+)"/);
        if (match?.[1]) fileName = match[1];

        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => {
        this.isExporting.set(false);
      }
    });
  }

  displayScale(): number {
    return this.pageWidth() ? this.zoom() : 1;
  }

  displayLeft(operation: EditorOperation): number {
    return operation.x * this.displayScale();
  }

  displayTop(operation: EditorOperation): number {
    return operation.y * this.displayScale();
  }

  displayWidth(operation: EditorOperation): number {
    return operation.width * this.displayScale();
  }

  displayHeight(operation: EditorOperation): number {
    return operation.height * this.displayScale();
  }

  ngOnDestroy(): void {
    this.pdfDocument?.destroy?.();
    this.seoService.cleanup();
  }

  private async loadPdf(file: File): Promise<void> {
    const typedArray = new Uint8Array(await file.arrayBuffer());
    const loadingTask = pdfjsLib.getDocument({ data: typedArray });
    this.pdfDocument = await loadingTask.promise;
    this.pageCount.set(this.pdfDocument.numPages);
    this.currentPage.set(1);
    await this.renderCurrentPage();
  }

  private async renderCurrentPage(): Promise<void> {
    if (!this.pdfDocument || !this.canvas()) return;

    this.isRendering.set(true);
    const page = await this.pdfDocument.getPage(this.currentPage());
    const viewport = page.getViewport({ scale: this.zoom() });
    const baseViewport = page.getViewport({ scale: 1 });
    const canvas = this.canvas()!.nativeElement;
    const context = canvas.getContext('2d');

    if (!context) {
      this.isRendering.set(false);
      return;
    }

    canvas.width = viewport.width;
    canvas.height = viewport.height;
    this.pageWidth.set(baseViewport.width);
    this.pageHeight.set(baseViewport.height);

    await page.render({ canvasContext: context, viewport }).promise;
    await this.collectTextProbes(page);
    this.isRendering.set(false);
  }

  private async collectTextProbes(page: any): Promise<void> {
    if (this.pageTextProbes.has(this.currentPage())) return;

    const baseViewport = page.getViewport({ scale: 1 });
    const textContent = await page.getTextContent();
    const probes: TextProbe[] = textContent.items.map((item: any) => {
      const transform = item.transform ?? [1, 0, 0, 1, 0, 0];
      const fontSize = Math.max(8, Math.round(Math.hypot(transform[2], transform[3]) || Math.abs(transform[0]) || 14));
      const fontFamily = this.resolveProbeFont(item.fontName);

      return {
        x: transform[4],
        y: Math.max(0, baseViewport.height - transform[5] - fontSize),
        width: Math.max(item.width ?? 80, fontSize * 2),
        height: Math.max(item.height ?? fontSize, fontSize),
        fontSize,
        fontFamily,
      };
    });

    this.pageTextProbes.set(this.currentPage(), probes);
  }

  private findNearestText(x: number, y: number): TextProbe | null {
    const probes = this.pageTextProbes.get(this.currentPage()) ?? [];
    let nearest: TextProbe | null = null;
    let nearestDistance = Number.MAX_VALUE;

    for (const probe of probes) {
      const centerX = probe.x + probe.width / 2;
      const centerY = probe.y + probe.height / 2;
      const distance = Math.hypot(centerX - x, centerY - y);
      if (distance < nearestDistance) {
        nearest = probe;
        nearestDistance = distance;
      }
    }

    return nearestDistance < 90 ? nearest : null;
  }

  private applyStyleDefaults(probe: TextProbe | null): void {
    if (!probe) return;
    this.fontFamily.set(probe.fontFamily);
    this.fontSize.set(Math.max(8, Math.min(72, Math.round(probe.fontSize))));
  }

  private toPdfPoint(event: MouseEvent): { x: number; y: number } | null {
    const canvas = this.canvas()?.nativeElement;
    if (!canvas) return null;

    const rect = canvas.getBoundingClientRect();
    const x = (event.clientX - rect.left) / this.displayScale();
    const y = (event.clientY - rect.top) / this.displayScale();

    return {
      x: this.clamp(x, 0, this.pageWidth() - 24),
      y: this.clamp(y, 0, this.pageHeight() - 18),
    };
  }

  private uploadFile(file: File): void {
    this.uploadStatus.set('uploading');
    this.fileUploadService.uploadPdf(file).subscribe({
      next: response => {
        this.uploadedFileId.set(response.data.fileId);
        this.uploadStatus.set('completed');
      },
      error: error => {
        console.error('PDF upload failed', error);
        this.uploadStatus.set('failed');
        this.snackbarService.error('Upload failed. Please try again.');
      }
    });
  }

  private resetEditor(fileName: string): void {
    this.pdfDocument?.destroy?.();
    this.pdfDocument = null;
    this.pageTextProbes.clear();
    this.undoStack = [];
    this.redoStack = [];
    this.fileName.set(fileName);
    this.uploadedFileId.set(null);
    this.uploadStatus.set('idle');
    this.operations.set([]);
    this.selectedId.set(null);
  }

  private resolveProbeFont(fontName: string | undefined): PdfEditFontFamily {
    const normalized = (fontName ?? '').toLowerCase();
    if (normalized.includes('courier') || normalized.includes('mono')) return 'Courier';
    if (normalized.includes('times') || normalized.includes('serif')) return 'Times';
    return 'Helvetica';
  }

  private isPageSurface(target: EventTarget | null): boolean {
    const element = target as HTMLElement | null;
    return !!element?.classList.contains('editor-page') || element?.tagName === 'CANVAS';
  }

  private pushUndo(): void {
    this.undoStack.push(this.cloneOperations(this.operations()));
    this.redoStack = [];
  }

  private cloneOperations(operations: EditorOperation[]): EditorOperation[] {
    return operations.map(operation => ({ ...operation }));
  }

  private createId(): string {
    return `edit-${Date.now()}-${Math.random().toString(16).slice(2)}`;
  }

  private clamp(value: number, min: number, max: number): number {
    return Math.min(Math.max(value, min), Math.max(min, max));
  }
}
