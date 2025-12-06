import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'pdf',
    pathMatch: 'full'
  },
  {
    path: 'pdf',
    loadComponent: () => import('./components/pdf/extract-page/extract-page.component').then(m => m.ExtractPageComponent)
  }
];
