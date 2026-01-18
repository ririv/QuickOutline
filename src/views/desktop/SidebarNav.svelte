<script lang="ts">
    import { appStore, FnTab } from '@/stores/appStore.svelte.ts';
    import HelpWindow from '../../components/HelpWindow.svelte';
    import Tooltip from '../../components/Tooltip.svelte';
    
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
        <Tooltip content="Bookmark" position="right">
            <button 
                class={getNavBtnClass(activeTab === FnTab.bookmark)} 
                onclick={() => switchTab(FnTab.bookmark)}
                aria-label="Bookmark"
            >
                <div class="icon icon-bookmark"></div>
            </button>
        </Tooltip>
        
        <Tooltip content="Page Label" position="right">
            <button 
                class={getNavBtnClass(activeTab === FnTab.label)} 
                onclick={() => switchTab(FnTab.label)}
                aria-label="Page Label"
            >
                <div class="icon icon-label"></div>
            </button>
        </Tooltip>

        <Tooltip content="TOC Generator" position="right">
            <button 
                class={getNavBtnClass(activeTab === FnTab.tocGenerator)} 
                onclick={() => switchTab(FnTab.tocGenerator)}
                aria-label="TOC Generator"
            >
                <div class="icon icon-toc"></div>
            </button>
        </Tooltip>

        <Tooltip content="Markdown" position="right">
            <button 
                class={getNavBtnClass(activeTab === FnTab.markdown)} 
                onclick={() => switchTab(FnTab.markdown)}
                aria-label="Markdown"
            >
                <div class="icon icon-markdown"></div>
            </button>
        </Tooltip>

        <Tooltip content="Viewer" position="right">
            <button 
                class={getNavBtnClass(activeTab === FnTab.viewer)}
                onclick={() => switchTab(FnTab.viewer)}
                aria-label="Viewer"
            >
                <div class="icon icon-viewer"></div>
            </button>
        </Tooltip>
    </div>

    <div class="flex flex-col items-center gap-2">
        {#if import.meta.env.DEV}
        <Tooltip content="Experimental Features" position="right">
            <button
                class={getNavBtnClass(activeTab === FnTab.experimental)}
                onclick={() => switchTab(FnTab.experimental)}
                aria-label="Experimental Features"
            >
                 <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M8.5 14.5A2.5 2.5 0 0 0 11 12c0-1.38-.5-2-1-3-1.072-2.143-.224-4.054 2-6 .5 2.5 2 4.9 4 6.5 2 1.6 3 3.5 3 5.5a7 7 0 1 1-14 0c0-1.1.2-2.1.5-3z"/></svg>
            </button>
        </Tooltip>
        {/if}

        <Tooltip content="Settings" position="right">
            <button
                class={getNavBtnClass(activeTab === FnTab.settings)}
                onclick={() => switchTab(FnTab.settings)}
                aria-label="Settings"
            >
                <div class="icon icon-settings"></div>
            </button>
        </Tooltip>

        <Tooltip content="Help" position="right">
            <button
                class={getNavBtnClass(false)}
                onclick={showHelp}
                aria-label="Help"
            >
                <div class="icon icon-help"></div>
            </button>
        </Tooltip>
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
    .icon-label { mask-image: url('../../assets/icons/pagenum2.svg'); -webkit-mask-image: url('../../assets/icons/pagenum2.svg'); }
    .icon-toc { mask-image: url('../../assets/icons/toc.svg'); -webkit-mask-image: url('../../assets/icons/toc.svg'); }
    .icon-markdown { mask-image: url('../../assets/icons/markdown.svg'); -webkit-mask-image: url('../../assets/icons/markdown.svg'); }
    .icon-viewer { mask-image: url('../../assets/icons/pages.svg'); -webkit-mask-image: url('../../assets/icons/pages.svg'); }
    .icon-settings { mask-image: url('../../assets/icons/settings.svg'); -webkit-mask-image: url('../../assets/icons/settings.svg'); }
    .icon-help { mask-image: url('../../assets/icons/help.svg'); -webkit-mask-image: url('../../assets/icons/help.svg'); }
</style>