// src/app/services/structured-data.service.ts
import { Injectable, inject, PLATFORM_ID } from '@angular/core';
import { isPlatformBrowser } from '@angular/common';

@Injectable({
  providedIn: 'root',
})
export class StructuredDataService {
  private platformId = inject(PLATFORM_ID);

  insertSchema(schema: object, id: string = 'structured-data') {
    if (isPlatformBrowser(this.platformId)) {
      // Remove existing schema with same id
      const existingScript = document.getElementById(id);
      if (existingScript) {
        existingScript.remove();
      }

      // Create new script
      const script = document.createElement('script');
      script.type = 'application/ld+json';
      script.id = id;
      script.text = JSON.stringify(schema);
      document.head.appendChild(script);
    }
  }

  removeSchema(id: string = 'structured-data') {
    if (isPlatformBrowser(this.platformId)) {
      const script = document.getElementById(id);
      if (script) {
        script.remove();
      }
    }
  }

  // Organization schema for homepage
  getOrganizationSchema() {
    return {
      '@context': 'https://schema.org',
      '@type': 'Organization',
      name: 'ConvertLab',
      url: 'https://www.easyconvertlab.com',
      logo: 'https://www.easyconvertlab.com/assets/logo.png',
      description: 'Free online PDF tools for merging, splitting, extracting, and converting PDFs',
      sameAs: [
        'https://twitter.com/yourhandle',
        'https://facebook.com/yourpage',
        'https://linkedin.com/company/yourcompany'
      ]
    };
  }

  // WebApplication schema
  getWebApplicationSchema() {
    return {
      '@context': 'https://schema.org',
      '@type': 'WebApplication',
      name: 'ConvertLab',
      url: 'https://www.easyconvertlab.com',
      applicationCategory: 'UtilitiesApplication',
      operatingSystem: 'Any',
      offers: {
        '@type': 'Offer',
        price: '0',
        priceCurrency: 'USD'
      },
      description: 'Free online PDF tools for merging, splitting, extracting, and converting PDFs'
    };
  }

  // SoftwareApplication schema for specific tools
  getToolSchema(toolName: string, description: string, url: string) {
    return {
      '@context': 'https://schema.org',
      '@type': 'SoftwareApplication',
      name: toolName,
      applicationCategory: 'UtilitiesApplication',
      operatingSystem: 'Any',
      offers: {
        '@type': 'Offer',
        price: '0',
        priceCurrency: 'USD'
      },
      description: description,
      url: url,
      aggregateRating: {
        '@type': 'AggregateRating',
        ratingValue: '4.8',
        ratingCount: '1250'
      }
    };
  }

  // BreadcrumbList schema
  getBreadcrumbSchema(items: { name: string; url: string }[]) {
    return {
      '@context': 'https://schema.org',
      '@type': 'BreadcrumbList',
      itemListElement: items.map((item, index) => ({
        '@type': 'ListItem',
        position: index + 1,
        name: item.name,
        item: item.url
      }))
    };
  }
}
