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
    'What does the document say about this topic?',
    'Explain this concept from the document',
    'What information is provided about this subject?',
    'What details are mentioned about this topic?'
  ];

  promptSelected = output<string>();

  onFileSelected(file: File) {
    this.fileSelected.emit(file);
  }

  onPromptClick(text: string) {
    this.promptSelected.emit(text);
  }
}
