<script lang="ts">
    import { connectionStore, type ConnectionStatus } from '@/stores/connectionStore.svelte';
    import { rpc } from '@/lib/api/rpc';

    let port = $state(0);
    const currentPort = $derived(connectionStore.serverPort);

    $effect(() => {
        if (currentPort > 0 && port === 0) {
            port = currentPort;
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
            connectionStore.setConnectionStatus('error');
            return;
        }

        connectionStore.setConnectionStatus('connecting');

        try {
            await rpc.connect(port);
            connectionStore.setServerPort(port);
            connectionStore.setConnectionStatus('connected');
        } catch (e: any) {
            connectionStore.setConnectionStatus('error');
        }
    }
</script>

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
                disabled={connectionStore.connectionStatus === 'connecting'}
            >
                {getConnectionStatusText(connectionStore.connectionStatus) === 'Connecting...' ? 'Connecting...' : 'Connect'}
            </button>
        </div>
        <p class="text-xs text-gray-600 mt-0">
            Current Port: <strong class="font-bold mr-2">{currentPort || 'Not Connected'}</strong>
            Status: <span class="font-bold {getConnectionStatusColor(connectionStore.connectionStatus)}">
                        {getConnectionStatusText(connectionStore.connectionStatus)}
                    </span>
        </p>
    </div>
</div>
