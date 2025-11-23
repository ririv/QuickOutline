const esbuild = require('esbuild');
const { copy } = require('esbuild-plugin-copy');
const fs = require('fs');
const path = require('path');

console.log('Start building...');

// 1. 提取 entryPoints 配置
const entryPoints = {
    'vditor': 'src/vditor/main.ts',
    'svg_preview': 'src/svg_preview/main.ts'
    // 以后增加页面只需要在这里加一行，HTML 会自动处理
};

// 2. 自定义 HTML 处理插件
const htmlAutoPlugin = {
    name: 'html-auto-plugin',
    setup(build) {
        const options = build.initialOptions;
        const outdir = options.outdir;

        build.onEnd(async () => {
            console.log('⚡ Processing HTML files...');

            // 遍历 entryPoints 的每一个 key
            for (const [key, entryPath] of Object.entries(entryPoints)) {
                const sourceDir = path.dirname(entryPath);
                const sourceHtmlPath = path.join(sourceDir, 'index.html');

                if (fs.existsSync(sourceHtmlPath)) {
                    let htmlContent = fs.readFileSync(sourceHtmlPath, 'utf-8');

                    const jsFileName = `${key}.bundle.js`;
                    const cssFileName = `${key}.bundle.css`;

                    // --- 1. 处理 CSS ---
                    // 检查是否已经手动引入了 (检测 href="name" 或 href='name')
                    const cssExists = htmlContent.includes(`href="${cssFileName}"`) || htmlContent.includes(`href='${cssFileName}'`);

                    if (cssExists) {
                        console.log(`   (Skipped CSS injection for ${key}: already exists in source)`);
                    } else {
                        // 插入到 </head> 之前
                        const cssTag = `<link rel="stylesheet" href="${cssFileName}">`;
                        // 如果找不到 </head>，就尝试找 <body>，再找不到就追加
                        if (htmlContent.includes('</head>')) {
                            htmlContent = htmlContent.replace('</head>', `    ${cssTag}\n</head>`);
                        } else {
                            htmlContent += `\n${cssTag}`;
                        }
                    }

                    // --- 2. 处理 JS ---
                    // 检查是否已经手动引入了 (检测 src="name" 或 src='name')
                    const jsExists = htmlContent.includes(`src="${jsFileName}"`) || htmlContent.includes(`src='${jsFileName}'`);

                    if (jsExists) {
                        console.log(`   (Skipped JS injection for ${key}: already exists in source)`);
                    } else {
                        // 插入到 </body> 之前
                        const jsTag = `<script src="${jsFileName}"></script>`;
                        // 如果找不到 </body>，就尝试找 </html>，再找不到就追加
                        if (htmlContent.includes('</body>')) {
                            htmlContent = htmlContent.replace('</body>', `    ${jsTag}\n</body>`);
                        } else {
                            htmlContent += `\n${jsTag}`;
                        }
                    }

                    // --- 输出文件 ---
                    const outHtmlPath = path.join(outdir, `${key}.html`);
                    fs.writeFileSync(outHtmlPath, htmlContent);
                    console.log(`   HTML Generated: ${sourceHtmlPath} -> ${outHtmlPath}`);
                } else {
                    console.warn(`   ⚠️ Warning: No index.html found for ${key} at ${sourceHtmlPath}`);
                }
            }
        });
    },
};

esbuild.build({
    entryPoints: entryPoints,
    bundle: true,
    format: 'iife',
    platform: 'browser',
    entryNames: '[name].bundle',
    outdir: '../src/main/resources/web',
    sourcemap: true,
    metafile: true,
    minify: true,
    logLevel: 'info',
    loader: {
        '.woff': 'dataurl',
        '.woff2': 'dataurl',
        '.ttf': 'dataurl',
    },
    plugins: [
        copy({
            assets: [
                { from: './node_modules/vditor/dist/*.{css,js}', to: 'vditor/dist' },
                { from: './node_modules/vditor/dist/css/**/*', to: 'vditor/dist/css' },
                { from: './node_modules/vditor/dist/js/icons/**/*', to: 'vditor/dist/js/icons' },
                { from: './node_modules/vditor/dist/js/i18n/zh_CN.js', to: 'vditor/dist/js/i18n/zh_CN.js' },
                { from: './node_modules/vditor/dist/js/i18n/en_US.js', to: 'vditor/dist/js/i18n/en_US.js' },
                { from: './node_modules/vditor/dist/js/lute/**/*', to: 'vditor/dist/js/lute' },
                { from: './node_modules/vditor/dist/js/highlight.js/*', to: 'vditor/dist/js/highlight.js/[name].[ext]' },
                { from: './node_modules/vditor/dist/js/highlight.js/styles/*', to: 'vditor/dist/js/highlight.js/styles/[name].[ext]' },
                { from: './node_modules/vditor/dist/js/mathjax/*.js', to: 'vditor/dist/js/mathjax' },
                { from: './node_modules/vditor/dist/js/mathjax/input/**/*', to: 'vditor/dist/js/mathjax/input/[path][name].[ext]' },
                { from: './assets/pdfjs/**/*', to: 'pdfjs' },
            ],
            verbose: false
        }),
        htmlAutoPlugin
    ],
})
    .then(() => console.log('Build finished successfully!'))
    .catch((e) => {
        console.error('Build failed!');
        console.error(e);
        process.exit(1);
    });