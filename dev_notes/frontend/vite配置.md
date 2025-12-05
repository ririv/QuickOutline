1. `plugins`:
    * svelte(): 必须。核心插件。
    * tailwindcss(): 必须。项目使用了 Tailwind CSS。
    * viteStaticCopy: 必须。用于复制 vditor 的静态资源，前面已确认。

2. `root: 'src'`:
    * 正确。我们将源码和 index.html 都放在 src 下，这样不仅结构清晰，也简化了路径引用。

3. `build.outDir: '../build'`:
    * 正确。配合 root: 'src'，输出路径需要回退一级到项目根目录的 build，与 tauri.conf.json 的 frontendDist 一致。

4. `build.emptyOutDir: true`:
    * 正确。构建前清理旧文件，防止文件堆积。

5. `build.rollupOptions.input`:
    * test: resolve(__dirname, 'src/test/index.html'): 存疑。是否存在
      src/test/index.html？如果有且用于测试，则保留。如果没有，则应删除。
    * main: resolve(__dirname, 'src/index.html'): 正确。这是 SPA 的唯一入口。

6. `build.rollupOptions.output`:
    * entryFileNames, chunkFileNames, assetFileNames: 可选，但推荐。保持默认的文件命名规则 assets/[name]-[hash].js
      是合理的，有助于浏览器缓存管理（虽然 Tauri 应用主要是本地加载，但这种结构很清晰）。

7. `clearScreen: false`:
    * 正确。Tauri 推荐配置，防止 Vite 清屏导致看不到 Rust 端的构建日志。

8. `server` (Tauri Dev Server):
    * port: 1420: 正确。Tauri 默认端口。
    * strictPort: true: 正确。如果端口被占用则报错，而不是自动切换，这对于 Tauri 这种写死端口的应用很重要。
    * host, hmr: 正确。用于处理 Tauri 在某些环境（如 Android/iOS 开发或远程调试）下的热更新。
    * watch.ignored: ["**/src-tauri/**"]: 正确。防止前端监视器去监视后端 Rust 代码的变化，避免死循环或资源浪费。

9. `resolve.alias`:
    * '@': resolve(__dirname, './src'): 正确。常用的别名，方便引用。
    * '/node_modules': resolve(__dirname, './node_modules'): 存疑。
        * 在 MdEditor.svelte 中确实使用了 /node_modules/vditor (当 import.meta.env.DEV 为 true 时)。
        * 但是，resolve(__dirname, './node_modules') 这种写法在 vite.config.ts (根目录下) 是指向根目录的 node_modules。
        * 因为 root: 'src'，Vite 会在 src 下寻找 /node_modules。
        * 如果不配置这个 alias，Vite 可能无法解析以 /node_modules 开头的请求（因为它会去 src/node_modules 找）。
        * 所以这个配置是为了让前端代码能通过绝对路径 /node_modules/... 访问到根目录的 node_modules。这是为了支持
          MdEditor 的开发模式加载逻辑。应该保留。