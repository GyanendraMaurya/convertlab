import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  input,
  output,
  viewChild,
} from '@angular/core';

@Component({
  selector: 'app-chat-input',
  standalone: true,
  templateUrl: './chat-input.component.html',
  styleUrl: './chat-input.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ChatInputComponent {
  /** Disable the whole input area (no doc loaded or still thinking) */
  disabled = input<boolean>(true);

  /** Emits the trimmed message text */
  messageSent = output<string>();

  private textareaRef = viewChild<ElementRef<HTMLTextAreaElement>>('textarea');

  onKeydown(event: KeyboardEvent) {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      this.submit(event.target as HTMLTextAreaElement);
    }
  }

  onInput(event: Event) {
    const el = event.target as HTMLTextAreaElement;
    this.autoResize(el);
  }

  onSendClick() {
    const el = this.textareaRef()?.nativeElement;
    if (el) this.submit(el);
  }

  private submit(el: HTMLTextAreaElement) {
    const text = el.value.trim();
    if (!text || this.disabled()) return;
    this.messageSent.emit(text);
    el.value = '';
    this.autoResize(el);
  }

  private autoResize(el: HTMLTextAreaElement) {
    el.style.height = 'auto';
    el.style.height = Math.min(el.scrollHeight, 120) + 'px';
  }

  /** Called by parent to focus the input */
  focus() {
    this.textareaRef()?.nativeElement.focus();
  }
}
