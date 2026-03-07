import {
  ChangeDetectionStrategy,
  Component,
  computed,
  input,
} from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { DocumentState } from '../../../models/docmind.models';

@Component({
  selector: 'app-doc-card',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './doc-card.component.html',
  styleUrl: './doc-card.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocCardComponent {
  state = input.required<DocumentState>();

  isIngesting = computed(() => this.state().status === 'ingesting');
  isReady     = computed(() => this.state().status === 'ready');
  isError     = computed(() => this.state().status === 'error');
  showChips   = computed(() => this.isReady());
  showProgress = computed(() => this.isIngesting());
  showIngestVisual = computed(
    () => this.isIngesting() && this.state().ingestLog.length > 0
  );

  statusClass = computed(() => {
    const s = this.state().status;
    if (s === 'ingesting' || s === 'uploading') return 'ingesting';
    if (s === 'ready')  return 'ready';
    if (s === 'error')  return 'error';
    return '';
  });

  statusText = computed(() => {
    switch (this.state().status) {
      case 'uploading':  return 'Uploading document…';
      case 'ingesting':  return 'Analyzing & embedding…';
      case 'ready':      return 'Document ready';
      case 'error':      return 'Processing failed. Try again.';
      default:           return 'Waiting…';
    }
  });

  fileSizeLabel = computed(() => {
    const b = this.state().fileSize;
    if (b < 1024)           return `${b} B`;
    if (b < 1024 * 1024)    return `${(b / 1024).toFixed(1)} KB`;
    return `${(b / 1024 / 1024).toFixed(1)} MB`;
  });
}
