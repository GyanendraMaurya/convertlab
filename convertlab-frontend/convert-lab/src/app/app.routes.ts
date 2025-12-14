import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'extract-pdf',
    pathMatch: 'full'
  },
  {
    path: 'extract-pdf',
    loadComponent: () => import('./components/pdf/extract-page/extract-page.component').then(m => m.ExtractPageComponent)
  },
  {
    path: 'merge-pdf',
    loadComponent: () => import('./components/pdf/merge-pdf/merge-pdf.component').then(m => m.MergePdfComponent)
  }

];
