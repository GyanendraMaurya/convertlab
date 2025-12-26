import { Injectable } from '@angular/core';
import * as pdfjsLib from 'pdfjs-dist';

@Injectable({
  providedIn: 'root',
})
export class ThumbnailGeneratorService {

  /**
   * Generate thumbnail from PDF first page
   */
  async generateThumbnail(file: File): Promise<string> {
    return new Promise((resolve, reject) => {
      const fileReader = new FileReader();

      fileReader.onload = async (e) => {
        try {
          const typedArray = new Uint8Array(e.target!.result as ArrayBuffer);

          // Load PDF document
          const loadingTask = pdfjsLib.getDocument({ data: typedArray });
          const pdf = await loadingTask.promise;

          // Get first page
          const page = await pdf.getPage(1);

          // Set up canvas
          const scale = 1.5;
          const viewport = page.getViewport({ scale });

          const canvas = document.createElement('canvas');
          const context = canvas.getContext('2d')!;
          canvas.width = viewport.width;
          canvas.height = viewport.height;

          // Render page
          const renderContext = {
            canvasContext: context,
            viewport: viewport,
            canvas
          };

          await page.render(renderContext).promise;

          // Convert to blob URL
          canvas.toBlob((blob) => {
            if (blob) {
              const url = URL.createObjectURL(blob);
              resolve(url);
            } else {
              reject(new Error('Failed to create thumbnail blob'));
            }
          }, 'image/png');

        } catch (error) {
          console.error('Failed to generate thumbnail:', error);
          reject(error);
        }
      };

      fileReader.onerror = () => reject(new Error('Failed to read file'));
      fileReader.readAsArrayBuffer(file);
    });
  }

  /**
   * Get page count from PDF
   */
  async getPageCount(file: File): Promise<number> {
    return new Promise((resolve, reject) => {
      const fileReader = new FileReader();

      fileReader.onload = async (e) => {
        try {
          const typedArray = new Uint8Array(e.target!.result as ArrayBuffer);
          const loadingTask = pdfjsLib.getDocument({ data: typedArray });
          const pdf = await loadingTask.promise;
          resolve(pdf.numPages);
        } catch (error) {
          console.error('Failed to get page count:', error);
          resolve(1); // Default to 1 on error
        }
      };

      fileReader.onerror = () => {
        console.error('Failed to read file for page count');
        resolve(1);
      };

      fileReader.readAsArrayBuffer(file);
    });
  }

  /**
   * Revoke a blob URL to free memory
   */
  revokeThumbnailUrl(url: string): void {
    if (url && url.startsWith('blob:')) {
      URL.revokeObjectURL(url);
    }
  }
}
