export enum CompressionLevel {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH'
}

export interface CompressPdfRequest {
  fileIds: string[];
  compressionLevel: CompressionLevel
}
