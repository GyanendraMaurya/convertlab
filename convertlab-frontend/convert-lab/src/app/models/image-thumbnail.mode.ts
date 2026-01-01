export interface ImageThumbnail {
  fileId: string | null;
  fileName: string;
  thumbnailUrl: string;
  rotation: 0 | 90 | 180 | 270;
  width: number;
  height: number;
  uploadStatus: 'pending' | 'uploading' | 'completed' | 'failed';
  file: File;
  error?: string;
  trackId?: string; // only for frontend optimization
}

export interface UploadedImageResponse {
  fileId: string;
  fileName: string;
}
