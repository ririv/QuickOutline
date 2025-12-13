// scripts/copy-katex-fonts.mjs
import { promises as fs } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

const sourceDir = join(__dirname, '../node_modules/katex/dist/fonts');
const targetDir = join(__dirname, '../public/fonts');

async function copyKatexFonts() {
  try {
    await fs.mkdir(targetDir, { recursive: true });
    const files = await fs.readdir(sourceDir);

    for (const file of files) {
      const srcPath = join(sourceDir, file);
      const destPath = join(targetDir, file);
      await fs.copyFile(srcPath, destPath);
      // console.log(`Copied ${file}`);
    }
    console.log('Successfully copied KaTeX fonts to public/fonts');
  } catch (err) {
    console.error('Error copying KaTeX fonts:', err);
    process.exit(1);
  }
}

copyKatexFonts();
