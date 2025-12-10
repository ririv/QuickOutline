import esbuild from 'esbuild';
import path from 'path';
import fs from 'fs';

// 1. Create a temporary entry file that imports runtime and preset
// Using simple string concatenation to avoid template literal hell
const entryContent = 
`import init from '@unocss/runtime';
import presetWind from '@unocss/preset-wind';

// Configure UnoCSS Runtime
window.__unocss = {
  presets: [
    presetWind(),
  ],
  preflight: false, // Disable global reset
  variants: [
    (matcher) => {
      return {
        matcher,
        selector: (s) => '.markdown-body ' + s
      }
    }
  ]
};

// Initialize
init();
`;

const entryPath = 'scripts/temp-unocss-entry.js';
fs.writeFileSync(entryPath, entryContent);

// 2. Bundle it using esbuild
esbuild.build({
  entryPoints: [entryPath],
  bundle: true,
  outfile: 'public/libs/unocss-runtime.bundle.js',
  minify: true,
  format: 'iife', // Immediately Invoked Function Expression for browser
  target: ['es2020'],
  define: { 'process.env.NODE_ENV': '"production"' } // Fix for some libs checking process.env
}).then(() => {
  console.log('✅ UnoCSS Runtime bundled successfully to public/libs/unocss-runtime.bundle.js');
  fs.unlinkSync(entryPath); // Cleanup
}).catch(() => process.exit(1));
