import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'merge-pdf',
    pathMatch: 'full'
  },
  {
    path: 'merge-pdf',
    loadComponent: () => import('./components/pdf/merge-pdf/merge-pdf.component').then(m => m.MergePdfComponent)
  },
  {
    path: 'extract-pdf',
    loadComponent: () => import('./components/pdf/extract-page/extract-page.component').then(m => m.ExtractPageComponent)
  }

];
