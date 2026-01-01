import { Injectable } from '@angular/core';

export type FileType = 'pdf' | 'image';

export interface FileTypeConstraints {
  maxSizeBytes: number;
  minSizeBytes: number;
  allowedMimeTypes: string[];
  allowedExtensions: string[];
  maxPages?: number; // Only for PDFs
  maxDimension?: number; // Only for images (width or height)
}

export interface ValidationResult {
  valid: boolean;
  errors: string[];
}

@Injectable({
  providedIn: 'root',
})
export class FileValidationService {
  // Default constraints for different file types
  private readonly constraints: Record<FileType, FileTypeConstraints> = {
    pdf: {
      maxSizeBytes: 15 * 1024 * 1024, // 15MB
      minSizeBytes: 1024, // 1KB
      allowedMimeTypes: ['application/pdf'],
      allowedExtensions: ['pdf'],
      maxPages: 1000,
    },
    image: {
      maxSizeBytes: 10 * 1024 * 1024, // 10MB
      minSizeBytes: 1024, // 1KB
      allowedMimeTypes: [
        'image/png',
        'image/jpeg',
        'image/jpg',
        'image/gif',
        'image/bmp',
        'image/webp',
      ],
      allowedExtensions: ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'],
      maxDimension: 10000, // 10000px max width or height
    },
  };

  constructor() { }

  /**
   * Validate a single file based on its type
   */
  validateFile(file: File, fileType: FileType): ValidationResult {
    const errors: string[] = [];
    const config = this.constraints[fileType];

    // 1. Check if file exists
    if (!file) {
      errors.push('No file selected');
      return { valid: false, errors };
    }

    // 2. Check file size (max)
    if (file.size > config.maxSizeBytes) {
      const maxSizeMB = (config.maxSizeBytes / (1024 * 1024)).toFixed(2);
      errors.push(`File size exceeds maximum allowed size of ${maxSizeMB}MB`);
    }

    // 3. Check file size (min)
    if (file.size < config.minSizeBytes) {
      const minSizeKB = (config.minSizeBytes / 1024).toFixed(2);
      errors.push(`File size is too small. Minimum size is ${minSizeKB}KB`);
    }

    // 4. Check MIME type
    if (config.allowedMimeTypes.length > 0 && !config.allowedMimeTypes.includes(file.type)) {
      errors.push(
        `Invalid file type. Allowed types: ${this.getReadableFileTypes(fileType)}`
      );
    }

    // 5. Check file extension
    const extension = this.getFileExtension(file.name);
    if (!config.allowedExtensions.includes(extension)) {
      errors.push(
        `Invalid file extension. Allowed extensions: ${config.allowedExtensions.join(', ')}`
      );
    }

    // 6. Check file name
    if (!this.isValidFileName(file.name)) {
      errors.push('Invalid file name. File name contains illegal characters');
    }

    return {
      valid: errors.length === 0,
      errors,
    };
  }

  /**
   * Validate multiple files of the same type
   */
  validateFiles(files: File[], fileType: FileType): ValidationResult {
    const allErrors: string[] = [];

    files.forEach((file, index) => {
      const result = this.validateFile(file, fileType);
      if (!result.valid) {
        result.errors.forEach((error) => {
          allErrors.push(`File ${index + 1} (${file.name}): ${error}`);
        });
      }
    });

    return {
      valid: allErrors.length === 0,
      errors: allErrors,
    };
  }

  /**
   * Validate image dimensions (requires loading the image)
   */
  async validateImageDimensions(file: File): Promise<ValidationResult> {
    const config = this.constraints.image;
    const errors: string[] = [];

    try {
      const dimensions = await this.getImageDimensions(file);

      if (config.maxDimension) {
        if (dimensions.width > config.maxDimension || dimensions.height > config.maxDimension) {
          errors.push(
            `Image dimensions exceed maximum allowed size of ${config.maxDimension}px`
          );
        }
      }
    } catch (error) {
      errors.push('Failed to read image dimensions');
    }

    return {
      valid: errors.length === 0,
      errors,
    };
  }

  /**
   * Get image dimensions
   */
  private getImageDimensions(file: File): Promise<{ width: number; height: number }> {
    return new Promise((resolve, reject) => {
      const img = new Image();
      const url = URL.createObjectURL(file);

      img.onload = () => {
        URL.revokeObjectURL(url);
        resolve({ width: img.width, height: img.height });
      };

      img.onerror = () => {
        URL.revokeObjectURL(url);
        reject(new Error('Failed to load image'));
      };

      img.src = url;
    });
  }

  /**
   * Get file extension from filename
   */
  private getFileExtension(filename: string): string {
    return filename.split('.').pop()?.toLowerCase() || '';
  }

  /**
   * Check if filename is valid (no illegal characters)
   */
  private isValidFileName(filename: string): boolean {
    // Disallow: \ / : * ? " < > |
    const illegalChars = /[\\/:*?"<>|]/;
    return !illegalChars.test(filename);
  }

  /**
   * Format file size for display
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }

  /**
   * Get human-readable file types
   */
  private getReadableFileTypes(fileType: FileType): string {
    const config = this.constraints[fileType];
    return config.allowedExtensions.map((ext) => ext.toUpperCase()).join(', ');
  }

  /**
   * Get constraints for a file type
   */
  getConstraints(fileType: FileType): FileTypeConstraints {
    return { ...this.constraints[fileType] };
  }

  /**
   * Get human-readable constraints description
   */
  getConstraintsDescription(fileType: FileType): string {
    const config = this.constraints[fileType];
    const parts: string[] = [];

    parts.push(`Max size: ${this.formatFileSize(config.maxSizeBytes)}`);
    parts.push(`Allowed: ${config.allowedExtensions.join(', ')}`);

    if (config.maxPages) {
      parts.push(`Max pages: ${config.maxPages}`);
    }

    if (config.maxDimension) {
      parts.push(`Max dimension: ${config.maxDimension}px`);
    }

    return parts.join(' | ');
  }

  /**
   * Update constraints for a specific file type (optional, for backend-driven config)
   */
  updateConstraints(fileType: FileType, newConstraints: Partial<FileTypeConstraints>): void {
    this.constraints[fileType] = { ...this.constraints[fileType], ...newConstraints };
  }
}
