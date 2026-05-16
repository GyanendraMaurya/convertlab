export type UploadStatus = 'idle' | 'uploading' | 'ingesting' | 'ready' | 'error';
export type IngestMode = 'DIRECT' | 'RAG';

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
  ingestMode: IngestMode | null;
  ingestLog: IngestLogLine[];
  ingestSteps: IngestStep[];
}

export interface IngestLogLine {
  label: string;
  value: string;
}

export type MessageRole = 'user' | 'ai';
export type ConversationRole = 'user' | 'assistant';

export interface ChatMessage {
  id: string;
  role: MessageRole;
  text: string;
  timestamp: Date;
  sources?: string[];
  isHtml?: boolean;
  isConversation?: boolean;
}

export interface ConversationMessage {
  role: ConversationRole;
  content: string;
}

export interface UploadResponse {
  fileId: string;
  fileName: string;
  pageCount?: number;
}

export interface IngestResponse {
  chunkCount: number;
  mode: IngestMode;
  characterCount: number;
}

export interface QueryResponse {
  answer: string;
  // sources?: string[];
}
