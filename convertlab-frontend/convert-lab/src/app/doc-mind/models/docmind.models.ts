export type UploadStatus = 'idle' | 'uploading' | 'ingesting' | 'ready' | 'error';

export type IngestStepStatus = 'pending' | 'active' | 'done';

export interface IngestStep {
  label: string;
  status: IngestStepStatus;
  type: 'DOCUMENT_EXTRACTED' | 'DOCUMENT_CLEANED' | 'DOCUMENT_CHUNKED' | 'DOCUMENT_EMBEDDED';
}

export interface DocumentState {
  status: UploadStatus;
  fileName: string;
  fileSize: number;
  pdfId: string | null;
  chunkCount: number | null;
  ingestLog: IngestLogLine[];
  ingestSteps: IngestStep[];
}

export interface IngestLogLine {
  label: string;
  value: string;
}

export type MessageRole = 'user' | 'ai';

export interface ChatMessage {
  id: string;
  role: MessageRole;
  text: string;
  timestamp: Date;
  sources?: string[];
  isHtml?: boolean;
}

export interface UploadResponse {
  fileId: string;
  fileName: string;
  pageCount?: number;
}

export interface IngestResponse {
  chunkCount: number;
}

export interface QueryResponse {
  answer: string;
  // sources?: string[];
}
