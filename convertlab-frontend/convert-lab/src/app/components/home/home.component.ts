import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { CommonModule } from '@angular/common';
import { MetaService } from '../../seo/meta.service';
import { StructuredDataService } from '../../seo/structured-data.service';
import { SeoService } from '../../seo/seo.service';

interface Feature {
  icon: string;
  title: string;
  description: string;
  route: string;
  available: boolean;
}

interface ValueProp {
  icon: string;
  title: string;
  description: string;
}

interface Step {
  number: number;
  icon: string;
  title: string;
  description: string;
}

@Component({
  selector: 'app-home',
  imports: [CommonModule, MatButtonModule, MatIconModule, MatCardModule],
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent {


  features: Feature[] = [
    {
      icon: 'merge',
      title: 'Merge PDF',
      description: 'Combine multiple PDF files into one document',
      route: '/merge-pdf',
      available: true,
    },
    {
      icon: 'content_cut',
      title: 'Extract Pages',
      description: 'Extract specific pages from your PDF files',
      route: '/extract-pdf',
      available: true,
    },
    {
      icon: 'splitscreen',
      title: 'Split PDF',
      description: 'Split PDF into multiple separate documents',
      route: '/split-pdf',
      available: true,
    },
    {
      icon: 'image',
      title: 'Image to PDF',
      description: 'Convert your images into PDF format',
      route: '/image-to-pdf',
      available: true,
    },
  ];

  comingSoonFeatures: Feature[] = [
    {
      icon: 'compress',
      title: 'Compress PDF',
      description: 'Reduce PDF file size without quality loss',
      route: '',
      available: false,
    },
    {
      icon: 'crop',
      title: 'Crop Images',
      description: 'Trim and resize your images perfectly',
      route: '',
      available: false,
    },
  ];

  valueProps: ValueProp[] = [
    {
      icon: 'lock',
      title: '100% Secure',
      description: 'Files deleted within 1 hour',
    },
    {
      icon: 'flash_on',
      title: 'Lightning Fast',
      description: 'Process documents in seconds',
    },
    {
      icon: 'check_circle',
      title: 'No Registration',
      description: 'Start using immediately',
    },
    {
      icon: 'devices',
      title: 'Works Everywhere',
      description: 'Mobile, tablet, and desktop',
    },
  ];

  steps: Step[] = [
    {
      number: 1,
      icon: 'upload_file',
      title: 'Upload Files',
      description: 'Drag & drop or select your documents',
    },
    {
      number: 2,
      icon: 'tune',
      title: 'Choose Operation',
      description: 'Select what you want to do',
    },
    {
      number: 3,
      icon: 'download',
      title: 'Result Downloads',
      description: 'Get your processed file instantly',
    },
  ];

  private router = inject(Router)
  private seoService = inject(SeoService);

  ngOnInit() {
    this.seoService.applySEO('home');
  }

  navigateToFeature(route: string) {
    if (route) {
      this.router.navigate([route]);
    }
    this.seoService.cleanup();
  }

  ngOnDestroy() {
    this.seoService.cleanup();
  }
}
