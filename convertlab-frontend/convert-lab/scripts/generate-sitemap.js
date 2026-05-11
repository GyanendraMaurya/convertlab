const fs = require('fs');
const path = require('path');

const baseUrl = 'https://www.easyconvertlab.com';
const today = new Date().toISOString().split('T')[0];

const routes = [
  { path: '/', priority: '1.0', changefreq: 'weekly' },
  { path: '/merge-pdf', priority: '0.9', changefreq: 'weekly' },
  { path: '/extract-pdf', priority: '0.9', changefreq: 'weekly' },
  { path: '/split-pdf', priority: '0.9', changefreq: 'weekly' },
  { path: '/image-to-pdf', priority: '0.9', changefreq: 'weekly' },
  { path: '/compress-pdf', priority: '0.9', changefreq: 'weekly' },
  { path: '/compress-image', priority: '0.9', changefreq: 'weekly' },
  { path: '/pdf-password', priority: '0.9', changefreq: 'weekly' },
  { path: '/crop-image', priority: '0.9', changefreq: 'weekly' },
  { path: '/docmind', priority: '0.8', changefreq: 'weekly' },
  { path: '/about', priority: '0.7', changefreq: 'monthly' },
  { path: '/contact', priority: '0.7', changefreq: 'monthly' },
];

const sitemap = `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9">
${routes
  .map(
    (route) => `  <url>
    <loc>${baseUrl}${route.path}</loc>
    <lastmod>${today}</lastmod>
    <changefreq>${route.changefreq}</changefreq>
    <priority>${route.priority}</priority>
  </url>`,
  )
  .join('\n')}
</urlset>`;

fs.writeFileSync(path.join(__dirname, '../public/sitemap.xml'), sitemap);
console.log('✅ Sitemap generated at public/sitemap.xml');
