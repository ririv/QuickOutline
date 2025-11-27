import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import { resolve } from 'path';
import fs from 'fs';
import { viteStaticCopy } from 'vite-plugin-static-copy';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig({
  plugins: [
    svelte(),
    tailwindcss(),
    // 自定义插件：构建完成后将 index.html 移动到对应的资源目录，并重命名以匹配 Java 侧的加载逻辑
    {
      name: 'move-html-plugin',
      closeBundle() {
        const distDir = resolve(__dirname, '../src/main/resources/web');
        
        // 确保目标目录存在
        if (!fs.existsSync(distDir)) {
          fs.mkdirSync(distDir, { recursive: true });
        }

        // 移动并重命名 markdown/index.html -> markdown-tab.html
        const mdHtmlSrc = resolve(distDir, 'pages/markdown/index.html');
        const mdHtmlDest = resolve(distDir, 'markdown-tab.html');
        if (fs.existsSync(mdHtmlSrc)) {
           fs.renameSync(mdHtmlSrc, mdHtmlDest);
           // 清理空目录
           fs.rmSync(resolve(distDir, 'pages/markdown'), { recursive: true, force: true });
           console.log('Moved markdown/index.html to markdown-tab.html');
        }

        // 移动并重命名 toc/index.html -> toc-tab.html
        const tocHtmlSrc = resolve(distDir, 'pages/toc/index.html');
        const tocHtmlDest = resolve(distDir, 'toc-tab.html');
        if (fs.existsSync(tocHtmlSrc)) {
           fs.renameSync(tocHtmlSrc, tocHtmlDest);
           // 清理空目录
           fs.rmSync(resolve(distDir, 'pages/toc'), { recursive: true, force: true });
           console.log('Moved toc/index.html to toc-tab.html');
        }

        // 移动并重命名 pagelabel/index.html -> pagelabel-tab.html
        const pageLabelHtmlSrc = resolve(distDir, 'pages/pagelabel/index.html');
        const pageLabelHtmlDest = resolve(distDir, 'pagelabel-tab.html');
        if (fs.existsSync(pageLabelHtmlSrc)) {
           fs.renameSync(pageLabelHtmlSrc, pageLabelHtmlDest);
           // 清理空目录
           fs.rmSync(resolve(distDir, 'pages/pagelabel'), { recursive: true, force: true });
           console.log('Moved pagelabel/index.html to pagelabel-tab.html');
        }
        
        // 移动并重命名 bookmark/index.html -> bookmark-tab.html
        const bookmarkHtmlSrc = resolve(distDir, 'pages/bookmark/index.html');
        const bookmarkHtmlDest = resolve(distDir, 'bookmark-tab.html');
        if (fs.existsSync(bookmarkHtmlSrc)) {
            fs.renameSync(bookmarkHtmlSrc, bookmarkHtmlDest);
            // 清理空目录
            fs.rmSync(resolve(distDir, 'pages/bookmark'), { recursive: true, force: true });
            console.log('Moved bookmark/index.html to bookmark-tab.html');
        }

        // 移动并重命名 app/index.html -> app.html
        const appHtmlSrc = resolve(distDir, 'pages/app/index.html');
        const appHtmlDest = resolve(distDir, 'app.html');
        if (fs.existsSync(appHtmlSrc)) {
            fs.renameSync(appHtmlSrc, appHtmlDest);
            // 清理空目录
            fs.rmSync(resolve(distDir, 'pages/app'), { recursive: true, force: true });
            console.log('Moved app/index.html to app.html');
        }

        // 清理 pages 空目录 (如果存在)
        if (fs.existsSync(resolve(distDir, 'pages'))) {
            fs.rmSync(resolve(distDir, 'pages'), { recursive: true, force: true });
        }
      }
    },
    viteStaticCopy({
      targets: [
        { src: '../node_modules/vditor/dist/*.{css,js}', dest: 'vditor/dist' },
        { src: '../node_modules/vditor/dist/css/**/*', dest: 'vditor/dist/css' },
        { src: '../node_modules/vditor/dist/js/icons/**/*', dest: 'vditor/dist/js/icons' },
        { src: '../node_modules/vditor/dist/js/i18n/zh_CN.js', dest: 'vditor/dist/js/i18n' },
        { src: '../node_modules/vditor/dist/js/i18n/en_US.js', dest: 'vditor/dist/js/i18n' },
        { src: '../node_modules/vditor/dist/js/lute/**/*', dest: 'vditor/dist/js/lute' },
        { src: '../node_modules/vditor/dist/js/highlight.js/*', dest: 'vditor/dist/js/highlight.js' },
        { src: '../node_modules/vditor/dist/js/highlight.js/styles/*', dest: 'vditor/dist/js/highlight.js/styles' },
        { src: '../node_modules/vditor/dist/js/mathjax/*.js', dest: 'vditor/dist/js/mathjax' },
        { src: '../node_modules/vditor/dist/js/mathjax/input/**/*', dest: 'vditor/dist/js/mathjax/input' },
      ],
      // 目标路径相对于 build.outDir
      // verbose: true // 开启 verbose 可以看到复制了哪些文件
    })
  ],
  root: 'src', // 源码根目录改为 src
  publicDir: '../public', // 如果有纯静态资源，放在 web/public 下
  build: {
    outDir: '../../src/main/resources/web', // 输出到 Java 资源目录
    emptyOutDir: true, // 构建前清空目录
    rollupOptions: {
      input: {
        markdown: resolve(__dirname, 'src/pages/markdown/index.html'),
        toc: resolve(__dirname, 'src/pages/toc/index.html'),
        pagelabel: resolve(__dirname, 'src/pages/pagelabel/index.html'),
        bookmark: resolve(__dirname, 'src/pages/bookmark/index.html'),
        app: resolve(__dirname, 'src/pages/app/index.html'),
      },
      output: {
        entryFileNames: 'assets/[name]-[hash].js',
        chunkFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash].[ext]'
      }
    }
  },
  server: {
    port: 5173
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
      '/node_modules': resolve(__dirname, './node_modules')
    }
  }
});