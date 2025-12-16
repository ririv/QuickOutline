// scripts/copy-katex-fonts.mjs
import { promises as fs } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const fontsSourceDir = join(__dirname, '../node_modules/katex/dist/fonts');
const cssSource = join(__dirname, '../node_modules/katex/dist/katex.min.css');

const libsDir = join(__dirname, '../public/libs');
const fontsDir = join(libsDir, 'fonts');
const cssDest = join(libsDir, 'katex.min.css');

async function copyKatexAssets() {
  try {
    // 1. Copy Fonts
    await fs.mkdir(fontsDir, { recursive: true });
    const files = await fs.readdir(fontsSourceDir);

    for (const file of files) {
      const srcPath = join(fontsSourceDir, file);
      const destPath = join(fontsDir, file);
      await fs.copyFile(srcPath, destPath);
    }
    console.log('Successfully copied KaTeX fonts to public/libs/fonts');

    // 2. Copy CSS
    await fs.copyFile(cssSource, cssDest);
    console.log('Successfully copied KaTeX CSS to public/libs/katex.min.css');

  } catch (err) {
    console.error('Error copying KaTeX assets:', err);
    process.exit(1);
  }
}

copyKatexAssets();
