const esbuild = require('esbuild');
const { copy } = require('esbuild-plugin-copy');

console.log('Start building...');

esbuild.build({
    entryPoints: ['vditor_entry.ts'],
    bundle: true,
    format: 'iife',
    platform: 'browser',
    metafile: true,
    minify: true,
    outfile: '../src/main/resources/web/vditor.bundle.js',
    logLevel: 'info',
    loader: {
        '.woff': 'dataurl',
        '.woff2': 'dataurl',
        '.ttf': 'dataurl',
    },
    plugins: [
        copy({
            // resolveFrom: 'cwd',
            assets: [
                // 1. 【基础文件】(保持不变)
                { from: './node_modules/vditor/dist/*.{css,js}', to: 'vditor/dist' },

                // 2. 【样式与图标】(保持不变)
                { from: './node_modules/vditor/dist/css/**/*', to: 'vditor/dist/css' },
                { from: './node_modules/vditor/dist/js/icons/**/*', to: 'vditor/dist/js/icons' },

                // 3. 【核心依赖 & 语言包】(修复了花括号问题)
                { from: './node_modules/vditor/dist/js/i18n/zh_CN.js', to: 'vditor/dist/js/i18n/zh_CN.js' },
                { from: './node_modules/vditor/dist/js/i18n/en_US.js', to: 'vditor/dist/js/i18n/en_US.js' },

                { from: './node_modules/vditor/dist/js/lute/**/*', to: 'vditor/dist/js/lute' },

                // 4. 【代码高亮】(修复了 EISDIR 错误)
                // 使用 [path][name].[ext] 告诉插件保留原始目录结构
                {
                    from: './node_modules/vditor/dist/js/highlight.js/**/*',
                    to: 'vditor/dist/js/highlight.js/[path][name].[ext]'
                },

                // 5. 【MathJax】(根据你之前的目录结构)
                // 拷贝 tex-svg-full.js
                { from: './node_modules/vditor/dist/js/mathjax/*.js', to: 'vditor/dist/js/mathjax' },
                // 拷贝 input 文件夹 (使用模板变量防止路径错误)
                {
                    from: './node_modules/vditor/dist/js/mathjax/input/**/*',
                    to: 'vditor/dist/js/mathjax/input/[path][name].[ext]'
                },
                {
                    from: './assets/pdfjs/**/*',
                    to: 'pdfjs'
                },
            ],
            verbose: false // 关掉 verbose，因为 highlight.js 文件太多刷屏
        }),
    ],
})
    .then(() => console.log('Build finished successfully!'))
    .catch((e) => {
        console.error('Build failed!');
        console.error(e);
        process.exit(1);
    });