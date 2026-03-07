import {
  AfterViewChecked,
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  inject,
  input,
  PLATFORM_ID,
  viewChild,
} from '@angular/core';
import { DatePipe, isPlatformBrowser } from '@angular/common';
import { ChatMessage, IngestStep } from '../../../models/docmind.models';

@Component({
  selector: 'app-chat-messages',
  standalone: true,
  imports: [DatePipe],
  templateUrl: './chat-messages.component.html',
  styleUrl: './chat-messages.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatMessagesComponent implements AfterViewChecked {
  messages = input.required<ChatMessage[]>();
  isThinking = input<boolean>(false);
  ingestSteps = input<IngestStep[]>([]);
  showIngestOverlay = input<boolean>(false);

  private bottomAnchor = viewChild<ElementRef<HTMLDivElement>>('bottomAnchor');

  private platformId = inject(PLATFORM_ID);


  ngAfterViewChecked() {
    if (isPlatformBrowser(this.platformId)) {
      this.scrollToBottom();
    }
  }

  private scrollToBottom() {
    this.bottomAnchor()?.nativeElement.scrollIntoView({ behavior: 'smooth' });
  }

  trackById(_: number, msg: ChatMessage) {
    return msg.id;
  }
}
