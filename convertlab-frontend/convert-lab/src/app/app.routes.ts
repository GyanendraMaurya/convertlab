import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./components/pdf/merge-pdf/merge-pdf.component').then(m => m.MergePdfComponent)
  },
  {
    path: 'merge-pdf',
    loadComponent: () => import('./components/pdf/merge-pdf/merge-pdf.component').then(m => m.MergePdfComponent)
  },
  {
    path: 'extract-pdf',
    loadComponent: () => import('./components/pdf/extract-page/extract-page.component').then(m => m.ExtractPageComponent)
  },
  {
    path: 'split-pdf',
    loadComponent: () => import('./components/pdf/split-pdf/split-pdf.component').then(m => m.SplitPdfComponent)
  },
  {
    path: 'image-to-pdf',
    loadComponent: () => import('./components/pdf/image-to-pdf/image-to-pdf.component').then(m => m.ImageToPdfComponent)
  }
];
