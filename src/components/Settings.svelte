<script lang="ts">
    import { appStore } from '@/stores/appStore';
    import { rpc } from '@/lib/api/rpc';

    let port = $state(0);
    let currentPort = $state(0);
    let status = $state<'idle' | 'connecting' | 'connected' | 'error'>('idle');
    let message = $state('');

    // Sync with store
    appStore.subscribe(state => {
        if (state.serverPort !== currentPort) {
            currentPort = state.serverPort;
            if (port === 0) port = currentPort;
        }
    });

    async function handleConnect() {
        if (port <= 0 || port > 65535) {
            message = 'Invalid port number';
            status = 'error';
            return;
        }

        status = 'connecting';
        message = 'Connecting...';

        try {
            // Attempt to connect
            await rpc.connect(port);
            
            // Update store if successful
            appStore.setServerPort(port);
            
            status = 'connected';
            message = 'Connected successfully!';
            
            // Reset status after a delay
            setTimeout(() => {
                status = 'idle';
                message = '';
            }, 2000);
        } catch (e: any) {
            status = 'error';
            message = `Connection failed: ${e.message}`;
        }
    }
</script>

<div class="p-6 max-w-2xl mx-auto">
    <h2 class="text-2xl font-semibold mb-8 text-gray-800">Settings</h2>
    
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
                           bg-blue-600 text-white hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed" 
                    onclick={handleConnect}
                    disabled={status === 'connecting'}
                >
                    {status === 'connecting' ? 'Connecting...' : 'Connect'}
                </button>
            </div>
            <p class="text-xs text-gray-600 mt-0">Current Port: <strong class="font-bold">{currentPort || 'Not Connected'}</strong></p>
            
            {#if message}
                <div class="mt-3 px-3 py-2 rounded-md text-sm
                            {status === 'error' ? 'bg-red-50 text-red-700 border border-red-200' : ''}
                            {status === 'connected' ? 'bg-green-50 text-green-700 border border-green-200' : ''}">
                    {message}
                </div>
            {/if}
        </div>
    </div>
</div>
