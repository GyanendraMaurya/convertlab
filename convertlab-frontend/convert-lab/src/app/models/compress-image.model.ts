import { CompressionLevel } from "./compression-level.model";


export interface CompressImageRequest {
  fileIds: string[];
  compressionLevel: CompressionLevel
}
