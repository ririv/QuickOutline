import { copyFile, mkdir } from 'fs/promises';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const src = resolve(__dirname, '../node_modules/pdfjs-dist/build/pdf.worker.min.mjs');
const destDir = resolve(__dirname, '../public/libs');
const dest = resolve(destDir, 'pdf.worker.min.mjs');

async function copy() {
    try {
        await mkdir(destDir, { recursive: true });
        await copyFile(src, dest);
        console.log(`Copied pdf.worker.min.mjs to ${dest}`);
    } catch (err) {
        console.error('Failed to copy PDF.js worker:', err);
        process.exit(1);
    }
}

copy();
