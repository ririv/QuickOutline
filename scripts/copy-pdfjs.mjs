import { copyFile, mkdir, readdir } from 'fs/promises';
import { resolve, dirname, join } from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// Worker
const workerSrc = resolve(__dirname, '../node_modules/pdfjs-dist/build/pdf.worker.min.mjs');
const workerDestDir = resolve(__dirname, '../public/libs');
const workerDest = resolve(workerDestDir, 'pdf.worker.min.mjs');

// CMaps
const cMapSrcDir = resolve(__dirname, '../node_modules/pdfjs-dist/cmaps');
const cMapDestDir = resolve(__dirname, '../public/libs/bcmaps');

async function copyDir(src, dest) {
    await mkdir(dest, { recursive: true });
    const entries = await readdir(src, { withFileTypes: true });

    for (let entry of entries) {
        const srcPath = join(src, entry.name);
        const destPath = join(dest, entry.name);

        if (entry.isDirectory()) {
            await copyDir(srcPath, destPath);
        } else {
            await copyFile(srcPath, destPath);
        }
    }
}

async function copy() {
    try {
        // Copy Worker
        await mkdir(workerDestDir, { recursive: true });
        await copyFile(workerSrc, workerDest);
        console.log(`Copied pdf.worker.min.mjs`);

        // Copy CMaps
        await copyDir(cMapSrcDir, cMapDestDir);
        console.log(`Copied CMaps to ${cMapDestDir}`);

    } catch (err) {
        console.error('Failed to copy PDF.js assets:', err);
        process.exit(1);
    }
}

copy();