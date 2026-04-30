import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { SeoService } from '../../seo/seo.service';

interface ServiceFocus {
  icon: string;
  title: string;
  description: string;
}

interface WorkValue {
  icon: string;
  label: string;
}

@Component({
  selector: 'app-about',
  imports: [CommonModule, MatButtonModule, MatIconModule],
  templateUrl: './about.component.html',
  styleUrl: './about.component.scss',
})
export class AboutComponent {
  private seoService = inject(SeoService);

  readonly serviceFocus: ServiceFocus[] = [
    {
      icon: 'web',
      title: 'Web Development',
      description:
        'Modern, responsive web applications built with maintainable frontend and backend architecture.',
    },
    {
      icon: 'psychology',
      title: 'AI and RAG',
      description:
        'Document-aware AI experiences, vector search workflows, and chat interfaces that make information easier to use.',
    },
    {
      icon: 'payments',
      title: 'Payment Interfaces',
      description:
        'Clear checkout, subscription, and payment-related UI flows that feel trustworthy for real users.',
    },
    {
      icon: 'integration_instructions',
      title: 'Product Engineering',
      description:
        'API integrations, dashboards, document tools, authentication, and end-to-end product features.',
    },
  ];

  readonly values: WorkValue[] = [
    { icon: 'verified', label: '7 years of industry experience' },
    { icon: 'speed', label: 'Fast delivery with clean implementation' },
    { icon: 'security', label: 'Security-conscious product decisions' },
    { icon: 'handshake', label: 'Reliable communication for freelance work' },
  ];

  ngOnInit() {
    this.seoService.applySEO('about');
  }

  ngOnDestroy() {
    this.seoService.cleanup();
  }
}
