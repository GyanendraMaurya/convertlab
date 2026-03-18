export interface CropImageRequest {
  fileId: string;
  /** Crop origin X in natural image pixels (post-rotation) */
  x: number;
  /** Crop origin Y in natural image pixels (post-rotation) */
  y: number;
  /** Crop width in natural image pixels */
  width: number;
  /** Crop height in natural image pixels */
  height: number;
  /** Degrees: 0 | 90 | 180 | 270 — applied BEFORE crop */
  rotation: number;
  flipHorizontal: boolean;
  flipVertical: boolean;
  /** "JPEG" | "PNG" */
  outputFormat: 'JPEG' | 'PNG';
  /** 1–100, JPEG only */
  quality: number;
}

export interface CropRect {
  x: number;
  y: number;
  width: number;
  height: number;
}

export type AspectRatio = 'free' | '1:1' | '4:3' | '16:9' | '3:4' | '9:16';

export interface CropEditorState {
  /** Natural image dimensions (pixels) */
  naturalWidth: number;
  naturalHeight: number;
  /** Current rotation in degrees */
  rotation: number;
  flipHorizontal: boolean;
  flipVertical: boolean;
  /** Crop rectangle in natural pixels */
  cropRect: CropRect;
  aspectRatio: AspectRatio;
}