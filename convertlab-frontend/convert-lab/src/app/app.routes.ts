import { Routes } from '@angular/router';
import { guestGuard, superAdminGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./components/home/home.component').then(m => m.HomeComponent)
  },
  {
    path: 'signup',
    loadComponent: () => import('./components/authentication/signup/signup.component').then(m => m.SignupComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'login',
    loadComponent: () => import('./components/authentication/login/login.component').then(m => m.LoginComponent),
    canActivate: [guestGuard]
  },
  {
    path: 'about',
    loadComponent: () => import('./components/about/about.component').then(m => m.AboutComponent)
  },
  {
    path: 'contact',
    loadComponent: () => import('./components/contact/contact.component').then(m => m.ContactComponent)
  },
  {
    path: 'contact-me',
    redirectTo: 'contact',
    pathMatch: 'full'
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
  },
  {
    path: 'compress-pdf',
    loadComponent: () => import('./components/pdf/compress-pdf/compress-pdf.component').then(m => m.CompressPdfComponent)
  },
  {
    path: 'edit-pdf',
    loadComponent: () => import('./components/pdf/edit-pdf/edit-pdf.component').then(m => m.EditPdfComponent)
  },
  {
    path: 'compress-image',
    loadComponent: () => import('./components/image/compress-image/compress-image.component').then(m => m.CompressImageComponent)
  },
  {
    path: 'pdf-password',
    loadComponent: () => import('./components/pdf/pdf-password/pdf-password.component').then(m => m.PdfPasswordComponent)
  },
  {
    path: 'crop-image',
    loadComponent: () => import('./components/image/crop-image/crop-image.component').then(m => m.CropImageComponent)
  },
  {
    path: 'doc-mind',
    redirectTo: 'docmind',
    pathMatch: 'full'
  },
  {
    path: 'docmind',
    loadComponent: () => import('./doc-mind/doc-mind.component').then(m => m.DocMindComponent),
  },
  {
    path: 'admin/broadcast',
    loadComponent: () => import('./components/admin/broadcast/admin-broadcast.component').then(m => m.AdminBroadcastComponent),
    canActivate: [superAdminGuard]
  },
  {
    path: 'admin/features',
    loadComponent: () => import('./components/admin/features/admin-features.component').then(m => m.AdminFeaturesComponent),
    canActivate: [superAdminGuard]
  },
];
