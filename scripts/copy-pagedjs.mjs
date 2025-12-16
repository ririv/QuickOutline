// scripts/copy-pagedjs.mjs
import { promises as fs } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const sourceFile = join(__dirname, '../node_modules/pagedjs/dist/paged.polyfill.min.js');
const targetDir = join(__dirname, '../public/libs');
const targetFile = join(targetDir, 'paged.polyfill.min.js');

async function copyPagedJs() {
  try {
    await fs.mkdir(targetDir, { recursive: true });
    await fs.copyFile(sourceFile, targetFile);
    console.log('Successfully copied Paged.js polyfill to public/libs/paged.polyfill.min.js');
  } catch (err) {
    console.error('Error copying Paged.js:', err);
    process.exit(1);
  }
}

copyPagedJs();
