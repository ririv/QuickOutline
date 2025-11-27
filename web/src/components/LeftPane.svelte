<script lang="ts">
    import { appStore, FnTab } from '../stores/appStore';
    
    let activeTab = $state(FnTab.bookmark);

    // Subscribe to the store to update local state
    appStore.subscribe(state => {
        activeTab = state.activeTab;
    });

    function switchTab(tab: FnTab) {
        appStore.switchTab(tab);
    }

    function showHelp() {
        // Placeholder for help action
        alert("Help window would open here.");
    }
</script>

<div class="left-pane">
    <div class="top-section">
        <button 
            class="nav-btn" 
            class:active={activeTab === FnTab.bookmark} 
            onclick={() => switchTab(FnTab.bookmark)}
            title="Bookmark"
        >
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z"/></svg>
        </button>
        
        <button 
            class="nav-btn" 
            class:active={activeTab === FnTab.label} 
            onclick={() => switchTab(FnTab.label)}
            title="Page Label"
        >
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-file-hash-open-v6">
                <path d="M4 20V4a2 2 0 0 1 2-2h8.5L20 7.5"/> <!-- Left, Top, and Diagonal Corner Fold -->
                <path d="M20 20V7.5"/> <!-- Right side -->
                <polyline points="14 2 14 8 20 8"/> <!-- Fold line restored -->
                
                <!-- Hash symbol shifted further down (center y=18) -->
                <line x1="9" x2="15" y1="16" y2="16"/>
                <line x1="9" x2="15" y1="20" y2="20"/>
                <line x1="10.5" x2="10.5" y1="14" y2="22"/>
                <line x1="13.5" x2="13.5" y1="14" y2="22"/>
            </svg>
        </button>

        <button 
            class="nav-btn" 
            class:active={activeTab === FnTab.tocGenerator} 
            onclick={() => switchTab(FnTab.tocGenerator)}
            title="TOC Generator"
        >
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-table-of-contents"><path d="M16 12H3"/><path d="M16 18H3"/><path d="M16 6H3"/><path d="M21 12h.01"/><path d="M21 18h.01"/><path d="M21 6h.01"/></svg>
        </button>

        <button 
            class="nav-btn" 
            class:active={activeTab === FnTab.markdown} 
            onclick={() => switchTab(FnTab.markdown)}
            title="Markdown"
        >
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-file-markdown-v6">
                <path d="M3 17V7l4.5 5 4.5-5v10"/> <!-- Balanced M shape (x=3 to 12) -->
                <line x1="18" x2="18" y1="7" y2="17"/> <!-- Arrow shaft -->
                <polyline points="15 14 18 17 21 14"/> <!-- Arrow head -->
            </svg>
        </button>

        <button 
            class="nav-btn" 
            class:active={activeTab === FnTab.preview} 
            onclick={() => switchTab(FnTab.preview)}
            title="Preview"
        >
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M2 12s3-7 10-7 10 7 10 7-3 7-10 7-10-7-10-7Z"/><circle cx="12" cy="12" r="3"/></svg>
        </button>
    </div>

    <div class="bottom-section">
        <button
            class="nav-btn"
            class:active={activeTab === FnTab.settings}
            onclick={() => switchTab(FnTab.settings)}
            title="Settings"
        >
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="lucide lucide-settings"><path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.1a2 2 0 0 1-1-1.72v-.51a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/></svg>
        </button>

        <button
            class="nav-btn"
            onclick={showHelp}
            title="Help"
        >
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"/><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/><path d="M12 17h.01"/></svg>
        </button>
    </div>
</div>

<style>
    .left-pane {
        width: 50px;
        background-color: #fff;
        border-right: 1px solid var(--color-el-default-border);
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        padding: 10px 0;
        height: 100%;
        box-sizing: border-box;
    }

    .top-section, .bottom-section {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 8px;
    }

    .nav-btn {
        width: 40px;
        height: 40px;
        display: flex;
        align-items: center;
        justify-content: center;
        border: none;
        background: transparent;
        cursor: pointer;
        border-radius: 6px;
        color: #606266;
        transition: all 0.2s;
    }

    .nav-btn:hover {
        background-color: #f5f7fa;
        color: #333;
    }

    .nav-btn.active {
        background-color: #ecf5ff;
        color: #409eff;
    }

    .nav-btn svg {
        width: 20px;
        height: 20px;
    }
</style>
