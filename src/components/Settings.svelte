<script lang="ts">
    import { appStore } from '@/stores/appStore.svelte';
    import { printStore } from '@/stores/printStore.svelte';
    import StyledSelect from '@/components/controls/StyledSelect.svelte';

    const printModeOptions = [
        { label: 'Native (iOS Compatible)', value: 'Native' },
        { label: 'Headless', value: 'Headless' },
        { label: 'Headless Chrome', value: 'HeadlessChrome' }
    ];

    const editorOptions = [
        { label: 'Auto Detect', value: 'auto' },
        { label: 'VS Code', value: 'code' },
        { label: 'VS Code Insiders', value: 'code-insiders' },
        { label: 'Zed', value: 'zed' }
    ];
</script>

<div class="p-6 max-w-2xl mx-auto">
    <h2 class="text-2xl font-semibold mb-8 text-gray-800">Settings</h2>
    
    <!-- External Editor Settings -->
    <div class="bg-white border border-gray-200 rounded-lg p-6 shadow-sm mb-6">
        <h3 class="text-lg font-semibold mb-4 text-gray-700">Editor Integration</h3>
        <div class="mb-4">
            <div class="block text-sm font-medium text-gray-700 mb-2">Preferred Editor</div>
            <div class="max-w-xs">
                <StyledSelect 
                    options={editorOptions} 
                    bind:value={appStore.externalEditor} 
                    placeholder="Select editor..."
                    displayKey="label"
                    optionKey="value"
                />
            </div>
             <p class="text-xs text-gray-500 mt-2">
                The editor used for "Open in VS Code" feature. Auto Detect tries Code first, then Zed.
            </p>
        </div>
    </div>
    
    <!-- PDF Generation Settings -->
    <div class="bg-white border border-gray-200 rounded-lg p-6 shadow-sm mb-6">
        <h3 class="text-lg font-semibold mb-4 text-gray-700">PDF Generation</h3>
        <div class="mb-4">
            <div class="block text-sm font-medium text-gray-700 mb-2">Print Mode</div>
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
</div>