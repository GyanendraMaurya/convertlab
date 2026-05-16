import {
  ChangeDetectionStrategy,
  Component,
  computed,
  ElementRef,
  inject,
  OnInit,
  PLATFORM_ID,
  signal,
  viewChild,
} from '@angular/core';
import {
  ChatMessage,
  DocumentState,
  IngestMode,
  IngestStep,
  UploadStatus,
} from './models/docmind.models';
import { DocumentRagService } from './services/document-rag.service';
import { DocPanelComponent } from './components/doc-panel/doc-panel.component';
import { ChatPanelComponent } from './components/chat-panel/chat-panel.component';
import { FileUploadService } from '../services/file-upload.service';
import { firstValueFrom } from 'rxjs';
import { SnackbarService } from '../services/snackbar.service';
import { WebSocketService } from '../services/websocket.service';
import { isPlatformBrowser } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-doc-mind',
  standalone: true,
  imports: [DocPanelComponent, ChatPanelComponent, MatIconModule],
  templateUrl: './doc-mind.component.html',
  styleUrl: './doc-mind.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocMindComponent implements OnInit {
  private readonly ragService = inject(DocumentRagService);
  private readonly fileUploadService = inject(FileUploadService);
  private readonly snackbarService = inject(SnackbarService);
  private readonly ws = inject(WebSocketService);
  private platformId = inject(PLATFORM_ID);

  readonly showBanner = signal(true);
  readonly bannerExiting = signal(false);

  /** True once the user has uploaded a file on mobile — switches to chat view */
  readonly isMobileChatMode = signal(false);

  // ── Document state ────────────────────────────────────────────────────────
  docState = signal<DocumentState>({
    status: 'idle',
    fileName: '',
    fileSize: 0,
    pdfId: null,
    chunkCount: null,
    ingestMode: null,
    ingestLog: [],
    ingestSteps: this.buildDefaultSteps(),
  });

  // ── Chat state ────────────────────────────────────────────────────────────
  messages = signal<ChatMessage[]>([]);
  isThinking = signal(false);
  hasMessageSent = signal(false);

  // ── Computed helpers for mobile doc bar ──────────────────────────────────
  isIngesting = computed(() =>
    this.docState().status === 'uploading' || this.docState().status === 'ingesting'
  );
  isReady = computed(() => this.docState().status === 'ready');

  mobileDocStatus = computed(() => {
    switch (this.docState().status) {
      case 'uploading': return 'Uploading…';
      case 'ingesting': return 'Analyzing…';
      case 'ready': return '✓ Ready';
      case 'error': return '✗ Error';
      default: return '';
    }
  });

  private chatPanel = viewChild(ChatPanelComponent);
  private mobileFileInput = viewChild<ElementRef<HTMLInputElement>>('mobileFileInput');

  ngOnInit() {
    if (isPlatformBrowser(this.platformId)) {
      this.subscribeToIngestEvents();
    }
  }

  // ── File selected → upload + ingest ──────────────────────────────────────
  async onFileSelected(file: File) {
    // Switch mobile to chat mode as soon as a file is chosen
    this.isMobileChatMode.set(true);
    this.dismissBanner();
    this.resetState(file);

    try {
      // Step 1 — Upload
      this.setStatus('uploading');
      this.appendLog('file', file.name);
      this.appendLog('size', this.formatSize(file.size));
      this.appendLog('status', 'uploading…');

      let pdfId: string | null = null;
      try {
        const res = await firstValueFrom(this.fileUploadService.uploadPdf(file));
        pdfId = res!.data.fileId;
      } catch {
        pdfId = 'demo_' + Date.now();
      }

      this.patchState({ pdfId });
      this.appendLog('id', pdfId?.substring(0, 16) + '…');

      // Step 2 — Ingest
      this.setStatus('ingesting');
      this.appendLog('status', 'processing…');

      let chunkCount: number | null = null;
      let ingestMode: IngestMode = 'RAG';
      try {
        const ingestRes = await firstValueFrom(this.ragService.ingestDocument(pdfId!));
        chunkCount = ingestRes!.data.chunkCount || 0;
        ingestMode = ingestRes!.data.mode || 'RAG';
      } catch (err) {
        this.patchState({ status: 'error' });
        this.snackbarService.error(this.getErrorMessage(err, 'Ingest failed. Please try again.'));
        return;
      }

      // Done
      this.patchState({ status: 'ready', chunkCount, ingestMode });
      if (ingestMode === 'DIRECT') {
        this.addAIMessage(
          `Document loaded! I can read the full text of <strong>${file.name}</strong> directly. You can now ask me anything about it.`,
          ['Full document ready']
        );
      } else {
        this.addAIMessage(
          `Document loaded! I've processed <strong>${file.name}</strong> into ${chunkCount ?? 'multiple'} semantic chunks and built a vector index. You can now ask me anything about it.`,
          ['Full document indexed', 'Embeddings ready']
        );
      }

      setTimeout(() => this.chatPanel()?.focusInput(), 100);

    } catch (err) {
      this.patchState({ status: 'error' });
      console.error('[DocMind] Processing error:', err);
    }
  }

  // ── Mobile re-upload button ───────────────────────────────────────────────
  onMobileReupload() {
    this.mobileFileInput()?.nativeElement.click();
  }

  onMobileFileInputChange(event: Event) {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (file) {
      input.value = '';
      this.onFileSelected(file);
    }
  }

  // ── Prompt chip selected ──────────────────────────────────────────────────
  onPromptSelected(text: string) {
    this.onMessageSent(text);
  }

  // ── User sends a message ──────────────────────────────────────────────────
  async onMessageSent(text: string) {
    if (this.isThinking() || this.docState().status !== 'ready') return;

    this.hasMessageSent.set(true);
    this.addUserMessage(text);
    this.isThinking.set(true);

    try {
      let answer = '';
      let sources: string[] = [];

      try {
        const res = await firstValueFrom(this.ragService
          .queryDocument(this.docState().pdfId!, text));
        answer = res!.data.answer;
      } catch (err) {
        answer = this.getErrorMessage(err, 'Sorry, I encountered an error while processing your request.');
      }

      this.addAIMessage(answer, sources);
    } finally {
      this.isThinking.set(false);
    }
  }

  // ── Helpers: state mutations ──────────────────────────────────────────────
  private resetState(file: File) {
    this.docState.set({
      status: 'idle',
      fileName: file.name,
      fileSize: file.size,
      pdfId: null,
      chunkCount: null,
      ingestMode: null,
      ingestLog: [],
      ingestSteps: this.buildDefaultSteps(),
    });
    this.messages.set([]);
  }

  private setStatus(status: UploadStatus) {
    this.docState.update(s => ({ ...s, status }));
  }

  private patchState(partial: Partial<DocumentState>) {
    this.docState.update(s => ({ ...s, ...partial }));
  }

  private appendLog(label: string, value: string) {
    this.docState.update(s => {
      const idx = s.ingestLog.findIndex(l => l.label === label);
      const ingestLog = idx >= 0
        ? s.ingestLog.map((l, i) => i === idx ? { label, value } : l)
        : [...s.ingestLog, { label, value }];
      return { ...s, ingestLog };
    });
  }

  private getErrorMessage(err: unknown, fallback: string): string {
    if (err && typeof err === 'object') {
      const error = err as { message?: unknown; error?: { error?: { message?: unknown }; message?: unknown } };
      if (typeof error.message === 'string' && error.message.trim()) return error.message;
      const apiError = error.error?.error?.message ?? error.error?.message;
      if (typeof apiError === 'string' && apiError.trim()) return apiError;
    }

    return fallback;
  }

  // ── Helpers: messages ─────────────────────────────────────────────────────
  private addUserMessage(text: string) {
    const msg: ChatMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      text,
      timestamp: new Date(),
    };
    this.messages.update(m => [...m, msg]);
  }

  private addAIMessage(text: string, sources: string[] = []) {
    const msg: ChatMessage = {
      id: crypto.randomUUID(),
      role: 'ai',
      text,
      timestamp: new Date(),
      sources,
      isHtml: true,
    };
    this.messages.update(m => [...m, msg]);
  }

  // ── Utilities ─────────────────────────────────────────────────────────────
  private buildDefaultSteps(): IngestStep[] {
    return [
      { label: 'Extracting text', status: 'pending', type: 'DOCUMENT_EXTRACTED' },
      { label: 'Cleaning text', status: 'pending', type: 'DOCUMENT_CLEANED' },
      { label: 'Chunking content', status: 'pending', type: 'DOCUMENT_CHUNKED' },
      { label: 'Generating embeddings', status: 'pending', type: 'DOCUMENT_EMBEDDED' },
    ];
  }

  private formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  }

  private dismissBanner(): void {
    if (!this.showBanner()) return;
    this.bannerExiting.set(true);
    setTimeout(() => this.showBanner.set(false), 360);
  }

  private subscribeToIngestEvents() {
    this.ws.on<string>('DOCUMENT_EXTRACTED', 'DOCUMENT_CLEANED', 'DOCUMENT_CHUNKED', 'DOCUMENT_EMBEDDED')
      .subscribe(event => {
        if (this.docState().pdfId === event.payload)
          console.log('Received WebSocket message:', event);
        if (event.type === 'DOCUMENT_EXTRACTED') {
          this.patchState({ ingestSteps: this.docState().ingestSteps.map(step => ({ ...step, status: step.type === 'DOCUMENT_EXTRACTED' ? 'done' : step.status })) });
        }
        if (event.type === 'DOCUMENT_CLEANED') {
          this.patchState({ ingestSteps: this.docState().ingestSteps.map(step => ({ ...step, status: step.type === 'DOCUMENT_CLEANED' ? 'done' : step.status })) });
        }
        if (event.type === 'DOCUMENT_CHUNKED') {
          this.patchState({ ingestSteps: this.docState().ingestSteps.map(step => ({ ...step, status: step.type === 'DOCUMENT_CHUNKED' ? 'done' : step.status })) });
        }
        if (event.type === 'DOCUMENT_EMBEDDED') {
          this.patchState({ ingestSteps: this.docState().ingestSteps.map(step => ({ ...step, status: step.type === 'DOCUMENT_EMBEDDED' ? 'done' : step.status })) });
        }
      });
  }
}
