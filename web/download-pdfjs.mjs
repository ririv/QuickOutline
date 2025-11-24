import download from 'download';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// 1. ESM 中没有 __dirname，需要手动构建
const __dirname = path.dirname(fileURLToPath(import.meta.url));

// 配置常量
// 选用 Legacy 版本保持兼容性
const PDFJS_VERSION = '5.4.394';
const DOWNLOAD_URL = `https://github.com/mozilla/pdf.js/releases/download/v${PDFJS_VERSION}/pdfjs-${PDFJS_VERSION}-legacy-dist.zip`;

const TARGET_DIR = path.join(__dirname, 'assets/pdfjs');
const CHECK_FILE = path.join(TARGET_DIR, 'web/viewer.html');

// 2. 检查是否已存在 (使用同步 API 保持逻辑简单)
if (fs.existsSync(CHECK_FILE)) {
    console.log('✅ PDF.js assets already exist. Skipping download.');
    process.exit(0);
}

// 3. 清理残余目录
if (fs.existsSync(TARGET_DIR)) {
    console.log('🧹 Cleaning incomplete directory...');
    fs.rmSync(TARGET_DIR, { recursive: true, force: true });
}

console.log(`⬇️  Downloading PDF.js (Legacy v${PDFJS_VERSION}) for JavaFX...`);

try {
    // 4. 使用 Top-level Await 下载并解压
    await download(DOWNLOAD_URL, TARGET_DIR, { extract: true });
    console.log('🎉 PDF.js downloaded successfully!');
} catch (error) {
    console.error('❌ Failed to download PDF.js:', error);
    process.exit(1);
}