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
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><line x1="3" x2="21" y1="9" y2="9"/><path d="m9 16 3-6 3 6"/></svg>
        </button>

        <button 
            class="nav-btn" 
            class:active={activeTab === FnTab.tocGenerator} 
            onclick={() => switchTab(FnTab.tocGenerator)}
            title="TOC Generator"
        >
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="8" x2="21" y1="6" y2="6"/><line x1="8" x2="21" y1="12" y2="12"/><line x1="8" x2="21" y1="18" y2="18"/><line x1="3" x2="3.01" y1="6" y2="6"/><line x1="3" x2="3.01" y1="12" y2="12"/><line x1="3" x2="3.01" y1="18" y2="18"/></svg>
        </button>

        <button 
            class="nav-btn" 
            class:active={activeTab === FnTab.markdown} 
            onclick={() => switchTab(FnTab.markdown)}
            title="Markdown"
        >
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect width="18" height="18" x="3" y="3" rx="2" ry="2"/><path d="M7 16V8l3 4 3-4v8"/><line x1="17" x2="17" y1="8" y2="16"/></svg>
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
        <!-- Settings (Hidden by default as in FXML) -->
        <!--
        <button class="nav-btn" title="Settings">
            <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.1a2 2 0 0 1-1-1.72v-.51a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/></svg>
        </button>
        -->

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
