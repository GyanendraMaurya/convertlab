import { Component, signal, input, output } from '@angular/core';
import { MatButtonAppearance, MatButtonModule, MatFabButton } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-action-button',
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './action-button.component.html',
  styleUrl: './action-button.component.scss',
})
export class ActionButtonComponent {

  /** Text on the button */
  label = input<string>('');

  /** Material icon name */
  icon = input<string | null>(null);

  /** icon position: left | right */
  iconPosition = input<'left' | 'right'>('left');

  /** Button color */
  color = input<'primary' | 'accent' | 'warn' | undefined>('primary');

  variant = input<MatButtonAppearance>('filled');

  loading = input(false);

  inputClass = input<string>('');

  disabled = input(false);

  /** Emits click event */
  onClick = output<void>();

  click() {
    if (!this.loading() && !this.disabled()) {
      this.onClick.emit();
    }
  }

  /** helper signal for class binding */
  isLoading = signal(this.loading());

}
