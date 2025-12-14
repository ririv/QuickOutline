<script lang="ts">
    import { appStore, type ConnectionStatus } from '@/stores/appStore'; 
    import { rpc } from '@/lib/api/rpc';
    import { printStore } from '@/stores/printStore.svelte';
    import StyledSelect from '@/components/controls/StyledSelect.svelte';

    let port = $state(0);
    let currentPort = $state(0);

    // Sync with store
    appStore.subscribe(state => {
        if (state.serverPort !== currentPort) {
            currentPort = state.serverPort;
            if (port === 0) port = currentPort;
        }
    });

    // Helper to get descriptive status string
    function getConnectionStatusText(status: ConnectionStatus): string {
        switch (status) {
            case 'init': return 'Initializing...';
            case 'connecting': return 'Connecting...';
            case 'connected': return 'Connected';
            case 'error': return 'Disconnected/Error';
            default: return 'Unknown';
        }
    }

    // Helper to get status color class
    function getConnectionStatusColor(status: ConnectionStatus): string {
        switch (status) {
            case 'connected': return 'text-green-600';
            case 'error': return 'text-red-600';
            case 'connecting': return 'text-yellow-600 animate-pulse'; // Use Tailwind pulse
            default: return 'text-gray-500';
        }
    }

    async function handleConnect() {
        if (port <= 0 || port > 65535) {
            // Update appStore status to error if invalid port
            appStore.setConnectionStatus('error');
            // Note: RpcProvider will pick up this error and start auto-reconnect if needed.
            return;
        }

        // Set status to connecting via appStore
        appStore.setConnectionStatus('connecting');

        try {
            // Attempt to connect
            await rpc.connect(port);
            
            // Update appStore if successful
            appStore.setServerPort(port);
            appStore.setConnectionStatus('connected'); // Set connected status
            
            // Note: RpcProvider already handles clearing reconnect timers etc.
            // This button click effectively acts as a manual override.
        } catch (e: any) {
            appStore.setConnectionStatus('error'); // Set error status
            // RpcProvider's auto-reconnect logic will take over if enabled
        }
    }

    const printModeOptions = [
        { label: 'Native (iOS Compatible)', value: 'Native' },
        { label: 'Headless', value: 'Headless' },
        { label: 'Headless Chrome', value: 'HeadlessChrome' }
    ];
</script>

<div class="p-6 max-w-2xl mx-auto">
    <h2 class="text-2xl font-semibold mb-8 text-gray-800">Settings</h2>
    
    <!-- PDF Generation Settings -->
    <div class="bg-white border border-gray-200 rounded-lg p-6 shadow-sm mb-6">
        <h3 class="text-lg font-semibold mb-4 text-gray-700">PDF Generation</h3>
        <div class="mb-4">
            <label class="block text-sm font-medium text-gray-700 mb-2">Print Mode</label>
            <div class="max-w-xs">
                <StyledSelect 
                    options={printModeOptions} 
                    bind:value={printStore.mode} 
                    placeholder="Select print mode..."
                    displayKey="label"
                    optionKey="label"
                />
            </div>
             <p class="text-xs text-gray-500 mt-2">
                "Native" uses the OS print service (best for Safari/iOS compatibility). "Headless Chrome" uses an embedded browser instance.
            </p>
        </div>
    </div>

    <!-- Connection Settings -->
    <div class="bg-white border border-gray-200 rounded-lg p-6 shadow-sm">
        <h3 class="text-lg font-semibold mb-4 text-gray-700">Connection</h3>
        <div class="mb-4">
            <label for="port-input" class="block text-sm font-medium text-gray-700 mb-2">Java Sidecar Port</label>
            <div class="flex gap-3 mb-2">
                <input 
                    id="port-input" 
                    type="number" 
                    bind:value={port} 
                    placeholder="e.g. 12345"
                    class="flex-1 px-3 py-2 border border-gray-300 rounded-md text-sm outline-none focus:border-blue-500 transition-colors"
                />
                <button 
                    class="px-4 py-2 rounded-md text-sm font-medium transition-all
                           bg-blue-600 text-white hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed
                           min-w-[120px] text-center" 
                    onclick={handleConnect}
                    disabled={$appStore.connectionStatus === 'connecting'}
                >
                    {getConnectionStatusText($appStore.connectionStatus) === 'Connecting...' ? 'Connecting...' : 'Connect'}
                </button>
            </div>
            <p class="text-xs text-gray-600 mt-0">
                Current Port: <strong class="font-bold mr-2">{currentPort || 'Not Connected'}</strong>
                Status: <span class="font-bold {getConnectionStatusColor($appStore.connectionStatus)}">
                            {getConnectionStatusText($appStore.connectionStatus)}
                        </span>
            </p>
        </div>
    </div>
</div>