<script lang="ts">
    import { onMount, onDestroy } from 'svelte';
    import { rpc } from '@/lib/api/rpc';
    import { listen } from '@tauri-apps/api/event';
    import { invoke } from '@tauri-apps/api/core';

    let { children } = $props();

    let status = $state<'init' | 'connecting' | 'connected' | 'error'>('connecting');
    let errorMessage = $state<string>('');
    let manualPort = $state('');
    let unlistenFn = $state<() => void>();

    // UX 优化：是否显示 Loading 界面
    // 默认为 false，给予 200-300ms 的宽限期，防止闪烁
    let showLoadingUI = $state(false);
    let loadingTimer: number | undefined;

    interface SidecarMessage {
        message: string;
    }

    onMount(async () => {
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

        try {
            // @ts-ignore
            if (window.__TAURI_INTERNALS__ || window.__TAURI__) {
                await setupTauriConnection();
            }
        } catch (e) {
            console.warn("Tauri setup failed:", e);
            handleError(e);
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
        clearTimeout(loadingTimer); // 清理计时器
    });

    async function performConnect(port: number, source: string = 'Manual') {
        if (status === 'connected') return;

        // 如果是重试连接，立即显示 Loading，不需要宽限期
        if (status === 'error') showLoadingUI = true;

        status = 'connecting';

        try {
            await rpc.connect(port);
            console.info(`%c[RpcProvider] 🚀 Connected via [${source}] on port ${port}`, 'color: #4caf50; font-weight: bold;');

            // 连接成功，立即清除 Loading 计时器
            clearTimeout(loadingTimer);
            status = 'connected';
        } catch (e: any) {
            console.error(`[RpcProvider] Connection failed (Source: ${source})`, e);
            handleError(e.message || String(e));
        }
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
</script>

<!-- 渲染逻辑： -->
{#if status === 'connected'}
    <!-- 1. 连接成功：直接显示内容 -->
    {@render children()}

{:else if status === 'connecting'}
    <!-- 2. 连接中：只有当超过宽限期(showLoadingUI为true)时，才显示转圈圈 -->
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
    <!-- 3. 出错：始终显示错误界面 -->
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

    /* 以下样式保持不变 */
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