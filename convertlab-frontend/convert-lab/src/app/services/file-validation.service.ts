import { Injectable } from '@angular/core';

export interface FileValidationConstraints {
  maxSizeBytes: number;
  allowedMimeTypes: string[];
  allowedExtensions: string[];
  minSizeBytes?: number;
  maxPages?: number;
}

export interface ValidationResult {
  valid: boolean;
  errors: string[];
}

@Injectable({
  providedIn: 'root',
})
export class FileValidationService {
  // Default constraints - can be overridden by backend config
  private readonly defaultConstraints: FileValidationConstraints = {
    maxSizeBytes: 5 * 1024 * 1024, // 5MB
    minSizeBytes: 1024, // 1KB
    allowedMimeTypes: ['application/pdf'],
    allowedExtensions: ['pdf'],
    maxPages: 1000, // Optional: maximum pages allowed
  };

  private constraints: FileValidationConstraints = { ...this.defaultConstraints };

  constructor() { }

  /**
   * Update validation constraints (useful for backend-driven config)
   */
  updateConstraints(newConstraints: Partial<FileValidationConstraints>): void {
    this.constraints = { ...this.constraints, ...newConstraints };
  }

  /**
   * Get current constraints
   */
  getConstraints(): FileValidationConstraints {
    return { ...this.constraints };
  }

  /**
   * Reset to default constraints
   */
  resetConstraints(): void {
    this.constraints = { ...this.defaultConstraints };
  }

  /**
   * Validate a file before upload
   */
  validateFile(file: File): ValidationResult {
    const errors: string[] = [];

    // 1. Check if file exists
    if (!file) {
      errors.push('No file selected');
      return { valid: false, errors };
    }

    // 2. Check file size (max)
    if (file.size > this.constraints.maxSizeBytes) {
      const maxSizeMB = (this.constraints.maxSizeBytes / (1024 * 1024)).toFixed(2);
      errors.push(`File size exceeds maximum allowed size of ${maxSizeMB}MB`);
    }

    // 3. Check file size (min)
    if (this.constraints.minSizeBytes && file.size < this.constraints.minSizeBytes) {
      const minSizeKB = (this.constraints.minSizeBytes / 1024).toFixed(2);
      errors.push(`File size is too small. Minimum size is ${minSizeKB}KB`);
    }

    // 4. Check MIME type
    if (
      this.constraints.allowedMimeTypes.length > 0 &&
      !this.constraints.allowedMimeTypes.includes(file.type)
    ) {
      errors.push(
        `Invalid file type. Allowed types: ${this.constraints.allowedMimeTypes.join(', ')}`
      );
    }

    // 5. Check file extension
    const extension = this.getFileExtension(file.name);
    if (
      this.constraints.allowedExtensions.length > 0 &&
      !this.constraints.allowedExtensions.includes(extension)
    ) {
      errors.push(
        `Invalid file extension. Allowed extensions: ${this.constraints.allowedExtensions.join(', ')}`
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
   * Validate multiple files
   */
  validateFiles(files: File[]): ValidationResult {
    const allErrors: string[] = [];

    files.forEach((file, index) => {
      const result = this.validateFile(file);
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
   * Get human-readable constraints for display
   */
  getConstraintsDescription(): string {
    const parts: string[] = [];

    parts.push(`Max size: ${this.formatFileSize(this.constraints.maxSizeBytes)}`);

    if (this.constraints.minSizeBytes) {
      parts.push(`Min size: ${this.formatFileSize(this.constraints.minSizeBytes)}`);
    }

    if (this.constraints.allowedExtensions.length > 0) {
      parts.push(`Allowed: ${this.constraints.allowedExtensions.join(', ')}`);
    }

    if (this.constraints.maxPages) {
      parts.push(`Max pages: ${this.constraints.maxPages}`);
    }

    return parts.join(' | ');
  }
}
