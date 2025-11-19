const esbuild = require('esbuild');
const { copy } = require('esbuild-plugin-copy');

esbuild.build({
    entryPoints: ['vditor_entry.ts'],
    bundle: true,
    format: 'iife',
    platform: 'browser',
    metafile: true,
    minify: true,
    outfile: '../src/main/resources/web/vditor.bundle.js',
    loader: {
        '.woff': 'dataurl',
        '.woff2': 'dataurl',
        '.ttf': 'dataurl',
    },
    plugins: [
        copy({
            assets: {
                from: ['./node_modules/vditor/dist/**/*'],
                to: ['vditor/dist'],
            },
        }),
    ],
})
.then(() => console.log('Build finished successfully with assets copied!'))
.catch(() => process.exit(1));
