import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';
import { DocumentState } from '../../models/docmind.models';
import { UploadZoneComponent } from './upload-zone/upload-zone.component';
import { DocCardComponent } from './doc-card/doc-card.component';

@Component({
  selector: 'app-doc-panel',
  standalone: true,
  imports: [UploadZoneComponent, DocCardComponent],
  templateUrl: './doc-panel.component.html',
  styleUrl: './doc-panel.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DocPanelComponent {
  docState = input.required<DocumentState>();
  hideSuggestions = input<boolean>(false);
  fileSelected = output<File>();

  /** Suggested prompt chips — parent can customise later */
  readonly suggestions = [
    'Summarize the key findings',
    'What are the main conclusions?',
    'List all mentioned dates or figures',
    'Explain the methodology used',
  ];

  promptSelected = output<string>();

  onFileSelected(file: File) {
    this.fileSelected.emit(file);
  }

  onPromptClick(text: string) {
    this.promptSelected.emit(text);
  }
}
