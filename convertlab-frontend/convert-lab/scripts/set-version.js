const fs = require('fs');
const path = require('path');

const version = process.env.npm_package_version || '1.0.0';
const buildTime = new Date().toISOString();

const environments = ['src/environments/environment.ts', 'src/environments/environment.prod.ts'];

environments.forEach((envPath) => {
  const fullPath = path.join(__dirname, '..', envPath);

  if (fs.existsSync(fullPath)) {
    let content = fs.readFileSync(fullPath, 'utf8');

    // Replace buildTime value
    content = content.replace(/buildTime:\s*['"][^'"]*['"]/, `buildTime: '${buildTime}'`);

    fs.writeFileSync(fullPath, content);
    console.log(`✅ Updated ${envPath} - buildTime: ${buildTime}`);
  }
});
