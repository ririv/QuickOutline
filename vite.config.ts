import { defineConfig } from 'vite';
import { svelte } from '@sveltejs/vite-plugin-svelte';
import { resolve } from 'path';
import { viteStaticCopy } from 'vite-plugin-static-copy';
import tailwindcss from '@tailwindcss/vite';

const host = process.env.TAURI_DEV_HOST;
export default defineConfig({
  plugins: [
    svelte(),
    tailwindcss(),
  ],
  root: 'src', // 源码根目录改为 src
  publicDir: '../public', // 指定 public 目录位置，因为 root 改为了 src
  build: {
    outDir: '../build', // 输出到项目根目录下的 build，匹配 Tauri 配置
    emptyOutDir: true, // 构建前清空目录
    rollupOptions: {
      input: {
        test: resolve(__dirname, 'src/test/index.html'),
        main: resolve(__dirname, 'src/index.html'),
      },
      output: {
        entryFileNames: 'assets/[name]-[hash].js',
        chunkFileNames: 'assets/[name]-[hash].js',
        assetFileNames: 'assets/[name]-[hash].[ext]'
      }
    }
  },
  // Vite options tailored for Tauri development and only applied in `tauri dev` or `tauri build`
  //
  // 1. prevent Vite from obscuring rust errors
  clearScreen: false,
  // 2. tauri expects a fixed port, fail if that port is not available
  server: {
    port: 1420,
    strictPort: true,
    host: host || false,
    hmr: host
        ? {
          protocol: "ws",
          host,
          port: 1421,
        }
        : undefined,
    watch: {
      // 3. tell Vite to ignore watching `src-tauri`
      ignored: ["**/src-tauri/**"],
    },
  },
  resolve: {
    alias: {
      '@': resolve(__dirname, './src'),
      '/node_modules': resolve(__dirname, './node_modules')
    }
  }
});