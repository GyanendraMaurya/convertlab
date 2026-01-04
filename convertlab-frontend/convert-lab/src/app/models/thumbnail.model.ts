export interface Thumbnail {
  fileId: string | null; // null while uploading
  pageCount: number;
  fileName: string;
  thumbnailUrl: string; // blob URL from frontend
  uploadStatus: 'pending' | 'uploading' | 'completed' | 'failed';
  file?: File; // Keep reference for retry
  error?: string;
  tempId?: string;
}

export interface UploadedPdfResponse {
  fileId: string;
  pageCount: number;
  fileName: string;
  thumbnailUrl: string; // We won't use this anymore
}
