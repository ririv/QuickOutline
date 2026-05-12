import { mkdir } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { build } from 'esbuild';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const outdir = resolve(root, 'src-tauri/resources');
const outfile = resolve(outdir, 'outline-parser-runtime.js');

await mkdir(outdir, { recursive: true });

await build({
  entryPoints: [resolve(root, 'scripts/outline-parser-runtime.ts')],
  outfile,
  bundle: true,
  platform: 'neutral',
  format: 'iife',
  globalName: 'quickOutlineParser',
  target: 'es2020',
  banner: {
    js: "globalThis.console = globalThis.console || { log() {}, warn() {}, error() {} };",
  },
  footer: {
    js: 'globalThis.quickOutlineParser = quickOutlineParser;',
  },
  alias: {
    '@': resolve(root, 'src'),
  },
  logLevel: 'info',
});
