export interface SEOConfig {
  meta: {
    title: string;
    description: string;
    keywords: string;
    url: string;
    image: string;
    type: string;
  };
  schema: {
    name: string;
    description: string;
    url: string;
    featureList: string;
    aggregateRating?: {
      ratingValue: string;
      ratingCount: string;
    };
  };
  breadcrumbs: Array<{ name: string; url: string }>;
}

export type PageIds = 'home' | 'merge-pdf' | 'split-pdf' | 'extract-pdf' | 'image-to-pdf';

export const SEO_CONFIGS: Record<string, SEOConfig> = {
  home: {
    meta: {
      title: 'EasyConvertLab - Free Online PDF Tools | Merge, Split, Extract & Convert PDFs',
      description: 'Free online PDF tools for merging, splitting, extracting pages, and converting images to PDF. Fast, secure, and easy to use. No registration required.',
      keywords: 'pdf tools, merge pdf, split pdf, extract pdf, image to pdf, pdf converter, free pdf tools, online pdf editor',
      url: 'https://www.easyconvertlab.com',
      image: 'https://www.easyconvertlab.com/assets/images/easyconvertlab-og.jpg',
      type: 'website'
    },
    schema: {
      name: 'EasyConvertLab',
      description: 'Free online PDF tools for merging, splitting, extracting pages, and converting images to PDF. Fast, secure, and easy to use.',
      url: 'https://www.easyconvertlab.com',
      featureList: 'Merge PDF, Split PDF, Extract PDF Pages, Image to PDF Converter'
    },
    breadcrumbs: [
      { name: 'Home', url: 'https://www.easyconvertlab.com' }
    ]
  },

  'merge-pdf': {
    meta: {
      title: 'Merge PDF Files Online - Free PDF Merger | EasyConvertLab',
      description: 'Merge multiple PDF files into one document online for free. Fast, secure PDF merger tool with drag-and-drop interface. No file size limits. No registration required.',
      keywords: 'merge pdf, combine pdf, pdf merger, join pdf files, merge pdf online, free pdf merger, combine multiple pdfs',
      url: 'https://www.easyconvertlab.com/merge-pdf',
      image: 'https://www.easyconvertlab.com/assets/images/merge-pdf-og.jpg',
      type: 'website'
    },
    schema: {
      name: 'Merge PDF - EasyConvertLab',
      description: 'Merge multiple PDF files into one document online. Fast, secure, and free PDF merger with drag-and-drop functionality.',
      url: 'https://www.easyconvertlab.com/merge-pdf',
      featureList: 'Merge unlimited PDFs, Drag and drop reordering, No file size limits, Secure processing',
      aggregateRating: {
        ratingValue: '4.8',
        ratingCount: '1250'
      }
    },
    breadcrumbs: [
      { name: 'Home', url: 'https://www.easyconvertlab.com' },
      { name: 'Merge PDF', url: 'https://www.easyconvertlab.com/merge-pdf' }
    ]
  },

  'extract-pdf': {
    meta: {
      title: 'Extract Pages from PDF Online - Free PDF Page Extractor | EasyConvertLab',
      description: 'Extract specific pages from PDF files online for free. Select and extract single or multiple pages from any PDF. Fast, secure, and easy to use.',
      keywords: 'extract pdf pages, pdf page extractor, extract from pdf, pdf splitter, remove pdf pages, online pdf extractor',
      url: 'https://www.easyconvertlab.com/extract-pdf',
      image: 'https://www.easyconvertlab.com/assets/images/extract-pdf-og.jpg',
      type: 'website'
    },
    schema: {
      name: 'Extract PDF Pages - EasyConvertLab',
      description: 'Extract specific pages from PDF files online. Select single or multiple pages to create a new PDF document.',
      url: 'https://www.easyconvertlab.com/extract-pdf',
      featureList: 'Extract specific pages, Visual page selection, Preview pages, Multiple page extraction',
      aggregateRating: {
        ratingValue: '4.7',
        ratingCount: '890'
      }
    },
    breadcrumbs: [
      { name: 'Home', url: 'https://www.easyconvertlab.com' },
      { name: 'Extract PDF', url: 'https://www.easyconvertlab.com/extract-pdf' }
    ]
  },

  'split-pdf': {
    meta: {
      title: 'Split PDF Files Online - Free PDF Splitter | EasyConvertLab',
      description: 'Split PDF files into multiple documents online for free. Extract pages, split by range, or create separate PDFs from each page. Fast and secure.',
      keywords: 'split pdf, pdf splitter, divide pdf, separate pdf pages, split pdf by pages, online pdf splitter, pdf divider',
      url: 'https://www.easyconvertlab.com/split-pdf',
      image: 'https://www.easyconvertlab.com/assets/images/split-pdf-og.jpg',
      type: 'website'
    },
    schema: {
      name: 'Split PDF - EasyConvertLab',
      description: 'Split PDF files into multiple documents. Extract pages by range or create separate PDFs from each page.',
      url: 'https://www.easyconvertlab.com/split-pdf',
      featureList: 'Split by page range, Extract individual pages, Multiple split options, Batch processing',
      aggregateRating: {
        ratingValue: '4.6',
        ratingCount: '720'
      }
    },
    breadcrumbs: [
      { name: 'Home', url: 'https://www.easyconvertlab.com' },
      { name: 'Split PDF', url: 'https://www.easyconvertlab.com/split-pdf' }
    ]
  },

  'image-to-pdf': {
    meta: {
      title: 'Image to PDF Converter - Convert JPG, PNG to PDF Online | EasyConvertLab',
      description: 'Convert images to PDF online for free. Support for JPG, PNG, JPEG, GIF, BMP, and WEBP. Combine multiple images into one PDF. Fast and secure conversion.',
      keywords: 'image to pdf, jpg to pdf, png to pdf, convert image to pdf, photo to pdf, picture to pdf, image converter',
      url: 'https://www.easyconvertlab.com/image-to-pdf',
      image: 'https://www.easyconvertlab.com/assets/images/image-to-pdf-og.jpg',
      type: 'website'
    },
    schema: {
      name: 'Image to PDF Converter - EasyConvertLab',
      description: 'Convert images to PDF online. Supports JPG, PNG, JPEG, GIF, BMP, and WEBP formats. Combine multiple images into one PDF.',
      url: 'https://www.easyconvertlab.com/image-to-pdf',
      featureList: 'Multiple image format support, Combine multiple images, Drag and drop ordering, High quality conversion',
      aggregateRating: {
        ratingValue: '4.9',
        ratingCount: '1540'
      }
    },
    breadcrumbs: [
      { name: 'Home', url: 'https://www.easyconvertlab.com' },
      { name: 'Image to PDF', url: 'https://www.easyconvertlab.com/image-to-pdf' }
    ]
  }
};
