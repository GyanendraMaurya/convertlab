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

export type PageIds =
  'home' | 'merge-pdf' | 'split-pdf' | 'extract-pdf' | 'image-to-pdf' | 'compress-pdf' | 'compress-image' | 'pdf-password'
  | 'crop-image' | 'docmind';

export const SEO_CONFIGS: Record<PageIds, SEOConfig> = {
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
  },
  'compress-pdf': {
    meta: {
      title: 'Compress PDF Files Online - Reduce PDF Size | EasyConvertLab',
      description: 'Compress PDF files online for free and reduce file size without losing quality. Choose from low, medium, or high compression levels. Fast, secure, and no registration required.',
      keywords: 'compress pdf, reduce pdf size, pdf compressor, shrink pdf, optimize pdf, compress pdf online, free pdf compression',
      url: 'https://www.easyconvertlab.com/compress-pdf',
      image: 'https://www.easyconvertlab.com/assets/images/compress-pdf-og.jpg',
      type: 'website'
    },
    schema: {
      name: 'Compress PDF - EasyConvertLab',
      description: 'Reduce PDF file size online using low, medium, or high compression levels while maintaining the best possible quality.',
      url: 'https://www.easyconvertlab.com/compress-pdf',
      featureList: 'Low compression (best quality), Medium compression (balanced), High compression (maximum size reduction), Fast and secure processing',
      aggregateRating: {
        ratingValue: '4.8',
        ratingCount: '980'
      }
    },
    breadcrumbs: [
      { name: 'Home', url: 'https://www.easyconvertlab.com' },
      { name: 'Compress PDF', url: 'https://www.easyconvertlab.com/compress-pdf' }
    ]
  },
  'compress-image': {
    meta: {
      title: 'Compress images Online - Reduce Image Size | EasyConvertLab',
      description: 'Compress image files online for free and reduce file size without losing quality. Choose from low, medium, or high compression levels. Fast, secure, and no registration required.',
      keywords: 'compress image, reduce image size, image compressor, shrink image, optimize image, compress image online, free image compression',
      url: 'https://www.easyconvertlab.com/compress-image',
      image: 'https://www.easyconvertlab.com/assets/images/compress-image-og.jpg',
      type: 'website'
    },
    schema: {
      name: 'Compress image - EasyConvertLab',
      description: 'Reduce image file size online using low, medium, or high compression levels while maintaining the best possible quality.',
      url: 'https://www.easyconvertlab.com/compress-image',
      featureList: 'Low compression (best quality), Medium compression (balanced), High compression (maximum size reduction), Fast and secure processing',
      aggregateRating: {
        ratingValue: '4.8',
        ratingCount: '980'
      }
    },
    breadcrumbs: [
      { name: 'Home', url: 'https://www.easyconvertlab.com' },
      { name: 'Compress image', url: 'https://www.easyconvertlab.com/compress-pdf' }
    ]
  },
  'pdf-password': {
    meta: {
      title: 'PDF Password Protection - Add or Remove PDF Password Online | EasyConvertLab',
      description: 'Add or remove password protection from PDF files online for free. Secure your PDFs with strong encryption or unlock password-protected PDFs. Fast and easy to use.',
      keywords: 'pdf password protection, add password to pdf, remove pdf password, encrypt pdf, decrypt pdf, secure pdf online, free pdf password tool',
      url: 'https://www.easyconvertlab.com/pdf-password',
      image: 'https://www.easyconvertlab.com/assets/images/pdf-password-og.jpg',
      type: 'website'
    },
    schema: {
      name: 'PDF Password Protection - EasyConvertLab',
      description: 'Add or remove password protection from PDF files online. Secure your PDFs with strong encryption or unlock password-protected PDFs.',
      url: 'https://www.easyconvertlab.com/pdf-password',
      featureList: 'Add password protection, Remove existing password, Strong encryption, Fast and secure processing',
      aggregateRating: {
        ratingValue: '4.7',
        ratingCount: '650'
      }
    },
    breadcrumbs: [
      { name: 'Home', url: 'https://www.easyconvertlab.com' },
      { name: 'PDF Password', url: 'https://www.easyconvertlab.com/pdf-password' }
    ]
  },
  'crop-image': {
    meta: {
      title: 'Crop Image Online - Free Image Cropper Tool | EasyConvertLab',
      description: 'Crop images online for free with our easy-to-use image cropper tool. Adjust the crop area, maintain aspect ratio, and download your cropped image in seconds.',
      keywords: 'crop image, image cropper, crop photo online, free image cropping tool, adjust crop area, maintain aspect ratio',
      url: 'https://www.easyconvertlab.com/crop-image',
      image: 'https://www.easyconvertlab.com/assets/images/crop-image-og.jpg',
      type: 'website'
    },
    schema: {
      name: 'Crop Image - EasyConvertLab',
      description: 'Crop images online with our user-friendly image cropper tool. Adjust the crop area and maintain aspect ratio for perfect results.',
      url: 'https://www.easyconvertlab.com/crop-image',
      featureList: 'Adjustable crop area, Maintain aspect ratio, Multiple output formats, Fast and secure processing',
      aggregateRating: {
        ratingValue: '4.8',
        ratingCount: '540'
      }
    },
    breadcrumbs: [
      { name: 'Home', url: 'https://www.easyconvertlab.com' },
      { name: 'Crop Image', url: 'https://www.easyconvertlab.com/crop-image' }
    ]
  },
  'docmind': {
    meta: {
      title: 'DocMind - AI-Powered Document Analysis and Insights | EasyConvertLab',
      description: 'DocMind is an AI-powered tool that analyzes your documents and provides insights, summaries, and key information extraction. Try it for free on EasyConvertLab.',
      keywords: 'docmind, ai document analysis, document insights, document summarization, key information extraction, free document analysis tool',
      url: 'https://www.easyconvertlab.com/docmind',
      image: 'https://www.easyconvertlab.com/assets/images/docmind-og.jpg',
      type: 'website'
    },
    schema: {
      name: 'DocMind - EasyConvertLab',
      description: 'DocMind is an AI-powered tool that analyzes your documents and provides insights, summaries, and key information extraction.',
      url: 'https://www.easyconvertlab.com/docmind',
      featureList: 'AI document analysis, Generate insights, Document summarization, Key information extraction',
      aggregateRating: {
        ratingValue: '4.9',
        ratingCount: '320'
      }
    },
    breadcrumbs: [
      { name: 'Home', url: 'https://www.easyconvertlab.com' },
      { name: 'DocMind', url: 'https://www.easyconvertlab.com/docmind' }
    ]
  },

};
