// src/app/services/seo.service.ts
import { Injectable, inject } from '@angular/core';
import { MetaService } from './meta.service';
import { StructuredDataService } from './structured-data.service';
import { PageIds, SEO_CONFIGS, SEOConfig } from './seo.config';

@Injectable({
  providedIn: 'root',
})
export class SeoService {
  private metaService = inject(MetaService);
  private structuredDataService = inject(StructuredDataService);

  /**
   * Apply SEO configuration for a specific page
   * @param pageId - Unique identifier for the page (e.g., 'merge-pdf', 'home')
   */
  applySEO(pageId: PageIds): void {
    const config = SEO_CONFIGS[pageId];

    if (!config) {
      console.warn(`SEO configuration not found for page: ${pageId}`);
      return;
    }

    // Apply meta tags
    this.metaService.updateMetaTags(config.meta);

    // Apply structured data schema
    this.applySchema(pageId, config);

    // Apply breadcrumb schema
    this.structuredDataService.insertSchema(
      this.structuredDataService.getBreadcrumbSchema(config.breadcrumbs),
      'breadcrumb-schema'
    );
  }

  /**
   * Apply appropriate schema based on page type
   */
  private applySchema(pageId: string, config: SEOConfig): void {
    if (pageId === 'home') {
      // WebApplication schema for homepage
      this.structuredDataService.insertSchema({
        '@context': 'https://schema.org',
        '@type': 'WebApplication',
        name: config.schema.name,
        url: config.schema.url,
        applicationCategory: 'UtilitiesApplication',
        operatingSystem: 'Any',
        offers: {
          '@type': 'Offer',
          price: '0',
          priceCurrency: 'USD'
        },
        description: config.schema.description,
        featureList: config.schema.featureList,
        screenshot: 'https://www.easyconvertlab.com/assets/images/screenshot.jpg'
      });
    } else if (pageId === 'about' || pageId === 'contact') {
      this.structuredDataService.insertSchema({
        '@context': 'https://schema.org',
        '@type': 'Person',
        name: config.schema.name,
        url: config.schema.url,
        jobTitle: 'Full stack developer',
        email: 'mailto:gmaurya973@gmail.com',
        sameAs: [
          'https://www.linkedin.com/in/gyanendramaurya/',
          'https://github.com/GyanendraMaurya'
        ],
        description: config.schema.description,
        knowsAbout: config.schema.featureList.split(', ')
      });
    } else {
      // SoftwareApplication schema for tool pages
      const schema: any = {
        '@context': 'https://schema.org',
        '@type': 'SoftwareApplication',
        name: config.schema.name,
        applicationCategory: 'UtilitiesApplication',
        operatingSystem: 'Any',
        offers: {
          '@type': 'Offer',
          price: '0',
          priceCurrency: 'USD'
        },
        description: config.schema.description,
        url: config.schema.url,
        featureList: config.schema.featureList
      };

      // Add aggregate rating if available
      if (config.schema.aggregateRating) {
        schema.aggregateRating = {
          '@type': 'AggregateRating',
          ratingValue: config.schema.aggregateRating.ratingValue,
          ratingCount: config.schema.aggregateRating.ratingCount,
          bestRating: '5',
          worstRating: '1'
        };
      }

      this.structuredDataService.insertSchema(schema);
    }
  }

  /**
   * Clean up SEO elements (call in ngOnDestroy)
   */
  cleanup(): void {
    this.structuredDataService.removeSchema();
    this.structuredDataService.removeSchema('breadcrumb-schema');
  }
}
