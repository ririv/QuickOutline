import { mkdir } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';
import { build } from 'esbuild';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const outdir = resolve(root, 'src-tauri/resources');
const outfile = resolve(outdir, 'outline-parser-cli.mjs');

await mkdir(outdir, { recursive: true });

await build({
  entryPoints: [resolve(root, 'scripts/outline-parser-cli.ts')],
  outfile,
  bundle: true,
  platform: 'node',
  format: 'esm',
  target: 'node18',
  alias: {
    '@': resolve(root, 'src'),
  },
  logLevel: 'info',
});
