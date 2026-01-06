// src/app/services/meta.service.ts
import { Injectable, inject } from '@angular/core';
import { Meta, Title } from '@angular/platform-browser';

export interface PageMeta {
  title: string;
  description: string;
  keywords?: string;
  image?: string;
  url?: string;
  type?: string;
}

@Injectable({
  providedIn: 'root',
})
export class MetaService {
  private meta = inject(Meta);
  private titleService = inject(Title);

  updateMetaTags(config: PageMeta) {
    // Set title
    this.titleService.setTitle(config.title);

    // Basic meta tags
    this.meta.updateTag({ name: 'description', content: config.description });
    if (config.keywords) {
      this.meta.updateTag({ name: 'keywords', content: config.keywords });
    }

    // Open Graph tags (Facebook, LinkedIn)
    this.meta.updateTag({ property: 'og:title', content: config.title });
    this.meta.updateTag({ property: 'og:description', content: config.description });
    this.meta.updateTag({ property: 'og:type', content: config.type || 'website' });

    if (config.url) {
      this.meta.updateTag({ property: 'og:url', content: config.url });
    }

    if (config.image) {
      this.meta.updateTag({ property: 'og:image', content: config.image });
      this.meta.updateTag({ property: 'og:image:alt', content: config.title });
    }

    // Twitter Card tags
    this.meta.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
    this.meta.updateTag({ name: 'twitter:title', content: config.title });
    this.meta.updateTag({ name: 'twitter:description', content: config.description });

    if (config.image) {
      this.meta.updateTag({ name: 'twitter:image', content: config.image });
    }

    // Additional SEO tags
    this.meta.updateTag({ name: 'robots', content: 'index, follow' });
    this.meta.updateTag({ name: 'author', content: 'ConvertLab' });
    this.meta.updateTag({ name: 'viewport', content: 'width=device-width, initial-scale=1' });

    // this.updateCanonical(config.url || window.location.href);
  }

  updateCanonical(url: string) {
    let link: HTMLLinkElement | null = document.querySelector("link[rel='canonical']");

    if (!link) {
      link = document.createElement('link');
      link.setAttribute('rel', 'canonical');
      document.head.appendChild(link);
    }

    link.setAttribute('href', url);
  }
}
