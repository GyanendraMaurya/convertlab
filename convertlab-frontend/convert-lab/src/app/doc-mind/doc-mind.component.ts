import {
  ChangeDetectionStrategy,
  Component,
  inject,
  OnInit,
  signal,
  viewChild,
} from '@angular/core';
import {
  ChatMessage,
  DocumentState,
  IngestStep,
  UploadStatus,
} from './models/docmind.models';
import { DocumentRagService } from './services/document-rag.service';
import { DocPanelComponent } from './components/doc-panel/doc-panel.component';
import { ChatPanelComponent } from './components/chat-panel/chat-panel.component';
import { FileUploadService } from '../services/file-upload.service';
import { firstValueFrom } from 'rxjs';
import { SnackbarService } from '../services/snackbar.service';

@Component({
  selector: 'app-doc-mind',
  standalone: true,
  imports: [DocPanelComponent, ChatPanelComponent],
  templateUrl: './doc-mind.component.html',
  styleUrl: './doc-mind.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocMindComponent {
  private readonly ragService = inject(DocumentRagService);
  private readonly fileUploadService = inject(FileUploadService);
  private readonly snackbarService = inject(SnackbarService);

  readonly showBanner = signal(true);
  readonly bannerExiting = signal(false);

  // ── Document state ────────────────────────────────────────────────────────
  docState = signal<DocumentState>({
    status: 'idle',
    fileName: '',
    fileSize: 0,
    pdfId: null,
    chunkCount: null,
    ingestLog: [],
    ingestSteps: this.buildDefaultSteps(),
  });

  // ── Chat state ────────────────────────────────────────────────────────────
  messages = signal<ChatMessage[]>([]);
  isThinking = signal(false);
  hasMessageSent = signal(false);

  private chatPanel = viewChild(ChatPanelComponent);

  // ── File selected → upload + ingest ──────────────────────────────────────
  async onFileSelected(file: File) {
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
        pdfId = 'demo_' + Date.now(); // demo fallback
      }

      this.patchState({ pdfId });
      this.appendLog('id', pdfId?.substring(0, 16) + '…');

      // Step 2 — Ingest
      this.setStatus('ingesting');
      this.appendLog('status', 'processing…');
      this.tickSteps();

      let chunkCount: number | null = null;
      try {
        const ingestRes = await firstValueFrom(this.ragService.ingestDocument(pdfId!));
        chunkCount = ingestRes!.data.chunkCount || 0;
      } catch {
        this.snackbarService.show('Ingest failed, Please try again.', 'error');
        return;
      }

      // Done
      this.patchState({ status: 'ready', chunkCount });
      this.addAIMessage(
        `Document loaded! I've processed <strong>${file.name}</strong> into ${chunkCount ?? 'multiple'} semantic chunks and built a vector index. You can now ask me anything about it.`,
        ['Full document indexed', 'Embeddings ready']
      );

      // Focus input after ready
      setTimeout(() => this.chatPanel()?.focusInput(), 100);

    } catch (err) {
      this.patchState({ status: 'error' });
      console.error('[DocMind] Processing error:', err);
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
        answer = res!.data.answer
        // sources = res!.data.sources ?? [];
      } catch {
        answer = 'Sorry, I encountered an error while processing your request.';
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
    this.docState.update(s => ({
      ...s,
      ingestLog: [...s.ingestLog, { label, value }],
    }));
  }

  // ── Animate through ingest steps ──────────────────────────────────────────
  private tickSteps() {
    const delays = [0, 700, 1500, 2300];
    const durations = [600, 700, 900, 400];

    delays.forEach((delay, i) => {
      // Mark active
      setTimeout(() => {
        this.docState.update(s => ({
          ...s,
          ingestSteps: s.ingestSteps.map((step, idx) => ({
            ...step,
            status: idx === i ? 'active' : idx < i ? 'done' : 'pending',
          })),
        }));
      }, delay);

      // Mark done
      setTimeout(() => {
        this.docState.update(s => ({
          ...s,
          ingestSteps: s.ingestSteps.map((step, idx) => ({
            ...step,
            status: idx <= i ? 'done' : 'pending',
          })),
        }));
      }, delay + durations[i]);
    });
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
      { label: 'Extracting text', status: 'pending' },
      { label: 'Chunking content', status: 'pending' },
      { label: 'Generating embeddings', status: 'pending' },
      { label: 'Indexing vectors', status: 'pending' },
    ];
  }

  private formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  }

  private sleep(ms: number): Promise<void> {
    return new Promise(r => setTimeout(r, ms));
  }

  private dismissBanner(): void {
    if (!this.showBanner()) return;
    this.bannerExiting.set(true);
    // Remove from DOM after exit animation completes (350 ms)
    setTimeout(() => this.showBanner.set(false), 360);
  }

}
