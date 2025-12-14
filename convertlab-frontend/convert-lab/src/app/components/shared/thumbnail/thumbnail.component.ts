import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

@Component({
  selector: 'app-thumbnail',
  imports: [],
  templateUrl: './thumbnail.component.html',
  styleUrl: './thumbnail.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ThumbnailComponent {
  id = input.required<string>();
  thumbnailUrl = input.required<string>();
  fileName = input<string>('');
  pageCount = input<number>(0);
  disabled = input(false);
  remove = output<string>();

  onRemoveClick(event: MouseEvent) {
    event.stopPropagation(); // important for future drag & drop
    if (!this.disabled()) {
      this.remove.emit(this.id());
    }
  }
}
