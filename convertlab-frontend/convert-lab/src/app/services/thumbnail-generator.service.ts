import { Injectable } from '@angular/core';
import * as pdfjsLib from 'pdfjs-dist';
import { RenderParameters } from 'pdfjs-dist/types/src/display/api';
pdfjsLib.GlobalWorkerOptions.workerSrc =
  '/assets/pdfjs/pdf.worker.min.mjs';

export interface PdfMetadata {
  thumbnailUrl: string;
  pageCount: number;
}

@Injectable({
  providedIn: 'root',
})
export class ThumbnailGeneratorService {

  //A reference to hold the worker in memory
  private pdfWorker: any = null;

  private getWorker() {
    // If we already have a worker object, reuse it.
    // This prevents the browser from re-fetching the .mjs file.
    if (!this.pdfWorker) {
      this.pdfWorker = new pdfjsLib.PDFWorker();
    }
    return this.pdfWorker;
  }

  /**
   * Revoke a blob URL to free memory
   */
  revokeThumbnailUrl(url: string): void {
    if (url && url.startsWith('blob:')) {
      URL.revokeObjectURL(url);
    }
  }

  async getPdfInfo(file: File): Promise<PdfMetadata> {
    const typedArray = new Uint8Array(await file.arrayBuffer());

    // 3. Pass the cached worker to the loading task
    const loadingTask = pdfjsLib.getDocument({
      data: typedArray,
      worker: this.getWorker(), // Reuse the same worker instance
    });

    const pdf = await loadingTask.promise;
    const pageCount = pdf.numPages;

    // Generate thumbnail from first page
    const page = await pdf.getPage(1);
    const scale = 1.5;
    const viewport = page.getViewport({ scale });

    const canvas = document.createElement('canvas');
    const context = canvas.getContext('2d')!;
    canvas.width = viewport.width;
    canvas.height = viewport.height;

    await page.render({
      canvasContext: context,
      viewport: viewport,
    } as RenderParameters).promise;

    return new Promise((resolve) => {
      canvas.toBlob((blob) => {
        const url = blob ? URL.createObjectURL(blob) : '';
        resolve({ thumbnailUrl: url, pageCount });
      }, 'image/png');
    });
  }
}
