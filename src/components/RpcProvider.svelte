<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { rpc } from '@/lib/api/rpc';
    import { listen } from '@tauri-apps/api/event';
    import { invoke } from '@tauri-apps/api/core';
    import { appStore } from '@/stores/appStore';

    let { children } = $props();

    let status = $state<'init' | 'connecting' | 'connected' | 'error'>('connecting');
    let errorMessage = $state<string>('');
    let manualPort = $state('');
    let unlistenFn = $state<() => void>();
    let reconnectTimer: number | undefined; // 自动重连计时器

    // UX 优化：是否显示 Loading 界面
    // 默认为 false，给予 200-300ms 的宽限期，防止闪烁
    let showLoadingUI = $state(false);
    let loadingTimer: number | undefined;

    interface SidecarMessage {
        message: string;
    }

    onMount(async () => {
        // 监听断开连接事件
        rpc.on('rpc-disconnected', handleDisconnect);

        // Android 环境直接秒连，不需要 Loading 逻辑
        // @ts-ignore
        const isAndroid = typeof window['AndroidRpc'] !== 'undefined';
        if (isAndroid) {
            status = 'connected';
            return;
        }

        // URL 参数也视为秒连尝试
        const params = new URLSearchParams(window.location.search);
        const portStr = params.get('port');
        if (portStr && parseInt(portStr, 10) > 0) {
            performConnect(parseInt(portStr, 10), 'URL Param');
            return;
        }

        // === 开始 Tauri 连接流程 ===

        // 1. 启动计时器：如果 300ms 后还没连上，才把 showLoadingUI 设为 true
        // 这个时间阈值可以根据你的 App 启动速度微调 (通常 200-500ms)
        loadingTimer = window.setTimeout(() => {
            if (status === 'connecting') {
                showLoadingUI = true;
            }
        }, 300);

        // === 环境检测与连接 ===
        // @ts-ignore
        const isTauri = !!(window.__TAURI_INTERNALS__ || window.__TAURI__);

        if (isTauri) {
            try {
                await setupTauriConnection();
            } catch (e) {
                console.warn("Tauri setup failed:", e);
                handleError(String(e));
            }
        } else {
            // 【关键修复】: 浏览器环境 (Vite Dev)
            // 如果不是 Tauri 环境，说明无法自动获取端口，直接显示手动连接界面
            console.warn("RpcProvider: Browser environment detected (No Tauri).");

            // 为了体验平滑，稍微延迟一点点再显示错误，或者直接显示
            // 这里选择直接显示，方便开发调试
            handleError("Browser Environment Detected.\n\nTauri APIs are unavailable in the browser.\nPlease enter the Java Sidecar port manually.");
        }
    });

    async function setupTauriConnection() {
        // A. 建立监听
        unlistenFn = await listen<SidecarMessage>('java-ready', (event) => {
            if (status === 'connected') return;
            try {
                const rawMsg = event.payload.message;
                const config = JSON.parse(rawMsg);
                if (config.port) performConnect(config.port, 'Tauri Event');
            } catch (e) {
                console.error("Payload parse failed", e);
            }
        });

        // B. 主动查询
        try {
            const port = await invoke<number>('get_java_port');
            if (port && port > 0) {
                performConnect(port, 'Active Check');
                return;
            }
        } catch (e) {
            console.debug("Active check skipped", e);
        }

        // C. 超时兜底 (4秒)
        // 注意：这里的超时仅仅是让状态变 error，和上面的 loadingTimer 不冲突
        setTimeout(() => {
            if (status === 'connecting') {
                handleError("Auto-discovery timed out. The backend service might be slow or not running.\n\nPlease enter the port manually if known.");
            }
        }, 4000);
    }

    onDestroy(() => {
        if (unlistenFn) unlistenFn();
        clearTimeout(loadingTimer);
        clearInterval(reconnectTimer); // 清理重连计时器
        rpc.off('rpc-disconnected', handleDisconnect);
    });

    async function performConnect(port: number, source: string = 'Manual') {
        if (status === 'connected') return;

        const isAutoRetry = source === 'Auto-Retry';

        // 只有非自动重连时，才更新 UI 为 "connecting" (防止闪烁)
        if (!isAutoRetry) {
            // 关键修复：如果是手动尝试或新事件，立即停止之前的自动重连循环，确保以新端口为准
            if (reconnectTimer) {
                console.log(`[RpcProvider] Manual override: Stopping previous auto-reconnect loop.`);
                clearInterval(reconnectTimer);
                reconnectTimer = undefined;
            }

            if (status === 'error') showLoadingUI = true;
            status = 'connecting';
        }

        try {
            await rpc.connect(port);
            console.info(`%c[RpcProvider] 🚀 Connected via [${source}] on port ${port}`, 'color: #4caf50; font-weight: bold;');
            
            appStore.setServerPort(port);

            // 连接成功，清理所有计时器
            clearTimeout(loadingTimer);
            clearInterval(reconnectTimer); 
            reconnectTimer = undefined;
            
            status = 'connected';
        } catch (e: any) {
            if (isAutoRetry) {
                // 静默失败，不打扰用户
                console.debug(`[RpcProvider] Auto-retry attempt failed: ${e.message}`);
            } else {
                console.error(`[RpcProvider] Connection failed (Source: ${source})`, e);
                handleError(e.message || String(e));
            }
            
            // 如果连接失败，且不在重连中，触发自动重连逻辑
            if (!reconnectTimer && port > 0) {
                initiateAutoReconnect(port);
            }
        }
    }

    function handleDisconnect(info: any) {
        console.warn("RpcProvider: Disconnected", info);
        if (status === 'connected') {
            const currentPort = appStore.serverPort || rpc.port;
            handleError(`Connection lost: ${info.reason || 'Server stopped'}.\nWaiting for recovery...`);
            
            if (currentPort > 0) {
                initiateAutoReconnect(currentPort);
            }
        }
    }

    function initiateAutoReconnect(port: number) {
        if (reconnectTimer) return; // 防止重复启动
        
        console.log(`[RpcProvider] Starting auto-reconnect loop for port ${port}...`);
        
        reconnectTimer = window.setInterval(() => {
            console.debug("[RpcProvider] Auto-reconnecting...");
            // 尝试重连，使用 'Auto-Retry' 作为源，这会复用 performConnect 的逻辑
            // 注意：performConnect 内部会处理成功时的计时器清理
            performConnect(port, 'Auto-Retry');
        }, 3000); // 每 3 秒重试一次
    }

    function handleError(msg: string) {
        status = 'error';
        errorMessage = msg;
        clearTimeout(loadingTimer);
        // 出错时肯定要显示 UI
        showLoadingUI = true;
    }

    function handleManualSubmit() {
        const p = parseInt(manualPort, 10);
        if (p > 0 && p < 65536) {
            performConnect(p, 'Manual Input');
        } else {
            alert("Please enter a valid port number (1-65535)");
        }
    }

    function handleSkipConnection() {
        status = 'connected';
    }
</script>

{#if status === 'connected'}
    {@render children()}
{:else if status === 'connecting'}
    {#if showLoadingUI}
        <div class="loading-screen fade-in">
            <div class="spinner"></div>
            <p>Connecting to backend service...</p>
            <p class="hint-small">Waiting for Java Sidecar...</p>
        </div>
    {:else}
        <!-- 宽限期内：显示空白 (防止闪烁) -->
        <!-- 如果你的 index.html 背景不是白色，可以在这里加个 div 占位 -->
    {/if}

{:else if status === 'error'}
    <div class="error-screen fade-in">
        <div class="card">
            <h2>Service Unavailable</h2>
            <pre class="error-msg">{errorMessage}</pre>

            <div class="status-indicator mb-4 flex items-center justify-center gap-3">
                <div class="relative flex h-3 w-3">
                    <span class="absolute inline-flex h-full w-full rounded-full bg-yellow-400 opacity-60 custom-ping"></span>
                    <span class="absolute inline-flex h-full w-full rounded-full bg-yellow-400 opacity-60 custom-ping delay-ping"></span>
                    <span class="relative inline-flex rounded-full h-3 w-3 bg-yellow-500 shadow-sm"></span>
                </div>
                <span class="text-yellow-600 font-semibold">Listening for Sidecar...</span>
            </div>

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
                <button onclick={handleSkipConnection} class="skip-button">Don't Connect</button>
            </div>
        </div>

        <p class="hint-small">
            Please start your external 'SidecarApp' (e.g., from your IDE) on the configured port.
            <br>
            (Check console for details: <code class="bg-gray-200 px-1 py-0.5 rounded font-mono text-gray-800">{`{"port": ...}`}</code>)
        </p>
    </div>
{/if}

<style>
    /* 添加一个简单的淡入动画，让 Loading 出现得更自然 */
    .fade-in {
        animation: fadeIn 0.3s ease-in;
    }

    @keyframes fadeIn {
        from { opacity: 0; }
        to { opacity: 1; }
    }

    .loading-screen, .error-screen {
        height: 100%;
        display: flex;
        flex-direction: column;
        align-items: center;
        justify-content: center;
        padding: 20px;
        text-align: center;
        color: #333;
        background-color: #f8f9fa; /* Light gray background for the whole screen */
    }

    .card {
        background: #fff;
        padding: 40px;
        border-radius: 12px;
        box-shadow: 0 4px 24px rgba(0,0,0,0.08);
        width: 100%;
        max-width: 500px;
        display: flex;
        flex-direction: column;
        align-items: center;
    }

    .error-screen h2 {
        color: #d32f2f; /* Error red color */
        margin-bottom: 20px;
        font-size: 1.8em;
        font-weight: 800; /* Make the title bolder */
        margin-top: 0;
    }

    .error-msg {
        color: #721c24;
        background-color: #f8d7da;
        border: 1px solid #f5c6cb;
        padding: 15px;
        border-radius: 6px;
        text-align: left;
        font-family: monospace;
        margin: 0 0 25px 0; /* Space below error message */
        white-space: pre-wrap;
        width: 100%;
        overflow-wrap: break-word;
        font-weight: normal;
        font-size: 0.9em;
        box-sizing: border-box;
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
        /* Removed independent card styles */
        width: 100%;
        display: flex;
        flex-direction: column;
        align-items: center;
    }

    .hint { margin: 0 0 10px 0; color: #555; font-weight: 500; }

    .input-group {
        display: flex;
        gap: 8px;
        justify-content: center;
        margin-bottom: 10px;
        width: 100%;
    }

    input {
        padding: 10px 14px;
        border: 1px solid #ddd;
        border-radius: 6px;
        font-size: 16px;
        width: 140px;
        outline: none;
        transition: border-color 0.2s;
    }

    input:focus {
        border-color: #007bff;
    }

    button {
        padding: 10px 20px;
        background-color: #007bff;
        color: white;
        border: none;
        border-radius: 6px;
        cursor: pointer;
        font-size: 16px;
        font-weight: 500;
        transition: all 0.2s;
    }

    button:hover {
        background-color: #0056b3;
        transform: translateY(-1px);
    }

    .manual-connect .skip-button {
        background-color: transparent;
        color: #6c757d;
        margin-top: 8px;
        font-size: 0.9em;
        padding: 8px 16px;
        font-weight: normal;
    }

    .manual-connect .skip-button:hover {
        background-color: #f1f3f5;
        color: #343a40;
        text-decoration: none;
        transform: none;
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

    .custom-ping {
        animation: ripple 2s cubic-bezier(0, 0, 0.2, 1) infinite;
    }

    .delay-ping {
        animation-delay: 1s;
    }

    @keyframes ripple {
        0% {
            transform: scale(1);
            opacity: 0.6;
        }
        75% {
            transform: scale(2.5);
            opacity: 0;
        }
        100% {
            transform: scale(2.5);
            opacity: 0;
        }
    }
</style>