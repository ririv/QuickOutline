<script lang="ts">
    import { onMount } from 'svelte';
    import { rpc } from '@/lib/api/rpc';
    import { core } from '@tauri-apps/api';

    let { children } = $props();

    // 状态机：init (Android直接跳过) -> connecting -> connected / error
    let status = $state<'init' | 'connecting' | 'connected' | 'error'>('init');
    let errorMessage = $state<string>('');
    let manualPort = $state('');

    onMount(async () => {
        // @ts-ignore
        const isAndroid = typeof window['AndroidRpc'] !== 'undefined';

        if (isAndroid) {
            console.log("RpcProvider: Android environment detected.");
            status = 'connected';
            return;
        }

        await autoConnect();
    });

    async function autoConnect() {
        status = 'connecting';
        errorMessage = '';
        let port = 0;
        let source = '';

        try {
            // 1. 尝试从 Rust 获取端口 (Tauri 环境)
            try {
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
                await performConnect(port, source);
            } else {
                throw new Error(
                    "Could not determine Java Sidecar port automatically."
                );
            }
        } catch (e: any) {
            console.error("RpcProvider: Auto connection failed.", e);
            status = 'error';
            errorMessage = e.message || String(e);
        }
    }

    async function performConnect(port: number, source: string = 'Manual') {
        try {
            await rpc.connect(port);
            console.log(`RpcProvider: Connected via ${source} on port ${port}`);
            status = 'connected';
        } catch (e: any) {
            throw e;
        }
    }

    function handleManualSubmit() {
        const p = parseInt(manualPort, 10);
        if (p > 0 && p < 65536) {
            status = 'connecting';
            performConnect(p, 'Manual Input').catch((e) => {
                status = 'error';
                errorMessage = "Connection failed: " + (e.message || String(e));
            });
        } else {
            alert("Please enter a valid port number (1-65535)");
        }
    }
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
        
        <div class="manual-connect">
            <p class="hint">Enter Java Sidecar port manually:</p>
            <div class="input-group">
                <input 
                    type="number" 
                    bind:value={manualPort} 
                    placeholder="e.g. 12345" 
                    onkeydown={(e) => e.key === 'Enter' && handleManualSubmit()}
                />
                <button onclick={handleManualSubmit}>Connect</button>
            </div>
        </div>

        <p class="hint-small">
            Ensure 'SidecarApp' is running.<br>
            Check console for: <code>{`{"port": ...}`}</code>
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
        white-space: pre-wrap; 
        max-width: 80%;
        overflow-wrap: break-word;
    }

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

    .manual-connect {
        margin-top: 20px;
        background: #fff;
        padding: 20px;
        border-radius: 8px;
        border: 1px solid #eee;
        box-shadow: 0 2px 12px rgba(0,0,0,0.05);
    }

    .hint { margin: 0 0 10px 0; color: #555; }

    .input-group {
        display: flex;
        gap: 8px;
        justify-content: center;
    }

    input {
        padding: 8px 12px;
        border: 1px solid #ccc;
        border-radius: 4px;
        font-size: 16px;
        width: 120px;
    }

    button {
        padding: 8px 16px;
        background-color: #007bff;
        color: white;
        border: none;
        border-radius: 4px;
        cursor: pointer;
        font-size: 16px;
        transition: background-color 0.2s;
    }

    button:hover {
        background-color: #0056b3;
    }
    
    .hint-small {
        margin-top: 30px;
        font-size: 0.85em;
        color: #999;
        line-height: 1.5;
    }
    
    code {
        background: #eee;
        padding: 2px 4px;
        border-radius: 3px;
        font-family: monospace;
    }
</style>
