<script lang="ts">
    import { appStore, FnTab } from '../../stores/appStore.svelte.js';
    import HelpWindow from '../../components/HelpWindow.svelte';
    
    const activeTab = $derived(appStore.activeTab);
    let showHelpModal = $state(false);

    function switchTab(tab: FnTab) {
        appStore.switchTab(tab);
    }

    function showHelp() {
        showHelpModal = true;
    }

    function getNavBtnClass(isActive: boolean) {
        const base = "w-10 h-10 shrink-0 flex items-center justify-center border-none cursor-pointer rounded-md transition-all duration-200";
        
        let stateClasses = "";
        let hoverClasses = "";

        if (isActive) {
            // Active state: Gray background, default gray text (user's '对调' for active)
            stateClasses = "bg-gray-200 text-[#606266]";
            // Active hover: Slightly darker gray background, text color becomes blue
            hoverClasses = "hover:bg-gray-300 hover:text-[#409eff]";
        } else {
            // Inactive state: Transparent background, default gray text
            stateClasses = "text-[#606266]";
            // Inactive hover: Light gray background, blue text (user's '对调' for hover)
            hoverClasses = "hover:bg-[#f5f7fa] hover:text-[#409eff]";
        }

        return `${base} ${stateClasses} ${hoverClasses}`;
    }
</script>

<div class="w-[50px] bg-white border-r border-[#dcdfe6] flex flex-col justify-between py-[10px] h-full box-border shrink-0">
    <div class="flex flex-col items-center gap-2">
        <button 
            class={getNavBtnClass(activeTab === FnTab.bookmark)} 
            onclick={() => switchTab(FnTab.bookmark)}
            title="Bookmark"
        >
            <div class="icon icon-bookmark"></div>
        </button>
        
        <button 
            class={getNavBtnClass(activeTab === FnTab.label)} 
            onclick={() => switchTab(FnTab.label)}
            title="Page Label"
        >
            <div class="icon icon-label"></div>
        </button>

        <button 
            class={getNavBtnClass(activeTab === FnTab.tocGenerator)} 
            onclick={() => switchTab(FnTab.tocGenerator)}
            title="TOC Generator"
        >
            <div class="icon icon-toc"></div>
        </button>

        <button 
            class={getNavBtnClass(activeTab === FnTab.markdown)} 
            onclick={() => switchTab(FnTab.markdown)}
            title="Markdown"
        >
            <div class="icon icon-markdown"></div>
        </button>

        <button 
            class={getNavBtnClass(activeTab === FnTab.preview)} 
            onclick={() => switchTab(FnTab.preview)}
            title="Preview"
        >
            <div class="icon icon-preview"></div>
        </button>
    </div>

    <div class="flex flex-col items-center gap-2">
        {#if import.meta.env.DEV}
        <button
            class={getNavBtnClass(activeTab === FnTab.experimental)}
            onclick={() => switchTab(FnTab.experimental)}
            title="Experimental Features"
        >
             <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.1.2-2.1.5-3z"/></svg>
        </button>
        {/if}

        <button
            class={getNavBtnClass(activeTab === FnTab.settings)}
            onclick={() => switchTab(FnTab.settings)}
            title="Settings"
        >
            <div class="icon icon-settings"></div>
        </button>

        <button
            class={getNavBtnClass(false)}
            onclick={showHelp}
            title="Help"
        >
            <div class="icon icon-help"></div>
        </button>
    </div>
</div>

<HelpWindow bind:showHelpModal={showHelpModal} />

<style>
    .icon {
        width: 20px;
        height: 20px;
        background-color: currentColor;
        mask-size: contain;
        mask-repeat: no-repeat;
        mask-position: center;
        -webkit-mask-size: contain;
        -webkit-mask-repeat: no-repeat;
        -webkit-mask-position: center;
    }

    .icon-bookmark { mask-image: url('../../assets/icons/bookmark.svg'); -webkit-mask-image: url('../../assets/icons/bookmark.svg'); }
    .icon-label { mask-image: url('../../assets/icons/page-label.svg'); -webkit-mask-image: url('../../assets/icons/page-label.svg'); }
    .icon-toc { mask-image: url('../../assets/icons/toc.svg'); -webkit-mask-image: url('../../assets/icons/toc.svg'); }
    .icon-markdown { mask-image: url('../../assets/icons/markdown.svg'); -webkit-mask-image: url('../../assets/icons/markdown.svg'); }
    .icon-preview { mask-image: url('../../assets/icons/preview.svg'); -webkit-mask-image: url('../../assets/icons/preview.svg'); }
    .icon-settings { mask-image: url('../../assets/icons/settings.svg'); -webkit-mask-image: url('../../assets/icons/settings.svg'); }
    .icon-help { mask-image: url('../../assets/icons/help.svg'); -webkit-mask-image: url('../../assets/icons/help.svg'); }
</style>