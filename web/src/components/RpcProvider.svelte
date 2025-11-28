<script lang="ts">
    import { onMount } from 'svelte';
    import { rpc } from '@/lib/api/rpc';
    import { core } from '@tauri-apps/api';

    let { children } = $props();

    // 状态机：init (Android直接跳过) -> connecting -> connected / error
    let status = $state<'init' | 'connecting' | 'connected' | 'error'>('init');
    let errorMessage = $state<string>('');

    onMount(async () => {
        // @ts-ignore
        const isAndroid = typeof window['AndroidRpc'] !== 'undefined';

        if (isAndroid) {
            console.log("RpcProvider: Android environment detected.");
            status = 'connected';
            return;
        }

        status = 'connecting';
        let port = 0;
        let source = '';

        try {
            // 1. 尝试从 Rust 获取端口 (Tauri 环境)
            // 我们尝试调用 invoke，如果是在纯浏览器里，这里可能会报错或者不存在
            try {
                // 简单检测 Tauri 环境，避免在纯浏览器中抛出大量 console error
                // @ts-ignore
                if (window.__TAURI_INTERNALS__ || window.__TAURI__) {
                     port = await core.invoke<number>('get_java_sidecar_port');
                     source = 'Tauri';
                }
            } catch (e) {
                console.warn("RpcProvider: Tauri invoke failed (ignore if in browser):", e);
            }

            // 2. 如果 Rust 没返回有效端口，尝试从 URL 参数获取 (浏览器调试)
            if (port <= 0) {
                const params = new URLSearchParams(window.location.search);
                const portStr = params.get('port');
                if (portStr) {
                    port = parseInt(portStr, 10);
                    source = 'URL Param';
                }
            }

            // 3. 连接
            if (port > 0) {
                await rpc.connect(port);
                console.log(`RpcProvider: Connected via ${source} on port ${port}`);
                status = 'connected';
            } else {
                throw new Error(
                    "Could not determine Java Sidecar port.\n" +
                    "Tauri: Check 'sidecar.jar'.\n" +
                    "Browser: Use '?port=YOUR_PORT'."
                );
            }
        } catch (e: any) {
            console.error("RpcProvider: Connection failed.", e);
            status = 'error';
            errorMessage = e.message || String(e);
        }
    });
</script>

{#if status === 'connected'}
    {@render children()}
{:else if status === 'connecting'}
    <div class="loading-screen">
        <div class="spinner"></div>
        <p>Connecting to backend service...</p>
    </div>
{:else if status === 'error'}
    <div class="error-screen">
        <h2>Service Unavailable</h2>
        <pre class="error-msg">{errorMessage}</pre>
        <p class="hint">
            Please ensure the Java Sidecar is running.<br>
            If running manually, append <code>?port=YOUR_PORT</code> to the URL.
        </p>
    </div>
{:else}
    <!-- init state -->
{/if}

<style>
    .loading-screen, .error-screen {
        height: 100%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 20px;
        text-align: center;
        color: #333;
    }
    
    .error-msg { 
        color: #d32f2f; 
        font-weight: bold; 
        margin: 10px 0; 
        white-space: pre-wrap; /* Respect newlines in error message */
    }
    .hint { color: #666; font-size: 0.9em; }

    .spinner {
        width: 40px;
        height: 40px;
        border: 4px solid #ccc;
        border-top-color: #007bff;
        border-radius: 50%;
        animation: spin 1s linear infinite;
        margin-bottom: 20px;
    }

    @keyframes spin {
        to { transform: rotate(360deg); }
    }
</style>