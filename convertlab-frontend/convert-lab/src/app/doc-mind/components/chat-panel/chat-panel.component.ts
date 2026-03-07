import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
  output,
  viewChild,
} from '@angular/core';
import { ChatMessage, DocumentState, IngestStep } from '../../models/docmind.models';
import { ChatMessagesComponent } from './chat-messages/chat-messages.component';
import { ChatInputComponent } from './chat-input/chat-input.component';

@Component({
  selector: 'app-chat-panel',
  standalone: true,
  imports: [ChatMessagesComponent, ChatInputComponent],
  templateUrl: './chat-panel.component.html',
  styleUrl: './chat-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatPanelComponent {
  messages    = input.required<ChatMessage[]>();
  docState    = input.required<DocumentState>();
  isThinking  = input<boolean>(false);
  ingestSteps = input<IngestStep[]>([]);

  messageSent = output<string>();

  // Derived flags
  showLockedOverlay  = computed(() => this.docState().status === 'idle');
  showIngestOverlay  = computed(
    () => this.docState().status === 'uploading' || this.docState().status === 'ingesting'
  );
  isInputDisabled = computed(
    () => this.docState().status !== 'ready' || this.isThinking()
  );
  subtitle = computed(() =>
    this.docState().status === 'ready' ? this.docState().fileName : 'No document loaded'
  );

  private chatInput = viewChild(ChatInputComponent);

  onMessageSent(text: string) {
    this.messageSent.emit(text);
  }

  /** Let the parent focus input after doc is ready */
  focusInput() {
    this.chatInput()?.focus();
  }
}
