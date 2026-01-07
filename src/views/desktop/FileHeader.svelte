<script lang="ts">
    import { onMount } from 'svelte';
    import { open } from '@tauri-apps/plugin-dialog';
    import { docStore } from '@/stores/docStore.svelte.ts'; // Import docStore
    import Icon from "@/components/Icon.svelte";

    const currentFilePath = $derived(docStore.currentFilePath);
    let fileName = $derived(currentFilePath ? getFileName(currentFilePath) : 'Click to Open or Drop PDF');

    function getFileName(path: string) {
        // Handle both Windows and Unix paths
        return path.split(/[\/\\]/).pop() || path;
    }

    async function handleOpen() {
        try {
            const selected = await open({
                multiple: false,
                filters: [{ name: 'PDF Files', extensions: ['pdf'] }]
            });
            
            if (selected) {
                // selected is string | null (since multiple: false)
                const path = selected as string; 
                await docStore.openFile(path); // Call docStore.openFile
            }
        } catch (e) {
            console.error("File open error:", e);
            // Fallback for browser/dev environment
            const path = prompt("Unable to open system dialog. Please enter PDF file path manually:");
            if (path) {
                await docStore.openFile(path); // Call docStore.openFile
            }
        }
    }
</script>

<header class="file-header">
    <!-- Centered Title/Prompt -->
    <!-- svelte-ignore a11y_click_events_have_key_events -->
    <div class="file-info" 
         class:clickable={!currentFilePath}
         onclick={!currentFilePath ? handleOpen : undefined}
         role="button"
         tabindex="0">
        <span class="filename" title={currentFilePath || 'Open File'}>{fileName}</span>
    </div>

    <!-- Right Actions -->
    <div class="actions">
        <button class="icon-btn open-btn" onclick={handleOpen} title="Open PDF File">
            <Icon name="folder-open" width="18" height="18" />
        </button>
    </div>
</header>

<style>
    .file-header {
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: flex-end; /* Align items to right (actions) */
        padding: 0 12px;
        background: #fff;
        border-bottom: 1px solid #e0e0e0;
        flex-shrink: 0;
        position: relative; /* For absolute positioning of title */
    }
    
    .file-info {
        position: absolute;
        left: 50%;
        transform: translateX(-50%);
        max-width: 60%;
        display: flex;
        align-items: center;
        justify-content: center;
        overflow: hidden;
    }
    
    .file-info.clickable {
        cursor: pointer;
        /* Removed border and background */
        padding: 0px 8px; /* Reduced padding */
        border-radius: 4px; /* Slight roundness */
        transition: all 0.2s ease-in-out;
    }
    
    .file-info.clickable:hover {
        background: #f0f0f0; /* Still a light background on hover */
        color: #333; /* Darker text on hover */
    }
    
    /* Removed .prompt-content and .prompt-icon styles */

    .filename {
        font-size: 14px;
        font-weight: 500;
        color: #333;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
    }
    
    .file-info.clickable .filename {
        color: #888; /* Softer text color */
        font-weight: normal; /* Less emphasis */
        font-size: 13px; /* Slightly smaller */
    }

    .actions {
        display: flex;
        align-items: center;
        /* margin-left: auto;  Not needed with justify-content: flex-end */
        z-index: 1; /* Ensure buttons are above centered title if overlap */
    }

    .icon-btn {
        width: 28px;
        height: 28px;
        border: 1px solid transparent;
        background: transparent;
        border-radius: 4px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #555;
        transition: all 0.2s;
        padding: 0;
    }
    
    .icon-btn:hover {
        background: rgba(0,0,0,0.06);
        color: #333;
    }
    
    .icon-btn:active {
        background: rgba(0,0,0,0.1);
    }
</style>
